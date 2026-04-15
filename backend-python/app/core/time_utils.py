from datetime import datetime, timezone


def local_naive_iso_now() -> str:
    return datetime.now(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()

