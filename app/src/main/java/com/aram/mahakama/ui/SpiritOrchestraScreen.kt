package com.aram.mahakama.ui

import android.net.Uri
import android.widget.VideoView
import com.aram.mahakama.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
    var sceneVideoView by remember { mutableStateOf<VideoView?>(null) }

    val sceneAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (leaving) 0f else 1f,
        androidx.compose.animation.core.tween(520),
        label = "orchestra-exit-fade"
    )

    DisposableEffect(Unit) {
        onDispose {
            sceneVideoView?.stopPlayback()
            sceneVideoView = null
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
    }

    LaunchedEffect(leaving) {
        if (leaving) {
            delay(540L)
            onContinue(selectedTrack)
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
                                .clickable(enabled = !leaving) {
                                    onPreviewTrack(id)
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

                    Button(
                        onClick = { if (!leaving) leaving = true },
                        enabled = hasChosenTrack && !leaving,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD1C0F5),
                            contentColor = Color(0xFF20162C),
                            disabledContainerColor = Color(0xFF30273A),
                            disabledContentColor = Color(0xFF8B7A9F)
                        )
                    ) {
                        Text("اعتماد اللحن والدخول")
                        Spacer(Modifier.width(7.dp))
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "سيظل اختيارك محفوظًا، ويمكنك تغييره من الإعدادات.",
                        color = Color(0xFFAA9BB9),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
