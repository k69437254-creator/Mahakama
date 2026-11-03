# Correction — Mahakama Audio Performance Repair Plan

**Applies to:** `MAHAKAMA_AUDIO_PERFORMANCE_REPAIR_PLAN.md`  
**Repository branch:** `audar-bridge-test`  
**Reason:** Exact Sherpa ONNX v2.0.1 Android build configuration was checked after the plan was uploaded.

## 1. Correct ONNX Runtime version

The original plan cited ONNX Runtime **1.27.1** because its `RunOptions::SetTerminate()` declaration and implementation had been inspected. That API check was valid, but 1.27.1 was not the correct version to cite for the Sherpa ONNX v2.0.1 Android build.

The Sherpa ONNX **v2.0.1** source file:

`cmake/onnxruntime-android-aarch64.cmake`

pins the Android ARM64 ONNX Runtime package to:

`onnxruntime-android-1.28.2.zip`

The corresponding Sherpa source configuration is therefore the version-specific reference that should be used when rebuilding the app's native AAR.

I separately checked the ONNX Runtime **v1.28.2** headers and confirmed that they still declare:

- `Ort::RunOptions::SetTerminate()`
- `Ort::RunOptions::UnsetTerminate()`

The declaration states that `SetTerminate()` can be called from another thread to force currently executing `Session::Run` calls that use the same `RunOptions` instance to fail.

Sources:

- Sherpa ONNX v2.0.1 Android ARM64 build configuration:  
  https://github.com/k2-fsa/sherpa-onnx/blob/v2.0.1/cmake/onnxruntime-android-aarch64.cmake
- ONNX Runtime v1.28.2 C++ API declaration:  
  https://github.com/microsoft/onnxruntime/blob/v1.28.2/include/onnxruntime/core/session/onnxruntime_cxx_api.h
- ONNX Runtime v1.28.2 C++ implementation:  
  https://github.com/microsoft/onnxruntime/blob/v1.28.2/include/onnxruntime/core/session/onnxruntime_cxx_inline.h

## 2. What changes in the implementation plan

Use ONNX Runtime **1.28.2**, not 1.27.1, as the initial target when rebuilding the ARM64 Sherpa ONNX v2.0.1 native library.

Do not mix headers from one ONNX Runtime release with a different `libonnxruntime.so`. The headers and native runtime library must come from the same pinned package/build.

Before replacing `app/libs/sherpa-onnx-2.0.1.aar`, verify:

1. The custom AAR was built from Sherpa ONNX tag `v2.0.1` with the matching ONNX Runtime 1.28.2 Android package.
2. The AAR contains the Android ABI required by the user's device (at minimum inspect `arm64-v8a` where applicable).
3. The packaged `libonnxruntime.so` and Sherpa JNI library are from the same build.
4. The new native cancellation method is present and callable from Kotlin.
5. A cancellation test can stop a long inference without releasing or destroying the model while it is in use.

## 3. Cancellation design remains conditional on device verification

The `RunOptions::SetTerminate()` API is available in the matching 1.28.2 headers. However, successful cancellation on the user's device is **not yet proven**.

The implementation must use the same `Ort::RunOptions` instance for the active `Session::Run` call and for the cross-thread termination signal. The native code must not destroy the `RunOptions`, model, or session while inference is still unwinding.

After `SetTerminate()` is requested:

- wait until the native `Session::Run` returns;
- classify the expected termination as cancellation when the request is stale;
- clear the terminate flag with `UnsetTerminate()` only after the previous run has returned;
- only then allow the next synthesis to use that shared run-options object.

The existing single synthesis worker should remain in place for this first implementation. Do not start overlapping generations on the same model.

## 4. Status

This correction fixes the ONNX Runtime version reference in the original plan. It does **not** mean the native cancellation patch has been implemented, compiled, or tested. The first plan commit and this correction are documentation only; no app behavior has changed yet.
