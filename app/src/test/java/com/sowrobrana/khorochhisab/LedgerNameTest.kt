package com.sowrobrana.khorochhisab

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerNameTest {
    @Test
    fun sameDay_singleDate() {
        assertEquals("৬ নভেম্বর, ২০২৬", ledgerName("2026-11-06", "2026-11-06"))
    }

    @Test
    fun range_twoDates() {
        assertEquals(
            "৬ নভেম্বর, ২০২৬ – ৩ ডিসেম্বর, ২০২৬",
            ledgerName("2026-11-06", "2026-12-03")
        )
    }

    @Test
    fun noDates_fallback() {
        assertEquals("হিসাব", ledgerName(null, null))
    }

    @Test
    fun startOnly() {
        assertEquals("৬ নভেম্বর, ২০২৬", ledgerName("2026-11-06", null))
    }
}
