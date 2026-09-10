package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.HostEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class HostProfile(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

class HostProfileStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("hosts").resolve("profiles.json")

    fun list(keyword: String = "", includeContent: Boolean = true): List<HostProfile> {
        val query = keyword.trim()
        return load()
            .filter { profile ->
                if (query.isEmpty()) true
                else profile.name.contains(query, ignoreCase = true) ||
                    (includeContent && profile.content.contains(query, ignoreCase = true))
            }
            .sortedByDescending { it.modifiedAt }
            .map { if (includeContent) it else it.copy(content = "") }
    }

    fun get(id: String): HostProfile? = load().firstOrNull { it.id == id }

    fun save(id: String?, name: String, content: String): HostProfile {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Host profile name is blank" }
        val items = load().toMutableList()
        val now = System.currentTimeMillis()
        val existing = id?.let { current -> items.indexOfFirst { it.id == current } } ?: -1
        val profile = if (existing >= 0) {
            items[existing].copy(name = trimmed, content = content, modifiedAt = now)
        } else {
            HostProfile(id = UUID.randomUUID().toString(), name = trimmed, content = content, createdAt = now, modifiedAt = now)
        }
        if (existing >= 0) items[existing] = profile else items.add(0, profile)
        persist(items)
        return profile
    }

    fun duplicate(id: String, suffix: String = " copy"): HostProfile {
        val source = get(id) ?: throw IllegalArgumentException("Host profile not found: $id")
        return save(null, HostEngine.copyName(source.name, suffix), source.content)
    }

    fun delete(id: String) {
        persist(load().filterNot { it.id == id })
    }

    private fun load(): List<HostProfile> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<HostProfile>>(file.readText()) }.getOrDefault(emptyList())
    }

    private fun persist(items: List<HostProfile>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
