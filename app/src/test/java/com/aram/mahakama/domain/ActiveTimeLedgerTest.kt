package com.aram.mahakama.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveTimeLedgerTest {
    @Test
    fun countsOnlyForegroundTimeAfterTrialBegins() {
        var now = 1_000L
        val ledger = ActiveTimeLedger { now }
        ledger.onForeground()
        now += 50_000L
        assertEquals(0L, ledger.elapsedMillis())

        ledger.beginTrial()
        now += 5_000L
        assertEquals(5_000L, ledger.elapsedMillis())
        assertEquals(5_000L, ledger.onBackground())

        now += 90_000L // Background time must not count.
        ledger.onForeground()
        now += 3_000L
        assertEquals(8_000L, ledger.elapsedMillis())
        assertEquals(3_000L, ledger.onBackground())
        assertEquals(8_000L, ledger.elapsedMillis())
    }

    @Test
    fun checkpointPersistsDeltaWithoutDoubleCounting() {
        var now = 0L
        val ledger = ActiveTimeLedger { now }
        ledger.restore(12_000L, true)
        ledger.onForeground()
        now = 30_000L
        assertTrue(ledger.checkpointDue(30_000L))
        assertEquals(30_000L, ledger.checkpoint())
        assertEquals(42_000L, ledger.elapsedMillis())

        now = 35_000L
        assertEquals(5_000L, ledger.onBackground())
        assertEquals(47_000L, ledger.elapsedMillis())
        ledger.restore(47_000L, true)
        assertEquals(47_000L, ledger.elapsedMillis())
        assertFalse(ledger.checkpointDue(30_000L))
    }

    @Test
    fun negativeOrStaleSavedTotalsNeverReduceElapsedTime() {
        var now = 100L
        val ledger = ActiveTimeLedger { now }
        ledger.restore(5_000L, true)
        ledger.onForeground()
        now += 1_500L
        assertEquals(6_500L, ledger.elapsedMillis())
        assertEquals(6_500L, ledger.restore(-12L, true))
    }
}
