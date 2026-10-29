package com.aram.mahakama.domain

/**
 * Builds the extended question bank from a structured matrix of 12 psychological domains,
 * 12 lived contexts, and 7 reflective lenses. Every retained combination yields a distinct prompt;
 * context-specific response detail, tags, and forward edges are deterministic (never random).
 */
internal object QuestionBankBuilder {
    private data class Domain(
        val heading: String,
        val focus: String,
        val activeTag: String,
        val reflectiveTag: String,
        val protectiveTag: String,
        val responses: List<ResponseStyle>
    )

    private data class ResponseStyle(
        val title: String,
        val detailLead: String,
        val tags: Set<String>
    )

    private val contexts = listOf(
        "يتغيّر اتفاق كنت تعتمد عليه",
        "يتأخر ردّ تنتظره من شخص مهم",
        "يُطلب منك قرار قبل أن تشعر بالاستعداد",
        "لا يُقدَّر جهدك كما كنت تتوقع",
        "تلاحظ اختلافًا بين ما تقوله وما تفعله",
        "تضطر إلى الاعتذار عن خطأ ارتكبته",
        "تسبق توقعات الآخرين رغبتك الشخصية",
        "تُمنح فرصةً تحمل احتمال الفشل",
        "يبتعد عن حياتك شيء ألفته طويلًا",
        "يواجهك صمت لا تستطيع ملأه بسهولة",
        "تتكرر عادة وعدت نفسك بتغييرها",
        "يتعارض خيار مريح مع قيمة تؤمن بها"
    )

    private val contextualResponseDetails = listOf(
        listOf(
            "اسأل عمّا بقي ثابتًا ويمكن البناء عليه بدل محاولة استعادة الاتفاق القديم كما كان.",
            "لا تجعل التغيّر وحده دليلًا على أن كل ما سبق فقد قيمته.",
            "قد يمنحك التمسك بالمألوف ثباتًا مؤقتًا، لكنه لا يعيد الاتفاق الذي تبدّل."
        ),
        listOf(
            "يمكنك طلب الوضوح مباشرةً بدل ترك الصمت يكتب تفسيره وحده.",
            "ميّز بين تأخر الرد وما تعرفه فعلًا عن نية الطرف الآخر.",
            "قد تحميك المسافة مؤقتًا، لكنها لا تكشف وحدها ما حدث."
        ),
        listOf(
            "قسّم القرار إلى خطوة قابلة للمراجعة بدل اشتراط صورة كاملة.",
            "سمِّ المعلومة الناقصة التي ستغيّر قرارك فعلًا، لا كل ما تتمنى معرفته.",
            "قد يمنحك الانتظار وقتًا، وقد يحوّل الاستعداد إلى شرطٍ لا يأتي."
        ),
        listOf(
            "سمِّ ما تحتاجه من تقدير أو وضوح بدل انتظار أن يقرأه الآخرون وحدهم.",
            "استجابة الآخرين ليست قياسًا كاملًا لقيمة الجهد الذي بذلته.",
            "خفض التوقعات قد يخفف الخيبة، لكنه لا يعالج غياب الإنصاف."
        ),
        listOf(
            "اختر سلوكًا صغيرًا يقرّب فعلك من القيمة التي تقول إنك تؤمن بها.",
            "افهم الظروف التي صنعت التناقض دون أن تستخدمها لإلغاء مسؤوليتك.",
            "تجنّب النظر إلى التناقض قد يخفف الانزعاج، لكنه يتركه بلا مراجعة."
        ),
        listOf(
            "تغدو الكلمات أصدق حين يتبعها إصلاح يمكن ملاحظته.",
            "أثر الفعل يستحق الإصغاء، حتى حين كانت نيتك مختلفة.",
            "المهلة مفيدة إذا قادت إلى مواجهة أصدق، لا إذا ألغت المسؤولية."
        ),
        listOf(
            "اعرف ما تريده أنت قبل أن توازنه مع ما ينتظره الآخرون.",
            "ليس كل ما يتوقعه الآخرون وعدًا قطعته أنت.",
            "قد يحفظ التوافق الهدوء، لكنه قد يجعل رغبتك غير مسموعة."
        ),
        listOf(
            "ابدأ بتجربة صغيرة تكشف حجم الخطر بدل تركه للخيال وحده.",
            "ميّز بين خطر يمكن إدارته ومخاوف لم يؤيدها دليل بعد.",
            "قد يكون الانسحاب تقديرًا حكيمًا للمخاطرة، أو تجنبًا لها؛ افحص ما دفعك إليه."
        ),
        listOf(
            "لا تحتاج إلى استعادة كل شيء كي تحتفظ بما علّمك إياه.",
            "صناعة المعنى لا تمحو الفقد، لكنها تمنعه من احتكار الحكاية.",
            "قد تمنحك المسافة وقتًا للحزن؛ لا تجعلها حكمًا بأن العودة مستحيلة."
        ),
        listOf(
            "ابدأ بسؤال واحد يمكنك احتماله بدل ملء الفراغ كله دفعة واحدة.",
            "لاحظ ما حدث فعلًا وما أضافته توقعاتك إلى الصمت.",
            "قد يريحك التشتيت مؤقتًا؛ يمكنك العودة إلى السؤال حين تستعد."
        ),
        listOf(
            "غيّر عنصرًا صغيرًا في محيط العادة واختبر أثره بدل مطالبة نفسك بتحول شامل.",
            "فهم وظيفة العادة يساعدك على بناء بديل يلبّي الحاجة نفسها.",
            "اللوم لا يوفّر بديلًا عند لحظة التكرار، وانتظار الدافع قد يطيل الدورة."
        ),
        listOf(
            "سمِّ القيمة والحد الذي لا تريد تجاوزه، ثم ابحث عن بديل قابل للتنفيذ.",
            "قد لا يكون الصراع ثنائيًا كما يبدو؛ افحص احتمالًا ثالثًا.",
            "لراحة اللحظة وزنها، لكن تأجيل القيمة باستمرار له ثمن."
        )
    )

    private data class Lens(val heading: String, val prompt: (String, String) -> String) 

    private val lenses = listOf(
        Lens("الاستجابة الأولى") { context, focus ->
            "حين $context، ما أول ما تفعله لتستعيد صلتك بـ$focus؟"
        },
        Lens("الحاجة الخفية") { context, focus ->
            "في موقفٍ مثل الذي يحدث حين $context، أيّ حاجةٍ تحاول تلبيتها في علاقتك بـ$focus؟"
        },
        Lens("ما تحاول حمايته") { context, focus ->
            "حين $context، ما الذي تحاول حمايته عبر طريقتك المعتادة في التعامل مع $focus؟"
        },
        Lens("الثمن المؤجَّل") { context, focus ->
            "إذا واجهت موقفًا تتذكره حين $context بالطريقة نفسها كل مرة، فما الثمن الذي قد تدفعه في علاقتك بـ$focus؟"
        },
        Lens("الدليل المضاد") { context, focus ->
            "حين $context، ما الدليل الذي قد يجعلك تراجع تفسيرك بدل أن تتمسك بأول انطباع عن $focus؟"
        },
        Lens("الاختيار الممكن") { context, focus ->
            "إذا عاد موقفٌ يشبه لحظةً حين $context، أيّ استجابةٍ تقرّبك من الشخص الذي تريد أن تكونه في علاقتك بـ$focus؟"
        },
        Lens("الحاجة التي لا تُقال") { context, focus ->
            "في قلب موقفٍ يبدأ حين $context، ما الحاجة المرتبطة بـ$focus التي يصعب عليك الاعتراف بها أو طلبها بوضوح؟"
        }
    )

    private val domains = listOf(
        Domain(
            "اليقين والسيطرة", "حاجتك إلى الوضوح", "control", "reflection", "avoidance",
            listOf(
                ResponseStyle("أحوّل الغموض إلى خطوةٍ واضحة", "قد تمنحك الخطة شعورًا بالفاعلية، لكنها لا تستطيع ضمان كل نتيجة.", setOf("control", "agency", "certainty")),
                ResponseStyle("أفصل ما أعرفه عمّا أتوقعه", "يمنحك التمهّل فرصةً لرؤية الفرق بين الوقائع ومخاوفك عنها.", setOf("reflection", "nuance", "acceptance")),
                ResponseStyle("أؤجل الحسم حتى أشعر بأمانٍ أكبر", "قد تحميك المسافة من قرارٍ متسرّع، وقد تُبقيك معلّقًا إذا طال انتظار اليقين.", setOf("avoidance", "self_protection", "uncertainty"))
            )
        ),
        Domain(
            "الصورة والحقيقة", "صورتك عن نفسك", "self_image", "honesty", "self_protection",
            listOf(
                ResponseStyle("أواجه ما ظهر حتى لو هزّ صورتي", "المواجهة تفتح باب التغيير حين تفصل بين الاعتراف بالخطأ وإلغاء قيمة الذات.", setOf("honesty", "courage", "self_awareness")),
                ResponseStyle("أبحث عن السياق قبل أن أصف نفسي", "السياق يحميك من حكمٍ متسرّع، ما دام لا يتحول إلى عذرٍ دائم.", setOf("reflection", "nuance", "curiosity")),
                ResponseStyle("أحتمي بالصورة التي أعرفها عن نفسي", "قد تمنحك الصورة المألوفة ثباتًا مؤقتًا، لكنها قد تجعل المراجعة أكثر كلفة.", setOf("self_protection", "self_image", "shame"))
            )
        ),
        Domain(
            "القرب والحدود", "القرب من الآخرين", "attachment", "empathy", "conflict_avoidance",
            listOf(
                ResponseStyle("أقول ما أحتاجه بوضوحٍ واحترام", "التعبير الصريح يمنح العلاقة فرصةً عادلة، حتى إن لم يضمن النتيجة التي تتمناها.", setOf("boundaries", "vulnerability", "authenticity")),
                ResponseStyle("أحاول فهم ما وراء الموقف أولًا", "الفهم قد يخفف سوء التأويل، لكن احتياجاتك تظل جديرة بأن تُسمَع.", setOf("empathy", "reflection", "attachment")),
                ResponseStyle("أصمت أو أبتعد كي لا يتسع الخلاف", "الابتعاد قد يمنح الطرفين وقتًا، وقد يجعل المشكلة تتحدث بدلًا منك.", setOf("conflict_avoidance", "avoidance", "self_protection"))
            )
        ),
        Domain(
            "القبول والانتماء", "حاجتك إلى القبول", "approval", "belonging", "authenticity",
            listOf(
                ResponseStyle("أحافظ على موقفي حتى مع الاختلاف", "قد يصون ذلك صلتك بقيمك، ويحتاج أيضًا إلى الإصغاء لما لا تراه من وجهة الآخر.", setOf("authenticity", "values", "courage")),
                ResponseStyle("أبحث عن صيغة تحفظ العلاقة والرأي", "محاولة الجمع بين القرب والاختلاف ليست ضعفًا؛ التحدي ألا تختفي حاجتك في المنتصف.", setOf("belonging", "nuance", "empathy")),
                ResponseStyle("أساير التوقع كي لا أخسر مكاني", "قد يحفظ التوافق هدوء اللحظة، لكنه يستحق سؤالًا عمّا يبقى من صوتك بعده.", setOf("approval", "conflict_avoidance", "attachment"))
            )
        ),
        Domain(
            "الفقد والتعلّق", "ما تخشى فقده", "attachment", "resilience", "self_protection",
            listOf(
                ResponseStyle("أتمسّك بما يمكن إصلاحه", "المثابرة قد تحفظ شيئًا ثمينًا، إذا تركت مساحةً أيضًا لاحتمال أن النهاية تغيّرت.", setOf("persistence", "attachment", "hope")),
                ResponseStyle("أبحث عن معنى يبقى بعد التغيّر", "صناعة المعنى لا تمحو الخسارة، لكنها قد تمنعها من احتكار الحكاية كلها.", setOf("meaning", "resilience", "acceptance")),
                ResponseStyle("أبتعد قليلًا كي أحتمل الشعور", "المسافة قد تكون رفقًا بالنفس، وتصبح عبئًا حين تمنعك من العودة إلى ما يهمك.", setOf("self_protection", "avoidance", "grief"))
            )
        ),
        Domain(
            "الخوف والشجاعة", "علاقتك بالخوف", "fear", "courage", "avoidance",
            listOf(
                ResponseStyle("أتحرك رغم بقاء الخوف", "الشجاعة ليست غياب الارتباك، بل اختيار خطوةٍ لها معنى مع الاعتراف بالمخاطر.", setOf("courage", "agency", "growth")),
                ResponseStyle("أفحص ما يخبرني به الخوف", "قد يحمل الخوف تحذيرًا مفيدًا أو توقعًا مبالغًا فيه؛ التمييز بينهما يحتاج إلى دليل.", setOf("reflection", "prudence", "nuance")),
                ResponseStyle("أنتظر حتى تخفّ رهبة الخطوة", "الانتظار قد يساعد على الاستعداد، لكنه لا يجعل الخوف يختفي دائمًا من تلقاء نفسه.", setOf("avoidance", "fear", "uncertainty"))
            )
        ),
        Domain(
            "النجاح والمعيار", "صورتك عن النجاح", "ambition", "growth", "perfectionism",
            listOf(
                ResponseStyle("أضع معيارًا واضحًا وأبدأ", "المعيار يوجّه الجهد حين يساعدك على التقدم، لا حين يطلب منك ألا تتعثر أبدًا.", setOf("ambition", "agency", "discipline")),
                ResponseStyle("أراجع معنى النجاح الذي أطارده", "السؤال عن المعنى قد يحميك من بلوغ غايةٍ لم تعد تشبهك.", setOf("growth", "meaning", "reflection")),
                ResponseStyle("أؤجل المحاولة حتى أضمن جودة النتيجة", "الرغبة في الإتقان قد ترفع مستوى العمل، وقد تحرمك من التعلم الذي لا يأتي إلا بالتجربة.", setOf("perfectionism", "judgment", "fear"))
            )
        ),
        Domain(
            "الذنب والمسؤولية", "ما تحمله من مسؤولية", "responsibility", "self_compassion", "shame",
            listOf(
                ResponseStyle("أعترف بنصيبي وأصلح ما أستطيع", "المسؤولية تصبح بنّاءة حين تتجه إلى الإصلاح بدل أن تتحول إلى عقوبةٍ مفتوحة.", setOf("responsibility", "accountability", "agency")),
                ResponseStyle("أميّز بين خطئي وما لم أكن أملكه", "الإنصاف للنفس لا يمحو أثر الفعل؛ إنه يحدد بدقة ما يقع ضمن مسؤوليتك.", setOf("self_compassion", "nuance", "clarity")),
                ResponseStyle("أحمل اللوم حتى بعد انتهاء الموقف", "قد يبدو اللوم دليلًا على الاهتمام، لكنه لا يصلح وحده ما حدث ولا يضمن ألّا يتكرر.", setOf("shame", "self_criticism", "perfectionism"))
            )
        ),
        Domain(
            "الغضب والجرح", "الغضب الذي يظهر عند الأذى", "anger", "boundaries", "resentment",
            listOf(
                ResponseStyle("أسمّي الأذى وأضع حدًا واضحًا", "الحدّ الواضح يحمي كرامتك حين يصف السلوك المطلوب تغييره بدل أن يختزل الشخص كله.", setOf("boundaries", "agency", "honesty")),
                ResponseStyle("أحاول فهم الجرح قبل الرد", "فهم ما أصابك قد يمنحك اختيارًا أوسع من ردّ الفعل، دون أن يلزمك بتبرير الأذى.", setOf("reflection", "empathy", "nuance")),
                ResponseStyle("أكتم الغضب ثم أبتعد", "الكتمان قد يؤجل انفجار اللحظة، لكنه قد يترك الاحتياج والحدود بلا صوت.", setOf("resentment", "avoidance", "conflict_avoidance"))
            )
        ),
        Domain(
            "العزلة والمعنى", "المعنى الذي تبنيه", "meaning", "curiosity", "restlessness",
            listOf(
                ResponseStyle("أختار فعلًا يمنح يومي معنى", "الفعل الصغير قد يربط الفكرة بالحياة حتى عندما لا تصل الإجابات الكبرى.", setOf("meaning", "agency", "authenticity")),
                ResponseStyle("أمنح الصمت فرصةً كي أفهم نفسي", "الهدوء قد يكشف ما يغطيه الضجيج، إذا بقي مساحةً للإصغاء لا للهروب فقط.", setOf("curiosity", "self_awareness", "reflection")),
                ResponseStyle("أملأ الفراغ كي لا أواجه أسئلته", "الانشغال قد يريحك مؤقتًا، لكنه لا يخبرك إن كان الفراغ يحتاج راحةً أم اتجاهًا.", setOf("restlessness", "avoidance", "uncertainty"))
            )
        ),
        Domain(
            "التغيير والعادة", "علاقتك بالتغيير", "change", "agency", "avoidance",
            listOf(
                ResponseStyle("أحوّل الرغبة إلى خطوةٍ صغيرة قابلة للقياس", "الخطوة المحددة تقلل المسافة بين النية والفعل، وتسمح لك بمراجعة ما ينجح.", setOf("change", "agency", "discipline")),
                ResponseStyle("أبحث عن الحاجة التي تخدمها العادة", "فهم وظيفة العادة يفتح بدائل واقعية بدل الاكتفاء بلوم النفس على تكرارها.", setOf("reflection", "curiosity", "self_awareness")),
                ResponseStyle("أنتظر أن تتغير رغبتي قبل أن أبدأ", "انتظار الاستعداد الكامل قد يطيل الوقوف؛ أحيانًا تُبنى الرغبة بعد أول خطوة.", setOf("avoidance", "procrastination", "uncertainty"))
            )
        ),
        Domain(
            "القيم والتنازل", "القيم التي تريد العيش بها", "values", "courage", "belonging",
            listOf(
                ResponseStyle("أختار ما أؤمن به وأقبل ثمنه بوعي", "القيمة تمنح القرار اتجاهًا، لكنها لا تعفيك من فحص أثره على الآخرين.", setOf("values", "courage", "integrity")),
                ResponseStyle("أبحث عن بديل يقلل الخسارة على الجميع", "البحث عن بديل ثالث قد يحفظ أكثر من قيمة، إن لم يتحول إلى تأجيلٍ لا ينتهي.", setOf("nuance", "empathy", "reflection")),
                ResponseStyle("أقدّم الانسجام كي لا أخسر العلاقة", "حماية العلاقة قد تكون اختيارًا واعيًا، لكن من المهم معرفة ما إذا كان التنازل متبادلًا.", setOf("belonging", "approval", "attachment"))
            )
        )
    )

    fun build(): List<TrialQuestion> {
        val authoredCombinations = buildList {
            // Interleave domains so a straight-through path does not spend dozens of questions
            // repeating one theme. All combinations remain deterministic and answerable.
            contexts.forEachIndexed { contextIndex, context ->
                lenses.forEachIndexed lensLoop@ { lensIndex, lens ->
                    domains.forEachIndexed domainLoop@ { _, domain ->
                        // Omit two fixed lens/context slots across every domain to keep the
                        // extended bank at 984 nodes, yielding exactly 1,000 with the foundation set.
                        if ((contextIndex == 5 || contextIndex == 11) && lensIndex == 6) {
                            return@domainLoop
                        }
                        add(Triple(domain, context, lens))
                    }
                }
            }
        }
        check(contextualResponseDetails.size == contexts.size && contextualResponseDetails.all { it.size == 3 }) {
            "Every lived context must define three context-sensitive response details."
        }
        check(authoredCombinations.size == EXTENDED_QUESTION_COUNT) {
            "Question matrix must produce exactly $EXTENDED_QUESTION_COUNT questions; got ${authoredCombinations.size}"
        }
        return authoredCombinations.mapIndexed { index, (domain, context, lens) ->
            val contextIndex = contexts.indexOf(context)
            require(contextIndex >= 0) { "Unknown question context: $context" }
            val currentId = questionId(index)
            // The first option preserves a complete forward chain, making every node reachable.
            // The other two route by answer tags toward a later domain/context with related evidence.
            val next1Index = (index + 1).takeIf { it <= authoredCombinations.lastIndex }
            val style2Tags = domain.responses[1].tags + domain.reflectiveTag
            val next2Index = semanticTargetIndex(
                combinations = authoredCombinations,
                currentIndex = index,
                preferredTags = style2Tags,
                excludedIndices = setOfNotNull(next1Index),
                fallbackOffset = 2
            )
            val style3Tags = domain.responses[2].tags + domain.protectiveTag
            val next3Index = semanticTargetIndex(
                combinations = authoredCombinations,
                currentIndex = index,
                preferredTags = style3Tags,
                excludedIndices = setOfNotNull(next1Index, next2Index),
                fallbackOffset = 3
            )
            val optionStyles = domain.responses
            TrialQuestion(
                id = currentId,
                heading = "${domain.heading} · ${lens.heading}",
                prompt = lens.prompt(context, domain.focus),
                options = listOf(
                    answer(currentId, 1, optionStyles[0], contextualResponseDetails[contextIndex][0], domain.activeTag, next1Index?.let(::questionId)),
                    answer(currentId, 2, optionStyles[1], contextualResponseDetails[contextIndex][1], domain.reflectiveTag, next2Index?.let(::questionId)),
                    answer(currentId, 3, optionStyles[2], contextualResponseDetails[contextIndex][2], domain.protectiveTag, next3Index?.let(::questionId))
                )
            )
        }
    }

    /**
     * Selects a future question whose domain tags and response vocabulary overlap the chosen
     * answer. Ties prefer the nearer node; a sequential fallback keeps the graph usable near
     * sparse thematic matches. All edges move forward, so the question path cannot cycle.
     */
    private fun semanticTargetIndex(
        combinations: List<Triple<Domain, String, Lens>>,
        currentIndex: Int,
        preferredTags: Set<String>,
        excludedIndices: Set<Int>,
        fallbackOffset: Int
    ): Int? {
        val lastIndex = combinations.lastIndex
        if (currentIndex >= lastIndex) return null
        val sourceContext = combinations[currentIndex].second
        val sourceLens = combinations[currentIndex].third.heading
        val allFutureIndices = (currentIndex + 1..lastIndex).toList()
        val futureIndices = allFutureIndices.filterNot { it in excludedIndices }
        // Near the final node there may be fewer unique targets than answer options. Reuse a
        // valid future node instead of making a non-final choice terminate the session early.
        if (futureIndices.isEmpty()) return allFutureIndices.firstOrNull()

        fun score(index: Int): Int {
            val (domain, context, lens) = combinations[index]
            val domainTags = setOf(domain.activeTag, domain.reflectiveTag, domain.protectiveTag)
            val responseTags = domain.responses.flatMap { it.tags }.toSet()
            val domainOverlap = preferredTags.intersect(domainTags).size
            val responseOverlap = preferredTags.intersect(responseTags).size
            val contextBonus = if (context != sourceContext) 2 else 0
            val lensBonus = if (lens.heading != sourceLens) 1 else 0
            return domainOverlap * 100 + responseOverlap * 10 + contextBonus + lensBonus
        }

        val best = futureIndices.maxWithOrNull(
            compareBy<Int> { score(it) }.thenBy { -it }
        )
        if (best != null && score(best) > 0) return best
        val fallback = (currentIndex + fallbackOffset).takeIf { it <= lastIndex && it !in excludedIndices }
        return fallback ?: futureIndices.first()
    }

    private fun answer(
        questionId: String,
        choice: Int,
        style: ResponseStyle,
        contextualDetail: String,
        domainTag: String,
        nextQuestionId: String?
    ): AnswerOption = AnswerOption(
        id = "${questionId}_a$choice",
        title = style.title,
        detail = "${style.detailLead} $contextualDetail",
        tags = style.tags + domainTag,
        nextQuestionId = nextQuestionId
    )

    private fun questionId(index: Int): String = "bank_${(index + 1).toString().padStart(3, '0')}"

    const val EXTENDED_QUESTION_COUNT = 984
}
