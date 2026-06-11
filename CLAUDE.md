# CLAUDE.md — 시프트 위젯 앱 (Android)

교대근무자 스케쥴 위젯 앱. 스크린샷 1장 → OCR 파싱 → 홈 화면 위젯 표시.

**스택**: Kotlin, Jetpack Compose, Glance(위젯), Room, ML Kit Text Recognition, Firebase Analytics

---

## 아키텍처

```
ui/ → domain/ ← data/ ocr/ analytics/
```

- `ui`는 `domain` 인터페이스만 의존. `data`·`ocr`를 직접 참조하지 않는다.
- 파서는 `ScheduleParser` 인터페이스 구현체로 DI 주입. 포맷 추가 = 구현체 추가.
- ViewModel → UI State → Composable 단방향 흐름. sealed class로 상태/이벤트 모델링.

---

## 개인정보 (예외 없음)

1. **이미지는 파싱 완료 즉시 메모리 해제.** 파일 저장·캐시 금지.
2. 실패 이미지 전송은 사용자가 [보내기]를 탭한 경우에만. 전송 후 로컬 폐기.
3. 분석 이벤트에 이미지·이름·사번 포함 금지. OCR 원문 로그 시 6자리 이상 연속 숫자 마스킹.
4. 출퇴근 시간 값은 분석 서버로 보내지 않는다 (`user_edit` 이벤트 제외 — PRD §11.3).
5. 익명 UUID 외 식별자 수집 금지 (광고ID·위치·계정 불가).

---

## 코드 품질

**빌드 실패 조건** — 아래 모두 CI에서 강제:
- `./gradlew detekt` — 인지복잡도 15, 메서드 40줄, `!!` 연산자, 매직 넘버 금지
- `./gradlew ktlintCheck`
- `./gradlew lint` — `AbortOnError true`
- `./gradlew test jacocoTestReport` — `ocr/` 모듈 라인 커버리지 90% 미달 시 실패

**테스트 원칙**:
- Room은 `inMemoryDatabaseBuilder`로 실제 쿼리 테스트. DAO 모킹 금지.
- 분석 이벤트는 `AnalyticsEvent` sealed class로만 정의. 문자열 리터럴 직접 사용 금지.
- 신규 파서 템플릿 추가 시 OCR 샘플 10장 이상 `src/test/resources/ocr_samples/`에 추가.

---

## 하드 금지

- 앱 명칭·UI 어디에도 특정 프랜차이즈 상호 사용 금지.
- 근태코드 임의 해석·매핑 금지. `code_label`에 원문 그대로 저장.
- `퇴근 ≤ 출근` 자정 넘김 처리 로직 추가 금지 (PRD §3 명시 제약).
- Main thread I/O 금지. 개발 빌드에서 `StrictMode.ThreadPolicy` 활성화 유지.
