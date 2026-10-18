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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cybertran.enquadraplus.ui.signage.SignageCatalogItem
import com.cybertran.enquadraplus.ui.signage.SignageViewModel

@Composable
fun SignageToolScreen(
    contentPadding: PaddingValues,
    onOpenPdf: (OfficialPdfRequest) -> Unit,
    viewModel: SignageViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedVolume by viewModel.selectedVolume.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val manuals = viewModel.manuals

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Placas e sinalização",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Índice pesquisável dos nove volumes oficiais do Manual Brasileiro de Sinalização de Trânsito. Cada item abre a página correspondente do PDF oficial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (manuals.isNotEmpty()) {
            item {
                Text(
                    text = "Manuais oficiais",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(
                items = manuals,
                key = { "manual-" + it.volume },
            ) { manual ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenPdf(
                                OfficialPdfRequest(
                                    url = manual.officialUrl,
                                    title = "Volume " + manual.volume + " • " + manual.title,
                                ),
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Volume " + manual.volume,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = manual.title,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = manual.entryCount.toString() +
                                " item(ns) indexado(s) • " +
                                manual.pageCount +
                                " página(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Abrir PDF oficial",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Índice pesquisável",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = viewModel::updateQuery,
                singleLine = true,
                label = { Text("Código, placa, marca ou seção") },
            )
        }

        item {
            val volumes =
                listOf(SignageViewModel.ALL_VOLUMES) +
                    manuals.map { it.volume }
            HorizontalChoicePills(
                options = volumes.map { volume ->
                    volume to if (volume == SignageViewModel.ALL_VOLUMES) {
                        "Todos"
                    } else {
                        "Vol. " + volume
                    }
                },
                selectedId = selectedVolume,
                onSelect = viewModel::selectVolume,
            )
        }

        item {
            Text(
                text = results.size.toString() + " resultado(s)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(
            items = results,
            key = SignageCatalogItem::id,
        ) { item ->
            SignageIndexCard(
                item = item,
                onOpenPdf = onOpenPdf,
            )
        }

        if (results.isEmpty()) {
            item {
                Text(
                    text = if (manuals.isEmpty()) {
                        "O catálogo de sinalização ainda não está disponível nesta instalação."
                    } else {
                        "Nenhum item encontrado."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SignageIndexCard(
    item: SignageCatalogItem,
    onOpenPdf: (OfficialPdfRequest) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onOpenPdf(
                    OfficialPdfRequest(
                        url = item.officialUrl,
                        title = buildString {
                            item.code?.let {
                                append(it)
                                append(" • ")
                            }
                            append(item.title)
                        },
                        initialPageIndex = item.pageIndex,
                    ),
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item.code?.let { code ->
                Text(
                    text = code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Volume " + item.volume + " • " + item.volumeTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.manualPage?.let { "Manual: p. " + it }
                    ?: "PDF: página " + (item.pageIndex + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.evidence,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Abrir na página oficial",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
