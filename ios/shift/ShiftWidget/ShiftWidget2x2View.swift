import SwiftUI
import WidgetKit

struct ShiftWidget2x2: Widget {
    let kind = "ShiftWidget2x2"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetProvider()) { entry in
            ShiftWidget2x2View(entry: entry)
                .containerBackground(WidgetColors.surface, for: .widget)
                .widgetURL(URL(string: "shift://open?source=widget_2x2"))
        }
        .configurationDisplayName("오늘 근무")
        .description("오늘의 출퇴근 시간을 확인하세요.")
        .supportedFamilies([.systemSmall])
    }
}

struct ShiftWidget2x2View: View {
    let entry: WidgetEntry

    var body: some View {
        let today = Date()
        let dayNumber = Calendar.current.component(.day, from: today)
        let day = entry.today

        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .lastTextBaseline, spacing: 5) {
                Text(today.formatted(.dateTime.weekday(.abbreviated)))
                    .font(.system(size: Layout.dayLabelSize, weight: .medium))
                    .foregroundStyle(WidgetColors.primary.opacity(0.8))
                Text(String(dayNumber))
                    .font(.system(size: Layout.dateLabelSize, weight: .semibold))
                    .foregroundStyle(WidgetColors.primary)
            }
            Rectangle()
                .fill(Color(white: 0.85))
                .frame(height: 1)
                .padding(.vertical, Layout.dividerVPad)
            stateContent(day: day)
            Spacer(minLength: 0)
        }
        .padding(Layout.outerPad)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    @ViewBuilder
    private func stateContent(day: WidgetScheduleDay?) -> some View {
        if let day, day.isWork {
            VStack(alignment: .leading, spacing: 4) {
                if !day.codeLabel.isEmpty {
                    Text(day.codeLabel)
                        .font(.system(size: Layout.codeLabelSize))
                        .foregroundStyle(WidgetColors.onSurfaceVariant)
                }
                Text("\(day.startTime ?? "") - \(day.endTime ?? "")")
                    .font(.system(size: Layout.workTimeFontSize, weight: .semibold))
                    .foregroundStyle(WidgetColors.primary)
                    .lineLimit(2)
            }
        } else {
            Text(day?.displayOffText ?? "스케줄 없음")
                .font(.system(size: Layout.offStateFontSize, weight: .medium))
                .foregroundStyle(WidgetColors.onSurface)
                .lineLimit(2)
        }
    }
}

private enum Layout {
    static let outerPad: CGFloat = 14
    static let dividerVPad: CGFloat = 8
    static let dayLabelSize: CGFloat = 13
    static let dateLabelSize: CGFloat = 28
    static let offStateFontSize: CGFloat = 22
    static let workTimeFontSize: CGFloat = 20
    static let codeLabelSize: CGFloat = 11
}
