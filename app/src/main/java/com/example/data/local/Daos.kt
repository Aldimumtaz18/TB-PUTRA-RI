package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY name ASC")
  fun observeAll(): Flow<List<ProductEntity>>

  @Query("SELECT COUNT(*) FROM products")
  suspend fun count(): Int

  @Query("SELECT * FROM products WHERE id = :id")
  suspend fun getById(id: Long): ProductEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(product: ProductEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(products: List<ProductEntity>)

  @Update
  suspend fun update(product: ProductEntity)

  @Query("UPDATE products SET stockQuantity = MAX(0.0, stockQuantity + :delta), updatedAt = :timestamp WHERE id = :id")
  suspend fun adjustStock(id: Long, delta: Double, timestamp: Long = System.currentTimeMillis())

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM products")
  suspend fun deleteAll()
}

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
  fun observeAll(): Flow<List<TransactionEntity>>

  @Query("SELECT COUNT(*) FROM transactions")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(transaction: TransactionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<TransactionEntity>)

  @Query("UPDATE transactions SET isPaid = 1, paidAt = :paidAt, amountPaid = totalAmount WHERE id = :id")
  suspend fun markKasbonPaid(id: Long, paidAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM transactions")
  suspend fun deleteAll()
}

@Dao
interface ExpenseDao {
  @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
  fun observeAll(): Flow<List<ExpenseEntity>>

  @Query("SELECT COUNT(*) FROM expenses")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(expense: ExpenseEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(expenses: List<ExpenseEntity>)

  @Query("DELETE FROM expenses WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM expenses")
  suspend fun deleteAll()
}
