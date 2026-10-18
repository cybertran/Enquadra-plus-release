#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import pathlib
import shutil
import time
import urllib.error
import urllib.request

USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) "
    "AppleWebKit/537.36 Chrome/141 Safari/537.36"
)


def download_pdf(url: str, target: pathlib.Path, attempts: int = 5) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    last_error: Exception | None = None

    for attempt in range(1, attempts + 1):
        request = urllib.request.Request(
            url,
            headers={
                "User-Agent": USER_AGENT,
                "Accept": "application/pdf,*/*;q=0.8",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                with target.open("wb") as output:
                    shutil.copyfileobj(response, output)

            if target.stat().st_size <= 0:
                raise RuntimeError(f"Downloaded file is empty: {target}")

            with target.open("rb") as handle:
                if handle.read(5) != b"%PDF-":
                    raise RuntimeError(f"Downloaded content is not a PDF: {target}")

            return
        except (OSError, urllib.error.URLError, RuntimeError) as exc:
            last_error = exc
            target.unlink(missing_ok=True)
            if attempt < attempts:
                time.sleep(attempt * 2)

    raise RuntimeError(f"Failed to download {url}: {last_error}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--base-output", required=True)
    parser.add_argument("--amendment-dir", required=True)
    args = parser.parse_args()

    manifest_path = pathlib.Path(args.manifest)
    base_output = pathlib.Path(args.base_output)
    amendment_dir = pathlib.Path(args.amendment_dir)

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    base_url = manifest["base"]["officialUrl"]

    print(f"Downloading official source: {base_url}")
    download_pdf(base_url, base_output)

    for amendment in manifest.get("amendments", []):
        if amendment.get("status") != "PATCH_REQUIRED":
            continue

        file_key = amendment.get("fileKey")
        source_url = amendment.get("sourceUrl")
        resolution = amendment.get("resolution", "-")

        if not file_key or not source_url:
            raise RuntimeError(
                f"PATCH_REQUIRED amendment missing fileKey/sourceUrl: {resolution}"
            )

        target = amendment_dir / f"{file_key}.pdf"
        print(f"Downloading amendment {resolution}: {source_url}")
        download_pdf(source_url, target)


if __name__ == "__main__":
    main()
