import Foundation
import WidgetKit

@MainActor
enum WidgetSharedStore {
    // WidgetProvider에서도 동일 값을 선언 — 타깃 분리로 공유 불가, 변경 시 함께 수정 필요
    static let appGroupID = "group.com.timezonealarm.shift"
    static let daysKey = "shift.widget.schedule_days"

    static func write(_ weeks: [ScheduleWeek]) {
        guard let defaults = UserDefaults(suiteName: appGroupID) else { return }
        let days = weeks.flatMap(\.days)
        if let data = try? JSONEncoder().encode(days) {
            defaults.set(data, forKey: daysKey)
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
