package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.HttpRequestDraft
import com.rememberber.mootool.next.compose.domain.HttpResponseResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class SavedHttpRequest(
    val id: String,
    val draft: HttpRequestDraft,
    val responseBody: String = "",
    val responseHeaders: String = "",
    val responseCookies: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

class HttpCollectionStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("http").resolve("requests.json")

    fun list(keyword: String = ""): List<SavedHttpRequest> {
        val query = keyword.trim()
        return load()
            .filter { item ->
                if (query.isEmpty()) true
                else item.draft.name.contains(query, ignoreCase = true) ||
                    item.draft.url.contains(query, ignoreCase = true) ||
                    item.draft.method.name.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.modifiedAt }
    }

    fun get(id: String): SavedHttpRequest? = load().firstOrNull { it.id == id }

    fun save(draft: HttpRequestDraft, response: HttpResponseResult? = null): SavedHttpRequest {
        val name = draft.name.trim().ifBlank { "Untitled" }
        val items = load().toMutableList()
        val now = System.currentTimeMillis()
        val existing = draft.id.takeIf { it.isNotBlank() }?.let { id -> items.indexOfFirst { it.id == id } } ?: -1
        val saved = if (existing >= 0) {
            val previous = items[existing]
            previous.copy(
                draft = draft.copy(id = previous.id, name = name),
                responseBody = response?.body ?: previous.responseBody,
                responseHeaders = response?.headers ?: previous.responseHeaders,
                responseCookies = response?.cookies ?: previous.responseCookies,
                modifiedAt = now
            )
        } else {
            val id = UUID.randomUUID().toString()
            SavedHttpRequest(
                id = id,
                draft = draft.copy(id = id, name = name),
                responseBody = response?.body.orEmpty(),
                responseHeaders = response?.headers.orEmpty(),
                responseCookies = response?.cookies.orEmpty(),
                createdAt = now,
                modifiedAt = now
            )
        }
        if (existing >= 0) items[existing] = saved else items.add(0, saved)
        persist(items)
        return saved
    }

    fun delete(id: String) {
        persist(load().filterNot { it.id == id })
    }

    private fun load(): List<SavedHttpRequest> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<SavedHttpRequest>>(file.readText()) }.getOrDefault(emptyList())
    }

    private fun persist(items: List<SavedHttpRequest>) {
        SettingsRepository.atomicWrite(file, json.encodeToString(items))
    }
}
