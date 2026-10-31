package com.aram.mahakama.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import com.aram.mahakama.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpiritOrchestraScreen(
    userName: String,
    selectedTrack: String,
    hasChosenTrack: Boolean,
    onPreviewTrack: (String) -> Unit,
    onPlayIntro: () -> Unit,
    onStopIntro: () -> Unit,
    onContinue: (String) -> Unit
) {
    var nameVisible by remember { mutableStateOf(false) }
    var musicSelectionVisible by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var selectedForEntry by remember { mutableStateOf<String?>(null) }
    var horrorPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var cuePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var sceneVideoView by remember { mutableStateOf<VideoView?>(null) }
    val context = LocalContext.current

    val spectralTransition = rememberInfiniteTransition(label = "invisible-voice")
    val spectralPhase by spectralTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spectral-phase"
    )

    val sceneAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (leaving) 0f else 1f,
        androidx.compose.animation.core.tween(520),
        label = "orchestra-exit-fade"
    )

    DisposableEffect(Unit) {
        onDispose {
            sceneVideoView?.stopPlayback()
            sceneVideoView = null
            horrorPlayer?.let { player ->
                runCatching { player.setOnCompletionListener(null) }
                runCatching { player.stop() }
                runCatching { player.release() }
            }
            horrorPlayer = null
            cuePlayer?.let { player ->
                runCatching { player.setOnCompletionListener(null) }
                runCatching { player.stop() }
                runCatching { player.release() }
            }
            cuePlayer = null
            onStopIntro()
        }
    }

    LaunchedEffect(Unit) {
        nameVisible = true
        onPlayIntro()
        delay(12_000L)
        nameVisible = false
        delay(2_000L)
        musicSelectionVisible = true

        // يبدأ الصوت المرعب بعد 25 ثانية من بداية المشهد، ويستمر حتى الاختيار.
        delay(11_000L)
        if (selectedForEntry == null && !leaving) {
            val ambience = runCatching {
                MediaPlayer.create(context.applicationContext, R.raw.horror_ambience)
            }.getOrNull()

            if (ambience != null && selectedForEntry == null && !leaving) {
                ambience.isLooping = true
                ambience.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                ambience.setVolume(0f, 0f)
                ambience.setOnErrorListener { failed, _, _ ->
                    if (horrorPlayer === failed) horrorPlayer = null
                    runCatching { failed.release() }
                    true
                }
                horrorPlayer = ambience

                try {
                    ambience.start()
                    for (step in 1..18) {
                        if (selectedForEntry != null || leaving) break
                        val volume = 0.26f * step / 18f
                        ambience.setVolume(volume, volume)
                        delay(100L)
                    }
                } catch (_: Exception) {
                    if (horrorPlayer === ambience) horrorPlayer = null
                    runCatching { ambience.release() }
                }
            }
        }
    }

    // التسجيل المختار يحدد مدة الشاشة السوداء، ثم يبدأ اللحن الأصلي المحفوظ.
    LaunchedEffect(selectedForEntry) {
        val chosenTrack = selectedForEntry ?: return@LaunchedEffect
        musicSelectionVisible = false
        nameVisible = false

        val ambience = horrorPlayer
        if (ambience != null) {
            for (step in 10 downTo 0) {
                val volume = 0.26f * step / 10f
                runCatching { ambience.setVolume(volume, volume) }
                delay(55L)
            }
            runCatching { ambience.stop() }
            runCatching { ambience.release() }
            if (horrorPlayer === ambience) horrorPlayer = null
        }

        onStopIntro()
        val cueResource = when (chosenTrack) {
            "death" -> R.raw.cue_death
            "sin" -> R.raw.cue_sin
            else -> R.raw.cue_fate
        }
        val cue = runCatching {
            MediaPlayer.create(context.applicationContext, cueResource)
        }.getOrNull()

        if (cue == null) {
            // لا نبدأ اللحن الأصلي إذا تعذّر إنشاء المقطع التمهيدي.
            selectedForEntry = null
            musicSelectionVisible = true
        } else {
            try {
                cue.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                cue.setVolume(0f, 0f)
                cuePlayer = cue

                cue.setOnCompletionListener { completed ->
                    if (cuePlayer === completed) cuePlayer = null
                    runCatching { completed.release() }
                    // الانتقال إلى اللحن الأصلي مسموح بعد اكتمال المقطع فقط.
                    leaving = true
                }

                cue.setOnErrorListener { failed, _, _ ->
                    if (cuePlayer === failed) cuePlayer = null
                    runCatching { failed.release() }
                    // الفشل يعيد المستخدم إلى الاختيارات ولا يبدأ اللحن الأصلي.
                    selectedForEntry = null
                    musicSelectionVisible = true
                    true
                }

                cue.start()

                for (step in 1..10) {
                    if (cuePlayer !== cue) break
                    val volume = 0.96f * step / 10f
                    cue.setVolume(volume, volume)
                    delay(45L)
                }
            } catch (_: Exception) {
                if (cuePlayer === cue) cuePlayer = null
                runCatching { cue.stop() }
                runCatching { cue.release() }
                selectedForEntry = null
                musicSelectionVisible = true
            }
        }
    }

    LaunchedEffect(leaving) {
        val chosenTrack = selectedForEntry
        if (leaving && chosenTrack != null) {
            delay(560L)
            onPreviewTrack(chosenTrack)
            onContinue(chosenTrack)
        }
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = sceneAlpha }
            .background(Color(0xFF050409))
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    sceneVideoView = this
                    val scenes = intArrayOf(
                        R.raw.court_wake,
                        R.raw.ghost_appearance,
                        R.raw.violinist,
                        R.raw.pianist,
                        R.raw.cellist,
                        R.raw.conductor,
                        R.raw.full_orchestra,
                        R.raw.return_silence
                    )
                    var index = 0
                    setOnPreparedListener { player ->
                        player.isLooping = false
                        player.setVolume(0f, 0f)
                        start()
                    }
                    setOnCompletionListener {
                        index++
                        if (index < scenes.size) {
                            setVideoURI(Uri.parse(
                                "android.resource://${context.packageName}/${scenes[index]}"
                            ))
                        } else {
                            stopPlayback()
                        }
                    }
                    setVideoURI(Uri.parse(
                        "android.resource://${context.packageName}/${scenes[0]}"
                    ))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // تدرج سينمائي خفيف لتحسين وضوح النصوص دون حجب الفيديو.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x66050009),
                    0.18f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color(0xCC050409)
                )
            )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // عنوان المشهد والنص التوجيهي في أعلى الفيديو.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "الأوركسترا الأخيرة",
                    color = Color(0xFFE0D3F7),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.2.sp
                )
                Text(
                    "قبل أن تبدأ المحاكمة",
                    color = Color(0xFFB5A6C9),
                    style = MaterialTheme.typography.bodySmall
                )

                AnimatedVisibility(
                    visible = musicSelectionVisible,
                    enter = fadeIn(androidx.compose.animation.core.tween(850)) +
                        slideInVertically(androidx.compose.animation.core.tween(850)) { -it / 3 },
                    exit = fadeOut(androidx.compose.animation.core.tween(300))
                ) {
                    Text(
                        "اختر اللحن الذي سيصاحب رحلتك؛ فلكل نغمة ظلٌّ مختلف.",
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 18.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x5520182B))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        color = Color(0xFFF0E9FA),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        lineHeight = 23.sp
                    )
                }
            }

            // الترحيب يظهر مؤقتًا ثم يختفي كترجمة سينمائية.
            AnimatedVisibility(
                visible = nameVisible,
                enter = fadeIn(androidx.compose.animation.core.tween(650)),
                exit = fadeOut(androidx.compose.animation.core.tween(900))
            ) {
                Text(
                    "مرحبًا أيُّها العابر الضائع، $userName…",
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0x99050009), RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFFF3EEF9),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp
                )
            }

            // اقتراحات الموسيقى في أسفل الفيديو.
            AnimatedVisibility(
                visible = musicSelectionVisible,
                enter = fadeIn(androidx.compose.animation.core.tween(900)) +
                    slideInVertically(androidx.compose.animation.core.tween(900)) { it / 4 },
                exit = fadeOut(androidx.compose.animation.core.tween(250))
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xE8171220), Color(0xD90D0A13))
                            )
                        )
                        .border(1.dp, Color(0x553E3152), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "الألحان الثلاثة",
                        color = Color(0xFFE0D2FA),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(7.dp))

                    val soundtracks = listOf(
                        Triple("death", "لحن الموت", "حين يقترب الصمت"),
                        Triple("sin", "لحن الخطيئة", "اعترافٌ يتأخر"),
                        Triple("fate", "لحن القدر", "ما كُتب وما تختاره")
                    )

                    soundtracks.forEach { (id, title, subtitle) ->
                        val selected = selectedTrack == id
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable(enabled = selectedForEntry == null && !leaving) {
                                    selectedForEntry = id
                                },
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected)
                                    Color(0xFF302442) else Color(0xCC191420)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (selected) Color(0xFFB9A1FF)
                                        else Color(0x443B3048),
                                        RoundedCornerShape(13.dp)
                                    )
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(34.dp).background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF8064B4), Color(0xFF21182D))
                                        ),
                                        CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color(0xFFE1D5FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        color = Color(0xFFF2EBFF),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        subtitle,
                                        color = Color(0xFFB7A9C9),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (selected && hasChosenTrack) {
                                    Text(
                                        "يُعزف الآن",
                                        color = Color(0xFFD0BCFF),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else if (selected) {
                                    Text(
                                        "اختر هذا اللحن",
                                        color = Color(0xFFB7A9C9),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "تجربة $title",
                                        tint = Color(0xFF8F7CA9)
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }

        AnimatedVisibility(
            visible = selectedForEntry != null,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(850)),
            exit = fadeOut(tween(650))
        ) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF010103)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width * 0.5f, size.height * 0.46f)
                    val extent = size.minDimension
                    val cycle = 2f * PI.toFloat()

                    for (ring in 0 until 6) {
                        val progress = ((spectralPhase / cycle + ring * 0.17f) % 1f)
                        val radius = extent * (0.07f + progress * 0.39f)
                        val alpha = (0.15f * (1f - progress)).coerceIn(0f, 0.15f)
                        drawCircle(
                            color = Color(0xFF9E83C9).copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style = Stroke(width = (0.7f + (1f - progress) * 1.1f).dp.toPx())
                        )
                    }

                    for (bar in 0 until 84) {
                        val angle = bar * cycle / 84f
                        val wave = abs(sin(angle * 3.2f + spectralPhase * 1.35f))
                        val innerRadius = extent * 0.115f
                        val outerRadius = innerRadius + extent * (0.025f + 0.075f * wave)
                        val start = Offset(
                            center.x + cos(angle) * innerRadius,
                            center.y + sin(angle) * innerRadius
                        )
                        val end = Offset(
                            center.x + cos(angle) * outerRadius,
                            center.y + sin(angle) * outerRadius
                        )
                        drawLine(
                            color = Color(0xFFC9B5F0).copy(alpha = 0.12f + 0.30f * wave),
                            start = start,
                            end = end,
                            strokeWidth = (0.8f + wave * 0.7f).dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x263C2A58), Color(0x14251A38), Color.Transparent),
                            center = center,
                            radius = extent * 0.31f
                        ),
                        radius = extent * 0.31f,
                        center = center
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                ) {
                    Text(
                        "لا تبحث عن مصدر الصوت…",
                        color = Color(0xFFE5DDF2),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "أنا أسمع ما لم تقله.",
                        color = Color(0xFF9F8DBB),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.7.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        when (selectedForEntry) {
                            "death" -> "لحن الموت"
                            "sin" -> "لحن الخطيئة"
                            else -> "لحن القدر"
                        },
                        color = Color(0xFF70617F),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
