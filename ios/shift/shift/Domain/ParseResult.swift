import Foundation

enum ParseResult {
    case success([ScheduleWeek])
    case failure(FailureReason)
}
