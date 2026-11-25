package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.ExpenseEntity
import com.example.data.local.KasbonPaymentCodec
import com.example.data.local.ProductEntity
import com.example.data.local.SettingsPreferences
import com.example.data.local.StoreSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemCodec
import com.example.data.model.CartItem
import com.example.data.model.CategoryItem
import com.example.data.model.CustomerType
import com.example.data.model.DEFAULT_PRODUCT_CATEGORIES
import com.example.data.model.EmployeeAccount
import com.example.data.model.ExpenseCategory
import com.example.data.model.KasbonPaymentRecord
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionItem
import com.example.data.model.UserRole
import com.example.data.model.retailUnitsPerBase
import com.example.data.remote.FirestoreMappers
import com.example.data.remote.FirestoreStoreWorkspace
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.userFriendlyFirestoreMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class CloudSyncInfo(
  val storeId: String = "",
  val joinCode: String = "",
  val ownerUid: String = "",
  val memberCount: Int = 1,
  val isOwnerOfWorkspace: Boolean = true,
  val isSyncing: Boolean = true,
  val cloudError: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StoreRepository(
  private val context: Context,
  private val authUid: String,
  private val displayName: String,
  private val prefs: SettingsPreferences,
) {
  private val db: FirebaseFirestore =
    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))

  private val idSequence =
    AtomicLong(System.currentTimeMillis() * 100L + Random.nextInt(10, 99).toLong())

  private val _cloudSyncState = MutableStateFlow(CloudSyncInfo())
  val cloudSyncState: StateFlow<CloudSyncInfo> = _cloudSyncState.asStateFlow()

  private fun nextNumericId(): Long {
    val candidate = System.currentTimeMillis() * 100L + Random.nextInt(10, 99).toLong()
    return idSequence.updateAndGet { prev -> if (candidate > prev) candidate else prev + 1L }
  }

  private val activeStoreIdFlow: Flow<String?> =
    db.collection("users")
      .document(authUid)
      .snapshots()
      .map { snap -> snap.getString("defaultStoreId")?.takeIf { it.isNotBlank() } }
      .catch { error ->
        val fsError = unwrapFirestoreException(error)
        if (fsError != null) {
          handleFirestoreError(fsError, OperationType.GET, "users/$authUid")
          _cloudSyncState.update {
            it.copy(
              isSyncing = false,
              cloudError = userFriendlyFirestoreMessage(fsError, "Gagal memuat profil toko."),
            )
          }
          emit(null)
        } else if (error is CancellationException) {
          throw error
        } else {
          handleFirestoreError(error, OperationType.GET, "users/$authUid")
          emit(null)
        }
      }

  private val storeWorkspaceFlow: Flow<FirestoreStoreWorkspace?> =
    activeStoreIdFlow.flatMapLatest { storeId ->
      if (storeId == null) {
        flowOf(null)
      } else {
        db.collection("stores")
          .document(storeId)
          .snapshots()
          .map { snap ->
            if (!snap.exists()) null
            else {
              val ws = snap.toObject(FirestoreStoreWorkspace::class.java)
              if (ws != null) {
                _cloudSyncState.update {
                  it.copy(
                    storeId = storeId,
                    joinCode = ws.joinCode,
                    ownerUid = ws.ownerUid,
                    memberCount = ws.memberUids.size.coerceAtLeast(1),
                    isOwnerOfWorkspace = ws.ownerUid == authUid,
                    isSyncing = false,
                    cloudError = null,
                  )
                }
              }
              ws
            }
          }
          .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError != null) {
              handleFirestoreError(fsError, OperationType.GET, "stores/$storeId")
              _cloudSyncState.update {
                it.copy(
                  isSyncing = false,
                  cloudError = userFriendlyFirestoreMessage(fsError, "Gagal menyinkronkan data toko."),
                )
              }
              emit(null)
            } else if (error is CancellationException) {
              throw error
            } else {
              handleFirestoreError(error, OperationType.GET, "stores/$storeId")
              emit(null)
            }
          }
      }
    }

  val productsFlow: Flow<List<ProductEntity>> =
    activeStoreIdFlow.flatMapLatest { storeId ->
      if (storeId == null) {
        flowOf(emptyList())
      } else {
        val path = "stores/$storeId/products"
        db.collection("stores")
          .document(storeId)
          .collection("products")
          .whereArrayContains("memberUids", authUid)
          .snapshots()
          .map { snap ->
            snap.documents
              .mapNotNull(FirestoreMappers::productFromSnapshot)
              .sortedBy { it.name.lowercase() }
          }
          .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError != null) {
              handleFirestoreError(fsError, OperationType.LIST, path)
              _cloudSyncState.update {
                it.copy(cloudError = userFriendlyFirestoreMessage(fsError, "Gagal memuat stok barang."))
              }
              emit(emptyList())
            } else if (error is CancellationException) {
              throw error
            } else {
              handleFirestoreError(error, OperationType.LIST, path)
              emit(emptyList())
            }
          }
      }
    }

  val transactionsFlow: Flow<List<TransactionEntity>> =
    activeStoreIdFlow.flatMapLatest { storeId ->
      if (storeId == null) {
        flowOf(emptyList())
      } else {
        val path = "stores/$storeId/transactions"
        db.collection("stores")
          .document(storeId)
          .collection("transactions")
          .whereArrayContains("memberUids", authUid)
          .snapshots()
          .map { snap ->
            snap.documents
              .mapNotNull(FirestoreMappers::transactionFromSnapshot)
              .sortedByDescending { it.timestamp }
          }
          .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError != null) {
              handleFirestoreError(fsError, OperationType.LIST, path)
              _cloudSyncState.update {
                it.copy(cloudError = userFriendlyFirestoreMessage(fsError, "Gagal memuat riwayat transaksi."))
              }
              emit(emptyList())
            } else if (error is CancellationException) {
              throw error
            } else {
              handleFirestoreError(error, OperationType.LIST, path)
              emit(emptyList())
            }
          }
      }
    }

  val expensesFlow: Flow<List<ExpenseEntity>> =
    activeStoreIdFlow.flatMapLatest { storeId ->
      if (storeId == null) {
        flowOf(emptyList())
      } else {
        val path = "stores/$storeId/expenses"
        db.collection("stores")
          .document(storeId)
          .collection("expenses")
          .whereArrayContains("memberUids", authUid)
          .snapshots()
          .map { snap ->
            snap.documents
              .mapNotNull(FirestoreMappers::expenseFromSnapshot)
              .sortedByDescending { it.timestamp }
          }
          .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError != null) {
              handleFirestoreError(fsError, OperationType.LIST, path)
              _cloudSyncState.update {
                it.copy(cloudError = userFriendlyFirestoreMessage(fsError, "Gagal memuat pengeluaran."))
              }
              emit(emptyList())
            } else if (error is CancellationException) {
              throw error
            } else {
              handleFirestoreError(error, OperationType.LIST, path)
              emit(emptyList())
            }
          }
      }
    }

  val employeesFlow: Flow<List<EmployeeAccount>> =
    activeStoreIdFlow.flatMapLatest { storeId ->
      if (storeId == null) {
        flowOf(emptyList())
      } else {
        val path = "stores/$storeId/employees"
        db.collection("stores")
          .document(storeId)
          .collection("employees")
          .whereArrayContains("memberUids", authUid)
          .snapshots()
          .map { snap ->
            snap.documents
              .mapNotNull(FirestoreMappers::employeeFromSnapshot)
              .sortedBy { it.name.lowercase() }
          }
          .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError != null) {
              handleFirestoreError(fsError, OperationType.LIST, path)
              _cloudSyncState.update {
                it.copy(cloudError = userFriendlyFirestoreMessage(fsError, "Gagal memuat daftar karyawan."))
              }
              emit(emptyList())
            } else if (error is CancellationException) {
              throw error
            } else {
              handleFirestoreError(error, OperationType.LIST, path)
              emit(emptyList())
            }
          }
      }
    }

  val categoriesFlow: Flow<List<CategoryItem>> =
    activeStoreIdFlow.flatMapLatest { storeId ->
      if (storeId == null) {
        flowOf(emptyList())
      } else {
        val path = "stores/$storeId/categories"
        db.collection("stores")
          .document(storeId)
          .collection("categories")
          .whereArrayContains("memberUids", authUid)
          .snapshots()
          .map { snap ->
            snap.documents
              .mapNotNull(FirestoreMappers::categoryFromSnapshot)
              .sortedBy { it.name.lowercase() }
          }
          .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError != null) {
              handleFirestoreError(fsError, OperationType.LIST, path)
              _cloudSyncState.update {
                it.copy(cloudError = userFriendlyFirestoreMessage(fsError, "Gagal memuat kategori barang."))
              }
              emit(emptyList())
            } else if (error is CancellationException) {
              throw error
            } else {
              handleFirestoreError(error, OperationType.LIST, path)
              emit(emptyList())
            }
          }
      }
    }

  val settingsFlow: Flow<StoreSettings> =
    combine(prefs.settingsFlow, storeWorkspaceFlow) { localSettings, workspace ->
      workspace?.toStoreSettings(localSettings) ?: localSettings
    }

  suspend fun seedInitialDataIfNeeded(): Result<Unit> =
    runSafeFirestoreWrite("users/$authUid", OperationType.WRITE) {
      _cloudSyncState.update { it.copy(isSyncing = true, cloudError = null) }

      val userRef = db.collection("users").document(authUid)
      val userSnap = userRef.get().await()
      val existingDefaultStoreId = userSnap.getString("defaultStoreId")?.takeIf { it.isNotBlank() }

      if (existingDefaultStoreId != null) {
        val validExistingStore =
          try {
            val snap = db.collection("stores").document(existingDefaultStoreId).get().await()
            snap.exists() && authUid in FirestoreMappers.memberUidsFromSnapshot(snap)
          } catch (_: Exception) {
            false
          }
        if (validExistingStore) {
          val storeSnap = db.collection("stores").document(existingDefaultStoreId).get().await()
          val ws = storeSnap.toObject(FirestoreStoreWorkspace::class.java)
          if (ws != null && ws.ownerUid == authUid) {
            ensureOwnerCredentialsAndDefaultEmployee(existingDefaultStoreId, storeSnap, ws)
            propagateMembersToSubcollections(existingDefaultStoreId, ws.memberUids)
          }
          _cloudSyncState.update { it.copy(isSyncing = false) }
          return@runSafeFirestoreWrite
        }
      }

      // Check if user already has personal store /stores/{authUid}
      val personalStoreRef = db.collection("stores").document(authUid)
      val personalSnap =
        try {
          personalStoreRef.get().await()
        } catch (_: Exception) {
          null
        }

      if (personalSnap == null || !personalSnap.exists()) {
        val joinCode = generateJoinCodeForUid(authUid)
        val storePayload =
          mapOf(
            "ownerUid" to authUid,
            "memberUids" to listOf(authUid),
            "joinCode" to joinCode,
            "storeName" to "TB PUTRA RI",
            "storeAddress" to "Jl. Raya Pembangunan No. 88",
            "storePhone" to "0812-3456-7890",
            "ownerName" to "H. Putra",
            "cashierName" to "Budi Santoso",
            "ownerUsername" to "owner",
            "ownerPassword" to "owner123",
            "cashierUsername" to "kasir",
            "cashierPassword" to "kasir123",
            "ownerPin" to "1234",
            "cashierPin" to "0000",
            "employeesInitialized" to true,
            "categoriesInitialized" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        personalStoreRef.set(storePayload).await()

        val inviteRef = db.collection("storeInvites").document(joinCode)
        inviteRef
          .set(
            mapOf(
              "joinCode" to joinCode,
              "storeId" to authUid,
              "ownerUid" to authUid,
              "storeName" to "TB PUTRA RI",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()

        personalStoreRef
          .collection("employees")
          .document("emp_default")
          .set(
            FirestoreMappers.employeeCreateMap(
              name = "Budi Santoso",
              username = "kasir",
              password = "kasir123",
              storeId = authUid,
              ownerUid = authUid,
              memberUids = listOf(authUid),
            )
          )
          .await()

        seedDummyStoreContent(storeId = authUid, memberUids = listOf(authUid))
      } else {
        // Backfill username/password credentials and default employee on legacy personal store doc if missing
        val ws = personalSnap.toObject(FirestoreStoreWorkspace::class.java)
        if (ws != null && ws.ownerUid == authUid) {
          ensureOwnerCredentialsAndDefaultEmployee(authUid, personalSnap, ws)
        }
        // Ensure store invite doc exists & check if store has products
        if (ws != null && ws.joinCode.isNotBlank()) {
          val inviteSnap = runCatching { db.collection("storeInvites").document(ws.joinCode).get().await() }.getOrNull()
          if (inviteSnap == null || !inviteSnap.exists()) {
            db.collection("storeInvites")
              .document(ws.joinCode)
              .set(
                mapOf(
                  "joinCode" to ws.joinCode,
                  "storeId" to authUid,
                  "ownerUid" to authUid,
                  "storeName" to ws.storeName.ifBlank { "TB PUTRA RI" },
                  "createdAt" to FieldValue.serverTimestamp(),
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
              .await()
          }
        }
      }

      if (!userSnap.exists()) {
        userRef
          .set(
            mapOf(
              "userId" to authUid,
              "defaultStoreId" to authUid,
              "displayName" to displayName.ifBlank { "Pemilik Toko" }.take(100),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        userRef
          .update(
            mapOf(
              "defaultStoreId" to authUid,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      _cloudSyncState.update { it.copy(isSyncing = false) }
    }

  suspend fun syncMembersIfOwner(): Result<Unit> =
    runSafeFirestoreWrite("stores", OperationType.UPDATE) {
      val storeId = resolveActiveStoreId()
      val storeSnap = db.collection("stores").document(storeId).get().await()
      val ws = storeSnap.toObject(FirestoreStoreWorkspace::class.java) ?: return@runSafeFirestoreWrite
      if (ws.ownerUid == authUid) {
        propagateMembersToSubcollections(storeId, ws.memberUids)
      }
    }

  private suspend fun ensureOwnerCredentialsAndDefaultEmployee(
    storeId: String,
    storeSnap: com.google.firebase.firestore.DocumentSnapshot,
    ws: FirestoreStoreWorkspace,
  ) {
    val storeUpdates = mutableMapOf<String, Any>()
    if (storeSnap.getString("ownerUsername").isNullOrBlank()) {
      storeUpdates["ownerUsername"] = "owner"
      storeUpdates["ownerPassword"] = "owner123"
      storeUpdates["cashierUsername"] = "kasir"
      storeUpdates["cashierPassword"] = "kasir123"
    }
    if (!ws.employeesInitialized) {
      val existingEmployees =
        db.collection("stores")
          .document(storeId)
          .collection("employees")
          .whereArrayContains("memberUids", authUid)
          .get()
          .await()
      if (existingEmployees.isEmpty) {
        db.collection("stores")
          .document(storeId)
          .collection("employees")
          .document("emp_default")
          .set(
            FirestoreMappers.employeeCreateMap(
              name = ws.cashierName.ifBlank { "Budi Santoso" },
              username = ws.cashierUsername.ifBlank { "kasir" },
              password = ws.cashierPassword.ifBlank { "kasir123" },
              storeId = storeId,
              ownerUid = ws.ownerUid,
              memberUids = ws.memberUids.ifEmpty { listOf(authUid) },
            )
          )
          .await()
      }
      storeUpdates["employeesInitialized"] = true
    }
    if (!ws.categoriesInitialized) {
      val existingCategories =
        db.collection("stores")
          .document(storeId)
          .collection("categories")
          .whereArrayContains("memberUids", authUid)
          .get()
          .await()
      if (existingCategories.isEmpty) {
        val existingProducts =
          db.collection("stores")
            .document(storeId)
            .collection("products")
            .whereArrayContains("memberUids", authUid)
            .get()
            .await()
        val productCategories =
          existingProducts.documents
            .mapNotNull { it.getString("category")?.trim()?.takeIf { c -> c.isNotBlank() } }
        val seedCategories =
          (DEFAULT_PRODUCT_CATEGORIES + productCategories).distinctBy { it.lowercase() }
        val members = ws.memberUids.ifEmpty { listOf(authUid) }
        db.runBatch { batch ->
          seedCategories.forEachIndexed { idx, catName ->
            val ref =
              db.collection("stores")
                .document(storeId)
                .collection("categories")
                .document(FirestoreMappers.categoryDocId((idx + 1).toLong()))
            batch.set(
              ref,
              FirestoreMappers.categoryCreateMap(
                name = catName,
                storeId = storeId,
                ownerUid = ws.ownerUid,
                memberUids = members,
              ),
            )
          }
        }.await()
      }
      storeUpdates["categoriesInitialized"] = true
    }
    if (storeUpdates.isNotEmpty()) {
      storeUpdates["updatedAt"] = FieldValue.serverTimestamp()
      db.collection("stores").document(storeId).update(storeUpdates).await()
    }
  }

  private suspend fun propagateMembersToSubcollections(storeId: String, memberUids: List<String>) {
    val targetSet = memberUids.toSet()
    val collections = listOf("employees", "categories", "products", "transactions", "expenses")
    for (col in collections) {
      val snap =
        db.collection("stores")
          .document(storeId)
          .collection(col)
          .whereArrayContains("memberUids", authUid)
          .get()
          .await()
      val outdated =
        snap.documents.filter { doc ->
          FirestoreMappers.memberUidsFromSnapshot(doc).toSet() != targetSet
        }
      outdated.chunked(350).forEach { chunk ->
        db.runBatch { batch ->
          chunk.forEach { doc ->
            batch.update(
              doc.reference,
              mapOf(
                "memberUids" to memberUids,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }.await()
      }
    }
  }

  private suspend fun seedDummyStoreContent(storeId: String, memberUids: List<String>) {
    val now = System.currentTimeMillis()
    val products = DummyDataSeeder.initialProducts()
    val transactions = DummyDataSeeder.initialTransactions(now)
    val expenses = DummyDataSeeder.initialExpenses(now)

    db.runBatch { batch ->
      DEFAULT_PRODUCT_CATEGORIES.forEachIndexed { idx, catName ->
        val numId = (idx + 1).toLong()
        val ref =
          db.collection("stores")
            .document(storeId)
            .collection("categories")
            .document(FirestoreMappers.categoryDocId(numId))
        batch.set(
          ref,
          FirestoreMappers.categoryCreateMap(
            name = catName,
            storeId = storeId,
            ownerUid = authUid,
            memberUids = memberUids,
          ),
        )
      }
      products.forEachIndexed { idx, p ->
        val numId = (idx + 1).toLong()
        val ref =
          db.collection("stores")
            .document(storeId)
            .collection("products")
            .document(FirestoreMappers.productDocId(numId))
        batch.set(ref, FirestoreMappers.productCreateMap(p, numId, storeId, authUid, memberUids))
      }
      transactions.forEachIndexed { idx, t ->
        val numId = (idx + 1).toLong()
        val ref =
          db.collection("stores")
            .document(storeId)
            .collection("transactions")
            .document(FirestoreMappers.transactionDocId(numId))
        batch.set(
          ref,
          FirestoreMappers.transactionCreateMap(t, numId, storeId, authUid, authUid, memberUids),
        )
      }
      expenses.forEachIndexed { idx, e ->
        val numId = (idx + 1).toLong()
        val ref =
          db.collection("stores")
            .document(storeId)
            .collection("expenses")
            .document(FirestoreMappers.expenseDocId(numId))
        batch.set(
          ref,
          FirestoreMappers.expenseCreateMap(e, numId, storeId, authUid, authUid, memberUids),
        )
      }
    }.await()
  }

  suspend fun joinStoreByCode(rawCode: String): Result<String> {
    val code = rawCode.trim().uppercase()
    if (!code.matches(Regex("^[A-Z0-9]{6,12}$"))) {
      return Result.failure(IllegalArgumentException("Kode Toko harus 6-12 huruf/angka."))
    }

    return runSafeFirestoreWrite("storeInvites/$code", OperationType.UPDATE) {
      val inviteSnap = db.collection("storeInvites").document(code).get().await()
      if (!inviteSnap.exists()) {
        throw IllegalStateException("Kode Toko '$code' tidak ditemukan.")
      }
      val targetStoreId =
        inviteSnap.getString("storeId")?.takeIf { it.isNotBlank() }
          ?: throw IllegalStateException("Data Kode Toko tidak valid.")

      // Check safely without PERMISSION_DENIED if already a member
      val alreadyMemberStores =
        db.collection("stores")
          .whereArrayContains("memberUids", authUid)
          .get()
          .await()
          .documents
          .map { it.id }

      if (targetStoreId !in alreadyMemberStores) {
        db.collection("stores")
          .document(targetStoreId)
          .update(
            mapOf(
              "memberUids" to FieldValue.arrayUnion(authUid),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      db.collection("users")
        .document(authUid)
        .update(
          mapOf(
            "defaultStoreId" to targetStoreId,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      prefs.setActiveRole(null)
      targetStoreId
    }
  }

  suspend fun switchToOwnStore(): Result<Unit> =
    runSafeFirestoreWrite("users/$authUid", OperationType.UPDATE) {
      val userRef = db.collection("users").document(authUid)
      userRef
        .update(
          mapOf(
            "defaultStoreId" to authUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      seedInitialDataIfNeeded().getOrThrow()
      prefs.setActiveRole(null)
    }

  suspend fun createNewStore(
    ownerName: String,
    ownerUsername: String,
    ownerPassword: String,
    storeAddress: String,
    storePhone: String,
    includeSampleProducts: Boolean = false,
  ): Result<String> =
    runSafeFirestoreWrite("stores", OperationType.WRITE) {
      val cleanStoreName = "TB PUTRA RI"
      val cleanOwnerName = ownerName.trim().take(80)
      val cleanOwnerUser = ownerUsername.trim().take(40)
      val cleanOwnerPass = ownerPassword.take(64)
      val cleanAddress = storeAddress.trim().ifBlank { "Alamat belum diisi" }.take(200)
      val cleanPhone = storePhone.trim().take(40)

      if (cleanOwnerName.isBlank()) {
        throw IllegalArgumentException("Nama pemilik wajib diisi.")
      }
      if (!cleanOwnerUser.matches(Regex("^[a-zA-Z0-9_.\\-]{3,40}$"))) {
        throw IllegalArgumentException("Username pemilik minimal 3 karakter (huruf, angka, ., _, -).")
      }
      if (cleanOwnerPass.length < 4) {
        throw IllegalArgumentException("Password pemilik minimal 4 karakter.")
      }

      val cleanUidPrefix = authUid.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(16).ifBlank { "user" }
      val newStoreId = "store_${cleanUidPrefix}_${System.currentTimeMillis()}"
      val joinCode = generateAvailableJoinCode()
      val memberUids = listOf(authUid)

      val storeRef = db.collection("stores").document(newStoreId)
      storeRef
        .set(
          mapOf(
            "ownerUid" to authUid,
            "memberUids" to memberUids,
            "joinCode" to joinCode,
            "storeName" to cleanStoreName,
            "storeAddress" to cleanAddress,
            "storePhone" to cleanPhone,
            "ownerName" to cleanOwnerName,
            "cashierName" to "Kasir",
            "ownerUsername" to cleanOwnerUser,
            "ownerPassword" to cleanOwnerPass,
            "cashierUsername" to "kasir",
            "cashierPassword" to "kasir123",
            "ownerPin" to "1234",
            "cashierPin" to "0000",
            "employeesInitialized" to true,
            "categoriesInitialized" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      db.collection("storeInvites")
        .document(joinCode)
        .set(
          mapOf(
            "joinCode" to joinCode,
            "storeId" to newStoreId,
            "ownerUid" to authUid,
            "storeName" to cleanStoreName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      db.runBatch { batch ->
        DEFAULT_PRODUCT_CATEGORIES.forEachIndexed { idx, catName ->
          val numId = (idx + 1).toLong()
          val catRef =
            storeRef
              .collection("categories")
              .document(FirestoreMappers.categoryDocId(numId))
          batch.set(
            catRef,
            FirestoreMappers.categoryCreateMap(
              name = catName,
              storeId = newStoreId,
              ownerUid = authUid,
              memberUids = memberUids,
            ),
          )
        }
        if (includeSampleProducts) {
          DummyDataSeeder.initialProducts().forEachIndexed { idx, p ->
            val numId = (idx + 1).toLong()
            val prodRef =
              storeRef
                .collection("products")
                .document(FirestoreMappers.productDocId(numId))
            batch.set(
              prodRef,
              FirestoreMappers.productCreateMap(p, numId, newStoreId, authUid, memberUids),
            )
          }
        }
      }.await()

      val userRef = db.collection("users").document(authUid)
      val userSnap = userRef.get().await()
      if (!userSnap.exists()) {
        userRef
          .set(
            mapOf(
              "userId" to authUid,
              "defaultStoreId" to newStoreId,
              "displayName" to displayName.ifBlank { cleanOwnerName }.take(100),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        userRef
          .update(
            mapOf(
              "defaultStoreId" to newStoreId,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      prefs.setActiveSession(UserRole.OWNER)
      joinCode
    }

  suspend fun resetDemoData(): Result<Unit> =
    runSafeFirestoreWrite("stores", OperationType.WRITE) {
      val storeCtx = resolveStoreContext()
      if (storeCtx.ownerUid != authUid) {
        throw IllegalStateException("Hanya pemilik utama akun toko yang dapat mereset data demo.")
      }
      val collections = listOf("categories", "products", "transactions", "expenses")
      for (col in collections) {
        val docs =
          db.collection("stores")
            .document(storeCtx.storeId)
            .collection(col)
            .whereArrayContains("memberUids", authUid)
            .get()
            .await()
            .documents
        docs.chunked(350).forEach { chunk ->
          db.runBatch { batch ->
            chunk.forEach { batch.delete(it.reference) }
          }.await()
        }
      }
      seedDummyStoreContent(storeId = storeCtx.storeId, memberUids = storeCtx.memberUids)
    }

  suspend fun setActiveSession(
    role: UserRole?,
    employeeId: String? = null,
    employeeName: String? = null,
  ) = prefs.setActiveSession(role, employeeId, employeeName)

  suspend fun updateOwnerAccountSettings(
    ownerUsername: String,
    ownerPassword: String,
    ownerName: String,
    storeAddress: String,
    storePhone: String,
  ): Result<Unit> =
    runSafeFirestoreWrite("stores", OperationType.UPDATE) {
      val cleanOwnerUser = ownerUsername.trim().take(40)
      val cleanOwnerPass = ownerPassword.take(64)
      val cleanOwnerName = ownerName.trim().take(80)
      val cleanAddress = storeAddress.trim().take(200)
      val cleanPhone = storePhone.trim().take(40)

      val ctx = resolveStoreContext()
      val updates =
        mapOf(
          "ownerUsername" to cleanOwnerUser,
          "ownerPassword" to cleanOwnerPass,
          "ownerName" to cleanOwnerName,
          "storeAddress" to cleanAddress,
          "storePhone" to cleanPhone,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      db.collection("stores")
        .document(ctx.storeId)
        .update(updates)
        .await()

      prefs.updateOwnerAccountSettings(
        ownerUsername = cleanOwnerUser,
        ownerPassword = cleanOwnerPass,
        ownerName = cleanOwnerName,
        storeAddress = cleanAddress,
        storePhone = cleanPhone,
      )
    }

  suspend fun saveThermalPrintPreferences(
    paperSize: com.example.data.model.ReceiptPaperSize,
    printerAddress: String? = null,
    printerName: String? = null,
  ) {
    prefs.saveThermalPrintPreferences(
      paperSize = paperSize,
      printerAddress = printerAddress,
      printerName = printerName,
    )
  }

  suspend fun saveEmployee(
    existingId: String?,
    name: String,
    username: String,
    password: String,
    phone: String = "",
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/employees", OperationType.WRITE) {
      val ctx = resolveStoreContext()
      if (ctx.ownerUid != authUid) {
        throw IllegalStateException("Hanya pemilik toko yang dapat mengelola karyawan.")
      }
      val colRef = db.collection("stores").document(ctx.storeId).collection("employees")
      val cleanPhone = phone.trim().take(40)
      if (existingId.isNullOrBlank()) {
        val docId = FirestoreMappers.employeeDocId(nextNumericId())
        colRef
          .document(docId)
          .set(
            FirestoreMappers.employeeCreateMap(
              name = name,
              username = username,
              password = password,
              storeId = ctx.storeId,
              ownerUid = ctx.ownerUid,
              memberUids = ctx.memberUids,
              phone = cleanPhone,
            )
          )
          .await()
      } else {
        colRef
          .document(existingId)
          .update(
            FirestoreMappers.employeeUpdateMap(
              name = name,
              username = username,
              password = password,
              memberUids = ctx.memberUids,
              phone = cleanPhone,
            )
          )
          .await()
      }
    }

  suspend fun updateOwnEmployeeAccount(
    employeeId: String,
    name: String,
    username: String,
    password: String,
    phone: String = "",
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/employees/$employeeId", OperationType.UPDATE) {
      val cleanId = employeeId.trim()
      require(cleanId.isNotBlank()) { "ID karyawan tidak valid." }
      val cleanName = name.trim().take(80)
      require(cleanName.isNotBlank()) { "Nama kasir tidak boleh kosong." }
      val cleanUser = username.trim()
      require(cleanUser.matches(Regex("^[a-zA-Z0-9_.\\-]{3,40}$"))) {
        "Username minimal 3 karakter (huruf, angka, titik, _, atau -)."
      }
      val cleanPass = password.take(64)
      require(cleanPass.length >= 4) { "Password minimal 4 karakter." }
      val cleanPhone = phone.trim().take(40)

      val ctx = resolveStoreContext()
      val empRef =
        db.collection("stores")
          .document(ctx.storeId)
          .collection("employees")
          .document(cleanId)
      val empSnap = empRef.get().await()
      if (empSnap.exists()) {
        empRef
          .update(
            FirestoreMappers.employeeSelfUpdateMap(
              name = cleanName,
              username = cleanUser,
              password = cleanPass,
              phone = cleanPhone,
            )
          )
          .await()
      } else if (ctx.ownerUid == authUid) {
        empRef
          .set(
            FirestoreMappers.employeeCreateMap(
              name = cleanName,
              username = cleanUser,
              password = cleanPass,
              storeId = ctx.storeId,
              ownerUid = ctx.ownerUid,
              memberUids = ctx.memberUids,
              phone = cleanPhone,
            )
          )
          .await()
      } else {
        throw IllegalStateException("Data akun kasir belum ditemukan di server.")
      }
      prefs.setActiveSession(
        role = UserRole.CASHIER,
        employeeId = cleanId,
        employeeName = cleanName,
      )
    }

  suspend fun deleteEmployee(employeeId: String): Result<Unit> =
    runSafeFirestoreWrite("stores/employees/$employeeId", OperationType.DELETE) {
      val ctx = resolveStoreContext()
      if (ctx.ownerUid != authUid) {
        throw IllegalStateException("Hanya pemilik toko yang dapat menghapus karyawan.")
      }
      db.collection("stores")
        .document(ctx.storeId)
        .collection("employees")
        .document(employeeId)
        .delete()
        .await()
    }

  suspend fun saveCategory(
    existing: CategoryItem?,
    name: String,
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/categories", OperationType.WRITE) {
      val ctx = resolveStoreContext()
      if (ctx.ownerUid != authUid) {
        throw IllegalStateException("Hanya pemilik toko yang dapat mengelola kategori barang.")
      }
      val cleanName = name.trim().take(60)
      if (cleanName.isBlank()) {
        throw IllegalArgumentException("Nama kategori wajib diisi.")
      }

      val storeRef = db.collection("stores").document(ctx.storeId)
      val colRef = storeRef.collection("categories")
      if (existing == null) {
        val docId = FirestoreMappers.categoryDocId(nextNumericId())
        colRef
          .document(docId)
          .set(
            FirestoreMappers.categoryCreateMap(
              name = cleanName,
              storeId = ctx.storeId,
              ownerUid = ctx.ownerUid,
              memberUids = ctx.memberUids,
            )
          )
          .await()
      } else {
        val productsSnap =
          storeRef
            .collection("products")
            .whereArrayContains("memberUids", authUid)
            .get()
            .await()
        val affectedProducts =
          productsSnap.documents.filter { doc ->
            (doc.getString("category") ?: "").equals(existing.name, ignoreCase = true)
          }
        val now = System.currentTimeMillis()
        db.runBatch { batch ->
          batch.update(
            colRef.document(existing.id),
            FirestoreMappers.categoryUpdateMap(
              name = cleanName,
              memberUids = ctx.memberUids,
            ),
          )
          affectedProducts.forEach { prodDoc ->
            batch.update(
              prodDoc.reference,
              mapOf(
                "category" to cleanName,
                "updatedAtMillis" to now,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }.await()
      }
    }

  suspend fun deleteCategory(
    category: CategoryItem,
    reassignTo: String? = null,
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/categories/${category.id}", OperationType.DELETE) {
      val ctx = resolveStoreContext()
      if (ctx.ownerUid != authUid) {
        throw IllegalStateException("Hanya pemilik toko yang dapat menghapus kategori barang.")
      }
      val storeRef = db.collection("stores").document(ctx.storeId)
      val productsSnap =
        storeRef
          .collection("products")
          .whereArrayContains("memberUids", authUid)
          .get()
          .await()
      val affectedProducts =
        productsSnap.documents.filter { doc ->
          (doc.getString("category") ?: "").equals(category.name, ignoreCase = true)
        }
      val targetCategory = reassignTo?.trim()?.takeIf { it.isNotBlank() }?.take(60) ?: "Lainnya"
      val now = System.currentTimeMillis()
      db.runBatch { batch ->
        batch.delete(storeRef.collection("categories").document(category.id))
        affectedProducts.forEach { prodDoc ->
          batch.update(
            prodDoc.reference,
            mapOf(
              "category" to targetCategory,
              "updatedAtMillis" to now,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
    }

  suspend fun saveProduct(product: ProductEntity): Result<Unit> =
    runSafeFirestoreWrite("stores/products", OperationType.WRITE) {
      val ctx = resolveStoreContext()
      if (product.id == 0L) {
        val numId = nextNumericId()
        val docRef =
          db.collection("stores")
            .document(ctx.storeId)
            .collection("products")
            .document(FirestoreMappers.productDocId(numId))
        val payload =
          FirestoreMappers.productCreateMap(
            product = product,
            numericId = numId,
            storeId = ctx.storeId,
            ownerUid = ctx.ownerUid,
            memberUids = ctx.memberUids,
          )
        docRef.set(payload).await()
      } else {
        val docRef =
          db.collection("stores")
            .document(ctx.storeId)
            .collection("products")
            .document(FirestoreMappers.productDocId(product.id))
        val payload = FirestoreMappers.productOwnerUpdateMap(product, ctx.memberUids)
        docRef.update(payload).await()
      }
    }

  suspend fun deleteProduct(id: Long): Result<Unit> =
    runSafeFirestoreWrite("stores/products/prod_$id", OperationType.DELETE) {
      val ctx = resolveStoreContext()
      db.collection("stores")
        .document(ctx.storeId)
        .collection("products")
        .document(FirestoreMappers.productDocId(id))
        .delete()
        .await()
    }

  suspend fun restockProduct(
    product: ProductEntity,
    addedBaseQty: Double,
    totalPurchaseCost: Long,
    deductFromCash: Boolean,
    updateCostPrice: Boolean = false,
    recordedBy: String,
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/products/prod_${product.id}", OperationType.WRITE) {
      val ctx = resolveStoreContext()
      val now = System.currentTimeMillis()
      val existingStock = product.stockQuantity.coerceAtLeast(0.0)
      val newStock = (existingStock + addedBaseQty).coerceAtLeast(0.0)
      val prodRef =
        db.collection("stores")
          .document(ctx.storeId)
          .collection("products")
          .document(FirestoreMappers.productDocId(product.id))

      val productUpdates = buildMap<String, Any> {
        put("stockQuantity", newStock)
        if (updateCostPrice && totalPurchaseCost > 0L && addedBaseQty > 0.0 && newStock > 0.0) {
          val oldStockValue = existingStock * product.costPricePerBaseUnit.coerceAtLeast(0L).toDouble()
          val weightedCost =
            kotlin.math.round((oldStockValue + totalPurchaseCost.toDouble()) / newStock)
              .toLong()
              .coerceAtLeast(0L)
          put("costPricePerBaseUnit", weightedCost)
        }
        put("updatedAtMillis", now)
        put("updatedAt", FieldValue.serverTimestamp())
      }

      db.runBatch { batch ->
        batch.update(prodRef, productUpdates)
        if (deductFromCash && totalPurchaseCost > 0L) {
          val expNumId = nextNumericId()
          val expRef =
            db.collection("stores")
              .document(ctx.storeId)
              .collection("expenses")
              .document(FirestoreMappers.expenseDocId(expNumId))
          val expense =
            ExpenseEntity(
              id = expNumId,
              title = "Kulakan: ${product.name}",
              category = ExpenseCategory.KULAKAN.label,
              amount = totalPurchaseCost,
              notes = "Restok +${addedBaseQty} ${product.baseUnit}",
              recordedBy = recordedBy,
              recordedRole = UserRole.OWNER.name,
              timestamp = now,
            )
          batch.set(
            expRef,
            FirestoreMappers.expenseCreateMap(
              exp = expense,
              numericId = expNumId,
              storeId = ctx.storeId,
              ownerUid = ctx.ownerUid,
              recordedByUid = authUid,
              memberUids = ctx.memberUids,
            ),
          )
        }
      }.await()
    }

  suspend fun repackToRetail(
    product: ProductEntity,
    baseQtyToConvert: Double,
  ): Result<Double> =
    runSafeFirestoreWrite("stores/products/prod_${product.id}", OperationType.UPDATE) {
      if (product.retailUnitName.isNullOrBlank() || product.retailConversionFactor <= 0.0) {
        throw IllegalArgumentException("Barang ini belum memiliki satuan eceran.")
      }
      if (baseQtyToConvert <= 0.0) {
        throw IllegalArgumentException("Jumlah yang diecer harus lebih dari 0.")
      }
      if (baseQtyToConvert > product.stockQuantity + 1e-6) {
        throw IllegalArgumentException("Stok ${product.baseUnit} tidak mencukupi untuk diecer.")
      }

      val ctx = resolveStoreContext()
      val now = System.currentTimeMillis()
      val unitsPerBase = retailUnitsPerBase(product.retailConversionFactor)
      val addedRetail = kotlin.math.round(baseQtyToConvert * unitsPerBase * 100.0) / 100.0
      val nextBase =
        kotlin.math.round((product.stockQuantity - baseQtyToConvert).coerceAtLeast(0.0) * 1000.0) /
          1000.0
      val nextRetail =
        kotlin.math.round(
          (product.retailStockQuantity.coerceAtLeast(0.0) + addedRetail) * 1000.0
        ) / 1000.0

      db.collection("stores")
        .document(ctx.storeId)
        .collection("products")
        .document(FirestoreMappers.productDocId(product.id))
        .update(
          mapOf(
            "stockQuantity" to nextBase,
            "retailStockQuantity" to nextRetail,
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      addedRetail
    }

  suspend fun checkoutCart(
    cart: List<CartItem>,
    currentProductsById: Map<Long, ProductEntity>,
    customerType: CustomerType,
    manualDiscount: Long = 0L,
    customerName: String,
    customerPhone: String,
    paymentMethod: PaymentMethod,
    cashReceived: Long,
    dpPaymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    cashierName: String,
    cashierRole: UserRole,
  ): Result<TransactionEntity> =
    runSafeFirestoreWrite("stores/transactions", OperationType.CREATE) {
      val ctx = resolveStoreContext()
      val now = System.currentTimeMillis()
      val subtotal = cart.sumOf { it.subtotal }
      val discountAmount =
        if (customerType == CustomerType.PELANGGAN) {
          manualDiscount.coerceIn(0L, subtotal)
        } else {
          kotlin.math.round(subtotal * customerType.discountRate).toLong()
        }
      val totalAmount = (subtotal - discountAmount).coerceAtLeast(0L)
      val totalCost = cart.sumOf { it.totalCost }
      val isPaid = paymentMethod != PaymentMethod.KASBON
      val dpAmount =
        if (paymentMethod == PaymentMethod.KASBON && totalAmount > 0L) {
          cashReceived.coerceIn(0L, (totalAmount - 1L).coerceAtLeast(0L))
        } else {
          0L
        }
      val effectiveDpMethod =
        if (dpPaymentMethod == PaymentMethod.TRANSFER_QRIS) PaymentMethod.TRANSFER_QRIS
        else PaymentMethod.TUNAI
      val paymentsList =
        if (paymentMethod == PaymentMethod.KASBON && dpAmount > 0L) {
          listOf(
            KasbonPaymentRecord(
              amount = dpAmount,
              paymentMethod = effectiveDpMethod.label,
              timestamp = now,
              recordedBy = cashierName.ifBlank { "Kasir" },
            )
          )
        } else {
          emptyList()
        }
      val paidAmount =
        when (paymentMethod) {
          PaymentMethod.TUNAI -> cashReceived.coerceAtLeast(totalAmount)
          PaymentMethod.TRANSFER_QRIS -> totalAmount
          PaymentMethod.KASBON -> dpAmount
        }
      val changeAmount =
        if (paymentMethod == PaymentMethod.TUNAI) (paidAmount - totalAmount).coerceAtLeast(0L)
        else 0L

      val items =
        cart.map { item ->
          TransactionItem(
            productId = item.productId,
            productName = item.productName,
            unitName = item.unitName,
            quantity = item.quantity,
            baseUnitsDeducted = item.baseUnitsRequired,
            unitPrice = item.unitPrice,
            unitCost = kotlin.math.round(item.conversionFactor * item.unitCostPerBase).toLong(),
            subtotal = item.subtotal,
            isRetailUnit = item.isRetailUnit,
            standardUnitPrice = item.standardUnitPrice,
          )
        }

      val txNumId = nextNumericId()
      val invoiceCode = "INV-PR-${(now / 1000L) % 100000L}"
      val entity =
        TransactionEntity(
          id = txNumId,
          invoiceNumber = invoiceCode,
          timestamp = now,
          cashierName = cashierName,
          cashierRole = cashierRole.name,
          customerType = customerType.label,
          customerName = customerName.ifBlank { customerType.label },
          customerPhone = customerPhone.trim(),
          paymentMethod = paymentMethod.label,
          subtotal = subtotal,
          discountAmount = discountAmount,
          totalAmount = totalAmount,
          totalCost = totalCost,
          amountPaid = paidAmount,
          changeAmount = changeAmount,
          isPaid = isPaid,
          paidAt = when {
            isPaid -> now
            dpAmount > 0L -> now
            else -> null
          },
          itemsJson = TransactionItemCodec.encode(items),
          paymentsJson = KasbonPaymentCodec.encode(paymentsList),
        )

      val cartByProduct = cart.groupBy { it.productId }

      db.runBatch { batch ->
        cartByProduct.forEach { (prodId, list) ->
          val currentProd = currentProductsById[prodId]
          val baseDeducted = list.filter { !it.isRetailUnit }.sumOf { it.quantity }
          val retailDeducted = list.filter { it.isRetailUnit }.sumOf { it.quantity }

          val updates = buildMap<String, Any> {
            if (baseDeducted > 0.0) {
              val currentBase = currentProd?.stockQuantity ?: 0.0
              val nextBase =
                kotlin.math.round((currentBase - baseDeducted).coerceAtLeast(0.0) * 1000.0) / 1000.0
              put("stockQuantity", nextBase)
            }
            if (retailDeducted > 0.0) {
              val currentRetail = currentProd?.retailStockQuantity ?: 0.0
              val nextRetail =
                kotlin.math.round((currentRetail - retailDeducted).coerceAtLeast(0.0) * 1000.0) /
                  1000.0
              put("retailStockQuantity", nextRetail)
            }
            put("updatedAtMillis", now)
            put("updatedAt", FieldValue.serverTimestamp())
          }

          val prodRef =
            db.collection("stores")
              .document(ctx.storeId)
              .collection("products")
              .document(FirestoreMappers.productDocId(prodId))
          batch.update(prodRef, updates)
        }
        val txRef =
          db.collection("stores")
            .document(ctx.storeId)
            .collection("transactions")
            .document(FirestoreMappers.transactionDocId(txNumId))
        batch.set(
          txRef,
          FirestoreMappers.transactionCreateMap(
            tx = entity,
            numericId = txNumId,
            storeId = ctx.storeId,
            ownerUid = ctx.ownerUid,
            recordedByUid = authUid,
            memberUids = ctx.memberUids,
          ),
        )
      }.await()

      entity
    }

  suspend fun payKasbon(
    tx: TransactionEntity,
    payAmount: Long,
    paymentMethod: PaymentMethod,
    recordedBy: String,
  ): Result<TransactionEntity> =
    runSafeFirestoreWrite("stores/transactions/tx_${tx.id}", OperationType.UPDATE) {
      if (tx.isPaid) return@runSafeFirestoreWrite tx
      val ctx = resolveStoreContext()
      val now = System.currentTimeMillis()
      val effectiveMethod =
        if (paymentMethod == PaymentMethod.TRANSFER_QRIS) PaymentMethod.TRANSFER_QRIS
        else PaymentMethod.TUNAI
      val remaining = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L)
      val actualPay = payAmount.coerceIn(0L, remaining)
      if (actualPay <= 0L && remaining > 0L) {
        throw IllegalArgumentException("Nominal pembayaran kasbon harus lebih dari Rp 0.")
      }

      val records = KasbonPaymentCodec.decode(tx.paymentsJson).toMutableList()
      if (records.isEmpty() && tx.amountPaid > 0L) {
        records.add(
          KasbonPaymentRecord(
            amount = tx.amountPaid,
            paymentMethod = PaymentMethod.TUNAI.label,
            timestamp = tx.paidAt ?: tx.timestamp,
            recordedBy = tx.cashierName.ifBlank { "Kasir" },
          )
        )
      }
      if (actualPay > 0L) {
        records.add(
          KasbonPaymentRecord(
            amount = actualPay,
            paymentMethod = effectiveMethod.label,
            timestamp = now,
            recordedBy = recordedBy.ifBlank { "Petugas" },
          )
        )
      }

      val newAmountPaid = (tx.amountPaid + actualPay).coerceAtMost(tx.totalAmount)
      val nowPaid = newAmountPaid >= tx.totalAmount
      val updatedMethodLabel =
        if (nowPaid) "${effectiveMethod.label} (Kasbon)" else PaymentMethod.KASBON.label
      val updatedPaymentsJson = KasbonPaymentCodec.encode(records)

      db.collection("stores")
        .document(ctx.storeId)
        .collection("transactions")
        .document(FirestoreMappers.transactionDocId(tx.id))
        .update(
          mapOf(
            "paidStatus" to nowPaid,
            "paidAtMillis" to now,
            "amountPaid" to newAmountPaid,
            "paymentMethod" to updatedMethodLabel,
            "paymentsJson" to updatedPaymentsJson,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      tx.copy(
        isPaid = nowPaid,
        paidAt = now,
        amountPaid = newAmountPaid,
        paymentMethod = updatedMethodLabel,
        paymentsJson = updatedPaymentsJson,
      )
    }

  suspend fun markKasbonPaid(tx: TransactionEntity): Result<TransactionEntity> =
    payKasbon(
      tx = tx,
      payAmount = (tx.totalAmount - tx.amountPaid).coerceAtLeast(0L),
      paymentMethod = PaymentMethod.TUNAI,
      recordedBy = "Pemilik",
    )

  suspend fun deleteTransaction(
    tx: TransactionEntity,
    currentProductsById: Map<Long, ProductEntity> = emptyMap(),
    restoreStock: Boolean = false,
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/transactions/tx_${tx.id}", OperationType.DELETE) {
      val ctx = resolveStoreContext()
      val now = System.currentTimeMillis()
      val txRef =
        db.collection("stores")
          .document(ctx.storeId)
          .collection("transactions")
          .document(FirestoreMappers.transactionDocId(tx.id))

      val restoredItemsByProduct =
        if (restoreStock) {
          TransactionItemCodec.decode(tx.itemsJson)
            .filter { it.productId > 0L && currentProductsById.containsKey(it.productId) }
            .groupBy { it.productId }
        } else {
          emptyMap()
        }

      db.runBatch { batch ->
        batch.delete(txRef)
        restoredItemsByProduct.forEach { (prodId, itemsList) ->
          val currentProd = currentProductsById[prodId] ?: return@forEach
          val baseToRestore = itemsList.filter { !it.isRetailUnit && it.baseUnitsDeducted > 0.0 }.sumOf { it.baseUnitsDeducted }
          val retailToRestore = itemsList.filter { it.isRetailUnit && it.quantity > 0.0 }.sumOf { it.quantity }
          if (baseToRestore <= 0.0 && retailToRestore <= 0.0) return@forEach

          val updates = buildMap<String, Any> {
            if (baseToRestore > 0.0) {
              put("stockQuantity", kotlin.math.round((currentProd.stockQuantity + baseToRestore).coerceAtLeast(0.0) * 1000.0) / 1000.0)
            }
            if (retailToRestore > 0.0) {
              put("retailStockQuantity", kotlin.math.round((currentProd.retailStockQuantity + retailToRestore).coerceAtLeast(0.0) * 1000.0) / 1000.0)
            }
            put("updatedAtMillis", now)
            put("updatedAt", FieldValue.serverTimestamp())
          }

          val prodRef =
            db.collection("stores")
              .document(ctx.storeId)
              .collection("products")
              .document(FirestoreMappers.productDocId(prodId))
          batch.update(prodRef, updates)
        }
      }.await()
    }

  suspend fun addExpense(
    title: String,
    category: ExpenseCategory,
    amount: Long,
    notes: String,
    recordedBy: String,
    recordedRole: UserRole,
    timestamp: Long = System.currentTimeMillis(),
  ): Result<Unit> =
    runSafeFirestoreWrite("stores/expenses", OperationType.CREATE) {
      val ctx = resolveStoreContext()
      val numId = nextNumericId()
      val entity =
        ExpenseEntity(
          id = numId,
          title = title.trim(),
          category = category.label,
          amount = amount,
          notes = notes.trim(),
          recordedBy = recordedBy,
          recordedRole = recordedRole.name,
          timestamp = timestamp,
        )
      db.collection("stores")
        .document(ctx.storeId)
        .collection("expenses")
        .document(FirestoreMappers.expenseDocId(numId))
        .set(
          FirestoreMappers.expenseCreateMap(
            exp = entity,
            numericId = numId,
            storeId = ctx.storeId,
            ownerUid = ctx.ownerUid,
            recordedByUid = authUid,
            memberUids = ctx.memberUids,
          )
        )
        .await()
    }

  suspend fun deleteExpense(expenseId: Long): Result<Unit> =
    runSafeFirestoreWrite("stores/expenses/exp_$expenseId", OperationType.DELETE) {
      val ctx = resolveStoreContext()
      db.collection("stores")
        .document(ctx.storeId)
        .collection("expenses")
        .document(FirestoreMappers.expenseDocId(expenseId))
        .delete()
        .await()
    }

  private data class StoreContext(
    val storeId: String,
    val ownerUid: String,
    val memberUids: List<String>,
  )

  private suspend fun resolveActiveStoreId(): String {
    val cached = _cloudSyncState.value.storeId
    if (cached.isNotBlank()) return cached
    val userSnap = db.collection("users").document(authUid).get().await()
    return userSnap.getString("defaultStoreId")?.takeIf { it.isNotBlank() } ?: authUid
  }

  private suspend fun resolveStoreContext(): StoreContext {
    val storeId = resolveActiveStoreId()
    val snap = db.collection("stores").document(storeId).get().await()
    val ownerUid = snap.getString("ownerUid")?.takeIf { it.isNotBlank() } ?: authUid
    val members =
      FirestoreMappers.memberUidsFromSnapshot(snap).ifEmpty { listOf(ownerUid, authUid).distinct() }
    return StoreContext(storeId = storeId, ownerUid = ownerUid, memberUids = members)
  }

  private suspend inline fun <T> runSafeFirestoreWrite(
    path: String,
    op: OperationType,
    crossinline block: suspend () -> T,
  ): Result<T> =
    try {
      Result.success(block())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, op, path)
      Log.e(TAG, "Firestore operation failed at $path", e)
      Result.failure(e)
    }

  private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
    generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()

  private fun generateJoinCodeForUid(uid: String): String {
    val clean = uid.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
    val suffix =
      if (clean.length >= 6) clean.take(6)
      else clean.padEnd(6, '8')
    return "TB$suffix"
  }

  private suspend fun generateAvailableJoinCode(): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    repeat(6) {
      val candidate = "TB" + (1..6).map { alphabet.random() }.joinToString("")
      val exists =
        runCatching { db.collection("storeInvites").document(candidate).get().await().exists() }
          .getOrDefault(false)
      if (!exists) return candidate
    }
    return "TB" + (System.currentTimeMillis() % 1_000_000L).toString().padStart(6, '0')
  }

  private companion object {
    const val TAG = "StoreRepository"
  }
}
