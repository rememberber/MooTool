package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class ColorFavoriteFolder(
    val id: String,
    val title: String
)

@Serializable
data class ColorFavoriteItem(
    val id: String,
    val folderId: String,
    val name: String,
    val value: String
)

@Serializable
private data class ColorFavoriteFile(
    val folders: List<ColorFavoriteFolder> = emptyList(),
    val items: List<ColorFavoriteItem> = emptyList()
)

class ColorFavoriteStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val file get() = directories.dataRoot.resolve("favorites").resolve("color.json")

    fun folders(): List<ColorFavoriteFolder> = load().folders

    fun items(folderId: String): List<ColorFavoriteItem> = load().items.filter { it.folderId == folderId }

    fun ensureDefaultFolder(title: String): ColorFavoriteFolder {
        val current = load()
        current.folders.firstOrNull()?.let { return it }
        val folder = ColorFavoriteFolder(id = UUID.randomUUID().toString(), title = title.trim().ifBlank { "Default" })
        save(ColorFavoriteFile(folders = listOf(folder), items = emptyList()))
        return folder
    }

    fun addFolder(title: String): ColorFavoriteFolder {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("empty")
        val current = load()
        if (current.folders.any { it.title == trimmed }) throw IllegalArgumentException("duplicate")
        val folder = ColorFavoriteFolder(id = UUID.randomUUID().toString(), title = trimmed)
        save(current.copy(folders = current.folders + folder))
        return folder
    }

    fun renameFolder(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val current = load()
        if (current.folders.any { it.title == trimmed && it.id != id }) throw IllegalArgumentException("duplicate")
        save(current.copy(folders = current.folders.map { if (it.id == id) it.copy(title = trimmed) else it }))
    }

    fun deleteFolder(id: String) {
        val current = load()
        save(ColorFavoriteFile(folders = current.folders.filterNot { it.id == id }, items = current.items.filterNot { it.folderId == id }))
    }

    fun addItem(folderId: String, name: String, value: String): ColorFavoriteItem {
        val current = load()
        val item = ColorFavoriteItem(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            name = name.trim().ifBlank { value },
            value = value
        )
        save(current.copy(items = current.items + item))
        return item
    }

    fun deleteItem(id: String) {
        val current = load()
        save(current.copy(items = current.items.filterNot { it.id == id }))
    }

    private fun load(): ColorFavoriteFile {
        if (!file.exists()) return ColorFavoriteFile()
        return runCatching { json.decodeFromString<ColorFavoriteFile>(file.readText()) }.getOrDefault(ColorFavoriteFile())
    }

    private fun save(data: ColorFavoriteFile) {
        SettingsRepository.atomicWrite(file, json.encodeToString(data))
    }
}
