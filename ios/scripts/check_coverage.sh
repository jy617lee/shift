#!/bin/bash
# iOS 커버리지 검증
# Usage: check_coverage.sh <path-to.xcresult> [min_pct]
# SwiftUI View 파일(*View.swift, *App.swift)은 선언형 UI로 단위 테스트 불가 → 커버리지 계산 제외
# 기준: 비즈니스 로직(Domain/Data/OCR/ViewModel) 30%

set -e

XCRESULT="$1"
MIN_PCT="${2:-30}"

if [ -z "$XCRESULT" ] || [ ! -e "$XCRESULT" ]; then
    echo "❌ xcresult 번들을 찾을 수 없음: $XCRESULT"
    exit 1
fi

python3 - "$XCRESULT" "$MIN_PCT" <<'EOF'
import json, subprocess, sys

xcresult = sys.argv[1]
threshold = int(sys.argv[2])

result = subprocess.run(
    ["xcrun", "xccov", "view", "--report", "--json", xcresult],
    capture_output=True, text=True, check=True
)
data = json.loads(result.stdout)

targets = data.get("targets", [])
app_target = next((t for t in targets if t["name"].endswith(".app")), None)

if not app_target:
    print("⚠️  앱 타겟 없음 — 커버리지 검사 건너뜀")
    sys.exit(0)

# SwiftUI 선언형 UI 파일은 단위 테스트 불가 — 커버리지 계산에서 제외
EXCLUDED_SUFFIXES = ("View.swift", "App.swift")

files = app_target.get("files", [])
included = [
    f for f in files
    if not any(f.get("path", "").endswith(s) for s in EXCLUDED_SUFFIXES)
]

if included:
    total_lines = sum(f.get("executableLines", 0) for f in included)
    covered_lines = sum(f.get("coveredLines", 0) for f in included)
    excluded_count = len(files) - len(included)
    print(f"View 파일 {excluded_count}개 제외 (선언형 UI)")
else:
    total_lines = app_target.get("executableLines", 0)
    covered_lines = app_target.get("coveredLines", 0)

coverage_pct = (covered_lines / total_lines * 100) if total_lines > 0 else 0

print(f"커버리지: {covered_lines}/{total_lines} lines = {coverage_pct:.1f}%")

if total_lines < 300:
    print(f"프로덕션 코드 {total_lines}줄 미만 — 커버리지 검사 건너뜀")
    sys.exit(0)

if coverage_pct < threshold:
    print(f"❌ 커버리지 부족 ({coverage_pct:.1f}% < {threshold}%)")
    sys.exit(1)

print(f"✅ 커버리지 통과 ({coverage_pct:.1f}% >= {threshold}%)")
EOF
