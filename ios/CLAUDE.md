# CLAUDE.md — iOS (준비 중)

> **상태**: Xcode 프로젝트 초기화 전 placeholder.
> Xcode로 `ios/` 디렉터리에 프로젝트를 생성한 후 이 파일을 채운다.

---

## 예정 스택

- **언어**: Swift
- **UI**: SwiftUI
- **위젯**: WidgetKit (Small / Medium — §7 대응표 참조)
- **OCR**: Vision Framework (`VNRecognizeTextRequest`)
- **데이터**: Core Data 또는 SwiftData (로컬 전용)
- **분석**: Firebase Analytics (Android와 동일 이벤트 스키마)

## 공유 문서

- PRD: [`docs/스케쥴_위젯_앱_PRD_v1.2.md`](../docs/스케쥴_위젯_앱_PRD_v1.2.md)
- 분석 이벤트: [`docs/analytics-events.md`](../docs/analytics-events.md)
- 공유 규칙: 루트 [`CLAUDE.md`](../CLAUDE.md)

## 개인정보 (루트 CLAUDE.md와 동일 원칙 적용)

- 이미지는 파싱 완료 즉시 메모리 해제. 저장·캐시 금지.
- 분석 이벤트에 이미지·이름·사번 포함 금지.
- 익명 UUID 외 식별자 수집 금지.
