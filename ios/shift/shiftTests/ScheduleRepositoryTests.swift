import XCTest
import SwiftData
@testable import shift

@MainActor
final class ScheduleRepositoryTests: XCTestCase {
    var container: ModelContainer!
    var repository: SwiftDataScheduleRepository!

    override func setUp() async throws {
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        container = try ModelContainer(for: ScheduleWeekEntry.self, configurations: config)
        repository = SwiftDataScheduleRepository(context: container.mainContext)
    }

    override func tearDown() async throws {
        container = nil
        repository = nil
    }

    func testSaveAndRetrieveWeek() async throws {
        let week = makeWeek(year: 2026, month: 6, day: 8)
        try await repository.saveWeek(week)

        let all = try await repository.getAllWeeks()
        XCTAssertEqual(all.count, 1)
        XCTAssertEqual(all.first?.weekStartDate, week.weekStartDate)
    }

    func testGetWeekByDate() async throws {
        let week = makeWeek(year: 2026, month: 6, day: 8)
        try await repository.saveWeek(week)

        let date = Calendar.current.date(from: DateComponents(year: 2026, month: 6, day: 10))!
        let found = try await repository.getWeekByDate(date)
        XCTAssertNotNil(found)
        XCTAssertEqual(found?.weekStartDate, week.weekStartDate)
    }

    func testGetWeekByDateMissing() async throws {
        let date = Calendar.current.date(from: DateComponents(year: 2026, month: 6, day: 10))!
        let found = try await repository.getWeekByDate(date)
        XCTAssertNil(found)
    }

    func testReplaceWeek() async throws {
        let week = makeWeek(year: 2026, month: 6, day: 8)
        try await repository.saveWeek(week)

        var updatedDays = week.days
        updatedDays[0] = ScheduleDay(
            date: updatedDays[0].date, type: .off,
            startTime: nil, endTime: nil, codeLabel: "대체", source: .edited
        )
        let updated = ScheduleWeek(weekStartDate: week.weekStartDate, days: updatedDays)
        try await repository.replaceWeek(updated)

        let all = try await repository.getAllWeeks()
        XCTAssertEqual(all.count, 1)
        XCTAssertEqual(all.first?.days[0].type, .off)
    }

    func testDeleteWeek() async throws {
        let week = makeWeek(year: 2026, month: 6, day: 8)
        try await repository.saveWeek(week)
        try await repository.deleteWeek(weekStartDate: week.weekStartDate)

        let all = try await repository.getAllWeeks()
        XCTAssertTrue(all.isEmpty)
    }

    func testGetAllWeeksReturnsSortedOrder() async throws {
        let week2 = makeWeek(year: 2026, month: 6, day: 15)
        let week1 = makeWeek(year: 2026, month: 6, day: 8)
        try await repository.saveWeek(week2)
        try await repository.saveWeek(week1)

        let all = try await repository.getAllWeeks()
        XCTAssertEqual(all.count, 2)
        XCTAssertTrue(all[0].weekStartDate < all[1].weekStartDate)
    }

    func testDaysArePreservedRoundTrip() async throws {
        let week = makeWeek(year: 2026, month: 6, day: 8)
        try await repository.saveWeek(week)

        let retrieved = try await repository.getAllWeeks()
        XCTAssertEqual(retrieved.first?.days.count, ScheduleWeek.daysInWeek)
        XCTAssertEqual(retrieved.first?.days[0].codeLabel, week.days[0].codeLabel)
        XCTAssertEqual(retrieved.first?.days[0].type, week.days[0].type)
    }

    private func makeWeek(year: Int, month: Int, day: Int) -> ScheduleWeek {
        let cal = Calendar.current
        let monday = cal.date(from: DateComponents(year: year, month: month, day: day))!
        let days = (0..<7).map { offset -> ScheduleDay in
            let date = cal.date(byAdding: .day, value: offset, to: monday)!
            return ScheduleDay(
                date: date, type: offset < 5 ? .work : .other,
                startTime: offset < 5 ? "09:00" : nil,
                endTime: offset < 5 ? "18:00" : nil,
                codeLabel: offset < 5 ? "정상" : "정규휴일",
                source: .parsed
            )
        }
        return ScheduleWeek(weekStartDate: monday, days: days)
    }
}
