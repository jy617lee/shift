package com.schedule.shift.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

@Dao
interface ScheduleWeekDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeek(week: ScheduleWeekEntity)

    @Query("SELECT * FROM schedule_weeks WHERE weekStartDate = :weekStartDate")
    suspend fun getByWeekStart(weekStartDate: LocalDate): ScheduleWeekEntity?

    @Query(
        "SELECT * FROM schedule_weeks WHERE weekStartDate >= :from AND weekStartDate <= :to ORDER BY weekStartDate ASC",
    )
    suspend fun getWeeksInRange(from: LocalDate, to: LocalDate): List<ScheduleWeekEntity>

    @Query("SELECT * FROM schedule_weeks ORDER BY weekStartDate ASC")
    suspend fun getAllWeeks(): List<ScheduleWeekEntity>

    @Query("SELECT * FROM schedule_weeks WHERE weekStartDate >= :from ORDER BY weekStartDate ASC LIMIT 1")
    suspend fun getNextWeekFrom(from: LocalDate): ScheduleWeekEntity?

    @Query("DELETE FROM schedule_weeks WHERE weekStartDate = :weekStartDate")
    suspend fun deleteWeek(weekStartDate: LocalDate)
}
