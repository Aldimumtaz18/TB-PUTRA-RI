package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemCodec
import com.example.data.model.CustomerType
import com.example.data.model.ReceiptPaperSize
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy900
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate900
import com.example.util.ReceiptHelper
import com.example.util.formatDateTime
import com.example.util.formatQty
import com.example.util.formatRupiah

@Composable
fun ReceiptModal(
  transaction: TransactionEntity,
  settings: StoreSettings,
  onSavePrintPreferences: (ReceiptPaperSize, String?, String?) -> Unit = { _, _, _ -> },
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val items = remember(transaction.itemsJson) { TransactionItemCodec.decode(transaction.itemsJson) }
  var isSuratJalan by remember { mutableStateOf(false) }
  var showPrintDialog by remember { mutableStateOf(false) }

  val orangeBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val orangeFg = if (isDark) Orange400 else Orange600

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        Modifier.widthIn(max = 520.dp)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 20.dp),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Polished Header with Icon Badge
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = orangeBg,
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector =
                    if (isSuratJalan) Icons.Default.LocalShipping else Icons.Default.ReceiptLong,
                  contentDescription = null,
                  tint = orangeFg,
                  modifier = Modifier.size(21.dp),
                )
              }
            }
            Column {
              Text(
                text = if (isSuratJalan) "Surat Jalan" else "Struk Thermal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = transaction.invoiceNumber,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_receipt_button"),
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup struk")
          }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = !isSuratJalan,
            onClick = { isSuratJalan = false },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("tab_struk_thermal"),
          ) {
            Text(
              text = "Struk Thermal",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          SegmentedButton(
            selected = isSuratJalan,
            onClick = { isSuratJalan = true },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("tab_surat_jalan"),
          ) {
            Text(
              text = "Surat Jalan",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Outer tray framing the thermal receipt paper cleanly in both light & dark modes
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.55f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .padding(10.dp)
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFFFFDF9), RoundedCornerShape(12.dp))
                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
          ) {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                painter = painterResource(id = R.drawable.tb_putra_ri_logo),
                contentDescription = "Logo TB PUTRA RI",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(68.dp),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = settings.storeName.ifBlank { "TB PUTRA RI" },
              fontFamily = JetBrainsMono,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Slate900,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
            )
            Text(
              text = "${settings.storeAddress}\nTelp: ${settings.storePhone}",
              fontFamily = JetBrainsMono,
              fontSize = 11.sp,
              lineHeight = 15.sp,
              color = Slate500,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            DashedReceiptDivider()
            Spacer(modifier = Modifier.height(10.dp))

            ReceiptMetaRow("Jenis", if (isSuratJalan) "SURAT JALAN" else "NOTA PENJUALAN")
            ReceiptMetaRow("No. Nota", transaction.invoiceNumber)
            ReceiptMetaRow("Tanggal", formatDateTime(transaction.timestamp))
            ReceiptMetaRow("Kasir", transaction.cashierName)
            val resolvedCustomerType = CustomerType.fromStoredLabel(transaction.customerType)
            ReceiptMetaRow("Pembeli", "${transaction.customerName} (${resolvedCustomerType.label})")
            if (transaction.customerPhone.isNotBlank()) {
              ReceiptMetaRow("No. WA", transaction.customerPhone)
            }

            Spacer(modifier = Modifier.height(10.dp))
            DashedReceiptDivider()
            Spacer(modifier = Modifier.height(10.dp))

            items.forEach { item ->
              if (isSuratJalan) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top,
                ) {
                  Text(
                    text = item.productName,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "${formatQty(item.quantity)} ${item.unitName}",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Slate900,
                  )
                }
              } else {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                  Text(
                    text = item.productName,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Slate900,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text =
                        "${formatQty(item.quantity)} ${item.unitName} x ${formatRupiah(item.unitPrice)}",
                      fontFamily = JetBrainsMono,
                      fontSize = 11.sp,
                      color = Slate500,
                      modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = formatRupiah(item.subtotal),
                      fontFamily = JetBrainsMono,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 12.sp,
                      color = Slate900,
                      textAlign = TextAlign.End,
                    )
                  }
                  if (item.unitBargainDiscount > 0L) {
                    Text(
                      text = "Tawar -${formatRupiah(item.lineBargainDiscount)}",
                      fontFamily = JetBrainsMono,
                      fontSize = 10.sp,
                      color = Color.Black,
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            DashedReceiptDivider()
            Spacer(modifier = Modifier.height(10.dp))

            if (!isSuratJalan) {
              ReceiptMetaRow("Subtotal", formatRupiah(transaction.subtotal))
              if (transaction.discountAmount > 0L) {
                val discountLabel =
                  if (resolvedCustomerType.discountRate > 0.0) {
                    val discPct = (resolvedCustomerType.discountRate * 100).toInt()
                    "Diskon $discPct%"
                  } else {
                    "Potongan Harga"
                  }
                ReceiptMetaRow(discountLabel, "-${formatRupiah(transaction.discountAmount)}")
              }
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "TOTAL",
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Slate900,
                )
                Text(
                  text = formatRupiah(transaction.totalAmount),
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Navy900,
                )
              }
              val paymentSummary =
                remember(transaction) { ReceiptHelper.buildReceiptPaymentSummary(transaction) }
              ReceiptMetaRow("Metode", transaction.paymentMethod)
              if (paymentSummary.isCashPaid) {
                ReceiptMetaRow("Tunai", formatRupiah(transaction.amountPaid))
                ReceiptMetaRow("Kembali", formatRupiah(transaction.changeAmount))
              }
              if (paymentSummary.paymentEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                DashedReceiptDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Rincian Pembayaran Kasbon",
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = Slate900,
                )
                Spacer(modifier = Modifier.height(4.dp))
                paymentSummary.paymentEntries.forEach { entry ->
                  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    ReceiptMetaRow(entry.label, formatRupiah(entry.amount))
                    Text(
                      text = entry.detail,
                      fontFamily = JetBrainsMono,
                      fontSize = 10.sp,
                      color = Slate500,
                    )
                  }
                }
                if (paymentSummary.paymentEntries.size > 1) {
                  ReceiptMetaRow("Total Dibayar", formatRupiah(paymentSummary.totalPaid))
                }
                if (!transaction.isPaid) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "Sisa Kasbon",
                      fontFamily = JetBrainsMono,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = Amber600,
                    )
                    Text(
                      text = formatRupiah(paymentSummary.remainingKasbon),
                      fontFamily = JetBrainsMono,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = Amber600,
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector =
                    if (transaction.isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
                  contentDescription = null,
                  tint = if (transaction.isPaid) Emerald600 else Amber600,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (transaction.isPaid) "STATUS: LUNAS" else "STATUS: TEMPO / KASBON",
                  fontFamily = JetBrainsMono,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (transaction.isPaid) Emerald600 else Amber600,
                )
              }
            } else {
              Text(
                text = "Barang telah diperiksa & diterima dalam kondisi baik.",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(modifier = Modifier.height(16.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Pengirim / Sopir", fontFamily = JetBrainsMono, fontSize = 11.sp, color = Slate500)
                  Spacer(modifier = Modifier.height(24.dp))
                  Text("( ____________ )", fontFamily = JetBrainsMono, fontSize = 11.sp, color = Slate900)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Penerima Barang", fontFamily = JetBrainsMono, fontSize = 11.sp, color = Slate500)
                  Spacer(modifier = Modifier.height(24.dp))
                  Text("( ____________ )", fontFamily = JetBrainsMono, fontSize = 11.sp, color = Slate900)
                }
              }
            }
          }
        }

        // Bottom Action Buttons with balanced padding & crisp single-line labels
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { showPrintDialog = true },
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("print_receipt_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Cetak · ${settings.thermalPaperSize.widthMm}mm",
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Button(
            onClick = { ReceiptHelper.shareViaWhatsApp(context, transaction, settings, isSuratJalan) },
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("share_whatsapp_button"),
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Emerald600,
                contentColor = Color.White,
              ),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "WhatsApp",
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }

  if (showPrintDialog) {
    ThermalPrintDialog(
      transaction = transaction,
      settings = settings,
      isSuratJalan = isSuratJalan,
      onSavePrintPreferences = onSavePrintPreferences,
      onDismiss = { showPrintDialog = false },
    )
  }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 1.5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      fontFamily = JetBrainsMono,
      fontSize = 11.sp,
      color = Slate500,
      modifier = Modifier.widthIn(min = 64.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      fontFamily = JetBrainsMono,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = Slate900,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun DashedReceiptDivider() {
  Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
    drawLine(
      color = Slate300,
      start = Offset(0f, 0f),
      end = Offset(size.width, 0f),
      strokeWidth = size.height,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
    )
  }
}
