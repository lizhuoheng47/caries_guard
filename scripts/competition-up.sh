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

echo "### Setting up Demo Data (TODO) ###"
# TODO: Import demo database dump, patients, files.
# Connect to mysql container or execute SQL scripts here.
echo "- Demo data setup pending implementation."

echo "### Importing Knowledge Base (TODO) ###"
# TODO: Inject vector database data or copy files to RAG directory.
echo "- Knowledge base import pending implementation."

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
