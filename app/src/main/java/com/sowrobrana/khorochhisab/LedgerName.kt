package com.sowrobrana.khorochhisab

/**
 * Display name of a closed ledger, from its first and last expense dates.
 * Same day -> one date; otherwise a range, e.g. "৬ নভেম্বর, ২০২৬ – ৩ ডিসেম্বর, ২০২৬".
 */
fun ledgerName(startIso: String?, endIso: String?): String {
    if (startIso == null && endIso == null) return "হিসাব"
    if (startIso == null) return Bangla.formatDate(endIso!!)
    if (endIso == null) return Bangla.formatDate(startIso)
    if (startIso == endIso) return Bangla.formatDate(startIso)
    return "${Bangla.formatDate(startIso)} – ${Bangla.formatDate(endIso)}"
}
