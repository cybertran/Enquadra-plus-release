#!/usr/bin/env python3
"""Build offline normative full-text packs and auditable infraction links."""

from __future__ import annotations

import argparse
import concurrent.futures
import gzip
import hashlib
import io
import json
import re
import subprocess
import tempfile
import time
import zipfile
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import requests
from pypdf import PdfReader
from striprtf.striprtf import rtf_to_text

ALLOWED_HOSTS = {"gov.br", "www.gov.br"}
ALLOWED_PATH_PREFIX = "/transportes/pt-br/assuntos/transito/"
MAX_DOCUMENT_BYTES = 40 * 1024 * 1024

ACCEPTED_EXTRACTION_FAILURE_CATEGORIES = {
    "LEGACY_DOC_EXTRACTION_FAILURE",
    "INDEX_ONLY_NO_DOCUMENT_URL",
}


def unaccepted_extraction_failures(
    failures: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    return [
        item
        for item in failures
        if item.get("category") not in ACCEPTED_EXTRACTION_FAILURE_CATEGORIES
    ]
REQUEST_TIMEOUT = 45
MAX_RETRIES = 3
CHUNK_SIZE = 250
REFERENCE_RE = re.compile(
    r"(?i)\b(resolu[cç][aã]o|delibera[cç][aã]o|portaria)"
    r"(?:\s+(?:CONTRAN|SENATRAN|DENATRAN))?\s*"
    r"(?:n[ºo.]?\s*)?(\d{1,4})\s*"
    r"(?:/|,?\s+de\s+(?:\d{1,2}\s+de\s+)?"
    r"(?:[A-Za-zÀ-ÿ]+\s+de\s+)?)"
    r"(\d{4})"
)
ARTICLE_RE = re.compile(r"(?i)\bart(?:igo|\.)?\s*(\d{1,3})\b")
ARTICLE_LIST_RE = re.compile(
    r"(?i)\barts?\.?\s*"
    r"((?:\d{1,3}\s*(?:,|e|a|até)\s*)+\d{1,3})"
)
INACTIVE_STATUS_RE = re.compile(
    r"(?i)\b(revogad[ao]|sem\s+efeito|cancelad[ao]|suspens[ao])\b"
)
CTB_CONTEXT_RE = re.compile(
    r"(?i)(CTB|C[oó]digo\s+de\s+Tr[aâ]nsito\s+Brasileiro|Lei\s+n[ºo.]?\s*9[.]503)"
)
OTHER_NORMATIVE_ACT_RE = re.compile(
    r"(?i)\b(resolu[cç][aã]o|delibera[cç][aã]o|portaria)\b"
)


@dataclass(frozen=True)
class ExtractedAct:
    act_id: str
    source_sha256: str
    media_type: str
    page_count: int | None
    text: str
    error: str | None = None


def canonical_json(value: Any) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest().upper()


def normalize_text(value: str) -> str:
    value = value.replace("\u00ad", "").replace("\u00a0", " ")
    value = value.replace("\r\n", "\n").replace("\r", "\n")
    value = re.sub(r"[ \t]+", " ", value)
    value = re.sub(r" *\n *", "\n", value)
    value = re.sub(r"\n{3,}", "\n\n", value)
    return value.strip()


def validate_url(url: str) -> None:
    parsed = urlparse(url)
    if parsed.scheme.lower() != "https":
        raise ValueError("official document must use HTTPS")
    if (parsed.hostname or "").lower() not in ALLOWED_HOSTS:
        raise ValueError("official document host is not allowlisted")
    if parsed.port not in (None, 443):
        raise ValueError("official document port is not allowlisted")
    if parsed.username or parsed.password or parsed.fragment:
        raise ValueError("official document URL contains forbidden components")
    if not parsed.path.startswith(ALLOWED_PATH_PREFIX):
        raise ValueError("official document path is not allowlisted")


def download_once(url: str) -> bytes:
    current = url
    for redirect_index in range(4):
        validate_url(current)
        with requests.get(
            current,
            timeout=REQUEST_TIMEOUT,
            allow_redirects=False,
            stream=True,
            headers={
                "User-Agent": "EnquadraPlus-NormativeFullText/0.18",
                "Accept": "*/*",
            },
        ) as response:
            if 300 <= response.status_code < 400:
                if redirect_index >= 3:
                    raise ValueError("too many official document redirects")
                location = response.headers.get("Location")
                if not location:
                    raise ValueError("official redirect without destination")
                current = requests.compat.urljoin(current, location)
                validate_url(current)
                continue

            response.raise_for_status()
            declared = int(response.headers.get("Content-Length") or 0)
            if declared > MAX_DOCUMENT_BYTES:
                raise ValueError("official document exceeds size limit")
            data = bytearray()
            for chunk in response.iter_content(64 * 1024):
                if not chunk:
                    continue
                data.extend(chunk)
                if len(data) > MAX_DOCUMENT_BYTES:
                    raise ValueError("official document exceeds size limit")
            return bytes(data)

    raise ValueError("official document redirect loop")


def download(url: str) -> bytes:
    validate_url(url)
    last_error: Exception | None = None
    for attempt in range(MAX_RETRIES):
        try:
            return download_once(url)
        except Exception as exc:
            last_error = exc
            if attempt + 1 < MAX_RETRIES:
                time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(str(last_error) if last_error else "download failed")


def extract_pdf(data: bytes) -> tuple[str, int]:
    reader = PdfReader(io.BytesIO(data), strict=False)
    parts: list[str] = []
    for page in reader.pages:
        try:
            parts.append(page.extract_text() or "")
        except Exception:
            parts.append("")
    return normalize_text("\n\n".join(parts)), len(reader.pages)


def extract_doc(data: bytes) -> str:
    with tempfile.NamedTemporaryFile(suffix=".doc") as handle:
        handle.write(data)
        handle.flush()
        completed = subprocess.run(
            ["antiword", handle.name],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    return normalize_text(completed.stdout.decode("utf-8", errors="replace"))


def extract_rtf(data: bytes) -> str:
    raw = data.decode("latin-1", errors="replace")
    return normalize_text(rtf_to_text(raw))


def extract_zip(data: bytes) -> tuple[str, int | None]:
    texts: list[str] = []
    page_count = 0
    expanded_bytes = 0
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        infos = [item for item in archive.infolist() if not item.is_dir()]
        if len(infos) > 50:
            raise ValueError("official ZIP contains too many entries")
        for info in sorted(infos, key=lambda item: item.filename):
            expanded_bytes += int(info.file_size)
            if expanded_bytes > MAX_DOCUMENT_BYTES:
                raise ValueError("official ZIP exceeds expanded size limit")
            name = info.filename
            nested = archive.read(info)
            suffix = Path(name).suffix.lower()
            try:
                if suffix == ".pdf":
                    text, pages = extract_pdf(nested)
                    texts.append(text)
                    page_count += pages
                elif suffix == ".doc":
                    texts.append(extract_doc(nested))
                elif suffix == ".rtf":
                    texts.append(extract_rtf(nested))
            except Exception:
                continue
    return normalize_text("\n\n".join(texts)), page_count or None


def extraction_failure_category(
    record: dict[str, Any],
    error: str,
) -> str:
    url = str(record.get("officialUrl") or "")
    source_page = str(record.get("sourcePageUrl") or "")
    suffix = Path(urlparse(url).path).suffix.lower()

    if (
        "unsupported document type" in error
        and url
        and source_page
        and url == source_page
        and not suffix
    ):
        return "INDEX_ONLY_NO_DOCUMENT_URL"
    if suffix == ".doc":
        return "LEGACY_DOC_EXTRACTION_FAILURE"
    return "EXTRACTION_FAILURE"


def extract_act(record: dict[str, Any]) -> ExtractedAct:
    act_id = str(record["id"])
    url = str(record["officialUrl"])
    try:
        data = download(url)
        digest = sha256_bytes(data)
        suffix = Path(urlparse(url).path).suffix.lower()
        if suffix == ".pdf":
            text, page_count = extract_pdf(data)
            media_type = "application/pdf"
        elif suffix == ".doc":
            text = extract_doc(data)
            page_count = None
            media_type = "application/msword"
        elif suffix == ".rtf":
            text = extract_rtf(data)
            page_count = None
            media_type = "application/rtf"
        elif suffix == ".zip":
            text, page_count = extract_zip(data)
            media_type = "application/zip"
        else:
            raise ValueError(f"unsupported document type: {suffix or 'unknown'}")

        if not text:
            raise ValueError("document has no extractable text")

        return ExtractedAct(
            act_id=act_id,
            source_sha256=digest,
            media_type=media_type,
            page_count=page_count,
            text=text,
        )
    except Exception as exc:
        return ExtractedAct(
            act_id=act_id,
            source_sha256="",
            media_type="",
            page_count=None,
            text="",
            error=str(exc),
        )


def inactive(record: dict[str, Any]) -> bool:
    return bool(INACTIVE_STATUS_RE.search(str(record.get("status") or "")))


def all_infraction_text(record: dict[str, Any]) -> str:
    parts = [
        record.get("article"),
        record.get("shortDescription"),
        record.get("description"),
        record.get("penalty"),
        record.get("administrativeMeasure"),
        record.get("authority"),
        record.get("verificationMethod"),
    ]
    for key in (
        "legalBasis",
        "whenToCite",
        "whenNotToCite",
        "procedures",
        "aitObservations",
    ):
        parts.extend(record.get(key) or [])
    for source in record.get("sources") or []:
        parts.extend([
            source.get("document"),
            source.get("section"),
        ])
    return "\n".join(str(item) for item in parts if item)


def article_number(record: dict[str, Any]) -> int | None:
    match = ARTICLE_RE.search(str(record.get("article") or ""))
    return int(match.group(1)) if match else None


def infraction_inciso(record: dict[str, Any]) -> str | None:
    article = normalize_text(str(record.get("article") or ""))
    match = re.search(
        r"(?i)\bart(?:igo|\.)?\s*\d{1,3}\s*,\s*([IVXLCDM]+)\b",
        article,
    )
    return match.group(1).upper() if match else None


def evidence_incisoes(
    evidence: str,
    article: int,
) -> set[str]:
    """Return explicit incisos tied to one CTB article in the evidence.

    An empty set means the cited text does not narrow the article to a
    particular inciso, so the act may regulate the article as a whole.
    """
    text = normalize_text(evidence)
    romans = r"[IVXLCDM]+"
    results: set[str] = set()

    after_comma_pattern = re.compile(
        rf"(?i)\bart(?:igo|\.)?\s*{article}\s*,\s*"
        rf"(?:incisos?\s*)?({romans}(?:\s*(?:,|e)\s*{romans})*)\b"
    )
    after_named_pattern = re.compile(
        rf"(?i)\bart(?:igo|\.)?\s*{article}\s*,?\s+"
        rf"incisos?\s+({romans}(?:\s*(?:,|e)\s*{romans})*)\b"
    )
    before_pattern = re.compile(
        rf"(?i)\bincisos?\s+({romans}(?:\s*(?:,|e)\s*{romans})*)\b"
        rf"[^.;:]{{0,120}}?\bart(?:igo|\.)?\s*{article}\b"
    )

    for pattern in (
        after_comma_pattern,
        after_named_pattern,
        before_pattern,
    ):
        for match in pattern.finditer(text):
            results.update(
                item.upper()
                for item in re.findall(romans, match.group(1), re.IGNORECASE)
            )

    return results


def evidence_matches_infraction_subdivision(
    record: dict[str, Any],
    evidence: str,
) -> bool:
    article = article_number(record)
    if article is None:
        return False

    required_inciso = infraction_inciso(record)
    if required_inciso is None:
        return True

    cited_incisoes = evidence_incisoes(evidence, article)
    return not cited_incisoes or required_inciso in cited_incisoes


def explicit_references(record: dict[str, Any]) -> set[tuple[str, int, int]]:
    references: set[tuple[str, int, int]] = {("RESOLUÇÃO", 985, 2022)}
    for match in REFERENCE_RE.finditer(all_infraction_text(record)):
        raw_type = match.group(1).lower()
        act_type = (
            "RESOLUÇÃO"
            if raw_type.startswith("resolu")
            else "DELIBERAÇÃO"
            if raw_type.startswith("delibera")
            else "PORTARIA"
        )
        references.add((act_type, int(match.group(2)), int(match.group(3))))
    return references


def parse_article_list(value: str) -> set[int]:
    numbers = [int(item) for item in re.findall(r"\b\d{1,3}\b", value)]
    if not numbers:
        return set()

    articles = set(numbers)
    range_match = re.search(
        r"(?i)\b(\d{1,3})\s*(?:a|até)\s*(\d{1,3})\b",
        value,
    )
    if range_match:
        start = int(range_match.group(1))
        end = int(range_match.group(2))
        if 1 <= start <= end <= 400 and end - start <= 80:
            articles.update(range(start, end + 1))

    return {item for item in articles if 1 <= item <= 400}


def direct_ctb_article_evidence(
    text: str,
) -> list[tuple[set[int], str]]:
    """Return article references directly tied to CTB context.

    A broad proximity check is deliberately avoided. If another normative
    instrument is named between the article token and the CTB marker, the
    candidate is rejected so an article of a Resolution is not mistaken for
    an article of the CTB.
    """
    normalized = normalize_text(text)
    candidates: list[tuple[re.Match[str], set[int]]] = []

    for match in ARTICLE_RE.finditer(normalized):
        candidates.append((match, {int(match.group(1))}))

    for match in ARTICLE_LIST_RE.finditer(normalized):
        articles = parse_article_list(match.group(1))
        if articles:
            candidates.append((match, articles))

    evidence: list[tuple[set[int], str]] = []
    for match, articles in candidates:
        after = normalized[match.end() : min(len(normalized), match.end() + 700)]
        before = normalized[max(0, match.start() - 350) : match.start()]

        after_ctb = CTB_CONTEXT_RE.search(after)
        before_matches = list(CTB_CONTEXT_RE.finditer(before))
        before_ctb = before_matches[-1] if before_matches else None

        direct = False
        if after_ctb is not None:
            bridge = after[: after_ctb.start()]
            direct = not OTHER_NORMATIVE_ACT_RE.search(bridge)

        if not direct and before_ctb is not None:
            bridge = before[before_ctb.end() :]
            direct = not OTHER_NORMATIVE_ACT_RE.search(bridge)

        if not direct:
            continue

        start = max(0, match.start() - 350)
        end = min(len(normalized), match.end() + 700)
        evidence.append((articles, normalize_text(normalized[start:end])))

    return evidence


def build_article_evidence_index(
    acts: list[dict[str, Any]],
    extracted_by_id: dict[str, ExtractedAct],
) -> dict[int, list[tuple[dict[str, Any], str]]]:
    index: dict[int, list[tuple[dict[str, Any], str]]] = {}
    seen: set[tuple[str, int]] = set()

    for act in acts:
        sources: list[str] = []
        metadata = "\n".join(
            str(value)
            for value in (act.get("subject"), act.get("status"))
            if value
        )
        if metadata:
            sources.append(metadata)

        extracted = extracted_by_id.get(str(act["id"]))
        if extracted and extracted.text:
            sources.append(extracted.text)

        for text in sources:
            for articles, evidence in direct_ctb_article_evidence(text):
                for article in articles:
                    key = (str(act["id"]), article)
                    if key in seen:
                        continue
                    seen.add(key)
                    index.setdefault(article, []).append((act, evidence))

    return index


def build_links(
    infractions: list[dict[str, Any]],
    acts: list[dict[str, Any]],
    extracted_by_id: dict[str, ExtractedAct],
) -> list[dict[str, Any]]:
    by_reference = {
        (str(act["actType"]), int(act["number"]), int(act["year"])): act
        for act in acts
    }
    active_acts = [act for act in acts if not inactive(act)]
    article_index = build_article_evidence_index(
        active_acts,
        extracted_by_id,
    )
    links: list[dict[str, Any]] = []
    seen: set[tuple[str, str, str]] = set()

    def append(
        code: str,
        act: dict[str, Any],
        kind: str,
        evidence: str,
    ) -> None:
        key = (code, str(act["id"]), kind)
        if key in seen:
            return
        seen.add(key)
        links.append(
            {
                "infractionCode": code,
                "actId": act["id"],
                "relationKind": kind,
                "evidence": evidence,
                "officialUrl": act["officialUrl"],
            }
        )

    for infraction in infractions:
        code = str(infraction["code"])
        for reference in explicit_references(infraction):
            act = by_reference.get(reference)
            if act and not inactive(act):
                append(
                    code,
                    act,
                    "EXPLICIT_MBFT_REFERENCE",
                    (
                        "Referência normativa explícita localizada na ficha MBFT "
                        f"ou em sua fonte: {act['actType']} "
                        f"{act['number']}/{act['year']}."
                    ),
                )

        article = article_number(infraction)
        if article is None:
            continue

        for act, evidence in article_index.get(article, []):
            if not evidence_matches_infraction_subdivision(
                infraction,
                evidence,
            ):
                continue
            append(
                code,
                act,
                "DIRECT_CTB_ARTICLE_REFERENCE",
                evidence,
            )

    return sorted(
        links,
        key=lambda item: (
            item["infractionCode"],
            item["relationKind"],
            item["actId"],
        ),
    )


def write_chunks(
    extracted: list[ExtractedAct],
    out_dir: Path,
    catalog_version: str,
) -> dict[str, Any]:
    out_dir.mkdir(parents=True, exist_ok=True)
    for old in out_dir.glob("chunk-*.json.gz"):
        old.unlink()

    successful = [item for item in extracted if item.text]
    chunks: list[dict[str, Any]] = []
    for index in range(0, len(successful), CHUNK_SIZE):
        items = successful[index : index + CHUNK_SIZE]
        records = [
            {
                "actId": item.act_id,
                "sourceSha256": item.source_sha256,
                "mediaType": item.media_type,
                "pageCount": item.page_count,
                "text": item.text,
            }
            for item in items
        ]
        payload = {
            "catalogVersion": catalog_version,
            "records": records,
        }
        name = f"chunk-{index // CHUNK_SIZE:02d}.json.gz"
        raw = canonical_json(payload)
        with gzip.open(out_dir / name, "wb", compresslevel=9) as handle:
            handle.write(raw)
        chunks.append(
            {
                "file": name,
                "recordCount": len(records),
                "contentSha256": sha256_bytes(raw),
            }
        )

    manifest_core = {
        "catalogVersion": catalog_version,
        "recordCount": len(successful),
        "chunks": chunks,
    }
    manifest = {
        **manifest_core,
        "contentSha256": sha256_bytes(canonical_json(manifest_core)),
    }
    (out_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return manifest


def build(args: argparse.Namespace) -> int:
    catalog = json.loads(Path(args.catalog).read_text(encoding="utf-8"))
    mbft = json.loads(Path(args.mbft).read_text(encoding="utf-8"))
    acts = list(catalog["records"])

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        extracted = list(pool.map(extract_act, acts))

    extracted_by_id = {item.act_id: item for item in extracted}
    manifest = write_chunks(
        extracted,
        Path(args.output_dir),
        str(catalog["version"]),
    )

    links = build_links(
        list(mbft["records"]),
        acts,
        extracted_by_id,
    )
    link_core = {
        "generatedAt": catalog.get("verifiedAt", "2026-10-15"),
        "catalogVersion": catalog["version"],
        "mbftVersion": mbft["version"],
        "recordCount": len(links),
        "records": links,
    }
    link_hash = sha256_bytes(canonical_json(link_core))
    link_dataset = {
        **link_core,
        "contentSha256": link_hash,
        "version": (
            str(link_core["generatedAt"]) +
            "-infraction-norm-links-" +
            link_hash[:12].lower()
        ),
    }
    Path(args.links).write_text(
        json.dumps(link_dataset, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    acts_by_id = {str(act["id"]): act for act in acts}
    failures = [
        {
            "actId": item.act_id,
            "category": extraction_failure_category(
                acts_by_id[item.act_id],
                str(item.error),
            ),
            "error": item.error,
        }
        for item in extracted
        if item.error
    ]
    failure_category_counts = dict(sorted(Counter(
        item["category"] for item in failures
    ).items()))
    pdf_act_ids = {
        str(act["id"])
        for act in acts
        if Path(urlparse(str(act["officialUrl"])).path).suffix.lower() == ".pdf"
    }
    extracted_ids = {
        item.act_id
        for item in extracted
        if item.text
    }
    pdf_extracted_count = len(pdf_act_ids & extracted_ids)
    linked_codes = {item["infractionCode"] for item in links}
    non_base_codes = {
        item["infractionCode"]
        for item in links
        if item["actId"] != "contran-resolucao-2022-985"
    }
    all_codes = {str(item["code"]) for item in mbft["records"]}
    codes_without_non_base = sorted(all_codes - non_base_codes)
    report = {
        "catalogVersion": catalog["version"],
        "actCount": len(acts),
        "extractedCount": manifest["recordCount"],
        "failureCount": len(failures),
        "failureCategoryCounts": failure_category_counts,
        "coverage": round(manifest["recordCount"] / max(len(acts), 1), 6),
        "pdfActCount": len(pdf_act_ids),
        "pdfExtractedCount": pdf_extracted_count,
        "pdfCoverage": round(
            pdf_extracted_count / max(len(pdf_act_ids), 1),
            6,
        ),
        "chunkCount": len(manifest["chunks"]),
        "infractionCount": len(all_codes),
        "auditedInfractionCount": len(all_codes),
        "linkCount": len(links),
        "codesWithNonBaseNormativeLink": len(non_base_codes),
        "codesWithoutNonBaseNormativeLink": codes_without_non_base,
        "failures": failures,
    }
    Path(args.report).parent.mkdir(parents=True, exist_ok=True)
    Path(args.report).write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["coverage"] >= args.min_coverage else 2


def validate(args: argparse.Namespace) -> int:
    out_dir = Path(args.output_dir)
    manifest = json.loads((out_dir / "manifest.json").read_text(encoding="utf-8"))
    links = json.loads(Path(args.links).read_text(encoding="utf-8"))
    report = json.loads(Path(args.report).read_text(encoding="utf-8"))

    errors: list[str] = []
    if report["coverage"] < args.min_coverage:
        errors.append("full-text coverage below required minimum")
    if report["pdfExtractedCount"] != report["pdfActCount"]:
        errors.append("not every official PDF has extractable full text")
    if report.get("pdfCoverage") != 1.0:
        errors.append("official PDF full-text coverage must be 100%")
    unaccepted_failures = unaccepted_extraction_failures(
        report.get("failures", [])
    )
    if unaccepted_failures:
        errors.append(
            "unclassified full-text extraction failures: "
            + ", ".join(
                str(item.get("actId") or "UNKNOWN")
                for item in unaccepted_failures[:10]
            )
        )
    if manifest["recordCount"] != report["extractedCount"]:
        errors.append("manifest/report record count mismatch")
    if links["recordCount"] != len(links["records"]):
        errors.append("link dataset count mismatch")
    link_core = {
        key: links[key]
        for key in (
            "generatedAt",
            "catalogVersion",
            "mbftVersion",
            "recordCount",
            "records",
        )
    }
    link_hash = sha256_bytes(canonical_json(link_core))
    if links.get("contentSha256") != link_hash:
        errors.append("link dataset canonical hash mismatch")
    expected_link_version = (
        str(links["generatedAt"]) +
        "-infraction-norm-links-" +
        link_hash[:12].lower()
    )
    if links.get("version") != expected_link_version:
        errors.append("link dataset version/hash mismatch")
    if links["recordCount"] != report["linkCount"]:
        errors.append("link report count mismatch")
    if report["auditedInfractionCount"] != report["infractionCount"]:
        errors.append("not every MBFT infraction was audited for normative links")
    if (
        report["codesWithNonBaseNormativeLink"] +
        len(report["codesWithoutNonBaseNormativeLink"])
        != report["infractionCount"]
    ):
        errors.append("normative link audit does not partition every MBFT infraction")

    seen: set[tuple[str, str, str]] = set()
    for item in links["records"]:
        key = (
            item["infractionCode"],
            item["actId"],
            item["relationKind"],
        )
        if key in seen:
            errors.append(f"duplicate normative link: {key}")
        seen.add(key)
        if item["relationKind"] not in {
            "EXPLICIT_MBFT_REFERENCE",
            "DIRECT_CTB_ARTICLE_REFERENCE",
        }:
            errors.append(f"invalid relation kind: {item['relationKind']}")
        if not item["evidence"].strip():
            errors.append(f"link without evidence: {key}")

    for chunk in manifest["chunks"]:
        path = out_dir / chunk["file"]
        if not path.is_file():
            errors.append(f"missing chunk: {chunk['file']}")
            continue
        with gzip.open(path, "rb") as handle:
            raw = handle.read()
        if sha256_bytes(raw) != chunk["contentSha256"]:
            errors.append(f"chunk hash mismatch: {chunk['file']}")
        payload = json.loads(raw)
        if len(payload["records"]) != chunk["recordCount"]:
            errors.append(f"chunk count mismatch: {chunk['file']}")

    if errors:
        for error in errors:
            print("ERROR:", error)
        return 1

    print(
        f"Validated {manifest['recordCount']} full-text acts, "
        f"{links['recordCount']} normative links, "
        f"coverage={report['coverage']:.2%}."
    )
    return 0


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    sub = root.add_subparsers(dest="command", required=True)
    for name in ("build", "validate"):
        cmd = sub.add_parser(name)
        cmd.add_argument("--catalog", required=True)
        cmd.add_argument("--mbft", required=True)
        cmd.add_argument("--output-dir", required=True)
        cmd.add_argument("--links", required=True)
        cmd.add_argument("--report", required=True)
        cmd.add_argument("--min-coverage", type=float, default=0.97)
        cmd.add_argument("--workers", type=int, default=12)
    return root


def main() -> int:
    args = parser().parse_args()
    return build(args) if args.command == "build" else validate(args)


if __name__ == "__main__":
    raise SystemExit(main())
