package com.example.data.repository

import com.example.data.local.ExpenseEntity
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemCodec
import com.example.data.model.CustomerType
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionItem
import com.example.data.model.UserRole

object DummyDataSeeder {
  fun initialProducts(): List<ProductEntity> =
    listOf(
      ProductEntity(
        name = "Semen Tiga Roda 50kg",
        category = ProductCategory.SEMEN.label,
        baseUnit = "Sak",
        stockQuantity = 84.0,
        minStock = 20.0,
        costPricePerBaseUnit = 57_000L,
        sellPricePerBaseUnit = 65_000L,
        minSellPricePerBaseUnit = 62_000L,
        retailUnitName = "Kg",
        retailConversionFactor = 0.02,
        retailSellPrice = 1_500L,
        minRetailSellPrice = 1_300L,
        retailStockQuantity = 50.0,
      ),
      ProductEntity(
        name = "Pipa PVC Wavin 3 Inch (4m)",
        category = ProductCategory.PIPA.label,
        baseUnit = "Batang",
        stockQuantity = 31.0,
        minStock = 10.0,
        costPricePerBaseUnit = 39_000L,
        sellPricePerBaseUnit = 48_000L,
        minSellPricePerBaseUnit = 45_000L,
        retailUnitName = "Meter",
        retailConversionFactor = 0.25,
        retailSellPrice = 13_000L,
        minRetailSellPrice = 12_000L,
        retailStockQuantity = 4.0,
      ),
      ProductEntity(
        name = "Pasir Hitam Cor Super",
        category = ProductCategory.PASIR_BATU.label,
        baseUnit = "Kubik",
        stockQuantity = 6.0,
        minStock = 3.0,
        costPricePerBaseUnit = 220_000L,
        sellPricePerBaseUnit = 280_000L,
        minSellPricePerBaseUnit = 265_000L,
        retailUnitName = "Angkong",
        retailConversionFactor = 0.125,
        retailSellPrice = 35_000L,
        minRetailSellPrice = 33_000L,
        retailStockQuantity = 4.0,
      ),
      ProductEntity(
        name = "Besi Beton Polos 10mm SNI",
        category = ProductCategory.BESI_BAJA.label,
        baseUnit = "Batang",
        stockQuantity = 14.0,
        minStock = 25.0, // Low stock warning demo
        costPricePerBaseUnit = 64_000L,
        sellPricePerBaseUnit = 76_000L,
        minSellPricePerBaseUnit = 72_000L,
        retailUnitName = "Meter",
        retailConversionFactor = 0.0833,
        retailSellPrice = 7_000L,
        minRetailSellPrice = 6_500L,
        retailStockQuantity = 0.0,
      ),
      ProductEntity(
        name = "Cat Tembok Dulux Catylac 5kg Putih",
        category = ProductCategory.CAT.label,
        baseUnit = "Galon",
        stockQuantity = 18.0,
        minStock = 5.0,
        costPricePerBaseUnit = 128_000L,
        sellPricePerBaseUnit = 155_000L,
        minSellPricePerBaseUnit = 148_000L,
        retailUnitName = "Kg",
        retailConversionFactor = 0.2,
        retailSellPrice = 34_000L,
        minRetailSellPrice = 32_000L,
        retailStockQuantity = 0.0,
      ),
      ProductEntity(
        name = "Batu Split / Koral Cor 1/2",
        category = ProductCategory.PASIR_BATU.label,
        baseUnit = "Kubik",
        stockQuantity = 2.0,
        minStock = 3.0, // Low stock warning demo
        costPricePerBaseUnit = 250_000L,
        sellPricePerBaseUnit = 320_000L,
        minSellPricePerBaseUnit = 300_000L,
        retailUnitName = "Angkong",
        retailConversionFactor = 0.125,
        retailSellPrice = 42_000L,
        minRetailSellPrice = 39_000L,
        retailStockQuantity = 0.0,
      ),
      ProductEntity(
        name = "Paku Kayu Campur 5cm - 10cm",
        category = ProductCategory.ALAT.label,
        baseUnit = "Kg",
        stockQuantity = 39.0,
        minStock = 10.0,
        costPricePerBaseUnit = 16_000L,
        sellPricePerBaseUnit = 22_000L,
        minSellPricePerBaseUnit = 20_000L,
        retailUnitName = "Ons",
        retailConversionFactor = 0.1,
        retailSellPrice = 2_500L,
        minRetailSellPrice = 2_200L,
        retailStockQuantity = 10.0,
      ),
      ProductEntity(
        name = "Kuas Cat Eterna 4 Inch",
        category = ProductCategory.ALAT.label,
        baseUnit = "Pcs",
        stockQuantity = 25.0,
        minStock = 8.0,
        costPricePerBaseUnit = 14_000L,
        sellPricePerBaseUnit = 20_000L,
        minSellPricePerBaseUnit = 20_000L,
        retailUnitName = null,
        retailConversionFactor = 1.0,
        retailSellPrice = 0L,
        minRetailSellPrice = 0L,
      ),
    )

  fun initialTransactions(now: Long = System.currentTimeMillis()): List<TransactionEntity> {
    val hour = 3_600_000L
    val items1 =
      listOf(
        TransactionItem(
          productId = 1,
          productName = "Semen Tiga Roda 50kg",
          unitName = "Sak",
          quantity = 10.0,
          baseUnitsDeducted = 10.0,
          unitPrice = 65_000L,
          unitCost = 57_000L,
          subtotal = 650_000L,
        ),
        TransactionItem(
          productId = 3,
          productName = "Pasir Hitam Cor Super",
          unitName = "Kubik",
          quantity = 1.0,
          baseUnitsDeducted = 1.0,
          unitPrice = 280_000L,
          unitCost = 220_000L,
          subtotal = 280_000L,
        ),
      )
    val sub1 = 930_000L
    val disc1 = 46_500L // Kontraktor 5%

    val items2 =
      listOf(
        TransactionItem(
          productId = 1,
          productName = "Semen Tiga Roda 50kg",
          unitName = "Kg",
          quantity = 10.0,
          baseUnitsDeducted = 0.2,
          unitPrice = 1_500L,
          unitCost = 1_140L,
          subtotal = 15_000L,
          isRetailUnit = true,
        ),
        TransactionItem(
          productId = 2,
          productName = "Pipa PVC Wavin 3 Inch (4m)",
          unitName = "Meter",
          quantity = 2.0,
          baseUnitsDeducted = 0.5,
          unitPrice = 13_000L,
          unitCost = 9_750L,
          subtotal = 26_000L,
          isRetailUnit = true,
        ),
      )

    val items3 =
      listOf(
        TransactionItem(
          productId = 4,
          productName = "Besi Beton Polos 10mm SNI",
          unitName = "Batang",
          quantity = 15.0,
          baseUnitsDeducted = 15.0,
          unitPrice = 76_000L,
          unitCost = 64_000L,
          subtotal = 1_140_000L,
        ),
        TransactionItem(
          productId = 5,
          productName = "Cat Tembok Dulux Catylac 5kg Putih",
          unitName = "Galon",
          quantity = 2.0,
          baseUnitsDeducted = 2.0,
          unitPrice = 155_000L,
          unitCost = 128_000L,
          subtotal = 310_000L,
        ),
      )
    val sub3 = 1_450_000L
    val disc3 = 72_500L

    val items4 =
      listOf(
        TransactionItem(
          productId = 3,
          productName = "Pasir Hitam Cor Super",
          unitName = "Angkong",
          quantity = 4.0,
          baseUnitsDeducted = 0.5,
          unitPrice = 35_000L,
          unitCost = 27_500L,
          subtotal = 140_000L,
          isRetailUnit = true,
        ),
        TransactionItem(
          productId = 7,
          productName = "Paku Kayu Campur 5cm - 10cm",
          unitName = "Kg",
          quantity = 3.0,
          baseUnitsDeducted = 3.0,
          unitPrice = 22_000L,
          unitCost = 16_000L,
          subtotal = 66_000L,
        ),
      )

    return listOf(
      TransactionEntity(
        invoiceNumber = "INV-PR-2401",
        timestamp = now - (26 * hour),
        cashierName = "H. Putra",
        cashierRole = UserRole.OWNER.name,
        customerType = CustomerType.KONTRAKTOR.label,
        customerName = "Pak Mandor Slamet",
        customerPhone = "081398765432",
        paymentMethod = PaymentMethod.KASBON.label,
        subtotal = sub1,
        discountAmount = disc1,
        totalAmount = sub1 - disc1,
        totalCost = 790_000L,
        amountPaid = 0L,
        changeAmount = 0L,
        isPaid = false,
        itemsJson = TransactionItemCodec.encode(items1),
      ),
      TransactionEntity(
        invoiceNumber = "INV-PR-2402",
        timestamp = now - (18 * hour),
        cashierName = "Budi Santoso",
        cashierRole = UserRole.CASHIER.name,
        customerType = CustomerType.PELANGGAN.label,
        customerName = "Ibu Sari",
        customerPhone = "",
        paymentMethod = PaymentMethod.TUNAI.label,
        subtotal = 41_000L,
        discountAmount = 0L,
        totalAmount = 41_000L,
        totalCost = 30_900L,
        amountPaid = 50_000L,
        changeAmount = 9_000L,
        isPaid = true,
        paidAt = now - (18 * hour),
        itemsJson = TransactionItemCodec.encode(items2),
      ),
      TransactionEntity(
        invoiceNumber = "INV-PR-2403",
        timestamp = now - (6 * hour),
        cashierName = "Budi Santoso",
        cashierRole = UserRole.CASHIER.name,
        customerType = CustomerType.KONTRAKTOR.label,
        customerName = "CV Karya Jaya (Mas Hendra)",
        customerPhone = "085211223344",
        paymentMethod = PaymentMethod.KASBON.label,
        subtotal = sub3,
        discountAmount = disc3,
        totalAmount = sub3 - disc3,
        totalCost = 1_216_000L,
        amountPaid = 0L,
        changeAmount = 0L,
        isPaid = false,
        itemsJson = TransactionItemCodec.encode(items3),
      ),
      TransactionEntity(
        invoiceNumber = "INV-PR-2404",
        timestamp = now - (2 * hour),
        cashierName = "Budi Santoso",
        cashierRole = UserRole.CASHIER.name,
        customerType = CustomerType.PELANGGAN.label,
        customerName = "Pak Joko",
        customerPhone = "",
        paymentMethod = PaymentMethod.TRANSFER_QRIS.label,
        subtotal = 206_000L,
        discountAmount = 0L,
        totalAmount = 206_000L,
        totalCost = 158_000L,
        amountPaid = 206_000L,
        changeAmount = 0L,
        isPaid = true,
        paidAt = now - (2 * hour),
        itemsJson = TransactionItemCodec.encode(items4),
      ),
    )
  }

  fun initialExpenses(now: Long = System.currentTimeMillis()): List<ExpenseEntity> {
    val hour = 3_600_000L
    return listOf(
      ExpenseEntity(
        title = "Isi Solar Pikap L300",
        category = ExpenseCategory.BBM.label,
        amount = 100_000L,
        notes = "Antar pesanan pasir & semen blok C",
        recordedBy = "Budi Santoso",
        recordedRole = UserRole.CASHIER.name,
        timestamp = now - (20 * hour),
      ),
      ExpenseEntity(
        title = "Upah Kuli Bongkar Semen",
        category = ExpenseCategory.KULI.label,
        amount = 75_000L,
        notes = "Bongkar 1 DO semen 100 sak",
        recordedBy = "Budi Santoso",
        recordedRole = UserRole.CASHIER.name,
        timestamp = now - (5 * hour),
      ),
      ExpenseEntity(
        title = "Token Listrik Toko & Gudang",
        category = ExpenseCategory.LISTRIK.label,
        amount = 150_000L,
        notes = "Pembelian token prabayar",
        recordedBy = "H. Putra",
        recordedRole = UserRole.OWNER.name,
        timestamp = now - (28 * hour),
      ),
    )
  }
}
