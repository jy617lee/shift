import XCTest
@testable import shift

// MARK: - Test Doubles

final class StubScheduleRepository: ScheduleRepository {
    var weeks: [ScheduleWeek] = []
    var shouldThrow = false

    func getAllWeeks() async throws -> [ScheduleWeek] {
        if shouldThrow { throw StubError.generic }
        return weeks
    }

    func getWeekByDate(_ date: Date) async throws -> ScheduleWeek? {
        let cal = Calendar.current
        return weeks.first { week in
            guard let end = cal.date(byAdding: .day, value: 7, to: week.weekStartDate) else { return false }
            return week.weekStartDate <= date && date < end
        }
    }

    func getWeeksInRange(from: Date, to: Date) async throws -> [ScheduleWeek] { weeks }
    func getNextWeekFrom(_ date: Date) async throws -> ScheduleWeek? { nil }
    func saveWeek(_ week: ScheduleWeek) async throws {}
    func replaceWeek(_ week: ScheduleWeek) async throws {}
    func deleteWeek(weekStartDate: Date) async throws {}
}

final class StubPreferencesRepository: UserPreferencesRepository {
    var skipConfirm = false
    var skipConfirmPromptShown = false

    func isSkipConfirm() -> Bool { skipConfirm }
    func setSkipConfirm(_ value: Bool) { skipConfirm = value }
    func isSkipConfirmPromptShown() -> Bool { skipConfirmPromptShown }
    func setSkipConfirmPromptShown(_ value: Bool) { skipConfirmPromptShown = value }
}

enum StubError: Error { case generic }

// MARK: - Shared helpers

private func makeTestWeek(startYear: Int = 2026, startMonth: Int = 6, startDay: Int = 8) -> ScheduleWeek {
    let cal = Calendar.current
    let monday = cal.date(from: DateComponents(year: startYear, month: startMonth, day: startDay))!
    let days = (0..<7).map { offset -> ScheduleDay in
        let date = cal.date(byAdding: .day, value: offset, to: monday)!
        return ScheduleDay(
            date: date, type: offset < 5 ? .work : .other,
            startTime: nil, endTime: nil, codeLabel: "정상", source: .parsed
        )
    }
    return ScheduleWeek(weekStartDate: monday, days: days)
}

// MARK: - HomeViewModelTests

@MainActor
final class HomeViewModelTests: XCTestCase {

    func testLoadWeeksPopulatesArray() async throws {
        let repo = StubScheduleRepository()
        repo.weeks = [makeTestWeek()]
        let vm = HomeViewModel(repository: repo)
        await vm.loadWeeks()
        XCTAssertEqual(vm.weeks.count, 1)
        XCTAssertFalse(vm.hasError)
        XCTAssertFalse(vm.isLoading)
    }

    func testLoadWeeksSortedAscending() async throws {
        let repo = StubScheduleRepository()
        let cal = Calendar.current
        let date1 = cal.date(from: DateComponents(year: 2026, month: 6, day: 15))!
        let date2 = cal.date(from: DateComponents(year: 2026, month: 6, day: 8))!
        repo.weeks = [
            ScheduleWeek(weekStartDate: date1, days: makeTestWeek(startDay: 15).days),
            ScheduleWeek(weekStartDate: date2, days: makeTestWeek(startDay: 8).days),
        ]
        let vm = HomeViewModel(repository: repo)
        await vm.loadWeeks()
        XCTAssertEqual(vm.weeks[0].weekStartDate, date2)
        XCTAssertEqual(vm.weeks[1].weekStartDate, date1)
    }

    func testLoadWeeksSetsErrorOnFailure() async throws {
        let repo = StubScheduleRepository()
        repo.shouldThrow = true
        let vm = HomeViewModel(repository: repo)
        await vm.loadWeeks()
        XCTAssertTrue(vm.hasError)
        XCTAssertFalse(vm.isLoading)
    }

    func testLoadWeeksEmptyRepoReturnsEmpty() async throws {
        let vm = HomeViewModel(repository: StubScheduleRepository())
        await vm.loadWeeks()
        XCTAssertTrue(vm.weeks.isEmpty)
        XCTAssertFalse(vm.hasError)
    }
}

// MARK: - UserDefaultsPreferencesRepositoryTests

final class UserDefaultsPreferencesRepositoryTests: XCTestCase {
    private var suiteName: String!
    private var repo: UserDefaultsPreferencesRepository!

    override func setUp() {
        suiteName = "test-\(UUID().uuidString)"
        repo = UserDefaultsPreferencesRepository(defaults: UserDefaults(suiteName: suiteName)!)
    }

    override func tearDown() {
        UserDefaults.standard.removeSuite(named: suiteName)
    }

    func testSkipConfirmDefaultsFalse() {
        XCTAssertFalse(repo.isSkipConfirm())
    }

    func testSetSkipConfirmTrue() {
        repo.setSkipConfirm(true)
        XCTAssertTrue(repo.isSkipConfirm())
    }

    func testSetSkipConfirmFalse() {
        repo.setSkipConfirm(true)
        repo.setSkipConfirm(false)
        XCTAssertFalse(repo.isSkipConfirm())
    }

    func testSkipConfirmPromptShownDefaultsFalse() {
        XCTAssertFalse(repo.isSkipConfirmPromptShown())
    }

    func testSetSkipConfirmPromptShownTrue() {
        repo.setSkipConfirmPromptShown(true)
        XCTAssertTrue(repo.isSkipConfirmPromptShown())
    }

    func testSkipConfirmAndPromptIndependent() {
        repo.setSkipConfirm(true)
        XCTAssertFalse(repo.isSkipConfirmPromptShown())
        repo.setSkipConfirmPromptShown(true)
        XCTAssertTrue(repo.isSkipConfirm())
    }
}
