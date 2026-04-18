#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

echo "### Resetting CariesGuard Competition Environment ###"
echo "This will stop the containers and remove all associated data volumes."

docker compose --env-file env/competition.env down -v --remove-orphans

echo "### Pruning demo state ###"
if [ -d "evidence/generated/demo_assets" ]; then
    rm -rf "evidence/generated/demo_assets"
    echo "Cleared generated demo assets."
fi

echo ""
echo "=================================================="
echo "    Competition Environment Successfully Reset    "
echo "=================================================="
