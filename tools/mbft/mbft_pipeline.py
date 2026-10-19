#!/usr/bin/env python3
"""Pipeline de extração e validação do MBFT para o Enquadra+."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from datetime import date
from pathlib import Path
from typing import Any

CODE_RE = re.compile(r"\b\d{3}-\d{2}\b")
CODE_HEADER_RE = re.compile(
    r"C[oó]digo(?:\s+(?:do|de))?\s+Enquadramento",
    re.IGNORECASE,
)
CODE_HEADER_WITH_VALUE_RE = re.compile(
    r"C[oó]digo(?:\s+(?:do|de))?\s+Enquadramento\s*:?\s*(\d{3}-\d{2})",
    re.IGNORECASE,
)

FIELD_LABELS = [
    "Tipificação Resumida",
    "Código do Enquadramento",
    "Amparo Legal",
    "Tipificação do Enquadramento",
    "Gravidade",
    "Penalidade",
    "Medida Administrativa",
    "Pode Configurar Crime de Trânsito",
    "Infrator",
    "Competência",
    "Pontuação",
    "Constatação da Infração",
    "Quando Autuar",
    "Quando NÃO Autuar",
    "Definições e Procedimentos",
    "Exemplos do Campo de Observações do AIT",
    "Informações Complementares",
]
FIELD_LABEL_ALIASES = {
    "Medida Administrativa": ["Medidas Administrativas"],
    "Exemplos do Campo de Observações do AIT": ["Observações do AIT"],
}
ALL_FIELD_LABELS = FIELD_LABELS + [
    alias
    for aliases in FIELD_LABEL_ALIASES.values()
    for alias in aliases
]
OPERATIONAL_LABELS = [
    "Quando Autuar",
    "Quando NÃO Autuar",
    "Definições e Procedimentos",
    "Exemplos do Campo de Observações do AIT",
]
VERIFICATION_COMPARE_FIELDS = (
    "article",
    "shortDescription",
    "description",
    "severity",
    "points",
    "offenderResponsibility",
    "authority",
    "verificationMethod",
    "penalty",
    "administrativeMeasure",
    "canConfigureTrafficCrime",
    "whenToCite",
    "whenNotToCite",
    "procedures",
    "aitObservations",
)
OPERATIONAL_DATA_FIELDS = (
    "whenToCite",
    "whenNotToCite",
    "procedures",
    "aitObservations",
)
JSON_FIELD_SOURCE_LABELS = {
    "article": "Amparo Legal",
    "shortDescription": "Tipificação Resumida",
    "description": "Tipificação do Enquadramento",
    "severity": "Gravidade",
    "points": "Pontuação",
    "offenderResponsibility": "Infrator",
    "authority": "Competência",
    "verificationMethod": "Constatação da Infração",
    "penalty": "Penalidade",
    "administrativeMeasure": "Medida Administrativa",
    "canConfigureTrafficCrime": "Pode Configurar Crime de Trânsito",
}
SCALAR_FIELD_LABEL_RE = re.compile(
    r"(?:Tipifica[cç][aã]o\s+do\s+Enquadramento|Amparo\s+Legal|Gravidade|"
    r"Penalidade|Medidas?\s+Administrativas?|Infrator|Compet[eê]ncia|"
    r"Pontua[cç][aã]o|Constata[cç][aã]o\s+da\s+Infra[cç][aã]o)\s*:",
    re.IGNORECASE,
)
LABEL_PATTERN = "|".join(
    re.escape(label).replace(r"\ ", r"\s+") for label in ALL_FIELD_LABELS
)


def field_label_pattern(label: str) -> str:
    labels = [label, *FIELD_LABEL_ALIASES.get(label, [])]
    return "(?:" + "|".join(
        re.escape(item).replace(r"\ ", r"\s+") for item in labels
    ) + ")"


def normalize_text(value: str) -> str:
    value = value.replace("\u00ad", "").replace("\u00a0", " ")
    value = re.sub(r"[ \t]+", " ", value)
    value = re.sub(r"\s*\n\s*", "\n", value)
    value = re.sub(r"\n{3,}", "\n\n", value)
    return value.strip()


def compact(value: str | None) -> str | None:
    if value is None:
        return None
    value = normalize_text(value)
    value = re.sub(r"\s+", " ", value).strip(" -;:\n")
    return value or None


def extract_field_from_text(text: str, label: str) -> str | None:
    label_re = field_label_pattern(label)
    pattern = re.compile(
        rf"{label_re}\s*:?\s*(.*?)(?=(?:{LABEL_PATTERN})\s*:?\s*|\Z)",
        re.IGNORECASE | re.DOTALL,
    )
    match = pattern.search(text)
    return compact(match.group(1)) if match else None


def cell_field(cells: list[str], label: str) -> str | None:
    label_re = re.compile(
        r"^\s*" + field_label_pattern(label) + r"\s*:?\s*",
        re.IGNORECASE,
    )
    for cell in cells:
        if not label_re.search(cell):
            continue
        value = label_re.sub("", cell, count=1)
        value = compact(value)
        if value:
            return value
    return None


def table_field(tables: list[list[list[str | None]]], label: str) -> str | None:
    label_re = re.compile(
        r"^\s*" + field_label_pattern(label) + r"\s*:?\s*",
        re.IGNORECASE,
    )

    def has_known_label(row: list[str | None]) -> bool:
        for raw_cell in row:
            if not raw_cell:
                continue
            cell = compact(str(raw_cell)) or ""
            if any(
                re.search(
                    re.escape(known).replace(r"\ ", r"\s+"),
                    cell,
                    re.IGNORECASE,
                )
                for known in ALL_FIELD_LABELS
            ):
                return True
        return False

    rows = [row for table in tables for row in table]

    for row_index, row in enumerate(rows):
        for column_index, raw_cell in enumerate(row):
            if not raw_cell:
                continue
            cell = normalize_text(str(raw_cell))
            if not label_re.search(cell):
                continue

            inline = compact(label_re.sub("", cell, count=1))
            if inline:
                return inline

            values: list[str] = []
            for next_row in rows[row_index + 1 :]:
                if has_known_label(next_row):
                    break
                if column_index >= len(next_row):
                    continue
                raw_value = next_row[column_index]
                if not raw_value:
                    continue
                value = compact(str(raw_value))
                if value:
                    values.append(value)

            if values:
                return compact(" ".join(values))
    return None


def operational_table_fields(
    tables: list[list[list[str | None]]],
) -> dict[str, str]:
    collected: dict[str, list[str]] = {label: [] for label in OPERATIONAL_LABELS}
    active_anchors: dict[str, float] | None = None
    in_operational_block = False

    def cell_matches(raw_cell: str | None, label: str) -> bool:
        if not raw_cell:
            return False
        return bool(
            re.search(
                r"^\s*" + field_label_pattern(label),
                normalize_text(str(raw_cell)),
                re.IGNORECASE,
            )
        )

    def append(label: str, raw_value: str | None) -> None:
        value = compact(raw_value)
        if value and value not in collected[label]:
            collected[label].append(value)

    for table in tables:
        for row in table:
            if any(
                cell_matches(cell, "Informações Complementares")
                for cell in row
            ):
                in_operational_block = False
                active_anchors = None
                continue

            denominator = max(len(row) - 1, 1)
            header_anchors: dict[str, float] = {}
            for column_index, cell in enumerate(row):
                for label in OPERATIONAL_LABELS:
                    if cell_matches(cell, label):
                        header_anchors[label] = column_index / denominator

            if len(header_anchors) >= 2:
                active_anchors = header_anchors
                in_operational_block = True
                continue

            if (
                not in_operational_block
                or not active_anchors
                or len(row) <= 1
            ):
                continue

            for column_index, raw_cell in enumerate(row):
                if not raw_cell:
                    continue
                value = compact(str(raw_cell))
                if not value:
                    continue
                if any(cell_matches(raw_cell, label) for label in ALL_FIELD_LABELS):
                    continue

                position = column_index / denominator
                label, distance = min(
                    (
                        (candidate, abs(position - anchor))
                        for candidate, anchor in active_anchors.items()
                    ),
                    key=lambda item: item[1],
                )
                if distance <= 0.22:
                    append(label, value)

    return {
        label: compact(" ".join(values)) or ""
        for label, values in collected.items()
    }

def operational_positioned_fields(
    rows: list[list[dict[str, Any]]],
) -> dict[str, str]:
    """Extract operational columns using absolute PDF cell coordinates."""
    collected: dict[str, list[str]] = {label: [] for label in OPERATIONAL_LABELS}
    active_anchors: dict[str, float] | None = None
    in_operational_block = False

    def cell_text(cell: dict[str, Any]) -> str:
        return normalize_text(str(cell.get("text") or ""))

    def cell_matches(cell: dict[str, Any], label: str) -> bool:
        return bool(
            re.search(
                r"^\s*" + field_label_pattern(label),
                cell_text(cell),
                re.IGNORECASE,
            )
        )

    def center(cell: dict[str, Any]) -> float:
        return (float(cell["x0"]) + float(cell["x1"])) / 2.0

    def append(label: str, raw_value: str) -> None:
        value = compact(raw_value)
        if value and value not in collected[label]:
            collected[label].append(value)

    for row in rows:
        if any(cell_matches(cell, "Informações Complementares") for cell in row):
            in_operational_block = False
            active_anchors = None
            continue

        header_anchors: dict[str, float] = {}
        for cell in row:
            for label in OPERATIONAL_LABELS:
                if cell_matches(cell, label):
                    header_anchors[label] = center(cell)

        if len(header_anchors) >= 2:
            active_anchors = header_anchors
            in_operational_block = True
            continue

        if not in_operational_block or not active_anchors:
            continue

        for cell in row:
            value = compact(cell_text(cell))
            if not value:
                continue
            if any(cell_matches(cell, label) for label in ALL_FIELD_LABELS):
                continue

            x = center(cell)
            label = min(
                active_anchors,
                key=lambda candidate: abs(x - active_anchors[candidate]),
            )
            append(label, value)

    def without_redundant_fragments(values: list[str]) -> list[str]:
        cleaned: list[str] = []
        normalized = [(value, compact(value) or "") for value in values]
        for index, (value, normalized_value) in enumerate(normalized):
            if not normalized_value:
                continue
            is_numbered_item = bool(
                re.match(r"^\d{1,2}[\.)]\s+", normalized_value)
            )
            redundant = (
                not is_numbered_item
                and len(normalized_value) >= 5
                and any(
                    other_index != index
                    and len(other_value) >= len(normalized_value) + 6
                    and normalized_value.casefold() in other_value.casefold()
                    for other_index, (_, other_value) in enumerate(normalized)
                )
            )
            if not redundant:
                cleaned.append(value)
        return cleaned

    return {
        label: compact(" ".join(without_redundant_fragments(values))) or ""
        for label, values in collected.items()
    }


def structured_source_fields(section: dict[str, Any]) -> set[str]:
    """Fields whose values are directly anchored to official table structure."""
    supported: set[str] = set()

    cells = [normalize_text(str(cell)) for cell in section.get("cells", [])]
    for json_field, label in JSON_FIELD_SOURCE_LABELS.items():
        label_re = re.compile(
            r"^\s*" + field_label_pattern(label) + r"\s*:?",
            re.IGNORECASE,
        )
        if any(label_re.search(cell) for cell in cells):
            supported.add(json_field)

    operational_headers: set[str] = set()
    for row in section.get("positionedRows", []):
        row_headers: set[str] = set()
        for cell in row:
            value = normalize_text(str(cell.get("text") or ""))
            for label in OPERATIONAL_LABELS:
                if re.search(
                    r"^\s*" + field_label_pattern(label),
                    value,
                    re.IGNORECASE,
                ):
                    row_headers.add(label)
        if len(row_headers) >= 4:
            operational_headers.update(row_headers)

    if len(operational_headers) >= 4:
        supported.update(OPERATIONAL_DATA_FIELDS)

    return supported


OPERATIONAL_JSON_BY_LABEL = {
    "Quando Autuar": "whenToCite",
    "Quando NÃO Autuar": "whenNotToCite",
    "Definições e Procedimentos": "procedures",
    "Exemplos do Campo de Observações do AIT": "aitObservations",
}


def explicit_empty_operational_fields(section: dict[str, Any]) -> set[str]:
    """Prove empty operational columns from an explicit four-column PDF table."""
    tables = section.get("tables", [])
    has_complete_header = False
    for table in tables:
        for row in table:
            row_headers: set[str] = set()
            for raw_cell in row:
                if not raw_cell:
                    continue
                value = normalize_text(str(raw_cell))
                for label in OPERATIONAL_LABELS:
                    if re.search(
                        r"^\s*" + field_label_pattern(label),
                        value,
                        re.IGNORECASE,
                    ):
                        row_headers.add(label)
            if len(row_headers) >= 4:
                has_complete_header = True
                break
        if has_complete_header:
            break

    if not has_complete_header:
        return set()

    extracted = operational_table_fields(tables)
    return {
        OPERATIONAL_JSON_BY_LABEL[label]
        for label in OPERATIONAL_LABELS
        if not (compact(extracted.get(label)) or "")
    }


def _safe_structured_source_correction(
    field: str,
    fresh_value: Any,
    *,
    explicit_empty_fields: set[str] | None = None,
) -> bool:
    # Point exceptions and boolean legal flags remain explicit-review decisions.
    if field in {"points", "canConfigureTrafficCrime"}:
        return False
    normalized = _normalized_compare_value(fresh_value)
    if normalized not in (None, "", []):
        return True
    return field in (explicit_empty_fields or set())


def section_field(section: dict[str, Any], label: str) -> str | None:
    value = table_field(section.get("tables", []), label)
    if value:
        return value
    value = cell_field(section["cells"], label)
    if value:
        return value
    return extract_field_from_text(section["text"], label)


def _strict_prefix_fragment_indices(values: list[str]) -> set[int]:
    """Find extraction fragments duplicated by a longer item in the same field."""
    normalized = [
        (compact(value) or "").casefold()
        for value in values
    ]
    fragments: set[int] = set()
    for index, value in enumerate(normalized):
        if len(value) < 8:
            continue
        for other_index, other in enumerate(normalized):
            if index == other_index:
                continue
            if len(other) < len(value) + 6:
                continue
            if other.startswith(value):
                fragments.add(index)
                break
    return fragments


def remove_strict_prefix_fragments(values: list[str]) -> list[str]:
    fragments = _strict_prefix_fragment_indices(values)
    return [
        value
        for index, value in enumerate(values)
        if index not in fragments
    ]


def split_numbered(value: str | None) -> list[str]:
    value = compact(value)
    if not value:
        return []
    pieces = re.split(
        r"(?<!Art\.)\s+(?=\d{1,2}[\.)]\s+)",
        value,
        flags=re.IGNORECASE,
    )
    cleaned: list[str] = []
    seen: set[str] = set()
    for piece in pieces:
        piece = re.sub(r"^\d{1,2}[\.)]\s*", "", piece).strip()
        if piece and piece not in seen:
            seen.add(piece)
            cleaned.append(piece)
    merged: list[str] = []
    for piece in cleaned:
        if (
            merged
            and re.match(r"^(?:ou|e|do|da|dos|das|de|em)\b", piece, re.IGNORECASE)
            and piece[:1].islower()
        ):
            merged[-1] = compact(f"{merged[-1]} {piece}") or merged[-1]
        else:
            merged.append(piece)
    merged = remove_strict_prefix_fragments(merged)
    return merged or [value]


def _ctb_match_text(value: str) -> str:
    value = normalize_text(value)
    value = re.sub(r"^do\s+CTB\s*:\s*", "", value, flags=re.IGNORECASE)
    value = re.sub(r"^Art\.?\s*\d+[A-Z-]*\.?\s*", "", value, flags=re.IGNORECASE)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def repair_orphan_article_procedures(
    record: dict[str, Any],
    ctb_records: list[dict[str, Any]],
) -> list[str]:
    procedures = list(record.get("procedures") or [])
    if not procedures:
        return []

    repairs: list[str] = []
    output: list[str] = []
    index = 0
    while index < len(procedures):
        current = compact(str(procedures[index])) or ""
        if (
            re.fullmatch(r"Art\.?", current, re.IGNORECASE)
            and index + 1 < len(procedures)
        ):
            following = compact(str(procedures[index + 1])) or ""
            needle = _ctb_match_text(following)
            sample = needle[:120].strip()
            matches: list[dict[str, Any]] = []
            if len(sample) >= 24:
                normalized_sample = sample.casefold()
                for article in ctb_records:
                    text = _ctb_match_text(str(article.get("text") or ""))
                    if normalized_sample in text.casefold():
                        matches.append(article)

            if len(matches) == 1:
                article = matches[0]
                label = str(article.get("label") or "").strip()
                if label:
                    if re.match(r"^do\s+CTB\s*:", following, re.IGNORECASE):
                        repaired = f"{label} do CTB: " + re.sub(
                            r"^do\s+CTB\s*:\s*",
                            "",
                            following,
                            count=1,
                            flags=re.IGNORECASE,
                        )
                    else:
                        repaired = f"{label}. {following}"
                    output.append(compact(repaired) or repaired)
                    repairs.append(
                        f"{record.get('code')}: reconstructed {label} from CTB text match."
                    )
                    index += 2
                    continue

        output.append(current)
        index += 1

    record["procedures"] = output
    return repairs


def normalize_article(value: str | None) -> str:
    value = compact(value) or ""
    return re.sub(r"\s+", " ", value).rstrip(".")


def parse_points(value: str | None) -> int | None:
    value = compact(value)
    if not value:
        return None
    match = re.search(r"\b([23457])\b", value)
    return int(match.group(1)) if match else None


def yes_no(value: str | None) -> bool:
    normalized = compact(value) or ""
    upper = normalized.upper()
    if upper.startswith(("NÃO", "NAO")):
        return False
    if upper.startswith("SIM"):
        return True
    # Some official MBFT fiches omit the literal "SIM" and put only the
    # applicable crime article in this cell (for example 733-10 / art. 310).
    return bool(re.search(r"\bArt\.?\s*\d+[A-Z-]*\b", normalized, re.IGNORECASE))


def source_record(
    code: str,
    manifest: dict[str, Any],
    source: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    if source is None:
        source = manifest["base"]
        source_id = f"mbft-{code}"
        document = manifest["document"]
    else:
        source_id = f"mbft-{source['fileKey']}-{code}"
        document = source["resolution"]

    return [{
        "id": source_id,
        "agency": "Conselho Nacional de Trânsito (CONTRAN)",
        "document": document,
        "section": f"Ficha de fiscalização - código {code}",
        "officialUrl": source["sourceUrl"] if "sourceUrl" in source else source["officialUrl"],
        "effectiveFrom": source.get("effectiveFrom"),
        "lastCheckedAt": manifest["lastCheckedAt"],
    }]


def parse_record(
    section: dict[str, Any],
    manifest: dict[str, Any],
    source: dict[str, Any] | None = None,
) -> dict[str, Any]:
    code = section["code"]
    short_description = section_field(section, "Tipificação Resumida")
    description = section_field(section, "Tipificação do Enquadramento")
    article = normalize_article(section_field(section, "Amparo Legal"))
    # Prefer the explicit table cells extracted from the official PDF.
    # Absolute-position rows are a fallback for layouts whose table extraction
    # yields no operational content; wide/merged positioned cells can span
    # multiple official columns and must not be assigned to one field by center.
    operational = operational_table_fields(section.get("tables", []))
    if not any(operational.values()):
        operational = operational_positioned_fields(
            section.get("positionedRows", [])
        )

    return {
        "code": code,
        "status": "PARTIAL_VALIDATION",
        "article": article,
        "shortDescription": short_description or description or f"Enquadramento {code}",
        "description": description or short_description or f"Ficha MBFT {code} pendente de revisão.",
        "severity": compact(section_field(section, "Gravidade")) or "Outra",
        "points": parse_points(section_field(section, "Pontuação")),
        "offenderResponsibility": compact(section_field(section, "Infrator")),
        "authority": compact(section_field(section, "Competência")),
        "verificationMethod": compact(section_field(section, "Constatação da Infração")),
        "penalty": compact(section_field(section, "Penalidade")),
        "administrativeMeasure": compact(section_field(section, "Medida Administrativa")),
        "canConfigureTrafficCrime": yes_no(section_field(section, "Pode Configurar Crime de Trânsito")),
        "whenToCite": split_numbered(
            operational.get("Quando Autuar") or section_field(section, "Quando Autuar")
        ),
        "whenNotToCite": split_numbered(
            operational.get("Quando NÃO Autuar") or section_field(section, "Quando NÃO Autuar")
        ),
        "procedures": split_numbered(
            operational.get("Definições e Procedimentos") or section_field(section, "Definições e Procedimentos")
        ),
        "aitObservations": split_numbered(
            operational.get("Exemplos do Campo de Observações do AIT") or section_field(section, "Exemplos do Campo de Observações do AIT")
        ),
        "related": [],
        "sources": source_record(code, manifest, source),
        "legalBasis": [article] if article else [],
        "sourcePages": sorted(set(section.get("pages", []))),
    }


def page_tables(page: Any) -> list[list[list[str | None]]]:
    try:
        raw_tables = page.extract_tables() or []
    except Exception:
        raw_tables = []

    tables: list[list[list[str | None]]] = []
    for raw_table in raw_tables:
        table: list[list[str | None]] = []
        for raw_row in raw_table or []:
            row: list[str | None] = []
            for raw_cell in raw_row or []:
                if raw_cell is None:
                    row.append(None)
                    continue
                value = normalize_text(str(raw_cell))
                row.append(value or None)
            table.append(row)
        if table:
            tables.append(table)
    return tables


def page_positioned_rows(page: Any) -> list[list[dict[str, Any]]]:
    """Return extracted table cells with their absolute horizontal coordinates."""
    try:
        tables = page.find_tables() or []
    except Exception:
        return []

    positioned: list[list[dict[str, Any]]] = []
    for table in tables:
        try:
            extracted = table.extract() or []
            table_rows = list(table.rows)
        except Exception:
            continue

        for row_object, values in zip(table_rows, extracted):
            row: list[dict[str, Any]] = []
            for bbox, raw_value in zip(row_object.cells, values):
                if bbox is None or raw_value is None:
                    continue
                value = normalize_text(str(raw_value))
                if not value:
                    continue
                row.append({
                    "text": value,
                    "x0": float(bbox[0]),
                    "x1": float(bbox[2]),
                })
            if row:
                positioned.append(row)
    return positioned


def table_cells(tables: list[list[list[str | None]]]) -> list[str]:
    cells: list[str] = []
    for table in tables:
        for row in table:
            for cell in row:
                if cell:
                    cells.append(cell)
    return cells


def find_page_code(text: str, cells: list[str]) -> str | None:
    for cell in cells:
        if CODE_HEADER_RE.search(cell):
            match = CODE_RE.search(cell)
            if match:
                return match.group(0)

    match = CODE_HEADER_WITH_VALUE_RE.search(text)
    return match.group(1) if match else None

def read_pdf_sections(pdf_path: Path) -> tuple[list[dict[str, Any]], str]:
    try:
        import pdfplumber
    except ImportError as exc:
        raise SystemExit("Dependência ausente: instale tools/mbft/requirements.txt") from exc

    sections: list[dict[str, Any]] = []
    current: dict[str, Any] | None = None

    with pdfplumber.open(str(pdf_path)) as pdf:
        for page_number, page in enumerate(pdf.pages, start=1):
            text = normalize_text(page.extract_text(layout=True) or "")
            tables = page_tables(page)
            positioned_rows = page_positioned_rows(page)
            cells = table_cells(tables)
            code = find_page_code(text, cells)

            if code:
                if current and current["code"] == code:
                    current["texts"].append(text)
                    current["cells"].extend(cells)
                    current["tables"].extend(tables)
                    current["positionedRows"].extend(positioned_rows)
                    current["pages"].append(page_number)
                    continue

                if current:
                    current["text"] = "\n".join(current.pop("texts"))
                    sections.append(current)

                current = {
                    "code": code,
                    "texts": [text],
                    "cells": list(cells),
                    "tables": list(tables),
                    "positionedRows": list(positioned_rows),
                    "pages": [page_number],
                }
            elif current:
                current["texts"].append(text)
                current["cells"].extend(cells)
                current["tables"].extend(tables)
                current["positionedRows"].extend(positioned_rows)
                current["pages"].append(page_number)

    if current:
        current["text"] = "\n".join(current.pop("texts"))
        sections.append(current)

    digest = hashlib.sha256(pdf_path.read_bytes()).hexdigest().upper()
    return sections, digest


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def amendment_source_hashes(
    manifest: dict[str, Any],
    amendment_dir: Path | None,
) -> dict[str, str]:
    if not amendment_dir:
        return {}

    hashes: dict[str, str] = {}
    for amendment in manifest.get("amendments", []):
        file_key = amendment.get("fileKey")
        resolution = amendment.get("resolution")
        if not file_key or not resolution:
            continue
        path = amendment_dir / f"{file_key}.pdf"
        if path.is_file():
            hashes[str(resolution)] = file_sha256(path)
    return hashes


def verified_overrides(path: Path | None) -> dict[str, dict[str, Any]]:
    if not path or not path.exists():
        return {}
    dataset = load_json(path)
    return {
        record["code"]: record
        for record in dataset.get("records", [])
        if record.get("status") == "VERIFIED"
    }


def _normalized_compare_value(value: Any) -> Any:
    if isinstance(value, list):
        return [compact(str(item)) or "" for item in value]
    if isinstance(value, str):
        return compact(value)
    return value


def semantic_record_issues(record: dict[str, Any]) -> list[str]:
    """Returns high-confidence structural corruption found in one MBFT record."""
    issues: list[str] = []
    code = str(record.get("code") or "UNKNOWN")

    for field in OPERATIONAL_DATA_FIELDS:
        values = record.get(field) or []
        if not isinstance(values, list):
            issues.append(f"{code}: {field} deve ser uma lista.")
            continue

        prefix_fragments = _strict_prefix_fragment_indices(
            [str(value) for value in values]
        )
        for fragment_index in sorted(prefix_fragments):
            issues.append(
                f"{code}: {field}[{fragment_index + 1}] contém fragmento prefixo "
                "duplicado por item completo da mesma coluna."
            )

        for index, raw_value in enumerate(values, start=1):
            value = compact(str(raw_value)) or ""
            if not value:
                continue
            location = f"{code}: {field}[{index}]"

            if "\ufffd" in value:
                issues.append(f"{location} contém caractere Unicode de substituição.")

            if re.fullmatch(r"Art\.?\s*", value, re.IGNORECASE):
                issues.append(f"{location} contém token órfão Art.")

            if re.search(r"\butilizar\s*$", value, re.IGNORECASE):
                issues.append(f"{location} termina em 'utilizar' sem enquadramento.")

            if (
                field == "aitObservations"
                and re.search(r"\.\s*possuir\s*$", value, re.IGNORECASE)
            ):
                issues.append(
                    f"{location} termina com cauda corrompida '. possuir'."
                )

            if (
                field == "procedures"
                and re.fullmatch(
                    r"Observa(?:ç|c)(?:ões|oes)\s+do\s+AIT\s*:?",
                    value,
                    re.IGNORECASE,
                )
            ):
                issues.append(
                    f"{location} contém cabeçalho de Observações do AIT em procedimentos."
                )

            exact_headers = {
                "whenToCite": r"Quando\s+Autuar\s*:?",
                "whenNotToCite": r"Quando\s+N[AÃ]O\s+Autuar\s*:?",
                "procedures": r"Defini[cç][oõ]es\s+e\s+Procedimentos\s*:?",
                "aitObservations": (
                    r"(?:Exemplos\s+do\s+Campo\s+de\s+)?"
                    r"Observa[cç][oõ]es\s+do\s+AIT\s*:?"
                ),
            }
            for owner_field, pattern in exact_headers.items():
                if owner_field == field:
                    continue
                if re.fullmatch(pattern, value, re.IGNORECASE):
                    issues.append(
                        f"{location} contém cabeçalho pertencente a {owner_field}."
                    )

    for field in (
        "article",
        "shortDescription",
        "description",
        "severity",
        "offenderResponsibility",
        "authority",
        "verificationMethod",
        "penalty",
        "administrativeMeasure",
    ):
        value = str(record.get(field) or "")
        if "\ufffd" in value:
            issues.append(f"{code}: {field} contém caractere Unicode de substituição.")

    return issues


def verified_record_differences(
    fresh: dict[str, Any],
    override: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    differences: dict[str, dict[str, Any]] = {}
    for field in VERIFICATION_COMPARE_FIELDS:
        fresh_value = _normalized_compare_value(fresh.get(field))
        stored_value = _normalized_compare_value(override.get(field))
        if fresh_value != stored_value:
            differences[field] = {
                "fresh": fresh_value,
                "stored": stored_value,
            }
    return differences


def _is_monotonic_source_recovery(stored: Any, fresh: Any) -> bool:
    stored = _normalized_compare_value(stored)
    fresh = _normalized_compare_value(fresh)
    if stored == fresh:
        return True
    if stored in (None, "", []) and fresh not in (None, "", []):
        return True
    if isinstance(stored, str) and isinstance(fresh, str):
        return len(stored) >= 8 and fresh.startswith(stored)
    if isinstance(stored, list) and isinstance(fresh, list):
        if not stored and fresh:
            return True
        fresh_values = [str(item) for item in fresh]
        for stored_item in stored:
            stored_text = str(stored_item)
            if not any(
                candidate == stored_text
                or (
                    len(stored_text) >= 8
                    and candidate.startswith(stored_text)
                )
                for candidate in fresh_values
            ):
                return False
        return True
    return False


def _semantic_issue_fields(issues: list[str]) -> set[str]:
    fields: set[str] = set()
    for issue in issues:
        match = re.match(r"[^:]+:\s*([A-Za-z][A-Za-z0-9]*)", str(issue))
        if match:
            fields.add(match.group(1))
    return fields


def classify_review_difference(item: dict[str, Any]) -> str:
    if item.get("semanticIssues"):
        return "MANUAL_REVIEW_REQUIRED"
    field_differences = item.get("fieldDifferences") or {}
    if not field_differences:
        return "MATCH"
    if all(
        _is_monotonic_source_recovery(
            values.get("stored"),
            values.get("fresh"),
        )
        for values in field_differences.values()
    ):
        return "MONOTONIC_SOURCE_RECOVERY"

    stored_corrupt_fields = _semantic_issue_fields(
        item.get("storedSemanticIssues") or []
    )
    if stored_corrupt_fields and all(
        field in stored_corrupt_fields
        or _is_monotonic_source_recovery(
            values.get("stored"),
            values.get("fresh"),
        )
        for field, values in field_differences.items()
    ):
        return "STRUCTURAL_SOURCE_RECOVERY"

    structured_fields = set(item.get("structuredSourceFields") or [])
    explicit_empty_fields = set(item.get("explicitEmptySourceFields") or [])
    if structured_fields and all(
        field in structured_fields
        and _safe_structured_source_correction(
            field,
            values.get("fresh"),
            explicit_empty_fields=explicit_empty_fields,
        )
        for field, values in field_differences.items()
    ):
        return "STRUCTURED_SOURCE_CORRECTION"

    return "MANUAL_REVIEW_REQUIRED"


def merge_verified_overrides(
    parsed: dict[str, dict[str, Any]],
    overrides: dict[str, dict[str, Any]],
) -> tuple[dict[str, dict[str, Any]], list[dict[str, Any]]]:
    """Keep VERIFIED only when the current extraction still proves the stored record."""
    review_differences: list[dict[str, Any]] = []

    for code, override in overrides.items():
        fresh = parsed.get(code)
        if not fresh:
            candidate = dict(override)
            candidate["status"] = "PARTIAL_VALIDATION"
            parsed[code] = candidate
            review_differences.append({
                "code": code,
                "reason": "MISSING_FROM_FRESH_EXTRACTION",
                "fieldDifferences": {},
                "semanticIssues": [],
            })
            continue

        field_differences = verified_record_differences(fresh, override)
        semantic_issues = semantic_record_issues(fresh)
        if field_differences or semantic_issues:
            fresh["status"] = (
                "MANUAL_REVIEW_REQUIRED"
                if semantic_issues
                else "PARTIAL_VALIDATION"
            )
            parsed[code] = fresh
            review_differences.append({
                "code": code,
                "reason": "DIFF_REVIEW_REQUIRED",
                "fieldDifferences": field_differences,
                "semanticIssues": semantic_issues,
                "storedSemanticIssues": semantic_record_issues(override),
            })
            continue

        verified = dict(fresh)
        verified["status"] = "VERIFIED"
        parsed[code] = verified

    return parsed, review_differences


def attach_exact_match_verification(
    records: dict[str, dict[str, Any]],
    *,
    source_sha256: str,
    source_document: str,
    verified_at: str,
) -> None:
    for code, record in records.items():
        if record.get("status") != "VERIFIED" or record.get("verification"):
            continue
        record["verification"] = {
            "state": "MATCH",
            "verifiedAt": verified_at,
            "verifiedAgainstSourceSha256": source_sha256,
            "verifiedSourceDocument": source_document,
            "verificationMethod": "DETERMINISTIC_SOURCE_REEXTRACTION_MATCH",
            "reviewRecordId": f"source-match:{verified_at}:{code}",
            "reviewedFields": list(VERIFICATION_COMPARE_FIELDS),
            "reviewReason": (
                "Stored VERIFIED content matches the current normalized extraction "
                "from the official MBFT source."
            ),
            "reviewEvidence": (
                f"{source_document}; SHA-256 {source_sha256}; "
                f"source pages {record.get('sourcePages') or []}."
            ),
        }


def attach_monotonic_recovery_verification(
    records: dict[str, dict[str, Any]],
    review_differences: list[dict[str, Any]],
    *,
    source_sha256: str,
    source_document: str,
    verified_at: str,
) -> None:
    """Promote only strict source recoveries that add/complete stored content."""
    by_code = {
        str(item.get("code")): item
        for item in review_differences
        if item.get("classification") == "MONOTONIC_SOURCE_RECOVERY"
    }
    for code, item in by_code.items():
        record = records.get(code)
        if not record or record.get("status") != "PARTIAL_VALIDATION":
            continue
        if item.get("semanticIssues"):
            continue

        field_differences = item.get("fieldDifferences") or {}
        reviewed_fields = sorted(field_differences)
        if not reviewed_fields:
            continue
        if not all(
            _is_monotonic_source_recovery(
                values.get("stored"),
                values.get("fresh"),
            )
            for values in field_differences.values()
        ):
            continue

        record["status"] = "VERIFIED"
        record["verification"] = {
            "state": "CORRECTED",
            "verifiedAt": verified_at,
            "verifiedAgainstSourceSha256": source_sha256,
            "verifiedSourceDocument": source_document,
            "verificationMethod": "DETERMINISTIC_MONOTONIC_SOURCE_RECOVERY",
            "reviewRecordId": f"source-recovery:{verified_at}:{code}",
            "reviewedFields": reviewed_fields,
            "reviewReason": (
                "Fresh structured extraction from the current official MBFT "
                "strictly completes or extends the previously stored content "
                "without removing reviewed meaning."
            ),
            "reviewEvidence": (
                f"{source_document}; SHA-256 {source_sha256}; "
                f"source pages {record.get('sourcePages') or []}; "
                f"recovered fields {', '.join(reviewed_fields)}."
            ),
        }


def attach_structural_recovery_verification(
    records: dict[str, dict[str, Any]],
    review_differences: list[dict[str, Any]],
    *,
    source_sha256: str,
    source_document: str,
    verified_at: str,
) -> None:
    """Promote clean fresh fields replacing known structural corruption."""
    for item in review_differences:
        if item.get("classification") != "STRUCTURAL_SOURCE_RECOVERY":
            continue
        code = str(item.get("code") or "")
        record = records.get(code)
        if not record or record.get("status") != "PARTIAL_VALIDATION":
            continue
        if semantic_record_issues(record):
            continue

        field_differences = item.get("fieldDifferences") or {}
        reviewed_fields = sorted(field_differences)
        stored_corrupt_fields = _semantic_issue_fields(
            item.get("storedSemanticIssues") or []
        )
        if not reviewed_fields or not all(
            field in stored_corrupt_fields
            or _is_monotonic_source_recovery(
                values.get("stored"),
                values.get("fresh"),
            )
            for field, values in field_differences.items()
        ):
            continue

        record["status"] = "VERIFIED"
        record["verification"] = {
            "state": "CORRECTED",
            "verifiedAt": verified_at,
            "verifiedAgainstSourceSha256": source_sha256,
            "verifiedSourceDocument": source_document,
            "verificationMethod": "DETERMINISTIC_STRUCTURAL_SOURCE_RECOVERY",
            "reviewRecordId": f"structural-recovery:{verified_at}:{code}",
            "reviewedFields": reviewed_fields,
            "reviewReason": (
                "Fresh structured extraction replaces fields whose stored "
                "values contained deterministic structural corruption."
            ),
            "reviewEvidence": (
                f"{source_document}; SHA-256 {source_sha256}; "
                f"source pages {record.get('sourcePages') or []}; "
                f"repaired fields {', '.join(reviewed_fields)}; "
                f"stored issues: {' | '.join(item.get('storedSemanticIssues') or [])}."
            ),
        }


def attach_structured_source_correction_verification(
    records: dict[str, dict[str, Any]],
    review_differences: list[dict[str, Any]],
    *,
    source_sha256: str,
    source_document: str,
    verified_at: str,
) -> None:
    """Promote non-empty differences proven by direct official table anchors."""
    for item in review_differences:
        if item.get("classification") != "STRUCTURED_SOURCE_CORRECTION":
            continue
        code = str(item.get("code") or "")
        record = records.get(code)
        if not record or record.get("status") != "PARTIAL_VALIDATION":
            continue
        if semantic_record_issues(record):
            continue

        field_differences = item.get("fieldDifferences") or {}
        structured_fields = set(item.get("structuredSourceFields") or [])
        explicit_empty_fields = set(item.get("explicitEmptySourceFields") or [])
        reviewed_fields = sorted(field_differences)
        if not reviewed_fields or not all(
            field in structured_fields
            and _safe_structured_source_correction(
                field,
                values.get("fresh"),
                explicit_empty_fields=explicit_empty_fields,
            )
            for field, values in field_differences.items()
        ):
            continue

        record["status"] = "VERIFIED"
        record["verification"] = {
            "state": "CORRECTED",
            "verifiedAt": verified_at,
            "verifiedAgainstSourceSha256": source_sha256,
            "verifiedSourceDocument": source_document,
            "verificationMethod": "DETERMINISTIC_STRUCTURED_SOURCE_CORRECTION",
            "reviewRecordId": f"structured-source:{verified_at}:{code}",
            "reviewedFields": reviewed_fields,
            "reviewReason": (
                "Current official MBFT table directly anchors every corrected "
                "field to its scalar label or physical operational column."
            ),
            "reviewEvidence": (
                f"{source_document}; SHA-256 {source_sha256}; "
                f"source pages {record.get('sourcePages') or []}; "
                f"anchored fields {', '.join(reviewed_fields)}."
            ),
        }


VALID_VERIFICATION_STATES = {
    "MATCH",
    "CORRECTED",
    "EXPLICIT_OVERRIDE",
    "SOURCE_CONFLICT",
}


def verification_evidence_issues(record: dict[str, Any]) -> list[str]:
    if record.get("status") not in {"VERIFIED", "SOURCE_CONFLICT"}:
        return []

    code = str(record.get("code") or "UNKNOWN")
    verification = record.get("verification")
    if not isinstance(verification, dict):
        return [f"{code}: VERIFIED sem prova de verificação."]

    required = (
        "state",
        "verifiedAt",
        "verifiedAgainstSourceSha256",
        "verifiedSourceDocument",
        "verificationMethod",
        "reviewRecordId",
        "reviewedFields",
        "reviewReason",
        "reviewEvidence",
    )
    missing = [field for field in required if not verification.get(field)]
    issues = [
        f"{code}: prova VERIFIED incompleta: {', '.join(missing)}."
    ] if missing else []

    state = str(verification.get("state") or "")
    if state not in VALID_VERIFICATION_STATES:
        issues.append(
            f"{code}: estado final de verificação não aceito: {state or 'EMPTY'}."
        )

    status = record.get("status")
    conflicts = record.get("sourceConflicts") or []
    if status == "SOURCE_CONFLICT":
        if state != "SOURCE_CONFLICT":
            issues.append(
                f"{code}: SOURCE_CONFLICT exige prova com state SOURCE_CONFLICT."
            )
        if not isinstance(conflicts, list) or not conflicts:
            issues.append(
                f"{code}: SOURCE_CONFLICT sem descrição explícita da divergência."
            )
    elif state == "SOURCE_CONFLICT":
        issues.append(
            f"{code}: prova SOURCE_CONFLICT exige status SOURCE_CONFLICT."
        )

    digest = str(verification.get("verifiedAgainstSourceSha256") or "")
    if digest and not re.fullmatch(r"[A-F0-9]{64}", digest):
        issues.append(f"{code}: hash da prova VERIFIED não é SHA-256 válido.")

    reviewed_fields = verification.get("reviewedFields")
    if not isinstance(reviewed_fields, list) or not reviewed_fields:
        issues.append(f"{code}: VERIFIED sem lista de campos revisados.")

    return issues


def apply_amendment_pdfs(
    records: dict[str, dict[str, Any]],
    manifest: dict[str, Any],
    amendment_dir: Path | None,
) -> tuple[
    dict[str, dict[str, Any]],
    list[str],
    dict[str, int],
    list[str],
]:
    if not amendment_dir:
        return records, [], {}, []

    applied: list[str] = []
    changed_counts: dict[str, int] = {}
    unparsed: list[str] = []

    for amendment in manifest.get("amendments", []):
        if amendment.get("status") != "PATCH_REQUIRED":
            continue

        if amendment.get("extractionMode") == "MANUAL_PATCH":
            continue

        file_key = amendment.get("fileKey")
        if not file_key:
            unparsed.append(amendment.get("resolution", "UNKNOWN"))
            continue

        path = amendment_dir / f"{file_key}.pdf"
        if not path.exists():
            unparsed.append(amendment["resolution"])
            continue

        sections, _ = read_pdf_sections(path)
        count = 0
        for section in sections:
            record = parse_record(section, manifest, source=amendment)
            records[record["code"]] = record
            count += 1

        if count == 0:
            # Some CONTRAN amendment annexes are raster images without a
            # searchable text layer. Keep the base dataset usable and report
            # the amendment for a curated patch instead of aborting staging.
            unparsed.append(amendment["resolution"])
            changed_counts[amendment["resolution"]] = 0
            continue

        applied.append(amendment["resolution"])
        changed_counts[amendment["resolution"]] = count

    return records, applied, changed_counts, unparsed


def apply_patch_files(
    records: dict[str, dict[str, Any]],
    patch_dir: Path | None,
    *,
    base_source_sha256: str | None = None,
    base_source_document: str | None = None,
    verified_at: str | None = None,
    amendment_hashes: dict[str, str] | None = None,
    review_differences: list[dict[str, Any]] | None = None,
    deferred_review_issues: list[dict[str, Any]] | None = None,
) -> tuple[dict[str, dict[str, Any]], list[str]]:
    if not patch_dir or not patch_dir.exists():
        return records, []

    applied: list[str] = []
    amendment_hashes = amendment_hashes or {}
    deferred_review_issues = deferred_review_issues if deferred_review_issues is not None else []
    review_differences_by_code = {
        str(item.get("code")): item
        for item in (review_differences or [])
        if item.get("code")
    }
    deferred_source_conflicts: list[tuple[Path, str, dict[str, Any]]] = []
    patchable_fields = set(VERIFICATION_COMPARE_FIELDS) | {
        "legalBasis",
        "related",
        "sources",
    }

    for path in sorted(patch_dir.glob("*.json")):
        patch = load_json(path)
        resolution = patch.get("resolution", path.stem)
        is_baseline = resolution == "BASELINE"

        for code in patch.get("removeCodes", []):
            records.pop(code, None)

        # Full upserts remain supported for genuinely new/removed official
        # records (for example an amending resolution). Baseline repairs
        # should prefer recordPatches so clean fresh extraction is preserved.
        for raw_record in patch.get("upserts", []):
            record = dict(raw_record)
            if record.get("status") == "VERIFIED" and not record.get("verification"):
                if is_baseline:
                    source_hash = base_source_sha256
                    source_document = base_source_document
                    method = "AUDITED_BASELINE_FULL_UPSERT"
                else:
                    source_hash = amendment_hashes.get(str(resolution))
                    source_document = str(resolution)
                    method = "AUDITED_AMENDMENT_PATCH"

                if not source_hash or not source_document or not verified_at:
                    raise ValueError(
                        f"{path.name}: VERIFIED full upsert lacks source proof for "
                        f"{record.get('code')}."
                    )
                record["verification"] = {
                    "state": "EXPLICIT_OVERRIDE",
                    "verifiedAt": verified_at,
                    "verifiedAgainstSourceSha256": source_hash,
                    "verifiedSourceDocument": source_document,
                    "verificationMethod": method,
                    "reviewRecordId": f"{path.stem}:{record.get('code')}",
                    "reviewedFields": list(VERIFICATION_COMPARE_FIELDS),
                    "reviewReason": (
                        "Curated full record retained from an audited official "
                        "MBFT amendment or baseline recovery."
                    ),
                    "reviewEvidence": (
                        f"{source_document}; official source SHA-256 {source_hash}."
                    ),
                }
            records[record["code"]] = record

        source_conflicts = patch.get("sourceConflicts", {})
        if source_conflicts and not isinstance(source_conflicts, dict):
            raise ValueError(f"{path.name}: sourceConflicts must be an object.")
        for code, specification in source_conflicts.items():
            if not isinstance(specification, dict):
                raise ValueError(
                    f"{path.name}: source conflict for {code} must be an object."
                )
            deferred_source_conflicts.append((path, str(code), specification))

        record_patches = patch.get("recordPatches", {})
        if record_patches and not isinstance(record_patches, dict):
            raise ValueError(f"{path.name}: recordPatches must be an object.")

        for code, specification in record_patches.items():
            if code not in records:
                raise ValueError(f"{path.name}: patch targets missing code {code}.")
            if not isinstance(specification, dict):
                raise ValueError(f"{path.name}: {code} patch must be an object.")

            fields = specification.get("fields", {})
            if not isinstance(fields, dict) or not fields:
                raise ValueError(f"{path.name}: {code} patch has no fields.")
            unsupported = sorted(set(fields) - patchable_fields)
            if unsupported:
                raise ValueError(
                    f"{path.name}: {code} unsupported patch field(s): "
                    + ", ".join(unsupported)
                )

            record = dict(records[code])
            for field, value in fields.items():
                record[field] = value

            verification = specification.get("verification")
            if not isinstance(verification, dict):
                raise ValueError(
                    f"{path.name}: {code} field patch requires verification evidence."
                )
            required_evidence = (
                "state",
                "verifiedAt",
                "verifiedAgainstSourceSha256",
                "verifiedSourceDocument",
                "verificationMethod",
                "reviewRecordId",
                "reviewedFields",
                "reviewReason",
                "reviewEvidence",
            )
            missing_evidence = [
                field
                for field in required_evidence
                if not verification.get(field)
            ]
            if missing_evidence:
                raise ValueError(
                    f"{path.name}: {code} missing verification evidence: "
                    + ", ".join(missing_evidence)
                )

            reviewed_fields = set(verification.get("reviewedFields", []))
            if not set(fields).issubset(reviewed_fields):
                raise ValueError(
                    f"{path.name}: {code} patched fields must be included in reviewedFields."
                )

            review_diff = review_differences_by_code.get(code, {})
            unresolved_fields = set(
                (review_diff.get("fieldDifferences") or {}).keys()
            )
            for issue in review_diff.get("semanticIssues") or []:
                match = re.match(
                    rf"{re.escape(code)}:\s*([A-Za-z][A-Za-z0-9]*)",
                    str(issue),
                )
                if match:
                    unresolved_fields.add(match.group(1))

            if not unresolved_fields.issubset(reviewed_fields):
                missing_review = sorted(unresolved_fields - reviewed_fields)
                fresh_record = dict(records[code])
                fresh_record["status"] = "MANUAL_REVIEW_REQUIRED"
                fresh_record.pop("verification", None)
                records[code] = fresh_record
                deferred_review_issues.append({
                    "code": code,
                    "patchFile": path.name,
                    "reason": "STALE_REVIEW_COVERAGE",
                    "missingReviewedFields": missing_review,
                })
                continue

            record["verification"] = verification
            record["status"] = "VERIFIED"
            records[code] = record

        record_approvals = patch.get("recordApprovals", {})
        if record_approvals and not isinstance(record_approvals, dict):
            raise ValueError(f"{path.name}: recordApprovals must be an object.")

        for code, specification in record_approvals.items():
            if code not in records:
                raise ValueError(f"{path.name}: approval targets missing code {code}.")
            if not isinstance(specification, dict):
                raise ValueError(f"{path.name}: {code} approval must be an object.")

            verification = specification.get("verification")
            if not isinstance(verification, dict):
                raise ValueError(
                    f"{path.name}: {code} approval requires verification evidence."
                )
            required_evidence = (
                "state",
                "verifiedAt",
                "verifiedAgainstSourceSha256",
                "verifiedSourceDocument",
                "verificationMethod",
                "reviewRecordId",
                "reviewedFields",
                "reviewReason",
                "reviewEvidence",
            )
            missing_evidence = [
                field for field in required_evidence if not verification.get(field)
            ]
            if missing_evidence:
                raise ValueError(
                    f"{path.name}: {code} approval evidence missing: "
                    + ", ".join(missing_evidence)
                )

            review_diff = review_differences_by_code.get(str(code), {})
            unresolved_fields = set(
                (review_diff.get("fieldDifferences") or {}).keys()
            )
            for issue in review_diff.get("semanticIssues") or []:
                match = re.match(
                    rf"{re.escape(str(code))}:\s*([A-Za-z][A-Za-z0-9]*)",
                    str(issue),
                )
                if match:
                    unresolved_fields.add(match.group(1))

            if unresolved_fields:
                fresh_record = dict(records[code])
                fresh_record["status"] = "MANUAL_REVIEW_REQUIRED"
                fresh_record.pop("verification", None)
                records[code] = fresh_record
                deferred_review_issues.append({
                    "code": str(code),
                    "patchFile": path.name,
                    "reason": "APPROVAL_CANNOT_RESOLVE_SOURCE_DIFF",
                    "unresolvedFields": sorted(unresolved_fields),
                })
                continue

            record = dict(records[code])
            if semantic_record_issues(record):
                raise ValueError(
                    f"{path.name}: {code} approval cannot promote structurally "
                    "invalid fresh extraction."
                )
            record["verification"] = verification
            record["status"] = "VERIFIED"
            records[code] = record

        if not is_baseline:
            applied.append(str(resolution))

    for path, code, specification in deferred_source_conflicts:
        if code not in records:
            raise ValueError(f"{path.name}: source conflict targets missing code {code}.")
        conflicts = specification.get("conflicts")
        verification = specification.get("verification")
        if not isinstance(conflicts, list) or not conflicts:
            raise ValueError(f"{path.name}: {code} source conflict list is empty.")
        if not isinstance(verification, dict):
            raise ValueError(
                f"{path.name}: {code} source conflict requires verification evidence."
            )
        required_evidence = (
            "state",
            "verifiedAt",
            "verifiedAgainstSourceSha256",
            "verifiedSourceDocument",
            "verificationMethod",
            "reviewRecordId",
            "reviewedFields",
            "reviewReason",
            "reviewEvidence",
        )
        missing_evidence = [
            field for field in required_evidence if not verification.get(field)
        ]
        if missing_evidence:
            raise ValueError(
                f"{path.name}: {code} source conflict evidence missing: "
                + ", ".join(missing_evidence)
            )

        record = dict(records[code])
        record["sourceConflicts"] = conflicts
        record["status"] = "SOURCE_CONFLICT"
        record["verification"] = verification
        records[code] = record

    return records, applied


def quality_report(records: list[dict[str, Any]]) -> dict[str, Any]:
    fields = [
        "article",
        "shortDescription",
        "description",
        "severity",
        "offenderResponsibility",
        "authority",
        "verificationMethod",
        "penalty",
        "whenToCite",
        "whenNotToCite",
        "procedures",
        "aitObservations",
    ]
    total = max(len(records), 1)
    completeness = {
        field: round(sum(1 for record in records if record.get(field)) / total, 4)
        for field in fields
    }
    status_counts = Counter(record.get("status", "") for record in records)
    return {
        "recordCount": len(records),
        "statusCounts": dict(sorted(status_counts.items())),
        "completeness": completeness,
    }


def validate_dataset(dataset: dict[str, Any], min_records: int = 1) -> list[str]:
    errors: list[str] = []
    records = dataset.get("records")
    if not isinstance(records, list):
        return ["Campo records ausente ou inválido."]

    if len(records) < min_records:
        errors.append(f"Dataset possui {len(records)} fichas; mínimo exigido: {min_records}.")

    codes = [record.get("code") for record in records]
    duplicates = sorted(
        code for code, count in Counter(codes).items() if code and count > 1
    )
    if duplicates:
        errors.append("Códigos duplicados: " + ", ".join(duplicates))

    valid_status = {
        "VERIFIED",
        "PARTIAL_VALIDATION",
        "MANUAL_REVIEW_REQUIRED",
        "SOURCE_CONFLICT",
    }
    for index, record in enumerate(records, start=1):
        code = record.get("code") or f"registro #{index}"
        if not CODE_RE.fullmatch(str(record.get("code", ""))):
            errors.append(f"{code}: código fora do padrão 999-99.")
        if record.get("status") not in valid_status:
            errors.append(f"{code}: status inválido.")
        for field in ("article", "shortDescription", "description", "severity"):
            if not str(record.get(field) or "").strip():
                errors.append(f"{code}: campo obrigatório vazio: {field}.")
        for field in (
            "severity",
            "offenderResponsibility",
            "authority",
            "verificationMethod",
            "penalty",
            "administrativeMeasure",
        ):
            value = str(record.get(field) or "").strip()
            if value and SCALAR_FIELD_LABEL_RE.search(value):
                errors.append(
                    f"{code}: campo {field} contém rótulo de outra coluna do MBFT."
                )
        sources = record.get("sources")
        if not isinstance(sources, list) or not sources:
            errors.append(f"{code}: nenhuma fonte oficial associada.")
        errors.extend(semantic_record_issues(record))
        errors.extend(verification_evidence_issues(record))
    return errors


def pending_amendments(
    manifest: dict[str, Any],
    applied: list[str],
) -> list[str]:
    return [
        item["resolution"]
        for item in manifest.get("amendments", [])
        if item.get("status") == "PATCH_REQUIRED"
        and item["resolution"] not in applied
    ]


def command_extract(args: argparse.Namespace) -> int:
    manifest = load_json(args.manifest)
    sections, pdf_sha256 = read_pdf_sections(args.pdf)
    ctb_records: list[dict[str, Any]] = []
    if args.ctb_dataset:
        ctb_records = load_json(args.ctb_dataset).get("records", [])

    parsed: dict[str, dict[str, Any]] = {}
    source_fields_by_code: dict[str, set[str]] = {}
    explicit_empty_fields_by_code: dict[str, set[str]] = {}
    orphan_article_repairs: list[str] = []
    duplicate_sections: list[str] = []
    for section in sections:
        record = parse_record(section, manifest)
        if ctb_records:
            orphan_article_repairs.extend(
                repair_orphan_article_procedures(record, ctb_records)
            )
        code = record["code"]
        if code in parsed:
            duplicate_sections.append(code)
        parsed[code] = record
        source_fields_by_code[code] = structured_source_fields(section)
        explicit_empty_fields_by_code[code] = explicit_empty_operational_fields(section)

    overrides = verified_overrides(args.verified_overrides)
    parsed, verified_override_diffs = merge_verified_overrides(parsed, overrides)
    for review_item in verified_override_diffs:
        review_item["structuredSourceFields"] = sorted(
            source_fields_by_code.get(str(review_item.get("code")), set())
        )
        review_item["explicitEmptySourceFields"] = sorted(
            explicit_empty_fields_by_code.get(
                str(review_item.get("code")),
                set(),
            )
        )
        review_item["classification"] = classify_review_difference(review_item)
    attach_exact_match_verification(
        parsed,
        source_sha256=pdf_sha256,
        source_document=manifest["document"],
        verified_at=manifest["lastCheckedAt"],
    )
    attach_monotonic_recovery_verification(
        parsed,
        verified_override_diffs,
        source_sha256=pdf_sha256,
        source_document=manifest["document"],
        verified_at=manifest["lastCheckedAt"],
    )
    attach_structural_recovery_verification(
        parsed,
        verified_override_diffs,
        source_sha256=pdf_sha256,
        source_document=manifest["document"],
        verified_at=manifest["lastCheckedAt"],
    )
    attach_structured_source_correction_verification(
        parsed,
        verified_override_diffs,
        source_sha256=pdf_sha256,
        source_document=manifest["document"],
        verified_at=manifest["lastCheckedAt"],
    )

    (
        parsed,
        automatic_amendments,
        amendment_counts,
        unparsed_amendments,
    ) = apply_amendment_pdfs(
        parsed,
        manifest,
        args.amendment_dir,
    )
    source_hashes = amendment_source_hashes(manifest, args.amendment_dir)
    deferred_patch_reviews: list[dict[str, Any]] = []
    parsed, manual_patches = apply_patch_files(
        parsed,
        args.patch_dir,
        base_source_sha256=pdf_sha256,
        base_source_document=manifest["document"],
        verified_at=manifest["lastCheckedAt"],
        amendment_hashes=source_hashes,
        review_differences=verified_override_diffs,
        deferred_review_issues=deferred_patch_reviews,
    )
    applied_patches = automatic_amendments + manual_patches

    final_resolution_counts: Counter[str] = Counter()
    unresolved_differences: list[str] = []
    for review_item in verified_override_diffs:
        code = str(review_item.get("code") or "")
        record = parsed.get(code, {})
        verification = record.get("verification")
        final_resolution = (
            verification.get("state")
            if isinstance(verification, dict)
            else None
        )
        review_item["finalResolution"] = final_resolution or "UNRESOLVED"
        final_resolution_counts[review_item["finalResolution"]] += 1
        if final_resolution not in VALID_VERIFICATION_STATES:
            unresolved_differences.append(code)

    records = sorted(parsed.values(), key=lambda item: item["code"])
    dataset = {
        "version": args.version or f"{date.today().isoformat()}-generated",
        "generatedAt": date.today().isoformat(),
        "verifiedAt": manifest["lastCheckedAt"],
        "sourceDocument": manifest["document"],
        "sourceUrl": manifest["base"]["officialUrl"],
        "sourceSha256": pdf_sha256,
        "knownAmendments": [
            item["resolution"] for item in manifest.get("amendments", [])
        ],
        "appliedAmendments": applied_patches,
        "records": records,
    }

    errors = validate_dataset(dataset, min_records=args.min_records)
    if unresolved_differences:
        errors.append(
            "Divergências VERIFIED sem resolução final aceita: "
            + ", ".join(sorted(code for code in unresolved_differences if code))
        )
    report = quality_report(records)
    report.update({
        "pdfSha256": pdf_sha256,
        "rawSections": len(sections),
        "duplicateSections": sorted(set(duplicate_sections)),
        "verifiedOverrides": len(overrides),
        "verifiedOverrideDiffCount": len(verified_override_diffs),
        "verifiedOverrideDiffs": verified_override_diffs,
        "verifiedOverrideDiffClassCounts": dict(sorted(Counter(
            item.get("classification", "UNCLASSIFIED")
            for item in verified_override_diffs
        ).items())),
        "verifiedOverrideDiffResolutionCounts": dict(
            sorted(final_resolution_counts.items())
        ),
        "unresolvedVerifiedOverrideDiffs": sorted(
            code for code in unresolved_differences if code
        ),
        "appliedAmendments": applied_patches,
        "amendmentRecordCounts": amendment_counts,
        "unparsedAmendments": unparsed_amendments,
        "pendingAmendments": pending_amendments(manifest, applied_patches),
        "amendmentSourceSha256": source_hashes,
        "orphanArticleRepairs": orphan_article_repairs,
        "deferredManualPatchReviews": deferred_patch_reviews,
        "layoutDebug": {
            section["code"]: section.get("tables", [])
            for section in sections
            if section["code"] in {
                "501-00", "502-91", "502-92", "506-10", "520-70",
                "541-00", "554-12", "585-11", "594-01", "601-75",
                "608-41", "625-40", "640-80", "641-60", "701-31",
                "703-03", "718-81", "721-80", "776-50"
            }
        },
    })

    for field in (
        "article",
        "shortDescription",
        "description",
        "severity",
        "offenderResponsibility",
        "authority",
        "verificationMethod",
        "penalty",
    ):
        ratio = report["completeness"].get(field, 0.0)
        if ratio < args.min_core_completeness:
            errors.append(
                f"Completude de {field}: {ratio:.1%}; mínimo exigido: "
                f"{args.min_core_completeness:.1%}."
            )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(dataset, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        print("\nERROS DE VALIDAÇÃO:", file=sys.stderr)
        for error in errors[:100]:
            print(f"- {error}", file=sys.stderr)
        if len(errors) > 100:
            print(f"- ... mais {len(errors) - 100} erro(s)", file=sys.stderr)
        return 1
    return 0


def command_validate(args: argparse.Namespace) -> int:
    dataset = load_json(args.dataset)
    errors = validate_dataset(dataset, min_records=args.min_records)
    print(
        json.dumps(
            quality_report(dataset.get("records", [])),
            ensure_ascii=False,
            indent=2,
        )
    )
    if errors:
        print("\nERROS DE VALIDAÇÃO:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    return 0


def build_parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description="Pipeline do dataset MBFT")
    sub = root.add_subparsers(dest="command", required=True)

    extract = sub.add_parser("extract", help="Extrai as fichas do PDF oficial")
    extract.add_argument("--pdf", type=Path, required=True)
    extract.add_argument("--manifest", type=Path, required=True)
    extract.add_argument("--ctb-dataset", type=Path)
    extract.add_argument("--verified-overrides", type=Path)
    extract.add_argument("--amendment-dir", type=Path)
    extract.add_argument("--patch-dir", type=Path)
    extract.add_argument("--output", type=Path, required=True)
    extract.add_argument("--report", type=Path)
    extract.add_argument("--version")
    extract.add_argument("--min-records", type=int, default=400)
    extract.add_argument("--min-core-completeness", type=float, default=0.90)
    extract.set_defaults(func=command_extract)

    validate = sub.add_parser("validate", help="Valida um dataset já gerado")
    validate.add_argument("--dataset", type=Path, required=True)
    validate.add_argument("--min-records", type=int, default=1)
    validate.set_defaults(func=command_validate)
    return root


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
