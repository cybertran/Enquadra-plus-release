package com.cybertran.enquadraplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private enum class CnhScenario(
    val label: String,
    val category: String,
    val detail: String,
) {
    TWO_OR_THREE_WHEELS(
        "Veículo motorizado de 2 ou 3 rodas",
        "Categoria A",
        "Com ou sem carro lateral.",
    ),
    LIGHT(
        "Até 3.500 kg e até 8 lugares",
        "Categoria B",
        "Excluído o lugar do motorista.",
    ),
    ELECTRIC_4250(
        "Elétrico/híbrido até 4.250 kg",
        "Categoria B",
        "Propulsão elétrica ou híbrida com tração predominantemente elétrica.",
    ),
    MOTORHOME(
        "Motor-casa: até 6.000 kg ou até 8 lugares",
        "Categoria B",
        "Basta uma das condições do art. 143, § 2º: peso de até 6.000 kg ou lotação de até 8 lugares, excluído o motorista.",
    ),
    CARGO_HEAVY(
        "Carga acima de 3.500 kg",
        "Categoria C",
        "Veículo motorizado utilizado em transporte de carga.",
    ),
    PASSENGER(
        "Passageiros acima de 8 lugares",
        "Categoria D",
        "Excluído o lugar do motorista.",
    ),
    HEAVY_COMBINATION(
        "Unidade acoplada com 6.000 kg ou mais",
        "Categoria E",
        "Também se aplica se a lotação da unidade acoplada exceder 8 lugares.",
    ),
    MULTIPLE_TRAILERS(
        "Mais de uma unidade tracionada",
        "Categoria E",
        "Independentemente do peso bruto total.",
    ),
}

@Composable
fun CnhToolScreen(contentPadding: PaddingValues) {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = CnhScenario.entries.firstOrNull { it.name == selectedName }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Qual categoria?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        items(CnhScenario.entries) { scenario ->
            SelectableListRow(
                title = scenario.label,
                selected = selectedName == scenario.name,
                onClick = { selectedName = scenario.name },
            )
        }

        selected?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = result.category,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = result.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Mudança de categoria",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            CompactRuleCard(
                title = "C",
                text = "1 ano na B + no máximo 1 gravíssima nos últimos 12 meses.",
            )
        }
        item {
            CompactRuleCard(
                title = "D",
                text = "21 anos + no máximo 1 gravíssima nos últimos 12 meses + 2 anos na B ou 1 ano na C + requisitos do art. 145.",
            )
        }
        item {
            CompactRuleCard(
                title = "E",
                text = "21 anos + no máximo 1 gravíssima nos últimos 12 meses + 1 ano na C + requisitos do art. 145.",
            )
        }

        item {
            Text(
                text = "Ver fonte no CTB",
                modifier = Modifier.clickable { uriHandler.openUri(CTB_SOURCE_URL) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Ver fonte complementar",
                modifier = Modifier.clickable { uriHandler.openUri(CNH_SOURCE_URL) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CompactRuleCard(
    title: String,
    text: String,
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
                text = "Categoria " + title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val CTB_SOURCE_URL =
    "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm"

private const val CNH_SOURCE_URL =
    "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao10202025.pdf"
