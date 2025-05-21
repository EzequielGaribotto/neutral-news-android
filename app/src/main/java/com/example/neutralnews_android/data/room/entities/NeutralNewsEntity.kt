package com.example.neutralnews_android.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.neutralnews_android.data.room.converters.StringListConverter

@Entity(tableName = "neutral_news")
data class NeutralNewsEntity(
    @PrimaryKey
    val id: String,
    val neutralTitle: String,
    val neutralDescription: String,
    val category: String? = null,
    val imageUrl: String? = null,
    val group: Int? = null,
    val createdAt: Long = 0,
    val date: Long = 0,
    val updatedAt: Long = 0,
    val imageMedium: String ? = null,

    @TypeConverters(StringListConverter::class)
    val sourceIds: List<String>? = null,
    val relevance: Double? = null
)
