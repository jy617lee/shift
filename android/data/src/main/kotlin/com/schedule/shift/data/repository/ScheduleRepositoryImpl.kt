package com.schedule.shift.data.repository

import com.schedule.shift.data.db.ScheduleWeekDao
import com.schedule.shift.data.db.toDomain
import com.schedule.shift.data.db.toEntity
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleRepositoryImpl(
    private val dao: ScheduleWeekDao,
) : ScheduleRepository {

    override suspend fun getWeekByDate(date: LocalDate): ScheduleWeek? =
        withContext(Dispatchers.IO) {
            dao.getByWeekStart(date.mondayOfWeek())?.toDomain()
        }

    override suspend fun getNextWeekFrom(date: LocalDate): ScheduleWeek? =
        withContext(Dispatchers.IO) {
            dao.getNextWeekFrom(date.mondayOfWeek())?.toDomain()
        }

    private fun LocalDate.mondayOfWeek(): LocalDate =
        with(DayOfWeek.MONDAY).let { if (it.isAfter(this)) it.minusWeeks(1) else it }

    override suspend fun getWeeksInRange(from: LocalDate, to: LocalDate): List<ScheduleWeek> =
        withContext(Dispatchers.IO) {
            dao.getWeeksInRange(from.mondayOfWeek(), to).map { it.toDomain() }
        }

    override suspend fun getAllWeeks(): List<ScheduleWeek> =
        withContext(Dispatchers.IO) {
            dao.getAllWeeks().map { it.toDomain() }
        }

    override suspend fun saveWeek(week: ScheduleWeek) =
        withContext(Dispatchers.IO) {
            dao.insertWeek(week.toEntity())
        }

    override suspend fun replaceWeek(week: ScheduleWeek) =
        withContext(Dispatchers.IO) {
            dao.insertWeek(week.toEntity())
        }

    override suspend fun deleteWeek(weekStartDate: LocalDate) =
        withContext(Dispatchers.IO) {
            dao.deleteWeek(weekStartDate)
        }
}
