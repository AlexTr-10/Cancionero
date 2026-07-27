package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val fileUriOrPath: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long = 0,
    val notes: String = "",
    val tags: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)
