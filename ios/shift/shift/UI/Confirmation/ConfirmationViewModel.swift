import Foundation
import Observation
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
            for week in weeks {
                if (try? await repository.getWeekByDate(week.weekStartDate)) != nil {
                    try? await repository.replaceWeek(week)
                } else {
                    try? await repository.saveWeek(week)
                }
            }
            conflictCount = 0
            await finishAfterSave()
        }
    }

    func dismissConflict() { conflictCount = 0 }

    private func saveAllAndFinish() async {
        for week in weeks { try? await repository.saveWeek(week) }
        await finishAfterSave()
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
