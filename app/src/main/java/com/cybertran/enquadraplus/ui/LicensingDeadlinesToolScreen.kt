package com.cybertran.enquadraplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cybertran.enquadraplus.domain.model.StateLicensingCalendars
import java.time.Year

private data class NationalDeadline(
    val title: String,
    val deadline: String,
    val reference: String,
)

private val NATIONAL_TRAFFIC_DEADLINES = listOf(
    NationalDeadline(
        "Transferência de propriedade",
        "30 dias",
        "CTB, art. 123, § 1º",
    ),
    NationalDeadline(
        "Comunicação de venda pelo antigo proprietário",
        "Até 60 dias, se a transferência não for efetivada",
        "CTB, art. 134",
    ),
    NationalDeadline(
        "Atualização de endereço no mesmo município",
        "30 dias",
        "CTB, art. 123, § 2º",
    ),
    NationalDeadline(
        "Identificação do condutor infrator",
        "30 dias da notificação da autuação",
        "CTB, art. 257, § 7º",
    ),
    NationalDeadline(
        "Defesa prévia",
        "Prazo indicado na notificação, nunca inferior a 30 dias",
        "CTB, art. 281-A",
    ),
    NationalDeadline(
        "Recurso contra penalidade",
        "Prazo indicado na notificação, nunca inferior a 30 dias",
        "CTB, art. 282, § 4º",
    ),
)

@Composable
fun LicensingDeadlinesToolScreen(contentPadding: PaddingValues) {
    var plateEnding by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedUf by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val nationalLimit = plateEnding?.let(::licensingNationalLimit)
    val currentYear = Year.now().value
    val auditedStateCalendars =
        if (currentYear == StateLicensingCalendars.CURRENT_YEAR) {
            StateLicensingCalendars.audited2026
        } else {
            emptyList()
        }
    val stateCalendar =
        selectedUf
            ?.takeIf { currentYear == StateLicensingCalendars.CURRENT_YEAR }
            ?.let(StateLicensingCalendars::find)
    val stateDeadline = plateEnding?.let { stateCalendar?.deadlineFor(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Licenciamento anual",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "UF do veículo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (auditedStateCalendars.isNotEmpty()) {
                    "O catálogo estadual mostra somente calendários 2026 já conferidos em fonte oficial. As demais UFs continuam na referência nacional até auditoria própria."
                } else {
                    "Não há calendário estadual auditado para $currentYear nesta versão. Use a referência nacional e confirme a fonte oficial da UF."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            HorizontalChoicePills(
                options = listOf("NATIONAL" to "Nacional") +
                    auditedStateCalendars.map { it.uf to it.uf },
                selectedId = selectedUf ?: "NATIONAL",
                onSelect = { id ->
                    selectedUf = id.takeUnless { it == "NATIONAL" }
                },
            )
        }

        item {
            Text(
                text = "Final da placa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            HorizontalChoicePills(
                options = (0..9).map { it.toString() to it.toString() },
                selectedId = plateEnding?.toString() ?: "",
                onSelect = { id -> plateEnding = id.toIntOrNull() },
            )
        }

        if (stateCalendar != null && stateDeadline != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stateCalendar.stateName + " • " + stateDeadline.display,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Calendário estadual 2026 auditado em ${stateCalendar.verifiedAt}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Ver fonte estadual",
                            modifier = Modifier.clickable {
                                uriHandler.openUri(stateCalendar.sourceUrl)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        } else if (nationalLimit != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Limite nacional: " + nationalLimit,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "A UF pode estabelecer calendário próprio. Selecione uma UF auditada acima ou consulte a fonte oficial estadual.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        item {
            DeadlineInfoCard(
                title = "CRLV-e",
                text = "O licenciamento é anual, digital e realizado pelo órgão executivo de trânsito da UF onde o veículo está registrado.",
                reference = "CTB, art. 130 • Resolução CONTRAN 1.027/2026",
            )
        }

        item {
            DeadlineInfoCard(
                title = "Requisitos",
                text = "Registro regular, ausência de restrição impeditiva, quitação de tributos, encargos e multas e atendimento às campanhas de recall quando exigível.",
                reference = "Resolução CONTRAN 1.027/2026, art. 12",
            )
        }

        item {
            Text(
                text = "Prazos nacionais",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        NATIONAL_TRAFFIC_DEADLINES.forEach { deadline ->
            item {
                DeadlineInfoCard(
                    title = deadline.title,
                    text = deadline.deadline,
                    reference = deadline.reference,
                )
            }
        }

        item {
            Text(
                text = "Ver fonte do licenciamento",
                modifier = Modifier.clickable {
                    uriHandler.openUri(LICENSING_1027_SOURCE_URL)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Ver fonte dos limites nacionais por placa",
                modifier = Modifier.clickable {
                    uriHandler.openUri(LICENSING_110_SOURCE_URL)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DeadlineInfoCard(
    title: String,
    text: String,
    reference: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = reference,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun licensingNationalLimit(plateEnding: Int): String =
    when (plateEnding) {
        1, 2 -> "setembro"
        3, 4, 5 -> "outubro"
        6, 7, 8 -> "novembro"
        else -> "dezembro"
    }

private const val LICENSING_1027_SOURCE_URL =
    "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao10272026.pdf"

private const val LICENSING_110_SOURCE_URL =
    "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucao-contran-no-110-de-24-de-fevereiro-de-2000"
