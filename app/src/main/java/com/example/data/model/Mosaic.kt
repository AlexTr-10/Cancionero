package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosaics")
data class Mosaic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val songIds: String, // Comma-separated list of song IDs, e.g., "1,5,10"
    val dateCreated: Long = System.currentTimeMillis()
)
