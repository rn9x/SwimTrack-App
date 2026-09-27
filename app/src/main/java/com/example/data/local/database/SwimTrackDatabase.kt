package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.SwimTrackDao
import com.example.data.local.entities.CompetitionEntity
import com.example.data.local.entities.CompetitionResultEntity
import com.example.data.local.entities.SwimRecordEntity
import com.example.data.local.entities.SwimTypeConverters

@Database(
    entities = [
        SwimRecordEntity::class,
        CompetitionEntity::class,
        CompetitionResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SwimTypeConverters::class)
abstract class SwimTrackDatabase : RoomDatabase() {
    abstract fun swimTrackDao(): SwimTrackDao

    companion object {
        @Volatile
        private var INSTANCE: SwimTrackDatabase? = null

        fun getInstance(context: Context): SwimTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwimTrackDatabase::class.java,
                    "swim_track_database.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
