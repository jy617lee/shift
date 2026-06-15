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
        let dayNumber = Calendar.current.component(.day, from: today)
        let monthNumber = Calendar.current.component(.month, from: today)
        VStack(spacing: Layout.spacing) {
            Text("\(monthNumber)/\(dayNumber) " + today.formatted(.dateTime.weekday(.abbreviated)))
                .font(.system(size: Layout.dateLabelSize))
                .foregroundStyle(WidgetColors.onSurfaceVariant)
            Text(mainText(day))
                .font(.system(size: mainFontSize(day), weight: day?.isWork == true ? .semibold : .medium))
                .foregroundStyle(mainColor(day))
                .lineLimit(2)
                .multilineTextAlignment(.center)
        }
        .padding(Layout.outerPad)
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
        day?.isWork == true ? Layout.workFontSize : Layout.offFontSize
    }

    private func mainColor(_ day: WidgetScheduleDay?) -> Color {
        day?.isWork == true ? WidgetColors.primary : WidgetColors.onSurface
    }
}

private enum Layout {
    static let spacing: CGFloat = 6
    static let dateLabelSize: CGFloat = 13
    static let workFontSize: CGFloat = 20
    static let offFontSize: CGFloat = 17
    static let outerPad: CGFloat = 16
}
