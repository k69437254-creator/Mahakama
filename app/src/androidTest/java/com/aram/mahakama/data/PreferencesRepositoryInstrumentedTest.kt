package com.aram.mahakama.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesRepositoryInstrumentedTest {
    @Test
    fun savesIdentityMutePreferenceTimerAndCurrentBranchAtomically() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PreferencesRepository(context, "mahakama_storage_test_${UUID.randomUUID()}")
        val name = "اختبار التخزين المحلي"

        repository.saveUserName(name)
        repository.setAmbientMuted(true)
        repository.markTrialStarted()
        repository.addActiveTime(12_345L)
        // First write one branch, then choose another at q01: evidence from the abandoned
        // branch must be removed from the persisted active path.
        repository.replaceAnswers(
            activeAnswers = mapOf("q01" to "control"),
            changedQuestionId = "q02_control",
            selectedOptionId = "plan_safety"
        )
        repository.replaceAnswers(
            activeAnswers = emptyMap(),
            changedQuestionId = "q01",
            selectedOptionId = "accept"
        )
        repository.replaceAnswers(
            activeAnswers = mapOf("q01" to "accept"),
            changedQuestionId = "q02_accept",
            selectedOptionId = "accept_trust"
        )
        repository.saveFreeWriting("نص اختبار محلي", "seeker")
        repository.saveUnmaskingFeedback("قريبة من تجربتي")

        val state = withTimeout(5_000L) {
            repository.state.first {
                it.userName == name && it.totalActiveMillis == 12_345L &&
                    it.answers["q02_accept"] == "accept_trust" && it.ambientMuted && it.trialStarted &&
                    it.freeWriting == "نص اختبار محلي" && it.unmaskingFeedback == "قريبة من تجربتي"
            }
        }
        assertEquals(name, state.userName)
        assertTrue(state.ambientMuted)
        assertTrue(state.trialStarted)
        assertEquals(12_345L, state.totalActiveMillis)
        assertEquals(mapOf("q01" to "accept", "q02_accept" to "accept_trust"), state.answers)
        assertTrue("q02_control" !in state.answers)
        assertEquals("نص اختبار محلي", state.freeWriting)
        assertEquals("seeker", state.freeWritingFaceId)
        assertEquals("قريبة من تجربتي", state.unmaskingFeedback)
    }
}
