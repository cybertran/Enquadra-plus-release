#!/usr/bin/env python3
"""Fail-closed comparison between official MBFT code headers and generated records."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

CODE_RE = re.compile(r"\b\d{3}-\d{2}\b")
HEADER_RE = re.compile(
    r"C[oó]digo(?:\s+(?P<connector>do|de))?\s+Enquadramento\s*:?\s*"
    r"(?P<code>\d{3}-\d{2})",
    re.IGNORECASE,
)


def normalize_text(value: str) -> str:
    value = value.replace("\u00ad", "").replace("\u00a0", " ")
    value = re.sub(r"[ \t]+", " ", value)
    value = re.sub(r"\s*\n\s*", "\n", value)
    return value.strip()


def extract_header_codes(text: str) -> list[tuple[str, str]]:
    """Return (code, connector) pairs from explicit MBFT code headers only."""
    normalized = normalize_text(text)
    return [
        (match.group("code"), (match.group("connector") or "none").lower())
        for match in HEADER_RE.finditer(normalized)
    ]


def read_official_codes(pdf_path: Path) -> tuple[set[str], dict[str, Any]]:
    try:
        import pdfplumber
    except ImportError as exc:
        raise SystemExit(
            "Dependência ausente: instale tools/mbft/requirements.txt"
        ) from exc

    codes: set[str] = set()
    variants: Counter[str] = Counter()
    first_page: dict[str, int] = {}

    with pdfplumber.open(str(pdf_path)) as pdf:
        for page_number, page in enumerate(pdf.pages, start=1):
            fragments: list[str] = [page.extract_text(layout=True) or ""]
            try:
                tables = page.extract_tables() or []
            except Exception:
                tables = []
            for table in tables:
                for row in table or []:
                    for cell in row or []:
                        if cell:
                            fragments.append(str(cell))

            page_hits = set(extract_header_codes("\n".join(fragments)))
            for code, connector in page_hits:
                codes.add(code)
                first_page.setdefault(code, page_number)
                variants[f"codigo_{connector}_enquadramento"] += 1

    return codes, {
        "pdfSha256": hashlib.sha256(pdf_path.read_bytes()).hexdigest().upper(),
        "headerVariants": dict(sorted(variants.items())),
        "firstPageByCode": dict(sorted(first_page.items())),
    }


def apply_patch_code_set(
    base_codes: set[str],
    patch_dir: Path | None,
) -> tuple[set[str], list[dict[str, Any]]]:
    expected = set(base_codes)
    applied: list[dict[str, Any]] = []
    if not patch_dir or not patch_dir.exists():
        return expected, applied

    for path in sorted(patch_dir.glob("*.json")):
        patch = json.loads(path.read_text(encoding="utf-8"))
        removed = [str(code) for code in patch.get("removeCodes", [])]
        added = [str(record.get("code", "")) for record in patch.get("upserts", [])]
        invalid = [code for code in removed + added if not CODE_RE.fullmatch(code)]
        if invalid:
            raise ValueError(
                f"{path.name}: código inválido no patch: {', '.join(invalid)}"
            )

        expected.difference_update(removed)
        expected.update(added)
        applied.append(
            {
                "file": path.name,
                "resolution": patch.get("resolution", path.stem),
                "removedCodes": sorted(removed),
                "addedCodes": sorted(added),
            }
        )

    return expected, applied


def read_generated_codes(dataset_path: Path) -> tuple[set[str], list[str]]:
    dataset = json.loads(dataset_path.read_text(encoding="utf-8"))
    records = dataset.get("records")
    if not isinstance(records, list):
        raise ValueError("Dataset gerado sem lista records.")

    raw_codes = [str(record.get("code", "")) for record in records]
    invalid = sorted(code for code in raw_codes if not CODE_RE.fullmatch(code))
    if invalid:
        raise ValueError(
            "Dataset gerado contém código fora do padrão: " + ", ".join(invalid[:20])
        )
    duplicates = sorted(
        code for code, count in Counter(raw_codes).items() if count > 1
    )
    return set(raw_codes), duplicates


def compare_code_sets(
    expected: set[str],
    generated: set[str],
) -> tuple[list[str], list[str]]:
    return sorted(expected - generated), sorted(generated - expected)


def build_report(
    base_codes: set[str],
    expected: set[str],
    generated: set[str],
    duplicates: list[str],
    metadata: dict[str, Any],
    patches: list[dict[str, Any]],
) -> dict[str, Any]:
    missing, unexpected = compare_code_sets(expected, generated)
    return {
        "baseOfficialCodeCount": len(base_codes),
        "expectedCodeCount": len(expected),
        "generatedCodeCount": len(generated),
        "pdfSha256": metadata["pdfSha256"],
        "headerVariants": metadata["headerVariants"],
        "appliedPatches": patches,
        "missingFromGenerated": missing,
        "unexpectedInGenerated": unexpected,
        "duplicateGeneratedCodes": duplicates,
        "matches": not missing and not unexpected and not duplicates,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Compara o conjunto de códigos encontrado diretamente nos cabeçalhos "
            "do MBFT oficial com o conjunto efetivamente gerado pelo pipeline."
        )
    )
    parser.add_argument("--pdf", type=Path, required=True)
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--patch-dir", type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--min-official-codes", type=int, default=400)
    args = parser.parse_args()

    base_codes, metadata = read_official_codes(args.pdf)
    expected, patches = apply_patch_code_set(base_codes, args.patch_dir)
    generated, duplicates = read_generated_codes(args.dataset)
    report = build_report(
        base_codes,
        expected,
        generated,
        duplicates,
        metadata,
        patches,
    )

    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    print(json.dumps(report, ensure_ascii=False, indent=2))

    errors: list[str] = []
    if len(base_codes) < args.min_official_codes:
        errors.append(
            "Extração independente dos cabeçalhos oficiais retornou apenas "
            f"{len(base_codes)} códigos; mínimo esperado: {args.min_official_codes}."
        )
    if report["missingFromGenerated"]:
        errors.append(
            "OFFICIAL_EXPECTED_CODES - GENERATED_CODES: "
            + ", ".join(report["missingFromGenerated"])
        )
    if report["unexpectedInGenerated"]:
        errors.append(
            "GENERATED_CODES - OFFICIAL_EXPECTED_CODES: "
            + ", ".join(report["unexpectedInGenerated"])
        )
    if duplicates:
        errors.append("Códigos duplicados no dataset: " + ", ".join(duplicates))

    if errors:
        print("\nERROS DE COBERTURA MBFT:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
