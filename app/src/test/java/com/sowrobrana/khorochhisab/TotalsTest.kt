package com.sowrobrana.khorochhisab

import org.junit.Assert.assertEquals
import org.junit.Test

class TotalsTest {
    @Test
    fun runningTotals_example() {
        // 500 + 100 + 200 -> totals 500 / 600 / 800; last row is the grand total.
        assertEquals(listOf(500L, 600L, 800L), runningTotals(listOf(500L, 100L, 200L)))
    }

    @Test
    fun runningTotals_empty() {
        assertEquals(emptyList<Long>(), runningTotals(emptyList()))
    }

    @Test
    fun runningTotals_single() {
        assertEquals(listOf(250L), runningTotals(listOf(250L)))
    }
}
