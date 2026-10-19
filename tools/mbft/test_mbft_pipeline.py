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

    def test_positioned_operational_fields_preserve_columns_across_sparse_tables(self):
        rows = [
            [
                {"text": "Quando Autuar", "x0": 20.0, "x1": 150.0},
                {"text": "Quando NÃO Autuar", "x0": 220.0, "x1": 330.0},
                {"text": "Definições e Procedimentos", "x0": 340.0, "x1": 470.0},
                {"text": "Observações do AIT", "x0": 480.0, "x1": 580.0},
            ],
            [
                {"text": "1. situação inicial", "x0": 20.0, "x1": 150.0},
                {"text": "1. exceção inicial", "x0": 220.0, "x1": 330.0},
                {"text": "1. procedimento", "x0": 340.0, "x1": 470.0},
                {"text": "1. observação parcial", "x0": 480.0, "x1": 580.0},
            ],
            # Sparse continuation tables must keep their physical page columns.
            [
                {"text": "continuação da observação", "x0": 485.0, "x1": 575.0},
            ],
            [
                {"text": "continuação da exceção", "x0": 225.0, "x1": 325.0},
            ],
            [
                {"text": "Informações Complementares:", "x0": 20.0, "x1": 200.0},
            ],
        ]

        values = mbft_pipeline.operational_positioned_fields(rows)

        self.assertEqual(
            "1. exceção inicial continuação da exceção",
            values["Quando NÃO Autuar"],
        )
        self.assertEqual(
            "1. observação parcial continuação da observação",
            values["Exemplos do Campo de Observações do AIT"],
        )
        self.assertNotIn(
            "continuação da exceção",
            values["Quando Autuar"],
        )

    def test_positioned_operational_fields_drop_overlapping_duplicate_fragments(self):
        rows = [
            [
                {"text": "Quando Autuar", "x0": 20.0, "x1": 150.0},
                {"text": "Quando NÃO Autuar", "x0": 220.0, "x1": 330.0},
                {"text": "Definições e Procedimentos", "x0": 340.0, "x1": 470.0},
                {"text": "Observações do AIT", "x0": 480.0, "x1": 580.0},
            ],
            [
                {
                    "text": "6. Condutor que não possuir os cursos especializados obrigatórios,",
                    "x0": 220.0,
                    "x1": 330.0,
                },
            ],
            [
                {"text": "possuir", "x0": 220.0, "x1": 250.0},
                {"text": "os cursos especializados", "x0": 220.0, "x1": 300.0},
            ],
            [
                {
                    "text": "quando estes sejam obrigatórios, utilizar enquadramento específico: 774-91.",
                    "x0": 220.0,
                    "x1": 330.0,
                },
            ],
            [
                {"text": "quando estes sejam", "x0": 220.0, "x1": 300.0},
                {"text": "obrigatórios", "x0": 220.0, "x1": 270.0},
            ],
        ]

        values = mbft_pipeline.operational_positioned_fields(rows)

        self.assertEqual(
            (
                "6. Condutor que não possuir os cursos especializados obrigatórios, "
                "quando estes sejam obrigatórios, utilizar enquadramento específico: 774-91."
            ),
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

    def test_explicit_empty_operational_column_is_source_proof(self):
        section = {
            "tables": [[
                [
                    "Quando Autuar",
                    "Quando NÃO Autuar",
                    "Definições e Procedimentos",
                    "Exemplos do Campo de Observações do AIT",
                ],
                [
                    "1. situação",
                    "1. exceção",
                    "1. procedimento",
                    None,
                ],
            ]]
        }

        self.assertEqual(
            {"aitObservations"},
            mbft_pipeline.explicit_empty_operational_fields(section),
        )
        self.assertTrue(
            mbft_pipeline._safe_structured_source_correction(
                "aitObservations",
                [],
                explicit_empty_fields={"aitObservations"},
            )
        )
        self.assertFalse(
            mbft_pipeline._safe_structured_source_correction(
                "aitObservations",
                [],
                explicit_empty_fields=set(),
            )
        )

    def test_parse_record_prefers_explicit_table_columns_over_positioned_leak(self):
        section = {
            "code": "625-40",
            "text": "",
            "cells": [],
            "pages": [456],
            "tables": [[
                ["Quando Autuar", "Quando NÃO Autuar", "Definições e Procedimentos", "Exemplos do Campo de Observações do AIT"],
                ["1. situação oficial", "1. exceção oficial", "1. procedimento oficial", None],
            ]],
            "positionedRows": [
                [
                    {"text": "Quando Autuar", "x0": 0.0, "x1": 100.0},
                    {"text": "Quando NÃO Autuar", "x0": 100.0, "x1": 200.0},
                    {"text": "Definições e Procedimentos", "x0": 200.0, "x1": 300.0},
                    {"text": "Observações do AIT", "x0": 300.0, "x1": 400.0},
                ],
                [
                    {
                        "text": "situação oficial exceção oficial procedimento oficial",
                        "x0": 0.0,
                        "x1": 400.0,
                    },
                ],
            ],
        }
        manifest = {
            "document": "MBFT",
            "lastCheckedAt": "2026-10-17",
            "base": {
                "officialUrl": "https://www.gov.br/transportes/mbft.pdf",
                "effectiveFrom": "2023-01-02",
            },
        }

        record = mbft_pipeline.parse_record(section, manifest)

        self.assertEqual(["situação oficial"], record["whenToCite"])
        self.assertEqual(["exceção oficial"], record["whenNotToCite"])
        self.assertEqual(["procedimento oficial"], record["procedures"])
        self.assertEqual([], record["aitObservations"])

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


    def test_find_page_code_accepts_codigo_do_enquadramento(self):
        self.assertEqual(
            "502-91",
            mbft_pipeline.find_page_code(
                "Código do Enquadramento: 502-91",
                [],
            ),
        )

    def test_find_page_code_accepts_codigo_de_enquadramento(self):
        self.assertEqual(
            "640-80",
            mbft_pipeline.find_page_code(
                "Código de Enquadramento: 640-80",
                [],
            ),
        )

    def test_find_page_code_accepts_variant_inside_table_cell(self):
        self.assertEqual(
            "516-91",
            mbft_pipeline.find_page_code(
                "",
                ["Código de Enquadramento:\n516-91"],
            ),
        )

    def test_find_page_code_accepts_codigo_enquadramento_without_connector(self):
        self.assertEqual(
            "763-32",
            mbft_pipeline.find_page_code(
                "Código Enquadramento: 763-32",
                [],
            ),
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
                base_source_sha256="A" * 64,
                base_source_document="MBFT",
                verified_at="2026-10-17",
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

            records, applied = mbft_pipeline.apply_patch_files(
                {},
                patch_dir,
                verified_at="2026-10-17",
                amendment_hashes={
                    "Resolução CONTRAN nº 1003/2023": "B" * 64,
                },
            )

        self.assertIn("694-73", records)
        self.assertEqual(["Resolução CONTRAN nº 1003/2023"], applied)


    def test_crime_flag_accepts_explicit_article_without_sim_literal(self):
        self.assertTrue(mbft_pipeline.yes_no("Art. 310 do CTB"))
        self.assertTrue(mbft_pipeline.yes_no("SIM\nArt. 309 do CTB"))
        self.assertFalse(mbft_pipeline.yes_no("NÃO"))
        self.assertFalse(mbft_pipeline.yes_no(None))

    def test_cell_field_does_not_treat_label_word_inside_value_as_header(self):
        cells = [
            "Tipificação do Enquadramento: Incide na mesma penalidade aquele que...",
            "Penalidade:\nMulta",
        ]
        self.assertEqual("Multa", mbft_pipeline.cell_field(cells, "Penalidade"))

    def test_medidas_administrativas_plural_alias_is_accepted(self):
        cells = ["Medidas Administrativas:\nRetenção do veículo."]
        self.assertEqual(
            "Retenção do veículo.",
            mbft_pipeline.cell_field(cells, "Medida Administrativa"),
        )

    def test_baseline_field_patch_requires_and_preserves_verification_evidence(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            verification = {
                "state": "CORRECTED",
                "verifiedAt": "2026-10-17",
                "verifiedAgainstSourceSha256": "A" * 64,
                "verifiedSourceDocument": "MBFT",
                "verificationMethod": "MANUAL_FIELD_REVIEW",
                "reviewRecordId": "review-606-85",
                "reviewedFields": ["penalty"],
                "reviewReason": "Corrigir campo contaminado.",
                "reviewEvidence": "Campo Penalidade da ficha oficial.",
            }
            (patch_dir / "002-baseline.json").write_text(
                json.dumps(
                    {
                        "resolution": "BASELINE",
                        "recordPatches": {
                            "606-85": {
                                "fields": {"penalty": "Multa"},
                                "verification": verification,
                            }
                        },
                    }
                ),
                encoding="utf-8",
            )

            records, applied = mbft_pipeline.apply_patch_files(
                {
                    "606-85": {
                        "code": "606-85",
                        "status": "PARTIAL_VALIDATION",
                        "penalty": "valor contaminado",
                        "procedures": ["procedimento fresco"],
                    }
                },
                patch_dir,
            )

        self.assertEqual([], applied)
        self.assertEqual("Multa", records["606-85"]["penalty"])
        self.assertEqual(["procedimento fresco"], records["606-85"]["procedures"])
        self.assertEqual("VERIFIED", records["606-85"]["status"])
        self.assertEqual(verification, records["606-85"]["verification"])


    def test_stale_field_review_is_deferred_instead_of_promoted(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            verification = {
                "state": "CORRECTED",
                "verifiedAt": "2026-10-17",
                "verifiedAgainstSourceSha256": "A" * 64,
                "verifiedSourceDocument": "MBFT",
                "verificationMethod": "MANUAL_FIELD_REVIEW",
                "reviewRecordId": "review-594-01",
                "reviewedFields": ["whenToCite"],
                "reviewReason": "Revisão antiga e parcial.",
                "reviewEvidence": "Campo Quando Autuar.",
            }
            (patch_dir / "003-baseline.json").write_text(
                json.dumps(
                    {
                        "resolution": "BASELINE",
                        "recordPatches": {
                            "594-01": {
                                "fields": {"whenToCite": ["valor antigo revisado"]},
                                "verification": verification,
                            }
                        },
                    }
                ),
                encoding="utf-8",
            )
            deferred = []
            records, applied = mbft_pipeline.apply_patch_files(
                {
                    "594-01": {
                        "code": "594-01",
                        "status": "PARTIAL_VALIDATION",
                        "whenToCite": ["valor fresco"],
                        "procedures": ["procedimento fresco"],
                    }
                },
                patch_dir,
                review_differences=[
                    {
                        "code": "594-01",
                        "fieldDifferences": {
                            "whenToCite": {
                                "stored": ["valor antigo"],
                                "fresh": ["valor fresco"],
                            },
                            "procedures": {
                                "stored": ["procedimento antigo"],
                                "fresh": ["procedimento fresco"],
                            },
                        },
                        "semanticIssues": [],
                    }
                ],
                deferred_review_issues=deferred,
            )

        self.assertEqual([], applied)
        self.assertEqual(["valor fresco"], records["594-01"]["whenToCite"])
        self.assertEqual(["procedimento fresco"], records["594-01"]["procedures"])
        self.assertEqual("MANUAL_REVIEW_REQUIRED", records["594-01"]["status"])
        self.assertNotIn("verification", records["594-01"])
        self.assertEqual(["procedures"], deferred[0]["missingReviewedFields"])

    def test_fresh_record_approval_cannot_resolve_open_source_diffs(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            verification = {
                "state": "CORRECTED",
                "verifiedAt": "2026-10-17",
                "verifiedAgainstSourceSha256": "A" * 64,
                "verifiedSourceDocument": "MBFT",
                "verificationMethod": "MANUAL_FIELD_REVIEW",
                "reviewRecordId": "manual-501-00",
                "reviewedFields": ["points", "procedures"],
                "reviewReason": "Revisão manual da extração fresca.",
                "reviewEvidence": "Ficha oficial e páginas da fonte.",
            }
            (patch_dir / "004-baseline.json").write_text(
                json.dumps({
                    "resolution": "BASELINE",
                    "recordApprovals": {
                        "501-00": {"verification": verification}
                    },
                }),
                encoding="utf-8",
            )
            deferred = []
            records, _ = mbft_pipeline.apply_patch_files(
                {
                    "501-00": {
                        "code": "501-00",
                        "status": "PARTIAL_VALIDATION",
                        "points": None,
                        "procedures": ["procedimento oficial"],
                        "whenToCite": [],
                        "whenNotToCite": [],
                        "aitObservations": [],
                    }
                },
                patch_dir,
                review_differences=[{
                    "code": "501-00",
                    "fieldDifferences": {
                        "points": {"stored": 7, "fresh": None},
                        "procedures": {
                            "stored": ["resumo antigo"],
                            "fresh": ["procedimento oficial"],
                        },
                    },
                    "semanticIssues": [],
                }],
                deferred_review_issues=deferred,
            )

        self.assertEqual("MANUAL_REVIEW_REQUIRED", records["501-00"]["status"])
        self.assertNotIn("verification", records["501-00"])
        self.assertEqual(
            "APPROVAL_CANNOT_RESOLVE_SOURCE_DIFF",
            deferred[0]["reason"],
        )
        self.assertEqual(["points", "procedures"], deferred[0]["unresolvedFields"])

    def test_fresh_record_approval_can_confirm_exact_match(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            verification = {
                "state": "MATCH",
                "verifiedAt": "2026-10-17",
                "verifiedAgainstSourceSha256": "A" * 64,
                "verifiedSourceDocument": "MBFT",
                "verificationMethod": "MANUAL_FIELD_REVIEW",
                "reviewRecordId": "manual-exact-501-00",
                "reviewedFields": ["points"],
                "reviewReason": "Confirmação de extração idêntica.",
                "reviewEvidence": "Ficha oficial.",
            }
            (patch_dir / "approval.json").write_text(
                json.dumps({
                    "resolution": "BASELINE",
                    "recordApprovals": {
                        "501-00": {"verification": verification}
                    },
                }),
                encoding="utf-8",
            )
            records, _ = mbft_pipeline.apply_patch_files(
                {
                    "501-00": {
                        "code": "501-00",
                        "status": "PARTIAL_VALIDATION",
                        "points": None,
                        "whenToCite": [],
                        "whenNotToCite": [],
                        "procedures": [],
                        "aitObservations": [],
                    }
                },
                patch_dir,
                review_differences=[],
            )

        self.assertEqual("VERIFIED", records["501-00"]["status"])
        self.assertEqual(verification, records["501-00"]["verification"])

    def test_amendment_full_upsert_gets_downloaded_source_hash_evidence(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            resolution = "Resolução CONTRAN nº 1003/2023"
            (patch_dir / "1003-2023.json").write_text(
                json.dumps(
                    {
                        "resolution": resolution,
                        "upserts": [
                            {
                                "code": "694-73",
                                "status": "VERIFIED",
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            records, _ = mbft_pipeline.apply_patch_files(
                {},
                patch_dir,
                verified_at="2026-10-17",
                amendment_hashes={resolution: "C" * 64},
            )

        verification = records["694-73"]["verification"]
        self.assertEqual("EXPLICIT_OVERRIDE", verification["state"])
        self.assertEqual("C" * 64, verification["verifiedAgainstSourceSha256"])
        self.assertEqual("AUDITED_AMENDMENT_PATCH", verification["verificationMethod"])



    def test_review_diff_classifies_pure_truncation_as_monotonic_recovery(self):
        item = {
            "fieldDifferences": {
                "procedures": {
                    "stored": ["Texto oficial truncado"],
                    "fresh": ["Texto oficial truncado e agora completo."],
                }
            },
            "semanticIssues": [],
        }
        self.assertEqual(
            "MONOTONIC_SOURCE_RECOVERY",
            mbft_pipeline.classify_review_difference(item),
        )

    def test_review_diff_recovers_field_with_proven_stored_corruption(self):
        item = {
            "fieldDifferences": {
                "procedures": {
                    "stored": ["Observações do AIT:"],
                    "fresh": ["Procedimento oficial completo."],
                }
            },
            "semanticIssues": [],
            "storedSemanticIssues": [
                "502-92: procedures[1] contém cabeçalho de Observações do AIT em procedimentos."
            ],
        }
        self.assertEqual(
            "STRUCTURAL_SOURCE_RECOVERY",
            mbft_pipeline.classify_review_difference(item),
        )

    def test_structural_source_recovery_gets_source_bound_verification(self):
        records = {
            "502-92": {
                "code": "502-92",
                "status": "PARTIAL_VALIDATION",
                "sourcePages": [4],
                "procedures": ["Procedimento oficial completo."],
                "whenToCite": [],
                "whenNotToCite": [],
                "aitObservations": [],
            }
        }
        differences = [{
            "code": "502-92",
            "classification": "STRUCTURAL_SOURCE_RECOVERY",
            "semanticIssues": [],
            "storedSemanticIssues": [
                "502-92: procedures[1] contém cabeçalho de Observações do AIT em procedimentos."
            ],
            "fieldDifferences": {
                "procedures": {
                    "stored": ["Observações do AIT:"],
                    "fresh": ["Procedimento oficial completo."],
                }
            },
        }]

        mbft_pipeline.attach_structural_recovery_verification(
            records,
            differences,
            source_sha256="F" * 64,
            source_document="MBFT oficial",
            verified_at="2026-10-17",
        )

        record = records["502-92"]
        self.assertEqual("VERIFIED", record["status"])
        self.assertEqual("CORRECTED", record["verification"]["state"])
        self.assertEqual(
            "DETERMINISTIC_STRUCTURAL_SOURCE_RECOVERY",
            record["verification"]["verificationMethod"],
        )

    def test_review_diff_accepts_nonempty_directly_anchored_source_correction(self):
        item = {
            "fieldDifferences": {
                "description": {
                    "stored": "Resumo antigo.",
                    "fresh": "Tipificação oficial integral.",
                },
                "procedures": {
                    "stored": ["Procedimento antigo."],
                    "fresh": ["Procedimento oficial integral."],
                },
            },
            "semanticIssues": [],
            "storedSemanticIssues": [],
            "structuredSourceFields": ["description", "procedures"],
        }
        self.assertEqual(
            "STRUCTURED_SOURCE_CORRECTION",
            mbft_pipeline.classify_review_difference(item),
        )

    def test_structured_source_correction_does_not_auto_accept_points_or_empty_removal(self):
        points = {
            "fieldDifferences": {
                "points": {"stored": 7, "fresh": None},
            },
            "semanticIssues": [],
            "storedSemanticIssues": [],
            "structuredSourceFields": ["points"],
        }
        removal = {
            "fieldDifferences": {
                "whenNotToCite": {
                    "stored": ["Texto antigo."],
                    "fresh": [],
                },
            },
            "semanticIssues": [],
            "storedSemanticIssues": [],
            "structuredSourceFields": ["whenNotToCite"],
        }
        self.assertEqual(
            "MANUAL_REVIEW_REQUIRED",
            mbft_pipeline.classify_review_difference(points),
        )
        self.assertEqual(
            "MANUAL_REVIEW_REQUIRED",
            mbft_pipeline.classify_review_difference(removal),
        )

    def test_structured_source_fields_require_all_four_operational_headers(self):
        section = {
            "cells": [
                "Amparo Legal:\nArt. 162, I.",
                "Pontuação:\nNão Computável",
            ],
            "positionedRows": [[
                {"text": "Quando Autuar", "x0": 10.0, "x1": 100.0},
                {"text": "Quando NÃO Autuar", "x0": 110.0, "x1": 200.0},
                {"text": "Definições e Procedimentos", "x0": 210.0, "x1": 320.0},
                {"text": "Observações do AIT", "x0": 330.0, "x1": 430.0},
            ]],
        }

        fields = mbft_pipeline.structured_source_fields(section)

        self.assertIn("article", fields)
        self.assertIn("points", fields)
        self.assertTrue(
            set(mbft_pipeline.OPERATIONAL_DATA_FIELDS).issubset(fields)
        )

    def test_review_diff_keeps_non_monotonic_change_for_manual_review(self):
        item = {
            "fieldDifferences": {
                "points": {"stored": 7, "fresh": None},
            },
            "semanticIssues": [],
        }
        self.assertEqual(
            "MANUAL_REVIEW_REQUIRED",
            mbft_pipeline.classify_review_difference(item),
        )

    def test_source_conflict_is_applied_after_full_upsert(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            patch_dir = Path(temp_dir)
            resolution = "Resolução CONTRAN nº 1.031/2026"
            (patch_dir / "1031-2026.json").write_text(
                json.dumps(
                    {
                        "resolution": resolution,
                        "upserts": [
                            {
                                "code": "516-91",
                                "status": "VERIFIED",
                                "administrativeMeasure": "Recolhimento.",
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            verification = {
                "state": "SOURCE_CONFLICT",
                "verifiedAt": "2026-10-17",
                "verifiedAgainstSourceSha256": "E" * 64,
                "verifiedSourceDocument": resolution,
                "verificationMethod": "MANUAL_SOURCE_CONFLICT_REVIEW",
                "reviewRecordId": "conflict:516-91",
                "reviewedFields": ["administrativeMeasure"],
                "reviewReason": "Conflito entre anexo e corpo da resolução.",
                "reviewEvidence": "Art. 12 versus Anexo III.",
            }
            (patch_dir / "9999-conflicts.json").write_text(
                json.dumps(
                    {
                        "resolution": "BASELINE",
                        "sourceConflicts": {
                            "516-91": {
                                "conflicts": ["Conflito documentado."],
                                "verification": verification,
                            }
                        },
                    }
                ),
                encoding="utf-8",
            )

            records, _ = mbft_pipeline.apply_patch_files(
                {},
                patch_dir,
                verified_at="2026-10-17",
                amendment_hashes={resolution: "E" * 64},
            )

        self.assertEqual("SOURCE_CONFLICT", records["516-91"]["status"])
        self.assertEqual(["Conflito documentado."], records["516-91"]["sourceConflicts"])
        self.assertEqual(verification, records["516-91"]["verification"])

    def test_validate_dataset_rejects_scalar_field_label_contamination(self):
        dataset = {
            "records": [
                {
                    "code": "641-60",
                    "status": "VERIFIED",
                    "article": "Art. 221, Parágrafo único",
                    "shortDescription": "Descrição resumida",
                    "description": "Descrição",
                    "severity": "Média",
                    "offenderResponsibility": "Pessoa Física ou Jurídica",
                    "authority": "Órgão ou Entidade de Trânsito Estadual e Rodoviário.",
                    "verificationMethod": "Mediante abordagem.",
                    "penalty": "Tipificação do Enquadramento: texto indevido",
                    "administrativeMeasure": "Retenção do veículo.",
                    "sources": [{"id": "fonte"}],
                }
            ]
        }

        errors = mbft_pipeline.validate_dataset(dataset)

        self.assertTrue(
            any("campo penalty contém rótulo" in error for error in errors),
            errors,
        )

    def test_verified_override_is_invalidated_when_fresh_content_differs(self):
        fresh_record = {
            "code": "502-92",
            "status": "PARTIAL_VALIDATION",
            "article": "Art. 162, II",
            "shortDescription": "Descrição",
            "description": "Descrição completa",
            "severity": "Gravíssima",
            "points": None,
            "offenderResponsibility": "Condutor",
            "authority": "Órgão estadual",
            "verificationMethod": "Mediante abordagem.",
            "penalty": "Multa (3x)",
            "administrativeMeasure": "Retenção.",
            "canConfigureTrafficCrime": True,
            "whenToCite": ["situação oficial"],
            "whenNotToCite": ["exceção oficial"],
            "procedures": ["procedimento extraído"],
            "aitObservations": ["observação oficial"],
        }
        parsed = {"502-92": dict(fresh_record)}
        override = dict(fresh_record)
        override["status"] = "VERIFIED"
        override["procedures"] = ["procedimento antigo"]
        overrides = {"502-92": override}

        merged, differences = mbft_pipeline.merge_verified_overrides(
            parsed,
            overrides,
        )

        self.assertEqual("PARTIAL_VALIDATION", merged["502-92"]["status"])
        self.assertEqual(["procedimento extraído"], merged["502-92"]["procedures"])
        self.assertEqual(1, len(differences))
        self.assertIn("procedures", differences[0]["fieldDifferences"])

    def test_verified_override_stays_verified_only_on_exact_current_match(self):
        record = {
            "code": "502-92",
            "status": "PARTIAL_VALIDATION",
            "article": "Art. 162, II",
            "shortDescription": "Descrição",
            "description": "Descrição completa",
            "severity": "Gravíssima",
            "points": None,
            "offenderResponsibility": "Condutor",
            "authority": "Órgão estadual",
            "verificationMethod": "Mediante abordagem.",
            "penalty": "Multa (3x)",
            "administrativeMeasure": "Retenção.",
            "canConfigureTrafficCrime": True,
            "whenToCite": ["situação oficial"],
            "whenNotToCite": ["exceção oficial"],
            "procedures": ["procedimento oficial"],
            "aitObservations": ["observação oficial"],
        }
        override = dict(record)
        override["status"] = "VERIFIED"

        merged, differences = mbft_pipeline.merge_verified_overrides(
            {"502-92": dict(record)},
            {"502-92": override},
        )

        self.assertEqual("VERIFIED", merged["502-92"]["status"])
        self.assertEqual([], differences)


    def test_monotonic_source_recovery_is_promoted_with_source_proof(self):
        records = {
            "508-81": {
                "code": "508-81",
                "status": "PARTIAL_VALIDATION",
                "sourcePages": [17],
                "whenNotToCite": [
                    "Texto oficial completo, utilizar enquadramento específico: 506-10."
                ],
            }
        }
        differences = [
            {
                "code": "508-81",
                "classification": "MONOTONIC_SOURCE_RECOVERY",
                "semanticIssues": [],
                "fieldDifferences": {
                    "whenNotToCite": {
                        "stored": ["Texto oficial completo, utilizar"],
                        "fresh": [
                            "Texto oficial completo, utilizar enquadramento específico: 506-10."
                        ],
                    }
                },
            }
        ]

        mbft_pipeline.attach_monotonic_recovery_verification(
            records,
            differences,
            source_sha256="E" * 64,
            source_document="MBFT oficial",
            verified_at="2026-10-17",
        )

        record = records["508-81"]
        self.assertEqual("VERIFIED", record["status"])
        self.assertEqual("CORRECTED", record["verification"]["state"])
        self.assertEqual(
            ["whenNotToCite"],
            record["verification"]["reviewedFields"],
        )
        self.assertIn("17", record["verification"]["reviewEvidence"])

    def test_structured_source_correction_gets_source_proof(self):
        records = {
            "504-50": {
                "code": "504-50",
                "status": "PARTIAL_VALIDATION",
                "sourcePages": [8],
                "description": "Texto oficial integral.",
                "whenToCite": ["Situação oficial."],
                "whenNotToCite": [],
                "procedures": [],
                "aitObservations": [],
            }
        }
        differences = [{
            "code": "504-50",
            "classification": "STRUCTURED_SOURCE_CORRECTION",
            "structuredSourceFields": ["description"],
            "fieldDifferences": {
                "description": {
                    "stored": "Texto antigo.",
                    "fresh": "Texto oficial integral.",
                }
            },
        }]

        mbft_pipeline.attach_structured_source_correction_verification(
            records,
            differences,
            source_sha256="1" * 64,
            source_document="MBFT oficial",
            verified_at="2026-10-17",
        )

        self.assertEqual("VERIFIED", records["504-50"]["status"])
        self.assertEqual(
            "DETERMINISTIC_STRUCTURED_SOURCE_CORRECTION",
            records["504-50"]["verification"]["verificationMethod"],
        )

    def test_non_monotonic_recovery_is_not_promoted(self):
        records = {
            "501-00": {
                "code": "501-00",
                "status": "PARTIAL_VALIDATION",
                "sourcePages": [1],
                "points": None,
            }
        }
        differences = [
            {
                "code": "501-00",
                "classification": "MANUAL_REVIEW_REQUIRED",
                "semanticIssues": [],
                "fieldDifferences": {
                    "points": {"stored": 7, "fresh": None},
                },
            }
        ]

        mbft_pipeline.attach_monotonic_recovery_verification(
            records,
            differences,
            source_sha256="E" * 64,
            source_document="MBFT oficial",
            verified_at="2026-10-17",
        )

        self.assertEqual("PARTIAL_VALIDATION", records["501-00"]["status"])
        self.assertNotIn("verification", records["501-00"])

    def test_exact_match_verification_binds_record_to_source_hash(self):
        records = {
            "502-92": {
                "code": "502-92",
                "status": "VERIFIED",
                "sourcePages": [12, 13],
            }
        }

        mbft_pipeline.attach_exact_match_verification(
            records,
            source_sha256="D" * 64,
            source_document="MBFT",
            verified_at="2026-10-17",
        )

        verification = records["502-92"]["verification"]
        self.assertEqual("MATCH", verification["state"])
        self.assertEqual("D" * 64, verification["verifiedAgainstSourceSha256"])
        self.assertIn("12", verification["reviewEvidence"])
        self.assertEqual(
            [],
            mbft_pipeline.verification_evidence_issues(records["502-92"]),
        )

    def test_verified_without_evidence_is_rejected(self):
        issues = mbft_pipeline.verification_evidence_issues(
            {"code": "501-00", "status": "VERIFIED"}
        )
        self.assertTrue(any("sem prova" in issue for issue in issues), issues)

    def test_verified_rejects_unknown_final_verification_state(self):
        record = {
            "code": "501-00",
            "status": "VERIFIED",
            "verification": {
                "state": "AUTO_ACCEPTED",
                "verifiedAt": "2026-10-17",
                "verifiedAgainstSourceSha256": "A" * 64,
                "verifiedSourceDocument": "MBFT",
                "verificationMethod": "TEST",
                "reviewRecordId": "test:501-00",
                "reviewedFields": ["points"],
                "reviewReason": "Teste.",
                "reviewEvidence": "Teste.",
            },
        }

        issues = mbft_pipeline.verification_evidence_issues(record)

        self.assertTrue(
            any("estado final de verificação não aceito" in issue for issue in issues),
            issues,
        )

    def test_source_conflict_requires_matching_state_and_description(self):
        base_verification = {
            "state": "MATCH",
            "verifiedAt": "2026-10-17",
            "verifiedAgainstSourceSha256": "B" * 64,
            "verifiedSourceDocument": "MBFT",
            "verificationMethod": "TEST",
            "reviewRecordId": "test:611-40",
            "reviewedFields": ["severity", "points"],
            "reviewReason": "Teste.",
            "reviewEvidence": "Teste.",
        }
        record = {
            "code": "611-40",
            "status": "SOURCE_CONFLICT",
            "sourceConflicts": [],
            "verification": base_verification,
        }

        issues = mbft_pipeline.verification_evidence_issues(record)

        self.assertTrue(
            any("exige prova com state SOURCE_CONFLICT" in issue for issue in issues),
            issues,
        )
        self.assertTrue(
            any("sem descrição explícita" in issue for issue in issues),
            issues,
        )


    def test_orphan_article_in_procedure_is_reconstructed_from_unique_ctb_match(self):
        record = {
            "code": "541-00",
            "procedures": [
                "Art.",
                "Nas paradas, operações de carga ou descarga e nos estacionamentos, o veículo deverá ser posicionado no sentido do fluxo.",
            ],
        }
        ctb = [
            {
                "label": "Art. 48",
                "text": "Art. 48. Nas paradas, operações de carga ou descarga e nos estacionamentos, o veículo deverá ser posicionado no sentido do fluxo.",
            },
            {
                "label": "Art. 49",
                "text": "Art. 49. O condutor e os passageiros não deverão abrir a porta do veículo.",
            },
        ]

        repairs = mbft_pipeline.repair_orphan_article_procedures(record, ctb)

        self.assertEqual(1, len(repairs))
        self.assertEqual(1, len(record["procedures"]))
        self.assertTrue(record["procedures"][0].startswith("Art. 48. Nas paradas"))

    def test_orphan_article_remains_for_review_when_ctb_match_is_not_unique(self):
        record = {"code": "000-00", "procedures": ["Art.", "Texto repetido de exemplo oficial."]}
        ctb = [
            {"label": "Art. 1", "text": "Art. 1. Texto repetido de exemplo oficial."},
            {"label": "Art. 2", "text": "Art. 2. Texto repetido de exemplo oficial."},
        ]

        repairs = mbft_pipeline.repair_orphan_article_procedures(record, ctb)

        self.assertEqual([], repairs)
        self.assertEqual(["Art.", "Texto repetido de exemplo oficial."], record["procedures"])

    def test_split_numbered_does_not_split_article_number(self):
        self.assertEqual(
            ["Aplicar conforme Art. 2. do regulamento vigente."],
            mbft_pipeline.split_numbered(
                "Aplicar conforme Art. 2. do regulamento vigente."
            ),
        )

    def test_split_numbered_joins_lowercase_conjunction_continuation(self):
        value = "1. Primeiro item termina em trecho 639 2. ou onde haja intensa movimentação. 3. Terceiro item."
        self.assertEqual(
            [
                "Primeiro item termina em trecho 639 ou onde haja intensa movimentação.",
                "Terceiro item.",
            ],
            mbft_pipeline.split_numbered(value),
        )

    def test_split_numbered_removes_strict_prefix_continuation_fragments(self):
        value = (
            "1. Proprietário do veículo com dano irrecuperável que não requereu a baixa. "
            "2. Proprietário do veículo "
            "3. Companhia Seguradora com veículo definitivamente desmontado. "
            "4. Companhia Segurador"
        )

        self.assertEqual(
            [
                "Proprietário do veículo com dano irrecuperável que não requereu a baixa.",
                "Companhia Seguradora com veículo definitivamente desmontado.",
            ],
            mbft_pipeline.split_numbered(value),
        )

    def test_semantic_gate_rejects_prefix_fragment_duplicate(self):
        record = {
            "code": "699-80",
            "status": "VERIFIED",
            "whenToCite": [],
            "whenNotToCite": [],
            "procedures": [
                'O veículo enquadrado na categoria "dano de grande monta" deve ser classificado como irrecuperável.',
                "O veículo enquadrado na",
            ],
            "aitObservations": [],
        }

        issues = mbft_pipeline.semantic_record_issues(record)

        self.assertTrue(
            any("fragmento prefixo duplicado" in issue for issue in issues),
            issues,
        )

    def test_validate_dataset_rejects_operational_structural_corruption(self):
        base = {
            "code": "501-00",
            "status": "VERIFIED",
            "article": "Art. 162, I",
            "shortDescription": "Descrição",
            "description": "Descrição completa",
            "severity": "Gravíssima",
            "offenderResponsibility": "Condutor",
            "authority": "Órgão estadual",
            "verificationMethod": "Mediante abordagem.",
            "penalty": "Multa",
            "administrativeMeasure": "Retenção.",
            "sources": [{"id": "fonte"}],
            "whenToCite": [],
            "whenNotToCite": ["Outra hipótese, utilizar"],
            "procedures": ["Observações do AIT:"],
            "aitObservations": [
                "Art.",
                "Aprendiz conduzindo veículo não destinado à aprendizagem. possuir",
            ],
        }

        errors = mbft_pipeline.validate_dataset({"records": [base]})

        self.assertTrue(any("termina em 'utilizar'" in error for error in errors))
        self.assertTrue(any("cabeçalho de Observações do AIT" in error for error in errors))
        self.assertTrue(any("token órfão Art" in error for error in errors))
        self.assertTrue(any("cauda corrompida '. possuir'" in error for error in errors))



if __name__ == "__main__":
    unittest.main()
