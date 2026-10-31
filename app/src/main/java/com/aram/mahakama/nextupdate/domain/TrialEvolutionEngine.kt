package com.aram.mahakama.nextupdate.domain

import com.aram.mahakama.nextupdate.model.TrialEvolutionChange
import com.aram.mahakama.nextupdate.model.TrialMemoryRecord
import com.aram.mahakama.nextupdate.model.TrialProfileSnapshot
import com.aram.mahakama.nextupdate.model.TrialTimelineItem

/** Pure transformation layer for the profile's longitudinal history. */
object TrialEvolutionEngine {
    fun snapshot(answers: Map<String, String>, atMillis: Long = System.currentTimeMillis()): TrialProfileSnapshot {
        val analysis = ActiveTrialAnalysis.analyze(answers)
        val graph = FaceGraphEngine.build(answers)
        return TrialProfileSnapshot(
            atMillis = atMillis,
            activeAnswerCount = analysis.answeredCount,
            tagCounts = analysis.tagCounts,
            leadingFaceIds = graph.nodes.filter { it.score > 0 }.take(3).map { it.id },
            tensionIds = analysis.tensions.map { it.id }
        )
    }

    fun compare(previous: TrialProfileSnapshot?, current: TrialProfileSnapshot): TrialEvolutionChange {
        if (previous == null) {
            return TrialEvolutionChange(
                addedLeadingFaces = current.leadingFaceIds,
                fadedLeadingFaces = emptyList(),
                newTensions = current.tensionIds,
                resolvedTensions = emptyList(),
                answerCountDelta = current.activeAnswerCount,
                changed = current.activeAnswerCount > 0
            )
        }
        val oldFaces = previous.leadingFaceIds.toSet()
        val newFaces = current.leadingFaceIds.toSet()
        val oldTensions = previous.tensionIds.toSet()
        val newTensions = current.tensionIds.toSet()
        val added = current.leadingFaceIds.filterNot { it in oldFaces }
        val faded = previous.leadingFaceIds.filterNot { it in newFaces }
        val tensionsAdded = current.tensionIds.filterNot { it in oldTensions }
        val tensionsResolved = previous.tensionIds.filterNot { it in newTensions }
        val countDelta = current.activeAnswerCount - previous.activeAnswerCount
        val changed = added.isNotEmpty() || faded.isNotEmpty() || tensionsAdded.isNotEmpty() ||
            tensionsResolved.isNotEmpty() || countDelta != 0 || previous.tagCounts != current.tagCounts
        return TrialEvolutionChange(added, faded, tensionsAdded, tensionsResolved, countDelta, changed)
    }

    /** Save on meaningful milestones, not on every keystroke or every recomposition. */
    fun shouldRecordSnapshot(previous: TrialProfileSnapshot?, current: TrialProfileSnapshot): Boolean {
        if (current.activeAnswerCount == 0) return false
        if (previous == null) return true
        val change = compare(previous, current)
        return current.activeAnswerCount % 4 == 0 ||
            change.addedLeadingFaces.isNotEmpty() ||
            change.newTensions.isNotEmpty() ||
            change.resolvedTensions.isNotEmpty()
    }

    fun timeline(
        records: List<TrialMemoryRecord>,
        activeAnswers: Map<String, String>
    ): List<TrialTimelineItem> {
        // If the same question/option was submitted more than once, only its latest event
        // is labelled current. Earlier repetitions remain visible as historical events.
        val latestActiveIndex = records.withIndex()
            .filter { (_, record) ->
                record.kind == "answer" &&
                    record.questionId?.let { activeAnswers[it] == record.optionId } == true
            }
            .groupBy { it.value.questionId }
            .mapValues { (_, indexed) -> indexed.maxOf { it.index } }

        return records.mapIndexedNotNull { index, record ->
            when (record.kind) {
                "answer" -> {
                    val isCurrent = latestActiveIndex[record.questionId] == index
                    TrialTimelineItem(
                        kind = record.kind,
                        atMillis = record.atMillis,
                        title = record.title.ifBlank { "إجابة في المحاكمة" },
                        body = record.body,
                        statusLabel = if (isCurrent) "ضمن المسار الحالي" else "من مسارٍ سابق أو إجابةٍ مستبدلة",
                        isCurrentAnswer = isCurrent
                    )
                }
                "feedback" -> TrialTimelineItem(record.kind, record.atMillis, "ردّك على طيف الروح", record.body, "ملاحظة محفوظة", false)
                "dialogue" -> TrialTimelineItem(record.kind, record.atMillis, record.title.ifBlank { "حوار مع طيف الروح" }, record.body, "حوار", false)
                "appearance" -> TrialTimelineItem(record.kind, record.atMillis, "لحظة ظهور طيف الروح", record.body, "لحظة في الرحلة", false)
                "symbol" -> TrialTimelineItem(record.kind, record.atMillis, "اختيار رمزي", record.body, "اختيار محفوظ", false)
                "court_verdict" -> TrialTimelineItem(record.kind, record.atMillis, "محضر المحكمة الداخلية", record.body, "موقف قابل للمراجعة", false)
                "profile_snapshot" -> TrialTimelineItem(record.kind, record.atMillis, "محطة في تطور الصورة", record.body, "ملخص محفوظ", false)
                else -> null
            }
        }.sortedByDescending { it.atMillis }
    }

    fun evolutionSummary(change: TrialEvolutionChange): String {
        if (!change.changed) return "لا يظهر تغيرٌ واضح في الأدلة المسجلة بين هاتين المحطتين. هذا لا يعني الثبات؛ قد تحتاج الصورة إلى سياقٍ أو إجاباتٍ إضافية."
        val parts = buildList {
            if (change.answerCountDelta > 0) add("أضيفت ${change.answerCountDelta} إجابة إلى المسار النشط")
            if (change.answerCountDelta < 0) add("تغيّر المسار النشط بمقدار ${-change.answerCountDelta} إجابة")
            if (change.addedLeadingFaces.isNotEmpty()) add("ظهرت إشارات إلى وجوه جديدة")
            if (change.fadedLeadingFaces.isNotEmpty()) add("تراجعت بعض الوجوه عن المراتب الأولى")
            if (change.newTensions.isNotEmpty()) add("ظهرت توترات جديدة تستحق التأمل")
            if (change.resolvedTensions.isNotEmpty()) add("لم تعد بعض التوترات مدعومة بالأدلة الحالية")
        }
        return parts.joinToString("، ") + ". هذه تغيرات في توزيع الأدلة، وليست أحكامًا على الشخصية."
    }
}
