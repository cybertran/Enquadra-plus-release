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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class TowCheck(
    val id: String,
    val text: String,
)

private val MODERN_TOW_CHECKS = listOf(
    TowCheck("declared", "Veículo possui capacidade para tracionar reboque declarada pelo fabricante/importador"),
    TowCheck("points", "Pontos de fixação constam do manual ou literatura oficial do veículo"),
    TowCheck("cmt", "Capacidade Máxima de Tração (CMT) consta da documentação do fabricante/importador"),
    TowCheck("inmetro", "Engate produzido por empresa registrada no Inmetro"),
    TowCheck("plate", "Plaqueta inviolável presente e visível"),
    TowCheck("manufacturer", "Plaqueta identifica fabricante, CNPJ e registro Inmetro"),
    TowCheck("vehicle", "Plaqueta identifica o modelo do veículo"),
    TowCheck("tow", "Plaqueta informa a CMT do engate"),
    TowCheck("installation", "Instalação segue o procedimento aprovado no Inmetro pelo fabricante do engate"),
    TowCheck("invoice", "Nota de venda do engate identifica o veículo em que o produto foi instalado"),
)

private val LEGACY_TOW_CHECKS = listOf(
    TowCheck("sphere", "Esfera maciça apropriada ao tracionamento"),
    TowCheck("socket", "Tomada e instalação apropriadas para conexão ao rebocado"),
    TowCheck("chain", "Dispositivo para fixação da corrente de segurança"),
    TowCheck("edges", "Sem superfícies cortantes ou cantos vivos na haste da esfera"),
    TowCheck("light", "Sem dispositivo de iluminação no engate"),
)

@Composable
fun TowHitchToolScreen(contentPadding: PaddingValues) {
    var pbtGroup by rememberSaveable { mutableStateOf("up_to_3500") }
    var originalFactory by rememberSaveable { mutableStateOf(false) }
    var legacyVehicle by rememberSaveable { mutableStateOf(false) }
    val checked = remember { mutableStateListOf<String>() }
    val uriHandler = LocalUriHandler.current

    val checks = if (legacyVehicle) LEGACY_TOW_CHECKS else MODERN_TOW_CHECKS
    val completed = checks.count { it.id in checked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Engate",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            HorizontalChoicePills(
                options = listOf(
                    "up_to_3500" to "PBT até 3.500 kg",
                    "over_3500" to "PBT acima de 3.500 kg",
                ),
                selectedId = pbtGroup,
                onSelect = { id ->
                    pbtGroup = id
                    checked.clear()
                },
            )
        }

        if (pbtGroup == "over_3500") {
            item {
                InfoCard(
                    title = "Fora do escopo",
                    text = "Esta ferramenta cobre engates usados em veículos com PBT de até 3.500 kg.",
                )
            }
        } else {
            item {
                ToolToggleCard(
                    title = "Engate original de fábrica",
                    enabled = originalFactory,
                    onClick = {
                        originalFactory = !originalFactory
                        checked.clear()
                    },
                )
            }

            if (originalFactory) {
                item {
                    InfoCard(
                        title = "Equipamento original",
                        text = "Para engate original de fábrica, confirme a especificação do fabricante do veículo.",
                    )
                }
            } else {
                item {
                    ToolToggleCard(
                        title = "Veículo já circulava em 30/07/2006",
                        enabled = legacyVehicle,
                        onClick = {
                            legacyVehicle = !legacyVehicle
                            checked.clear()
                        },
                    )
                }

                item {
                    Text(
                        text = "Conferidos: " + completed + "/" + checks.size,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(checks) { check ->
                    TowChecklistCard(
                        text = check.text,
                        checked = check.id in checked,
                        onClick = {
                            if (check.id in checked) checked.remove(check.id)
                            else checked.add(check.id)
                        },
                    )
                }

                item {
                    val status = when {
                        completed == 0 -> "Inicie a conferência."
                        completed == checks.size -> "Checklist documental concluído."
                        else -> "Há itens ainda não conferidos."
                    }
                    InfoCard(
                        title = "Resultado",
                        text = status,
                    )
                }
            }
        }

        item {
            InfoCard(
                title = "Enquadramento",
                text = "Engate em desacordo: art. 230, XII do CTB, sem prejuízo de outras infrações aplicáveis.",
            )
        }

        item {
            Text(
                text = "Ver fonte",
                modifier = Modifier.clickable { uriHandler.openUri(TOW_HITCH_SOURCE_URL) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TowChecklistCard(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    ChecklistRow(
        title = text,
        checked = checked,
        onClick = onClick,
    )
}

@Composable
private fun ToolToggleCard(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    SelectableListRow(
        title = title,
        selected = enabled,
        onClick = onClick,
    )
}

@Composable
private fun InfoCard(
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
                text = title,
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

private const val TOW_HITCH_SOURCE_URL =
    "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao9372022.pdf"
