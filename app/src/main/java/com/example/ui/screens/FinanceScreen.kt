package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemCodec
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinanceDateFilter
import com.example.data.model.FinanceDatePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole
import com.example.ui.components.CustomPeriodPickerDialog
import com.example.ui.components.CustomerPurchaseHistorySection
import com.example.ui.components.FinanceReportExportDialog
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StoreSearchBar
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.Orange100
import com.example.ui.theme.Orange400
import com.example.ui.theme.Orange500
import com.example.ui.theme.Orange600
import com.example.ui.theme.Red400
import com.example.ui.theme.Red600
import com.example.ui.theme.Sky400
import com.example.ui.viewmodel.DailyIncomeSummary
import com.example.ui.viewmodel.StoreUiState
import com.example.util.FinanceReportHelper
import com.example.util.addDaysMillis
import com.example.util.formatDateShort
import com.example.util.formatQty
import com.example.util.formatRupiah
import com.example.util.formatShortDateOnly
import com.example.util.localDayStartToUtcPickerMillis
import com.example.util.startOfDayMillis
import com.example.util.utcPickerMillisToLocalDayStart

@Composable
fun FinanceScreen(
  uiState: StoreUiState,
  onSelectDateFilter: (FinanceDateFilter) -> Unit,
  onAddExpense: (String, ExpenseCategory, Long, String, Long) -> Unit,
  onDeleteExpense: (Long) -> Unit,
  onPayKasbon: (Long, Long, PaymentMethod) -> Unit,
  onOpenReceipt: (TransactionEntity) -> Unit,
  onDeleteTransaction: (Long, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isOwner = uiState.settings.activeRole == UserRole.OWNER
  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var payingKasbonTx by remember { mutableStateOf<TransactionEntity?>(null) }

  OwnerFinanceView(
    uiState = uiState,
    isOwner = isOwner,
    onSelectDateFilter = onSelectDateFilter,
    onOpenAddExpense = { showAddExpenseDialog = true },
    onDeleteExpense = onDeleteExpense,
    onOpenPayKasbon = { payingKasbonTx = it },
    onOpenReceipt = onOpenReceipt,
    onDeleteTransaction = onDeleteTransaction,
    modifier = modifier,
  )

  payingKasbonTx?.let { target ->
    PayKasbonDialog(
      tx = target,
      onDismiss = { payingKasbonTx = null },
      onConfirm = { amount, method ->
        onPayKasbon(target.id, amount, method)
        payingKasbonTx = null
      },
    )
  }

  if (showAddExpenseDialog) {
    val defaultTimestamp =
      remember(uiState.financeDateFilter) {
        val today = startOfDayMillis()
        when (uiState.financeDateFilter.preset) {
          FinanceDatePreset.BESOK -> addDaysMillis(today, 1) + 9 * 3_600_000L
          FinanceDatePreset.KEMARIN -> addDaysMillis(today, -1) + 12 * 3_600_000L
          FinanceDatePreset.TANGGAL_PILIHAN ->
            (uiState.financeDateFilter.customStartDayMillis ?: today) + 9 * 3_600_000L
          else -> System.currentTimeMillis()
        }
      }
    AddExpenseDialog(
      isOwner = true,
      initialTimestamp = defaultTimestamp,
      onDismiss = { showAddExpenseDialog = false },
      onConfirm = { title, cat, amount, notes, timestamp ->
        onAddExpense(title, cat, amount, notes, timestamp)
        showAddExpenseDialog = false
      },
    )
  }
}

@Composable
private fun OwnerFinanceView(
  uiState: StoreUiState,
  isOwner: Boolean,
  onSelectDateFilter: (FinanceDateFilter) -> Unit,
  onOpenAddExpense: () -> Unit,
  onDeleteExpense: (Long) -> Unit,
  onOpenPayKasbon: (TransactionEntity) -> Unit,
  onOpenReceipt: (TransactionEntity) -> Unit,
  onDeleteTransaction: (Long, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  var selectedSubTab by remember { mutableIntStateOf(0) }
  var deletingTx by remember { mutableStateOf<TransactionEntity?>(null) }
  var showExportDialog by remember { mutableStateOf(false) }

  val isAllTime = uiState.financeDateFilter.preset == FinanceDatePreset.SEMUA
  val periodLabel = remember(uiState.financeDateFilter) { uiState.financePeriodLabel() }
  val filteredPaidTx =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredPaidTransactions() }
  val filteredPaidRevenue =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredRevenuePaid() }
  val filteredCashRevenue =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredCashRevenue() }
  val filteredTransferRevenue =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredTransferRevenue() }
  val filteredPaidCount =
    remember(uiState.transactions, uiState.financeDateFilter, filteredPaidTx) {
      (filteredPaidTx.map { it.id } + uiState.filteredCashInflows().map { it.transactionId }).distinct().size
    }
  val filteredGrossProfit =
    remember(filteredPaidTx) { filteredPaidTx.sumOf { it.totalAmount - it.totalCost } }
  val filteredExpensesList =
    remember(uiState.expenses, uiState.financeDateFilter) { uiState.filteredExpenses() }
  val filteredTotalExpenses =
    remember(filteredExpensesList) { filteredExpensesList.sumOf { it.amount } }
  val filteredOperationalExpenses =
    remember(filteredExpensesList) {
      filteredExpensesList.filter { it.category != ExpenseCategory.KULAKAN.label }.sumOf { it.amount }
    }
  val filteredKulakanExpenses =
    remember(filteredExpensesList) {
      filteredExpensesList.filter { it.category == ExpenseCategory.KULAKAN.label }.sumOf { it.amount }
    }
  val filteredNetProfit = filteredGrossProfit - filteredOperationalExpenses
  val filteredKasbonList =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredUnpaidKasbon() }
  val filteredReceivables =
    remember(filteredKasbonList) {
      filteredKasbonList.sumOf { (it.totalAmount - it.amountPaid).coerceAtLeast(0L) }
    }
  val filteredTxCount =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredTransactionCount() }
  val filteredCustomerTx =
    remember(uiState.transactions, uiState.financeDateFilter) { uiState.filteredCustomerTransactions() }
  val filteredDailyList =
    remember(uiState.transactions, uiState.expenses, uiState.financeDateFilter) {
      uiState.filteredDailyIncomeHistory()
    }
  val netCashFlow = filteredPaidRevenue - filteredTotalExpenses
  val totalInventoryCostValue = uiState.totalInventoryCostValue

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isCompactHeight = maxHeight < 520.dp
    Column(modifier = Modifier.fillMaxSize()) {
      FinanceDateFilterBar(
        currentFilter = uiState.financeDateFilter,
        periodLabel = periodLabel,
        onSelectDateFilter = onSelectDateFilter,
        onOpenExportDialog = { showExportDialog = true },
      )

      FinanceSubTabBar(
        selectedSubTab = selectedSubTab,
        kasbonCount = filteredKasbonList.size,
        onSelectTab = { selectedSubTab = it },
        compact = isCompactHeight,
      )

    when (selectedSubTab) {
      0 -> {
        val profitAccent = if (isDark) Emerald400 else Emerald600
        val expenseAccent = if (isDark) Red400 else Red600
        val incomeAccent = if (isDark) Sky400 else MaterialTheme.colorScheme.primary
        val kasbonAccent = if (isDark) Amber400 else Amber600

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          item(key = "hero_banner") {
            FinanceHeroSummaryCard(
              periodLabel = periodLabel,
              netCashFlow = netCashFlow,
              paidRevenue = filteredPaidRevenue,
              cashRevenue = filteredCashRevenue,
              transferRevenue = filteredTransferRevenue,
              grossProfit = filteredGrossProfit,
              netProfit = filteredNetProfit,
              txCount = filteredTxCount,
            )
          }

          item(key = "overall_metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                MetricStatCard(
                  title = "Uang Masuk",
                  valueText = formatRupiah(filteredPaidRevenue),
                  subtitle =
                    "$filteredPaidCount nota • Tunai ${formatRupiah(filteredCashRevenue)} • TF ${formatRupiah(filteredTransferRevenue)}",
                  icon = Icons.Default.AccountBalanceWallet,
                  accentColor = incomeAccent,
                  modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                MetricStatCard(
                  title = "Uang Keluar",
                  valueText = formatRupiah(filteredTotalExpenses),
                  subtitle =
                    "Op: ${formatRupiah(filteredOperationalExpenses)} • Kulakan: ${formatRupiah(filteredKulakanExpenses)}",
                  icon = Icons.Default.MoneyOff,
                  accentColor = expenseAccent,
                  modifier = Modifier.weight(1f).fillMaxHeight(),
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                MetricStatCard(
                  title = "Untung Bersih",
                  valueText = formatRupiah(filteredNetProfit),
                  subtitle =
                    "Kotor ${formatRupiah(filteredGrossProfit)} - Biaya Op.",
                  icon = Icons.AutoMirrored.Filled.TrendingUp,
                  accentColor = if (filteredNetProfit >= 0L) profitAccent else expenseAccent,
                  modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                MetricStatCard(
                  title = "Kasbon Belum Lunas",
                  valueText = formatRupiah(filteredReceivables),
                  subtitle = "${filteredKasbonList.size} bon belum dibayar",
                  icon = Icons.Default.Schedule,
                  accentColor = kasbonAccent,
                  modifier = Modifier.weight(1f).fillMaxHeight(),
                )
              }
              InventoryAssetCard(
                inventoryCostValue = totalInventoryCostValue,
                productCount = uiState.products.size,
              )
            }
          }

          item(key = "daily_section_header") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (isAllTime) "Catatan Harian" else "Catatan Harian • $periodLabel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (filteredDailyList.isNotEmpty()) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Text(
                    text = "${filteredDailyList.size} hari",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
            }
          }

          if (filteredDailyList.isEmpty()) {
            item(key = "empty_daily") {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Belum ada transaksi pada periode ini",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  TextButton(
                    onClick = { onSelectDateFilter(FinanceDateFilter(FinanceDatePreset.SEMUA)) }
                  ) {
                    Text("Tampilkan Semua Tanggal")
                  }
                }
              }
            }
          } else {
            items(filteredDailyList, key = { it.dayStartMillis }) { daySummary ->
              DailyIncomeCard(summary = daySummary)
            }
          }
        }
      }
      1 -> {
        CustomerPurchaseHistorySection(
          transactions = filteredCustomerTx,
          isOwner = isOwner,
          showProfit = true,
          onOpenReceipt = onOpenReceipt,
          onPayKasbon = onOpenPayKasbon,
          onDeleteTransaction = if (isOwner) { tx -> deletingTx = tx } else null,
        )
      }
      2 -> {
        KasbonSectionContent(
          isAllTime = isAllTime,
          periodLabel = periodLabel,
          totalUnpaidCount = uiState.unpaidKasbonList.size,
          filteredKasbonList = filteredKasbonList,
          onShowAllDates = { onSelectDateFilter(FinanceDateFilter(FinanceDatePreset.SEMUA)) },
          onOpenReceipt = onOpenReceipt,
          onOpenPayKasbon = onOpenPayKasbon,
        )
      }
      3 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item(key = "add_expense_button") {
            Button(
              onClick = onOpenAddExpense,
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("owner_add_expense_btn"),
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (isDark) Orange600 else Navy900,
                  contentColor = Color.White,
                ),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Catat Pengeluaran Baru", fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
          if (filteredExpensesList.isEmpty()) {
            item(key = "empty_expenses") {
              Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "Belum ada catatan pengeluaran pada periode ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (!isAllTime) {
                    TextButton(
                      onClick = { onSelectDateFilter(FinanceDateFilter(FinanceDatePreset.SEMUA)) }
                    ) {
                      Text("Tampilkan Semua")
                    }
                  }
                }
              }
            }
          } else {
            items(filteredExpensesList, key = { it.id }) { exp ->
              ExpenseCard(
                expense = exp,
                canDelete = isOwner,
                onDelete = { onDeleteExpense(exp.id) },
              )
            }
          }
        }
      }
    }
    }
  }

  if (showExportDialog) {
    FinanceReportExportDialog(
      uiState = uiState,
      onDismiss = { showExportDialog = false },
      onPrintPdf = { report -> FinanceReportHelper.printFinanceReportPdf(context, report) },
      onShareWhatsApp = { report -> FinanceReportHelper.shareReportToWhatsApp(context, report) },
      onShareOther = { report -> FinanceReportHelper.shareReportPdfGeneric(context, report) },
    )
  }

  deletingTx?.let { target ->
    var restoreStock by remember(target.id) { mutableStateOf(true) }
    AlertDialog(
      onDismissRequest = { deletingTx = null },
      title = { Text("Hapus Riwayat Transaksi?", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Hapus nota ${target.invoiceNumber} (${formatRupiah(target.totalAmount)})?")
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { restoreStock = !restoreStock },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = restoreStock,
                onCheckedChange = { restoreStock = it },
                modifier = Modifier.testTag("delete_tx_restore_stock_checkbox"),
              )
              Text(
                text = "Kembalikan stok barang ke gudang (Retur)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteTransaction(target.id, restoreStock)
            deletingTx = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
        ) {
          Text("Hapus", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = { TextButton(onClick = { deletingTx = null }) { Text("Batal") } },
    )
  }
}

@Composable
private fun InventoryAssetCard(
  inventoryCostValue: Long,
  productCount: Int,
) {
  val isDark = isSystemInDarkTheme()
  val iconBg = if (isDark) Orange500.copy(alpha = 0.16f) else Orange100
  val iconFg = if (isDark) Orange400 else Orange600

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("inventory_asset_card"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = iconBg,
        modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            tint = iconFg,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Nilai Aset Stok Gudang",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "$productCount jenis barang (dihitung dari harga modal)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = formatRupiah(inventoryCostValue),
        fontFamily = JetBrainsMono,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = iconFg,
      )
    }
  }
}

@Composable
private fun FinanceHeroSummaryCard(
  periodLabel: String,
  netCashFlow: Long,
  paidRevenue: Long,
  cashRevenue: Long,
  transferRevenue: Long,
  grossProfit: Long,
  netProfit: Long,
  txCount: Int,
) {
  val isSurplus = netCashFlow >= 0L
  val netAccentColor = if (isSurplus) Color.White else Color(0xFFFCA5A5)
  val profitColor = if (netProfit >= 0L) Color(0xFF86EFAC) else Color(0xFFFCA5A5)

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("daily_income_hero_card"),
    shape = RoundedCornerShape(20.dp),
    shadowElevation = 4.dp,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.linearGradient(listOf(Navy950, Navy900, Navy800)))
          .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              tint = Color(0xFFFDBA74),
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Sisa Uang Toko • $periodLabel",
              style = MaterialTheme.typography.labelLarge,
              color = Color(0xFFE2E8F0),
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Orange600,
          ) {
            Text(
              text = "$txCount Transaksi",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = formatRupiah(netCashFlow),
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = netAccentColor,
          )
          Text(
            text = "Total uang masuk kas dikurangi semua uang keluar",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
          )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Total Uang Masuk",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
            )
            Text(
              text = formatRupiah(paidRevenue),
              fontFamily = JetBrainsMono,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDBA74),
            )
            Text(
              text = "Tunai ${formatRupiah(cashRevenue)} • TF ${formatRupiah(transferRevenue)}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = "Untung Bersih",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
            )
            Text(
              text = formatRupiah(netProfit),
              fontFamily = JetBrainsMono,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = profitColor,
            )
            Text(
              text = "Kotor: ${formatRupiah(grossProfit)}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FinanceSubTabBar(
  selectedSubTab: Int,
  kasbonCount: Int,
  onSelectTab: (Int) -> Unit,
  compact: Boolean = false,
) {
  val tabs =
    listOf(
      Triple(0, "Ringkasan", 1.05f to "finance_tab_daily"),
      Triple(1, "Pelanggan", 1.15f to "finance_tab_history"),
      Triple(2, "Kasbon", 1.10f to "finance_tab_kasbon"),
      Triple(3, "Biaya", 0.70f to "finance_tab_expenses"),
    )
  SharedFinanceSubTabBar(
    tabs = tabs,
    selectedSubTab = selectedSubTab,
    kasbonTabIndex = 2,
    kasbonCount = kasbonCount,
    onSelectTab = onSelectTab,
    compact = compact,
  )
}

@Composable
private fun SharedFinanceSubTabBar(
  tabs: List<Triple<Int, String, Pair<Float, String>>>,
  selectedSubTab: Int,
  kasbonTabIndex: Int,
  kasbonCount: Int,
  onSelectTab: (Int) -> Unit,
  compact: Boolean = false,
) {
  val isDark = isSystemInDarkTheme()
  val activeTextColor = if (isDark) Orange400 else MaterialTheme.colorScheme.primary
  val activeIndicatorColor = if (isDark) Orange500 else MaterialTheme.colorScheme.primary
  val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEach { (index, label, weightAndTag) ->
          val (weight, tag) = weightAndTag
          val selected = selectedSubTab == index
          Column(
            modifier =
              Modifier.weight(weight)
                .heightIn(min = if (compact) 42.dp else 48.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .clickable { onSelectTab(index) }
                .testTag(tag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier =
                Modifier.padding(
                  horizontal = 4.dp,
                  vertical = if (compact) 8.dp else 12.dp,
                ),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeTextColor else inactiveTextColor,
                maxLines = 1,
                softWrap = false,
              )
              if (index == kasbonTabIndex) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color =
                    if (selected) activeIndicatorColor.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Text(
                    text = "$kasbonCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) activeTextColor else inactiveTextColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
              }
            }
            Box(
              modifier =
                Modifier.fillMaxWidth(0.75f)
                  .height(3.dp)
                  .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                  .background(if (selected) activeIndicatorColor else Color.Transparent)
            )
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun KasbonSectionContent(
  isAllTime: Boolean,
  periodLabel: String,
  totalUnpaidCount: Int,
  filteredKasbonList: List<TransactionEntity>,
  onShowAllDates: () -> Unit,
  onOpenReceipt: (TransactionEntity) -> Unit,
  onOpenPayKasbon: (TransactionEntity) -> Unit,
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val bannerBg = if (isDark) Amber400.copy(alpha = 0.14f) else Amber100
  val bannerFg = if (isDark) Amber400 else Amber600
  var searchQuery by remember { mutableStateOf("") }

  val visibleKasbons =
    remember(filteredKasbonList, searchQuery) {
      if (searchQuery.isBlank()) filteredKasbonList
      else {
        filteredKasbonList.filter { tx ->
          tx.customerName.contains(searchQuery, ignoreCase = true) ||
            tx.customerPhone.contains(searchQuery, ignoreCase = true) ||
            tx.invoiceNumber.contains(searchQuery, ignoreCase = true)
        }
      }
    }

  Column(modifier = Modifier.fillMaxSize()) {
    if (!isAllTime && totalUnpaidCount > filteredKasbonList.size) {
      Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = bannerBg,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text =
              "Menampilkan ${filteredKasbonList.size} dari $totalUnpaidCount kasbon belum lunas ($periodLabel).",
            style = MaterialTheme.typography.labelSmall,
            color = bannerFg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = onShowAllDates) {
            Text("Semua", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    if (filteredKasbonList.isNotEmpty()) {
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        StoreSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = "Cari nama pelanggan atau no. nota kasbon...",
          testTag = "kasbon_search_input",
        )
      }
    }

    if (visibleKasbons.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp),
        ) {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isDark) Emerald400 else Emerald600,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              when {
                searchQuery.isNotBlank() && filteredKasbonList.isNotEmpty() ->
                  "Kasbon Tidak Ditemukan"
                isAllTime -> "Semua Kasbon Sudah Lunas!"
                else -> "Tidak Ada Kasbon di Periode Ini"
              },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          if (!isAllTime && totalUnpaidCount > 0 && searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onShowAllDates) {
              Text("Lihat $totalUnpaidCount Kasbon Aktif Lainnya")
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(visibleKasbons, key = { it.id }) { tx ->
          KasbonCard(
            tx = tx,
            onViewReceipt = { onOpenReceipt(tx) },
            onRemindWhatsApp = {
              val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
              val phone = tx.customerPhone.filter { it.isDigit() }
              val waPhone = if (phone.startsWith("0")) "62" + phone.drop(1) else phone
              val detailText =
                if (tx.amountPaid > 0L) {
                  "sisa tagihan ${formatRupiah(remaining)} (dari total ${formatRupiah(tx.totalAmount)})"
                } else {
                  "sebesar ${formatRupiah(remaining)}"
                }
              val msg =
                "Halo ${tx.customerName}, pengingat nota tempo TB PUTRA RI (${tx.invoiceNumber}) $detailText. Terima kasih."
              runCatching {
                context.startActivity(
                  Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$waPhone?text=${Uri.encode(msg)}"),
                  )
                )
              }
            },
            onPayKasbon = { onOpenPayKasbon(tx) },
          )
        }
      }
    }
  }
}

@Composable
private fun DailyIncomeCard(summary: DailyIncomeSummary) {
  val isDark = isSystemInDarkTheme()
  val profitColor = if (isDark) Emerald400 else Emerald600
  val kasbonColor = if (isDark) Amber400 else Amber600
  val dangerColor = if (isDark) Red400 else Red600
  val incomeColor = if (isDark) Sky400 else MaterialTheme.colorScheme.primary
  val netDailyCash = summary.paidRevenue - summary.totalExpenses

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = summary.dayLabel,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        val notaBg = if (isDark) Sky400.copy(alpha = 0.16f) else Navy100
        val notaFg = if (isDark) Sky400 else Navy900
        Surface(shape = RoundedCornerShape(999.dp), color = notaBg) {
          Text(
            text = "${summary.transactionCount} Nota",
            style = MaterialTheme.typography.labelSmall,
            color = notaFg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Masuk",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatRupiah(summary.paidRevenue),
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = incomeColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Keluar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatRupiah(summary.totalExpenses),
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = dangerColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            text = "Untung Bersih",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatRupiah(summary.netProfit),
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (summary.netProfit >= 0L) profitColor else dangerColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (summary.cashRevenue > 0L || summary.transferRevenue > 0L) {
        Text(
          text =
            "Tunai: ${formatRupiah(summary.cashRevenue)}  •  Transfer/QRIS: ${formatRupiah(summary.transferRevenue)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Sisa Uang: ${formatRupiah(netDailyCash)}",
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (netDailyCash >= 0) profitColor else dangerColor,
          )
          if (summary.kasbonRevenue > 0L) {
            Text(
              text = "Kasbon: ${formatRupiah(summary.kasbonRevenue)}",
              fontFamily = JetBrainsMono,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = kasbonColor,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun KasbonCard(
  tx: TransactionEntity,
  onViewReceipt: () -> Unit,
  onRemindWhatsApp: () -> Unit,
  onPayKasbon: () -> Unit,
) {
  val items = remember(tx.itemsJson) { TransactionItemCodec.decode(tx.itemsJson) }
  val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
  val isDark = isSystemInDarkTheme()
  val kasbonBg = if (isDark) Amber400.copy(alpha = 0.18f) else Amber100
  val kasbonFg = if (isDark) Amber400 else Amber600
  val paidFg = if (isDark) Emerald400 else Emerald600

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, kasbonFg.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Surface(shape = RoundedCornerShape(6.dp), color = kasbonBg) {
            Text(
              text =
                if (tx.amountPaid > 0L) "${tx.invoiceNumber} • CICILAN / DP"
                else "${tx.invoiceNumber} • BELUM LUNAS",
              style = MaterialTheme.typography.labelSmall,
              color = kasbonFg,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = tx.customerName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${tx.customerPhone.ifBlank { "Tanpa No. HP" }} • ${formatDateShort(tx.timestamp)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (tx.amountPaid > 0L) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Total Nota ${formatRupiah(tx.totalAmount)} • Sudah Bayar ${formatRupiah(tx.amountPaid)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = paidFg,
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
          if (tx.amountPaid > 0L) {
            Text(
              text = "Sisa Tagihan",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = formatRupiah(remaining),
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Red400 else Red600,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = items.joinToString(", ") { "${it.productName} (${formatQty(it.quantity)} ${it.unitName})" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onViewReceipt,
          modifier = Modifier.height(48.dp),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = "Lihat nota",
            modifier = Modifier.size(18.dp),
          )
        }
        OutlinedButton(
          onClick = onRemindWhatsApp,
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tagih WA", style = MaterialTheme.typography.labelMedium)
        }
        Button(
          onClick = onPayKasbon,
          modifier = Modifier.weight(1.1f).height(48.dp).testTag("lunasi_kasbon_${tx.id}"),
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Emerald600,
              contentColor = Color.White,
            ),
        ) {
          Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            "Bayar / Lunasi",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun PayKasbonDialog(
  tx: TransactionEntity,
  onDismiss: () -> Unit,
  onConfirm: (Long, PaymentMethod) -> Unit,
) {
  val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
  var selectedMethod by remember(tx.id) { mutableStateOf(PaymentMethod.TUNAI) }
  var amountText by remember(tx.id) { mutableStateOf(remaining.toString()) }
  var error by remember { mutableStateOf<String?>(null) }

  val payAmount = amountText.toLongOrNull() ?: 0L
  val isFullSettlement = payAmount == remaining && remaining > 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text("Bayar / Lunasi Kasbon", fontWeight = FontWeight.Bold)
        Text(
          text = "${tx.customerName} • ${tx.invoiceNumber}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Total Nota", style = MaterialTheme.typography.bodySmall)
              Text(formatRupiah(tx.totalAmount), fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodySmall)
            }
            if (tx.amountPaid > 0L) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sudah Dibayar", style = MaterialTheme.typography.bodySmall)
                Text(formatRupiah(tx.amountPaid), fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodySmall, color = Emerald600)
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Sisa Tagihan", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
              Text(
                formatRupiah(remaining),
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Red600,
              )
            }
          }
        }

        Text(
          text = "Cara Terima Uang",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          val methods = listOf(PaymentMethod.TUNAI, PaymentMethod.TRANSFER_QRIS)
          methods.forEachIndexed { idx, method ->
            SegmentedButton(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              shape = SegmentedButtonDefaults.itemShape(idx, methods.size),
            ) {
              Text(method.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it.filter { c -> c.isDigit() }
            error = null
          },
          label = { Text("Nominal Pembayaran (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("kasbon_pay_amount_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          val quickOptions =
            buildList {
              add("Lunasi Sisa" to remaining)
              if (remaining > 20_000L) add("Cicil 50%" to (remaining / 2L))
              if (remaining > 100_000L) add("100rb" to 100_000L)
              if (remaining > 500_000L) add("500rb" to 500_000L)
            }
          quickOptions.forEach { (label, preset) ->
            FilterChip(
              selected = payAmount == preset,
              onClick = {
                amountText = preset.toString()
                error = null
              },
              label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
            )
          }
        }

        if (payAmount > 0L && payAmount <= remaining) {
          Text(
            text =
              if (isFullSettlement) "Status nota akan berubah menjadi LUNAS."
              else "Sisa tagihan setelah bayar: ${formatRupiah(remaining - payAmount)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isFullSettlement) Emerald600 else Amber600,
          )
        }

        if (error != null) {
          Text(error!!, color = Red600, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            payAmount <= 0L -> error = "Masukkan nominal pembayaran yang valid."
            payAmount > remaining -> error = "Nominal melebihi sisa tagihan (${formatRupiah(remaining)})."
            else -> onConfirm(payAmount, selectedMethod)
          }
        },
        modifier = Modifier.testTag("confirm_pay_kasbon_btn"),
        colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
      ) {
        Text(
          text = if (isFullSettlement) "Lunasi" else "Catat Cicilan",
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
private fun ExpenseCard(
  expense: ExpenseEntity,
  canDelete: Boolean,
  onDelete: () -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val catBg = if (isDark) Orange500.copy(alpha = 0.18f) else Orange100
  val catFg = if (isDark) Orange400 else Orange600
  val dangerFg = if (isDark) Red400 else Red600

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Surface(shape = RoundedCornerShape(6.dp), color = catBg) {
          Text(
            text = expense.category,
            style = MaterialTheme.typography.labelSmall,
            color = catFg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = expense.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        if (expense.notes.isNotBlank()) {
          Text(
            text = expense.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "${expense.recordedBy} • ${formatDateShort(expense.timestamp)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "-${formatRupiah(expense.amount)}",
          fontFamily = JetBrainsMono,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = dangerFg,
        )
        if (canDelete) {
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Hapus pengeluaran",
              tint = dangerFg,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FinanceDateFilterBar(
  currentFilter: FinanceDateFilter,
  periodLabel: String,
  onSelectDateFilter: (FinanceDateFilter) -> Unit,
  onOpenExportDialog: (() -> Unit)?,
) {
  val isDark = isSystemInDarkTheme()
  val badgeBg = if (isDark) Orange500.copy(alpha = 0.20f) else Orange100
  val badgeFg = if (isDark) Orange400 else Orange600

  var showCustomPeriodDialog by remember { mutableStateOf(false) }

  val isAll = currentFilter.preset == FinanceDatePreset.SEMUA
  val isToday = currentFilter.preset == FinanceDatePreset.HARI_INI
  val isYesterday = currentFilter.preset == FinanceDatePreset.KEMARIN
  val isCustom = !isAll && !isToday && !isYesterday

  val chipItems =
    listOf(
      Triple("Semua Waktu", isAll, "date_preset_semua"),
      Triple("Hari Ini", isToday, "date_preset_hari_ini"),
      Triple("Kemarin", isYesterday, "date_preset_kemarin"),
      Triple("Custom Periode", isCustom, "date_preset_custom"),
    )

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("finance_date_filter_bar"),
    color = MaterialTheme.colorScheme.surface,
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val useSingleRow = maxWidth >= 600.dp

      @Composable
      fun PeriodBadge() {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = badgeFg,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(shape = RoundedCornerShape(999.dp), color = badgeBg) {
            Text(
              text = periodLabel,
              style = MaterialTheme.typography.labelSmall,
              color = badgeFg,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            )
          }
        }
      }

      @Composable
      fun FilterChipsStrip(chipModifier: Modifier = Modifier) {
        Row(
          modifier = chipModifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          chipItems.forEachIndexed { idx, (label, selected, tag) ->
            FilterChip(
              selected = selected,
              onClick = {
                when (idx) {
                  0 -> onSelectDateFilter(FinanceDateFilter(FinanceDatePreset.SEMUA))
                  1 -> onSelectDateFilter(FinanceDateFilter(FinanceDatePreset.HARI_INI))
                  2 -> onSelectDateFilter(FinanceDateFilter(FinanceDatePreset.KEMARIN))
                  else -> showCustomPeriodDialog = true
                }
              },
              shape = RoundedCornerShape(999.dp),
              label = {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                )
              },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Orange600,
                  selectedLabelColor = Color.White,
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              border =
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = Color.Transparent,
                  selectedBorderColor = Orange600,
                ),
              modifier = Modifier.testTag(tag),
            )
          }
        }
      }

      @Composable
      fun PrintReportPill(onClick: () -> Unit) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Orange600,
          modifier =
            Modifier.heightIn(min = 34.dp)
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onClick)
              .testTag("finance_print_report_btn"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Print,
              contentDescription = "Cetak Laporan",
              tint = Color.White,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Cetak Laporan",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
            )
          }
        }
      }

      if (useSingleRow) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          PeriodBadge()
          FilterChipsStrip(chipModifier = Modifier.weight(1f))
          if (onOpenExportDialog != null) {
            PrintReportPill(onClick = onOpenExportDialog)
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Box(modifier = Modifier.weight(1f)) { PeriodBadge() }
            if (onOpenExportDialog != null) {
              Spacer(modifier = Modifier.width(8.dp))
              PrintReportPill(onClick = onOpenExportDialog)
            }
          }
          FilterChipsStrip(chipModifier = Modifier.fillMaxWidth())
        }
      }
    }
  }

  if (showCustomPeriodDialog) {
    CustomPeriodPickerDialog(
      initialFilter = currentFilter,
      onDismiss = { showCustomPeriodDialog = false },
      onApply = { customFilter ->
        onSelectDateFilter(customFilter)
        showCustomPeriodDialog = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
  isOwner: Boolean,
  initialTimestamp: Long,
  onDismiss: () -> Unit,
  onConfirm: (String, ExpenseCategory, Long, String, Long) -> Unit,
) {
  val categories =
    remember(isOwner) {
      if (isOwner) ExpenseCategory.entries
      else ExpenseCategory.entries.filter { it != ExpenseCategory.KULAKAN }
    }
  var title by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(categories.first()) }
  var amountText by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var selectedTimestamp by remember(initialTimestamp) { mutableLongStateOf(initialTimestamp) }
  var showExpenseDatePicker by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Catat Pengeluaran", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          categories.forEach { cat ->
            val selected = selectedCategory == cat
            FilterChip(
              selected = selected,
              onClick = { selectedCategory = cat },
              label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Orange600,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val today = startOfDayMillis()
          val tomorrow = addDaysMillis(today, 1)
          val yesterday = addDaysMillis(today, -1)
          val selectedDay = startOfDayMillis(selectedTimestamp)
          val chipColors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = Orange600,
              selectedLabelColor = Color.White,
            )
          FilterChip(
            selected = selectedDay == today,
            onClick = { selectedTimestamp = System.currentTimeMillis() },
            colors = chipColors,
            label = { Text("Hari Ini", style = MaterialTheme.typography.labelSmall) },
          )
          FilterChip(
            selected = selectedDay == tomorrow,
            onClick = { selectedTimestamp = tomorrow + 9 * 3_600_000L },
            colors = chipColors,
            label = { Text("Besok", style = MaterialTheme.typography.labelSmall) },
          )
          FilterChip(
            selected = selectedDay == yesterday,
            onClick = { selectedTimestamp = yesterday + 12 * 3_600_000L },
            colors = chipColors,
            label = { Text("Kemarin", style = MaterialTheme.typography.labelSmall) },
          )
          FilterChip(
            selected = selectedDay != today && selectedDay != tomorrow && selectedDay != yesterday,
            onClick = { showExpenseDatePicker = true },
            colors = chipColors,
            leadingIcon = {
              Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
            },
            label = {
              Text(formatShortDateOnly(selectedDay), style = MaterialTheme.typography.labelSmall)
            },
          )
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Keterangan (mis. Bensin Pikap)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text("Nominal (Rp)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Catatan Tambahan (Opsional)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
          Text(error!!, color = Red600, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amount = amountText.toLongOrNull() ?: 0L
          if (title.isBlank() || amount <= 0L) {
            error = "Isi keterangan & nominal pengeluaran yang valid"
          } else {
            onConfirm(title, selectedCategory, amount, notes, selectedTimestamp)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = Color.White),
      ) {
        Text("Simpan", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )

  if (showExpenseDatePicker) {
    val pickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = localDayStartToUtcPickerMillis(startOfDayMillis(selectedTimestamp))
      )
    DatePickerDialog(
      onDismissRequest = { showExpenseDatePicker = false },
      confirmButton = {
        Button(
          onClick = {
            val utc = pickerState.selectedDateMillis
            if (utc != null) {
              selectedTimestamp = utcPickerMillisToLocalDayStart(utc) + 9 * 3_600_000L
            }
            showExpenseDatePicker = false
          }
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExpenseDatePicker = false }) { Text("Batal") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}
