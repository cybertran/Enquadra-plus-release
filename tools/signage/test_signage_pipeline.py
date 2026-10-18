import json
import unittest
from pathlib import Path

from signage_pipeline import parse_toc_line


class SignageContentsParserTest(unittest.TestCase):

    def test_parses_numbered_contents_entry(self) -> None:
        self.assertEqual(
            parse_toc_line(
                "4.1 Linhas de divisão de fluxos opostos (LFO) "
                "................................................ 10"
            ),
            ("4.1", "Linhas de divisão de fluxos opostos (LFO)", 10),
        )

    def test_parses_section_heading(self) -> None:
        self.assertEqual(
            parse_toc_line(
                "SEÇÃO I – CRUZAMENTOS COM TRENS "
                "............................................... 11"
            ),
            ("SEÇÃO I", "CRUZAMENTOS COM TRENS", 11),
        )

    def test_rejects_body_sentence(self) -> None:
        self.assertIsNone(
            parse_toc_line(
                "1. O condutor deve observar as condições da via."
            ),
        )


    def test_uses_current_official_manuals_for_volumes_two_and_three(self) -> None:
        manifest = json.loads(
            Path("tools/signage/source-manifest.json").read_text(encoding="utf-8")
        )
        by_volume = {item["volume"]: item["url"] for item in manifest["volumes"]}
        self.assertIn("/docs/copy_of___02___MBST_Vol._II", by_volume["II"])
        self.assertIn("/docs/copy_of___03___MBST_Vol._III", by_volume["III"])
        self.assertNotIn("/educacao/publicacoes/", by_volume["II"])
        self.assertNotIn("/educacao/publicacoes/", by_volume["III"])


if __name__ == "__main__":
    unittest.main()
