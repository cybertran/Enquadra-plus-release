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
VERIFIED_ENRICHABLE_FIELDS = (
    "whenToCite",
    "whenNotToCite",
    "procedures",
    "aitObservations",
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
        field_label_pattern(label) + r"\s*:?\s*",
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
        field_label_pattern(label) + r"\s*:?\s*",
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

def section_field(section: dict[str, Any], label: str) -> str | None:
    value = table_field(section.get("tables", []), label)
    if value:
        return value
    value = cell_field(section["cells"], label)
    if value:
        return value
    return extract_field_from_text(section["text"], label)


def split_numbered(value: str | None) -> list[str]:
    value = compact(value)
    if not value:
        return []
    pieces = re.split(r"(?<!\d)\s+(?=\d{1,2}[\.)]\s+)", value)
    cleaned: list[str] = []
    seen: set[str] = set()
    for piece in pieces:
        piece = re.sub(r"^\d{1,2}[\.)]\s*", "", piece).strip()
        if piece and piece not in seen:
            seen.add(piece)
            cleaned.append(piece)
    return cleaned or [value]


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
    return (compact(value) or "").upper().startswith("SIM")


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
    operational = operational_table_fields(section.get("tables", []))

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
        if re.search(r"C[oó]digo\s+do\s+Enquadramento", cell, re.IGNORECASE):
            match = CODE_RE.search(cell)
            if match:
                return match.group(0)

    match = re.search(
        r"C[oó]digo\s+do\s+Enquadramento\s*:?\s*(\d{3}-\d{2})",
        text,
        re.IGNORECASE,
    )
    return match.group(1) if match else None


def read_pdf_sections(pdf_path: Path) -> tuple[list[dict[str, Any]], str]:
    try:
        import pdfplumber
    except ImportError as exc:
        raise SystemExit("Dependência ausente: instale tools/mbft/requirements.txt") from exc

    sections: list[dict[str, Any]] = []
    current: dict[str, Any] | None = None

    with pdfplumber.open(str(pdf_path)) as pdf:
        for page in pdf.pages:
            text = normalize_text(page.extract_text(layout=True) or "")
            tables = page_tables(page)
            cells = table_cells(tables)
            code = find_page_code(text, cells)

            if code:
                if current and current["code"] == code:
                    current["texts"].append(text)
                    current["cells"].extend(cells)
                    current["tables"].extend(tables)
                    continue

                if current:
                    current["text"] = "\n".join(current.pop("texts"))
                    sections.append(current)

                current = {
                    "code": code,
                    "texts": [text],
                    "cells": list(cells),
                    "tables": list(tables),
                }
            elif current:
                current["texts"].append(text)
                current["cells"].extend(cells)
                current["tables"].extend(tables)

    if current:
        current["text"] = "\n".join(current.pop("texts"))
        sections.append(current)

    digest = hashlib.sha256(pdf_path.read_bytes()).hexdigest().upper()
    return sections, digest


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def verified_overrides(path: Path | None) -> dict[str, dict[str, Any]]:
    if not path or not path.exists():
        return {}
    dataset = load_json(path)
    return {
        record["code"]: record
        for record in dataset.get("records", [])
        if record.get("status") == "VERIFIED"
    }


def merge_verified_overrides(
    parsed: dict[str, dict[str, Any]],
    overrides: dict[str, dict[str, Any]],
) -> dict[str, dict[str, Any]]:
    """Preserve reviewed records while enriching fields that were previously empty."""
    for code, override in overrides.items():
        fresh = parsed.get(code)
        if not fresh:
            parsed[code] = override
            continue

        merged = dict(override)
        for field in VERIFIED_ENRICHABLE_FIELDS:
            if not merged.get(field) and fresh.get(field):
                merged[field] = fresh[field]
        parsed[code] = merged
    return parsed


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
) -> tuple[dict[str, dict[str, Any]], list[str]]:
    if not patch_dir or not patch_dir.exists():
        return records, []

    applied: list[str] = []
    for path in sorted(patch_dir.glob("*.json")):
        patch = load_json(path)
        resolution = patch.get("resolution", path.stem)
        is_baseline = resolution == "BASELINE"
        for code in patch.get("removeCodes", []):
            records.pop(code, None)
        for record in patch.get("upserts", []):
            records[record["code"]] = record
        if not is_baseline:
            applied.append(str(resolution))
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

    valid_status = {"VERIFIED", "PARTIAL_VALIDATION"}
    for index, record in enumerate(records, start=1):
        code = record.get("code") or f"registro #{index}"
        if not CODE_RE.fullmatch(str(record.get("code", ""))):
            errors.append(f"{code}: código fora do padrão 999-99.")
        if record.get("status") not in valid_status:
            errors.append(f"{code}: status inválido.")
        for field in ("article", "shortDescription", "description", "severity"):
            if not str(record.get(field) or "").strip():
                errors.append(f"{code}: campo obrigatório vazio: {field}.")
        sources = record.get("sources")
        if not isinstance(sources, list) or not sources:
            errors.append(f"{code}: nenhuma fonte oficial associada.")
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

    parsed: dict[str, dict[str, Any]] = {}
    duplicate_sections: list[str] = []
    for section in sections:
        record = parse_record(section, manifest)
        code = record["code"]
        if code in parsed:
            duplicate_sections.append(code)
        parsed[code] = record

    overrides = verified_overrides(args.verified_overrides)
    parsed = merge_verified_overrides(parsed, overrides)

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
    parsed, manual_patches = apply_patch_files(parsed, args.patch_dir)
    applied_patches = automatic_amendments + manual_patches

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
    report = quality_report(records)
    report.update({
        "pdfSha256": pdf_sha256,
        "rawSections": len(sections),
        "duplicateSections": sorted(set(duplicate_sections)),
        "verifiedOverrides": len(overrides),
        "appliedAmendments": applied_patches,
        "amendmentRecordCounts": amendment_counts,
        "unparsedAmendments": unparsed_amendments,
        "pendingAmendments": pending_amendments(manifest, applied_patches),
        "layoutDebug": {
            section["code"]: section.get("tables", [])
            for section in sections
            if section["code"] in {
                "502-91", "502-92", "506-10", "554-12",
                "701-31", "703-03", "718-81"
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
