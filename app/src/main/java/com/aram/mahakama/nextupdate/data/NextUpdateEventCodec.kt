package com.aram.mahakama.nextupdate.data

import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.nextupdate.model.CourtVerdict
import com.aram.mahakama.nextupdate.model.TrialMemoryRecord
import com.aram.mahakama.nextupdate.model.TrialProfileSnapshot
import org.json.JSONArray
import org.json.JSONObject

/** Backward-compatible codec for the existing spiritMemoryJson event stream. */
object NextUpdateEventCodec {

    /**
     * Produces a temporary, active-branch-only event stream for behavioral reflection.
     * The original event log is untouched and remains available to the historical timeline.
     * Non-answer events are retained; answer events are limited to the exact current path.
     */
    fun activeBranchMemory(raw: String, answers: Map<String, String>): String {
        val source = try { JSONArray(raw.ifBlank { "[]" }) } catch (_: Exception) { return "[]" }
        val active = QuestionnaireEngine.activeAnswers(answers)
        val activeAnswerEvents = linkedMapOf<String, JSONObject>()
        val otherEvents = mutableListOf<JSONObject>()

        for (index in 0 until source.length()) {
            val event = source.optJSONObject(index) ?: continue
            if (event.optString("kind") != "answer") {
                otherEvents += JSONObject(event.toString())
                continue
            }
            val questionId = event.optString("questionId")
            val optionId = event.optString("optionId")
            if (active[questionId] == optionId && questionId.isNotBlank()) {
                val previous = activeAnswerEvents[questionId]
                if (previous == null || event.optLong("at") >= previous.optLong("at")) {
                    activeAnswerEvents[questionId] = JSONObject(event.toString())
                }
            }
        }

        val selectedAnswers = activeAnswerEvents.values.sortedBy { it.optLong("at") }
        val merged = (otherEvents + selectedAnswers).sortedBy { it.optLong("at") }
        val answerTimes = selectedAnswers.map { it.optLong("at") }
        merged.filter { it.optString("kind") == "appearance" }.forEach { appearance ->
            val appearanceAt = appearance.optLong("at")
            appearance.put("answerCount", answerTimes.count { it <= appearanceAt })
        }
        val result = JSONArray()
        merged.forEach { result.put(it) }
        return result.toString()
    }

    fun parseRecords(raw: String): List<TrialMemoryRecord> {
        val array = try { JSONArray(raw.ifBlank { "[]" }) } catch (_: Exception) { return emptyList() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val kind = item.optString("kind")
                val at = item.optLong("at", 0L)
                val record = when (kind) {
                    "answer" -> TrialMemoryRecord(
                        kind = kind,
                        atMillis = at,
                        title = item.optString("heading", "إجابة في المحاكمة"),
                        body = buildString {
                            val answer = item.optString("answer")
                            val detail = item.optString("detail")
                            append(answer)
                            if (detail.isNotBlank()) append("\n").append(detail)
                            val prompt = item.optString("prompt")
                            if (prompt.isNotBlank()) append("\n\nالسؤال: ").append(prompt)
                        },
                        questionId = item.optString("questionId").takeIf { it.isNotBlank() },
                        optionId = item.optString("optionId").takeIf { it.isNotBlank() }
                    )
                    "feedback" -> TrialMemoryRecord(
                        kind, at, "ردّ على انعكاس", item.optString("originalReply").ifBlank { item.optString("reply") }
                    )
                    "dialogue" -> TrialMemoryRecord(
                        kind, at,
                        if (item.optString("role") == "user") "رسالتك إلى طيف الروح" else "ردّ طيف الروح",
                        item.optString("text")
                    )
                    "appearance" -> TrialMemoryRecord(
                        kind, at, "لحظة ظهور طيف الروح", item.optString("pendingQuestion").ifBlank { "حضر الطيف في هذه اللحظة من الرحلة." }
                    )
                    "symbol" -> TrialMemoryRecord(kind, at, "اختيار رمزي", item.optString("choice"))
                    "court_verdict" -> TrialMemoryRecord(
                        kind, at, item.optString("caseTitle", "محضر المحكمة الداخلية"),
                        buildString {
                            append("الموقف: ").append(item.optString("position"))
                            append("\nالدليل الأهم: ").append(item.optString("evidenceFocus"))
                            append("\nدرجة الثقة: ").append(item.optInt("confidencePercent", 0)).append("٪")
                            item.optString("reflection").takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
                        }
                    )
                    "profile_snapshot" -> TrialMemoryRecord(
                        kind, at, "محطة في تطور الصورة",
                        item.optString("summary").ifBlank { "حُفظت محطة جديدة من محطات تطور المسار." }
                    )
                    else -> null
                }
                if (record != null) add(record)
            }
        }
    }

    fun latestProfileSnapshot(raw: String): TrialProfileSnapshot? {
        val array = try { JSONArray(raw.ifBlank { "[]" }) } catch (_: Exception) { return null }
        for (index in array.length() - 1 downTo 0) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optString("kind") != "profile_snapshot") continue
            val tagsObject = item.optJSONObject("tagCounts") ?: JSONObject()
            val tags = buildMap {
                val keys = tagsObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, tagsObject.optInt(key, 0))
                }
            }
            return TrialProfileSnapshot(
                atMillis = item.optLong("at", 0L),
                activeAnswerCount = item.optInt("activeAnswerCount", 0),
                tagCounts = tags,
                leadingFaceIds = item.optJSONArray("leadingFaceIds").toStringList(),
                tensionIds = item.optJSONArray("tensionIds").toStringList()
            )
        }
        return null
    }

    fun encodeProfileSnapshot(snapshot: TrialProfileSnapshot, summary: String): String {
        val tags = JSONObject()
        snapshot.tagCounts.forEach { (key, value) -> tags.put(key, value) }
        return JSONObject()
            .put("kind", "profile_snapshot")
            .put("at", snapshot.atMillis)
            .put("activeAnswerCount", snapshot.activeAnswerCount)
            .put("tagCounts", tags)
            .put("leadingFaceIds", JSONArray(snapshot.leadingFaceIds))
            .put("tensionIds", JSONArray(snapshot.tensionIds))
            .put("summary", summary)
            .toString()
    }

    fun encodeCourtVerdict(verdict: CourtVerdict, caseTitle: String): String = JSONObject()
        .put("kind", "court_verdict")
        .put("at", verdict.atMillis)
        .put("id", verdict.id)
        .put("caseId", verdict.caseId)
        .put("caseTitle", caseTitle)
        .put("position", verdict.position)
        .put("evidenceFocus", verdict.evidenceFocus)
        .put("confidencePercent", verdict.confidencePercent)
        .put("whatCouldChangeMyMind", verdict.whatCouldChangeMyMind)
        .put("reflection", verdict.reflection)
        .put("evidenceQuestionIds", JSONArray(verdict.evidenceQuestionIds))
        .toString()

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
