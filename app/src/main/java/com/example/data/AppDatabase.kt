package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CatalogDao
import com.example.data.dao.ChecklistItemDao
import com.example.data.dao.DropshipProductDao
import com.example.data.model.CatalogProduct
import com.example.data.model.CategoryItem
import com.example.data.model.ChecklistItem
import com.example.data.model.CompanyProfile
import com.example.data.model.DropshipProduct

@Database(
    entities = [
        DropshipProduct::class,
        ChecklistItem::class,
        CatalogProduct::class,
        CategoryItem::class,
        CompanyProfile::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dropshipProductDao(): DropshipProductDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dropship_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.let { db ->
                    if (db.isOpen) {
                        db.close()
                    }
                }
                INSTANCE = null
            }
        }
    }
}
