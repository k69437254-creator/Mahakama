package com.aram.mahakama.nextupdate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.mahakama.data.UserState
import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.nextupdate.data.NextUpdateEventCodec
import com.aram.mahakama.nextupdate.domain.FaceGraphEngine
import com.aram.mahakama.nextupdate.domain.InnerCourtEngine
import com.aram.mahakama.nextupdate.domain.TrialEvolutionEngine
import com.aram.mahakama.nextupdate.model.CourtVerdict
import com.aram.mahakama.nextupdate.model.FaceGraphNode
import com.aram.mahakama.nextupdate.model.SpiritEvidenceReflection
import java.text.DateFormat
import java.util.Date

private val NextBackdrop = Color(0xFF0C0A12)
private val NextPanel = Color(0xFF17121F)
private val NextRaised = Color(0xFF21192D)
private val NextViolet = Color(0xFFB9A1FF)
private val NextMuted = Color(0xFFB4A9C2)
private val NextLine = Color(0xFF493B58)

/** Interactive network view. The graph is navigable; evidence remains visible below it. */
@Composable
fun FaceTreeScreen(
    userState: UserState,
    onOpenEvolution: () -> Unit
) {
    val graph = remember(userState.answers) { FaceGraphEngine.build(userState.answers) }
    var selectedFaceId by rememberSaveable { mutableStateOf(graph.nodes.firstOrNull { it.score > 0 }?.id ?: "guardian") }
    val selected = graph.node(selectedFaceId) ?: graph.nodes.firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = NextRaised), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(graph.rootTitle, color = NextViolet, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(5.dp))
                Text(graph.rootDescription, color = NextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(9.dp))
                Text("${graph.activeAnswerCount} إجابة في المسار النشط · ${graph.nodes.count { it.score > 0 }} وجوه تلقت إشارات", color = NextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }

        Text("شبكة الوجوه المتصلة", color = NextViolet, style = MaterialTheme.typography.titleMedium)
        Text("اختر وجهًا لعرض أدلته وصلاته. الخطوط تعني علاقةً تأملية، لا علاقة سببية أو تشخيصية.", color = NextMuted, style = MaterialTheme.typography.bodySmall)

        val positions = mapOf(
            "guardian" to (0.18f to 0.17f),
            "seeker" to (0.50f to 0.15f),
            "critic" to (0.82f to 0.17f),
            "keeper" to (0.18f to 0.53f),
            "mirror" to (0.50f to 0.47f),
            "challenger" to (0.82f to 0.53f),
            "wanderer" to (0.50f to 0.83f)
        )
        BoxWithConstraints(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(22.dp)).background(NextBackdrop)) {
            Canvas(Modifier.fillMaxSize()) {
                graph.edges.forEach { edge ->
                    val start = positions[edge.fromId] ?: return@forEach
                    val end = positions[edge.toId] ?: return@forEach
                    drawLine(
                        color = if (edge.fromId == selectedFaceId || edge.toId == selectedFaceId) NextViolet.copy(alpha = 0.72f) else NextLine.copy(alpha = 0.65f),
                        start = Offset(size.width * start.first, size.height * start.second),
                        end = Offset(size.width * end.first, size.height * end.second),
                        strokeWidth = if (edge.fromId == selectedFaceId || edge.toId == selectedFaceId) 2.4.dp.toPx() else 1.25.dp.toPx()
                    )
                }
                drawCircle(color = NextViolet.copy(alpha = 0.08f), radius = 84.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.48f))
            }
            graph.nodes.forEach { node ->
                val point = positions[node.id] ?: (0.5f to 0.5f)
                val chosen = node.id == selectedFaceId
                Surface(
                    modifier = Modifier.offset(
                        x = maxWidth * point.first - 47.dp,
                        y = maxHeight * point.second - 31.dp
                    ).width(94.dp).height(62.dp).clickable { selectedFaceId = node.id },
                    shape = RoundedCornerShape(15.dp),
                    color = if (chosen) Color(0xFF392A50) else NextPanel,
                    border = androidx.compose.foundation.BorderStroke(if (chosen) 1.6.dp else 0.8.dp, if (chosen) NextViolet else NextLine)
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(node.title, color = if (chosen) Color.White else NextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        Text(if (node.score > 0) "${node.score} إشارة" else "بلا دليل كافٍ", color = if (chosen) NextViolet else NextMuted, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }

        selected?.let { FaceEvidenceDetail(node = it, onSelectRelated = { selectedFaceId = it }) }

        OutlinedButton(onClick = onOpenEvolution, modifier = Modifier.fillMaxWidth()) {
            Text("افتح سجل تطور المحاكمة")
        }
        Text("لا تمثل النقاط أو الأرقام نسبًا لاحتمال صفةٍ أو تشخيصًا؛ إنها تلخيصٌ لتقاطع إجابات المسار الحالي مع سماتٍ مكتوبة مسبقًا.", color = NextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FaceEvidenceDetail(node: FaceGraphNode, onSelectRelated: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NextPanel), shape = RoundedCornerShape(21.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(node.title, color = NextViolet, style = MaterialTheme.typography.titleLarge)
            Text(node.subtitle, color = NextMuted, style = MaterialTheme.typography.labelLarge)
            Text(node.description, color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
            Text("حضور الأدلة: ${node.signalLabel}", color = NextViolet, style = MaterialTheme.typography.labelLarge)
            HorizontalDivider(color = NextLine)
            Text("الأدلة المؤيدة", color = NextViolet, style = MaterialTheme.typography.titleSmall)
            if (node.supportingEvidence.isEmpty()) {
                Text("لم تتجمع أدلة كافية لهذا الوجه في المسار النشط.", color = NextMuted, style = MaterialTheme.typography.bodySmall)
            } else node.supportingEvidence.forEach { evidence ->
                EvidenceLine(evidence.heading, evidence.answer, evidence.detail)
            }
            Text("أدلة قد تعارض القراءة", color = NextViolet, style = MaterialTheme.typography.titleSmall)
            if (node.counterEvidence.isEmpty()) {
                Text("لا تظهر حاليًا أدلة مضادة محددة. غيابها لا يثبت صحة القراءة.", color = NextMuted, style = MaterialTheme.typography.bodySmall)
            } else node.counterEvidence.forEach { evidence ->
                EvidenceLine(evidence.heading, evidence.answer, evidence.detail)
            }
            HorizontalDivider(color = NextLine)
            Text("السياق المحتمل: ${node.contextLabel}", color = NextViolet, style = MaterialTheme.typography.labelLarge)
            Text(node.contextNote, color = NextMuted, style = MaterialTheme.typography.bodySmall)
            Text("سؤال لم يُحسم", color = NextViolet, style = MaterialTheme.typography.titleSmall)
            Text(node.unresolvedQuestion, color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
            Text("وجوه مرتبطة", color = NextViolet, style = MaterialTheme.typography.titleSmall)
            if (node.relatedFaceIds.isEmpty()) Text("لا توجد روابط متاحة.", color = NextMuted, style = MaterialTheme.typography.bodySmall)
            else Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                node.relatedFaceIds.take(3).forEach { id ->
                    val label = when (id) {
                        "guardian" -> "الحارس"
                        "seeker" -> "الباحث"
                        "mirror" -> "المرآة"
                        "keeper" -> "حارس الانتماء"
                        "challenger" -> "المقاوم"
                        "critic" -> "القاضي الداخلي"
                        "wanderer" -> "المنسحب"
                        else -> id
                    }
                    OutlinedButton(onClick = { onSelectRelated(id) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
                        Text(label, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceLine(heading: String, answer: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(heading, color = Color(0xFFD9C8F3), style = MaterialTheme.typography.labelMedium)
        Text(answer, color = Color.White, style = MaterialTheme.typography.bodySmall)
        Text(detail, color = NextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Historical view distinguishes current-path answers from replaced/abandoned answers. */
@Composable
fun TrialEvolutionScreen(userState: UserState) {
    val records = remember(userState.spiritMemoryJson) { NextUpdateEventCodec.parseRecords(userState.spiritMemoryJson) }
    val activeAnswers = remember(userState.answers) { QuestionnaireEngine.activeAnswers(userState.answers) }
    val timeline = remember(records, activeAnswers) { TrialEvolutionEngine.timeline(records, activeAnswers) }
    val current = remember(userState.answers) { TrialEvolutionEngine.snapshot(userState.answers) }
    val previous = remember(userState.spiritMemoryJson) { NextUpdateEventCodec.latestProfileSnapshot(userState.spiritMemoryJson) }
    val change = remember(previous, current) { TrialEvolutionEngine.compare(previous, current) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = NextRaised), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("سجل تطور المحاكمة", color = NextViolet, style = MaterialTheme.typography.titleLarge)
                Text("هذا السجل لا يحاول تثبيت صورةٍ واحدة عنك؛ إنه يعرض ما تغيّر في الإجابات والأدلة بمرور الوقت.", color = NextMuted, style = MaterialTheme.typography.bodyMedium)
                Text("${current.activeAnswerCount} إجابة نشطة · ${timeline.size} حدثًا محفوظًا", color = NextViolet, style = MaterialTheme.typography.labelLarge)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = NextPanel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مقارنة المحطة الحالية بآخر لقطة محفوظة", color = NextViolet, style = MaterialTheme.typography.titleSmall)
                Text(TrialEvolutionEngine.evolutionSummary(change), color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
                Text("الوجوه الأبرز الآن: ${current.leadingFaceIds.joinToString("، ").ifBlank { "لم تظهر إشارات كافية" }}", color = NextMuted, style = MaterialTheme.typography.bodySmall)
                Text("التوترات المسجلة: ${current.tensionIds.size}", color = NextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("الأحداث والانعكاسات", color = NextViolet, style = MaterialTheme.typography.titleMedium)
        if (timeline.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = NextPanel), shape = RoundedCornerShape(18.dp)) {
                Text("لم يُسجل تاريخٌ كافٍ بعد. ستظهر هنا الإجابات والحوارات ومحاضر المحكمة ومحطات التطور مع تقدمك.", Modifier.padding(15.dp), color = NextMuted)
            }
        } else timeline.forEach { item ->
            Card(colors = CardDefaults.cardColors(containerColor = if (item.isCurrentAnswer) NextRaised else NextPanel), shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.title, color = NextViolet, style = MaterialTheme.typography.titleSmall)
                    Text(item.statusLabel, color = NextMuted, style = MaterialTheme.typography.labelSmall)
                    Text(item.body, color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodySmall)
                    Text(formatEventTime(item.atMillis), color = NextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("ملاحظة: السجل يعتمد على الأحداث التي حفظها التطبيق. لا تُستعاد تلقائيًا بيانات لم تكن مسجلة، وقد تظهر الإجابات القديمة بوصفها أحداثًا تاريخية لا جزءًا من المسار الحالي.", color = NextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Persist the returned JSON event with PreferencesRepository.appendSpiritMemoryEvent(). */
@Composable
fun EnhancedInnerCourtScreen(
    userState: UserState,
    onSaveRecord: (String) -> Unit
) {
    val courtCase = remember(userState.answers) { InnerCourtEngine.buildCase(userState.answers) }
    var selectedPosition by rememberSaveable(courtCase.id, userState.answers.hashCode()) { mutableStateOf("") }
    var selectedEvidence by rememberSaveable(courtCase.id, userState.answers.hashCode()) { mutableStateOf("") }
    var confidence by rememberSaveable(courtCase.id, userState.answers.hashCode()) { mutableStateOf(55f) }
    var changeMind by rememberSaveable(courtCase.id, userState.answers.hashCode()) { mutableStateOf("") }
    var savedVerdict by remember(courtCase.id, userState.answers.hashCode()) { mutableStateOf<CourtVerdict?>(null) }
    val positions = listOf("مسؤولية الفعل أولًا", "الظروف تخفف الحكم", "الأدلة لا تكفي بعد")
    val evidenceChoices = listOf("الأثر الفعلي للقرار", "النية والدافع", "الظروف والضغوط", "تكرار السلوك أو كونه استثناءً")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = NextRaised), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("المحكمة الداخلية", color = NextViolet, style = MaterialTheme.typography.titleLarge)
                Text(courtCase.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(courtCase.context, color = NextMuted, style = MaterialTheme.typography.bodyMedium)
                Text(courtCase.centralQuestion, color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = NextPanel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الأدلة من مسارك الحالي", color = NextViolet, style = MaterialTheme.typography.titleSmall)
                Text(courtCase.evidenceSummary, color = NextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("المرافعة", color = NextViolet, style = MaterialTheme.typography.titleMedium)
        courtCase.arguments.forEach { argument ->
            Card(colors = CardDefaults.cardColors(containerColor = NextPanel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(argument.role, color = NextViolet, style = MaterialTheme.typography.titleSmall)
                    Text(argument.question, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text(argument.purpose, color = NextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("أيّ موقفٍ تتبناه الآن؟", color = NextViolet, style = MaterialTheme.typography.titleMedium)
        positions.forEach { option ->
            OutlinedButton(
                onClick = { selectedPosition = option; savedVerdict = null },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectedPosition == option) Color.White else NextMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedPosition == option) NextViolet else NextLine)
            ) { Text(if (selectedPosition == option) "✓  $option" else option) }
        }

        Text("ما الدليل الذي ينبغي أن يحمل الوزن الأكبر؟", color = NextViolet, style = MaterialTheme.typography.titleSmall)
        evidenceChoices.forEach { option ->
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable { selectedEvidence = option; savedVerdict = null },
                color = if (selectedEvidence == option) NextRaised else NextPanel,
                shape = RoundedCornerShape(13.dp)
            ) {
                Text((if (selectedEvidence == option) "●  " else "○  ") + option, Modifier.padding(13.dp), color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("درجة ثقتك في هذا الموقف", color = NextMuted, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text("${confidence.toInt()}٪", color = NextViolet, style = MaterialTheme.typography.titleSmall)
        }
        Slider(value = confidence, onValueChange = { confidence = it; savedVerdict = null }, valueRange = 0f..100f)
        OutlinedTextField(
            value = changeMind,
            onValueChange = { changeMind = it; savedVerdict = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ما الذي قد يغيّر موقفك؟") },
            placeholder = { Text("معلومة أو تجربة قد تعيد فتح القضية") },
            minLines = 2
        )
        Button(
            onClick = {
                savedVerdict = InnerCourtEngine.createVerdict(
                    courtCase, selectedPosition, selectedEvidence, confidence.toInt(), changeMind
                )
            },
            enabled = selectedPosition.isNotBlank() && selectedEvidence.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("اعرض محضر موقفي") }

        savedVerdict?.let { verdict ->
            Card(colors = CardDefaults.cardColors(containerColor = NextRaised), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("محضر قابل للمراجعة", color = NextViolet, style = MaterialTheme.typography.titleMedium)
                    Text("موقفك: ${verdict.position}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("الدليل الأهم: ${verdict.evidenceFocus}", color = NextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("درجة الثقة: ${verdict.confidencePercent}٪", color = NextMuted, style = MaterialTheme.typography.bodySmall)
                    Text(verdict.reflection, color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
                    if (verdict.whatCouldChangeMyMind.isNotBlank()) Text("ما قد يغيّر الموقف: ${verdict.whatCouldChangeMyMind}", color = NextMuted, style = MaterialTheme.typography.bodySmall)
                    Text(courtCase.reviewQuestion, color = NextViolet, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onSaveRecord(NextUpdateEventCodec.encodeCourtVerdict(verdict, courtCase.title)); savedVerdict = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("احفظ المحضر في سجل الرحلة")
                    }
                }
            }
        }
        Text("هذه مرافعة تأملية وليست حكمًا أخلاقيًا أو تشخيصًا نفسيًا. يمكنك تغيير موقفك عندما تتغير الأدلة.", color = NextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SpiritEvidenceReflectionCard(reflection: SpiritEvidenceReflection) {
    Card(colors = CardDefaults.cardColors(containerColor = NextRaised), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("طيف الروح", color = NextViolet, style = MaterialTheme.typography.titleMedium)
            Text(reflection.line, color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
            Text("ما وراء الانعكاس", color = NextViolet, style = MaterialTheme.typography.labelLarge)
            Text(reflection.evidenceSummary, color = NextMuted, style = MaterialTheme.typography.bodySmall)
            Text("سؤال مفتوح: ${reflection.followUp}", color = Color(0xFFEAE3F2), style = MaterialTheme.typography.bodyMedium)
            Text(reflection.limitation, color = NextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatEventTime(atMillis: Long): String = if (atMillis <= 0L) "وقت غير مسجل" else
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(atMillis))
