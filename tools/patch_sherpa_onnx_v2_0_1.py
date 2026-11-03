#!/usr/bin/env python3
"""Apply the guarded cooperative-cancellation patch to Sherpa ONNX v2.0.1."""
from __future__ import annotations
import sys
from pathlib import Path


def replace_once(root: Path, relative: str, old: str, new: str) -> None:
    path = root / relative
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"PATCH ABORTED: expected one source anchor in {relative}; found {count}")
    path.write_text(source.replace(old, new, 1), encoding="utf-8")
    print(f"PATCHED: {relative}")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: patch_sherpa_onnx_v2_0_1.py <sherpa-onnx-source-dir>")
    root = Path(sys.argv[1]).resolve()
    required = [
        "CMakeLists.txt",
        "sherpa-onnx/csrc/offline-tts.h",
        "sherpa-onnx/csrc/offline-tts-impl.h",
        "sherpa-onnx/csrc/offline-tts.cc",
        "sherpa-onnx/csrc/offline-tts-supertonic-impl.h",
        "sherpa-onnx/csrc/offline-tts-supertonic-impl.cc",
        "sherpa-onnx/jni/offline-tts.cc",
        "android/SherpaOnnxAar/sherpa_onnx/src/main/java/com/k2fsa/sherpa/onnx/Tts.kt",
    ]
    for item in required:
        if not (root / item).is_file():
            raise SystemExit(f"PATCH ABORTED: required source file missing: {item}")
    cmake = (root / "CMakeLists.txt").read_text(encoding="utf-8")
    if 'set(SHERPA_ONNX_VERSION "2.0.1")' not in cmake:
        raise SystemExit("PATCH ABORTED: this patch only targets Sherpa ONNX v2.0.1")

    replace_once(root, "sherpa-onnx/csrc/offline-tts-impl.h",
        '  virtual std::string Lang() const { return ""; }\n',
        '  virtual std::string Lang() const { return ""; }\n\n'
        '  // Reset the cancellation state before starting a new generation.\n'
        '  virtual void ResetCancellation() {}\n\n'
        '  // Request cancellation of the active generation, when supported.\n'
        '  virtual void Cancel() {}\n')
    replace_once(root, "sherpa-onnx/csrc/offline-tts.h",
        '  std::string Lang() const;\n',
        '  std::string Lang() const;\n\n'
        '  // Reset or request cancellation of the current TTS generation.\n'
        '  // Models without cancellation support keep the default no-op.\n'
        '  void ResetCancellation();\n'
        '  void Cancel();\n')
    replace_once(root, "sherpa-onnx/csrc/offline-tts.cc",
        'std::string OfflineTts::Lang() const { return impl_->Lang(); }\n',
        'std::string OfflineTts::Lang() const { return impl_->Lang(); }\n\n'
        'void OfflineTts::ResetCancellation() { impl_->ResetCancellation(); }\n\n'
        'void OfflineTts::Cancel() { impl_->Cancel(); }\n')

    replace_once(root, "sherpa-onnx/csrc/offline-tts-supertonic-impl.h",
        "#include <array>\n", "#include <array>\n#include <atomic>\n")
    replace_once(root, "sherpa-onnx/csrc/offline-tts-supertonic-impl.h",
        "  int32_t SampleRate() const override;\n",
        "  int32_t SampleRate() const override;\n\n"
        "  void ResetCancellation() override {\n"
        "    cancel_requested_.store(false, std::memory_order_release);\n"
        "  }\n\n"
        "  void Cancel() override {\n"
        "    cancel_requested_.store(true, std::memory_order_release);\n"
        "  }\n")
    replace_once(root, "sherpa-onnx/csrc/offline-tts-supertonic-impl.h",
        "  Ort::MemoryInfo memory_info_;\n",
        "  Ort::MemoryInfo memory_info_;\n"
        "  mutable std::atomic<bool> cancel_requested_{false};\n")

    cpp = "sherpa-onnx/csrc/offline-tts-supertonic-impl.cc"
    replace_once(root, cpp,
        "  Ort::Value dp_output = model_->RunDurationPredictor(\n"
        "      std::move(text_ids_tensor), std::move(style_dp_tensor),\n"
        "      std::move(text_mask_tensor));\n"
        "  auto dp_output_info = dp_output.GetTensorTypeAndShapeInfo();\n",
        "  Ort::Value dp_output = model_->RunDurationPredictor(\n"
        "      std::move(text_ids_tensor), std::move(style_dp_tensor),\n"
        "      std::move(text_mask_tensor));\n"
        "  if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "  auto dp_output_info = dp_output.GetTensorTypeAndShapeInfo();\n")
    replace_once(root, cpp,
        "          text_mask_shape.data(), text_mask_shape.size()));\n"
        "  auto text_emb_info = text_enc_output.GetTensorTypeAndShapeInfo();\n",
        "      text_mask_shape.size()));\n"
        "  if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "  auto text_emb_info = text_enc_output.GetTensorTypeAndShapeInfo();\n")
    replace_once(root, cpp,
        "  for (int32_t step = 0; step < num_steps; step++) {\n"
        "    current_step = static_cast<float>(step);\n",
        "  for (int32_t step = 0; step < num_steps; step++) {\n"
        "    if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "    current_step = static_cast<float>(step);\n")
    replace_once(root, cpp,
        "        text_emb_const, style_ttl_const, latent_mask_const, text_mask_const,\n"
        "        total_step_const);\n"
        "    auto vector_est_output_info = vector_est_output.GetTensorTypeAndShapeInfo();\n",
        "        text_emb_const, style_ttl_const, latent_mask_const, text_mask_const,\n"
        "        total_step_const);\n"
        "    if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "    auto vector_est_output_info = vector_est_output.GetTensorTypeAndShapeInfo();\n")
    replace_once(root, cpp,
        "  Ort::Value latent_tensor = Ort::Value::CreateTensor<float>(\n"
        "      memory_info_, xt_flat.data(), xt_flat.size(), latent_shape.data(),\n"
        "      latent_shape.size());\n"
        "  Ort::Value vocoder_output = model_->RunVocoder(std::move(latent_tensor));\n"
        "  auto wav_info = vocoder_output.GetTensorTypeAndShapeInfo();\n",
        "  if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "  Ort::Value latent_tensor = Ort::Value::CreateTensor<float>(\n"
        "      memory_info_, xt_flat.data(), xt_flat.size(), latent_shape.data(),\n"
        "      latent_shape.size());\n"
        "  Ort::Value vocoder_output = model_->RunVocoder(std::move(latent_tensor));\n"
        "  if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "  auto wav_info = vocoder_output.GetTensorTypeAndShapeInfo();\n")
    replace_once(root, cpp,
        "  for (int32_t i = 0; i < num_chunks; ++i) {\n"
        "    auto chunk_result =\n",
        "  for (int32_t i = 0; i < num_chunks; ++i) {\n"
        "    if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "    auto chunk_result =\n")
    replace_once(root, cpp,
        "        Process(text_chunks[i], lang, sid, num_steps, speed, gen);\n"
        "    if (chunk_result.samples.empty()) {\n",
        "        Process(text_chunks[i], lang, sid, num_steps, speed, gen);\n"
        "    if (cancel_requested_.load(std::memory_order_acquire)) return {};\n"
        "    if (chunk_result.samples.empty()) {\n")

    replace_once(root, "sherpa-onnx/jni/offline-tts.cc",
        "JNIEXPORT void JNICALL Java_com_k2fsa_sherpa_onnx_OfflineTts_delete(\n"
        "    JNIEnv * /*env*/, jobject /*obj*/, jlong ptr) {\n"
        "  delete reinterpret_cast<sherpa_onnx::OfflineTts *>(ptr);\n"
        "}\n",
        "JNIEXPORT void JNICALL Java_com_k2fsa_sherpa_onnx_OfflineTts_delete(\n"
        "    JNIEnv * /*env*/, jobject /*obj*/, jlong ptr) {\n"
        "  delete reinterpret_cast<sherpa_onnx::OfflineTts *>(ptr);\n"
        "}\n\n"
        "SHERPA_ONNX_EXTERN_C\n"
        "JNIEXPORT void JNICALL Java_com_k2fsa_sherpa_onnx_OfflineTts_resetCancellationImpl(\n"
        "    JNIEnv * /*env*/, jobject /*obj*/, jlong ptr) {\n"
        "  if (ptr == 0) return;\n"
        "  reinterpret_cast<sherpa_onnx::OfflineTts *>(ptr)->ResetCancellation();\n"
        "}\n\n"
        "SHERPA_ONNX_EXTERN_C\n"
        "JNIEXPORT void JNICALL Java_com_k2fsa_sherpa_onnx_OfflineTts_cancelImpl(\n"
        "    JNIEnv * /*env*/, jobject /*obj*/, jlong ptr) {\n"
        "  if (ptr == 0) return;\n"
        "  reinterpret_cast<sherpa_onnx::OfflineTts *>(ptr)->Cancel();\n"
        "}\n")

    kotlin = "android/SherpaOnnxAar/sherpa_onnx/src/main/java/com/k2fsa/sherpa/onnx/Tts.kt"
    replace_once(root, kotlin, "    private var ptr: Long\n", "    @Volatile private var ptr: Long\n")
    replace_once(root, kotlin, "    fun release() = finalize()\n",
        "    fun release() = finalize()\n\n"
        "    /** Clear a previous cancellation request before a new synthesis. */\n"
        "    fun resetCancellation() {\n"
        "        val nativePtr = ptr\n"
        "        if (nativePtr != 0L) resetCancellationImpl(nativePtr)\n"
        "    }\n\n"
        "    /** Request cancellation of the active Supertonic synthesis. */\n"
        "    fun cancel() {\n"
        "        val nativePtr = ptr\n"
        "        if (nativePtr != 0L) cancelImpl(nativePtr)\n"
        "    }\n")
    replace_once(root, kotlin, "    private external fun delete(ptr: Long)\n",
        "    private external fun delete(ptr: Long)\n"
        "    private external fun resetCancellationImpl(ptr: Long)\n"
        "    private external fun cancelImpl(ptr: Long)\n")

    print("PATCH RESULT: Sherpa ONNX v2.0.1 cancellation bridge applied successfully.")
    print("Behavior: cooperative checkpoints between ONNX runs; the active Session::Run is not forcibly interrupted.")

if __name__ == "__main__":
    main()
