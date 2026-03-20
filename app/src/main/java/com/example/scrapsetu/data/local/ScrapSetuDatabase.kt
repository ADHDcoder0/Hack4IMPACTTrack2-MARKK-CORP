package com.example.scrapsetu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ScrapSetuDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
