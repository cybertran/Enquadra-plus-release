#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Exporta fichas PARTIAL_VALIDATION em lotes pequenos para revisão."
    )
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--batch-size", type=int, default=20)
    args = parser.parse_args()

    if args.batch_size < 1:
        raise SystemExit("--batch-size deve ser maior que zero.")

    dataset = json.loads(args.dataset.read_text(encoding="utf-8"))
    records = [
        record
        for record in dataset.get("records", [])
        if record.get("status") == "PARTIAL_VALIDATION"
    ]
    records.sort(key=lambda item: item.get("code", ""))

    args.output_dir.mkdir(parents=True, exist_ok=True)
    for old in args.output_dir.glob("partial-*.json"):
        old.unlink()

    manifest = {
        "sourceDataset": str(args.dataset),
        "partialCount": len(records),
        "batchSize": args.batch_size,
        "batches": [],
    }

    for index, start in enumerate(range(0, len(records), args.batch_size), start=1):
        batch = records[start : start + args.batch_size]
        name = f"partial-{index:02d}.json"
        payload = {
            "batch": index,
            "recordCount": len(batch),
            "codes": [record.get("code") for record in batch],
            "records": batch,
        }
        (args.output_dir / name).write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        manifest["batches"].append(
            {"file": name, "recordCount": len(batch), "codes": payload["codes"]}
        )

    (args.output_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
