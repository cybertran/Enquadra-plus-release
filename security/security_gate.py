#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
from pathlib import Path

REQUIRED = [
    Path("SECURITY.md"),
    Path("SECURITY_BASELINE.md"),
    Path("SECURITY_CHECKLIST.md"),
    Path("SECURITY_PROFILE.yml"),
    Path("CONTRIBUTING.md"),
]

FORBIDDEN_FILE_PATTERNS = [
    re.compile(r"(^|/)\.env($|\.)", re.IGNORECASE),
    re.compile(r"(^|/)(?:keystore|signing|key)\.properties$", re.IGNORECASE),
    re.compile(r"(^|/)google-services\.json$", re.IGNORECASE),
    re.compile(r"\.(?:jks|keystore|p12|pfx|pem|key)$", re.IGNORECASE),
]

SECRET_PATTERNS = [
    ("private-key", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")),
    ("github-token", re.compile(r"\bgh[pousr]_[A-Za-z0-9_]{20,}\b")),
    ("github-fine-grained-token", re.compile(r"\bgithub_pat_[A-Za-z0-9_]{20,}\b")),
    ("aws-access-key", re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("google-api-key", re.compile(r"\bAIza[0-9A-Za-z_-]{30,}\b")),
    ("slack-token", re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{10,}\b")),
    ("generic-password", re.compile(r"(?i)(?:password|passwd|pwd)\s*[:=]\s*['\"][^'\"\r\n]{8,}['\"]")),
    ("generic-secret", re.compile(r"(?i)(?:api[_-]?key|client[_-]?secret|access[_-]?token)\s*[:=]\s*['\"][^'\"\r\n]{12,}['\"]")),
]

TEXT_EXTENSIONS = {
    ".md", ".txt", ".yml", ".yaml", ".json", ".toml", ".ini", ".cfg",
    ".conf", ".properties", ".gradle", ".kts", ".kt", ".java", ".py",
    ".js", ".ts", ".tsx", ".jsx", ".html", ".xml", ".sh", ".ps1",
    ".bat", ".cmd",
}

EXCLUDED_DIRS = {
    ".git", "build", "dist", "bin", "obj", ".gradle", ".idea",
    ".venv", "node_modules", "vendor",
}

def tracked_files() -> list[Path]:
    raw = subprocess.check_output(["git", "ls-files", "-z"])
    return [Path(item.decode()) for item in raw.split(b"\0") if item]

errors: list[str] = []

for required in REQUIRED:
    if not required.is_file():
        errors.append(f"arquivo obrigatório ausente: {required}")

for path in tracked_files():
    if any(part in EXCLUDED_DIRS for part in path.parts):
        continue

    normalized = path.as_posix()
    low = normalized.lower()

    if any(pattern.search(normalized) for pattern in FORBIDDEN_FILE_PATTERNS):
        if ".example" not in low and ".sample" not in low:
            errors.append(f"arquivo sensível proibido no Git: {path}")

    if path.suffix.lower() not in TEXT_EXTENSIONS:
        continue

    try:
        data = path.read_text("utf-8", errors="ignore")
    except Exception:
        continue

    for label, pattern in SECRET_PATTERNS:
        if pattern.search(data):
            errors.append(f"possível segredo ({label}) em {path}")
            break

if errors:
    print("SECURITY_GATE=BLOCKED")
    for error in sorted(set(errors)):
        print("- " + error)
    raise SystemExit(1)

print("SECURITY_GATE=PASS")
print("Gate local aprovado; scanners especializados e revisão humana continuam obrigatórios.")
