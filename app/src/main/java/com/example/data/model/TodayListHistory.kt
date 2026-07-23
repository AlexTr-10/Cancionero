package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "today_list_history")
data class TodayListHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateLabel: String,
    val songIds: String,
    val songTitlesSummary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
