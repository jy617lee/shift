# 코드 리뷰 기준

## 1. 단일 책임 (Single Responsibility)
- 함수/클래스는 하나의 역할만 수행하는가
- 함수가 20줄을 넘어가면 분리를 고려할 것

## 2. 중복 제거 (DRY)
- 동일하거나 유사한 로직이 2곳 이상 존재하는가
- 있다면 공통 함수/클래스로 추출할 것

## 3. 명확한 네이밍
- 함수명은 동사로 시작하며 역할을 설명하는가 (예: `getUserSchedule`)
- `data`, `temp`, `obj` 같은 의미 없는 이름 금지
- 매직 넘버는 상수로 선언할 것 (예: `3` → `MAX_PARSE_FAIL_ROWS`)

## 4. 함수 인자
- 인자가 3개를 초과하면 data class로 묶을 것

## 5. 불필요한 코드 제거
- 사용하지 않는 import, 변수, 함수가 없는가
- 주석 처리된 dead code가 없는가
- TODO 주석은 GitHub 이슈로 대체할 것

## 6. 에러 처리
- 예외 상황이 명시적으로 처리되었는가
- 빈 catch 블록이 없는가

## 7. 의존성 방향
- UI 레이어가 DB/데이터 레이어를 직접 참조하지 않는가
- `ViewModel → Repository → DB` 방향을 지킬 것

## 8. Compose 전용
- Composable 함수가 side effect를 직접 실행하지 않는가
- 상태는 가능한 상위로 끌어올렸는가 (State Hoisting)

## 9. Kotlin Null 안전성
- `!!` 연산자 사용 금지. `?.let`, `?:`, `requireNotNull()`로 처리할 것
- nullable 반환값을 처리 없이 체이닝하지 않는가

## 10. 코루틴
- `GlobalScope` 사용 금지. `viewModelScope` 또는 DI로 주입된 scope 사용
- IO 작업은 `Dispatchers.IO`, CPU 집약 작업은 `Dispatchers.Default` 사용
- `suspend` 함수 내에서 blocking call(`Thread.sleep`, `runBlocking`) 금지

## 11. sealed class / when 완전성
- `when`이 sealed class/interface를 다룰 때 `else` 분기 금지
- 새 케이스 추가 시 컴파일 에러로 누락을 잡을 수 있도록 exhaustive하게 유지
