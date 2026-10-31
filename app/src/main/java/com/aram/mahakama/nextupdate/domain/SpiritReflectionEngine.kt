package com.aram.mahakama.nextupdate.domain

import com.aram.mahakama.nextupdate.model.SpiritEvidenceReflection

/**
 * Deterministic, transparent reflection composer. It uses authored answers and never claims
 * to read hidden motives. Replace with a remote model only behind explicit consent and controls.
 */
object SpiritReflectionEngine {
    fun create(answers: Map<String, String>): SpiritEvidenceReflection {
        val analysis = ActiveTrialAnalysis.analyze(answers)
        val graph = FaceGraphEngine.build(answers)
        val latest = analysis.activeAnswers.lastOrNull()
        val strongest = graph.nodes.firstOrNull { it.score > 0 }
        val tension = analysis.tensions.maxByOrNull { it.evidenceQuestionIds.size }

        val key = tension?.id ?: strongest?.id?.let { "face_$it" } ?: "insufficient_evidence"
        val line = when {
            tension != null -> "ألاحظ تقاطعًا بين ${tension.title}. ظهر ذلك في ${tension.evidenceQuestionIds.size} مواضع من المسار الحالي؛ وقد يكون اختلافًا بين السياقات لا تناقضًا ثابتًا فيك."
            strongest != null && latest != null -> "في إجاباتك الحالية تظهر إشارات إلى ${strongest.title}، وآخرها في اختيارك ضمن «${latest.heading}». هذه قراءة أولية وليست وصفًا نهائيًا لك."
            else -> "لم تتجمع بعد أدلة كافية لأقدّم انعكاسًا مسؤولًا. يمكن أن نترك الصورة مفتوحة حتى تظهر إجابات أكثر."
        }
        val followUp = tension?.reviewQuestion
            ?: strongest?.unresolvedQuestion
            ?: "ما السؤال الذي يستحق أن نبدأ منه دون أن نستعجل تفسيرك؟"
        val evidence = when {
            tension != null -> "المواضع المرتبطة: ${tension.evidenceQuestionIds.joinToString("، ")}."
            latest != null -> "آخر دليل في المسار: ${latest.heading} — ${latest.answer}."
            else -> "لا توجد إجابات في المسار النشط حتى الآن."
        }
        return SpiritEvidenceReflection(
            key = key,
            line = line,
            followUp = followUp,
            evidenceSummary = evidence,
            limitation = "هذه ملاحظة استكشافية مبنية على الإجابات النشطة فقط؛ لا تشخّص ولا تدّعي معرفة النوايا، ويمكنك الاعتراض عليها أو تصحيحها."
        )
    }
}
