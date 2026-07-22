package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val section: String, // "inicio", "logistica", "reputacion", "ropa"
    val isCompleted: Boolean = false
)
