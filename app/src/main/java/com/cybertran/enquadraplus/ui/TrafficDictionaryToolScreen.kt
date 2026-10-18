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

private data class TrafficGlossaryEntry(
    val term: String,
    val definition: String,
)

private val TRAFFIC_GLOSSARY = listOf(
    TrafficGlossaryEntry(
        "Acostamento",
        "Parte da via diferenciada da pista de rolamento, destinada à parada ou ao estacionamento em emergência e à circulação de pedestres e bicicletas quando não houver local apropriado.",
    ),
    TrafficGlossaryEntry(
        "Agente da autoridade de trânsito",
        "Agente de trânsito ou policial rodoviário federal competente para fiscalização, controle, operação, patrulhamento e lavratura do AIT; inclui policial militar e os agentes do art. 25-A quando designados pela autoridade com circunscrição sobre a via, mediante convênio.",
    ),
    TrafficGlossaryEntry(
        "Automóvel",
        "Veículo automotor para transporte de passageiros, com capacidade de até oito pessoas além do condutor.",
    ),
    TrafficGlossaryEntry(
        "Autoridade de trânsito",
        "Dirigente máximo de órgão ou entidade executivo integrante do Sistema Nacional de Trânsito, ou pessoa expressamente credenciada por ele.",
    ),
    TrafficGlossaryEntry(
        "Bicicleta",
        "Veículo de propulsão humana com duas rodas, distinto de motocicleta, motoneta e ciclomotor.",
    ),
    TrafficGlossaryEntry(
        "Bicicletário",
        "Local, na via ou fora dela, destinado ao estacionamento de bicicletas.",
    ),
    TrafficGlossaryEntry(
        "Caminhão",
        "Veículo automotor de carga com PBT superior a 3.500 kg, podendo tracionar outro dentro da CMT.",
    ),
    TrafficGlossaryEntry(
        "Caminhão-trator",
        "Veículo automotor destinado a tracionar ou arrastar outro veículo.",
    ),
    TrafficGlossaryEntry(
        "Caminhonete",
        "Veículo de carga com PBT de até 3.500 kg.",
    ),
    TrafficGlossaryEntry(
        "Camioneta",
        "Veículo misto para transporte de passageiros e carga no mesmo compartimento.",
    ),
    TrafficGlossaryEntry(
        "Calçada",
        "Parte da via normalmente segregada e em nível diferente, não destinada à circulação de veículos, reservada a pedestres e, quando possível, a mobiliário urbano, sinalização, vegetação e outros fins.",
    ),
    TrafficGlossaryEntry(
        "Ciclo",
        "Veículo com pelo menos duas rodas movido por força humana.",
    ),
    TrafficGlossaryEntry(
        "Ciclofaixa",
        "Parte da pista de rolamento destinada à circulação exclusiva de ciclos, delimitada por sinalização específica.",
    ),
    TrafficGlossaryEntry(
        "Ciclomotor",
        "Veículo de duas ou três rodas, até 50 cm³ ou 4 kW elétricos, com velocidade máxima de fabricação de até 50 km/h.",
    ),
    TrafficGlossaryEntry(
        "Ciclovia",
        "Pista própria para circulação de ciclos, fisicamente separada do tráfego comum.",
    ),
    TrafficGlossaryEntry(
        "Circulação",
        "Movimentação de pessoas, animais e veículos em deslocamento, conduzidos ou não, em vias públicas ou privadas abertas ao público e de uso coletivo.",
    ),
    TrafficGlossaryEntry(
        "Conversão",
        "Movimento à esquerda ou à direita que altera a direção original do veículo.",
    ),
    TrafficGlossaryEntry(
        "Cruzamento",
        "Interseção de duas vias no mesmo nível.",
    ),
    TrafficGlossaryEntry(
        "Dispositivo de segurança",
        "Elemento com função específica de aumentar a segurança e alertar sobre perigos capazes de ameaçar a integridade dos usuários da via ou causar dano grave ao veículo.",
    ),
    TrafficGlossaryEntry(
        "Estacionamento",
        "Imobilização do veículo por tempo superior ao necessário para embarque ou desembarque.",
    ),
    TrafficGlossaryEntry(
        "Estrada",
        "Via rural não pavimentada.",
    ),
    TrafficGlossaryEntry(
        "Etilômetro",
        "Aparelho utilizado para medir álcool no ar alveolar.",
    ),
    TrafficGlossaryEntry(
        "Faixa de trânsito",
        "Uma das áreas longitudinais em que a pista pode ser subdividida, com ou sem marcas viárias longitudinais, com largura suficiente para a circulação de veículos automotores.",
    ),
    TrafficGlossaryEntry(
        "Motocicleta",
        "Veículo automotor de duas rodas, com ou sem side-car, dirigido por condutor em posição montada.",
    ),
    TrafficGlossaryEntry(
        "Motoneta",
        "Veículo automotor de duas rodas, dirigido por condutor em posição sentada.",
    ),
    TrafficGlossaryEntry(
        "Ônibus",
        "Veículo automotor de transporte coletivo com capacidade para mais de vinte passageiros, ainda que adaptações de conforto reduzam o número efetivamente transportado.",
    ),
    TrafficGlossaryEntry(
        "Parada",
        "Imobilização do veículo com a finalidade e pelo tempo estritamente necessário para embarque ou desembarque de passageiros.",
    ),
    TrafficGlossaryEntry(
        "Rodovia",
        "Via rural pavimentada.",
    ),
    TrafficGlossaryEntry(
        "Trânsito",
        "Movimentação e imobilização de veículos, pessoas e animais nas vias terrestres.",
    ),
    TrafficGlossaryEntry(
        "Triciclo",
        "Veículo automotor de três rodas, com ou sem cabine, dirigido por condutor sentado ou montado e que não possui características de ciclomotor.",
    ),
    TrafficGlossaryEntry(
        "Ultrapassagem",
        "Movimento de passar à frente de veículo que segue no mesmo sentido, em menor velocidade e na mesma faixa, exigindo sair e retornar à faixa de origem.",
    ),
    TrafficGlossaryEntry(
        "Veículo articulado",
        "Combinação de veículos acoplados em que um deles é automotor.",
    ),
    TrafficGlossaryEntry(
        "Veículo de grande porte",
        "Veículo de carga com PBT máximo superior a 10.000 kg ou de passageiros com capacidade superior a vinte passageiros.",
    ),
    TrafficGlossaryEntry(
        "Veículo especial",
        "Veículo de passageiros, carga, tração, coleção ou misto com características diferenciadas para função especial, exigindo arranjos específicos de carroceria ou equipamento.",
    ),
    TrafficGlossaryEntry(
        "Via",
        "Superfície por onde transitam veículos, pessoas e animais, abrangendo pista, calçada, acostamento, ilha e canteiro central.",
    ),
    TrafficGlossaryEntry(
        "Via arterial",
        "Via urbana com interseções em nível, geralmente semaforizadas, acesso a lotes lindeiros e às vias secundárias e locais, permitindo o trânsito entre regiões da cidade.",
    ),
    TrafficGlossaryEntry(
        "Via de trânsito rápido",
        "Via com acessos especiais, sem interseções em nível, sem acesso direto a lotes e sem travessia de pedestres em nível.",
    ),
)

@Composable
fun TrafficDictionaryToolScreen(contentPadding: PaddingValues) {
    var query by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val normalized = query.trim().lowercase()

    val filtered = if (normalized.isBlank()) {
        TRAFFIC_GLOSSARY
    } else {
        TRAFFIC_GLOSSARY.filter { entry ->
            entry.term.lowercase().contains(normalized) ||
                entry.definition.lowercase().contains(normalized)
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
                text = "Dicionário",
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
                label = { Text("Buscar termo") },
            )
        }

        item {
            Text(
                text = filtered.size.toString() + " termo(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(
            items = filtered,
            key = { it.term },
        ) { entry ->
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
                        text = entry.term,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = entry.definition,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "Nenhum termo encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Text(
                text = "Ver fonte",
                modifier = Modifier.clickable {
                    uriHandler.openUri(DICTIONARY_SOURCE_URL)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

    }
}

private const val DICTIONARY_SOURCE_URL =
    "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm"
