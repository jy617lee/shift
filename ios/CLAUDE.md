# CLAUDE.md — 시프트 위젯 앱 (iOS)

교대근무자 스케쥴 위젯 앱. 스크린샷 1장 → OCR 파싱 → 홈 화면 위젯 표시.

**스택**: Swift, SwiftUI, WidgetKit, Vision Framework, SwiftData, Firebase Analytics

> **공유 규칙**: 루트 [`CLAUDE.md`](../CLAUDE.md) · **PRD**: [`docs/스케쥴_위젯_앱_PRD_v1.2.md`](../docs/스케쥴_위젯_앱_PRD_v1.2.md)

---

## 프로젝트 구조

```
ios/shift/
  shift/              ← 앱 소스
    shiftApp.swift    ← 앱 진입점
    ContentView.swift ← 루트 뷰 (개발 중 교체 예정)
    Assets.xcassets/
  shiftTests/         ← 단위 테스트 (XCTest)
  shiftUITests/       ← UI 테스트
  shift.xcodeproj/
    project.pbxproj   ← 프로젝트 파일 (git 추적)
    project.xcworkspace/
      contents.xcworkspacedata  ← git 추적
      xcshareddata/             ← git 추적 (공유 스킴)
```

---

## 아키텍처 (Android 대응)

```
Android          →   iOS
domain/          →   Sources/Domain/      (Swift struct/enum, 프로토콜)
data/            →   Sources/Data/        (SwiftData, UserDefaults)
ocr/             →   Sources/OCR/         (Vision Framework + 파서)
ui/              →   Sources/UI/          (SwiftUI 화면)
analytics/       →   Sources/Analytics/   (Firebase Analytics)
app (widget)     →   ShiftWidget target   (WidgetKit Extension)
```

- UI 레이어는 Domain 프로토콜만 의존. SwiftData·Vision을 직접 참조하지 않는다.
- ViewModel → `@Observable` / `ObservableObject` → View 단방향 흐름.
- 비동기 작업은 `async/await` + `Task`. Combine 신규 사용 금지.

---

## 개인정보 (예외 없음)

1. **이미지는 파싱 완료 즉시 메모리 해제.** `UIImage` 참조 nil 처리. 파일 저장·캐시 금지.
2. 실패 이미지 전송은 사용자가 [보내기]를 탭한 경우에만. 전송 후 로컬 폐기.
3. 분석 이벤트에 이미지·이름·사번 포함 금지. OCR 원문 로그 시 6자리 이상 연속 숫자 마스킹.
4. 출퇴근 시간 값은 분석 서버로 보내지 않는다 (`user_edit` 이벤트 제외 — PRD §11.3).
5. 익명 UUID 외 식별자 수집 금지 (광고ID·위치·계정·IDFA 불가). UUID는 **Keychain** 저장.

---

## 코드 품질

**빌드 실패 조건** — CI에서 강제:
- `xcodebuild test` — 단위 테스트 전체 통과
- **SwiftLint** — force unwrap(`!`) 금지, 함수 40줄 이하, 인지복잡도 15 이하
- OCR 소스 커버리지 90%+ (Android와 동일 기준)

**테스트 원칙**:
- SwiftData는 in-memory 컨테이너(`ModelConfiguration(isStoredInMemoryOnly: true)`)로 테스트. 실제 파일 DB 사용 금지.
- 분석 이벤트는 `AnalyticsEvent` enum으로만 정의. 문자열 리터럴 직접 사용 금지.
- OCR 파서 테스트는 Android와 동일한 샘플(`android/ocr/src/test/resources/ocr_samples/`)을 재사용.

---

## 위젯 (WidgetKit)

- **App Group** 필수: 앱 ↔ Widget Extension 간 SwiftData / UserDefaults 공유.
  - App Group ID: `group.com.yourteam.shift` (Xcode Signing & Capabilities에서 설정)
- `TimelineProvider`: 매일 00:00 + 스케쥴 저장/교체 직후 `WidgetCenter.shared.reloadAllTimelines()`
- 딥링크: `.widgetURL(URL(string: "shift://open?source=widget_\(size)"))` — `app_open` 이벤트 source 전달

**위젯 사이즈 매핑** (PRD §7):
| Android | iOS WidgetKit | `WidgetFamily` |
|---------|--------------|----------------|
| 2×1 | Small | `.systemSmall` |
| 4×1 | Medium | `.systemMedium` |
| 4×2 주간 미리보기 | Medium | `.systemMedium` |

---

## 데이터 로깅

모든 분석 이벤트 목록·필드·발생 위치는 **`../docs/analytics-events.md`** 에 관리한다.

**`AnalyticsEvent`를 변경(추가·수정·삭제)할 때는 반드시 `../docs/analytics-events.md`도 함께 수정한다.**

---

## iOS 전용 주의사항

- **Privacy Manifest** (`PrivacyInfo.xcprivacy`): iOS 17+ App Store 심사 필수. UserDefaults·FileManager 사용 이유 명시.
- **PhotosPicker** (`PhotosUI`): `PHPickerViewController` 기반, 권한 팝업 없이 갤러리 접근 가능 (iOS 16+).
- **`Info.plist`**: `NSPhotoLibraryUsageDescription` 불필요 (PhotosPicker 사용 시). 단, 직접 저장 시 필요.
- **force unwrap(`!`) 절대 금지**: `guard let` / `if let` / `??` 사용.
- **Main thread**: UI 업데이트는 `@MainActor`. 백그라운드 작업은 `Task.detached` 또는 actor 분리.

---

## 하드 금지

- 앱 명칭·UI 어디에도 특정 프랜차이즈 상호 사용 금지.
- 근태코드 임의 해석·매핑 금지. `codeLabel`에 원문 그대로 저장.
- `퇴근 ≤ 출근` 자정 넘김 처리 로직 추가 금지 (PRD §3 명시 제약).
- Main thread I/O 금지.
