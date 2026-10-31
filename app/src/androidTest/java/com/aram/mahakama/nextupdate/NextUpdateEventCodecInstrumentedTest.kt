package com.aram.mahakama.nextupdate

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aram.mahakama.nextupdate.data.NextUpdateEventCodec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NextUpdateEventCodecInstrumentedTest {
    @Test
    fun activeBranchMemoryFiltersAbandonedAnswersButKeepsHistoricalEvents() {
        val raw = JSONArray()
            .put(answer("q01", "control", 10L))
            .put(answer("q02_control", "plan_safety", 20L))
            .put(answer("q02_accept", "accept_trust", 21L))
            .put(JSONObject().put("kind", "dialogue").put("at", 22L).put("text", "حوار محفوظ"))
            .put(JSONObject().put("kind", "appearance").put("at", 23L).put("answerCount", 3))
            .toString()

        val filtered = JSONArray(
            NextUpdateEventCodec.activeBranchMemory(
                raw,
                mapOf("q01" to "control", "q02_control" to "plan_safety", "q02_accept" to "accept_trust")
            )
        )
        val answerIds = buildList {
            for (index in 0 until filtered.length()) {
                val item = filtered.getJSONObject(index)
                if (item.optString("kind") == "answer") add(item.optString("questionId"))
            }
        }

        assertEquals(listOf("q01", "q02_control"), answerIds)
        assertTrue((0 until filtered.length()).any { filtered.getJSONObject(it).optString("kind") == "dialogue" })
        val appearance = (0 until filtered.length()).map { filtered.getJSONObject(it) }
            .first { it.optString("kind") == "appearance" }
        assertEquals(2, appearance.optInt("answerCount"))
    }

    private fun answer(questionId: String, optionId: String, at: Long) = JSONObject()
        .put("kind", "answer")
        .put("at", at)
        .put("questionId", questionId)
        .put("optionId", optionId)
        .put("answer", optionId)
}
