# CLAUDE.md — 시프트 위젯 앱 (모노레포 루트)

교대근무자 스케쥴 위젯 앱. 스크린샷 1장 → OCR 파싱 → 홈 화면 위젯 표시.

> **플랫폼별 상세 규칙**: [`android/CLAUDE.md`](android/CLAUDE.md) · [`ios/CLAUDE.md`](ios/CLAUDE.md)

---

## 레포 구조

```
shift/
  android/          ← Android 앱 (Kotlin, Jetpack Compose, Glance, ML Kit)
    app/            ← 앱 모듈
    domain/         ← 비즈니스 로직, 인터페이스 정의
    data/           ← Room DB, DataStore, Repository 구현체
    ocr/            ← ML Kit OCR 엔진 + 규칙 기반 파서
    ui/             ← Composable 화면
    analytics/      ← Firebase Analytics 구현체
    scripts/        ← pre-push 품질 검사 훅
  ios/              ← iOS 앱 (Xcode 초기화 대기 중)
  docs/             ← PRD, 분석 이벤트 정의
  .github/
    workflows/
      android.yml   ← Android CI (PR 시 자동 실행)
      ios.yml       ← iOS CI (placeholder)
```

---

## 개인정보 (양 플랫폼 예외 없음)

1. **이미지는 파싱 완료 즉시 메모리 해제.** 파일 저장·캐시 금지.
2. 실패 이미지 전송은 사용자가 [보내기]를 탭한 경우에만. 전송 후 로컬 폐기.
3. 분석 이벤트에 이미지·이름·사번 포함 금지. OCR 원문 로그 시 6자리 이상 연속 숫자 마스킹.
4. 출퇴근 시간 값은 분석 서버로 보내지 않는다 (`user_edit` 이벤트 제외 — PRD §11.3).
5. 익명 UUID 외 식별자 수집 금지 (광고ID·위치·계정 불가).

---

## 브랜치 · PR 워크플로우

- **main 직접 푸시 금지.** 모든 변경은 브랜치 → PR 경로로.
- 브랜치 명명: `feat/issue-N-설명`, `fix/issue-N-설명`, `docs/issue-N-설명`, `chore/issue-N-설명`
- PR은 반드시 관련 이슈 번호를 본문에 포함 (`Closes #N`).
- Android 변경: CI(`android.yml`)가 detekt·ktlint·lint·테스트·커버리지를 자동 검증.

---

## 공유 문서

- PRD: [`docs/스케쥴_위젯_앱_PRD_v1.2.md`](docs/스케쥴_위젯_앱_PRD_v1.2.md)
- 분석 이벤트: [`docs/analytics-events.md`](docs/analytics-events.md)

---

## 하드 금지 (양 플랫폼)

- 앱 명칭·UI 어디에도 특정 프랜차이즈 상호 사용 금지.
- 근태코드 임의 해석·매핑 금지. `code_label`에 원문 그대로 저장.
- `퇴근 ≤ 출근` 자정 넘김 처리 로직 추가 금지 (PRD §3 명시 제약).
