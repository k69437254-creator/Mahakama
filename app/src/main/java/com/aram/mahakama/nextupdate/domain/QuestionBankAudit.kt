package com.aram.mahakama.nextupdate.domain

import com.aram.mahakama.domain.QuestionnaireEngine
import com.aram.mahakama.nextupdate.model.QuestionAuditIssue
import com.aram.mahakama.nextupdate.model.QuestionBankAuditReport
import java.text.Normalizer
import java.util.ArrayDeque

/** Static integrity checks for the authored + generated 1,000-question graph. */
object QuestionBankAudit {
    fun inspect(): QuestionBankAuditReport = inspect(QuestionnaireEngine.questions)

    fun inspect(questions: List<com.aram.mahakama.domain.TrialQuestion>): QuestionBankAuditReport {
        val issues = mutableListOf<QuestionAuditIssue>()
        val byId = questions.groupBy { it.id }
        val uniqueIds = byId.keys
        val indexById = questions.mapIndexed { index, question -> question.id to index }.toMap()
        val totalEdges = questions.sumOf { question -> question.options.count { it.nextQuestionId != null } }

        byId.filterValues { it.size > 1 }.forEach { (id, duplicates) ->
            issues += issue("DUPLICATE_QUESTION_ID", "ERROR", id, "ظهر معرف السؤال ${duplicates.size} مرات.")
        }

        questions.forEach { question ->
            if (question.id.isBlank()) issues += issue("EMPTY_QUESTION_ID", "ERROR", "?", "يوجد سؤال بلا معرف.")
            if (question.heading.isBlank() || question.prompt.isBlank()) {
                issues += issue("EMPTY_QUESTION_TEXT", "ERROR", question.id, "عنوان السؤال أو نصه فارغ.")
            }
            if (question.options.size != 3) {
                issues += issue("OPTION_COUNT", "ERROR", question.id, "عدد الخيارات ${question.options.size} بدلًا من ثلاثة.")
            }
            val duplicateOptions = question.options.groupBy { it.id }.filterValues { it.size > 1 }
            duplicateOptions.keys.forEach { optionId ->
                issues += issue("DUPLICATE_OPTION_ID", "ERROR", question.id, "معرف الخيار مكرر: $optionId")
            }
            question.options.forEach { option ->
                if (option.title.isBlank() || option.detail.isBlank()) {
                    issues += issue("EMPTY_OPTION_TEXT", "ERROR", question.id, "الخيار ${option.id} لا يحتوي عنوانًا وتفصيلًا كاملين.")
                }
                if (option.tags.isEmpty()) {
                    issues += issue("EMPTY_OPTION_TAGS", "WARNING", question.id, "الخيار ${option.id} بلا tags للتحليل.")
                }
                option.nextQuestionId?.let { nextId ->
                    if (nextId !in uniqueIds) {
                        issues += issue("MISSING_NEXT_QUESTION", "ERROR", question.id, "يشير الخيار ${option.id} إلى سؤال غير موجود: $nextId")
                    } else {
                        val currentIndex = indexById[question.id] ?: -1
                        val nextIndex = indexById[nextId] ?: -1
                        if (nextIndex <= currentIndex) {
                            issues += issue("NON_FORWARD_EDGE", "ERROR", question.id, "المسار ${question.id} → $nextId ليس إلى عقدة لاحقة.")
                        }
                    }
                }
            }
        }

        questions.groupBy { normalizePrompt(it.prompt) }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .values.flatten().forEach { question ->
                issues += issue("DUPLICATE_PROMPT", "WARNING", question.id, "قد تتكرر صياغة السؤال أو تقترب كثيرًا من سؤال آخر.")
            }

        val reachable = reachableFrom("q01", byId)
        questions.filter { it.id !in reachable }.forEach { question ->
            issues += issue("UNREACHABLE_QUESTION", "ERROR", question.id, "لا يمكن الوصول إلى السؤال من نقطة بداية المحاكمة.")
        }

        return QuestionBankAuditReport(
            totalQuestions = questions.size,
            uniqueQuestionIds = uniqueIds.size,
            reachableQuestions = reachable.size,
            totalEdges = totalEdges,
            issues = issues.distinctBy { Triple(it.code, it.questionId, it.detail) }
        )
    }

    private fun reachableFrom(
        startId: String,
        byId: Map<String, List<com.aram.mahakama.domain.TrialQuestion>>
    ): Set<String> {
        if (startId !in byId) return emptySet()
        val visited = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!visited.add(id)) continue
            val question = byId[id]?.firstOrNull() ?: continue
            question.options.mapNotNull { it.nextQuestionId }.forEach { next ->
                if (next !in visited) queue.addLast(next)
            }
        }
        return visited
    }

    private fun normalizePrompt(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
        .lowercase()
        .replace(Regex("[\\p{Punct}\\s]+"), "")

    private fun issue(code: String, severity: String, questionId: String, detail: String) =
        QuestionAuditIssue(code, severity, questionId, detail)
}
