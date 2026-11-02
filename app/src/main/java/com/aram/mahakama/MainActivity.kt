package com.aram.mahakama

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.mutableStateListOf
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
import com.aram.mahakama.domain.SpiritMemoryEngine
import com.aram.mahakama.domain.TrialClock
import com.aram.mahakama.ui.CinematicOpeningScreen
import com.aram.mahakama.ui.CinematicAtmosphere
import com.aram.mahakama.ui.ClockScreen
import com.aram.mahakama.ui.EchoScreen
import com.aram.mahakama.ui.FacesScreen
import com.aram.mahakama.ui.HomeScreen
import com.aram.mahakama.ui.InnerCourtScreen
import com.aram.mahakama.ui.NameEntryScreen
import com.aram.mahakama.ui.AiSetupScreen
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
import com.aram.mahakama.nextupdate.data.NextUpdateEventCodec
import com.aram.mahakama.nextupdate.domain.SpiritReflectionEngine
import com.aram.mahakama.nextupdate.domain.TrialEvolutionEngine
import com.aram.mahakama.nextupdate.ui.EnhancedInnerCourtScreen
import com.aram.mahakama.nextupdate.ui.FaceTreeScreen
import com.aram.mahakama.nextupdate.ui.TrialEvolutionScreen

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
        Log.e("MAHAKAMA_LIFECYCLE", "MAIN ACTIVITY onDestroy CALLED")
        clock.checkpointAndClose()
        audioController.release()
        super.onDestroy()
    }
}

private data class SpiritDialogue(
    val firstLine: String,
    val followUp: String,
    val closeLine: String = "أحيانًا لا نحتاج إلى حكمٍ جديد، بل إلى شجاعةٍ أكبر في الإصغاء.",
    val memoryKey: String = "general"
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
    var aiSetupComplete by remember { mutableStateOf(false) }
    var isForeground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var route by remember { mutableStateOf("home") }
    val backStack = remember { mutableStateListOf<String>() }
    var navigationDirection by remember { mutableIntStateOf(1) }


    fun navigateTo(destination: String) {
        if (destination != route) {
            navigationDirection = 1
            backStack.add(route)
            route = destination
        }
    }

    fun navigateBack() {
        navigationDirection = -1
        route = if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            "home"
        }
    }
    var spiritLine by remember { mutableStateOf<String?>(null) }
    var spiritStage by remember { mutableIntStateOf(0) }
    var speechFinished by remember { mutableStateOf(false) }
    var dialogue by remember { mutableStateOf<SpiritDialogue?>(null) }
    var lastSpiritAtCount by remember { mutableIntStateOf(0) }
    var spiritLayer by remember { mutableStateOf("المراقب") }
    var spiritEvidence by remember { mutableStateOf("") }
    var spiritSymbolicLine by remember { mutableStateOf("") }
    var spiritSilent by remember { mutableStateOf(false) }

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
    LaunchedEffect(isForeground, userState.trialStarted, userState.spiritMemoryJson) {
        if (isForeground && userState.trialStarted && spiritLine == null) {
            val activeMemory = NextUpdateEventCodec.activeBranchMemory(
            raw = userState.spiritMemoryJson,
            answers = userState.answers
        )
        val returnInsight = SpiritMemoryEngine.returnAfterAbsence(activeMemory)
            if (returnInsight != null) {
                spiritLayer = returnInsight.layer
                spiritEvidence = "${returnInsight.evidence}\n${returnInsight.confidence}"
                spiritSymbolicLine = returnInsight.symbolicLine
                dialogue = SpiritDialogue(returnInsight.line, returnInsight.followUp, memoryKey = returnInsight.key)
                spiritSilent = false
                spiritStage = 0
                spiritLine = returnInsight.line
                scope.launch {
                    repository.appendSpiritMemoryEvent(
                        SpiritMemoryEngine.appearanceEvent(userState.spiritMemoryJson, returnInsight.key, returnInsight.followUp)
                    )
                }
            }
        }
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
    LaunchedEffect(spiritLine, spiritSilent) {
        val line = spiritLine
        android.util.Log.i(
            "MahakamaSpirit",
            "Speech effect triggered: hasLine=${line != null}, silent=$spiritSilent, stage=$spiritStage, chars=${line?.length ?: 0}"
        )
        if (line == null || spiritSilent) {
            audio.stopSpirit()
            if (spiritSilent) speechFinished = true
        } else {
            speechFinished = false
            android.util.Log.i("MahakamaSpirit", "Calling speakSpirit: chars=${line.length}")
            audio.speakSpirit(line) {
                android.util.Log.i("MahakamaSpirit", "Spirit speech completion callback")
                speechFinished = true
            }
            // Keep the spirit request alive until playback reports completion.
            while (!speechFinished && spiritLine == line) {
                delay(250L)
            }
            if (spiritLine == line && spiritStage != 1) {
                spiritLine = null
                spiritStage = 0
                dialogue = null
            }
        }
    }

    BackHandler(enabled = route != "home" && userState.trialStarted && spiritLine == null) {
        navigateBack()
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
                    !userState.orchestraIntroCompleted && !aiSetupComplete -> {
                        AiSetupScreen(
                            audio = audio,
                            onContinue = { aiSetupComplete = true }
                        )
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
                            onPlayIntro = { audio.playOrchestraIntro() },
                            onStopIntro = { audio.stopOrchestraIntro() },
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
                            backStack.clear()
                            navigationDirection = -1
                            route = "home"
                            clock.beginTrial()
                        }
                    }
                    else -> {
                        if (route != "home") {
                            SectionHeader(title = routeTitle(route), subtitle = routeSubtitle(route), onBack = { navigateBack() })
                        }
                        AnimatedContent(
                            targetState = route,
                            transitionSpec = {
                                val direction = navigationDirection
                                (
                                    fadeIn(tween(350, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(
                                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                                        initialOffsetX = { width ->
                                            if (direction > 0) width / 4 else -width / 4
                                        }
                                    )
                                ) togetherWith (
                                    fadeOut(tween(280)) +
                                    slideOutHorizontally(
                                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                                        targetOffsetX = { width ->
                                            if (direction > 0) -width / 4 else width / 4
                                        }
                                    )
                                )
                            },
                            label = "cinematic-route-transition"
                        ) { visibleRoute ->
                        when (visibleRoute) {
                            "home" -> HomeScreen(userState, elapsedMillis, onNavigate = { navigateTo(it) })
                            "questions" -> QuestionnaireScreen(
                                userName = userState.userName,
                                question = QuestionnaireEngine.nextQuestion(userState.answers),
                                answeredCount = QuestionnaireEngine.answeredOptions(userState.answers).size,
                                totalAnswers = userState.answers.size,
                                onAnswer = { question, optionId ->
                                    val activeBefore = QuestionnaireEngine.activeAnswers(userState.answers)
                                    val nextAnswers = activeBefore + (question.id to optionId)
                                    val next = QuestionnaireEngine.option(question.id, optionId)
                                    val answerEvent = next?.let { SpiritMemoryEngine.answerEvent(question, it) }
                                    val nextMemory = if (answerEvent != null) {
                                        SpiritMemoryEngine.appendEvent(userState.spiritMemoryJson, answerEvent)
                                    } else userState.spiritMemoryJson
                                    val count = QuestionnaireEngine.answeredOptions(nextAnswers).size
                                    val sinceLastSpirit = count - lastSpiritAtCount
                                    val activeMemory = NextUpdateEventCodec.activeBranchMemory(
        raw = nextMemory,
        answers = nextAnswers
    )
    val insight = SpiritMemoryEngine.createInsight(activeMemory, nextAnswers)
    val transparentReflection = SpiritReflectionEngine.create(nextAnswers)
                                    val previousSnapshot = NextUpdateEventCodec.latestProfileSnapshot(activeMemory)
    val currentSnapshot = TrialEvolutionEngine.snapshot(nextAnswers)
    val snapshotEvent = if (
        TrialEvolutionEngine.shouldRecordSnapshot(previousSnapshot, currentSnapshot)
    ) {
        val change = TrialEvolutionEngine.compare(previousSnapshot, currentSnapshot)
        NextUpdateEventCodec.encodeProfileSnapshot(
            currentSnapshot,
            TrialEvolutionEngine.evolutionSummary(change)
        )
    } else null
    val shouldAppear = next != null && QuestionnaireEngine.nextQuestion(nextAnswers) != null && (
                                        QuestionnaireEngine.shouldRevealSpirit(nextAnswers, sinceLastSpirit, count) ||
                                            SpiritMemoryEngine.shouldReveal(activeMemory)
                                        )
                                    scope.launch {
                                        repository.replaceAnswers(nextAnswers, question.id, optionId)
                                        if (answerEvent != null) repository.appendSpiritMemoryEvent(answerEvent)
        snapshotEvent?.let { repository.appendSpiritMemoryEvent(it) }
                                        if (shouldAppear) {
                                            repository.appendSpiritMemoryEvent(
                                                SpiritMemoryEngine.appearanceEvent(nextMemory, insight.key, insight.followUp)
                                            )
                                        }
                                    }
                                    if (shouldAppear) {
                                        lastSpiritAtCount = count
                                        spiritLayer = insight.layer
                                        spiritEvidence = buildString {
            append(insight.evidence)
            append("\n")
            append(transparentReflection.evidenceSummary)
            append("\n")
            append(transparentReflection.limitation)
        }
                                        spiritSymbolicLine = insight.symbolicLine
                                        dialogue = SpiritDialogue(
                                            firstLine = insight.line,
                                            followUp = insight.followUp,
                                            memoryKey = insight.key
                                        )
                                        spiritSilent = false
                                        spiritStage = 0
                                        spiritLine = insight.line
                                    }
                                },
                                onGoFaces = { navigateTo("faces") },
                                onGoUnmasking = { navigateTo("unmasking") }
                            )
                            "faces" -> FaceTreeScreen(
        userState = userState,
        onOpenEvolution = { navigateTo("evolution") }
    )
    "evolution" -> TrialEvolutionScreen(userState)
                            "unmasking" -> UnmaskingScreen(userState) { feedback ->
                                scope.launch { repository.saveUnmaskingFeedback(feedback) }
                            }
                            "other_you" -> OtherYouScreen(userState)
                            "inner_court" -> EnhancedInnerCourtScreen(userState) { eventJson ->
        scope.launch { repository.appendSpiritMemoryEvent(eventJson) }
    }
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
                            else -> HomeScreen(userState, elapsedMillis, onNavigate = { navigateTo(it) })
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
                    layerLabel = spiritLayer,
                    evidenceLine = spiritEvidence,
                    symbolicLine = spiritSymbolicLine,
                    onAsk = {
                        val followUp = dialogue?.followUp ?: "ما الذي أثاره هذا السؤال فيك؟"
                        spiritSilent = false
                        spiritStage = 1
                        spiritLine = followUp
                    },
                    onReply = { reply ->
                        val memoryKey = dialogue?.memoryKey ?: "general"

                        if (reply.startsWith("حوار:")) {
                            val userText = reply.removePrefix("حوار:").trim()
                            if (userText.isNotEmpty()) {
                                val userEvent = SpiritMemoryEngine.dialogueEvent(
                                    userText,
                                    memoryKey,
                                    "user"
                                )

                                scope.launch {
                                    val memoryWithUser = SpiritMemoryEngine.appendEvent(
                                        userState.spiritMemoryJson,
                                        userEvent
                                    )
                                    repository.appendSpiritMemoryEvent(userEvent)

                                    val answer = SpiritMemoryEngine.dialogueResponse(
                                        memoryWithUser,
                                        userText
                                    )

                                    val spiritEvent = SpiritMemoryEngine.dialogueEvent(
                                        answer,
                                        memoryKey,
                                        "spirit"
                                    )
                                    repository.appendSpiritMemoryEvent(spiritEvent)

                                    spiritSilent = false
                                    speechFinished = false
                                    spiritStage = 1
                                    spiritLine = answer
                                }
                            }
                        } else {
                            val event = if (reply.startsWith("رمز:")) {
                                SpiritMemoryEngine.symbolChoiceEvent(
                                    reply.removePrefix("رمز:").trim(),
                                    memoryKey
                                )
                            } else {
                                SpiritMemoryEngine.feedbackEvent(reply, memoryKey)
                            }

                            scope.launch {
                                repository.appendSpiritMemoryEvent(event)
                            }

                            when {
                                reply == "أفضل الصمت الآن" -> {
                                    spiritSilent = true
                                    speechFinished = true
                                    spiritStage = 3
                                    spiritLine = "لن أضيف سؤالًا آخر. لك أن تبقى هنا، أو تنهي لحظة الصمت متى شئت."
                                }

                                reply.startsWith("رمز:") -> {
                                    val choice = reply.removePrefix("رمز:").trim()
                                    spiritSilent = false
                                    spiritStage = 3
                                    spiritLine = SpiritMemoryEngine.symbolChoiceLine(choice)
                                }

                                else -> {
                                    val answerLine = when (reply) {
                                        "قريبة مما أشعر به" ->
                                            "لن أحوّل قبولك إلى حقيقةٍ نهائية؛ سأحتفظ بها كخيطٍ يستحق أن نعود إليه حين تتغير الأدلة."

                                        "الصورة ليست دقيقة" ->
                                            "إذن سأترك هذه الصورة مفتوحة للمراجعة. سأحفظ اعتراضك حتى لا أكرر القراءة نفسها دون أدلةٍ جديدة."

                                        else ->
                                            "لا بأس. بعض الأسئلة تحتاج إلى زمنٍ لا إلى إجابةٍ فورية. يمكنك أن تتركها معك حتى تتضح ملامحها."
                                    }

                                    spiritSilent = false
                                    spiritStage = 2
                                    spiritLine = answerLine
                                }
                            }
                        }
                    },
                    onClose = {
                        spiritLine = null
                        spiritStage = 0
                        spiritSilent = false
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
    "evolution" -> "سجل تطور المحاكمة"
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
    "evolution" -> "محطات الرحلة وما تغيّر في صورتها"
    "clock" -> "الزمن الذي جمعته جلساتك"
    "silent_room" -> "مساحةٌ لك، لا لحكمٍ عليك"
    else -> null
}
