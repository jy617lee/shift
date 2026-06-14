import Foundation

struct ScheduleParserImpl: ScheduleParser {
    private let validator: Stage1Validator
    private let today: Date

    // swiftlint:disable:next line_length
    private static let linePattern = makePattern(
        #"(\d{1,2})/(\d{1,2})\([^)]*\)\s*(?:(?:[P@O0R]\s*)*(\d{2}:\d{2})[~\s]*(?:[P@O0R]\s*)*(\d{2}:\d{2})\s*)?(?:(?:[P@O0R]\s*)*(.*))?"#
    )
    private static let dateOnlyPattern = makePattern(
        #"^(\d{1,2})/(\d{1,2})\([^)]*\)\s*$"#
    )
    private static let orphanTimePattern = makePattern(
        #"(?:[P@O0R]\s*)*(\d{2}:\d{2})[~\s]*(?:[P@O0R]\s*)*(\d{2}:\d{2})\s*(?:(?:[P@O0R]\s*)*(.*))?"#
    )

    private static let pastDaysThreshold = -90
    private static let futureDaysThreshold = 56

    init(validator: Stage1Validator = Stage1Validator(), today: Date = Date()) {
        self.validator = validator
        self.today = today
    }

    func parse(_ text: String) async -> ParseResult {
        guard validator.isScheduleText(text) else { return .failure(.notASchedule) }
        let days = extractDays(text)
        guard !days.isEmpty else { return .failure(.parseError) }
        return .success(groupIntoWeeks(days))
    }

    private func extractDays(_ text: String) -> [ScheduleDay] {
        var dayMap: [Date: ScheduleDay] = [:]
        var lastDate: Date?
        for rawLine in text.components(separatedBy: "\n") {
            let line = rawLine.trimmingCharacters(in: .whitespaces)
            processLine(line, lastDate: &lastDate, dayMap: &dayMap)
        }
        return dayMap.values.sorted { $0.date < $1.date }
    }

    private func processLine(_ line: String, lastDate: inout Date?, dayMap: inout [Date: ScheduleDay]) {
        if let full = parseLine(line) {
            if dayMap[full.date] == nil { dayMap[full.date] = full }
            lastDate = full.date
        } else if let date = parseDateOnly(line) {
            lastDate = date
        } else if let ld = lastDate, dayMap[ld] == nil,
                  let orphan = parseOrphanedTimeLine(line, date: ld) {
            dayMap[ld] = orphan
        }
    }

    private func parseLine(_ line: String) -> ScheduleDay? {
        let range = NSRange(line.startIndex..., in: line)
        guard let match = Self.linePattern.firstMatch(in: line, range: range) else { return nil }
        let monthStr = capture(match, at: 1, in: line)
        let dayStr = capture(match, at: 2, in: line)
        guard let month = Int(monthStr), let day = Int(dayStr) else { return nil }
        let startTime = capture(match, at: 3, in: line).nilIfEmpty
        let endTime = capture(match, at: 4, in: line).nilIfEmpty
        let code = capture(match, at: 5, in: line)
        guard startTime != nil || !code.isEmpty else { return nil }
        return ScheduleDay(
            date: assignYear(month: month, day: day),
            type: startTime != nil ? .work : .other,
            startTime: startTime,
            endTime: endTime,
            codeLabel: code,
            source: .parsed
        )
    }

    private func parseDateOnly(_ line: String) -> Date? {
        let range = NSRange(line.startIndex..., in: line)
        guard let match = Self.dateOnlyPattern.firstMatch(in: line, range: range) else { return nil }
        let month = Int(capture(match, at: 1, in: line)) ?? 0
        let day = Int(capture(match, at: 2, in: line)) ?? 0
        guard month > 0, day > 0 else { return nil }
        return assignYear(month: month, day: day)
    }

    private func parseOrphanedTimeLine(_ line: String, date: Date) -> ScheduleDay? {
        let range = NSRange(line.startIndex..., in: line)
        guard let match = Self.orphanTimePattern.firstMatch(in: line, range: range) else { return nil }
        let startTime = capture(match, at: 1, in: line).nilIfEmpty
        let endTime = capture(match, at: 2, in: line).nilIfEmpty
        guard startTime != nil, endTime != nil else { return nil }
        return ScheduleDay(
            date: date, type: .work,
            startTime: startTime, endTime: endTime,
            codeLabel: capture(match, at: 3, in: line), source: .parsed
        )
    }

    private func assignYear(month: Int, day: Int) -> Date {
        let cal = Calendar.current
        let year = cal.component(.year, from: today)
        var comps = DateComponents()
        comps.year = year; comps.month = month; comps.day = day
        comps.hour = 0; comps.minute = 0; comps.second = 0
        guard let candidate = cal.date(from: comps) else { return cal.startOfDay(for: today) }
        let past = cal.date(byAdding: .day, value: Self.pastDaysThreshold, to: today) ?? today
        let future = cal.date(byAdding: .day, value: Self.futureDaysThreshold, to: today) ?? today
        if candidate < past { return cal.date(byAdding: .year, value: 1, to: candidate) ?? candidate }
        if candidate > future { return cal.date(byAdding: .year, value: -1, to: candidate) ?? candidate }
        return candidate
    }

    private func groupIntoWeeks(_ days: [ScheduleDay]) -> [ScheduleWeek] {
        guard let first = days.first, let last = days.last else { return [] }
        let cal = Calendar.current
        let weekStart = mondayOfWeek(for: first.date)
        let weekEnd = mondayOfWeek(for: last.date)
        let dayMap = Dictionary(days.map { ($0.date, $0) }, uniquingKeysWith: { existing, _ in existing })
        var weeks: [ScheduleWeek] = []
        var current = weekStart
        while current <= weekEnd {
            weeks.append(makeWeek(startingAt: current, dayMap: dayMap, calendar: cal))
            current = cal.date(byAdding: .weekOfYear, value: 1, to: current) ?? current
        }
        return weeks
    }

    private func makeWeek(startingAt monday: Date, dayMap: [Date: ScheduleDay], calendar: Calendar) -> ScheduleWeek {
        let weekDays = (0..<7).map { offset -> ScheduleDay in
            let date = calendar.date(byAdding: .day, value: offset, to: monday) ?? monday
            return dayMap[date] ?? ScheduleDay(
                date: date, type: .unregistered,
                startTime: nil, endTime: nil, codeLabel: "", source: .parsed
            )
        }
        return ScheduleWeek(weekStartDate: monday, days: weekDays)
    }

    private func mondayOfWeek(for date: Date) -> Date {
        let cal = Calendar.current
        let weekday = cal.component(.weekday, from: date)
        let offset = weekday == 1 ? -6 : 2 - weekday
        return cal.startOfDay(for: cal.date(byAdding: .day, value: offset, to: date) ?? date)
    }

    private func capture(_ match: NSTextCheckingResult, at index: Int, in text: String) -> String {
        let range = match.range(at: index)
        guard range.location != NSNotFound,
              let swiftRange = Range(range, in: text) else { return "" }
        return String(text[swiftRange]).trimmingCharacters(in: .whitespaces)
    }

    private static func makePattern(_ pattern: String) -> NSRegularExpression {
        do {
            return try NSRegularExpression(pattern: pattern)
        } catch {
            fatalError("ScheduleParserImpl: invalid regex — \(error)")
        }
    }
}

extension String {
    fileprivate var nilIfEmpty: String? { isEmpty ? nil : self }
}
