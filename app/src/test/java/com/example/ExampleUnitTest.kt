package com.example

import com.example.data.local.TransactionEntity
import com.example.data.model.CustomerType
import com.example.data.model.KasbonPaymentRecord
import com.example.data.model.PaymentMethod
import com.example.util.ReceiptHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for core store & receipt logic.
 */
class ExampleUnitTest {
  private fun sampleKasbonTx(
    timestamp: Long,
    totalAmount: Long = 500_000L,
    amountPaid: Long = 0L,
    isPaid: Boolean = false,
    paymentMethod: String = PaymentMethod.KASBON.label,
    paidAt: Long? = null,
  ): TransactionEntity =
    TransactionEntity(
      id = 1L,
      invoiceNumber = "INV-TEST-1",
      timestamp = timestamp,
      cashierName = "Budi",
      cashierRole = "CASHIER",
      customerType = CustomerType.PELANGGAN.label,
      customerName = "Pak Hendra",
      customerPhone = "08123456789",
      paymentMethod = paymentMethod,
      subtotal = totalAmount,
      discountAmount = 0L,
      totalAmount = totalAmount,
      totalCost = 350_000L,
      amountPaid = amountPaid,
      changeAmount = 0L,
      isPaid = isPaid,
      paidAt = paidAt,
      itemsJson = "[]",
      paymentsJson = "",
    )

  @Test
  fun kasbonWithInitialDpOnlyShowsDpAndRemainingBalance() {
    val t0 = 1_700_000_000_000L
    val records =
      listOf(
        KasbonPaymentRecord(
          amount = 150_000L,
          paymentMethod = PaymentMethod.TUNAI.label,
          timestamp = t0,
          recordedBy = "Budi",
        )
      )
    val tx =
      sampleKasbonTx(
        timestamp = t0,
        amountPaid = 150_000L,
        isPaid = false,
        paidAt = t0,
      )

    val summary = ReceiptHelper.buildReceiptPaymentSummary(tx, records)
    assertFalse(summary.isCashPaid)
    assertEquals(150_000L, summary.dpAmount)
    assertEquals("Tunai", summary.dpMethodLabel)
    assertEquals(150_000L, summary.totalPaid)
    assertEquals(350_000L, summary.remainingKasbon)
    assertFalse(summary.hasPartialOrMultiplePayments)
    assertEquals(1, summary.paymentEntries.size)
    assertEquals("DP (Tunai)", summary.paymentEntries[0].label)
    assertEquals(150_000L, summary.paymentEntries[0].amount)
    assertTrue(summary.paymentEntries[0].detail.contains("Budi"))
  }

  @Test
  fun kasbonWithoutDpAndFirstPartialInstallmentShowsCicilan1AndRemaining() {
    val t0 = 1_700_000_000_000L
    val t1 = t0 + 3_600_000L
    val records =
      listOf(
        KasbonPaymentRecord(
          amount = 200_000L,
          paymentMethod = PaymentMethod.TRANSFER_QRIS.label,
          timestamp = t1,
          recordedBy = "H. Putra",
        )
      )
    val tx =
      sampleKasbonTx(
        timestamp = t0,
        amountPaid = 200_000L,
        isPaid = false,
        paidAt = t1,
      )

    val summary = ReceiptHelper.buildReceiptPaymentSummary(tx, records)
    assertEquals(0L, summary.dpAmount)
    assertNull(summary.dpMethodLabel)
    assertEquals(200_000L, summary.totalPaid)
    assertEquals(300_000L, summary.remainingKasbon)
    assertTrue(summary.hasPartialOrMultiplePayments)
    assertEquals(1, summary.paymentEntries.size)
    assertEquals("Cicilan 1 (Transfer/QRIS)", summary.paymentEntries[0].label)
    assertEquals(200_000L, summary.paymentEntries[0].amount)
    assertTrue(summary.paymentEntries[0].detail.contains("H. Putra"))
  }

  @Test
  fun kasbonWithDpPartialInstallmentAndSettlementListsFullChronology() {
    val t0 = 1_700_000_000_000L
    val dp = KasbonPaymentRecord(100_000L, PaymentMethod.TUNAI.label, t0, "Budi")
    val cicilan1 = KasbonPaymentRecord(150_000L, PaymentMethod.TRANSFER_QRIS.label, t0 + 3_600_000L, "H. Putra")
    val cicilan2 = KasbonPaymentRecord(100_000L, PaymentMethod.TUNAI.label, t0 + 7_200_000L, "H. Putra")
    val pelunasan = KasbonPaymentRecord(150_000L, PaymentMethod.TRANSFER_QRIS.label, t0 + 86_400_000L, "H. Putra")

    val partialRecords = listOf(dp, cicilan1, cicilan2)
    val partialTx =
      sampleKasbonTx(
        timestamp = t0,
        amountPaid = 350_000L,
        isPaid = false,
        paidAt = cicilan2.timestamp,
      )
    val partialSummary = ReceiptHelper.buildReceiptPaymentSummary(partialTx, partialRecords)
    assertEquals(350_000L, partialSummary.totalPaid)
    assertEquals(150_000L, partialSummary.remainingKasbon)
    assertEquals(
      listOf("DP (Tunai)", "Cicilan 1 (Transfer/QRIS)", "Cicilan 2 (Tunai)"),
      partialSummary.paymentEntries.map { it.label },
    )

    val settledRecords = listOf(dp, cicilan1, cicilan2, pelunasan)
    val settledTx =
      sampleKasbonTx(
        timestamp = t0,
        amountPaid = 500_000L,
        isPaid = true,
        paymentMethod = "Transfer/QRIS (Kasbon)",
        paidAt = pelunasan.timestamp,
      )
    val settledSummary = ReceiptHelper.buildReceiptPaymentSummary(settledTx, settledRecords)
    assertEquals(500_000L, settledSummary.totalPaid)
    assertEquals(0L, settledSummary.remainingKasbon)
    assertEquals(
      listOf(
        "DP (Tunai)",
        "Cicilan 1 (Transfer/QRIS)",
        "Cicilan 2 (Tunai)",
        "Pelunasan (Transfer/QRIS)",
      ),
      settledSummary.paymentEntries.map { it.label },
    )
  }

  @Test
  fun cashierActiveEmployeeAndSelfUpdateMapRespectRulesAndLiveName() {
    val emp1 =
      com.example.data.model.EmployeeAccount(
        id = "emp_1",
        name = "Budi Lama",
        username = "budi",
        password = "pass1234",
      )
    val emp2 =
      com.example.data.model.EmployeeAccount(
        id = "emp_2",
        name = "Siti Rahma",
        username = "siti_kasir",
        password = "pass5678",
        phone = "0812-1111-2222",
      )
    val uiState =
      com.example.ui.viewmodel.StoreUiState(
        settings =
          com.example.data.local.StoreSettings(
            activeRole = com.example.data.model.UserRole.CASHIER,
            activeEmployeeId = "emp_2",
            activeEmployeeName = "Siti Lama",
          ),
        employees = listOf(emp1, emp2),
      )

    assertEquals("emp_2", uiState.activeEmployee?.id)
    assertEquals("Siti Rahma", uiState.activeUserName)
    assertEquals("0812-1111-2222", uiState.activeEmployee?.phone)

    val selfUpdatePayload =
      com.example.data.remote.FirestoreMappers.employeeSelfUpdateMap(
        name = "  Siti Rahma Putri  ",
        username = "  siti_baru  ",
        password = "newpass123",
        phone = "  0813-9999-0000  ",
      )
    assertEquals(setOf("name", "username", "password", "phone", "updatedAt"), selfUpdatePayload.keys)
    assertEquals("Siti Rahma Putri", selfUpdatePayload["name"])
    assertEquals("siti_baru", selfUpdatePayload["username"])
    assertEquals("newpass123", selfUpdatePayload["password"])
    assertEquals("0813-9999-0000", selfUpdatePayload["phone"])

    val ownerUpdatePayload =
      com.example.data.remote.FirestoreMappers.employeeUpdateMap(
        name = "Budi Baru",
        username = "budi_baru",
        password = "newpass123",
        memberUids = listOf("owner_1"),
        phone = "0811-2222-3333",
      )
    assertEquals("0811-2222-3333", ownerUpdatePayload["phone"])
  }

  @Test
  fun financeReportSignatoryReflectsCashierOrOwnerPrinter() {
    val emp =
      com.example.data.model.EmployeeAccount(
        id = "emp_1",
        name = "Siti Rahma",
        username = "siti_kasir",
        password = "pass1234",
      )
    val cashierState =
      com.example.ui.viewmodel.StoreUiState(
        settings =
          com.example.data.local.StoreSettings(
            storeName = "TB PUTRA RI",
            ownerName = "H. Putra",
            activeRole = com.example.data.model.UserRole.CASHIER,
            activeEmployeeId = "emp_1",
          ),
        employees = listOf(emp),
      )
    val cashierReport =
      cashierState.buildReportDataForFilter(
        com.example.data.model.FinanceDateFilter(com.example.data.model.FinanceDatePreset.SEMUA)
      )
    val cashierSignatory = com.example.util.FinanceReportHelper.buildReportSignatoryInfo(cashierReport)
    assertEquals("Dicetak oleh,", cashierSignatory.headerText)
    assertEquals("Kasir TB PUTRA RI", cashierSignatory.roleTitle)
    assertEquals("Siti Rahma", cashierSignatory.signerName)
    assertEquals("Siti Rahma (Kasir)", cashierSignatory.printedBySummary)

    val ownerState =
      cashierState.copy(
        settings = cashierState.settings.copy(activeRole = com.example.data.model.UserRole.OWNER)
      )
    val ownerReport =
      ownerState.buildReportDataForFilter(
        com.example.data.model.FinanceDateFilter(com.example.data.model.FinanceDatePreset.SEMUA)
      )
    val ownerSignatory = com.example.util.FinanceReportHelper.buildReportSignatoryInfo(ownerReport)
    assertEquals("Mengetahui,", ownerSignatory.headerText)
    assertEquals("Pemilik TB PUTRA RI", ownerSignatory.roleTitle)
    assertEquals("H. Putra", ownerSignatory.signerName)
    assertEquals("H. Putra (Pemilik)", ownerSignatory.printedBySummary)
  }

  @Test
  fun escPosReceiptFormattingRespects58mmAnd80mmPaperWidths() {
    val tx =
      sampleKasbonTx(
        timestamp = 1_700_000_000_000L,
        totalAmount = 500_000L,
        amountPaid = 150_000L,
        isPaid = false,
      ).copy(
        itemsJson =
          """[{"productId":1,"productName":"Semen Tiga Roda 50kg","unitName":"Sak","quantity":5.0,"unitPrice":62000,"standardUnitPrice":65000,"minUnitPrice":60000,"costPrice":57000,"subtotal":310000}]""",
        paymentsJson =
          """[{"amount":150000,"paymentMethod":"Tunai","timestamp":1700000000000,"recordedBy":"Budi"}]""",
      )
    val settings = com.example.data.local.StoreSettings(storeName = "TB PUTRA RI")

    val lines58 =
      ReceiptHelper.buildEscPosLines(
        tx = tx,
        settings = settings,
        isSuratJalan = false,
        paperSize = com.example.data.model.ReceiptPaperSize.THERMAL_58MM,
      )
    assertTrue(lines58.all { it.text.length <= 32 })
    assertTrue(lines58.any { it.text.trim() == "STRUK PEMBELIAN" })
    assertTrue(lines58.none { it.text.contains("58 mm") })
    assertTrue(lines58.none { it.text.contains("Standar ") })
    assertTrue(lines58.any { it.text.contains("DP (Tunai)") })
    assertTrue(lines58.any { it.text.contains("Sisa Kasbon") })

    val lines80 =
      ReceiptHelper.buildEscPosLines(
        tx = tx,
        settings = settings,
        isSuratJalan = false,
        paperSize = com.example.data.model.ReceiptPaperSize.THERMAL_80MM,
      )
    assertTrue(lines80.all { it.text.length <= 48 })
    assertTrue(lines80.any { it.text.trim() == "STRUK PEMBELIAN" })
    assertTrue(lines80.none { it.text.contains("80 mm") })
    assertTrue(lines80.any { it.text == "-".repeat(48) })

    val rawBytes =
      ReceiptHelper.buildEscPosBytes(
        tx = tx,
        settings = settings,
        isSuratJalan = false,
        paperSize = com.example.data.model.ReceiptPaperSize.THERMAL_80MM,
      )
    assertEquals(0x1B.toByte(), rawBytes[0])
    assertEquals(0x40.toByte(), rawBytes[1])
    assertEquals(
      com.example.data.model.ReceiptPaperSize.THERMAL_80MM,
      com.example.data.model.ReceiptPaperSize.fromName("THERMAL_80MM"),
    )
    assertEquals(
      com.example.data.model.ReceiptPaperSize.THERMAL_58MM,
      com.example.data.model.ReceiptPaperSize.fromName("UNKNOWN"),
    )
  }

  @Test
  fun retailUnitConversionHelpersRoundCleanly() {
    assertEquals(50.0, com.example.data.model.retailUnitsPerBase(0.02), 0.001)
    assertEquals(12.0, com.example.data.model.retailUnitsPerBase(0.0833), 0.001)
    assertEquals(4.0, com.example.data.model.retailUnitsPerBase(0.25), 0.001)
    assertEquals(0.02, com.example.data.model.conversionFactorFromUnitsPerBase(50.0), 0.0001)
  }

  @Test
  fun totalInventoryCostValueConservedAcrossRepack() {
    val baseProduct =
      com.example.data.local.ProductEntity(
        id = 1L,
        name = "Semen Tiga Roda 50kg",
        category = "Semen",
        baseUnit = "Sak",
        stockQuantity = 50.0,
        minStock = 10.0,
        costPricePerBaseUnit = 57_000L,
        sellPricePerBaseUnit = 65_000L,
        retailUnitName = "Kg",
        retailConversionFactor = 0.02,
        retailSellPrice = 1_500L,
        retailStockQuantity = 0.0,
      )
    val repackedProduct =
      baseProduct.copy(
        stockQuantity = 49.0,
        retailStockQuantity = 50.0,
      )

    val beforeValue = com.example.ui.viewmodel.StoreUiState(products = listOf(baseProduct)).totalInventoryCostValue
    val afterValue = com.example.ui.viewmodel.StoreUiState(products = listOf(repackedProduct)).totalInventoryCostValue

    assertEquals(2_850_000L, beforeValue)
    assertEquals(beforeValue, afterValue)
  }

  @Test
  fun productMappersIncludeRetailStockAndCartSeparatesBaseAndRetailPools() {
    val product =
      com.example.data.local.ProductEntity(
        id = 10L,
        name = "Semen Tiga Roda 50kg",
        category = "Semen",
        baseUnit = "Sak",
        stockQuantity = 48.0,
        minStock = 10.0,
        costPricePerBaseUnit = 57_000L,
        sellPricePerBaseUnit = 65_000L,
        retailUnitName = "Kg",
        retailConversionFactor = 0.02,
        retailSellPrice = 1_500L,
        retailStockQuantity = 100.0,
      )

    val createMap =
      com.example.data.remote.FirestoreMappers.productCreateMap(
        product = product,
        numericId = 10L,
        storeId = "store_1",
        ownerUid = "owner_1",
        memberUids = listOf("owner_1"),
      )
    assertEquals(100.0, createMap["retailStockQuantity"] as Double, 0.001)

    val updateMap =
      com.example.data.remote.FirestoreMappers.productOwnerUpdateMap(
        product = product,
        memberUids = listOf("owner_1"),
      )
    assertEquals(100.0, updateMap["retailStockQuantity"] as Double, 0.001)

    val retailCartItem =
      com.example.data.model.CartItem(
        productId = product.id,
        productName = product.name,
        category = product.category,
        unitName = "Kg",
        isRetailUnit = true,
        conversionFactor = 0.02,
        quantity = 5.0,
        unitPrice = 1_500L,
        unitCostPerBase = 57_000L,
        availableBaseStock = 48.0,
        availableRetailStock = 100.0,
      )
    assertEquals(7_500L, retailCartItem.subtotal)
    assertEquals(100.0, retailCartItem.availableRetailStock, 0.001)

    val retailTxItem =
      com.example.data.model.TransactionItem(
        productId = 10L,
        productName = "Semen Tiga Roda 50kg",
        unitName = "Kg",
        quantity = 5.0,
        baseUnitsDeducted = 0.1,
        unitPrice = 1_500L,
        unitCost = 5_700L,
        subtotal = 7_500L,
        isRetailUnit = true,
      )
    assertTrue(retailTxItem.isRetailUnit)
  }

  @Test
  fun minimumSellingPriceFallbacksAndFirestoreMappersWork() {
    val legacyProduct =
      com.example.data.local.ProductEntity(
        id = 20L,
        name = "Semen Gresik 40kg",
        category = "Semen",
        baseUnit = "Sak",
        stockQuantity = 10.0,
        minStock = 2.0,
        costPricePerBaseUnit = 50_000L,
        sellPricePerBaseUnit = 60_000L,
        minSellPricePerBaseUnit = 0L,
        retailUnitName = "Kg",
        retailConversionFactor = 0.025,
        retailSellPrice = 1_800L,
        minRetailSellPrice = 0L,
      )
    assertEquals(60_000L, legacyProduct.effectiveMinBaseSellPrice())
    assertEquals(1_800L, legacyProduct.effectiveMinRetailSellPrice())

    val bargainProduct =
      legacyProduct.copy(
        minSellPricePerBaseUnit = 56_000L,
        minRetailSellPrice = 1_600L,
      )
    assertEquals(56_000L, bargainProduct.effectiveMinBaseSellPrice())
    assertEquals(1_600L, bargainProduct.effectiveMinRetailSellPrice())

    val createMap =
      com.example.data.remote.FirestoreMappers.productCreateMap(
        product = bargainProduct,
        numericId = 20L,
        storeId = "store_1",
        ownerUid = "owner_1",
        memberUids = listOf("owner_1"),
      )
    assertEquals(56_000L, createMap["minSellPricePerBaseUnit"])
    assertEquals(1_600L, createMap["minRetailSellPrice"])

    val updateMap =
      com.example.data.remote.FirestoreMappers.productOwnerUpdateMap(
        product = bargainProduct,
        memberUids = listOf("owner_1"),
      )
    assertEquals(56_000L, updateMap["minSellPricePerBaseUnit"])
    assertEquals(1_600L, updateMap["minRetailSellPrice"])
  }

  @Test
  fun perItemBargainAndOverallDiscountStackCleanly() {
    val item1 =
      com.example.data.model.CartItem(
        productId = 1L,
        productName = "Semen Tiga Roda 50kg",
        category = "Semen",
        unitName = "Sak",
        isRetailUnit = false,
        conversionFactor = 1.0,
        quantity = 10.0,
        unitPrice = 62_000L, // bargained down from 65_000 to min 62_000 (-30_000 total)
        unitCostPerBase = 57_000L,
        availableBaseStock = 50.0,
        standardUnitPrice = 65_000L,
        minUnitPrice = 62_000L,
      )
    val item2 =
      com.example.data.model.CartItem(
        productId = 2L,
        productName = "Paku Kayu",
        category = "Alat Pertukangan",
        unitName = "Kg",
        isRetailUnit = false,
        conversionFactor = 1.0,
        quantity = 4.0,
        unitPrice = 21_000L, // bargained down from 22_000 (-4_000 total)
        unitCostPerBase = 16_000L,
        availableBaseStock = 20.0,
        standardUnitPrice = 22_000L,
        minUnitPrice = 20_000L,
      )

    val pelangganState =
      com.example.ui.viewmodel.StoreUiState(
        cart = listOf(item1, item2),
        customerType = CustomerType.PELANGGAN,
        manualDiscountAmount = 14_000L, // whole-order bargain on top
      )
    assertEquals(738_000L, pelangganState.cartStandardSubtotal)
    assertEquals(34_000L, pelangganState.totalItemBargainDiscount)
    assertEquals(704_000L, pelangganState.cartSubtotal)
    assertEquals(14_000L, pelangganState.cartDiscount)
    assertEquals(48_000L, pelangganState.totalCombinedSavings)
    assertEquals(690_000L, pelangganState.cartTotal)

    val contractorState =
      pelangganState.copy(
        customerType = CustomerType.KONTRAKTOR,
        manualDiscountAmount = 0L,
      )
    // 5% contractor discount applied to post-item-bargain subtotal (704_000 * 0.05 = 35_200)
    assertEquals(35_200L, contractorState.cartDiscount)
    assertEquals(668_800L, contractorState.cartTotal)

    val receiptTxItem =
      com.example.data.model.TransactionItem(
        productId = 1L,
        productName = "Semen Tiga Roda 50kg",
        unitName = "Sak",
        quantity = 5.0,
        baseUnitsDeducted = 5.0,
        unitPrice = 62_000L,
        unitCost = 57_000L,
        subtotal = 310_000L,
        standardUnitPrice = 65_000L,
      )
    assertEquals(3_000L, receiptTxItem.unitBargainDiscount)
    assertEquals(15_000L, receiptTxItem.lineBargainDiscount)
  }
}
