#!/usr/bin/env python3
"""Safely add callback-driven Supertonic PCM streaming to Mahakama.

Target snapshot: k69437254-creator/Mahakama, branch
experiment/supertonic-5steps at commit 92a58ceb61ba172a904c682e4473e32f5b8702ed.

No files are changed unless every repository, source, AAR, API, and anchor check
passes. --apply creates backups under .git/mahakama-streaming-backups, applies
three files, runs git diff --check and :app:compileDebugKotlin, and rolls back
source changes automatically if either verification fails. It never commits,
pushes, switches branches, or changes any other branch.
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from datetime import datetime
from pathlib import Path

BRANCH = "experiment/supertonic-5steps"
EXPECTED_HEAD = "92a58ceb61ba172a904c682e4473e32f5b8702ed"

ENGINE_REL = Path("app/src/main/java/com/aram/mahakama/audio/SherpaSupertonicEngine.kt")
CONTROLLER_REL = Path("app/src/main/java/com/aram/mahakama/audio/AppAudioController.kt")
PLAYER_REL = Path("app/src/main/java/com/aram/mahakama/audio/StreamingPcmPlayer.kt")
AAR_REL = Path("app/libs/sherpa-onnx-2.0.1.aar")

# Git blob IDs read from the exact target branch snapshot.
EXPECTED_BLOBS = {
    ENGINE_REL: "8eeed5f51e9164acf9aa7cf9033ee4f468f19088",
    CONTROLLER_REL: "efd18eacc32269572ac53aee35296e1e738a857e",
    AAR_REL: "ee95fdd3ea4b817298e9e367aae2a7defc16a2f5",
}

PLAYER_SOURCE = r'''package com.aram.mahakama.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** Incremental mono PCM16 playback for Supertonic callback chunks. */
internal class StreamingPcmPlayer(
    sampleRate: Int,
    private val onPlaybackComplete: () -> Unit,
    private val onPlaybackError: (Throwable) -> Unit
) {
    companion object {
        private const val TAG = "MahakamaPcmStream"
        private const val QUEUE_CAPACITY = 3
    }

    private val stopped = AtomicBoolean(false)
    private val inputFinished = AtomicBoolean(false)
    private val queue = ArrayBlockingQueue<ShortArray>(QUEUE_CAPACITY)
    private val audioTrack: AudioTrack
    private val playbackThread: Thread

    init {
        require(sampleRate > 0) { "معدل عينة الصوت غير صالح: $sampleRate" }

        val minBufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferBytes <= 0) {
            throw IOException("تعذر تحديد حجم مخزن AudioTrack: $minBufferBytes")
        }

        val bufferBytes = maxOf(minBufferBytes * 2, sampleRate)
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (created.state != AudioTrack.STATE_INITIALIZED) {
            try { created.release() } catch (_: Exception) { }
            throw IOException("تعذر تهيئة AudioTrack للبث الصوتي")
        }

        try { created.setVolume(1.0f) } catch (_: Exception) { }
        audioTrack = created
        playbackThread = Thread({ consumeAudio() }, "MahakamaPcmPlayback").apply {
            start()
        }
    }

    /** Copies callback samples into a bounded queue; returns false after stop. */
    fun enqueue(samples: FloatArray): Boolean {
        if (samples.isEmpty()) return true
        if (stopped.get() || inputFinished.get()) return false

        val pcm = ShortArray(samples.size)
        for (index in samples.indices) {
            val sample = samples[index]
            if (!sample.isFinite()) {
                throw IOException("وصلت عينة صوتية غير رقمية إلى مشغل البث")
            }
            pcm[index] = (sample.coerceIn(-1.0f, 1.0f) * 32767.0f)
                .roundToInt()
                .toShort()
        }

        while (!stopped.get() && !inputFinished.get()) {
            try {
                if (queue.offer(pcm, 100, TimeUnit.MILLISECONDS)) return true
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    /** Called only after Sherpa has returned from its final callback. */
    fun finishInput() {
        inputFinished.set(true)
    }

    /** Stops immediately; the playback thread releases AudioTrack in its finally block. */
    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        queue.clear()
        try { audioTrack.pause() } catch (_: Exception) { }
        try { audioTrack.flush() } catch (_: Exception) { }
        playbackThread.interrupt()
    }

    private fun consumeAudio() {
        var started = false
        var writtenFrames = 0L
        var completedNormally = false
        var failure: Throwable? = null

        try {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: SecurityException) {
            }

            while (!stopped.get()) {
                val chunk = queue.poll(100, TimeUnit.MILLISECONDS)
                if (chunk != null) {
                    if (!started) {
                        audioTrack.play()
                        started = true
                        Log.i(TAG, "PCM_STREAM_PLAYBACK_STARTED: sampleRate=${audioTrack.sampleRate}")
                    }

                    var offset = 0
                    while (offset < chunk.size && !stopped.get()) {
                        val written = audioTrack.write(
                            chunk,
                            offset,
                            chunk.size - offset,
                            AudioTrack.WRITE_NON_BLOCKING
                        )
                        when {
                            written > 0 -> {
                                offset += written
                                writtenFrames += written.toLong()
                            }
                            written == 0 -> Thread.sleep(5)
                            else -> throw IOException("فشل AudioTrack.write: $written")
                        }
                    }
                } else if (inputFinished.get() && queue.isEmpty()) {
                    break
                }
            }

            // Do not report completion until AudioTrack has played every written frame.
            if (!stopped.get() && started) {
                while (!stopped.get()) {
                    val playedFrames = audioTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                    if (playedFrames >= writtenFrames) break
                    Thread.sleep(10)
                }
            }
            completedNormally = !stopped.get()
        } catch (error: InterruptedException) {
            if (!stopped.get()) failure = error
        } catch (error: Throwable) {
            if (!stopped.get()) failure = error
        } finally {
            // Close the producer side before releasing the track. This prevents a
            // late callback from adding audio after the playback thread has exited.
            stopped.set(true)
            queue.clear()
            try {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
            } catch (_: Exception) {
            }
            try { audioTrack.release() } catch (_: Exception) { }
        }

        when {
            failure != null -> onPlaybackError(failure!!)
            completedNormally -> onPlaybackComplete()
        }
    }
}
'''

NEW_SYNTHESIZE = r'''    fun synthesize(
        text: String,
        onAudioChunk: (FloatArray, Int) -> Boolean,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (closed.get()) {
            post { onError(IllegalStateException("محرك الصوت مغلق")) }
            return
        }

        val requestGeneration = synchronized(cancellationLock) {
            synthesisGeneration.get()
        }
        val executorQueuedAtMs = SystemClock.elapsedRealtime()

        try {
            executor.execute {
                try {
                    if (!isCurrentRequest(requestGeneration)) return@execute

                    Log.i(
                        TAG,
                        "SYNTHESIS_EXECUTOR_START: waitMs=${SystemClock.elapsedRealtime() - executorQueuedAtMs}, chars=${text.length}, generation=$requestGeneration"
                    )

                    val engine = tts
                        ?: throw IllegalStateException("محرك الصوت لم تتم تهيئته")
                    require(text.isNotBlank()) { "النص الصوتي فارغ" }

                    // Serialize reset against cancellation; never hold this lock during inference.
                    val mayStart = synchronized(cancellationLock) {
                        if (
                            closed.get() ||
                            synthesisGeneration.get() != requestGeneration ||
                            tts !== engine
                        ) {
                            false
                        } else {
                            engine.resetCancellation()
                            true
                        }
                    }
                    if (!mayStart || !isCurrentRequest(requestGeneration)) return@execute

                    val sampleRate = outputSampleRate
                    if (sampleRate <= 0) {
                        throw IOException("معدل عينة محرك الصوت غير صالح")
                    }

                    val config = GenerationConfig(
                        sid = SPEAKER_ID,
                        numSteps = NUM_STEPS,
                        speed = 1.0f,
                        extra = mapOf(
                            "lang" to LANGUAGE,
                            "max_len" to STREAM_CHUNK_MAX_CHARS.toString(),
                            "silence_duration" to STREAM_CHUNK_SILENCE_SECONDS.toString()
                        )
                    )

                    var callbackChunkCount = 0
                    var callbackFailure: Throwable? = null
                    var firstChunkLatencyMs = -1L
                    val generationStartedAtMs = SystemClock.elapsedRealtime()
                    Log.i(
                        TAG,
                        "SYNTHESIS_STREAM_BEGIN: chars=${text.length}, maxChunkCodepoints=$STREAM_CHUNK_MAX_CHARS, generation=$requestGeneration"
                    )

                    // Sherpa v2.0.1 calls back once for each synthesized text chunk.
                    // Its Supertonic implementation does not use the callback's Int return
                    // value to break the native chunk loop. Therefore, after a delivery error
                    // we stop accepting later chunks and report the error when native returns;
                    // real cancellation remains the existing cancelSynthesis() path.
                    val audio = engine.generateWithConfigAndCallback(text, config) { samples ->
                        if (!isCurrentRequest(requestGeneration)) {
                            0
                        } else if (callbackFailure != null) {
                            0
                        } else if (samples.isEmpty()) {
                            1
                        } else {
                            try {
                                var accepted = true
                                if (callbackChunkCount > 0) {
                                    val silenceSamples =
                                        (sampleRate * STREAM_CHUNK_SILENCE_SECONDS).toInt()
                                    if (silenceSamples > 0) {
                                        accepted = onAudioChunk(FloatArray(silenceSamples), sampleRate)
                                    }
                                }
                                if (accepted) {
                                    accepted = onAudioChunk(samples, sampleRate)
                                }

                                if (accepted) {
                                    if (callbackChunkCount == 0) {
                                        firstChunkLatencyMs =
                                            SystemClock.elapsedRealtime() - generationStartedAtMs
                                    }
                                    callbackChunkCount++
                                    Log.i(
                                        TAG,
                                        "SYNTHESIS_STREAM_CHUNK: index=$callbackChunkCount, samples=${samples.size}, durationMs=${samples.size * 1000L / sampleRate}, firstChunkLatencyMs=$firstChunkLatencyMs, generation=$requestGeneration"
                                    )
                                    1
                                } else {
                                    callbackFailure = IOException(
                                        "مشغل الصوت لم يستقبل دفعة التوليد"
                                    )
                                    Log.e(TAG, "SYNTHESIS_STREAM_DELIVERY_REJECTED: generation=$requestGeneration")
                                    0
                                }
                            } catch (error: Throwable) {
                                callbackFailure = error
                                0
                            }
                        }
                    }

                    if (!isCurrentRequest(requestGeneration)) return@execute
                    callbackFailure?.let {
                        throw IOException("تعذر تسليم إحدى دفعات الصوت إلى المشغل", it)
                    }
                    if (audio.samples.isEmpty() || callbackChunkCount == 0) {
                        throw IOException("لم ينتج Supertonic أي دفعة صوتية قابلة للتشغيل")
                    }
                    if (audio.sampleRate != sampleRate) {
                        throw IOException(
                            "معدل عينة الناتج لا يطابق مشغل البث: ${audio.sampleRate} بدلًا من $sampleRate"
                        )
                    }

                    Log.i(
                        TAG,
                        "SYNTHESIS_STREAM_COMPLETE: elapsedMs=${SystemClock.elapsedRealtime() - generationStartedAtMs}, firstChunkLatencyMs=$firstChunkLatencyMs, chars=${text.length}, chunks=$callbackChunkCount, samples=${audio.samples.size}, generation=$requestGeneration"
                    )

                    post {
                        if (isCurrentRequest(requestGeneration)) {
                            onComplete()
                        } else {
                            Log.w(TAG, "STREAM_COMPLETION_SKIPPED: stale generation=$requestGeneration")
                        }
                    }
                } catch (error: Throwable) {
                    if (!isCurrentRequest(requestGeneration)) {
                        Log.i(TAG, "SYNTHESIS_CANCELLED: generation=$requestGeneration")
                        return@execute
                    }

                    Log.e(TAG, "Streaming speech generation failed", error)
                    post {
                        if (isCurrentRequest(requestGeneration)) onError(error)
                    }
                }
            }
        } catch (error: RejectedExecutionException) {
            if (isCurrentRequest(requestGeneration)) {
                post {
                    if (isCurrentRequest(requestGeneration)) {
                        onError(IllegalStateException("تعذر جدولة توليد الصوت", error))
                    }
                }
            }
        }
    }

'''

NEW_CONTROLLER_SYNTHESIZE = r'''    @Synchronized
    private fun synthesizeSherpa(text: String, generation: Long) {
        if (released || generation != speechGeneration) return

        releaseSpeechPlayer()
        val sampleRate = sherpaEngine.sampleRate
        if (sampleRate <= 0) {
            Log.e(TAG, "Streaming playback rejected: invalid Sherpa sample rate=$sampleRate")
            finishSpeakingIfCurrent(generation)
            return
        }

        val streamPlayer = try {
            StreamingPcmPlayer(
                sampleRate = sampleRate,
                onPlaybackComplete = {
                    mainHandler.post {
                        synchronized(this@AppAudioController) {
                            if (!released && generation == speechGeneration) {
                                if (streamingSpeechPlayer === streamPlayer) {
                                    streamingSpeechPlayer = null
                                }
                                Log.i(TAG, "Streaming speech playback drained: generation=$generation")
                                finishSpeakingIfCurrent(generation)
                            }
                        }
                    }
                },
                onPlaybackError = { error ->
                    mainHandler.post {
                        synchronized(this@AppAudioController) {
                            if (!released && generation == speechGeneration) {
                                Log.e(TAG, "Streaming AudioTrack playback failed", error)
                                if (streamingSpeechPlayer === streamPlayer) {
                                    streamingSpeechPlayer = null
                                }
                                streamPlayer.stop()
                                sherpaEngine.cancelSynthesis()
                                finishSpeakingIfCurrent(generation)
                            }
                        }
                    }
                }
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Could not initialize streaming AudioTrack", error)
            finishSpeakingIfCurrent(generation)
            return
        }

        streamingSpeechPlayer = streamPlayer
        Log.i(
            TAG,
            "Sherpa streaming playback ready: generation=$generation, sampleRate=$sampleRate"
        )

        sherpaEngine.synthesize(
            text = text,
            onAudioChunk = { samples, chunkSampleRate ->
                !released &&
                    generation == speechGeneration &&
                    chunkSampleRate == sampleRate &&
                    streamPlayer.enqueue(samples)
            },
            onComplete = {
                if (!released && generation == speechGeneration) {
                    streamPlayer.finishInput()
                } else {
                    streamPlayer.stop()
                }
            },
            onError = { error ->
                synchronized(this@AppAudioController) {
                    if (!released && generation == speechGeneration) {
                        Log.e(TAG, "Supertonic streaming synthesis failed", error)
                        if (streamingSpeechPlayer === streamPlayer) {
                            streamingSpeechPlayer = null
                        }
                        streamPlayer.stop()
                        finishSpeakingIfCurrent(generation)
                    } else {
                        streamPlayer.stop()
                    }
                }
            }
        )
    }

'''


def abort(message: str) -> None:
    raise SystemExit(f"PATCH ABORTED: {message}")


def run_git(root: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", *args], cwd=root, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False
    )
    if result.returncode != 0:
        abort(f"git {' '.join(args)} failed: {result.stdout.strip()}")
    return result.stdout.strip()


def replace_exact(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        abort(f"expected exactly one anchor for {label}; found {count}")
    return source.replace(old, new, 1)


def replace_between(source: str, start: str, end: str, replacement: str, label: str) -> str:
    if source.count(start) != 1:
        abort(f"expected exactly one start marker for {label}; found {source.count(start)}")
    if source.count(end) != 1:
        abort(f"expected exactly one end marker for {label}; found {source.count(end)}")
    a = source.index(start)
    b = source.index(end, a)
    if b <= a:
        abort(f"invalid marker order for {label}")
    return source[:a] + replacement + source[b:]


def verify_aar(aar_path: Path) -> None:
    if not aar_path.is_file():
        abort(f"required AAR is missing: {aar_path}")
    if shutil.which("javap") is None:
        abort("javap is unavailable; no project files were changed")

    try:
        with zipfile.ZipFile(aar_path) as archive:
            bad_member = archive.testzip()
            if bad_member is not None:
                abort(f"AAR archive is corrupt at member: {bad_member}")
            try:
                jar_bytes = archive.read("classes.jar")
            except KeyError:
                abort("classes.jar is missing from the Sherpa AAR")
    except (zipfile.BadZipFile, OSError) as error:
        abort(f"could not read the Sherpa AAR safely: {error}")

    with tempfile.TemporaryDirectory(prefix="mahakama-sherpa-check-") as temp:
        jar_path = Path(temp) / "classes.jar"
        jar_path.write_bytes(jar_bytes)
        try:
            result = subprocess.run(
                ["javap", "-classpath", str(jar_path), "-p", "com.k2fsa.sherpa.onnx.OfflineTts"],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False
            )
        except OSError as error:
            abort(f"could not execute javap: {error}")
        if result.returncode != 0:
            abort("javap could not inspect OfflineTts in the AAR: " + result.stdout.strip())
        required = (
            "generateWithConfigAndCallback",
            "resetCancellation",
            "cancel(",
        )
        missing = [name for name in required if name not in result.stdout]
        if missing:
            abort("Sherpa AAR is missing required OfflineTts API(s): " + ", ".join(missing))


def build_candidate(root: Path) -> dict[Path, str]:
    engine_path = root / ENGINE_REL
    controller_path = root / CONTROLLER_REL
    player_path = root / PLAYER_REL
    aar_path = root / AAR_REL

    for path in (engine_path, controller_path, aar_path):
        if not path.is_file():
            abort(f"required file is missing: {path.relative_to(root)}")
    if player_path.exists():
        abort(f"{PLAYER_REL} already exists; refusing to overwrite it")

    # No modifications to any of the three source targets or the AAR are allowed.
    status = run_git(
        root, "status", "--porcelain", "--",
        str(ENGINE_REL), str(CONTROLLER_REL), str(PLAYER_REL), str(AAR_REL)
    )
    if status:
        abort("target files have local modifications; preserve/review them before patching:\n" + status)

    head = run_git(root, "rev-parse", "HEAD")
    if head != EXPECTED_HEAD:
        abort(
            f"unexpected HEAD {head}; this script only targets the reviewed snapshot "
            f"{EXPECTED_HEAD}. Do not bypass this check."
        )
    branch = run_git(root, "branch", "--show-current")
    if branch != BRANCH:
        abort(f"wrong branch '{branch}'; switch to '{BRANCH}' first")

    for path, expected in EXPECTED_BLOBS.items():
        actual = run_git(root, "hash-object", str(path))
        if actual != expected:
            abort(
                f"{path} does not match the reviewed branch file (blob {actual}); "
                "no files were changed"
            )

    verify_aar(aar_path)

    engine_source = engine_path.read_text(encoding="utf-8")
    controller_source = controller_path.read_text(encoding="utf-8")

    if "SYNTHESIS_STREAM_BEGIN" in engine_source or "streamingSpeechPlayer" in controller_source:
        abort("streaming patch markers already exist; refusing to apply twice")
    if "private const val NUM_STEPS = 5\n" not in engine_source:
        abort("the reviewed engine no longer has NUM_STEPS = 5")

    engine_source = replace_exact(
        engine_source,
        "    @Volatile\n    private var outputSampleRate: Int = 0\n\n",
        "    @Volatile\n    private var outputSampleRate: Int = 0\n\n"
        "    val sampleRate: Int\n"
        "        get() = outputSampleRate\n\n",
        "Sherpa sample-rate accessor"
    )

    engine_source = replace_exact(
        engine_source,
        "        private const val NUM_STEPS = 5\n",
        "        private const val NUM_STEPS = 5\n"
        "        private const val STREAM_CHUNK_MAX_CHARS = 70\n"
        "        private const val STREAM_CHUNK_SILENCE_SECONDS = 0.3f\n",
        "stream chunk settings (preserve existing five inference steps)"
    )

    engine_source = replace_between(
        engine_source,
        "    fun synthesize(\n",
        "    private fun installModel(\n",
        NEW_SYNTHESIZE,
        "Sherpa streaming synthesize method"
    )

    controller_source = replace_exact(
        controller_source,
        "    private var speechPlayer: MediaPlayer? = null\n",
        "    private var speechPlayer: MediaPlayer? = null\n"
        "    private var streamingSpeechPlayer: StreamingPcmPlayer? = null\n",
        "stream player field"
    )

    controller_source = replace_between(
        controller_source,
        "    @Synchronized\n    private fun synthesizeSherpa(",
        "    @Synchronized\n    private fun playSpeechFile(",
        NEW_CONTROLLER_SYNTHESIZE,
        "controller streaming synthesize method"
    )

    controller_source = replace_exact(
        controller_source,
        "    private fun releaseSpeechPlayer() {\n",
        "    private fun releaseSpeechPlayer() {\n"
        "        val stream = streamingSpeechPlayer\n"
        "        streamingSpeechPlayer = null\n"
        "        try { stream?.stop() } catch (_: Exception) { }\n\n",
        "stream player release hook"
    )

    # Final invariants on the proposed in-memory source, before any write.
    required_engine = (
        "generateWithConfigAndCallback",
        "STREAM_CHUNK_MAX_CHARS = 70",
        "STREAM_CHUNK_SILENCE_SECONDS = 0.3f",
        "private const val NUM_STEPS = 5",
        "SYNTHESIS_STREAM_BEGIN",
        "callbackFailure != null",
    )
    for marker in required_engine:
        if marker not in engine_source:
            abort(f"candidate engine is missing required marker: {marker}")
    for marker in ("StreamingPcmPlayer(", "streamPlayer.finishInput()", "streamingSpeechPlayer"):
        if marker not in controller_source:
            abort(f"candidate controller is missing required marker: {marker}")
    if "AudioTrack.MODE_STREAM" not in PLAYER_SOURCE or "WRITE_NON_BLOCKING" not in PLAYER_SOURCE:
        abort("candidate AudioTrack player failed its internal checks")

    return {
        engine_path: engine_source,
        controller_path: controller_source,
        player_path: PLAYER_SOURCE,
    }


def atomic_write(path: Path, content: str) -> None:
    fd, temp_name = tempfile.mkstemp(prefix=path.name + ".tmp.", dir=path.parent)
    temp_path = Path(temp_name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(content)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_path, path)
    finally:
        try:
            temp_path.unlink()
        except FileNotFoundError:
            pass


def rollback(changes: dict[Path, str], backups: dict[Path, Path], created_player: bool) -> None:
    for original, backup in backups.items():
        try:
            shutil.copy2(backup, original)
        except Exception as error:
            print(f"ROLLBACK WARNING: could not restore {original}: {error}", file=sys.stderr)
    player_path = next((p for p in changes if p.name == "StreamingPcmPlayer.kt"), None)
    if created_player and player_path is not None:
        try:
            player_path.unlink(missing_ok=True)
        except Exception as error:
            print(f"ROLLBACK WARNING: could not remove {player_path}: {error}", file=sys.stderr)


def main() -> None:
    parser = argparse.ArgumentParser(description="Safely add Mahakama Supertonic streaming")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="run all preflight checks without modifying files")
    mode.add_argument("--apply", action="store_true", help="apply patch, compile Kotlin, and rollback on failure")
    args = parser.parse_args()

    root = Path.cwd().resolve()
    top = Path(run_git(root, "rev-parse", "--show-toplevel")).resolve()
    if top != root:
        abort("run this script from the repository root (~/Mahakama), not a subdirectory")
    if not (root / "gradlew").is_file():
        abort("Gradle wrapper ./gradlew is missing; no files were changed")

    changes = build_candidate(root)
    print("PREFLIGHT PASSED")
    print(f"- Branch: {BRANCH}")
    print(f"- Reviewed commit: {EXPECTED_HEAD}")
    print("- Source files and AAR match the reviewed GitHub blobs")
    print("- AAR OfflineTts callback and existing cancellation APIs verified with javap")
    print("- Sherpa step count remains 5; text chunk limit is 70 codepoints")
    print("- Existing source anchors are unique; no files have been modified")
    if args.check:
        print("CHECK ONLY: no files were changed. Run with --apply to install the patch.")
        return

    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_dir = root / ".git" / "mahakama-streaming-backups" / stamp
    if backup_dir.exists():
        abort(f"backup directory already exists: {backup_dir}")
    backup_dir.mkdir(parents=True, exist_ok=False)

    backups: dict[Path, Path] = {}
    created_player = False
    try:
        for path in (root / ENGINE_REL, root / CONTROLLER_REL):
            backup = backup_dir / path.name
            shutil.copy2(path, backup)
            backups[path] = backup

        for path, content in changes.items():
            if path.name == "StreamingPcmPlayer.kt" and path.exists():
                abort(f"{path.relative_to(root)} appeared during preflight; refusing overwrite")
            atomic_write(path, content)
            if path.name == "StreamingPcmPlayer.kt":
                created_player = True

        diff_check = subprocess.run(
            [
                "git", "diff", "--check", "--",
                str(ENGINE_REL), str(CONTROLLER_REL)
            ], cwd=root, text=True,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False
        )
        if diff_check.returncode != 0:
            abort("git diff --check failed:\n" + diff_check.stdout.strip())

        print("PATCH WRITTEN; running Kotlin compile verification...")
        build = subprocess.run(
            ["bash", "./gradlew", ":app:compileDebugKotlin"], cwd=root, check=False
        )
        if build.returncode != 0:
            abort(f"Gradle Kotlin compilation failed (exit {build.returncode}); source rollback will run")

    except BaseException as error:
        rollback(changes, backups, created_player)
        print(f"PATCH ROLLED BACK. Backups retained in: {backup_dir.relative_to(root)}", file=sys.stderr)
        if isinstance(error, SystemExit):
            raise
        if isinstance(error, KeyboardInterrupt):
            abort("interrupted by user; source rollback was attempted")
        abort(f"application failed; source rollback was attempted: {error}")

    print("STREAMING PATCH APPLIED AND KOTLIN COMPILE PASSED")
    print(f"- Updated: {ENGINE_REL}")
    print(f"- Updated: {CONTROLLER_REL}")
    print(f"- Created: {PLAYER_REL}")
    print(f"- Backups: {backup_dir.relative_to(root)}")
    print("- No commit or push was performed")
    print("NEXT: inspect with git diff --stat && git status --short, then install and test on Android")


if __name__ == "__main__":
    main()
