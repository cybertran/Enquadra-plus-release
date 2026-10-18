from __future__ import annotations

import unittest

from tools.ctb.ctb_pipeline import (
    Article,
    dataset_version,
    parse_annex_i,
    parse_articles,
    validate_records,
    validate_source_url,
)


class CtbPipelineTest(unittest.TestCase):
    def test_extracts_current_articles_and_skips_struck_versions(self) -> None:
        source = """
        <html><body>
          <h2>CAPÍTULO I</h2>
          <p>Art. 1º Texto vigente.</p>
          <strike><p>Art. 2º Texto revogado.</p></strike>
          <p><span style="text-decoration: line-through">Art. 2º Outra redação antiga.</span></p>
          <p>Art. 2º Texto vigente.</p>
          <p>Seção I</p>
          <p>Art. 165-A. Recusar-se a procedimento.</p>
          <p>art. 59 do Código Penal é apenas uma referência interna.</p>
          <p>Art.</p><p>139-B. Cabeçalho quebrado no HTML.</p>
          <p>Art. 341. Disposição final.</p>
          <p>ANEXO I</p>
          <p>DOS CONCEITOS E DEFINIÇÕES</p>
          <p>ACOSTAMENTO - definição.</p>
          <p>VIADUTO - definição.</p>
          <p>(Vide Resolução nº 160, de 2004 do CONTRAN)</p>
        </body></html>
        """

        records = parse_articles(source)

        self.assertEqual(
            [record.id for record in records],
            ["art-1", "art-2", "art-165-a", "art-139-b", "art-341"],
        )
        self.assertNotIn("revogado", records[1].text)
        self.assertNotIn("Outra redação antiga", records[1].text)
        self.assertEqual(records[2].section, "Seção I")
        self.assertIn("art. 59 do Código Penal", records[2].text)
        self.assertEqual(records[3].label, "Art. 139-B")
        self.assertNotIn("ACOSTAMENTO", records[-1].text)

        annex = parse_annex_i(source)
        self.assertEqual(annex.id, "anexo-i")
        self.assertIn("ACOSTAMENTO - definição.", annex.text)
        self.assertIn("VIADUTO - definição.", annex.text)
        self.assertNotIn("Resolução nº 160", annex.text)

    def test_dataset_version_changes_with_source_hash(self) -> None:
        first = dataset_version("2026-10-13", "A" * 64)
        second = dataset_version("2026-10-13", "B" * 64)

        self.assertNotEqual(first, second)
        self.assertEqual(first, "2026-10-13-planalto-aaaaaaaaaaaa")

    def test_source_url_rejects_non_official_hosts(self) -> None:
        with self.assertRaisesRegex(ValueError, "exact official compiled-text URL"):
            validate_source_url("https://example.com/ctb.html")

    def test_validation_rejects_duplicate_ids(self) -> None:
        record = Article(
            id="art-165-a",
            label="Art. 165-A",
            number=165,
            suffix="A",
            chapter=None,
            section=None,
            subsection=None,
            text="Art. 165-A. Texto.",
        )

        errors = validate_records([record, record], min_articles=1)

        self.assertTrue(any("Duplicate article IDs" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
