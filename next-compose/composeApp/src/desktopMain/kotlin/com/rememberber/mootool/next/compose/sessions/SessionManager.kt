package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import com.rememberber.mootool.next.compose.domain.JsonFormatOptions
import com.rememberber.mootool.next.compose.editor.EditorBuffer
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class JsonSessionSnapshot(
    val content: String,
    val wrap: Boolean = true,
    val inspectorOpen: Boolean = true,
    val findOpen: Boolean = false,
    val findQuery: String = "",
    val replaceText: String = "",
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false,
    val jsonPath: String = "$",
    val currentFile: String = "",
    val spaces: Int = 2,
    val sortKeys: Boolean = false,
    val ignoreCase: Boolean = false,
    val checkDuplicateKeys: Boolean = true,
    val vaultQuery: String = ""
)

class JsonSession {
    val editor = EditorBuffer(SAMPLE_JSON)
    var wrap: Boolean = true
    var inspectorOpen: Boolean = true
    var findOpen: Boolean = false
    var findQuery: String = ""
    var replaceText: String = ""
    var findOptions: FindReplaceOptions = FindReplaceOptions()
    var jsonPath: String = "$"
    var currentFile: String = ""
    var formatOptions: JsonFormatOptions = JsonFormatOptions()
    var vaultQuery: String = ""
    var notice: String = ""
    var pathResult: String = ""
    var dialogTitle: String = ""
    var dialogBody: String = ""
    var dialogInputMode: String = ""
    var dialogInput: String = ""
    var historyOpen: Boolean = false

    fun snapshot(): JsonSessionSnapshot = JsonSessionSnapshot(
        content = editor.text,
        wrap = wrap,
        inspectorOpen = inspectorOpen,
        findOpen = findOpen,
        findQuery = findQuery,
        replaceText = replaceText,
        matchCase = findOptions.matchCase,
        wholeWord = findOptions.wholeWord,
        regex = findOptions.regex,
        jsonPath = jsonPath,
        currentFile = currentFile,
        spaces = formatOptions.spaces,
        sortKeys = formatOptions.sortKeys,
        ignoreCase = formatOptions.ignoreCase,
        checkDuplicateKeys = formatOptions.checkDuplicateKeys,
        vaultQuery = vaultQuery
    )

    fun restore(snapshot: JsonSessionSnapshot) {
        editor.setText(snapshot.content, recordUndo = false)
        wrap = snapshot.wrap
        inspectorOpen = snapshot.inspectorOpen
        findOpen = snapshot.findOpen
        findQuery = snapshot.findQuery
        replaceText = snapshot.replaceText
        findOptions = FindReplaceOptions(snapshot.matchCase, snapshot.wholeWord, snapshot.regex)
        jsonPath = snapshot.jsonPath
        currentFile = snapshot.currentFile
        formatOptions = JsonFormatOptions(
            spaces = snapshot.spaces,
            sortKeys = snapshot.sortKeys,
            ignoreCase = snapshot.ignoreCase,
            checkDuplicateKeys = snapshot.checkDuplicateKeys
        )
        vaultQuery = snapshot.vaultQuery
    }

    companion object {
        const val SAMPLE_JSON = """{
  "name": "MooTool Next Compose",
  "stack": ["Kotlin", "Compose Desktop", "Jackson"],
  "desktop": {
    "style": "modern workspace",
    "theme": "system"
  }
}"""
    }
}

enum class WindowRole { Main, Detached }

data class HostedSession(
    val toolId: ToolId,
    val role: WindowRole
)

class SessionManager(private val store: SessionStore) {
    private val jsonCodec = Json { ignoreUnknownKeys = true }
    private val sessions = HashMap<ToolId, JsonSession>()
    private val _detached = MutableStateFlow<Set<ToolId>>(emptySet())
    val detached: StateFlow<Set<ToolId>> = _detached
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision

    fun jsonSession(): JsonSession = sessions.getOrPut(ToolId.Json) {
        JsonSession().also { session ->
            store.load(ToolId.Json.id)?.let { raw ->
                runCatching { jsonCodec.decodeFromString<JsonSessionSnapshot>(raw) }
                    .getOrNull()
                    ?.let(session::restore)
            }
        }
    }

    fun persistJson() {
        store.save(ToolId.Json.id, jsonCodec.encodeToString(jsonSession().snapshot()))
    }

    fun detach(toolId: ToolId) {
        if (!toolId.detachable) return
        _detached.value = _detached.value + toolId
        bump()
    }

    fun reattach(toolId: ToolId) {
        _detached.value = _detached.value - toolId
        bump()
    }

    fun isDetached(toolId: ToolId): Boolean = toolId in _detached.value

    fun bump() {
        _revision.value += 1
    }
}
