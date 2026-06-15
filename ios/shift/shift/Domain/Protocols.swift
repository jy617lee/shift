import UIKit

protocol ScheduleRepository {
    func getAllWeeks() async throws -> [ScheduleWeek]
    func getWeekByDate(_ date: Date) async throws -> ScheduleWeek?
    func getWeeksInRange(from: Date, to: Date) async throws -> [ScheduleWeek]
    func getNextWeekFrom(_ date: Date) async throws -> ScheduleWeek?
    func saveWeek(_ week: ScheduleWeek) async throws
    func replaceWeek(_ week: ScheduleWeek) async throws
    func deleteWeek(weekStartDate: Date) async throws
}

protocol UserPreferencesRepository {
    func isSkipConfirm() -> Bool
    func setSkipConfirm(_ value: Bool)
    func isSkipConfirmPromptShown() -> Bool
    func setSkipConfirmPromptShown(_ value: Bool)
}

protocol FailedImageReporter {
    func reportFailure(image: UIImage, errorReason: String)
}

protocol OcrEngine {
    func recognizeText(image: UIImage) async -> OcrResult
}

protocol ScheduleParser {
    func parse(_ text: String) async -> ParseResult
}
