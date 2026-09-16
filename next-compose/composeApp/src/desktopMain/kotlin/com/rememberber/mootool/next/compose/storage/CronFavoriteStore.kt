package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class CronFavorite(
    val id: String,
    val name: String,
    val expression: String,
    val group: String = ""
)

class CronFavoriteStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("favorites").resolve("cron.json")

    fun list(query: String = "", group: String = ""): List<CronFavorite> {
        if (!file.exists()) return emptyList()
        val all = runCatching { json.decodeFromString<List<CronFavorite>>(file.readText()) }.getOrDefault(emptyList())
        val needle = query.trim()
        val folder = group.trim()
        return all.filter { item ->
            (folder.isEmpty() || item.group.equals(folder, ignoreCase = true)) &&
                (needle.isEmpty() || item.name.contains(needle, ignoreCase = true) ||
                    item.expression.contains(needle, ignoreCase = true) || item.group.contains(needle, ignoreCase = true))
        }
    }

    fun add(name: String, expression: String, group: String = ""): CronFavorite {
        val trimmedName = name.trim().ifBlank { expression.take(32) }
        val favorite = CronFavorite(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            expression = expression,
            group = group.trim()
        )
        save(loadAll() + favorite)
        return favorite
    }

    private fun loadAll(): List<CronFavorite> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<CronFavorite>>(file.readText()) }.getOrDefault(emptyList())
    }

    fun delete(id: String) {
        save(loadAll().filterNot { it.id == id })
    }

    fun mergeImport(items: List<CronFavorite>): Int {
        if (items.isEmpty()) return 0
        val existing = loadAll()
        val seen = existing.map { it.expression.trim() to it.name.trim() }.toSet()
        val newItems = items.filter { item ->
            (item.expression.trim() to item.name.trim()) !in seen
        }
        if (newItems.isEmpty()) return 0
        save(existing + newItems)
        return newItems.size
    }

    private fun save(items: List<CronFavorite>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
