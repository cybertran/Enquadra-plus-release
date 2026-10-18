#!/usr/bin/env python3

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import mbft_pipeline


class MbftPipelineFieldExtractionTest(unittest.TestCase):
    def test_operational_fields_aggregate_repeated_headers_across_pages(self):
        tables = [
            [
                ["Quando Autuar", "Quando NÃO Autuar", "Definições e Procedimentos", "Exemplos do Campo de Observações do AIT"],
                ["1. situação A", "1. exceção A", "1. procedimento A", "1. observação A"],
            ],
            [
                ["Quando Autuar", "Quando NÃO Autuar", "Definições e Procedimentos", "Exemplos do Campo de Observações do AIT"],
                ["2. situação B", "2. exceção B", "2. procedimento B", "2. observação B"],
            ],
        ]

        values = mbft_pipeline.operational_table_fields(tables)

        self.assertEqual(
            "1. situação A 2. situação B",
            values["Quando Autuar"],
        )
        self.assertEqual(
            "1. exceção A 2. exceção B",
            values["Quando NÃO Autuar"],
        )

    def test_operational_fields_remap_shifted_continuation_column(self):
        tables = [
            [
                ["Quando Autuar", None, "Quando NÃO Autuar", "Definições e Procedimentos", "Exemplos do Campo de Observações do AIT"],
                ["1. situação", None, "1. exceção parcial", "1. procedimento", "1. observação"],
            ],
            [
                [None, "continuação da exceção 2. nova exceção", None, None],
            ],
        ]

        values = mbft_pipeline.operational_table_fields(tables)

        self.assertEqual(
            "1. exceção parcial continuação da exceção 2. nova exceção",
            values["Quando NÃO Autuar"],
        )

    def test_short_ait_header_is_accepted(self):
        tables = [
            [
                ["Quando Autuar", "Quando NÃO Autuar", "Definições e Procedimentos", "Observações do AIT"],
                ["1. situação", "1. exceção", "1. procedimento", "1. observação"],
            ]
        ]

        values = mbft_pipeline.operational_table_fields(tables)

        self.assertEqual(
            "1. observação",
            values["Exemplos do Campo de Observações do AIT"],
        )

    def test_operational_content_may_reference_field_labels(self):
        tables = [
            [
                ["Quando Autuar", None, "Quando NÃO Autuar", "Definições e Procedimentos", "Exemplos do Campo de Observações do AIT"],
                [
                    "1. situação com informações complementares válidas",
                    None,
                    "1. exceção",
                    "1. A penalidade somente se aplica no caso descrito no campo Quando Autuar.",
                    "1. observação",
                ],
            ]
        ]

        values = mbft_pipeline.operational_table_fields(tables)

        self.assertIn(
            "penalidade somente se aplica",
            values["Definições e Procedimentos"],
        )
        self.assertIn(
            "informações complementares válidas",
            values["Quando Autuar"],
        )

    def test_split_numbered_deduplicates_repeated_items(self):
        value = "1. item A 2. item B 1. item A"

        self.assertEqual(
            ["item A", "item B"],
            mbft_pipeline.split_numbered(value),
        )


    def test_baseline_patch_applies_records_without_amendment_marker(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            (patch_dir / "001-baseline.json").write_text(
                json.dumps(
                    {
                        "resolution": "BASELINE",
                        "removeCodes": [],
                        "upserts": [{"code": "551-71", "status": "VERIFIED"}],
                    }
                ),
                encoding="utf-8",
            )

            records, applied = mbft_pipeline.apply_patch_files(
                {"551-72": {"code": "551-72", "status": "VERIFIED"}},
                patch_dir,
            )

        self.assertIn("551-71", records)
        self.assertEqual([], applied)

    def test_amendment_patch_is_recorded_as_applied(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            (patch_dir / "1003.json").write_text(
                json.dumps(
                    {
                        "resolution": "Resolução CONTRAN nº 1003/2023",
                        "removeCodes": [],
                        "upserts": [{"code": "694-73", "status": "VERIFIED"}],
                    }
                ),
                encoding="utf-8",
            )

            records, applied = mbft_pipeline.apply_patch_files({}, patch_dir)

        self.assertIn("694-73", records)
        self.assertEqual(["Resolução CONTRAN nº 1003/2023"], applied)

    def test_verified_override_enriches_only_empty_operational_fields(self):
        parsed = {
            "502-92": {
                "code": "502-92",
                "status": "PARTIAL_VALIDATION",
                "whenToCite": ["situação oficial"],
                "whenNotToCite": ["exceção oficial"],
                "procedures": ["procedimento extraído"],
                "aitObservations": ["observação oficial"],
            }
        }
        overrides = {
            "502-92": {
                "code": "502-92",
                "status": "VERIFIED",
                "whenToCite": [],
                "whenNotToCite": [],
                "procedures": ["procedimento revisado"],
                "aitObservations": [],
                "sources": [{"lastCheckedAt": "2026-10-13"}],
            }
        }

        merged = mbft_pipeline.merge_verified_overrides(parsed, overrides)["502-92"]

        self.assertEqual("VERIFIED", merged["status"])
        self.assertEqual(["situação oficial"], merged["whenToCite"])
        self.assertEqual(["exceção oficial"], merged["whenNotToCite"])
        self.assertEqual(["procedimento revisado"], merged["procedures"])
        self.assertEqual(["observação oficial"], merged["aitObservations"])
        self.assertEqual("2026-10-13", merged["sources"][0]["lastCheckedAt"])



if __name__ == "__main__":
    unittest.main()
