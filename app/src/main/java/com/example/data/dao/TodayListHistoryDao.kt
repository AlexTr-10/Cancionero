package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TodayListHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayListHistoryDao {
    @Query("SELECT * FROM today_list_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TodayListHistory>>

    @Query("SELECT * FROM today_list_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): TodayListHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TodayListHistory): Long

    @Delete
    suspend fun deleteHistory(history: TodayListHistory)

    @Query("DELETE FROM today_list_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM today_list_history")
    suspend fun clearHistory()
}
