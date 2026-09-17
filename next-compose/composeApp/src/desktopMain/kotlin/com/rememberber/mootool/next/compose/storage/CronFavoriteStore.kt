package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.FavoritePresentation
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
        return all.filter { item ->
            FavoritePresentation.matchesQuery(query, group, item.name, item.expression, item.group)
        }
    }

    fun add(name: String, expression: String, group: String = ""): CronFavorite {
        val trimmedName = FavoritePresentation.defaultName(name, expression)
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
