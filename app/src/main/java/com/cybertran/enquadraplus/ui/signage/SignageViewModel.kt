package com.cybertran.enquadraplus.ui.signage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cybertran.enquadraplus.ui.validateOfficialPdfUrl
import java.text.Normalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import org.json.JSONArray
import org.json.JSONObject

data class SignageManualItem(
    val volume: String,
    val title: String,
    val officialUrl: String,
    val sourceSha256: String,
    val pageCount: Int,
    val entryCount: Int,
)

data class SignageCatalogItem(
    val id: String,
    val volume: String,
    val volumeTitle: String,
    val kind: String,
    val code: String?,
    val title: String,
    val pageIndex: Int,
    val manualPage: Int?,
    val evidence: String,
    val officialUrl: String,
)

data class SignageCatalog(
    val version: String,
    val manuals: List<SignageManualItem>,
    val entries: List<SignageCatalogItem>,
)

class SignageViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val catalog = loadCatalog(application)

    val query = MutableStateFlow("")
    val selectedVolume = MutableStateFlow(ALL_VOLUMES)

    val manuals: List<SignageManualItem> = catalog.manuals

    val results: StateFlow<List<SignageCatalogItem>> =
        combine(query, selectedVolume) { rawQuery, volume ->
            val normalized = rawQuery.normalizeSearch()
            catalog.entries.filter { item ->
                (volume == ALL_VOLUMES || item.volume == volume) &&
                    (
                        normalized.isBlank() ||
                            item.code.orEmpty().normalizeSearch().contains(normalized) ||
                            item.title.normalizeSearch().contains(normalized) ||
                            item.volumeTitle.normalizeSearch().contains(normalized)
                        )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = catalog.entries,
        )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun selectVolume(value: String) {
        selectedVolume.value = value
    }

    private fun loadCatalog(application: Application): SignageCatalog =
        runCatching {
            application.assets
                .open(ASSET_PATH)
                .bufferedReader()
                .use { SignageCatalogParser.parse(it.readText()) }
        }.getOrElse {
            SignageCatalog(
                version = "unavailable",
                manuals = emptyList(),
                entries = emptyList(),
            )
        }

    companion object {
        const val ALL_VOLUMES = "ALL"
        private const val ASSET_PATH = "signage/catalog.json"
    }
}

object SignageCatalogParser {

    fun parse(json: String): SignageCatalog {
        val root = JSONObject(json)
        val manuals = root.getJSONArray("volumes").objects().map { item ->
            val url = item.getString("officialUrl")
            validateOfficialPdfUrl(url)
            SignageManualItem(
                volume = item.getString("volume"),
                title = item.getString("title"),
                officialUrl = url,
                sourceSha256 = item.getString("sourceSha256"),
                pageCount = item.getInt("pageCount"),
                entryCount = item.getInt("entryCount"),
            )
        }
        val entries = root.getJSONArray("records").objects().map { item ->
            val url = item.getString("officialUrl")
            validateOfficialPdfUrl(url)
            SignageCatalogItem(
                id = item.getString("id"),
                volume = item.getString("volume"),
                volumeTitle = item.getString("volumeTitle"),
                kind = item.getString("kind"),
                code = item.nullableString("code"),
                title = item.getString("title"),
                pageIndex = item.getInt("pageIndex"),
                manualPage =
                    if (item.isNull("manualPage")) null else item.getInt("manualPage"),
                evidence = item.getString("evidence"),
                officialUrl = url,
            )
        }

        require(root.getInt("volumeCount") == manuals.size) {
            "Catálogo de sinalização com contagem de volumes divergente."
        }
        require(manuals.size == 9) {
            "Catálogo de sinalização deve conter os nove volumes oficiais."
        }
        require(root.getInt("entryCount") == entries.size) {
            "Catálogo de sinalização com contagem de itens divergente."
        }
        require(entries.map { it.id }.distinct().size == entries.size) {
            "Catálogo de sinalização contém IDs duplicados."
        }
        require(entries.all { it.pageIndex >= 0 }) {
            "Catálogo de sinalização contém página inválida."
        }

        return SignageCatalog(
            version = root.getString("version"),
            manuals = manuals,
            entries = entries,
        )
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeIf(String::isNotBlank)

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map(::getJSONObject)
}

private fun String.normalizeSearch(): String =
    Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .trim()
