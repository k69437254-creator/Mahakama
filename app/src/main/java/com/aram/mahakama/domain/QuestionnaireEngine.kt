package com.aram.mahakama.domain

/** Each option carries reflective tags and the next question it opens. */
data class AnswerOption(
    val id: String,
    val title: String,
    val detail: String,
    val tags: Set<String>,
    val nextQuestionId: String? = null
)

data class TrialQuestion(
    val id: String,
    val heading: String,
    val prompt: String,
    val options: List<AnswerOption>
)

data class FaceInsight(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val tags: Set<String>
)

object QuestionnaireEngine {
    /** The sixteen foundation nodes preserve the authored early branching path. */
    private val foundationQuestions: List<TrialQuestion> = listOf(
        q("q01", "قبل أن تبدأ المحاكمة", "حين تفقد السيطرة على أمرٍ يهمك، ما أول ما يتحرك داخلك؟",
            a("control", "أحاول استعادة زمام الأمور", "أبحث عن خطة تجعل المجهول أقل اتساعًا.", setOf("control", "certainty"), "q02_control"),
            a("accept", "أترك مساحة لما لا أستطيع تغييره", "أحاول أن أميّز بين ما أملكه وما لا أملكه.", setOf("acceptance", "flexibility"), "q02_accept"),
            a("delay", "أؤجل المواجهة حتى تتضح الصورة", "أحتاج إلى وقت قبل أن أختار موقفي.", setOf("avoidance", "uncertainty"), "q02_delay")
        ),
        q("q02_control", "الحاجة إلى اليقين", "ماذا يمنحك التخطيط الدقيق أكثر من غيره؟",
            a("plan_safety", "شعورًا بالأمان", "كأن ترتيب الاحتمالات يخفف ضجيجها.", setOf("control", "safety"), "q03"),
            a("plan_success", "فرصة أكبر للنجاح", "أفضّل أن أصل مستعدًا بدل أن أترك الأمر للصدفة.", setOf("ambition", "control"), "q03"),
            a("plan_respect", "طمأنينة بأنني لن أُلام", "أخشى أحيانًا أن يصبح الخطأ دليلًا ضدي.", setOf("judgment", "perfectionism"), "q03")
        ),
        q("q02_accept", "حدود ما تملكه", "حين تترك شيئًا خارج سيطرتك، ما الذي يساعدك على تقبّل ذلك؟",
            a("accept_trust", "الثقة بأنني سأتعامل مع ما يأتي", "لا أحتاج إلى معرفة كل شيء كي أواصل الطريق.", setOf("acceptance", "resilience"), "q03"),
            a("accept_priority", "معرفة ما يستحق طاقتي", "ليس كل صراع جديرًا بأن أستهلك نفسي فيه.", setOf("boundaries", "clarity"), "q03"),
            a("accept_fatigue", "التعب من محاولة السيطرة", "أحيانًا أترك الأمر لأن مقاومته أنهكتني.", setOf("fatigue", "avoidance"), "q03")
        ),
        q("q02_delay", "تأجيل المواجهة", "ما الذي تنتظره غالبًا قبل اتخاذ القرار؟",
            a("delay_info", "معلومة تجعل الصورة أوضح", "أريد أن أختار بوعي لا بعجلة.", setOf("reflection", "certainty"), "q03"),
            a("delay_courage", "الشجاعة لتحمّل النتيجة", "أعرف أحيانًا ما أريده، لكن ثمنه يؤخرني.", setOf("fear", "avoidance"), "q03"),
            a("delay_others", "أن يتخذ الآخرون الخطوة أولًا", "يصعب عليّ أن أكون أول من يغيّر ما اعتاده الجميع.", setOf("approval", "avoidance"), "q03")
        ),
        q("q03", "الحقيقة التي تختارها", "إذا اصطدمت الحقيقة بصورةٍ تحبها عن نفسك، كيف تتعامل مع هذا الاصطدام؟",
            a("truth_face", "أواجهها مهما أزعجتني", "أفضل ألم الفهم على راحة الإنكار.", setOf("honesty", "courage"), "q04_face"),
            a("truth_context", "أبحث عن السياق قبل أن أحكم", "قد تكون الصورة أعقد من خطأ أو صواب.", setOf("nuance", "reflection"), "q04_context"),
            a("truth_protect", "أحتاج إلى وقت قبل الاعتراف بها", "ليس كل ما أراه في نفسي سهل الاحتمال.", setOf("self_protection", "fear"), "q04_protect")
        ),
        q("q04_face", "ثمن الصراحة", "حين تعترف بخطئك، ما الذي يصعب عليك أكثر؟",
            a("face_pride", "أن أتنازل عن صورةٍ بنيتها عن نفسي", "أحيانًا أحمي الصورة بدل أن أراجع الفعل.", setOf("pride", "self_image"), "q05"),
            a("face_hurt", "أن أجرح شخصًا أهتم به", "الحقيقة قد تكون واجبة، لكن أثرها لا يختفي.", setOf("empathy", "responsibility"), "q05"),
            a("face_change", "أن أغيّر سلوكًا اعتدت عليه", "الفهم أسهل أحيانًا من العيش وفقه.", setOf("change", "habit"), "q05")
        ),
        q("q04_context", "ما وراء الحكم", "حين ترى تناقضًا في نفسك، إلى أين تذهب أولًا؟",
            a("context_reason", "أبحث عن السبب الذي صنعه", "أريد أن أفهم قبل أن أختصر نفسي في وصف.", setOf("reflection", "curiosity"), "q05"),
            a("context_effect", "أنظر إلى أثره على الآخرين", "النوايا مهمة، لكن النتائج لها وزنها أيضًا.", setOf("responsibility", "empathy"), "q05"),
            a("context_escape", "أحاول ألا أبالغ في التفكير فيه", "أحيانًا أخشى أن يتحول الفهم إلى دورانٍ في المكان.", setOf("avoidance", "overthinking"), "q05")
        ),
        q("q04_protect", "ما الذي تحميه؟", "عندما تؤجل الاعتراف بشيءٍ تعرفه، ما الذي تحاول حمايته؟",
            a("protect_dignity", "كرامتي وصورتي أمام نفسي", "أخشى أن يتسع الخطأ حتى يبتلع إحساسي بقيمتي.", setOf("self_image", "shame"), "q05"),
            a("protect_relations", "علاقة أخشى أن تتصدع", "قد أؤخر الحقيقة لأنني لا أعرف كيف سأحمل نتائجها.", setOf("attachment", "fear"), "q05"),
            a("protect_time", "وقتًا أحتاجه لأفهم ما حدث", "لا أريد اعترافًا سريعًا لا يتبعه فهم حقيقي.", setOf("reflection", "patience"), "q05")
        ),
        q("q05", "ما الذي يبقى بعد الفقد؟", "حين تخسر شيئًا كان جزءًا من حياتك، ما الذي تبحث عنه بعد الصدمة الأولى؟",
            a("loss_meaning", "المعنى الذي يمكن أن يبقى", "أحاول ألا أترك الخسارة تروي القصة كاملة.", setOf("meaning", "resilience"), "q06"),
            a("loss_return", "طريقة لاستعادته", "يصعب عليّ أن أقبل النهاية قبل أن أجرّب كل احتمال.", setOf("attachment", "persistence"), "q06"),
            a("loss_distance", "مسافة تحميني من الشعور", "أحيانًا أبتعد كي أستطيع الوقوف من جديد.", setOf("self_protection", "avoidance"), "q06")
        ),
        q("q06", "لغة الخوف", "أيّ خوفٍ يصعب عليك الاعتراف به؟",
            a("fear_failure", "أن أفشل بعد أن يراهن الآخرون عليّ", "كأن الفشل سيجيب عن سؤال قيمتي.", setOf("judgment", "perfectionism"), "q07"),
            a("fear_abandon", "أن أتعلق ثم أُترك", "القرب جميل، لكن احتمالات فقده حاضرة.", setOf("attachment", "fear"), "q07"),
            a("fear_stagnant", "أن أبقى الشخص نفسه رغم مرور الوقت", "أخشى أن تتحول الأيام إلى تكرارٍ لا يغيّرني.", setOf("growth", "meaning"), "q07")
        ),
        q("q07", "العين التي تحاكمك", "عندما تخطئ، أيّ صوتٍ يكون أعلى في داخلك؟",
            a("judge_harsh", "صوت يطالبني بألّا أخطئ", "كأن الرحمة بالنفس قد تفتح باب التهاون.", setOf("perfectionism", "self_criticism"), "q08"),
            a("judge_learn", "صوت يسألني ماذا أستطيع أن أتعلم", "الخطأ مؤلم، لكنه لا يحتاج أن يصبح هويتي.", setOf("growth", "self_compassion"), "q08"),
            a("judge_others", "أتخيل كيف يراني الآخرون", "قد يكون حكمهم حاضرًا حتى حين لا يتكلمون.", setOf("approval", "judgment"), "q08")
        ),
        q("q08", "ثمن الانتماء", "في علاقةٍ مهمة، ما الذي يصعب عليك الحفاظ عليه أكثر؟",
            a("belonging_self", "أن أبقى وفيًا لما أؤمن به", "أخشى أن أفقد نفسي كي لا أفقد المكان.", setOf("boundaries", "authenticity"), "q09"),
            a("belonging_peace", "أن أحافظ على الهدوء بيننا", "أحيانًا أصمت كي لا يتحول الاختلاف إلى قطيعة.", setOf("conflict_avoidance", "attachment"), "q09"),
            a("belonging_need", "أن أطلب ما أحتاجه بوضوح", "أعرف احتياجي، لكن التعبير عنه ليس دائمًا سهلًا.", setOf("vulnerability", "boundaries"), "q09")
        ),
        q("q09", "حين لا يراك أحد", "أيّ اختيارٍ يشبهك حين لا ينتظر منك أحدٌ أداءً أو تبريرًا؟",
            a("alone_create", "أفعل ما يمنحني معنى", "أحتاج إلى ما يجعل حياتي تخصني فعلًا.", setOf("meaning", "authenticity"), "q10"),
            a("alone_rest", "أبحث عن الراحة بعيدًا عن التوقعات", "التوقف أحيانًا طريقة للعودة إلى نفسي.", setOf("rest", "self_awareness"), "q10"),
            a("alone_scroll", "أملأ الصمت بأي شيء", "قد يكون الصمت مساحة، وقد يكون مرآة لا أريدها الآن.", setOf("avoidance", "restlessness"), "q10")
        ),
        q("q10", "المعنى أم القبول؟", "إذا تعارض ما تؤمن به مع ما يجعلك مقبولًا، أيّهما يميل إلى الفوز؟",
            a("value_meaning", "ما أؤمن به، حتى لو دفعت ثمنًا", "القبول لا يعوّض دائمًا خسارة نفسي.", setOf("values", "courage"), "q11"),
            a("value_belong", "العلاقة بالآخرين، لأنني أحتاجهم", "الانتماء حاجة إنسانية، وليس ضعفًا بحد ذاته.", setOf("belonging", "attachment"), "q11"),
            a("value_balance", "أحاول أن أجد طريقًا لا يمحو أيًّا منهما", "لا أريد أن أختزل حياتي في اختيارٍ سهل.", setOf("nuance", "reflection"), "q11")
        ),
        q("q11", "الوجه الذي لا تحبه", "إذا ظهر فيك سلوكٌ لا تحبه، ما أول ما تحتاجه كي تتعامل معه؟",
            a("shadow_accept", "أن أعترف بوجوده دون أن أساويه بنفسي كلها", "الاعتراف ليس استسلامًا، بل بداية اختيارٍ مختلف.", setOf("honesty", "self_compassion"), "q12"),
            a("shadow_change", "خطة واضحة لتغييره", "أحتاج إلى فعلٍ ملموس لا إلى جلدٍ طويل للنفس.", setOf("change", "agency"), "q12"),
            a("shadow_hide", "وقتًا بعيدًا عن أعين الآخرين", "ما زلت أتعلم كيف أواجه ما أخجل منه.", setOf("shame", "self_protection"), "q12")
        ),
        q("q12", "ما الذي تريد أن يتغير؟", "لو استطعت أن تغيّر شيئًا واحدًا في علاقتك بنفسك، ماذا تختار؟",
            a("future_trust", "أن أثق بحكمي دون أن أتعالى عليه", "أريد يقينًا يسمح لي بالمراجعة لا يقينًا يغلقها.", setOf("self_trust", "nuance"), "bank_001"),
            a("future_boundaries", "أن أحمي حدودي دون أن أقسو", "أريد أن أقول لا دون أن أضطر إلى إلغاء محبتي.", setOf("boundaries", "self_compassion"), "bank_002"),
            a("future_courage", "أن أتحرك رغم أن الخوف لم يختفِ", "لا أحتاج غياب الخوف بقدر ما أحتاج ألا يقودني وحده.", setOf("courage", "agency"), "bank_003")
        )
    )

    /** Exactly 1,000 unique question nodes: foundation + extended editorial matrix. */
    val questions: List<TrialQuestion> = foundationQuestions + QuestionBankBuilder.build()

    private val byId = questions.associateBy { it.id }
    val faces = listOf(
        FaceInsight("guardian", "الحارس", "حين يصبح الأمان أولوية", "قد تميل إلى تنظيم المجهول أو حماية ما يهمك. هذا الوجه لا يعني الخوف وحده؛ فقد يحمل أيضًا حرصًا ومسؤولية.", setOf("control", "certainty", "safety", "self_protection")),
        FaceInsight("seeker", "الباحث عن المعنى", "ما وراء الإجابة السهلة", "تظهر في اختياراتك رغبة في فهم السياق والأسباب، لا الاكتفاء بحكم سريع. يبقى السؤال: متى يخدمك التفكير، ومتى يؤخرك؟", setOf("meaning", "reflection", "curiosity", "nuance")),
        FaceInsight("mirror", "الصريح مع المرآة", "القدرة على النظر إلى الداخل", "تلمح بعض إجاباتك استعدادًا لفحص صورتك عن نفسك. هذه القدرة قد تحتاج إلى رحمة بقدر ما تحتاج إلى شجاعة.", setOf("honesty", "courage", "self_awareness", "self_compassion")),
        FaceInsight("keeper", "حارس الانتماء", "بين القرب وحدود الذات", "قد يكون الحفاظ على العلاقة حاضرًا بقوة في بعض قراراتك. من المفيد التمييز بين المحبة والتنازل الذي يتركك غائبًا عن نفسك.", setOf("attachment", "approval", "belonging", "conflict_avoidance")),
        FaceInsight("challenger", "المقاوم", "حين يتحول الخوف إلى حركة", "قد تظهر في اختياراتك رغبة في تغيير ما لا يرضيك أو تحمّل نتيجة الاختيار. لا يلغي ذلك التردد؛ فقد يجتمعان في الإنسان نفسه.", setOf("agency", "change", "growth", "resilience", "persistence")),
        FaceInsight("critic", "القاضي الداخلي", "المعيار الذي لا يغفو", "قد تضع على نفسك معيارًا مرتفعًا أو تستحضر نظرة الآخرين عند الخطأ. يستحق هذا الوجه سؤالًا عن الفرق بين المسؤولية والعقاب.", setOf("perfectionism", "self_criticism", "judgment", "shame")),
        FaceInsight("wanderer", "المنسحب إلى الصمت", "المسافة بوصفها حماية", "قد تستخدم التأجيل أو الابتعاد لتحصل على مساحة. أحيانًا تكون المسافة حكمة، وأحيانًا تصبح بابًا يؤجل ما يحتاج إلى مواجهة.", setOf("avoidance", "self_protection", "fear", "restlessness"))
    )

    /** The next unanswered node on the currently selected branch. */
    fun nextQuestion(answers: Map<String, String>): TrialQuestion? {
        var current = byId["q01"] ?: return null
        val visited = mutableSetOf<String>()
        while (true) {
            if (!visited.add(current.id)) return null
            val selectedId = answers[current.id] ?: return current
            val selected = current.options.firstOrNull { it.id == selectedId } ?: return current
            val nextId = selected.nextQuestionId ?: return null
            current = byId[nextId] ?: return null
        }
    }

    fun option(questionId: String, optionId: String?): AnswerOption? =
        byId[questionId]?.options?.firstOrNull { it.id == optionId }

    /**
     * Returns evidence only from the path selected by the current answers. Old answers from a
     * previously explored branch must not silently contaminate the user's current reflection.
     */
    fun activePath(answers: Map<String, String>): List<Pair<TrialQuestion, AnswerOption>> {
        val path = mutableListOf<Pair<TrialQuestion, AnswerOption>>()
        var current = byId["q01"] ?: return emptyList()
        val visited = mutableSetOf<String>()
        while (visited.add(current.id)) {
            val selectedId = answers[current.id] ?: break
            val selected = current.options.firstOrNull { it.id == selectedId } ?: break
            path += current to selected
            val nextId = selected.nextQuestionId ?: break
            current = byId[nextId] ?: break
        }
        return path
    }

    /** Keep only active-branch answers. The repository uses this to discard stale branch evidence. */
    fun activeAnswers(answers: Map<String, String>): Map<String, String> =
        activePath(answers).associate { (question, option) -> question.id to option.id }

    fun answeredOptions(answers: Map<String, String>): List<Pair<TrialQuestion, AnswerOption>> = activePath(answers)

    fun tagCounts(answers: Map<String, String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        answeredOptions(answers).forEach { (_, option) -> option.tags.forEach { tag -> result[tag] = (result[tag] ?: 0) + 1 } }
        return result
    }

    fun faceScore(face: FaceInsight, answers: Map<String, String>): Int =
        answeredOptions(answers).sumOf { (_, option) -> option.tags.count { it in face.tags } }

    fun shouldRevealSpirit(answers: Map<String, String>, countSinceLastAppearance: Int, answeredCount: Int): Boolean {
        if (answeredCount < 3 || countSinceLastAppearance < 3) return false
        val tags = tagCounts(answers).keys
        val contradictions = listOf(
            setOf("control", "acceptance"),
            setOf("avoidance", "courage"),
            setOf("approval", "authenticity"),
            setOf("self_criticism", "self_compassion"),
            setOf("attachment", "boundaries")
        )
        val tension = contradictions.any { pair -> pair.all { it in tags } }
        val quietMilestone = answeredCount == 3 || answeredCount == 7 || answeredCount == 11
        return quietMilestone || tension
    }

    fun reflectionFor(answers: Map<String, String>): String {
        val path = activePath(answers)
        if (path.size < 3) {
            return "ما زالت الأدلة قليلة؛ لا أريد أن أختصر حكايتك في أول انطباع. أجب عن مزيد من الأسئلة، ثم نراجع الخيوط التي تكررت وما خالفها."
        }
        val tags = tagCounts(answers)
        val tensions = listOf(
            Triple(setOf("control", "acceptance"), "بين ضمان كل شيء وقبول بعض المجهول", "قد تجتمع فيك رغبةٌ في ترتيب الأمور وقدرةٌ على ترك ما لا تملكه. السؤال ليس أيّ الوجهين هو أنت، بل متى يساعدك كلٌّ منهما ومتى يرهقك."),
            Triple(setOf("avoidance", "courage"), "بين الاقتراب والحاجة إلى المسافة", "تظهر إشارات إلى الحركة وإشارات إلى التأجيل. لا يلزم أن يكون أحدهما زيفًا؛ فقد تختلف استجابتك باختلاف الخطر والسياق."),
            Triple(setOf("approval", "authenticity"), "بين الانتماء والوفاء لصوتك", "يبدو أن القبول وصوتك الشخصي حاضران في بعض اختياراتك. راقب المواقف التي تستطيع فيها الجمع بينهما، وتلك التي تدفعك إلى محو أحدهما."),
            Triple(setOf("self_criticism", "self_compassion"), "بين المحاسبة والرحمة", "قد تطلب من نفسك أن تتحمل المسؤولية، وفي الوقت نفسه تحتاج إلى ألا يتحول الخطأ إلى حكمٍ على قيمتك كلها."),
            Triple(setOf("attachment", "boundaries"), "بين القرب والحدود", "تظهر أهمية العلاقة، وتظهر معها الحاجة إلى مساحةٍ تحفظك. القرب الصحي لا يلغي الحدود، والحدود لا تعني بالضرورة الانسحاب."),
            Triple(setOf("fear", "agency"), "بين الخوف والقدرة على الفعل", "وجود الخوف لا ينفي قدرتك على المبادرة؛ ما يستحق الفحص هو مقدار ما يوجّه القرار، ومقدار ما يبقى لك من اختيار.")
        )
        val tension = tensions.firstOrNull { (pair, _, _) -> pair.all { (tags[it] ?: 0) > 0 } }
        val latest = path.lastOrNull()
        val latestEvidence = latest?.let { (question, answer) ->
            "في أحدث اختيارٍ مسجل، عند سؤال «${question.heading}»، اخترت «${answer.title}». هذا يصف استجابةً في موقفٍ محدد، لا هويتك كلها."
        }.orEmpty()
        if (tension != null) return "${tension.second}: ${tension.third}\n\n$latestEvidence"

        val strongestPattern = listOf(
            setOf("control", "certainty", "safety", "perfectionism") to "قد يمنحك الاستعداد شعورًا بالأمان والقدرة، لكن لا توجد خطةٌ تضمن كل شيء. راقب متى يخدمك التنظيم ومتى يؤجل التجربة.",
            setOf("honesty", "courage", "self_awareness") to "تظهر إشارات إلى الاستعداد لمراجعة النفس. حافظ على الصراحة، لكن لا تجعلها اسمًا آخر للعقاب؛ الفهم الأجدى يفتح بابًا لفعلٍ مختلف.",
            setOf("meaning", "growth", "agency", "curiosity") to "تظهر رغبةٌ في أن يحمل التغيير معنى. اختبرها بخطوةٍ صغيرة يمكن ملاحظتها، بدل أن تظل الفكرة وعدًا جميلًا للمستقبل.",
            setOf("attachment", "belonging", "approval") to "تبدو العلاقة بالآخرين مهمةً في بعض قراراتك. السؤال المفتوح هو كيف تحمي القرب دون أن تجعل احتياجاتك غير مرئية.",
            setOf("avoidance", "self_protection", "fear", "restlessness") to "قد تكون المسافة أحيانًا وسيلةً لاستعادة التوازن، وقد تتحول أحيانًا إلى تأجيل. لا تحكم عليها من شكلها؛ افحص أثرها بعد أن تهدأ اللحظة."
        ).maxByOrNull { (group, _) -> group.sumOf { tags[it] ?: 0 } }
        val body = strongestPattern?.second
            ?: "لا يظهر خيطٌ واحدٌ يفسّر إجاباتك كلها. هذا ليس نقصًا في الصورة؛ قد يكون دليلًا على أن استجاباتك تتبدل بحسب السياق، وأن اختصارك في سمة واحدة سيكون مضللًا."
        return "$body\n\n$latestEvidence"
    }

    fun unmasking(answers: Map<String, String>): Pair<String, String> {
        val answered = activePath(answers)
        if (answered.size < 3) return "الأدلة لم تكتمل بعد" to "أجب عن ثلاثة أسئلة على الأقل قبل أن نحاول وصل الخيوط. لا تستحق نفسك حكمًا متعجلًا."
        val tags = tagCounts(answers)
        val tensions = listOf(
            Triple(setOf("control", "acceptance"), "بين السيطرة والتقبّل", "تجمع إجاباتك إشارات إلى ترتيب ما يمكنك التأثير فيه وإلى قبول ما يخرج عن قدرتك. قد يكون لكل استجابة موضعها؛ افحص الظروف التي تستدعي كل واحدة."),
            Triple(setOf("avoidance", "courage"), "بين الاقتراب والابتعاد", "تظهر في المسار المختار إشارات إلى المواجهة وأخرى إلى التأجيل أو طلب المسافة. هذا ليس تناقضًا مثبتًا؛ قد تكون استجابتك حساسةً للسياق الذي واجهته."),
            Triple(setOf("approval", "authenticity"), "بين صوتك وصدى الآخرين", "تظهر أهمية نظرة الآخرين إلى جانب إشاراتٍ إلى رغبتك في الوفاء لنفسك. ابحث عن المواقف التي يتعايش فيها الأمران، لا عن وجهٍ واحدٍ يلغي الآخر."),
            Triple(setOf("attachment", "boundaries"), "بين حفظ العلاقة وحفظ المساحة", "تظهر إشارات إلى أهمية القرب وإلى الحاجة للحدود. يستحق الأمر أن تسأل أين يكون التنازل اختيارًا حرًا، وأين يصبح ثمنًا لا يُقال."),
            Triple(setOf("self_criticism", "self_compassion"), "بين المسؤولية والقسوة", "تظهر إشارات إلى المحاسبة وإلى إمكان معاملة النفس برحمة. تحمّل مسؤولية الفعل لا يتطلب أن تجعل الخطأ تعريفًا كاملًا لك.")
        )
        val tension = tensions.firstOrNull { (pair, _, _) -> pair.all { (tags[it] ?: 0) > 0 } }
        val evidence = answered.lastOrNull()?.let { (q, a) -> "ومن أحدث الأدلة اختيارك في «${q.heading}»: «${a.title}»." }.orEmpty()
        if (tension != null) return tension.second to "${tension.third}\n\n$evidence\nهذه قراءةٌ مؤقتة؛ راجعها في ضوء السياق والأدلة المضادة."

        val strongest = tags.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).firstOrNull()?.key
        val fallback = when (strongest) {
            "control", "certainty", "safety" -> "حين يصبح الترتيب وسيلةً للأمان" to "تكرر في بعض اختياراتك البحث عن الوضوح أو استعادة زمام الأمور. قد يكون ذلك مهارة نافعة، وقد يرهقك حين تطلب من الحياة ضمانات لا تستطيع تقديمها."
            "approval", "judgment", "perfectionism" -> "العين التي تبقى بعد غياب الآخرين" to "تظهر في إجاباتك إشارات إلى حضور نظرة الآخرين أو المعيار المرتفع. حدّد أين تساعدك المراجعة وأين تحرمك من التجربة."
            "avoidance", "self_protection", "fear" -> "المسافة التي تحمي أو تؤجل" to "تظهر إشارات إلى طلب المسافة في بعض المواقف. قد تكون حكمةً أو تأجيلًا؛ ما يحدد معناها هو أثرها وسياقها، لا الاسم وحده."
            "honesty", "courage", "self_compassion" -> "الصراحة مع مساحةٍ للرحمة" to "تظهر بعض الاختيارات استعدادًا للنظر إلى النفس؛ حافظ على هذه الشجاعة دون أن تجعلها محاكمةً لا تنتهي."
            "attachment", "belonging" -> "العلاقة التي تريد أن تحافظ عليها" to "تبدو العلاقة بالآخرين حاضرةً في بعض قراراتك. هل تحميها لأنك تختارها، أم لأن خسارتها تبدو أصعب من خسارة صوتك؟"
            "meaning", "growth", "agency" -> "التغيير الذي يحتاج إلى معنى" to "تظهر إشارات إلى الرغبة في أن يكون للتغيير اتجاه. ما الخطوة الصغيرة التي تنقل هذه الرغبة من الفكرة إلى الممارسة؟"
            else -> "خيطٌ يستحق الانتباه" to reflectionFor(answers)
        }
        return fallback.first to "${fallback.second}\n\n$evidence"
    }

    /** The inner court uses the user's active evidence to choose the tension worth examining. */
    fun innerCourtLens(answers: Map<String, String>): Pair<String, String> {
        val path = activePath(answers)
        if (path.isEmpty()) return "لا قضية دون أدلة" to "ابدأ من إجابةٍ واحدة على الأقل. ستبدأ المحكمة بسؤالٍ لا بحكم، ثم تضع النية والأثر والسياق على الطاولة معًا."
        val tags = tagCounts(answers)
        val strongest = tags.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).firstOrNull()?.key
        val latest = path.last()
        val line = "آخر اختيارٍ في مسارك كان «${latest.second.title}»، ضمن سؤال «${latest.first.heading}»."
        val insight = when (strongest) {
            "approval", "belonging", "attachment" -> "القضية المقترحة: متى يصبح الحفاظ على العلاقة تنازلًا عن حاجةٍ مشروعة؟ افصل بين ما تعرفه عن الطرف الآخر وما تخشاه، وبين أثر قرارك على كلٍّ منكما."
            "control", "certainty", "perfectionism" -> "القضية المقترحة: هل يخدم الاستعداد القرار أم يؤجل احتمال الخطأ؟ اطلب دليلًا على الخطر، وحدّد ما لا يمكن ضمانه مهما خططت."
            "avoidance", "fear", "self_protection" -> "القضية المقترحة: هل تمنحك المسافة وقتًا للفهم أم تبقي المسألة معلّقة؟ قارن بين أثر التوقف الآن وأثره إن طال."
            "honesty", "responsibility", "courage" -> "القضية المقترحة: كيف تقول الحقيقة وتتحمل أثرها دون أن تختزل الشخص في خطئه؟ ضع الإصلاح والحدود والرحمة في ميزانٍ واحد."
            "meaning", "growth", "agency", "change" -> "القضية المقترحة: ما الدليل الذي يحوّل رغبتك في التغيير إلى خطوة قابلة للملاحظة؟ ميّز بين نيةٍ صادقة وخطةٍ يمكن اختبارها."
            else -> "القضية المقترحة: ما الذي تعرفه، وما الذي تفترضه، وما المعلومة التي ستغيّر حكمك؟ اترك مساحةً لما لم تكشفه الإجابات بعد."
        }
        return "من إجاباتك إلى طاولة المحكمة" to "$line\n\n$insight"
    }

    /** A counterfactual prompt tied to the strongest current pattern, not a fixed personality label. */
    fun alternateSelfReflection(answers: Map<String, String>): String {
        val path = activePath(answers)
        if (path.size < 2) return "لم تتشكل أدلة كافية بعد. خذ وقتك مع الأسئلة؛ لا تحتاج إلى استعجال صورةٍ عن ذاتك الأخرى."
        val tags = tagCounts(answers)
        val latest = path.last().second.title
        val strongest = tags.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).firstOrNull()?.key
        val counterfactual = when (strongest) {
            "avoidance", "fear", "self_protection" -> "تخيّل أنك لم تُجبر نفسك على قفزةٍ كبيرة، بل اخترت خطوةً صغيرة لا تحتاج إلى زوال الخوف. ماذا ستختبر؟"
            "control", "certainty", "perfectionism" -> "تخيّل أنك سمحت بنسبةٍ صغيرة من عدم اليقين، مع الاحتفاظ بما تستطيع التخطيط له. أيّ فرصة ستصبح ممكنة؟"
            "approval", "belonging", "attachment" -> "تخيّل أنك قلت حاجتك بوضوحٍ من دون أن تطلب من الآخر الموافقة عليها فورًا. ما الذي ستتعلمه عن العلاقة؟"
            "self_criticism", "shame" -> "تخيّل أنك تحمل مسؤولية الفعل كاملًا، لكنك لا تحوّل الخطأ إلى اسمٍ لك. ما الخطوة الإصلاحية التي ستصبح أسهل؟"
            "meaning", "growth", "agency", "change" -> "تخيّل أنك اخترت فعلًا صغيرًا يعبّر عن القيمة التي تؤمن بها، بدل انتظار لحظةٍ مثالية. ما أول دليل على التغيير؟"
            else -> "تخيّل استجابةً مختلفة قليلًا عن المعتاد، لا نقيضًا كاملًا له. أيّ نتيجة تريد اختبارها، وما الدليل الذي سيخبرك إن كانت نافعة؟"
        }
        return "في آخر اختيارٍ لك اخترت «$latest». لا يعني ذلك أنك تختار هكذا دائمًا؛ إنه موضعٌ واحد يمكن أن نختبر حوله احتمالًا آخر.\n\n$counterfactual"
    }

    /** Compare the first and latest active response without pretending they prove personal change. */
    fun echoReflection(answers: Map<String, String>): String {
        val path = activePath(answers)
        if (path.size < 2) return "حين تتراكم إجابتان أو أكثر، يمكننا وضع البدايات والاختيارات الأحدث جنبًا إلى جنب. لن نفترض أنك تغيّرت؛ سنسأل عمّا تغيّر في الموقف أو في نظرتك إليه."
        val first = path.first()
        val last = path.last()
        val overlap = first.second.tags intersect last.second.tags
        return when {
            overlap.isNotEmpty() -> "بين اختيارك الأول «${first.second.title}» واختيارك الأحدث «${last.second.title}» خيطٌ مشترك هو ${overlap.joinToString("، ") { it.replace('_', ' ') }}. قد يدل ذلك على استمرارية، أو على أن الموقفين يستدعيان حاجةً متشابهة. ما السياق الذي يفسّر هذا التشابه؟"
            else -> "يختلف اختيارك الأول «${first.second.title}» عن اختيارك الأحدث «${last.second.title}» في الخيوط التي يحملها. لا يثبت الاختلاف أنك تغيّرت؛ ربما اختلف الموقف أو ما كان متاحًا لك من بدائل. ما الذي تغيّر بين اللحظتين؟"
        }
    }

    private fun q(id: String, heading: String, prompt: String, vararg options: AnswerOption) =
        TrialQuestion(id, heading, prompt, options.toList())

    private fun a(id: String, title: String, detail: String, tags: Set<String>, next: String?) =
        AnswerOption(id, title, detail, tags, next)
}
