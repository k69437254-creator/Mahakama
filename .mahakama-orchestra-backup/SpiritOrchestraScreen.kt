package com.aram.mahakama.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onSpeakName: (String, () -> Unit) -> Unit,
    onSpeakQuestion: (String) -> Unit,
    onContinue: (String) -> Unit
) {
    val motion = rememberInfiniteTransition(label = "spectral-orchestra-motion")
    val drift by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Restart), label = "spectral-fog-drift")
    val breath by motion.animateFloat(0.94f, 1.055f, infiniteRepeatable(tween(3900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "spectral-breath")
    val reveal = remember { Animatable(0f) }
    val selectionGlow = remember { Animatable(0.25f) }
    val scope = rememberCoroutineScope()
    var nameVisible by remember { mutableStateOf(false) }
    var questionVisible by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val sceneAlpha by animateFloatAsState(if (leaving) 0f else 1f, tween(520, easing = FastOutSlowInEasing), label = "orchestra-exit-fade")

    LaunchedEffect(userName) {
        delay(450L)
        reveal.animateTo(1f, tween(2300, easing = FastOutSlowInEasing))
        delay(500L)
        nameVisible = true
        onSpeakName("$userName…") {
            scope.launch {
                delay(260L)
                questionVisible = true
                onSpeakQuestion("$userName، أيُّ لحنٍ ترغب أن نعزفه أثناء محاكمتك؟")
            }
        }
    }
    LaunchedEffect(selectedTrack) {
        selectionGlow.snapTo(0.25f)
        selectionGlow.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(leaving) {
        if (leaving) {
            val chosenTrack = selectedTrack
            delay(540L)
            onContinue(chosenTrack)
        }
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = sceneAlpha }
            .background(Brush.verticalGradient(listOf(Color(0xFF050409), Color(0xFF100A18), Color(0xFF0B0811))))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val phase = drift * 2f * PI.toFloat()
            val extent = size.maxDimension
            drawRect(brush = Brush.radialGradient(listOf(Color(0x663D285C), Color(0x25201531), Color.Transparent), center = Offset(w * 0.5f, h * 0.39f), radius = extent * 0.8f))
            for (i in 0 until 9) {
                val x = w * (0.07f + i * 0.108f) + sin(phase * 0.42f + i) * w * 0.025f
                val y = h * (0.24f + (i % 4) * 0.16f + cos(phase * 0.55f + i * 0.4f) * 0.035f)
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFB39BEB).copy(alpha = 0.055f * reveal.value), Color.Transparent), center = Offset(x, y), radius = extent * (0.12f + (i % 3) * 0.035f)), radius = extent * (0.12f + (i % 3) * 0.035f), center = Offset(x, y))
            }
            // Featureless, drifting smoke entities; deliberately no eyes, mouths, masks, or human faces.
            val convergence = (0.5f + 0.5f * sin(phase * 0.31f)) * 0.035f
            drawSmokePresence(Offset(w * (0.255f + convergence), h * (0.345f + sin(phase * 0.55f) * 0.012f)), w * 0.28f, h * 0.34f, phase + 1.4f, reveal.value * 0.78f)
            drawSmokePresence(Offset(w * (0.745f - convergence), h * (0.35f + cos(phase * 0.48f) * 0.014f)), w * 0.27f, h * 0.33f, phase + 4.1f, reveal.value * 0.75f)
            drawSmokePresence(Offset(w * 0.5f, h * 0.325f), w * 0.39f * breath, h * 0.46f * breath, phase + 2.5f, reveal.value * (0.86f + selectionGlow.value * 0.14f))
            // A curling ribbon of mist is the conductor's cue, not a literal arm or face.
            val cx = w * 0.5f
            val cy = h * 0.35f
            val cue = Path().apply {
                moveTo(cx + w * 0.075f, cy + h * 0.02f)
                cubicTo(cx + w * 0.16f, cy - h * 0.035f, cx + w * 0.12f, cy - h * 0.12f, cx + w * 0.065f, cy - h * 0.145f)
                cubicTo(cx + w * 0.025f, cy - h * 0.17f, cx + w * 0.055f, cy - h * 0.205f, cx + w * 0.09f, cy - h * 0.23f)
            }
            drawPath(cue, brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0xB9C8B5FF).copy(alpha = reveal.value * selectionGlow.value * 0.58f), Color.Transparent), startY = cy - h * 0.24f, endY = cy + h * 0.03f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
            for (i in 0 until 34) {
                val seed = i * 0.6180339f
                val x = ((seed + drift * (if (i % 2 == 0) 0.085f else -0.065f) + 1f) % 1f) * w
                val y = ((i * 0.271f + drift * (0.055f + (i % 3) * 0.027f)) % 1f) * h
                val alpha = (0.018f + 0.045f * abs(sin(phase + i * 0.76f))) * reveal.value
                drawCircle(Color(0xFFE2D6FF).copy(alpha = alpha.coerceIn(0f, 0.065f)), (0.8f + i % 3 * 0.35f), Offset(x, y))
            }
            drawRect(brush = Brush.radialGradient(listOf(Color.Transparent, Color(0x66000000)), center = Offset(w * 0.5f, h * 0.42f), radius = extent * 0.87f))
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("الأوركسترا الأخيرة", color = Color(0xFFD7C6FF), style = MaterialTheme.typography.labelLarge, letterSpacing = 3.2.sp)
            Spacer(Modifier.height(2.dp))
            Text("قبل أن تبدأ المحاكمة", color = Color(0xFF9181A9), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(170.dp))
            AnimatedVisibility(visible = nameVisible, enter = fadeIn(tween(850)) + slideInVertically(tween(850, easing = FastOutSlowInEasing)) { it / 7 }) {
                Text("$userName…", color = Color(0xFFF1E9FF), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, letterSpacing = 1.2.sp)
            }
            AnimatedVisibility(visible = questionVisible, enter = fadeIn(tween(900)) + slideInVertically(tween(900, easing = FastOutSlowInEasing)) { it / 9 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Text("أيُّ لحنٍ ترغب أن نعزفه أثناء محاكمتك؟", color = Color(0xFFE2D7F3), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, lineHeight = 34.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("اختر ما تستطيع الإصغاء إليه؛ لا ما يبدو جميلًا فحسب.", color = Color(0xFFAA9ABF), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(19.dp))
            Text("الألحان الثلاثة", color = Color(0xFFD7C6FF), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(9.dp))
            val soundtracks = listOf(Triple("death", "لحن الموت", "حين يقترب الصمت"), Triple("sin", "لحن الخطيئة", "اعترافٌ يتأخر"), Triple("fate", "لحن القدر", "ما كُتب وما تختاره"))
            soundtracks.forEach { (id, title, subtitle) ->
                val selected = selectedTrack == id
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(enabled = !leaving) { onPreviewTrack(id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF2B203D) else Color(0xD916111F)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(Modifier.fillMaxWidth().border(1.dp, if (selected) Color(0xFFB9A1FF) else Color(0x443B3048), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(43.dp).background(Brush.radialGradient(listOf(if (selected) Color(0xFF8064B4) else Color(0xFF352842), Color(0xFF17111F))), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFE1D5FF), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = Color(0xFFF2EBFF), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(subtitle, color = Color(0xFFB7A9C9), style = MaterialTheme.typography.bodySmall)
                        }
                        if (selected && hasChosenTrack) Text("يُعزف الآن", color = Color(0xFFD0BCFF), style = MaterialTheme.typography.labelSmall)
                        else if (selected) Text("اختر هذا اللحن", color = Color(0xFFB7A9C9), style = MaterialTheme.typography.labelSmall)
                        else Icon(Icons.Default.ChevronLeft, contentDescription = "تجربة $title", tint = Color(0xFF8F7CA9))
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text("يتراجع الضباب حين تختار، ويبقى اللحن معك عبر شاشات المحاكمة.", color = Color(0xFF9587A8), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { if (!leaving) leaving = true },
                enabled = questionVisible && hasChosenTrack && !leaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD1C0F5), contentColor = Color(0xFF20162C), disabledContainerColor = Color(0xFF30273A), disabledContentColor = Color(0xFF8B7A9F))
            ) {
                Text("اعتماد اللحن والدخول")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }
            Spacer(Modifier.height(10.dp))
            Text("سيظل اختيارك محفوظًا، ويمكنك تغييره من الإعدادات.", color = Color(0xFF887A9A), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(15.dp))
        }
    }
}

private fun DrawScope.drawSmokePresence(center: Offset, width: Float, height: Float, phase: Float, strength: Float) {
    if (strength <= 0.001f) return
    val w = width
    val h = height
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF9A7BCD).copy(alpha = 0.17f * strength), Color(0xFF5A427D).copy(alpha = 0.075f * strength), Color.Transparent), center = center, radius = w * 0.78f), radius = w * 0.78f, center = center)
    val cx = center.x
    val cy = center.y
    val body = Path().apply {
        moveTo(cx - w * 0.08f, cy - h * 0.48f)
        cubicTo(cx - w * 0.34f, cy - h * 0.43f, cx - w * 0.28f, cy - h * 0.20f, cx - w * 0.31f, cy - h * 0.05f)
        cubicTo(cx - w * 0.42f, cy + h * 0.12f, cx - w * 0.23f, cy + h * 0.19f, cx - w * 0.36f, cy + h * 0.35f)
        cubicTo(cx - w * 0.24f, cy + h * 0.31f, cx - w * 0.16f, cy + h * 0.48f, cx - w * 0.04f, cy + h * 0.39f)
        cubicTo(cx + w * 0.10f, cy + h * 0.52f, cx + w * 0.20f, cy + h * 0.32f, cx + w * 0.35f, cy + h * 0.38f)
        cubicTo(cx + w * 0.23f, cy + h * 0.18f, cx + w * 0.42f, cy + h * 0.07f, cx + w * 0.28f, cy - h * 0.10f)
        cubicTo(cx + w * 0.31f, cy - h * 0.29f, cx + w * 0.20f, cy - h * 0.43f, cx - w * 0.08f, cy - h * 0.48f)
        close()
    }
    drawPath(body, brush = Brush.verticalGradient(colorStops = arrayOf(
        0f to Color(0xFFE4D9FF).copy(alpha = 0.19f * strength),
        0.38f to Color(0xFF8B70B6).copy(alpha = 0.17f * strength),
        0.73f to Color(0xFF433154).copy(alpha = 0.10f * strength),
        1f to Color(0xFF21182D).copy(alpha = 0.015f * strength)
    ), startY = cy - h * 0.49f, endY = cy + h * 0.49f))
    for (i in 0 until 5) {
        val direction = if (i % 2 == 0) 1f else -1f
        val startX = cx + (i - 2) * w * 0.105f
        val sway = sin(phase + i * 0.9f) * w * 0.075f
        val tendril = Path().apply {
            moveTo(startX, cy + h * 0.22f)
            cubicTo(startX + sway, cy + h * 0.33f, startX + sway + direction * w * 0.10f, cy + h * 0.40f, startX + sway - direction * w * 0.04f, cy + h * (0.53f + (i % 2) * 0.025f))
        }
        drawPath(tendril, brush = Brush.verticalGradient(listOf(Color(0xFFBCA7E8).copy(alpha = 0.12f * strength), Color(0xFF8064A5).copy(alpha = 0.07f * strength), Color.Transparent), startY = cy + h * 0.2f, endY = cy + h * 0.58f), style = Stroke(width = (0.8f + i % 3 * 0.45f).dp.toPx(), cap = StrokeCap.Round))
    }
    val eddy = Path().apply {
        moveTo(cx - w * 0.34f, cy + h * 0.11f)
        cubicTo(cx - w * (0.48f + sin(phase) * 0.025f), cy - h * 0.02f, cx - w * 0.11f, cy - h * 0.28f, cx + w * 0.21f, cy - h * 0.12f)
    }
    drawPath(eddy, brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFB9A1FF).copy(alpha = 0.065f * strength), Color.Transparent), startX = cx - w * 0.48f, endX = cx + w * 0.30f), style = Stroke(width = 1.15f.dp.toPx(), cap = StrokeCap.Round))
}
