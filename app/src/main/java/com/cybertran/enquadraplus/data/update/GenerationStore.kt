package com.cybertran.enquadraplus.data.update

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

class GenerationStore(
    context: Context,
) {
    private val root = File(context.filesDir, "normative-update").apply {
        mkdirs()
    }

    @Synchronized
    fun bootstrap(
        dataset: UpdateDataset,
        activeVersion: String,
        activeJson: String,
        manifest: GenerationManifest,
    ) {
        val current = readState(dataset)
        writeGeneration(dataset, activeVersion, activeJson)
        writeManifest(dataset, activeVersion, manifest)

        if (current.activeVersion == null) {
            writeState(
                dataset,
                current.copy(
                    activeVersion = activeVersion,
                    status = UpdateStatus.IDLE,
                    message = "Base validada inicial registrada.",
                ),
            )
            appendHistory(dataset, "BOOTSTRAP", activeVersion, "Base inicial registrada.")
        }
    }

    @Synchronized
    fun recoverAgainstDatabase(
        dataset: UpdateDataset,
        databaseVersion: String?,
    ) {
        if (databaseVersion.isNullOrBlank()) return

        val current = readState(dataset)
        if (current.activeVersion == databaseVersion) return

        val recovered = when {
            current.candidateVersion == databaseVersion -> {
                current.copy(
                    previousVersion = current.activeVersion,
                    activeVersion = databaseVersion,
                    candidateVersion = null,
                    status = UpdateStatus.APPLIED,
                    message = "Promoção concluída após recuperação de interrupção.",
                )
            }

            current.previousVersion == databaseVersion -> {
                current.copy(
                    previousVersion = current.activeVersion,
                    activeVersion = databaseVersion,
                    candidateVersion = null,
                    status = UpdateStatus.ROLLED_BACK,
                    message = "Rollback concluído após recuperação de interrupção.",
                )
            }

            generationFile(dataset, databaseVersion).isFile -> {
                current.copy(
                    previousVersion = current.activeVersion?.takeIf { it != databaseVersion },
                    activeVersion = databaseVersion,
                    candidateVersion = null,
                    status = UpdateStatus.IDLE,
                    message = "Ponteiro da geração reconciliado com o banco local.",
                )
            }

            else -> current.copy(
                status = UpdateStatus.CANDIDATE_BLOCKED,
                message = "Versão ativa do banco não possui snapshot de geração correspondente.",
            )
        }

        writeState(dataset, recovered)
        appendHistory(
            dataset,
            "RECOVERY",
            databaseVersion,
            recovered.message,
        )
    }

    @Synchronized
    fun stageApprovedCandidate(
        dataset: UpdateDataset,
        version: String,
        json: String,
        manifest: GenerationManifest,
        checkedAt: String,
        diff: UpdateDiff,
        message: String,
    ) {
        require(manifest.version == version && manifest.datasetId == dataset.id) {
            "Manifesto não corresponde à candidata."
        }
        writeGeneration(dataset, version, json)
        writeManifest(dataset, version, manifest)
        val current = readState(dataset)
        writeState(
            dataset,
            current.copy(
                candidateVersion = version,
                lastCheckedAt = checkedAt,
                status = UpdateStatus.CANDIDATE_APPROVED,
                message = message,
                diff = diff,
            ),
        )
        appendHistory(dataset, "CANDIDATE_APPROVED", version, "$message • ${diff.shortSummary()}")
    }

    @Synchronized
    fun mark(
        dataset: UpdateDataset,
        status: UpdateStatus,
        checkedAt: String? = null,
        message: String,
        diff: UpdateDiff? = null,
        clearCandidate: Boolean = false,
    ) {
        val current = readState(dataset)
        val updated = current.copy(
            candidateVersion = if (clearCandidate) null else current.candidateVersion,
            lastCheckedAt = checkedAt ?: current.lastCheckedAt,
            status = status,
            message = message,
            diff = diff,
        )
        writeState(dataset, updated)
        cleanupUnreferenced(dataset, updated)
        appendHistory(dataset, status.name, current.candidateVersion, message)
    }

    @Synchronized
    fun promoteAfterDatabaseCommit(dataset: UpdateDataset) {
        val current = readState(dataset)
        val candidate = requireNotNull(current.candidateVersion) {
            "Não existe geração candidata para promover."
        }
        require(generationFile(dataset, candidate).isFile) {
            "Arquivo da candidata não existe."
        }
        require(manifestFile(dataset, candidate).isFile) {
            "Manifesto da candidata não existe."
        }

        val promoted = current.copy(
            previousVersion = current.activeVersion,
            activeVersion = candidate,
            candidateVersion = null,
            status = UpdateStatus.APPLIED,
            message = "Atualização promovida com sucesso.",
        )
        writeState(dataset, promoted)
        cleanupUnreferenced(dataset, promoted)
        appendHistory(dataset, "PROMOTED", candidate, promoted.message)
    }

    @Synchronized
    fun rollbackAfterDatabaseCommit(dataset: UpdateDataset) {
        val current = readState(dataset)
        val previous = requireNotNull(current.previousVersion) {
            "Não existe geração anterior para restaurar."
        }
        require(generationFile(dataset, previous).isFile) {
            "Snapshot anterior não existe."
        }
        require(manifestFile(dataset, previous).isFile) {
            "Manifesto da geração anterior não existe."
        }

        val rolledBack = current.copy(
            previousVersion = current.activeVersion,
            activeVersion = previous,
            candidateVersion = null,
            status = UpdateStatus.ROLLED_BACK,
            message = "Versão anterior restaurada.",
        )
        writeState(dataset, rolledBack)
        cleanupUnreferenced(dataset, rolledBack)
        appendHistory(dataset, "ROLLBACK", previous, rolledBack.message)
    }

    @Synchronized
    fun markParserProven(
        dataset: UpdateDataset,
        parserVersion: String,
        activeVersion: String,
        checkedAt: String,
    ) {
        val proof = JSONObject()
            .put("parserVersion", parserVersion)
            .put("provenAgainstVersion", activeVersion)
            .put("provenAt", checkedAt)
            .toString(2) + "\n"
        atomicWrite(parserProofFile(dataset), proof)
        appendHistory(
            dataset,
            "PARSER_PROVEN",
            activeVersion,
            "Parser $parserVersion reproduziu a geração ativa sem diferença material.",
        )
    }

    @Synchronized
    fun isParserProven(
        dataset: UpdateDataset,
        parserVersion: String,
    ): Boolean {
        val file = parserProofFile(dataset)
        if (!file.isFile) return false
        return runCatching {
            JSONObject(file.readText(Charsets.UTF_8))
                .optString("parserVersion") == parserVersion
        }.getOrDefault(false)
    }

    @Synchronized
    fun state(dataset: UpdateDataset): DatasetUpdateState =
        readState(dataset).toPublic(dataset)

    @Synchronized
    fun candidateJson(dataset: UpdateDataset): String? =
        readState(dataset).candidateVersion?.let { readGeneration(dataset, it) }

    @Synchronized
    fun activeJson(dataset: UpdateDataset): String? =
        readState(dataset).activeVersion?.let { readGeneration(dataset, it) }

    @Synchronized
    fun previousJson(dataset: UpdateDataset): String? =
        readState(dataset).previousVersion?.let { readGeneration(dataset, it) }

    private fun writeGeneration(
        dataset: UpdateDataset,
        version: String,
        json: String,
    ) {
        val target = generationFile(dataset, version)
        target.parentFile?.mkdirs()

        if (target.exists()) {
            require(target.readText(Charsets.UTF_8) == json) {
                "Geração imutável já existe com conteúdo diferente."
            }
            return
        }

        atomicWrite(target, json)
    }

    private fun readGeneration(
        dataset: UpdateDataset,
        version: String,
    ): String? =
        generationFile(dataset, version)
            .takeIf(File::isFile)
            ?.readText(Charsets.UTF_8)

    private fun writeManifest(
        dataset: UpdateDataset,
        version: String,
        manifest: GenerationManifest,
    ) {
        val target = manifestFile(dataset, version)
        target.parentFile?.mkdirs()
        val json = manifest.toJson()

        if (target.exists()) {
            require(target.readText(Charsets.UTF_8) == json) {
                "Manifesto imutável já existe com conteúdo diferente."
            }
            return
        }

        atomicWrite(target, json)
    }

    private fun generationFile(
        dataset: UpdateDataset,
        version: String,
    ): File {
        val fileKey = CanonicalJson.sha256Upper(version).lowercase()
        return File(datasetDir(dataset), "generations/$fileKey.json")
    }

    private fun manifestFile(
        dataset: UpdateDataset,
        version: String,
    ): File {
        val fileKey = CanonicalJson.sha256Upper(version).lowercase()
        return File(datasetDir(dataset), "manifests/$fileKey.json")
    }

    private fun cleanupUnreferenced(
        dataset: UpdateDataset,
        state: StoredState,
    ) {
        val retainedKeys = listOfNotNull(
            state.activeVersion,
            state.previousVersion,
            state.candidateVersion,
        ).mapTo(mutableSetOf()) { version ->
            CanonicalJson.sha256Upper(version).lowercase() + ".json"
        }

        listOf(
            File(datasetDir(dataset), "generations"),
            File(datasetDir(dataset), "manifests"),
        ).forEach { directory ->
            directory.listFiles()
                ?.filter { it.isFile && it.name !in retainedKeys }
                ?.forEach { it.delete() }
        }
    }

    private fun datasetDir(dataset: UpdateDataset): File =
        File(root, dataset.id).apply { mkdirs() }

    private fun stateFile(dataset: UpdateDataset): File =
        File(datasetDir(dataset), "state.json")

    private fun historyFile(dataset: UpdateDataset): File =
        File(datasetDir(dataset), "history.jsonl")

    private fun parserProofFile(dataset: UpdateDataset): File =
        File(datasetDir(dataset), "parser-proof.json")

    private fun readState(dataset: UpdateDataset): StoredState {
        val file = stateFile(dataset)
        if (!file.isFile) return StoredState()

        return runCatching {
            StoredState.fromJson(JSONObject(file.readText(Charsets.UTF_8)))
        }.getOrElse {
            StoredState(
                status = UpdateStatus.ERROR,
                message = "Estado local de atualização inválido.",
            )
        }
    }

    private fun writeState(
        dataset: UpdateDataset,
        state: StoredState,
    ) {
        atomicWrite(stateFile(dataset), state.toJson().toString(2) + "\n")
    }

    private fun atomicWrite(
        target: File,
        text: String,
    ) {
        target.parentFile?.mkdirs()
        val atomic = AtomicFile(target)
        val output = atomic.startWrite()
        try {
            output.write(text.toByteArray(Charsets.UTF_8))
            output.fd.sync()
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
    }

    private fun appendHistory(
        dataset: UpdateDataset,
        event: String,
        version: String?,
        message: String,
    ) {
        val payload = JSONObject()
            .put("event", event)
            .put("version", version ?: JSONObject.NULL)
            .put("message", message.take(1_000))
            .toString()

        historyFile(dataset).appendText(payload + "\n", Charsets.UTF_8)
    }

    private data class StoredState(
        val activeVersion: String? = null,
        val previousVersion: String? = null,
        val candidateVersion: String? = null,
        val lastCheckedAt: String? = null,
        val status: UpdateStatus = UpdateStatus.IDLE,
        val message: String = "",
        val diff: UpdateDiff? = null,
    ) {
        fun toPublic(dataset: UpdateDataset): DatasetUpdateState =
            DatasetUpdateState(
                dataset = dataset,
                status = status,
                activeVersion = activeVersion,
                previousVersion = previousVersion,
                candidateVersion = candidateVersion,
                lastCheckedAt = lastCheckedAt,
                message = message,
                diff = diff,
            )

        fun toJson(): JSONObject =
            JSONObject()
                .put("activeVersion", activeVersion ?: JSONObject.NULL)
                .put("previousVersion", previousVersion ?: JSONObject.NULL)
                .put("candidateVersion", candidateVersion ?: JSONObject.NULL)
                .put("lastCheckedAt", lastCheckedAt ?: JSONObject.NULL)
                .put("status", status.name)
                .put("message", message)
                .put(
                    "diff",
                    diff?.let {
                        JSONObject()
                            .put("activeCount", it.activeCount)
                            .put("candidateCount", it.candidateCount)
                            .put("added", it.added)
                            .put("changed", it.changed)
                            .put("removed", it.removed)
                            .put("brokenReferences", it.brokenReferences)
                    } ?: JSONObject.NULL,
                )

        companion object {
            fun fromJson(json: JSONObject): StoredState {
                val diff = json.optJSONObject("diff")?.let {
                    UpdateDiff(
                        activeCount = it.getInt("activeCount"),
                        candidateCount = it.getInt("candidateCount"),
                        added = it.getInt("added"),
                        changed = it.getInt("changed"),
                        removed = it.getInt("removed"),
                        brokenReferences = it.optInt("brokenReferences", 0),
                    )
                }

                val status = runCatching {
                    UpdateStatus.valueOf(json.optString("status", UpdateStatus.IDLE.name))
                }.getOrDefault(UpdateStatus.ERROR)

                return StoredState(
                    activeVersion = json.nullableString("activeVersion"),
                    previousVersion = json.nullableString("previousVersion"),
                    candidateVersion = json.nullableString("candidateVersion"),
                    lastCheckedAt = json.nullableString("lastCheckedAt"),
                    status = status,
                    message = json.optString("message"),
                    diff = diff,
                )
            }

            private fun JSONObject.nullableString(key: String): String? =
                if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)
        }
    }
}
