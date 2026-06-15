import SwiftUI
import WidgetKit

// MARK: - 잠금화면 오늘 근무 (4×1 요소)

struct ShiftLockWidget4x1: Widget {
    let kind = "ShiftLockWidget4x1"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftLockWidget4x1View(entry: entry)
                .containerBackground(.clear, for: .widget)
                .widgetURL(URL(string: "shift://open?source=lock_widget_4x1"))
        }
        .configurationDisplayName("오늘 근무 (잠금화면)")
        .description("잠금화면에서 오늘의 출퇴근 시간을 확인하세요.")
        .supportedFamilies([.accessoryRectangular])
    }
}

struct ShiftLockWidget4x1View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        let dayNumber = Calendar.current.component(.day, from: today)
        let day = entry.today

        HStack(spacing: 0) {
            VStack(spacing: 0) {
                Text(today.formatted(.dateTime.weekday(.abbreviated)))
                    .font(.system(size: LockLayout.dayLabelSize, weight: .medium))
                    .foregroundStyle(.secondary)
                Text(String(dayNumber))
                    .font(.system(size: LockLayout.dateLabelSize, weight: .semibold))
            }
            .frame(width: LockLayout.dateColumnWidth)

            Rectangle()
                .frame(width: 1, height: LockLayout.dividerHeight)
                .padding(.horizontal, LockLayout.dividerHPad)
                .foregroundStyle(.secondary)

            stateContent(day: day)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func stateContent(day: WidgetScheduleDay?) -> some View {
        if let day, day.isWork {
            VStack(alignment: .leading, spacing: 1) {
                if !day.codeLabel.isEmpty {
                    Text(day.codeLabel)
                        .font(.system(size: LockLayout.codeLabelSize))
                        .foregroundStyle(.secondary)
                }
                Text("\(day.startTime ?? "") - \(day.endTime ?? "")")
                    .font(.system(size: LockLayout.workTimeFontSize, weight: .semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
        } else {
            Text(offText(day))
                .font(.system(size: LockLayout.offStateFontSize, weight: .medium))
                .lineLimit(2)
        }
    }

    private func offText(_ day: WidgetScheduleDay?) -> String {
        guard let day else { return "스케줄 없음" }
        return day.codeLabel.isEmpty ? "휴무" : day.codeLabel
    }
}

// MARK: - 잠금화면 주간 미리보기 (4×2 요소)

struct ShiftLockWidget4x2: Widget {
    let kind = "ShiftLockWidget4x2"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftLockWidget4x2View(entry: entry)
                .containerBackground(.clear, for: .widget)
                .widgetURL(URL(string: "shift://open?source=lock_widget_4x2"))
        }
        .configurationDisplayName("주간 스케줄 (잠금화면)")
        .description("잠금화면에서 이번 주 근무 일정을 확인하세요.")
        .supportedFamilies([.accessoryRectangular])
    }
}

struct ShiftLockWidget4x2View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        let days = entry.weekDays(around: today)

        HStack(alignment: .top, spacing: 0) {
            ForEach(0..<5, id: \.self) { idx in
                let offset = idx - 2
                let date = Calendar.current.date(
                    byAdding: .day,
                    value: offset,
                    to: Calendar.current.startOfDay(for: today)
                ) ?? today
                LockDayCell(date: date, day: days[idx], isToday: offset == 0)
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct LockDayCell: View {
    let date: Date
    let day: WidgetScheduleDay?
    let isToday: Bool

    var body: some View {
        VStack(spacing: 2) {
            Text(date.formatted(.dateTime.weekday(.narrow)))
                .font(.system(size: 9, weight: isToday ? .semibold : .regular))
                .foregroundStyle(isToday ? AnyShapeStyle(.primary) : AnyShapeStyle(.secondary))
            Text(cellText)
                .font(.system(size: 8))
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .foregroundStyle(isToday ? AnyShapeStyle(.primary) : AnyShapeStyle(.secondary))
        }
        .frame(maxWidth: .infinity, alignment: .top)
    }

    private var cellText: String {
        guard let day else { return "-" }
        if day.isWork, let start = day.startTime, let end = day.endTime {
            return "\(start)\n\(end)"
        }
        let label = day.codeLabel
        return label.isEmpty ? "휴" : String(label.prefix(3))
    }
}

// MARK: - 잠금화면 카운트다운 (오늘→내일 자동 전환)

struct ShiftLockWidgetCountdown: Widget {
    let kind = "ShiftLockWidgetCountdown"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftLockWidgetCountdownView(entry: entry)
                .containerBackground(.clear, for: .widget)
                .widgetURL(URL(string: "shift://open?source=lock_widget_countdown"))
        }
        .configurationDisplayName("근무 카운트다운 (잠금화면)")
        .description("출근·퇴근까지 남은 시간을 잠금화면에서 확인하세요.")
        .supportedFamilies([.accessoryRectangular])
    }
}

struct ShiftLockWidgetCountdownView: View {
    let entry: WidgetEntry

    var body: some View {
        let targetDate = entry.targetDate
        let day = entry.today

        VStack(alignment: .leading, spacing: 2) {
            Text(dateLabel(targetDate, isTomorrow: entry.isShowingTomorrow))
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(.secondary)

            if let day {
                if day.isWork, let start = day.startTime, let end = day.endTime {
                    Text("\(start) - \(end)")
                        .font(.system(size: 14, weight: .semibold))
                        .lineLimit(1)
                    countdownRow(day: day, targetDate: targetDate)
                } else {
                    let label = day.codeLabel.isEmpty ? "휴무" : day.codeLabel
                    Text(label)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.secondary)
                }
            } else {
                Text("스케줄 없음")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func countdownRow(day: WidgetScheduleDay, targetDate: Date) -> some View {
        let now = Date()
        if let startDate = day.shiftStartDate(on: targetDate), now < startDate {
            HStack(spacing: 3) {
                Text("출근까지")
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
                Text(timerInterval: now...startDate, countsDown: true)
                    .font(.system(size: 10, weight: .semibold))
                    .monospacedDigit()
            }
        } else if let endDate = day.shiftEndDate(on: targetDate), now < endDate {
            HStack(spacing: 3) {
                Text("퇴근까지")
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
                Text(timerInterval: now...endDate, countsDown: true)
                    .font(.system(size: 10, weight: .semibold))
                    .monospacedDigit()
            }
        } else {
            Text("근무 종료")
                .font(.system(size: 10))
                .foregroundStyle(.secondary)
        }
    }

    private func dateLabel(_ date: Date, isTomorrow: Bool) -> String {
        let cal = Calendar.current
        let month = cal.component(.month, from: date)
        let day = cal.component(.day, from: date)
        let weekday = date.formatted(.dateTime.weekday(.abbreviated))
        let prefix = isTomorrow ? "내일 · " : ""
        return "\(prefix)\(month)/\(day)(\(weekday))"
    }
}

// MARK: - 공통 레이아웃 상수

private enum LockLayout {
    static let dateColumnWidth: CGFloat = 32
    static let dividerHeight: CGFloat = 28
    static let dividerHPad: CGFloat = 8
    static let dayLabelSize: CGFloat = 9
    static let dateLabelSize: CGFloat = 20
    static let codeLabelSize: CGFloat = 9
    static let workTimeFontSize: CGFloat = 13
    static let offStateFontSize: CGFloat = 12
}
