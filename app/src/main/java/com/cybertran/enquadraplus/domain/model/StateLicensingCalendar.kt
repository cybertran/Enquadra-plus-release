package com.cybertran.enquadraplus.domain.model

data class StateLicensingCalendar(
    val uf: String,
    val stateName: String,
    val year: Int,
    val sourceLabel: String,
    val sourceUrl: String,
    val verifiedAt: String,
    val entries: List<StateLicensingDeadline>,
) {
    fun deadlineFor(plateEnding: Int): StateLicensingDeadline? =
        entries.firstOrNull { plateEnding in it.plateEndings }
}

data class StateLicensingDeadline(
    val plateEndings: Set<Int>,
    val display: String,
)

object StateLicensingCalendars {
    const val CURRENT_YEAR = 2026

    val audited2026: List<StateLicensingCalendar> = listOf(
        StateLicensingCalendar(
            uf = "AL",
            stateName = "Alagoas",
            year = 2026,
            sourceLabel = "DETRAN/AL — Calendário de Licenciamento 2026",
            sourceUrl = "https://www.detran.al.gov.br/conteudo/calendario-de-licenciamento/",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline(setOf(1, 2), "Validade do CRLV até 31/08/2026"),
                deadline(setOf(3, 4), "Validade do CRLV até 30/09/2026"),
                deadline(setOf(5, 6), "Validade do CRLV até 31/10/2026"),
                deadline(setOf(7, 8), "Validade do CRLV até 30/11/2026"),
                deadline(setOf(9, 0), "Validade do CRLV até 31/12/2026"),
            ),
        ),
        StateLicensingCalendar(
            uf = "ES",
            stateName = "Espírito Santo",
            year = 2026,
            sourceLabel = "DETRAN/ES — Calendário de Licenciamento 2026",
            sourceUrl = "https://detran.es.gov.br/Media/detran/Calendarios%20de%20pagamento/2026/Calend%C3%A1rio%20de%20Licenciamento%202026.pdf",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline(setOf(1, 2), "Vencimento em 09/09/2026"),
                deadline(setOf(3, 4), "Vencimento em 10/09/2026"),
                deadline(setOf(5, 6), "Vencimento em 11/09/2026"),
                deadline(setOf(7, 8), "Vencimento em 14/09/2026"),
                deadline(setOf(9, 0), "Vencimento em 15/09/2026"),
            ),
        ),
        StateLicensingCalendar(
            uf = "GO",
            stateName = "Goiás",
            year = 2026,
            sourceLabel = "DETRAN/GO — Licenciamento Anual 2026",
            sourceUrl = "https://goias.gov.br/detran/prazo-para-pagamento-do-licenciamento-anual-2026-vence-nesta-quinta-feira/",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline(setOf(1, 2), "Prazo até 15/09/2026"),
                deadline(setOf(3, 4, 5, 6, 7, 8, 9, 0), "Prazo até 15/10/2026"),
            ),
        ),
        StateLicensingCalendar(
            uf = "MG",
            stateName = "Minas Gerais",
            year = 2026,
            sourceLabel = "DETRAN/MG — Portaria nº 0993/2026",
            sourceUrl = "https://www.agenciaminas.mg.gov.br/noticia/licenciamento-anual-de-veiculos-em-minas-gerais-tem-calendario-definido-para-2026",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline(setOf(1, 2, 3), "CRLV 2026 exigido a partir de 01/09/2026"),
                deadline(setOf(4, 5, 6), "CRLV 2026 exigido a partir de 01/10/2026"),
                deadline(setOf(7, 8, 9, 0), "CRLV 2026 exigido a partir de 01/11/2026"),
            ),
        ),
        StateLicensingCalendar(
            uf = "MT",
            stateName = "Mato Grosso",
            year = 2026,
            sourceLabel = "DETRAN/MT — Portaria nº 751/GP",
            sourceUrl = "https://transparencia.detran.mt.gov.br/documents/d/detran-transparencia/doe-2026-01-05-portaria-fiscais",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline(setOf(1, 2, 3), "Prazo até 31/03/2026"),
                deadline(setOf(4), "Prazo até 30/04/2026"),
                deadline(setOf(5), "Prazo até 31/05/2026"),
                deadline(setOf(6), "Prazo até 30/06/2026"),
                deadline(setOf(7), "Prazo até 31/07/2026"),
                deadline(setOf(8), "Prazo até 31/08/2026"),
                deadline(setOf(9), "Prazo até 30/09/2026"),
                deadline(setOf(0), "Prazo até 31/10/2026"),
            ),
        ),
        StateLicensingCalendar(
            uf = "RJ",
            stateName = "Rio de Janeiro",
            year = 2026,
            sourceLabel = "DETRAN/RJ — Calendário de Licenciamento 2026",
            sourceUrl = "https://www.detran.rj.gov.br/veiculo/calendarios/licenciamento.html",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline(setOf(0, 1, 2), "Prazo até 31/07/2026"),
                deadline(setOf(3, 4, 5), "Prazo até 31/08/2026"),
                deadline(setOf(6, 7, 8, 9), "Prazo até 30/09/2026"),
            ),
        ),
        StateLicensingCalendar(
            uf = "RS",
            stateName = "Rio Grande do Sul",
            year = 2026,
            sourceLabel = "DETRAN/RS — Portaria nº 555/2025",
            sourceUrl = "https://www.detran.rs.gov.br/veiculos/servicos/986",
            verifiedAt = "2026-10-15",
            entries = listOf(
                deadline((0..9).toSet(), "Data-limite: 31/07/2026"),
            ),
        ),
    ).sortedBy { it.uf }

    fun find(uf: String): StateLicensingCalendar? =
        audited2026.firstOrNull { it.uf == uf.uppercase() }

    private fun deadline(
        plateEndings: Set<Int>,
        display: String,
    ) = StateLicensingDeadline(
        plateEndings = plateEndings,
        display = display,
    )
}
