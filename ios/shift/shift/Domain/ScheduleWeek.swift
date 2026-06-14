import Foundation

struct ScheduleWeek: Hashable, Sendable {
    static let daysInWeek = 7

    let weekStartDate: Date
    let days: [ScheduleDay]

    init(weekStartDate: Date, days: [ScheduleDay]) {
        precondition(days.count == Self.daysInWeek, "ScheduleWeek must have exactly 7 days")
        self.weekStartDate = weekStartDate
        self.days = days
    }
}
