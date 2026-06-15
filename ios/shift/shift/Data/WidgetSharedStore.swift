import Foundation
import WidgetKit

@MainActor
enum WidgetSharedStore {
    static let appGroupID = "group.com.timezonealarm.shift"
    static let daysKey = "shift.widget.schedule_days"

    static func write(_ weeks: [ScheduleWeek]) {
        guard let ud = UserDefaults(suiteName: appGroupID) else { return }
        let days = weeks.flatMap(\.days)
        if let data = try? JSONEncoder().encode(days) {
            ud.set(data, forKey: daysKey)
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
