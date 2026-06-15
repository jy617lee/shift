import SwiftUI

struct WeekCardView: View {
    let week: ScheduleWeek
    let today: Date

    private static let weekRangeFmt: DateFormatter = {
        let fmt = DateFormatter()
        fmt.dateFormat = "M월 d일"
        return fmt
    }()

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("내 스케줄")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(ShiftColors.mutedForeground)
                    .kerning(1.5)
                Spacer()
                Text(headerText)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(ShiftColors.mutedForeground)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(ShiftColors.muted.opacity(0.6))

            Divider()

            ForEach(Array(week.days.enumerated()), id: \.offset) { index, day in
                DayRowView(day: day, isToday: Calendar.current.isDate(day.date, inSameDayAs: today))
                if index < week.days.count - 1 {
                    Divider()
                        .padding(.horizontal, 14)
                }
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: Color.black.opacity(0.06), radius: 8, x: 0, y: 2)
    }

    private var headerText: String {
        let start = week.weekStartDate
        guard let end = Calendar.current.date(byAdding: .day, value: 6, to: start) else { return "" }
        let startStr = Self.weekRangeFmt.string(from: start)
        let cal = Calendar.current
        let year = cal.component(.year, from: start)
        if cal.component(.month, from: start) == cal.component(.month, from: end) {
            return "\(year)년 \(startStr)–\(cal.component(.day, from: end))일"
        }
        return "\(year)년 \(startStr) – \(Self.weekRangeFmt.string(from: end))"
    }
}

struct DayRowView: View {
    let day: ScheduleDay
    let isToday: Bool

    private static let dayFmt: DateFormatter = {
        let fmt = DateFormatter()
        fmt.dateFormat = "EE"
        fmt.locale = Locale(identifier: "ko_KR")
        return fmt
    }()
    private static let dateFmt: DateFormatter = {
        let fmt = DateFormatter()
        fmt.dateFormat = "d"
        return fmt
    }()

    var body: some View {
        HStack(spacing: 10) {
            dateColumn
            shiftContent
            Spacer()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(isToday ? Color.accentColor.opacity(0.06) : Color.clear)
        .overlay(alignment: .leading) {
            if isToday { Color.accentColor.frame(width: 3) }
        }
    }

    private var dateColumn: some View {
        VStack(spacing: 2) {
            Text(Self.dayFmt.string(from: day.date))
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(dayColor)
            Text(Self.dateFmt.string(from: day.date))
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(isToday ? Color.accentColor : Color.primary)
        }
        .frame(width: 32, alignment: .center)
    }

    @ViewBuilder
    private var shiftContent: some View {
        switch day.type {
        case .work:
            HStack(spacing: 12) {
                ShiftTypeBadge(label: "근무", type: .work)
                if let start = day.startTime, let end = day.endTime {
                    Text("\(start)–\(end)")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.primary)
                }
            }
        case .off:
            ShiftTypeBadge(label: day.codeLabel.isEmpty ? "휴무" : day.codeLabel, type: .off)
        case .other:
            ShiftTypeBadge(label: day.codeLabel, type: .off)
        case .unregistered:
            Text("—").font(.system(size: 13)).foregroundStyle(.secondary)
        }
    }

    private var dayColor: Color {
        if isToday { return Color.accentColor }
        let weekday = Calendar.current.component(.weekday, from: day.date)
        if weekday == 1 { return Color(.systemRed) }
        if weekday == 7 { return Color(.systemBlue) }
        return ShiftColors.mutedForeground
    }
}
