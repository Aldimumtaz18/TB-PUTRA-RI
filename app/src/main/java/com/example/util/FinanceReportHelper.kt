package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.ExpenseEntity
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.model.UserRole
import com.example.ui.viewmodel.DailyIncomeSummary
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinanceReportData(
  val storeSettings: StoreSettings,
  val periodLabel: String,
  val generatedBy: String,
  val generatedByRole: UserRole = storeSettings.activeRole ?: UserRole.OWNER,
  val generatedAtMillis: Long = System.currentTimeMillis(),
  val paidRevenue: Long,
  val cashRevenue: Long = 0L,
  val transferRevenue: Long = 0L,
  val paidCount: Int,
  val totalExpenses: Long,
  val operationalExpenses: Long = 0L,
  val kulakanExpenses: Long = 0L,
  val expenseCount: Int,
  val grossProfit: Long,
  val netProfit: Long = grossProfit,
  val inventoryCostValue: Long = 0L,
  val unpaidReceivables: Long,
  val unpaidKasbonCount: Int,
  val totalTxCount: Int,
  val dailySummaries: List<DailyIncomeSummary>,
  val expenses: List<ExpenseEntity>,
  val unpaidKasbonList: List<TransactionEntity>,
  val paidTransactions: List<TransactionEntity>,
) {
  val netCashFlow: Long
    get() = paidRevenue - totalExpenses
}

data class ReportSignatoryInfo(
  val headerText: String,
  val roleTitle: String,
  val signerName: String,
  val printedBySummary: String,
)

object FinanceReportHelper {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN_H = 34f
  private const val MARGIN_TOP = 32f
  private const val MARGIN_BOTTOM = 42f
  private const val RIGHT_EDGE = PAGE_WIDTH - MARGIN_H
  private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_H * 2f

  private const val navyDark = 0xFF0F294A.toInt()
  private const val navyMuted = 0xFF334155.toInt()
  private const val inkDark = 0xFF0F172A.toInt()
  private const val inkMuted = 0xFF64748B.toInt()
  private const val borderSoft = 0xFFCBD5E1.toInt()
  private const val dividerLight = 0xFFE2E8F0.toInt()
  private const val surfaceSoft = 0xFFF8FAFC.toInt()
  private const val orangeAccent = 0xFFEA580C.toInt()
  private const val emeraldAccent = 0xFF15803D.toInt()
  private const val emeraldSoft = 0xFFF0FDF4.toInt()
  private const val redAccent = 0xFFDC2626.toInt()
  private const val redSoft = 0xFFFEF2F2.toInt()
  private const val amberAccent = 0xFFB45309.toInt()
  private const val amberSoft = 0xFFFFFBEB.toInt()
  private const val skyAccent = 0xFF0369A1.toInt()
  private const val skySoft = 0xFFEFF6FF.toInt()

  fun generateReportPdfFile(context: Context, data: FinanceReportData): File {
    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val stamp =
      SimpleDateFormat("yyyyMMdd_HHmmss", Locale("id", "ID")).format(Date(data.generatedAtMillis))
    val outFile = File(reportsDir, "Laporan_Keuangan_TB_PUTRA_RI_$stamp.pdf")

    val pdf = buildFinanceReportPdfDocument(context, data)
    try {
      FileOutputStream(outFile).use { fos ->
        pdf.writeTo(fos)
      }
    } finally {
      pdf.close()
    }
    return outFile
  }

  fun printFinanceReportPdf(context: Context, data: FinanceReportData) {
    try {
      val pdfFile = generateReportPdfFile(context, data)
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
      if (printManager == null) {
        Toast.makeText(context, "Layanan cetak PDF tidak tersedia di perangkat ini", Toast.LENGTH_SHORT)
          .show()
        return
      }
      val jobName = "Laporan_Keuangan_TB_PUTRA_RI_${data.periodLabel.replace(" ", "_")}"
      val adapter =
        object : PrintDocumentAdapter() {
          override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?,
          ) {
            if (cancellationSignal?.isCanceled == true) {
              callback?.onLayoutCancelled()
              return
            }
            val info =
              PrintDocumentInfo.Builder(pdfFile.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback?.onLayoutFinished(info, true)
          }

          override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?,
          ) {
            if (cancellationSignal?.isCanceled == true) {
              callback?.onWriteCancelled()
              return
            }
            if (destination == null) {
              callback?.onWriteFailed("Tujuan cetak tidak valid")
              return
            }
            try {
              FileInputStream(pdfFile).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                  input.copyTo(output)
                }
              }
              callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
              callback?.onWriteFailed(e.message ?: "Gagal menulis file PDF")
            }
          }
        }
      printManager.print(
        jobName,
        adapter,
        PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
      )
    } catch (_: Exception) {
      Toast.makeText(context, "Gagal menyiapkan file PDF laporan keuangan", Toast.LENGTH_SHORT).show()
    }
  }

  fun shareReportToWhatsApp(context: Context, data: FinanceReportData) {
    try {
      val pdfFile = generateReportPdfFile(context, data)
      val pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
      val summaryText = buildWhatsAppSummaryText(data)

      val baseIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "application/pdf"
          putExtra(Intent.EXTRA_STREAM, pdfUri)
          putExtra(Intent.EXTRA_TEXT, summaryText)
          putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan TB PUTRA RI (${data.periodLabel})")
          clipData = ClipData.newRawUri("Laporan Keuangan TB PUTRA RI", pdfUri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

      val pm = context.packageManager
      val waPackage =
        listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
          runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }

      if (waPackage != null) {
        val waIntent = Intent(baseIntent).apply { setPackage(waPackage) }
        try {
          context.startActivity(waIntent)
          return
        } catch (_: Exception) {
          // Fallback to chooser below
        }
      }

      context.startActivity(
        Intent.createChooser(baseIntent, "Kirim Laporan Keuangan PDF ke WhatsApp")
      )
    } catch (_: Exception) {
      Toast.makeText(context, "Gagal membuat PDF laporan untuk dikirim ke WhatsApp", Toast.LENGTH_SHORT)
        .show()
    }
  }

  fun shareReportPdfGeneric(context: Context, data: FinanceReportData) {
    try {
      val pdfFile = generateReportPdfFile(context, data)
      val pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
      val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "application/pdf"
          putExtra(Intent.EXTRA_STREAM, pdfUri)
          putExtra(Intent.EXTRA_TEXT, buildWhatsAppSummaryText(data))
          putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan TB PUTRA RI (${data.periodLabel})")
          clipData = ClipData.newRawUri("Laporan Keuangan TB PUTRA RI", pdfUri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan Laporan PDF"))
    } catch (_: Exception) {
      Toast.makeText(context, "Gagal membagikan file PDF laporan keuangan", Toast.LENGTH_SHORT).show()
    }
  }

  fun buildReportSignatoryInfo(data: FinanceReportData): ReportSignatoryInfo {
    val storeName = data.storeSettings.storeName.ifBlank { "TB PUTRA RI" }
    val isCashier = data.generatedByRole == UserRole.CASHIER
    val roleLabel = if (isCashier) "Kasir" else "Pemilik"
    val signerName = data.generatedBy.ifBlank { roleLabel }
    return ReportSignatoryInfo(
      headerText = if (isCashier) "Dicetak oleh," else "Mengetahui,",
      roleTitle = "$roleLabel $storeName",
      signerName = signerName,
      printedBySummary = "$signerName ($roleLabel)",
    )
  }

  fun buildWhatsAppSummaryText(data: FinanceReportData): String {
    val storeTitle = data.storeSettings.storeName.ifBlank { "TB PUTRA RI" }
    val signatory = buildReportSignatoryInfo(data)
    return buildString {
      appendLine("*LAPORAN KEUANGAN $storeTitle*")
      appendLine("Periode : ${data.periodLabel}")
      appendLine("Dicetak : ${formatDateTime(data.generatedAtMillis)} • ${signatory.printedBySummary}")
      appendLine("--------------------------------")
      appendLine("Uang Masuk        : ${formatRupiah(data.paidRevenue)} (${data.paidCount} nota)")
      appendLine("  • Tunai (Laci)  : ${formatRupiah(data.cashRevenue)}")
      appendLine("  • Transfer/QRIS : ${formatRupiah(data.transferRevenue)}")
      appendLine("Uang Keluar       : ${formatRupiah(data.totalExpenses)} (${data.expenseCount} catatan)")
      appendLine("  • Operasional   : ${formatRupiah(data.operationalExpenses)}")
      appendLine("  • Kulakan Stok  : ${formatRupiah(data.kulakanExpenses)}")
      appendLine("Sisa Uang Toko    : ${formatRupiah(data.netCashFlow)}")
      appendLine("Untung Kotor      : ${formatRupiah(data.grossProfit)}")
      appendLine("Untung Bersih     : ${formatRupiah(data.netProfit)}")
      appendLine("Kasbon Belum Lunas: ${formatRupiah(data.unpaidReceivables)} (${data.unpaidKasbonCount} bon)")
      appendLine("Nilai Stok Gudang : ${formatRupiah(data.inventoryCostValue)}")
      appendLine("--------------------------------")
      append("Terlampir file PDF rincian laporan keuangan lengkap.")
    }
  }

  private fun buildFinanceReportPdfDocument(context: Context, data: FinanceReportData): PdfDocument {
    val document = PdfDocument()
    val logoBitmap =
      runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.tb_putra_ri_logo) }
        .getOrNull()

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyDark
        textSize = 16.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    val subTitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkMuted
        textSize = 8.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      }
    val subTitleBoldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyMuted
        textSize = 8.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    val badgeTitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val sectionTitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyDark
        textSize = 10.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    val tableHeaderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 8f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    val bodyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 8f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      }
    val bodyBoldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 8f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    val monoPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 7.8f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
      }
    val monoBoldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 7.8f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      }
    val linePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerLight
        strokeWidth = 0.8f
      }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderSoft
        style = Paint.Style.STROKE
        strokeWidth = 0.9f
      }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun centerY(top: Float, height: Float, paint: Paint): Float =
      top + height / 2f - (paint.ascent() + paint.descent()) / 2f

    fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
      if (paint.measureText(text) <= maxWidth) return text
      var trimmed = text
      while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) {
        trimmed = trimmed.dropLast(1)
      }
      return if (trimmed.isEmpty()) "" else "$trimmed…"
    }

    fun wrapLines(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 2): List<String> {
      val clean = text.trim().replace("\n", " ")
      if (clean.isEmpty()) return listOf("-")
      if (paint.measureText(clean) <= maxWidth) return listOf(clean)

      val words = clean.split(Regex("\\s+"))
      val lines = mutableListOf<String>()
      var current = ""
      for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
          current = candidate
        } else {
          if (current.isNotEmpty()) {
            lines.add(current)
            if (lines.size == maxLines - 1) {
              val remaining = words.drop(words.indexOf(word)).joinToString(" ")
              lines.add(ellipsize(remaining, paint, maxWidth))
              return lines
            }
          } else {
            lines.add(ellipsize(word, paint, maxWidth))
            if (lines.size >= maxLines) return lines
          }
          current = word
        }
      }
      if (current.isNotEmpty() && lines.size < maxLines) {
        lines.add(ellipsize(current, paint, maxWidth))
      }
      return lines
    }

    var pageNumber = 0
    var currentPage: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var cursorY = MARGIN_TOP

    fun finishCurrentPage() {
      val page = currentPage ?: return
      val c = page.canvas
      val footerY = PAGE_HEIGHT - 18f
      c.drawLine(MARGIN_H, footerY - 12f, RIGHT_EDGE, footerY - 12f, linePaint)
      val footerLeft =
        Paint(subTitlePaint).apply {
          textAlign = Paint.Align.LEFT
          textSize = 7.5f
        }
      val footerRight =
        Paint(subTitlePaint).apply {
          textAlign = Paint.Align.RIGHT
          textSize = 7.5f
        }
      val storeTitle = data.storeSettings.storeName.ifBlank { "TB PUTRA RI" }
      c.drawText("$storeTitle • Laporan Keuangan Resmi", MARGIN_H, footerY, footerLeft)
      c.drawText("Halaman $pageNumber", RIGHT_EDGE, footerY, footerRight)
      document.finishPage(page)
      currentPage = null
      canvas = null
    }

    fun startNewPage(isContinuation: Boolean) {
      finishCurrentPage()
      pageNumber += 1
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      val newPage = document.startPage(pageInfo)
      currentPage = newPage
      canvas = newPage.canvas
      cursorY = MARGIN_TOP

      if (isContinuation) {
        val contPaint =
          Paint(subTitlePaint).apply {
            textAlign = Paint.Align.LEFT
            textSize = 8.5f
            color = navyDark
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        val periodRight =
          Paint(subTitlePaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 8f
          }
        newPage.canvas.drawText(
          "${data.storeSettings.storeName.ifBlank { "TB PUTRA RI" }} — Laporan Keuangan",
          MARGIN_H,
          cursorY + 10f,
          contPaint,
        )
        newPage.canvas.drawText(
          ellipsize("Periode: ${data.periodLabel}", periodRight, 230f),
          RIGHT_EDGE,
          cursorY + 10f,
          periodRight,
        )
        cursorY += 17f
        newPage.canvas.drawLine(MARGIN_H, cursorY, RIGHT_EDGE, cursorY, linePaint)
        cursorY += 13f
      }
    }

    fun ensureSpace(neededHeight: Float, repeatHeader: (() -> Unit)? = null) {
      if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
        startNewPage(isContinuation = true)
        repeatHeader?.invoke()
      }
    }

    // ==================== HALAMAN 1: KOP & RINGKASAN ====================
    startNewPage(isContinuation = false)
    val c1 = canvas!!

    val storeName = data.storeSettings.storeName.ifBlank { "TB PUTRA RI" }
    var headerLeftX = MARGIN_H
    if (logoBitmap != null) {
      val logoRect = RectF(MARGIN_H, cursorY, MARGIN_H + 42f, cursorY + 42f)
      c1.drawBitmap(
        logoBitmap,
        null,
        logoRect,
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
      )
      c1.drawRoundRect(logoRect, 6f, 6f, borderPaint)
      headerLeftX = MARGIN_H + 52f
    }
    c1.drawText(storeName, headerLeftX, cursorY + 14f, titlePaint)
    val addr = data.storeSettings.storeAddress.ifBlank { "Toko Bahan Bangunan" }
    val phone = data.storeSettings.storePhone.ifBlank { "-" }
    c1.drawText(ellipsize(addr, subTitlePaint, 280f), headerLeftX, cursorY + 27f, subTitlePaint)
    c1.drawText("Telp / WA: $phone", headerLeftX, cursorY + 39f, subTitlePaint)

    // Right report pill badge
    val badgeRect = RectF(RIGHT_EDGE - 146f, cursorY + 2f, RIGHT_EDGE, cursorY + 24f)
    fillPaint.color = orangeAccent
    c1.drawRoundRect(badgeRect, 6f, 6f, fillPaint)
    c1.drawText("LAPORAN KEUANGAN", badgeRect.centerX(), centerY(badgeRect.top, badgeRect.height(), badgeTitlePaint), badgeTitlePaint)

    val rightSub = Paint(subTitlePaint).apply {
      textAlign = Paint.Align.RIGHT
      textSize = 7.8f
    }
    c1.drawText("Ringkasan Pembukuan Toko", RIGHT_EDGE, cursorY + 37f, rightSub)

    cursorY += 50f

    // Clean Info Strip (anti-overlap for long periods & printed timestamp)
    val metaStrip = RectF(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + 24f)
    fillPaint.color = surfaceSoft
    c1.drawRoundRect(metaStrip, 6f, 6f, fillPaint)
    c1.drawRoundRect(metaStrip, 6f, 6f, borderPaint)

    val signatory = buildReportSignatoryInfo(data)
    val metaCenterY = centerY(metaStrip.top, metaStrip.height(), subTitlePaint)
    c1.drawText(
      ellipsize("Periode: ${data.periodLabel}", subTitleBoldPaint, 275f),
      MARGIN_H + 10f,
      metaCenterY,
      subTitleBoldPaint,
    )
    val printedRight = Paint(subTitlePaint).apply { textAlign = Paint.Align.RIGHT }
    c1.drawText(
      ellipsize("Dicetak: ${formatDateShort(data.generatedAtMillis)} • ${signatory.printedBySummary}", printedRight, 225f),
      RIGHT_EDGE - 10f,
      metaCenterY,
      printedRight,
    )
    cursorY += 34f

    // Hero Card: Sisa Uang Toko
    val heroRect = RectF(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + 64f)
    fillPaint.color = navyDark
    c1.drawRoundRect(heroRect, 10f, 10f, fillPaint)

    val heroLabelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        textSize = 8.2f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
    val heroValuePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 19f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      }
    val heroHintPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 7.8f
      }
    c1.drawText("SISA UANG TOKO (UANG MASUK - UANG KELUAR)", MARGIN_H + 14f, cursorY + 18f, heroLabelPaint)
    c1.drawText(formatRupiah(data.netCashFlow), MARGIN_H + 14f, cursorY + 41f, heroValuePaint)
    c1.drawText(
      "Berdasarkan ${data.totalTxCount} transaksi penjualan & ${data.expenseCount} catatan pengeluaran",
      MARGIN_H + 14f,
      cursorY + 55f,
      heroHintPaint,
    )

    val statusText =
      when {
        data.netCashFlow > 0L -> "Kas Lebih"
        data.netCashFlow < 0L -> "Keluar Lebih Besar"
        else -> "Seimbang"
      }
    val statusPillColor =
      when {
        data.netCashFlow > 0L -> emeraldAccent
        data.netCashFlow < 0L -> redAccent
        else -> navyMuted
      }
    val statusPillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 8f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val statusPillW = (statusPillPaint.measureText(statusText) + 20f).coerceAtLeast(80f)
    val statusPillRect = RectF(RIGHT_EDGE - 14f - statusPillW, cursorY + 11f, RIGHT_EDGE - 14f, cursorY + 29f)
    fillPaint.color = statusPillColor
    c1.drawRoundRect(statusPillRect, 9f, 9f, fillPaint)
    c1.drawText(
      statusText,
      statusPillRect.centerX(),
      centerY(statusPillRect.top, statusPillRect.height(), statusPillPaint),
      statusPillPaint,
    )

    cursorY += 70f

    // Compact breakdown strip: Tunai vs Transfer/QRIS & Nilai Aset Stok Gudang
    val splitStrip = RectF(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + 22f)
    fillPaint.color = surfaceSoft
    c1.drawRoundRect(splitStrip, 6f, 6f, fillPaint)
    c1.drawRoundRect(splitStrip, 6f, 6f, borderPaint)
    val splitText =
      "Tunai: ${formatRupiah(data.cashRevenue)}  •  Transfer/QRIS: ${formatRupiah(data.transferRevenue)}  •  Stok Gudang (Modal): ${formatRupiah(data.inventoryCostValue)}"
    c1.drawText(
      ellipsize(splitText, subTitleBoldPaint, CONTENT_WIDTH - 16f),
      MARGIN_H + 8f,
      centerY(splitStrip.top, splitStrip.height(), subTitleBoldPaint),
      subTitleBoldPaint,
    )
    cursorY += 30f

    // 4 Kartu Ringkasan (Grid 2x2 dengan accent bar kiri)
    val gap = 10f
    val cardW = (CONTENT_WIDTH - gap) / 2f
    val cardH = 54f

    fun drawStatBox(
      x: Float,
      y: Float,
      label: String,
      value: String,
      subtitle: String,
      bgColor: Int,
      accentColor: Int,
    ) {
      val rect = RectF(x, y, x + cardW, y + cardH)
      fillPaint.color = bgColor
      c1.drawRoundRect(rect, 8f, 8f, fillPaint)
      c1.drawRoundRect(rect, 8f, 8f, borderPaint)

      // Left vertical accent strip
      val accentRect = RectF(x, y, x + 4.5f, y + cardH)
      fillPaint.color = accentColor
      c1.drawRoundRect(accentRect, 4f, 4f, fillPaint)

      val lPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = inkMuted
          textSize = 7.8f
          typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
      val vPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = accentColor
          textSize = 12.5f
          typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
      val sPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = inkMuted
          textSize = 7.3f
        }
      c1.drawText(label, x + 13f, y + 16f, lPaint)
      c1.drawText(ellipsize(value, vPaint, cardW - 22f), x + 13f, y + 33.5f, vPaint)
      c1.drawText(ellipsize(subtitle, sPaint, cardW - 22f), x + 13f, y + 46f, sPaint)
    }

    drawStatBox(
      MARGIN_H,
      cursorY,
      "UANG MASUK (KAS)",
      formatRupiah(data.paidRevenue),
      "${data.paidCount} nota • Tunai ${formatRupiah(data.cashRevenue)} • TF ${formatRupiah(data.transferRevenue)}",
      skySoft,
      skyAccent,
    )
    drawStatBox(
      MARGIN_H + cardW + gap,
      cursorY,
      "UANG KELUAR (BIAYA)",
      formatRupiah(data.totalExpenses),
      "Op: ${formatRupiah(data.operationalExpenses)} • Kulakan: ${formatRupiah(data.kulakanExpenses)}",
      redSoft,
      redAccent,
    )
    cursorY += cardH + 8f
    drawStatBox(
      MARGIN_H,
      cursorY,
      "UNTUNG BERSIH (LABA)",
      formatRupiah(data.netProfit),
      "Kotor ${formatRupiah(data.grossProfit)} - Op. ${formatRupiah(data.operationalExpenses)}",
      if (data.netProfit >= 0L) emeraldSoft else redSoft,
      if (data.netProfit >= 0L) emeraldAccent else redAccent,
    )
    drawStatBox(
      MARGIN_H + cardW + gap,
      cursorY,
      "KASBON BELUM LUNAS",
      formatRupiah(data.unpaidReceivables),
      "${data.unpaidKasbonCount} bon (sisa belum dibayar)",
      amberSoft,
      amberAccent,
    )
    cursorY += cardH + 20f

    fun drawSectionHeading(title: String, countBadge: String? = null) {
      val c = canvas!!
      c.drawText(title, MARGIN_H, cursorY + 11f, sectionTitlePaint)
      if (!countBadge.isNullOrBlank()) {
        val rightP =
          Paint(subTitleBoldPaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 8f
            color = inkMuted
          }
        c.drawText(countBadge, RIGHT_EDGE, cursorY + 11f, rightP)
      }
      cursorY += 17f
    }

    fun drawTableHeaderBar(drawLabels: (Canvas, Float, Float) -> Unit) {
      val c = canvas!!
      val h = 21f
      val hRect = RectF(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + h)
      fillPaint.color = navyDark
      c.drawRoundRect(hRect, 4f, 4f, fillPaint)
      drawLabels(c, cursorY, h)
      cursorY += h
    }

    val headerH = 21f

    // ==================== 1. CATATAN HARIAN ====================
    if (data.dailySummaries.isNotEmpty()) {
      // Col X anchors (all within 34..561, width 527)
      val colDateX = MARGIN_H + 8f         // 42..152 (110)
      val colNotaRight = MARGIN_H + 150f   // 184 (32)
      val colMasukRight = MARGIN_H + 244f  // 278 (94)
      val colKeluarRight = MARGIN_H + 338f // 372 (94)
      val colSisaRight = MARGIN_H + 434f   // 468 (96)
      val colUntungRight = RIGHT_EDGE - 8f // 553 (85)

      val drawDailyHeader: () -> Unit = {
        drawTableHeaderBar { c, top, h ->
          val y = centerY(top, h, tableHeaderPaint)
          val rightHdr = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
          c.drawText("Tanggal", colDateX, y, tableHeaderPaint)
          c.drawText("Nota", colNotaRight, y, rightHdr)
          c.drawText("Masuk", colMasukRight, y, rightHdr)
          c.drawText("Keluar", colKeluarRight, y, rightHdr)
          c.drawText("Sisa Uang", colSisaRight, y, rightHdr)
          c.drawText("U. Bersih", colUntungRight, y, rightHdr)
        }
      }

      ensureSpace(64f)
      drawSectionHeading("1. CATATAN HARIAN", "${data.dailySummaries.size} hari")
      drawDailyHeader()

      val rowH = 20f
      data.dailySummaries.forEachIndexed { idx, day ->
        ensureSpace(rowH + 2f) {
          drawSectionHeading("1. CATATAN HARIAN (LANJUTAN)")
          drawDailyHeader()
        }
        val c = canvas!!
        if (idx % 2 == 1) {
          fillPaint.color = surfaceSoft
          c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, fillPaint)
        }
        val netDay = day.paidRevenue - day.totalExpenses
        val yBody = centerY(cursorY, rowH, bodyPaint)
        val yMono = centerY(cursorY, rowH, monoPaint)
        val rightMono = Paint(monoPaint).apply { textAlign = Paint.Align.RIGHT }
        val netMono =
          Paint(monoBoldPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = if (netDay >= 0) emeraldAccent else redAccent
          }
        val labaMono =
          Paint(monoPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = if (day.netProfit >= 0) emeraldAccent else redAccent
          }
        c.drawText(ellipsize(day.dayLabel, bodyPaint, 108f), colDateX, yBody, bodyPaint)
        c.drawText("${day.transactionCount}", colNotaRight, yMono, rightMono)
        c.drawText(formatRupiah(day.paidRevenue), colMasukRight, yMono, rightMono)
        c.drawText(formatRupiah(day.totalExpenses), colKeluarRight, yMono, rightMono)
        c.drawText(formatRupiah(netDay), colSisaRight, yMono, netMono)
        c.drawText(formatRupiah(day.netProfit), colUntungRight, yMono, labaMono)
        c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, borderPaint)
        cursorY += rowH
      }

      // Summary Row Total Harian
      ensureSpace(22f)
      val cTotal = canvas!!
      val totalH = 22f
      fillPaint.color = skySoft
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, fillPaint)
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, borderPaint)
      val yTotalB = centerY(cursorY, totalH, bodyBoldPaint)
      val yTotalM = centerY(cursorY, totalH, monoBoldPaint)
      val sumNotes = data.dailySummaries.sumOf { it.transactionCount }
      val sumIn = data.dailySummaries.sumOf { it.paidRevenue }
      val sumOut = data.dailySummaries.sumOf { it.totalExpenses }
      val sumNet = sumIn - sumOut
      val sumNetProfit = data.dailySummaries.sumOf { it.netProfit }

      val rightBold = Paint(monoBoldPaint).apply { textAlign = Paint.Align.RIGHT }
      val netTotalPaint =
        Paint(monoBoldPaint).apply {
          textAlign = Paint.Align.RIGHT
          color = if (sumNet >= 0) emeraldAccent else redAccent
        }
      val labaTotalPaint =
        Paint(monoBoldPaint).apply {
          textAlign = Paint.Align.RIGHT
          color = if (sumNetProfit >= 0) emeraldAccent else redAccent
        }
      cTotal.drawText("TOTAL", colDateX, yTotalB, bodyBoldPaint)
      cTotal.drawText("$sumNotes", colNotaRight, yTotalM, rightBold)
      cTotal.drawText(formatRupiah(sumIn), colMasukRight, yTotalM, rightBold)
      cTotal.drawText(formatRupiah(sumOut), colKeluarRight, yTotalM, rightBold)
      cTotal.drawText(formatRupiah(sumNet), colSisaRight, yTotalM, netTotalPaint)
      cTotal.drawText(formatRupiah(sumNetProfit), colUntungRight, yTotalM, labaTotalPaint)
      cursorY += totalH + 18f
    }

    // ==================== 2. RINCIAN UANG KELUAR ====================
    if (data.expenses.isNotEmpty()) {
      val colNoX = MARGIN_H + 8f
      val colTglX = MARGIN_H + 34f
      val colKatX = MARGIN_H + 126f
      val colKetX = MARGIN_H + 220f
      val colNomRight = RIGHT_EDGE - 8f

      val drawExpHeader: () -> Unit = {
        drawTableHeaderBar { c, top, h ->
          val y = centerY(top, h, tableHeaderPaint)
          val rightHdr = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
          c.drawText("No", colNoX, y, tableHeaderPaint)
          c.drawText("Tanggal", colTglX, y, tableHeaderPaint)
          c.drawText("Kategori", colKatX, y, tableHeaderPaint)
          c.drawText("Keterangan", colKetX, y, tableHeaderPaint)
          c.drawText("Nominal", colNomRight, y, rightHdr)
        }
      }

      ensureSpace(64f)
      drawSectionHeading("2. RINCIAN UANG KELUAR", "${data.expenses.size} catatan")
      drawExpHeader()

      data.expenses.forEachIndexed { idx, exp ->
        val descRaw = if (exp.notes.isNotBlank()) "${exp.title} (${exp.notes})" else exp.title
        val descLines = wrapLines(descRaw, bodyPaint, 224f, maxLines = 2)
        val rowH = if (descLines.size > 1) 30f else 20f

        ensureSpace(rowH + 2f) {
          drawSectionHeading("2. RINCIAN UANG KELUAR (LANJUTAN)")
          drawExpHeader()
        }
        val c = canvas!!
        if (idx % 2 == 1) {
          fillPaint.color = surfaceSoft
          c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, fillPaint)
        }
        val ySingle = centerY(cursorY, rowH, bodyPaint)
        val rightRed =
          Paint(monoBoldPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = redAccent
          }
        c.drawText("${idx + 1}", colNoX, ySingle, bodyPaint)
        c.drawText(ellipsize(formatDateShort(exp.timestamp), bodyPaint, 88f), colTglX, ySingle, bodyPaint)
        c.drawText(ellipsize(exp.category, bodyBoldPaint, 90f), colKatX, ySingle, bodyBoldPaint)
        if (descLines.size == 1) {
          c.drawText(descLines[0], colKetX, ySingle, bodyPaint)
        } else {
          c.drawText(descLines[0], colKetX, cursorY + 12f, bodyPaint)
          c.drawText(descLines[1], colKetX, cursorY + 23f, bodyPaint)
        }
        c.drawText(formatRupiah(exp.amount), colNomRight, centerY(cursorY, rowH, monoBoldPaint), rightRed)
        c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, borderPaint)
        cursorY += rowH
      }

      ensureSpace(24f)
      val cTotal = canvas!!
      val totalH = 22f
      fillPaint.color = redSoft
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, fillPaint)
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, borderPaint)
      cTotal.drawText("TOTAL UANG KELUAR", colNoX, centerY(cursorY, totalH, bodyBoldPaint), bodyBoldPaint)
      val rightTotalRed =
        Paint(monoBoldPaint).apply {
          textAlign = Paint.Align.RIGHT
          color = redAccent
        }
      cTotal.drawText(
        formatRupiah(data.totalExpenses),
        colNomRight,
        centerY(cursorY, totalH, monoBoldPaint),
        rightTotalRed,
      )
      cursorY += totalH + 18f
    }

    // ==================== 3. KASBON BELUM LUNAS ====================
    if (data.unpaidKasbonList.isNotEmpty()) {
      val colNoX = MARGIN_H + 8f
      val colNotaX = MARGIN_H + 34f
      val colPelangganX = MARGIN_H + 132f
      val colTglX = MARGIN_H + 344f
      val colTagihanRight = RIGHT_EDGE - 8f

      val drawKasbonHeader: () -> Unit = {
        drawTableHeaderBar { c, top, h ->
          val y = centerY(top, h, tableHeaderPaint)
          val rightHdr = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
          c.drawText("No", colNoX, y, tableHeaderPaint)
          c.drawText("No. Nota", colNotaX, y, tableHeaderPaint)
          c.drawText("Pelanggan", colPelangganX, y, tableHeaderPaint)
          c.drawText("Tanggal", colTglX, y, tableHeaderPaint)
          c.drawText("Tagihan", colTagihanRight, y, rightHdr)
        }
      }

      ensureSpace(64f)
      drawSectionHeading("3. DAFTAR KASBON BELUM LUNAS", "${data.unpaidKasbonList.size} bon")
      drawKasbonHeader()

      data.unpaidKasbonList.forEachIndexed { idx, tx ->
        val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
        val baseCust =
          if (tx.customerPhone.isNotBlank()) "${tx.customerName} (${tx.customerPhone})"
          else tx.customerName
        val custRaw =
          if (tx.amountPaid > 0L) "$baseCust • DP/Cicil ${formatRupiah(tx.amountPaid)}"
          else baseCust
        val custLines = wrapLines(custRaw, bodyPaint, 206f, maxLines = 2)
        val rowH = if (custLines.size > 1) 30f else 20f

        ensureSpace(rowH + 2f) {
          drawSectionHeading("3. DAFTAR KASBON BELUM LUNAS (LANJUTAN)")
          drawKasbonHeader()
        }
        val c = canvas!!
        if (idx % 2 == 1) {
          fillPaint.color = surfaceSoft
          c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, fillPaint)
        }
        val ySingle = centerY(cursorY, rowH, bodyPaint)
        val rightAmber =
          Paint(monoBoldPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = amberAccent
          }
        c.drawText("${idx + 1}", colNoX, ySingle, bodyPaint)
        c.drawText(ellipsize(tx.invoiceNumber, monoPaint, 92f), colNotaX, centerY(cursorY, rowH, monoPaint), monoPaint)
        if (custLines.size == 1) {
          c.drawText(custLines[0], colPelangganX, ySingle, bodyPaint)
        } else {
          c.drawText(custLines[0], colPelangganX, cursorY + 12f, bodyPaint)
          c.drawText(custLines[1], colPelangganX, cursorY + 23f, bodyPaint)
        }
        c.drawText(ellipsize(formatDateShort(tx.timestamp), bodyPaint, 95f), colTglX, ySingle, bodyPaint)
        c.drawText(formatRupiah(remaining), colTagihanRight, centerY(cursorY, rowH, monoBoldPaint), rightAmber)
        c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, borderPaint)
        cursorY += rowH
      }

      ensureSpace(24f)
      val cTotal = canvas!!
      val totalH = 22f
      fillPaint.color = amberSoft
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, fillPaint)
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, borderPaint)
      cTotal.drawText("TOTAL KASBON BELUM LUNAS", colNoX, centerY(cursorY, totalH, bodyBoldPaint), bodyBoldPaint)
      val rightTotalAmber =
        Paint(monoBoldPaint).apply {
          textAlign = Paint.Align.RIGHT
          color = amberAccent
        }
      cTotal.drawText(
        formatRupiah(data.unpaidReceivables),
        colTagihanRight,
        centerY(cursorY, totalH, monoBoldPaint),
        rightTotalAmber,
      )
      cursorY += totalH + 18f
    }

    // ==================== 4. DAFTAR TRANSAKSI LUNAS ====================
    if (data.paidTransactions.isNotEmpty()) {
      val colNoX = MARGIN_H + 8f
      val colNotaX = MARGIN_H + 34f
      val colPelangganX = MARGIN_H + 132f
      val colMetodeX = MARGIN_H + 296f
      val colTotalRight = RIGHT_EDGE - 8f

      val drawPaidHeader: () -> Unit = {
        drawTableHeaderBar { c, top, h ->
          val y = centerY(top, h, tableHeaderPaint)
          val rightHdr = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
          c.drawText("No", colNoX, y, tableHeaderPaint)
          c.drawText("No. Nota", colNotaX, y, tableHeaderPaint)
          c.drawText("Pelanggan", colPelangganX, y, tableHeaderPaint)
          c.drawText("Cara Bayar & Tanggal", colMetodeX, y, tableHeaderPaint)
          c.drawText("Total Bayar", colTotalRight, y, rightHdr)
        }
      }

      ensureSpace(64f)
      drawSectionHeading("4. DAFTAR TRANSAKSI LUNAS", "${data.paidTransactions.size} nota")
      drawPaidHeader()

      val rowH = 20f
      data.paidTransactions.forEachIndexed { idx, tx ->
        ensureSpace(rowH + 2f) {
          drawSectionHeading("4. DAFTAR TRANSAKSI LUNAS (LANJUTAN)")
          drawPaidHeader()
        }
        val c = canvas!!
        if (idx % 2 == 1) {
          fillPaint.color = surfaceSoft
          c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, fillPaint)
        }
        val ySingle = centerY(cursorY, rowH, bodyPaint)
        val rightMono = Paint(monoBoldPaint).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("${idx + 1}", colNoX, ySingle, bodyPaint)
        c.drawText(ellipsize(tx.invoiceNumber, monoPaint, 92f), colNotaX, centerY(cursorY, rowH, monoPaint), monoPaint)
        c.drawText(ellipsize(tx.customerName, bodyPaint, 158f), colPelangganX, ySingle, bodyPaint)
        val effDate = formatDateShort(tx.paidAt ?: tx.timestamp)
        c.drawText(ellipsize("${tx.paymentMethod} • $effDate", bodyPaint, 138f), colMetodeX, ySingle, bodyPaint)
        c.drawText(formatRupiah(tx.totalAmount), colTotalRight, centerY(cursorY, rowH, monoBoldPaint), rightMono)
        c.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + rowH, borderPaint)
        cursorY += rowH
      }

      ensureSpace(24f)
      val cTotal = canvas!!
      val totalH = 22f
      fillPaint.color = skySoft
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, fillPaint)
      cTotal.drawRect(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + totalH, borderPaint)
      cTotal.drawText("TOTAL UANG MASUK LUNAS", colNoX, centerY(cursorY, totalH, bodyBoldPaint), bodyBoldPaint)
      val rightTotalBlue =
        Paint(monoBoldPaint).apply {
          textAlign = Paint.Align.RIGHT
          color = navyDark
        }
      cTotal.drawText(
        formatRupiah(data.paidRevenue),
        colTotalRight,
        centerY(cursorY, totalH, monoBoldPaint),
        rightTotalBlue,
      )
      cursorY += totalH + 18f
    }

    if (
      data.dailySummaries.isEmpty() &&
        data.expenses.isEmpty() &&
        data.unpaidKasbonList.isEmpty() &&
        data.paidTransactions.isEmpty()
    ) {
      ensureSpace(44f)
      val cEmpty = canvas!!
      val emptyRect = RectF(MARGIN_H, cursorY, RIGHT_EDGE, cursorY + 38f)
      fillPaint.color = surfaceSoft
      cEmpty.drawRoundRect(emptyRect, 8f, 8f, fillPaint)
      cEmpty.drawRoundRect(emptyRect, 8f, 8f, borderPaint)
      val centerEmpty = Paint(subTitlePaint).apply { textAlign = Paint.Align.CENTER }
      cEmpty.drawText(
        "Belum ada transaksi maupun catatan pengeluaran pada periode ini.",
        emptyRect.centerX(),
        centerY(emptyRect.top, emptyRect.height(), centerEmpty),
        centerEmpty,
      )
      cursorY += 48f
    }

    // ==================== PENUTUP & TANDA TANGAN ====================
    ensureSpace(74f)
    val cSign = canvas!!
    val signCenterX = RIGHT_EDGE - 85f
    val signCenter = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
    val signBold = Paint(bodyBoldPaint).apply { textAlign = Paint.Align.CENTER }
    val signMuted = Paint(subTitlePaint).apply {
      textAlign = Paint.Align.CENTER
      textSize = 7.5f
    }
    cSign.drawText(signatory.headerText, signCenterX, cursorY + 12f, signCenter)
    cSign.drawText(signatory.roleTitle, signCenterX, cursorY + 24f, signMuted)
    cSign.drawLine(signCenterX - 68f, cursorY + 56f, signCenterX + 68f, cursorY + 56f, borderPaint)
    cSign.drawText(signatory.signerName, signCenterX, cursorY + 67f, signBold)

    finishCurrentPage()
    logoBitmap?.recycle()
    return document
  }
}
