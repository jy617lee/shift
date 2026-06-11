package com.schedule.shift.data.db.converter

import androidx.room.TypeConverter
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun toLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromDaysJson(value: String?): List<ScheduleDay>? =
        value?.let { deserializeDays(JSONArray(it)) }

    @TypeConverter
    fun toDaysJson(days: List<ScheduleDay>?): String? =
        days?.let { serializeDays(it).toString() }

    private fun serializeDays(days: List<ScheduleDay>): JSONArray {
        val array = JSONArray()
        days.forEach { day ->
            val obj = JSONObject()
            obj.put("date", day.date.toString())
            obj.put("type", day.type.name)
            obj.put("startTime", day.startTime?.toString())
            obj.put("endTime", day.endTime?.toString())
            obj.put("codeLabel", day.codeLabel)
            obj.put("source", day.source.name)
            array.put(obj)
        }
        return array
    }

    private fun deserializeDays(array: JSONArray): List<ScheduleDay> {
        val days = mutableListOf<ScheduleDay>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            days.add(
                ScheduleDay(
                    date = LocalDate.parse(obj.getString("date")),
                    type = DayType.valueOf(obj.getString("type")),
                    startTime = obj.optString("startTime").takeIf { it.isNotEmpty() && it != "null" }
                        ?.let { LocalTime.parse(it) },
                    endTime = obj.optString("endTime").takeIf { it.isNotEmpty() && it != "null" }
                        ?.let { LocalTime.parse(it) },
                    codeLabel = obj.getString("codeLabel"),
                    source = SourceType.valueOf(obj.getString("source")),
                ),
            )
        }
        return days
    }
}
