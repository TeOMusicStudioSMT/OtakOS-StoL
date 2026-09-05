package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.StolDao
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.data.entity.TaskCommentEntity

@Database(
    entities = [ArtifactEntity::class, ActionLogEntity::class, TaskCommentEntity::class],
    version = 4,
    exportSchema = false
)
abstract class StolDatabase : RoomDatabase() {
    abstract fun stolDao(): StolDao

    companion object {
        @Volatile
        private var INSTANCE: StolDatabase? = null

        fun getDatabase(context: Context): StolDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StolDatabase::class.java,
                    "stol_otakos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
