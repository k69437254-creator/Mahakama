package com.aram.mahakama.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.aram.mahakama.R
import java.util.Locale
import java.util.UUID

/**
 * One continuous ambient soundtrack and the Arabic voice of "طيف الذات".
 * During speech the same soundtrack remains audible at a lower, gently faded level;
 * no extra section-specific track or sound effect is introduced.
 */
class AppAudioController(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var orchestraIntroPlayer: MediaPlayer? = null
    private var playerTrackId: String? = null
    private var loadingPlayer: MediaPlayer? = null
    private var playerLoading = false
    private var playerGeneration = 0L
    private var released = false
    private var selectedTrackId: String = "death"
    private var trackSwitchPending = false
    private var trackSwitchRunnable: Runnable? = null
    private var masterVolume = 0.82f
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ttsReady = false
    private var appIsForeground = false
    private var muted = false
    private var speaking = false
    private var queuedText: String? = null
    private var queuedLowVoice = false
    private var finishedCallback: (() -> Unit)? = null
    private var activeUtteranceId: String? = null
    private var volumeAnimator: ValueAnimator? = null
    private var currentMusicVolume = 0f

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
            if (currentMusicVolume <= 0.001f && audio.isPlaying) audio.pause()
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
    private fun speakWithProfile(text: String, lowVoice: Boolean, onFinished: () -> Unit) {
        if (text.isBlank()) return
        speaking = true
        finishedCallback = onFinished
        queuedLowVoice = lowVoice
        if (appIsForeground && !muted) {
            ensurePlayer()
            player?.let { audio ->
                try { if (!audio.isPlaying) audio.start() } catch (_: IllegalStateException) { }
            }
            // Keep the same soft atmospheric track audible under the voice, at a ducked level.
            fadeMusicTo(ambientTargetVolume(true), 650L)
        }
        if (!ttsReady) {
            queuedText = text
            return
        }
        speakNow(prepareArabicSpeech(text), lowVoice)
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
        queuedText = null
        queuedLowVoice = false
        finishedCallback = null
        activeUtteranceId = null
        try { tts?.stop() } catch (_: Exception) { }
        speaking = false
        fadeMusicTo(ambientTargetVolume(false), 800L)
    }

    @Synchronized
    fun release() {
        released = true
        try { orchestraIntroPlayer?.stop() } catch (_: Exception) { }
        try { orchestraIntroPlayer?.release() } catch (_: Exception) { }
        orchestraIntroPlayer = null
        playerGeneration++
        playerLoading = false

        volumeAnimator?.cancel()
        volumeAnimator = null
        queuedText = null
        finishedCallback = null
        activeUtteranceId = null
        speaking = false

        trackSwitchRunnable?.let { mainHandler.removeCallbacks(it) }
        trackSwitchRunnable = null
        trackSwitchPending = false

        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) { }
        tts = null

        try {
            loadingPlayer?.setOnPreparedListener(null)
            loadingPlayer?.setOnErrorListener(null)
            loadingPlayer?.release()
        } catch (_: Exception) { }
        loadingPlayer = null

        try { player?.release() } catch (_: Exception) { }
        player = null
        playerTrackId = null
        currentMusicVolume = 0f
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Text-to-speech engine initialization failed")
            finishWithoutVoice()
            return
        }
        val engine = tts ?: return
        try {
            val preferredLocales = listOf(
                Locale("ar", "SA"),
                Locale("ar", "AE"),
                Locale("ar", "EG"),
                Locale("ar")
            )
            val supportedLocale = preferredLocales.firstOrNull { locale ->
                engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
            }
            val arabicVoices = engine.voices
                ?.filter { it.locale.language.equals("ar", ignoreCase = true) }
                ?.sortedWith(
                    compareByDescending<android.speech.tts.Voice> { it.quality }
                        .thenBy { it.isNetworkConnectionRequired }
                        .thenBy { it.locale.country != "SA" }
                )
                .orEmpty()
            val chosenLocale = supportedLocale ?: arabicVoices.firstOrNull()?.locale
            if (chosenLocale == null) {
                ttsReady = false
                Log.w(TAG, "No Arabic speech voice is available on this device")
                finishWithoutVoice()
                return
            }

            val languageStatus = engine.setLanguage(chosenLocale)
            ttsReady = languageStatus >= TextToSpeech.LANG_AVAILABLE
            if (!ttsReady) {
                Log.w(TAG, "Arabic language data is unavailable on this device")
                finishWithoutVoice()
                return
            }

            arabicVoices.firstOrNull { voice ->
                voice.locale.language.equals("ar", ignoreCase = true) &&
                    (voice.locale.country == chosenLocale.country || chosenLocale.country.isBlank())
            }?.let { voice ->
                try {
                    engine.voice = voice
                } catch (e: Exception) {
                    Log.w(TAG, "Could not select the preferred Arabic voice", e)
                }
            }

            // Balanced pace and pitch. The final voice timbre still depends on the Arabic engine installed on the device.
            engine.setSpeechRate(SPEECH_RATE)
            engine.setPitch(SPEECH_PITCH)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finishSpeakingIfCurrent(utteranceId)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finishSpeakingIfCurrent(utteranceId)
                override fun onError(utteranceId: String?, errorCode: Int) = finishSpeakingIfCurrent(utteranceId)
            })
            val queued = synchronized(this) {
                val text = queuedText
                val lowVoice = queuedLowVoice
                queuedText = null
                queuedLowVoice = false
                if (text == null) null else text to lowVoice
            }
            if (queued != null && ttsReady) speakNow(prepareArabicSpeech(queued.first), queued.second)
        } catch (e: Exception) {
            Log.w(TAG, "Arabic TTS setup failed; the spirit message remains visible", e)
            ttsReady = false
            finishWithoutVoice()
        }
    }

    @Synchronized
    private fun speakNow(text: String, lowVoice: Boolean = false) {
        val engine = tts ?: return
        if (!ttsReady) return
        try {
            engine.setSpeechRate(if (lowVoice) ORCHESTRA_SPEECH_RATE else SPEECH_RATE)
            engine.setPitch(if (lowVoice) ORCHESTRA_SPEECH_PITCH else SPEECH_PITCH)
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply the selected voice profile", e)
        }
        val id = "spirit-${UUID.randomUUID()}"
        activeUtteranceId = id
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result == TextToSpeech.ERROR) finishSpeakingIfCurrent(id)
    }

    @Synchronized
    private fun finishWithoutVoice() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { finishWithoutVoice() }
            return
        }
        queuedText = null
        activeUtteranceId = null
        speaking = false
        fadeMusicTo(ambientTargetVolume(false), 700L)
        val callback = finishedCallback
        finishedCallback = null
        callback?.invoke()
    }

    @Synchronized
    private fun finishSpeakingIfCurrent(utteranceId: String?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { finishSpeakingIfCurrent(utteranceId) }
            return
        }
        if (utteranceId == null || utteranceId != activeUtteranceId) return
        activeUtteranceId = null
        speaking = false
        fadeMusicTo(ambientTargetVolume(false), 850L)
        val callback = finishedCallback
        finishedCallback = null
        callback?.invoke()
    }

    @Synchronized
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
                        loadingPlayer = null
                        playerLoading = false
                    }
                    try { failed.release() } catch (_: Exception) { }
                    Log.e(TAG, "Ambient audio preparation failed: $what/$extra")
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
        private const val SPEECH_RATE = 0.91f
        private const val SPEECH_PITCH = 1.02f
        private const val ORCHESTRA_SPEECH_RATE = 0.76f
        private const val ORCHESTRA_SPEECH_PITCH = 0.76f

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
