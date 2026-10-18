#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

CODE_RE = re.compile(r"^\d{3}-\d{2}$")
PATCHABLE_FIELDS = {"article", "legalBasis", "whenToCite", "whenNotToCite", "procedures", "aitObservations"}


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def promoted_codes(review: dict[str, Any]) -> list[str]:
    raw = review.get("promotedCodes")
    if not isinstance(raw, list):
        raise ValueError("Review must contain a promotedCodes array.")

    codes = [str(code) for code in raw]
    if len(codes) != len(set(codes)):
        raise ValueError("Review contains duplicate promoted codes.")

    invalid = [code for code in codes if not CODE_RE.fullmatch(code)]
    if invalid:
        raise ValueError("Invalid promoted code(s): " + ", ".join(invalid))
    return codes


def apply_review(dataset: dict[str, Any], review: dict[str, Any]) -> int:
    records = dataset.get("records")
    if not isinstance(records, list):
        raise ValueError("Dataset records field is missing or invalid.")

    codes = promoted_codes(review)
    remove_codes = [str(code) for code in review.get("removeCodes", [])]
    if len(remove_codes) != len(set(remove_codes)):
        raise ValueError("Review contains duplicate remove codes.")
    invalid_remove = [code for code in remove_codes if not CODE_RE.fullmatch(code)]
    if invalid_remove:
        raise ValueError("Invalid remove code(s): " + ", ".join(invalid_remove))
    overlap_removed = sorted(set(codes) & set(remove_codes))
    if overlap_removed:
        raise ValueError(
            "Codes cannot be both promoted and removed: " + ", ".join(overlap_removed)
        )

    review_date = str(review.get("reviewedAt") or "").strip()
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", review_date):
        raise ValueError("Review must contain reviewedAt in YYYY-MM-DD format.")

    held_codes = {
        str(item.get("code"))
        for item in review.get("held", [])
        if isinstance(item, dict) and item.get("code")
    }
    overlap = sorted(set(codes) & held_codes)
    if overlap:
        raise ValueError("Codes cannot be both promoted and held: " + ", ".join(overlap))

    by_code = {str(record.get("code")): record for record in records}
    missing = [code for code in codes if code not in by_code]
    if missing:
        raise ValueError("Promoted code(s) missing from dataset: " + ", ".join(missing))

    patches = review.get("recordPatches", {})
    if not isinstance(patches, dict):
        raise ValueError("recordPatches must be an object when present.")
    unknown_patch_codes = sorted(set(patches) - set(codes))
    if unknown_patch_codes:
        raise ValueError(
            "Record patches require promotion in the same review: "
            + ", ".join(unknown_patch_codes)
        )

    for code, patch in patches.items():
        if not isinstance(patch, dict):
            raise ValueError(f"{code}: record patch must be an object.")
        unsupported = sorted(set(patch) - PATCHABLE_FIELDS)
        if unsupported:
            raise ValueError(
                f"{code}: unsupported patch field(s): " + ", ".join(unsupported)
            )

    removed_set = set(remove_codes)
    if removed_set:
        dataset["records"] = [
            record
            for record in records
            if str(record.get("code")) not in removed_set
        ]

    changed = 0
    for code in codes:
        record = by_code[code]
        for field, value in patches.get(code, {}).items():
            record[field] = value
        status = record.get("status")
        if status not in {"PARTIAL_VALIDATION", "VERIFIED"}:
            raise ValueError(f"{code}: unexpected status {status!r}.")
        if status == "PARTIAL_VALIDATION":
            record["status"] = "VERIFIED"
            changed += 1

        for source in record.get("sources", []):
            if isinstance(source, dict):
                source["lastCheckedAt"] = review_date

    return changed


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Applies an auditable MBFT review to a dataset."
    )
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--review", type=Path, required=True)
    args = parser.parse_args()

    dataset = load_json(args.dataset)
    review = load_json(args.review)
    changed = apply_review(dataset, review)

    args.dataset.write_text(
        json.dumps(dataset, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Promoted {changed} record(s); review is idempotent.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
