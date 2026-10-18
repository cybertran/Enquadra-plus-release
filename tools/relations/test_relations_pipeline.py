from __future__ import annotations

import unittest

from tools.relations.relations_pipeline import (
    build_dataset,
    classify,
    collect_unresolved_references,
    extract_target_codes,
    validate_dataset,
)


class RelationsPipelineTest(unittest.TestCase):
    def test_classifies_only_high_confidence_rules(self) -> None:
        self.assertEqual(
            classify("whenNotToCite", "utilizar 504-50"),
            "ALTERNATIVE",
        )
        self.assertEqual(
            classify("procedures", "utilizar concomitantemente 691-20"),
            "CONCOMITANT",
        )
        self.assertEqual(
            classify("procedures", "autuação deve ser precedida pela lavratura 501-00"),
            "PREREQUISITE",
        )
        self.assertIsNone(
            classify("procedures", "consultar a ficha 501-00"),
        )
        self.assertIsNone(
            classify(
                "procedures",
                "ocorrência concomitante de manifestação; utilizar 531-20",
            ),
        )

    def test_concomitant_extraction_ignores_codes_before_instruction(self) -> None:
        text = (
            "texto misturado: 686-61 ou 686-62; "
            "lavrar também outro AIT, utilizando enquadramento específico: 691-20."
        )

        targets = extract_target_codes(
            "procedures",
            text,
            "CONCOMITANT",
            {"686-61", "686-62", "691-20"},
        )

        self.assertEqual(targets, ["691-20"])

    def test_reports_unresolved_codes_without_generating_them(self) -> None:
        mbft = {
            "version": "v1",
            "verifiedAt": "2026-10-13",
            "records": [
                {
                    "code": "501-00",
                    "status": "VERIFIED",
                    "whenNotToCite": ["Utilizar 999-99 em situação específica."],
                    "procedures": [],
                    "related": [],
                },
            ],
        }

        unresolved = collect_unresolved_references(mbft)

        self.assertEqual(len(unresolved), 1)
        self.assertEqual(unresolved[0]["unresolvedCodes"], ["999-99"])

    def test_builds_official_and_curated_relations(self) -> None:
        mbft = {
            "version": "v1",
            "verifiedAt": "2026-10-13",
            "records": [
                {
                    "code": "501-00",
                    "status": "VERIFIED",
                    "whenNotToCite": [
                        "CNH vencida: utilizar 504-50, art. 162, V.",
                    ],
                    "procedures": [
                        "Se necessário, utilizar concomitantemente 691-20, art. 232.",
                        "A autuação deve ser precedida pela lavratura 504-50.",
                    ],
                    "related": [
                        {"code": "518-51", "reason": "Relação curada adicional."},
                    ],
                },
                {
                    "code": "504-50",
                    "status": "VERIFIED",
                    "whenNotToCite": [],
                    "procedures": [],
                    "related": [],
                },
                {
                    "code": "691-20",
                    "status": "VERIFIED",
                    "whenNotToCite": [],
                    "procedures": [],
                    "related": [],
                },
                {
                    "code": "518-51",
                    "status": "VERIFIED",
                    "whenNotToCite": [],
                    "procedures": [],
                    "related": [],
                },
            ],
        }

        dataset = build_dataset(mbft)

        self.assertEqual(dataset["recordCount"], 4)
        self.assertFalse(validate_dataset(dataset, mbft))
        types = {item["relationType"] for item in dataset["records"]}
        self.assertEqual(
            types,
            {"ALTERNATIVE", "CONCOMITANT", "PREREQUISITE", "RELATED"},
        )

    def test_does_not_derive_official_rule_from_partial_source(self) -> None:
        mbft = {
            "version": "v1",
            "verifiedAt": "2026-10-13",
            "records": [
                {
                    "code": "501-00",
                    "status": "PARTIAL_VALIDATION",
                    "whenNotToCite": ["utilizar 504-50"],
                    "procedures": [],
                    "related": [],
                },
                {
                    "code": "504-50",
                    "status": "VERIFIED",
                    "whenNotToCite": [],
                    "procedures": [],
                    "related": [],
                },
            ],
        }

        with self.assertRaisesRegex(ValueError, "no concomitant relations"):
            build_dataset(mbft)


if __name__ == "__main__":
    unittest.main()
