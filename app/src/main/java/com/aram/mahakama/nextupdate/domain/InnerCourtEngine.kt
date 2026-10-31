package com.aram.mahakama.nextupdate.domain

import com.aram.mahakama.nextupdate.model.CourtArgument
import com.aram.mahakama.nextupdate.model.CourtCase
import com.aram.mahakama.nextupdate.model.CourtVerdict
import java.util.UUID

/** Builds a reflective case from current evidence; it never assigns guilt or diagnoses. */
object InnerCourtEngine {
    fun buildCase(answers: Map<String, String>): CourtCase {
        val analysis = ActiveTrialAnalysis.analyze(answers)
        val graph = FaceGraphEngine.build(answers)
        val strongestTension = analysis.tensions.maxByOrNull { it.evidenceQuestionIds.size }
        val latest = analysis.activeAnswers.lastOrNull()

        val caseId: String
        val title: String
        val context: String
        val question: String
        val evidenceSummary: String
        val evidenceIds: List<String>
        val reviewQuestion: String

        if (strongestTension != null) {
            caseId = strongestTension.id
            title = strongestTension.title
            context = strongestTension.interpretation
            question = strongestTension.reviewQuestion
            evidenceSummary = analysis.activeAnswers
                .filter { it.questionId in strongestTension.evidenceQuestionIds }
                .takeLast(4)
                .joinToString("\n") { "${it.heading}: ${it.answer}" }
                .ifBlank { "توجد إشارات متقاطعة في المسار الحالي، لكنها لا تكفي وحدها لحسم القضية." }
            evidenceIds = strongestTension.evidenceQuestionIds.toList()
            reviewQuestion = "ما الموقف الواقعي الذي قد يوضح متى يظهر كل اتجاه؟"
        } else {
            val leadingFace = graph.nodes.firstOrNull { it.score > 0 }
            caseId = leadingFace?.let { "face_${it.id}" } ?: "open_question"
            title = leadingFace?.let { "قضية ${it.title}" } ?: "قضية لم تتضح أدلتها بعد"
            context = leadingFace?.contextNote
                ?: "لم تتجمع بعد إجابات كافية لبناء قضية شخصية. يمكن استخدام الأسئلة العامة دون افتراضات عنك."
            question = leadingFace?.unresolvedQuestion
                ?: "ما الأمر الذي تودّ فحصه الآن دون استعجال الوصول إلى حكم؟"
            evidenceSummary = latest?.let { "${it.heading}: ${it.answer} — ${it.detail}" }
                ?: "لا توجد إجابات نشطة بعد. ابدأ من سؤالٍ واحد يهمك، أو اترك القضية مفتوحة."
            evidenceIds = latest?.let { listOf(it.questionId) }.orEmpty()
            reviewQuestion = "ما المعلومة أو التجربة التي يمكن أن تغيّر موقفك؟"
        }

        return CourtCase(
            id = caseId,
            title = title,
            context = context,
            centralQuestion = question,
            evidenceSummary = evidenceSummary,
            evidenceQuestionIds = evidenceIds,
            arguments = listOf(
                CourtArgument(
                    role = "المُدّعي",
                    question = "ما الأثر أو المسؤولية التي لا ينبغي تجاهلها؟",
                    purpose = "يذكّر بأثر الفعل ونتائجه دون اختزال الشخص في خطئه."
                ),
                CourtArgument(
                    role = "المُدافع",
                    question = "ما الظروف أو الاحتياجات التي تساعد على فهم القرار؟",
                    purpose = "يفحص السياق من دون أن يحوّل الفهم إلى تبرير تلقائي."
                ),
                CourtArgument(
                    role = "الشاهد",
                    question = "ما الذي نعرفه فعلًا، وما الذي ما زال افتراضًا؟",
                    purpose = "يفصل بين الدليل والتأويل، ويترك مساحة للمعلومات الناقصة."
                ),
                CourtArgument(
                    role = "القاضي",
                    question = question,
                    purpose = "يوازن الحجج ويذكّر بأن الموقف الحالي قابل للمراجعة."
                )
            ),
            reviewQuestion = reviewQuestion
        )
    }

    fun createVerdict(
        courtCase: CourtCase,
        position: String,
        evidenceFocus: String,
        confidencePercent: Int,
        whatCouldChangeMyMind: String,
        atMillis: Long = System.currentTimeMillis()
    ): CourtVerdict {
        require(position.isNotBlank()) { "يجب اختيار موقف قبل حفظ المحضر." }
        require(evidenceFocus.isNotBlank()) { "يجب تحديد نوع الدليل الأهم." }
        val confidence = confidencePercent.coerceIn(0, 100)
        val reflection = when {
            position.contains("الأدلة لا تكفي") -> "اخترت تعليق الحكم بدل ملء الفراغ بتخمين. هذه ليست نهاية القضية؛ إنها اعترافٌ بحدود ما نعرفه الآن."
            position.contains("الظروف") -> "منحت السياق وزنًا في قراءتك. يبقى مهمًا أن نفهم الظروف من دون محو أثر الاختيار أو مسؤولية صاحبه."
            else -> "منحت أثر الفعل ومسؤوليته الأولوية في موقفك الحالي. يبقى فحص السياق ضروريًا كي لا يتحول العدل إلى اختزال."
        }
        return CourtVerdict(
            id = UUID.randomUUID().toString(),
            caseId = courtCase.id,
            atMillis = atMillis,
            position = position,
            evidenceFocus = evidenceFocus,
            confidencePercent = confidence,
            whatCouldChangeMyMind = whatCouldChangeMyMind.trim(),
            reflection = reflection,
            evidenceQuestionIds = courtCase.evidenceQuestionIds
        )
    }
}
