#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

echo "### Resetting CariesGuard Competition Environment ###"
echo "This will stop the containers and remove all associated data volumes."

docker compose --env-file env/competition.env down -v --remove-orphans

echo "### Pruning demo state (TODO) ###"
# TODO: Remove any residual local cache data if stored outside volumes.
echo "- Pruning pending implementation."

echo ""
echo "=================================================="
echo "    Competition Environment Successfully Reset    "
echo "=================================================="
