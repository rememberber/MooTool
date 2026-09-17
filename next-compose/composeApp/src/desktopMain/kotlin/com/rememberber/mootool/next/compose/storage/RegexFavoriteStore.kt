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
data class RegexFavorite(
    val id: String,
    val name: String,
    val pattern: String,
    val group: String = ""
)

class RegexFavoriteStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("favorites").resolve("regex.json")

    fun list(query: String = "", group: String = ""): List<RegexFavorite> {
        if (!file.exists()) return emptyList()
        val all = runCatching { json.decodeFromString<List<RegexFavorite>>(file.readText()) }.getOrDefault(emptyList())
        return all.filter { item ->
            FavoritePresentation.matchesQuery(query, group, item.name, item.pattern, item.group)
        }
    }

    fun add(name: String, pattern: String, group: String = ""): RegexFavorite {
        val trimmedName = FavoritePresentation.defaultName(name, pattern)
        val favorite = RegexFavorite(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            pattern = pattern,
            group = group.trim()
        )
        save(loadAll() + favorite)
        return favorite
    }

    private fun loadAll(): List<RegexFavorite> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<RegexFavorite>>(file.readText()) }.getOrDefault(emptyList())
    }

    fun delete(id: String) {
        save(loadAll().filterNot { it.id == id })
    }

    fun mergeImport(items: List<RegexFavorite>): Int {
        if (items.isEmpty()) return 0
        val existing = loadAll()
        val seen = existing.map { it.pattern.trim() to it.name.trim() }.toSet()
        val newItems = items.filter { item ->
            (item.pattern.trim() to item.name.trim()) !in seen
        }
        if (newItems.isEmpty()) return 0
        save(existing + newItems)
        return newItems.size
    }

    private fun save(items: List<RegexFavorite>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
