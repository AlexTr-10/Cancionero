package com.example.data.dao

import androidx.room.*
import com.example.data.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items")
    fun getAllItems(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllItemsList(): List<ChecklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItem>)

    @Query("UPDATE checklist_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM checklist_items")
    suspend fun getItemCount(): Int

    @Query("DELETE FROM checklist_items")
    suspend fun clearAllChecklistItems()
}
