package com.aram.mahakama.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionnaireEngineTest {
    @Test
    fun firstQuestionIsAvailableWithoutAnswers() {
        assertEquals("q01", QuestionnaireEngine.nextQuestion(emptyMap())?.id)
    }

    @Test
    fun answerBranchChangesNextQuestion() {
        val controlAnswers = mapOf("q01" to "control")
        val acceptanceAnswers = mapOf("q01" to "accept")
        assertEquals("q02_control", QuestionnaireEngine.nextQuestion(controlAnswers)?.id)
        assertEquals("q02_accept", QuestionnaireEngine.nextQuestion(acceptanceAnswers)?.id)
    }

    @Test
    fun bankContainsExactlyOneThousandUniqueNodesAndPrompts() {
        val questions = QuestionnaireEngine.questions
        assertEquals(1_000, questions.size)
        assertEquals(1_000, questions.map { it.id }.toSet().size)
        assertEquals(1_000, questions.map { it.prompt }.toSet().size)
        assertTrue(questions.all { it.options.size == 3 })
    }

    @Test
    fun sameDomainAndLensReceiveDifferentContextSpecificPromptsAndEvidence() {
        val firstContext = QuestionnaireEngine.questions.first { it.id == "bank_001" }
        val secondContext = QuestionnaireEngine.questions.first { it.id == "bank_085" }
        assertEquals(firstContext.heading, secondContext.heading)
        assertNotEquals(firstContext.prompt, secondContext.prompt)
        assertNotEquals(firstContext.options.first().detail, secondContext.options.first().detail)
    }

    @Test
    fun everyQuestionIsReachableAndEveryForwardLinkExists() {
        val questions = QuestionnaireEngine.questions
        val byId = questions.associateBy { it.id }
        assertTrue(questions.flatMap { it.options }.all { option ->
            option.nextQuestionId == null || option.nextQuestionId in byId
        })

        val reachable = mutableSetOf<String>()
        val pending = ArrayDeque<String>().apply { add("q01") }
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (!reachable.add(id)) continue
            byId[id]?.options?.mapNotNull { it.nextQuestionId }?.forEach(pending::add)
        }
        assertEquals(1_000, reachable.size)
    }

    @Test
    fun allNonterminalQuestionsOfferForwardTargetsWithoutEarlyNullBranches() {
        val questions = QuestionnaireEngine.questions
        val rank = questions.mapIndexed { index, question -> question.id to index }.toMap()
        questions.dropLast(1).forEach { question ->
            question.options.forEach { option ->
                val target = option.nextQuestionId
                assertNotNull("${question.id}/${option.id} must continue the path", target)
                assertTrue("${question.id} must move forward to $target", rank.getValue(target!!) > rank.getValue(question.id))
            }
        }
        assertTrue(questions.last().options.all { it.nextQuestionId == null })
    }

    @Test
    fun selectedSeedPathContinuesIntoExtendedBank() {
        val answers = seedPathAnswers()
        assertEquals("bank_001", QuestionnaireEngine.nextQuestion(answers)?.id)
        assertEquals(12, QuestionnaireEngine.activePath(answers).size)
    }

    @Test
    fun fullyAnsweredSequentialPathTerminatesCleanly() {
        val answers = seedPathAnswers().toMutableMap()
        QuestionnaireEngine.questions.filter { it.id.startsWith("bank_") }.forEach { question ->
            answers[question.id] = question.options.first().id
        }
        assertNull(QuestionnaireEngine.nextQuestion(answers))
        assertEquals(996, QuestionnaireEngine.activePath(answers).size)
    }


    @Test
    fun answerOptionsOpenDifferentAndSemanticallyRelatedFutureNodes() {
        val firstBankQuestion = QuestionnaireEngine.questions.first { it.id == "bank_001" }
        val options = firstBankQuestion.options
        assertEquals(3, options.size)
        assertNotNull(options[0].nextQuestionId)
        assertNotNull(options[1].nextQuestionId)
        assertNotNull(options[2].nextQuestionId)
        assertNotEquals(options[0].nextQuestionId, options[1].nextQuestionId)
        assertNotEquals(options[0].nextQuestionId, options[2].nextQuestionId)
        assertNotEquals(options[1].nextQuestionId, options[2].nextQuestionId)

        val base = seedPathAnswers()
        val reflective = base + ("bank_001" to options[1].id)
        val protective = base + ("bank_001" to options[2].id)
        assertNotEquals(
            QuestionnaireEngine.nextQuestion(reflective)?.id,
            QuestionnaireEngine.nextQuestion(protective)?.id
        )
    }

    @Test
    fun inactiveBranchAnswersAreRemovedFromTheActivePath() {
        val answers = mapOf(
            "q01" to "accept",
            "q02_control" to "plan_safety",
            "q02_accept" to "accept_trust"
        )
        assertEquals("q03", QuestionnaireEngine.nextQuestion(answers)?.id)
        assertEquals(
            mapOf("q01" to "accept", "q02_accept" to "accept_trust"),
            QuestionnaireEngine.activeAnswers(answers)
        )
    }

    @Test
    fun reflectionsUseActualLatestAnswerAndRemainTentative() {
        val answers = mapOf(
            "q01" to "control",
            "q02_control" to "plan_safety",
            "q03" to "truth_face"
        )
        val reflection = QuestionnaireEngine.reflectionFor(answers)
        assertTrue(reflection.contains("أواجهها مهما أزعجتني"))
        assertTrue(reflection.contains("لا هويتك كلها") || reflection.contains("ليست") || reflection.contains("قد"))
        assertFalse(reflection.contains("تشخيص"))
    }

    @Test
    fun innerCourtAndCounterfactualReflectSelectedEvidence() {
        val answers = mapOf(
            "q01" to "delay",
            "q02_delay" to "delay_courage"
        )
        assertTrue(QuestionnaireEngine.innerCourtLens(answers).second.contains("آخر اختيارٍ"))
        assertTrue(QuestionnaireEngine.alternateSelfReflection(answers).contains("في آخر اختيارٍ لك"))
    }

    @Test
    fun spiritWaitsForEnoughAnswersAndRevealsAtSelectedMoments() {
        val answers = mapOf("q01" to "control", "q02_control" to "plan_safety", "q03" to "truth_face")
        assertEquals(false, QuestionnaireEngine.shouldRevealSpirit(answers, 2, 3))
        assertEquals(true, QuestionnaireEngine.shouldRevealSpirit(answers, 3, 3))
    }

    @Test
    fun faceScoresReflectMatchingTags() {
        val answers = mapOf("q01" to "control", "q02_control" to "plan_safety")
        val guardian = QuestionnaireEngine.faces.first { it.id == "guardian" }
        assertEquals(4, QuestionnaireEngine.faceScore(guardian, answers))
    }

    private fun seedPathAnswers(): Map<String, String> = mapOf(
        "q01" to "control",
        "q02_control" to "plan_safety",
        "q03" to "truth_face",
        "q04_face" to "face_hurt",
        "q05" to "loss_meaning",
        "q06" to "fear_failure",
        "q07" to "judge_learn",
        "q08" to "belonging_self",
        "q09" to "alone_create",
        "q10" to "value_meaning",
        "q11" to "shadow_accept",
        "q12" to "future_trust"
    )
}
