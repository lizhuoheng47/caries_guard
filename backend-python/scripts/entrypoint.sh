#!/bin/sh
# CariesGuard Python AI service entrypoint.
# Runs Alembic migrations before starting the API and message consumer.
set -e

if [ "${CG_DB_MIGRATION_ENABLED:-true}" = "true" ]; then
  echo "[entrypoint] Alembic migration enabled; checking database state"

  # Probe with PyMySQL, which is already part of the runtime image.
  ALEMBIC_STATE=$(python -c "
import os
import pymysql

try:
    connection = pymysql.connect(
        host=os.getenv('CG_MYSQL_HOST', 'mysql'),
        port=int(os.getenv('CG_MYSQL_PORT', '3306')),
        user=os.getenv('CG_MYSQL_USERNAME', 'root'),
        password=os.getenv('CG_MYSQL_PASSWORD', '1234'),
        database=os.getenv('CG_MYSQL_DATABASE', 'caries_ai'),
        connect_timeout=10,
    )
    with connection.cursor() as cursor:
        cursor.execute(\"SHOW TABLES LIKE 'alembic_version'\")
        has_alembic = cursor.fetchone() is not None
        cursor.execute(\"SHOW TABLES LIKE 'ai_infer_job'\")
        has_business_tables = cursor.fetchone() is not None
    connection.close()
    if has_alembic:
        print('HAS_ALEMBIC')
    elif has_business_tables:
        print('STAMP_NEEDED')
    else:
        print('FRESH')
except Exception as exc:
    print('ERROR:' + str(exc), file=__import__('sys').stderr)
    print('FRESH')
")

  case "$ALEMBIC_STATE" in
    HAS_ALEMBIC)
      echo "[entrypoint] Migration history found; upgrading to head"
      alembic upgrade head
      ;;
    STAMP_NEEDED)
      echo "[entrypoint] Existing schema found without migration history; stamping baseline 0001"
      alembic stamp 0001
      alembic upgrade head
      ;;
    FRESH)
      echo "[entrypoint] Fresh database; creating schema through Alembic"
      alembic upgrade head
      ;;
    *)
      echo "[entrypoint] WARNING: unexpected probe result: $ALEMBIC_STATE; attempting upgrade"
      alembic upgrade head
      ;;
  esac

  echo "[entrypoint] Alembic migration complete"
else
  echo "[entrypoint] Alembic migration disabled; skipping"
fi

exec python -m app.main
