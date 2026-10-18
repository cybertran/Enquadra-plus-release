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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cybertran.enquadraplus.domain.model.CtbArticle
import com.cybertran.enquadraplus.domain.model.NormativeAct
import com.cybertran.enquadraplus.ui.ctb.CtbViewModel
import com.cybertran.enquadraplus.ui.norms.NormativeViewModel

private enum class NormType(val label: String) {
    ALL("Todas"),
    CTB("CTB"),
    MBFT("MBFT"),
    CONTRAN("Resoluções"),
    DELIBERATIONS("Deliberações"),
    SENATRAN("Senatran"),
    SIGNAGE("Sinalização"),
}

private data class OfficialNorm(
    val type: NormType,
    val title: String,
    val description: String,
    val status: String,
    val url: String,
)

private val STATIC_NORMS = listOf(
    OfficialNorm(
        NormType.MBFT,
        "MBFT — fonte oficial",
        "Manual Brasileiro de Fiscalização de Trânsito • Resolução CONTRAN nº 985/2022",
        "As fichas estruturadas são consultadas em Infrações. Aqui você abre o documento oficial.",
        "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/mbvt20222.pdf",
    ),
)

@Composable
fun OfficialNormsScreen(
    contentPadding: PaddingValues,
    onOpenSignage: () -> Unit = {},
    onOpenPdf: (OfficialPdfRequest) -> Unit = {},
    ctbViewModel: CtbViewModel = viewModel(),
    normativeViewModel: NormativeViewModel = viewModel(),
) {
    val ctbResults by ctbViewModel.results.collectAsStateWithLifecycle()
    val selectedArticle by ctbViewModel.selected.collectAsStateWithLifecycle()
    val normativeResults by normativeViewModel.results.collectAsStateWithLifecycle()
    val selectedNormativeAct by normativeViewModel.selected.collectAsStateWithLifecycle()

    selectedArticle?.let { article ->
        CtbArticleDetailScreen(
            article = article,
            contentPadding = contentPadding,
            onBack = ctbViewModel::closeDetail,
        )
        return
    }

    selectedNormativeAct?.let { act ->
        NormativeActDetailScreen(
            act = act,
            contentPadding = contentPadding,
            onBack = normativeViewModel::closeDetail,
            onOpenPdf = onOpenPdf,
        )
        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(NormType.ALL) }
    val uriHandler = LocalUriHandler.current

    val normalized = query.trim().lowercase()
    val staticFiltered = STATIC_NORMS.filter { norm ->
        (selectedType == NormType.ALL || norm.type == selectedType) &&
            (
                selectedType == NormType.MBFT ||
                    normalized.isBlank() ||
                    norm.title.lowercase().contains(normalized) ||
                    norm.description.lowercase().contains(normalized)
                )
    }

    val filteredNormativeActs = normativeResults.filter { act ->
        when (selectedType) {
            NormType.CONTRAN ->
                act.agency == "CONTRAN" && act.actType == "RESOLUÇÃO"
            NormType.DELIBERATIONS ->
                act.agency == "CONTRAN" && act.actType == "DELIBERAÇÃO"
            NormType.SENATRAN -> act.agency == "SENATRAN"
            NormType.ALL -> true
            else -> false
        }
    }

    val showCtbResults =
        normalized.isNotBlank() &&
            (selectedType == NormType.ALL || selectedType == NormType.CTB)

    val showNormativeResults =
        selectedType == NormType.CONTRAN ||
            selectedType == NormType.DELIBERATIONS ||
            selectedType == NormType.SENATRAN ||
            (selectedType == NormType.ALL && normalized.isNotBlank())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Normas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (selectedType != NormType.MBFT && selectedType != NormType.SIGNAGE) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = {
                        query = it
                        ctbViewModel.updateQuery(it)
                        normativeViewModel.updateQuery(it)
                    },
                    singleLine = true,
                    label = {
                        Text(
                            when (selectedType) {
                                NormType.CTB -> "Artigo ou termo no CTB"
                                NormType.CONTRAN -> "Número, assunto ou termo no texto da Resolução"
                                NormType.DELIBERATIONS -> "Número, assunto ou termo no texto da Deliberação"
                                NormType.SENATRAN -> "Número, assunto ou termo no texto da Portaria"
                                else -> "Buscar norma, artigo ou termo no texto integral"
                            },
                        )
                    },
                )
            }
        }

        item {
            HorizontalChoicePills(
                options = NormType.entries.map { it.name to it.label },
                selectedId = selectedType.name,
                onSelect = { name ->
                    NormType.entries
                        .firstOrNull { it.name == name }
                        ?.let { selectedType = it }
                },
            )
        }

        if (selectedType == NormType.ALL || selectedType == NormType.CTB) {
            item {
                LocalBaseCard(
                    title = "CTB local pesquisável",
                    description = "Artigos e Anexo I do texto compilado oficial, disponíveis para consulta offline.",
                    source = "Presidência da República • Planalto",
                )
            }
        }

        if (
            selectedType == NormType.ALL ||
            selectedType == NormType.CONTRAN ||
            selectedType == NormType.DELIBERATIONS ||
            selectedType == NormType.SENATRAN
        ) {
            item {
                LocalBaseCard(
                    title = "Base normativa CONTRAN/SENATRAN",
                    description = "Resoluções e Deliberações CONTRAN e Portarias SENATRAN com metadados e texto integral pesquisável offline quando extraível da fonte oficial.",
                    source = "Ministério dos Transportes • fontes oficiais",
                )
            }
        }

        if (selectedType == NormType.ALL || selectedType == NormType.SIGNAGE) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSignage),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "Sinalização oficial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Índice pesquisável dos nove volumes do Manual Brasileiro de Sinalização de Trânsito.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Abrir sinalização",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (showCtbResults) {
            item {
                Text(
                    text = "CTB • ${ctbResults.size} resultado(s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(
                items = ctbResults.take(100),
                key = { it.id },
            ) { article ->
                CtbArticleResultCard(
                    article = article,
                    onClick = { ctbViewModel.open(article.id) },
                )
            }

            if (ctbResults.size > 100) {
                item {
                    ResultLimitText()
                }
            }
        }

        if (showNormativeResults) {
            item {
                val sectionLabel = when (selectedType) {
                    NormType.CONTRAN -> "Resoluções CONTRAN"
                    NormType.DELIBERATIONS -> "Deliberações CONTRAN"
                    NormType.SENATRAN -> "SENATRAN"
                    else -> "CONTRAN/SENATRAN"
                }
                Text(
                    text = "$sectionLabel • ${filteredNormativeActs.size} resultado(s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(
                items = filteredNormativeActs.take(100),
                key = { it.id },
            ) { act ->
                NormativeActResultCard(
                    act = act,
                    onClick = { normativeViewModel.open(act.id) },
                )
            }

            if (filteredNormativeActs.size > 100) {
                item {
                    ResultLimitText()
                }
            }
        }

        if (selectedType == NormType.ALL || selectedType == NormType.MBFT) {
            items(
                items = staticFiltered,
                key = { it.title },
            ) { norm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(norm.url) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = norm.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = norm.description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = norm.type.label + " • " + norm.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (
            selectedType == NormType.CTB &&
            normalized.isBlank()
        ) {
            item {
                HintText("Digite um artigo, expressão ou palavra-chave para pesquisar no CTB.")
            }
        }

        if (
            (
                selectedType == NormType.CONTRAN ||
                    selectedType == NormType.DELIBERATIONS ||
                    selectedType == NormType.SENATRAN
                ) &&
            filteredNormativeActs.isEmpty()
        ) {
            item {
                HintText("Nenhum ato encontrado para o filtro atual.")
            }
        }

    }
}

@Composable
private fun LocalBaseCard(
    title: String,
    description: String,
    source: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Fonte: $source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CtbArticleResultCard(
    article: CtbArticle,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = article.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            article.chapter?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = article.text.replace("\n", " ").take(320),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NormativeActResultCard(
    act: NormativeAct,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = act.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = act.subject,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = buildString {
                    append(act.agency)
                    act.publicationDate?.let {
                        append(" • publicação ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            act.status?.let {
                Text(
                    text = it.take(260),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CtbArticleDetailScreen(
    article: CtbArticle,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BackText(onClick = onBack)
        }

        item {
            Text(
                text = article.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        article.chapter?.let { chapter ->
            item {
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        article.section?.let { section ->
            item {
                Text(
                    text = section,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        article.subsection?.let { subsection ->
            item {
                Text(
                    text = subsection,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        item {
            SelectionContainer {
                Text(
                    text = article.text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            Text(
                text = "Verificado na fonte em ${article.lastCheckedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            OfficialLinkText(
                text = "Abrir texto compilado oficial no Planalto",
                onClick = { uriHandler.openUri(article.sourceUrl) },
            )
        }
    }
}

@Composable
private fun NormativeActDetailScreen(
    act: NormativeAct,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPdf: (OfficialPdfRequest) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BackText(onClick = onBack)
        }

        item {
            Text(
                text = act.displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = act.agency,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SelectionContainer {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = act.subject,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    act.status?.let {
                        Text(
                            text = "Situação/observação oficial: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    act.actDate?.let {
                        Text(
                            text = "Data do ato: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    act.publicationDate?.let {
                        Text(
                            text = "Publicação: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        act.fullText?.takeIf { it.isNotBlank() }?.let { fullText ->
            val displayedText = fullText.take(FULL_TEXT_DISPLAY_LIMIT)
            val wasTruncated = displayedText.length < fullText.length
            item {
                Text(
                    text = "Texto integral indexado offline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (wasTruncated) {
                item {
                    Text(
                        text = "A pesquisa usa o documento completo. Para preservar o desempenho da tela, a leitura abaixo mostra os primeiros " +
                            FULL_TEXT_DISPLAY_LIMIT +
                            " caracteres; o documento oficial completo pode ser aberto ao final.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SelectionContainer {
                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            Text(
                text = "Índice oficial verificado em ${act.lastCheckedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            OfficialLinkText(
                text = if (act.officialUrl.isDirectPdfUrl()) {
                    "Abrir PDF oficial no app"
                } else {
                    "Abrir documento oficial"
                },
                onClick = {
                    if (act.officialUrl.isDirectPdfUrl()) {
                        onOpenPdf(
                            OfficialPdfRequest(
                                url = act.officialUrl,
                                title = act.displayTitle,
                            ),
                        )
                    } else {
                        uriHandler.openUri(act.officialUrl)
                    }
                },
            )
        }

        if (act.sourcePageUrl != act.officialUrl) {
            item {
                OfficialLinkText(
                    text = "Abrir índice oficial de origem",
                    onClick = { uriHandler.openUri(act.sourcePageUrl) },
                )
            }
        }
    }
}

private fun String.isDirectPdfUrl(): Boolean =
    substringBefore('?').endsWith(".pdf", ignoreCase = true)

@Composable
private fun BackText(onClick: () -> Unit) {
    Text(
        text = "← Voltar aos resultados",
        modifier = Modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun OfficialLinkText(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ResultLimitText() {
    Text(
        text = "Mostrando os primeiros 100 resultados. Refine a busca para localizar o item desejado.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}


private const val FULL_TEXT_DISPLAY_LIMIT = 60_000
