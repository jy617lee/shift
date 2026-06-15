#!/bin/bash
# 테스트 코드가 프로덕션 대비 50% 이상인지 검사
# SwiftUI View 파일(*View.swift, *App.swift)은 선언형 UI — 단위 테스트 불가이므로 PROD 카운트에서 제외

REPO_ROOT="$(git rev-parse --show-toplevel)"
PROJ_DIR="$REPO_ROOT/ios/shift"

count_lines() {
    find "$1" -name "*.swift" 2>/dev/null \
        | xargs grep -v '^\s*$' 2>/dev/null \
        | wc -l \
        | tr -d ' '
}

count_lines_excl_views() {
    find "$1" -name "*.swift" \
        ! -name "*View.swift" \
        ! -name "*App.swift" 2>/dev/null \
        | xargs grep -v '^\s*$' 2>/dev/null \
        | wc -l \
        | tr -d ' '
}

PROD_LINES=$(count_lines_excl_views "$PROJ_DIR/shift")
TEST_LINES=$(count_lines "$PROJ_DIR/shiftTests")
TOTAL_PROD=$(count_lines "$PROJ_DIR/shift")
EXCLUDED=$((TOTAL_PROD - PROD_LINES))

echo "프로덕션 코드: ${PROD_LINES}줄 (View/App 파일 ${EXCLUDED}줄 제외)"
echo "테스트 코드: ${TEST_LINES}줄"

if [ "$PROD_LINES" -lt 300 ]; then
    echo "프로덕션 코드 300줄 미만 — 비율 검사 건너뜀"
    exit 0
fi

PCT=$((TEST_LINES * 100 / PROD_LINES))
echo "테스트 비율: ${PCT}%"

if [ "$PCT" -lt 50 ]; then
    echo "❌ 테스트 코드 부족 (${PCT}% < 50%). 테스트를 추가해주세요."
    exit 1
fi

echo "✅ 테스트 비율 통과 (${PCT}% >= 50%)"
exit 0
