package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val key: String,
    val lyrics: String,
    val notes: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val displayOrder: Int = 0
)
