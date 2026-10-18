package com.cybertran.enquadraplus.data.update

import android.content.Context
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SafeNormativeUpdateRepository(
    private val context: Context,
    private val database: EnquadraDatabase,
    private val sourceClient: OfficialSourceClient = OfficialSourceClient(),
    private val store: GenerationStore = GenerationStore(context),
    private val promoter: SafeDatasetPromoter = SafeDatasetPromoter(database),
) {

    private val mutex = Mutex()

    private val _states = MutableStateFlow(
        UpdateDataset.entries.map { dataset ->
            DatasetUpdateState(dataset = dataset)
        },
    )
    val states: StateFlow<List<DatasetUpdateState>> = _states.asStateFlow()

    suspend fun bootstrap() {
        mutex.withLock {
            bootstrapLocked()
        }
    }

    suspend fun checkForUpdates() {
        mutex.withLock {
            bootstrapLocked()
            checkCtbLocked()
            checkNormativeCatalogLocked()
            checkMbftLocked()
            refreshStates()
        }
    }

    suspend fun applyCandidate(dataset: UpdateDataset) {
        mutex.withLock {
            val state = store.state(dataset)
            require(state.canApply) {
                "Não existe candidata aprovada para ${dataset.displayName}."
            }
            val candidateJson = requireNotNull(store.candidateJson(dataset)) {
                "Snapshot candidato não encontrado."
            }

            store.mark(
                dataset = dataset,
                status = UpdateStatus.APPLYING,
                message = "Promovendo candidata validada em transação atômica.",
            )
            refreshStates()

            try {
                val promotedVersion = promoter.promote(dataset, candidateJson)
                require(promotedVersion == state.candidateVersion) {
                    "A versão promovida não corresponde à candidata aprovada."
                }
                store.promoteAfterDatabaseCommit(dataset)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.ERROR,
                    message = "Promoção recusada: ${safeMessage(error)}",
                )
                throw error
            } finally {
                refreshStates()
            }
        }
    }

    suspend fun rollback(dataset: UpdateDataset) {
        mutex.withLock {
            val state = store.state(dataset)
            val previousVersion = requireNotNull(state.previousVersion) {
                "Não existe geração anterior para restaurar."
            }
            val previousJson = requireNotNull(store.previousJson(dataset)) {
                "Snapshot anterior não encontrado."
            }

            store.mark(
                dataset = dataset,
                status = UpdateStatus.ROLLING_BACK,
                message = "Restaurando geração validada anterior.",
            )
            refreshStates()

            try {
                val restoredVersion = promoter.promote(dataset, previousJson)
                require(restoredVersion == previousVersion) {
                    "Snapshot restaurado não corresponde à geração anterior."
                }
                store.rollbackAfterDatabaseCommit(dataset)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.ERROR,
                    message = "Rollback recusado: ${safeMessage(error)}",
                )
                throw error
            } finally {
                refreshStates()
            }
        }
    }

    private suspend fun bootstrapLocked() {
        UpdateDataset.entries.forEach { dataset ->
            val bundledJson = loadBundledAsset(dataset)
            val bundledVersion = JSONObject(bundledJson).getString("version")
            val installedVersion = installedVersion(dataset)

            if (installedVersion == bundledVersion) {
                validateBundledSnapshot(dataset, bundledJson)
                val root = JSONObject(bundledJson)
                val recordCount = root.optInt(
                    "recordCount",
                    root.optJSONArray("records")?.length() ?: 0,
                )
                val declaredContentSha = root
                    .optString("contentSha256")
                    .takeIf { it.matches(Regex("[A-F0-9]{64}")) }
                val manifestContentSha = when (dataset) {
                    UpdateDataset.MBFT ->
                        MbftUpdateCodec.parse(bundledJson).contentSha256
                    else ->
                        declaredContentSha ?: CanonicalJson.sha256Upper(bundledJson)
                }
                store.bootstrap(
                    dataset = dataset,
                    activeVersion = bundledVersion,
                    activeJson = bundledJson,
                    manifest = GenerationManifest.bootstrap(
                        dataset = dataset,
                        version = bundledVersion,
                        generatedAt = root.optString("generatedAt"),
                        verifiedAt = root.optString("verifiedAt"),
                        contentSha256 = manifestContentSha,
                        recordCount = recordCount,
                    ),
                )
            }

            store.recoverAgainstDatabase(dataset, installedVersion)
        }

        refreshStates()
    }

    private suspend fun checkCtbLocked() {
        val dataset = UpdateDataset.CTB
        val checkedAt = todayUtc()
        store.mark(
            dataset = dataset,
            status = UpdateStatus.CHECKING,
            checkedAt = checkedAt,
            message = "Consultando o texto compilado oficial do Planalto.",
        )
        refreshStates()

        try {
            val activeJson = requireNotNull(store.activeJson(dataset)) {
                "Snapshot ativo do CTB não está disponível."
            }
            val active = CtbUpdateCodec.parse(activeJson)
            CtbUpdateCodec.validate(active).requireValid("Base ativa do CTB inválida")

            val fetch = sourceClient.fetch(OfficialSourcePolicy.ctbSource())
            val candidate = CtbUpdateCodec.build(fetch, checkedAt)
            val diff = CtbUpdateCodec.diff(active, candidate)

            if (!diff.hasSemanticChanges) {
                store.markParserProven(
                    dataset = dataset,
                    parserVersion = CtbUpdateCodec.PARSER_VERSION,
                    activeVersion = active.version,
                    checkedAt = checkedAt,
                )
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.UP_TO_DATE,
                    checkedAt = checkedAt,
                    message = "Fonte oficial verificada; nenhuma alteração jurídica estruturada detectada.",
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            val report = CtbUpdateCodec.validateDiff(diff)
            if (!report.isValid) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_BLOCKED,
                    checkedAt = checkedAt,
                    message = blockedMessage(report),
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            if (!store.isParserProven(dataset, CtbUpdateCodec.PARSER_VERSION)) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_BLOCKED,
                    checkedAt = checkedAt,
                    message = "Atualização bloqueada: esta versão do parser CTB ainda não reproduziu uma base conhecida e boa sem divergências.",
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            val stagedCtb = store.candidateJson(dataset)?.let(CtbUpdateCodec::parse)
            if (
                stagedCtb != null &&
                !CtbUpdateCodec.diff(stagedCtb, candidate).hasSemanticChanges
            ) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_APPROVED,
                    checkedAt = checkedAt,
                    message = "A candidata já validada continua atual nas fontes oficiais.",
                    diff = diff,
                )
                return
            }

            store.stageApprovedCandidate(
                dataset = dataset,
                version = candidate.version,
                json = candidate.toJson(),
                manifest = GenerationManifest.approvedCandidate(
                    dataset = dataset,
                    version = candidate.version,
                    parserVersion = CtbUpdateCodec.PARSER_VERSION,
                    generatedAt = candidate.generatedAt,
                    verifiedAt = candidate.verifiedAt,
                    contentSha256 = candidate.contentSha256,
                    recordCount = candidate.recordCount,
                    baseVersion = active.version,
                    sources = listOf(GenerationSourceEvidence.fromFetch(fetch)),
                    validationCodes = listOf(
                        "OFFICIAL_SOURCE_ALLOWLIST",
                        "HTTPS_ONLY",
                        "RAW_SHA256",
                        "CANONICAL_SHA256",
                        "STRUCTURAL_INVARIANTS",
                        "REQUIRED_ARTICLES",
                        "DIFF_NO_UNEXPLAINED_REMOVALS",
                    ),
                    diff = diff,
                ),
                checkedAt = checkedAt,
                diff = diff,
                message = "Candidata CTB aprovada pelas validações determinísticas.",
            )
        } catch (error: SourceUnavailableException) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.SOURCE_UNAVAILABLE,
                checkedAt = checkedAt,
                message = "Fonte oficial indisponível; a base ativa foi preservada.",
            )
        } catch (error: IllegalArgumentException) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.CANDIDATE_BLOCKED,
                checkedAt = checkedAt,
                message = "Atualização bloqueada: ${safeMessage(error)}",
                clearCandidate = true,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.ERROR,
                checkedAt = checkedAt,
                message = "Falha segura ao verificar CTB: ${safeMessage(error)}",
            )
        } finally {
            refreshStates()
        }
    }

    private suspend fun checkNormativeCatalogLocked() {
        val dataset = UpdateDataset.NORMATIVE_CATALOG
        val checkedAt = todayUtc()
        store.mark(
            dataset = dataset,
            status = UpdateStatus.CHECKING,
            checkedAt = checkedAt,
            message = "Consultando índices oficiais CONTRAN e SENATRAN.",
        )
        refreshStates()

        try {
            val activeJson = requireNotNull(store.activeJson(dataset)) {
                "Snapshot normativo ativo não está disponível."
            }
            val active = NormativeUpdateCodec.parse(activeJson)
            NormativeUpdateCodec.validate(active).requireValid("Base normativa ativa inválida")

            val specs = OfficialSourcePolicy.normativeSources(currentYearUtc())
            val pages = mutableListOf<Pair<OfficialSourceSpec, OfficialFetchResult>>()
            for (spec in specs) {
                pages += spec to sourceClient.fetch(spec)
            }

            val candidate = NormativeUpdateCodec.build(
                pages = pages,
                checkedAt = checkedAt,
            )
            val diff = NormativeUpdateCodec.diff(active, candidate)

            if (!diff.hasSemanticChanges) {
                store.markParserProven(
                    dataset = dataset,
                    parserVersion = NormativeUpdateCodec.PARSER_VERSION,
                    activeVersion = active.version,
                    checkedAt = checkedAt,
                )
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.UP_TO_DATE,
                    checkedAt = checkedAt,
                    message = "Índices oficiais verificados; nenhuma alteração material detectada.",
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            val report = NormativeUpdateCodec.validateTransition(
                active = active,
                candidate = candidate,
                diff = diff,
            )
            if (!report.isValid) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_BLOCKED,
                    checkedAt = checkedAt,
                    message = blockedMessage(report),
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            if (!store.isParserProven(dataset, NormativeUpdateCodec.PARSER_VERSION)) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_BLOCKED,
                    checkedAt = checkedAt,
                    message = "Atualização bloqueada: esta versão do parser normativo ainda não reproduziu uma base conhecida e boa sem divergências.",
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            val stagedNorms = store.candidateJson(dataset)?.let(NormativeUpdateCodec::parse)
            if (
                stagedNorms != null &&
                !NormativeUpdateCodec.diff(stagedNorms, candidate).hasSemanticChanges
            ) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_APPROVED,
                    checkedAt = checkedAt,
                    message = "A candidata já validada continua atual nas fontes oficiais.",
                    diff = diff,
                )
                return
            }

            store.stageApprovedCandidate(
                dataset = dataset,
                version = candidate.version,
                json = candidate.toJson(),
                manifest = GenerationManifest.approvedCandidate(
                    dataset = dataset,
                    version = candidate.version,
                    parserVersion = NormativeUpdateCodec.PARSER_VERSION,
                    generatedAt = candidate.generatedAt,
                    verifiedAt = candidate.verifiedAt,
                    contentSha256 = candidate.contentSha256,
                    recordCount = candidate.recordCount,
                    baseVersion = active.version,
                    sources = pages.map { (_, fetch) ->
                        GenerationSourceEvidence.fromFetch(fetch)
                    },
                    validationCodes = listOf(
                        "OFFICIAL_SOURCE_ALLOWLIST",
                        "HTTPS_ONLY",
                        "RAW_SHA256_PER_SOURCE",
                        "CANONICAL_SHA256",
                        "STRUCTURAL_INVARIANTS",
                        "REQUIRED_ACTS",
                        "SOURCE_SET_SUPERSET",
                        "DIFF_NO_UNEXPLAINED_REMOVALS",
                    ),
                    diff = diff,
                ),
                checkedAt = checkedAt,
                diff = diff,
                message = "Candidata CONTRAN/SENATRAN aprovada pelas validações determinísticas.",
            )
        } catch (error: SourceUnavailableException) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.SOURCE_UNAVAILABLE,
                checkedAt = checkedAt,
                message = "Uma ou mais fontes oficiais estão indisponíveis; a base ativa foi preservada.",
            )
        } catch (error: IllegalArgumentException) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.CANDIDATE_BLOCKED,
                checkedAt = checkedAt,
                message = "Atualização bloqueada: ${safeMessage(error)}",
                clearCandidate = true,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.ERROR,
                checkedAt = checkedAt,
                message = "Falha segura ao verificar catálogo: ${safeMessage(error)}",
            )
        } finally {
            refreshStates()
        }
    }

    private suspend fun checkMbftLocked() {
        val dataset = UpdateDataset.MBFT
        val checkedAt = todayUtc()
        val legalDate = todayBrazil()

        store.mark(
            dataset = dataset,
            status = UpdateStatus.CHECKING,
            checkedAt = checkedAt,
            message = "Verificando alterações oficiais do MBFT.",
        )
        refreshStates()

        try {
            val activeJson = requireNotNull(store.activeJson(dataset)) {
                "Snapshot ativo do MBFT não está disponível."
            }
            val active = MbftUpdateCodec.parse(activeJson)
            MbftUpdateCodec.validate(active).requireValid("Base ativa do MBFT inválida")

            val rebuiltRelations = InfractionRelationsUpdateCodec.build(active)
            val dao = database.infractionDao()
            val installedRelationsVersion =
                dao.getDatasetVersion(RELATIONS_DATASET_ID)
            val installedRelationCount = dao.relationCount()

            require(
                installedRelationsVersion == rebuiltRelations.version &&
                    installedRelationCount == rebuiltRelations.recordCount,
            ) {
                "As relações instaladas não reproduzem deterministicamente a base MBFT ativa."
            }

            store.markParserProven(
                dataset = dataset,
                parserVersion = MbftUpdateCodec.PARSER_VERSION,
                activeVersion = active.version,
                checkedAt = checkedAt,
            )

            val registryJson = context.assets
                .open(MbftUpdateCodec.REGISTRY_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
            val registry = MbftUpdateCodec.parseRegistry(registryJson)

            val indexSpec = OfficialSourceSpec(
                url = OfficialSourcePolicy.CONTRAN_INDEX_URL,
                kind = OfficialSourceKind.TRANSPORT_HTML,
                minBytes = 5_000,
                maxBytes = 8_000_000,
            )
            val indexFetch = sourceClient.fetch(indexSpec)
            val indexRecords = NormativeHtmlParser.parseContran(
                pageUrl = indexSpec.url,
                sourceHtml = indexFetch.decodeText(),
                checkedAt = checkedAt,
            )
            val detectedMbftActs = indexRecords.filter { act ->
                act.number != 985 &&
                    act.year >= 2023 &&
                    MBFT_985_REFERENCE.containsMatchIn(
                        act.subject + " " + act.status.orEmpty(),
                    )
            }

            val unknownActs = detectedMbftActs.filter { act ->
                (act.number to act.year) !in registry.recognizedAmendments
            }
            if (unknownActs.isNotEmpty()) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_BLOCKED,
                    checkedAt = checkedAt,
                    message = "Atualização bloqueada: o índice oficial registra alteração nova da Resolução 985/2022 ainda não suportada por este parser: " +
                        unknownActs.joinToString { "${it.number}/${it.year}" },
                    clearCandidate = true,
                )
                return
            }

            val appliedKeys = active.appliedAmendments
                .mapNotNull(MbftUpdateCodec::amendmentKey)
                .toSet()
            val pendingActs = detectedMbftActs
                .filter { (it.number to it.year) !in appliedKeys }
                .sortedWith(compareBy<NormativeUpdateRecord> { it.year }.thenBy { it.number })

            if (pendingActs.isEmpty()) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.UP_TO_DATE,
                    checkedAt = checkedAt,
                    message = "Índice CONTRAN verificado; nenhuma emenda MBFT pendente.",
                    diff = UpdateDiff(
                        activeCount = active.recordCount,
                        candidateCount = active.recordCount,
                        added = 0,
                        changed = 0,
                        removed = 0,
                    ),
                    clearCandidate = true,
                )
                return
            }

            var candidate = active
            val evidences = mutableListOf(
                GenerationSourceEvidence.fromFetch(indexFetch),
            )

            for (act in pendingActs) {
                val patch = MbftUpdateCodec.findPatch(registry, act.number, act.year)
                if (patch == null) {
                    store.mark(
                        dataset = dataset,
                        status = UpdateStatus.CANDIDATE_BLOCKED,
                        checkedAt = checkedAt,
                        message = "Atualização bloqueada: a alteração ${act.number}/${act.year} é reconhecida, mas não possui patch auditado promovível.",
                        clearCandidate = true,
                    )
                    return
                }

                if (legalDate < patch.effectiveFrom) {
                    store.mark(
                        dataset = dataset,
                        status = UpdateStatus.UP_TO_DATE,
                        checkedAt = checkedAt,
                        message = "A ${patch.resolution} já foi identificada e auditada, mas sua alteração do MBFT só entra em vigor em ${patch.effectiveFrom}. A base atual permanece ativa.",
                        clearCandidate = true,
                    )
                    return
                }

                require(store.isParserProven(dataset, MbftUpdateCodec.PARSER_VERSION)) {
                    "Parser MBFT ainda não foi calibrado contra a base conhecida e boa."
                }

                val resolutionFetch = sourceClient.fetch(
                    OfficialSourcePolicy.mbftResolutionSource(patch.officialUrl),
                )
                candidate = MbftUpdateCodec.applyPatch(
                    active = candidate,
                    patch = patch,
                    checkedAt = checkedAt,
                    legalDate = legalDate,
                    resolutionFetch = resolutionFetch,
                )
                evidences += GenerationSourceEvidence.fromFetch(resolutionFetch)
            }

            val activeUnresolved =
                InfractionRelationsUpdateCodec.unresolvedReferenceCount(active)
            val candidateUnresolved =
                InfractionRelationsUpdateCodec.unresolvedReferenceCount(candidate)
            val newlyBroken = (candidateUnresolved - activeUnresolved).coerceAtLeast(0)
            val diff = MbftUpdateCodec.diff(
                active = active,
                candidate = candidate,
                brokenReferences = newlyBroken,
            )

            val transition = MbftUpdateCodec.validateTransition(active, candidate, diff)
            if (!transition.isValid) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_BLOCKED,
                    checkedAt = checkedAt,
                    message = blockedMessage(transition),
                    diff = diff,
                    clearCandidate = true,
                )
                return
            }

            val candidateRelations = InfractionRelationsUpdateCodec.build(candidate)
            InfractionRelationsUpdateCodec.validate(candidateRelations, candidate)
                .requireValid("Relações da candidata MBFT inválidas")

            val staged = store.candidateJson(dataset)?.let(MbftUpdateCodec::parse)
            if (
                staged != null &&
                !MbftUpdateCodec.diff(staged, candidate).hasSemanticChanges
            ) {
                store.mark(
                    dataset = dataset,
                    status = UpdateStatus.CANDIDATE_APPROVED,
                    checkedAt = checkedAt,
                    message = "A candidata MBFT já validada continua atual nas fontes oficiais.",
                    diff = diff,
                )
                return
            }

            store.stageApprovedCandidate(
                dataset = dataset,
                version = candidate.version,
                json = candidate.toJson(),
                manifest = GenerationManifest.approvedCandidate(
                    dataset = dataset,
                    version = candidate.version,
                    parserVersion = MbftUpdateCodec.PARSER_VERSION,
                    generatedAt = candidate.generatedAt,
                    verifiedAt = candidate.verifiedAt,
                    contentSha256 = candidate.contentSha256,
                    recordCount = candidate.recordCount,
                    baseVersion = active.version,
                    sources = evidences,
                    validationCodes = listOf(
                        "OFFICIAL_INDEX_DISCOVERY",
                        "UNKNOWN_AMENDMENT_FAIL_CLOSED",
                        "PARSER_KNOWN_GOOD_CALIBRATION",
                        "EXACT_PDF_SHA256",
                        "AUDITED_PATCH_ONLY",
                        "CANONICAL_SHA256",
                        "STRUCTURAL_INVARIANTS",
                        "SEMANTIC_DIFF",
                        "RELATIONS_REBUILT_AND_VALIDATED",
                        "NO_NEW_BROKEN_REFERENCES",
                    ),
                    diff = diff,
                ),
                checkedAt = checkedAt,
                diff = diff,
                message = "Candidata MBFT aprovada; relações derivadas também foram reconstruídas e validadas.",
            )
        } catch (error: SourceUnavailableException) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.SOURCE_UNAVAILABLE,
                checkedAt = checkedAt,
                message = "Fonte oficial MBFT indisponível; a base ativa foi preservada.",
            )
        } catch (error: IllegalArgumentException) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.CANDIDATE_BLOCKED,
                checkedAt = checkedAt,
                message = "Atualização MBFT bloqueada: ${safeMessage(error)}",
                clearCandidate = true,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            store.mark(
                dataset = dataset,
                status = UpdateStatus.ERROR,
                checkedAt = checkedAt,
                message = "Falha segura ao verificar MBFT: ${safeMessage(error)}",
            )
        } finally {
            refreshStates()
        }
    }

    private fun validateBundledSnapshot(
        dataset: UpdateDataset,
        json: String,
    ) {
        when (dataset) {
            UpdateDataset.CTB -> {
                val payload = CtbUpdateCodec.parse(json)
                CtbUpdateCodec.validate(payload).requireValid("Asset CTB inválido")
            }

            UpdateDataset.NORMATIVE_CATALOG -> {
                val payload = NormativeUpdateCodec.parse(json)
                NormativeUpdateCodec.validate(payload).requireValid("Asset normativo inválido")
            }

            UpdateDataset.MBFT -> {
                val payload = MbftUpdateCodec.parse(json)
                MbftUpdateCodec.validate(payload).requireValid("Asset MBFT inválido")
                val relations = InfractionRelationsUpdateCodec.build(payload)
                InfractionRelationsUpdateCodec.validate(relations, payload)
                    .requireValid("Relações derivadas do asset MBFT inválidas")
            }
        }
    }

    private suspend fun installedVersion(dataset: UpdateDataset): String? =
        when (dataset) {
            UpdateDataset.CTB ->
                database.ctbDao().getDatasetVersion(dataset.id)

            UpdateDataset.NORMATIVE_CATALOG ->
                database.normativeDao().getDatasetVersion(dataset.id)

            UpdateDataset.MBFT ->
                database.infractionDao().getDatasetVersion(dataset.id)
        }

    private fun loadBundledAsset(dataset: UpdateDataset): String =
        context.assets
            .open(dataset.bundledAssetPath)
            .bufferedReader()
            .use { it.readText() }

    private fun refreshStates() {
        _states.value = UpdateDataset.entries.map(store::state)
    }

    private fun blockedMessage(report: ValidationReport): String =
        "Candidata recusada. " +
            report.issues.take(4).joinToString(" • ") { issue -> issue.message }

    private fun safeMessage(error: Throwable): String =
        (error.message ?: error::class.java.simpleName)
            .replace(Regex("\\s+"), " ")
            .take(500)

    private fun todayUtc(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC
        }.format(java.util.Date())

    private fun todayBrazil(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = BRAZIL_TIME_ZONE
        }.format(java.util.Date())

    private fun currentYearUtc(): Int =
        Calendar.getInstance(UTC).get(Calendar.YEAR)

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        val BRAZIL_TIME_ZONE: TimeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val MBFT_985_REFERENCE = Regex("""\b985(?:/22)?\b""", RegexOption.IGNORE_CASE)
        const val RELATIONS_DATASET_ID = "infraction-relations"
    }
}
