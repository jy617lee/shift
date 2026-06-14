import Foundation
import Observation
import SwiftData

@MainActor
@Observable
final class AppContainer {
    let modelContainer: ModelContainer
    let repository: any ScheduleRepository
    let preferencesRepository: any UserPreferencesRepository
    let ocrEngine: any OcrEngine
    let parser: any ScheduleParser
    let reporter: any FailedImageReporter

    var processImageUseCase: ProcessScheduleImageUseCase {
        ProcessScheduleImageUseCase(ocrEngine: ocrEngine, parser: parser, reporter: reporter)
    }

    init() throws {
        let container = try ModelContainer(for: ScheduleWeekEntry.self)
        modelContainer = container
        repository = SwiftDataScheduleRepository(context: container.mainContext)
        preferencesRepository = UserDefaultsPreferencesRepository()
        ocrEngine = VisionOcrEngine()
        parser = ScheduleParserImpl()
        reporter = NoOpFailedImageReporter()
    }
}
