from __future__ import annotations

import copy
import unittest

from tools.mbft.apply_review import apply_review


class ApplyReviewTest(unittest.TestCase):
    def setUp(self) -> None:
        self.dataset = {
            "records": [
                {
                    "code": "502-92",
                    "status": "PARTIAL_VALIDATION",
                    "sources": [{"lastCheckedAt": "2026-10-10"}],
                },
                {
                    "code": "503-71",
                    "status": "VERIFIED",
                    "sources": [{"lastCheckedAt": "2026-10-10"}],
                },
                {
                    "code": "764-10",
                    "status": "PARTIAL_VALIDATION",
                    "sources": [{"lastCheckedAt": "2026-10-10"}],
                },
            ]
        }

    def test_promotes_partial_and_updates_source_check_date(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": ["502-92"],
            "held": [],
        }
        changed = apply_review(self.dataset, review)
        self.assertEqual(changed, 1)
        self.assertEqual(self.dataset["records"][0]["status"], "VERIFIED")
        self.assertEqual(
            self.dataset["records"][0]["sources"][0]["lastCheckedAt"],
            "2026-10-13",
        )

    def test_applies_reviewed_field_patch(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": ["502-92"],
            "held": [],
            "recordPatches": {
                "502-92": {
                    "article": "Art. 162, II",
                    "legalBasis": ["Art. 162, II"],
                }
            },
        }
        changed = apply_review(self.dataset, review)
        self.assertEqual(changed, 1)
        self.assertEqual(self.dataset["records"][0]["article"], "Art. 162, II")
        self.assertEqual(
            self.dataset["records"][0]["legalBasis"],
            ["Art. 162, II"],
        )

    def test_removes_superseded_codes_idempotently(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": [],
            "removeCodes": ["764-10"],
            "held": [],
        }
        changed = apply_review(self.dataset, review)
        self.assertEqual(changed, 0)
        self.assertNotIn(
            "764-10",
            [record["code"] for record in self.dataset["records"]],
        )
        changed_again = apply_review(self.dataset, review)
        self.assertEqual(changed_again, 0)

    def test_rejects_promoted_and_removed_overlap(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": ["502-92"],
            "removeCodes": ["502-92"],
            "held": [],
        }
        with self.assertRaisesRegex(ValueError, "both promoted and removed"):
            apply_review(copy.deepcopy(self.dataset), review)

    def test_is_idempotent_for_already_verified_record(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": ["503-71"],
            "held": [],
        }
        changed = apply_review(self.dataset, review)
        self.assertEqual(changed, 0)
        self.assertEqual(self.dataset["records"][1]["status"], "VERIFIED")

    def test_rejects_promoted_and_held_overlap(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": ["502-92"],
            "held": [{"code": "502-92", "reason": "test"}],
        }
        with self.assertRaisesRegex(ValueError, "both promoted and held"):
            apply_review(copy.deepcopy(self.dataset), review)

    def test_rejects_unknown_code(self) -> None:
        review = {
            "reviewedAt": "2026-10-13",
            "promotedCodes": ["999-99"],
            "held": [],
        }
        with self.assertRaisesRegex(ValueError, "missing from dataset"):
            apply_review(copy.deepcopy(self.dataset), review)


if __name__ == "__main__":
    unittest.main()
