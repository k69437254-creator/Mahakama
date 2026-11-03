package com.aram.mahakama.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class SherpaSupertonicEngine(context: Context) : AutoCloseable {

    companion object {
        private const val TAG = "SherpaSupertonic"

        private const val MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2"

        private const val MODEL_DIRECTORY = "sherpa-supertonic-3"
        private const val ARCHIVE_NAME = "supertonic-3.tar.bz2"
        private const val MAX_ARCHIVE_BYTES = 250L * 1024L * 1024L
        private const val MAX_EXTRACTED_BYTES = 400L * 1024L * 1024L

        private const val SPEAKER_ID = 9
        private const val LANGUAGE = "ar"
        private const val NUM_STEPS = 5
        private const val STREAM_CHUNK_MAX_CHARS = 70
        private const val STREAM_CHUNK_SILENCE_SECONDS = 0.3f

        private val REQUIRED_FILES = setOf(
            "duration_predictor.int8.onnx",
            "text_encoder.int8.onnx",
            "vector_estimator.int8.onnx",
            "vocoder.int8.onnx",
            "tts.json",
            "unicode_indexer.bin",
            "voice.bin"
        )
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val closed = AtomicBoolean(false)
    private val cancellationLock = Any()
    private val synthesisGeneration = AtomicLong(0L)

    private val modelDirectory =
        File(appContext.filesDir, MODEL_DIRECTORY)

    @Volatile
    private var tts: OfflineTts? = null

    @Volatile
    private var outputSampleRate: Int = 0

    val sampleRate: Int
        get() = outputSampleRate

    @Volatile
    var isReady: Boolean = false
        private set

    fun prepare(
        onProgress: (Int) -> Unit = {},
        onReady: () -> Unit,
        onError: (Throwable) -> Unit,
        onStatus: (String) -> Unit = {}
    ) {
        if (closed.get()) {
            post { onError(IllegalStateException("محرك الصوت مغلق")) }
            return
        }

        try {
            executor.execute {
                try {
                    if (closed.get()) return@execute

                    val prepareStartedAtMs = SystemClock.elapsedRealtime()
                    val engineWasInitialized = tts != null

                    if (tts == null) {
                        post { onStatus("checking") }
                        installModel(onProgress, onStatus)
                        if (closed.get()) return@execute
                        post { onStatus("initializing") }
                        initializeEngine()
                    }

                    if (closed.get()) return@execute

                    isReady = true
                    Log.i(
                        TAG,
                        "ENGINE_PREPARE_COMPLETE: elapsedMs=${SystemClock.elapsedRealtime() - prepareStartedAtMs}, reusedExistingEngine=$engineWasInitialized"
                    )
                    post {
                        if (!closed.get()) {
                            onStatus("ready")
                            onReady()
                        }
                    }
                } catch (error: Throwable) {
                    isReady = false
                    Log.e(TAG, "Engine preparation failed", error)
                    if (!closed.get()) {
                        post {
                            if (!closed.get()) onError(error)
                        }
                    }
                }
            }
        } catch (error: RejectedExecutionException) {
            post {
                onError(IllegalStateException("تعذر جدولة تحضير محرك الصوت", error))
            }
        }
    }

    fun synthesize(
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

    private fun installModel(
        onProgress: (Int) -> Unit,
        onStatus: (String) -> Unit
    ) {
        if (isModelInstalled()) {
            post {
                onStatus("verifying")
                onProgress(100)
            }
            return
        }

        post { onStatus("downloading") }

        val archive = File(appContext.cacheDir, ARCHIVE_NAME)
        val partialArchive = File(appContext.cacheDir, "$ARCHIVE_NAME.part")

        if (partialArchive.exists()) {
            partialArchive.delete()
        }

        downloadModel(partialArchive, onProgress)

        if (archive.exists()) {
            archive.delete()
        }

        if (!partialArchive.renameTo(archive)) {
            partialArchive.delete()
            throw IOException("تعذر تجهيز أرشيف النموذج")
        }

        post { onStatus("extracting") }
        try {
            extractModel(archive)
        } finally {
            archive.delete()
        }

        post { onStatus("verifying") }
        if (!isModelInstalled()) {
            throw IOException("فشل التحقق النهائي من ملفات النموذج")
        }
    }

    private fun isModelInstalled(): Boolean {
        val marker = File(modelDirectory, ".ready")

        return marker.isFile &&
            REQUIRED_FILES.all { name ->
                val file = File(modelDirectory, name)
                file.isFile && file.length() > 0L
            }
    }

    private fun downloadModel(
        destination: File,
        onProgress: (Int) -> Unit
    ) {
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mahakama-Android")
            }

            connection.connect()

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("فشل تنزيل النموذج: HTTP $status")
            }

            val declaredSize = connection.contentLengthLong
            if (declaredSize > MAX_ARCHIVE_BYTES) {
                throw IOException("حجم أرشيف النموذج أكبر من الحد المسموح")
            }

            var downloaded = 0L
            var lastReported = -1

            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(FileOutputStream(destination)).use { output ->
                    val buffer = ByteArray(64 * 1024)

                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            throw IOException("تم إيقاف تنزيل النموذج")
                        }

                        val count = input.read(buffer)
                        if (count < 0) break

                        downloaded += count

                        if (downloaded > MAX_ARCHIVE_BYTES) {
                            throw IOException("تجاوز تنزيل النموذج الحد المسموح")
                        }

                        output.write(buffer, 0, count)

                        val progress = if (declaredSize > 0L) {
                            ((downloaded * 100L) / declaredSize)
                                .toInt()
                                .coerceIn(0, 100)
                        } else {
                            0
                        }

                        if (progress != lastReported) {
                            lastReported = progress
                            post { onProgress(progress) }
                        }
                    }
                }
            }

            if (downloaded == 0L) {
                throw IOException("الأرشيف الذي تم تنزيله فارغ")
            }

            if (declaredSize > 0L && downloaded != declaredSize) {
                throw IOException("حجم الملف المنزّل لا يطابق الحجم المتوقع")
            }

            post { onProgress(100) }
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractModel(archive: File) {
        val staging = File(appContext.filesDir, "$MODEL_DIRECTORY.tmp")

        staging.deleteRecursively()

        if (!staging.mkdirs()) {
            throw IOException("تعذر إنشاء مجلد استخراج النموذج")
        }

        val extracted = mutableSetOf<String>()
        var totalBytes = 0L

        try {
            BufferedInputStream(FileInputStream(archive)).use { fileInput ->
                BZip2CompressorInputStream(fileInput).use { bzipInput ->
                    TarArchiveInputStream(bzipInput).use { tarInput ->

                        while (true) {
                            val entry = tarInput.getNextTarEntry() ?: break
                            val name = entry.name.substringAfterLast('/')

                            if (entry.isDirectory) continue
                            if (name !in REQUIRED_FILES) continue

                            if (!entry.isFile) {
                                throw IOException("نوع ملف غير مسموح داخل الأرشيف")
                            }

                            if (!extracted.add(name)) {
                                throw IOException("ملف مكرر داخل الأرشيف: $name")
                            }

                            if (entry.size <= 0L ||
                                entry.size > MAX_EXTRACTED_BYTES
                            ) {
                                throw IOException("حجم غير صالح للملف: $name")
                            }

                            val target = File(staging, name)
                            var fileBytes = 0L

                            BufferedOutputStream(FileOutputStream(target)).use { output ->
                                val buffer = ByteArray(64 * 1024)

                                while (true) {
                                    val count = tarInput.read(buffer)
                                    if (count < 0) break

                                    fileBytes += count
                                    totalBytes += count

                                    if (totalBytes > MAX_EXTRACTED_BYTES) {
                                        throw IOException("تجاوز استخراج النموذج الحد المسموح")
                                    }

                                    output.write(buffer, 0, count)
                                }
                            }

                            if (fileBytes != entry.size) {
                                throw IOException("استخراج غير مكتمل للملف: $name")
                            }
                        }
                    }
                }
            }

            if (extracted != REQUIRED_FILES) {
                val missing = REQUIRED_FILES - extracted
                throw IOException("ملفات النموذج ناقصة: ${missing.joinToString()}")
            }

            modelDirectory.deleteRecursively()

            if (!staging.renameTo(modelDirectory)) {
                throw IOException("تعذر تثبيت ملفات النموذج")
            }

            File(modelDirectory, ".ready").writeText("supertonic-3-int8")
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun initializeEngine() {
        val model = OfflineTtsSupertonicModelConfig(
            durationPredictor = modelPath("duration_predictor.int8.onnx"),
            textEncoder = modelPath("text_encoder.int8.onnx"),
            vectorEstimator = modelPath("vector_estimator.int8.onnx"),
            vocoder = modelPath("vocoder.int8.onnx"),
            ttsJson = modelPath("tts.json"),
            unicodeIndexer = modelPath("unicode_indexer.bin"),
            voiceStyle = modelPath("voice.bin")
        )

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                supertonic = model,
                numThreads = 2,
                debug = false,
                provider = "cpu"
            )
        )

        val engine = OfflineTts(null, config)

        try {
            val sampleRate = engine.sampleRate()
            val speakers = engine.numSpeakers()

            if (sampleRate <= 0) {
                throw IOException("أعاد محرك الصوت معدل عينة غير صالح: $sampleRate")
            }

            outputSampleRate = sampleRate

            if (speakers <= SPEAKER_ID) {
                throw IOException("الصوت رقم $SPEAKER_ID غير متاح؛ عدد الأصوات: $speakers")
            }

            tts = engine
            Log.i(TAG, "Supertonic ready: sampleRate=$sampleRate, speakers=$speakers")
        } catch (error: Throwable) {
            engine.release()
            throw error
        }
    }

    private fun modelPath(name: String): String =
        File(modelDirectory, name).absolutePath

    private fun isCurrentRequest(generation: Long): Boolean {
        return !closed.get() && synthesisGeneration.get() == generation
    }

    fun cancelSynthesis() {
        synchronized(cancellationLock) {
            val cancelledGeneration = synthesisGeneration.incrementAndGet()

            try {
                // Must be called directly, not queued behind native synthesis.
                tts?.cancel()
                Log.i(
                    TAG,
                    "NATIVE_SYNTHESIS_CANCEL_REQUESTED: generation=$cancelledGeneration"
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Native synthesis cancellation failed", error)
            }
        }
    }

    private fun post(action: () -> Unit) {
        Log.i(TAG, "CALLBACK_POST_BEGIN")
        val accepted = mainHandler.post {
            Log.i(TAG, "CALLBACK_POST_EXECUTED")
            action()
        }
        Log.i(TAG, "CALLBACK_POST_RESULT: accepted=$accepted")
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return

    cancelSynthesis()

        executor.execute {
            try {
                tts?.release()
            } catch (error: Exception) {
                Log.e(TAG, "Engine release failed", error)
            } finally {
                tts = null
                isReady = false
            }
        }

        executor.shutdown()
    }
}
