#!/usr/bin/env python3
"""Deterministic validation for the v0.6 tooling audit artifacts."""

from __future__ import annotations

import json
import re
from pathlib import Path
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parents[2]
EQUIPMENT = ROOT / "app/src/main/assets/tools/equipment_res993.json"
CTB = ROOT / "app/src/main/assets/ctb/articles.json"
DICTIONARY_UI = (
    ROOT
    / "app/src/main/java/com/cybertran/enquadraplus/ui/TrafficDictionaryToolScreen.kt"
)
DICTIONARY_AUDIT = ROOT / "tools/audit/dictionary_audit.json"
CNH_UI = ROOT / "app/src/main/java/com/cybertran/enquadraplus/ui/CnhToolScreen.kt"
TOW_UI = ROOT / "app/src/main/java/com/cybertran/enquadraplus/ui/TowHitchToolScreen.kt"
APP_UI = ROOT / "app/src/main/java/com/cybertran/enquadraplus/ui/EnquadraApp.kt"


def validate_equipment() -> None:
    data = json.loads(EQUIPMENT.read_text(encoding="utf-8"))

    assert data["schemaVersion"] == 1
    assert data["sourcePageCount"] == 53
    parsed = urlparse(data["sourceUrl"])
    assert parsed.scheme == "https"
    assert parsed.hostname in {"gov.br", "www.gov.br"}
    assert parsed.path.endswith("/Resolucao9932023Anexo.pdf")

    page_audit = data["pageAudit"]
    assert len(page_audit) == 53
    assert [row["page"] for row in page_audit] == list(range(1, 54))
    assert all(row["status"] == "CELL_BY_CELL_VERIFIED" for row in page_audit)

    annex = data["annexI"]
    vehicles = annex["vehicleTypes"]
    requirements = annex["requirements"]
    notes = annex["notes"]

    assert len(vehicles) == 16
    assert len({row["id"] for row in vehicles}) == 16
    assert [sum(1 for row in vehicles if row["table"] == table) for table in range(1, 5)] == [
        5,
        4,
        2,
        5,
    ]
    assert len(requirements) == 53
    assert len({row["id"] for row in requirements}) == 53
    assert set(map(int, notes.keys())) == set(range(1, 50))

    vehicle_ids = {row["id"] for row in vehicles}
    explicit_cells = 0
    required_cells = 0
    referenced_notes: set[int] = set()

    for requirement in requirements:
        assert "sourcePage" not in requirement
        cells = requirement["cells"]
        assert set(cells) == vehicle_ids
        explicit_cells += len(cells)

        for value in cells.values():
            assert re.fullmatch(r"-|X(?::\d+(?:,\d+)*)?", value), value
            if value == "-":
                continue

            required_cells += 1
            if ":" in value:
                note_refs = {int(part) for part in value.split(":", 1)[1].split(",")}
                assert note_refs <= set(range(1, 50))
                referenced_notes |= note_refs

    assert explicit_cells == 848 == annex["explicitCellCount"]
    assert required_cells == annex["requiredCellCount"]
    assert required_cells > 0
    assert referenced_notes <= set(map(int, notes.keys()))

    assert data["annexIIAudit"]["status"] == "CELL_BY_CELL_VERIFIED"
    assert data["annexIIAudit"]["pages"] == "13-51"
    assert data["annexIIIAudit"]["status"] == "CELL_AND_ILLUSTRATION_VERIFIED"
    assert data["annexIIIAudit"]["pages"] == "51-53"


def validate_dictionary() -> None:
    source = DICTIONARY_UI.read_text(encoding="utf-8")
    entries = re.findall(
        r'TrafficGlossaryEntry\(\s*"([^"]+)",\s*"([^"]+)",\s*\)',
        source,
        flags=re.DOTALL,
    )
    assert len(entries) == 37
    assert len({term for term, _ in entries}) == 37

    proof = json.loads(DICTIONARY_AUDIT.read_text(encoding="utf-8"))
    assert proof["entryCount"] == 37
    proof_entries = proof["entries"]
    assert len(proof_entries) == 37
    assert {entry["term"] for entry in proof_entries} == {term for term, _ in entries}
    assert sum(entry["status"] == "AJUSTADO" for entry in proof_entries) == 18
    assert all(entry["status"] in {"AJUSTADO", "CONFORME"} for entry in proof_entries)

    ctb = json.loads(CTB.read_text(encoding="utf-8"))
    annex = next(record for record in ctb["records"] if record["id"] == "anexo-i")
    legal_text = annex["text"]

    for entry in proof_entries:
        heading = entry["officialHeading"]
        assert f"\n{heading} -" in legal_text, heading
        assert entry["rationale"].strip()


def validate_targeted_corrections() -> None:
    cnh = CNH_UI.read_text(encoding="utf-8")
    assert "Motor-casa: até 6.000 kg ou até 8 lugares" in cnh
    assert "Basta uma das condições do art. 143, § 2º" in cnh
    assert "Desde que a lotação não exceda 8 lugares" not in cnh
    assert (
        "Categoria D" in cnh
        and "no máximo 1 gravíssima nos últimos 12 meses + 2 anos na B ou 1 ano na C"
        in cnh
    )
    assert (
        "Categoria E" in cnh
        and "no máximo 1 gravíssima nos últimos 12 meses + 1 ano na C" in cnh
    )

    dictionary = DICTIONARY_UI.read_text(encoding="utf-8")
    assert (
        "inclui policial militar e os agentes do art. 25-A quando designados "
        "pela autoridade com circunscrição sobre a via, mediante convênio."
        in dictionary
    )
    assert "inclui os demais agentes legalmente designados" not in dictionary

    tow = TOW_UI.read_text(encoding="utf-8")
    assert 'TowCheck("invoice"' in tow
    assert "Nota de venda do engate identifica o veículo" in tow

    app = APP_UI.read_text(encoding="utf-8")
    for title in (
        "Triagem / pré-teste",
        "Etilômetro",
        "Recusa",
        "Registro no AIT",
        "Crime de trânsito",
        "Veículo e habilitação",
    ):
        assert f'title = "{title}"' in app
    assert "errorMargin = errorMargin," in app
    assert "errorMargin.setScale(3" not in app


def main() -> None:
    validate_equipment()
    validate_dictionary()
    validate_targeted_corrections()
    print("Tooling audit validation passed: 53-page equipment source, 848 Annex I cells, 37 glossary terms.")


if __name__ == "__main__":
    main()
