import Foundation
import WidgetKit

@MainActor
enum WidgetSharedStore {
    // WidgetProvider에서도 동일 값을 선언 — 타깃 분리로 공유 불가, 변경 시 함께 수정 필요
    static let appGroupID = "group.com.timezonealarm.shift"
    static let daysKey = "shift.widget.schedule_days"

    static func write(_ weeks: [ScheduleWeek]) {
        guard let defaults = UserDefaults(suiteName: appGroupID) else {
            print("[Widget] ❌ App Group UserDefaults 초기화 실패 — \(appGroupID)")
            return
        }
        let days = weeks.flatMap(\.days)
        print("[Widget] \(weeks.count)개 주, \(days.count)개 날짜를 App Group에 저장 중")
        if let data = try? JSONEncoder().encode(days) {
            defaults.set(data, forKey: daysKey)
            defaults.synchronize()
            print("[Widget] ✅ 저장 완료 — \(data.count) bytes")
        } else {
            print("[Widget] ❌ JSON 인코딩 실패")
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
