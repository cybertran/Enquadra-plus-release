#!/usr/bin/env python3
"""Build and validate high-confidence infraction relations from the verified MBFT dataset."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

CODE_RE = re.compile(r"\b\d{3}-\d{2}\b")
CONCOMITANT_RE = re.compile(
    r"(?:utilizar|usar)[^.;:]{0,80}concomitantemente"
    r"|lavrar\s+tamb[eé]m(?:\s+outro)?\s+ait"
    r"|autuar\s+tamb[eé]m"
    r"|lavrar\s+outro\s+ait",
    re.IGNORECASE,
)
PREREQUISITE_RE = re.compile(
    r"(precedid[ao]|preceder|deve ser precedida)",
    re.IGNORECASE,
)

OFFICIAL_TYPES = {"ALTERNATIVE", "CONCOMITANT", "PREREQUISITE"}
ALL_TYPES = OFFICIAL_TYPES | {"RELATED"}


def classify(field: str, text: str) -> str | None:
    if field == "whenNotToCite":
        return "ALTERNATIVE"
    if field == "procedures" and CONCOMITANT_RE.search(text):
        return "CONCOMITANT"
    if field == "procedures" and PREREQUISITE_RE.search(text):
        return "PREREQUISITE"
    return None


def extract_target_codes(
    field: str,
    text: str,
    relation_type: str,
    valid_codes: set[str],
) -> list[str]:
    relevant_text = text
    if relation_type == "CONCOMITANT":
        match = CONCOMITANT_RE.search(text)
        if match is None:
            return []
        relevant_text = text[match.start():]
    elif relation_type == "PREREQUISITE":
        match = PREREQUISITE_RE.search(text)
        if match is None:
            return []
        relevant_text = text[match.start():]

    return sorted(
        {
            code
            for code in CODE_RE.findall(relevant_text)
            if code in valid_codes
        }
    )


def collect_unresolved_references(
    mbft: dict[str, Any],
) -> list[dict[str, Any]]:
    records = mbft.get("records") or []
    valid_codes = {record.get("code") for record in records}
    unresolved: list[dict[str, Any]] = []

    for record in records:
        if record.get("status") != "VERIFIED":
            continue
        source = record.get("code")

        for field in ("whenNotToCite", "procedures"):
            for evidence in record.get(field, []) or []:
                relation_type = classify(field, evidence)
                if relation_type is None:
                    continue

                relevant_text = evidence
                if relation_type == "CONCOMITANT":
                    match = CONCOMITANT_RE.search(evidence)
                    if match is None:
                        continue
                    relevant_text = evidence[match.start():]
                elif relation_type == "PREREQUISITE":
                    match = PREREQUISITE_RE.search(evidence)
                    if match is None:
                        continue
                    relevant_text = evidence[match.start():]

                missing = sorted(
                    {
                        code
                        for code in CODE_RE.findall(relevant_text)
                        if code not in valid_codes and code != source
                    }
                )
                if missing:
                    unresolved.append(
                        {
                            "sourceCode": source,
                            "relationType": relation_type,
                            "sourceField": field,
                            "unresolvedCodes": missing,
                            "evidence": evidence,
                        }
                    )

    return unresolved


def canonical_hash(records: list[dict[str, Any]]) -> str:
    payload = json.dumps(
        records,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest().upper()


def dataset_version(verified_at: str, content_sha256: str) -> str:
    return f"{verified_at}-relations-{content_sha256[:12].lower()}"


def build_dataset(mbft: dict[str, Any]) -> dict[str, Any]:
    records = mbft.get("records")
    if not isinstance(records, list) or not records:
        raise ValueError("MBFT dataset has no records.")

    by_code = {record["code"]: record for record in records}
    valid_codes = set(by_code)
    relation_map: dict[tuple[str, str, str], dict[str, Any]] = {}

    for record in records:
        if record.get("status") != "VERIFIED":
            continue
        source = record["code"]

        for field in ("whenNotToCite", "procedures"):
            for evidence in record.get(field, []) or []:
                relation_type = classify(field, evidence)
                if relation_type is None:
                    continue

                targets = [
                    code
                    for code in extract_target_codes(
                        field,
                        evidence,
                        relation_type,
                        valid_codes,
                    )
                    if code != source
                ]
                for target in targets:
                    key = (source, target, relation_type)
                    item = relation_map.setdefault(
                        key,
                        {
                            "sourceCode": source,
                            "targetCode": target,
                            "relationType": relation_type,
                            "origin": "MBFT_OFFICIAL_FIELD",
                            "sourceField": field,
                            "reason": None,
                            "evidence": [],
                        },
                    )
                    if evidence not in item["evidence"]:
                        item["evidence"].append(evidence)

    official_pairs = {
        (item["sourceCode"], item["targetCode"])
        for item in relation_map.values()
        if item["relationType"] in OFFICIAL_TYPES
    }

    for record in records:
        if record.get("status") != "VERIFIED":
            continue
        source = record["code"]
        for related in record.get("related", []) or []:
            target = related.get("code")
            if (
                target not in valid_codes
                or target == source
                or (source, target) in official_pairs
            ):
                continue
            key = (source, target, "RELATED")
            item = relation_map.setdefault(
                key,
                {
                    "sourceCode": source,
                    "targetCode": target,
                    "relationType": "RELATED",
                    "origin": "CURATED_PROJECT_RELATION",
                    "sourceField": "related",
                    "reason": related.get("reason"),
                    "evidence": [],
                },
            )
            if not item.get("reason") and related.get("reason"):
                item["reason"] = related["reason"]

    derived = sorted(
        relation_map.values(),
        key=lambda item: (
            item["sourceCode"],
            item["relationType"],
            item["targetCode"],
        ),
    )
    digest = canonical_hash(derived)
    verified_at = str(mbft.get("verifiedAt") or "")
    dataset = {
        "version": dataset_version(verified_at, digest),
        "generatedAt": verified_at,
        "verifiedAt": verified_at,
        "mbftVersion": mbft.get("version"),
        "sourceDocument": (
            "Relações derivadas das fichas verificadas do Manual Brasileiro "
            "de Fiscalização de Trânsito"
        ),
        "contentSha256": digest,
        "recordCount": len(derived),
        "records": derived,
    }

    errors = validate_dataset(dataset, mbft)
    if errors:
        raise ValueError("\n".join(errors))
    return dataset


def validate_dataset(
    dataset: dict[str, Any],
    mbft: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    relation_records = dataset.get("records")
    mbft_records = mbft.get("records")

    if not isinstance(relation_records, list):
        return ["relations records is missing or invalid."]
    if not isinstance(mbft_records, list):
        return ["MBFT records is missing or invalid."]

    codes = {record.get("code") for record in mbft_records}
    verified_codes = {
        record.get("code")
        for record in mbft_records
        if record.get("status") == "VERIFIED"
    }

    keys: list[tuple[str, str, str]] = []
    for item in relation_records:
        source = item.get("sourceCode")
        target = item.get("targetCode")
        relation_type = item.get("relationType")
        origin = item.get("origin")
        source_field = item.get("sourceField")
        evidence = item.get("evidence")

        key = (str(source), str(target), str(relation_type))
        keys.append(key)

        if source not in codes or target not in codes:
            errors.append(f"{key}: source or target code is missing from MBFT.")
        if source == target:
            errors.append(f"{key}: self relation is not allowed.")
        if relation_type not in ALL_TYPES:
            errors.append(f"{key}: invalid relation type.")
        if relation_type in OFFICIAL_TYPES:
            if source not in verified_codes:
                errors.append(f"{key}: official-derived source is not VERIFIED.")
            if origin != "MBFT_OFFICIAL_FIELD":
                errors.append(f"{key}: official relation has invalid origin.")
            if source_field not in {"whenNotToCite", "procedures"}:
                errors.append(f"{key}: official relation has invalid source field.")
            if not isinstance(evidence, list) or not evidence:
                errors.append(f"{key}: official relation has no evidence.")
            else:
                for line in evidence:
                    if target not in CODE_RE.findall(str(line)):
                        errors.append(
                            f"{key}: evidence does not explicitly mention target."
                        )
        elif relation_type == "RELATED":
            if origin != "CURATED_PROJECT_RELATION":
                errors.append(f"{key}: curated relation has invalid origin.")
            if not str(item.get("reason") or "").strip():
                errors.append(f"{key}: curated relation has no reason.")

    duplicate_keys = sorted({key for key in keys if keys.count(key) > 1})
    if duplicate_keys:
        errors.append(
            "Duplicate relation keys: "
            + ", ".join("|".join(key) for key in duplicate_keys[:20])
        )

    if not any(item.get("relationType") == "CONCOMITANT" for item in relation_records):
        errors.append("Dataset has no concomitant relations.")
    if not any(item.get("relationType") == "PREREQUISITE" for item in relation_records):
        errors.append("Dataset has no prerequisite relations.")
    if not any(item.get("relationType") == "ALTERNATIVE" for item in relation_records):
        errors.append("Dataset has no alternative relations.")

    digest = canonical_hash(relation_records)
    if dataset.get("contentSha256") != digest:
        errors.append("contentSha256 does not match relation records.")

    expected_version = dataset_version(str(dataset.get("verifiedAt") or ""), digest)
    if dataset.get("version") != expected_version:
        errors.append("version does not match canonical relation hash.")

    if dataset.get("recordCount") != len(relation_records):
        errors.append("recordCount does not match relations length.")

    if dataset.get("mbftVersion") != mbft.get("version"):
        errors.append("Relation dataset is not bound to the current MBFT version.")

    return errors


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def command_build(args: argparse.Namespace) -> int:
    mbft = load_json(args.mbft)
    try:
        dataset = build_dataset(mbft)
    except ValueError as exc:
        print(str(exc))
        return 1

    write_json(args.output, dataset)
    counts: dict[str, int] = {}
    for item in dataset["records"]:
        key = item["relationType"]
        counts[key] = counts.get(key, 0) + 1

    unresolved = collect_unresolved_references(mbft)

    report = {
        "version": dataset["version"],
        "mbftVersion": dataset["mbftVersion"],
        "recordCount": dataset["recordCount"],
        "contentSha256": dataset["contentSha256"],
        "typeCounts": counts,
        "officialDerivedCount": sum(
            1
            for item in dataset["records"]
            if item["origin"] == "MBFT_OFFICIAL_FIELD"
        ),
        "curatedCount": sum(
            1
            for item in dataset["records"]
            if item["origin"] == "CURATED_PROJECT_RELATION"
        ),
        "unresolvedReferenceCount": len(unresolved),
        "unresolvedReferences": unresolved,
    }
    if args.report:
        write_json(args.report, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


def command_validate(args: argparse.Namespace) -> int:
    mbft = load_json(args.mbft)
    dataset = load_json(args.dataset)
    errors = validate_dataset(dataset, mbft)
    print(
        json.dumps(
            {
                "recordCount": len(dataset.get("records", [])),
                "version": dataset.get("version"),
                "errors": errors,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 1 if errors else 0


def build_parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description="MBFT relation derivation pipeline")
    sub = root.add_subparsers(dest="command", required=True)

    build = sub.add_parser("build")
    build.add_argument("--mbft", type=Path, required=True)
    build.add_argument("--output", type=Path, required=True)
    build.add_argument("--report", type=Path)
    build.set_defaults(func=command_build)

    validate = sub.add_parser("validate")
    validate.add_argument("--mbft", type=Path, required=True)
    validate.add_argument("--dataset", type=Path, required=True)
    validate.set_defaults(func=command_validate)
    return root


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
