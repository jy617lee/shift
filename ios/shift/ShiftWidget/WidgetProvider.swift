import Foundation
import WidgetKit

struct WidgetProvider: TimelineProvider {
    private let appGroupID = "group.com.timezonealarm.shift"
    private let daysKey = "shift.widget.schedule_days"

    func placeholder(in context: Context) -> WidgetEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (WidgetEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WidgetEntry>) -> Void) {
        let entry = loadEntry()
        let nextUpdate = nextMidnight()
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }

    private func loadEntry() -> WidgetEntry {
        guard let ud = UserDefaults(suiteName: appGroupID),
              let data = ud.data(forKey: daysKey),
              let days = try? JSONDecoder().decode([WidgetScheduleDay].self, from: data) else {
            return .placeholder
        }
        return WidgetEntry(date: Date(), days: days)
    }

    private func nextMidnight() -> Date {
        let cal = Calendar.current
        return cal.nextDate(
            after: Date(),
            matching: DateComponents(hour: 0, minute: 0),
            matchingPolicy: .nextTime
        ) ?? Date().addingTimeInterval(86400)
    }
}
