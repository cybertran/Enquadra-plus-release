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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class TrafficCrime(
    val article: String,
    val title: String,
    val conduct: String,
    val penalty: String,
    val note: String? = null,
)

private val TRAFFIC_CRIMES = listOf(
    TrafficCrime(
        "Art. 302",
        "Homicídio culposo",
        "Praticar homicídio culposo na direção de veículo automotor.",
        "Detenção de 2 a 4 anos + suspensão/proibição de habilitação.",
        "Sob influência de álcool ou outra substância psicoativa: reclusão de 5 a 8 anos, além da suspensão/proibição.",
    ),
    TrafficCrime(
        "Art. 303",
        "Lesão corporal culposa",
        "Praticar lesão corporal culposa na direção de veículo automotor.",
        "Detenção de 6 meses a 2 anos + suspensão/proibição de habilitação.",
        "Há hipóteses de aumento e forma qualificada previstas no próprio artigo.",
    ),
    TrafficCrime(
        "Art. 304",
        "Omissão de socorro",
        "Deixar de prestar imediato socorro à vítima de sinistro ou de solicitar auxílio da autoridade pública.",
        "Detenção de 6 meses a 1 ano, ou multa.",
    ),
    TrafficCrime(
        "Art. 305",
        "Afastar-se do local",
        "Afastar-se do local do sinistro para fugir à responsabilidade penal ou civil.",
        "Detenção de 6 meses a 1 ano, ou multa.",
    ),
    TrafficCrime(
        "Art. 306",
        "Capacidade psicomotora alterada",
        "Conduzir veículo automotor com capacidade psicomotora alterada por álcool ou outra substância psicoativa.",
        "Detenção de 6 meses a 3 anos + multa + suspensão/proibição de habilitação.",
        "O Enquadra+ possui ferramentas específicas de alcoolemia e sinais psicomotores.",
    ),
    TrafficCrime(
        "Art. 307",
        "Violar suspensão ou proibição",
        "Violar suspensão ou proibição de obter permissão ou habilitação imposta com fundamento no CTB.",
        "Detenção de 6 meses a 1 ano + multa + nova suspensão/proibição pelo mesmo prazo.",
    ),
    TrafficCrime(
        "Art. 308",
        "Corrida ou exibição não autorizada",
        "Participar, em via pública, de corrida, disputa, competição, exibição ou demonstração de perícia não autorizada, gerando risco.",
        "Detenção de 6 meses a 3 anos + multa + suspensão/proibição de habilitação.",
        "O artigo prevê penas maiores se resultar lesão grave ou morte sem dolo eventual.",
    ),
    TrafficCrime(
        "Art. 309",
        "Direção sem habilitação com perigo de dano",
        "Dirigir em via pública sem PPD/CNH, ou com direito de dirigir cassado, gerando perigo de dano.",
        "Detenção de 6 meses a 1 ano, ou multa.",
        "A tipificação penal exige perigo de dano.",
    ),
    TrafficCrime(
        "Art. 310",
        "Entregar direção a pessoa sem condições",
        "Permitir, confiar ou entregar veículo a pessoa não habilitada, cassada, suspensa ou sem condições seguras de condução.",
        "Detenção de 6 meses a 1 ano, ou multa.",
    ),
    TrafficCrime(
        "Art. 311",
        "Velocidade incompatível em local sensível",
        "Trafegar em velocidade incompatível com a segurança nos locais previstos no artigo, gerando perigo de dano.",
        "Detenção de 6 meses a 1 ano, ou multa.",
        "A tipificação penal exige perigo de dano.",
    ),
    TrafficCrime(
        "Art. 312",
        "Inovação artificiosa em sinistro com vítima",
        "Alterar artificiosamente estado de lugar, coisa ou pessoa para induzir autoridade, perito ou juiz a erro.",
        "Detenção de 6 meses a 1 ano, ou multa.",
    ),
)

@Composable
fun TrafficCrimesToolScreen(contentPadding: PaddingValues) {
    var query by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    val normalized = query.trim().lowercase()
    val filtered = if (normalized.isBlank()) {
        TRAFFIC_CRIMES
    } else {
        TRAFFIC_CRIMES.filter { crime ->
            listOf(crime.article, crime.title, crime.conduct, crime.penalty, crime.note.orEmpty())
                .any { it.lowercase().contains(normalized) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Crimes de trânsito",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Artigo ou termo") },
            )
        }

        items(filtered) { crime ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = crime.article + " • " + crime.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = crime.conduct,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = crime.penalty,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    crime.note?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "Nenhum artigo encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Text(
                text = "Ver fonte",
                modifier = Modifier.clickable { uriHandler.openUri(TRAFFIC_CRIMES_SOURCE_URL) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

    }
}

private const val TRAFFIC_CRIMES_SOURCE_URL =
    "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm"
