package com.cybertran.enquadraplus.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cybertran.enquadraplus.domain.model.AitObservationComposer
import com.cybertran.enquadraplus.domain.model.TrafficInfraction

@Composable
fun AitObservationAssistantScreen(
    contentPadding: PaddingValues,
    infraction: TrafficInfraction,
) {
    val context = LocalContext.current
    var fact by rememberSaveable(infraction.code) { mutableStateOf("") }
    var location by rememberSaveable(infraction.code) { mutableStateOf("") }
    var circumstance by rememberSaveable(infraction.code) { mutableStateOf("") }
    var complement by rememberSaveable(infraction.code) { mutableStateOf("") }
    var copied by rememberSaveable(infraction.code) { mutableStateOf(false) }

    val observation = AitObservationComposer.compose(
        fact = fact,
        location = location,
        circumstance = circumstance,
        complement = complement,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Observação do AIT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Código ${infraction.code} • ${infraction.shortDescription}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (infraction.aitOfficialObservations.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Referências oficiais da ficha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Use apenas como referência. O assistente não insere esses exemplos automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        infraction.aitOfficialObservations.forEachIndexed { index, value ->
                            Text(
                                text = "${index + 1}. $value",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        item {
            ObservationField(
                label = "Fato constatado",
                value = fact,
                onValueChange = { fact = it; copied = false },
                supporting = "Descreva somente o que foi efetivamente observado.",
            )
        }
        item {
            ObservationField(
                label = "Localização ou posição",
                value = location,
                onValueChange = { location = it; copied = false },
                supporting = "Ex.: faixa, sentido, posição do veículo ou local da sinalização, quando relevante.",
            )
        }
        item {
            ObservationField(
                label = "Circunstância relevante",
                value = circumstance,
                onValueChange = { circumstance = it; copied = false },
                supporting = "Inclua apenas circunstância necessária para caracterizar o fato.",
            )
        }
        item {
            ObservationField(
                label = "Complemento verificável",
                value = complement,
                onValueChange = { complement = it; copied = false },
                supporting = "Identificação de equipamento, medição, documento ou outro dado conferido.",
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Texto montado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = observation.ifBlank { "Preencha ao menos um fato para montar a observação." },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        enabled = observation.isNotBlank(),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Observação do AIT", observation),
                            )
                            copied = true
                        },
                    ) {
                        Text(if (copied) "Copiado" else "Copiar observação")
                    }
                }
            }
        }

        item {
            Text(
                text = "O Enquadra+ apenas organiza os fatos digitados. Ele não cria fatos, não decide o enquadramento e não substitui a conferência da ficha MBFT e da norma vigente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ObservationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supporting: String,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        minLines = 2,
        label = { Text(label) },
        supportingText = { Text(supporting) },
    )
}
