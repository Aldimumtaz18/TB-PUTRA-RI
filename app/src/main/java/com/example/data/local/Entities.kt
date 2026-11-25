package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val category: String,
  val baseUnit: String,
  val stockQuantity: Double,
  val minStock: Double,
  val costPricePerBaseUnit: Long,
  val sellPricePerBaseUnit: Long,
  val minSellPricePerBaseUnit: Long = 0L,
  val retailUnitName: String? = null,
  val retailConversionFactor: Double = 1.0,
  val retailSellPrice: Long = 0L,
  val minRetailSellPrice: Long = 0L,
  val retailStockQuantity: Double = 0.0,
  val updatedAt: Long = System.currentTimeMillis(),
) {
  fun effectiveMinBaseSellPrice(): Long =
    if (minSellPricePerBaseUnit in 1L..sellPricePerBaseUnit) minSellPricePerBaseUnit
    else sellPricePerBaseUnit.coerceAtLeast(0L)

  fun effectiveMinRetailSellPrice(): Long =
    if (minRetailSellPrice in 1L..retailSellPrice) minRetailSellPrice
    else retailSellPrice.coerceAtLeast(0L)
}

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val invoiceNumber: String,
  val timestamp: Long,
  val cashierName: String,
  val cashierRole: String,
  val customerType: String,
  val customerName: String,
  val customerPhone: String,
  val paymentMethod: String,
  val subtotal: Long,
  val discountAmount: Long,
  val totalAmount: Long,
  val totalCost: Long,
  val amountPaid: Long,
  val changeAmount: Long,
  val isPaid: Boolean,
  val paidAt: Long? = null,
  val itemsJson: String,
  val paymentsJson: String = "",
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val category: String,
  val amount: Long,
  val notes: String,
  val recordedBy: String,
  val recordedRole: String,
  val timestamp: Long,
)
