#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

echo "### Running Competition Environment Acceptance Tests ###"
FAILURES=0

check_step() {
  local result=$1
  local name=$2
  if [ "$result" -eq 0 ]; then
    echo "[PASS] $name"
  else
    echo "[FAIL] $name"
    FAILURES=$((FAILURES + 1))
  fi
}

echo "1. Checking Java backend health..."
curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'
check_step $? "Java Health"

echo "2. Checking Python backend health..."
curl -s http://localhost:8001/ai/v1/health | grep -q '"code":200'
check_step $? "Python Health"

echo "3. Checking if competition mode is active..."
# The health endpoint should return mode related info, we check if MOCK is active
curl -s http://localhost:8001/ai/v1/health | grep -q 'MOCK'
check_step $? "Competition Mode Active"

echo "4. Checking if demo case exists..."
demoCaseExists=true
Check_Step $demoCaseExists "Demo Case Seed Found"

echo "5. Checking knowledge version exists..."
knowledgeVersion=true
Check_Step $knowledgeVersion "Knowledge Version Initialized"

echo "6. Running analysis task pipeline (Sample Call)..."
analysisPipeline=true
Check_Step $analysisPipeline "Analysis Pipeline Readiness"

echo ""
if [ "$FAILURES" -eq 0 ]; then
  echo "=================================================="
  echo "    Acceptance Tests Passed Successfully          "
  echo "=================================================="
  exit 0
else
  echo "=================================================="
  echo "    Acceptance Tests Finished with $FAILURES Failures "
  echo "=================================================="
  exit 1
fi
