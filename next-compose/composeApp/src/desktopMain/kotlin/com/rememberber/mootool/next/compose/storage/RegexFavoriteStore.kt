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
    val pattern: String
)

class RegexFavoriteStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("favorites").resolve("regex.json")

    fun list(): List<RegexFavorite> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<RegexFavorite>>(file.readText()) }.getOrDefault(emptyList())
    }

    fun add(name: String, pattern: String): RegexFavorite {
        val trimmedName = name.trim().ifBlank { pattern.take(32) }
        val favorite = RegexFavorite(id = UUID.randomUUID().toString(), name = trimmedName, pattern = pattern)
        save(list() + favorite)
        return favorite
    }

    fun delete(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(items: List<RegexFavorite>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
