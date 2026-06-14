// swiftlint:disable file_length
import SwiftUI

struct ConfirmationView: View {
    @State var viewModel: ConfirmationViewModel
    let onDismiss: () -> Void
    @State private var showEditSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if let image = viewModel.image {
                    ImagePreviewView(image: image)
                }
                weekList
            }
            .navigationTitle("스케쥴 확인")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbarContent }
        }
        .onChange(of: viewModel.savedSuccessfully) { _, saved in if saved { onDismiss() } }
        .onChange(of: viewModel.isCancelled) { _, cancelled in if cancelled { onDismiss() } }
        .onChange(of: viewModel.editing) { _, state in showEditSheet = state != nil }
        .alert("스케쥴 교체", isPresented: .constant(viewModel.conflictCount > 0)) {
            Button("교체하기", action: viewModel.proceedWithReplace)
            Button("취소", role: .cancel, action: viewModel.dismissConflict)
        } message: {
            Text("\(viewModel.conflictCount)개 주 스케쥴이 이미 있습니다.\n교체할까요?")
        }
        .alert("다음부터 이 단계를 건너뛸까요?", isPresented: $viewModel.showSkipPrompt) {
            Button("다음부터 건너뛰기") { viewModel.answerSkipPrompt(skipInFuture: true) }
            Button("매번 확인할게요", role: .cancel) { viewModel.answerSkipPrompt(skipInFuture: false) }
        } message: {
            Text("저장하기를 누르면 스케쥴 확인 없이 바로 저장돼요.\n설정에서 언제든 바꿀 수 있어요.")
        }
        .sheet(isPresented: $showEditSheet, onDismiss: viewModel.dismissEdit) {
            if let editing = viewModel.editing {
                DayEditSheet(editing: editing, onDraftChange: viewModel.updateDraft,
                             onCommit: viewModel.commitEdit, onDismiss: viewModel.dismissEdit)
                    .presentationDetents([.medium, .large])
            }
        }
    }

    private var weekList: some View {
        List {
            ForEach(Array(viewModel.weeks.enumerated()), id: \.offset) { weekIdx, week in
                Section(header: WeekSectionHeader(week: week)) {
                    ForEach(Array(week.days.enumerated()), id: \.offset) { dayIdx, day in
                        ReviewDayRow(day: day, isToday: Calendar.current.isDateInToday(day.date)) {
                            viewModel.startEdit(weekIndex: weekIdx, dayIndex: dayIdx)
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button("취소", action: viewModel.cancel)
        }
        ToolbarItem(placement: .confirmationAction) {
            Button("저장하기", action: viewModel.confirm)
                .bold()
        }
    }
}

struct ImagePreviewView: View {
    let image: UIImage
    @State private var scale: CGFloat = 1.0
    @State private var offset: CGSize = .zero

    var body: some View {
        Image(uiImage: image)
            .resizable()
            .scaledToFit()
            .frame(height: 220)
            .clipShape(Rectangle())
            .scaleEffect(scale)
            .offset(offset)
            .gesture(MagnifyGesture()
                .onChanged { val in scale = max(1, min(5, val.magnification)) }
            )
    }
}

struct WeekSectionHeader: View {
    let week: ScheduleWeek
    private static let fmt: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "M월 d일"; return f
    }()

    var body: some View {
        let start = week.weekStartDate
        let end = Calendar.current.date(byAdding: .day, value: 6, to: start) ?? start
        let year = Calendar.current.component(.year, from: start)
        let endDay = Calendar.current.component(.day, from: end)
        let sameMonth = Calendar.current.component(.month, from: start) == Calendar.current.component(.month, from: end)
        let header = sameMonth
            ? "\(year)년 \(Self.fmt.string(from: start))–\(endDay)일"
            : "\(year)년 \(Self.fmt.string(from: start)) – \(Self.fmt.string(from: end))"
        Text(header)
    }
}

struct ReviewDayRow: View {
    let day: ScheduleDay
    let isToday: Bool
    let onTap: () -> Void

    private static let dayFmt: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "EE"; f.locale = Locale(identifier: "ko_KR"); return f
    }()
    private static let dateFmt: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "M/d"; return f
    }()

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                VStack(spacing: 1) {
                    Text(Self.dayFmt.string(from: day.date))
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(dayColor)
                    Text(Self.dateFmt.string(from: day.date))
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(isToday ? Color.accentColor : Color.primary)
                        .fontDesign(.monospaced)
                }
                .frame(width: 40)
                shiftContent
                Image(systemName: "pencil").font(.system(size: 12)).foregroundStyle(.secondary)
            }
        }
        .foregroundStyle(.primary)
        .listRowBackground(isToday ? Color.accentColor.opacity(0.08) : Color(.secondarySystemBackground))
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
                ShiftTypeBadge(label: day.codeLabel.isEmpty ? "기타" : day.codeLabel, type: .off)
            case .unregistered:
                Text("—").font(.system(size: 13)).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var dayColor: Color {
        if isToday { return .accentColor }
        let weekday = Calendar.current.component(.weekday, from: day.date)
        return weekday == 1 ? Color(.systemRed) : weekday == 7 ? Color(.systemBlue) : .secondary
    }
}

struct DayEditSheet: View {
    let editing: EditingState
    let onDraftChange: (ScheduleDay) -> Void
    let onCommit: () -> Void
    let onDismiss: () -> Void

    @State private var startTimeText: String
    @State private var endTimeText: String
    @State private var codeLabelText: String
    @State private var startError = false
    @State private var endError = false

    private static let dayFmt: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "M/d(EE)"; f.locale = Locale(identifier: "ko_KR"); return f
    }()

    init(editing: EditingState, onDraftChange: @escaping (ScheduleDay) -> Void,
         onCommit: @escaping () -> Void, onDismiss: @escaping () -> Void) {
        self.editing = editing
        self.onDraftChange = onDraftChange
        self.onCommit = onCommit
        self.onDismiss = onDismiss
        _startTimeText = State(initialValue: editing.draft.startTime ?? "")
        _endTimeText = State(initialValue: editing.draft.endTime ?? "")
        _codeLabelText = State(initialValue: editing.draft.codeLabel)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("\(Self.dayFmt.string(from: editing.draft.date)) 수정")
                .font(.headline)
                .padding(.horizontal, 20)
                .padding(.top, 20)
            Form {
                dayTypeSection
                if editing.draft.type == .work { timeSection }
                codeSection
            }
            editButtons
        }
    }

    private var dayTypeSection: some View {
        Section("유형") {
            HStack(spacing: 8) {
                typeButton("근무", .work)
                typeButton("휴무", .off)
                typeButton("기타", .other)
            }
        }
    }

    private func typeButton(_ label: String, _ type: DayType) -> some View {
        let selected = editing.draft.type == type
        return Button(label) {
            var draft = editing.draft
            draft.type = type
            if type != .work { draft.startTime = nil; draft.endTime = nil }
            onDraftChange(draft)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(selected ? Color.accentColor : Color(.systemGray5), in: RoundedRectangle(cornerRadius: 8))
        .foregroundStyle(selected ? .white : .primary)
    }

    private var timeSection: some View {
        Section("시간") {
            HStack(spacing: 12) {
                LabeledContent("출근") {
                    TextField("HH:mm", text: $startTimeText)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .foregroundStyle(startError ? .red : .primary)
                        .onChange(of: startTimeText) { _, text in
                            startTimeText = autoFormatTime(text)
                            startError = false
                        }
                }
                LabeledContent("퇴근") {
                    TextField("HH:mm", text: $endTimeText)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .foregroundStyle(endError ? .red : .primary)
                        .onChange(of: endTimeText) { _, text in
                            endTimeText = autoFormatTime(text)
                            endError = false
                        }
                }
            }
        }
    }

    private var codeSection: some View {
        Section("근태코드") {
            TextField("근태코드", text: $codeLabelText)
        }
    }

    private var editButtons: some View {
        HStack(spacing: 10) {
            Button("취소", role: .cancel, action: onDismiss)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color(.systemGray5), in: RoundedRectangle(cornerRadius: 12))
            Button("저장") { trySave() }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
                .fontWeight(.semibold)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }

    private func trySave() {
        var draft = editing.draft
        if draft.type == .work {
            guard isValidTime(startTimeText), isValidTime(endTimeText) else {
                startError = !isValidTime(startTimeText)
                endError = !isValidTime(endTimeText)
                return
            }
            draft.startTime = startTimeText
            draft.endTime = endTimeText
        }
        draft.codeLabel = codeLabelText
        onDraftChange(draft)
        onCommit()
    }

    private func isValidTime(_ text: String) -> Bool {
        guard text.count == 5, text.dropFirst(2).first == ":" else { return false }
        let parts = text.split(separator: ":")
        guard parts.count == 2,
              let hour = Int(parts[0]), let min = Int(parts[1]) else { return false }
        return hour < 24 && min < 60
    }

    private func autoFormatTime(_ raw: String) -> String {
        let digits = raw.filter(\.isNumber)
        guard digits.count >= 2 else { return digits }
        let h = String(digits.prefix(2))
        let m = String(digits.dropFirst(2).prefix(2))
        return m.isEmpty ? h : "\(h):\(m)"
    }
}
// swiftlint:enable file_length
