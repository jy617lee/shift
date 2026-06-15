import PhotosUI
import SwiftUI

struct HomeView: View {
    @State var viewModel: HomeViewModel
    @State private var registrationVM: RegistrationViewModel
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var showPhotoPicker = false
    @State private var showConfirmation = false
    @State private var confirmationVM: ConfirmationViewModel?
    @State private var showNotAScheduleAlert = false
    @State private var showParseErrorAlert = false
    @State private var parseErrorImage: UIImage?
    @State private var showSkipConflictDialog = false
    @State private var skipConflictCount = 0
    @State private var showSettings = false

    let repository: any ScheduleRepository
    let preferencesRepository: any UserPreferencesRepository

    init(repository: any ScheduleRepository, preferencesRepository: any UserPreferencesRepository,
         processImageUseCase: ProcessScheduleImageUseCase) {
        self.repository = repository
        self.preferencesRepository = preferencesRepository
        _viewModel = State(initialValue: HomeViewModel(repository: repository))
        _registrationVM = State(initialValue: RegistrationViewModel(
            processImage: processImageUseCase,
            preferencesRepository: preferencesRepository,
            repository: repository
        ))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                appBar
                ZStack {
                    contentBody
                    if registrationVM.phase == .processing { ProcessingOverlay() }
                    if let msg = viewModel.toastMessage { ToastBanner(message: msg) }
                }
            }
            .background(ShiftColors.background)
            .toolbar(.hidden, for: .navigationBar)
            .overlay(alignment: .bottomTrailing) { fabButton }
        }
        .photosPicker(isPresented: $showPhotoPicker, selection: $selectedPhotoItem, matching: .images)
        .onChange(of: selectedPhotoItem) { _, item in
            guard let item else { return }
            Task { await registrationVM.processSelectedItem(item) }
            selectedPhotoItem = nil
        }
        .onChange(of: registrationVM.phase) { _, phase in handlePhaseChange(phase) }
        .sheet(
            isPresented: $showConfirmation,
            onDismiss: { Task { await viewModel.loadWeeks() } },
            content: {
                if let vm = confirmationVM {
                    ConfirmationView(viewModel: vm, onDismiss: {
                        showConfirmation = false
                        if vm.savedSuccessfully { viewModel.showToast(savedToastMessage(vm.savedWeeksCount)) }
                    })
                }
            }
        )
        .alert("지원하지 않는 이미지 형식입니다", isPresented: $showNotAScheduleAlert) {
            Button("확인", role: .cancel) { registrationVM.reset() }
        } message: { Text("스케줄 표가 포함된 이미지를 선택해주세요.") }
        .alert("인식 실패", isPresented: $showParseErrorAlert) {
            Button("보내기") { sendFailureReport(); registrationVM.reset() }
            Button("취소", role: .cancel) { registrationVM.reset() }
        } message: { Text("스케줄을 인식하지 못했습니다. 이미지를 보내주시면 개선에 활용할게요.") }
        .alert("스케줄 교체", isPresented: $showSkipConflictDialog) {
            Button("교체하기") { Task { await registrationVM.proceedSkipReplace() } }
            Button("취소", role: .cancel) { registrationVM.reset() }
        } message: { Text("\(skipConflictCount)개 주 스케줄이 이미 있습니다. 교체할까요?") }
        .task { await viewModel.loadWeeks() }
    }

    @ViewBuilder
    private var contentBody: some View {
        if viewModel.isLoading {
            ProgressView()
        } else if viewModel.hasError {
            HomeErrorView { Task { await viewModel.loadWeeks() } }
        } else if viewModel.weeks.isEmpty {
            HomeEmptyView { showPhotoPicker = true }
        } else {
            WeekListView(weeks: viewModel.weeks, today: Date())
        }
    }

    private var appBar: some View {
        HStack(spacing: 8) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.accentColor)
                    .frame(width: 28, height: 28)
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
            }
            Text("Shift")
                .font(.system(size: 16, weight: .medium))
            Spacer()
            Button { showSettings = true } label: {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(ShiftColors.mutedForeground)
            }
            .buttonStyle(.plain)
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
        .overlay(alignment: .bottom) {
            Divider()
        }
    }

    private var fabButton: some View {
        Button { showPhotoPicker = true } label: {
            HStack(spacing: 6) {
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .medium))
                Text("스케줄 추가")
                    .font(.system(size: 14, weight: .medium))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 18)
            .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .padding()
        .opacity(registrationVM.phase == .processing ? 0 : 1)
    }

    private func handlePhaseChange(_ phase: RegistrationPhase) {
        switch phase {
        case .readyToReview:
            showConfirmationSheet()
        case .skipModeConflict(let count):
            skipConflictCount = count
            showSkipConflictDialog = true
        case .skipModeSaved(let count):
            viewModel.showToast(savedToastMessage(count))
            Task { await viewModel.loadWeeks() }
            registrationVM.reset()
        case .notASchedule:
            showNotAScheduleAlert = true
        case .parseError:
            parseErrorImage = registrationVM.capturedImage
            showParseErrorAlert = true
        case .saveError:
            viewModel.showToast("저장에 실패했어요. 다시 시도해주세요.")
            registrationVM.reset()
        case .idle, .processing:
            break
        }
    }

    private func showConfirmationSheet() {
        let vm = ConfirmationViewModel(
            weeks: registrationVM.parsedWeeks,
            image: registrationVM.capturedImage,
            repository: repository,
            preferencesRepository: preferencesRepository
        )
        confirmationVM = vm
        showConfirmation = true
        registrationVM.reset()
    }

    private func sendFailureReport() {
        if let img = parseErrorImage {
            registrationVM.reportFailure(image: img)
        }
    }

    private func savedToastMessage(_ count: Int) -> String {
        count == 1 ? "스케줄이 저장됐어요" : "\(count)주 스케줄이 저장됐어요"
    }
}

struct WeekListView: View {
    let weeks: [ScheduleWeek]
    let today: Date

    var body: some View {
        let todayNorm = Calendar.current.startOfDay(for: today)
        let initialIndex = weeks.firstIndex { week in
            todayNorm >= week.weekStartDate && todayNorm <= Calendar.current.date(
                byAdding: .day, value: 6, to: week.weekStartDate) ?? week.weekStartDate
        } ?? 0
        ScrollViewReader { proxy in
            List(weeks, id: \.weekStartDate) { week in
                WeekCardView(week: week, today: todayNorm)
                    .listRowInsets(EdgeInsets(top: 5, leading: 16, bottom: 5, trailing: 16))
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .id(week.weekStartDate)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .onAppear { proxy.scrollTo(weeks[safe: initialIndex]?.weekStartDate, anchor: .top) }
        }
    }
}

struct ProcessingOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.4).ignoresSafeArea()
            ProgressView().tint(.white)
        }
    }
}

struct ToastBanner: View {
    let message: String
    var body: some View {
        VStack {
            Spacer()
            Text(message)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color(.systemGray2), in: Capsule())
                .padding(.bottom, 80)
        }
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

struct HomeEmptyView: View {
    let onAdd: () -> Void
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 40))
                .foregroundStyle(.secondary)
            Text("등록된 스케줄이 없어요")
                .font(.headline)
            Text("스케줄 추가 버튼을 눌러\n첫 번째 일정을 등록해 보세요.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("지금 추가하기", action: onAdd)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
        }
        .padding(32)
    }
}

struct HomeErrorView: View {
    let onRetry: () -> Void
    var body: some View {
        VStack(spacing: 16) {
            Text("스케줄을 불러오는 중\n오류가 발생했습니다")
                .multilineTextAlignment(.center)
                .font(.headline)
            Button("다시 시도", action: onRetry)
                .buttonStyle(.borderedProminent)
        }
    }
}

extension Array {
    fileprivate subscript(safe index: Int) -> Element? {
        guard index >= 0, index < count else { return nil }
        return self[index]
    }
}
