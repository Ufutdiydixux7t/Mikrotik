package com.example.mikrotik.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageCounterMathTest {
    @Test
    fun calculatesDeltaFromMonotonicCounter() {
        assertEquals(300L, UsageCounterMath.delta(1_000L, 1_300L))
    }

    @Test
    fun countsCurrentValueAfterCounterReset() {
        assertEquals(250L, UsageCounterMath.delta(8_000L, 250L))
    }

    @Test
    fun firstObservationIsBaselineRatherThanPreLoginUsage() {
        assertEquals(0L, UsageCounterMath.delta(-1L, 12_000L))
    }

    @Test
    fun reconcilesOverlappingSourcesWithoutDoubleCounting() {
        assertEquals(2_000L, UsageCounterMath.reconciledTotal(1_000L, 1_600L, 2_000L))
    }

    @Test
    fun cumulativeAdditionSaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, UsageCounterMath.saturatingAdd(Long.MAX_VALUE - 2L, 10L))
    }
}
