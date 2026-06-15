import SwiftUI
import WidgetKit

@main struct ShiftWidgetBundle: WidgetBundle {
    var body: some Widget {
        // 홈 화면 위젯
        ShiftWidget2x2()           // 2×2 (systemSmall)  — 오늘 근무
        ShiftWidget4x2()           // 4×2 (systemMedium) — 주간 스케줄
        // 잠금화면 위젯 (accessoryRectangular)
        ShiftLockWidget4x1()       // 오늘 날짜 + 근무 상태
        ShiftLockWidget4x2()       // 5일 미니 그리드
        ShiftLockWidgetCountdown() // 출근·퇴근 카운트다운
    }
}
