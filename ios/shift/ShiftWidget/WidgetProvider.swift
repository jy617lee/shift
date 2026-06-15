import Foundation
import WidgetKit

struct WidgetProvider: TimelineProvider {
    // WidgetSharedStore에서도 동일 값을 선언 — 타깃 분리로 공유 불가, 변경 시 함께 수정 필요
    private let appGroupID = "group.com.timezonealarm.shift"
    private let daysKey = "shift.widget.schedule_days"

    func placeholder(in context: Context) -> WidgetEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (WidgetEntry) -> Void) {
        completion(makeEntry(at: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WidgetEntry>) -> Void) {
        let now = Date()
        let cal = Calendar.current
        var entries: [WidgetEntry] = []

        let current = makeEntry(at: now)
        entries.append(current)

        let today = cal.startOfDay(for: now)
        let tomorrow = cal.date(byAdding: .day, value: 1, to: today) ?? today
        let days = current.days
        let todayDay = days.first(where: { cal.isDate($0.date, inSameDayAs: today) })

        if let todayDay, todayDay.isWork,
           let endDate = todayDay.shiftEndDate(on: today), now < endDate {
            // 오늘 근무 종료 시점에 '내일' 엔트리 추가
            entries.append(WidgetEntry(date: endDate, days: days, targetDate: tomorrow))
        } else if todayDay.map({ !$0.isWork }) ?? true,
                  let nextWork = current.nextWorkDay(after: now),
                  let nextStart = nextWork.shiftStartDate(on: nextWork.date),
                  now < nextStart {
            // 오프데이: 다음 근무 시작 시점에 갱신
            entries.append(WidgetEntry(date: nextStart, days: days, targetDate: today))
        }

        completion(Timeline(entries: entries, policy: .after(nextMidnight())))
    }

    // MARK: - Helpers

    private func makeEntry(at now: Date) -> WidgetEntry {
        let cal = Calendar.current
        let today = cal.startOfDay(for: now)
        let tomorrow = cal.date(byAdding: .day, value: 1, to: today) ?? today

        let days = loadDays()

        // 오늘 근무가 이미 끝났으면 targetDate = 내일
        let todayDay = days.first { cal.isDate($0.date, inSameDayAs: today) }
        let targetDate: Date
        if let todayDay, todayDay.isWork,
           let endDate = todayDay.shiftEndDate(on: today),
           now >= endDate {
            targetDate = tomorrow
        } else {
            targetDate = today
        }

        return WidgetEntry(date: now, days: days, targetDate: targetDate)
    }

    private func loadDays() -> [WidgetScheduleDay] {
        guard let ud = UserDefaults(suiteName: appGroupID),
              let data = ud.data(forKey: daysKey),
              let days = try? JSONDecoder().decode([WidgetScheduleDay].self, from: data) else {
            return []
        }
        return days
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
