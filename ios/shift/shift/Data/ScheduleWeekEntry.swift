import Foundation
import SwiftData

@Model
final class ScheduleWeekEntry {
    @Attribute(.unique) var weekStartDate: Date
    var daysJSON: Data

    init(weekStartDate: Date, daysJSON: Data) {
        self.weekStartDate = weekStartDate
        self.daysJSON = daysJSON
    }

    func toScheduleWeek() throws -> ScheduleWeek {
        let days = try JSONDecoder().decode([ScheduleDay].self, from: daysJSON)
        return ScheduleWeek(weekStartDate: weekStartDate, days: days)
    }

    static func from(_ week: ScheduleWeek) throws -> ScheduleWeekEntry {
        let data = try JSONEncoder().encode(week.days)
        return ScheduleWeekEntry(weekStartDate: week.weekStartDate, daysJSON: data)
    }
}
