import Foundation
import SwiftData

@MainActor
final class SwiftDataScheduleRepository: ScheduleRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    func getAllWeeks() async throws -> [ScheduleWeek] {
        let descriptor = FetchDescriptor<ScheduleWeekEntry>(
            sortBy: [SortDescriptor(\ScheduleWeekEntry.weekStartDate)]
        )
        return try context.fetch(descriptor).map { try $0.toScheduleWeek() }
    }

    func getWeekByDate(_ date: Date) async throws -> ScheduleWeek? {
        let monday = mondayOfWeek(for: date)
        let descriptor = FetchDescriptor<ScheduleWeekEntry>(
            predicate: #Predicate { $0.weekStartDate == monday }
        )
        return try context.fetch(descriptor).first.map { try $0.toScheduleWeek() }
    }

    func getWeeksInRange(from: Date, to: Date) async throws -> [ScheduleWeek] {
        let start = mondayOfWeek(for: from)
        let descriptor = FetchDescriptor<ScheduleWeekEntry>(
            predicate: #Predicate { $0.weekStartDate >= start && $0.weekStartDate <= to },
            sortBy: [SortDescriptor(\ScheduleWeekEntry.weekStartDate)]
        )
        return try context.fetch(descriptor).map { try $0.toScheduleWeek() }
    }

    func getNextWeekFrom(_ date: Date) async throws -> ScheduleWeek? {
        let monday = mondayOfWeek(for: date)
        var descriptor = FetchDescriptor<ScheduleWeekEntry>(
            predicate: #Predicate { $0.weekStartDate >= monday },
            sortBy: [SortDescriptor(\ScheduleWeekEntry.weekStartDate)]
        )
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first.map { try $0.toScheduleWeek() }
    }

    func saveWeek(_ week: ScheduleWeek) async throws {
        let entry = try ScheduleWeekEntry.from(week)
        context.insert(entry)
        try context.save()
        await refreshWidget()
    }

    func replaceWeek(_ week: ScheduleWeek) async throws {
        let monday = week.weekStartDate
        let descriptor = FetchDescriptor<ScheduleWeekEntry>(
            predicate: #Predicate { $0.weekStartDate == monday }
        )
        if let existing = try context.fetch(descriptor).first {
            context.delete(existing)
        }
        let entry = try ScheduleWeekEntry.from(week)
        context.insert(entry)
        try context.save()
        await refreshWidget()
    }

    func deleteWeek(weekStartDate: Date) async throws {
        let descriptor = FetchDescriptor<ScheduleWeekEntry>(
            predicate: #Predicate { $0.weekStartDate == weekStartDate }
        )
        if let entry = try context.fetch(descriptor).first {
            context.delete(entry)
            try context.save()
        }
        await refreshWidget()
    }

    private func refreshWidget() async {
        guard let weeks = try? await getAllWeeks() else { return }
        WidgetSharedStore.write(weeks)
    }

    private func mondayOfWeek(for date: Date) -> Date {
        let cal = Calendar.current
        let weekday = cal.component(.weekday, from: date)
        let offset = weekday == 1 ? -6 : 2 - weekday
        return cal.startOfDay(for: cal.date(byAdding: .day, value: offset, to: date) ?? date)
    }
}
