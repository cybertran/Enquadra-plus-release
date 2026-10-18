import unittest

from fulltext_pipeline import (
    direct_ctb_article_evidence,
    evidence_matches_infraction_subdivision,
)


class DirectCtbEvidenceTest(unittest.TestCase):

    def articles(self, text: str) -> set[int]:
        result: set[int] = set()
        for articles, _ in direct_ctb_article_evidence(text):
            result.update(articles)
        return result

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


    def test_common_word_after_comma_is_not_parsed_as_inciso(self) -> None:
        record = {"article": "Art. 162, I."}
        evidence = "O art. 162, mesmo quando citado no CTB, permanece aplicável."
        self.assertTrue(
            evidence_matches_infraction_subdivision(record, evidence)
        )


if __name__ == "__main__":
    unittest.main()
