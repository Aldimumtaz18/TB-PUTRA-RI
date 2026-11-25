package com.example.data.model

enum class UserRole(val displayName: String, val badgeLabel: String) {
  OWNER("Pemilik (Owner)", "Pemilik"),
  CASHIER("Karyawan (Kasir)", "Kasir"),
}

data class EmployeeAccount(
  val id: String,
  val name: String,
  val username: String,
  val password: String,
  val phone: String = "",
)

enum class ReceiptPaperSize(
  val label: String,
  val subtitle: String,
  val widthMm: Int,
  val charColumns: Int,
  val cssWidth: String,
) {
  THERMAL_58MM(
    label = "58 mm",
    subtitle = "32 kolom · Portabel",
    widthMm = 58,
    charColumns = 32,
    cssWidth = "58mm",
  ),
  THERMAL_80MM(
    label = "80 mm",
    subtitle = "48 kolom · Meja",
    widthMm = 80,
    charColumns = 48,
    cssWidth = "80mm",
  );

  companion object {
    fun fromName(name: String?): ReceiptPaperSize =
      entries.firstOrNull { it.name == name } ?: THERMAL_58MM
  }
}

data class CategoryItem(
  val id: String,
  val name: String,
)

const val ALL_CATEGORY_LABEL = "Semua"

val DEFAULT_PRODUCT_CATEGORIES =
  listOf(
    "Semen",
    "Besi & Baja",
    "Pipa",
    "Pasir & Batu",
    "Cat",
    "Alat Pertukangan",
  )

enum class CustomerType(
  val label: String,
  val shortLabel: String,
  val discountRate: Double,
) {
  PELANGGAN("Pelanggan", "Harga Normal", 0.0),
  KONTRAKTOR("Kontraktor", "Diskon 5%", 0.05);

  companion object {
    fun fromStoredLabel(raw: String): CustomerType =
      when {
        raw.contains("Kontraktor", ignoreCase = true) || raw.contains("Tukang", ignoreCase = true) ->
          KONTRAKTOR
        else -> PELANGGAN
      }
  }
}

enum class FinanceDatePreset(val label: String) {
  SEMUA("Semua"),
  HARI_INI("Hari Ini"),
  BESOK("Besok"),
  KEMARIN("Kemarin"),
  MINGGU_INI("Minggu Ini"),
  BULAN_INI("Bulan Ini"),
  TAHUN_INI("Tahun Ini"),
  TANGGAL_PILIHAN("Pilih Tanggal"),
  RENTANG_TANGGAL("Rentang"),
}

data class FinanceDateFilter(
  val preset: FinanceDatePreset = FinanceDatePreset.SEMUA,
  val customStartDayMillis: Long? = null,
  val customEndDayMillis: Long? = null,
  val customLabel: String? = null,
)

data class SavedCustomerOption(
  val name: String,
  val phone: String,
  val customerType: CustomerType,
)

enum class PaymentMethod(val label: String) {
  TUNAI("Tunai"),
  TRANSFER_QRIS("Transfer/QRIS"),
  KASBON("Tempo / Kasbon"),
}

enum class ProductCategory(val label: String) {
  SEMUA("Semua"),
  SEMEN("Semen"),
  BESI_BAJA("Besi & Baja"),
  PIPA("Pipa"),
  PASIR_BATU("Pasir & Batu"),
  CAT("Cat"),
  ALAT("Alat Pertukangan"),
}

enum class ExpenseCategory(val label: String) {
  BBM("BBM Armada"),
  KULI("Kuli Bongkar"),
  LISTRIK("Listrik & Air"),
  KULAKAN("Kulakan Supplier"),
  LAINNYA("Operasional Lain"),
}

enum class AppTab(val label: String) {
  POS("Kasir"),
  STOCK("Stok"),
  FINANCE("Keuangan"),
  ACCOUNT("Akun"),
}

data class TransactionItem(
  val productId: Long,
  val productName: String,
  val unitName: String,
  val quantity: Double,
  val baseUnitsDeducted: Double,
  val unitPrice: Long,
  val unitCost: Long,
  val subtotal: Long,
  val isRetailUnit: Boolean = false,
  val standardUnitPrice: Long = unitPrice,
) {
  val unitBargainDiscount: Long
    get() = (standardUnitPrice - unitPrice).coerceAtLeast(0L)

  val lineBargainDiscount: Long
    get() = kotlin.math.round(quantity * unitBargainDiscount).toLong()
}

data class CartItem(
  val productId: Long,
  val productName: String,
  val category: String,
  val unitName: String,
  val isRetailUnit: Boolean,
  val conversionFactor: Double,
  val quantity: Double,
  val unitPrice: Long,
  val unitCostPerBase: Long,
  val availableBaseStock: Double,
  val availableRetailStock: Double = 0.0,
  val standardUnitPrice: Long = unitPrice,
  val minUnitPrice: Long = unitPrice,
) {
  val baseUnitsRequired: Double
    get() = quantity * conversionFactor

  val subtotal: Long
    get() = kotlin.math.round(quantity * unitPrice).toLong()

  val totalCost: Long
    get() = kotlin.math.round(baseUnitsRequired * unitCostPerBase).toLong()

  val effectiveMinUnitPrice: Long
    get() = minUnitPrice.coerceIn(0L, standardUnitPrice)

  val unitBargainDiscount: Long
    get() = (standardUnitPrice - unitPrice).coerceAtLeast(0L)

  val lineBargainDiscount: Long
    get() = kotlin.math.round(quantity * unitBargainDiscount).toLong()

  val standardSubtotal: Long
    get() = kotlin.math.round(quantity * standardUnitPrice).toLong()
}

data class KasbonPaymentRecord(
  val amount: Long,
  val paymentMethod: String,
  val timestamp: Long,
  val recordedBy: String,
)

fun retailUnitsPerBase(conversionFactor: Double): Double {
  if (conversionFactor <= 0.0) return 1.0
  val raw = 1.0 / conversionFactor
  val roundedInt = kotlin.math.round(raw)
  return if (roundedInt >= 1.0 && kotlin.math.abs(raw - roundedInt) <= 0.05) {
    roundedInt
  } else {
    kotlin.math.round(raw * 100.0) / 100.0
  }
}

fun conversionFactorFromUnitsPerBase(unitsPerBase: Double): Double {
  if (unitsPerBase <= 0.0) return 1.0
  return 1.0 / unitsPerBase
}

