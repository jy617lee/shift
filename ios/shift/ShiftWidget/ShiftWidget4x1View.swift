import SwiftUI
import WidgetKit

struct ShiftWidget4x1: Widget {
    let kind = "ShiftWidget4x1"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftWidget4x1View(entry: entry)
                .containerBackground(WidgetColors.surface, for: .widget)
                .widgetURL(URL(string: "shift://open?source=widget_4x1"))
        }
        .configurationDisplayName("오늘 근무")
        .description("오늘의 출퇴근 시간과 근태 코드를 확인하세요.")
        .supportedFamilies([.systemMedium])
    }
}

struct ShiftWidget4x1View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        HStack(spacing: 0) {
            todayDateColumn(today: today)
            Rectangle()
                .fill(Color(white: 0.8))
                .frame(width: 1, height: 44)
                .padding(.horizontal, 16)
            stateColumn(day: entry.today)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func todayDateColumn(today: Date) -> some View {
        VStack(spacing: 2) {
            Text(today.formatted(.dateTime.weekday(.abbreviated)))
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(WidgetColors.primary.opacity(0.8))
            Text(today.formatted(.dateTime.day()))
                .font(.system(size: 40, weight: .semibold))
                .foregroundStyle(WidgetColors.primary)
        }
        .frame(width: 60)
    }

    @ViewBuilder
    private func stateColumn(day: WidgetScheduleDay?) -> some View {
        if let day, day.isWork {
            workStateView(day: day)
        } else {
            Text(offStateText(day))
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(WidgetColors.onSurface)
        }
    }

    private func workStateView(day: WidgetScheduleDay) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            if !day.codeLabel.isEmpty {
                Text(day.codeLabel)
                    .font(.system(size: 11))
                    .foregroundStyle(WidgetColors.onSurfaceVariant)
            }
            Text("\(day.startTime ?? "") - \(day.endTime ?? "")")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(WidgetColors.primary)
        }
    }

    private func offStateText(_ day: WidgetScheduleDay?) -> String {
        guard let day else { return "스케줄 없음" }
        return day.codeLabel.isEmpty ? "휴무" : day.codeLabel
    }
}
