package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorshipCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface WorshipCommandDao {
    @Query("SELECT * FROM commands ORDER BY displayOrder ASC")
    fun getAllCommands(): Flow<List<WorshipCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: WorshipCommand): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommands(commands: List<WorshipCommand>)

    @Update
    suspend fun updateCommand(command: WorshipCommand)

    @Delete
    suspend fun deleteCommand(command: WorshipCommand)

    @Query("DELETE FROM commands")
    suspend fun deleteAllCommands()
}
