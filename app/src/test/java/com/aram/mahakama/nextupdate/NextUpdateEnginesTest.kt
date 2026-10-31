package com.aram.mahakama.nextupdate

import com.aram.mahakama.nextupdate.domain.ActiveTrialAnalysis
import com.aram.mahakama.nextupdate.domain.FaceGraphEngine
import com.aram.mahakama.nextupdate.domain.InnerCourtEngine
import com.aram.mahakama.nextupdate.domain.QuestionBankAudit
import com.aram.mahakama.nextupdate.domain.TrialEvolutionEngine
import com.aram.mahakama.nextupdate.model.TrialMemoryRecord
import com.aram.mahakama.nextupdate.model.TrialProfileSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextUpdateEnginesTest {
    @Test
    fun activeAnalysisIgnoresAnswersFromAbandonedBranches() {
        val answers = mapOf(
            "q01" to "control",
            "q02_control" to "plan_safety",
            "q02_accept" to "accept_trust" // stale branch: must not appear
        )

        val result = ActiveTrialAnalysis.analyze(answers)

        assertEquals(listOf("q01", "q02_control"), result.activeAnswers.map { it.questionId })
        assertFalse(result.activeAnswers.any { it.questionId == "q02_accept" })
    }

    @Test
    fun faceGraphUsesOnlyActiveEvidenceAndHasValidEdges() {
        val answers = mapOf("q01" to "control", "q02_control" to "plan_safety")
        val graph = FaceGraphEngine.build(answers)
        val validIds = graph.nodes.map { it.id }.toSet()

        assertEquals(7, graph.nodes.size)
        assertTrue(graph.nodes.first().score >= graph.nodes.last().score)
        assertTrue(graph.edges.all { it.fromId in validIds && it.toId in validIds })
        assertTrue(graph.nodes.flatMap { it.supportingEvidence }.all { it.questionId in setOf("q01", "q02_control") })
    }

    @Test
    fun timelineMarksOnlyExactCurrentQuestionOptionAsActive() {
        val records = listOf(
            TrialMemoryRecord("answer", 10, "قبل", "الإجابة القديمة", "q01", "accept"),
            TrialMemoryRecord("answer", 20, "الآن", "الإجابة الحالية", "q01", "control")
        )
        val timeline = TrialEvolutionEngine.timeline(records, mapOf("q01" to "control"))

        assertEquals(2, timeline.size)
        assertTrue(timeline.first().isCurrentAnswer)
        assertFalse(timeline.last().isCurrentAnswer)
    }

    @Test
    fun profileComparisonFindsNewFaceAndTension() {
        val old = TrialProfileSnapshot(100, 4, mapOf("control" to 1), listOf("guardian"), emptyList())
        val current = TrialProfileSnapshot(200, 6, mapOf("control" to 2), listOf("seeker", "guardian"), listOf("control_and_acceptance"))

        val result = TrialEvolutionEngine.compare(old, current)

        assertEquals(listOf("seeker"), result.addedLeadingFaces)
        assertEquals(listOf("control_and_acceptance"), result.newTensions)
        assertEquals(2, result.answerCountDelta)
        assertTrue(result.changed)
    }

    @Test
    fun authoredQuestionGraphHasNoBrokenOrUnreachableEdges() {
        val report = QuestionBankAudit.inspect()

        assertEquals(1000, report.totalQuestions)
        assertEquals(report.totalQuestions, report.uniqueQuestionIds)
        assertEquals(report.totalQuestions, report.reachableQuestions)
        assertFalse(report.hasErrors)
    }

    @Test
    fun courtVerdictClampsConfidenceAndRetainsEvidenceReferences() {
        val case = InnerCourtEngine.buildCase(mapOf("q01" to "control", "q02_control" to "plan_safety"))
        val verdict = InnerCourtEngine.createVerdict(
            courtCase = case,
            position = "الأدلة لا تكفي بعد",
            evidenceFocus = "الظروف والضغوط",
            confidencePercent = 130,
            whatCouldChangeMyMind = "ظهور دليل جديد",
            atMillis = 500
        )

        assertNotNull(case.arguments.firstOrNull { it.role == "الشاهد" })
        assertEquals(100, verdict.confidencePercent)
        assertEquals(case.evidenceQuestionIds, verdict.evidenceQuestionIds)
        assertTrue(verdict.reflection.contains("تعليق الحكم"))
    }
}
