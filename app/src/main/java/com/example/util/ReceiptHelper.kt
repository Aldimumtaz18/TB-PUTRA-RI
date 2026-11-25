package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.KasbonPaymentCodec
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemCodec
import com.example.data.model.CustomerType
import com.example.data.model.ReceiptPaperSize
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class ReceiptPaymentEntry(
  val label: String,
  val detail: String,
  val amount: Long,
)

data class ReceiptPaymentSummary(
  val isCashPaid: Boolean,
  val dpAmount: Long,
  val dpMethodLabel: String?,
  val totalPaid: Long,
  val remainingKasbon: Long,
  val hasPartialOrMultiplePayments: Boolean,
  val paymentEntries: List<ReceiptPaymentEntry> = emptyList(),
)

enum class EscPosAlign {
  LEFT,
  CENTER,
}

data class EscPosLine(
  val text: String,
  val align: EscPosAlign = EscPosAlign.LEFT,
  val bold: Boolean = false,
  val doubleSize: Boolean = false,
)

object ReceiptHelper {

  fun buildReceiptPaymentSummary(
    tx: TransactionEntity,
    predecodedPayments: List<com.example.data.model.KasbonPaymentRecord>? = null,
  ): ReceiptPaymentSummary {
    val decodedPayments = predecodedPayments ?: KasbonPaymentCodec.decode(tx.paymentsJson)
    val isCashPaid = tx.isPaid && tx.paymentMethod.equals("Tunai", ignoreCase = true)
    val isKasbonTx =
      !tx.isPaid ||
        tx.paymentMethod.contains("Kasbon", ignoreCase = true) ||
        decodedPayments.isNotEmpty()

    val effectiveRecords =
      if (decodedPayments.isNotEmpty()) {
        decodedPayments
      } else if (isKasbonTx && tx.amountPaid > 0L) {
        listOf(
          com.example.data.model.KasbonPaymentRecord(
            amount = tx.amountPaid.coerceAtMost(tx.totalAmount),
            paymentMethod = PaymentMethodLabelFallback(tx.paymentMethod),
            timestamp = tx.paidAt ?: tx.timestamp,
            recordedBy = tx.cashierName,
          )
        )
      } else {
        emptyList()
      }

    var cicilanNumber = 0
    var runningTotal = 0L
    val entries =
      if (!isKasbonTx) {
        emptyList()
      } else {
        effectiveRecords.mapIndexed { index, rec ->
          runningTotal += rec.amount
          val isAtCheckout = kotlin.math.abs(rec.timestamp - tx.timestamp) <= 5_000L
          val isInitialDp = index == 0 && isAtCheckout && rec.amount < tx.totalAmount
          val isFinalSettlement =
            tx.isPaid &&
              index == effectiveRecords.lastIndex &&
              effectiveRecords.size > 1 &&
              runningTotal >= tx.totalAmount
          val label =
            when {
              isInitialDp -> "DP (${rec.paymentMethod})"
              isFinalSettlement -> "Pelunasan (${rec.paymentMethod})"
              else -> {
                cicilanNumber++
                "Cicilan $cicilanNumber (${rec.paymentMethod})"
              }
            }
          val detail =
            buildString {
              append(formatDateShort(rec.timestamp))
              if (rec.recordedBy.isNotBlank()) append(" • ${rec.recordedBy}")
            }
          ReceiptPaymentEntry(label = label, detail = detail, amount = rec.amount)
        }
      }

    val firstRecord = effectiveRecords.firstOrNull()
    val dpAmount =
      when {
        !isKasbonTx -> 0L
        firstRecord != null &&
          kotlin.math.abs(firstRecord.timestamp - tx.timestamp) <= 5_000L &&
          firstRecord.amount in 1L until tx.totalAmount -> firstRecord.amount
        else -> 0L
      }
    val dpMethodLabel =
      if (dpAmount > 0L) firstRecord?.paymentMethod?.takeIf { it.isNotBlank() } else null
    val totalPaid =
      if (isKasbonTx) tx.amountPaid.coerceIn(0L, tx.totalAmount) else tx.amountPaid
    val remainingKasbon =
      if (!tx.isPaid) (tx.totalAmount - totalPaid).coerceAtLeast(0L) else 0L

    return ReceiptPaymentSummary(
      isCashPaid = isCashPaid,
      dpAmount = dpAmount,
      dpMethodLabel = dpMethodLabel,
      totalPaid = totalPaid,
      remainingKasbon = remainingKasbon,
      hasPartialOrMultiplePayments =
        isKasbonTx && (effectiveRecords.size > 1 || totalPaid > dpAmount),
      paymentEntries = entries,
    )
  }

  private fun PaymentMethodLabelFallback(rawMethod: String): String {
    return when {
      rawMethod.contains("Transfer", ignoreCase = true) || rawMethod.contains("QRIS", ignoreCase = true) ->
        "Transfer/QRIS"
      else -> "Tunai"
    }
  }

  fun renderReceiptBitmap(
    tx: TransactionEntity,
    settings: StoreSettings,
    isSuratJalan: Boolean,
    context: Context? = null,
  ): Bitmap {
    val logoBitmap =
      context?.let { ctx ->
        runCatching { BitmapFactory.decodeResource(ctx.resources, R.drawable.tb_putra_ri_logo) }
          .getOrNull()
      }
    val logoDrawSize = if (logoBitmap != null) 148f else 0f
    val logoBlockHeight = if (logoBitmap != null) logoDrawSize + 18f else 0f
    val items = TransactionItemCodec.decode(tx.itemsJson)
    val width = 760
    val outerMargin = 24f
    val paperLeft = outerMargin
    val paperRight = width - outerMargin
    val innerLeft = paperLeft + 32f
    val innerRight = paperRight - 32f
    val contentWidth = innerRight - innerLeft

    val inkDark = Color.parseColor("#0F172A")
    val inkMuted = Color.parseColor("#475569")
    val paperBg = Color.parseColor("#FFFDF9")
    val backdropBg = Color.parseColor("#E2E8F0")
    val navyColor = Color.parseColor("#0F294A")
    val emeraldColor = Color.parseColor("#16A34A")
    val amberColor = Color.parseColor("#D97706")
    val dashColor = Color.parseColor("#CBD5E1")

    val headerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 34f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val subHeaderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkMuted
        textSize = 20f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
      }
    val badgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 23f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val labelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkMuted
        textSize = 21f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
      }
    val subDetailPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkMuted
        textSize = 17f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
      }
    val blackSubDetailPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 17f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
      }
    val valuePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 21f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }
    val itemTitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 22f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
      }
    val totalLabelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inkDark
        textSize = 27f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
      }
    val totalValuePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }
    val dashPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dashColor
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
      }

    // Wrap long text lines safely
    fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
      val words = text.split(" ")
      val lines = mutableListOf<String>()
      var current = ""
      for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
          current = candidate
        } else {
          if (current.isNotEmpty()) lines.add(current)
          current = word
        }
      }
      if (current.isNotEmpty()) lines.add(current)
      return if (lines.isEmpty()) listOf("") else lines
    }

    val addressLines = wrapText(settings.storeAddress, subHeaderPaint, contentWidth)
    val itemBlocks =
      items.map { item ->
        val maxNameWidth = if (isSuratJalan) contentWidth * 0.68f else contentWidth
        wrapText(item.productName, itemTitlePaint, maxNameWidth) to item
      }

    // Calculate dynamic receipt height with generous top/bottom breathing room
    var estimatedHeight = 96f + logoBlockHeight // top outer + tooth + logo + initial baseline
    estimatedHeight += 32f // store title
    estimatedHeight += addressLines.size * 26f
    estimatedHeight += 28f // phone
    estimatedHeight += 36f // dash
    estimatedHeight += 36f // doc badge
    estimatedHeight += 5 * 30f // meta rows
    if (tx.customerPhone.isNotBlank()) estimatedHeight += 30f
    estimatedHeight += 36f // dash
    itemBlocks.forEach { (lines, item) ->
      estimatedHeight += lines.size * 28f
      if (!isSuratJalan) {
        estimatedHeight += 32f
        if (item.unitBargainDiscount > 0L) estimatedHeight += 24f
      } else {
        estimatedHeight += 8f
      }
    }
    estimatedHeight += 34f // dash
    if (isSuratJalan) {
      estimatedHeight += 150f // receiver note + signature boxes
    } else {
      estimatedHeight += 34f // subtotal
      if (tx.discountAmount > 0L) estimatedHeight += 34f
      estimatedHeight += 48f // TOTAL
      estimatedHeight += 34f // method
      val paymentSummary = buildReceiptPaymentSummary(tx)
      if (paymentSummary.isCashPaid) estimatedHeight += 64f
      if (paymentSummary.paymentEntries.isNotEmpty()) {
        estimatedHeight += 64f // divider + section title + spacing
        estimatedHeight += paymentSummary.paymentEntries.size * 52f
        if (paymentSummary.paymentEntries.size > 1) estimatedHeight += 32f
        if (!tx.isPaid) estimatedHeight += 34f
      }
      estimatedHeight += 86f // status stamp box + padding
    }
    estimatedHeight += 128f // footer + bottom tooth + outer margin

    val height = estimatedHeight.toInt().coerceAtLeast(640)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backdropBg)

    // Draw thermal paper with sawtooth top & bottom edges
    val toothWidth = 18f
    val toothHeight = 12f
    val paperTop = outerMargin
    val paperBottom = height - outerMargin

    val paperPath = Path()
    paperPath.moveTo(paperLeft, paperTop + toothHeight)
    var x = paperLeft
    while (x < paperRight - 1f) {
      val nextX = (x + toothWidth).coerceAtMost(paperRight)
      val midX = (x + nextX) / 2f
      paperPath.lineTo(midX, paperTop)
      paperPath.lineTo(nextX, paperTop + toothHeight)
      x = nextX
    }
    paperPath.lineTo(paperRight, paperBottom - toothHeight)
    x = paperRight
    while (x > paperLeft + 1f) {
      val prevX = (x - toothWidth).coerceAtLeast(paperLeft)
      val midX = (x + prevX) / 2f
      paperPath.lineTo(midX, paperBottom)
      paperPath.lineTo(prevX, paperBottom - toothHeight)
      x = prevX
    }
    paperPath.close()

    val shadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.FILL
      }
    canvas.save()
    canvas.translate(0f, 5f)
    canvas.drawPath(paperPath, shadowPaint)
    canvas.restore()

    val paperPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = paperBg
        style = Paint.Style.FILL
      }
    canvas.drawPath(paperPath, paperPaint)

    var y = paperTop + toothHeight + 28f
    val centerX = width / 2f

    // Store Logo
    if (logoBitmap != null) {
      val halfSize = logoDrawSize / 2f
      val logoRect = RectF(centerX - halfSize, y, centerX + halfSize, y + logoDrawSize)
      val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      canvas.drawBitmap(logoBitmap, null, logoRect, imgPaint)
      logoBitmap.recycle()
      y += logoDrawSize + 44f
    } else {
      y += 18f
    }

    // Store Header
    val displayStoreName = settings.storeName.ifBlank { "TB PUTRA RI" }
    canvas.drawText(displayStoreName, centerX, y, headerPaint)
    y += 30f
    addressLines.forEach { line ->
      canvas.drawText(line, centerX, y, subHeaderPaint)
      y += 26f
    }
    canvas.drawText("Telp/WA: ${settings.storePhone}", centerX, y, subHeaderPaint)
    y += 24f

    canvas.drawLine(innerLeft, y, innerRight, y, dashPaint)
    y += 34f

    // Document Title
    canvas.drawText(
      if (isSuratJalan) "== SURAT JALAN PENGIRIMAN ==" else "== STRUK PEMBELIAN ==",
      centerX,
      y,
      badgePaint,
    )
    y += 34f

    fun drawMetaRow(label: String, value: String) {
      canvas.drawText(label, innerLeft, y, labelPaint)
      val maxValWidth = contentWidth - labelPaint.measureText(label) - 18f
      var displayVal = value
      if (valuePaint.measureText(displayVal) > maxValWidth) {
        while (displayVal.length > 3 && valuePaint.measureText("$displayVal...") > maxValWidth) {
          displayVal = displayVal.dropLast(1)
        }
        displayVal = "$displayVal..."
      }
      canvas.drawText(displayVal, innerRight, y, valuePaint)
      y += 30f
    }

    drawMetaRow("No. Nota", tx.invoiceNumber)
    drawMetaRow("Tanggal", formatDateShort(tx.timestamp))
    drawMetaRow("Kasir", tx.cashierName)
    val resolvedCustomerType = CustomerType.fromStoredLabel(tx.customerType)
    drawMetaRow("Pelanggan", "${tx.customerName} (${resolvedCustomerType.label})")
    if (tx.customerPhone.isNotBlank()) {
      drawMetaRow("No. HP/WA", tx.customerPhone)
    }

    y += 4f
    canvas.drawLine(innerLeft, y, innerRight, y, dashPaint)
    y += 30f

    // Items
    itemBlocks.forEach { (lines, item) ->
      if (isSuratJalan) {
        lines.forEachIndexed { idx, line ->
          canvas.drawText(line, innerLeft, y, itemTitlePaint)
          if (idx == 0) {
            canvas.drawText("${formatQty(item.quantity)} ${item.unitName}", innerRight, y, valuePaint)
          }
          y += 28f
        }
        y += 8f
      } else {
        lines.forEach { line ->
          canvas.drawText(line, innerLeft, y, itemTitlePaint)
          y += 27f
        }
        val qtyDetail = "  ${formatQty(item.quantity)} ${item.unitName} x ${formatRupiah(item.unitPrice)}"
        canvas.drawText(qtyDetail, innerLeft, y, labelPaint)
        canvas.drawText(formatRupiah(item.subtotal), innerRight, y, valuePaint)
        y += if (item.unitBargainDiscount > 0L) 24f else 32f
        if (item.unitBargainDiscount > 0L) {
          canvas.drawText(
            "  Tawar -${formatRupiah(item.lineBargainDiscount)}",
            innerLeft,
            y,
            blackSubDetailPaint,
          )
          y += 24f
        }
      }
    }

    canvas.drawLine(innerLeft, y, innerRight, y, dashPaint)
    y += 32f

    if (!isSuratJalan) {
      drawMetaRow("Subtotal", formatRupiah(tx.subtotal))
      if (tx.discountAmount > 0L) {
        val discPaint =
          Paint(valuePaint).apply {
            color = emeraldColor
          }
        val discountLabel =
          if (resolvedCustomerType.discountRate > 0.0) {
            val discPct = (resolvedCustomerType.discountRate * 100).toInt()
            "Diskon ($discPct%)"
          } else {
            "Potongan Harga"
          }
        canvas.drawText(discountLabel, innerLeft, y, labelPaint)
        canvas.drawText("-${formatRupiah(tx.discountAmount)}", innerRight, y, discPaint)
        y += 32f
      }

      y += 6f
      canvas.drawText("TOTAL", innerLeft, y, totalLabelPaint)
      canvas.drawText(formatRupiah(tx.totalAmount), innerRight, y, totalValuePaint)
      y += 38f

      drawMetaRow("Metode Bayar", tx.paymentMethod)
      val paymentSummary = buildReceiptPaymentSummary(tx)
      if (paymentSummary.isCashPaid) {
        drawMetaRow("Tunai", formatRupiah(tx.amountPaid))
        drawMetaRow("Kembali", formatRupiah(tx.changeAmount))
      }
      if (paymentSummary.paymentEntries.isNotEmpty()) {
        y += 6f
        canvas.drawLine(innerLeft, y, innerRight, y, dashPaint)
        y += 28f
        canvas.drawText("Rincian Pembayaran Kasbon", innerLeft, y, itemTitlePaint)
        y += 28f
        paymentSummary.paymentEntries.forEach { entry ->
          canvas.drawText(entry.label, innerLeft, y, labelPaint)
          canvas.drawText(formatRupiah(entry.amount), innerRight, y, valuePaint)
          y += 23f
          canvas.drawText("  ${entry.detail}", innerLeft, y, subDetailPaint)
          y += 27f
        }
        if (paymentSummary.paymentEntries.size > 1) {
          drawMetaRow("Total Dibayar", formatRupiah(paymentSummary.totalPaid))
        }
        if (!tx.isPaid) {
          val remainPaint = Paint(valuePaint).apply { color = amberColor }
          canvas.drawText("Sisa Kasbon", innerLeft, y, itemTitlePaint)
          canvas.drawText(formatRupiah(paymentSummary.remainingKasbon), innerRight, y, remainPaint)
          y += 32f
        }
      }

      y += 10f
      val statusColor = if (tx.isPaid) emeraldColor else amberColor
      val stampBgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = if (tx.isPaid) Color.parseColor("#DCFCE7") else Color.parseColor("#FEF3C7")
          style = Paint.Style.FILL
        }
      val stampBorderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = statusColor
          style = Paint.Style.STROKE
          strokeWidth = 3f
        }
      val stampTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = statusColor
          textSize = 23f
          typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
          textAlign = Paint.Align.CENTER
        }
      val stampRect = RectF(innerLeft + 40f, y, innerRight - 40f, y + 48f)
      canvas.drawRoundRect(stampRect, 12f, 12f, stampBgPaint)
      canvas.drawRoundRect(stampRect, 12f, 12f, stampBorderPaint)
      canvas.drawText(
        if (tx.isPaid) "STATUS: LUNAS" else "STATUS: TEMPO / KASBON",
        centerX,
        y + 32f,
        stampTextPaint,
      )
      y += 66f
    } else {
      canvas.drawText(
        "Mohon periksa jumlah & kondisi barang saat diterima.",
        centerX,
        y,
        subHeaderPaint,
      )
      y += 42f
      val sigPaint =
        Paint(subHeaderPaint).apply {
          textAlign = Paint.Align.CENTER
        }
      val leftSigX = innerLeft + contentWidth * 0.25f
      val rightSigX = innerLeft + contentWidth * 0.75f
      canvas.drawText("Pengirim / Sopir", leftSigX, y, sigPaint)
      canvas.drawText("Penerima Barang", rightSigX, y, sigPaint)
      y += 58f
      canvas.drawText("( _______________ )", leftSigX, y, sigPaint)
      canvas.drawText("( _______________ )", rightSigX, y, sigPaint)
      y += 30f
    }

    canvas.drawLine(innerLeft, y, innerRight, y, dashPaint)
    y += 32f
    canvas.drawText("Terima kasih telah berbelanja!", centerX, y, subHeaderPaint)
    y += 26f
    canvas.drawText("${settings.storeName.ifBlank { "TB PUTRA RI" }} - Bahan Bangunan", centerX, y, subHeaderPaint)

    return bitmap
  }

  fun shareViaWhatsApp(
    context: Context,
    tx: TransactionEntity,
    settings: StoreSettings,
    isSuratJalan: Boolean,
  ) {
    try {
      val bitmap = renderReceiptBitmap(tx, settings, isSuratJalan, context)
      val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
      val fileName = "${if (isSuratJalan) "surat_jalan" else "struk"}_${tx.invoiceNumber}.png"
      val outFile = File(receiptsDir, fileName)
      FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
      }
      bitmap.recycle()

      val imageUri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)

      val rawPhone = tx.customerPhone.filter { it.isDigit() }
      val waPhone =
        when {
          rawPhone.startsWith("0") -> "62" + rawPhone.drop(1)
          rawPhone.startsWith("62") -> rawPhone
          rawPhone.isNotEmpty() -> "62$rawPhone"
          else -> ""
        }

      val baseIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "image/png"
          putExtra(Intent.EXTRA_STREAM, imageUri)
          clipData = ClipData.newRawUri("Struk ${settings.storeName.ifBlank { "TB PUTRA RI" }}", imageUri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          if (waPhone.isNotEmpty()) {
            putExtra("jid", "$waPhone@s.whatsapp.net")
          }
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
        Intent.createChooser(
          baseIntent,
          if (isSuratJalan) "Kirim Gambar Surat Jalan" else "Kirim Gambar Struk ke WhatsApp",
        )
      )
    } catch (_: Exception) {
      Toast.makeText(context, "Gagal membuat gambar struk untuk dibagikan", Toast.LENGTH_SHORT)
        .show()
    }
  }

  fun buildEscPosLines(
    tx: TransactionEntity,
    settings: StoreSettings,
    isSuratJalan: Boolean,
    paperSize: ReceiptPaperSize,
  ): List<EscPosLine> {
    val cols = paperSize.charColumns
    val dashLine = "-".repeat(cols)
    val doubleDashLine = "=".repeat(cols)
    val items = TransactionItemCodec.decode(tx.itemsJson)
    val resolvedCustomerType = CustomerType.fromStoredLabel(tx.customerType)
    val lines = mutableListOf<EscPosLine>()

    fun sanitizeAscii(input: String): String =
      input
        .replace('\u2022', '-') // bullet
        .replace('\u2013', '-')
        .replace('\u2014', '-')
        .map { ch -> if (ch.code in 32..126) ch else ' ' }
        .joinToString("")
        .trim()

    fun wrapEscPos(text: String, maxCols: Int = cols): List<String> {
      val clean = sanitizeAscii(text)
      if (clean.isEmpty()) return listOf("")
      val words = clean.split(Regex("\\s+"))
      val out = mutableListOf<String>()
      var current = ""
      for (word in words) {
        if (word.length > maxCols) {
          if (current.isNotEmpty()) {
            out.add(current)
            current = ""
          }
          var remaining = word
          while (remaining.length > maxCols) {
            out.add(remaining.take(maxCols))
            remaining = remaining.drop(maxCols)
          }
          if (remaining.isNotEmpty()) current = remaining
        } else {
          val candidate = if (current.isEmpty()) word else "$current $word"
          if (candidate.length <= maxCols) {
            current = candidate
          } else {
            if (current.isNotEmpty()) out.add(current)
            current = word
          }
        }
      }
      if (current.isNotEmpty()) out.add(current)
      return out
    }

    fun formatPair(leftRaw: String, rightRaw: String, bold: Boolean = false) {
      val left = sanitizeAscii(leftRaw)
      val right = sanitizeAscii(rightRaw)
      if (left.length + right.length + 1 <= cols) {
        val spaces = cols - left.length - right.length
        lines.add(EscPosLine(left + " ".repeat(spaces) + right, bold = bold))
      } else {
        wrapEscPos(left).forEach { lines.add(EscPosLine(it, bold = bold)) }
        val pad = (cols - right.length).coerceAtLeast(0)
        lines.add(EscPosLine(" ".repeat(pad) + right.take(cols), bold = bold))
      }
    }

    val storeName = settings.storeName.ifBlank { "TB PUTRA RI" }
    lines.add(EscPosLine(sanitizeAscii(storeName), align = EscPosAlign.CENTER, bold = true, doubleSize = true))
    wrapEscPos(settings.storeAddress).forEach {
      lines.add(EscPosLine(it, align = EscPosAlign.CENTER))
    }
    if (settings.storePhone.isNotBlank()) {
      lines.add(EscPosLine(sanitizeAscii("Telp/WA: ${settings.storePhone}"), align = EscPosAlign.CENTER))
    }
    lines.add(EscPosLine(dashLine))

    val docTitle =
      if (isSuratJalan) "SURAT JALAN PENGIRIMAN" else "STRUK PEMBELIAN"
    lines.add(EscPosLine(docTitle, align = EscPosAlign.CENTER, bold = true))
    lines.add(EscPosLine(dashLine))

    formatPair("No. Nota", tx.invoiceNumber)
    formatPair("Tanggal", formatDateShort(tx.timestamp))
    formatPair("Kasir", tx.cashierName.ifBlank { "Kasir" })
    formatPair("Pembeli", "${tx.customerName} (${resolvedCustomerType.label})")
    if (tx.customerPhone.isNotBlank()) {
      formatPair("No. WA", tx.customerPhone)
    }
    lines.add(EscPosLine(dashLine))

    items.forEach { item ->
      if (isSuratJalan) {
        val qtyStr = "${formatQty(item.quantity)} ${item.unitName}"
        formatPair(item.productName, qtyStr, bold = true)
      } else {
        wrapEscPos(item.productName).forEach {
          lines.add(EscPosLine(it, bold = true))
        }
        val qtyExpr = " ${formatQty(item.quantity)} ${item.unitName} x ${formatRupiah(item.unitPrice)}"
        formatPair(qtyExpr, formatRupiah(item.subtotal))
        if (item.unitBargainDiscount > 0L) {
          wrapEscPos(" Tawar -${formatRupiah(item.lineBargainDiscount)}").forEach {
            lines.add(EscPosLine(it))
          }
        }
      }
    }
    lines.add(EscPosLine(dashLine))

    if (!isSuratJalan) {
      formatPair("Subtotal", formatRupiah(tx.subtotal))
      if (tx.discountAmount > 0L) {
        val discLabel =
          if (resolvedCustomerType.discountRate > 0.0) {
            "Diskon (${(resolvedCustomerType.discountRate * 100).toInt()}%)"
          } else {
            "Potongan Harga"
          }
        formatPair(discLabel, "-${formatRupiah(tx.discountAmount)}")
      }
      lines.add(EscPosLine(doubleDashLine))
      formatPair("TOTAL", formatRupiah(tx.totalAmount), bold = true)
      lines.add(EscPosLine(dashLine))
      formatPair("Metode", tx.paymentMethod)

      val paymentSummary = buildReceiptPaymentSummary(tx)
      if (paymentSummary.isCashPaid) {
        formatPair("Tunai", formatRupiah(tx.amountPaid))
        formatPair("Kembali", formatRupiah(tx.changeAmount))
      }
      if (paymentSummary.paymentEntries.isNotEmpty()) {
        lines.add(EscPosLine(dashLine))
        lines.add(EscPosLine("Rincian Pembayaran Kasbon", bold = true))
        paymentSummary.paymentEntries.forEach { entry ->
          formatPair(entry.label, formatRupiah(entry.amount))
          wrapEscPos(" ${entry.detail}").forEach { lines.add(EscPosLine(it)) }
        }
        if (paymentSummary.paymentEntries.size > 1) {
          formatPair("Total Dibayar", formatRupiah(paymentSummary.totalPaid), bold = true)
        }
        if (!tx.isPaid) {
          formatPair("Sisa Kasbon", formatRupiah(paymentSummary.remainingKasbon), bold = true)
        }
      }

      lines.add(EscPosLine(dashLine))
      val statusText = if (tx.isPaid) "*** STATUS: LUNAS ***" else "** STATUS: TEMPO / KASBON **"
      lines.add(EscPosLine(statusText, align = EscPosAlign.CENTER, bold = true))
    } else {
      wrapEscPos("Barang telah diperiksa & diterima dalam kondisi baik.").forEach {
        lines.add(EscPosLine(it, align = EscPosAlign.CENTER))
      }
      lines.add(EscPosLine(""))
      formatPair("Sopir / Pengirim", "Penerima")
      lines.add(EscPosLine(""))
      lines.add(EscPosLine(""))
      formatPair("( __________ )", "( __________ )")
    }

    lines.add(EscPosLine(dashLine))
    lines.add(EscPosLine("Terima kasih telah berbelanja!", align = EscPosAlign.CENTER))
    lines.add(EscPosLine(sanitizeAscii("$storeName - Bahan Bangunan"), align = EscPosAlign.CENTER))
    return lines
  }

  fun buildEscPosBytes(
    tx: TransactionEntity,
    settings: StoreSettings,
    isSuratJalan: Boolean,
    paperSize: ReceiptPaperSize,
  ): ByteArray {
    val baos = ByteArrayOutputStream()
    // ESC @ : Initialize printer
    baos.write(byteArrayOf(0x1B, 0x40))

    val lines = buildEscPosLines(tx, settings, isSuratJalan, paperSize)
    lines.forEach { line ->
      val alignByte: Byte =
        when (line.align) {
          EscPosAlign.LEFT -> 0x00
          EscPosAlign.CENTER -> 0x01
        }
      // ESC a n : Justification
      baos.write(byteArrayOf(0x1B, 0x61, alignByte))
      // ESC E n : Bold
      baos.write(byteArrayOf(0x1B, 0x45, if (line.bold) 0x01 else 0x00))
      // GS ! n : Character size (normal vs 2x height when doubleSize)
      baos.write(byteArrayOf(0x1D, 0x21, if (line.doubleSize) 0x01 else 0x00))

      baos.write(line.text.toByteArray(Charsets.US_ASCII))
      baos.write(0x0A)
    }

    // Reset formatting + feed 4 lines + partial cut (GS V 66 3)
    baos.write(byteArrayOf(0x1B, 0x61, 0x00, 0x1B, 0x45, 0x00, 0x1D, 0x21, 0x00))
    baos.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
    baos.write(byteArrayOf(0x1D, 0x56, 0x42, 0x03))
    return baos.toByteArray()
  }

  fun printReceipt(
    context: Context,
    tx: TransactionEntity,
    settings: StoreSettings,
    isSuratJalan: Boolean,
    paperSize: ReceiptPaperSize = settings.thermalPaperSize,
  ) {
    val items = TransactionItemCodec.decode(tx.itemsJson)
    val resolvedCustomerType = CustomerType.fromStoredLabel(tx.customerType)
    val bodyFontPx = if (paperSize == ReceiptPaperSize.THERMAL_80MM) 13 else 11
    val detailFontPx = if (paperSize == ReceiptPaperSize.THERMAL_80MM) 11 else 10
    val logoMaxWidthMm = if (paperSize == ReceiptPaperSize.THERMAL_80MM) 32 else 24

    val rowsHtml =
      items.joinToString("") { item ->
        if (isSuratJalan) {
          "<tr><td>${item.productName}</td><td style='text-align:right'><b>${formatQty(item.quantity)} ${item.unitName}</b></td></tr>"
        } else {
          val bargainHtml =
            if (item.unitBargainDiscount > 0L) {
              "<tr><td colspan='2' style='font-size:${detailFontPx}px;color:#000000'>Tawar -${formatRupiah(item.lineBargainDiscount)}</td></tr>"
            } else {
              ""
            }
          "<tr><td colspan='2'><b>${item.productName}</b></td></tr>" +
            "<tr><td>${formatQty(item.quantity)} ${item.unitName} x ${formatRupiah(item.unitPrice)}</td>" +
            "<td style='text-align:right'>${formatRupiah(item.subtotal)}</td></tr>" +
            bargainHtml
        }
      }

    val totalsHtml =
      if (isSuratJalan) {
        "<hr/><p>Penerima: _______________</p><p>Sopir: _______________</p>"
      } else {
        val discountRowHtml =
          if (tx.discountAmount > 0L) {
            val discLabel =
              if (resolvedCustomerType.discountRate > 0.0) {
                "Diskon (${(resolvedCustomerType.discountRate * 100).toInt()}%)"
              } else {
                "Potongan Harga"
              }
            "<tr><td>$discLabel</td><td style='text-align:right'>-${formatRupiah(tx.discountAmount)}</td></tr>"
          } else {
            ""
          }
        val paymentSummary = buildReceiptPaymentSummary(tx)
        val cashRowsHtml =
          if (paymentSummary.isCashPaid) {
            "<tr><td>Tunai</td><td style='text-align:right'>${formatRupiah(tx.amountPaid)}</td></tr>" +
              "<tr><td>Kembali</td><td style='text-align:right'>${formatRupiah(tx.changeAmount)}</td></tr>"
          } else {
            ""
          }
        val dpRowsHtml =
          if (paymentSummary.paymentEntries.isNotEmpty()) {
            buildString {
              append("<tr><td colspan='2' style='padding-top:6px;border-top:1px dashed #94A3B8'><b>Rincian Pembayaran Kasbon</b></td></tr>")
              paymentSummary.paymentEntries.forEach { entry ->
                append(
                  "<tr><td>${entry.label}<br/><span style='font-size:${detailFontPx}px;color:#64748B'>${entry.detail}</span></td>" +
                    "<td style='text-align:right;vertical-align:top'>${formatRupiah(entry.amount)}</td></tr>"
                )
              }
              if (paymentSummary.paymentEntries.size > 1) {
                append("<tr><td>Total Dibayar</td><td style='text-align:right'>${formatRupiah(paymentSummary.totalPaid)}</td></tr>")
              }
              if (!tx.isPaid) {
                append("<tr><td><b>Sisa Kasbon</b></td><td style='text-align:right'><b>${formatRupiah(paymentSummary.remainingKasbon)}</b></td></tr>")
              }
            }
          } else {
            ""
          }
        """
        <hr/>
        <table style='width:100%;font-size:${bodyFontPx}px;'>
          <tr><td>Subtotal</td><td style='text-align:right'>${formatRupiah(tx.subtotal)}</td></tr>
          $discountRowHtml
          <tr><td><b>TOTAL</b></td><td style='text-align:right'><b>${formatRupiah(tx.totalAmount)}</b></td></tr>
          <tr><td>Metode</td><td style='text-align:right'>${tx.paymentMethod}</td></tr>
          <tr><td>Status</td><td style='text-align:right'><b>${if (tx.isPaid) "LUNAS" else "KASBON"}</b></td></tr>
          $cashRowsHtml
          $dpRowsHtml
        </table>
        """
      }

    val logoHtml =
      runCatching {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.tb_putra_ri_logo)
        val scaled = Bitmap.createScaledBitmap(raw, 180, 180, true)
        if (scaled != raw) raw.recycle()
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        scaled.recycle()
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        "<img src='data:image/png;base64,$b64' style='width:${logoMaxWidthMm}mm;max-width:120px;height:auto;margin-bottom:4px;'/><br/>"
      }.getOrDefault("")

    val displayStoreName = settings.storeName.ifBlank { "TB PUTRA RI" }
    val html =
      """
      <html>
      <head>
        <style>
          @page { size: ${paperSize.cssWidth} auto; margin: 2mm; }
          body { font-family: monospace; font-size: ${bodyFontPx}px; width: ${paperSize.cssWidth}; margin: 0 auto; padding: 6px; box-sizing: border-box; }
          hr { border: none; border-top: 1px dashed #64748B; }
        </style>
      </head>
      <body>
        <div style='text-align:center;'>
          $logoHtml
          <h3 style='margin:0;'>$displayStoreName</h3>
          <div>${settings.storeAddress}</div>
          <div>Telp: ${settings.storePhone}</div>
        </div>
        <hr/>
        <div><b>${if (isSuratJalan) "SURAT JALAN" else "STRUK PEMBELIAN"}</b></div>
        <div>No: ${tx.invoiceNumber}</div>
        <div>Tgl: ${formatDateShort(tx.timestamp)}</div>
        <div>Kasir: ${tx.cashierName}</div>
        <div>Pembeli: ${tx.customerName}</div>
        <hr/>
        <table style='width:100%;font-size:${bodyFontPx}px;'>$rowsHtml</table>
        $totalsHtml
        <hr/>
        <div style='text-align:center;'>Terima Kasih!<br/>$displayStoreName - Solusi Bahan Bangunan</div>
      </body></html>
      """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient =
      object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
          if (printManager != null && view != null) {
            val jobName = "Struk_${tx.invoiceNumber}_${paperSize.widthMm}mm"
            val adapter = view.createPrintDocumentAdapter(jobName)
            val mediaSize =
              if (paperSize == ReceiptPaperSize.THERMAL_80MM) {
                PrintAttributes.MediaSize("THERMAL_80MM", "Thermal 80mm", 3150, 11811)
              } else {
                PrintAttributes.MediaSize("THERMAL_58MM", "Thermal 58mm", 2283, 11811)
              }
            val attrs =
              PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, adapter, attrs)
          }
        }
      }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }
}
