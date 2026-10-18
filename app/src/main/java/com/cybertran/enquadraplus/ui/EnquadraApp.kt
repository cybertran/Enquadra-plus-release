package com.cybertran.enquadraplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cybertran.enquadraplus.domain.model.InfractionClassification
import com.cybertran.enquadraplus.domain.model.InfractionFilters
import com.cybertran.enquadraplus.domain.model.InfractionListMode
import com.cybertran.enquadraplus.domain.model.InfractionContentStatus
import com.cybertran.enquadraplus.domain.model.InfractionRelationType
import com.cybertran.enquadraplus.domain.model.InfractionNormativeLink
import com.cybertran.enquadraplus.domain.model.TrafficInfraction
import com.cybertran.enquadraplus.domain.model.fineAmountDisplay
import com.cybertran.enquadraplus.ui.infractions.InfractionsViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch

private enum class TopDestination(
    val label: String,
    val symbol: String,
) {
    HOME("Início", "⌂"),
    INFRACTIONS("Infrações", "⌕"),
    NORMS("Normas", "§"),
    TOOLS("Ferramentas", "⚒"),
}

private enum class ToolDestination(val title: String) {
    ALCOHOL("Fiscalização de alcoolemia"),
    CNH("CNH e habilitação"),
    EQUIPMENT("Equipamentos obrigatórios"),
    TOW_HITCH("Engate"),
    TRAFFIC_CRIMES("Crimes de trânsito"),
    LICENSING_DEADLINES("Licenciamento e prazos"),
    DICTIONARY("Dicionário"),
    SIGNAGE("Placas e sinalização"),
    GUIDES("Guias operacionais"),
    NORMATIVE_UPDATES("Atualizações jurídicas"),
}

private enum class SecondaryDestination(
    val title: String,
    val symbol: String,
) {
    SETTINGS("Configurações", "⚙"),
    ABOUT("Sobre", "ⓘ"),
    VERSION("Versão", "v"),
    DEVELOPER("Desenvolvedor", "</>"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnquadraApp(
    infractionsViewModel: InfractionsViewModel = viewModel(),
) {
    var destination by rememberSaveable { mutableStateOf(TopDestination.HOME) }
    var toolDestination by rememberSaveable { mutableStateOf<ToolDestination?>(null) }
    var secondaryDestination by rememberSaveable {
        mutableStateOf<SecondaryDestination?>(null)
    }
    var pdfRequest by remember { mutableStateOf<OfficialPdfRequest?>(null) }
    var aitAssistantInfraction by remember { mutableStateOf<TrafficInfraction?>(null) }
    val selected by infractionsViewModel.selected.collectAsStateWithLifecycle()
    val favoriteCodes by infractionsViewModel.favoriteCodes.collectAsStateWithLifecycle()
    val annotationDraft by infractionsViewModel.annotationDraft.collectAsStateWithLifecycle()
    val annotationSaved by infractionsViewModel.annotationSaved.collectAsStateWithLifecycle()
    val applicableNorms by infractionsViewModel.applicableNorms.collectAsStateWithLifecycle()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = DrawerValue.Closed,
    )
    val scope = rememberCoroutineScope()
    val canUseDrawer =
        pdfRequest == null &&
            aitAssistantInfraction == null &&
            selected == null &&
            toolDestination == null &&
            secondaryDestination == null

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = canUseDrawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Enquadra+",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    SecondaryDestination.entries.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            icon = { Text(item.symbol) },
                            selected = secondaryDestination == item,
                            onClick = {
                                secondaryDestination = item
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                pdfRequest != null -> "Documento oficial"
                                aitAssistantInfraction != null -> "Observação do AIT"
                                selected != null -> "Ficha da infração"
                                toolDestination != null -> toolDestination!!.title
                                secondaryDestination != null -> secondaryDestination!!.title
                                else -> "Enquadra+"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        if (
                            pdfRequest != null ||
                            aitAssistantInfraction != null ||
                            selected != null ||
                            toolDestination != null ||
                            secondaryDestination != null
                        ) {
                            IconButton(
                                onClick = {
                                    when {
                                        pdfRequest != null -> pdfRequest = null
                                        aitAssistantInfraction != null -> aitAssistantInfraction = null
                                        selected != null -> infractionsViewModel.closeDetail()
                                        toolDestination != null -> toolDestination = null
                                        secondaryDestination != null -> secondaryDestination = null
                                    }
                                },
                            ) {
                                Text("←", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.open() }
                                },
                            ) {
                                Text(
                                    text = "☰",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            bottomBar = {
                if (
                    pdfRequest == null &&
                    aitAssistantInfraction == null &&
                    selected == null &&
                    toolDestination == null &&
                    secondaryDestination == null
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        TopDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Text(item.symbol) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            when {
                pdfRequest != null -> {
                    OfficialPdfScreen(
                        contentPadding = innerPadding,
                        request = pdfRequest!!,
                    )
                }
                aitAssistantInfraction != null -> {
                    AitObservationAssistantScreen(
                        contentPadding = innerPadding,
                        infraction = aitAssistantInfraction!!,
                    )
                }
                selected != null -> {
                    InfractionDetailScreen(
                        infraction = selected!!,
                        contentPadding = innerPadding,
                        onOpenRelated = infractionsViewModel::open,
                        isFavorite = selected!!.code in favoriteCodes,
                        annotationDraft = annotationDraft,
                        annotationSaved = annotationSaved,
                        onToggleFavorite = {
                            infractionsViewModel.toggleFavorite(selected!!.code)
                        },
                        onAnnotationChange = infractionsViewModel::updateAnnotationDraft,
                        onSaveAnnotation = infractionsViewModel::saveAnnotation,
                        applicableNorms = applicableNorms,
                        onOpenPdf = { pdfRequest = it },
                        onOpenAitAssistant = { aitAssistantInfraction = selected },
                    )
                }
                toolDestination == ToolDestination.ALCOHOL -> {
                    AlcoholScreen(innerPadding)
                }
                toolDestination == ToolDestination.CNH -> {
                    CnhToolScreen(innerPadding)
                }
                toolDestination == ToolDestination.EQUIPMENT -> {
                    EquipmentToolScreen(innerPadding)
                }
                toolDestination == ToolDestination.TOW_HITCH -> {
                    TowHitchToolScreen(innerPadding)
                }
                toolDestination == ToolDestination.TRAFFIC_CRIMES -> {
                    TrafficCrimesToolScreen(innerPadding)
                }
                toolDestination == ToolDestination.LICENSING_DEADLINES -> {
                    LicensingDeadlinesToolScreen(innerPadding)
                }
                toolDestination == ToolDestination.DICTIONARY -> {
                    TrafficDictionaryToolScreen(innerPadding)
                }
                toolDestination == ToolDestination.SIGNAGE -> {
                    SignageToolScreen(
                        contentPadding = innerPadding,
                        onOpenPdf = { pdfRequest = it },
                    )
                }
                toolDestination == ToolDestination.GUIDES -> {
                    OperationalGuidesScreen(innerPadding)
                }
                toolDestination == ToolDestination.NORMATIVE_UPDATES -> {
                    NormativeUpdateScreen(innerPadding)
                }
                secondaryDestination == SecondaryDestination.SETTINGS -> {
                    AppSettingsScreen(
                        contentPadding = innerPadding,
                        onOpenUpdates = {
                            secondaryDestination = null
                            toolDestination = ToolDestination.NORMATIVE_UPDATES
                        },
                    )
                }
                secondaryDestination == SecondaryDestination.ABOUT -> {
                    AboutAppScreen(innerPadding)
                }
                secondaryDestination == SecondaryDestination.VERSION -> {
                    VersionInfoScreen(innerPadding)
                }
                secondaryDestination == SecondaryDestination.DEVELOPER -> {
                    DeveloperInfoScreen(
                        contentPadding = innerPadding,
                        onOpenUpdates = {
                            secondaryDestination = null
                            toolDestination = ToolDestination.NORMATIVE_UPDATES
                        },
                    )
                }
                else -> {
                    when (destination) {
                        TopDestination.HOME -> HomeScreen(
                            contentPadding = innerPadding,
                            onOpenInfractions = { destination = TopDestination.INFRACTIONS },
                            onOpenNorms = { destination = TopDestination.NORMS },
                            onOpenTools = { destination = TopDestination.TOOLS },
                        )
                        TopDestination.INFRACTIONS -> InfractionsScreen(
                            contentPadding = innerPadding,
                            viewModel = infractionsViewModel,
                        )
                        TopDestination.NORMS -> OfficialNormsScreen(
                            contentPadding = innerPadding,
                            onOpenSignage = { toolDestination = ToolDestination.SIGNAGE },
                            onOpenPdf = { pdfRequest = it },
                        )
                        TopDestination.TOOLS -> ToolsScreen(
                            contentPadding = innerPadding,
                            onOpenAlcohol = { toolDestination = ToolDestination.ALCOHOL },
                            onOpenCnh = { toolDestination = ToolDestination.CNH },
                            onOpenEquipment = { toolDestination = ToolDestination.EQUIPMENT },
                            onOpenTowHitch = { toolDestination = ToolDestination.TOW_HITCH },
                            onOpenTrafficCrimes = { toolDestination = ToolDestination.TRAFFIC_CRIMES },
                            onOpenLicensingDeadlines = { toolDestination = ToolDestination.LICENSING_DEADLINES },
                            onOpenDictionary = { toolDestination = ToolDestination.DICTIONARY },
                            onOpenGuides = { toolDestination = ToolDestination.GUIDES },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenInfractions: () -> Unit,
    onOpenNorms: () -> Unit,
    onOpenTools: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item { HomeHero() }

        item {
            HomeSection(title = "Acesso rápido") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeShortcutCard(
                        modifier = Modifier.fillMaxWidth(),
                        symbol = "⌕",
                        title = "Infrações",
                        description = "Buscar por código, artigo ou palavra-chave",
                        onClick = onOpenInfractions,
                    )
                    HomeShortcutCard(
                        modifier = Modifier.fillMaxWidth(0.48f),
                        symbol = "§",
                        title = "Normas",
                        description = "CTB, atos e sinalização",
                        onClick = onOpenNorms,
                    )
                    HomeShortcutCard(
                        modifier = Modifier.fillMaxWidth(0.48f),
                        symbol = "⚒",
                        title = "Ferramentas",
                        description = "Fiscalização e consultas rápidas",
                        onClick = onOpenTools,
                    )
                }
            }
        }

        item {
            Button(
                onClick = onOpenInfractions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Buscar infração",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HomeHero() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Enquadra+",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "Consulta prática de trânsito.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun HomeShortcutCard(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 116.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfractionsScreen(
    contentPadding: PaddingValues,
    viewModel: InfractionsViewModel,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val listMode by viewModel.listMode.collectAsStateWithLifecycle()
    val favoriteCodes by viewModel.favoriteCodes.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Infrações",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = viewModel::updateQuery,
                singleLine = true,
                leadingIcon = { Text("⌕") },
                label = { Text("Código, artigo ou palavra-chave") },
            )
        }
        item {
            HorizontalChoicePills(
                options = InfractionListMode.entries.map { it.name to it.label() },
                selectedId = listMode.name,
                onSelect = { name ->
                    InfractionListMode.entries
                        .firstOrNull { it.name == name }
                        ?.let(viewModel::setListMode)
                },
            )
        }
        item {
            TonalChoicePill(
                label = buildString {
                    append("Filtros")
                    if (filters.activeCount > 0) {
                        append(" (")
                        append(filters.activeCount)
                        append(")")
                    }
                },
                selected = showFilters || filters.activeCount > 0,
                onClick = { showFilters = !showFilters },
            )
        }
        if (showFilters) {
            item {
                InfractionFiltersPanel(
                    filters = filters,
                    onToggleClassification = viewModel::toggleClassification,
                    onTogglePoints = viewModel::togglePoints,
                    onToggleCrimeOnly = viewModel::toggleCrimeOnly,
                    onAuthorityChange = viewModel::updateAuthorityFilter,
                    onVerificationChange = viewModel::updateVerificationFilter,
                    onClear = viewModel::clearFilters,
                )
            }
        }
        if (results.isEmpty()) {
            item {
                Text(
                    text = when (listMode) {
                        InfractionListMode.ALL -> "Nenhuma infração encontrada."
                        InfractionListMode.FAVORITES -> "Nenhuma infração favorita encontrada."
                        InfractionListMode.RECENT -> "Nenhuma infração recente encontrada."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(
                items = results,
                key = { it.code },
            ) { infraction ->
                InfractionResultCard(
                    infraction = infraction,
                    isFavorite = infraction.code in favoriteCodes,
                    onClick = { viewModel.open(infraction.code) },
                )
            }
        }
    }
}

@Composable
private fun InfractionFiltersPanel(
    filters: InfractionFilters,
    onToggleClassification: (InfractionClassification) -> Unit,
    onTogglePoints: (Int) -> Unit,
    onToggleCrimeOnly: () -> Unit,
    onAuthorityChange: (String) -> Unit,
    onVerificationChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Natureza",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    InfractionClassification.LIGHT,
                    InfractionClassification.MEDIUM,
                    InfractionClassification.SERIOUS,
                    InfractionClassification.VERY_SERIOUS,
                ).forEach { classification ->
                    TonalChoicePill(
                        label = classification.label(),
                        selected = classification in filters.classifications,
                        onClick = { onToggleClassification(classification) },
                    )
                }
            }
            Text(
                text = "Pontos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(3, 4, 5, 7).forEach { points ->
                    TonalChoicePill(
                        label = points.toString(),
                        selected = points in filters.points,
                        onClick = { onTogglePoints(points) },
                    )
                }
                TonalChoicePill(
                    label = "Pode configurar crime",
                    selected = filters.crimeOnly,
                    onClick = onToggleCrimeOnly,
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = filters.authorityQuery,
                onValueChange = onAuthorityChange,
                singleLine = true,
                label = { Text("Competência contém") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = filters.verificationQuery,
                onValueChange = onVerificationChange,
                singleLine = true,
                label = { Text("Constatação contém") },
            )
            if (filters.activeCount > 0) {
                Button(onClick = onClear) {
                    Text("Limpar filtros")
                }
            }
        }
    }
}

@Composable
private fun InfractionResultCard(
    infraction: TrafficInfraction,
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StaticTag(infraction.code)
                StaticTag(infraction.article)
                if (isFavorite) {
                    StaticTag("★ Favorita")
                }
                if (infraction.contentStatus == InfractionContentStatus.PARTIAL_VALIDATION) {
                    StaticTag("Em validação")
                }
            }
            Text(
                text = infraction.shortDescription,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun InfractionDetailScreen(
    infraction: TrafficInfraction,
    contentPadding: PaddingValues,
    onOpenRelated: (String) -> Unit,
    isFavorite: Boolean,
    annotationDraft: String,
    annotationSaved: Boolean,
    onToggleFavorite: () -> Unit,
    onAnnotationChange: (String) -> Unit,
    onSaveAnnotation: () -> Unit,
    applicableNorms: List<InfractionNormativeLink>,
    onOpenPdf: (OfficialPdfRequest) -> Unit,
    onOpenAitAssistant: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showUserAnnotation by rememberSaveable(infraction.code) {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StaticTag("Código " + infraction.code)
                    StaticTag(infraction.classification.label())
                    infraction.points?.let { points ->
                        StaticTag("$points pontos")
                    }
                    Surface(
                        modifier = Modifier.clickable(onClick = onToggleFavorite),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isFavorite) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                        },
                    ) {
                        Text(
                            text = if (isFavorite) "★ Favorita" else "☆ Favoritar",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFavorite) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                if (infraction.contentStatus == InfractionContentStatus.PARTIAL_VALIDATION) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "Conteúdo em validação: esta ficha ainda não contém todos os campos do MBFT. Consulte a fonte oficial antes de usar a informação em fiscalização.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Text(
                    text = infraction.article,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = infraction.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            DetailFields(infraction)
        }

        if (infraction.legalBasis.isNotEmpty()) {
            item { TextSection("Amparo legal", infraction.legalBasis) }
        }

        if (infraction.whenToCite.isNotEmpty()) {
            item { TextSection("Quando autuar", infraction.whenToCite) }
        }

        if (infraction.whenNotToCite.isNotEmpty()) {
            item { TextSection("Quando não autuar", infraction.whenNotToCite) }
        }

        if (infraction.definitionsAndProcedures.isNotEmpty()) {
            item {
                TextSection(
                    "Definições e procedimentos",
                    infraction.definitionsAndProcedures,
                )
            }
        }

        if (infraction.aitOfficialObservations.isNotEmpty()) {
            item {
                TextSection(
                    "Exemplos oficiais do campo de observações do AIT",
                    infraction.aitOfficialObservations,
                )
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenAitAssistant,
            ) {
                Text("Montar observação do AIT")
            }
        }

        if (applicableNorms.isNotEmpty()) {
            item {
                ApplicableNormsSection(
                    acts = applicableNorms,
                    onOpenPdf = onOpenPdf,
                )
            }
        }

        item {
            Text(
                text = if (showUserAnnotation) {
                    "▴ Ocultar minha anotação"
                } else {
                    "▾ Minha anotação"
                },
                modifier = Modifier.clickable {
                    showUserAnnotation = !showUserAnnotation
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (showUserAnnotation) {
            item {
                UserAnnotationSection(
                    value = annotationDraft,
                    saved = annotationSaved,
                    onValueChange = onAnnotationChange,
                    onSave = onSaveAnnotation,
                )
            }
        }

        if (infraction.relatedInfractions.isNotEmpty()) {
            item {
                RelatedInfractionSection(
                    infraction = infraction,
                    onOpenRelated = onOpenRelated,
                )
            }
        }

        val sourceLinks = infraction.sources.filter { it.officialUrl != null }
        if (sourceLinks.isNotEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sourceLinks.forEachIndexed { index, source ->
                        Text(
                            text = if (sourceLinks.size == 1) {
                                "Ver fonte"
                            } else {
                                "Ver fonte ${index + 1}"
                            },
                            modifier = Modifier.clickable {
                                source.officialUrl?.let(uriHandler::openUri)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFields(infraction: TrafficInfraction) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        infraction.fineAmountDisplay()?.let { fine ->
            DetailLine("Valor da multa", fine.value)
            fine.detail?.let { detail ->
                DetailLine("Observação do valor", detail)
            }
        }
        DetailLine("Penalidade", infraction.penalty)
        DetailLine("Medida administrativa", infraction.administrativeMeasure)
        DetailLine("Infrator", infraction.offenderResponsibility)
        DetailLine("Competência", infraction.authority)
        DetailLine("Constatação", infraction.verificationMethod)
        DetailLine(
            "Pode configurar crime de trânsito",
            if (infraction.canConfigureTrafficCrime) "Sim" else "Não",
        )
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun TextSection(
    title: String,
    values: List<String>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )

        values.forEachIndexed { index, value ->
            Text(
                text = (index + 1).toString() + ". " + value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ToolsScreen(
    contentPadding: PaddingValues,
    onOpenAlcohol: () -> Unit,
    onOpenCnh: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenTowHitch: () -> Unit,
    onOpenTrafficCrimes: () -> Unit,
    onOpenLicensingDeadlines: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenGuides: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Ferramentas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item { ToolSectionTitle("Fiscalização guiada") }
        item {
            ToolCard(
                title = "Fiscalização de alcoolemia",
                description = "Etilômetro, sinais psicomotores e fluxo oficial",
                onClick = onOpenAlcohol,
            )
        }
        item {
            ToolCard(
                title = "CNH e habilitação",
                description = "Categorias e requisitos",
                onClick = onOpenCnh,
            )
        }
        item {
            ToolCard(
                title = "Equipamentos obrigatórios",
                description = "Checklist por veículo",
                onClick = onOpenEquipment,
            )
        }
        item {
            ToolCard(
                title = "Engate",
                description = "Checklist de conformidade",
                onClick = onOpenTowHitch,
            )
        }
        item {
            ToolCard(
                title = "Licenciamento e prazos",
                description = "Calendários e prazos oficiais",
                onClick = onOpenLicensingDeadlines,
            )
        }

        item { ToolSectionTitle("Consultas rápidas") }
        item {
            ToolCard(
                title = "Crimes de trânsito",
                description = "Consulta aos arts. 302 a 312 do CTB",
                onClick = onOpenTrafficCrimes,
            )
        }
        item {
            ToolCard(
                title = "Dicionário",
                description = "Termos e definições oficiais",
                onClick = onOpenDictionary,
            )
        }

        item { ToolSectionTitle("Guias operacionais") }
        item {
            ToolCard(
                title = "Guias de fiscalização",
                description = "Fluxos próprios para conferência rápida",
                onClick = onOpenGuides,
            )
        }
    }
}

@Composable
private fun ToolSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlcoholScreen(contentPadding: PaddingValues) {
    var measuredText by rememberSaveable { mutableStateOf("") }
    val selectedSigns = remember { mutableStateListOf<String>() }
    val result = calculateAlcohol(measuredText)
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Fiscalização de alcoolemia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Reúna medição, sinais observados e referências do procedimento em um único fluxo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text(
                text = "1. Etilômetro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = measuredText,
                onValueChange = { value ->
                    measuredText = value.filter { it.isDigit() || it == ',' || it == '.' }
                },
                singleLine = true,
                label = { Text("Medição realizada (mg/L)") },
                supportingText = { Text("Informe o valor exibido pelo etilômetro.") },
            )
        }

        result?.let { alcohol ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = alcohol.outcome.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        DetailLine("Medição realizada", formatMg(alcohol.measured))
                        DetailLine("Valor considerado", formatMg(alcohol.considered))
                        DetailLine("Erro máximo admissível", formatMg(alcohol.errorMargin))
                        Text(
                            text = alcohol.outcome.reference,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "2. Sinais de alteração psicomotora",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(
                    text = if (selectedSigns.size >= 2) {
                        "Foram marcados ${selectedSigns.size} sinais. O conjunto deve ser avaliado no contexto da abordagem; a contagem isolada não substitui a constatação."
                    } else {
                        "Marque somente os sinais efetivamente observados. A Resolução nº 1.031/2026 exige ao menos dois sinais considerados em conjunto para caracterização por sinais."
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        PSYCHOMOTOR_SIGN_GROUPS.forEach { group ->
            item {
                Text(
                    text = group.first,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(group.second) { sign ->
                ChecklistRow(
                    title = sign,
                    checked = sign in selectedSigns,
                    onClick = {
                        if (sign in selectedSigns) {
                            selectedSigns.remove(sign)
                        } else {
                            selectedSigns.add(sign)
                        }
                    },
                )
            }
        }

        item {
            Text(
                text = "3. Encaminhamento operacional",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Fluxo oficial de fiscalização",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        items(ALCOHOL_OPERATIONAL_GUIDANCE) { guidance ->
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
                        text = guidance.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = guidance.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
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
                        text = "Referência",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Resolução CONTRAN nº 1.031/2026 • Anexo I",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "0,05 a 0,33 mg/L: art. 165 • a partir de 0,34 mg/L: art. 165 + art. 306.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Ver fonte",
                        modifier = Modifier.clickable {
                            uriHandler.openUri(ALCOHOL_SOURCE_URL)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            Text(
                text = "Ferramenta de apoio à conferência. O procedimento deve observar o equipamento, o contexto da fiscalização e a norma vigente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ApplicableNormsSection(
    acts: List<InfractionNormativeLink>,
    onOpenPdf: (OfficialPdfRequest) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Normas relacionadas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Vínculos oficiais auditados. Toque em uma norma para consultar a fonte.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        acts.forEach { link ->
            val act = link.act
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (act.officialUrl.isOfficialPdfLink()) {
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                        text = "Ver norma oficial",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun String.isOfficialPdfLink(): Boolean =
    substringBefore('?').endsWith(".pdf", ignoreCase = true)

@Composable
private fun UserAnnotationSection(
    value: String,
    saved: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Minha anotação",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Conteúdo pessoal armazenado pelo app; não faz parte do MBFT.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                minLines = 3,
                label = { Text("Anotação") },
            )
            Button(onClick = onSave) {
                Text(if (saved) "Salvo" else "Salvar anotação")
            }
        }
    }
}

@Composable
private fun RelatedInfractionSection(
    infraction: TrafficInfraction,
    onOpenRelated: (String) -> Unit,
) {
    val groups = listOf(
        InfractionRelationType.CONCOMITANT to "Autuar também",
        InfractionRelationType.PREREQUISITE to "Autuação prévia exigida",
        InfractionRelationType.ALTERNATIVE to "Usar outro enquadramento",
        InfractionRelationType.RELATED to "Enquadramentos relacionados",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groups.forEach { (type, title) ->
            val relations = infraction.relatedInfractions.filter { it.type == type }
            if (relations.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )

                relations.forEach { related ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRelated(related.code) },
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = related.code,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            related.reason?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            related.evidence.forEach { evidence ->
                                SelectionContainer {
                                    Text(
                                        text = evidence,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }

                            Text(
                                text = when (related.origin) {
                                    "MBFT_OFFICIAL_FIELD" ->
                                        "Regra extraída de campo oficial do MBFT."
                                    else ->
                                        "Relação de navegação criada pelo Enquadra+ a partir das fontes oficiais; não é campo do MBFT."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderList(
    contentPadding: PaddingValues,
    title: String,
    items: List<Pair<String, String>>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        items(items) { item ->
            FeatureCard(
                title = item.first,
                description = item.second,
            )
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val ALCOHOL_SOURCE_URL =
    "https://anttlegis.antt.gov.br/action/ActionDatalegis.php?acao=abrirTextoAto&cod_menu=7145&cod_modulo=420&nomeTitulo=codigos&numeroAto=00001031&orgao=CONTRAN%2FMT&seqAto=000&tipo=RES&valorAno=2026"

private data class AlcoholOperationalGuidance(
    val title: String,
    val text: String,
)

private val ALCOHOL_OPERATIONAL_GUIDANCE = listOf(
    AlcoholOperationalGuidance(
        title = "Triagem / pré-teste",
        text = "O pré-teste ou teste passivo é facultativo e apenas orientativo. O resultado isolado não caracteriza infração nem pode fundamentar autuação.",
    ),
    AlcoholOperationalGuidance(
        title = "Etilômetro",
        text = "Use equipamento com modelo aprovado e verificação metrológica válida. Faça reteste somente quando fator técnico ou operacional prejudicar um resultado válido, inclusive para afastar álcool residual no trato respiratório superior.",
    ),
    AlcoholOperationalGuidance(
        title = "Recusa",
        text = "Se houver recusa e menos de dois sinais de alteração psicomotora, aplica-se o art. 165-A. Com dois ou mais sinais que, em conjunto, comprovem a alteração, aplica-se o art. 165, sem prejuízo do art. 306. Na mesma abordagem, não se acumulam os arts. 165 e 165-A.",
    ),
    AlcoholOperationalGuidance(
        title = "Registro no AIT",
        text = "Registre os dados exigidos para o meio utilizado: identificação do etilômetro e do teste, medição realizada e valor considerado; ou sinais observados/termo específico; além de recusa, testemunhas e demais provas quando houver.",
    ),
    AlcoholOperationalGuidance(
        title = "Crime de trânsito",
        text = "Medição realizada a partir de 0,34 mg/L ou os demais meios previstos na Resolução podem caracterizar o art. 306. A constatação criminal não elimina a autuação administrativa do art. 165.",
    ),
    AlcoholOperationalGuidance(
        title = "Veículo e habilitação",
        text = "O veículo fica retido até a apresentação de condutor habilitado que também seja submetido à fiscalização. Sem condutor apto, deve ser removido. A CNH não é recolhida apenas em razão dessas infrações.",
    ),
)

private val PSYCHOMOTOR_SIGN_GROUPS = listOf(
    "Aparência" to listOf(
        "Sonolência",
        "Olhos vermelhos",
        "Vômito",
        "Soluços",
        "Desordem nas vestes",
        "Odor de álcool no hálito",
    ),
    "Atitude" to listOf(
        "Agressividade",
        "Arrogância",
        "Exaltação",
        "Ironia",
        "Falante",
        "Dispersão",
    ),
    "Orientação" to listOf(
        "Não sabe onde está",
        "Não sabe a data ou a hora",
    ),
    "Memória" to listOf(
        "Não sabe o próprio endereço",
        "Não lembra dos atos cometidos",
    ),
    "Capacidade motora e verbal" to listOf(
        "Dificuldade no equilíbrio",
        "Fala alterada",
    ),
)

private enum class AlcoholOutcome(
    val title: String,
    val reference: String,
) {
    BELOW_LIMIT(
        "Sem enquadramento pelo etilômetro",
        "Medição inferior a 0,05 mg/L.",
    ),
    ADMINISTRATIVE(
        "Infração administrativa",
        "Art. 165 do CTB.",
    ),
    CRIME(
        "Infração administrativa + crime",
        "Art. 165 e art. 306 do CTB.",
    ),
}

private data class AlcoholResult(
    val measured: BigDecimal,
    val considered: BigDecimal,
    val errorMargin: BigDecimal,
    val outcome: AlcoholOutcome,
)

private fun calculateAlcohol(input: String): AlcoholResult? {
    val normalized = input.trim().replace(',', '.')
    val measured = normalized.toBigDecimalOrNull() ?: return null
    if (measured.signum() < 0) return null

    val errorMargin = when {
        measured <= BigDecimal("0.40") -> BigDecimal("0.032")
        measured <= BigDecimal("2.00") -> measured.multiply(BigDecimal("0.08"))
        else -> measured.multiply(BigDecimal("0.30"))
    }

    val rawConsidered = measured.subtract(errorMargin)
    val considered = if (rawConsidered.signum() < 0) {
        BigDecimal.ZERO.setScale(2)
    } else {
        rawConsidered.setScale(2, RoundingMode.DOWN)
    }

    val outcome = when {
        measured < BigDecimal("0.05") -> AlcoholOutcome.BELOW_LIMIT
        measured < BigDecimal("0.34") -> AlcoholOutcome.ADMINISTRATIVE
        else -> AlcoholOutcome.CRIME
    }

    return AlcoholResult(
        measured = measured,
        considered = considered,
        errorMargin = errorMargin,
        outcome = outcome,
    )
}

private fun formatMg(value: BigDecimal): String =
    value.stripTrailingZeros()
        .toPlainString()
        .replace('.', ',') + " mg/L"

private fun InfractionListMode.label(): String =
    when (this) {
        InfractionListMode.ALL -> "Todas"
        InfractionListMode.FAVORITES -> "Favoritas"
        InfractionListMode.RECENT -> "Recentes"
    }

private fun InfractionClassification.label(): String =
    when (this) {
        InfractionClassification.LIGHT -> "Leve"
        InfractionClassification.MEDIUM -> "Média"
        InfractionClassification.SERIOUS -> "Grave"
        InfractionClassification.VERY_SERIOUS -> "Gravíssima"
        InfractionClassification.OTHER -> "Outra"
    }
