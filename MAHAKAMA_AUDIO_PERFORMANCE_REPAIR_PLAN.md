# Mahakama — Audio Performance & Cancellation Repair Plan

**Repository:** `k69437254-creator/Mahakama`  
**Branch reviewed:** `audar-bridge-test`  
**Reviewed app commit:** `187eda3c14feb87756d05e00a6d0042ef56ef801`  
**Sherpa binding in app:** `app/libs/sherpa-onnx-2.0.1.aar`  
**Speech model:** Supertonic 3 INT8, local CPU inference  
**Status of this document:** source-backed repair plan; it does not itself modify app code or replace the AAR.

---

## 1. Executive conclusion

There are two separate problems:

1. **Generation itself is too slow.** The measured native `generateWithConfig()` call took 47.844 seconds for 198 characters and 21.896 seconds for 83 characters. The WAV player preparation took only 18 ms, so MediaPlayer is not the bottleneck.
2. **A superseded request keeps occupying the only synthesis worker.** The second request waited 44.542 seconds in the Sherpa engine's single-thread executor because the first native inference was still running. The app invalidates the old result and prevents it from being played, but it does not stop the computation.

The recommended repair is not to create parallel TTS engines. It is to reduce the amount of work per utterance, measure the real inference stages, and add native ONNX Runtime cancellation so the current synthesis can be stopped safely before the latest request starts.

## 2. Verified baseline

The timings below came from the user's actual Android log, not estimates:

| Measurement | Observed value | Meaning |
|---|---:|---|
| First text length | 198 chars | One Supertonic text chunk under the default 300-character limit |
| First generation | 47,844 ms | Time inside `generateWithConfig()` |
| Second request submitted | 3.46 s after first started | The user moved to another question while the first was synthesizing |
| Second request executor wait | 44,542 ms | Waited behind the first native generation |
| Second generation | 21,896 ms | Time inside `generateWithConfig()` for 83 chars |
| MediaPlayer prepare | 18 ms | Not the cause of the long delay |
| Second WAV duration | 9,795 ms | About 2.23 seconds of compute per second of generated audio |

The observed real-time factor (RTF) is approximately 2.28 for the first sample (47.844 s / 20.95 s) and 2.24 for the second sample (21.896 s / 9.795 s). Lower is better; 1.0 means generation takes the same time as the resulting audio duration.

### Current app configuration confirmed in the branch

- `numThreads = 2`
- `provider = "cpu"`
- `GenerationConfig.numSteps = 8`
- `speed = 1.0f`
- `extra = mapOf("lang" to "ar")`
- Model files explicitly use `duration_predictor.int8.onnx`, `text_encoder.int8.onnx`, `vector_estimator.int8.onnx`, and `vocoder.int8.onnx`.
- `SherpaSupertonicEngine` uses `Executors.newSingleThreadExecutor()`.
- `stopSpirit()` increments the app speech generation, clears the app queue, and releases the playback player, but it does **not** cancel the active Sherpa native inference.

The model-file paths therefore point to the INT8 model. Re-downloading the model or changing the WAV player is not the next useful action.

## 3. The actual Supertonic compute path

In the exact Sherpa ONNX `v2.0.1` implementation, one text chunk runs these ONNX stages:

1. Duration Predictor — once.
2. Text Encoder — once.
3. Vector Estimator — once for every `num_steps` iteration.
4. Vocoder — once.

With this app's current `numSteps = 8`, the Vector Estimator is invoked eight times for every text chunk. The project explicitly selects 8 even though the Sherpa Kotlin `GenerationConfig` and Supertonic C++ implementation both use 5 as their normal default when no override is supplied.

**Safe first performance experiment:** change only `NUM_STEPS` from 8 to 5, build, and compare generation time and Arabic listening quality. This reduces Vector Estimator invocations from 8 to 5 (37.5% fewer calls to that stage), but it does **not** guarantee a 37.5% reduction in total synthesis time. The final decision must be based on measured latency and intelligibility/voice quality.

A 3-step or 2-step configuration is a later quality/performance experiment, not a production default. Do not select it without listening tests on Arabic text. Third-party two-step benchmark results on desktop hardware are not a reliable prediction for this Android phone.

## 4. Confirmed cancellation path: ONNX Runtime RunOptions

### Important correction to the earlier assessment

The earlier statement that native generation could not be safely stopped was incomplete. The public ONNX Runtime C++ API included with the Sherpa v2.0.1 generation supports:

```cpp
Ort::RunOptions::SetTerminate()
Ort::RunOptions::UnsetTerminate()
```

The API documentation explicitly says `SetTerminate()` can be called from another thread to force currently executing `Session::Run` calls that use that same `RunOptions` instance to fail. The ONNX Runtime `v1.27.1` C++ headers contain both methods.

Sherpa's current Supertonic implementation does not expose this capability to Kotlin. In the current source, each of the four model-stage methods constructs a fresh temporary `Ort::RunOptions{nullptr}` for its `Session::Run`. The app therefore has no shared run-options object to signal.

### Recommended native implementation

Patch the vendored/upstream Sherpa source at the exact matching release (`k2-fsa/sherpa-onnx`, tag `v2.0.1`):

1. Keep one persistent `Ort::RunOptions` object inside `OfflineTtsSupertonicModel::Impl` (or an equivalent shared cancellation controller owned by the model).
2. Pass that same object to every `Session::Run`: Duration Predictor, Text Encoder, every Vector Estimator iteration, and Vocoder.
3. Expose a thread-safe `CancelCurrentGeneration()` path down to that object. It calls `SetTerminate()` and does not free or destroy the model.
4. Expose a reset method that calls `UnsetTerminate()` **only after the cancelled native `Session::Run` has returned and before the next synthesis begins**. The existing single synthesis worker is useful here because it serializes access to the shared model and run-options object.
5. Check the cancellation state between stages and between Vector Estimator iterations. If cancelled, return an empty/cancelled result and do not run later stages or save/play a WAV.
6. Add a narrow JNI method for cancellation and a Kotlin `OfflineTts` wrapper method. Do not try to reach the native pointer through reflection or free it from another thread.
7. In the app, `stopSpirit()` first invalidates the current app generation and then calls the new native cancellation method. The next request is scheduled after the cancelled native call unwinds.
8. Treat the expected ONNX termination exception as cancellation when the app generation is stale. Delete any partial output and do not report it as a real user-visible synthesis failure.

**Why this is safe:** cancellation signals the active ONNX run through the API designed for cross-thread termination. The model, sessions, and native pointer remain alive on the synthesis worker. `release()` is still performed only after no inference is using the model.

**What it does not promise:** ONNX Runtime says termination occurs as soon as possible, not necessarily at the exact instant of the user's tap. The currently executing kernel/provider must observe the terminate signal and unwind. Device testing is required to measure cancellation latency.

### Also fix the callback contract in the Supertonic loop

The public Sherpa callback contract says returning `0` stops generation. In `v2.0.1` Supertonic's `ProcessChunksAndConcatenate()` calls the callback but ignores its return value. Fix that so a zero return stops processing subsequent chunks. This alone will not stop a one-chunk utterance during its eight inference steps; it complements the RunOptions cancellation and makes chunk cancellation conform to the documented callback contract.

## 5. Latest-request-wins scheduling in the app

The app must never spend time synthesizing speech that has already been superseded when it can be avoided.

Implement these rules in `SherpaSupertonicEngine` and `AppAudioController`:

- Every synthesis request carries the existing monotonically increasing generation/token.
- When a newer question replaces the current one, invalidate the old token immediately, request native cancellation, and remove/skip all queued stale synthesis jobs.
- Keep at most one pending synthesis request: the newest one. Do not allow a queue of obsolete texts to build up.
- Before starting a queued job, compare its token to the newest token. If stale, discard it without entering native inference.
- After native inference returns, compare the token again before saving, dispatching callbacks, or starting playback.
- A stale/cancelled request must never play its audio, invoke the current question's completion callback, or clear the current question's state.
- Keep the single synthesis worker. Do not create a second concurrent `OfflineTts` generation to make the second question appear faster; that can double model memory and CPU demand and can make both requests slower.

This is the direct fix for the 44.542-second stale-job wait once native cancellation is implemented: the old inference is terminated, queued old work is discarded, and the newest text gets the worker next.

## 6. Performance experiments worth running (one variable at a time)

### A. `numSteps`: priority 1

Test values in this order:

| Test | `numSteps` | Decision |
|---|---:|---|
| Existing baseline | 8 | Keep as quality reference |
| First candidate | 5 | Recommended first experiment; matches Sherpa's default |
| Lower-latency candidate | 3 | Try only if 5 remains too slow and quality is acceptable |
| Aggressive candidate | 2 | Optional laboratory test only; do not ship without strong listening results |

Use the same three Arabic utterances in every test: short (about 30–50 characters), medium (about 80–120), and long (about 180–220). Measure at least three warmed-up runs per setting and compare median generation time, RTF, and listening quality.

### B. XNNPACK provider: priority 2, availability must be proven

Sherpa v2.0.1 contains an XNNPACK provider branch. If the bundled ONNX Runtime build exposes `XnnpackExecutionProvider`, Sherpa can append XNNPACK; its source logs available providers and falls back to CPU if XNNPACK is unavailable.

Do not assume the project's AAR contains XNNPACK just because the source supports it. Add a startup diagnostic that prints `Ort::GetAvailableProviders()` from the native library, then test `provider = "xnnpack"` only if it is listed. Compare the exact same utterances against CPU. Keep XNNPACK only if it is measurably faster and all four Supertonic sessions initialize and generate valid audio. Otherwise retain CPU.

### C. Thread count: priority 3, measure rather than guess

`GetSessionOptionsImpl()` applies `numThreads` to both ONNX Runtime intra-op and inter-op thread counts. Supertonic creates four separate ONNX sessions (one for each model component), all using the session options. The current value 2 is not proven optimal for this phone.

Benchmark `numThreads = 1`, `2`, and `4`, one value per build, while keeping the provider and step count unchanged. Record latency, app responsiveness, device heat, and repeat-run slowdown. Do not increase thread count automatically; more threads can increase contention and power/thermal throttling on a mobile device.

### D. Speech speed: priority 4, not a primary fix

Supertonic's `speed` changes predicted speech duration. A modest test such as 1.05 can shorten the resulting audio and may reduce some compute, but it also changes speaking rate. It is not a cancellation mechanism and should not replace step/provider/thread tests. Keep 1.0 as the baseline; listen before adopting a faster setting.

### E. Stage-level instrumentation: required before advanced tuning

Add native elapsed-time logs around:

- text preprocessing and chunking;
- Duration Predictor;
- Text Encoder;
- each Vector Estimator iteration (step index and elapsed time);
- Vocoder;
- PCM assembly and JNI FloatArray transfer;
- WAV save.

Retain app-level timings for executor wait, complete generation, MediaPlayer preparation, and playback. This will show whether most time is in the eight Vector Estimator runs, another ONNX stage, or data transfer. Do not infer stage shares from character count alone.

## 7. Things that should NOT be used as the fix

- `Future.cancel(true)` or `Thread.interrupt()` alone: the current native JNI/ONNX call does not check Java thread interruption. They do not provide the needed native termination guarantee.
- Calling `OfflineTts.release()` while a native generation is running: this can destroy a native object still in use.
- Creating a second engine/executor for every new question: likely to increase peak memory, CPU contention, and heat; not a proper cancellation strategy.
- Increasing `numThreads` without measurements.
- Switching to XNNPACK without checking provider availability and validating model support.
- Treating `speed > 1` as the solution to stale request waiting.
- Re-downloading the INT8 model or changing MediaPlayer: current evidence does not point there.

## 8. Build/release implications

A Kotlin-only app commit cannot implement true mid-inference cancellation because `app/libs/sherpa-onnx-2.0.1.aar` contains the native JNI/ONNX code. A patched AAR is required for the RunOptions fix.

Recommended delivery sequence:

1. Prepare a patch set against the exact Sherpa ONNX `v2.0.1` source and its matching ONNX Runtime headers/libraries.
2. Build and inspect a custom Android AAR in a Linux GitHub Actions runner (the official Sherpa Android build instructions use Linux/macOS build tooling). Do not attempt to patch native `.so` binaries inside the AAR by hand.
3. Replace `app/libs/sherpa-onnx-2.0.1.aar` only after the patched AAR is successfully built and checked for the app's Android ABIs.
4. Add the Kotlin JNI wrapper and app latest-request-wins logic.
5. Run the existing Gradle build and unit tests, install the resulting APK, and run the cancellation/performance matrix on the actual phone.

The repository's existing Android workflow runs on pushes to `main`/`master`, pull requests into those branches, or manual dispatch. A push only to `audar-bridge-test` does not trigger it automatically; use the Actions manual dispatch or adjust the workflow's branch triggers deliberately.

## 9. Acceptance test checklist

A repair is accepted only after all of these pass on the actual device:

- [ ] A single short, medium, and long Arabic text each generates valid, audible WAV output.
- [ ] The generated spoken text matches the displayed spirit text.
- [ ] Normal completion keeps the spirit dialogue open and completes the correct current generation callback.
- [ ] Start a long utterance, move to a new question during synthesis, and verify a native cancellation log is emitted.
- [ ] The newest request starts after the current ONNX operation unwinds; it does not wait for the full old 198-character generation.
- [ ] No old/stale WAV is played after changing questions.
- [ ] Repeat rapid changes across at least 10 requests; no crash, native use-after-free, corrupted WAV, or stuck speaking state.
- [ ] Compare median generation time and RTF for 8 vs 5 steps (and optional 3) with identical text and device conditions.
- [ ] Verify the selected execution provider in logs; do not infer it from the configured string.
- [ ] Verify CPU temperature/thermal behavior and app responsiveness during repeated synthesis.

Suggested log events for the new build:

```text
SYNTHESIS_CANCEL_REQUESTED: generation=...
SYNTHESIS_CANCELLED_NATIVE: generation=..., stage=...
SYNTHESIS_STALE_JOB_DROPPED: generation=...
SYNTHESIS_STAGE: name=vector_estimator, step=..., elapsedMs=...
SYNTHESIS_GENERATE_COMPLETE: generation=..., elapsedMs=..., chars=...
```

## 10. Source references checked

All Sherpa source links below are pinned to `v2.0.1`, not to the moving `master` branch:

- Sherpa Kotlin TTS API, including `GenerationConfig` defaults and `OfflineTts` wrapper: https://github.com/k2-fsa/sherpa-onnx/blob/v2.0.1/sherpa-onnx/kotlin-api/Tts.kt
- Sherpa Supertonic generation loop and `num_steps`: https://github.com/k2-fsa/sherpa-onnx/blob/v2.0.1/sherpa-onnx/csrc/offline-tts-supertonic-impl.cc
- Supertonic model sessions and ONNX `Session::Run` calls: https://github.com/k2-fsa/sherpa-onnx/blob/v2.0.1/sherpa-onnx/csrc/offline-tts-supertonic-model.cc
- Sherpa JNI TTS bridge: https://github.com/k2-fsa/sherpa-onnx/blob/v2.0.1/sherpa-onnx/jni/offline-tts.cc
- Sherpa ONNX session options and XNNPACK provider branch: https://github.com/k2-fsa/sherpa-onnx/blob/v2.0.1/sherpa-onnx/csrc/session.cc
- ONNX Runtime v1.27.1 C++ declaration of `RunOptions::SetTerminate()` and `UnsetTerminate()`: https://github.com/microsoft/onnxruntime/blob/v1.27.1/include/onnxruntime/core/session/onnxruntime_cxx_api.h
- ONNX Runtime v1.27.1 implementation of those methods: https://github.com/microsoft/onnxruntime/blob/v1.27.1/include/onnxruntime/core/session/onnxruntime_cxx_inline.h
- ONNX Runtime API explanation of cross-thread termination: https://onnxruntime.ai/docs/api/c/struct_ort_1_1_run_options.html
- Sherpa official Android build instructions: https://k2-fsa.github.io/sherpa/onnx/android/build-sherpa-onnx.html
- Sherpa v2.0.1 release and Android AAR assets: https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1

---

## 11. Termux: add this plan to the project and push it

After downloading this Markdown file to the Android `Download` folder, run:

```bash
cd ~/Mahakama || exit 1
cp /sdcard/Download/MAHAKAMA_AUDIO_PERFORMANCE_REPAIR_PLAN.md .
git status --short
git add MAHAKAMA_AUDIO_PERFORMANCE_REPAIR_PLAN.md
git commit -m "docs: add verified audio performance repair plan"
git push origin audar-bridge-test
```

If Termux cannot access `/sdcard/Download`, run `termux-setup-storage` once, approve access, then repeat the copy command. This commit uploads the investigation/repair specification only; it does not change the installed app or claim that audio fixes have already been built.
