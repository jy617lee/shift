import Foundation
import WidgetKit

struct WidgetScheduleDay: Codable, Sendable {
    let date: Date
    let type: String
    let startTime: String?
    let endTime: String?
    let codeLabel: String

    var isWork: Bool { type == "WORK" }

    var displayOffText: String {
        codeLabel.isEmpty ? "휴무" : codeLabel
    }

    func shiftStartDate(on referenceDate: Date) -> Date? {
        parseTime(startTime, on: referenceDate)
    }

    func shiftEndDate(on referenceDate: Date) -> Date? {
        parseTime(endTime, on: referenceDate)
    }

    private func parseTime(_ timeString: String?, on referenceDate: Date) -> Date? {
        guard let timeString else { return nil }
        let parts = timeString.split(separator: ":").compactMap { Int($0) }
        guard parts.count == 2 else { return nil }
        var comps = Calendar.current.dateComponents([.year, .month, .day], from: referenceDate)
        comps.hour = parts[0]
        comps.minute = parts[1]
        comps.second = 0
        return Calendar.current.date(from: comps)
    }

    enum CodingKeys: String, CodingKey {
        case date, type, startTime, endTime, codeLabel
    }
}

struct WidgetEntry: TimelineEntry {
    let date: Date
    let days: [WidgetScheduleDay]
    let targetDate: Date

    /// 표시 대상 날짜의 스케줄 (오늘 또는 내일)
    var today: WidgetScheduleDay? {
        days.first { Calendar.current.isDate($0.date, inSameDayAs: targetDate) }
    }

    var isShowingTomorrow: Bool {
        !Calendar.current.isDateInToday(targetDate)
    }

    func nextWorkDay(after referenceDate: Date) -> WidgetScheduleDay? {
        let cal = Calendar.current
        let start = cal.startOfDay(for: referenceDate)
        return days
            .filter { !cal.isDate($0.date, inSameDayAs: start) && $0.date > start && $0.isWork }
            .min(by: { $0.date < $1.date })
    }

    func weekDays(around referenceDate: Date) -> [WidgetScheduleDay?] {
        let cal = Calendar.current
        let todayStart = cal.startOfDay(for: referenceDate)
        return (-2...2).map { offset -> WidgetScheduleDay? in
            guard let target = cal.date(byAdding: .day, value: offset, to: todayStart) else { return nil }
            return days.first { cal.isDate($0.date, inSameDayAs: target) }
        }
    }

    static var placeholder: WidgetEntry {
        WidgetEntry(date: Date(), days: [], targetDate: Date())
    }
}
