package com.schedule.shift.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import java.time.LocalDate

@Entity(tableName = "schedule_weeks")
data class ScheduleWeekEntity(
    @PrimaryKey val weekStartDate: LocalDate,
    val days: List<ScheduleDay>,
)

fun ScheduleWeekEntity.toDomain() = ScheduleWeek(
    weekStartDate = weekStartDate,
    days = days,
)

fun ScheduleWeek.toEntity() = ScheduleWeekEntity(
    weekStartDate = weekStartDate,
    days = days,
)
