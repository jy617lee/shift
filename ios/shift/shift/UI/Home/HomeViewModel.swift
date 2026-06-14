import Observation
import SwiftUI

@MainActor
@Observable
final class HomeViewModel {
    private let repository: any ScheduleRepository

    var weeks: [ScheduleWeek] = []
    var isLoading = false
    var hasError = false
    var toastMessage: String?

    init(repository: any ScheduleRepository) {
        self.repository = repository
        Task { await loadWeeks() }
    }

    func loadWeeks() async {
        isLoading = true
        hasError = false
        do {
            weeks = try await repository.getAllWeeks()
                .sorted { $0.weekStartDate < $1.weekStartDate }
        } catch {
            hasError = true
        }
        isLoading = false
    }

    func showToast(_ message: String) {
        toastMessage = message
        Task {
            try? await Task.sleep(for: .seconds(3))
            withAnimation { self.toastMessage = nil }
        }
    }
}
