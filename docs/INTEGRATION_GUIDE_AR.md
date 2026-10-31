# دليل دمج حزمة التحديث القادم

## 0. نقطة الانطلاق

هذه التعليمات مبنية على نسخة المشروع عند:

- Branch: `review/mahakama-local-20261031`
- Commit: `7d80296d98b5a615ef40c3b5812cd2908241ee5a`
- Package: `com.aram.mahakama`
- ملفات الارتكاز التي تمت مراجعتها: `MainActivity.kt`, `AppScreens.kt`, `QuestionnaireEngine.kt`, `SpiritMemoryEngine.kt`, `PreferencesRepository.kt`.

أنشئ فرعًا مستقلًا من Commit المراجعة. لا تستخدم `main` أساسًا إذا لم يكن يحتوي على آخر التعديلات، ولا تُجرِ دمجًا تلقائيًا.

## 1. نسخ ملفات المصدر

انسخ مجلد `app/src/main/java/com/aram/mahakama/nextupdate/` كاملًا إلى المشروع بنفس المسارات. أضف اختبار `NextUpdateEnginesTest.kt` إلى `app/src/test/java/com/aram/mahakama/nextupdate/`.

لا توجد إضافة dependency جديدة مطلوبة لهذه الملفات؛ المشروع الحالي يحتوي Compose وMaterial 3 وDataStore وJUnit وAndroid `org.json`.

## 2. ربط الشاشات في MainActivity.kt

أضف imports:

```kotlin
import com.aram.mahakama.nextupdate.data.NextUpdateEventCodec
import com.aram.mahakama.nextupdate.domain.SpiritReflectionEngine
import com.aram.mahakama.nextupdate.domain.TrialEvolutionEngine
import com.aram.mahakama.nextupdate.ui.EnhancedInnerCourtScreen
import com.aram.mahakama.nextupdate.ui.FaceTreeScreen
import com.aram.mahakama.nextupdate.ui.TrialEvolutionScreen
```

في `when (visibleRoute)`:

```kotlin
"faces" -> FaceTreeScreen(
    userState = userState,
    onOpenEvolution = { route = "evolution" }
)

"evolution" -> TrialEvolutionScreen(userState)

"inner_court" -> EnhancedInnerCourtScreen(userState) { eventJson ->
    scope.launch { repository.appendSpiritMemoryEvent(eventJson) }
}
```

احذف فقط توجيه الشاشة القديم المقابل لكل من `faces` و`inner_court` حتى لا توجد حالتا route مكررتان. لا تحذف `FacesScreen` أو `InnerCourtScreen` من `AppScreens.kt` في هذه المرحلة؛ اتركهما كمرجع حتى ينجح البناء واختبار النسخة الجديدة.

## 3. إضافة سجل التطور إلى الصفحة الرئيسية

داخل قائمة `features` في `HomeScreen` أضف عنصرًا:

```kotlin
FeatureItem(
    "سجل تطور المحاكمة",
    "محطات إجاباتك وما تغيّر في الأدلة",
    Icons.Default.History,
    "evolution"
)
```

الأيقونة `History` موجودة بالفعل في imports الحالية لـ `AppScreens.kt`.

في `routeTitle` داخل `MainActivity.kt` أضف:

```kotlin
"evolution" -> "سجل تطور المحاكمة"
```

وفي `routeSubtitle` أضف:

```kotlin
"evolution" -> "محطات الرحلة وما تغيّر في صورتها"
```

## 4. منع تلوث ذاكرة طيف الروح بإجابات الفروع القديمة

هذه خطوة أساسية. `PreferencesRepository.replaceAnswers()` يحفظ المسار النشط الحالي، لكن سجل `spiritMemoryJson` يحتفظ تاريخيًا بأحداث الفروع السابقة. لذلك لا تمرر السجل الخام إلى محرك الانعكاس وكأنه كله ينتمي إلى المسار الحالي.

في `MainActivity.kt` داخل `onAnswer`، بعد إنشاء `nextMemory`، أنشئ سجلًا مؤقتًا مفلترًا:

```kotlin
val activeMemory = NextUpdateEventCodec.activeBranchMemory(
    raw = nextMemory,
    answers = nextAnswers
)
```

ثم استبدل حساب الانعكاس:

```kotlin
val insight = SpiritMemoryEngine.createInsight(activeMemory, nextAnswers)
val shouldAppear = next != null &&
    QuestionnaireEngine.nextQuestion(nextAnswers) != null &&
    (
        QuestionnaireEngine.shouldRevealSpirit(nextAnswers, sinceLastSpirit, count) ||
        SpiritMemoryEngine.shouldReveal(activeMemory)
    )
```

احتفظ بكتابة `answerEvent` إلى سجل DataStore الأصلي كما هي. `activeMemory` نسخة مؤقتة للتحليل فقط؛ لا تحفظها مكان السجل الأصلي، لأن ذلك سيمحو التاريخ السابق.

في `LaunchedEffect` الخاص بالعودة بعد الغياب، قبل استدعاء `returnAfterAbsence` استخدم:

```kotlin
val activeMemory = NextUpdateEventCodec.activeBranchMemory(
    raw = userState.spiritMemoryJson,
    answers = userState.answers
)
val returnInsight = SpiritMemoryEngine.returnAfterAbsence(activeMemory)
```

### إثراء دليل طيف الروح دون استبدال محرك الحوار الحالي

عندما تقرر إظهار الطيف، احتفظ بالـ `line` و`followUp` الناتجين من `SpiritMemoryEngine` للحفاظ على السلوك الحواري الحالي، وأضف دليلًا مستقلًا من المحرك الجديد:

```kotlin
val transparentReflection = SpiritReflectionEngine.create(nextAnswers)
spiritEvidence = buildString {
    append(insight.evidence)
    append("\n")
    append(transparentReflection.evidenceSummary)
    append("\n")
    append(transparentReflection.limitation)
}
```

يمكن اعتماد `SpiritReflectionEngine.line` و`followUp` بدل المخرجات الحالية في مرحلة لاحقة، بعد مراجعة تكرار الصياغات مع المستخدم. لا تغيّر صوت الطيف أو توقيت ظهوره أو آلية إغلاقه ضمن هذا الدمج.

## 5. حفظ لقطات تطور الملف

في `onAnswer`، بعد حساب `activeMemory`، أضف:

```kotlin
val previousSnapshot = NextUpdateEventCodec.latestProfileSnapshot(activeMemory)
val currentSnapshot = TrialEvolutionEngine.snapshot(nextAnswers)
val snapshotEvent = if (
    TrialEvolutionEngine.shouldRecordSnapshot(previousSnapshot, currentSnapshot)
) {
    val change = TrialEvolutionEngine.compare(previousSnapshot, currentSnapshot)
    NextUpdateEventCodec.encodeProfileSnapshot(
        currentSnapshot,
        TrialEvolutionEngine.evolutionSummary(change)
    )
} else null
```

داخل `scope.launch` الحالي، بعد حفظ `answerEvent`، أضف:

```kotlin
snapshotEvent?.let { repository.appendSpiritMemoryEvent(it) }
```

بهذا تُحفظ لقطة عند كل أربع إجابات نشطة أو عند تغير الوجوه الأبرز أو التوترات. لا تحفظ لقطة في كل recomposition أو عند كتابة مسودة نصية.

## 6. توسيع حد سجل الأحداث

في `PreferencesRepository.kt` يوجد حد ثابت حاليًا مقداره 500 حدث داخل `appendSpiritMemoryEvent`. استبدل الرقم الثابت بثابت موثق:

```kotlin
private const val MAX_PERSISTED_SPIRIT_EVENTS = 3000
```

ثم غيّر حساب بداية القص:

```kotlin
val start = (current.length() - MAX_PERSISTED_SPIRIT_EVENTS).coerceAtLeast(0)
```

السبب: قد تتضمن الرحلة ما يصل إلى 1000 إجابة، بالإضافة إلى محطات التطور والظهور والحوارات ومحاضر المحكمة. حد 500 قد يزيل أجزاءً قديمة من السجل قبل انتهاء رحلة طويلة.

هذا تغيير في حد الاحتفاظ فقط؛ لا تغيّر صيغة `spiritMemoryJson` القديمة ولا تحذف الأحداث السابقة عند الترقية.

## 7. سياقات الأسئلة وجودة بنك الأسئلة

محرك التحليل في هذه الحزمة يصنف السياق اعتمادًا على tags الموجودة في الخيارات، وليس بحثًا عن كلمات عشوائية داخل نص الإجابة. هذا يقلل التخمين ويحافظ على التوافق مع بنك الأسئلة الحالي.

التحسين التحريري الأوسع المقترح للخطوة التالية:

- إضافة `contextId` اختياري إلى `TrialQuestion` بعد تحديد قاموس سياقات ثابت.
- تعبئة السياق في بنك الأسئلة عند إنشائه، لا عند تشغيل الواجهة.
- مراجعة كل سؤال من الأسئلة الألف للتأكد من وضوح السؤال، وتمايز الخيارات، وعدم تكرار المعنى، وارتباط tags بالسلوك المكتوب.
- إضافة اختبارات تضمن أن `nextQuestionId` صالح، وأن كل عقدة يمكن بلوغها أو مبرر استثناؤها، وأن المسارات لا تدخل حلقات.

لا تضف `contextId` إلى النموذج قبل وضع قاموس نهائي ومراجعة مولد `QuestionBankBuilder`؛ إدخال قيمة شكلية لكل سؤال لن يحل مشكلة السياق.

## 8. حفظ محاضر المحكمة

`EnhancedInnerCourtScreen` يعيد JSON event إلى callback. callback في `MainActivity.kt` يحفظ الحدث بواسطة `repository.appendSpiritMemoryEvent(eventJson)`. لا تنشئ DataStore منفصلًا للمحكمة.

يُحفظ لكل محضر: وقت الجلسة، معرف القضية، الموقف المختار، نوع الدليل الأهم، مستوى الثقة، المعلومة التي قد تغيّر الرأي، الانعكاس، ومعرفات الأسئلة التي استُخدمت كأدلة.

## 9. اختبارات ما بعد الدمج

نفّذ بالترتيب:

```bash
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon connectedDebugAndroidTest
```

ثم اختبر على هاتف حقيقي:

1. إجابة عدة أسئلة في فرع، ثم تغيير إجابة مبكرة والتأكد من أن الوجوه والطيف لا يستخدمان أدلة الفرع المتروك.
2. فتح كل وجه، ومراجعة الأدلة المؤيدة والمضادة، والضغط على وجه مرتبط للانتقال إليه.
3. فتح سجل التطور، والتأكد من تمييز الإجابة الحالية عن الإجابة القديمة.
4. إنشاء محضر محكمة، إغلاق التطبيق، فتحه، والتأكد من ظهور المحضر في السجل.
5. اختبار الاسم والوقت المتراكم وإعدادات الصوت كما كانت قبل التحديث.
6. تشغيل فيديو طيف الروح والأوركسترا على الهاتف، والتأكد من الصورة والصوت والعودة للخلفية.

## 10. حدود النطاق التي يجب عدم تجاوزها

- لا تغيّر ترتيب مقاطع الأوركسترا الثمانية.
- لا تغيّر موسيقى الأوركسترا أو ملفات الصوت أو مستوياتها أو سياسة كتمها دون موافقة.
- لا تحذف `spirit_specter.mp4` أو ملفات `res/raw`.
- لا تحوّل درجات تقاطع السمات إلى نسب احتمالية أو تشخيص.
- لا تستبدل ردود طيف الروح بنموذج خارجي أو API دون موافقة صريحة ومراجعة الخصوصية والتكلفة.
- لا تدمج فرع التحديث إلى `main` قبل نجاح الاختبارات ومراجعة الفروقات.
