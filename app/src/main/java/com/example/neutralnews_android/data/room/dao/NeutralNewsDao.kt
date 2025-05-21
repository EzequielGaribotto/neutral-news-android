package com.example.neutralnews_android.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neutralnews_android.data.room.entities.NeutralNewsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NeutralNewsDao {
    @Query("SELECT * FROM neutral_news ORDER BY createdAt DESC")
    fun getAllNews(): Flow<List<NeutralNewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(news: List<NeutralNewsEntity>)

    @Query("DELETE FROM neutral_news")
    suspend fun clearAll()
}

