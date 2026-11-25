package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemCodec
import com.example.data.model.CustomerType
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy900
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.ui.theme.Sky400
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.util.formatDateTime
import com.example.util.formatQty
import com.example.util.formatRupiah

private enum class HistoryFilter(val label: String) {
  ALL("Semua"),
  PELANGGAN("Pelanggan"),
  KONTRAKTOR("Kontraktor"),
  KASBON("Kasbon"),
}

@Composable
fun CustomerPurchaseHistorySection(
  transactions: List<TransactionEntity>,
  isOwner: Boolean,
  onOpenReceipt: (TransactionEntity) -> Unit,
  onPayKasbon: ((TransactionEntity) -> Unit)? = null,
  onDeleteTransaction: ((TransactionEntity) -> Unit)? = null,
  showProfit: Boolean = isOwner,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

  val filteredTransactions =
    remember(transactions, searchQuery, selectedFilter) {
      transactions.filter { tx ->
        val resolvedType = CustomerType.fromStoredLabel(tx.customerType)
        val matchesFilter =
          when (selectedFilter) {
            HistoryFilter.ALL -> true
            HistoryFilter.PELANGGAN -> resolvedType == CustomerType.PELANGGAN
            HistoryFilter.KONTRAKTOR -> resolvedType == CustomerType.KONTRAKTOR
            HistoryFilter.KASBON -> !tx.isPaid
          }
        val matchesQuery =
          searchQuery.isBlank() ||
            tx.customerName.contains(searchQuery, ignoreCase = true) ||
            tx.customerPhone.contains(searchQuery, ignoreCase = true) ||
            tx.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
            tx.itemsJson.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesQuery
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val useSingleRow = maxWidth >= 600.dp

      @Composable
      fun FilterChipsRow(chipsModifier: Modifier = Modifier) {
        Row(
          modifier = chipsModifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HistoryFilter.entries.forEach { filter ->
            val selected = selectedFilter == filter
            FilterChip(
              selected = selected,
              onClick = { selectedFilter = filter },
              label = {
                Text(
                  text = filter.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                )
              },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Orange600,
                  selectedLabelColor = Color.White,
                  containerColor = MaterialTheme.colorScheme.surface,
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              border =
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = MaterialTheme.colorScheme.outlineVariant,
                  selectedBorderColor = Orange600,
                ),
            )
          }
        }
      }

      if (useSingleRow) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StoreSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Cari nama pelanggan, nota, atau barang...",
            modifier = Modifier.weight(1f),
            testTag = "history_search_input",
          )
          FilterChipsRow()
        }
      } else {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          StoreSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Cari nama pelanggan, nota, atau barang...",
            testTag = "history_search_input",
          )
          Spacer(modifier = Modifier.height(8.dp))
          FilterChipsRow(chipsModifier = Modifier.fillMaxWidth())
        }
      }
    }

    if (filteredTransactions.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Belum ada catatan belanja pelanggan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Setiap transaksi kasir akan otomatis muncul di sini.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredTransactions, key = { it.id }) { tx ->
          CustomerPurchaseCard(
            tx = tx,
            showProfit = showProfit,
            onOpenReceipt = { onOpenReceipt(tx) },
            onPayKasbon =
              if (!tx.isPaid && onPayKasbon != null) {
                { onPayKasbon(tx) }
              } else null,
            onDelete =
              if (isOwner && onDeleteTransaction != null) {
                { onDeleteTransaction(tx) }
              } else null,
          )
        }
      }
    }
  }
}

@Composable
private fun CustomerPurchaseCard(
  tx: TransactionEntity,
  showProfit: Boolean,
  onOpenReceipt: () -> Unit,
  onPayKasbon: (() -> Unit)? = null,
  onDelete: (() -> Unit)? = null,
) {
  val items = remember(tx.itemsJson) { TransactionItemCodec.decode(tx.itemsJson) }
  val customerType = remember(tx.customerType) { CustomerType.fromStoredLabel(tx.customerType) }
  val isContractor = customerType == CustomerType.KONTRAKTOR

  val isDark = androidx.compose.foundation.isSystemInDarkTheme()
  val profitColor = if (isDark) Emerald400 else Emerald600
  val dangerColor = if (isDark) Red400 else Red600
  val statusBg =
    when {
      tx.isPaid && isDark -> Emerald600.copy(alpha = 0.2f)
      tx.isPaid -> Emerald100
      isDark -> Amber400.copy(alpha = 0.2f)
      else -> Amber100
    }
  val statusFg =
    when {
      tx.isPaid && isDark -> Emerald400
      tx.isPaid -> Emerald600
      isDark -> Amber400
      else -> Amber600
    }

  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenReceipt),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = tx.invoiceNumber,
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = statusBg,
          ) {
            Text(
              text = if (tx.isPaid) "LUNAS" else "KASBON",
              style = MaterialTheme.typography.labelSmall,
              color = statusFg,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = formatDateTime(tx.timestamp),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Orange600,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = tx.customerName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                buildString {
                  append(tx.paymentMethod)
                  if (tx.customerPhone.isNotBlank()) append(" • ${tx.customerPhone}")
                  append(" • Kasir: ${tx.cashierName}")
                },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val custTypeBg =
          when {
            isContractor && isDark -> Orange500.copy(alpha = 0.2f)
            isContractor -> Orange100
            isDark -> Sky400.copy(alpha = 0.16f)
            else -> Navy100
          }
        val custTypeFg =
          when {
            isContractor && isDark -> Orange400
            isContractor -> Orange600
            isDark -> Sky400
            else -> Navy900
          }
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = custTypeBg,
        ) {
          Text(
            text = if (isContractor) "Kontraktor" else "Pelanggan",
            style = MaterialTheme.typography.labelSmall,
            color = custTypeFg,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Itemized list of customer purchases inside soft box
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          items.forEach { item ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "• ${item.productName} (${formatQty(item.quantity)} ${item.unitName})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = formatRupiah(item.subtotal),
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
              )
            }
          }

          if (tx.discountAmount > 0L) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text =
                  if (customerType.discountRate > 0.0) {
                    "Diskon ${customerType.label} (${(customerType.discountRate * 100).toInt()}%)"
                  } else {
                    "Potongan Harga"
                  },
                style = MaterialTheme.typography.labelSmall,
                color = profitColor,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "-${formatRupiah(tx.discountAmount)}",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                color = profitColor,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Total Transaksi",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatRupiah(tx.totalAmount),
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        if (!tx.isPaid && tx.amountPaid > 0L) {
          val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
          Text(
            text = "Dibayar: ${formatRupiah(tx.amountPaid)} • Sisa: ${formatRupiah(remaining)}",
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Amber400 else Amber600,
            fontWeight = FontWeight.SemiBold,
          )
        }
        if (showProfit) {
          Text(
            text = "Untung Kotor Nota: ${formatRupiah(tx.totalAmount - tx.totalCost)}",
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.labelSmall,
            color = profitColor,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (!tx.isPaid && onPayKasbon != null) {
        Button(
          onClick = onPayKasbon,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pay_kasbon_history_${tx.id}"),
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Emerald600,
              contentColor = Color.White,
            ),
        ) {
          Icon(
            Icons.Default.Payments,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Bayar / Cicil Kasbon",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onOpenReceipt,
          modifier = Modifier.weight(1f).height(48.dp).testTag("open_receipt_${tx.id}"),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Struk / WA",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (onDelete != null) {
          OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.height(48.dp).testTag("delete_transaction_${tx.id}"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, dangerColor.copy(alpha = 0.45f)),
            contentPadding = PaddingValues(horizontal = 14.dp),
          ) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Hapus transaksi",
              tint = dangerColor,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Hapus",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = dangerColor,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
