import Foundation
import WidgetKit

struct WidgetScheduleDay: Codable, Sendable {
    let date: Date
    let type: String
    let startTime: String?
    let endTime: String?
    let codeLabel: String

    var isWork: Bool { type == "WORK" }

    enum CodingKeys: String, CodingKey {
        case date, type, startTime, endTime, codeLabel
    }
}

struct WidgetEntry: TimelineEntry {
    let date: Date
    let days: [WidgetScheduleDay]

    var today: WidgetScheduleDay? {
        let cal = Calendar.current
        return days.first { cal.isDateInToday($0.date) }
    }

    func weekDays(around referenceDate: Date) -> [WidgetScheduleDay?] {
        let cal = Calendar.current
        let todayStart = cal.startOfDay(for: referenceDate)
        return (-2...2).map { offset -> WidgetScheduleDay? in
            guard let target = cal.date(byAdding: .day, value: offset, to: todayStart) else { return nil }
            return days.first { cal.isDate($0.date, inSameDayAs: target) }
        }
    }

    static var placeholder: WidgetEntry { WidgetEntry(date: Date(), days: []) }
}
