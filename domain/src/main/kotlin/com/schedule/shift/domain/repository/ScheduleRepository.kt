package com.schedule.shift.domain.repository

import com.schedule.shift.domain.model.ScheduleWeek
import java.time.LocalDate

interface ScheduleRepository {
    suspend fun getWeekByDate(date: LocalDate): ScheduleWeek?
    suspend fun getWeeksInRange(from: LocalDate, to: LocalDate): List<ScheduleWeek>
    suspend fun getAllWeeks(): List<ScheduleWeek>
    suspend fun saveWeek(week: ScheduleWeek)
    suspend fun replaceWeek(week: ScheduleWeek)
    suspend fun deleteWeek(weekStartDate: LocalDate)
}
