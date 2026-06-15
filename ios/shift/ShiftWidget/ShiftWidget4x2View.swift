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
                    .frame(height: 12)
                headerContent(today: today)
                    .padding(.horizontal, 14)
                    .padding(.bottom, 14)
            }
            .frame(height: 66)
            WeekGrid(days: entry.weekDays(around: today), today: today)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.horizontal, 8)
                .padding(.bottom, 10)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func headerContent(today: Date) -> some View {
        HStack(spacing: 0) {
            VStack(spacing: 1) {
                Text(today.formatted(.dateTime.weekday(.abbreviated)))
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(.white.opacity(0.8))
                Text(today.formatted(.dateTime.day()))
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .frame(width: 52)
            Rectangle()
                .fill(Color.white.opacity(0.35))
                .frame(width: 1, height: 32)
                .padding(.horizontal, 12)
            Text(headerStateText(entry.today))
                .font(.system(size: entry.today?.isWork == true ? 18 : 14, weight: .regular))
                .foregroundStyle(.white.opacity(entry.today?.isWork == true ? 1 : 0.8))
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
        HStack(spacing: 0) {
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
        .padding(.top, 8)
    }
}

private struct DayCell: View {
    let date: Date
    let day: WidgetScheduleDay?
    let isToday: Bool

    var body: some View {
        VStack(spacing: 2) {
            dayBadge
            dayValue
        }
    }

    private var dayBadge: some View {
        let label = date.formatted(.dateTime.weekday(.narrow))
        return Group {
            if isToday {
                Text(label)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 22, height: 22)
                    .background(WidgetColors.primary)
                    .clipShape(Circle())
            } else {
                Text(label)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(WidgetColors.gridDate)
                    .frame(width: 22, height: 22)
            }
        }
    }

    @ViewBuilder
    private var dayValue: some View {
        let color: Color = isToday ? WidgetColors.primary : WidgetColors.gridMuted
        let style = Font.system(size: 9)
        if let day, day.isWork, let start = day.startTime, let end = day.endTime {
            Text(start).font(style).foregroundStyle(color)
            Text(end).font(style).foregroundStyle(color)
        } else {
            Text(offText).font(style).foregroundStyle(color)
                .lineLimit(2).multilineTextAlignment(.center)
        }
    }

    private var offText: String {
        guard let day else { return "-" }
        return day.codeLabel.isEmpty ? "휴무" : String(day.codeLabel.prefix(4))
    }
}
