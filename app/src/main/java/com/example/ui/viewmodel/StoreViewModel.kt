package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExpenseEntity
import com.example.data.local.KasbonPaymentCodec
import com.example.data.local.ProductEntity
import com.example.data.local.SettingsPreferences
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.model.ALL_CATEGORY_LABEL
import com.example.data.model.AppTab
import com.example.data.model.CartItem
import com.example.data.model.CategoryItem
import com.example.data.model.CustomerType
import com.example.data.model.DEFAULT_PRODUCT_CATEGORIES
import com.example.data.model.EmployeeAccount
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinanceDateFilter
import com.example.data.model.FinanceDatePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.ReceiptPaperSize
import com.example.data.model.SavedCustomerOption
import com.example.data.model.UserRole
import com.example.data.remote.userFriendlyFirestoreMessage
import com.example.data.repository.CloudSyncInfo
import com.example.data.repository.StoreRepository
import com.example.util.FinanceReportData
import com.example.util.addDaysMillis
import com.example.util.endOfMonthExclusiveMillis
import com.example.util.endOfWeekExclusiveMillis
import com.example.util.endOfYearExclusiveMillis
import com.example.util.formatDayLabel
import com.example.util.formatMonthYear
import com.example.util.formatQty
import com.example.util.formatShortDateOnly
import com.example.util.formatShortDayMonth
import com.example.util.formatYearOnly
import com.example.util.startOfDayMillis
import com.example.util.startOfMonthMillis
import com.example.util.startOfWeekMillis
import com.example.util.startOfYearMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashInflowEvent(
  val transactionId: Long,
  val timestamp: Long,
  val amount: Long,
  val paymentMethod: String,
)

data class DailyIncomeSummary(
  val dayStartMillis: Long,
  val dayLabel: String,
  val transactionCount: Int,
  val paidRevenue: Long,
  val cashRevenue: Long = 0L,
  val transferRevenue: Long = 0L,
  val kasbonRevenue: Long,
  val grossProfit: Long,
  val totalExpenses: Long,
  val operationalExpenses: Long = 0L,
  val kulakanExpenses: Long = 0L,
) {
  val netProfit: Long
    get() = grossProfit - operationalExpenses
}

data class StoreUiState(
  val settings: StoreSettings = StoreSettings(),
  val employees: List<EmployeeAccount> = emptyList(),
  val categories: List<CategoryItem> = emptyList(),
  val products: List<ProductEntity> = emptyList(),
  val transactions: List<TransactionEntity> = emptyList(),
  val expenses: List<ExpenseEntity> = emptyList(),
  val cloudSync: CloudSyncInfo = CloudSyncInfo(),
  val googleUserEmail: String = "",
  val googleDisplayName: String = "",
  val activeTab: AppTab = AppTab.POS,
  val posSearchQuery: String = "",
  val posSelectedCategory: String = ALL_CATEGORY_LABEL,
  val stockSearchQuery: String = "",
  val stockSelectedCategory: String = ALL_CATEGORY_LABEL,
  val cart: List<CartItem> = emptyList(),
  val customerType: CustomerType = CustomerType.PELANGGAN,
  val manualDiscountAmount: Long = 0L,
  val financeDateFilter: FinanceDateFilter = FinanceDateFilter(),
  val lastCompletedTransaction: TransactionEntity? = null,
  val loginError: String? = null,
  val statusMessage: String? = null,
) {
  val categoryNames: List<String>
    get() =
      if (categories.isNotEmpty() || settings.categoriesInitialized) {
        (categories.map { it.name.trim() } + products.map { it.category.trim() })
          .filter { it.isNotBlank() && !it.equals(ALL_CATEGORY_LABEL, ignoreCase = true) }
          .distinctBy { it.lowercase() }
      } else {
        DEFAULT_PRODUCT_CATEGORIES
      }
  val activeEmployee: EmployeeAccount?
    get() =
      if (settings.activeRole == UserRole.CASHIER) {
        settings.activeEmployeeId?.let { id -> employees.find { it.id == id } }
          ?: employees.firstOrNull()
      } else {
        null
      }

  val activeUserName: String
    get() =
      when (settings.activeRole) {
        UserRole.OWNER -> settings.ownerName
        UserRole.CASHIER -> {
          activeEmployee?.name?.takeIf { it.isNotBlank() }
            ?: settings.activeEmployeeName.ifBlank { settings.cashierName }
        }
        null -> ""
      }

  val lowStockCount: Int
    get() = products.count { it.stockQuantity < it.minStock }

  val totalItemBargainDiscount: Long
    get() = cart.sumOf { it.lineBargainDiscount }

  val cartStandardSubtotal: Long
    get() = cart.sumOf { it.standardSubtotal }

  val cartSubtotal: Long
    get() = cart.sumOf { it.subtotal }

  val cartDiscount: Long
    get() =
      if (customerType == CustomerType.PELANGGAN) {
        manualDiscountAmount.coerceIn(0L, cartSubtotal)
      } else {
        kotlin.math.round(cartSubtotal * customerType.discountRate).toLong()
      }

  val totalCombinedSavings: Long
    get() = totalItemBargainDiscount + cartDiscount

  val cartTotal: Long
    get() = (cartSubtotal - cartDiscount).coerceAtLeast(0L)

  private fun inflowsForTx(tx: TransactionEntity): List<CashInflowEvent> {
    val records = KasbonPaymentCodec.decode(tx.paymentsJson)
    if (records.isNotEmpty()) {
      return records
        .filter { it.amount > 0L }
        .map { rec ->
          CashInflowEvent(
            transactionId = tx.id,
            timestamp = if (rec.timestamp > 0L) rec.timestamp else (tx.paidAt ?: tx.timestamp),
            amount = rec.amount,
            paymentMethod = rec.paymentMethod,
          )
        }
    }
    return when {
      tx.isPaid ->
        listOf(
          CashInflowEvent(
            transactionId = tx.id,
            timestamp = tx.paidAt ?: tx.timestamp,
            amount = tx.totalAmount,
            paymentMethod = tx.paymentMethod,
          )
        )
      tx.amountPaid > 0L ->
        listOf(
          CashInflowEvent(
            transactionId = tx.id,
            timestamp = tx.paidAt ?: tx.timestamp,
            amount = tx.amountPaid.coerceAtMost(tx.totalAmount),
            paymentMethod = tx.paymentMethod,
          )
        )
      else -> emptyList()
    }
  }

  private fun isCashMethod(method: String): Boolean =
    method.contains("Tunai", ignoreCase = true)

  private fun isTransferMethod(method: String): Boolean =
    method.contains("Transfer", ignoreCase = true) || method.contains("QRIS", ignoreCase = true)

  private fun allCashInflows(): List<CashInflowEvent> =
    transactions.flatMap(::inflowsForTx)

  val totalInventoryCostValue: Long
    get() =
      products.sumOf { product ->
        val baseQty = product.stockQuantity.coerceAtLeast(0.0)
        val retailEquivBase =
          if (!product.retailUnitName.isNullOrBlank() && product.retailConversionFactor > 0.0) {
            product.retailStockQuantity.coerceAtLeast(0.0) * product.retailConversionFactor
          } else {
            0.0
          }
        val cost = product.costPricePerBaseUnit.coerceAtLeast(0L).toDouble()
        kotlin.math.round((baseQty + retailEquivBase) * cost).toLong()
      }

  val totalRevenuePaid: Long
    get() = allCashInflows().sumOf { it.amount }

  val totalExpenses: Long
    get() = expenses.sumOf { it.amount }

  val totalOperationalExpenses: Long
    get() = expenses.filter { it.category != ExpenseCategory.KULAKAN.label }.sumOf { it.amount }

  val totalGrossProfit: Long
    get() = transactions.filter { it.isPaid }.sumOf { it.totalAmount - it.totalCost }

  val totalNetProfit: Long
    get() = totalGrossProfit - totalOperationalExpenses

  val totalReceivables: Long
    get() = transactions.filter { !it.isPaid }.sumOf { (it.totalAmount - it.amountPaid).coerceAtLeast(0L) }

  val unpaidKasbonList: List<TransactionEntity>
    get() = transactions.filter { !it.isPaid }

  val todayStartMillis: Long
    get() = startOfDayMillis()

  val tomorrowStartMillis: Long
    get() = addDaysMillis(todayStartMillis, 1)

  val todayPaidRevenue: Long
    get() =
      allCashInflows()
        .filter { it.timestamp >= todayStartMillis && it.timestamp < tomorrowStartMillis }
        .sumOf { it.amount }

  val todayTransactionCount: Int
    get() = transactions.count { it.timestamp >= todayStartMillis && it.timestamp < tomorrowStartMillis }

  val todayGrossProfit: Long
    get() =
      transactions
        .filter {
          val eff = it.paidAt ?: it.timestamp
          it.isPaid && eff >= todayStartMillis && eff < tomorrowStartMillis
        }
        .sumOf { it.totalAmount - it.totalCost }

  val todayExpenses: Long
    get() = expenses.filter { it.timestamp >= todayStartMillis && it.timestamp < tomorrowStartMillis }.sumOf { it.amount }

  val savedCustomers: List<SavedCustomerOption>
    get() =
      transactions
        .filter {
          val trimmed = it.customerName.trim()
          trimmed.isNotBlank() &&
            !trimmed.equals("Pelanggan", ignoreCase = true) &&
            !trimmed.equals("Kontraktor", ignoreCase = true) &&
            !trimmed.startsWith("Pelanggan ", ignoreCase = true)
        }
        .sortedByDescending { it.timestamp }
        .distinctBy { it.customerName.trim().lowercase() }
        .take(10)
        .map {
          SavedCustomerOption(
            name = it.customerName.trim(),
            phone = it.customerPhone.trim(),
            customerType = CustomerType.fromStoredLabel(it.customerType),
          )
        }

  fun rangeForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): Pair<Long, Long>? {
    val today = startOfDayMillis(now)
    return when (filter.preset) {
      FinanceDatePreset.SEMUA -> null
      FinanceDatePreset.HARI_INI -> today to addDaysMillis(today, 1)
      FinanceDatePreset.BESOK -> {
        val tomorrow = addDaysMillis(today, 1)
        tomorrow to addDaysMillis(tomorrow, 1)
      }
      FinanceDatePreset.KEMARIN -> {
        val yesterday = addDaysMillis(today, -1)
        yesterday to today
      }
      FinanceDatePreset.MINGGU_INI -> startOfWeekMillis(now) to endOfWeekExclusiveMillis(now)
      FinanceDatePreset.BULAN_INI -> startOfMonthMillis(now) to endOfMonthExclusiveMillis(now)
      FinanceDatePreset.TAHUN_INI -> startOfYearMillis(now) to endOfYearExclusiveMillis(now)
      FinanceDatePreset.TANGGAL_PILIHAN -> {
        val day = startOfDayMillis(filter.customStartDayMillis ?: today)
        day to addDaysMillis(day, 1)
      }
      FinanceDatePreset.RENTANG_TANGGAL -> {
        val s = startOfDayMillis(filter.customStartDayMillis ?: today)
        val e = startOfDayMillis(filter.customEndDayMillis ?: s).coerceAtLeast(s)
        s to addDaysMillis(e, 1)
      }
    }
  }

  fun labelForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): String {
    filter.customLabel?.takeIf { it.isNotBlank() }?.let { return it }
    val today = startOfDayMillis(now)
    return when (filter.preset) {
      FinanceDatePreset.SEMUA -> "Semua Waktu"
      FinanceDatePreset.HARI_INI -> "Hari Ini (${formatShortDateOnly(today)})"
      FinanceDatePreset.BESOK -> "Besok (${formatShortDateOnly(addDaysMillis(today, 1))})"
      FinanceDatePreset.KEMARIN -> "Kemarin (${formatShortDateOnly(addDaysMillis(today, -1))})"
      FinanceDatePreset.MINGGU_INI -> {
        val ws = startOfWeekMillis(now)
        val we = addDaysMillis(ws, 6)
        "Minggu Ini (${formatShortDayMonth(ws)} - ${formatShortDateOnly(we)})"
      }
      FinanceDatePreset.BULAN_INI -> "Bulan Ini (${formatMonthYear(now)})"
      FinanceDatePreset.TAHUN_INI -> "Tahun Ini (${formatYearOnly(now)})"
      FinanceDatePreset.TANGGAL_PILIHAN -> {
        val day = startOfDayMillis(filter.customStartDayMillis ?: today)
        formatShortDateOnly(day)
      }
      FinanceDatePreset.RENTANG_TANGGAL -> {
        val s = startOfDayMillis(filter.customStartDayMillis ?: today)
        val e = startOfDayMillis(filter.customEndDayMillis ?: s).coerceAtLeast(s)
        if (s == e) formatShortDateOnly(s)
        else "${formatShortDateOnly(s)} - ${formatShortDateOnly(e)}"
      }
    }
  }

  fun financeRangeMillis(now: Long = System.currentTimeMillis()): Pair<Long, Long>? =
    rangeForFilter(financeDateFilter, now)

  fun financePeriodLabel(now: Long = System.currentTimeMillis()): String =
    labelForFilter(financeDateFilter, now)

  fun cashInflowsForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): List<CashInflowEvent> {
    val range = rangeForFilter(filter, now)
    val inflows = allCashInflows()
    return if (range == null) inflows
    else inflows.filter { it.timestamp >= range.first && it.timestamp < range.second }
  }

  fun paidTransactionsForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): List<TransactionEntity> {
    val range = rangeForFilter(filter, now)
    return transactions.filter { tx ->
      val eff = tx.paidAt ?: tx.timestamp
      tx.isPaid && (range == null || (eff >= range.first && eff < range.second))
    }
  }

  fun expensesForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): List<ExpenseEntity> {
    val range = rangeForFilter(filter, now)
    return if (range == null) expenses
    else expenses.filter { it.timestamp >= range.first && it.timestamp < range.second }
  }

  fun unpaidKasbonForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): List<TransactionEntity> {
    val range = rangeForFilter(filter, now)
    return if (range == null) unpaidKasbonList
    else unpaidKasbonList.filter { it.timestamp >= range.first && it.timestamp < range.second }
  }

  fun transactionCountForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): Int {
    val range = rangeForFilter(filter, now)
    return if (range == null) transactions.size
    else transactions.count { it.timestamp >= range.first && it.timestamp < range.second }
  }

  fun dailyIncomeHistoryForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): List<DailyIncomeSummary> {
    val range = rangeForFilter(filter, now) ?: return dailyIncomeHistory
    val inRange =
      dailyIncomeHistory.filter { it.dayStartMillis >= range.first && it.dayStartMillis < range.second }
    if (inRange.isNotEmpty()) return inRange
    val isSingleDay =
      filter.preset in
        setOf(
          FinanceDatePreset.HARI_INI,
          FinanceDatePreset.BESOK,
          FinanceDatePreset.KEMARIN,
          FinanceDatePreset.TANGGAL_PILIHAN,
        )
    return if (isSingleDay) {
      listOf(
        DailyIncomeSummary(
          dayStartMillis = range.first,
          dayLabel = formatDayLabel(range.first),
          transactionCount = 0,
          paidRevenue = 0L,
          cashRevenue = 0L,
          transferRevenue = 0L,
          kasbonRevenue = 0L,
          grossProfit = 0L,
          totalExpenses = 0L,
          operationalExpenses = 0L,
          kulakanExpenses = 0L,
        )
      )
    } else {
      emptyList()
    }
  }

  fun buildReportDataForFilter(
    filter: FinanceDateFilter,
    now: Long = System.currentTimeMillis(),
  ): FinanceReportData {
    val paidTx = paidTransactionsForFilter(filter, now)
    val inflows = cashInflowsForFilter(filter, now)
    val exps = expensesForFilter(filter, now)
    val kasbons = unpaidKasbonForFilter(filter, now)

    val paidRevenue = inflows.sumOf { it.amount }
    val cashRevenue = inflows.filter { isCashMethod(it.paymentMethod) }.sumOf { it.amount }
    val transferRevenue = inflows.filter { isTransferMethod(it.paymentMethod) }.sumOf { it.amount }
    val paidOrPartialCount =
      (paidTx.map { it.id } + inflows.map { it.transactionId }).distinct().size

    val grossProfit = paidTx.sumOf { it.totalAmount - it.totalCost }
    val totalExpenses = exps.sumOf { it.amount }
    val operationalExpenses =
      exps.filter { it.category != ExpenseCategory.KULAKAN.label }.sumOf { it.amount }
    val kulakanExpenses =
      exps.filter { it.category == ExpenseCategory.KULAKAN.label }.sumOf { it.amount }
    val netProfit = grossProfit - operationalExpenses
    val receivables = kasbons.sumOf { (it.totalAmount - it.amountPaid).coerceAtLeast(0L) }

    val reportRole = settings.activeRole ?: UserRole.OWNER
    val defaultSignerName = if (reportRole == UserRole.CASHIER) "Kasir" else "Pemilik"

    return FinanceReportData(
      storeSettings = settings,
      periodLabel = labelForFilter(filter, now),
      generatedBy = activeUserName.ifBlank { defaultSignerName },
      generatedByRole = reportRole,
      generatedAtMillis = now,
      paidRevenue = paidRevenue,
      cashRevenue = cashRevenue,
      transferRevenue = transferRevenue,
      paidCount = paidOrPartialCount,
      totalExpenses = totalExpenses,
      operationalExpenses = operationalExpenses,
      kulakanExpenses = kulakanExpenses,
      expenseCount = exps.size,
      grossProfit = grossProfit,
      netProfit = netProfit,
      inventoryCostValue = totalInventoryCostValue,
      unpaidReceivables = receivables,
      unpaidKasbonCount = kasbons.size,
      totalTxCount = transactionCountForFilter(filter, now),
      dailySummaries = dailyIncomeHistoryForFilter(filter, now),
      expenses = exps,
      unpaidKasbonList = kasbons,
      paidTransactions = paidTx,
    )
  }

  fun filteredCashInflows(now: Long = System.currentTimeMillis()): List<CashInflowEvent> =
    cashInflowsForFilter(financeDateFilter, now)

  fun filteredPaidTransactions(now: Long = System.currentTimeMillis()): List<TransactionEntity> =
    paidTransactionsForFilter(financeDateFilter, now)

  fun filteredRevenuePaid(now: Long = System.currentTimeMillis()): Long =
    filteredCashInflows(now).sumOf { it.amount }

  fun filteredCashRevenue(now: Long = System.currentTimeMillis()): Long =
    filteredCashInflows(now).filter { isCashMethod(it.paymentMethod) }.sumOf { it.amount }

  fun filteredTransferRevenue(now: Long = System.currentTimeMillis()): Long =
    filteredCashInflows(now).filter { isTransferMethod(it.paymentMethod) }.sumOf { it.amount }

  fun filteredGrossProfit(now: Long = System.currentTimeMillis()): Long =
    filteredPaidTransactions(now).sumOf { it.totalAmount - it.totalCost }

  fun filteredExpenses(now: Long = System.currentTimeMillis()): List<ExpenseEntity> =
    expensesForFilter(financeDateFilter, now)

  fun filteredTotalExpenses(now: Long = System.currentTimeMillis()): Long =
    filteredExpenses(now).sumOf { it.amount }

  fun filteredOperationalExpenses(now: Long = System.currentTimeMillis()): Long =
    filteredExpenses(now).filter { it.category != ExpenseCategory.KULAKAN.label }.sumOf { it.amount }

  fun filteredKulakanExpenses(now: Long = System.currentTimeMillis()): Long =
    filteredExpenses(now).filter { it.category == ExpenseCategory.KULAKAN.label }.sumOf { it.amount }

  fun filteredNetProfit(now: Long = System.currentTimeMillis()): Long =
    filteredGrossProfit(now) - filteredOperationalExpenses(now)

  fun filteredUnpaidKasbon(now: Long = System.currentTimeMillis()): List<TransactionEntity> =
    unpaidKasbonForFilter(financeDateFilter, now)

  fun filteredReceivables(now: Long = System.currentTimeMillis()): Long =
    filteredUnpaidKasbon(now).sumOf { (it.totalAmount - it.amountPaid).coerceAtLeast(0L) }

  fun filteredTransactionCount(now: Long = System.currentTimeMillis()): Int =
    transactionCountForFilter(financeDateFilter, now)

  fun filteredCustomerTransactions(now: Long = System.currentTimeMillis()): List<TransactionEntity> {
    val range = financeRangeMillis(now) ?: return transactions
    return transactions.filter { tx ->
      val createdIn = tx.timestamp >= range.first && tx.timestamp < range.second
      val paidIn = tx.isPaid && tx.paidAt != null && tx.paidAt >= range.first && tx.paidAt < range.second
      val inflowIn = inflowsForTx(tx).any { it.timestamp >= range.first && it.timestamp < range.second }
      createdIn || paidIn || inflowIn
    }
  }

  fun filteredDailyIncomeHistory(now: Long = System.currentTimeMillis()): List<DailyIncomeSummary> =
    dailyIncomeHistoryForFilter(financeDateFilter, now)

  val dailyIncomeHistory: List<DailyIncomeSummary>
    get() {
      val createdByDay = transactions.groupBy { startOfDayMillis(it.timestamp) }
      val settledByDay =
        transactions
          .filter { it.isPaid }
          .groupBy { startOfDayMillis(it.paidAt ?: it.timestamp) }
      val inflowsByDay = allCashInflows().groupBy { startOfDayMillis(it.timestamp) }
      val expByDay = expenses.groupBy { startOfDayMillis(it.timestamp) }
      val allDays =
        (createdByDay.keys + settledByDay.keys + inflowsByDay.keys + expByDay.keys + todayStartMillis)
          .distinct()
          .sortedDescending()
      return allDays.map { day ->
        val createdTx = createdByDay[day].orEmpty()
        val settledTx = settledByDay[day].orEmpty()
        val dayInflows = inflowsByDay[day].orEmpty()
        val unpaidCreated = createdTx.filter { !it.isPaid }
        val dayExp = expByDay[day].orEmpty()
        val opExp = dayExp.filter { it.category != ExpenseCategory.KULAKAN.label }.sumOf { it.amount }
        val kulakanExp = dayExp.filter { it.category == ExpenseCategory.KULAKAN.label }.sumOf { it.amount }
        DailyIncomeSummary(
          dayStartMillis = day,
          dayLabel = formatDayLabel(day),
          transactionCount = createdTx.size,
          paidRevenue = dayInflows.sumOf { it.amount },
          cashRevenue = dayInflows.filter { isCashMethod(it.paymentMethod) }.sumOf { it.amount },
          transferRevenue = dayInflows.filter { isTransferMethod(it.paymentMethod) }.sumOf { it.amount },
          kasbonRevenue = unpaidCreated.sumOf { (it.totalAmount - it.amountPaid).coerceAtLeast(0L) },
          grossProfit = settledTx.sumOf { it.totalAmount - it.totalCost },
          totalExpenses = dayExp.sumOf { it.amount },
          operationalExpenses = opExp,
          kulakanExpenses = kulakanExp,
        )
      }
    }
}

class StoreViewModel(
  application: Application,
  authUid: String,
  private val displayName: String,
  private val userEmail: String,
) : AndroidViewModel(application) {
  private val repository: StoreRepository =
    StoreRepository(
      context = application,
      authUid = authUid,
      displayName = displayName,
      prefs = SettingsPreferences(application),
    )

  private data class TransientUiState(
    val activeTab: AppTab = AppTab.POS,
    val posSearchQuery: String = "",
    val posSelectedCategory: String = ALL_CATEGORY_LABEL,
    val stockSearchQuery: String = "",
    val stockSelectedCategory: String = ALL_CATEGORY_LABEL,
    val cart: List<CartItem> = emptyList(),
    val customerType: CustomerType = CustomerType.PELANGGAN,
    val manualDiscountAmount: Long = 0L,
    val financeDateFilter: FinanceDateFilter = FinanceDateFilter(),
    val lastCompletedTransaction: TransactionEntity? = null,
    val loginError: String? = null,
    val statusMessage: String? = null,
  )

  private val transientState = MutableStateFlow(TransientUiState())

  private val syncedDataFlow =
    combine(
      combine(repository.settingsFlow, repository.employeesFlow, repository.categoriesFlow) {
        settings,
        employees,
        categories ->
        Triple(settings, employees, categories)
      },
      repository.productsFlow,
      repository.transactionsFlow,
      repository.expensesFlow,
      repository.cloudSyncState,
    ) { (settings, employees, categories), products, transactions, expenses, cloudSync ->
      SyncedSnapshot(settings, employees, categories, products, transactions, expenses, cloudSync)
    }

  private data class SyncedSnapshot(
    val settings: StoreSettings,
    val employees: List<EmployeeAccount>,
    val categories: List<CategoryItem>,
    val products: List<ProductEntity>,
    val transactions: List<TransactionEntity>,
    val expenses: List<ExpenseEntity>,
    val cloudSync: CloudSyncInfo,
  )

  val uiState: StateFlow<StoreUiState> =
    combine(syncedDataFlow, transientState) { synced, transient ->
        val sanitizedTab = transient.activeTab
        val availableNames =
          if (synced.categories.isNotEmpty() || synced.settings.categoriesInitialized) {
            (synced.categories.map { it.name.trim() } + synced.products.map { it.category.trim() })
              .filter { it.isNotBlank() && !it.equals(ALL_CATEGORY_LABEL, ignoreCase = true) }
              .distinctBy { it.lowercase() }
          } else {
            DEFAULT_PRODUCT_CATEGORIES
          }
        val validPosCategory =
          if (transient.posSelectedCategory == ALL_CATEGORY_LABEL ||
            availableNames.any { it.equals(transient.posSelectedCategory, ignoreCase = true) }
          ) {
            transient.posSelectedCategory
          } else {
            ALL_CATEGORY_LABEL
          }
        val validStockCategory =
          if (transient.stockSelectedCategory == ALL_CATEGORY_LABEL ||
            availableNames.any { it.equals(transient.stockSelectedCategory, ignoreCase = true) }
          ) {
            transient.stockSelectedCategory
          } else {
            ALL_CATEGORY_LABEL
          }
        StoreUiState(
          settings = synced.settings,
          employees = synced.employees,
          categories = synced.categories,
          products = synced.products,
          transactions = synced.transactions,
          expenses = synced.expenses,
          cloudSync = synced.cloudSync,
          googleUserEmail = userEmail,
          googleDisplayName = displayName,
          activeTab = sanitizedTab,
          posSearchQuery = transient.posSearchQuery,
          posSelectedCategory = validPosCategory,
          stockSearchQuery = transient.stockSearchQuery,
          stockSelectedCategory = validStockCategory,
          cart = transient.cart,
          customerType = transient.customerType,
          manualDiscountAmount = transient.manualDiscountAmount,
          financeDateFilter = transient.financeDateFilter,
          lastCompletedTransaction = transient.lastCompletedTransaction,
          loginError = transient.loginError,
          statusMessage = transient.statusMessage,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StoreUiState(googleUserEmail = userEmail, googleDisplayName = displayName),
      )

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfNeeded().onFailure { e ->
        transientState.update {
          it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menyinkronkan data toko."))
        }
      }
    }
    // Automatically propagate memberUids to subcollection docs on owner's device when a new device joins
    viewModelScope.launch {
      repository.cloudSyncState
        .map { it.memberCount to it.isOwnerOfWorkspace }
        .distinctUntilChanged()
        .collect { (memberCount, isOwner) ->
          if (isOwner && memberCount > 1) {
            repository.syncMembersIfOwner()
          }
        }
    }
  }

  fun retryCloudSync() {
    viewModelScope.launch {
      repository.seedInitialDataIfNeeded().onFailure { e ->
        transientState.update {
          it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menghubungkan ke cloud."))
        }
      }
    }
  }

  fun joinStoreByCode(joinCode: String) {
    viewModelScope.launch {
      repository
        .joinStoreByCode(joinCode)
        .onSuccess {
          transientState.update {
            it.copy(
              cart = emptyList(),
              manualDiscountAmount = 0L,
              activeTab = AppTab.POS,
              statusMessage = "Berhasil terhubung ke toko! Silakan masuk dengan username & password.",
            )
          }
        }
        .onFailure { e ->
          val msg =
            if (e is IllegalArgumentException || e is IllegalStateException) {
              e.message ?: "Gagal bergabung ke toko."
            } else {
              userFriendlyFirestoreMessage(e, "Gagal bergabung dengan Kode Toko.")
            }
          transientState.update { it.copy(statusMessage = msg) }
        }
    }
  }

  fun switchToOwnStore() {
    viewModelScope.launch {
      repository
        .switchToOwnStore()
        .onSuccess {
          transientState.update {
            it.copy(
              cart = emptyList(),
              manualDiscountAmount = 0L,
              activeTab = AppTab.POS,
              statusMessage = "Kembali ke toko pribadi Anda.",
            )
          }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal beralih ke toko pribadi."))
          }
        }
    }
  }

  fun createNewStore(
    ownerName: String,
    ownerUsername: String,
    ownerPassword: String,
    storeAddress: String,
    storePhone: String,
    includeSampleProducts: Boolean = false,
  ) {
    viewModelScope.launch {
      repository
        .createNewStore(
          ownerName = ownerName,
          ownerUsername = ownerUsername,
          ownerPassword = ownerPassword,
          storeAddress = storeAddress,
          storePhone = storePhone,
          includeSampleProducts = includeSampleProducts,
        )
        .onSuccess { joinCode ->
          transientState.update {
            it.copy(
              cart = emptyList(),
              manualDiscountAmount = 0L,
              activeTab = AppTab.POS,
              loginError = null,
              statusMessage = "Toko TB PUTRA RI dibuka! Kode Toko: $joinCode",
            )
          }
        }
        .onFailure { e ->
          val msg =
            if (e is IllegalArgumentException || e is IllegalStateException) {
              e.message ?: "Gagal membuka toko."
            } else {
              userFriendlyFirestoreMessage(e, "Gagal membuka toko.")
            }
          transientState.update { it.copy(statusMessage = msg) }
        }
    }
  }

  fun attemptLogin(username: String, password: String) {
    val cleanUsername = username.trim()
    if (cleanUsername.isBlank() || password.isBlank()) {
      transientState.update {
        it.copy(loginError = "Username dan password wajib diisi.")
      }
      return
    }

    val snapshot = uiState.value
    val settings = snapshot.settings
    val matchesOwner =
      cleanUsername.equals(settings.ownerUsername, ignoreCase = true) &&
        password == settings.ownerPassword

    if (matchesOwner) {
      transientState.update {
        it.copy(activeTab = AppTab.POS, loginError = null, statusMessage = null)
      }
      viewModelScope.launch { repository.setActiveSession(UserRole.OWNER) }
      return
    }

    val matchedEmployee =
      snapshot.employees.find { emp ->
        cleanUsername.equals(emp.username, ignoreCase = true) && password == emp.password
      }
    if (matchedEmployee != null) {
      transientState.update {
        it.copy(activeTab = AppTab.POS, loginError = null, statusMessage = null)
      }
      viewModelScope.launch {
        repository.setActiveSession(
          role = UserRole.CASHIER,
          employeeId = matchedEmployee.id,
          employeeName = matchedEmployee.name,
        )
      }
      return
    }

    // Fallback before cloud employee subcollection completes initial migration
    val legacyCashierMatches =
      !settings.employeesInitialized &&
        snapshot.employees.isEmpty() &&
        cleanUsername.equals(settings.cashierUsername, ignoreCase = true) &&
        password == settings.cashierPassword

    if (legacyCashierMatches) {
      transientState.update {
        it.copy(activeTab = AppTab.POS, loginError = null, statusMessage = null)
      }
      viewModelScope.launch {
        repository.setActiveSession(
          role = UserRole.CASHIER,
          employeeId = "emp_default",
          employeeName = settings.cashierName,
        )
      }
    } else {
      transientState.update {
        it.copy(loginError = "Username atau password salah. Coba periksa kembali.")
      }
    }
  }

  fun clearLoginError() {
    transientState.update { it.copy(loginError = null) }
  }

  fun logout() {
    transientState.update {
      it.copy(
        activeTab = AppTab.POS,
        cart = emptyList(),
        manualDiscountAmount = 0L,
        lastCompletedTransaction = null,
        loginError = null,
      )
    }
    viewModelScope.launch { repository.setActiveSession(null) }
  }

  fun selectTab(tab: AppTab) {
    transientState.update { it.copy(activeTab = tab) }
  }

  fun updatePosSearch(query: String) {
    transientState.update { it.copy(posSearchQuery = query) }
  }

  fun selectPosCategory(category: String) {
    transientState.update { it.copy(posSelectedCategory = category) }
  }

  fun updateStockSearch(query: String) {
    transientState.update { it.copy(stockSearchQuery = query) }
  }

  fun selectStockCategory(category: String) {
    transientState.update { it.copy(stockSelectedCategory = category) }
  }

  fun selectCustomerType(type: CustomerType) {
    transientState.update { current ->
      current.copy(
        customerType = type,
        manualDiscountAmount = if (type == CustomerType.PELANGGAN) current.manualDiscountAmount else 0L,
      )
    }
  }

  fun updateManualDiscount(amount: Long) {
    transientState.update { it.copy(manualDiscountAmount = amount.coerceAtLeast(0L)) }
  }

  fun selectFinanceDateFilter(filter: FinanceDateFilter) {
    transientState.update { it.copy(financeDateFilter = filter) }
  }

  fun addToCart(product: ProductEntity, useRetailUnit: Boolean, deltaQty: Double = 1.0) {
    val hasValidRetail = useRetailUnit && !product.retailUnitName.isNullOrBlank()
    val unitName = if (hasValidRetail) product.retailUnitName!! else product.baseUnit
    val conversionFactor = if (hasValidRetail) product.retailConversionFactor else 1.0
    val standardPrice =
      (if (hasValidRetail) product.retailSellPrice else product.sellPricePerBaseUnit).coerceAtLeast(0L)
    val minPrice =
      (if (hasValidRetail) product.effectiveMinRetailSellPrice() else product.effectiveMinBaseSellPrice())
        .coerceIn(0L, standardPrice)
    val availablePoolStock =
      if (hasValidRetail) product.retailStockQuantity else product.stockQuantity

    transientState.update { current ->
      val existingIdx =
        current.cart.indexOfFirst {
          it.productId == product.id && it.isRetailUnit == hasValidRetail
        }
      val currentQtyInSamePool = if (existingIdx >= 0) current.cart[existingIdx].quantity else 0.0
      val nextQty = currentQtyInSamePool + deltaQty

      if (nextQty > availablePoolStock + 1e-6) {
        val msg =
          if (hasValidRetail && product.stockQuantity >= 1.0 - 1e-6) {
            "Stok eceran ${product.retailUnitName} habis/kurang. Gunakan 'Ecer Stok' di menu Stok untuk memecah ${product.baseUnit}."
          } else if (hasValidRetail) {
            "Stok eceran ${product.name} tidak mencukupi!"
          } else {
            "Stok ${product.baseUnit} ${product.name} tidak mencukupi!"
          }
        return@update current.copy(statusMessage = msg)
      }

      val updatedCart = current.cart.toMutableList()
      if (existingIdx >= 0) {
        val existingItem = updatedCart[existingIdx]
        updatedCart[existingIdx] =
          existingItem.copy(
            quantity = nextQty,
            unitPrice = existingItem.unitPrice.coerceIn(minPrice, standardPrice),
            standardUnitPrice = standardPrice,
            minUnitPrice = minPrice,
            availableBaseStock = product.stockQuantity,
            availableRetailStock = product.retailStockQuantity,
          )
      } else {
        updatedCart.add(
          CartItem(
            productId = product.id,
            productName = product.name,
            category = product.category,
            unitName = unitName,
            isRetailUnit = hasValidRetail,
            conversionFactor = conversionFactor,
            quantity = deltaQty,
            unitPrice = standardPrice,
            unitCostPerBase = product.costPricePerBaseUnit,
            availableBaseStock = product.stockQuantity,
            availableRetailStock = product.retailStockQuantity,
            standardUnitPrice = standardPrice,
            minUnitPrice = minPrice,
          )
        )
      }
      current.copy(cart = updatedCart)
    }
  }

  fun updateCartItemUnitPrice(productId: Long, isRetailUnit: Boolean, negotiatedUnitPrice: Long) {
    transientState.update { current ->
      val target =
        current.cart.find { it.productId == productId && it.isRetailUnit == isRetailUnit }
          ?: return@update current
      val minAllowed = target.effectiveMinUnitPrice
      val maxAllowed = target.standardUnitPrice
      if (negotiatedUnitPrice < minAllowed) {
        return@update current.copy(
          statusMessage = "Harga tawar minimal untuk ${target.productName} (${target.unitName}) adalah Rp $minAllowed"
        )
      }
      val clampedPrice = negotiatedUnitPrice.coerceIn(minAllowed, maxAllowed)
      val updatedCart =
        current.cart.map { item ->
          if (item.productId == productId && item.isRetailUnit == isRetailUnit) {
            item.copy(unitPrice = clampedPrice)
          } else {
            item
          }
        }
      val nextSubtotal = updatedCart.sumOf { it.subtotal }
      current.copy(
        cart = updatedCart,
        manualDiscountAmount = current.manualDiscountAmount.coerceIn(0L, nextSubtotal),
      )
    }
  }

  fun updateCartItemQuantity(productId: Long, isRetailUnit: Boolean, newQuantity: Double) {
    if (newQuantity <= 0.0) {
      removeCartItem(productId, isRetailUnit)
      return
    }
    val product = uiState.value.products.find { it.id == productId } ?: return
    val availablePoolStock =
      if (isRetailUnit) product.retailStockQuantity else product.stockQuantity

    transientState.update { current ->
      val updated =
        current.cart.map { item ->
          if (item.productId == productId && item.isRetailUnit == isRetailUnit) {
            if (newQuantity > availablePoolStock + 1e-6) {
              val unitLabel =
                if (isRetailUnit) "eceran ${product.retailUnitName ?: ""}".trim()
                else product.baseUnit
              return@update current.copy(
                statusMessage = "Melebihi sisa stok $unitLabel ${product.name}"
              )
            }
            item.copy(
              quantity = newQuantity,
              availableBaseStock = product.stockQuantity,
              availableRetailStock = product.retailStockQuantity,
            )
          } else {
            item
          }
        }
      val nextSubtotal = updated.sumOf { it.subtotal }
      current.copy(
        cart = updated,
        manualDiscountAmount = current.manualDiscountAmount.coerceIn(0L, nextSubtotal),
      )
    }
  }

  fun removeCartItem(productId: Long, isRetailUnit: Boolean) {
    transientState.update { current ->
      val nextCart =
        current.cart.filterNot { it.productId == productId && it.isRetailUnit == isRetailUnit }
      val nextSubtotal = nextCart.sumOf { it.subtotal }
      current.copy(
        cart = nextCart,
        manualDiscountAmount =
          if (nextCart.isEmpty()) 0L else current.manualDiscountAmount.coerceIn(0L, nextSubtotal),
      )
    }
  }

  fun clearCart() {
    transientState.update { it.copy(cart = emptyList(), manualDiscountAmount = 0L) }
  }

  fun repackToRetail(product: ProductEntity, baseQtyToConvert: Double) {
    if (uiState.value.settings.activeRole == null) return
    val baseInCart =
      uiState.value.cart.filter { it.productId == product.id && !it.isRetailUnit }.sumOf {
        it.quantity
      }
    if (baseQtyToConvert <= 0.0) {
      transientState.update { it.copy(statusMessage = "Jumlah yang diecer harus lebih dari 0.") }
      return
    }
    if (baseQtyToConvert > (product.stockQuantity - baseInCart) + 1e-6) {
      val msg =
        if (baseInCart > 0.0) {
          "Sebagian ${product.baseUnit} sedang ada di keranjang. Kurangi keranjang terlebih dahulu."
        } else {
          "Stok ${product.baseUnit} ${product.name} tidak mencukupi untuk diecer."
        }
      transientState.update { it.copy(statusMessage = msg) }
      return
    }

    viewModelScope.launch {
      repository
        .repackToRetail(product = product, baseQtyToConvert = baseQtyToConvert)
        .onSuccess { addedRetail ->
          val retailLabel = product.retailUnitName ?: "eceran"
          transientState.update {
            it.copy(
              statusMessage =
                "${formatQty(baseQtyToConvert)} ${product.baseUnit} ${product.name} dipecah menjadi +${formatQty(addedRetail)} $retailLabel"
            )
          }
        }
        .onFailure { e ->
          val msg =
            if (e is IllegalArgumentException) {
              e.message ?: "Gagal memecah stok ke eceran."
            } else {
              userFriendlyFirestoreMessage(e, "Gagal memecah stok ke eceran.")
            }
          transientState.update { it.copy(statusMessage = msg) }
        }
    }
  }

  fun processCheckout(
    customerName: String,
    customerPhone: String,
    paymentMethod: PaymentMethod,
    cashReceived: Long,
    dpPaymentMethod: PaymentMethod = PaymentMethod.TUNAI,
  ) {
    val snapshot = uiState.value
    val role = snapshot.settings.activeRole ?: return
    if (snapshot.cart.isEmpty()) return
    val productsById = snapshot.products.associateBy { it.id }

    viewModelScope.launch {
      repository
        .checkoutCart(
          cart = snapshot.cart,
          currentProductsById = productsById,
          customerType = snapshot.customerType,
          manualDiscount =
            if (snapshot.customerType == CustomerType.PELANGGAN) snapshot.manualDiscountAmount
            else 0L,
          customerName = customerName,
          customerPhone = customerPhone,
          paymentMethod = paymentMethod,
          cashReceived = cashReceived,
          dpPaymentMethod = dpPaymentMethod,
          cashierName = snapshot.activeUserName,
          cashierRole = role,
        )
        .onSuccess { tx ->
          transientState.update {
            it.copy(
              cart = emptyList(),
              customerType = CustomerType.PELANGGAN,
              manualDiscountAmount = 0L,
              lastCompletedTransaction = tx,
              statusMessage = "Transaksi ${tx.invoiceNumber} berhasil disimpan & tersinkronisasi",
            )
          }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal memproses transaksi."))
          }
        }
    }
  }

  fun openReceipt(tx: TransactionEntity) {
    transientState.update { it.copy(lastCompletedTransaction = tx) }
  }

  fun dismissReceipt() {
    transientState.update { it.copy(lastCompletedTransaction = null) }
  }

  fun saveProduct(product: ProductEntity) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .saveProduct(product)
        .onSuccess {
          transientState.update { it.copy(statusMessage = "Barang '${product.name}' disimpan") }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menyimpan barang."))
          }
        }
    }
  }

  fun deleteProduct(id: Long) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .deleteProduct(id)
        .onSuccess {
          transientState.update { it.copy(statusMessage = "Barang dihapus dari daftar stok") }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menghapus barang."))
          }
        }
    }
  }

  fun saveCategory(existing: CategoryItem?, name: String) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    val cleanName = name.trim().take(60)
    if (cleanName.isBlank()) return
    if (cleanName.equals(ALL_CATEGORY_LABEL, ignoreCase = true)) {
      transientState.update {
        it.copy(statusMessage = "Nama '$ALL_CATEGORY_LABEL' dicadangkan untuk filter semua barang.")
      }
      return
    }
    val duplicate =
      uiState.value.categories.any {
        it.id != existing?.id && it.name.equals(cleanName, ignoreCase = true)
      }
    if (duplicate) {
      transientState.update {
        it.copy(statusMessage = "Kategori '$cleanName' sudah ada.")
      }
      return
    }
    viewModelScope.launch {
      repository
        .saveCategory(existing = existing, name = cleanName)
        .onSuccess {
          transientState.update { current ->
            val nextPosCat =
              if (existing != null && current.posSelectedCategory.equals(existing.name, ignoreCase = true)) {
                cleanName
              } else {
                current.posSelectedCategory
              }
            val nextStockCat =
              if (existing != null && current.stockSelectedCategory.equals(existing.name, ignoreCase = true)) {
                cleanName
              } else {
                current.stockSelectedCategory
              }
            current.copy(
              posSelectedCategory = nextPosCat,
              stockSelectedCategory = nextStockCat,
              statusMessage =
                if (existing == null) "Kategori '$cleanName' ditambahkan"
                else "Kategori diubah menjadi '$cleanName'",
            )
          }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menyimpan kategori."))
          }
        }
    }
  }

  fun deleteCategory(category: CategoryItem, reassignTo: String?) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .deleteCategory(category = category, reassignTo = reassignTo)
        .onSuccess {
          transientState.update { current ->
            current.copy(
              posSelectedCategory =
                if (current.posSelectedCategory.equals(category.name, ignoreCase = true)) {
                  ALL_CATEGORY_LABEL
                } else {
                  current.posSelectedCategory
                },
              stockSelectedCategory =
                if (current.stockSelectedCategory.equals(category.name, ignoreCase = true)) {
                  ALL_CATEGORY_LABEL
                } else {
                  current.stockSelectedCategory
                },
              statusMessage = "Kategori '${category.name}' dihapus",
            )
          }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menghapus kategori."))
          }
        }
    }
  }

  fun restockProduct(
    product: ProductEntity,
    addedBaseQty: Double,
    totalPurchaseCost: Long,
    deductFromCash: Boolean,
    updateCostPrice: Boolean = false,
  ) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .restockProduct(
          product = product,
          addedBaseQty = addedBaseQty,
          totalPurchaseCost = totalPurchaseCost,
          deductFromCash = deductFromCash,
          updateCostPrice = updateCostPrice,
          recordedBy = uiState.value.activeUserName,
        )
        .onSuccess {
          val msg =
            if (updateCostPrice && totalPurchaseCost > 0L) {
              "Restok ${product.name} (+${addedBaseQty} ${product.baseUnit}) & modal diperbarui"
            } else {
              "Restok ${product.name} (+${addedBaseQty} ${product.baseUnit}) berhasil"
            }
          transientState.update { it.copy(statusMessage = msg) }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal restok barang."))
          }
        }
    }
  }

  fun payKasbon(txId: Long, payAmount: Long, paymentMethod: PaymentMethod) {
    val snapshot = uiState.value
    if (snapshot.settings.activeRole == null) return
    val tx = snapshot.transactions.find { it.id == txId } ?: return
    val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
    if (remaining <= 0L && tx.isPaid) return
    val willFullySettle = payAmount >= remaining

    viewModelScope.launch {
      repository
        .payKasbon(
          tx = tx,
          payAmount = payAmount,
          paymentMethod = paymentMethod,
          recordedBy = snapshot.activeUserName,
        )
        .onSuccess { updatedTx ->
          val msg =
            if (willFullySettle) "Kasbon berhasil dilunasi (${paymentMethod.label}) & masuk kas"
            else "Cicilan kasbon (${paymentMethod.label}) berhasil dicatat"
          transientState.update {
            it.copy(
              lastCompletedTransaction = updatedTx,
              statusMessage = msg,
            )
          }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal mencatat pembayaran kasbon."))
          }
        }
    }
  }

  fun markKasbonPaid(txId: Long) {
    val tx = uiState.value.transactions.find { it.id == txId } ?: return
    val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
    payKasbon(txId = txId, payAmount = remaining, paymentMethod = PaymentMethod.TUNAI)
  }

  fun deleteTransaction(txId: Long, restoreStock: Boolean = true) {
    val snapshot = uiState.value
    if (snapshot.settings.activeRole != UserRole.OWNER) return
    val tx = snapshot.transactions.find { it.id == txId } ?: return
    val productsById = snapshot.products.associateBy { it.id }
    viewModelScope.launch {
      repository
        .deleteTransaction(
          tx = tx,
          currentProductsById = productsById,
          restoreStock = restoreStock,
        )
        .onSuccess {
          val msg =
            if (restoreStock) "Transaksi dihapus & stok barang dikembalikan ke gudang"
            else "Riwayat transaksi dihapus"
          transientState.update { it.copy(statusMessage = msg) }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menghapus transaksi."))
          }
        }
    }
  }

  fun addExpense(
    title: String,
    category: ExpenseCategory,
    amount: Long,
    notes: String,
    timestamp: Long = System.currentTimeMillis(),
  ) {
    val snapshot = uiState.value
    val role = snapshot.settings.activeRole ?: return
    viewModelScope.launch {
      repository
        .addExpense(
          title = title,
          category = category,
          amount = amount,
          notes = notes,
          recordedBy = snapshot.activeUserName,
          recordedRole = role,
          timestamp = timestamp,
        )
        .onSuccess {
          transientState.update { it.copy(statusMessage = "Pengeluaran '$title' dicatat") }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal mencatat pengeluaran."))
          }
        }
    }
  }

  fun deleteExpense(expenseId: Long) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .deleteExpense(expenseId)
        .onSuccess {
          transientState.update { it.copy(statusMessage = "Catatan pengeluaran dihapus") }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menghapus pengeluaran."))
          }
        }
    }
  }

  fun updateOwnerAccountSettings(
    ownerUsername: String,
    ownerPassword: String,
    ownerName: String,
    storeAddress: String,
    storePhone: String,
  ) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .updateOwnerAccountSettings(
          ownerUsername = ownerUsername,
          ownerPassword = ownerPassword,
          ownerName = ownerName,
          storeAddress = storeAddress,
          storePhone = storePhone,
        )
        .onSuccess {
          transientState.update { it.copy(statusMessage = "Pengaturan pemilik & toko diperbarui") }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal memperbarui pengaturan."))
          }
        }
    }
  }

  fun saveThermalPrintPreferences(
    paperSize: ReceiptPaperSize,
    printerAddress: String? = null,
    printerName: String? = null,
  ) {
    viewModelScope.launch {
      repository.saveThermalPrintPreferences(
        paperSize = paperSize,
        printerAddress = printerAddress,
        printerName = printerName,
      )
    }
  }

  fun saveEmployee(
    existingId: String?,
    name: String,
    username: String,
    password: String,
    phone: String = "",
  ) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .saveEmployee(
          existingId = existingId,
          name = name,
          username = username,
          password = password,
          phone = phone,
        )
        .onSuccess {
          val msg =
            if (existingId.isNullOrBlank()) "Karyawan '$name' berhasil ditambahkan"
            else "Data karyawan '$name' diperbarui"
          transientState.update { it.copy(statusMessage = msg) }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menyimpan data karyawan."))
          }
        }
    }
  }

  fun updateOwnCashierAccount(
    name: String,
    username: String,
    password: String,
    phone: String = "",
  ) {
    val snapshot = uiState.value
    if (snapshot.settings.activeRole != UserRole.CASHIER) return
    val targetId =
      snapshot.activeEmployee?.id
        ?: snapshot.settings.activeEmployeeId?.takeIf { it.isNotBlank() }
        ?: "emp_default"
    val cleanName = name.trim()
    val cleanUser = username.trim()
    val cleanPhone = phone.trim().take(40)
    val clashesOwner = cleanUser.equals(snapshot.settings.ownerUsername, ignoreCase = true)
    val clashesEmployee =
      snapshot.employees.any { it.id != targetId && it.username.equals(cleanUser, ignoreCase = true) }
    val validationError =
      when {
        cleanName.isBlank() -> "Nama profil tidak boleh kosong."
        !cleanUser.matches(Regex("^[a-zA-Z0-9_.\\-]{3,40}$")) ->
          "Username minimal 3 karakter (huruf, angka, titik, _, atau -)."
        clashesOwner -> "Username tidak boleh sama dengan username pemilik."
        clashesEmployee -> "Username '$cleanUser' sudah digunakan karyawan lain."
        password.length < 4 -> "Password minimal 4 karakter."
        else -> null
      }
    if (validationError != null) {
      transientState.update { it.copy(statusMessage = validationError) }
      return
    }

    viewModelScope.launch {
      repository
        .updateOwnEmployeeAccount(
          employeeId = targetId,
          name = cleanName,
          username = cleanUser,
          password = password,
          phone = cleanPhone,
        )
        .onSuccess {
          transientState.update {
            it.copy(statusMessage = "Profil & akun login kasir berhasil diperbarui")
          }
        }
        .onFailure { e ->
          val msg =
            if (e is IllegalArgumentException || e is IllegalStateException) {
              e.message ?: "Gagal memperbarui akun kasir."
            } else {
              userFriendlyFirestoreMessage(e, "Gagal memperbarui akun kasir.")
            }
          transientState.update { it.copy(statusMessage = msg) }
        }
    }
  }

  fun deleteEmployee(employee: EmployeeAccount) {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .deleteEmployee(employee.id)
        .onSuccess {
          transientState.update {
            it.copy(statusMessage = "Karyawan '${employee.name}' dihapus")
          }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal menghapus karyawan."))
          }
        }
    }
  }

  fun resetDemoData() {
    if (uiState.value.settings.activeRole != UserRole.OWNER) return
    viewModelScope.launch {
      repository
        .resetDemoData()
        .onSuccess {
          transientState.update { it.copy(statusMessage = "Data demo berhasil dipulihkan") }
        }
        .onFailure { e ->
          transientState.update {
            it.copy(statusMessage = userFriendlyFirestoreMessage(e, "Gagal memulihkan data demo."))
          }
        }
    }
  }

  fun consumeStatusMessage() {
    transientState.update { it.copy(statusMessage = null) }
  }

  companion object {
    fun factory(
      application: Application,
      authUid: String,
      displayName: String,
      userEmail: String,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return StoreViewModel(application, authUid, displayName, userEmail) as T
        }
      }
  }
}
