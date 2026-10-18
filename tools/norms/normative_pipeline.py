#!/usr/bin/env python3
"""Build a searchable local catalog of official CONTRAN and SENATRAN acts."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import date
from html.parser import HTMLParser
from pathlib import Path
from typing import Any

ALLOWED_HOSTS = {"gov.br", "www.gov.br"}
ALLOWED_PATH_PREFIX = "/transportes/pt-br/assuntos/transito/"
MAX_SOURCE_BYTES = 8_000_000
SPACE_RE = re.compile(r"\s+")
NUMBER_RE = re.compile(r"(\d{1,4})")
SHORT_DATE_RE = re.compile(r"^(\d{1,2})[./](\d{1,2})[./](\d{2,4})$")


@dataclass(frozen=True)
class Cell:
    text: str
    href: str | None


@dataclass(frozen=True)
class SourcePage:
    agency: str
    act_type: str
    url: str
    year: int | None


class TableParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.rows: list[list[Cell]] = []
        self._row: list[Cell] | None = None
        self._cell_parts: list[str] | None = None
        self._cell_href: str | None = None

    def handle_starttag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        tag = tag.lower()
        if tag == "tr":
            self._row = []
        elif tag in {"td", "th"} and self._row is not None:
            self._cell_parts = []
            self._cell_href = None
        elif tag == "a" and self._cell_parts is not None and self._cell_href is None:
            for key, value in attrs:
                if key.lower() == "href" and value:
                    self._cell_href = value
                    break
        elif tag == "br" and self._cell_parts is not None:
            self._cell_parts.append(" ")

    def handle_data(self, data: str) -> None:
        if self._cell_parts is not None:
            self._cell_parts.append(data)

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag in {"td", "th"} and self._row is not None and self._cell_parts is not None:
            text = normalize_text("".join(self._cell_parts))
            self._row.append(Cell(text=text, href=self._cell_href))
            self._cell_parts = None
            self._cell_href = None
        elif tag == "tr" and self._row is not None:
            if self._row:
                self.rows.append(self._row)
            self._row = None


def normalize_text(value: str) -> str:
    return SPACE_RE.sub(" ", html.unescape(value).replace("\u00a0", " ")).strip()


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_value = "".join(ch for ch in normalized if not unicodedata.combining(ch))
    slug = re.sub(r"[^a-z0-9]+", "-", ascii_value.lower()).strip("-")
    return slug[:80]


def validate_source_url(url: str) -> None:
    parsed = urllib.parse.urlparse(url)
    host = (parsed.hostname or "").lower()
    if (
        parsed.scheme.lower() != "https"
        or host not in ALLOWED_HOSTS
        or parsed.port not in (None, 443)
        or not parsed.path.startswith(ALLOWED_PATH_PREFIX)
        or parsed.params
        or parsed.fragment
    ):
        raise ValueError(f"Unsupported official source URL: {url}")


def fetch_source(url: str, attempts: int = 5) -> tuple[str, str]:
    validate_source_url(url)
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": (
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/141.0 Safari/537.36 "
                "EnquadraPlus-Normative-Pipeline/1.0"
            ),
            "Accept": "text/html,application/xhtml+xml",
            "Accept-Language": "pt-BR,pt;q=0.9,en;q=0.7",
            "Connection": "close",
        },
    )

    last_error: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                validate_source_url(response.geturl())
                raw = response.read(MAX_SOURCE_BYTES + 1)
                if len(raw) > MAX_SOURCE_BYTES:
                    raise ValueError("Official source exceeds configured size limit.")
                if len(raw) < 5_000:
                    raise ValueError(
                        f"Official source is unexpectedly small: {len(raw)} bytes."
                    )
                digest = hashlib.sha256(raw).hexdigest().upper()
                charset = response.headers.get_content_charset() or "utf-8"
                try:
                    text = raw.decode(charset)
                except (LookupError, UnicodeDecodeError):
                    text = raw.decode("utf-8", errors="replace")
                return text, digest
        except (OSError, urllib.error.URLError, ValueError) as exc:
            last_error = exc
            if attempt == attempts:
                break
            time.sleep(min(2 ** (attempt - 1), 8))

    raise RuntimeError(
        f"Unable to download official source after {attempts} attempts: {last_error}"
    )


def parse_year(value: str) -> int | None:
    normalized = normalize_text(value)
    match = SHORT_DATE_RE.match(normalized)
    if match:
        year = int(match.group(3))
        if year >= 100:
            return year
        return 1900 + year if year >= 90 else 2000 + year

    long_year = re.search(r"\b(19\d{2}|20\d{2})\b", normalized)
    return int(long_year.group(1)) if long_year else None


def parse_number(value: str) -> int | None:
    match = NUMBER_RE.search(value)
    return int(match.group(1)) if match else None


def normalize_href(page_url: str, href: str | None) -> str:
    if not href:
        return page_url
    target = urllib.parse.urljoin(page_url, href)
    try:
        validate_source_url(target)
    except ValueError:
        return page_url
    return target


def unique_id(
    agency: str,
    act_type: str,
    year: int,
    number: int,
    number_label: str,
) -> str:
    agency_slug = slugify(agency)
    type_slug = slugify(act_type)
    base = f"{agency_slug}-{type_slug}-{year}-{number}"
    remainder = normalize_text(NUMBER_RE.sub("", number_label, count=1))
    if remainder:
        suffix = slugify(remainder)
        if suffix and suffix != type_slug:
            return f"{base}-{suffix}"
    return base


def parse_contran(page: SourcePage, source_html: str, checked_at: str) -> list[dict[str, Any]]:
    parser = TableParser()
    parser.feed(source_html)
    records: list[dict[str, Any]] = []

    for row in parser.rows:
        if len(row) < 5:
            continue
        number_label, act_date, publication_date, subject, status = row[:5]
        if "resolu" in number_label.text.lower() and parse_number(number_label.text) is None:
            continue
        number = parse_number(number_label.text)
        year = parse_year(act_date.text) or parse_year(publication_date.text)
        if number is None or year is None or not subject.text:
            continue

        records.append(
            {
                "id": unique_id("CONTRAN", "RESOLUÇÃO", year, number, number_label.text),
                "agency": "CONTRAN",
                "actType": "RESOLUÇÃO",
                "number": number,
                "numberLabel": number_label.text,
                "year": year,
                "actDate": act_date.text or None,
                "publicationDate": publication_date.text or None,
                "subject": subject.text,
                "status": status.text if status.text not in {"", "--"} else None,
                "officialUrl": normalize_href(page.url, number_label.href),
                "sourcePageUrl": page.url,
                "lastCheckedAt": checked_at,
            }
        )
    return records


def parse_contran_deliberations(
    page: SourcePage,
    source_html: str,
    checked_at: str,
) -> list[dict[str, Any]]:
    parser = TableParser()
    parser.feed(source_html)
    records: list[dict[str, Any]] = []

    for row in parser.rows:
        if len(row) < 3:
            continue
        number_label = row[0]
        publication_date = row[1]
        subject = row[2]
        observation = row[3] if len(row) >= 4 else Cell("", None)

        if "delibera" not in number_label.text.lower():
            continue

        number = parse_number(number_label.text)
        year = parse_year(publication_date.text)
        if number is None or year is None or not subject.text:
            continue

        records.append(
            {
                "id": unique_id(
                    "CONTRAN",
                    "DELIBERAÇÃO",
                    year,
                    number,
                    number_label.text,
                ),
                "agency": "CONTRAN",
                "actType": "DELIBERAÇÃO",
                "number": number,
                "numberLabel": number_label.text,
                "year": year,
                "actDate": None,
                "publicationDate": publication_date.text or None,
                "subject": subject.text,
                "status": observation.text if observation.text not in {"", "--"} else None,
                "officialUrl": normalize_href(page.url, number_label.href),
                "sourcePageUrl": page.url,
                "lastCheckedAt": checked_at,
            }
        )

    return records


def parse_senatran(page: SourcePage, source_html: str, checked_at: str) -> list[dict[str, Any]]:
    if page.year is None:
        raise ValueError("SENATRAN source page requires a year.")

    parser = TableParser()
    parser.feed(source_html)
    records: list[dict[str, Any]] = []

    for row in parser.rows:
        if len(row) < 3:
            continue
        number_label = row[0]
        publication_date = row[1]
        subject = row[2]
        observation = row[3] if len(row) >= 4 else Cell("", None)

        number = parse_number(number_label.text)
        if number is None or not subject.text:
            continue
        if "portaria" in number_label.text.lower() and number is None:
            continue

        records.append(
            {
                "id": unique_id(
                    "SENATRAN",
                    "PORTARIA",
                    page.year,
                    number,
                    number_label.text,
                ),
                "agency": "SENATRAN",
                "actType": "PORTARIA",
                "number": number,
                "numberLabel": number_label.text,
                "year": page.year,
                "actDate": None,
                "publicationDate": publication_date.text or None,
                "subject": subject.text,
                "status": observation.text if observation.text not in {"", "--"} else None,
                "officialUrl": normalize_href(page.url, number_label.href),
                "sourcePageUrl": page.url,
                "lastCheckedAt": checked_at,
            }
        )
    return records


def dedupe_records(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_id: dict[str, dict[str, Any]] = {}
    collisions: dict[str, list[dict[str, Any]]] = {}
    for record in records:
        key = record["id"]
        if key in by_id and by_id[key] != record:
            collisions.setdefault(key, [by_id[key]]).append(record)
        else:
            by_id[key] = record

    if collisions:
        details = []
        for key, items in sorted(collisions.items()):
            details.append(
                f"{key}: " + " | ".join(item["numberLabel"] for item in items)
            )
        raise ValueError("Conflicting normative IDs:\n" + "\n".join(details))

    return sorted(
        by_id.values(),
        key=lambda item: (
            item["agency"],
            item["actType"],
            -int(item["year"]),
            -int(item["number"]),
            item["id"],
        ),
    )


def canonical_hash(records: list[dict[str, Any]]) -> str:
    payload = json.dumps(
        records,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest().upper()


def dataset_version(checked_at: str, content_sha256: str) -> str:
    return f"{checked_at}-norms-{content_sha256[:12].lower()}"


def validate_dataset(dataset: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    records = dataset.get("records")
    if not isinstance(records, list):
        return ["records is missing or invalid."]

    ids = [str(record.get("id") or "") for record in records]
    duplicate_ids = sorted({item for item in ids if ids.count(item) > 1 and item})
    if duplicate_ids:
        errors.append("Duplicate IDs: " + ", ".join(duplicate_ids[:20]))

    if len(records) < 300:
        errors.append(f"Dataset unexpectedly small: {len(records)} records.")

    contran_resolutions = [
        r for r in records
        if r.get("agency") == "CONTRAN" and r.get("actType") == "RESOLUÇÃO"
    ]
    contran_deliberations = [
        r for r in records
        if r.get("agency") == "CONTRAN" and r.get("actType") == "DELIBERAÇÃO"
    ]
    senatran = [r for r in records if r.get("agency") == "SENATRAN"]
    if not contran_resolutions:
        errors.append("No CONTRAN resolutions found.")
    if not contran_deliberations:
        errors.append("No CONTRAN deliberations found.")
    if not senatran:
        errors.append("No SENATRAN ordinances found.")

    required = {
        ("CONTRAN", 985, 2022),
        ("CONTRAN", 1031, 2026),
        ("CONTRAN", 279, 2026),
        ("SENATRAN", 354, 2022),
    }
    present = {
        (str(r.get("agency")), int(r.get("number") or 0), int(r.get("year") or 0))
        for r in records
    }
    for item in sorted(required - present):
        errors.append(f"Required official act missing: {item}")

    try:
        verified_year = int(str(dataset.get("verifiedAt") or "")[:4])
    except ValueError:
        verified_year = 0
        errors.append("verifiedAt has an invalid year.")

    for record in records:
        if record.get("agency") not in {"CONTRAN", "SENATRAN"}:
            errors.append(f"{record.get('id')}: invalid agency.")
        if record.get("actType") not in {"RESOLUÇÃO", "DELIBERAÇÃO", "PORTARIA"}:
            errors.append(f"{record.get('id')}: invalid act type.")
        record_year = int(record.get("year") or 0)
        if record_year < 1900 or (verified_year and record_year > verified_year):
            errors.append(f"{record.get('id')}: invalid year {record_year}.")
        if not str(record.get("subject") or "").strip():
            errors.append(f"{record.get('id')}: missing subject.")
        try:
            validate_source_url(str(record.get("sourcePageUrl") or ""))
            validate_source_url(str(record.get("officialUrl") or ""))
        except ValueError as exc:
            errors.append(f"{record.get('id')}: {exc}")

    content_digest = canonical_hash(records)
    declared_digest = str(dataset.get("contentSha256") or "")
    if declared_digest != content_digest:
        errors.append("contentSha256 does not match structured records.")

    expected_version = dataset_version(
        str(dataset.get("verifiedAt") or ""),
        content_digest,
    )
    if dataset.get("version") != expected_version:
        errors.append("version does not match canonical content hash.")

    if dataset.get("recordCount") != len(records):
        errors.append("recordCount does not match records length.")

    return errors


def build_dataset(manifest: dict[str, Any], checked_at: str) -> dict[str, Any]:
    all_records: list[dict[str, Any]] = []
    source_pages: list[dict[str, Any]] = []

    for item in manifest["sources"]:
        page = SourcePage(
            agency=item["agency"],
            act_type=item["actType"],
            url=item["url"],
            year=item.get("year"),
        )
        source_html, digest = fetch_source(page.url)
        if page.agency == "CONTRAN" and page.act_type == "RESOLUÇÃO":
            records = parse_contran(page, source_html, checked_at)
        elif page.agency == "CONTRAN" and page.act_type == "DELIBERAÇÃO":
            records = parse_contran_deliberations(page, source_html, checked_at)
        elif page.agency == "SENATRAN":
            records = parse_senatran(page, source_html, checked_at)
        else:
            raise ValueError(f"Unsupported agency in manifest: {page.agency}")

        if not records:
            raise ValueError(f"No records parsed from {page.url}")

        all_records.extend(records)
        source_pages.append(
            {
                "agency": page.agency,
                "actType": page.act_type,
                "year": page.year,
                "url": page.url,
                "sourceSha256": digest,
                "recordCount": len(records),
            }
        )

    records = dedupe_records(all_records)
    content_digest = canonical_hash(records)
    dataset = {
        "version": dataset_version(checked_at, content_digest),
        "generatedAt": checked_at,
        "verifiedAt": checked_at,
        "sourceDocument": "Índices oficiais de Resoluções e Deliberações CONTRAN e Portarias SENATRAN",
        "recordCount": len(records),
        "contentSha256": content_digest,
        "sources": source_pages,
        "records": records,
    }

    errors = validate_dataset(dataset)
    if errors:
        raise ValueError("\n".join(errors))
    return dataset


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def command_build(args: argparse.Namespace) -> int:
    manifest = load_json(args.manifest)
    checked_at = args.checked_at or manifest.get("lastCheckedAt") or date.today().isoformat()
    try:
        dataset = build_dataset(manifest, checked_at)
    except (KeyError, TypeError, ValueError, RuntimeError) as exc:
        print(str(exc), file=sys.stderr)
        return 1

    write_json(args.output, dataset)
    summary = {
        "version": dataset["version"],
        "verifiedAt": dataset["verifiedAt"],
        "recordCount": dataset["recordCount"],
        "contentSha256": dataset["contentSha256"],
        "contranCount": sum(
            1 for record in dataset["records"] if record["agency"] == "CONTRAN"
        ),
        "deliberationCount": sum(
            1
            for record in dataset["records"]
            if record["agency"] == "CONTRAN"
            and record["actType"] == "DELIBERAÇÃO"
        ),
        "senatranCount": sum(
            1 for record in dataset["records"] if record["agency"] == "SENATRAN"
        ),
        "sourcePages": dataset["sources"],
    }
    if args.report:
        write_json(args.report, summary)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


def command_validate(args: argparse.Namespace) -> int:
    dataset = load_json(args.dataset)
    errors = validate_dataset(dataset)
    print(
        json.dumps(
            {
                "version": dataset.get("version"),
                "recordCount": len(dataset.get("records", [])),
                "errors": errors,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 1 if errors else 0


def build_parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description="CONTRAN/SENATRAN normative catalog pipeline")
    sub = root.add_subparsers(dest="command", required=True)

    build = sub.add_parser("build")
    build.add_argument("--manifest", type=Path, required=True)
    build.add_argument("--output", type=Path, required=True)
    build.add_argument("--report", type=Path)
    build.add_argument("--checked-at")
    build.set_defaults(func=command_build)

    validate = sub.add_parser("validate")
    validate.add_argument("--dataset", type=Path, required=True)
    validate.set_defaults(func=command_validate)
    return root


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
