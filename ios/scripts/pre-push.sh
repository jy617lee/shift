#!/bin/bash
# iOS pre-push 품질 검사. 실패한 항목이 하나라도 있으면 push를 차단한다.
# 긴급 우회: git push --no-verify (권장하지 않음)

set -o pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
IOS_DIR="$REPO_ROOT/ios"
PROJ_DIR="$IOS_DIR/shift"

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
echo " pre-push 품질 검사 (iOS)"
echo "================================================"

# ── 1. SwiftLint (정적 분석 + 린터) ──────────────────
step "SwiftLint 정적 분석 + 린터"
if command -v swiftlint &>/dev/null; then
    if swiftlint --strict --config "$IOS_DIR/.swiftlint.yml" --path "$IOS_DIR/shift/shift" 2>&1; then
        pass
    else
        fail
        echo "    → 자동 수정: swiftlint --fix --config ios/.swiftlint.yml"
    fi
else
    skip "swiftlint 미설치. 설치 후 정적 분석이 활성화됩니다."
    echo "       설치: brew install swiftlint"
fi

# ── 2. 단위 테스트 + 커버리지 ─────────────────────────
step "단위 테스트 + 커버리지 수집"
XCRESULT_PATH="/tmp/shift_prepush.xcresult"
rm -rf "$XCRESULT_PATH"
if xcodebuild test \
    -project "$PROJ_DIR/shift.xcodeproj" \
    -scheme shift \
    -destination 'platform=iOS Simulator,OS=latest,name=iPhone 16' \
    -enableCodeCoverage YES \
    -resultBundlePath "$XCRESULT_PATH" \
    -quiet 2>&1; then
    pass
else
    fail
    echo "    → 빌드 로그를 확인하거나 Xcode에서 직접 테스트를 실행해주세요."
fi

# ── 3. 커버리지 검증 ───────────────────────────────────
step "커버리지 검증 (기준: 30%)"
if [ -e "$XCRESULT_PATH" ]; then
    if bash "$IOS_DIR/scripts/check_coverage.sh" "$XCRESULT_PATH" 2>&1; then
        pass
    else
        fail
    fi
else
    skip "xcresult 없음 — 테스트 단계 실패로 건너뜀"
fi

# ── 4. 테스트 코드 비율 ───────────────────────────────
step "테스트 코드 비율 검사 (기준: 50%)"
if bash "$IOS_DIR/scripts/check_test_ratio.sh" 2>&1; then
    pass
else
    fail
fi

# ── 5. 중복 코드 (jscpd) ─────────────────────────────
step "중복 코드 검사 (jscpd)"
if command -v jscpd &>/dev/null; then
    NODE_MAJOR=$(node -e "process.stdout.write(process.version.split('.')[0].replace('v',''))" 2>/dev/null)
    if [ -z "$NODE_MAJOR" ] || [ "$NODE_MAJOR" -lt 14 ]; then
        skip "Node.js 14+ 필요 (현재: $(node --version 2>/dev/null || echo '미설치'))"
    elif jscpd --config "$IOS_DIR/.jscpd.json" 2>&1; then
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
