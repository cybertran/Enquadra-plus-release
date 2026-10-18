from __future__ import annotations

import unittest

from tools.norms.normative_pipeline import (
    SourcePage,
    dataset_version,
    parse_contran,
    parse_contran_deliberations,
    parse_senatran,
    parse_year,
    validate_source_url,
)


class NormativePipelineTest(unittest.TestCase):
    def test_parses_contran_index_row(self) -> None:
        source = """
        <table>
          <tr><th>Resolução</th><th>Data</th><th>Publicação</th><th>Assunto</th><th>Situação</th></tr>
          <tr>
            <td><a href="/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/r1031.pdf">1031</a></td>
            <td>17.08.26</td><td>18.08.26</td>
            <td>Fiscalização do consumo de álcool.</td>
            <td>Em vigor. Revoga a Resolução 432/13.</td>
          </tr>
        </table>
        """
        page = SourcePage(
            agency="CONTRAN",
            act_type="RESOLUÇÃO",
            url="https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/resolucoes-contran",
            year=None,
        )

        records = parse_contran(page, source, "2026-10-13")

        self.assertEqual(len(records), 1)
        self.assertEqual(records[0]["id"], "contran-resolucao-2026-1031")
        self.assertEqual(records[0]["year"], 2026)
        self.assertIn("álcool", records[0]["subject"])
        self.assertTrue(records[0]["officialUrl"].endswith("r1031.pdf"))

    def test_parses_contran_deliberation_row(self) -> None:
        source = """
        <table>
          <tr><th>Deliberação/Portaria</th><th>Publicação</th><th>Assunto</th><th>Observação</th></tr>
          <tr>
            <td><a href="/transportes/pt-br/assuntos/transito/conteudo-contran/deliberacoes/d279.pdf">Deliberação 279</a></td>
            <td>09 de Setembro de 2026</td>
            <td>Prorroga excepcionalmente a validade da CNH.</td>
            <td></td>
          </tr>
        </table>
        """
        page = SourcePage(
            agency="CONTRAN",
            act_type="DELIBERAÇÃO",
            url="https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/deliberacoes-contran",
            year=None,
        )

        records = parse_contran_deliberations(page, source, "2026-10-15")

        self.assertEqual(len(records), 1)
        self.assertEqual(records[0]["id"], "contran-deliberacao-2026-279")
        self.assertEqual(records[0]["year"], 2026)
        self.assertEqual(records[0]["actType"], "DELIBERAÇÃO")

    def test_parses_senatran_index_row(self) -> None:
        source = """
        <table>
          <tr><th>Portaria</th><th>Publicação</th><th>Assunto</th><th>Observação</th></tr>
          <tr>
            <td><a href="/transportes/pt-br/assuntos/transito/arquivos-senatran/portarias/2022/Portaria3542022.pdf">Portaria 354</a></td>
            <td>01.04.2022</td>
            <td>Estabelece tabela de códigos de enquadramento.</td>
            <td>Em vigor</td>
          </tr>
        </table>
        """
        page = SourcePage(
            agency="SENATRAN",
            act_type="PORTARIA",
            url="https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/portarias-2022",
            year=2022,
        )

        records = parse_senatran(page, source, "2026-10-13")

        self.assertEqual(len(records), 1)
        self.assertEqual(records[0]["id"], "senatran-portaria-2022-354")
        self.assertEqual(records[0]["year"], 2022)
        self.assertEqual(records[0]["status"], "Em vigor")

    def test_rejects_non_official_source(self) -> None:
        with self.assertRaisesRegex(ValueError, "Unsupported official source URL"):
            validate_source_url("https://example.com/normas")

    def test_short_year_uses_correct_century(self) -> None:
        self.assertEqual(parse_year("01.01.99"), 1999)
        self.assertEqual(parse_year("01.01.98"), 1998)
        self.assertEqual(parse_year("01.01.26"), 2026)
        self.assertEqual(parse_year("09 de Setembro de 2026"), 2026)

    def test_version_depends_on_canonical_hash(self) -> None:
        self.assertNotEqual(
            dataset_version("2026-10-13", "A" * 64),
            dataset_version("2026-10-13", "B" * 64),
        )


if __name__ == "__main__":
    unittest.main()
