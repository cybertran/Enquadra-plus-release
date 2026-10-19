#!/usr/bin/env python3

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import official_code_set


class OfficialCodeSetTest(unittest.TestCase):
    def test_extracts_all_real_header_variants(self):
        hits = official_code_set.extract_header_codes(
            "Código do Enquadramento: 502-91\n"
            "Código de Enquadramento: 640-80\n"
            "Código Enquadramento: 763-32"
        )

        self.assertEqual(
            [("502-91", "do"), ("640-80", "de"), ("763-32", "none")],
            hits,
        )

    def test_does_not_treat_bare_cross_reference_as_official_header(self):
        self.assertEqual(
            [],
            official_code_set.extract_header_codes(
                "Utilizar enquadramento específico: 516-91, art. 165."
            ),
        )

    def test_patch_code_set_applies_removals_and_upserts(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            (patch_dir / "1031.json").write_text(
                json.dumps(
                    {
                        "resolution": "Resolução CONTRAN nº 1.031/2026",
                        "removeCodes": ["757-90"],
                        "upserts": [
                            {"code": "516-91"},
                            {"code": "757-91"},
                            {"code": "757-92"},
                        ],
                    }
                ),
                encoding="utf-8",
            )

            expected, applied = official_code_set.apply_patch_code_set(
                {"516-92", "757-90"},
                patch_dir,
            )

        self.assertEqual(
            {"516-91", "516-92", "757-91", "757-92"},
            expected,
        )
        self.assertEqual("1031.json", applied[0]["file"])

    def test_compare_reports_both_missing_and_unexpected(self):
        missing, unexpected = official_code_set.compare_code_sets(
            {"516-91", "516-92"},
            {"516-92", "999-99"},
        )

        self.assertEqual(["516-91"], missing)
        self.assertEqual(["999-99"], unexpected)


if __name__ == "__main__":
    unittest.main()
