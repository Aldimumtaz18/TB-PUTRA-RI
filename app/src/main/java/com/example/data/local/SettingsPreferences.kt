package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ReceiptPaperSize
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "tb_putra_ri_prefs")

data class StoreSettings(
  val storeName: String = "TB PUTRA RI",
  val ownerUsername: String = "owner",
  val ownerPassword: String = "owner123",
  val cashierUsername: String = "kasir",
  val cashierPassword: String = "kasir123",
  val ownerPin: String = "1234",
  val cashierPin: String = "0000",
  val ownerName: String = "H. Putra",
  val cashierName: String = "Budi Santoso",
  val storeAddress: String = "Jl. Raya Pembangunan No. 88",
  val storePhone: String = "0812-3456-7890",
  val activeRole: UserRole? = null,
  val activeEmployeeId: String? = null,
  val activeEmployeeName: String = "",
  val employeesInitialized: Boolean = false,
  val categoriesInitialized: Boolean = false,
  val isSeeded: Boolean = false,
  val thermalPaperSize: ReceiptPaperSize = ReceiptPaperSize.THERMAL_58MM,
  val savedPrinterAddress: String = "",
  val savedPrinterName: String = "",
)

class SettingsPreferences(private val context: Context) {
  private object Keys {
    val OWNER_USERNAME = stringPreferencesKey("owner_username")
    val OWNER_PASSWORD = stringPreferencesKey("owner_password")
    val CASHIER_USERNAME = stringPreferencesKey("cashier_username")
    val CASHIER_PASSWORD = stringPreferencesKey("cashier_password")
    val OWNER_PIN = stringPreferencesKey("owner_pin")
    val CASHIER_PIN = stringPreferencesKey("cashier_pin")
    val OWNER_NAME = stringPreferencesKey("owner_name")
    val CASHIER_NAME = stringPreferencesKey("cashier_name")
    val STORE_ADDRESS = stringPreferencesKey("store_address")
    val STORE_PHONE = stringPreferencesKey("store_phone")
    val ACTIVE_ROLE = stringPreferencesKey("active_role")
    val ACTIVE_EMPLOYEE_ID = stringPreferencesKey("active_employee_id")
    val ACTIVE_EMPLOYEE_NAME = stringPreferencesKey("active_employee_name")
    val IS_SEEDED = booleanPreferencesKey("is_seeded")
    val THERMAL_PAPER_SIZE = stringPreferencesKey("thermal_paper_size")
    val SAVED_PRINTER_ADDRESS = stringPreferencesKey("saved_printer_address")
    val SAVED_PRINTER_NAME = stringPreferencesKey("saved_printer_name")
  }

  val settingsFlow: Flow<StoreSettings> =
    context.settingsDataStore.data.map { prefs ->
      val roleRaw = prefs[Keys.ACTIVE_ROLE]
      val role =
        when (roleRaw) {
          UserRole.OWNER.name -> UserRole.OWNER
          UserRole.CASHIER.name -> UserRole.CASHIER
          else -> null
        }
      StoreSettings(
        ownerUsername = prefs[Keys.OWNER_USERNAME] ?: "owner",
        ownerPassword = prefs[Keys.OWNER_PASSWORD] ?: "owner123",
        cashierUsername = prefs[Keys.CASHIER_USERNAME] ?: "kasir",
        cashierPassword = prefs[Keys.CASHIER_PASSWORD] ?: "kasir123",
        ownerPin = prefs[Keys.OWNER_PIN] ?: "1234",
        cashierPin = prefs[Keys.CASHIER_PIN] ?: "0000",
        ownerName = prefs[Keys.OWNER_NAME] ?: "H. Putra",
        cashierName = prefs[Keys.CASHIER_NAME] ?: "Budi Santoso",
        storeAddress = prefs[Keys.STORE_ADDRESS] ?: "Jl. Raya Pembangunan No. 88",
        storePhone = prefs[Keys.STORE_PHONE] ?: "0812-3456-7890",
        activeRole = role,
        activeEmployeeId = prefs[Keys.ACTIVE_EMPLOYEE_ID],
        activeEmployeeName = prefs[Keys.ACTIVE_EMPLOYEE_NAME] ?: "",
        isSeeded = prefs[Keys.IS_SEEDED] ?: false,
        thermalPaperSize = ReceiptPaperSize.fromName(prefs[Keys.THERMAL_PAPER_SIZE]),
        savedPrinterAddress = prefs[Keys.SAVED_PRINTER_ADDRESS] ?: "",
        savedPrinterName = prefs[Keys.SAVED_PRINTER_NAME] ?: "",
      )
    }

  suspend fun setActiveSession(
    role: UserRole?,
    employeeId: String? = null,
    employeeName: String? = null,
  ) {
    context.settingsDataStore.edit { prefs ->
      if (role == null) {
        prefs.remove(Keys.ACTIVE_ROLE)
        prefs.remove(Keys.ACTIVE_EMPLOYEE_ID)
        prefs.remove(Keys.ACTIVE_EMPLOYEE_NAME)
      } else {
        prefs[Keys.ACTIVE_ROLE] = role.name
        if (role == UserRole.CASHIER && !employeeId.isNullOrBlank()) {
          prefs[Keys.ACTIVE_EMPLOYEE_ID] = employeeId
          prefs[Keys.ACTIVE_EMPLOYEE_NAME] = employeeName.orEmpty()
        } else {
          prefs.remove(Keys.ACTIVE_EMPLOYEE_ID)
          prefs.remove(Keys.ACTIVE_EMPLOYEE_NAME)
        }
      }
    }
  }

  suspend fun setActiveRole(role: UserRole?) = setActiveSession(role)

  suspend fun markSeeded() {
    context.settingsDataStore.edit { prefs -> prefs[Keys.IS_SEEDED] = true }
  }

  suspend fun updateOwnerAccountSettings(
    ownerUsername: String,
    ownerPassword: String,
    ownerName: String,
    storeAddress: String,
    storePhone: String,
  ) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.OWNER_USERNAME] = ownerUsername
      prefs[Keys.OWNER_PASSWORD] = ownerPassword
      prefs[Keys.OWNER_NAME] = ownerName
      prefs[Keys.STORE_ADDRESS] = storeAddress
      prefs[Keys.STORE_PHONE] = storePhone
    }
  }

  suspend fun saveThermalPrintPreferences(
    paperSize: ReceiptPaperSize,
    printerAddress: String? = null,
    printerName: String? = null,
  ) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.THERMAL_PAPER_SIZE] = paperSize.name
      if (printerAddress != null) {
        prefs[Keys.SAVED_PRINTER_ADDRESS] = printerAddress
      }
      if (printerName != null) {
        prefs[Keys.SAVED_PRINTER_NAME] = printerName
      }
    }
  }
}
