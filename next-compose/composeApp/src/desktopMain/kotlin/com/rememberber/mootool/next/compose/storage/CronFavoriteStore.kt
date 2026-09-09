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
    val expression: String
)

class CronFavoriteStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("favorites").resolve("cron.json")

    fun list(): List<CronFavorite> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<CronFavorite>>(file.readText()) }.getOrDefault(emptyList())
    }

    fun add(name: String, expression: String): CronFavorite {
        val trimmedName = name.trim().ifBlank { expression.take(32) }
        val favorite = CronFavorite(id = UUID.randomUUID().toString(), name = trimmedName, expression = expression)
        save(list() + favorite)
        return favorite
    }

    fun delete(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(items: List<CronFavorite>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
