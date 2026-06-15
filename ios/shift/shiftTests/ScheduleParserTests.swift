// swiftlint:disable file_length
import XCTest
@testable import shift

final class ScheduleParserTests: XCTestCase {
    var parser: ScheduleParserImpl!
    private static let fixedToday = makeDate(year: 2026, month: 6, day: 12)

    override func setUp() {
        super.setUp()
        parser = ScheduleParserImpl(validator: Stage1Validator(), today: Self.fixedToday)
    }

    // MARK: - Stage 1 failures

    func testNotAScheduleReturnsFailure() async {
        let result = await parser.parse("오늘 점심은 김치찌개입니다.")
        guard case .failure(let reason) = result else { return XCTFail("Expected failure") }
        XCTAssertEqual(reason, .notASchedule)
    }

    func testKeywordOnlyWithoutDateReturnsNotASchedule() async {
        let result = await parser.parse("근무 관련 공지사항입니다.")
        guard case .failure(let reason) = result else { return XCTFail("Expected failure") }
        XCTAssertEqual(reason, .notASchedule)
    }

    // MARK: - Basic parsing

    func testParsesWorkAndOffDays() async {
        let text = sampleWeek()
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks.count, 1)
        XCTAssertEqual(weeks[0].days[0].type, .work)
        XCTAssertEqual(weeks[0].days[5].type, .other)
    }

    func testParsesWorkDayTimes() async {
        guard case .success(let weeks) = await parser.parse(sampleWeek()) else { return XCTFail() }
        let monday = weeks[0].days[0]
        XCTAssertEqual(monday.startTime, "09:00")
        XCTAssertEqual(monday.endTime, "18:00")
    }

    func testStoresCodeLabelVerbatim() async {
        let text = """
근무 스케쥴
06/08(월) 09:00~18:00 정상
06/09(화) 연차
06/10(수) 09:00~18:00 정상
06/11(목) 반차
06/12(금) 09:00~18:00 정상
06/13(토) 정규휴일
06/14(일) 정규휴일
"""
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[0].codeLabel, "정상")
        XCTAssertEqual(days[1].codeLabel, "연차")
        XCTAssertEqual(days[3].codeLabel, "반차")
        XCTAssertEqual(days[5].codeLabel, "정규휴일")
    }

    func testDayWithoutTimesIsOtherType() async {
        guard case .success(let weeks) = await parser.parse(sampleWeek()) else { return XCTFail() }
        let saturday = weeks[0].days[5]
        XCTAssertEqual(saturday.type, .other)
        XCTAssertNil(saturday.startTime)
        XCTAssertNil(saturday.endTime)
    }

    func testAssignsCorrectYear() async {
        guard case .success(let weeks) = await parser.parse(sampleWeek()) else { return XCTFail() }
        let comps = Calendar.current.dateComponents([.year, .month, .day], from: weeks[0].days[0].date)
        XCTAssertEqual(comps.year, 2026)
        XCTAssertEqual(comps.month, 6)
        XCTAssertEqual(comps.day, 8)
    }

    func testFillsMissingDaysWithUnregistered() async {
        let text = """
근무 스케쥴
06/10(수) 09:00~18:00 정상
06/11(목) 09:00~18:00 정상
06/12(금) 09:00~18:00 정상
"""
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks[0].days.count, ScheduleWeek.daysInWeek)
        XCTAssertEqual(weeks[0].days[0].type, .unregistered)
        XCTAssertEqual(weeks[0].days[1].type, .unregistered)
        XCTAssertEqual(weeks[0].days[2].type, .work)
    }

    func testParsesTwoWeeks() async throws {
        let text = try loadSample("sample_07_two_weeks.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks.count, 2)
    }

    func testSample01FullMonth() async throws {
        let text = try loadSample("sample_01_june_full_month.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertFalse(weeks.isEmpty)
        XCTAssertTrue(weeks.allSatisfy { $0.days.count == ScheduleWeek.daysInWeek })
    }

    func testSample03ShiftWorkCodes() async throws {
        let text = try loadSample("sample_03_shift_work.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let workDays = weeks.flatMap(\.days).filter { $0.type == .work }
        XCTAssertTrue(workDays.contains { $0.codeLabel == "조번" })
        XCTAssertTrue(workDays.contains { $0.codeLabel == "석번" })
    }

    func testSample05OcrNoise() async throws {
        let text = try loadSample("sample_05_ocr_noise.txt")
        let result = await parser.parse(text)
        guard case .success = result else { return XCTFail("Expected success") }
    }

    func testSample06SingleDigitDates() async throws {
        let text = try loadSample("sample_06_single_digit_dates.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let firstWork = weeks.flatMap(\.days).first { $0.type == .work }
        let comps = Calendar.current.dateComponents([.month, .day], from: firstWork!.date)
        XCTAssertEqual(comps.month, 7)
        XCTAssertEqual(comps.day, 6)
    }

    // MARK: - Real schedule samples (sample_11)

    func testSample11ParsesOneWeek() async throws {
        let text = try loadSample("sample_11_real_schedule_jun22.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks.count, 1)
        XCTAssertEqual(weeks[0].days.count, ScheduleWeek.daysInWeek)
        assertDate(weeks[0].weekStartDate, year: 2026, month: 6, day: 22)
    }

    func testSample11WorkDayTimes() async throws {
        let text = try loadSample("sample_11_real_schedule_jun22.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[0].startTime, "14:00")
        XCTAssertEqual(days[0].endTime, "19:30")
        XCTAssertEqual(days[1].startTime, "15:00")
        XCTAssertEqual(days[1].endTime, "20:30")
        XCTAssertEqual(days[6].startTime, "06:30")
        XCTAssertEqual(days[6].endTime, "12:00")
    }

    func testSample11DayTypes() async throws {
        let text = try loadSample("sample_11_real_schedule_jun22.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[0].type, .work)
        XCTAssertEqual(days[1].type, .work)
        XCTAssertEqual(days[2].type, .other)
        XCTAssertEqual(days[3].type, .work)
        XCTAssertEqual(days[4].type, .work)
        XCTAssertEqual(days[5].type, .other)
        XCTAssertEqual(days[6].type, .work)
    }

    func testAtSignIndicator() async {
        let text = """
주간 스케쥴 조회
06/22(월) @ 14:00 @ 19:30 @ 정상
06/23(화) @ 15:00 @ 20:30 @ 정상
06/24(수) 정규휴일
06/25(목) @ 15:00 @ 20:30 @ 정상
06/26(금) @ 15:00 @ 20:30 @ 정상
06/27(토) 정규휴일
06/28(일) 정규휴일
"""
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks[0].days[0].type, .work)
        XCTAssertEqual(weeks[0].days[0].startTime, "14:00")
        XCTAssertEqual(weeks[0].days[0].endTime, "19:30")
        XCTAssertEqual(weeks[0].days[2].type, .other)
    }

    // MARK: - Real schedule samples (sample_12 — carry-forward)

    func testSample12ParsesOneWeek() async throws {
        let text = try loadSample("sample_12_real_schedule_jun08_actual.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks.count, 1)
        assertDate(weeks[0].weekStartDate, year: 2026, month: 6, day: 8)
    }

    func testSample12DayTypes() async throws {
        let text = try loadSample("sample_12_real_schedule_jun08_actual.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[0].type, .other)
        XCTAssertEqual(days[1].type, .work)
        XCTAssertEqual(days[2].type, .work)
        XCTAssertEqual(days[3].type, .other)
        XCTAssertEqual(days[4].type, .work)
        XCTAssertEqual(days[5].type, .work)
        XCTAssertEqual(days[6].type, .work)
    }

    func testSample12WorkTimes() async throws {
        let text = try loadSample("sample_12_real_schedule_jun08_actual.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[1].startTime, "15:00")
        XCTAssertEqual(days[1].endTime, "20:30")
        XCTAssertEqual(days[5].startTime, "17:00")
        XCTAssertEqual(days[5].endTime, "22:30")
    }

    // MARK: - Real schedule samples (sample_13 — past dates)

    func testSample13ParsesOneWeek() async throws {
        let text = try loadSample("sample_13_real_schedule_apr27.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks.count, 1)
        assertDate(weeks[0].weekStartDate, year: 2026, month: 4, day: 27)
    }

    func testSample13YearIs2026NotFuture() async throws {
        let text = try loadSample("sample_13_real_schedule_apr27.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let year = Calendar.current.component(.year, from: weeks[0].days[0].date)
        XCTAssertEqual(year, 2026)
    }

    func testSample13DayTypes() async throws {
        let text = try loadSample("sample_13_real_schedule_apr27.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[0].type, .other)
        XCTAssertEqual(days[1].type, .work)
        XCTAssertEqual(days[2].type, .work)
        XCTAssertEqual(days[3].type, .work)
        XCTAssertEqual(days[4].type, .other)
        XCTAssertEqual(days[5].type, .other)
        XCTAssertEqual(days[6].type, .other)
    }

    func testSample13WorkTimes() async throws {
        let text = try loadSample("sample_13_real_schedule_apr27.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[1].startTime, "08:00")
        XCTAssertEqual(days[1].endTime, "17:00")
        XCTAssertEqual(days[2].startTime, "08:30")
        XCTAssertEqual(days[2].endTime, "18:30")
        XCTAssertEqual(days[3].startTime, "06:30")
        XCTAssertEqual(days[3].endTime, "15:30")
    }

    func testSample13CodeLabels() async throws {
        let text = try loadSample("sample_13_real_schedule_apr27.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[0].codeLabel, "정규휴일")
        XCTAssertEqual(days[1].codeLabel, "정상")
        XCTAssertEqual(days[4].codeLabel, "법정휴일")
        XCTAssertEqual(days[5].codeLabel, "연차휴가")
    }

    // MARK: - iOS Vision OCR duplicate time format (sample_14)

    func testSample14ParsesOneWeek() async throws {
        let text = try loadIOSSample("sample_14_ios_vision_duplicate_times.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        XCTAssertEqual(weeks.count, 1)
        assertDate(weeks[0].weekStartDate, year: 2026, month: 4, day: 27)
    }

    func testSample14DuplicateTimesDeduped() async throws {
        let text = try loadIOSSample("sample_14_ios_vision_duplicate_times.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[1].startTime, "08:00")
        XCTAssertEqual(days[1].endTime, "17:00")
        XCTAssertEqual(days[2].startTime, "08:30")
        XCTAssertEqual(days[2].endTime, "18:30")
        XCTAssertEqual(days[3].startTime, "06:30")
        XCTAssertEqual(days[3].endTime, "15:30")
    }

    func testSample14StartNotEqualEnd() async throws {
        let text = try loadIOSSample("sample_14_ios_vision_duplicate_times.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let workDays = weeks.flatMap(\.days).filter { $0.type == .work }
        for day in workDays {
            XCTAssertNotEqual(day.startTime, day.endTime, "start == end for \(day.date)")
        }
    }

    func testSample14CodeLabels() async throws {
        let text = try loadIOSSample("sample_14_ios_vision_duplicate_times.txt")
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days
        XCTAssertEqual(days[1].codeLabel, "정상")
        XCTAssertEqual(days[2].codeLabel, "정상")
        XCTAssertEqual(days[3].codeLabel, "정상")
        XCTAssertEqual(days[0].codeLabel, "정규휴일")
        XCTAssertEqual(days[4].codeLabel, "법정휴일")
    }

    func testRegisteredSymbolNoiseIsDedupedAndStripped() async {
        // ® is an iOS Vision OCR artifact that appears between duplicate times.
        let text = """
주간 스케줄 조회
06/09(화) 15:00 ® 15:00 20:30 ® 20:30 정상
06/10(수) 15:00 ® 15:00 20:30 ® 20:30 정상
06/11(목) 15:00 ® 15:00 20:30 ® 20:30 정상
"""
        guard case .success(let weeks) = await parser.parse(text) else { return XCTFail() }
        let days = weeks[0].days.filter { $0.type == .work }
        XCTAssertFalse(days.isEmpty)
        for day in days {
            XCTAssertEqual(day.startTime, "15:00")
            XCTAssertEqual(day.endTime, "20:30")
            XCTAssertEqual(day.codeLabel, "정상")
        }
    }

    // MARK: - Helpers

    private func sampleWeek() -> String {
        """
근무 스케쥴
06/08(월) 09:00~18:00 정상
06/09(화) 09:00~18:00 정상
06/10(수) 09:00~18:00 정상
06/11(목) 09:00~18:00 정상
06/12(금) 09:00~18:00 정상
06/13(토) 정규휴일
06/14(일) 정규휴일
"""
    }

    private func assertDate(_ date: Date, year: Int, month: Int, day: Int) {
        let comps = Calendar.current.dateComponents([.year, .month, .day], from: date)
        XCTAssertEqual(comps.year, year)
        XCTAssertEqual(comps.month, month)
        XCTAssertEqual(comps.day, day)
    }

    private func loadSample(_ fileName: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let samplesURL = fileURL
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("android/ocr/src/test/resources/ocr_samples")
            .appendingPathComponent(fileName)
        return try String(contentsOf: samplesURL, encoding: .utf8)
    }

    private func loadIOSSample(_ fileName: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let samplesURL = fileURL
            .deletingLastPathComponent()
            .appendingPathComponent("ocr_samples")
            .appendingPathComponent(fileName)
        return try String(contentsOf: samplesURL, encoding: .utf8)
    }

    private static func makeDate(year: Int, month: Int, day: Int) -> Date {
        var comps = DateComponents()
        comps.year = year; comps.month = month; comps.day = day
        return Calendar.current.date(from: comps) ?? Date()
    }
}
// swiftlint:enable file_length
