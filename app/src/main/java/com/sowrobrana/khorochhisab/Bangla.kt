package com.sowrobrana.khorochhisab

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Bangla digit + date formatting helpers. */
object Bangla {
    private val digits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun num(n: Long): String {
        val s = n.toString()
        val b = StringBuilder(s.length)
        for (c in s) b.append(if (c in '0'..'9') digits[c - '0'] else c)
        return b.toString()
    }

    fun num(n: Int): String = num(n.toLong())

    private val months = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    /** "2026-11-06" -> "৬ নভেম্বর, ২০২৬" */
    fun formatDate(iso: String): String {
        return try {
            val d = LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE)
            "${num(d.dayOfMonth)} ${months[d.monthValue - 1]}, ${num(d.year)}"
        } catch (e: Exception) {
            iso
        }
    }
}
