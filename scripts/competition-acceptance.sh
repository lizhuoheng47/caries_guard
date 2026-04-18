#!/bin/bash
set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo -e "\033[36m### Running Competition Environment Acceptance Tests ###\033[0m"
FAILURES=0

check_step() {
    local result=$1
    local name=$2
    local details=$3
    if [ "$result" = "true" ]; then
        echo -e "\033[32m[PASS] $name\033[0m"
        if [ -n "$details" ]; then echo -e "       \033[90m$details\033[0m"; fi
    else
        echo -e "\033[31m[FAIL] $name\033[0m"
        if [ -n "$details" ]; then echo -e "       \033[31m$details\033[0m"; fi
        ((FAILURES++))
    fi
}

echo "1. Checking Evidence Directories..."
DIRS_EXIST="true"
for d in "evidence/demo-screenshots" "evidence/payloads" "evidence/metrics" "evidence/qa-cases" "evidence/review-cases"; do
    if [ ! -d "$d" ]; then
        DIRS_EXIST="false"
        break
    fi
done
check_step "$DIRS_EXIST" "Evidence Directories Created"

echo "2. Checking Java backend API health..."
JAVA_HEALTH="false"
if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    JAVA_HEALTH="true"
fi
check_step "$JAVA_HEALTH" "Java Health"

echo "3. Querying AI Governance Dashboard Metrics..."
DASHBOARD_JSON=$(curl -s -H "Authorization: Bearer competition-admin-token" http://localhost:8080/api/v1/dashboard/model-runtime || true)

if [[ -n "$DASHBOARD_JSON" && "$DASHBOARD_JSON" != *"Could not connect"* && "$DASHBOARD_JSON" == *"{"* ]]; then
    echo "4. Semantic Validation of AI Governance Metrics..."
    
    # Simple semantic validation using grep/awk or assuming jq if available
    if command -v jq &> /dev/null; then
        RATES_VALID=$(echo "$DASHBOARD_JSON" | jq 'if .data.callbackSuccessRate >= 0 and .data.callbackSuccessRate <= 1 then "true" else "false" end' -r)
        check_step "$RATES_VALID" "Metrics Semantic Range Check [0, 1]" "Rates are within expected bounds"

        NUM_DENOM_VALID=$(echo "$DASHBOARD_JSON" | jq 'if .data.callbackSuccessCount <= .data.callbackTotalCount and .data.visualAssetGeneratedCount <= .data.visualAssetExpectedCount then "true" else "false" end' -r)
        check_step "$NUM_DENOM_VALID" "Numerator/Denominator Logical Check" "Numerators do not exceed denominators"
        
        CONFIG_VALID=$(echo "$DASHBOARD_JSON" | jq 'if .data.llmModelName != "UNKNOWN" and .data.knowledgeVersion != "UNKNOWN" then "true" else "false" end' -r)
        check_step "$CONFIG_VALID" "Runtime Configuration Check" "LLM and Knowledge Version successfully sourced from mdl_model_version"
    else
        echo -e "\033[33m'jq' not installed. Skipping JSON semantic parsing.\033[0m"
    fi
else
    echo -e "\033[33mSkipping API semantic checks due to API unavailability.\033[0m"
fi

echo ""
if [ "$FAILURES" -eq 0 ]; then
    echo -e "\033[32m==================================================\033[0m"
    echo -e "\033[32m    Acceptance Tests Passed Successfully          \033[0m"
    echo -e "\033[32m==================================================\033[0m"
    exit 0
else
    echo -e "\033[31m==================================================\033[0m"
    echo -e "\033[31m    Acceptance Tests Finished with $FAILURES Failures \033[0m"
    echo -e "\033[31m==================================================\033[0m"
    exit 1
fi
