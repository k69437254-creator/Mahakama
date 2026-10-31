package com.aram.mahakama.nextupdate.domain

import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.nextupdate.model.FaceEvidenceItem
import com.aram.mahakama.nextupdate.model.FaceGraphEdge
import com.aram.mahakama.nextupdate.model.FaceGraphNode
import com.aram.mahakama.nextupdate.model.FaceGraphSnapshot

/** Evidence-first graph built from the seven authored faces already present in the app. */
object FaceGraphEngine {
    private val counterTags: Map<String, Set<String>> = mapOf(
        "guardian" to setOf("acceptance", "flexibility", "agency"),
        "seeker" to setOf("avoidance", "restlessness"),
        "mirror" to setOf("self_protection", "shame"),
        "keeper" to setOf("authenticity", "boundaries", "values"),
        "challenger" to setOf("avoidance", "fear"),
        "critic" to setOf("self_compassion", "acceptance"),
        "wanderer" to setOf("courage", "agency", "persistence")
    )

    /**
     * Curated semantic links, not inferred diagnoses. Every relation is checked against the
     * actual face catalog so a renamed/removed face cannot leave a broken graph edge.
     */
    private val related: Map<String, Set<String>> = mapOf(
        "guardian" to setOf("seeker", "critic", "keeper"),
        "seeker" to setOf("guardian", "mirror", "wanderer"),
        "mirror" to setOf("seeker", "challenger", "critic"),
        "keeper" to setOf("guardian", "mirror", "critic"),
        "challenger" to setOf("mirror", "wanderer", "seeker"),
        "critic" to setOf("guardian", "keeper", "mirror"),
        "wanderer" to setOf("seeker", "challenger", "guardian")
    )

    fun build(answers: Map<String, String>): FaceGraphSnapshot {
        val analysis = ActiveTrialAnalysis.analyze(answers)
        val faceCatalog = QuestionnaireEngine.faces
        val validIds = faceCatalog.map { it.id }.toSet()

        val nodes = faceCatalog.map { face ->
            val support = analysis.activeAnswers.mapNotNull { answer ->
                val matched = answer.tags.intersect(face.tags)
                if (matched.isEmpty()) null else answer.toEvidence(matched)
            }
            val opposing = analysis.activeAnswers.mapNotNull { answer ->
                val matched = answer.tags.intersect(counterTags[face.id].orEmpty())
                if (matched.isEmpty()) null else answer.toEvidence(matched)
            }
            val score = support.sumOf { it.matchedTags.size }
            val context = dominantContext(support)

            FaceGraphNode(
                id = face.id,
                title = face.title,
                subtitle = face.subtitle,
                description = face.description,
                score = score,
                signalLabel = when {
                    support.isEmpty() -> "لم تظهر أدلة كافية"
                    support.size == 1 -> "إشارة أولية واحدة"
                    support.size <= 3 -> "إشارات متعددة محدودة"
                    else -> "إشارات متكررة في المسار"
                },
                supportingEvidence = support,
                counterEvidence = opposing,
                contextLabel = context,
                contextNote = contextNote(face.id),
                relatedFaceIds = related[face.id].orEmpty().filter { it in validIds }.sorted(),
                unresolvedQuestion = unresolvedQuestion(face.id)
            )
        }.sortedWith(compareByDescending<FaceGraphNode> { it.score }.thenBy { it.title })

        val undirectedEdges = linkedSetOf<Pair<String, String>>()
        related.forEach { (from, targets) ->
            targets.filter { it in validIds && it != from }.forEach { to ->
                undirectedEdges += if (from < to) from to to else to to from
            }
        }

        return FaceGraphSnapshot(
            rootTitle = "الذات التي تراقب",
            rootDescription = "لا تمثل الوجوه هويةً ثابتة؛ إنها زوايا محتملة تتغير قوتها وحدودها مع السياق والأدلة.",
            nodes = nodes,
            edges = undirectedEdges.map { FaceGraphEdge(it.first, it.second) },
            activeAnswerCount = analysis.answeredCount
        )
    }

    private fun com.aram.mahakama.nextupdate.model.ActiveAnswerEvidence.toEvidence(
        matched: Set<String>
    ) = FaceEvidenceItem(
        questionId = questionId,
        heading = heading,
        answer = answer,
        detail = detail,
        matchedTags = matched,
        contextLabel = contextLabel
    )

    private fun dominantContext(
        support: List<FaceEvidenceItem>
    ): String {
        if (support.isEmpty()) return "لم يتحدد بعد"
        return support.groupingBy { it.contextLabel }.eachCount()
            .entries.sortedByDescending { it.value }.firstOrNull()?.key
            ?: "سياقات متعددة"
    }

    private fun contextNote(faceId: String): String = when (faceId) {
        "guardian" -> "قد يبرز هذا الوجه عندما يصبح الأمان أو تقليل الغموض مهمًا. افحص أيضًا إن كان التخطيط يفتح خياراتك أم يضيّقها."
        "seeker" -> "قد يظهر مع البحث عن المعنى وفهم السياق. السؤال المقابل هو متى يتحول الفهم إلى تأجيلٍ للفعل."
        "mirror" -> "قد يظهر عندما تفحص صورتك عن نفسك أو تعترف بخطأ. الصراحة تحتاج إلى رحمة كي لا تتحول إلى جلدٍ للذات."
        "keeper" -> "قد يظهر في المواقف التي تتقاطع فيها العلاقة والانتماء والحدود. القرب لا يلغي حقك في الاحتفاظ بصوتك."
        "challenger" -> "قد يظهر عندما تتجه إلى التغيير أو تحمل نتيجة قرار. وجود الخوف لا ينفي وجود القدرة على الفعل."
        "critic" -> "قد يظهر عندما يرتفع معيارك أو تستحضر حكم الآخرين. ميّز بين المسؤولية والعقوبة الداخلية."
        "wanderer" -> "قد يظهر عندما تصبح المسافة أو التأجيل وسيلةً للحماية. تحقق مما إذا كانت المسافة اختيارًا واعيًا أم بابًا مغلقًا."
        else -> "هذا وجهٌ احتمالي يحتاج إلى أدلة وسياق إضافيين."
    }

    private fun unresolvedQuestion(faceId: String): String = when (faceId) {
        "guardian" -> "ما الذي تحاول حمايته تحديدًا: نفسك، علاقةً مهمة، أم صورةً تريد الحفاظ عليها؟"
        "seeker" -> "ما السؤال الذي تبحث عن إجابته، وما الخطوة التي ستتخذها حتى لو بقي جزءٌ من الغموض؟"
        "mirror" -> "كيف يمكن أن تعترف بما حدث دون أن تختصر نفسك كلها في ذلك الحدث؟"
        "keeper" -> "في أيّ علاقةٍ تحتاج إلى حدٍّ واضح كي يبقى القرب صادقًا؟"
        "challenger" -> "ما الخطوة الصغيرة التي تثبت التغيير بالفعل، لا بالرغبة وحدها؟"
        "critic" -> "هل المعيار الذي تحاسب نفسك به عادلٌ لو طُبّق على شخصٍ تحبه؟"
        "wanderer" -> "ما الذي تحتاج إلى مواجهته، وما المسافة التي تحتاجها كي تواجهه بأمان؟"
        else -> "ما الدليل الذي قد يغيّر هذه القراءة؟"
    }
}
