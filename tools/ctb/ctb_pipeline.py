#!/usr/bin/env python3
"""Build and validate the local searchable CTB dataset from Planalto's compiled text."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import date
from html.parser import HTMLParser
from pathlib import Path
from typing import Any

ARTICLE_RE = re.compile(r"^Art\.\s*(\d+)(?:[ºo°])?(?:-([A-Z]))?\.?\s*(.*)$")
CHAPTER_RE = re.compile(r"^CAP[IÍ]TULO\s+(?:[IVXLCDM]+|ÚNICO)(?:-[A-Z])?$")
SECTION_RE = re.compile(r"^Se[cç][aã]o\s+(?:[IVXLCDM]+|Única)(?:-[A-Z])?$", re.IGNORECASE)
SUBSECTION_RE = re.compile(r"^Subse[cç][aã]o\s+(?:[IVXLCDM]+|Única)(?:-[A-Z])?$", re.IGNORECASE)
SKIP_TAGS = {"script", "style", "noscript", "strike", "s", "del"}
BREAK_TAGS = {
    "p", "div", "br", "li", "tr", "td", "th", "h1", "h2", "h3", "h4", "h5", "h6",
    "center", "blockquote",
}
VOID_TAGS = {
    "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
    "param", "source", "track", "wbr",
}
ALLOWED_SOURCE_HOSTS = {"planalto.gov.br", "www.planalto.gov.br"}
ALLOWED_SOURCE_PATH = "/ccivil_03/leis/l9503compilado.htm"
MAX_SOURCE_BYTES = 10_000_000


class VisibleTextParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []
        self.skip_depth = 0
        self.tag_stack: list[tuple[str, bool]] = []

    @staticmethod
    def _is_hidden_revision(
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> bool:
        if tag in SKIP_TAGS:
            return True
        style = " ".join(
            value or ""
            for key, value in attrs
            if key.lower() == "style"
        ).lower()
        return "line-through" in style

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        own_skip = self._is_hidden_revision(tag, attrs)
        if tag not in VOID_TAGS:
            self.tag_stack.append((tag, own_skip))
        if own_skip:
            self.skip_depth += 1
        if self.skip_depth == 0 and tag in BREAK_TAGS:
            self.parts.append("\n")

    def handle_startendtag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        tag = tag.lower()
        if self.skip_depth == 0 and tag in BREAK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag in VOID_TAGS:
            return

        match_index = next(
            (
                index
                for index in range(len(self.tag_stack) - 1, -1, -1)
                if self.tag_stack[index][0] == tag
            ),
            None,
        )
        if match_index is not None:
            closing = self.tag_stack[match_index:]
            del self.tag_stack[match_index:]
            self.skip_depth -= sum(1 for _, own_skip in closing if own_skip)
            self.skip_depth = max(self.skip_depth, 0)

        if self.skip_depth == 0 and tag in BREAK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        if self.skip_depth == 0:
            self.parts.append(data)

    def text(self) -> str:
        return "".join(self.parts)


@dataclass(frozen=True)
class Article:
    id: str
    label: str
    number: int
    suffix: str | None
    chapter: str | None
    section: str | None
    subsection: str | None
    text: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "label": self.label,
            "number": self.number,
            "suffix": self.suffix,
            "chapter": self.chapter,
            "section": self.section,
            "subsection": self.subsection,
            "text": self.text,
        }


def decode_html(raw: bytes) -> str:
    head = raw[:4096].decode("ascii", errors="ignore")
    match = re.search(r"charset\s*=\s*[\"']?([A-Za-z0-9._-]+)", head, re.IGNORECASE)
    candidates = [match.group(1)] if match else []
    candidates += ["utf-8", "windows-1252", "iso-8859-1"]
    tried: set[str] = set()
    for encoding in candidates:
        key = encoding.lower()
        if key in tried:
            continue
        tried.add(key)
        try:
            return raw.decode(encoding)
        except (LookupError, UnicodeDecodeError):
            pass
    return raw.decode("windows-1252", errors="replace")


def normalize_line(value: str) -> str:
    value = html.unescape(value).replace("\u00a0", " ").replace("\u00ad", "")
    value = re.sub(r"[ \t\r\f\v]+", " ", value)
    return value.strip()


def visible_lines(source_html: str) -> list[str]:
    parser = VisibleTextParser()
    parser.feed(source_html)
    raw_lines: list[str] = []
    for raw_line in parser.text().splitlines():
        line = normalize_line(raw_line)
        if not line:
            continue
        raw_lines.append(line)

    lines: list[str] = []
    index = 0
    while index < len(raw_lines):
        line = raw_lines[index]
        if (
            line == "Art."
            and index + 1 < len(raw_lines)
            and re.match(r"^\d+(?:[ºo°])?(?:-[A-Z])?\.?", raw_lines[index + 1])
        ):
            lines.append("Art. " + raw_lines[index + 1])
            index += 2
            continue
        lines.append(line)
        index += 1
    return lines


def article_id(number: int, suffix: str | None) -> str:
    return f"art-{number}" + (f"-{suffix.lower()}" if suffix else "")


def article_label(number: int, suffix: str | None) -> str:
    if suffix:
        return f"Art. {number}-{suffix}"
    return f"Art. {number}º" if 1 <= number <= 9 else f"Art. {number}"


def parse_annex_i(source_html: str) -> Article:
    lines = visible_lines(source_html)
    try:
        start = lines.index("ANEXO I")
    except ValueError as exc:
        raise ValueError("Required CTB Annex I is missing.") from exc

    annex_lines: list[str] = []
    for line in lines[start:]:
        if annex_lines and line.startswith("(Vide Resolução"):
            break
        annex_lines.append(line)

    text = "\n".join(annex_lines).strip()
    if "DOS CONCEITOS E DEFINIÇÕES" not in text:
        raise ValueError("CTB Annex I heading is incomplete.")
    if "ACOSTAMENTO -" not in text or "VIADUTO -" not in text:
        raise ValueError("CTB Annex I definitions appear incomplete.")

    return Article(
        id="anexo-i",
        label="Anexo I",
        number=10_000,
        suffix=None,
        chapter="ANEXO I",
        section="DOS CONCEITOS E DEFINIÇÕES",
        subsection=None,
        text=text,
    )


def parse_records(source_html: str) -> list[Article]:
    return [*parse_articles(source_html), parse_annex_i(source_html)]


def parse_articles(source_html: str) -> list[Article]:
    lines = visible_lines(source_html)
    articles: list[Article] = []
    current_number: int | None = None
    current_suffix: str | None = None
    current_chapter: str | None = None
    current_section: str | None = None
    current_subsection: str | None = None
    current_text: list[str] = []
    article_chapter: str | None = None
    article_section: str | None = None
    article_subsection: str | None = None

    def flush() -> None:
        nonlocal current_number, current_suffix, current_text
        if current_number is None:
            return
        text = "\n".join(current_text).strip()
        if not text:
            raise ValueError(f"Article {current_number} has no text")
        articles.append(
            Article(
                id=article_id(current_number, current_suffix),
                label=article_label(current_number, current_suffix),
                number=current_number,
                suffix=current_suffix,
                chapter=article_chapter,
                section=article_section,
                subsection=article_subsection,
                text=text,
            )
        )
        current_number = None
        current_suffix = None
        current_text = []

    for line in lines:
        chapter_match = CHAPTER_RE.match(line)
        if chapter_match:
            flush()
            current_chapter = line
            current_section = None
            current_subsection = None
            continue

        section_match = SECTION_RE.match(line)
        if section_match:
            flush()
            current_section = line
            current_subsection = None
            continue

        subsection_match = SUBSECTION_RE.match(line)
        if subsection_match:
            flush()
            current_subsection = line
            continue

        match = ARTICLE_RE.match(line)
        if match:
            flush()
            current_number = int(match.group(1))
            current_suffix = match.group(2).upper() if match.group(2) else None
            article_chapter = current_chapter
            article_section = current_section
            article_subsection = current_subsection
            current_text = [line]
            continue

        if current_number is not None:
            if line.upper().startswith("ANEXO I"):
                flush()
                break
            current_text.append(line)

    flush()
    return articles


def validate_records(records: list[Article], min_articles: int = 300) -> list[str]:
    errors: list[str] = []
    if len(records) < min_articles:
        errors.append(f"Dataset has {len(records)} articles; minimum required is {min_articles}.")

    ids = [record.id for record in records]
    duplicates = sorted({item for item in ids if ids.count(item) > 1})
    if duplicates:
        errors.append("Duplicate article IDs: " + ", ".join(duplicates))

    numbers = {record.number for record in records}
    for required in (1, 20, 61, 162, 165, 181, 230, 244, 302, 306, 320, 341):
        if required not in numbers:
            errors.append(f"Required article missing: {required}")

    required_ids = {
        "anexo-i",
        "art-139-a",
        "art-139-b",
        "art-148-a",
        "art-165-a",
        "art-268-a",
        "art-282-a",
        "art-326-c",
    }
    present_ids = {record.id for record in records}
    for required_id in sorted(required_ids - present_ids):
        errors.append(f"Required suffixed article missing: {required_id}")

    for record in records:
        if record.id == "anexo-i":
            if not record.text.startswith("ANEXO I"):
                errors.append("anexo-i: annex text does not start with ANEXO I.")
        elif not record.text.lower().startswith("art."):
            errors.append(f"{record.id}: article text does not start with Art.")
        if len(record.text) > 200_000:
            errors.append(f"{record.id}: suspiciously large article text.")
    return errors


def dataset_version(checked_at: str, source_sha256: str) -> str:
    return f"{checked_at}-planalto-{source_sha256[:12].lower()}"


def build_dataset(
    source_html: str,
    source_url: str,
    checked_at: str,
    source_sha256: str | None = None,
) -> dict[str, Any]:
    records = parse_records(source_html)
    errors = validate_records(records)
    if errors:
        grouped: dict[str, list[Article]] = {}
        for record in records:
            grouped.setdefault(record.id, []).append(record)
        duplicates = {
            key: items
            for key, items in grouped.items()
            if len(items) > 1
        }
        if duplicates:
            print("===== CTB DUPLICATE DIAGNOSTICS =====", file=sys.stderr)
            for key, items in sorted(duplicates.items()):
                print(f"{key}: {len(items)} occurrence(s)", file=sys.stderr)
                for index, item in enumerate(items, start=1):
                    preview = re.sub(r"\\s+", " ", item.text)[:700]
                    print(
                        f"  #{index} chapter={item.chapter!r} "
                        f"section={item.section!r} text={preview!r}",
                        file=sys.stderr,
                    )
        raise ValueError("\n".join(errors))
    source_digest = (
        source_sha256
        or hashlib.sha256(source_html.encode("utf-8")).hexdigest().upper()
    )
    record_payload = [record.to_dict() for record in records]
    content_digest = hashlib.sha256(
        json.dumps(
            record_payload,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")
    ).hexdigest().upper()

    return {
        "version": dataset_version(checked_at, content_digest),
        "generatedAt": checked_at,
        "verifiedAt": checked_at,
        "sourceDocument": "Lei nº 9.503/1997 — Código de Trânsito Brasileiro — texto compilado",
        "sourceUrl": source_url,
        "sourceSha256": source_digest,
        "contentSha256": content_digest,
        "recordCount": len(records),
        "records": record_payload,
    }


def validate_source_url(url: str) -> None:
    parsed = urllib.parse.urlparse(url)
    host = (parsed.hostname or "").lower()
    if (
        parsed.scheme.lower() != "https"
        or host not in ALLOWED_SOURCE_HOSTS
        or parsed.port not in (None, 443)
        or parsed.path.lower() != ALLOWED_SOURCE_PATH
        or parsed.params
        or parsed.query
        or parsed.fragment
    ):
        raise ValueError(
            "CTB source URL must be the exact official compiled-text URL on Planalto."
        )


def fetch_source(url: str, attempts: int = 5) -> tuple[str, str]:
    validate_source_url(url)
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": (
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/141.0 Safari/537.36 "
                "EnquadraPlus-CTB-Pipeline/1.0"
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
                    raise ValueError("Official CTB response exceeds the configured size limit.")
                if len(raw) < 100_000:
                    raise ValueError(
                        f"Official CTB response is unexpectedly small: {len(raw)} bytes."
                    )
                digest = hashlib.sha256(raw).hexdigest().upper()
                return decode_html(raw), digest
        except (OSError, urllib.error.URLError, ValueError) as exc:
            last_error = exc
            if attempt == attempts:
                break
            time.sleep(min(2 ** (attempt - 1), 8))

    raise RuntimeError(
        f"Unable to download official CTB source after {attempts} attempts: {last_error}"
    )


def load_dataset(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def validate_dataset(dataset: dict[str, Any], min_articles: int = 300) -> list[str]:
    raw_records = dataset.get("records")
    if not isinstance(raw_records, list):
        return ["Field records is missing or invalid."]

    articles: list[Article] = []
    errors: list[str] = []
    for index, item in enumerate(raw_records, start=1):
        try:
            articles.append(
                Article(
                    id=str(item["id"]),
                    label=str(item["label"]),
                    number=int(item["number"]),
                    suffix=item.get("suffix"),
                    chapter=item.get("chapter"),
                    section=item.get("section"),
                    subsection=item.get("subsection"),
                    text=str(item["text"]),
                )
            )
        except (KeyError, TypeError, ValueError) as exc:
            errors.append(f"Record #{index} is invalid: {exc}")

    errors.extend(validate_records(articles, min_articles=min_articles))
    for field in (
        "version",
        "generatedAt",
        "verifiedAt",
        "sourceDocument",
        "sourceUrl",
        "sourceSha256",
        "contentSha256",
    ):
        if not str(dataset.get(field) or "").strip():
            errors.append(f"Required metadata field is empty: {field}")

    source_digest = str(dataset.get("sourceSha256") or "")
    content_digest = str(dataset.get("contentSha256") or "")
    if not re.fullmatch(r"[A-F0-9]{64}", source_digest):
        errors.append("sourceSha256 is invalid.")
    if not re.fullmatch(r"[A-F0-9]{64}", content_digest):
        errors.append("contentSha256 is invalid.")

    calculated_content_digest = hashlib.sha256(
        json.dumps(
            raw_records,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")
    ).hexdigest().upper()
    if content_digest and content_digest != calculated_content_digest:
        errors.append("contentSha256 does not match the structured records.")

    expected_version = dataset_version(
        str(dataset.get("verifiedAt") or ""),
        calculated_content_digest,
    )
    if dataset.get("version") != expected_version:
        errors.append("version does not match the structured content hash.")

    if dataset.get("recordCount") != len(raw_records):
        errors.append("recordCount does not match records length.")
    return errors


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def command_build(args: argparse.Namespace) -> int:
    checked_at = args.checked_at or date.today().isoformat()
    source_html, source_sha256 = fetch_source(args.source_url)
    try:
        dataset = build_dataset(
            source_html,
            args.source_url,
            checked_at,
            source_sha256=source_sha256,
        )
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    write_json(args.output, dataset)
    report = {
        "recordCount": dataset["recordCount"],
        "version": dataset["version"],
        "verifiedAt": dataset["verifiedAt"],
        "sourceUrl": dataset["sourceUrl"],
        "sourceSha256": dataset["sourceSha256"],
        "contentSha256": dataset["contentSha256"],
        "firstArticle": dataset["records"][0]["label"],
        "lastArticle": dataset["records"][-1]["label"],
    }
    if args.report:
        write_json(args.report, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


def command_validate(args: argparse.Namespace) -> int:
    dataset = load_dataset(args.dataset)
    errors = validate_dataset(dataset, min_articles=args.min_articles)
    summary = {
        "recordCount": len(dataset.get("records", [])),
        "version": dataset.get("version"),
        "verifiedAt": dataset.get("verifiedAt"),
        "errors": errors,
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 1 if errors else 0


def build_parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description="CTB local dataset pipeline")
    sub = root.add_subparsers(dest="command", required=True)

    build = sub.add_parser("build", help="Download and structure Planalto's compiled CTB")
    build.add_argument("--source-url", required=True)
    build.add_argument("--output", type=Path, required=True)
    build.add_argument("--report", type=Path)
    build.add_argument("--checked-at")
    build.set_defaults(func=command_build)

    validate = sub.add_parser("validate", help="Validate an existing CTB dataset")
    validate.add_argument("--dataset", type=Path, required=True)
    validate.add_argument("--min-articles", type=int, default=300)
    validate.set_defaults(func=command_validate)
    return root


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
