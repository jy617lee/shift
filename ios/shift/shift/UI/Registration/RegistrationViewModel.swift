import Foundation
import Observation
import PhotosUI
import SwiftUI

enum RegistrationPhase: Equatable {
    case idle
    case processing
    case readyToReview
    case skipModeConflict(count: Int)
    case skipModeSaved(weeksCount: Int)
    case notASchedule
    case parseError
    case saveError
}

@MainActor
@Observable
final class RegistrationViewModel {
    private let processImage: ProcessScheduleImageUseCase
    private let preferencesRepository: any UserPreferencesRepository
    private let repository: any ScheduleRepository

    var phase: RegistrationPhase = .idle
    var parsedWeeks: [ScheduleWeek] = []
    var capturedImage: UIImage?

    init(
        processImage: ProcessScheduleImageUseCase,
        preferencesRepository: any UserPreferencesRepository,
        repository: any ScheduleRepository
    ) {
        self.processImage = processImage
        self.preferencesRepository = preferencesRepository
        self.repository = repository
    }

    func processSelectedItem(_ item: PhotosPickerItem) async {
        phase = .processing
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else {
            phase = .parseError
            return
        }
        capturedImage = image
        let result = await processImage.invoke(image: image)
        switch result {
        case .success(let weeks):
            await handleSuccess(weeks: weeks, image: image)
        case .failure(let reason):
            capturedImage = reason == .parseError ? image : nil
            phase = reason == .notASchedule ? .notASchedule : .parseError
        }
    }

    private func handleSuccess(weeks: [ScheduleWeek], image: UIImage) async {
        parsedWeeks = weeks
        if preferencesRepository.isSkipConfirm() {
            await runSkipFlow(weeks: weeks)
        } else {
            phase = .readyToReview
        }
    }

    private func runSkipFlow(weeks: [ScheduleWeek]) async {
        var conflictCount = 0
        for week in weeks {
            conflictCount += (try? await repository.getWeekByDate(week.weekStartDate)) != nil ? 1 : 0
        }
        if conflictCount > 0 {
            phase = .skipModeConflict(count: conflictCount)
        } else {
            await directSave(weeks: weeks)
        }
    }

    func proceedSkipReplace() async {
        do {
            for week in parsedWeeks {
                if (try? await repository.getWeekByDate(week.weekStartDate)) != nil {
                    try await repository.replaceWeek(week)
                } else {
                    try await repository.saveWeek(week)
                }
            }
            phase = .skipModeSaved(weeksCount: parsedWeeks.count)
        } catch {
            phase = .saveError
        }
    }

    private func directSave(weeks: [ScheduleWeek]) async {
        do {
            for week in weeks { try await repository.saveWeek(week) }
            phase = .skipModeSaved(weeksCount: weeks.count)
        } catch {
            phase = .saveError
        }
    }

    func reportFailure(image: UIImage) {
        processImage.reportFailure(image: image, reason: "parse_error")
    }

    func reset() {
        phase = .idle
        parsedWeeks = []
        capturedImage = nil
    }
}
