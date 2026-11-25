package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.local.ExpenseEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.model.CategoryItem
import com.example.data.model.EmployeeAccount
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class FirestoreUserWorkspace(
  @DocumentId val docId: String = "",
  val userId: String = "",
  val defaultStoreId: String = "",
  val displayName: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FirestoreStoreWorkspace(
  @DocumentId val docId: String = "",
  val ownerUid: String = "",
  val memberUids: List<String> = emptyList(),
  val joinCode: String = "",
  val storeName: String = "TB PUTRA RI",
  val storeAddress: String = "Jl. Raya Pembangunan No. 88",
  val storePhone: String = "0812-3456-7890",
  val ownerName: String = "H. Putra",
  val cashierName: String = "Budi Santoso",
  val ownerUsername: String = "owner",
  val ownerPassword: String = "owner123",
  val cashierUsername: String = "kasir",
  val cashierPassword: String = "kasir123",
  val ownerPin: String = "1234",
  val cashierPin: String = "0000",
  val employeesInitialized: Boolean = false,
  val categoriesInitialized: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toStoreSettings(localSettings: StoreSettings): StoreSettings =
    localSettings.copy(
      storeName = storeName.ifBlank { "TB PUTRA RI" },
      ownerUsername = ownerUsername.ifBlank { "owner" },
      ownerPassword = ownerPassword.ifBlank { "owner123" },
      cashierUsername = cashierUsername.ifBlank { "kasir" },
      cashierPassword = cashierPassword.ifBlank { "kasir123" },
      ownerPin = ownerPin.ifBlank { "1234" },
      cashierPin = cashierPin.ifBlank { "0000" },
      ownerName = ownerName.ifBlank { "H. Putra" },
      cashierName = cashierName.ifBlank { "Budi Santoso" },
      storeAddress = storeAddress.ifBlank { "Jl. Raya Pembangunan No. 88" },
      storePhone = storePhone.ifBlank { "0812-3456-7890" },
      employeesInitialized = employeesInitialized,
      categoriesInitialized = categoriesInitialized,
      isSeeded = true,
    )
}

object FirestoreMappers {
  fun employeeDocId(numericId: Long): String = "emp_$numericId"

  fun categoryDocId(numericId: Long): String = "cat_$numericId"

  fun productDocId(numericId: Long): String = "prod_$numericId"

  fun transactionDocId(numericId: Long): String = "tx_$numericId"

  fun expenseDocId(numericId: Long): String = "exp_$numericId"

  fun productCreateMap(
    product: ProductEntity,
    numericId: Long,
    storeId: String,
    ownerUid: String,
    memberUids: List<String>,
  ): Map<String, Any> =
    buildMap<String, Any?> {
      put("storeId", storeId)
      put("ownerUid", ownerUid)
      put("memberUids", memberUids)
      put("clientNumericId", numericId)
      put("name", product.name.trim().take(120))
      put("category", product.category.trim().take(60))
      put("baseUnit", product.baseUnit.trim().take(30))
      put("stockQuantity", product.stockQuantity.coerceAtLeast(0.0))
      put("minStock", product.minStock.coerceAtLeast(0.0))
      put("costPricePerBaseUnit", product.costPricePerBaseUnit.coerceAtLeast(0L))
      put("sellPricePerBaseUnit", product.sellPricePerBaseUnit.coerceAtLeast(0L))
      put("minSellPricePerBaseUnit", product.effectiveMinBaseSellPrice())
      val cleanRetail = product.retailUnitName?.trim()?.takeIf { it.isNotEmpty() }?.take(30)
      if (cleanRetail != null) {
        put("retailUnitName", cleanRetail)
      }
      put(
        "retailConversionFactor",
        if (product.retailConversionFactor > 0.0) product.retailConversionFactor else 1.0,
      )
      put("retailSellPrice", product.retailSellPrice.coerceAtLeast(0L))
      put(
        "minRetailSellPrice",
        if (cleanRetail != null) product.effectiveMinRetailSellPrice() else 0L,
      )
      put("retailStockQuantity", product.retailStockQuantity.coerceAtLeast(0.0))
      put("updatedAtMillis", product.updatedAt.coerceAtLeast(1L))
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }

  fun productOwnerUpdateMap(
    product: ProductEntity,
    memberUids: List<String>,
  ): Map<String, Any> =
    buildMap {
      put("memberUids", memberUids)
      put("name", product.name.trim().take(120))
      put("category", product.category.trim().take(60))
      put("baseUnit", product.baseUnit.trim().take(30))
      put("stockQuantity", product.stockQuantity.coerceAtLeast(0.0))
      put("minStock", product.minStock.coerceAtLeast(0.0))
      put("costPricePerBaseUnit", product.costPricePerBaseUnit.coerceAtLeast(0L))
      put("sellPricePerBaseUnit", product.sellPricePerBaseUnit.coerceAtLeast(0L))
      put("minSellPricePerBaseUnit", product.effectiveMinBaseSellPrice())
      val cleanRetail = product.retailUnitName?.trim()?.takeIf { it.isNotEmpty() }?.take(30)
      if (cleanRetail != null) {
        put("retailUnitName", cleanRetail)
      } else {
        put("retailUnitName", FieldValue.delete())
      }
      put(
        "retailConversionFactor",
        if (product.retailConversionFactor > 0.0) product.retailConversionFactor else 1.0,
      )
      put("retailSellPrice", product.retailSellPrice.coerceAtLeast(0L))
      put(
        "minRetailSellPrice",
        if (cleanRetail != null) product.effectiveMinRetailSellPrice() else 0L,
      )
      put("retailStockQuantity", product.retailStockQuantity.coerceAtLeast(0.0))
      put("updatedAtMillis", System.currentTimeMillis())
      put("updatedAt", FieldValue.serverTimestamp())
    }

  fun transactionCreateMap(
    tx: TransactionEntity,
    numericId: Long,
    storeId: String,
    ownerUid: String,
    recordedByUid: String,
    memberUids: List<String>,
  ): Map<String, Any> =
    buildMap<String, Any?> {
      put("storeId", storeId)
      put("ownerUid", ownerUid)
      put("recordedByUid", recordedByUid)
      put("memberUids", memberUids)
      put("clientNumericId", numericId)
      put("invoiceNumber", tx.invoiceNumber.take(60))
      put("timestampMillis", tx.timestamp.coerceAtLeast(1L))
      put("cashierName", tx.cashierName.ifBlank { "Kasir" }.take(80))
      put("cashierRole", if (tx.cashierRole == "OWNER") "OWNER" else "CASHIER")
      put("customerType", tx.customerType.take(40))
      put("customerName", tx.customerName.ifBlank { tx.customerType }.take(100))
      put("customerPhone", tx.customerPhone.take(40))
      put("paymentMethod", tx.paymentMethod.take(40))
      put("subtotal", tx.subtotal.coerceAtLeast(0L))
      put("discountAmount", tx.discountAmount.coerceAtLeast(0L))
      put("totalAmount", tx.totalAmount.coerceAtLeast(0L))
      put("totalCost", tx.totalCost.coerceAtLeast(0L))
      put("amountPaid", tx.amountPaid.coerceAtLeast(0L))
      put("changeAmount", tx.changeAmount.coerceAtLeast(0L))
      put("paidStatus", tx.isPaid)
      if (tx.paidAt != null && tx.paidAt > 0L) {
        put("paidAtMillis", tx.paidAt)
      }
      put("itemsJson", tx.itemsJson.ifBlank { "[]" }.take(50_000))
      if (tx.paymentsJson.isNotBlank()) {
        put("paymentsJson", tx.paymentsJson.take(20_000))
      }
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }

  fun expenseCreateMap(
    exp: ExpenseEntity,
    numericId: Long,
    storeId: String,
    ownerUid: String,
    recordedByUid: String,
    memberUids: List<String>,
  ): Map<String, Any> =
    mapOf(
      "storeId" to storeId,
      "ownerUid" to ownerUid,
      "recordedByUid" to recordedByUid,
      "memberUids" to memberUids,
      "clientNumericId" to numericId,
      "title" to exp.title.ifBlank { "Biaya" }.take(120),
      "category" to exp.category.take(60),
      "amount" to exp.amount.coerceAtLeast(1L),
      "notes" to exp.notes.take(300),
      "recordedBy" to exp.recordedBy.ifBlank { "Kasir" }.take(80),
      "recordedRole" to if (exp.recordedRole == "OWNER") "OWNER" else "CASHIER",
      "timestampMillis" to exp.timestamp.coerceAtLeast(1L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun productFromSnapshot(doc: DocumentSnapshot): ProductEntity? {
    val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val numericId = (data["clientNumericId"] as? Number)?.toLong() ?: return null
    val sellBase = ((data["sellPricePerBaseUnit"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
    val rawMinBase = (data["minSellPricePerBaseUnit"] as? Number)?.toLong()
    val minSellBase =
      if (rawMinBase != null && rawMinBase in 1L..sellBase) rawMinBase else sellBase
    val sellRetail = ((data["retailSellPrice"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
    val rawMinRetail = (data["minRetailSellPrice"] as? Number)?.toLong()
    val minSellRetail =
      if (rawMinRetail != null && rawMinRetail in 1L..sellRetail) rawMinRetail else sellRetail
    return ProductEntity(
      id = numericId,
      name = data["name"] as? String ?: return null,
      category = data["category"] as? String ?: "",
      baseUnit = data["baseUnit"] as? String ?: "Pcs",
      stockQuantity = (data["stockQuantity"] as? Number)?.toDouble() ?: 0.0,
      minStock = (data["minStock"] as? Number)?.toDouble() ?: 0.0,
      costPricePerBaseUnit = (data["costPricePerBaseUnit"] as? Number)?.toLong() ?: 0L,
      sellPricePerBaseUnit = sellBase,
      minSellPricePerBaseUnit = minSellBase,
      retailUnitName = data["retailUnitName"] as? String,
      retailConversionFactor = (data["retailConversionFactor"] as? Number)?.toDouble() ?: 1.0,
      retailSellPrice = sellRetail,
      minRetailSellPrice = minSellRetail,
      retailStockQuantity = (data["retailStockQuantity"] as? Number)?.toDouble() ?: 0.0,
      updatedAt = (data["updatedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    )
  }

  fun transactionFromSnapshot(doc: DocumentSnapshot): TransactionEntity? {
    val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val numericId = (data["clientNumericId"] as? Number)?.toLong() ?: return null
    return TransactionEntity(
      id = numericId,
      invoiceNumber = data["invoiceNumber"] as? String ?: return null,
      timestamp = (data["timestampMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
      cashierName = data["cashierName"] as? String ?: "",
      cashierRole = data["cashierRole"] as? String ?: "CASHIER",
      customerType = data["customerType"] as? String ?: "Pelanggan",
      customerName = data["customerName"] as? String ?: "Pelanggan",
      customerPhone = data["customerPhone"] as? String ?: "",
      paymentMethod = data["paymentMethod"] as? String ?: "Tunai",
      subtotal = (data["subtotal"] as? Number)?.toLong() ?: 0L,
      discountAmount = (data["discountAmount"] as? Number)?.toLong() ?: 0L,
      totalAmount = (data["totalAmount"] as? Number)?.toLong() ?: 0L,
      totalCost = (data["totalCost"] as? Number)?.toLong() ?: 0L,
      amountPaid = (data["amountPaid"] as? Number)?.toLong() ?: 0L,
      changeAmount = (data["changeAmount"] as? Number)?.toLong() ?: 0L,
      isPaid = data["paidStatus"] as? Boolean ?: false,
      paidAt = (data["paidAtMillis"] as? Number)?.toLong(),
      itemsJson = data["itemsJson"] as? String ?: "[]",
      paymentsJson = data["paymentsJson"] as? String ?: "",
    )
  }

  fun expenseFromSnapshot(doc: DocumentSnapshot): ExpenseEntity? {
    val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val numericId = (data["clientNumericId"] as? Number)?.toLong() ?: return null
    return ExpenseEntity(
      id = numericId,
      title = data["title"] as? String ?: return null,
      category = data["category"] as? String ?: "",
      amount = (data["amount"] as? Number)?.toLong() ?: 0L,
      notes = data["notes"] as? String ?: "",
      recordedBy = data["recordedBy"] as? String ?: "",
      recordedRole = data["recordedRole"] as? String ?: "CASHIER",
      timestamp = (data["timestampMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    )
  }

  @Suppress("UNCHECKED_CAST")
  fun memberUidsFromSnapshot(doc: DocumentSnapshot): List<String> =
    (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()

  fun employeeCreateMap(
    name: String,
    username: String,
    password: String,
    storeId: String,
    ownerUid: String,
    memberUids: List<String>,
    phone: String = "",
  ): Map<String, Any> =
    mapOf(
      "storeId" to storeId,
      "ownerUid" to ownerUid,
      "memberUids" to memberUids,
      "name" to name.trim().take(80),
      "username" to username.trim().take(40),
      "password" to password.take(64),
      "phone" to phone.trim().take(40),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun employeeUpdateMap(
    name: String,
    username: String,
    password: String,
    memberUids: List<String>,
    phone: String = "",
  ): Map<String, Any> =
    mapOf(
      "memberUids" to memberUids,
      "name" to name.trim().take(80),
      "username" to username.trim().take(40),
      "password" to password.take(64),
      "phone" to phone.trim().take(40),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun employeeSelfUpdateMap(
    name: String,
    username: String,
    password: String,
    phone: String = "",
  ): Map<String, Any> =
    mapOf(
      "name" to name.trim().take(80),
      "username" to username.trim().take(40),
      "password" to password.take(64),
      "phone" to phone.trim().take(40),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun employeeFromSnapshot(doc: DocumentSnapshot): EmployeeAccount? {
    val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val username = (data["username"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val password = (data["password"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val phone = (data["phone"] as? String)?.trim() ?: ""
    return EmployeeAccount(
      id = doc.id,
      name = name,
      username = username,
      password = password,
      phone = phone,
    )
  }

  fun categoryCreateMap(
    name: String,
    storeId: String,
    ownerUid: String,
    memberUids: List<String>,
  ): Map<String, Any> =
    mapOf(
      "storeId" to storeId,
      "ownerUid" to ownerUid,
      "memberUids" to memberUids,
      "name" to name.trim().take(60),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun categoryUpdateMap(
    name: String,
    memberUids: List<String>,
  ): Map<String, Any> =
    mapOf(
      "memberUids" to memberUids,
      "name" to name.trim().take(60),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun categoryFromSnapshot(doc: DocumentSnapshot): CategoryItem? {
    val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val name = (data["name"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return CategoryItem(
      id = doc.id,
      name = name,
    )
  }
}
