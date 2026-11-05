package com.aram.mahakama.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.aram.mahakama.R
import com.aram.mahakama.audio.AppAudioController

@Composable
fun AiSetupScreen(
    audio: AppAudioController,
    onContinue: () -> Unit
) {
    var status by remember { mutableStateOf("checking") }
var ready by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(attempt) {
        ready = false
        errorMessage = null
        status = "checking"

        audio.prepareGeminiService(
            onStatus = { status = it },
            onReady = {
                status = "ready"
                ready = true
                errorMessage = null
            },
            onError = { error ->
                errorMessage = error.localizedMessage
                    ?: "تعذر الاتصال بخدمة الصوت. تحقق من الإنترنت وحاول مجددًا."
                ready = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoView = this
                    setVideoURI(
                        Uri.parse("android.resource://${ctx.packageName}/${R.raw.mahakama_ai_setup}")
                    )
                    setOnPreparedListener { player ->
                        player.setVolume(0f, 0f)
                        player.isLooping = true
                        start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                }
            }
        )

        DisposableEffect(videoView) {
            onDispose {
                videoView?.stopPlayback()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xC9080710),
                            Color(0xD90C0918),
                            Color(0xF20A0812),
                            Color(0xF5080710)
                        )
                    )
                )
        )

        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "مرحلة التهيئة",
                    color = Color(0xFFD8B9FF),
                    fontSize = 14.sp,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "إيقاظ الذكاء الاصطناعي",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xE6100D1B),
                    border = BorderStroke(1.dp, Color(0x665F438B))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "قبل أن تبدأ المحاكمة",
                            color = Color(0xFFE6D9F7),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "تُهيّئ هذه المرحلة المساحة الصوتية للمحاكمة، لتصبح الحوارات والانعكاسات أكثر حضورًا وعمقًا، ويغدو صوت طيفك الذاتي جزءًا من تجربتك في استكشاف أعماقك.\n\nتأكد من اتصالك بالإنترنت لإتمام التهيئة والاستمتاع بالتجربة الصوتية.",
                            color = Color(0xFFD0C8D9),
                            fontSize = 15.sp,
                            lineHeight = 25.sp,
                            textAlign = TextAlign.Start
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = statusLabel(status),
                            color = if (errorMessage == null) Color(0xFFE1C9FF) else Color(0xFFFFA6A6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(13.dp))

                        if (!ready && errorMessage == null) {
                            CircularProgressIndicator(
                                color = Color(0xFFB58AEF),
                                strokeWidth = 3.dp
                            )
                        } else if (ready) {
                            Text(
                                text = "الخدمة متاحة",
                                color = Color(0xFFB8F0CC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        if (errorMessage != null) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFFA6A6),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { attempt++ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF69449A)
                                )
                            ) {
                                Text("إعادة المحاولة")
                            }
                        } else {
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = onContinue,
                                enabled = ready,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7952AA),
                                    disabledContainerColor = Color(0xFF393044),
                                    contentColor = Color.White,
                                    disabledContentColor = Color(0xFF958BA0)
                                )
                            ) {
                                Text(
                                    text = if (ready) "متابعة إلى المحاكمة" else "بانتظار اكتمال التهيئة",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "checking" -> "التحقق من الاتصال بخدمة الصوت..."
    "downloading" -> "جارٍ الاتصال بخدمة الصوت..."
    "extracting" -> "تجهيز الاتصال..."
    "verifying" -> "التحقق من استجابة الخدمة..."
    "initializing" -> "تهيئة خدمة الصوت..."
    "ready" -> "خدمة الصوت جاهزة"
    else -> "تهيئة المكوّنات..."
}
