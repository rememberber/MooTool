package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
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
        val needle = query.trim()
        val folder = group.trim()
        return all.filter { item ->
            (folder.isEmpty() || item.group.equals(folder, ignoreCase = true)) &&
                (needle.isEmpty() || item.name.contains(needle, ignoreCase = true) ||
                    item.pattern.contains(needle, ignoreCase = true) || item.group.contains(needle, ignoreCase = true))
        }
    }

    fun add(name: String, pattern: String, group: String = ""): RegexFavorite {
        val trimmedName = name.trim().ifBlank { pattern.take(32) }
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

    private fun save(items: List<RegexFavorite>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
