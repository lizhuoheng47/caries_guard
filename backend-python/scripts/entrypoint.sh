#!/bin/sh
# CariesGuard Python AI service entrypoint
# Phase 4 Step 8 — Docker entrypoint with Alembic migration support
#
# Behaviour:
#   CG_DB_MIGRATION_ENABLED=true (default):
#     1. If alembic_version table does NOT exist but business tables DO exist
#        → stamp baseline (do NOT run DDL, tables already present)
#     2. Otherwise → alembic upgrade head (creates tables if fresh, or migrates)
#   CG_DB_MIGRATION_ENABLED=false:
#     → skip migration, go straight to app startup
set -e

if [ "${CG_DB_MIGRATION_ENABLED:-true}" = "true" ]; then
  echo "[entrypoint] Alembic migration enabled — checking database state…"

  # Build a minimal MySQL connection string for probing.
  # We use Python because the image already has PyMySQL; no extra deps needed.
  ALEMBIC_EXISTS=$(python -c "
import pymysql, os, sys
try:
    conn = pymysql.connect(
        host=os.getenv('CG_MYSQL_HOST', 'mysql'),
        port=int(os.getenv('CG_MYSQL_PORT', '3306')),
        user=os.getenv('CG_MYSQL_USERNAME', 'root'),
        password=os.getenv('CG_MYSQL_PASSWORD', '1234'),
        database=os.getenv('CG_MYSQL_DATABASE', 'caries_ai'),
        connect_timeout=10,
    )
    with conn.cursor() as cur:
        cur.execute(\"SHOW TABLES LIKE 'alembic_version'\")
        has_alembic = cur.fetchone() is not None
        cur.execute(\"SHOW TABLES LIKE 'ai_infer_job'\")
        has_biz = cur.fetchone() is not None
    conn.close()
    if has_alembic:
        print('HAS_ALEMBIC')
    elif has_biz:
        print('STAMP_NEEDED')
    else:
        print('FRESH')
except Exception as e:
    print('ERROR:' + str(e), file=sys.stderr)
    print('FRESH')
")

  case "$ALEMBIC_EXISTS" in
    HAS_ALEMBIC)
      echo "[entrypoint] alembic_version exists — running alembic upgrade head"
      alembic upgrade head
      ;;
    STAMP_NEEDED)
      echo "[entrypoint] Business tables exist but no alembic_version — stamping baseline 0001"
      alembic stamp 0001
      echo "[entrypoint] Stamp complete — running alembic upgrade head for any pending migrations"
      alembic upgrade head
      ;;
    FRESH)
      echo "[entrypoint] Fresh database — running alembic upgrade head (will create all tables)"
      alembic upgrade head
      ;;
    *)
      echo "[entrypoint] WARNING: unexpected probe result: $ALEMBIC_EXISTS — attempting upgrade head"
      alembic upgrade head
      ;;
  esac

  echo "[entrypoint] Alembic migration complete."
else
  echo "[entrypoint] Alembic migration disabled (CG_DB_MIGRATION_ENABLED=false) — skipping."
fi

exec python -m app.main
