package com.aram.mahakama.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.mahakama.R
import com.aram.mahakama.data.UserState
import com.aram.mahakama.domain.FaceInsight
import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.domain.TrialQuestion
import com.aram.mahakama.domain.formatElapsedTime
import kotlinx.coroutines.delay

private val Backdrop = Color(0xFF0C0A12)
private val Panel = Color(0xFF15111D)
private val PanelRaised = Color(0xFF1D1728)
private val Violet = Color(0xFFB9A1FF)
private val MutedText = Color(0xFFACA1BC)
private val Line = Color(0xFF382E46)


@Composable
fun CinematicOpeningScreen(onSkip: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "mahakama-opening-motion")
    val pulse by infinite.animateFloat(
        initialValue = 0.965f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem-pulse"
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit-rotation"
    )
    val progress = remember { Animatable(0f) }
    var revealTitle by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(420L)
        revealTitle = true
        progress.animateTo(1f, animationSpec = tween(durationMillis = 2850, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF251733), Color(0xFF100D17), Backdrop),
                    radius = 1050f
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.37f)
            val haloRadius = size.maxDimension * 0.53f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x355E3C91), Color(0x1E3E285F), Color.Transparent),
                    center = center,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = center
            )
            rotate(degrees = orbit, pivot = center) {
                drawCircle(
                    color = Color(0x397F61B8),
                    center = center,
                    radius = size.minDimension * 0.30f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawArc(
                    color = Color(0xA6B9A1FF),
                    startAngle = -52f,
                    sweepAngle = 64f,
                    useCenter = false,
                    topLeft = Offset(center.x - size.minDimension * 0.30f, center.y - size.minDimension * 0.30f),
                    size = Size(size.minDimension * 0.60f, size.minDimension * 0.60f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x4EB9A1FF), Color(0x95B9A1FF), Color(0x30B9A1FF), Color.Transparent),
                    startY = size.height * 0.08f,
                    endY = size.height * 0.78f
                ),
                start = Offset(size.width * 0.5f, size.height * 0.08f),
                end = Offset(size.width * 0.5f, size.height * 0.78f),
                strokeWidth = 1.dp.toPx()
            )
            for (i in 0 until 22) {
                val x = size.width * ((i * 37 % 97) / 100f)
                val y = size.height * ((i * 53 % 89) / 100f)
                val radius = if (i % 4 == 0) 1.5.dp.toPx() else 0.7.dp.toPx()
                drawCircle(Color(0x447E65A7), radius = radius, center = Offset(x, y))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.70f))
            Box(Modifier.size(178.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color(0x2EB9A1FF),
                        radius = size.minDimension * (0.47f + pulse * 0.015f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0x497C5AA9),
                        radius = size.minDimension * 0.39f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    rotate(degrees = -orbit * 0.45f, pivot = center) {
                        drawArc(
                            color = Color(0xFF9D80D7).copy(alpha = 0.8f),
                            startAngle = 200f,
                            sweepAngle = 74f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
                            size = Size(size.width * 0.84f, size.height * 0.84f),
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
                }
                Image(
                    painter = painterResource(R.drawable.mahakama_brand_mark),
                    contentDescription = "شعار المحاكمة",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, Color(0x80C5AEFF), RoundedCornerShape(32.dp))
                )
            }
            Spacer(Modifier.height(29.dp))
            AnimatedVisibility(
                visible = revealTitle,
                enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 7 }
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "المحاكمة",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 36.sp,
                        color = Color(0xFFF0E7FF),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "حين تصبح أنت القاضي",
                        color = Color(0xFFC7B1F2),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(Modifier.height(13.dp))
                    Text(
                        text = "بعض الحقائق لا تُكتشف إلا حين تصغي إلى نفسك.",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.weight(0.95f))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.64f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x443F3154))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7352AE), Color(0xFFD5C4FF))))
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("افتح عينيك على ما وراء الرواية…", color = Color(0xFF8D819F), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(9.dp))
            TextButton(onClick = onSkip) {
                Text("تجاوز المشهد", color = Color(0xFF9B8CB2), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun NameEntryScreen(onSubmit: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(104.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF5D4388), Color(0xFF21172F), Backdrop))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Gavel, contentDescription = null, tint = Violet, modifier = Modifier.size(46.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("المحاكمة", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFF0E8FF))
        Spacer(Modifier.height(7.dp))
        Text("رحلة إلى ما وراء الأقنعة", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(38.dp))
        Text("قبل أن تُفتح القاعة…", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "بأيّ اسمٍ تريد أن تُواجَه هنا؟ الاسم الذي تعرفك به الحياة، أم الاسم الذي تختاره حين تقف أمام نفسك؟",
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(32) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("هويتك في المحاكمة") },
            placeholder = { Text("اكتب الاسم الذي يمثّلك") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, focusedLabelColor = Violet)
        )
        Spacer(Modifier.height(10.dp))
        Text("يمكنك اختيار اسمك الحقيقي أو اسمًا يمثّلك. سيظل معك داخل رحلتك.", color = MutedText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = { if (name.isNotBlank()) onSubmit(name.trim()) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp)
        ) { Text("تثبيت الاسم والدخول") }
        Spacer(Modifier.height(20.dp))
        Text("ليس مطلوبًا أن تكشف كل شيء عن نفسك؛ يكفي أن تبدأ بشيءٍ تختاره بنفسك.", color = Color(0xFF7D728E), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
fun WelcomeScreen(userName: String, onBegin: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 22.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = Violet, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(8.dp))
                Text("المحاكمة", style = MaterialTheme.typography.titleLarge)
                Text("الجلسة الأولى", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
        Divider(color = Line)
        Spacer(Modifier.height(22.dp))
        Text("أهلًا بك، $userName.", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF0E7FF))
        Spacer(Modifier.height(18.dp))
        val paragraphs = listOf(
            "لقد وصلت إلى المكان الذي لا تكفي فيه الإجابات الجاهزة، ولا تنفع فيه الأقنعة التي أتقنت ارتداءها أمام الآخرين.",
            "هنا، لن تُستدعَ لتُدان، ولن تُمنح براءةً لأنك أحسنت الدفاع عن نفسك.",
            "ستُعرض أمامك اختياراتك، وتناقضاتك، ودوافعك، والأشياء التي تدافع عنها بشدة؛ لعلّ بعضها يستحق دفاعك حقًا، ولعلّ بعضها الآخر لم تسأل نفسك يومًا لماذا تتمسك به.",
            "ستسمع رواياتٍ متعددة عن الشخص الذي أنت عليه؛ رواية ما تقوله عن نفسك، ورواية ما تكشفه اختياراتك، وشهادة المواقف التي تتكرر في حياتك. وقد لا تتفق هذه الروايات دائمًا.",
            "لكن تذكّر: ليس كل تناقض إدانة، وليس كل اعتراف حقيقةً كاملة.",
            "في هذه القاعة، لن يكون هناك قاضٍ يفرض عليك حكمًا من الخارج. ستتولى أنت هذه المهمة؛ لا لتجلد نفسك، ولا لتبرئها بأي ثمن، بل لتتعلم كيف تنظر إليها من دون أن يخدعك غرورك، أو يعميك خوفك، أو تقودك رغبتك في أن تبدو شخصًا آخر.",
            "لن نخبرك من أنت؛ سنساعدك على فحص الأدلة التي تكوّن بها تصورك عن نفسك.",
            "فأنت هنا المتهم في استعارة المحاكمة، والشاهد على حياتك، والقاضي الذي يجب أن يتعلم الإنصاف.",
            "ارفع الستار عن روايتك، واستعد للجلسة الأولى. فالمحاكمة الحقيقية لا تبدأ حين تُوجَّه إليك التهمة… بل حين تجرؤ على الإصغاء إلى الحقيقة، حتى عندما لا تأتي على هواك."
        )
        paragraphs.forEachIndexed { index, paragraph ->
            Text(paragraph, color = if (index == 4 || index == 7) Color(0xFFD3C3F5) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(13.dp))
        }
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("هل أنت مستعد لمواجهة نفسك؟", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("لا تبحث عن الإجابة المثالية. ابحث عن الإجابة التي تستطيع الوقوف خلفها.", color = MutedText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBegin, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("ابدأ المحاكمة") }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

data class FeatureItem(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)

@Composable
fun HomeScreen(userState: UserState, elapsedMillis: Long, onNavigate: (String) -> Unit) {
    val answeredCount = QuestionnaireEngine.answeredOptions(userState.answers).size
    val features = listOf(
        FeatureItem("جلسة الأسئلة", "كل اختيار يغيّر مسار المواجهة", Icons.Default.QuestionAnswer, "questions"),
        FeatureItem("شجرة الوجوه", "احتمالات تدعمها الأدلة وتراجعها", Icons.Default.AccountTree, "faces"),
        FeatureItem("كشف القناع", "ما تقوله وما تكشفه اختياراتك", Icons.Default.Visibility, "unmasking"),
        FeatureItem("ذاتك الأخرى", "طرق لم تعشها بعد", Icons.Default.AutoAwesome, "other_you"),
        FeatureItem("المحكمة الداخلية", "استمع إلى الحجج قبل الحكم", Icons.Default.Gavel, "inner_court"),
        FeatureItem("صدى الماضي", "قارن بين إجاباتك عبر الزمن", Icons.Default.History, "echo"),
        FeatureItem("الغرفة الصامتة", "مساحة تكتب فيها بلا مقاطعة", Icons.Default.EditNote, "silent_room"),
        FeatureItem("إعدادات المحاكمة", "الاسم والصوت والتفضيلات", Icons.Default.Settings, "settings")
    )

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("المحاكمة", color = Color(0xFFF4EEFF), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(3.dp))
                Text("أهلًا بعودتك، ${userState.userName}.", color = Color(0xFFCFC3E0), style = MaterialTheme.typography.bodyMedium)
            }
            Box(
                Modifier.size(52.dp)
                    .clip(CircleShape)
                    .background(PanelRaised)
                    .border(1.dp, Color(0x667B61A7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = "رمز المحاكمة", tint = Violet, modifier = Modifier.size(29.dp))
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF30213F), Color(0xFF1A1425), Color(0xFF24172F))))
                .border(1.dp, Color(0x557D63AD), RoundedCornerShape(28.dp))
        ) {
            Canvas(Modifier.matchParentSize()) {
                val extent = size.maxDimension
                val center = Offset(size.width * 0.12f, size.height * 0.42f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x55B9A1FF), Color(0x183D2A59), Color.Transparent),
                        center = center,
                        radius = extent * 0.62f
                    ),
                    radius = extent * 0.62f,
                    center = center
                )
                for (i in 0 until 4) {
                    drawCircle(
                        color = Color(0x25D5C5FF),
                        radius = extent * (0.12f + i * 0.07f),
                        center = center,
                        style = Stroke(width = (0.7f + i * 0.25f).dp.toPx())
                    )
                }
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0x88B9A1FF), Color.Transparent)),
                    startAngle = -65f,
                    sweepAngle = 145f,
                    useCenter = false,
                    topLeft = Offset(center.x - extent * 0.36f, center.y - extent * 0.36f),
                    size = Size(extent * 0.72f, extent * 0.72f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("ملف القضية  •  ٠٠١", color = Color(0xFFD4C4FF), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "لم يصدر الحكم بعد.",
                            color = Color(0xFFF8F3FF),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "هذه ليست قائمة أسئلة؛ إنها رحلة تتغيّر ملامحها مع كل اختيار.",
                            color = Color(0xFFD0C4DF),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(62.dp)
                            .clip(CircleShape)
                            .background(Color(0x33291E39))
                            .border(1.dp, Color(0x667D63AD), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFD1BFFF), modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(32.dp).clip(CircleShape).background(Violet))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (answeredCount == 0) "كل شيء يبدأ بملاحظة صادقة" else "لقد تركت أثرًا في هذا الملف",
                            color = Color(0xFFF1EAFE),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "$answeredCount إجابة محفوظة من مسارك الحالي",
                            color = Color(0xFFC2B5D5),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onNavigate("questions") },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4C4FF),
                        contentColor = Color(0xFF21172D)
                    )
                ) {
                    Text(
                        if (answeredCount == 0) "ابدأ المواجهة" else "تابع من حيث توقفت",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        TimerCard(elapsedMillis = elapsedMillis, compact = true, onClick = { onNavigate("clock") })

        Spacer(Modifier.height(26.dp))
        Text("خريطة المحاكمة", color = Color(0xFFF1EAFE), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "مسارات مترابطة داخل قضية واحدة؛ افتح الباب الذي تحتاجه الآن.",
            color = MutedText,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                features.forEachIndexed { index, feature ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onNavigate(feature.route) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PanelRaised)
                                .border(1.dp, Color(0x443F3450), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(feature.icon, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(feature.title, color = Color(0xFFF0E9FA), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(feature.subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                String.format(java.util.Locale.ROOT, "%02d", index + 1),
                                color = Color(0xFF9F8BBE),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Icon(Icons.Default.ChevronLeft, contentDescription = "فتح ${feature.title}", tint = Color(0xFFB9A1FF), modifier = Modifier.size(18.dp))
                        }
                    }
                    if (index < features.lastIndex) {
                        Divider(color = Color(0xFF34283F), modifier = Modifier.padding(start = 14.dp, end = 14.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth()
                .clickable { onNavigate("profile") }
                .border(1.dp, Color(0x557B61A7), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF20182B)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = Violet, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ملف الانعكاس", color = Color(0xFFF1EAFE), style = MaterialTheme.typography.titleSmall)
                    Text("صورة متحركة قابلة للمراجعة، لا تعريف نهائي بك.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Violet)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun TimerCard(elapsedMillis: Long, compact: Boolean, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(if (compact) 22.dp else 28.dp)
    val modifier = Modifier.fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .border(1.dp, Color(0xFF3A2E49), shape)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (compact) Color(0xFF191421) else Color(0xFF1D1728)),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) 15.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerDial(elapsedMillis, modifier = Modifier.size(if (compact) 82.dp else 148.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (compact) "الوقت المتراكم" else "الزمن الذي شهد مواجهتك",
                    color = Color(0xFFD0BCFF),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatElapsedTime(elapsedMillis),
                    color = Color(0xFFF6F0FF),
                    style = (if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
                        .copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr, letterSpacing = 1.2.sp),
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "وقت حقيقي محفوظ، لا يبدأ من الصفر عند عودتك.",
                    color = Color(0xFFBEB2CF),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimerDial(elapsedMillis: Long, modifier: Modifier = Modifier) {
    val elapsedSeconds = (elapsedMillis.coerceAtLeast(0L) / 1_000L) % 60L
    val progress = elapsedSeconds.toFloat() / 60f
    Canvas(modifier) {
        val minSize = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringRadius = minSize * 0.365f
        val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
        val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x553D2D56), Color(0x22271D38), Color.Transparent),
                center = center,
                radius = minSize * 0.49f
            ),
            radius = minSize * 0.49f,
            center = center
        )
        drawArc(
            color = Color(0xFF3A2E49),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = minSize * 0.095f, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(Color(0xFF7252AC), Color(0xFFB9A1FF), Color(0xFFE0D5FF))),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = minSize * 0.095f, cap = StrokeCap.Round)
        )
        for (tick in 0 until 60) {
            val angle = (-90.0 + tick * 6.0) * PI / 180.0
            val outerRadius = minSize * 0.475f
            val innerRadius = outerRadius - if (tick % 5 == 0) minSize * 0.065f else minSize * 0.028f
            val start = Offset(
                center.x + cos(angle).toFloat() * innerRadius,
                center.y + sin(angle).toFloat() * innerRadius
            )
            val end = Offset(
                center.x + cos(angle).toFloat() * outerRadius,
                center.y + sin(angle).toFloat() * outerRadius
            )
            drawLine(
                color = if (tick.toLong() <= elapsedSeconds) Color(0xFFD0BCFF).copy(alpha = 0.78f) else Color(0xFF776987).copy(alpha = if (tick % 5 == 0) 0.9f else 0.45f),
                start = start,
                end = end,
                strokeWidth = if (tick % 5 == 0) 1.35.dp.toPx() else 0.7.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = Color(0xFFE3D7FF), radius = minSize * 0.035f, center = center)
        val handAngle = (-90.0 + elapsedSeconds * 6.0) * PI / 180.0
        drawLine(
            color = Color(0xFFD2C2FF),
            start = center,
            end = Offset(
                center.x + cos(handAngle).toFloat() * minSize * 0.22f,
                center.y + sin(handAngle).toFloat() * minSize * 0.22f
            ),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "عودة", tint = Violet) }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun QuestionnaireScreen(
    userName: String,
    question: TrialQuestion?,
    answeredCount: Int,
    totalAnswers: Int,
    onAnswer: (TrialQuestion, String) -> Unit,
    onGoFaces: () -> Unit,
    onGoUnmasking: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("الجلسة الجارية", color = Violet, style = MaterialTheme.typography.bodySmall)
                Text("${userName}، أصغِ إلى اختيارك", style = MaterialTheme.typography.titleLarge)
                Text("مسار متفرّع من بنكٍ يضم ١٠٠٠ سؤال", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.clip(CircleShape).background(PanelRaised).padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text("$answeredCount إجابة", color = Violet, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Line)) {
            Box(Modifier.fillMaxWidth((answeredCount.toFloat() / QuestionnaireEngine.questions.size.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF6E4CA6), Violet))))
        }
        Spacer(Modifier.height(5.dp))
        Text("عدد الإجابات في مسارك الحالي من أصل بنك الأسئلة، لا من جلسةٍ ثابتة.", color = MutedText, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(20.dp))
        if (question == null) {
            Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Violet, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("وصلت إلى نهاية هذا المسار", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("اختياراتك أوصلتك إلى عقدةٍ أخيرة في هذا الطريق. ما ظهر لك ليس حكمًا أخيرًا؛ راجع الأدلة والسياق والأوجه التي لم تتسع لها هذه الجلسة.", color = MutedText, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onGoFaces, modifier = Modifier.fillMaxWidth()) { Text("افتح شجرة الوجوه") }
                    TextButton(onClick = onGoUnmasking) { Text("اعرض كشف القناع") }
                }
            }
        } else {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2B2039), Color(0xFF18131F), Color(0xFF21172B))))
                    .border(1.dp, Color(0x557B61A7), RoundedCornerShape(26.dp))
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width * 0.92f, size.height * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0x339C7BDF), Color.Transparent), center, size.maxDimension * 0.56f),
                        radius = size.maxDimension * 0.56f,
                        center = center
                    )
                }
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(Color(0x332D2340))
                                .border(1.dp, Color(0x557B61A7), RoundedCornerShape(10.dp))
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "المشهد ${String.format(java.util.Locale.ROOT, "%03d", answeredCount + 1)}",
                                color = Color(0xFFD3C3FF),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(question.heading, color = Color(0xFFD3C3FF), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        question.prompt,
                        color = Color(0xFFF7F2FF),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "لا تبحث عن الإجابة التي تبدو أجمل؛ لاحظ ما يحدث فيك عندما يصبح الاختيار حقيقيًا.",
                        color = Color(0xFFC9BDD8),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Violet))
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("ما الاستجابة الأقرب إليك؟", color = Color(0xFFF0E9FA), style = MaterialTheme.typography.titleMedium)
                    Text("اختر أثرًا، وسيتغيّر الطريق بعده.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17121E)),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    question.options.forEachIndexed { index, option ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onAnswer(question, option.id) }
                                .padding(horizontal = 15.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(38.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color(0xFF2A2038))
                                    .border(1.dp, Color(0x665C477D), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    String.format(java.util.Locale.ROOT, "%02d", index + 1),
                                    color = Color(0xFFD2C1FF),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(option.title, color = Color(0xFFF3ECFB), style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(option.detail, color = Color(0xFFBEB2CF), style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Violet)
                        }
                        if (index < question.options.lastIndex) {
                            Divider(color = Color(0xFF33283E), modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "لا توجد إجابة مثالية. توجد زاوية أخرى تستحق أن تُرى.",
                color = Color(0xFFB7A9C9),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FacesScreen(userState: UserState) {
    val faces = QuestionnaireEngine.faces.map { it to QuestionnaireEngine.faceScore(it, userState.answers) }.sortedByDescending { it.second }
    var expandedFaceId by rememberSaveable { mutableStateOf(faces.firstOrNull()?.first?.id.orEmpty()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = Violet, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("شجرة الوجوه", style = MaterialTheme.typography.titleMedium)
                        Text("الجذع واحد؛ أما الوجوه فتتغير بحسب السياق.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("لا تنظر إلى هذه الوجوه كأنها هويتك النهائية. إنها احتمالات تتقوى وتضعف مع ما تقدمه من أدلة.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(17.dp))
        faces.forEachIndexed { index, (face, score) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(2.dp).height(13.dp).background(if (index == 0) Violet else Line))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (expandedFaceId == face.id) Violet else Color(0xFF67557D)))
                    if (index < faces.lastIndex) Box(Modifier.width(2.dp).height(36.dp).background(Line))
                }
                Spacer(Modifier.width(4.dp))
                FaceDetailCard(
                    face = face,
                    score = score,
                    index = index,
                    userState = userState,
                    expanded = expandedFaceId == face.id,
                    onClick = { expandedFaceId = if (expandedFaceId == face.id) "" else face.id },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FaceDetailCard(
    face: FaceInsight,
    score: Int,
    index: Int,
    userState: UserState,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allAnswers = QuestionnaireEngine.answeredOptions(userState.answers)
    val support = allAnswers.filter { (_, option) -> option.tags.any { it in face.tags } }
    val counterTags = when (face.id) {
        "guardian" -> setOf("acceptance", "flexibility", "agency")
        "seeker" -> setOf("avoidance", "restlessness")
        "mirror" -> setOf("self_protection", "shame")
        "keeper" -> setOf("authenticity", "boundaries", "values")
        "challenger" -> setOf("avoidance", "fear")
        "critic" -> setOf("self_compassion", "acceptance")
        else -> setOf("courage", "agency", "authenticity")
    }
    val counter = allAnswers.filter { (_, option) -> option.tags.any { it in counterTags } }
    val context = when (face.id) {
        "guardian" -> "قد يظهر هذا الوجه حين يزداد الغموض أو تشعر أن نتيجةً مهمة قد تفلت من يدك."
        "seeker" -> "قد يظهر حين تحتاج إلى فهم دوافعك أو حين تبدو الإجابة السهلة أقل من تعقيد التجربة."
        "mirror" -> "قد يظهر بعد خطأ أو اختيارٍ صعب، حين تقرر النظر إلى ما فعلته دون اختصار نفسك فيه."
        "keeper" -> "قد يظهر في العلاقات التي تخشى خسارتها، خاصةً حين تتعارض حاجتك إلى القرب مع حدودك."
        "challenger" -> "قد يظهر عندما يتحول التردد إلى قرار أو حين تختار تحمل نتائج خطوةٍ جديدة."
        "critic" -> "قد يظهر عندما تقارن نفسك بمعيارٍ مرتفع أو تتخيل حكم الآخرين على خطئك."
        else -> "قد يظهر حين تحتاج إلى المسافة أو تؤجل مواجهةً لا تعرف بعد كيف تحمل نتائجها."
    }
    val related = when (face.id) {
        "guardian" -> "الباحث عن المعنى، القاضي الداخلي"
        "seeker" -> "الصريح مع المرآة، المنسحب إلى الصمت"
        "mirror" -> "الباحث عن المعنى، المقاوم"
        "keeper" -> "القاضي الداخلي، الصريح مع المرآة"
        "challenger" -> "الصريح مع المرآة، الباحث عن المعنى"
        "critic" -> "حارس الانتماء، الحارس"
        else -> "الحارس، المقاوم"
    }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (expanded || index == 0) PanelRaised else Panel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(35.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2B2039)), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = Violet, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(face.title, style = MaterialTheme.typography.titleSmall)
                    Text(face.subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text("$score", color = Violet, style = MaterialTheme.typography.titleMedium)
                Icon(if (expanded) Icons.Default.ChevronLeft else Icons.Default.Lightbulb, contentDescription = null, tint = Violet, modifier = Modifier.padding(start = 3.dp))
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text(face.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text("السياق المحتمل", color = Violet, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(context, color = MutedText, style = MaterialTheme.typography.bodySmall)
                Divider(Modifier.padding(vertical = 10.dp), color = Line)
                Text("الأدلة المؤيدة", color = Violet, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(5.dp))
                Text(
                    support.take(2).joinToString("\n") { "• ${it.second.title} — ${it.second.detail}" }.ifBlank { "لم تتجمع أدلة كافية بعد. تابع الإجابة، واترك الصورة تتشكل ببطء." },
                    color = MutedText, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                Text("الأدلة التي قد تعارضه", color = Violet, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(5.dp))
                Text(
                    counter.take(2).joinToString("\n") { "• ${it.second.title} — ${it.second.detail}" }.ifBlank { "لا تظهر حتى الآن أدلة مضادة كافية؛ وغيابها لا يعني أن هذا الوجه يصفك بالكامل." },
                    color = MutedText, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                Text("وجوه قريبة: $related", color = Color(0xFFD5C5F4), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                Text("سؤال لم يُحسم: في أيّ موقفٍ من حياتك يظهر هذا الوجه، وفي أيّ موقفٍ يختفي؟", color = Color(0xFFD5C5F4), style = MaterialTheme.typography.bodySmall)
            } else {
                Spacer(Modifier.height(7.dp))
                Text("اضغط لعرض الأدلة، والسياق، والوجوه القريبة.", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun UnmaskingScreen(userState: UserState, onFeedback: (String) -> Unit) {
    val (title, body) = QuestionnaireEngine.unmasking(userState.answers)
    val evidence = QuestionnaireEngine.answeredOptions(userState.answers).takeLast(4)
    var feedback by rememberSaveable(userState.unmaskingFeedback) { mutableStateOf(userState.unmaskingFeedback) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Violet, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(14.dp))
                Text("كشف القناع", color = Violet, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("الأدلة التي تستحق العودة إليها", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        evidence.forEach { (question, option) ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(question.heading, color = Violet, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(option.title, style = MaterialTheme.typography.titleSmall)
                    Text(option.detail, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("هذه القراءة احتمالٌ يستند إلى إجاباتك، وليست حكمًا عليك. أنت أدرى بالسياق الذي جاءت منه.", color = MutedText, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Text("هل تجد هذه الملاحظة قريبة من تجربتك؟", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("قريبة من تجربتي", "لا تمثلني بدقة").forEach { choice ->
                Button(onClick = { feedback = choice; onFeedback(choice) }, colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = Color(0xFFE9DEFF)), modifier = Modifier.weight(1f)) { Text(choice, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (feedback.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(if (feedback == "قريبة من تجربتي") "سنعاملها كقراءة مؤقتة، لا كحقيقة نهائية." else "ستبقى الملاحظة قابلة للمراجعة؛ رفضك لها دليل مهم أيضًا.", color = Violet, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun OtherYouScreen(userState: UserState) {
    var selected by rememberSaveable { mutableStateOf("") }
    val personalizedPath = QuestionnaireEngine.alternateSelfReflection(userState.answers)
    val scenarios = listOf(
        Triple("truth", "لو قلت الحقيقة كاملة", "تخيّل أنك اخترت الصراحة في موقفٍ كنت تحاول فيه حماية صورة أو علاقة. ما الذي قد تخسره؟ وما الذي قد تستعيده من نفسك؟"),
        Triple("leave", "لو غادرت ما اعتدت عليه", "تخيّل أن الخوف لم يختفِ، لكنه لم يعد صاحب القرار. ما الخطوة الصغيرة التي ستتخذها، وما الثمن الذي تقبل تحمله؟"),
        Triple("forgive", "لو سامحت نفسك دون إنكار الخطأ", "تخيّل أنك تحمل مسؤولية ما فعلت، لكنك توقفت عن تحويل الخطأ إلى تعريفٍ كامل لشخصك. كيف ستبدو خطوتك التالية؟")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("هذه ليست نبوءات عن حياةٍ لم تعشها؛ إنها تجارب فكرية تكشف القيم التي قد تقود خياراتك.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("المسار الأقرب إلى إجاباتك", color = Violet, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(personalizedPath, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(15.dp))
        scenarios.forEach { (id, title, description) ->
            Card(colors = CardDefaults.cardColors(containerColor = if (selected == id) PanelRaised else Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selected = if (selected == id) "" else id }) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Violet)
                        Spacer(Modifier.width(10.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }
                    if (selected == id) {
                        Spacer(Modifier.height(12.dp))
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        Text("سؤال لك: أيّ قيمةٍ تريد أن تحملها معك إلى القرار الحقيقي؟", color = Violet, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Spacer(Modifier.height(5.dp))
                        Text("افتح هذا الاحتمال وتأمل ما يوقظه فيك.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun InnerCourtScreen(userState: UserState) {
    var selectedCase by rememberSaveable { mutableIntStateOf(0) }
    val personalizedLens = QuestionnaireEngine.innerCourtLens(userState.answers)
    val cases = listOf(
        Triple("صديقك أخطأ، والاعتراف قد يدمّر ما تبقى من سمعته.", "هل تحميه من الحقيقة أم تحميه من نتائجها؟", "العدل لا يتجاهل السياق، لكنه لا يمحو أثر الفعل."),
        Triple("تملك فرصةً نادرة، لكن الوصول إليها يتطلب أن تتراجع عن وعدٍ قطعته.", "أيّهما أثقل في ميزانك: الوعد أم ما قد يغير مستقبلك؟", "قد تكون هناك تفاصيل مفقودة؛ افحص البدائل قبل أن تختار بين طرفين فقط."),
        Triple("شخصٌ جرحك اعتذر بصدق، لكن الثقة لم تعد كما كانت.", "هل يكفي الاعتذار لتعود العلاقة، أم تحتاج الثقة إلى زمنٍ وسلوك؟", "المسامحة والثقة قراران مختلفان، ولا يجب أن يتطابق توقيتهما.")
    )
    val selected = cases[selectedCase]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("أربعة أصوات على الطاولة، لكن الحكم الأخير يبقى لك. الغرض ليس الوصول إلى إجابةٍ مثالية، بل رؤية ما تحمله كل حجة من قوةٍ ونقص.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(13.dp))
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(personalizedLens.first, color = Violet, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(personalizedLens.second, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text("القضية", color = Violet, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(7.dp))
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(selected.first, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cases.forEachIndexed { index, _ ->
                        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (selectedCase == index) Violet else Line).clickable { selectedCase = index })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cases.indices.forEach { index ->
                        Text("قضية ${index + 1}", modifier = Modifier.weight(1f).clickable { selectedCase = index }, color = if (selectedCase == index) Violet else MutedText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val roles = listOf(
            Triple("المُدّعي", Icons.Default.Gavel, "ما الضرر الذي قد يحدث إذا تجاهلنا مسؤولية الشخص؟"),
            Triple("المُدافع", Icons.Default.Gavel, "ما الظروف أو الدوافع التي تجعل القرار مفهومًا دون أن تبرره بالكامل؟"),
            Triple("الشاهد", Icons.Default.Visibility, "ما المعلومة التي لا نعرفها، وما الذي نحتاج إلى التحقق منه قبل الحكم؟"),
            Triple("القاضي", Icons.Default.Psychology, "${selected.second}\n\n${selected.third}")
        )
        roles.forEach { (role, icon, content) ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
                    Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(role, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(content, color = MutedText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun EchoScreen(userState: UserState) {
    val answered = QuestionnaireEngine.answeredOptions(userState.answers)
    val byQuestionId = answered.associateBy { it.first.id }
    val chronological = userState.answerTimestamps.entries.sortedBy { it.value }.mapNotNull { byQuestionId[it.key] }
    val history = chronological.ifEmpty { answered }
    val first = history.firstOrNull()
    val last = history.lastOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("أحيانًا لا نفهم إجابةً قديمة إلا حين نسمع إجابةً جديدة. هنا يضع التطبيق أثرين جنبًا إلى جنب، ويترك لك تفسير المسافة بينهما.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(18.dp))
        if (first == null || last == null || history.size < 2) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Violet, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("ما زال الصدى يتكوّن", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("أجب عن مزيد من الأسئلة، ثم عد لتسمع كيف تتجاور اختياراتك عبر الرحلة.", color = MutedText, textAlign = TextAlign.Center)
                }
            }
        } else {
            EchoAnswerCard("الأثر الأول", first.first.heading, first.first.prompt, first.second.title, first.second.detail)
            Box(Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(Violet.copy(alpha = 0.1f), Violet, Violet.copy(alpha = 0.1f)))))
            }
            EchoAnswerCard("الأثر الأحدث في مسارك", last.first.heading, last.first.prompt, last.second.title, last.second.detail)
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                    Text("السؤال الذي يبقى", color = Violet, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(7.dp))
                    Text(QuestionnaireEngine.echoReflection(userState.answers), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(7.dp))
                    Text("تعتمد المقارنة على المسار الحالي وأوقات إجاباتك المحفوظة. لا يفترض الصدى أنك تغيّرت؛ إنه يمنحك مساحةً لفحص ما تغيّر، وما بقي كما هو.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EchoAnswerCard(label: String, heading: String, prompt: String, answer: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Text(label, color = Violet, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(heading, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(5.dp))
            Text(prompt, color = MutedText, style = MaterialTheme.typography.bodySmall)
            Divider(Modifier.padding(vertical = 11.dp), color = Line)
            Text(answer, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(detail, color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SilentRoomScreen(savedWriting: String, savedFaceId: String, onSave: (String, String) -> Unit) {
    var writing by rememberSaveable(savedWriting) { mutableStateOf(savedWriting) }
    var linkedFaceId by rememberSaveable(savedFaceId) { mutableStateOf(savedFaceId) }
    var faceMenuExpanded by remember { mutableStateOf(false) }
    val faceOptions = listOf("" to "لا أربطها بوجه") + QuestionnaireEngine.faces.map { it.id to it.title }
    val selectedFaceLabel = faceOptions.firstOrNull { it.first == linkedFaceId }?.second ?: "لا أربطها بوجه"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("هذه الغرفة لا تطلب منك أن تبدو متماسكًا. اكتب كما أنت، ثم قرر بنفسك ما الذي تريد الاحتفاظ به.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("ما الذي لم تجد له اسمًا بعد؟", color = Violet, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = writing,
                    onValueChange = { writing = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                    placeholder = { Text("اكتب دون أن تبحث عن جملةٍ جميلة…") },
                    minLines = 8,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(17.dp))
                Text("هل تريد ربط هذه الكتابة بوجهٍ من شجرتك؟", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Box {
                    OutlinedButton(onClick = { faceMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedFaceLabel, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Violet)
                    }
                    DropdownMenu(expanded = faceMenuExpanded, onDismissRequest = { faceMenuExpanded = false }) {
                        faceOptions.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { linkedFaceId = id; faceMenuExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(13.dp))
                Button(onClick = { onSave(writing, linkedFaceId) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("احفظ ما اخترت الاحتفاظ به") }
                Spacer(Modifier.height(8.dp))
                Text("تبقى كتابتك على هذا الجهاز في هذه النسخة. لن تُحوّل تلقائيًا إلى تحليلٍ لشخصيتك.", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ClockScreen(elapsedMillis: Long) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(260.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF39254F), Color(0xFF1C1428), Backdrop))), contentAlignment = Alignment.Center) {
            TimerDial(elapsedMillis, Modifier.size(225.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الزمن الذي شهد مواجهتك", color = Violet, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(formatElapsedTime(elapsedMillis), style = MaterialTheme.typography.headlineMedium, color = Color(0xFFF1E8FF))
                Text("ساعة : دقيقة : ثانية", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(25.dp))
        Text("كل ثانيةٍ قضيتها هنا، كانت شاهدًا على شيءٍ فيك.", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE4D8FF), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("يجمع هذا العداد وقت استخدام المحاكمة في جلساتك المختلفة. يتوقف عندما تغادر التطبيق، ثم يستأنف من حيث انتهى؛ لا يعيد كتابة تاريخك من الصفر.", color = MutedText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ProfileScreen(userState: UserState) {
    val answered = QuestionnaireEngine.answeredOptions(userState.answers)
    val tags = QuestionnaireEngine.tagCounts(userState.answers).entries.sortedByDescending { it.value }.take(5)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF61468B), Color(0xFF241932)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFFE1D5FF), modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(userState.userName, style = MaterialTheme.typography.headlineSmall)
                Text("هويةٌ تتطور، لا وصفٌ يُغلق عليك", color = MutedText, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(17.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${answered.size}", style = MaterialTheme.typography.titleLarge, color = Violet); Text("إجابات", color = MutedText, style = MaterialTheme.typography.bodySmall) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${QuestionnaireEngine.faces.count { QuestionnaireEngine.faceScore(it, userState.answers) > 0 }}", style = MaterialTheme.typography.titleLarge, color = Violet); Text("وجوه لها أدلة", color = MutedText, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("خيوط ظهرت في إجاباتك", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (tags.isEmpty()) {
            Text("ما زالت الصفحة تنتظر أول الأدلة. ابدأ بجلسة الأسئلة، ودع صورتك تتشكل عبر الاختيارات.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        } else tags.forEach { (tag, count) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tag.replace('_', ' '), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                repeat(count.coerceAtMost(5)) { Box(Modifier.padding(start = 4.dp).size(7.dp).clip(CircleShape).background(Violet)) }
            }
        }
        Spacer(Modifier.height(17.dp))
        Text("هذه مؤشرات أولية مستخرجة من اختيارك، وليست قياسًا نفسيًا معتمدًا أو تشخيصًا. الأدلة المضادة والسياق جزءٌ من الصورة أيضًا.", color = MutedText, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsScreen(userState: UserState, onSaveName: (String) -> Unit, onToggleMuted: (Boolean) -> Unit) {
    var name by rememberSaveable(userState.userName) { mutableStateOf(userState.userName) }
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("إعداداتك لا تغيّر ما اكتشفته؛ إنها تضبط الطريقة التي ترافقك بها المحاكمة.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Violet)
                    Spacer(Modifier.width(10.dp))
                    Text("هوية المستخدم", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it.take(32) }, modifier = Modifier.fillMaxWidth(), label = { Text("الاسم الذي يرافق رحلتك") }, singleLine = true, shape = RoundedCornerShape(15.dp))
                Spacer(Modifier.height(10.dp))
                Button(onClick = { if (name.isNotBlank()) onSaveName(name.trim()) }, modifier = Modifier.fillMaxWidth()) { Text("حفظ الاسم") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(PanelRaised), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, contentDescription = null, tint = Violet) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("الموسيقى الخلفية", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(3.dp))
                    Text(if (userState.ambientMuted) "الصمت حاضر حتى تختار عودة الموسيقى." else "لحن المحاكمة يرافقك من خلف الكلمات.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = !userState.ambientMuted, onCheckedChange = { enabled -> onToggleMuted(!enabled) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(PanelRaised), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Violet)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("حماية النصوص", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(3.dp))
                    Text("خيارات النسخ والقص وتحديد الكل معطّلة في واجهة التطبيق، لتبقى كلمات المحاكمة داخل تجربتك.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri("https://t.me/ro_gy") }) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF332345)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Violet, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("المطوّر", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Text("Aram Aoyal", style = MaterialTheme.typography.titleMedium)
                    Text("t.me/ro_gy", color = Violet, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Violet)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text("صُممت المحاكمة للاستكشاف والتأمل، لا للتشخيص النفسي. يمكنك مراجعة قراءاتك أو رفضها؛ فالسياق جزءٌ من الحقيقة.", color = MutedText, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * A continuously animated atmospheric layer shared by all normal app screens.
 * It is intentionally subtle: the motion lives at the edges and behind content,
 * so text stays readable and the canvas never intercepts touch input.
 */
@Composable
fun CinematicAtmosphere(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "global-cinematic-atmosphere")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "atmosphere-drift"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.76f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "atmosphere-breathe"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val extent = size.maxDimension
        val topCenter = Offset(w * (0.18f + 0.06f * sin(drift * 2f * PI.toFloat())), h * 0.17f)
        val lowCenter = Offset(w * (0.88f - 0.08f * drift), h * 0.82f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B62D0).copy(alpha = 0.11f * intensity * breathe), Color(0xFF63458A).copy(alpha = 0.035f), Color.Transparent),
                center = topCenter,
                radius = extent * 0.52f
            ),
            radius = extent * 0.52f,
            center = topCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7B51AF).copy(alpha = 0.085f * intensity * breathe), Color(0xFF3B2859).copy(alpha = 0.025f), Color.Transparent),
                center = lowCenter,
                radius = extent * 0.44f
            ),
            radius = extent * 0.44f,
            center = lowCenter
        )
        // Slow, sparse dust motes: fixed seeds with smoothly moving positions.
        for (i in 0 until 19) {
            val seedX = ((i * 0.6180339f) % 1f)
            val seedY = ((i * 0.381966f + 0.11f) % 1f)
            val direction = if (i % 2 == 0) 1f else -1f
            val x = ((seedX + drift * 0.09f * direction + 1f) % 1f) * w
            val y = ((seedY + drift * (0.06f + (i % 4) * 0.018f)) % 1f) * h
            val glow = (0.025f + 0.022f * abs(sin(drift * 2f * PI.toFloat() + i))) * intensity
            drawCircle(
                color = Color(0xFFD5C4FF).copy(alpha = glow.coerceIn(0f, 0.075f)),
                radius = (0.9f + (i % 3) * 0.35f).dp.toPx(),
                center = Offset(x, y)
            )
        }
        // Vignette creates a quiet cinematic edge without dimming the center text.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x12000000)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = extent * 0.79f
            )
        )
    }
}

@Composable
fun SpiritOverlay(
    message: String,
    stage: Int,
    speechFinished: Boolean,
    onAsk: () -> Unit,
    onReply: (String) -> Unit,
    onClose: () -> Unit
) {
    val motion = rememberInfiniteTransition(label = "spirit-character-motion")
    val breathing by motion.animateFloat(
        initialValue = 0.965f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(2700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spirit-breathing"
    )
    val floatY by motion.animateFloat(
        initialValue = -7f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(5100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spirit-floating"
    )
    val tilt by motion.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(7800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spirit-tilt"
    )
    val mistPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "spirit-mist"
    )
    val haloPulse by motion.animateFloat(
        initialValue = 0.68f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spirit-halo-pulse"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(680)) + slideInVertically(animationSpec = tween(680, easing = FastOutSlowInEasing), initialOffsetY = { it / 18 })
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xF408060D)), contentAlignment = Alignment.Center) {
            // Layered, slow-moving fog and particles fill the whole encounter scene.
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val extent = size.maxDimension
                val phase = mistPhase * 2f * PI.toFloat()
                val fogOne = Offset(w * (0.5f + 0.13f * sin(phase)), h * (0.30f + 0.055f * cos(phase * 0.75f)))
                val fogTwo = Offset(w * (0.5f - 0.16f * cos(phase * 0.8f)), h * (0.64f + 0.05f * sin(phase * 0.65f)))
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x624A2E76), Color(0x2D332044), Color.Transparent), center = fogOne, radius = extent * 0.58f),
                    radius = extent * 0.58f,
                    center = fogOne
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x354D327B), Color(0x18302340), Color.Transparent), center = fogTwo, radius = extent * 0.48f),
                    radius = extent * 0.48f,
                    center = fogTwo
                )
                for (i in 0 until 36) {
                    val seed = i * 0.6180339f
                    val x = ((seed + mistPhase * (if (i % 2 == 0) 0.075f else -0.055f) + 1f) % 1f) * w
                    val y = ((i * 0.317f + mistPhase * (0.11f + (i % 3) * 0.025f)) % 1f) * h
                    val alpha = (0.025f + 0.045f * abs(sin(phase + i * 0.8f))).coerceIn(0f, 0.07f)
                    drawCircle(Color(0xFFD3BFFF).copy(alpha = alpha), radius = (0.8f + (i % 3) * 0.4f).dp.toPx(), center = Offset(x, y))
                }
                // A slow halo and narrow light shafts give the apparition depth without flashing.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x449D72ED).copy(alpha = 0.52f * haloPulse), Color(0x183D2A55), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.32f),
                        radius = extent * (0.32f + 0.04f * haloPulse)
                    ),
                    radius = extent * (0.32f + 0.04f * haloPulse),
                    center = Offset(w * 0.5f, h * 0.32f)
                )
                for (i in 0 until 7) {
                    val x = w * (0.12f + i * 0.127f)
                    val drift = sin(phase * 0.45f + i * 0.71f) * w * 0.025f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0xFFBBA2FF).copy(alpha = 0.025f + 0.014f * haloPulse), Color.Transparent),
                            start = Offset(x + drift, h * 0.12f),
                            end = Offset(x - drift, h * 0.78f)
                        ),
                        start = Offset(x + drift, h * 0.12f),
                        end = Offset(x - drift, h * 0.78f),
                        strokeWidth = (1.2f + (i % 3) * 0.55f).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(11.dp))

                // The apparition breathes, drifts, tilts, and changes its eye-light instead of remaining a static icon.
                Box(
                    Modifier
                        .size(240.dp)
                        .graphicsLayer {
                            translationY = floatY
                            scaleX = breathing
                            scaleY = breathing
                            rotationZ = tilt
                        }
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0x493C285D), Color(0x25201731), Color.Transparent)))
                        .clickable(enabled = stage == 0) { onAsk() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w * 0.5f, h * 0.47f)
                        val extent = size.minDimension

                        // An abstract, veiled presence: light and silhouette rather than cartoon eyes or a moving mouth.
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0x559C7BDF), Color(0x273F2D5E), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.43f),
                                radius = extent * 0.54f
                            ),
                            radius = extent * 0.54f,
                            center = Offset(w * 0.5f, h * 0.43f)
                        )
                        drawCircle(
                            color = Color(0x405A437D),
                            radius = extent * 0.405f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0x2DAD92EF),
                            radius = extent * 0.345f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        rotate(degrees = mistPhase * 360f, pivot = center) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0xC2C2ADFF), Color.Transparent)),
                                startAngle = 198f,
                                sweepAngle = 112f,
                                useCenter = false,
                                topLeft = Offset(center.x - extent * 0.42f, center.y - extent * 0.42f),
                                size = Size(extent * 0.84f, extent * 0.84f),
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        // A soft cloak-like silhouette dissolves into the mist at the shoulders.
                        val silhouette = PathBuilder(w, h).make()
                        drawPath(
                            silhouette,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xCA45335E), Color(0xA21B1427), Color(0x0009070D)),
                                startY = h * 0.18f,
                                endY = h * 0.9f
                            )
                        )
                        drawArc(
                            color = Color(0x889F82D5),
                            startAngle = 194f,
                            sweepAngle = 152f,
                            useCenter = false,
                            topLeft = Offset(w * 0.28f, h * 0.07f),
                            size = Size(w * 0.44f, h * 0.43f),
                            style = Stroke(width = 1.25.dp.toPx())
                        )
                        // A narrow vertical glimmer suggests a hidden presence without facial animation.
                        drawLine(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xB9D8CBFF), Color(0x559C7BDF), Color.Transparent),
                                startY = h * 0.24f,
                                endY = h * 0.52f
                            ),
                            start = Offset(w * 0.5f, h * 0.25f),
                            end = Offset(w * 0.5f, h * 0.5f),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xD8E5DBFF), Color(0x559F82D5), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.34f),
                                radius = extent * 0.085f
                            ),
                            radius = extent * 0.085f,
                            center = Offset(w * 0.5f, h * 0.34f)
                        )
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFB9A1FF).copy(alpha = 0.8f), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).size(21.dp))
                }

                Spacer(Modifier.height(8.dp))
                Text("طيف الروح", color = Violet, style = MaterialTheme.typography.bodySmall, letterSpacing = 2.2.sp)
                Spacer(Modifier.height(13.dp))

                AnimatedContent(
                    targetState = message,
                    transitionSpec = {
                        (fadeIn(tween(430)) + slideInVertically(tween(430, easing = FastOutSlowInEasing)) { it / 15 }) togetherWith fadeOut(tween(210))
                    },
                    label = "spirit-dialogue-transition"
                ) { visibleMessage ->
                    Text(
                        visibleMessage,
                        color = Color(0xFFF0E8FF),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (speechFinished) Color(0xFF8B7AA5) else Violet))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (speechFinished) "سكت الطيف، وبقي السؤال" else "الطيف يتحدث إليك الآن",
                        color = Color(0xFFB9A9CA),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Canvas(Modifier.fillMaxWidth(0.46f).height(28.dp).padding(top = 3.dp)) {
                    val count = 25
                    val barWidth = size.width / (count * 2f)
                    for (i in 0 until count) {
                        val wave = abs(sin(i * 0.54f + System.nanoTime() / 1_000_000_000f))
                        val speakingScale = if (speechFinished) 0.22f else 0.35f + wave * 0.65f
                        val barHeight = (3f + 18f * wave * speakingScale).dp.toPx()
                        val x = i * (barWidth * 2f) + barWidth / 2f
                        drawLine(
                            color = Color(0xFFB9A1FF).copy(alpha = if (speechFinished) 0.28f else 0.36f + wave * 0.5f),
                            start = Offset(x, size.height / 2f - barHeight / 2f),
                            end = Offset(x, size.height / 2f + barHeight / 2f),
                            strokeWidth = barWidth.coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("هذه قراءة محتملة من إجاباتك، وليست حقيقةً نهائية عنك.", color = Color(0xFF9F93B0), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                if (stage == 0) {
                    Spacer(Modifier.height(21.dp))
                    Button(onClick = onAsk, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("لديك سؤالٌ آخر؟") }
                    Spacer(Modifier.height(3.dp))
                    Text("يمكنك لمس الطيف أيضًا لفتح حوار قصير.", color = Color(0xFF81748F), style = MaterialTheme.typography.labelSmall)
                } else if (stage == 1) {
                    Spacer(Modifier.height(21.dp))
                    Text("كيف استقبلت هذه الملاحظة؟", color = Color(0xFFD5C8E5), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    listOf("قريبة مما أشعر به", "الصورة ليست دقيقة", "لا أملك إجابةً الآن").forEach { reply ->
                        Button(
                            onClick = { onReply(reply) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = Color(0xFFF0E8FF))
                        ) { Text(reply) }
                    }
                } else {
                    Spacer(Modifier.height(21.dp))
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("أعود إلى الجلسة") }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onClose) { Text("دعني أعود إلى نفسي", color = Color(0xFFBBA7D8)) }
            }
        }
    }
}

private class PathBuilder(private val width: Float, private val height: Float) {
    fun make(): androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path().apply {
        moveTo(width * 0.5f, height * 0.15f)
        cubicTo(width * 0.34f, height * 0.16f, width * 0.31f, height * 0.28f, width * 0.34f, height * 0.37f)
        cubicTo(width * 0.25f, height * 0.44f, width * 0.24f, height * 0.55f, width * 0.24f, height * 0.69f)
        cubicTo(width * 0.27f, height * 0.81f, width * 0.34f, height * 0.89f, width * 0.5f, height * 0.9f)
        cubicTo(width * 0.66f, height * 0.89f, width * 0.73f, height * 0.81f, width * 0.76f, height * 0.69f)
        cubicTo(width * 0.76f, height * 0.55f, width * 0.75f, height * 0.44f, width * 0.66f, height * 0.37f)
        cubicTo(width * 0.69f, height * 0.28f, width * 0.66f, height * 0.16f, width * 0.5f, height * 0.15f)
        close()
    }
}
