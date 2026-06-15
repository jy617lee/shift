import SwiftUI
import WidgetKit

struct ShiftWidget4x2: Widget {
    let kind = "ShiftWidget4x2"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftWidget4x2View(entry: entry)
                .containerBackground(WidgetColors
                    .headerBg, for: .widget)
                .widgetURL(URL(string: "shift://open?source=widget_4x2"))
        }
        .configurationDisplayName("주간 스케줄")
        .description("이번 주 근무 일정을 한눈에 확인하세요.")
        .supportedFamilies([.systemMedium])
    }
}

struct ShiftWidget4x2View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        VStack(spacing: 0) {
            headerContent(today: today)
                .padding(.horizontal, Layout.headerHPad)
                .frame(height: Layout.headerHeight)
            WeekGrid(days: entry.weekDays(around: today), today: today)
                .padding(.horizontal, Layout.gridHPad)
                .padding(.bottom, Layout.gridBPad)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(WidgetColors.surface)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func headerContent(today: Date) -> some View {
        let day = entry.today
        let dayNumber = Calendar.current.component(.day, from: today)
        HStack(spacing: 0) {
            VStack(spacing: Layout.headerInnerSpacing) {
                Text(today.formatted(.dateTime.weekday(.abbreviated)))
                    .font(.system(size: Layout.headerDayLabelSize, weight: .medium))
                    .foregroundStyle(.white.opacity(0.8))
                Text(String(dayNumber))
                    .font(.system(size: Layout.dateLabelSize, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .frame(width: Layout.headerLeftWidth)
            Rectangle()
                .fill(Color.white.opacity(0.35))
                .frame(width: 1, height: Layout.headerDividerHeight)
                .padding(.horizontal, Layout.headerDividerPadding)
            Text(headerStateText(day))
                .font(.system(size: day?.isWork == true ? Layout.stateFontSizeWork : Layout.stateFontSizeOff, weight: .regular))
                .foregroundStyle(.white.opacity(day?.isWork == true ? 1 : 0.8))
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }

    private func headerStateText(_ day: WidgetScheduleDay?) -> String {
        guard let day else { return "-" }
        if day.isWork, let start = day.startTime, let end = day.endTime {
            return "\(start) - \(end)"
        }
        return day.codeLabel.isEmpty ? "휴무" : day.codeLabel
    }
}

private struct WeekGrid: View {
    let days: [WidgetScheduleDay?]
    let today: Date

    var body: some View {
        let cal = Calendar.current
        let todayStart = cal.startOfDay(for: today)
        let dates: [Date] = (0..<5).map { idx in
            cal.date(byAdding: .day, value: idx - 2, to: todayStart) ?? todayStart
        }

        VStack(spacing: Layout.cellSpacing) {
            HStack(spacing: 0) {
                ForEach(0..<5, id: \.self) { idx in
                    dayBadge(date: dates[idx], isToday: idx == 2)
                        .frame(maxWidth: .infinity)
                }
            }
            HStack(alignment: .top, spacing: 0) {
                ForEach(0..<5, id: \.self) { idx in
                    dayValue(day: days[idx], isToday: idx == 2)
                        .frame(maxWidth: .infinity, minHeight: Layout.cellValueMinHeight, alignment: .top)
                }
            }
        }
        .padding(.top, Layout.gridTopPad)
    }

    @ViewBuilder
    private func dayBadge(date: Date, isToday: Bool) -> some View {
        let label = date.formatted(.dateTime.weekday(.narrow))
        if isToday {
            Text(label)
                .font(.system(size: Layout.cellDayLabelSize, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: Layout.badgeSize, height: Layout.badgeSize)
                .background(WidgetColors.primary)
                .clipShape(Circle())
        } else {
            Text(label)
                .font(.system(size: Layout.cellDayLabelSize, weight: .medium))
                .foregroundStyle(WidgetColors.gridDate)
                .frame(width: Layout.badgeSize, height: Layout.badgeSize)
        }
    }

    @ViewBuilder
    private func dayValue(day: WidgetScheduleDay?, isToday: Bool) -> some View {
        let color: Color = isToday ? WidgetColors.primary : WidgetColors.gridMuted
        let style = Font.system(size: Layout.cellValueSize)
        if let day, day.isWork, let start = day.startTime, let end = day.endTime {
            VStack(spacing: 1) {
                Text(start).font(style).foregroundStyle(color)
                Text(end).font(style).foregroundStyle(color)
            }
        } else {
            Text(offLabel(day))
                .font(style)
                .foregroundStyle(color)
                .multilineTextAlignment(.center)
        }
    }

    private func offLabel(_ day: WidgetScheduleDay?) -> String {
        guard let day else { return "-" }
        return day.codeLabel.isEmpty ? "휴무" : String(day.codeLabel.prefix(4))
    }
}

private enum Layout {
    static let headerHeight: CGFloat = 74
    static let headerHPad: CGFloat = 14
    static let headerLeftWidth: CGFloat = 72
    static let headerInnerSpacing: CGFloat = 1
    static let headerDividerHeight: CGFloat = 32
    static let headerDividerPadding: CGFloat = 10
    static let headerDayLabelSize: CGFloat = 13
    static let cellDayLabelSize: CGFloat = 12
    static let dateLabelSize: CGFloat = 28
    static let stateFontSizeWork: CGFloat = 26
    static let stateFontSizeOff: CGFloat = 20
    static let gridHPad: CGFloat = 8
    static let gridBPad: CGFloat = 16
    static let gridTopPad: CGFloat = 8
    static let badgeSize: CGFloat = 26
    static let cellSpacing: CGFloat = 3
    static let cellValueSize: CGFloat = 12
    static let cellValueMinHeight: CGFloat = 30
}

#Preview(as: .systemMedium) {
    ShiftWidget4x2()
} timeline: {
    WidgetEntry.placeholder
}
