package com.cybertran.enquadraplus.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cybertran.enquadraplus.domain.model.OperationalGuide
import com.cybertran.enquadraplus.domain.model.OperationalGuideCatalog

@Composable
fun OperationalGuidesScreen(contentPadding: PaddingValues) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalized = query.trim().lowercase()
    val guides = if (normalized.isBlank()) {
        OperationalGuideCatalog.entries
    } else {
        OperationalGuideCatalog.entries.filter { guide ->
            buildList {
                add(guide.title)
                add(guide.summary)
                addAll(guide.legalReferences)
                addAll(guide.normativeSearchTerms)
            }.any { it.lowercase().contains(normalized) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Guias operacionais",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            Text(
                text = "Fluxos próprios do Enquadra+ para organizar a conferência. A ficha MBFT e a norma oficial continuam sendo a referência para a autuação.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Buscar guia") },
            )
        }
        items(guides, key = { it.id }) { guide ->
            OperationalGuideCard(guide)
        }
        if (guides.isEmpty()) {
            item {
                Text(
                    text = "Nenhum guia encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun OperationalGuideCard(guide: OperationalGuide) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = guide.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = guide.summary,
                style = MaterialTheme.typography.bodyMedium,
            )
            guide.steps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "Referências: " + guide.legalReferences.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Pesquise em Normas: " + guide.normativeSearchTerms.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
