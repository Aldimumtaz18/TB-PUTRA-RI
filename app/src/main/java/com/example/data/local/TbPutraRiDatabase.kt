package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [ProductEntity::class, TransactionEntity::class, ExpenseEntity::class],
  version = 3,
  exportSchema = false,
)
abstract class TbPutraRiDatabase : RoomDatabase() {
  abstract fun productDao(): ProductDao
  abstract fun transactionDao(): TransactionDao
  abstract fun expenseDao(): ExpenseDao

  companion object {
    @Volatile private var INSTANCE: TbPutraRiDatabase? = null

    fun getInstance(context: Context): TbPutraRiDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                TbPutraRiDatabase::class.java,
                "tb_putra_ri_pos.db",
              )
              .fallbackToDestructiveMigration(true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
