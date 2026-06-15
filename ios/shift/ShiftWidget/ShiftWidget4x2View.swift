import SwiftUI
import WidgetKit

struct ShiftWidget4x2: Widget {
    let kind = "ShiftWidget4x2"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftWidget4x2View(entry: entry)
                .containerBackground(WidgetColors.surface, for: .widget)
                .widgetURL(URL(string: "shift://open?source=widget_4x2"))
        }
        .configurationDisplayName("주간 스케줄")
        .description("이번 주 근무 일정을 한눈에 확인하세요.")
        .supportedFamilies([.systemMedium])
        .contentMarginsDisabled()
    }
}

struct ShiftWidget4x2View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        VStack(spacing: 0) {
            ZStack(alignment: .bottom) {
                WidgetColors.headerBg
                HeaderWave()
                    .fill(WidgetColors.surface)
                    .frame(height: Layout.waveHeight)
                headerContent(today: today)
                    .padding(.horizontal, Layout.headerHPad)
                    .padding(.bottom, Layout.headerBPad)
            }
            .frame(height: Layout.headerHeight)
            WeekGrid(days: entry.weekDays(around: today), today: today)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.horizontal, Layout.gridHPad)
                .padding(.bottom, Layout.gridBPad)
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

private struct HeaderWave: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: 0, y: 0))
        path.addLine(to: CGPoint(x: rect.maxX, y: 0))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addQuadCurve(
            to: CGPoint(x: 0, y: rect.maxY),
            control: CGPoint(x: rect.midX, y: rect.minY - 4)
        )
        path.closeSubpath()
        return path
    }
}

private struct WeekGrid: View {
    let days: [WidgetScheduleDay?]
    let today: Date

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(0..<5, id: \.self) { idx in
                let offset = idx - 2
                let date = Calendar.current.date(
                    byAdding: .day,
                    value: offset,
                    to: Calendar.current.startOfDay(for: today)
                ) ?? today
                DayCell(date: date, day: days[idx], isToday: offset == 0)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.top, Layout.gridTopPad)
    }
}

private struct DayCell: View {
    let date: Date
    let day: WidgetScheduleDay?
    let isToday: Bool

    var body: some View {
        VStack(spacing: Layout.cellSpacing) {
            dayBadge
            dayValue
        }
    }

    private var dayBadge: some View {
        let label = date.formatted(.dateTime.weekday(.narrow))
        return Group {
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
    }

    @ViewBuilder
    private var dayValue: some View {
        let color: Color = isToday ? WidgetColors.primary : WidgetColors.gridMuted
        let style = Font.system(size: Layout.cellValueSize)
        VStack(spacing: 1) {
            if let day, day.isWork, let start = day.startTime, let end = day.endTime {
                Text(start).font(style).foregroundStyle(color)
                Text(end).font(style).foregroundStyle(color)
            } else {
                Text(offText).font(style).foregroundStyle(color)
                    .lineLimit(2).multilineTextAlignment(.center)
                Text("").font(style)  // 빈 줄로 work day와 높이 동일하게 유지
            }
        }
        .frame(minHeight: Layout.cellValueMinHeight, alignment: .top)
    }

    private var offText: String {
        guard let day else { return "-" }
        return day.codeLabel.isEmpty ? "휴무" : String(day.codeLabel.prefix(4))
    }
}

private enum Layout {
    static let headerHeight: CGFloat = 74
    static let waveHeight: CGFloat = 18
    static let headerHPad: CGFloat = 14
    static let headerBPad: CGFloat = 22
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
