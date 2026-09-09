package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.TranslationEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class TranslationWord(
    val id: String,
    val sourceText: String,
    val targetText: String,
    val sourceLang: String,
    val targetLang: String,
    val remark: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

@Serializable
data class TranslationHistoryItem(
    val id: String,
    val sourceText: String,
    val targetText: String,
    val sourceLang: String,
    val targetLang: String,
    val translatorType: String,
    val createdAt: Long = System.currentTimeMillis()
)

class TranslationStore(
    private val directories: AppDirectories,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val wordsFile get() = directories.dataRoot.resolve("translation").resolve("words.json")
    private val historyFile get() = directories.dataRoot.resolve("translation").resolve("history.json")
    private val lock = Any()

    fun listWords(keyword: String = ""): List<TranslationWord> {
        val query = keyword.trim()
        return loadWords()
            .filter { word ->
                query.isEmpty() ||
                    word.sourceText.contains(query, ignoreCase = true) ||
                    word.targetText.contains(query, ignoreCase = true) ||
                    word.remark.contains(query, ignoreCase = true)
            }
            .sortedWith(compareByDescending<TranslationWord> { it.modifiedAt }.thenByDescending { it.id })
    }

    fun saveWord(
        id: String?,
        sourceText: String,
        targetText: String,
        sourceLang: String,
        targetLang: String,
        remark: String
    ): TranslationWord {
        val source = sourceText.trim()
        require(source.isNotEmpty()) { "Translation word source is blank" }
        val languages = TranslationEngine.normalizeLanguagePair(sourceLang, targetLang)
        val now = System.currentTimeMillis()
        return synchronized(lock) {
            val items = loadWords().toMutableList()
            val existingIndex = when {
                !id.isNullOrBlank() -> items.indexOfFirst { it.id == id }
                else -> items.indexOfFirst {
                    it.sourceText == source && it.sourceLang == languages.first && it.targetLang == languages.second
                }
            }
            val word = if (existingIndex >= 0) {
                items[existingIndex].copy(
                    sourceText = source,
                    targetText = targetText,
                    sourceLang = languages.first,
                    targetLang = languages.second,
                    remark = remark,
                    modifiedAt = now
                )
            } else {
                TranslationWord(
                    id = UUID.randomUUID().toString(),
                    sourceText = source,
                    targetText = targetText,
                    sourceLang = languages.first,
                    targetLang = languages.second,
                    remark = remark,
                    createdAt = now,
                    modifiedAt = now
                )
            }
            if (existingIndex >= 0) items[existingIndex] = word else items.add(0, word)
            persistWords(items)
            word
        }
    }

    fun deleteWord(id: String) {
        synchronized(lock) {
            persistWords(loadWords().filterNot { it.id == id })
        }
    }

    fun listHistory(keyword: String = ""): List<TranslationHistoryItem> {
        val query = keyword.trim()
        return loadHistory()
            .filter { item ->
                query.isEmpty() ||
                    item.sourceText.contains(query, ignoreCase = true) ||
                    item.targetText.contains(query, ignoreCase = true) ||
                    item.sourceLang.contains(query, ignoreCase = true) ||
                    item.targetLang.contains(query, ignoreCase = true)
            }
            .sortedWith(compareByDescending<TranslationHistoryItem> { it.createdAt }.thenByDescending { it.id })
            .take(TranslationEngine.HISTORY_LIMIT)
    }

    fun saveHistory(
        sourceText: String,
        targetText: String,
        sourceLang: String,
        targetLang: String,
        translatorType: String
    ): TranslationHistoryItem {
        val languages = TranslationEngine.normalizeLanguagePair(sourceLang, targetLang)
        val item = TranslationHistoryItem(
            id = UUID.randomUUID().toString(),
            sourceText = sourceText,
            targetText = targetText,
            sourceLang = languages.first,
            targetLang = languages.second,
            translatorType = translatorType.ifBlank { "google" },
            createdAt = System.currentTimeMillis()
        )
        synchronized(lock) {
            persistHistory((listOf(item) + loadHistory()).take(TranslationEngine.HISTORY_LIMIT))
        }
        return item
    }

    fun deleteHistory(id: String) {
        synchronized(lock) {
            persistHistory(loadHistory().filterNot { it.id == id })
        }
    }

    fun clearHistory() {
        synchronized(lock) { persistHistory(emptyList()) }
    }

    private fun loadWords(): List<TranslationWord> {
        if (!wordsFile.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<TranslationWord>>(wordsFile.readText()) }.getOrDefault(emptyList())
    }

    private fun loadHistory(): List<TranslationHistoryItem> {
        if (!historyFile.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<TranslationHistoryItem>>(historyFile.readText()) }.getOrDefault(emptyList())
    }

    private fun persistWords(items: List<TranslationWord>) {
        SettingsRepository.atomicWrite(wordsFile, json.encodeToString(items))
    }

    private fun persistHistory(items: List<TranslationHistoryItem>) {
        SettingsRepository.atomicWrite(historyFile, json.encodeToString(items))
    }
}
