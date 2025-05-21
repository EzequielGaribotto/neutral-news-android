package com.example.neutralnews_android.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.neutralnews_android.data.Constants.LocalNew.NEWS_DATABASE
import com.example.neutralnews_android.data.room.converters.StringListConverter
import com.example.neutralnews_android.data.room.dao.NeutralNewsDao
import com.example.neutralnews_android.data.room.dao.NewsDao
import com.example.neutralnews_android.data.room.entities.NeutralNewsEntity
import com.example.neutralnews_android.data.room.entities.NewsEntity

@Database(
    entities = [NeutralNewsEntity::class, NewsEntity::class],
    version = 9, // Incrementar versión para aplicar cambios
    exportSchema = false
)

@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun neutralNewsDao(): NeutralNewsDao
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NEWS_DATABASE
                )
                    .fallbackToDestructiveMigration() // Esto recreará la base de datos si hay cambios de esquema
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
