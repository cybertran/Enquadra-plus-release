package com.cybertran.enquadraplus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cybertran.enquadraplus.data.update.DatasetUpdateState
import com.cybertran.enquadraplus.data.update.UpdateStatus
import com.cybertran.enquadraplus.ui.update.UpdateViewModel

@Composable
fun NormativeUpdateScreen(
    contentPadding: PaddingValues,
    viewModel: UpdateViewModel = viewModel(),
) {
    val states by viewModel.states.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val busy = states.any {
        it.status in setOf(
            UpdateStatus.CHECKING,
            UpdateStatus.APPLYING,
            UpdateStatus.ROLLING_BACK,
        )
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
                text = "Atualizações jurídicas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = viewModel::checkNow,
            ) {
                Text(if (busy) "Verificando atualizações…" else "Verificar atualizações")
            }
        }

        actionError?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        items(
            items = states,
            key = { it.dataset.id },
        ) { state ->
            DatasetUpdateCard(
                state = state,
                onApply = { viewModel.apply(state.dataset) },
                onRollback = { viewModel.rollback(state.dataset) },
            )
        }
    }
}

@Composable
private fun DatasetUpdateCard(
    state: DatasetUpdateState,
    onApply: () -> Unit,
    onRollback: () -> Unit,
) {
    var showTechnicalDetails by rememberSaveable(state.dataset.id) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.dataset.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = statusLabel(state.status),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            state.lastCheckedAt?.let {
                Text(
                    text = "${checkTimestampLabel(state.status)}: ${formatUpdateDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            userFacingMessage(state.status)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.canApply) {
                Button(onClick = onApply) {
                    Text("Aplicar atualização")
                }
            }

            TextButton(
                onClick = { showTechnicalDetails = !showTechnicalDetails },
            ) {
                Text(
                    if (showTechnicalDetails) {
                        "Ocultar detalhes técnicos"
                    } else {
                        "Detalhes técnicos"
                    },
                )
            }

            if (showTechnicalDetails) {
                state.activeVersion?.let {
                    Text(
                        text = "Versão ativa: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                state.previousVersion?.let {
                    Text(
                        text = "Versão anterior: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                state.candidateVersion?.let {
                    Text(
                        text = "Versão candidata: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                state.diff?.let { diff ->
                    Text(
                        text = "Comparação: +${diff.added} incluídos • ${diff.changed} alterados • ${diff.removed} removidos • ${diff.brokenReferences} referências quebradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.message.isNotBlank()) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.canRollback) {
                    OutlinedButton(onClick = onRollback) {
                        Text("Restaurar versão anterior")
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: UpdateStatus): String =
    when (status) {
        UpdateStatus.IDLE -> "Ainda não verificado"
        UpdateStatus.CHECKING -> "Verificando…"
        UpdateStatus.UP_TO_DATE -> "Nenhuma atualização disponível"
        UpdateStatus.CANDIDATE_APPROVED -> "Atualização disponível"
        UpdateStatus.CANDIDATE_BLOCKED -> "Atualização encontrada, mas não validada"
        UpdateStatus.APPLYING -> "Aplicando atualização…"
        UpdateStatus.APPLIED -> "Atualização aplicada"
        UpdateStatus.ROLLING_BACK -> "Restaurando versão anterior…"
        UpdateStatus.ROLLED_BACK -> "Versão anterior restaurada"
        UpdateStatus.PROTECTED -> "Base atual preservada"
        UpdateStatus.SOURCE_UNAVAILABLE -> "Não foi possível concluir a verificação"
        UpdateStatus.ERROR -> "Erro durante a verificação"
    }

private fun userFacingMessage(status: UpdateStatus): String? =
    when (status) {
        UpdateStatus.IDLE ->
            "Toque em “Verificar atualizações” para consultar a fonte oficial."
        UpdateStatus.CHECKING ->
            "A fonte oficial está sendo consultada. Aguarde o resultado."
        UpdateStatus.UP_TO_DATE ->
            "A verificação terminou e não foi encontrada atualização aplicável."
        UpdateStatus.CANDIDATE_APPROVED ->
            "Uma atualização validada está pronta para aplicação."
        UpdateStatus.CANDIDATE_BLOCKED ->
            "Uma atualização foi encontrada, mas não pôde ser validada. A base atual foi mantida."
        UpdateStatus.APPLIED ->
            "A atualização validada foi aplicada com sucesso."
        UpdateStatus.ROLLED_BACK ->
            "A versão anterior foi restaurada com sucesso."
        UpdateStatus.PROTECTED ->
            "A base atual foi mantida por segurança."
        UpdateStatus.SOURCE_UNAVAILABLE ->
            "A consulta à fonte oficial não foi concluída. Por isso, não é possível determinar se há atualização disponível neste momento. A base instalada foi mantida sem alterações."
        UpdateStatus.ERROR ->
            "O processo de verificação falhou antes de terminar. A base atual foi preservada."
        else -> null
    }

private fun checkTimestampLabel(status: UpdateStatus): String =
    when (status) {
        UpdateStatus.SOURCE_UNAVAILABLE,
        UpdateStatus.ERROR -> "Última tentativa"

        else -> "Última verificação"
    }

private fun formatUpdateDate(value: String): String {
    val date = value.take(10)
    val parts = date.split("-")
    return if (
        parts.size == 3 &&
        parts[0].length == 4 &&
        parts[1].length == 2 &&
        parts[2].length == 2
    ) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
        value
    }
}
