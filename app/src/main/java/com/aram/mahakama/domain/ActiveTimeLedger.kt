package com.aram.mahakama.domain

import kotlin.math.max

/** Pure, testable state machine for cumulative foreground time. */
internal class ActiveTimeLedger(private val monotonicNow: () -> Long) {
    private var persistedBaselineMillis = 0L
    private var trialHasStarted = false
    private var appIsForeground = false
    private var segmentStartedAt: Long? = null
    private var lastCheckpointAt = 0L

    @Synchronized
    fun restore(savedTotalMillis: Long, isTrialStarted: Boolean): Long {
        if (segmentStartedAt == null) {
            persistedBaselineMillis = max(persistedBaselineMillis, savedTotalMillis.coerceAtLeast(0L))
        }
        if (isTrialStarted) trialHasStarted = true
        if (trialHasStarted && appIsForeground && segmentStartedAt == null) startSegmentLocked()
        return elapsedMillisLocked()
    }

    @Synchronized
    fun onForeground(): Long {
        appIsForeground = true
        if (trialHasStarted && segmentStartedAt == null) startSegmentLocked()
        return elapsedMillisLocked()
    }

    @Synchronized
    fun beginTrial(): Long {
        trialHasStarted = true
        if (appIsForeground && segmentStartedAt == null) startSegmentLocked()
        return elapsedMillisLocked()
    }

    /** Stops the active segment and returns only the newly accrued time to persist. */
    @Synchronized
    fun onBackground(): Long {
        appIsForeground = false
        return stopSegmentLocked()
    }

    /** Returns whether the current foreground segment has reached the persistence interval. */
    @Synchronized
    fun checkpointDue(intervalMillis: Long): Boolean {
        if (segmentStartedAt == null) return false
        return monotonicNow() - lastCheckpointAt >= intervalMillis
    }

    /** Persists a segment boundary while leaving a new segment running when appropriate. */
    @Synchronized
    fun checkpoint(): Long {
        val start = segmentStartedAt ?: return 0L
        val now = monotonicNow()
        val delta = (now - start).coerceAtLeast(0L)
        persistedBaselineMillis += delta
        if (appIsForeground && trialHasStarted) {
            segmentStartedAt = now
            lastCheckpointAt = now
        } else {
            segmentStartedAt = null
            lastCheckpointAt = now
        }
        return delta
    }

    @Synchronized
    fun close(): Long {
        appIsForeground = false
        return stopSegmentLocked()
    }

    @Synchronized
    fun elapsedMillis(): Long = elapsedMillisLocked()

    private fun startSegmentLocked() {
        val now = monotonicNow()
        segmentStartedAt = now
        lastCheckpointAt = now
    }

    private fun stopSegmentLocked(): Long {
        val start = segmentStartedAt ?: return 0L
        val now = monotonicNow()
        val delta = (now - start).coerceAtLeast(0L)
        persistedBaselineMillis += delta
        segmentStartedAt = null
        lastCheckpointAt = now
        return delta
    }

    private fun elapsedMillisLocked(): Long {
        val start = segmentStartedAt
        val currentSegment = if (start == null) 0L else (monotonicNow() - start).coerceAtLeast(0L)
        return persistedBaselineMillis + currentSegment
    }
}
