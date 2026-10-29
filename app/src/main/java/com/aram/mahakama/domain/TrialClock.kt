package com.aram.mahakama.domain

import android.os.SystemClock
import com.aram.mahakama.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Cumulative active-time clock. Only foreground time after the trial begins is counted.
 * The pure ledger owns accounting; this class checkpoints deltas to local DataStore.
 */
class TrialClock(private val repository: PreferencesRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ledger = ActiveTimeLedger { SystemClock.elapsedRealtime() }
    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis

    @Volatile private var closed = false
    private val tickerJob: Job = scope.launch {
        while (isActive && !closed) {
            _elapsedMillis.value = ledger.elapsedMillis()
            if (ledger.checkpointDue(CHECKPOINT_INTERVAL_MS)) {
                persistDelta(ledger.checkpoint())
                _elapsedMillis.value = ledger.elapsedMillis()
            }
            delay(1_000L)
        }
    }

    fun restore(savedTotalMillis: Long, trialStarted: Boolean) {
        if (closed) return
        _elapsedMillis.value = ledger.restore(savedTotalMillis, trialStarted)
    }

    fun onForeground() {
        if (closed) return
        _elapsedMillis.value = ledger.onForeground()
    }

    fun onBackground() {
        if (closed) return
        persistDelta(ledger.onBackground())
        _elapsedMillis.value = ledger.elapsedMillis()
    }

    fun beginTrial() {
        if (closed) return
        _elapsedMillis.value = ledger.beginTrial()
        scope.launch { repository.markTrialStarted() }
    }

    /** Flushes the current segment and stops the ticker when the Activity is destroyed. */
    fun checkpointAndClose() {
        if (closed) return
        closed = true
        persistDelta(ledger.close())
        _elapsedMillis.value = ledger.elapsedMillis()
        tickerJob.cancel()
    }

    private fun persistDelta(delta: Long) {
        if (delta > 0L) scope.launch { repository.addActiveTime(delta) }
    }

    companion object {
        private const val CHECKPOINT_INTERVAL_MS = 30_000L
    }
}

fun formatElapsedTime(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatHumanElapsed(millis: Long): String {
    val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0 && minutes > 0 -> "$hours ساعة و$minutes دقيقة"
        hours > 0 -> "$hours ساعة"
        minutes > 0 -> "$minutes دقيقة"
        else -> "أقل من دقيقة"
    }
}
