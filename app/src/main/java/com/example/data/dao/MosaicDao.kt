package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Mosaic
import kotlinx.coroutines.flow.Flow

@Dao
interface MosaicDao {
    @Query("SELECT * FROM mosaics ORDER BY dateCreated DESC")
    fun getAllMosaics(): Flow<List<Mosaic>>

    @Query("SELECT * FROM mosaics WHERE id = :id")
    suspend fun getMosaicById(id: Long): Mosaic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMosaic(mosaic: Mosaic): Long

    @Update
    suspend fun updateMosaic(mosaic: Mosaic)

    @Delete
    suspend fun deleteMosaic(mosaic: Mosaic)
}
