#!/usr/bin/env python3
"""Deterministic static guardrails for the in-app normative updater."""

from pathlib import Path
import copy
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "app/src/main/java/com/cybertran/enquadraplus"
UPDATE = APP / "data/update"

required_files = [
    UPDATE / "OfficialSourcePolicy.kt",
    UPDATE / "OfficialSourceClient.kt",
    UPDATE / "GenerationStore.kt",
    UPDATE / "GenerationManifest.kt",
    UPDATE / "CtbUpdateCodec.kt",
    UPDATE / "NormativeUpdateCodec.kt",
    UPDATE / "MbftUpdateCodec.kt",
    UPDATE / "InfractionRelationsUpdateCodec.kt",
    UPDATE / "SafeDatasetPromoter.kt",
    UPDATE / "SafeNormativeUpdateRepository.kt",
    UPDATE / "SafeNormativeUpdateWorker.kt",
    APP / "ui/NormativeUpdateScreen.kt",
]
for path in required_files:
    assert path.is_file(), f"Missing safe-update file: {path.relative_to(ROOT)}"

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
assert 'android.permission.INTERNET' in manifest
assert 'android:usesCleartextTraffic="false"' in manifest

gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
assert "isCoreLibraryDesugaringEnabled = true" in gradle
assert 'coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")' in gradle
assert 'implementation("org.jsoup:jsoup:1.23.2")' in gradle

policy = (UPDATE / "OfficialSourcePolicy.kt").read_text(encoding="utf-8")
assert "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm" in policy
assert "setOf(\"planalto.gov.br\", \"www.planalto.gov.br\")" in policy
assert "setOf(\"gov.br\", \"www.gov.br\")" in policy
assert 'require(uri.scheme.equals("https"' in policy
assert "uri.userInfo == null" in policy
assert 'uri.rawPath?.contains("..") != true' in policy

client = (UPDATE / "OfficialSourceClient.kt").read_text(encoding="utf-8")
assert "instanceFollowRedirects = false" in client
assert "MAX_REDIRECTS = 3" in client
assert "MAX_ATTEMPTS = 3" in client
assert "catch (error: IOException)" in client
assert "catch (error: Throwable)" not in client
assert "HttpsURLConnection" in client

store = (UPDATE / "GenerationStore.kt").read_text(encoding="utf-8")
assert "writeGeneration(dataset, version, json)" in store
assert "writeManifest(dataset, version, manifest)" in store
assert "previousVersion = current.activeVersion" in store
assert "manifestFile(dataset, candidate).isFile" in store
assert "markParserProven" in store
assert "isParserProven" in store
assert "cleanupUnreferenced" in store
assert "AtomicFile" in store

promoter = (UPDATE / "SafeDatasetPromoter.kt").read_text(encoding="utf-8")
assert "database.withTransaction" in promoter
assert "UpdateDataset.MBFT -> promoteMbft(json)" in promoter
assert "InfractionRelationsUpdateCodec.build(payload)" in promoter
assert 'const val RELATIONS_DATASET_ID = "infraction-relations"' in promoter
assert "clearInfractionNormLinks()" in promoter
assert "clearFullTextSearchIndex()" in promoter
assert "clearFullTexts()" in promoter
assert 'NORMATIVE_FULLTEXT_DATASET_ID = "normative-fulltext"' in promoter
assert 'INFRACTION_NORM_LINKS_DATASET_ID = "infraction-norm-links"' in promoter

repository = (UPDATE / "SafeNormativeUpdateRepository.kt").read_text(encoding="utf-8")
assert "CANDIDATE_BLOCKED" in repository
assert "GenerationManifest.approvedCandidate" in repository
assert "checkMbftLocked()" in repository
assert "UNKNOWN_AMENDMENT_FAIL_CLOSED" in repository
assert "EXACT_PDF_SHA256" in repository
assert "RELATIONS_REBUILT_AND_VALIDATED" in repository
assert "isParserProven" in repository
assert "markParserProven" in repository
assert "sourceClient.fetch" in repository
assert "MBFT_ANNEX_PDF" not in policy
assert "anexosportal.datalegis.net" not in policy
assert "CONTRAN_DELIBERATIONS_INDEX_URL" in policy
assert "deliberacoes-contran" in policy

normative_codec = (UPDATE / "NormativeUpdateCodec.kt").read_text(encoding="utf-8")
assert 'PARSER_VERSION = "normative-html-v2"' in normative_codec
assert '"DELIBERAÇÃO"' in normative_codec
assert "parseContranDeliberations" in normative_codec
assert "hasMismatchedSenatranPortariaDocument" in normative_codec
assert '"norms.portaria_url_mismatch"' in normative_codec

for importer_name in (
    "CtbAssetImporter.kt",
    "MbftAssetImporter.kt",
):
    importer = (
        APP / "data/importer" / importer_name
    ).read_text(encoding="utf-8")
    assert "getDatasetVersion(DATASET_ID) != null" in importer, (
        f"{importer_name} must treat bundled data as bootstrap-only"
    )

normative_importer = (
    APP / "data/importer" / "NormativeAssetImporter.kt"
).read_text(encoding="utf-8")
assert "shouldInstallBundled(dataset)" in normative_importer
assert 'countByActType("DELIBERAÇÃO") > 0' in normative_importer
assert "installedDate > bundledDate" in normative_importer
assert "installedDate < bundledDate" in normative_importer
assert "clearInfractionNormLinks()" in normative_importer
assert "clearFullTextSearchIndex()" in normative_importer
assert "clearFullTexts()" in normative_importer

fulltext_importer = (
    APP / "data/importer" / "NormativeFullTextAssetImporter.kt"
).read_text(encoding="utf-8")
assert 'FULLTEXT_DATASET_ID = "normative-fulltext"' in fulltext_importer
assert 'LINKS_DATASET_ID = "infraction-norm-links"' in fulltext_importer
assert "installedCatalog != manifest.catalogVersion" in fulltext_importer
assert "root.getString(\"mbftVersion\") == installedMbftVersion" in fulltext_importer
assert "sha256(raw) == chunk.contentSha256" in fulltext_importer

database_schema = (
    APP / "data/local" / "EnquadraDatabase.kt"
).read_text(encoding="utf-8")
assert "NormativeFullTextEntity::class" in database_schema
assert "InfractionNormLinkEntity::class" in database_schema
assert "version = 7" in database_schema

fulltext_pipeline = (
    ROOT / "tools/norms/fulltext_pipeline.py"
).read_text(encoding="utf-8")
assert "allow_redirects=False" in fulltext_pipeline
assert "validate_url(current)" in fulltext_pipeline
assert "MAX_DOCUMENT_BYTES" in fulltext_pipeline
assert "DIRECT_CTB_ARTICLE_REFERENCE" in fulltext_pipeline

registry_path = ROOT / "app/src/main/assets/updates/mbft_supported_amendments.json"
registry = json.loads(registry_path.read_text(encoding="utf-8"))
assert registry["schemaVersion"] == 1
assert registry["parserVersion"] == "mbft-hash-patch-v1"
assert {(item["number"], item["year"]) for item in registry["recognizedAmendments"]} == {
    (1003, 2023),
    (1009, 2024),
    (1012, 2024),
    (1013, 2024),
    (1022, 2026),
    (1023, 2026),
    (1031, 2026),
}
assert len(registry["patches"]) == 1
patch = registry["patches"][0]
assert (patch["number"], patch["year"]) == (1031, 2026)
assert patch["effectiveFrom"] == "2026-10-17"
assert patch["expectedSha256"] == (
    "9F6D8D8C6E7B7996FCE21326A229C9B262D45178907FFF7BD9DD159CA5BA9B5A"
)
assert patch["removeCodes"] == ["757-90"]
assert {item["code"] for item in patch["upserts"]} == {
    "516-91",
    "516-92",
    "757-91",
    "757-92",
}
assert all(item["status"] == "VERIFIED" for item in patch["upserts"])
assert "annexUrl" not in patch
assert "annexExpectedSha256" not in patch
assert all(
    source["officialUrl"] == patch["officialUrl"]
    for item in patch["upserts"]
    for source in item["sources"]
)
patch_by_code = {item["code"]: item for item in patch["upserts"]}
assert len(patch_by_code["516-91"]["aitObservations"]) == 3
assert "dois sinais" not in patch_by_code["516-92"]["whenToCite"][0].lower()
assert "516-91" in patch_by_code["757-92"]["whenNotToCite"][1]
assert "757-91" not in patch_by_code["757-92"]["whenNotToCite"][1]
assert not any(
    "reincidência" in item.lower()
    for item in patch_by_code["757-92"]["procedures"]
)
assert "anexosportal.datalegis.net" not in json.dumps(
    registry,
    ensure_ascii=False,
)

mbft_path = ROOT / "app/src/main/assets/mbft/infractions.json"
relations_path = ROOT / "app/src/main/assets/relations/relations.json"
mbft = json.loads(mbft_path.read_text(encoding="utf-8"))
relations = json.loads(relations_path.read_text(encoding="utf-8"))

# Current production proof: the bundled MBFT must exactly match the
# independently extracted official code set for 17/10/2026.
code_set_path = ROOT / ".mbft-staging/code-set-report.json"
assert code_set_path.is_file(), "Missing MBFT official code-set report"
code_set = json.loads(code_set_path.read_text(encoding="utf-8"))
assert code_set["matches"] is True
assert code_set["expectedCodeCount"] == 415
assert code_set["generatedCodeCount"] == 415
assert code_set["missingFromGenerated"] == []
assert code_set["unexpectedInGenerated"] == []
assert code_set["duplicateGeneratedCodes"] == []

mbft_records = mbft["records"]
assert len(mbft_records) == 415
production_source_conflicts = {
    "516-91",
    "516-92",
    "611-40",
    "757-91",
    "757-92",
}
assert not any(
    item["status"] in {"PARTIAL_VALIDATION", "MANUAL_REVIEW_REQUIRED"}
    for item in mbft_records
), "Production MBFT contains unresolved review states"
assert {
    item["code"]
    for item in mbft_records
    if item["status"] == "SOURCE_CONFLICT"
} == production_source_conflicts
assert all(
    item["status"] == "SOURCE_CONFLICT"
    if item["code"] in production_source_conflicts
    else item["status"] == "VERIFIED"
    for item in mbft_records
)
for item in mbft_records:
    verification = item.get("verification")
    assert isinstance(verification, dict), (
        f"MBFT {item['code']} lacks verification evidence"
    )
    assert re.fullmatch(
        r"[A-F0-9]{64}",
        str(verification.get("verifiedAgainstSourceSha256") or ""),
    ), f"MBFT {item['code']} has invalid verification source hash"
    assert verification.get("reviewedFields"), (
        f"MBFT {item['code']} lacks reviewed field evidence"
    )
mbft_by_code = {item["code"]: item for item in mbft_records}
assert len(mbft_by_code) == 415
assert "757-90" not in mbft_by_code
for required_code in (
    "516-91",
    "516-92",
    "594-01",
    "608-41",
    "640-80",
    "721-80",
    "757-91",
    "757-92",
):
    assert required_code in mbft_by_code

assert mbft_by_code["501-00"]["points"] is None
assert not any(
    str(item).rstrip().endswith(" possuir")
    for item in mbft_by_code["501-00"].get("aitObservations", [])
), "MBFT 501-00 retains the historical corrupted AIT tail"

baseline_501_patch = json.loads(
    (ROOT / "tools/mbft/amendments/004-baseline-501-review-2026-10-17.json")
    .read_text(encoding="utf-8")
)
assert "recordApprovals" not in baseline_501_patch, (
    "501-00 correction must not rely on approval-only evidence"
)
patch_501 = baseline_501_patch["recordPatches"]["501-00"]
assert patch_501["fields"]["points"] is None
assert patch_501["fields"]["aitObservations"][-1] == (
    "Aprendiz conduzindo veículo não destinado à aprendizagem."
)
assert set(patch_501["fields"]).issubset(
    set(patch_501["verification"]["reviewedFields"])
)

assert mbft_by_code["606-85"]["penalty"] == "Multa"
assert mbft_by_code["641-60"]["penalty"] == "Multa"
assert mbft_by_code["776-50"]["penalty"] == (
    "Multa e suspensão do direito de dirigir."
)
assert mbft_by_code["640-80"]["points"] is None
assert mbft_by_code["640-80"]["verificationMethod"] == (
    "Vide Definições e Procedimentos."
)
assert mbft_by_code["640-80"]["administrativeMeasure"] == (
    "Retenção do veículo para regularização e apreensão das placas irregulares."
)

coverage_tool = ROOT / "tools/mbft/official_code_set.py"
assert coverage_tool.is_file()
coverage_source = coverage_tool.read_text(encoding="utf-8")
assert "OFFICIAL_EXPECTED_CODES - GENERATED_CODES" in coverage_source
assert "GENERATED_CODES - OFFICIAL_EXPECTED_CODES" in coverage_source
mbft_workflow = (
    ROOT / ".github/workflows/mbft-pipeline.yml"
).read_text(encoding="utf-8")
assert "tools/mbft/official_code_set.py" in mbft_workflow
assert ".mbft-staging/code-set-report.json" in mbft_workflow
assert "Apply reviewed MBFT batch" not in mbft_workflow
assert "apply_review.py" not in mbft_workflow

# A normative act marked inactive in the official catalog must not be
# presented through the audited infraction-link dataset.
norms = json.loads(
    (ROOT / "app/src/main/assets/norms/acts.json").read_text(encoding="utf-8")
)
norm_links = json.loads(
    (ROOT / "app/src/main/assets/infraction_norm_links.json").read_text(
        encoding="utf-8"
    )
)
acts_by_id = {item["id"]: item for item in norms["records"]}
inactive_status = re.compile(
    r"(?i)\b(revogad[ao]|sem\s+efeito|cancelad[ao]|suspens[ao])\b"
)
for link in norm_links["records"]:
    status = str(acts_by_id[link["actId"]].get("status") or "")
    assert not inactive_status.search(status), (
        f"Inactive normative act linked to infraction: {link['actId']}"
    )

sys.path.insert(0, str(ROOT / "tools/relations"))
import relations_pipeline  # noqa: E402

rebuilt_current = relations_pipeline.build_dataset(mbft)
assert rebuilt_current["version"] == relations["version"]
assert rebuilt_current["contentSha256"] == relations["contentSha256"]
assert rebuilt_current["records"] == relations["records"]

candidate = copy.deepcopy(mbft)
by_code = {item["code"]: item for item in candidate["records"]}
for code in patch["removeCodes"]:
    by_code.pop(code, None)
for item in patch["upserts"]:
    by_code[item["code"]] = item
candidate["records"] = sorted(by_code.values(), key=lambda item: item["code"])
candidate["version"] = "2026-10-17-mbft-static-proof"
candidate["generatedAt"] = "2026-10-17"
candidate["verifiedAt"] = "2026-10-17"
candidate["appliedAmendments"] = list(dict.fromkeys(
    candidate.get("appliedAmendments", []) + [patch["resolution"]]
))

current_codes = {item["code"] for item in mbft["records"]}
expected_candidate_codes = (
    current_codes - set(patch["removeCodes"])
) | set(patch_by_code)
candidate_codes = {item["code"] for item in candidate["records"]}
assert candidate_codes == expected_candidate_codes
assert len(candidate["records"]) == len(expected_candidate_codes)
candidate_by_code = {item["code"]: item for item in candidate["records"]}
assert "757-90" not in candidate_by_code
assert all(
    candidate_by_code[code]["status"] == "VERIFIED"
    for code in ("516-91", "516-92", "757-91", "757-92")
)
rebuilt_candidate = relations_pipeline.build_dataset(candidate)
assert rebuilt_candidate["mbftVersion"] == candidate["version"]
current_unresolved = len(relations_pipeline.collect_unresolved_references(mbft))
candidate_unresolved = len(relations_pipeline.collect_unresolved_references(candidate))
assert candidate_unresolved <= current_unresolved

all_update_text = "\n".join(
    path.read_text(encoding="utf-8")
    for path in UPDATE.glob("*.kt")
)
assert "WebView" not in all_update_text
assert not re.search(r'["\']http://', all_update_text), "Cleartext URL found in updater"

tests = list(
    (ROOT / "app/src/test/java/com/cybertran/enquadraplus/data/update").glob("*Test.kt")
)
assert len(tests) >= 5, "Safe updater must have JVM unit tests"

print(
    "Safe updater static validation passed: "
    f"{len(required_files)} required files / {len(tests)} test files / "
    f"MBFT production {len(mbft_records)} source-verified records / "
    f"official-code-set {code_set['expectedCodeCount']} / "
    f"relations {rebuilt_candidate['recordCount']} records"
)
