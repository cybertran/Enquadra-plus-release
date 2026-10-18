#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from mbft_pipeline import load_json, parse_record, read_pdf_sections


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Exports parsed amendment records for manual MBFT review."
    )
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--amendment-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()

    manifest = load_json(args.manifest)
    args.output_dir.mkdir(parents=True, exist_ok=True)

    exported: list[dict[str, object]] = []
    for amendment in manifest.get("amendments", []):
        if amendment.get("status") != "PATCH_REQUIRED":
            continue
        file_key = amendment.get("fileKey")
        if not file_key:
            continue
        pdf_path = args.amendment_dir / f"{file_key}.pdf"
        if not pdf_path.exists():
            raise FileNotFoundError(f"Missing downloaded amendment: {pdf_path}")

        sections, pdf_sha256 = read_pdf_sections(pdf_path)
        records = [
            parse_record(section, manifest, source=amendment)
            for section in sections
        ]
        output = {
            "resolution": amendment.get("resolution"),
            "fileKey": file_key,
            "pdfSha256": pdf_sha256,
            "recordCount": len(records),
            "records": records,
        }
        target = args.output_dir / f"{file_key}.json"
        target.write_text(
            json.dumps(output, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        exported.append({
            "fileKey": file_key,
            "resolution": amendment.get("resolution"),
            "recordCount": len(records),
            "file": target.name,
        })

    (args.output_dir / "manifest.json").write_text(
        json.dumps({"amendments": exported}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(exported, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
