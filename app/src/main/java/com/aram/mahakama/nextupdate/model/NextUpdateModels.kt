package com.aram.mahakama.nextupdate.model

/** A single answer on the currently active questionnaire branch. */
data class ActiveAnswerEvidence(
    val questionId: String,
    val heading: String,
    val prompt: String,
    val optionId: String,
    val answer: String,
    val detail: String,
    val tags: Set<String>,
    val contextLabel: String
)

data class TensionRuleResult(
    val id: String,
    val title: String,
    val interpretation: String,
    val reviewQuestion: String,
    val matchedTags: Set<String>,
    val evidenceQuestionIds: Set<String>
)

data class ActiveTrialAnalysisResult(
    val activeAnswers: List<ActiveAnswerEvidence>,
    val tagCounts: Map<String, Int>,
    val tensions: List<TensionRuleResult>,
    val contextCounts: Map<String, Int>
) {
    val answeredCount: Int get() = activeAnswers.size
}

data class FaceEvidenceItem(
    val questionId: String,
    val heading: String,
    val answer: String,
    val detail: String,
    val matchedTags: Set<String>,
    val contextLabel: String
)

data class FaceGraphNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val score: Int,
    val signalLabel: String,
    val supportingEvidence: List<FaceEvidenceItem>,
    val counterEvidence: List<FaceEvidenceItem>,
    val contextLabel: String,
    val contextNote: String,
    val relatedFaceIds: List<String>,
    val unresolvedQuestion: String
)

data class FaceGraphEdge(val fromId: String, val toId: String)

data class FaceGraphSnapshot(
    val rootTitle: String,
    val rootDescription: String,
    val nodes: List<FaceGraphNode>,
    val edges: List<FaceGraphEdge>,
    val activeAnswerCount: Int
) {
    fun node(id: String): FaceGraphNode? = nodes.firstOrNull { it.id == id }
}

data class TrialProfileSnapshot(
    val atMillis: Long,
    val activeAnswerCount: Int,
    val tagCounts: Map<String, Int>,
    val leadingFaceIds: List<String>,
    val tensionIds: List<String>
)

data class TrialEvolutionChange(
    val addedLeadingFaces: List<String>,
    val fadedLeadingFaces: List<String>,
    val newTensions: List<String>,
    val resolvedTensions: List<String>,
    val answerCountDelta: Int,
    val changed: Boolean
)

data class TrialMemoryRecord(
    val kind: String,
    val atMillis: Long,
    val title: String,
    val body: String,
    val questionId: String? = null,
    val optionId: String? = null,
    val activeNow: Boolean? = null
)

data class TrialTimelineItem(
    val kind: String,
    val atMillis: Long,
    val title: String,
    val body: String,
    val statusLabel: String,
    val isCurrentAnswer: Boolean
)

data class CourtArgument(
    val role: String,
    val question: String,
    val purpose: String
)

data class CourtCase(
    val id: String,
    val title: String,
    val context: String,
    val centralQuestion: String,
    val evidenceSummary: String,
    val evidenceQuestionIds: List<String>,
    val arguments: List<CourtArgument>,
    val reviewQuestion: String
)

data class CourtVerdict(
    val id: String,
    val caseId: String,
    val atMillis: Long,
    val position: String,
    val evidenceFocus: String,
    val confidencePercent: Int,
    val whatCouldChangeMyMind: String,
    val reflection: String,
    val evidenceQuestionIds: List<String>
)

data class SpiritEvidenceReflection(
    val key: String,
    val line: String,
    val followUp: String,
    val evidenceSummary: String,
    val limitation: String
)


data class QuestionAuditIssue(
    val code: String,
    val severity: String,
    val questionId: String,
    val detail: String
)

data class QuestionBankAuditReport(
    val totalQuestions: Int,
    val uniqueQuestionIds: Int,
    val reachableQuestions: Int,
    val totalEdges: Int,
    val issues: List<QuestionAuditIssue>
) {
    val hasErrors: Boolean get() = issues.any { it.severity == "ERROR" }
    val isComplete: Boolean get() = !hasErrors && totalQuestions == 1000 && reachableQuestions == totalQuestions
}
