import Foundation

struct ScheduleDay: Codable, Hashable, Sendable {
    var date: Date
    var type: DayType
    var startTime: String?
    var endTime: String?
    var codeLabel: String
    var source: SourceType
}
