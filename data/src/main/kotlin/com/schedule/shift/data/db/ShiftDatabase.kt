package com.schedule.shift.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.schedule.shift.data.db.converter.Converters

@Database(entities = [ScheduleWeekEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ShiftDatabase : RoomDatabase() {
    abstract fun scheduleWeekDao(): ScheduleWeekDao

    companion object {
        const val DATABASE_NAME = "shift_db"
    }
}
