package com.example.data.dao

import androidx.room.*
import com.example.data.model.ActaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActaDao {
    @Query("SELECT * FROM actas ORDER BY createdAt DESC")
    fun getAllActas(): Flow<List<ActaEntity>>

    @Query("SELECT * FROM actas ORDER BY createdAt DESC")
    suspend fun getActasSync(): List<ActaEntity>

    @Query("SELECT * FROM actas WHERE id = :id")
    suspend fun getActaById(id: String): ActaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActa(acta: ActaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActas(actas: List<ActaEntity>)

    @Update
    suspend fun updateActa(acta: ActaEntity)

    @Delete
    suspend fun deleteActa(acta: ActaEntity)

    @Query("DELETE FROM actas WHERE id = :id")
    suspend fun deleteActaById(id: String)

    @Query("DELETE FROM actas")
    suspend fun deleteAllActas()
}
