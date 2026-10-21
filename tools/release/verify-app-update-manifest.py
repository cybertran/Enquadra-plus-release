#!/usr/bin/env python3
import json
import re
from pathlib import Path
from urllib.parse import urlparse

MANIFEST = Path("app-update.json")
RELEASE_STATE = Path("RELEASE_STATE.yml")
OFFICIAL_CERT = "36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53"
REPO_PATH = "/cybertran/Enquadra-plus-release"


def fail(message: str) -> None:
    raise SystemExit(f"ERROR: {message}")


def state_version_code() -> int:
    raw = RELEASE_STATE.read_text(encoding="utf-8")
    match = re.search(r"^versionCode:\s*([0-9]+)\s*$", raw, re.MULTILINE)
    if not match:
        fail("versionCode missing from RELEASE_STATE.yml")
    return int(match.group(1))


def validate_url(value: str, expected_path: str) -> None:
    parsed = urlparse(value)
    if parsed.scheme != "https":
        fail("update URL must use HTTPS")
    if parsed.hostname != "github.com":
        fail("update URL must use github.com")
    if parsed.username or parsed.password or parsed.port:
        fail("update URL contains unsupported authority components")
    if parsed.query or parsed.fragment:
        fail("update URL cannot contain query or fragment")
    if parsed.path != expected_path:
        fail(f"unexpected update URL path: {parsed.path}")


def main() -> None:
    if not MANIFEST.is_file():
        fail("app-update.json not found")
    if MANIFEST.stat().st_size > 32 * 1024:
        fail("app-update.json exceeds 32 KiB")

    data = json.loads(MANIFEST.read_text(encoding="utf-8"))

    if data.get("schemaVersion") != 1:
        fail("unsupported schemaVersion")

    version_code = data.get("versionCode")
    if not isinstance(version_code, int) or version_code <= 0:
        fail("versionCode must be a positive integer")
    if version_code > state_version_code():
        fail("published app-update versionCode cannot exceed public source versionCode")

    version_name = data.get("versionName")
    if not isinstance(version_name, str) or not re.fullmatch(
        r"[0-9A-Za-z][0-9A-Za-z.+-]{0,63}", version_name
    ):
        fail("invalid versionName")

    validate_url(
        data.get("releaseUrl", ""),
        f"{REPO_PATH}/releases/tag/v{version_name}",
    )
    validate_url(
        data.get("apkUrl", ""),
        f"{REPO_PATH}/releases/download/v{version_name}/Enquadra.apk",
    )

    apk_sha = str(data.get("apkSha256", "")).upper()
    if not re.fullmatch(r"[0-9A-F]{64}", apk_sha):
        fail("invalid apkSha256")

    cert = str(data.get("signingCertificateSha256", "")).upper()
    if cert != OFFICIAL_CERT:
        fail("signing certificate fingerprint does not match official identity")

    print(
        f"App update manifest OK: {version_name} "
        f"(versionCode {version_code}, source ceiling {state_version_code()})"
    )


if __name__ == "__main__":
    main()
