package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val doi: String,
    val title: String,
    val creator: String?,
    val publicationName: String?,
    val coverDate: String?,
    val scopusUrl: String?,
    val localPdfPath: String?,
    val isBookmarked: Boolean,
    val savedAt: Long = System.currentTimeMillis()
)
