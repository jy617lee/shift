import Foundation
import Observation
import SwiftUI
import UIKit

struct EditingState: Equatable {
    var weekIndex: Int
    var dayIndex: Int
    var draft: ScheduleDay
}

@MainActor
@Observable
final class ConfirmationViewModel {
    private let repository: any ScheduleRepository
    private let preferencesRepository: any UserPreferencesRepository

    var weeks: [ScheduleWeek]
    var image: UIImage?
    var editing: EditingState?
    var conflictCount = 0
    var showSkipPrompt = false
    var savedSuccessfully = false
    var savedWeeksCount = 0
    var isCancelled = false
    var toastMessage: String?

    init(
        weeks: [ScheduleWeek],
        image: UIImage?,
        repository: any ScheduleRepository,
        preferencesRepository: any UserPreferencesRepository
    ) {
        self.weeks = weeks
        self.image = image
        self.repository = repository
        self.preferencesRepository = preferencesRepository
    }

    func confirm() {
        Task { await runConfirm() }
    }

    private func runConfirm() async {
        var conflicts = 0
        for week in weeks {
            conflicts += (try? await repository.getWeekByDate(week.weekStartDate)) != nil ? 1 : 0
        }
        conflictCount = conflicts
        if conflicts == 0 { await saveAllAndFinish() }
    }

    func proceedWithReplace() {
        Task {
            do {
                for week in weeks {
                    if (try? await repository.getWeekByDate(week.weekStartDate)) != nil {
                        try await repository.replaceWeek(week)
                    } else {
                        try await repository.saveWeek(week)
                    }
                }
                conflictCount = 0
                await finishAfterSave()
            } catch {
                conflictCount = 0
                showToast(saveErrorMessage(error))
            }
        }
    }

    func dismissConflict() { conflictCount = 0 }

    private func saveAllAndFinish() async {
        do {
            for week in weeks { try await repository.saveWeek(week) }
            await finishAfterSave()
        } catch {
            showToast(saveErrorMessage(error))
        }
    }

    func showToast(_ message: String) {
        toastMessage = message
        Task {
            try? await Task.sleep(for: .seconds(3))
            withAnimation { self.toastMessage = nil }
        }
    }

    private func saveErrorMessage(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == NSPOSIXErrorDomain && nsError.code == 28 {
            return "저장 공간이 부족합니다"
        }
        return "저장에 실패했어요. 다시 시도해주세요."
    }

    private func finishAfterSave() async {
        if !preferencesRepository.isSkipConfirmPromptShown() {
            showSkipPrompt = true
        } else {
            markSaved()
        }
    }

    func answerSkipPrompt(skipInFuture: Bool) {
        preferencesRepository.setSkipConfirm(skipInFuture)
        preferencesRepository.setSkipConfirmPromptShown(true)
        markSaved()
    }

    func cancel() { isCancelled = true }

    func startEdit(weekIndex: Int, dayIndex: Int) {
        guard weekIndex < weeks.count, dayIndex < weeks[weekIndex].days.count else { return }
        let day = weeks[weekIndex].days[dayIndex]
        editing = EditingState(weekIndex: weekIndex, dayIndex: dayIndex, draft: day)
    }

    func updateDraft(_ draft: ScheduleDay) {
        editing?.draft = draft
    }

    func commitEdit() {
        guard let editing else { return }
        var updated = weeks
        var days = updated[editing.weekIndex].days
        days[editing.dayIndex] = editing.draft
        updated[editing.weekIndex] = ScheduleWeek(
            weekStartDate: weeks[editing.weekIndex].weekStartDate, days: days
        )
        weeks = updated
        self.editing = nil
    }

    func dismissEdit() { editing = nil }

    private func markSaved() {
        savedWeeksCount = weeks.count
        savedSuccessfully = true
    }
}
