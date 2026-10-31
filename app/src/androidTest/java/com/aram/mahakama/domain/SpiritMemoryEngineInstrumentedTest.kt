package com.aram.mahakama.domain

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SpiritMemoryEngineInstrumentedTest {

    @Test
    fun feedbackEvent_preservesOriginalAndCanonicalReplies() {
        val event = JSONObject(
            SpiritMemoryEngine.feedbackEvent(
                reply = "لا تمثلني بدقة",
                insightKey = "desire_fear",
                now = 100L
            )
        )

        assertEquals("feedback", event.getString("kind"))
        assertEquals("لا تمثلني بدقة", event.getString("originalReply"))
        assertEquals("الصورة ليست دقيقة", event.getString("reply"))
        assertEquals("desire_fear", event.getString("insightKey"))
        assertEquals(100L, event.getLong("at"))
    }

    @Test
    fun createInsight_recognizesLegacyNegativeFeedback() {
        val legacyEvent = JSONObject()
            .put("kind", "feedback")
            .put("reply", "الصورة ليست دقيقة")
            .put("insightKey", "desire_fear")
            .put("at", 100L)

        val memory = JSONArray().put(legacyEvent).toString()
        val insight = SpiritMemoryEngine.createInsight(memory, emptyMap())

        assertTrue(insight.evidence.contains("اعتراضك السابق"))
        assertTrue(insight.evidence.contains("desire_fear"))
    }

    @Test
    fun createInsight_recognizesOriginalNegativeFeedback() {
        val event = SpiritMemoryEngine.feedbackEvent(
            reply = "لا تمثلني بدقة",
            insightKey = "control_responsibility",
            now = 200L
        )

        val memory = SpiritMemoryEngine.appendEvent("[]", event)
        val insight = SpiritMemoryEngine.createInsight(memory, emptyMap())

        assertTrue(insight.evidence.contains("اعتراضك السابق"))
        assertTrue(insight.evidence.contains("control_responsibility"))
    }

    @Test
    fun createInsight_recognizesPositiveFeedback() {
        val event = SpiritMemoryEngine.feedbackEvent(
            reply = "قريبة من تجربتي",
            insightKey = "opposing_motives",
            now = 300L
        )

        val memory = SpiritMemoryEngine.appendEvent("[]", event)
        val insight = SpiritMemoryEngine.createInsight(memory, emptyMap())

        assertTrue(insight.evidence.contains("قريبة من تجربتك"))
        assertTrue(insight.evidence.contains("قابلة للمراجعة"))
    }
}
