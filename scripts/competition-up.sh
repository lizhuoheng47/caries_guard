#!/usr/bin/env bash
set -e

# Change to the directory of the script, then go up to root
cd "$(dirname "$0")/.."

echo "### Starting CariesGuard Competition/Demo Environment ###"
docker compose --env-file env/competition.env up -d --build

echo "Waiting for services to start up..."
sleep 5

echo "### Waiting for Java Backend ###"
until curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; do
  sleep 2
  echo -n "."
done
echo " Java Backend is UP!"

echo "### Waiting for Python Backend ###"
until curl -s http://localhost:8001/ai/v1/health | grep -q '"code":200'; do
  sleep 2
  echo -n "."
done
echo " Python Backend is UP!"

echo "### Setting up Demo Data (SQL) ###"
CONTAINER_ID=$(docker compose ps -q mysql)
if [ -n "$CONTAINER_ID" ]; then
    echo "Injecting demo seed data into MySQL..."
    docker exec -i "$CONTAINER_ID" mysql -u root -p123456 smbms < scripts/seed_demo_data.sql
    if [ $? -eq 0 ]; then
        echo "Demo data injected successfully."
    else
        echo "Failed to inject demo data via mysql CLI."
    fi
else
    echo "Could not locate running MySQL container."
fi

echo "### Seeding Demo Assets ###"
bash scripts/seed_demo_assets.sh

echo "### Importing Knowledge Base ###"
if command -v python3 >/dev/null 2>&1; then
    python3 scripts/seed_demo_knowledge.py
elif command -v python >/dev/null 2>&1; then
    python scripts/seed_demo_knowledge.py
else
    echo "Python not found, skipping knowledge base seeding."
fi

echo ""
echo "=================================================="
echo "    Competition Environment Successfully Started  "
echo "=================================================="
echo "Access URL          : http://localhost:8080"
echo "API Docs (Java)     : http://localhost:8080/swagger-ui.html"
echo "API Docs (Python)   : http://localhost:8001/docs"
echo "RabbitMQ Management : http://localhost:15672 (guest/guest)"
echo "MinIO Console       : http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "Demo Summary:"
echo " - Competition Mode: ENABLED (skips complex real-world checks)"
echo " - AI Runtime Mode: MOCK (fast, offline-friendly predictable outputs)"
echo " - Knowledge Base: v1.0 (local loaded)"
echo "=================================================="
