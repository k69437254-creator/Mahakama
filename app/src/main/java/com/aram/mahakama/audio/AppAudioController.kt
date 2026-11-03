package com.aram.mahakama.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.aram.mahakama.R
import java.io.File
import java.util.ArrayDeque

/**
 * One continuous ambient soundtrack and the Arabic voice of "طيف الذات".
 * During speech the same soundtrack remains audible at a lower, gently faded level;
 * no extra section-specific track or sound effect is introduced.
 */
class AppAudioController(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var orchestraIntroPlayer: MediaPlayer? = null
    private var playerTrackId: String? = null
    private var loadingPlayer: MediaPlayer? = null
    private var playerLoading = false
    private var playerGeneration = 0L
    @Volatile private var released = false
    private var selectedTrackId: String = "death"
    private var trackSwitchPending = false
    private var trackSwitchRunnable: Runnable? = null
    private var masterVolume = 0.82f
    private var appIsForeground = false
    private var muted = false
    private var speaking = false
    private data class QueuedSpeech(
        val text: String,
        val onFinished: () -> Unit,
        val queuedAtMs: Long = SystemClock.elapsedRealtime()
    )

    private val speechQueue = ArrayDeque<QueuedSpeech>()
    private var speechRequestActive = false
    private var finishedCallback: (() -> Unit)? = null

    private val sherpaEngine = SherpaSupertonicEngine(appContext)
    private var sherpaPreparing = false
    private var pendingSpeechText: String? = null
    private var pendingSpeechGeneration = 0L
    private var speechPlayer: MediaPlayer? = null
    private var streamingSpeechPlayer: StreamingPcmPlayer? = null

    @Volatile private var speechGeneration = 0L
    private var volumeAnimator: ValueAnimator? = null
    private var currentMusicVolume = 0f

    private fun isPlayingSafely(audio: MediaPlayer): Boolean {
        return try {
            audio.isPlaying
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Ambient MediaPlayer entered an invalid state; discarding it", error)
            if (player === audio) {
                player = null
                playerTrackId = null
                currentMusicVolume = 0f
            }
            try { audio.release() } catch (_: Exception) { }
            false
        }
    }

    @Synchronized
    fun updateAmbientState(
        isForeground: Boolean,
        isMuted: Boolean,
        trackId: String = selectedTrackId,
        volume: Float = masterVolume
    ) {
        appIsForeground = isForeground
        muted = isMuted
        masterVolume = volume.coerceIn(0f, 1f)
        val requestedTrack = normalizeTrackId(trackId)
        if (requestedTrack != selectedTrackId) selectedTrackId = requestedTrack
        if (trackSwitchPending) return
        if (player != null && playerTrackId != selectedTrackId) {
            selectTrack(selectedTrackId)
            return
        }
        ensurePlayer()
        val audio = player ?: return
        if (!AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
            // Let the fade finish before pausing; pausing immediately would make the fade inaudible.
            fadeMusicTo(ambientTargetVolume(speaking), 220L)
            if (currentMusicVolume <= 0.001f && isPlayingSafely(audio)) {
                try {
                    audio.pause()
                } catch (error: IllegalStateException) {
                    Log.w(TAG, "Ambient pause failed", error)
                }
            }
        } else {
            try {
                if (!audio.isPlaying) audio.start()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Ambient audio could not start", e)
            }
            fadeMusicTo(ambientTargetVolume(speaking), if (speaking) 520L else 950L)
        }
    }

    @Synchronized
    fun playOrchestraIntro() {
        stopOrchestraIntro()
        speaking = true
        fadeMusicTo(ambientTargetVolume(true), 700L)
        val intro = try { MediaPlayer.create(appContext, R.raw.orchestra_intro) } catch (_: Exception) { null }
        if (intro == null) {
            speaking = false
            fadeMusicTo(ambientTargetVolume(false), 700L)
            return
        }
        orchestraIntroPlayer = intro
        intro.setOnCompletionListener {
            synchronized(this@AppAudioController) {
                if (orchestraIntroPlayer === it) {
                    try { it.release() } catch (_: Exception) { }
                    orchestraIntroPlayer = null
                    speaking = false
                    fadeMusicTo(ambientTargetVolume(false), 900L)
                }
            }
        }
        intro.setOnErrorListener { media, _, _ ->
            synchronized(this@AppAudioController) {
                if (orchestraIntroPlayer === media) {
                    try { media.release() } catch (_: Exception) { }
                    orchestraIntroPlayer = null
                    speaking = false
                    fadeMusicTo(ambientTargetVolume(false), 700L)
                }
            }
            true
        }
        try { intro.start() } catch (_: Exception) {
            try { intro.release() } catch (_: Exception) { }
            orchestraIntroPlayer = null
            speaking = false
            fadeMusicTo(ambientTargetVolume(false), 700L)
        }
    }

    @Synchronized
    fun stopOrchestraIntro() {
        val intro = orchestraIntroPlayer
        orchestraIntroPlayer = null
        try { intro?.stop() } catch (_: Exception) { }
        try { intro?.release() } catch (_: Exception) { }
        speaking = false
        fadeMusicTo(ambientTargetVolume(false), 500L)
    }

    @Synchronized
    fun speakSpirit(text: String, lowVoice: Boolean = false, onFinished: () -> Unit = {}) {
        speakWithProfile(text, lowVoice = lowVoice, onFinished = onFinished)
    }

    /** Lower and slower orchestra speech; timbre depends on the device TTS engine. */
    @Synchronized
    fun speakOrchestra(text: String, onFinished: () -> Unit = {}) {
        speakWithProfile(text, lowVoice = true, onFinished = onFinished)
    }

    @Synchronized
    private fun speakWithProfile(
        text: String,
        lowVoice: Boolean,
        onFinished: () -> Unit
    ) {
        if (text.isBlank() || released) {
            Log.w(TAG, "Spirit speech skipped: blank text or released controller")
            onFinished()
            return
        }

        val speechText = try {
            prepareArabicSpeech(text)
        } catch (error: Exception) {
            Log.e(TAG, "Arabic speech preparation failed", error)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onFinished()
            } else {
                mainHandler.post { onFinished() }
            }
            return
        }

        if (speechText.isBlank()) {
            Log.w(TAG, "Spirit speech skipped: Arabic preparation produced blank text")
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onFinished()
            } else {
                mainHandler.post { onFinished() }
            }
            return
        }

        Log.i(TAG, "Spirit speech requested: chars=${text.length}, foreground=$appIsForeground, muted=$muted, engineReady=${sherpaEngine.isReady}, preparing=$sherpaPreparing")

        speaking = true

        if (appIsForeground && !muted) {
            ensurePlayer()
            player?.let { audio ->
                try {
                    if (!audio.isPlaying) audio.start()
                } catch (_: IllegalStateException) {
                }
            }
            fadeMusicTo(ambientTargetVolume(true), 650L)
        }

        speechQueue.addLast(QueuedSpeech(speechText, onFinished))
        Log.i(
            TAG,
            "Speech added to queue: queued=${speechQueue.size}, active=$speechRequestActive"
        )
        startNextSpeechIfIdle()
    }

    @Synchronized
    private fun startNextSpeechIfIdle() {
        if (released || speechRequestActive || speechQueue.isEmpty()) return

        val request = speechQueue.removeFirst()
        speechRequestActive = true
        speechGeneration++
        val generation = speechGeneration
        finishedCallback = request.onFinished

        Log.i(
            TAG,
            "Speech queue starting: generation=$generation, remaining=${speechQueue.size}, chars=${request.text.length}, queueWaitMs=${SystemClock.elapsedRealtime() - request.queuedAtMs}"
        )
        requestSherpaSpeech(request.text)
    }

    /**
     * Prepares the shared Sherpa engine for the initial setup screen.
     */
    @Synchronized
    fun prepareEngine(
        onStatus: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (released) {
            onError(IllegalStateException("متحكم الصوت مغلق"))
            return
        }

        if (sherpaEngine.isReady) {
            onStatus("ready")
            onProgress(100)
            onReady()
            return
        }

        sherpaPreparing = true

        sherpaEngine.prepare(
            onProgress = onProgress,
            onStatus = onStatus,
            onReady = {
                synchronized(this@AppAudioController) {
                    sherpaPreparing = false

                    val pending = pendingSpeechText
                    val generation = pendingSpeechGeneration
                    pendingSpeechText = null

                    if (
                        !released &&
                        pending != null &&
                        generation == speechGeneration
                    ) {
                        synthesizeSherpa(pending, generation)
                    }

                    if (!released) onReady()
                }
            },
            onError = { error ->
                synchronized(this@AppAudioController) {
                sherpaPreparing = false
                val failedGeneration = pendingSpeechGeneration
                val hadPendingSpeech = pendingSpeechText != null
                pendingSpeechText = null

                if (
                    !released &&
                    hadPendingSpeech &&
                    speechRequestActive &&
                    failedGeneration == speechGeneration
                ) {
                    Log.e(TAG, "Queued speech failed while shared engine was preparing", error)
                    finishSpeakingIfCurrent(failedGeneration)
                }

                if (!released) onError(error)
                }
            }
        )
    }

    @Synchronized
    private fun requestSherpaSpeech(text: String) {
        if (released || text.isBlank()) return
        val generation = speechGeneration

        Log.i(TAG, "Sherpa speech queued: generation=$generation, chars=${text.length}, engineReady=${sherpaEngine.isReady}, preparing=$sherpaPreparing")

        releaseSpeechPlayer()
        pendingSpeechText = text
        pendingSpeechGeneration = generation

        if (sherpaEngine.isReady) {
            val pending = pendingSpeechText
            pendingSpeechText = null
            if (pending != null) synthesizeSherpa(pending, generation)
            return
        }

        if (sherpaPreparing) return

        sherpaPreparing = true
        sherpaEngine.prepare(
            onProgress = { progress ->
                Log.d(TAG, "Supertonic model download: $progress%")
            },
            onReady = {
                synchronized(this@AppAudioController) {
                    sherpaPreparing = false

                    val pending = pendingSpeechText
                    val pendingGeneration = pendingSpeechGeneration
                    pendingSpeechText = null

                    if (
                        !released &&
                        pending != null &&
                        pendingGeneration == speechGeneration
                    ) {
                        synthesizeSherpa(pending, pendingGeneration)
                    }
                }
            },
            onError = { error ->
                synchronized(this@AppAudioController) {
                    sherpaPreparing = false

                    val failedGeneration = pendingSpeechGeneration
                    pendingSpeechText = null

                    if (!released && failedGeneration == speechGeneration) {
                        Log.e(TAG, "Supertonic preparation failed", error)
                        finishSpeakingIfCurrent(failedGeneration)
                    }
                }
            }
        )
    }

    @Synchronized
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

    @Synchronized
    private fun playSpeechFile(file: File, generation: Long) {
        if (released || generation != speechGeneration) {
            file.delete()
            return
        }

        releaseSpeechPlayer()

        val audio = try {
            MediaPlayer()
        } catch (error: Exception) {
            Log.e(TAG, "Could not create speech MediaPlayer", error)
            file.delete()
            finishSpeakingIfCurrent(generation)
            return
        }

        speechPlayer = audio
        Log.i(TAG, "Speech MediaPlayer created: generation=$generation, wavBytes=${file.length()}")

        try {
            audio.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // Set the speech player's gain explicitly instead of relying on its default.
            audio.setVolume(1.0f, 1.0f)
            audio.setDataSource(file.absolutePath)
            val mediaPrepareStartedAt = SystemClock.elapsedRealtime()

            audio.setOnPreparedListener { prepared ->
                synchronized(this@AppAudioController) {
                    if (
                        released ||
                        generation != speechGeneration ||
                        speechPlayer !== prepared
                    ) {
                        try { prepared.release() } catch (_: Exception) {}
                        file.delete()
                        return@synchronized
                    }

                    try {
                        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val musicMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val availableOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                            .joinToString { "${it.productName}#${it.id}/type=${it.type}" }
                        prepared.setVolume(1.0f, 1.0f)
                        Log.i(
                            TAG,
                            "Speech output diagnosis: duration=${prepared.duration}ms, mediaPrepareMs=${SystemClock.elapsedRealtime() - mediaPrepareStartedAt}, audioSession=${prepared.getAudioSessionId()}, streamMusic=$musicVolume/$musicMaxVolume, playerGain=1.0/1.0, availableOutputs=[$availableOutputs], generation=$generation"
                        )
                        if (musicVolume == 0) {
                            Log.e(TAG, "Android STREAM_MUSIC volume is zero; media output is muted at system level")
                        }
                        prepared.start()
                        val actuallyPlaying = prepared.isPlaying
                        Log.i(TAG, "Speech playback started: generation=$generation, isPlaying=$actuallyPlaying")
                        if (!actuallyPlaying) {
                            Log.w(
                                TAG,
                                "MediaPlayer is not yet reporting active playback immediately after start; awaiting playback callbacks"
                            )
                        }
                    } catch (error: Exception) {
                        Log.e(TAG, "Speech playback could not start", error)
                        releaseSpeechPlayer()
                        file.delete()
                        finishSpeakingIfCurrent(generation)
                    }
                }
            }

            audio.setOnCompletionListener { completed ->
                synchronized(this@AppAudioController) {
                    if (
                        speechPlayer === completed &&
                        generation == speechGeneration
                    ) {
                        releaseSpeechPlayer()
                        file.delete()
                        finishSpeakingIfCurrent(generation)
                    }
                }
            }

            audio.setOnErrorListener { failed, what, extra ->
                synchronized(this@AppAudioController) {
                    if (
                        speechPlayer === failed &&
                        generation == speechGeneration
                    ) {
                        Log.e(TAG, "Speech playback error: $what/$extra")
                        releaseSpeechPlayer()
                        file.delete()
                        finishSpeakingIfCurrent(generation)
                    }
                }
                true
            }

            audio.prepareAsync()
        } catch (error: Exception) {
            Log.e(TAG, "Speech WAV setup failed", error)
            releaseSpeechPlayer()
            file.delete()
            finishSpeakingIfCurrent(generation)
        }
    }

    @Synchronized
    private fun releaseSpeechPlayer() {
        val stream = streamingSpeechPlayer
        streamingSpeechPlayer = null
        try { stream?.stop() } catch (_: Exception) { }

        val audio = speechPlayer
        speechPlayer = null

        try {
            audio?.setOnPreparedListener(null)
        } catch (_: Exception) {
        }

        try {
            audio?.setOnCompletionListener(null)
        } catch (_: Exception) {
        }

        try {
            audio?.setOnErrorListener(null)
        } catch (_: Exception) {
        }

        try {
            audio?.stop()
        } catch (_: Exception) {
        }

        try {
            audio?.release()
        } catch (_: Exception) {
        }
    }

    /** Switches the looping soundtrack with a fade and never overlaps two players. */
    @Synchronized
    fun selectTrack(trackId: String) {
        val requested = normalizeTrackId(trackId)
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { selectTrack(requested) }
            return
        }
        selectedTrackId = requested
        if (trackSwitchPending) return
        if (player != null && playerTrackId == requested) return
        if (player == null) {
            ensurePlayer()
            val next = player ?: return
            if (AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
                try { if (!next.isPlaying) next.start() } catch (_: IllegalStateException) { }
            }
            fadeMusicTo(ambientTargetVolume(speaking), 850L)
            return
        }
        trackSwitchPending = true
        fadeMusicTo(0f, 430L)
        val switchTask = Runnable {
            synchronized(this) {
                trackSwitchRunnable = null
                trackSwitchPending = false
                try { player?.release() } catch (_: Exception) { }
                player = null
                playerTrackId = null
                currentMusicVolume = 0f
                ensurePlayer()
                val next = player ?: return@synchronized
                if (AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
                    try { if (!next.isPlaying) next.start() } catch (_: IllegalStateException) { }
                    fadeMusicTo(ambientTargetVolume(speaking), if (speaking) 520L else 900L)
                } else {
                    setMusicVolume(0f)
                }
            }
        }
        trackSwitchRunnable = switchTask
        mainHandler.postDelayed(switchTask, 450L)
    }

    @Synchronized
    fun stopSpirit() {
        stopOrchestraIntro()
        speechGeneration++
    sherpaEngine.cancelSynthesis()
        pendingSpeechText = null
        speechQueue.clear()
        speechRequestActive = false
        releaseSpeechPlayer()

        speaking = false
        finishedCallback = null
        fadeMusicTo(ambientTargetVolume(false), 800L)
    }

    @Synchronized
    fun release() {
        if (released) return

        released = true
        speechGeneration++

        try {
            orchestraIntroPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            orchestraIntroPlayer?.release()
        } catch (_: Exception) {
        }

        orchestraIntroPlayer = null

        playerGeneration++
        playerLoading = false

        volumeAnimator?.cancel()
        volumeAnimator = null

        finishedCallback = null
        speaking = false

        pendingSpeechText = null
        speechQueue.clear()
        speechRequestActive = false
        releaseSpeechPlayer()
        sherpaEngine.close()


        trackSwitchRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        trackSwitchRunnable = null
        trackSwitchPending = false

        try {
            loadingPlayer?.setOnPreparedListener(null)
            loadingPlayer?.setOnErrorListener(null)
            loadingPlayer?.release()
        } catch (_: Exception) {
        }
        loadingPlayer = null

        try {
            player?.release()
        } catch (_: Exception) {
        }

        player = null
        playerTrackId = null
        currentMusicVolume = 0f
    }

    @Synchronized
    private fun finishSpeakingIfCurrent(generation: Long) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                finishSpeakingIfCurrent(generation)
            }
            return
        }

        if (released || generation != speechGeneration || !speechRequestActive) return

        val callback = finishedCallback
        finishedCallback = null
        speechRequestActive = false

        Log.i(
            TAG,
            "Speech queue item finished: generation=$generation, remaining=${speechQueue.size}"
        )

        try {
            callback?.invoke()
        } catch (error: Exception) {
            Log.e(TAG, "Speech completion callback failed", error)
        }

        // The callback may have stopped the controller, released it, or enqueued a new item.
        if (released || generation != speechGeneration) return

        if (speechQueue.isNotEmpty()) {
            startNextSpeechIfIdle()
        } else {
            speaking = false
            fadeMusicTo(ambientTargetVolume(false), 850L)
            Log.i(TAG, "Speech queue is empty")
        }
    }

    private fun ensurePlayer() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { ensurePlayer() }
            return
        }

        if (released || player != null || playerLoading) return

        val trackToLoad = selectedTrackId
        val resourceId = when (trackToLoad) {
            "sin" -> R.raw.track_sin
            "fate" -> R.raw.track_fate
            else -> R.raw.track_death
        }

        val generation = ++playerGeneration
        var next: MediaPlayer? = null
        playerLoading = true

        try {
            val created = MediaPlayer()
            next = created
            loadingPlayer = created

            created.isLooping = true
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            created.setOnPreparedListener { prepared ->
                synchronized(this) {
                    if (released || generation != playerGeneration) {
                        try { prepared.release() } catch (_: Exception) { }
                        if (loadingPlayer === prepared) {
                            loadingPlayer = null
                            playerLoading = false
                        }
                        return@synchronized
                    }

                    if (selectedTrackId != trackToLoad) {
                        try { prepared.release() } catch (_: Exception) { }
                        loadingPlayer = null
                        playerLoading = false
                        ensurePlayer()
                        return@synchronized
                    }

                    loadingPlayer = null
                    playerLoading = false
                    player = prepared
                    playerTrackId = trackToLoad
                    currentMusicVolume = 0f

                    if (AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
                        try {
                            if (!prepared.isPlaying) prepared.start()
                        } catch (e: IllegalStateException) {
                            Log.w(TAG, "Ambient audio could not start", e)
                        }
                        fadeMusicTo(
                            ambientTargetVolume(speaking),
                            if (speaking) 520L else 950L
                        )
                    } else {
                        setMusicVolume(0f)
                    }
                }
            }

            created.setOnErrorListener { failed, what, extra ->
                synchronized(this) {
                    if (generation == playerGeneration) {
                        if (loadingPlayer === failed) {
                            loadingPlayer = null
                        }
                        if (player === failed) {
                            player = null
                            playerTrackId = null
                            currentMusicVolume = 0f
                            volumeAnimator?.cancel()
                            volumeAnimator = null
                        }
                        playerLoading = false
                    }
                    try { failed.release() } catch (_: Exception) { }
                    Log.e(TAG, "Ambient audio failed: $what/$extra; current reference cleared when applicable")
                }
                true
            }

            appContext.resources.openRawResourceFd(resourceId).use { descriptor ->
                created.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }

            created.prepareAsync()
        } catch (e: Exception) {
            if (generation == playerGeneration) {
                playerLoading = false
                loadingPlayer = null
            }
            try { next?.release() } catch (_: Exception) { }
            Log.e(TAG, "Ambient track could not be loaded", e)
        }
    }

    private fun normalizeTrackId(trackId: String): String = when (trackId) {
        "death", "sin", "fate" -> trackId
        else -> "death"
    }

    private fun ambientTargetVolume(isSpeaking: Boolean): Float =
        (AmbientAudioPolicy.targetVolume(appIsForeground, muted, isSpeaking) * masterVolume).coerceIn(0f, 1f)

    @Synchronized
    private fun fadeMusicTo(target: Float, durationMs: Long) {
        val safeTarget = target.coerceIn(0f, 1f)
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { fadeMusicTo(safeTarget, durationMs) }
            return
        }
        volumeAnimator?.cancel()
        val start = currentMusicVolume
        if (player == null) {
            currentMusicVolume = safeTarget
            return
        }
        if (durationMs <= 0L || kotlin.math.abs(start - safeTarget) < 0.0005f) {
            setMusicVolume(safeTarget)
            if (safeTarget <= 0.001f && !AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
                try { player?.pause() } catch (_: IllegalStateException) { }
            }
            return
        }
        volumeAnimator = ValueAnimator.ofFloat(start, safeTarget).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                setMusicVolume(value)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    synchronized(this@AppAudioController) {
                        if (currentMusicVolume <= 0.001f && !AmbientAudioPolicy.shouldPlay(appIsForeground, muted)) {
                            try { player?.pause() } catch (_: IllegalStateException) { }
                        }
                    }
                }
            })
            start()
        }
    }

    @Synchronized
    private fun setMusicVolume(volume: Float) {
        currentMusicVolume = volume.coerceIn(0f, 1f)
        try { player?.setVolume(currentMusicVolume, currentMusicVolume) } catch (_: IllegalStateException) { }
    }

    /** Use reviewed vocalization for recurring spirit lines, then normalize pauses. */
    private fun prepareArabicSpeech(text: String): String {
        val vocalized = VOCALIZED_LINES[text] ?: text
        return vocalized
            .replace("…", "، ")
            .replace("—", "، ")
            .replace("–", "، ")
            .replace(Regex("[\\t\\n\\r ]+"), " ")
            .trim()
    }

    companion object {
        private const val TAG = "MahakamaAudio"

        /** Reviewed tashkeel for recurring lines; new lines still depend on device TTS. */
        private val VOCALIZED_LINES = mapOf(
            "ربما لا تبحث عن السيطرة لذاتها؛ ربما تحاول أن تجعل المجهول مكانًا يمكن احتماله. لكن متى يتحول الاستعداد إلى قفص؟" to
                "رُبَّما لا تَبحَثُ عَنِ السَّيطَرَةِ لِذَاتِها؛ رُبَّما تُحاوِلُ أَنْ تَجعَلَ المَجهولَ مَكانًا يُمكِنُ احْتِمالُهُ. لَكِنْ مَتى يَتَحَوَّلُ الاسْتِعْدادُ إِلى قَفَصٍ؟",
            "قد يكون صوت الآخرين حاضرًا في بعض اختياراتك حتى حين لا يكونون هناك. أيّ جزء من هذا الصوت يشبهك، وأيّ جزء ورثته من خوفك؟" to
                "قَدْ يَكونُ صَوتُ الآخَرينَ حاضِرًا في بَعضِ اخْتِياراتِكَ، حَتّى حينَ لا يَكونونَ هُناكَ. أَيُّ جُزءٍ مِنْ هذا الصَّوتِ يُشبِهُكَ، وَأَيُّ جُزءٍ وَرِثتَهُ مِنْ خَوفِكَ؟",
            "أحيانًا تكون المسافة حمايةً، وأحيانًا تؤجل لقاءً لا مفر منه. لا أعرف أيّهما يحدث لك الآن؛ لكن إجاباتك تستحق هذا السؤال." to
                "أَحيانًا تَكونُ المَسافَةُ حِمايَةً، وَأَحيانًا تُؤَجِّلُ لِقاءً لا مَفَرَّ مِنْهُ. لا أَعرِفُ أَيَّهُما يَحدُثُ لَكَ الآنَ؛ لَكِنَّ إِجاباتِكَ تَستَحِقُّ هذا السُّؤالَ.",
            "في بعض اختياراتك استعدادٌ للنظر إلى نفسك دون اختصارها في خطأ واحد. تذكّر أن الصراحة التي لا تترك مكانًا للرحمة قد تتحول إلى محاكمة أخرى." to
                "في بَعضِ اخْتِياراتِكَ اسْتِعدادٌ لِلنَّظَرِ إِلى نَفسِكَ دونَ اخْتِصارِها في خَطَأٍ واحِدٍ. تَذَكَّرْ أَنَّ الصَّراحَةَ الَّتي لا تَترُكُ مَكانًا لِلرَّحمَةِ قَدْ تَتَحَوَّلُ إِلى مُحاكَمَةٍ أُخرى.",
            "تبدو العلاقة بالآخرين حاضرة في بعض قراراتك. هل تحمي العلاقة لأنك تختارها، أم لأنك تخشى ما يبقى منك إذا انتهت؟" to
                "تَبدو العَلاقَةُ بِالآخَرينَ حاضِرَةً في بَعضِ قَراراتِكَ. هَلْ تَحمي العَلاقَةَ لِأَنَّكَ تَختارُها، أَمْ لِأَنَّكَ تَخشى ما يَبقى مِنكَ إِذا انْتَهَتْ؟",
            "تظهر في إجاباتك رغبة في أن يكون للتغيير معنى، لا أن يكون حركةً فارغة. ما الخطوة الصغيرة التي ستجعل هذا المعنى واقعًا؟" to
                "تَظهَرُ في إِجاباتِكَ رَغْبَةٌ في أَنْ يَكونَ لِلتَّغْييرِ مَعْنًى، لا أَنْ يَكونَ حَرَكَةً فارِغَةً. ما الخُطْوَةُ الصَّغيرَةُ الَّتي سَتَجعَلُ هذا المَعْنى واقِعًا؟",
            "لا أملك حكمًا نهائيًا عنك. أرى خيطًا صغيرًا في إجاباتك، وأدعوك إلى النظر إليه: هل يصفك فعلًا، أم يصف لحظةً مررت بها؟" to
                "لا أَملِكُ حُكمًا نِهائِيًّا عَنكَ. أَرى خَيطًا صَغيرًا في إِجاباتِكَ، وَأَدعوكَ إِلى النَّظَرِ إِلَيهِ: هَلْ يَصِفُكَ فِعلًا، أَمْ يَصِفُ لَحظَةً مَرَرتَ بِها؟",
            "قبل أن أتابع… هل تبدو هذه الملاحظة قريبة من تجربتك، أم أن هناك سياقًا لم أره؟" to
                "قَبلَ أَنْ أُتابِعَ، هَلْ تَبدو هذِهِ المُلاحَظَةُ قَريبَةً مِنْ تَجرِبَتِكَ، أَمْ أَنَّ هُناكَ سِياقًا لَمْ أَرَهُ؟",
            "لن أحوّل قبولك إلى حقيقةٍ نهائية؛ سأحتفظ بها كخيطٍ يستحق أن نعود إليه حين تتغير الأدلة." to
                "لَنْ أُحَوِّلَ قَبولَكَ إِلى حَقيقَةٍ نِهائِيَّةٍ؛ سَأَحتَفِظُ بِها كَخَيطٍ يَستَحِقُّ أَنْ نَعودَ إِلَيهِ حينَ تَتَغَيَّرُ الأَدِلَّةُ.",
            "إذن سأترك هذه الصورة مفتوحة للمراجعة. ما لا يشبهك مهم بقدر ما يشبهك؛ لا أريد أن أختصر روايتك." to
                "إِذَنْ سَأَترُكُ هذِهِ الصُّورَةَ مَفتوحَةً لِلمُراجَعَةِ. ما لا يُشبِهُكَ مُهِمٌّ بِقَدرِ ما يُشبِهُكَ؛ لا أُريدُ أَنْ أَختَصِرَ رِوايَتَكَ.",
            "لا بأس. بعض الأسئلة تحتاج إلى زمنٍ لا إلى إجابةٍ فورية. يمكنك أن تتركها معك حتى تتضح ملامحها." to
                "لا بَأْسَ. بَعضُ الأَسئِلَةِ تَحتاجُ إِلى زَمَنٍ لا إِلى إِجابَةٍ فَورِيَّةٍ. يُمكِنُكَ أَنْ تَترُكَها مَعَكَ حَتّى تَتَّضِحَ مَلامِحُها."
        )
    }
}
