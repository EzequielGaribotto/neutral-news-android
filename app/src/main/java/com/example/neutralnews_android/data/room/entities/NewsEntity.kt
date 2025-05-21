package com.example.neutralnews_android.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String? = null,
    val imageUrl: String? = null,
    val link: String? = null,
    val pubDate: Long = 0,  // These should be timestamps, not strings
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val group: Int? = null,
    val neutralScore: Float? = null,
    val sourceMediumName: String? = null
)
