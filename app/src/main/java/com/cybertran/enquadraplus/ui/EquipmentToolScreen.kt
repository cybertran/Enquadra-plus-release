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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private data class EquipmentVehicle(
    val id: String,
    val label: String,
    val table: Int,
)

private data class EquipmentCell(
    val required: Boolean,
    val noteRefs: List<Int>,
)

private data class EquipmentRequirement(
    val id: String,
    val label: String,
    val reference: String,
    val cells: Map<String, EquipmentCell>,
)

private data class EquipmentCatalog(
    val sourceUrl: String,
    val sourcePageCount: Int,
    val explicitCellCount: Int,
    val requiredCellCount: Int,
    val vehicles: List<EquipmentVehicle>,
    val notes: Map<Int, String>,
    val requirements: List<EquipmentRequirement>,
)

@Composable
fun EquipmentToolScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val catalogResult = remember {
        runCatching {
            context.assets.open("tools/equipment_res993.json").bufferedReader().use { reader ->
                parseEquipmentCatalog(reader.readText())
            }
        }
    }
    val catalog = catalogResult.getOrNull()

    if (catalog == null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    text = "Não foi possível carregar a matriz oficial de equipamentos.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        return
    }

    var selectedId by rememberSaveable { mutableStateOf(catalog.vehicles.first().id) }
    val checked = remember { mutableStateListOf<String>() }
    val selected = catalog.vehicles.firstOrNull { it.id == selectedId } ?: catalog.vehicles.first()
    val applicable = catalog.requirements.mapNotNull { requirement ->
        val cell = requirement.cells[selected.id] ?: return@mapNotNull null
        if (cell.required) requirement to cell else null
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
                text = "Equipamentos obrigatórios",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Text(
                text = "Selecione o tipo de veículo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SearchableSingleChoiceSelector(
                label = "Tipo de veículo",
                options = catalog.vehicles.map { it.id to it.label },
                selectedId = selected.id,
                onSelect = { vehicleId ->
                    selectedId = vehicleId
                    checked.clear()
                },
            )
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
                        text = selected.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = applicable.size.toString() + " requisito(s) aplicável(is)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Conferidos: " + checked.count { id ->
                            applicable.any { it.first.id == id }
                        } + "/" + applicable.size,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(
            items = applicable,
            key = { it.first.id },
        ) { (requirement, cell) ->
            val isChecked = requirement.id in checked
            ChecklistRow(
                title = requirement.label,
                checked = isChecked,
                onClick = {
                    if (isChecked) checked.remove(requirement.id)
                    else checked.add(requirement.id)
                },
                supportingLines = buildList {
                    if (requirement.reference.isNotBlank()) {
                        add(requirement.reference)
                    }
                    cell.noteRefs.forEach { noteRef ->
                        catalog.notes[noteRef]?.let { note ->
                            add("Nota " + noteRef + ": " + note)
                        }
                    }
                },
            )
        }

        item {
            Text(
                text = "Ver fonte",
                modifier = Modifier.clickable {
                    uriHandler.openUri(catalog.sourceUrl)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun parseEquipmentCatalog(raw: String): EquipmentCatalog {
    val root = JSONObject(raw)
    require(root.getInt("schemaVersion") == 1)
    require(root.getInt("sourcePageCount") == 53)

    val annex = root.getJSONObject("annexI")
    val explicitCellCount = annex.getInt("explicitCellCount")
    val requiredCellCount = annex.getInt("requiredCellCount")

    val vehiclesJson = annex.getJSONArray("vehicleTypes")
    val vehicles = buildList {
        repeat(vehiclesJson.length()) { index ->
            val item = vehiclesJson.getJSONObject(index)
            add(
                EquipmentVehicle(
                    id = item.getString("id"),
                    label = item.getString("label"),
                    table = item.getInt("table"),
                ),
            )
        }
    }

    val notesJson = annex.getJSONObject("notes")
    val notes = buildMap {
        notesJson.keys().forEach { key ->
            put(key.toInt(), notesJson.getString(key))
        }
    }

    val requirementsJson = annex.getJSONArray("requirements")
    val requirements = buildList {
        repeat(requirementsJson.length()) { index ->
            val item = requirementsJson.getJSONObject(index)
            val cellsJson = item.getJSONObject("cells")
            val cells = buildMap {
                vehicles.forEach { vehicle ->
                    put(vehicle.id, parseEquipmentCell(cellsJson.getString(vehicle.id)))
                }
            }
            add(
                EquipmentRequirement(
                    id = item.getString("id"),
                    label = item.getString("label"),
                    reference = item.optString("reference"),
                    cells = cells,
                ),
            )
        }
    }

    require(vehicles.size == 16)
    require(requirements.size == 53)
    require(notes.keys == (1..49).toSet())
    require(explicitCellCount == requirements.size * vehicles.size)
    require(
        requiredCellCount == requirements.sumOf { requirement ->
            requirement.cells.values.count { it.required }
        },
    )
    require(
        requirements.all { requirement ->
            requirement.cells.keys == vehicles.map { it.id }.toSet()
        },
    )
    require(
        requirements
            .flatMap { it.cells.values }
            .flatMap { it.noteRefs }
            .all { it in notes },
    )

    return EquipmentCatalog(
        sourceUrl = root.getString("sourceUrl"),
        sourcePageCount = root.getInt("sourcePageCount"),
        explicitCellCount = explicitCellCount,
        requiredCellCount = requiredCellCount,
        vehicles = vehicles,
        notes = notes,
        requirements = requirements,
    )
}

private fun parseEquipmentCell(value: String): EquipmentCell {
    if (value == "-") return EquipmentCell(required = false, noteRefs = emptyList())
    require(value == "X" || value.startsWith("X:"))

    val notes = value
        .substringAfter(':', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
        ?.split(',')
        ?.map { it.toInt() }
        .orEmpty()

    return EquipmentCell(required = true, noteRefs = notes)
}
