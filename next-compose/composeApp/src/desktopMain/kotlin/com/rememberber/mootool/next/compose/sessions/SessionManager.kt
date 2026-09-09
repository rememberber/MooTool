package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import com.rememberber.mootool.next.compose.domain.JsonFormatOptions
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.domain.TimestampUnit
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

@Serializable
data class TimeSessionSnapshot(
    val timestamp: String,
    val localTime: String,
    val unit: String = "second",
    val zone: String
)

class TimeSession {
    var timestamp: String
    var localTime: String
    var unit: TimestampUnit
    var zone: String
    var historyOpen: Boolean = false
    var clockOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    init {
        val now = System.currentTimeMillis()
        zone = TimeEngine.systemZone()
        unit = TimestampUnit.Second
        timestamp = (now / 1000).toString()
        localTime = TimeEngine.formatLocalTime(now, zone)
    }

    fun snapshot(): TimeSessionSnapshot = TimeSessionSnapshot(
        timestamp = timestamp,
        localTime = localTime,
        unit = if (unit == TimestampUnit.Millisecond) "millisecond" else "second",
        zone = zone
    )

    fun restore(snapshot: TimeSessionSnapshot) {
        timestamp = snapshot.timestamp
        localTime = snapshot.localTime
        unit = if (snapshot.unit == "millisecond") TimestampUnit.Millisecond else TimestampUnit.Second
        zone = snapshot.zone.ifBlank { TimeEngine.systemZone() }
        historyOpen = false
        clockOpen = false
        notice = ""
        error = ""
    }
}

@Serializable
data class CalculatorSessionSnapshot(
    val expression: String = "2 * (3 + 4)",
    val result: String = "14",
    val decimal: String = "255",
    val hex: String = "ff",
    val binary: String = "11111111",
    val gcdFirst: String = "54",
    val gcdSecond: String = "24",
    val lcmFirst: String = "54",
    val lcmSecond: String = "24",
    val permutationN: String = "5",
    val permutationM: String = "2",
    val combinationN: String = "5",
    val combinationM: String = "2",
    val log: List<String> = listOf("2 * (3 + 4) = 14")
)

class CalculatorSession {
    var expression: String = "2 * (3 + 4)"
    var result: String = "14"
    var decimal: String = "255"
    var hex: String = "ff"
    var binary: String = "11111111"
    var gcdFirst: String = "54"
    var gcdSecond: String = "24"
    var lcmFirst: String = "54"
    var lcmSecond: String = "24"
    var permutationN: String = "5"
    var permutationM: String = "2"
    var combinationN: String = "5"
    var combinationM: String = "2"
    var log: List<String> = listOf("2 * (3 + 4) = 14")
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): CalculatorSessionSnapshot = CalculatorSessionSnapshot(
        expression, result, decimal, hex, binary, gcdFirst, gcdSecond, lcmFirst, lcmSecond,
        permutationN, permutationM, combinationN, combinationM, log
    )

    fun restore(snapshot: CalculatorSessionSnapshot) {
        expression = snapshot.expression
        result = snapshot.result
        decimal = snapshot.decimal
        hex = snapshot.hex
        binary = snapshot.binary
        gcdFirst = snapshot.gcdFirst
        gcdSecond = snapshot.gcdSecond
        lcmFirst = snapshot.lcmFirst
        lcmSecond = snapshot.lcmSecond
        permutationN = snapshot.permutationN
        permutationM = snapshot.permutationM
        combinationN = snapshot.combinationN
        combinationM = snapshot.combinationM
        log = snapshot.log
        historyOpen = false
        notice = ""
        error = ""
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
    private var timeSessionCache: TimeSession? = null
    private var calculatorSessionCache: CalculatorSession? = null
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

    fun timeSession(): TimeSession {
        timeSessionCache?.let { return it }
        val session = TimeSession()
        store.load(ToolId.TimeConvert.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<TimeSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        timeSessionCache = session
        return session
    }

    fun persistTime() {
        store.save(ToolId.TimeConvert.id, jsonCodec.encodeToString(timeSession().snapshot()))
    }

    fun calculatorSession(): CalculatorSession {
        calculatorSessionCache?.let { return it }
        val session = CalculatorSession()
        store.load(ToolId.Calculator.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<CalculatorSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        calculatorSessionCache = session
        return session
    }

    fun persistCalculator() {
        store.save(ToolId.Calculator.id, jsonCodec.encodeToString(calculatorSession().snapshot()))
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
