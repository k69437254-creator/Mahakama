package com.aram.mahakama.nextupdate.domain

import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.nextupdate.model.ActiveAnswerEvidence
import com.aram.mahakama.nextupdate.model.ActiveTrialAnalysisResult
import com.aram.mahakama.nextupdate.model.TensionRuleResult

/**
 * The single source of truth for current-session analysis.
 * All calculations are restricted to QuestionnaireEngine.activePath(); abandoned branches
 * are never silently mixed into the present profile.
 */
object ActiveTrialAnalysis {
    private data class Rule(
        val id: String,
        val left: Set<String>,
        val right: Set<String>,
        val title: String,
        val interpretation: String,
        val question: String
    )

    private val rules = listOf(
        Rule(
            id = "control_and_acceptance",
            left = setOf("control", "certainty"),
            right = setOf("acceptance", "flexibility"),
            title = "بين السيطرة والقبول",
            interpretation = "تظهر في المسار إشارات إلى الرغبة في ضبط ما يمكن ضبطه، وإلى ترك مساحة لما لا يقع تحت السيطرة. قد يتعايش الاتجاهان بحسب الموقف.",
            question = "في أيّ موقفٍ تمنحك السيطرة أمانًا، وفي أيّ موقفٍ يصبح القبول خيارًا أصدق؟"
        ),
        Rule(
            id = "fear_and_courage",
            left = setOf("fear", "avoidance"),
            right = setOf("courage", "agency", "change"),
            title = "بين الحذر والحركة",
            interpretation = "تجتمع إشارات إلى التراجع أو الخوف مع إشارات إلى المبادرة والتغيير. لا يلغي أحدهما الآخر؛ فقد يتبدل حضور كلٍّ منهما مع السياق.",
            question = "ما الذي يجعل التريث حمايةً نافعة، وما الذي يحوّله إلى تأجيلٍ يقيّدك؟"
        ),
        Rule(
            id = "belonging_and_authenticity",
            left = setOf("approval", "belonging", "attachment"),
            right = setOf("authenticity", "values", "boundaries"),
            title = "بين الانتماء والأصالة",
            interpretation = "تظهر أهمية العلاقة والانتماء إلى جانب إشارات إلى حماية القيم والحدود الشخصية. المسألة ليست اختيار طرفٍ دائم، بل فهم ثمن كل موقف.",
            question = "متى يكون التنازل تعبيرًا عن محبة، ومتى يجعلك تتخلى عن أمرٍ لا تريد خسارته؟"
        ),
        Rule(
            id = "self_criticism_and_compassion",
            left = setOf("self_criticism", "perfectionism", "shame", "judgment"),
            right = setOf("self_compassion", "acceptance", "resilience"),
            title = "بين المحاسبة والرحمة",
            interpretation = "توجد إشارات إلى معايير مرتفعة أو لوم الذات، وإشارات أخرى إلى التقبل والرحمة. يمكن للمسؤولية أن تبقى حاضرة دون أن تتحول إلى عقابٍ دائم.",
            question = "كيف تميّز بين مراجعة خطئك وبين تحويل الخطأ إلى حكمٍ شامل على نفسك؟"
        ),
        Rule(
            id = "reflection_and_action",
            left = setOf("reflection", "certainty", "nuance"),
            right = setOf("agency", "persistence", "action", "change"),
            title = "بين الفهم والفعل",
            interpretation = "تتقاطع الرغبة في الفهم والتأكد مع إشارات إلى الفعل والاستمرار. قد يساعد التفكير القرار، وقد يحتاج أحيانًا إلى حدٍّ يتوقف عنده ليبدأ الاختيار.",
            question = "ما المعلومة التي تحتاجها فعلًا قبل أن تتحرك، وما المعلومة التي تنتظرها لأن القرار صعب؟"
        ),
        Rule(
            id = "closeness_and_boundaries",
            left = setOf("attachment", "belonging", "empathy"),
            right = setOf("boundaries", "agency", "self_trust"),
            title = "بين القرب والحدود",
            interpretation = "تظهر أهمية القرب من الآخرين إلى جانب إشارات إلى الاستقلال وحماية الحدود. لا يجعل القرب الحدود قسوة، ولا تجعل الحدود القرب مستحيلًا.",
            question = "كيف تحافظ على علاقةٍ مهمة دون أن تختفي احتياجاتك داخلها؟"
        )
    )

    fun analyze(answers: Map<String, String>): ActiveTrialAnalysisResult {
        val evidence = QuestionnaireEngine.activePath(answers).map { (question, option) ->
            ActiveAnswerEvidence(
                questionId = question.id,
                heading = question.heading,
                prompt = question.prompt,
                optionId = option.id,
                answer = option.title,
                detail = option.detail,
                tags = option.tags,
                contextLabel = contextLabel(option.tags)
            )
        }

        val tagCounts = linkedMapOf<String, Int>()
        evidence.forEach { item ->
            item.tags.sorted().forEach { tag -> tagCounts[tag] = (tagCounts[tag] ?: 0) + 1 }
        }

        val tensions = rules.mapNotNull { rule ->
            val present = tagCounts.keys
            val leftPresent = rule.left.intersect(present)
            val rightPresent = rule.right.intersect(present)
            if (leftPresent.isEmpty() || rightPresent.isEmpty()) return@mapNotNull null

            val matched = leftPresent + rightPresent
            val supportingQuestions = evidence
                .filter { item -> item.tags.any { it in matched } }
                .map { it.questionId }
                .toSet()

            // Requiring evidence from at least two distinct questions prevents a single
            // multi-tag answer from being mislabeled as an internal contradiction.
            if (supportingQuestions.size < 2) return@mapNotNull null

            TensionRuleResult(
                id = rule.id,
                title = rule.title,
                interpretation = rule.interpretation,
                reviewQuestion = rule.question,
                matchedTags = matched,
                evidenceQuestionIds = supportingQuestions
            )
        }

        val contexts = linkedMapOf<String, Int>()
        evidence.forEach { item -> contexts[item.contextLabel] = (contexts[item.contextLabel] ?: 0) + 1 }

        return ActiveTrialAnalysisResult(
            activeAnswers = evidence,
            tagCounts = tagCounts,
            tensions = tensions,
            contextCounts = contexts
        )
    }

    private fun contextLabel(tags: Set<String>): String = when {
        tags.any { it in setOf("attachment", "belonging", "approval", "boundaries", "empathy", "conflict_avoidance") } -> "العلاقات والحدود"
        tags.any { it in setOf("values", "meaning", "purpose", "authenticity", "identity") } -> "القيم والهوية"
        tags.any { it in setOf("control", "certainty", "safety", "uncertainty", "acceptance") } -> "الأمان والغموض"
        tags.any { it in setOf("fear", "avoidance", "courage", "agency", "change", "resilience") } -> "الخوف والتغيير"
        tags.any { it in setOf("self_criticism", "self_compassion", "perfectionism", "shame", "judgment") } -> "الصورة الذاتية"
        else -> "التأمل والاختيار"
    }
}
