
from pathlib import Path
from datetime import datetime
import shutil
import sys

ROOT = Path.home() / "Mahakama"

ENGINE = ROOT / "app/src/main/java/com/aram/mahakama/domain/SpiritMemoryEngine.kt"
SCREENS = ROOT / "app/src/main/java/com/aram/mahakama/ui/AppScreens.kt"
MAIN = ROOT / "app/src/main/java/com/aram/mahakama/MainActivity.kt"

def fail(message):
    print("STOP:", message)
    sys.exit(1)

def read_file(path):
    if not path.is_file():
        fail(f"الملف غير موجود: {path}")
    return path.read_text(encoding="utf-8")

def replace_once(source, old, new, label):
    count = source.count(old)
    if count != 1:
        fail(f"{label}: توقعت تطابقًا واحدًا، ووجدت {count}. لم تُكتب أي تغييرات.")
    return source.replace(old, new, 1)

def replace_between(source, start, end, replacement, label):
    if source.count(start) != 1:
        fail(f"{label}: علامة البداية غير فريدة.")
    begin = source.index(start)
    finish = source.find(end, begin + len(start))
    if finish < 0:
        fail(f"{label}: علامة النهاية غير موجودة.")
    return source[:begin] + replacement + source[finish:]

engine = read_file(ENGINE)
screens = read_file(SCREENS)
main = read_file(MAIN)

# 1. إضافة أحداث الحوار والرد المحلي إلى محرك الذاكرة.
engine_addition = '''
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

'''

engine = replace_once(
    engine,
    '    fun answerEvent(question: TrialQuestion, option: AnswerOption, now: Long = System.currentTimeMillis()): String =',
    engine_addition + '    fun answerEvent(question: TrialQuestion, option: AnswerOption, now: Long = System.currentTimeMillis()): String =',
    "إضافة وظائف الحوار إلى محرك الذاكرة"
)

# تحديث وصف الذاكرة ليتوافق مع حفظ الرسائل المرسلة فقط.
engine = replace_once(
    engine,
    '* It stores only submitted questionnaire choices, dialogue reactions,',
    '* It stores submitted questionnaire choices, dialogue messages and reactions,',
    "تحديث وصف الذاكرة"
)

# 2. إضافة حقل الكتابة إلى واجهة الطيف.
screens = replace_once(
    screens,
    'fun SpiritOverlay(\n',
    'fun SpiritOverlay(\n',
    "التحقق من واجهة الطيف"
)

screens = replace_once(
    screens,
    '    val motion = rememberInfiniteTransition(label = "spirit-character-motion")',
    '    var freeReply by rememberSaveable { mutableStateOf("") }\n'
    '    val motion = rememberInfiniteTransition(label = "spirit-character-motion")',
    "إضافة حالة حقل الحوار"
)

stage1_start = '} else if (stage == 1) {'
stage2_start = '} else if (stage == 2) {'

stage1_replacement = '''} else if (stage == 1) {
                    Spacer(Modifier.height(21.dp))
                    Text(
                        "كيف استقبلت هذه الملاحظة؟",
                        color = Color(0xFFD5C8E5),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "قريبة مما أشعر به",
                        "الصورة ليست دقيقة",
                        "لا أملك إجابةً الآن",
                        "أفضل الصمت الآن"
                    ).forEach { reply ->
                        Button(
                            onClick = { onReply(reply) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PanelRaised,
                                contentColor = Color(0xFFF0E8FF)
                            )
                        ) { Text(reply) }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "أو اكتب ما تريد قوله للطيف...",
                        color = Color(0xFFD5C8E5),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = freeReply,
                        onValueChange = { freeReply = it.take(1000) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("اكتب بحرية، فليس كل ما في الداخل اختيارًا جاهزًا...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet,
                            focusedLabelColor = Violet,
                            cursorColor = Violet
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val messageText = freeReply.trim()
                            if (messageText.isNotEmpty()) {
                                onReply("حوار:$messageText")
                                freeReply = ""
                            }
                        },
                        enabled = freeReply.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("أرسل ما بداخلك")
                    }
                '''

screens = replace_between(
    screens,
    stage1_start,
    stage2_start,
    stage1_replacement,
    "إضافة حقل الحوار إلى المرحلة الأولى"
)

# 3. منع مؤقت الكلام من إغلاق الطيف أثناء الحوار الحر.
main = replace_once(
    main,
    'if (spiritLine == line) {',
    'if (spiritLine == line && spiritStage != 1) {',
    "حماية الحوار من مؤقت الإغلاق"
)

# 4. استبدال معالج الرد مع الإبقاء على التفاعلات القديمة.
reply_start = '                    onReply = { reply ->'
reply_end = '                    onClose = {'

new_reply_block = '''                    onReply = { reply ->
                        val memoryKey = dialogue?.memoryKey ?: "general"

                        if (reply.startsWith("حوار:")) {
                            val userText = reply.removePrefix("حوار:").trim()
                            if (userText.isNotEmpty()) {
                                val userEvent = SpiritMemoryEngine.dialogueEvent(
                                    userText,
                                    memoryKey,
                                    "user"
                                )

                                scope.launch {
                                    val memoryWithUser = SpiritMemoryEngine.appendEvent(
                                        userState.spiritMemoryJson,
                                        userEvent
                                    )
                                    repository.appendSpiritMemoryEvent(userEvent)

                                    val answer = SpiritMemoryEngine.dialogueResponse(
                                        memoryWithUser,
                                        userText
                                    )

                                    val spiritEvent = SpiritMemoryEngine.dialogueEvent(
                                        answer,
                                        memoryKey,
                                        "spirit"
                                    )
                                    repository.appendSpiritMemoryEvent(spiritEvent)

                                    spiritSilent = false
                                    speechFinished = false
                                    spiritStage = 1
                                    spiritLine = answer
                                }
                            }
                        } else {
                            val event = if (reply.startsWith("رمز:")) {
                                SpiritMemoryEngine.symbolChoiceEvent(
                                    reply.removePrefix("رمز:").trim(),
                                    memoryKey
                                )
                            } else {
                                SpiritMemoryEngine.feedbackEvent(reply, memoryKey)
                            }

                            scope.launch {
                                repository.appendSpiritMemoryEvent(event)
                            }

                            when {
                                reply == "أفضل الصمت الآن" -> {
                                    spiritSilent = true
                                    speechFinished = true
                                    spiritStage = 3
                                    spiritLine = "لن أضيف سؤالًا آخر. لك أن تبقى هنا، أو تنهي لحظة الصمت متى شئت."
                                }

                                reply.startsWith("رمز:") -> {
                                    val choice = reply.removePrefix("رمز:").trim()
                                    spiritSilent = false
                                    spiritStage = 3
                                    spiritLine = SpiritMemoryEngine.symbolChoiceLine(choice)
                                }

                                else -> {
                                    val answerLine = when (reply) {
                                        "قريبة مما أشعر به" ->
                                            "لن أحوّل قبولك إلى حقيقةٍ نهائية؛ سأحتفظ بها كخيطٍ يستحق أن نعود إليه حين تتغير الأدلة."

                                        "الصورة ليست دقيقة" ->
                                            "إذن سأترك هذه الصورة مفتوحة للمراجعة. سأحفظ اعتراضك حتى لا أكرر القراءة نفسها دون أدلةٍ جديدة."

                                        else ->
                                            "لا بأس. بعض الأسئلة تحتاج إلى زمنٍ لا إلى إجابةٍ فورية. يمكنك أن تتركها معك حتى تتضح ملامحها."
                                    }

                                    spiritSilent = false
                                    spiritStage = 2
                                    spiritLine = answerLine
                                }
                            }
                        }
                    },
'''

main = replace_between(
    main,
    reply_start,
    reply_end,
    new_reply_block,
    "ربط الحوار الحر بالذاكرة"
)

# جميع الفحوص اكتملت. أنشئ نسخًا احتياطية ثم اكتب الملفات.
stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
files = [ENGINE, SCREENS, MAIN]
updated = [engine, screens, main]

for path in files:
    backup = path.with_name(path.name + ".dialogue_backup_" + stamp)
    shutil.copy2(path, backup)
    print("BACKUP:", backup.name)

for path, content in zip(files, updated):
    path.write_text(content, encoding="utf-8")

print()
print("تم تطبيق تحديث الحوار الحر.")
print("الملفات المعدلة: SpiritMemoryEngine.kt و AppScreens.kt و MainActivity.kt")
print("لم يتم تغيير نظام الصوت.")

