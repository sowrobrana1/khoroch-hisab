package com.sowrobrana.khorochhisab

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Share a whole ledger (current or archived) with someone else, keeping the
 * serial order: as formatted text, or as a rendered image. Only what the user
 * chooses to share leaves the phone.
 */
object ShareHelper {

    fun buildShareText(ledgerName: String, expenses: List<Expense>): String {
        val sb = StringBuilder()
        sb.append("খরচের হিসাব — ").append(ledgerName).append("\n")
        val totals = runningTotals(expenses.map { it.amount })
        expenses.forEachIndexed { i, e ->
            sb.append(Bangla.formatDate(e.date)).append(" — ").append(e.place)
                .append(" — ").append(Bangla.num(e.amount)).append(" টাকা")
                .append(" — মোট ").append(Bangla.num(totals[i])).append(" টাকা\n")
        }
        sb.append("সব মিলিয়ে মোট: ").append(Bangla.num(totals.lastOrNull() ?: 0L))
            .append(" টাকা")
        return sb.toString()
    }

    fun shareAsText(context: Context, ledgerName: String, expenses: List<Expense>) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildShareText(ledgerName, expenses))
        }
        context.startActivity(Intent.createChooser(intent, "হিসাব পাঠাও"))
    }

    fun shareAsImage(context: Context, ledgerName: String, expenses: List<Expense>) {
        val dir = File(context.cacheDir, "shared")
        dir.mkdirs()
        val file = File(dir, "hisab_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            renderLedgerBitmap(ledgerName, expenses)
                .compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "হিসাব পাঠাও"))
    }

    /** Renders the ledger (title, rows in order, grand total) onto a white image. */
    private fun renderLedgerBitmap(ledgerName: String, expenses: List<Expense>): Bitmap {
        val width = 1080
        val pad = 56
        val green = Color.parseColor("#1B5E20")
        val greenMid = Color.parseColor("#2E7D32")
        val text = SpannableStringBuilder()

        var s = text.length
        text.append("খরচের হিসাব\n")
        text.setSpan(StyleSpan(Typeface.BOLD), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(AbsoluteSizeSpan(72, true), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(green), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        s = text.length
        text.append(ledgerName).append("\n\n")
        text.setSpan(AbsoluteSizeSpan(44, true), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(Color.DKGRAY), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val totals = runningTotals(expenses.map { it.amount })
        expenses.forEachIndexed { i, e ->
            s = text.length
            text.append(Bangla.formatDate(e.date)).append(" — ").append(e.place).append("\n")
            text.setSpan(AbsoluteSizeSpan(44, true), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            s = text.length
            text.append(Bangla.num(e.amount)).append(" টাকা — মোট ")
                .append(Bangla.num(totals[i])).append(" টাকা\n\n")
            text.setSpan(AbsoluteSizeSpan(44, true), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(ForegroundColorSpan(greenMid), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        s = text.length
        text.append("সব মিলিয়ে মোট: ").append(Bangla.num(totals.lastOrNull() ?: 0L))
            .append(" টাকা")
        text.setSpan(StyleSpan(Typeface.BOLD), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(AbsoluteSizeSpan(52, true), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(green), s, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 44f
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width - pad * 2)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)
            .build()
        val bmp = Bitmap.createBitmap(width, layout.height + pad * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate(pad.toFloat(), pad.toFloat())
        layout.draw(canvas)
        canvas.restore()
        return bmp
    }
}
