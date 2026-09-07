package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DirectMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectMessageDao {
    @Query("SELECT * FROM direct_messages ORDER BY id ASC")
    fun getAllDirectMessages(): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessages(messages: List<DirectMessageEntity>)

    @Update
    suspend fun updateDirectMessage(message: DirectMessageEntity)

    @Delete
    suspend fun deleteDirectMessage(message: DirectMessageEntity)

    @Query("DELETE FROM direct_messages")
    suspend fun deleteAllDirectMessages()
}
