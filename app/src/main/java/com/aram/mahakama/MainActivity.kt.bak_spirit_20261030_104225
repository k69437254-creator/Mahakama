package com.aram.mahakama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aram.mahakama.audio.AppAudioController
import com.aram.mahakama.data.PreferencesRepository
import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.domain.TrialClock
import com.aram.mahakama.ui.CinematicOpeningScreen
import com.aram.mahakama.ui.CinematicAtmosphere
import com.aram.mahakama.ui.ClockScreen
import com.aram.mahakama.ui.EchoScreen
import com.aram.mahakama.ui.FacesScreen
import com.aram.mahakama.ui.HomeScreen
import com.aram.mahakama.ui.InnerCourtScreen
import com.aram.mahakama.ui.NameEntryScreen
import com.aram.mahakama.ui.OtherYouScreen
import com.aram.mahakama.ui.ProfileScreen
import com.aram.mahakama.ui.QuestionnaireScreen
import com.aram.mahakama.ui.SectionHeader
import com.aram.mahakama.ui.SettingsScreen
import com.aram.mahakama.ui.SilentRoomScreen
import com.aram.mahakama.ui.SpiritOverlay
import com.aram.mahakama.ui.SpiritOrchestraScreen
import com.aram.mahakama.ui.TimerCard
import com.aram.mahakama.ui.UnmaskingScreen
import com.aram.mahakama.ui.WelcomeScreen
import com.aram.mahakama.ui.MahakamaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: PreferencesRepository
    private lateinit var clock: TrialClock
    private lateinit var audioController: AppAudioController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(12, 10, 18)
        window.navigationBarColor = android.graphics.Color.rgb(12, 10, 18)
        repository = PreferencesRepository(applicationContext)
        clock = TrialClock(repository)
        audioController = AppAudioController(applicationContext)

        setContent {
            MahakamaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0C0A12),
                    contentColor = Color(0xFFF0EAF8)
                ) {
                    MahakamaApp(repository, clock, audioController)
                }
            }
        }
    }

    override fun onDestroy() {
        clock.checkpointAndClose()
        audioController.release()
        super.onDestroy()
    }
}

private data class SpiritDialogue(
    val firstLine: String,
    val followUp: String,
    val closeLine: String = "أحيانًا لا نحتاج إلى حكمٍ جديد، بل إلى شجاعةٍ أكبر في الإصغاء."
)

@Composable
private fun MahakamaApp(
    repository: PreferencesRepository,
    clock: TrialClock,
    audio: AppAudioController
) {
    val userState by repository.state.collectAsState()
    val elapsedMillis by clock.elapsedMillis.collectAsState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCinematicIntro by remember { mutableStateOf(true) }
    var isForeground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var route by remember { mutableStateOf("home") }
    var spiritLine by remember { mutableStateOf<String?>(null) }
    var spiritStage by remember { mutableIntStateOf(0) }
    var speechFinished by remember { mutableStateOf(false) }
    var dialogue by remember { mutableStateOf<SpiritDialogue?>(null) }
    var lastSpiritAtCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        clock.onForeground()
    }
    LaunchedEffect(showCinematicIntro) {
        if (showCinematicIntro) {
            delay(3300L)
            showCinematicIntro = false
        }
    }
    LaunchedEffect(userState.totalActiveMillis, userState.trialStarted) {
        clock.restore(userState.totalActiveMillis, userState.trialStarted)
    }
    DisposableEffect(lifecycleOwner, clock) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isForeground = true
                    clock.onForeground()
                }
                Lifecycle.Event.ON_STOP -> {
                    isForeground = false
                    clock.onBackground()
                    spiritLine = null
                    spiritStage = 0
                    dialogue = null
                    audio.stopSpirit()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(isForeground, userState.ambientMuted, userState.selectedMusicTrack, userState.hasChosenMusicTrack, userState.trialStarted, userState.musicVolume) {
        val silenceBeforeTrackChoice = !userState.hasChosenMusicTrack
        audio.updateAmbientState(isForeground, userState.ambientMuted || silenceBeforeTrackChoice, userState.selectedMusicTrack, userState.musicVolume)
    }
    LaunchedEffect(spiritLine) {
        val line = spiritLine
        if (line == null) {
            audio.stopSpirit()
        } else {
            speechFinished = false
            audio.speakSpirit(line) { speechFinished = true }
            // Longer evidence-based reflections need more speaking time than short dialogue lines.
            val safetyTimeout = (line.length * 220L + 14_000L).coerceIn(28_000L, 75_000L)
            delay(safetyTimeout)
            if (spiritLine == line) {
                spiritLine = null
                spiritStage = 0
                dialogue = null
            }
        }
    }

    BackHandler(enabled = route != "home" && userState.trialStarted && spiritLine == null) {
        route = "home"
    }

    @Suppress("DEPRECATION")
    val platformTextToolbar = LocalTextToolbar.current
    @Suppress("DEPRECATION")
    val protectedTextToolbar = remember(platformTextToolbar) {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = platformTextToolbar.status

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                // Copy, cut and select-all are intentionally omitted. Paste remains available for text inputs.
                platformTextToolbar.showMenu(
                    rect = rect,
                    onCopyRequested = null,
                    onPasteRequested = onPasteRequested,
                    onCutRequested = null,
                    onSelectAllRequested = null
                )
            }

            override fun hide() = platformTextToolbar.hide()
        }
    }

    @Suppress("DEPRECATION")
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTextToolbar provides protectedTextToolbar
    ) {
        if (showCinematicIntro) {
            CinematicOpeningScreen(onSkip = { showCinematicIntro = false })
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                CinematicAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.9f)
                Column(Modifier.fillMaxSize()) {
                when {
                    userState.userName.isBlank() -> {
                        NameEntryScreen { name -> scope.launch { repository.saveUserName(name) } }
                    }
                    !userState.orchestraIntroCompleted -> {
                        SpiritOrchestraScreen(
                            userName = userState.userName,
                            selectedTrack = userState.selectedMusicTrack,
                            hasChosenTrack = userState.hasChosenMusicTrack,
                            onPreviewTrack = { trackId ->
                                audio.selectTrack(trackId)
                                scope.launch { repository.setSelectedMusicTrack(trackId) }
                            },
                            onSpeakName = { line, onFinished -> audio.speakOrchestra(line, onFinished) },
                            onSpeakQuestion = { line -> audio.speakOrchestra(line) },
                            onContinue = { trackId ->
                                audio.stopSpirit()
                                scope.launch {
                                    repository.setSelectedMusicTrack(trackId)
                                    repository.setOrchestraIntroCompleted(true)
                                }
                            }
                        )
                    }
                    !userState.trialStarted -> {
                        WelcomeScreen(userState.userName) {
                            route = "home"
                            clock.beginTrial()
                        }
                    }
                    else -> {
                        if (route != "home") {
                            SectionHeader(title = routeTitle(route), subtitle = routeSubtitle(route), onBack = { route = "home" })
                        }
                        AnimatedContent(
                            targetState = route,
                            transitionSpec = {
                                (fadeIn(tween(420)) + slideInVertically(
                                    animationSpec = tween(470, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 22 }
                                )) togetherWith fadeOut(tween(230))
                            },
                            label = "cinematic-route-transition"
                        ) { visibleRoute ->
                        when (visibleRoute) {
                            "home" -> HomeScreen(userState, elapsedMillis, onNavigate = { route = it })
                            "questions" -> QuestionnaireScreen(
                                userName = userState.userName,
                                question = QuestionnaireEngine.nextQuestion(userState.answers),
                                answeredCount = QuestionnaireEngine.answeredOptions(userState.answers).size,
                                totalAnswers = userState.answers.size,
                                onAnswer = { question, optionId ->
                                    val activeBefore = QuestionnaireEngine.activeAnswers(userState.answers)
                                    val nextAnswers = activeBefore + (question.id to optionId)
                                    scope.launch { repository.replaceAnswers(nextAnswers, question.id, optionId) }
                                    val next = QuestionnaireEngine.option(question.id, optionId)
                                    val count = QuestionnaireEngine.answeredOptions(nextAnswers).size
                                    val sinceLastSpirit = count - lastSpiritAtCount
                                    if (next != null && QuestionnaireEngine.nextQuestion(nextAnswers) != null &&
                                        QuestionnaireEngine.shouldRevealSpirit(nextAnswers, sinceLastSpirit, count)) {
                                        lastSpiritAtCount = count
                                        val reflection = QuestionnaireEngine.reflectionFor(nextAnswers)
                                        dialogue = SpiritDialogue(
                                            firstLine = reflection,
                                            followUp = "قبل أن أتابع… هل تبدو هذه الملاحظة قريبة من تجربتك، أم أن هناك سياقًا لم أره؟"
                                        )
                                        spiritStage = 0
                                        spiritLine = reflection
                                    }
                                },
                                onGoFaces = { route = "faces" },
                                onGoUnmasking = { route = "unmasking" }
                            )
                            "faces" -> FacesScreen(userState)
                            "unmasking" -> UnmaskingScreen(userState) { feedback ->
                                scope.launch { repository.saveUnmaskingFeedback(feedback) }
                            }
                            "other_you" -> OtherYouScreen(userState)
                            "inner_court" -> InnerCourtScreen(userState)
                            "echo" -> EchoScreen(userState)
                            "silent_room" -> SilentRoomScreen(userState.freeWriting, userState.freeWritingFaceId) { text, faceId ->
                                scope.launch { repository.saveFreeWriting(text, faceId) }
                            }
                            "clock" -> ClockScreen(elapsedMillis)
                            "settings" -> SettingsScreen(
                                userState = userState,
                                onSaveName = { name -> scope.launch { repository.saveUserName(name) } },
                                onToggleMuted = { muted -> scope.launch { repository.setAmbientMuted(muted) } },
                                onSelectMusicTrack = { trackId ->
                                    audio.selectTrack(trackId)
                                    scope.launch { repository.setSelectedMusicTrack(trackId) }
                                },
                                onVolumeChange = { volume -> scope.launch { repository.setMusicVolume(volume) } }
                            )
                            "profile" -> ProfileScreen(userState)
                            else -> HomeScreen(userState, elapsedMillis, onNavigate = { route = it })
                        }
                        }
                    }
                }
            }
            if (spiritLine != null && userState.trialStarted) {
                SpiritOverlay(
                    message = spiritLine.orEmpty(),
                    stage = spiritStage,
                    speechFinished = speechFinished,
                    onAsk = {
                        val followUp = dialogue?.followUp ?: "ما الذي أثاره هذا السؤال فيك؟"
                        spiritStage = 1
                        spiritLine = followUp
                    },
                    onReply = { reply ->
                        val answerLine = when (reply) {
                            "قريبة مما أشعر به" -> "لن أحوّل قبولك إلى حقيقةٍ نهائية؛ سأحتفظ بها كخيطٍ يستحق أن نعود إليه حين تتغير الأدلة."
                            "الصورة ليست دقيقة" -> "إذن سأترك هذه الصورة مفتوحة للمراجعة. ما لا يشبهك مهم بقدر ما يشبهك؛ لا أريد أن أختصر روايتك."
                            else -> "لا بأس. بعض الأسئلة تحتاج إلى زمنٍ لا إلى إجابةٍ فورية. يمكنك أن تتركها معك حتى تتضح ملامحها."
                        }
                        spiritStage = 2
                        spiritLine = answerLine
                    },
                    onClose = {
                        spiritLine = null
                        spiritStage = 0
                        dialogue = null
                        audio.stopSpirit()
                    }
                )
                }
            }
        }
    }
}

private fun routeTitle(route: String): String = when (route) {
    "questions" -> "جلسة الأسئلة"
    "faces" -> "شجرة الوجوه"
    "unmasking" -> "كشف القناع"
    "other_you" -> "ذاتك الأخرى"
    "inner_court" -> "المحكمة الداخلية"
    "echo" -> "صدى الماضي"
    "silent_room" -> "الغرفة الصامتة"
    "clock" -> "زمن المحاكمة"
    "settings" -> "إعدادات المحاكمة"
    "profile" -> "ملف الانعكاس"
    else -> "المحاكمة"
}

private fun routeSubtitle(route: String): String? = when (route) {
    "questions" -> "كل إجابة تفتح احتمالًا جديدًا"
    "faces" -> "احتمالات مدعومة بالأدلة ومفتوحة للمراجعة"
    "clock" -> "الزمن الذي جمعته جلساتك"
    "silent_room" -> "مساحةٌ لك، لا لحكمٍ عليك"
    else -> null
}
