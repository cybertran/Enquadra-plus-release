import unittest

from fulltext_pipeline import (
    direct_ctb_article_evidence,
    evidence_matches_infraction_subdivision,
    inactive,
    extraction_failure_category,
    unaccepted_extraction_failures,
)


class DirectCtbEvidenceTest(unittest.TestCase):

    def articles(self, text: str) -> set[int]:
        result: set[int] = set()
        for articles, _ in direct_ctb_article_evidence(text):
            result.update(articles)
        return result

    def test_suspended_act_is_inactive(self) -> None:
        self.assertTrue(
            inactive(
                {
                    "status": (
                        "Suspensa por Tempo Indeterminado pela Deliberação 170"
                    )
                }
            )
        )

    def test_active_act_is_not_inactive(self) -> None:
        self.assertFalse(inactive({"status": "Em vigor"}))

    def test_accepts_direct_ctb_article(self) -> None:
        self.assertEqual(
            self.articles("Regulamenta o art. 276 do Código de Trânsito Brasileiro."),
            {276},
        )

    def test_accepts_direct_ctb_article_list(self) -> None:
        self.assertEqual(
            self.articles("Regulamenta os arts. 231 e 323 do CTB."),
            {231, 323},
        )

    def test_rejects_article_of_resolution_before_later_ctb_article(self) -> None:
        text = (
            "Altera o artigo 6º da Resolução 405, de 2012, que dispõe sobre "
            "o artigo 67-A, incluído no Código de Trânsito Brasileiro - CTB."
        )
        articles = self.articles(text)
        self.assertNotIn(6, articles)
        self.assertIn(67, articles)

    def test_rejects_normative_article_without_ctb_context(self) -> None:
        self.assertEqual(
            self.articles("Altera o art. 17-B da Resolução CONTRAN nº 258/2007."),
            set(),
        )


    def test_rejects_different_inciso_of_same_ctb_article(self) -> None:
        record = {"article": "Art. 162, I."}
        evidence = "Aplica-se ao inciso V do art. 162 do CTB."
        self.assertFalse(
            evidence_matches_infraction_subdivision(record, evidence)
        )

    def test_accepts_matching_inciso_of_same_ctb_article(self) -> None:
        record = {"article": "Art. 162, V."}
        evidence = "Aplica-se ao inciso V do art. 162 do CTB."
        self.assertTrue(
            evidence_matches_infraction_subdivision(record, evidence)
        )

    def test_accepts_whole_article_reference_for_specific_infraction(self) -> None:
        record = {"article": "Art. 162, I."}
        evidence = "Regulamenta o art. 162 do CTB."
        self.assertTrue(
            evidence_matches_infraction_subdivision(record, evidence)
        )


    def test_index_only_failure_is_distinguished_from_extraction_error(self) -> None:
        act = {
            "officialUrl": "https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/portarias-2026",
            "sourcePageUrl": "https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/portarias-2026",
        }
        self.assertEqual(
            "INDEX_ONLY_NO_DOCUMENT_URL",
            extraction_failure_category(
                act,
                "unsupported document type: unknown",
            ),
        )

    def test_legacy_doc_failure_is_classified_separately(self) -> None:
        act = {
            "officialUrl": "https://www.gov.br/transportes/pt-br/assuntos/transito/arquivo.doc",
            "sourcePageUrl": "https://www.gov.br/transportes/pt-br/assuntos/transito/resolucoes",
        }
        self.assertEqual(
            "LEGACY_DOC_EXTRACTION_FAILURE",
            extraction_failure_category(
                act,
                "antiword returned non-zero exit status 1",
            ),
        )

    def test_only_documented_fulltext_failure_categories_are_accepted(self) -> None:
        failures = [
            {"actId": "legacy", "category": "LEGACY_DOC_EXTRACTION_FAILURE"},
            {"actId": "index-only", "category": "INDEX_ONLY_NO_DOCUMENT_URL"},
            {"actId": "unexpected", "category": "EXTRACTION_FAILURE"},
        ]

        self.assertEqual(
            [{"actId": "unexpected", "category": "EXTRACTION_FAILURE"}],
            unaccepted_extraction_failures(failures),
        )

    def test_common_word_after_comma_is_not_parsed_as_inciso(self) -> None:
        record = {"article": "Art. 162, I."}
        evidence = "O art. 162, mesmo quando citado no CTB, permanece aplicável."
        self.assertTrue(
            evidence_matches_infraction_subdivision(record, evidence)
        )


if __name__ == "__main__":
    unittest.main()
