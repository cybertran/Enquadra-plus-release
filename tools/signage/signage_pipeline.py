#!/usr/bin/env python3
"""Build a searchable, page-addressable index for the nine official signage manuals."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import re
import time
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import requests
from pypdf import PdfReader

ALLOWED_HOSTS = {"gov.br", "www.gov.br"}
ALLOWED_PREFIX = "/transportes/pt-br/assuntos/transito/"
MAX_BYTES = 80 * 1024 * 1024
TIMEOUT = 60
MAX_ATTEMPTS = 3
CODE_LINE_RE = re.compile(
    r"(?mi)^\s*([A-Z]{1,5}(?:-[A-Z]{1,3})?-\d{1,3}[a-z]?(?:\.\d+)?)"
    r"\s*(?:[-–—:]\s*)?(.{3,140})$"
)
SPACE_RE = re.compile(r"\s+")
TOC_LINE_RE = re.compile(
    r"^\s*(?P<section>(?:\d+(?:\.\d+){0,4}|SE[CÇ][AÃ]O\s+[IVXLCDM]+))"
    r"\s*[.\-–—]?\s*(?P<title>.+?)\s*\.{3,}\s*(?P<page>\d{1,3})\s*$",
    re.IGNORECASE,
)


def canonical_json(value: Any) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest().upper()


def normalize(value: str) -> str:
    return SPACE_RE.sub(" ", value.replace("\u00ad", "")).strip()


def validate_url(url: str) -> None:
    parsed = urlparse(url)
    if parsed.scheme.lower() != "https":
        raise ValueError("signage source must use HTTPS")
    if (parsed.hostname or "").lower() not in ALLOWED_HOSTS:
        raise ValueError("signage source host is not allowlisted")
    if parsed.port not in (None, 443):
        raise ValueError("signage source port is not allowlisted")
    if parsed.username or parsed.password or parsed.fragment:
        raise ValueError("invalid signage source URL")
    if not parsed.path.startswith(ALLOWED_PREFIX):
        raise ValueError("signage source path is not allowlisted")
    if not parsed.path.lower().endswith(".pdf"):
        raise ValueError("signage source must be a PDF")


def download_once(url: str) -> bytes:
    current = url
    data = bytearray()
    for redirect_index in range(4):
        validate_url(current)
        with requests.get(
            current,
            timeout=TIMEOUT,
            stream=True,
            allow_redirects=False,
            headers={"User-Agent": "EnquadraPlus-Signage/0.18"},
        ) as response:
            if 300 <= response.status_code < 400:
                if redirect_index >= 3:
                    raise ValueError("too many signage source redirects")
                location = response.headers.get("Location")
                if not location:
                    raise ValueError("signage redirect without destination")
                current = requests.compat.urljoin(current, location)
                validate_url(current)
                continue

            response.raise_for_status()
            declared = int(response.headers.get("Content-Length") or 0)
            if declared > MAX_BYTES:
                raise ValueError("signage PDF exceeds size limit")
            data = bytearray()
            for chunk in response.iter_content(64 * 1024):
                if not chunk:
                    continue
                data.extend(chunk)
                if len(data) > MAX_BYTES:
                    raise ValueError("signage PDF exceeds size limit")
            if declared > 0 and len(data) != declared:
                raise ValueError(
                    "signage PDF ended before declared content length"
                )
            if not bytes(data[:5]) == b"%PDF-":
                raise ValueError("signage source is not a valid PDF")
            return bytes(data)

    raise ValueError("signage redirect loop")


def download(url: str) -> bytes:
    validate_url(url)
    last_error: Exception | None = None
    for attempt in range(MAX_ATTEMPTS):
        try:
            return download_once(url)
        except (
            requests.exceptions.ConnectionError,
            requests.exceptions.Timeout,
            requests.exceptions.ChunkedEncodingError,
            ValueError,
        ) as exc:
            last_error = exc
            if attempt + 1 < MAX_ATTEMPTS:
                time.sleep(2 * (attempt + 1))
    raise RuntimeError(
        str(last_error) if last_error else "signage download failed"
    )

def flatten_outline(
    reader: PdfReader,
    items: list[Any],
    level: int = 0,
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for item in items:
        if isinstance(item, list):
            result.extend(flatten_outline(reader, item, level + 1))
            continue

        title = normalize(str(getattr(item, "title", item)))
        if not title or title.isdigit():
            continue
        try:
            page_index = reader.get_destination_page_number(item)
        except Exception:
            continue
        if page_index is None or page_index < 0:
            continue
        result.append(
            {
                "kind": "SECTION",
                "code": None,
                "title": title,
                "pageIndex": int(page_index),
                "level": level,
                "evidence": "Marcador interno do PDF oficial.",
            }
        )
    return result


def extract_page_texts(reader: PdfReader) -> list[str]:
    texts: list[str] = []
    for page in reader.pages:
        try:
            texts.append(page.extract_text() or "")
        except Exception:
            texts.append("")
    return texts


def coded_entries(page_texts: list[str]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for page_index, text in enumerate(page_texts):
        for match in CODE_LINE_RE.finditer(text):
            code = normalize(match.group(1))
            title = normalize(match.group(2))
            if len(title) < 3 or title.isdigit():
                continue
            lowered = title.lower()
            if any(
                noise in lowered
                for noise in (
                    "resolução",
                    "resolucao",
                    "figura ",
                    "tabela ",
                    "página ",
                    "pagina ",
                    "volume ",
                )
            ):
                continue
            result.append(
                {
                    "kind": "CODED_ITEM",
                    "code": code,
                    "title": title[:180],
                    "pageIndex": page_index,
                    "level": 0,
                    "evidence": "Código e título extraídos do texto do PDF oficial.",
                }
            )
    return result


def parse_toc_line(line: str) -> tuple[str, str, int] | None:
    match = TOC_LINE_RE.match(normalize(line))
    if not match:
        return None
    section = normalize(match.group("section"))
    title = normalize(match.group("title")).strip(" .-–—")
    manual_page = int(match.group("page"))
    if not title or manual_page <= 0:
        return None
    return section, title, manual_page


def normalized_words(value: str) -> list[str]:
    cleaned = re.sub(r"[^0-9A-Za-zÀ-ÿ]+", " ", value).lower()
    return [
        token
        for token in cleaned.split()
        if len(token) >= 3 and token not in {"para", "com", "das", "dos", "uma"}
    ]


def resolve_toc_page(
    reader: PdfReader,
    page_texts: list[str],
    manual_page: int,
    title: str,
) -> int:
    labels = list(getattr(reader, "page_labels", []) or [])
    for index, label in enumerate(labels):
        if str(label).strip() == str(manual_page):
            return index

    guess = max(0, min(len(page_texts) - 1, manual_page - 1))
    words = normalized_words(title)[:8]
    best_index = guess
    best_score = -1

    for index in range(max(0, guess - 5), min(len(page_texts), guess + 10)):
        haystack = normalize(page_texts[index]).lower()
        score = sum(1 for word in words if word in haystack)
        if score > best_score:
            best_score = score
            best_index = index

    minimum = 1 if len(words) <= 2 else min(3, len(words))
    return best_index if best_score >= minimum else guess


def toc_entries(
    reader: PdfReader,
    page_texts: list[str],
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for text in page_texts[: min(14, len(page_texts))]:
        for line in text.splitlines():
            parsed = parse_toc_line(line)
            if parsed is None:
                continue
            section, title, manual_page = parsed
            result.append(
                {
                    "kind": "SECTION",
                    "code": section,
                    "title": title[:180],
                    "pageIndex": resolve_toc_page(
                        reader,
                        page_texts,
                        manual_page,
                        title,
                    ),
                    "manualPage": manual_page,
                    "level": section.count("."),
                    "evidence": "Entrada extraída do sumário oficial do PDF.",
                }
            )
    return result


def dedupe(entries: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seen: set[tuple[str, str]] = set()
    output: list[dict[str, Any]] = []
    for entry in entries:
        code = normalize(str(entry.get("code") or "")).lower()
        title = normalize(str(entry.get("title") or "")).lower()
        key = (code, title)
        if not title or key in seen:
            continue
        seen.add(key)
        output.append(entry)
    return output


def seed_volume_one(seed: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        {
            "kind": "SIGN",
            "code": item["code"],
            "title": item["title"],
            "pageIndex": int(item["pageIndex"]),
            "manualPage": item.get("manualPage"),
            "level": 0,
            "evidence": "Entrada auditada do índice/tabela oficial do Volume I.",
        }
        for item in seed["entries"]
    ]


def build(args: argparse.Namespace) -> int:
    manifest = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    seed = json.loads(Path(args.regulatory_seed).read_text(encoding="utf-8"))

    volume_rows: list[dict[str, Any]] = []
    records: list[dict[str, Any]] = []

    for volume in manifest["volumes"]:
        pdf = download(volume["url"])
        reader = PdfReader(io.BytesIO(pdf), strict=False)

        if volume["volume"] == "I":
            entries = seed_volume_one(seed)
        else:
            page_texts = extract_page_texts(reader)
            outline = flatten_outline(reader, list(reader.outline or []))
            codes = coded_entries(page_texts)
            contents = toc_entries(reader, page_texts)
            entries = dedupe(codes + contents + outline)

        for index, entry in enumerate(entries):
            records.append(
                {
                    "id": (
                        "mbst-" + volume["volume"].lower() + "-" +
                        (entry.get("code") or "section")
                            .lower()
                            .replace(" ", "-")
                            .replace("/", "-") +
                        "-p" + str(int(entry["pageIndex"]) + 1) +
                        "-" + str(index + 1)
                    ),
                    "volume": volume["volume"],
                    "volumeTitle": volume["title"],
                    "kind": entry["kind"],
                    "code": entry.get("code"),
                    "title": entry["title"],
                    "pageIndex": entry["pageIndex"],
                    "manualPage": entry.get("manualPage"),
                    "level": entry.get("level", 0),
                    "evidence": entry["evidence"],
                    "officialUrl": volume["url"],
                }
            )

        volume_rows.append(
            {
                "volume": volume["volume"],
                "title": volume["title"],
                "officialUrl": volume["url"],
                "sourceSha256": sha256(pdf),
                "pageCount": len(reader.pages),
                "entryCount": len(entries),
            }
        )

    core = {
        "generatedAt": manifest["lastCheckedAt"],
        "verifiedAt": manifest["lastCheckedAt"],
        "sourceDocument": "Manuais Brasileiros de Sinalização de Trânsito",
        "sourcePageUrl": (
            "https://www.gov.br/transportes/pt-br/assuntos/transito/"
            "senatran/manuais-brasileiros-de-sinalizacao-de-transito"
        ),
        "volumeCount": len(volume_rows),
        "entryCount": len(records),
        "volumes": volume_rows,
        "records": records,
    }
    digest = sha256(canonical_json(core))
    dataset = {
        **core,
        "contentSha256": digest,
        "version": (
            manifest["lastCheckedAt"] +
            "-signage-" +
            digest[:12].lower()
        ),
    }

    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.output).write_text(
        json.dumps(dataset, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    report = {
        "version": dataset["version"],
        "volumeCount": dataset["volumeCount"],
        "entryCount": dataset["entryCount"],
        "entriesByVolume": {
            row["volume"]: row["entryCount"] for row in volume_rows
        },
        "pagesByVolume": {
            row["volume"]: row["pageCount"] for row in volume_rows
        },
    }
    Path(args.report).parent.mkdir(parents=True, exist_ok=True)
    Path(args.report).write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return validate_dataset(dataset, args.min_entries_per_volume)


def validate_dataset(
    dataset: dict[str, Any],
    min_entries_per_volume: int,
) -> int:
    errors: list[str] = []
    if dataset["volumeCount"] != 9:
        errors.append("expected exactly nine signage volumes")
    if dataset["entryCount"] != len(dataset["records"]):
        errors.append("declared signage entry count mismatch")

    volume_counts = {
        item["volume"]: item["entryCount"] for item in dataset["volumes"]
    }
    if set(volume_counts) != {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"}:
        errors.append("missing signage volume")

    if volume_counts.get("I") != 57:
        errors.append("Volume I must preserve 57 audited regulatory signs")

    for volume, count in volume_counts.items():
        if volume != "I" and count < min_entries_per_volume:
            errors.append(
                f"Volume {volume} has only {count} indexed entries "
                f"(minimum {min_entries_per_volume})"
            )

    ids = [item["id"] for item in dataset["records"]]
    if len(ids) != len(set(ids)):
        errors.append("duplicate signage entry ids")

    for item in dataset["records"]:
        if item["pageIndex"] < 0:
            errors.append("negative signage PDF page index")
        validate_url(item["officialUrl"])

    core = {
        key: dataset[key]
        for key in (
            "generatedAt",
            "verifiedAt",
            "sourceDocument",
            "sourcePageUrl",
            "volumeCount",
            "entryCount",
            "volumes",
            "records",
        )
    }
    expected_hash = sha256(canonical_json(core))
    if expected_hash != dataset["contentSha256"]:
        errors.append("signage canonical hash mismatch")
    expected_version = (
        dataset["verifiedAt"] +
        "-signage-" +
        expected_hash[:12].lower()
    )
    if expected_version != dataset["version"]:
        errors.append("signage version/hash mismatch")

    if errors:
        for error in errors:
            print("ERROR:", error)
        return 1
    return 0


def validate(args: argparse.Namespace) -> int:
    dataset = json.loads(Path(args.output).read_text(encoding="utf-8"))
    result = validate_dataset(dataset, args.min_entries_per_volume)
    if result == 0:
        print(
            f"Validated {dataset['entryCount']} signage entries "
            f"across {dataset['volumeCount']} official volumes."
        )
    return result


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    sub = root.add_subparsers(dest="command", required=True)
    for name in ("build", "validate"):
        cmd = sub.add_parser(name)
        cmd.add_argument("--manifest", required=True)
        cmd.add_argument("--regulatory-seed", required=True)
        cmd.add_argument("--output", required=True)
        cmd.add_argument("--report", required=True)
        cmd.add_argument("--min-entries-per-volume", type=int, default=8)
    return root


def main() -> int:
    args = parser().parse_args()
    if args.command == "build":
        return build(args)
    return validate(args)


if __name__ == "__main__":
    raise SystemExit(main())
