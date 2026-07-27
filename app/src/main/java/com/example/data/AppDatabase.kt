package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ActaDao
import com.example.data.dao.DocumentDao
import com.example.data.dao.MosaicDao
import com.example.data.dao.SongDao
import com.example.data.dao.WorshipCommandDao
import com.example.data.model.ActaEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.Mosaic
import com.example.data.model.Song
import com.example.data.model.WorshipCommand

@Database(entities = [Song::class, Mosaic::class, WorshipCommand::class, ActaEntity::class, DocumentEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun mosaicDao(): MosaicDao
    abstract fun commandDao(): WorshipCommandDao
    abstract fun actaDao(): ActaDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cancionero_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
