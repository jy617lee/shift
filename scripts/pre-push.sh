#!/bin/bash
# pre-push 품질 검사. 실패한 항목이 하나라도 있으면 push를 차단한다.
# 긴급 우회: git push --no-verify (권장하지 않음)

set -o pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT" || exit 1

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FAILED=0
STEP=0

step() {
    STEP=$((STEP + 1))
    echo ""
    echo "[$STEP] $1..."
}

pass() { echo -e "${GREEN}    ✅ 통과${NC}"; }
fail() { echo -e "${RED}    ❌ 실패${NC}"; FAILED=1; }
skip() { echo -e "${YELLOW}    ⏭  건너뜀: $1${NC}"; }

echo "================================================"
echo " pre-push 품질 검사"
echo "================================================"

# ── 1. detekt (정적 분석 + 죽은 코드) ───────────────
step "detekt 정적 분석"
if ./gradlew :app:detekt --quiet 2>&1; then
    pass
else
    fail
    echo "    → build/reports/detekt/detekt.html 에서 상세 확인"
fi

# ── 2. ktlint (린터) ─────────────────────────────────
step "ktlint 린터"
if ./gradlew :app:ktlintCheck --quiet 2>&1; then
    pass
else
    fail
    echo "    → 자동 수정: ./gradlew :app:ktlintFormat"
fi

# ── 3. Android Lint ───────────────────────────────────
step "Android Lint"
if ./gradlew :app:lint --quiet 2>&1; then
    pass
else
    fail
    echo "    → build/reports/lint/lint-report.html 에서 상세 확인"
fi

# ── 4. 테스트 + 커버리지 ──────────────────────────────
step "단위 테스트 + 커버리지 검증 (기준: 30%)"
if ./gradlew :app:testDebugUnitTest :app:jacocoTestCoverageVerification --quiet 2>&1; then
    pass
else
    fail
    echo "    → build/reports/jacoco/jacocoTestReport/html/index.html 에서 상세 확인"
fi

# ── 5. 테스트-프로덕션 비율 ───────────────────────────
step "테스트 코드 비율 검사 (기준: 50%)"
if ./gradlew :app:checkTestRatio --quiet 2>&1; then
    pass
else
    fail
fi

# ── 6. 중복 코드 (jscpd) ─────────────────────────────
step "중복 코드 검사 (jscpd)"
if command -v jscpd &>/dev/null; then
    if jscpd --config .jscpd.json 2>&1; then
        pass
    else
        fail
        echo "    → 위에 출력된 파일:라인 위치의 중복 코드를 제거해주세요."
    fi
else
    skip "jscpd 미설치. 설치 후 중복 검사가 활성화됩니다."
    echo "       설치: npm install -g jscpd  또는  brew install jscpd"
fi

# ── 결과 ─────────────────────────────────────────────
echo ""
echo "================================================"
if [ $FAILED -ne 0 ]; then
    echo -e "${RED} ❌ 품질 검사 실패. 위 항목을 수정 후 다시 push하세요.${NC}"
    echo "================================================"
    exit 1
fi

echo -e "${GREEN} ✅ 모든 품질 검사 통과${NC}"
echo "================================================"
exit 0
