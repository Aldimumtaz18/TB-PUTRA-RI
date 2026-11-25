package com.example

import com.example.data.local.StoreSettings
import com.example.data.model.CustomerType
import com.example.data.model.FinanceDateFilter
import com.example.data.model.FinanceDatePreset
import com.example.data.repository.DummyDataSeeder
import com.example.ui.viewmodel.StoreUiState
import com.example.util.ReceiptHelper
import com.example.util.addDaysMillis
import com.example.util.localDayStartToUtcPickerMillis
import com.example.util.startOfDayMillis
import com.example.util.utcPickerMillisToLocalDayStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoreCalculationsTest {
  @Test
  fun totalRevenueMatchesSumOfDailyPaidRevenueEvenAfterKasbonSettlement() {
    val now = System.currentTimeMillis()
    val initialTx = DummyDataSeeder.initialTransactions(now)
    val initialExpenses = DummyDataSeeder.initialExpenses(now)

    val stateBefore = StoreUiState(transactions = initialTx, expenses = initialExpenses)
    assertEquals(247_000L, stateBefore.totalRevenuePaid)
    assertEquals(
      stateBefore.totalRevenuePaid,
      stateBefore.dailyIncomeHistory.sumOf { it.paidRevenue },
    )

    // Simulate settling the oldest unpaid kasbon today
    val settledTx =
      initialTx.mapIndexed { index, tx ->
        if (index == 0) tx.copy(isPaid = true, paidAt = now, amountPaid = tx.totalAmount) else tx
      }
    val stateAfter = StoreUiState(transactions = settledTx, expenses = initialExpenses)
    assertEquals(247_000L + 883_500L, stateAfter.totalRevenuePaid)
    assertEquals(
      stateAfter.totalRevenuePaid,
      stateAfter.dailyIncomeHistory.sumOf { it.paidRevenue },
    )
    assertEquals(stateAfter.todayPaidRevenue, stateAfter.dailyIncomeHistory.first().paidRevenue)
  }

  @Test
  fun financeDateFilterSupportsTodayTomorrowMonthAndCustomPicker() {
    val now = System.currentTimeMillis()
    val todayStart = startOfDayMillis(now)
    val tomorrowStart = addDaysMillis(todayStart, 1)
    val initialTx = DummyDataSeeder.initialTransactions(now)
    val initialExpenses = DummyDataSeeder.initialExpenses(now)

    val todayState =
      StoreUiState(
        transactions = initialTx,
        expenses = initialExpenses,
        financeDateFilter = FinanceDateFilter(FinanceDatePreset.HARI_INI),
      )
    assertEquals(todayState.todayPaidRevenue, todayState.filteredRevenuePaid(now))

    val tomorrowState =
      StoreUiState(
        transactions = initialTx,
        expenses = initialExpenses,
        financeDateFilter = FinanceDateFilter(FinanceDatePreset.BESOK),
      )
    assertEquals(0L, tomorrowState.filteredRevenuePaid(now))
    assertEquals(1, tomorrowState.filteredDailyIncomeHistory(now).size)
    assertEquals(tomorrowStart, tomorrowState.filteredDailyIncomeHistory(now).first().dayStartMillis)

    val monthState =
      StoreUiState(
        transactions = initialTx,
        expenses = initialExpenses,
        financeDateFilter = FinanceDateFilter(FinanceDatePreset.BULAN_INI),
      )
    assertTrue(monthState.filteredRevenuePaid(now) >= 0L)

    // Verify UTC picker <-> local day start round-trip
    val roundTrip = utcPickerMillisToLocalDayStart(localDayStartToUtcPickerMillis(tomorrowStart))
    assertEquals(tomorrowStart, roundTrip)
  }

  @Test
  fun customerTypesContainOnlyPelangganAndKontraktor() {
    assertEquals(2, CustomerType.entries.size)
    assertEquals("Pelanggan", CustomerType.PELANGGAN.label)
    assertEquals("Kontraktor", CustomerType.KONTRAKTOR.label)
    assertEquals(CustomerType.PELANGGAN, CustomerType.fromStoredLabel("Umum/Eceran"))
    assertEquals(CustomerType.PELANGGAN, CustomerType.fromStoredLabel("Perorangan"))
    assertEquals(CustomerType.KONTRAKTOR, CustomerType.fromStoredLabel("Kontraktor/Tukang"))

    val state = StoreUiState(transactions = DummyDataSeeder.initialTransactions())
    assertTrue(state.savedCustomers.isNotEmpty())
  }

  @Test
  fun receiptBitmapRendersForThermalAndSuratJalan() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val tx = DummyDataSeeder.initialTransactions(System.currentTimeMillis()).first()
    val settings = StoreSettings()

    val strukBitmap = ReceiptHelper.renderReceiptBitmap(tx, settings, isSuratJalan = false, context = context)
    assertEquals(760, strukBitmap.width)
    assertTrue(strukBitmap.height >= 640)
    strukBitmap.recycle()

    val suratJalanBitmap = ReceiptHelper.renderReceiptBitmap(tx, settings, isSuratJalan = true, context = context)
    assertEquals(760, suratJalanBitmap.width)
    assertTrue(suratJalanBitmap.height >= 640)
    suratJalanBitmap.recycle()
  }

  @Test
  fun firestoreMappersProduceCleanSchemaCompliantPayloads() {
    val product = DummyDataSeeder.initialProducts().first()
    val createMap =
      com.example.data.remote.FirestoreMappers.productCreateMap(
        product = product,
        numericId = 1L,
        storeId = "owner_1",
        ownerUid = "owner_1",
        memberUids = listOf("owner_1", "cashier_1"),
      )
    assertTrue(!createMap.containsKey("id") && !createMap.containsKey("docId"))
    assertEquals("owner_1", createMap["storeId"])
    assertEquals("Kg", createMap["retailUnitName"])

    val noRetailProduct = product.copy(retailUnitName = null, retailSellPrice = 0L)
    val noRetailCreate =
      com.example.data.remote.FirestoreMappers.productCreateMap(
        product = noRetailProduct,
        numericId = 2L,
        storeId = "owner_1",
        ownerUid = "owner_1",
        memberUids = listOf("owner_1"),
      )
    assertTrue(!noRetailCreate.containsKey("retailUnitName"))

    val tx = DummyDataSeeder.initialTransactions(1_700_000_000_000L).first()
    val txMap =
      com.example.data.remote.FirestoreMappers.transactionCreateMap(
        tx = tx,
        numericId = 10L,
        storeId = "owner_1",
        ownerUid = "owner_1",
        recordedByUid = "owner_1",
        memberUids = listOf("owner_1"),
      )
    assertEquals(false, txMap["paidStatus"])
    assertTrue(!txMap.containsKey("isPaid"))
    assertTrue(!txMap.containsKey("paidAtMillis"))
    assertTrue(!txMap.containsKey("id"))
  }

  @Test
  fun storeWorkspaceMapsUsernameAndPasswordWithLegacyDefaults() {
    val legacyWorkspace =
      com.example.data.remote.FirestoreStoreWorkspace(
        ownerUsername = "",
        ownerPassword = "",
        cashierUsername = "",
        cashierPassword = "",
        ownerName = "H. Putra",
        cashierName = "Budi Santoso",
      )
    val mappedDefault = legacyWorkspace.toStoreSettings(StoreSettings())
    assertEquals("owner", mappedDefault.ownerUsername)
    assertEquals("owner123", mappedDefault.ownerPassword)
    assertEquals("kasir", mappedDefault.cashierUsername)
    assertEquals("kasir123", mappedDefault.cashierPassword)
    assertTrue(mappedDefault.isSeeded)

    val customWorkspace =
      com.example.data.remote.FirestoreStoreWorkspace(
        storeName = "TB MAJU JAYA",
        ownerUsername = "bos_putra",
        ownerPassword = "rahasia99",
        cashierUsername = "kasir_pagi",
        cashierPassword = "pagi1234",
      )
    val mappedCustom = customWorkspace.toStoreSettings(StoreSettings())
    assertEquals("TB MAJU JAYA", mappedCustom.storeName)
    assertEquals("bos_putra", mappedCustom.ownerUsername)
    assertEquals("rahasia99", mappedCustom.ownerPassword)
    assertEquals("kasir_pagi", mappedCustom.cashierUsername)
    assertEquals("pagi1234", mappedCustom.cashierPassword)
  }

  @Test
  fun activeUserNameResolvesLoggedInEmployeeFromList() {
    val emp1 = com.example.data.model.EmployeeAccount("emp_1", "Budi Santoso", "budi", "budi123")
    val emp2 = com.example.data.model.EmployeeAccount("emp_2", "Siti Rahma", "siti", "siti123")
    val cashierState =
      StoreUiState(
        settings =
          StoreSettings(
            activeRole = com.example.data.model.UserRole.CASHIER,
            activeEmployeeId = "emp_2",
            activeEmployeeName = "Siti",
          ),
        employees = listOf(emp1, emp2),
      )
    assertEquals("Siti Rahma", cashierState.activeUserName)

    val ownerState =
      cashierState.copy(
        settings = cashierState.settings.copy(activeRole = com.example.data.model.UserRole.OWNER)
      )
    assertEquals("H. Putra", ownerState.activeUserName)
  }

  @Test
  fun manualPriceCutAppliesOnlyToPelangganAndClampsToSubtotal() {
    val item =
      com.example.data.model.CartItem(
        productId = 1L,
        productName = "Semen Tiga Roda 50Kg",
        category = "Semen",
        unitName = "Sak",
        isRetailUnit = false,
        conversionFactor = 1.0,
        quantity = 2.0,
        unitPrice = 68_000L,
        unitCostPerBase = 58_000L,
        availableBaseStock = 10.0,
      )
    val baseState =
      StoreUiState(
        cart = listOf(item),
        customerType = CustomerType.PELANGGAN,
        manualDiscountAmount = 16_000L,
      )
    assertEquals(136_000L, baseState.cartSubtotal)
    assertEquals(16_000L, baseState.cartDiscount)
    assertEquals(120_000L, baseState.cartTotal)

    val clampedState = baseState.copy(manualDiscountAmount = 500_000L)
    assertEquals(136_000L, clampedState.cartDiscount)
    assertEquals(0L, clampedState.cartTotal)

    val contractorState = baseState.copy(customerType = CustomerType.KONTRAKTOR)
    assertEquals(6_800L, contractorState.cartDiscount)
    assertEquals(129_200L, contractorState.cartTotal)
  }

  @Test
  fun receiptBitmapRendersPelangganWithManualDiscountWithoutCrash() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val baseTx = DummyDataSeeder.initialTransactions(System.currentTimeMillis()).first()
    val negotiatedTx =
      baseTx.copy(
        customerType = CustomerType.PELANGGAN.label,
        customerName = "Pak Joko",
        subtotal = 150_000L,
        discountAmount = 15_000L,
        totalAmount = 135_000L,
        isPaid = true,
        paymentMethod = "Tunai",
        amountPaid = 150_000L,
        changeAmount = 15_000L,
      )
    val bitmap =
      ReceiptHelper.renderReceiptBitmap(
        tx = negotiatedTx,
        settings = StoreSettings(),
        isSuratJalan = false,
        context = context,
      )
    assertEquals(760, bitmap.width)
    assertTrue(bitmap.height >= 640)
    bitmap.recycle()
  }

  @Test
  fun dynamicCategoryNamesMergeManagedAndProductCategoriesAndMappersStayClean() {
    val managed =
      listOf(
        com.example.data.model.CategoryItem(id = "cat_1", name = "Semen"),
        com.example.data.model.CategoryItem(id = "cat_2", name = "Cat"),
      )
    val customProduct = DummyDataSeeder.initialProducts().first().copy(category = "Keramik")
    val state =
      StoreUiState(
        products = listOf(customProduct),
        categories = managed,
      )
    assertEquals(listOf("Semen", "Cat", "Keramik"), state.categoryNames)

    val emptyState = StoreUiState(products = emptyList(), categories = emptyList())
    assertEquals(com.example.data.model.DEFAULT_PRODUCT_CATEGORIES, emptyState.categoryNames)

    val createMap =
      com.example.data.remote.FirestoreMappers.categoryCreateMap(
        name = "  Baja Ringan  ",
        storeId = "store_1",
        ownerUid = "owner_1",
        memberUids = listOf("owner_1", "kasir_1"),
      )
    assertEquals("Baja Ringan", createMap["name"])
    assertEquals("store_1", createMap["storeId"])
    assertEquals("owner_1", createMap["ownerUid"])
    assertTrue(!createMap.containsKey("id"))

    val updateMap =
      com.example.data.remote.FirestoreMappers.categoryUpdateMap(
        name = "  Besi & Baja  ",
        memberUids = listOf("owner_1"),
      )
    assertEquals("Besi & Baja", updateMap["name"])
    assertTrue(!updateMap.containsKey("storeId") && !updateMap.containsKey("ownerUid"))
  }

  @Test
  fun customReportPeriodsSupportWeeklyMonthlyYearlyAndCustomRanges() {
    val cal =
      java.util.Calendar.getInstance().apply {
        clear()
        set(2026, java.util.Calendar.NOVEMBER, 22, 10, 0, 0)
        set(java.util.Calendar.MILLISECOND, 0)
      }
    val now = cal.timeInMillis
    val tx = DummyDataSeeder.initialTransactions(now)
    val exps = DummyDataSeeder.initialExpenses(now)
    val state = StoreUiState(transactions = tx, expenses = exps)

    val weekReport = state.buildReportDataForFilter(FinanceDateFilter(FinanceDatePreset.MINGGU_INI), now)
    assertTrue(weekReport.periodLabel.startsWith("Minggu Ini"))

    val monthReport = state.buildReportDataForFilter(FinanceDateFilter(FinanceDatePreset.BULAN_INI), now)
    assertEquals("Bulan Ini (November 2026)", monthReport.periodLabel)

    val yearReport = state.buildReportDataForFilter(FinanceDateFilter(FinanceDatePreset.TAHUN_INI), now)
    assertEquals("Tahun Ini (2026)", yearReport.periodLabel)
    assertEquals(state.totalRevenuePaid, yearReport.paidRevenue)

    val (mStart, mEnd) = com.example.util.monthDayRangeMillis(2026, java.util.Calendar.OCTOBER)
    val octReport =
      state.buildReportDataForFilter(
        FinanceDateFilter(
          preset = FinanceDatePreset.RENTANG_TANGGAL,
          customStartDayMillis = mStart,
          customEndDayMillis = mEnd,
          customLabel = "Bulanan • Oktober 2026",
        ),
        now,
      )
    assertEquals("Bulanan • Oktober 2026", octReport.periodLabel)
    assertEquals(0L, octReport.paidRevenue)

    val (yStart, yEnd) = com.example.util.yearDayRangeMillis(2026)
    val customYearReport =
      state.buildReportDataForFilter(
        FinanceDateFilter(
          preset = FinanceDatePreset.RENTANG_TANGGAL,
          customStartDayMillis = yStart,
          customEndDayMillis = yEnd,
          customLabel = "Tahunan • 2026",
        ),
        now,
      )
    assertEquals(state.totalRevenuePaid, customYearReport.paidRevenue)
  }

  @Test
  fun netProfitExcludesKulakanAndSplitsCashVsTransferIncludingKasbonDpAndCicilan() {
    val cal =
      java.util.Calendar.getInstance().apply {
        clear()
        set(2026, java.util.Calendar.NOVEMBER, 22, 10, 0, 0)
        set(java.util.Calendar.MILLISECOND, 0)
      }
    val day1 = cal.timeInMillis
    val day2 = addDaysMillis(day1, 1)

    val paidCashTx =
      com.example.data.local.TransactionEntity(
        id = 1L,
        invoiceNumber = "INV-1",
        timestamp = day1,
        cashierName = "Kasir",
        cashierRole = "CASHIER",
        customerType = CustomerType.PELANGGAN.label,
        customerName = "Budi",
        customerPhone = "08123456789",
        paymentMethod = com.example.data.model.PaymentMethod.TUNAI.label,
        subtotal = 200_000L,
        discountAmount = 0L,
        totalAmount = 200_000L,
        totalCost = 140_000L,
        amountPaid = 200_000L,
        changeAmount = 0L,
        isPaid = true,
        paidAt = day1,
        itemsJson = "[]",
      )

    val dpPayments =
      listOf(
        com.example.data.model.KasbonPaymentRecord(
          amount = 100_000L,
          paymentMethod = com.example.data.model.PaymentMethod.TUNAI.label,
          timestamp = day1,
          recordedBy = "Kasir",
        ),
        com.example.data.model.KasbonPaymentRecord(
          amount = 150_000L,
          paymentMethod = com.example.data.model.PaymentMethod.TRANSFER_QRIS.label,
          timestamp = day2,
          recordedBy = "Pemilik",
        ),
      )
    val partialKasbonTx =
      com.example.data.local.TransactionEntity(
        id = 2L,
        invoiceNumber = "INV-2",
        timestamp = day1,
        cashierName = "Kasir",
        cashierRole = "CASHIER",
        customerType = CustomerType.KONTRAKTOR.label,
        customerName = "Pak Mandor",
        customerPhone = "08129876543",
        paymentMethod = com.example.data.model.PaymentMethod.KASBON.label,
        subtotal = 400_000L,
        discountAmount = 0L,
        totalAmount = 400_000L,
        totalCost = 300_000L,
        amountPaid = 250_000L,
        changeAmount = 0L,
        isPaid = false,
        paidAt = day2,
        itemsJson = "[]",
        paymentsJson = com.example.data.local.KasbonPaymentCodec.encode(dpPayments),
      )

    val opExpense =
      com.example.data.local.ExpenseEntity(
        id = 1L,
        title = "Bensin Pikap",
        category = com.example.data.model.ExpenseCategory.BBM.label,
        amount = 40_000L,
        notes = "",
        recordedBy = "Kasir",
        recordedRole = "CASHIER",
        timestamp = day1,
      )
    val kulakanExpense =
      com.example.data.local.ExpenseEntity(
        id = 2L,
        title = "Restok Semen",
        category = com.example.data.model.ExpenseCategory.KULAKAN.label,
        amount = 500_000L,
        notes = "",
        recordedBy = "Pemilik",
        recordedRole = "OWNER",
        timestamp = day1,
      )

    val products =
      listOf(
        com.example.data.local.ProductEntity(
          id = 1L,
          name = "Semen 50Kg",
          category = "Semen",
          baseUnit = "Sak",
          stockQuantity = 10.0,
          minStock = 5.0,
          costPricePerBaseUnit = 55_000L,
          sellPricePerBaseUnit = 68_000L,
        )
      )

    val state =
      StoreUiState(
        products = products,
        transactions = listOf(paidCashTx, partialKasbonTx),
        expenses = listOf(opExpense, kulakanExpense),
      )

    assertEquals(550_000L, state.totalInventoryCostValue)
    assertEquals(450_000L, state.totalRevenuePaid)
    assertEquals(300_000L, state.filteredCashRevenue(day1))
    assertEquals(150_000L, state.filteredTransferRevenue(day1))
    assertEquals(150_000L, state.totalReceivables)
    assertEquals(60_000L, state.totalGrossProfit)
    assertEquals(40_000L, state.totalOperationalExpenses)
    assertEquals(20_000L, state.totalNetProfit)

    val report = state.buildReportDataForFilter(FinanceDateFilter(FinanceDatePreset.SEMUA), day2)
    assertEquals(450_000L, report.paidRevenue)
    assertEquals(300_000L, report.cashRevenue)
    assertEquals(150_000L, report.transferRevenue)
    assertEquals(40_000L, report.operationalExpenses)
    assertEquals(500_000L, report.kulakanExpenses)
    assertEquals(20_000L, report.netProfit)
    assertEquals(150_000L, report.unpaidReceivables)
    assertEquals(550_000L, report.inventoryCostValue)
  }

  @Test
  fun receiptPaymentSummaryTracksKasbonDpInstallmentAndSettlement() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val now = 1_700_000_000_000L
    val dpOnlyRecords =
      listOf(
        com.example.data.model.KasbonPaymentRecord(
          amount = 150_000L,
          paymentMethod = com.example.data.model.PaymentMethod.TUNAI.label,
          timestamp = now,
          recordedBy = "Kasir",
        )
      )
    val kasbonWithDp =
      com.example.data.local.TransactionEntity(
        id = 10L,
        invoiceNumber = "INV-DP-1",
        timestamp = now,
        cashierName = "Kasir",
        cashierRole = "CASHIER",
        customerType = CustomerType.PELANGGAN.label,
        customerName = "Pak Hendra",
        customerPhone = "08123456789",
        paymentMethod = com.example.data.model.PaymentMethod.KASBON.label,
        subtotal = 500_000L,
        discountAmount = 0L,
        totalAmount = 500_000L,
        totalCost = 380_000L,
        amountPaid = 150_000L,
        changeAmount = 0L,
        isPaid = false,
        paidAt = now,
        itemsJson = "[]",
        paymentsJson = com.example.data.local.KasbonPaymentCodec.encode(dpOnlyRecords),
      )

    val dpSummary = ReceiptHelper.buildReceiptPaymentSummary(kasbonWithDp)
    assertEquals(false, dpSummary.isCashPaid)
    assertEquals(150_000L, dpSummary.dpAmount)
    assertEquals("Tunai", dpSummary.dpMethodLabel)
    assertEquals(150_000L, dpSummary.totalPaid)
    assertEquals(350_000L, dpSummary.remainingKasbon)
    assertEquals(false, dpSummary.hasPartialOrMultiplePayments)
    assertEquals(1, dpSummary.paymentEntries.size)
    assertEquals("DP (Tunai)", dpSummary.paymentEntries.first().label)
    assertEquals(150_000L, dpSummary.paymentEntries.first().amount)
    assertTrue(dpSummary.paymentEntries.first().detail.contains("Kasir"))

    val partialInstallmentRecords =
      dpOnlyRecords +
        com.example.data.model.KasbonPaymentRecord(
          amount = 200_000L,
          paymentMethod = com.example.data.model.PaymentMethod.TRANSFER_QRIS.label,
          timestamp = now + 43_200_000L,
          recordedBy = "Pemilik",
        )
    val partialKasbon =
      kasbonWithDp.copy(
        isPaid = false,
        amountPaid = 350_000L,
        paymentMethod = com.example.data.model.PaymentMethod.KASBON.label,
        paymentsJson = com.example.data.local.KasbonPaymentCodec.encode(partialInstallmentRecords),
      )
    val partialSummary = ReceiptHelper.buildReceiptPaymentSummary(partialKasbon)
    assertEquals(150_000L, partialSummary.dpAmount)
    assertEquals(350_000L, partialSummary.totalPaid)
    assertEquals(150_000L, partialSummary.remainingKasbon)
    assertEquals(true, partialSummary.hasPartialOrMultiplePayments)
    assertEquals(2, partialSummary.paymentEntries.size)
    assertEquals("DP (Tunai)", partialSummary.paymentEntries[0].label)
    assertEquals("Cicilan 1 (Transfer/QRIS)", partialSummary.paymentEntries[1].label)
    assertEquals(200_000L, partialSummary.paymentEntries[1].amount)
    assertTrue(partialSummary.paymentEntries[1].detail.contains("Pemilik"))

    val settledWithDpRecords =
      partialInstallmentRecords +
        com.example.data.model.KasbonPaymentRecord(
          amount = 150_000L,
          paymentMethod = com.example.data.model.PaymentMethod.TUNAI.label,
          timestamp = now + 86_400_000L,
          recordedBy = "Pemilik",
        )
    val settledKasbon =
      kasbonWithDp.copy(
        isPaid = true,
        amountPaid = 500_000L,
        paymentMethod = "Tunai (Kasbon)",
        paymentsJson = com.example.data.local.KasbonPaymentCodec.encode(settledWithDpRecords),
      )

    val settledSummary = ReceiptHelper.buildReceiptPaymentSummary(settledKasbon)
    assertEquals(false, settledSummary.isCashPaid)
    assertEquals(150_000L, settledSummary.dpAmount)
    assertEquals(500_000L, settledSummary.totalPaid)
    assertEquals(0L, settledSummary.remainingKasbon)
    assertEquals(true, settledSummary.hasPartialOrMultiplePayments)
    assertEquals(3, settledSummary.paymentEntries.size)
    assertEquals("DP (Tunai)", settledSummary.paymentEntries[0].label)
    assertEquals("Cicilan 1 (Transfer/QRIS)", settledSummary.paymentEntries[1].label)
    assertEquals("Pelunasan (Tunai)", settledSummary.paymentEntries[2].label)

    val bitmap = ReceiptHelper.renderReceiptBitmap(partialKasbon, StoreSettings(), isSuratJalan = false, context = context)
    assertEquals(760, bitmap.width)
    assertTrue(bitmap.height >= 640)
    bitmap.recycle()
  }
}
