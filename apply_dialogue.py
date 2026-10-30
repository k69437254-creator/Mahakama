
#!/usr/bin/env python3
from pathlib import Path
from datetime import datetime
import shutil
import sys

ROOT = Path("app/src/main/java/com/aram/mahakama")
MAIN = ROOT / "MainActivity.kt"
SCREEN = ROOT / "ui/AppScreens.kt"
ENGINE = ROOT / "domain/SpiritMemoryEngine.kt"
FILES = [MAIN, SCREEN, ENGINE]


def stop(message):
    print("توقف دون تعديل:", message)
    sys.exit(1)


for path in FILES:
    if not path.is_file():
        stop(f"الملف غير موجود: {path}")

m = MAIN.read_text(encoding="utf-8")
s = SCREEN.read_text(encoding="utf-8")
e = ENGINE.read_text(encoding="utf-8")


def replace_once(source, old, new, label):
    count = source.count(old)
    if count != 1:
        stop(
            f"موضع {label} غير مطابق؛ "
            f"عدد مرات ظهوره: {count}"
        )
    return source.replace(old, new, 1)


# منع تكرار التحديث
if (
    "fun dialogueEvent(" in e
    or  reply.startsWith("حوار:")  in m
    or "اكتب ردك بحرية" in s
):
    stop("يبدو أن تحديث الحوار موجود مسبقًا.")


# 1. إضافة وظائف الحوار إلى محرك الذاكرة
addition = r   
    fun dialogueEvent(
        text: String,
        insightKey: String,
        role: String = "user",
        now: Long = System.currentTimeMillis()
    ): String = JSONObject()
        .put("kind", "dialogue")
        .put("at", now)
        .put("role", role)
        .put("text", text.trim())
        .put("insightKey", insightKey)
        .toString()

    fun dialogueResponse(
        memoryJson: String,
        userText: String
    ): String {
        val text = userText.trim()
        val history = events(memoryJson)

        val previousMessages = history.filter {
            it.optString("kind") == "dialogue" &&
            it.optString("role") == "user"
        }

        val latestAnswer = history.lastOrNull {
            it.optString("kind") == "answer"
        }

        val previousText = previousMessages.lastOrNull()
            ?.optString("text").orEmpty()

        if (text.length < 4) {
            return "وصلتني كلمتك، لكنني لا أريد أن أحمّلها معنى لم تقصده. هل تستطيع أن توضّح ما الذي تعنيه بها؟"
        }

        if (
            listOf(
                "غير دقيق",
                "لا يشبهني",
                "أخطأت",
                "ليس هذا ما أقصد"
            ).any { it in text }
        ) {
            return "سأراجع قراءتي إذن. اعتراضك جزءٌ من الأدلة، وليس شيئًا ينبغي أن أتجاوزه. ما الجزء الذي فهمته على نحوٍ خاطئ؟"
        }

        if (
            listOf(
                "لا أعرف",
                "لا أدري",
                "لست متأكد"
            ).any { it in text }
        ) {
            return "لا بأس بأن تبقى الإجابة معلّقة. لن أعتبر عدم اليقين اعترافًا أو رفضًا. هل الحيرة في شعورك، أم في اختيارك، أم في فهم السؤال نفسه؟"
        }

        if (previousText == text) {
            return "لاحظت أنك عدت إلى العبارة نفسها. لن أفترض أن تكرارها يعني شيئًا بعينه؛ هل ما زالت تعبّر عن موقفك، أم أنك تحاول تثبيت معنى لم يتضح بعد؟"
        }

        val context = latestAnswer?.let {
            "أضع ما قلته الآن بجوار إجابتك السابقة عن «${it.optString("heading")}»: «${it.optString("answer")}»."
        }.orEmpty()

        val followUp = when {
            listOf(
                "أخاف",
                "الخوف",
                "قلق",
                "أخشى"
            ).any { it in text } ->
                "ما الذي تخشى حدوثه تحديدًا، وما الذي تعرفه فعلًا وما الذي تتوقعه فقط؟"

            listOf(
                "أريد",
                "أرغب",
                "أتمنى",
                "أحتاج"
            ).any { it in text } ->
                "لو فصلنا رغبتك عن توقعات الآخرين، ما الجزء الذي ستختاره بإرادتك؟"

            listOf(
                "غضب",
                "غاضب",
                "أكره",
                "أزعجني"
            ).any { it in text } ->
                "ما الحدث الذي أثار هذا الشعور، وما الذي كان يمثله لك في تلك اللحظة؟"

            listOf(
                "ندم",
                "أخطأت",
                "خطأ",
                "آسف"
            ).any { it in text } ->
                "هل ندمك متعلق بالفعل نفسه، أم بنتيجته، أم بالصورة التي أصبحت تراها عن نفسك بعده؟"

            listOf(
                "حرية",
                "اختيار",
                "قرار"
            ).any { it in text } ->
                "ما الذي كان متاحًا لك فعلًا وقت ذلك الاختيار، وما الذي لم يكن متاحًا؟"

            else ->
                "أي جزء من كلامك تريد أن نتوقف عنده أولًا، وما السياق الذي قد يغيّر فهمه؟"
        }

        return if (context.isBlank()) {
            followUp
        } else {
            "$context\n\n$followUp"
        }
    }
   

if not e.rstrip().endswith("}"):
    stop("نهاية محرك الذاكرة غير متوقعة.")

e = e.rstrip()[:-1] + addition + "\n}\n"


# 2. ربط تسجيل ردود الطيف
old_event =    val event = if (reply.startsWith("رمز:")) {
                            SpiritMemoryEngine.symbolChoiceEvent(reply.removePrefix("رمز:").trim(), memoryKey)
                        } else {
                            SpiritMemoryEngine.feedbackEvent(reply, memoryKey)
                        }   

new_event =    val event = when {
                            reply.startsWith("رمز:") ->
                                SpiritMemoryEngine.symbolChoiceEvent(reply.removePrefix("رمز:").trim(), memoryKey)

                            reply.startsWith("حوار:") ->
                                SpiritMemoryEngine.dialogueEvent(reply.removePrefix("حوار:").trim(), memoryKey)

                            else ->
                                SpiritMemoryEngine.feedbackEvent(reply, memoryKey)
                        }   

m = replace_once(
    m,
    old_event,
    new_event,
    "تسجيل ردود الطيف"
)


# 3. إضافة مسار الحوار الحر
old_when =    when {
                            reply == "أفضل الصمت الآن" -> {   

new_when =    when {
                            reply.startsWith("حوار:") -> {
                                val userText = reply.removePrefix("حوار:").trim()

                                val answerLine =
                                    SpiritMemoryEngine.dialogueResponse(
                                        userState.spiritMemoryJson,
                                        userText
                                    )

                                spiritSilent = false
                                spiritStage = 1
                                spiritLine = answerLine

                                scope.launch {
                                    repository.appendSpiritMemoryEvent(
                                        SpiritMemoryEngine.dialogueEvent(
                                            answerLine,
                                            memoryKey,
                                            "spirit"
                                        )
                                    )
                                }
                            }

                            reply == "أفضل الصمت الآن" -> {   

m = replace_once(
    m,
    old_when,
    new_when,
    "مسار الحوار الحر"
)


# 4. إبقاء واجهة الحوار ظاهرة بعد نطق الرد
m = replace_once(
    m,
    "if (spiritLine == line) {",
    "if (spiritLine == line && spiritStage != 1) {",
    "استمرار جلسة الحوار بعد نطق الرد"
)


# 5. إضافة حالة الكتابة الحرة
state_anchor =    ) {
    val motion = rememberInfiniteTransition(label = "spirit-character-motion")   

state_new =    ) {
    var freeReply by remember { mutableStateOf("") }
    val motion = rememberInfiniteTransition(label = "spirit-character-motion")   

s = replace_once(
    s,
    state_anchor,
    state_new,
    "حالة نص الحوار"
)


# 6. إضافة حقل الكتابة وزر الإرسال
old_ui =    Text("كيف استقبلت هذه الملاحظة؟", color = Color(0xFFD5C8E5), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        listOf("قريبة مما أشعر به", "الصورة ليست دقيقة", "لا أملك إجابةً الآن", "أفضل الصمت الآن").forEach { reply ->   

new_ui =    Text("كيف استقبلت هذه الملاحظة؟", color = Color(0xFFD5C8E5), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))

                        Text(
                            "يمكنك اختيار رد، أو كتابة ما تريد قوله للطيف بكلماتك.",
                            color = Color(0xFFB9A9CA),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(9.dp))

                        OutlinedTextField(
                            value = freeReply,
                            onValueChange = {
                                freeReply = it.take(1200)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("اكتب ردك بحرية")
                            },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(15.dp)
                        )

                        Spacer(Modifier.height(7.dp))

                        Button(
                            onClick = {
                                val text = freeReply.trim()

                                if (text.isNotEmpty()) {
                                    onReply("حوار:$text")
                                    freeReply = ""
                                }
                            },
                            enabled = freeReply.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("أرسل ما تريد قوله")
                        }

                        Spacer(Modifier.height(12.dp))

                        listOf("قريبة مما أشعر به", "الصورة ليست دقيقة", "لا أملك إجابةً الآن", "أفضل الصمت الآن").forEach { reply ->   

s = replace_once(
    s,
    old_ui,
    new_ui,
    "واجهة الكتابة الحرة"
)


# 7. التحقق قبل الحفظ
if (
    "fun dialogueEvent(" not in e
    or "fun dialogueResponse(" not in e
):
    stop("فشل التحقق من محرك الحوار.")

if (
     reply.startsWith("حوار:")  not in m
    or "SpiritMemoryEngine.dialogueResponse(" not in m
):
    stop("فشل التحقق من ربط الحوار.")

if (
    "اكتب ردك بحرية" not in s
    or  onReply("حوار:$text")  not in s
):
    stop("فشل التحقق من واجهة الحوار.")


# 8. إنشاء نسخ احتياطية ثم الكتابة
stamp = datetime.now().strftime("%Y%m%d_%H%M%S")

backups = []

for path in FILES:
    backup = Path(
        str(path) + f".bak_dialogue_{stamp}"
    )
    shutil.copy2(path, backup)
    backups.append(backup)

MAIN.write_text(m, encoding="utf-8")
SCREEN.write_text(s, encoding="utf-8")
ENGINE.write_text(e, encoding="utf-8")

print("تم تطبيق حزمة الحوار الحر.")
print("تم حفظ النسخ الاحتياطية:")
for backup in backups:
    print(" -", backup)

print("لم يتم تغيير نظام الصوت الحالي.")
print("لم يتم حذف خيارات الرد أو الرموز.")
print(
    "تنبيه: الردود محلية قائمة على قواعد وسياق محفوظ، "
    "وليست نموذجًا توليديًا."
)

