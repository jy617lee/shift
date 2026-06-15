import SwiftUI
import WidgetKit

struct ShiftWidget2x1: Widget {
    let kind = "ShiftWidget2x1"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftWidget2x1View(entry: entry)
                .containerBackground(WidgetColors.surface, for: .widget)
                .widgetURL(URL(string: "shift://open?source=widget_2x1"))
        }
        .configurationDisplayName("오늘 근무 (소)")
        .description("오늘의 출퇴근 시간을 확인하세요.")
        .supportedFamilies([.systemSmall])
    }
}

struct ShiftWidget2x1View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        let day = entry.today
        VStack(spacing: 6) {
            Text(today.formatted(.dateTime.month(.defaultDigits).day().weekday(.abbreviated)))
                .font(.system(size: 13))
                .foregroundStyle(WidgetColors.onSurfaceVariant)
            Text(mainText(day))
                .font(.system(size: mainFontSize(day), weight: day?.isWork == true ? .semibold : .medium))
                .foregroundStyle(mainColor(day))
                .lineLimit(2)
                .multilineTextAlignment(.center)
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func mainText(_ day: WidgetScheduleDay?) -> String {
        guard let day else { return "미등록" }
        if day.isWork, let start = day.startTime, let end = day.endTime {
            return "\(start)\n\(end)"
        }
        return day.codeLabel.isEmpty ? "휴무" : day.codeLabel
    }

    private func mainFontSize(_ day: WidgetScheduleDay?) -> CGFloat {
        day?.isWork == true ? 20 : 17
    }

    private func mainColor(_ day: WidgetScheduleDay?) -> Color {
        day?.isWork == true ? WidgetColors.primary : WidgetColors.onSurface
    }
}
