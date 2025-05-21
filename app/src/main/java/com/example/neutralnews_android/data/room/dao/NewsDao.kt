package com.example.neutralnews_android.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neutralnews_android.data.room.entities.NewsEntity

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY pubDate DESC")
    fun getAllNews(): List<NewsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(news: List<NewsEntity>)

    @Query("DELETE FROM news")
    suspend fun clearAll()
}