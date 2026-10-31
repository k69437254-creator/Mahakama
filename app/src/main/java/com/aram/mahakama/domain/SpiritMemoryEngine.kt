package com.aram.mahakama.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, evidence-led memory for Spirit of the Soul.
 * It stores submitted questionnaire choices, dialogue messages and reactions,
 * symbolic-world choices and appearance timestamps. Free-writing drafts are
 * intentionally excluded.
 */
data class SpiritInsight(
    val key: String,
    val line: String,
    val followUp: String,
    val layer: String,
    val evidence: String,
    val symbolicLine: String,
    val confidence: String
)

object SpiritMemoryEngine {
    private data class TensionPattern(
        val tags: Set<String>,
        val key: String,
        val title: String,
        val description: String,
        val question: String,
        val symbol: String
    )

    private const val MAX_EVENTS = 500
    private const val RETURN_AFTER_MILLIS = 24L * 60L * 60L * 1000L


    fun dialogueEvent(
        text: String,
        insightKey: String,
        role: String,
        now: Long = System.currentTimeMillis()
    ): String = JSONObject()
        .put("kind", "dialogue")
        .put("at", now)
        .put("role", role)
        .put("text", text)
        .put("insightKey", insightKey)
        .toString()

    fun dialogueResponse(memoryJson: String, userText: String): String {
        val text = userText.trim()
        val previousDialogue = events(memoryJson)
            .filter { it.optString("kind") == "dialogue" }
            .takeLast(8)

        val lower = text.lowercase()
        val repeated = previousDialogue
            .filter { it.optString("role") == "user" }
            .any { it.optString("text").equals(text, ignoreCase = true) }

        return when {
            text.isBlank() ->
                "وصلني صمتك هذه المرة. لا حاجة إلى أن تملأه بكلمات لا تشبهك."

            repeated ->
                "ألاحظ أنك عدت إلى العبارة نفسها. ربما لم تجد بعد الكلمات التي تقترب مما تريد قوله."

            listOf("لا تفهم", "لم تفهم", "مو هذا", "ليس هذا", "خطأ").any { it in lower } ->
                "ربما قرأت كلماتك على نحوٍ لا يشبه قصدك. صحّح لي ما أسأت فهمه؛ لا أريد أن أتمسك بتأويلٍ لا تؤيده كلماتك."

            listOf("لا أعرف", "ما أعرف", "لا ادري", "لا أدري", "محتار").any { it in lower } ->
                "عدم معرفتك الآن ليس نقصًا. يمكننا أن نترك السؤال مفتوحًا بدل أن نخترع له إجابة."

            listOf("خائف", "خوف", "أخاف", "قلق", "قلقان").any { it in lower } ->
                "أسمع في كلامك حضور الخوف. لن أفترض سببه؛ ما أستطيع فعله هو أن أبقى معك عند حدود ما قلته."

            listOf("غاضب", "غضب", "عصبي", "منزعج").any { it in lower } ->
                "الغضب يستحق أن يُسمع قبل أن نحكم عليه. لن أختصره في صفة؛ ربما وراءه أمرٌ لم يُفهم بعد."

            listOf("ندم", "نادم", "أندم", "آسف").any { it in lower } ->
                "الندم يعيدنا إلى ما حدث، لكنه لا يمنحنا وحده صورةً كاملة. ما الذي تتمنى لو أنك فهمته وقتها؟"

            listOf("أريد", "أتمنى", "أرغب", "أتطلع").any { it in lower } ->
                "هناك رغبةٌ تحاول أن تجد طريقها إلى الكلام. لن أقرر معناها نيابةً عنك؛ أخبرني ما الذي يجعلها مهمة."

            listOf("أكره", "كره", "يؤلمني", "متألم").any { it in lower } ->
                "لن أطلب منك تلطيف ما تشعر به. قل ما تستطيع قوله، وسأتعامل معه بوصفه تجربةً تخصك لا حكمًا عليك."

            previousDialogue.isNotEmpty() ->
                "أريد أن أفهم ما تقصده دون أن أضع كلماتٍ في فمك. أي جزء من حديثنا السابق يرتبط بما قلته الآن؟"

            else ->
                "أنا هنا لأصغي، لا لأحسم معنى كلامك. أخبرني أكثر: ما الذي جعل هذه الفكرة حاضرةً في ذهنك؟"
        }
    }

    fun answerEvent(question: TrialQuestion, option: AnswerOption, now: Long = System.currentTimeMillis()): String =
        JSONObject()
            .put("kind", "answer")
            .put("at", now)
            .put("questionId", question.id)
            .put("heading", question.heading)
            .put("prompt", question.prompt)
            .put("optionId", option.id)
            .put("answer", option.title)
            .put("detail", option.detail)
            .put("tags", JSONArray(option.tags.toList()))
            .put("context", contextFor(question.heading + " " + question.prompt))
            .toString()

    fun feedbackEvent(reply: String, insightKey: String, now: Long = System.currentTimeMillis()): String =
        JSONObject()
            .put("kind", "feedback")
            .put("at", now)
            .put("reply", when (reply) {
            "لا تمثلني بدقة" -> "الصورة ليست دقيقة"
            "قريبة من تجربتي" -> "قريبة مما أشعر به"
            else -> reply
        })
        .put("originalReply", reply)
            .put("insightKey", insightKey)
            .toString()

    fun symbolChoiceEvent(choice: String, insightKey: String, now: Long = System.currentTimeMillis()): String =
        JSONObject()
            .put("kind", "symbol")
            .put("at", now)
            .put("choice", choice)
            .put("insightKey", insightKey)
            .toString()

    fun appearanceEvent(
        memoryJson: String,
        insightKey: String,
        pendingQuestion: String = "",
        now: Long = System.currentTimeMillis()
    ): String =
        JSONObject()
            .put("kind", "appearance")
            .put("at", now)
            .put("insightKey", insightKey)
            .put("pendingQuestion", pendingQuestion)
            .put("answerCount", events(memoryJson).count { it.optString("kind") == "answer" })
            .toString()

    fun appendEvent(memoryJson: String, eventJson: String): String {
        val items = events(memoryJson).toMutableList()
        val event = try { JSONObject(eventJson) } catch (_: Exception) { return memoryJson.ifBlank { "[]" } }
        items += event
        val kept = items.takeLast(MAX_EVENTS)
        val result = JSONArray()
        kept.forEach { result.put(it) }
        return result.toString()
    }

    fun shouldReveal(memoryJson: String): Boolean {
        val all = events(memoryJson)
        val answers = all.filter { it.optString("kind") == "answer" }
        if (answers.size < 3) return false
        val lastAppearance = all.lastOrNull { it.optString("kind") == "appearance" }
        val lastCount = lastAppearance?.optInt("answerCount", 0) ?: 0
        val unseenAnswers = answers.size - lastCount
        if (unseenAnswers >= 3) return true
        if (unseenAnswers < 2) return false
        val insight = createInsight(memoryJson, emptyMap())
        val highSignal = insight.key.startsWith("revisited_") || insight.key in setOf(
            "desire_fear", "control_responsibility", "context_images", "opposing_motives", "speech_action"
        )
        return highSignal && insight.key != lastAppearance?.optString("insightKey").orEmpty()
    }

    fun createInsight(memoryJson: String, currentAnswers: Map<String, String>): SpiritInsight {
        val all = events(memoryJson)
        val answers = all.filter { it.optString("kind") == "answer" }
        val latest = answers.lastOrNull()
        val previous = answers.dropLast(1).lastOrNull()
        val tagEvents = answers.map { event -> event to tagsOf(event) }
        val counts = mutableMapOf<String, Int>()
        val tagQuestionIds = mutableMapOf<String, MutableSet<String>>()
        tagEvents.forEach { (event, tags) ->
            tags.forEach { tag ->
                counts[tag] = (counts[tag] ?: 0) + 1
                tagQuestionIds.getOrPut(tag) { mutableSetOf() }.add(event.optString("questionId"))
            }
        }
        val tagSet = counts.keys
        val contexts = answers.map { it.optString("context") }.filter { it.isNotBlank() }.toSet()
        val rememberedWorldChoice = all.lastOrNull { it.optString("kind") == "symbol" }
            ?.optString("choice").orEmpty()
        val lastFeedback = all.lastOrNull { it.optString("kind") == "feedback" }
        val feedbackReply = lastFeedback?.optString("originalReply")
            ?.takeIf { it.isNotBlank() }
            ?: lastFeedback?.optString("reply").orEmpty()
        val feedbackNote = when (feedbackReply) {
            "الصورة ليست دقيقة", "لا تمثلني بدقة" ->
                "\nواعتراضك السابق على قراءة «${lastFeedback?.optString("insightKey").orEmpty()}» محفوظ؛ لا أتعامل معها كحقيقةٍ نهائية."
            "قريبة مما أشعر به", "قريبة من تجربتي" ->
                "\nسبق أن قلت إن قراءةً ما قريبة من تجربتك، وسأبقي قبولك احتمالًا قابلًا للمراجعة."
            else -> ""
        }
        val evidence = evidenceFor(latest, previous) +
            (if (rememberedWorldChoice.isNotBlank()) "\nومن ذاكرة المكان: سبق أن اخترت «$rememberedWorldChoice»." else "") +
            feedbackNote
        val repeatedValue = counts.entries
            .filter { (tag, count) -> count >= 3 && (tagQuestionIds[tag]?.size ?: 0) >= 3 }
            .maxByOrNull { it.value }

        // A revised answer to an earlier node is stored as a new event instead of erasing the old one.
        val revised = answers.groupBy { it.optString("questionId") }.values
            .firstOrNull { group -> group.map { it.optString("optionId") }.distinct().size > 1 }
        if (revised != null) {
            val old = revised.first()
            val new = revised.last()
            return insight(
                key = "revisited_${new.optString("questionId")}_${new.optString("optionId")}",
                line = "المراقب: إجابتك عن سؤالٍ قديم لم تبقَ كما كانت.\nالمحلّل: هذا لا يثبت تناقضًا؛ قد تكون الظروف أو فهمك للسؤال قد تغيّرا.\nالمواجه: ما الذي تغيّر بين اختيارك الأول واختيارك الأحدث؟",
                followUp = "حين أجبت أول مرة «${old.optString("answer")}»، ثم اخترت لاحقًا «${new.optString("answer")}»، ما الذي أصبح مختلفًا في فهمك أو في ظروفك؟",
                layer = "المحلّل",
                evidence = "السؤال: ${new.optString("heading")}\nالأثر الأول: ${old.optString("answer")}\nالأثر الأحدث: ${new.optString("answer")}",
                symbolicLine = "المرآة القديمة لا تختفي؛ يتبدل الانعكاس حين تتبدل زاوية الوقوف.",
                confidence = "تحوّل موثّق في إجابة السؤال نفسه"
            )
        }

        // Values stated in one answer can sit beside protective or avoidant choices elsewhere.
        if (("honesty" in tagSet || "authenticity" in tagSet || "responsibility" in tagSet) &&
            ("avoidance" in tagSet || "approval" in tagSet || "self_protection" in tagSet || "fear" in tagSet)) {
            return insight(
                key = "words_actions_${answers.size / 2}",
                line = "المراقب: تظهر في إجاباتك قيمٌ تريد أن تكون حاضرًا في أفعالك.\nالمحلّل: وفي مواقف أخرى تظهر اختيارات قد تبتعد عنها، وربما تفسرها ظروف مختلفة.\nالمواجه: أيّ قيمةٍ يصعب عليك الحفاظ عليها عندما تصبح كلفتها حقيقية؟",
                followUp = "هل ترى مسافةً بين ما تؤمن به وما فعلته في بعض المواقف، أم أن السياق جعل الاختيارين معًا مفهومين؟",
                layer = "المواجه",
                evidence = evidence,
                symbolicLine = "على المرآة خطّان متجاوران؛ لا يمحو أحدهما الآخر قبل أن نفهم المسافة بينهما.",
                confidence = if (answers.size >= 6) "إشارات متكررة في أكثر من موضع" else "احتمال أولي يحتاج إلى سياق"
            )
        }

        val tensionPatterns = listOf(
            TensionPattern(setOf("fear", "agency", "avoidance"), "desire_fear", "بين الرغبة والخوف", "قد تتجه رغبتك نحو خطوةٍ ما، بينما تظهر في اختيارات أخرى حاجةٌ إلى التريث أو الحماية. لا أعرف أن الخوف هو السبب؛ أريد أن أفهم ما الذي يجعل الاقتراب صعبًا أحيانًا.", "ما الذي تريده في هذا الموقف، وما الذي قد تخسره أو تتحمله إن اقتربت منه؟", "طريقٌ ينقسم عند مفترق، ولا يحمل أيّ اتجاهٍ حكمًا مسبقًا."),
            TensionPattern(setOf("control", "acceptance"), "control_responsibility", "بين السيطرة وحدود المسؤولية", "تظهر في مسارك رغبةٌ في التأثير، ومعها اعترافٌ بأن بعض النتائج لا تقع كلها في يدك. المسافة بينهما تستحق أن تُفحص دون تحميلك ما ليس بوسعك.", "ما الجزء الذي كان بيدك فعلًا، وما الجزء الذي صنعته الظروف أو اختيارات الآخرين؟", "بابٌ نصف مفتوح؛ خلفه ما تملكه، وأمامه ما لا تملكه."),
            TensionPattern(setOf("approval", "authenticity"), "context_images", "بين صورك في السياقات المختلفة", "تظهر اختياراتك في أكثر من سياق، وقد تتبدل طريقتك بحسب العلاقة أو الموقف. لا أرى في ذلك صورةً مزيفة وأخرى حقيقية؛ أريد أن أفهم ما الذي يستدعي كل استجابة.", "مع من أو في أيّ ظرفٍ تظهر هذه الصورة منك، وما الذي تسمح به ولا تسمح به الصورة الأخرى؟", "مرآتان في ممر واحد؛ لكل واحدة زاوية، ولا تكفي واحدة لتمثيل المشهد."),
            TensionPattern(setOf("attachment", "boundaries"), "opposing_motives", "بين القرب والحاجة إلى المسافة", "قد يجتمع في اختياراتك طلب القرب وحماية الحدود. ليس على أحدهما أن يلغي الآخر؛ المهم أن نعرف متى تحمي المسافة علاقةً، ومتى تمنعها من أن تُفهم.", "متى تكون المسافة اختيارًا يحفظك، ومتى تصبح عائقًا أمام ما تريده؟", "كرسيان بينهما مسافة قابلة للتغير، لا فراغٌ نهائي."),
            TensionPattern(setOf("honesty", "avoidance"), "speech_action", "بين الصراحة وتوقيت قول الحقيقة", "تظهر قيمة الصراحة في بعض اختياراتك، ويظهر التأجيل أو التحفظ في أخرى. قد يكون ذلك اختلافًا في السياق، وقد يكون توترًا يستحق السؤال.", "ما الذي يجعل قول الحقيقة ممكنًا في موقفٍ وصعبًا في موقفٍ آخر؟", "مرآةٌ تحمل أثرًا قديمًا وبابٌ لم يُفتح بعد.")
        )
        val tension = tensionPatterns.firstOrNull { pattern -> pattern.tags.all { it in tagSet } }
        if (tension != null) {
            return insight(
                key = tension.key,
                line = "${tension.title}: ${tension.description}",
                followUp = tension.question,
                layer = if ((tension.tags.sumOf { counts[it] ?: 0 }) >= 5) "المواجه" else "المحلّل",
                evidence = evidence,
                symbolicLine = tension.symbol,
                confidence = if (tension.tags.all { (tagQuestionIds[it]?.size ?: 0) >= 2 }) "قرائن تكررت في سياقات متعددة" else "إشارات أولية قابلة للمراجعة"
            )
        }

        if (contexts.size >= 2 && answers.size >= 4) {
            return insight(
                key = "multiple_contexts_${contexts.sorted().joinToString("_")}",
                line = "المراقب: إجاباتك جاءت من سياقات مختلفة، ولم تتخذ فيها استجابةً واحدة دائمًا.\nالمحلّل: قد يكون اختلافك استجابةً لما يتغير حولك، لا انقسامًا بين شخصيتين.\nالمواجه: أيّ سياقٍ يغيّر اختيارك أكثر من غيره؟",
                followUp = "لو وضعنا موقفين من سياقين مختلفين جنبًا إلى جنب، ما العامل الذي يغيّر قرارك بينهما؟",
                layer = "المحلّل",
                evidence = "السياقات التي ظهرت في الإجابات: ${contexts.joinToString("، ")}.\n$evidence",
                symbolicLine = "الممر يتفرع إلى مشاهد متعددة، وتبقى آثار خطواتك في كل طريق.",
                confidence = "اختلاف سياقي ظاهر؛ تفسيره ما زال مفتوحًا"
            )
        }

        if (repeatedValue != null) {
            val tag = repeatedValue.key
            val label = when (tag) {
                "avoidance" -> "التأجيل أو الانسحاب"
                "control" -> "محاولة استعادة السيطرة"
                "approval" -> "مراعاة نظرة الآخرين"
                "honesty" -> "طلب الصراحة"
                "boundaries" -> "حماية الحدود"
                "fear" -> "حضور الخوف"
                "agency" -> "المبادرة والقدرة على الفعل"
                else -> "${tag}"
            }
            return insight(
                key = "repeated_choice_$tag",
                line = "هناك اختيارٌ يتكرر في أكثر من موضع: $label. التكرار لا يجعله عيبًا ولا حقيقةً ثابتة؛ قد يكون وسيلةً نافعة في بعض المواقف، وقد يضيّق الخيارات في مواقف أخرى.",
                followUp = "متى يخدمك هذا الاختيار، ومتى تتمنى لو أنك جرّبت استجابةً مختلفة؟",
                layer = "المراقب",
                evidence = evidence,
                symbolicLine = "طريقٌ يحمل آثار خطواتٍ متكررة، وبجواره ممر لم تختبره بعد.",
                confidence = "تكرار ظاهر في ${tagQuestionIds[tag]?.size ?: 0} أسئلة مختلفة"
            )
        }

        val stable = counts.entries
            .filter { (tag, count) -> count >= 3 && (tagQuestionIds[tag]?.size ?: 0) >= 3 }
            .maxByOrNull { it.value }
        if (stable != null) {
            return insight(
                key = "stable_${stable.key}",
                line = "رغم اختلاف بعض إجاباتك، يعود خيطٌ واحد في أكثر من موضع. قد يكون قيمةً بقيت حاضرة أو طريقةً اعتدت الرجوع إليها؛ لا أريد أن أسميها ثابتًا قبل أن نختبر معناها.",
                followUp = "هل بقي هذا الخيط مهمًا لك عبر الزمن، أم أن معناه تغيّر بينما بقي اسمه؟",
                layer = "المراقب",
                evidence = evidence,
                symbolicLine = "نقشٌ قديم بقي على الجدار، لكن الضوء الساقط عليه تغيّر.",
                confidence = "إشارة متكررة عبر ثلاثة مواضع أو أكثر"
            )
        }

        if (answers.size >= 8) {
            val known = setOf("العلاقات", "الذات", "الاختيار", "القيم", "المجتمع", "المسؤولية")
            val missing = known - contexts
            if (missing.isNotEmpty()) {
                val missingLabel = missing.first()
                return insight(
                    key = "unexplored_${missingLabel}",
                    line = "هناك مساحةٌ لم تأخذ حقها من الأسئلة حتى الآن: $missingLabel. لا أعدّ غيابها دليلًا على تجنبك لها؛ ربما لم يصل المسار إليها بعد.",
                    followUp = "هل ترى أن هذا الجانب يستحق أن نفتحه، أم تفضّل أن نتركه خارج هذه الجلسة؟",
                    layer = "المراقب",
                    evidence = "توزعت إجاباتك المسجلة على: ${contexts.joinToString("، ")}.",
                    symbolicLine = "بابٌ غير مفتوح في الممر؛ لا يعني أنه يخفي شيئًا، بل أن الطريق لم يمر به بعد.",
                    confidence = "مساحة غير مستكشفة، وليست استنتاجًا عن سبب غيابها"
                )
            }
        }

        val question = latest?.optString("prompt").orEmpty()
        val followUp = when {
            "حرية" in question || "اختيار" in question -> "ما الذي يجعل هذا الاختيار اختيارك أنت، لا مجرد استجابةٍ لما يحيط بك؟"
            "علاقة" in question || "آخرين" in question -> "كيف يتغير موقفك عندما يتغير الشخص أو شكل العلاقة؟"
            else -> "ما السياق الذي قد يجعل إجابتك مختلفة، وما الدليل الذي قد يغيّر قراءتي؟"
        }
        return insight(
            key = "open_thread_${answers.size}",
            line = if (answers.size < 3) "ما زالت الذاكرة تجمع خيوطها. لن أستعجل قراءةً لا تسندها أدلة كافية." else "لا أرى بعد نمطًا أستطيع الدفاع عنه بثقة. سأحتفظ بهذا الخيط مفتوحًا بدل أن أملأ فراغه بتفسيرٍ جاهز.",
            followUp = followUp,
            layer = "المراقب",
            evidence = evidence,
            symbolicLine = "ضبابٌ هادئ حول ممر لم تتضح نهايته بعد.",
            confidence = "أدلة غير كافية لاستنتاج أوسع"
        )
    }

    fun returnAfterAbsence(memoryJson: String, now: Long = System.currentTimeMillis()): SpiritInsight? {
        val all = events(memoryJson)
        val lastAppearance = all.lastOrNull { it.optString("kind") == "appearance" } ?: return null
        val lastAt = lastAppearance.optLong("at", 0L)
        if (lastAt <= 0L || now - lastAt < RETURN_AFTER_MILLIS) return null
        val lastAnswer = all.lastOrNull { it.optString("kind") == "answer" } ?: return null
        val days = ((now - lastAt) / RETURN_AFTER_MILLIS).coerceAtLeast(1L)
        val pendingQuestion = lastAppearance.optString("pendingQuestion")
        val rememberedQuestion = if (pendingQuestion.isNotBlank()) " وكان السؤال الذي تركناه مفتوحًا: «$pendingQuestion»." else ""
        return insight(
            key = "return_$lastAt",
            line = "عدتَ إلى المحاكمة بعد مسافةٍ من الزمن. لا أستنتج شيئًا من غيابك؛ ربما كانت الحياة مزدحمة، أو احتجت إلى الابتعاد، أو ببساطة لم ترغب في المتابعة. آخر خيطٍ تركناه كان: «${lastAnswer.optString("answer")}».$rememberedQuestion",
            followUp = if (pendingQuestion.isNotBlank()) pendingQuestion else "هل ما زال ذلك السؤال قريبًا من تجربتك، أم أن ما حدث منذ آخر لقاء غيّر معناه؟",
            layer = "المراقب",
            evidence = "آخر أثرٍ مسجل: ${lastAnswer.optString("heading")} — ${lastAnswer.optString("answer")}\nالفاصل التقريبي: $days يومًا.",
            symbolicLine = "كرسيٌ بقي في مكانه، وعلى الطاولة سؤالٌ لم نطالبك بإجابته.",
            confidence = "استدعاء زمني للسياق، لا تفسيرٌ لسبب الغياب"
        )
    }

    fun symbolChoiceLine(choice: String): String = when (choice) {
        "أفتح الباب" -> "انفتح الباب في عالم المحاكمة. لن أقرر ما يعنيه لك؛ سأحتفظ باختيارك لنرى كيف يتغير المشهد لاحقًا."
        "أترك الباب مغلقًا" -> "بقي الباب مغلقًا. لا أفسر ذلك رفضًا أو خوفًا؛ أحيانًا يكون ترك السؤال في مكانه اختيارًا كافيًا."
        "ألمس المرآة" -> "تركت أثرًا على المرآة. سنعود إليها حين تتغير الإجابات، ونرى ما إذا كان انعكاسها قد تبدل."
        else -> "اخترت مواصلة الطريق. سأحتفظ بهذا المسار في ذاكرة العالم الرمزي، دون أن أقرر إلى أين ينبغي أن يقودك."
    }

    private fun insight(
        key: String,
        line: String,
        followUp: String,
        layer: String,
        evidence: String,
        symbolicLine: String,
        confidence: String
    ) = SpiritInsight(key, line, followUp, layer, evidence, symbolicLine, confidence)

    private fun evidenceFor(latest: JSONObject?, previous: JSONObject?): String {
        if (latest == null) return "لم تُسجّل إجابةٌ بعد."
        val current = "«${latest.optString("answer")}» عند سؤال «${latest.optString("heading")}»."
        val earlier = previous?.let { "\nوقبلها: «${it.optString("answer")}» عند «${it.optString("heading") }»." }.orEmpty()
        return current + earlier
    }

    private fun tagsOf(event: JSONObject): Set<String> {
        val array = event.optJSONArray("tags") ?: return emptySet()
        return buildSet {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun events(raw: String): List<JSONObject> = try {
        val array = JSONArray(raw.ifBlank { "[]" })
        buildList {
            for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(it) }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun contextFor(text: String): String = when {
        listOf("علاقة", "انتماء", "الآخر", "الآخرين", "القرب", "الحب").any { it in text } -> "العلاقات"
        listOf("عمل", "مجتمع", "الناس", "نظرة", "حكم الآخرين").any { it in text } -> "المجتمع"
        listOf("مسؤولية", "واجب", "خطأ", "نتيجة").any { it in text } -> "المسؤولية"
        listOf("حرية", "قيمة", "مبدأ", "حقيقة", "الصدق").any { it in text } -> "القيم"
        listOf("قرار", "اختيار", "سيطرة", "مواجهة", "تغيير").any { it in text } -> "الاختيار"
        else -> "الذات"
    }
}
