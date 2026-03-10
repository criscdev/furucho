#!/usr/bin/env bash
# =============================================================================
# Furucho — Build & Quality Gate
# =============================================================================
# Single script that validates every layer before code leaves the branch.
# Designed to run locally pre-commit OR in CI — same checks, same exit codes.
#
# Usage:
#   ./scripts/check.sh              # full gate (all checks)
#   ./scripts/check.sh quick        # fast: conflicts + typecheck + unit tests
#   ./scripts/check.sh conflicts    # merge conflict markers only
#   ./scripts/check.sh status       # git health summary
#   ./scripts/check.sh backend      # Java: compile + unit tests
#   ./scripts/check.sh frontend     # TS: typecheck + vitest
#   ./scripts/check.sh arch         # architecture & hygiene
#   ./scripts/check.sh a11y         # accessibility (axe via Playwright)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# ─── Output helpers ───────────────────────────────────────────────────────────
RED='\033[0;31m'  GREEN='\033[0;32m'  YELLOW='\033[1;33m'
CYAN='\033[0;36m' BOLD='\033[1m'      NC='\033[0m'

pass()   { echo -e "  ${GREEN}✔${NC} $1"; }
fail()   { echo -e "  ${RED}✘${NC} $1"; }
warn()   { echo -e "  ${YELLOW}⚠${NC} $1"; }
info()   { echo -e "  ${CYAN}→${NC} $1"; }
header() { echo -e "\n${BOLD}━━━ $1 ━━━${NC}"; }

FAILURES=0
record_fail() { ((FAILURES++)); }

# =============================================================================
# 1. GIT HEALTH — estado do repositório antes de qualquer validação
# =============================================================================
check_status() {
  header "Git Health"
  echo -e "  ${BOLD}Branch:${NC} $(git branch --show-current)"

  # Merge state
  if [ -f .git/MERGE_HEAD ]; then
    warn "Merge in progress — finish or abort before committing"
  elif [ -f .git/REBASE_HEAD ]; then
    warn "Rebase in progress"
  else
    pass "Working tree state: clean"
  fi

  # Counts
  local staged modified untracked
  staged=$(git diff --cached --name-only 2>/dev/null | wc -l)
  modified=$(git diff --name-only 2>/dev/null | wc -l)
  untracked=$(git ls-files --others --exclude-standard 2>/dev/null | wc -l)
  echo -e "  ${BOLD}Staged:${NC} $staged  ${BOLD}Modified:${NC} $modified  ${BOLD}Untracked:${NC} $untracked"

  # Stash reminder
  local stash_count
  stash_count=$(git stash list 2>/dev/null | wc -l)
  if [ "$stash_count" -gt 0 ]; then
    warn "$stash_count stash(es) — remember to pop or drop"
  fi
}

# =============================================================================
# 2. CONFLICT MARKERS — merge residue that must never reach a commit
# =============================================================================
check_conflicts() {
  header "Conflict Markers"
  local conflicts
  conflicts=$(grep -rn '<<<<<<<\|>>>>>>>' \
    --include='*.java' --include='*.tsx' --include='*.ts' \
    --include='*.properties' --include='*.xml' --include='*.json' \
    --include='*.css' --include='*.md' \
    --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.git \
    --exclude-dir=coverage --exclude-dir=playwright-report \
    --exclude-dir=test-results --exclude='package-lock.json' \
    . 2>/dev/null || true)

  if [ -z "$conflicts" ]; then
    pass "No conflict markers"
  else
    fail "Conflict markers found:"; record_fail
    echo "$conflicts" | head -20
    local count
    count=$(echo "$conflicts" | wc -l)
    [ "$count" -gt 20 ] && warn "... and $((count - 20)) more"
  fi
}

# =============================================================================
# 3. ARCHITECTURE & HYGIENE — structural rules that prevent drift
# =============================================================================
check_arch() {
  header "Architecture & Hygiene"

  # 3a. Build artifacts must never be versioned
  local tracked_artifacts
  tracked_artifacts=$(git ls-files 'backend/target/' 2>/dev/null | head -5)
  if [ -n "$tracked_artifacts" ]; then
    fail "Build artifacts tracked in git (backend/target/)"; record_fail
  else
    pass "No build artifacts in git"
  fi

  # 3b. .gitignore must have target/ rule
  if grep -q '^target/' backend/.gitignore 2>/dev/null; then
    pass "backend/.gitignore blocks target/"
  else
    warn "backend/.gitignore missing 'target/' rule"
  fi

  # 3c. No @SuppressWarnings("null") on individual methods (class-level only)
  local method_suppress
  method_suppress=$(grep -rn '@SuppressWarnings("null")' \
    --include='*.java' backend/src/ 2>/dev/null \
    | grep -v "^.*class " | grep -v "^[^:]*:[0-9]*:@SuppressWarnings" || true)
  # Heuristic: if the line after @Suppress is not a class declaration, it's method-level
  local inline_count=0
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    local file linenum
    file=$(echo "$line" | cut -d: -f1)
    linenum=$(echo "$line" | cut -d: -f2)
    local nextline
    nextline=$(sed -n "$((linenum+1))p" "$file" 2>/dev/null || true)
    if ! echo "$nextline" | grep -qE '^\s*(public\s+)?class\s'; then
      ((inline_count++))
    fi
  done <<< "$(grep -rn '@SuppressWarnings("null")' --include='*.java' backend/src/ 2>/dev/null || true)"
  if [ "$inline_count" -eq 0 ]; then
    pass "No method-level @SuppressWarnings(\"null\") — class-level only"
  else
    warn "$inline_count method-level @SuppressWarnings(\"null\") found (prefer class-level)"
  fi

  # 3d. Production code must not suppress — should use Objects.requireNonNull
  local prod_suppress
  prod_suppress=$(grep -rn '@SuppressWarnings("null")' \
    --include='*.java' backend/src/main/ 2>/dev/null || true)
  if [ -z "$prod_suppress" ]; then
    pass "No @SuppressWarnings in production code"
  else
    fail "@SuppressWarnings(\"null\") in production code (use Objects.requireNonNull)"; record_fail
    echo "$prod_suppress" | head -5
  fi

  # 3e. No System.out/err in prod code (use logging)
  local sysout
  sysout=$(grep -rn 'System\.\(out\|err\)\.print' \
    --include='*.java' backend/src/main/ 2>/dev/null || true)
  if [ -z "$sysout" ]; then
    pass "No System.out/err in production code"
  else
    warn "System.out/err found — use SLF4J logger instead"
    echo "$sysout" | head -5
  fi

  # 3f. Controller layer must not import Repository directly (SRP)
  local controller_repo
  controller_repo=$(grep -rn 'import.*Repository' \
    --include='*Controller.java' backend/src/main/ 2>/dev/null || true)
  if [ -z "$controller_repo" ]; then
    pass "Controllers don't import repositories (SRP clean)"
  else
    fail "Controller imports Repository directly — route through Service"; record_fail
    echo "$controller_repo" | head -5
  fi

  # 3g. No hardcoded secrets in properties
  local hardcoded_secrets
  hardcoded_secrets=$(grep -rn 'password=.\|secret=.\|token=.' \
    --include='*.properties' backend/src/main/resources/ 2>/dev/null \
    | grep -v '=${' | grep -v '=\s*$' | grep -v 'CHANGE_THIS' || true)
  if [ -z "$hardcoded_secrets" ]; then
    pass "No hardcoded secrets in properties"
  else
    fail "Possible hardcoded secrets in properties"; record_fail
    echo "$hardcoded_secrets" | head -5
  fi

  # 3h. Unused imports check (basic heuristic for Java)
  local unused_imports=0
  while IFS= read -r jfile; do
    local imports
    imports=$(grep '^import ' "$jfile" 2>/dev/null | sed 's/import \(static \)\?//' | sed 's/;$//' || true)
    while IFS= read -r imp; do
      [ -z "$imp" ] && continue
      local classname
      classname=$(echo "$imp" | awk -F'.' '{print $NF}')
      [ "$classname" = "*" ] && continue
      # Check if the classname appears outside import lines
      if ! grep -q "$classname" <(grep -v '^import ' "$jfile") 2>/dev/null; then
        ((unused_imports++))
      fi
    done <<< "$imports"
  done < <(find backend/src/main -name '*.java' 2>/dev/null)
  if [ "$unused_imports" -eq 0 ]; then
    pass "No obvious unused imports in production code"
  else
    warn "$unused_imports potentially unused import(s) in production code"
  fi
}

# =============================================================================
# 4. BACKEND — compile + unit tests (Maven)
# =============================================================================
check_backend() {
  header "Backend (Maven)"
  if [ ! -f "$ROOT/backend/pom.xml" ]; then
    info "No backend/pom.xml — skipping"; return 0
  fi

  local output exit_code=0
  output=$(cd "$ROOT/backend" && mvn test -q 2>&1) || exit_code=$?

  local summary
  summary=$(echo "$output" | grep -E "^.*Tests run:" | tail -1)

  if [ $exit_code -eq 0 ]; then
    pass "BUILD SUCCESS — $summary"
  else
    fail "BUILD FAILURE"; record_fail
    echo "$output" | grep -E "^\[ERROR\]" \
      | grep -v "Re-run\|Help 1\|stack trace\|For more info\|full debug" \
      | head -15
    [ -n "$summary" ] && echo -e "  ${RED}$summary${NC}"
  fi
}

# =============================================================================
# 5. FRONTEND — typecheck + unit tests (tsc + Vitest)
# =============================================================================
check_frontend() {
  header "Frontend"
  if [ ! -f "$ROOT/package.json" ]; then
    info "No package.json — skipping"; return 0
  fi

  # 5a. TypeScript strict compilation
  info "TypeScript typecheck..."
  local ts_output ts_exit=0
  ts_output=$(cd "$ROOT" && npx tsc --noEmit 2>&1) || ts_exit=$?
  if [ $ts_exit -eq 0 ]; then
    pass "tsc --noEmit: zero errors"
  else
    fail "TypeScript errors"; record_fail
    echo "$ts_output" | head -15
  fi

  # 5b. Vitest unit tests
  info "Vitest unit tests..."
  local vt_output vt_exit=0
  vt_output=$(cd "$ROOT" && npx vitest run 2>&1) || vt_exit=$?
  local vt_summary
  vt_summary=$(echo "$vt_output" | grep -E "Tests\s+[0-9]" | tail -1)
  if [ $vt_exit -eq 0 ]; then
    pass "Vitest PASS — $vt_summary"
  else
    fail "Vitest FAIL"; record_fail
    echo "$vt_output" | grep -E "FAIL|Error|✗|×" | head -10
    [ -n "$vt_summary" ] && echo -e "  ${RED}$vt_summary${NC}"
  fi
}

# =============================================================================
# 6. ACCESSIBILITY — axe-core via Playwright (WCAG 2.2 AA)
# =============================================================================
check_a11y() {
  header "Accessibility (axe / Playwright)"
  if [ ! -f "$ROOT/e2e/a11y.spec.ts" ]; then
    info "No e2e/a11y.spec.ts — skipping"; return 0
  fi

  # Check if dev server is needed
  info "Running a11y spec..."
  local output exit_code=0
  output=$(cd "$ROOT" && npx playwright test e2e/a11y.spec.ts --reporter=line 2>&1) || exit_code=$?

  local passed failed
  passed=$(echo "$output" | grep -oE '[0-9]+ passed' | head -1 || echo "0 passed")
  failed=$(echo "$output" | grep -oE '[0-9]+ failed' | head -1 || echo "")

  if [ $exit_code -eq 0 ]; then
    pass "a11y: $passed"
  else
    fail "a11y: $passed ${failed:+/ $failed}"; record_fail
    echo "$output" | grep -E "✘|Error|fail|violation" | head -10
  fi
}

# =============================================================================
# MAIN — orchestration
# =============================================================================
main() {
  local cmd="${1:-all}"

  echo -e "\n${BOLD}🧸 Furucho Quality Gate — $(date '+%Y-%m-%d %H:%M:%S')${NC}"

  case "$cmd" in
    status)    check_status ;;
    conflicts) check_conflicts ;;
    arch)      check_arch ;;
    backend)   check_backend ;;
    frontend)  check_frontend ;;
    a11y)      check_a11y ;;
    quick)
      # Fast feedback loop: no e2e, no a11y
      check_conflicts
      check_frontend
      check_backend
      ;;
    all)
      check_status
      check_conflicts
      check_arch
      check_backend
      check_frontend
      check_a11y
      ;;
    *)
      echo "Usage: $0 [all|quick|status|conflicts|arch|backend|frontend|a11y]"
      exit 1
      ;;
  esac

  # ─── Summary ────────────────────────────────────────────────────────────────
  echo ""
  if [ "$FAILURES" -eq 0 ]; then
    echo -e "${GREEN}${BOLD}  ✔ All checks passed${NC}"
  else
    echo -e "${RED}${BOLD}  ✘ $FAILURES check(s) failed${NC}"
  fi
  exit "$FAILURES"
}

main "$@"
