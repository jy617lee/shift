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
            weekHeader
            Divider()
            ForEach(Array(week.days.enumerated()), id: \.offset) { index, day in
                DayRowView(day: day, isToday: Calendar.current.isDate(day.date, inSameDayAs: today))
                if index < week.days.count - 1 { Divider().padding(.leading, 52) }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var weekHeader: some View {
        HStack {
            Text("내 스케쥴")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(.secondary)
                .kerning(1.5)
            Spacer()
            Text(headerText)
                .font(.system(size: 10))
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(Color(.tertiarySystemBackground))
    }

    private var headerText: String {
        let start = week.weekStartDate
        guard let end = Calendar.current.date(byAdding: .day, value: 6, to: start) else { return "" }
        let startStr = Self.weekRangeFmt.string(from: start)
        if Calendar.current.component(.month, from: start) == Calendar.current.component(.month, from: end) {
            return "\(Calendar.current.component(.year, from: start))년 \(startStr)–\(Calendar.current.component(.day, from: end))일"
        }
        return "\(Calendar.current.component(.year, from: start))년 \(startStr) – \(Self.weekRangeFmt.string(from: end))"
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
        HStack(spacing: 0) {
            todayBar
            HStack(spacing: 10) {
                dateColumn
                shiftContent
            }
            .padding(.horizontal, 11)
            .padding(.vertical, 10)
        }
        .background(isToday ? Color.accentColor.opacity(0.08) : Color.clear)
    }

    private var todayBar: some View {
        Rectangle()
            .fill(isToday ? Color.accentColor : Color.clear)
            .frame(width: 3, height: 44)
    }

    private var dateColumn: some View {
        VStack(spacing: 1) {
            Text(Self.dayFmt.string(from: day.date))
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(dayColor)
            Text(Self.dateFmt.string(from: day.date))
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(isToday ? Color.accentColor : Color.primary)
        }
        .frame(width: 28)
    }

    private var shiftContent: some View {
        HStack(spacing: 8) {
            switch day.type {
            case .work:
                ShiftTypeBadge(label: "근무", type: .work)
                Text("\(day.startTime ?? "")–\(day.endTime ?? "")")
                    .font(.system(size: 13))
            case .off:
                ShiftTypeBadge(label: day.codeLabel.isEmpty ? "휴무" : day.codeLabel, type: .off)
            case .other:
                ShiftTypeBadge(label: day.codeLabel, type: .off)
            case .unregistered:
                Text("—").font(.system(size: 13)).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var dayColor: Color {
        if isToday { return Color.accentColor }
        let weekday = Calendar.current.component(.weekday, from: day.date)
        if weekday == 1 { return Color(.systemRed) }
        if weekday == 7 { return Color(.systemBlue) }
        return Color.secondary
    }
}
