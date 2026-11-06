package com.sowrobrana.khorochhisab

/**
 * Running totals: each row's total is that row's amount plus all rows above it.
 * Example: [500, 100, 200] -> [500, 600, 800]. The last value is the grand total.
 */
fun runningTotals(amounts: List<Long>): List<Long> {
    val out = ArrayList<Long>(amounts.size)
    var sum = 0L
    for (a in amounts) {
        sum += a
        out.add(sum)
    }
    return out
}
