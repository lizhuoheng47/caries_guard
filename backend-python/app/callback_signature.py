import base64
import hashlib
import hmac


def sign_callback(raw_body: str, timestamp: str, secret: str) -> str:
    payload = f"{timestamp}.{raw_body}".encode("utf-8")
    digest = hmac.new(secret.encode("utf-8"), payload, hashlib.sha256).digest()
    return base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")