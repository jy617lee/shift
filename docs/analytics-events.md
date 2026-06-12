# 데이터 로깅 이벤트 목록

> **유지 관리 규칙**: 분석 이벤트를 추가·변경·삭제할 때는 이 파일도 함께 수정해야 합니다.
> `AnalyticsEvent.kt`와 이 파일은 항상 동기화 상태를 유지합니다.

모든 이벤트는 `domain/analytics/AnalyticsEvent.kt`의 sealed class로 정의됩니다.
이벤트 문자열 리터럴 직접 사용은 금지됩니다 (CLAUDE.md 참조).

---

## §11.2 사용·리텐션 이벤트

### `AppOpen`
| 필드 | 타입 | 설명 |
|------|------|------|
| `source` | `AppOpenSource` | 앱 진입 경로 |

**`AppOpenSource` 값**: `icon`, `widget_2x1`, `widget_2x2`, `widget_4x1`, `widget_4x2_countdown`, `widget_4x2_weekly`

---

### `WidgetActive`
| 필드 | 타입 | 설명 |
|------|------|------|
| `types` | `List<String>` | 현재 배치된 위젯 종류 목록 |

---

### `HomeWeekViewed`
| 필드 | 타입 | 설명 |
|------|------|------|
| `offset` | `Int` | 현재 주 기준 오프셋 (0=이번 주, -1=지난 주 등) |

---

### `SettingChanged`
| 필드 | 타입 | 설명 |
|------|------|------|
| `key` | `SettingKey` | 변경된 설정 키 |
| `value` | `String` | 변경 후 값 (문자열) |

**`SettingKey` 값**: `skip_confirm`

**발생 위치**: `SettingsViewModel.setSkipConfirm()`

---

## §11.3 등록 퍼널 이벤트

퍼널 단계: `RegisterStart` → `ImageSelected` → `Stage1Result` → `ParseResult` → `ConfirmShown` → `UserEdit` (0회 이상) → `RegisterComplete` 또는 `RegisterAbandon`

모든 퍼널 이벤트는 `sessionId`(UUID)로 연결됩니다.

### `RegisterStart`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |

**발생 위치**: `RegistrationViewModel.onRegisterStart()` — 이미지 피커 열기 직전

---

### `ImageSelected`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `imageWidth` | `Int` | 선택된 이미지 가로 픽셀 |
| `imageHeight` | `Int` | 선택된 이미지 세로 픽셀 |

**발생 위치**: `RegistrationViewModel` — 이미지 선택 후 OCR 시작 전

---

### `Stage1Result`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `pass` | `Boolean` | 품질 검사 통과 여부 |
| `failReason` | `String?` | 실패 시 사유 (통과 시 null) |

**발생 위치**: `RegistrationViewModel` — OCR 전처리/품질 검사 완료 시

---

### `ParseResult`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `failedRows` | `Int` | 파싱 실패한 행 수 |
| `durationMs` | `Long` | OCR + 파싱 소요 시간 (밀리초) |
| `ocrConfidenceAvg` | `Float` | OCR 평균 신뢰도 |

**발생 위치**: `RegistrationViewModel` — 파싱 완료 후

> **개인정보**: OCR 원문 미포함. 6자리 이상 연속 숫자는 마스킹 처리.

---

### `RowFailDetail`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `rowIndex` | `Int` | 실패한 행 인덱스 |
| `failReason` | `String` | 실패 사유 |
| `rawTextMasked` | `String` | 마스킹된 OCR 원문 |
| `cellConfidence` | `Float` | 해당 셀 신뢰도 |

> **개인정보**: `rawTextMasked`는 6자리 이상 연속 숫자 마스킹 필수.

---

### `SendDialog`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `consented` | `Boolean` | 사용자가 이미지 전송에 동의했는지 여부 |

---

### `ConfirmShown`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `skipped` | `Boolean` | 확인 화면 생략 여부 (현재 항상 `false`) |

**발생 위치**: `ConfirmationViewModel` init — 확인 화면 진입 시

---

### `UserEdit`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `rowIndex` | `Int` | 편집한 행 인덱스 |
| `field` | `String` | 편집한 필드명 |
| `parsedValue` | `String` | OCR 파싱 원본 값 |
| `correctedValue` | `String` | 사용자가 수정한 값 |
| `wasFailedRow` | `Boolean` | 해당 행이 파싱 실패 행이었는지 |
| `editSource` | `String` | 편집 경로 (예: `"manual"`) |

**발생 위치**: `ConfirmationViewModel.trackUserEdit()` — 확인 화면에서 셀 편집 시

> **개인정보**: 출퇴근 시간 값 포함 가능. PRD §11.3에 따라 전송 허용.

---

### `RegisterComplete`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `editedRows` | `Int` | 사용자가 수정한 행 수 |
| `manualRows` | `Int` | 수동 입력 행 수 |
| `replace` | `Boolean` | 기존 스케줄 교체 여부 |
| `totalDurationMs` | `Long` | 등록 시작부터 완료까지 소요 시간 (밀리초) |

**발생 위치**:
- `ConfirmationViewModel.confirmSave()` — 확인 화면에서 저장 시
- `RegistrationFlowStateHolder.autoSave()` — skipConfirm 활성 시 자동 저장 시

---

### `RegisterAbandon`
| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `String` | 등록 세션 UUID |
| `lastStep` | `String` | 이탈 시점 단계명 |

**발생 위치**:
- `RegistrationViewModel` — 이미지 선택 화면 이탈 시 (`lastStep`: 단계명)
- `ConfirmationViewModel.cancel()` — 확인 화면 취소 시 (`lastStep: "confirm"`)

---

## 미수집 항목 (수집 금지)

- 광고 ID (GAID / IDFA)
- 위치 정보
- 계정 정보 (이름·이메일·전화번호)
- 이미지 파일 (파싱 후 즉시 메모리 해제)
- 출퇴근 시간 원본값 (`user_edit` 이벤트 외)

## 구현 현황

| 항목 | 상태 |
|------|------|
| Firebase 연동 | 미완료 (`NoOpAnalyticsTracker` 사용 중) |
| 이벤트 정의 | 완료 (`AnalyticsEvent.kt`) |
| 퍼널 이벤트 발생 | 완료 |
| 설정 이벤트 발생 | 완료 |

Firebase `google-services.json` 추가 후 `NoOpAnalyticsTracker` → `FirebaseAnalyticsTracker`로 교체 예정.
