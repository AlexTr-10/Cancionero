package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commands")
data class WorshipCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val displayOrder: Int
)
