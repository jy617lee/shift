# 시프트 — 교대근무 스케쥴 위젯 앱

교대(시프트) 근무자가 사내 앱 주간 스케쥴 화면을 **스크린샷 1장**으로 등록하면,  
홈 화면 **위젯**에서 오늘의 근무 시간을 즉시 확인할 수 있는 앱입니다.

> 스크린샷 → OCR 파싱 → 위젯 표시. 모든 데이터는 기기 내 로컬 저장.

---

## 플랫폼

| 플랫폼 | 상태 | 디렉터리 |
|--------|------|----------|
| Android | 개발 중 | [`android/`](android/) |
| iOS | 준비 중 (Xcode 초기화 예정) | [`ios/`](ios/) |

---

## 디렉터리 구조

```
shift/
  android/      Android 앱 (Kotlin, Jetpack Compose, Glance, ML Kit)
  ios/          iOS 앱 (Swift, SwiftUI, WidgetKit — 준비 중)
  docs/         PRD, 분석 이벤트 정의
  .github/
    workflows/
      android.yml   Android CI (PR 시 자동 실행)
      ios.yml       iOS CI (준비 중)
```

---

## Android 개발 시작

**필수 환경**
- Android Studio Ladybug 이상
- JDK 17
- Android SDK (ANDROID_SDK_ROOT 환경 변수 설정)

```bash
cd android
./gradlew assembleDebug          # 빌드
./gradlew testDebugUnitTest      # 단위 테스트
./gradlew detekt ktlintCheck     # 정적 분석
```

**pre-push 훅 설치** (로컬 품질 검사 자동화)
```bash
ln -sf ../../android/scripts/pre-push.sh .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

---

## iOS 개발 시작

> Xcode 프로젝트 초기화 후 이 섹션을 업데이트할 예정입니다.

---

## 공유 문서

- [PRD (제품 요구사항)](docs/스케쥴_위젯_앱_PRD_v1.2.md)
- [분석 이벤트 정의](docs/analytics-events.md)

---

## 기여 가이드

1. 작업 전 이슈 생성 또는 기존 이슈 확인
2. `feat/issue-N-설명` 형식으로 브랜치 생성
3. PR 본문에 `Closes #N` 포함
4. CI 통과 + 리뷰 승인 후 머지

> **main 직접 푸시 금지.**
