package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.AsciiFormat
import com.rememberber.mootool.next.compose.domain.EncodeTab
import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import com.rememberber.mootool.next.compose.domain.JsonFormatOptions
import com.rememberber.mootool.next.compose.domain.DiffEngine
import com.rememberber.mootool.next.compose.domain.DiffResult
import com.rememberber.mootool.next.compose.domain.DiffSegment
import com.rememberber.mootool.next.compose.domain.ReformatEngine
import com.rememberber.mootool.next.compose.domain.ReformatType
import com.rememberber.mootool.next.compose.domain.ConfigEngine
import com.rememberber.mootool.next.compose.domain.CronFields
import com.rememberber.mootool.next.compose.domain.CronEngine
import com.rememberber.mootool.next.compose.domain.RegexMatch
import com.rememberber.mootool.next.compose.domain.RegexOptions
import com.rememberber.mootool.next.compose.domain.TimeEngine
import com.rememberber.mootool.next.compose.domain.TimestampUnit
import com.rememberber.mootool.next.compose.domain.UaEngine
import com.rememberber.mootool.next.compose.domain.UaResult
import com.rememberber.mootool.next.compose.domain.UrlCharset
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

@Serializable
data class EncodeSessionSnapshot(
    val tab: String = "unicode",
    val unicodeLeft: String = "MooTool 编码转换",
    val unicodeRight: String = "",
    val urlLeft: String = "https://mootool.app/search?q=编码",
    val urlRight: String = "",
    val hexLeft: String = "MooTool",
    val hexRight: String = "",
    val asciiLeft: String = "MooTool",
    val asciiRight: String = "",
    val charset: String = "utf-8",
    val asciiFormat: String = "decimal"
)

class EncodeSession {
    var tab: EncodeTab = EncodeTab.Unicode
    var unicodeLeft: String = "MooTool 编码转换"
    var unicodeRight: String = ""
    var urlLeft: String = "https://mootool.app/search?q=编码"
    var urlRight: String = ""
    var hexLeft: String = "MooTool"
    var hexRight: String = ""
    var asciiLeft: String = "MooTool"
    var asciiRight: String = ""
    var charset: UrlCharset = UrlCharset.Utf8
    var asciiFormat: AsciiFormat = AsciiFormat.Decimal
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun left(): String = when (tab) {
        EncodeTab.Unicode -> unicodeLeft
        EncodeTab.Url -> urlLeft
        EncodeTab.Hex -> hexLeft
        EncodeTab.Ascii -> asciiLeft
    }

    fun right(): String = when (tab) {
        EncodeTab.Unicode -> unicodeRight
        EncodeTab.Url -> urlRight
        EncodeTab.Hex -> hexRight
        EncodeTab.Ascii -> asciiRight
    }

    fun setLeft(value: String) {
        when (tab) {
            EncodeTab.Unicode -> unicodeLeft = value
            EncodeTab.Url -> urlLeft = value
            EncodeTab.Hex -> hexLeft = value
            EncodeTab.Ascii -> asciiLeft = value
        }
    }

    fun setRight(value: String) {
        when (tab) {
            EncodeTab.Unicode -> unicodeRight = value
            EncodeTab.Url -> urlRight = value
            EncodeTab.Hex -> hexRight = value
            EncodeTab.Ascii -> asciiRight = value
        }
    }

    fun clearCurrent() {
        setLeft("")
        setRight("")
        error = ""
    }

    fun snapshot(): EncodeSessionSnapshot = EncodeSessionSnapshot(
        tab = tab.name.lowercase(),
        unicodeLeft = unicodeLeft,
        unicodeRight = unicodeRight,
        urlLeft = urlLeft,
        urlRight = urlRight,
        hexLeft = hexLeft,
        hexRight = hexRight,
        asciiLeft = asciiLeft,
        asciiRight = asciiRight,
        charset = if (charset == UrlCharset.Gb2312) "gb2312" else "utf-8",
        asciiFormat = if (asciiFormat == AsciiFormat.Hex) "hex" else "decimal"
    )

    fun restore(snapshot: EncodeSessionSnapshot) {
        tab = when (snapshot.tab) {
            "url" -> EncodeTab.Url
            "hex" -> EncodeTab.Hex
            "ascii" -> EncodeTab.Ascii
            else -> EncodeTab.Unicode
        }
        unicodeLeft = snapshot.unicodeLeft
        unicodeRight = snapshot.unicodeRight
        urlLeft = snapshot.urlLeft
        urlRight = snapshot.urlRight
        hexLeft = snapshot.hexLeft
        hexRight = snapshot.hexRight
        asciiLeft = snapshot.asciiLeft
        asciiRight = snapshot.asciiRight
        charset = if (snapshot.charset == "gb2312") UrlCharset.Gb2312 else UrlCharset.Utf8
        asciiFormat = if (snapshot.asciiFormat == "hex") AsciiFormat.Hex else AsciiFormat.Decimal
        historyOpen = false
        notice = ""
        error = ""
    }
}

@Serializable
data class UaSessionSnapshot(
    val source: String,
    val resultJson: String = ""
)

class UaSession {
    var source: String = UaEngine.presets.first().second
    var result: UaResult? = runCatching { UaEngine.parse(source) }.getOrNull()
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): UaSessionSnapshot = UaSessionSnapshot(
        source = source,
        resultJson = result?.let { uaResultCodec.encodeToString(it) }.orEmpty()
    )

    fun restore(snapshot: UaSessionSnapshot) {
        source = snapshot.source
        result = snapshot.resultJson.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { uaResultCodec.decodeFromString<UaResult>(raw) }.getOrNull()
        }
        historyOpen = false
        notice = ""
        error = ""
    }
}

private val uaResultCodec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class RegexSessionSnapshot(
    val tab: String = "test",
    val pattern: String = "(moo)(\\d+)",
    val source: String = "moo1\nMOO22\nmoo333",
    val global: Boolean = true,
    val ignoreCase: Boolean = false,
    val multiline: Boolean = false,
    val dotAll: Boolean = false
)

class RegexSession {
    var tab: String = "test"
    var pattern: String = "(moo)(\\d+)"
    var source: String = "moo1\nMOO22\nmoo333"
    var options: RegexOptions = RegexOptions()
    var matches: List<RegexMatch> = emptyList()
    var historyOpen: Boolean = false
    var favoritesOpen: Boolean = false
    var running: Boolean = false
    var notice: String = ""
    var error: String = ""
    var favoriteName: String = ""
    var matchGeneration: Long = 0

    fun snapshot(): RegexSessionSnapshot = RegexSessionSnapshot(
        tab = tab,
        pattern = pattern,
        source = source,
        global = options.global,
        ignoreCase = options.ignoreCase,
        multiline = options.multiline,
        dotAll = options.dotAll
    )

    fun restore(snapshot: RegexSessionSnapshot) {
        tab = snapshot.tab
        pattern = snapshot.pattern
        source = snapshot.source
        options = RegexOptions(snapshot.global, snapshot.ignoreCase, snapshot.multiline, snapshot.dotAll)
        matches = emptyList()
        historyOpen = false
        favoritesOpen = false
        running = false
        notice = ""
        error = ""
        favoriteName = ""
        matchGeneration += 1
    }
}

@Serializable
data class CronSessionSnapshot(
    val second: String = "0",
    val minute: String = "*",
    val hour: String = "*",
    val day: String = "*",
    val month: String = "*",
    val week: String = "?",
    val year: String = "",
    val expression: String = "0 * * * * ?",
    val zone: String = "UTC"
)

class CronSession {
    var fields: CronFields = CronEngine.defaultFields
    var expression: String = CronEngine.build(CronEngine.defaultFields)
    var zone: String = TimeEngine.systemZone()
    var runs: List<String> = emptyList()
    var description: String = ""
    var historyOpen: Boolean = false
    var favoritesOpen: Boolean = false
    var favoriteName: String = ""
    var notice: String = ""
    var error: String = ""

    fun snapshot(): CronSessionSnapshot = CronSessionSnapshot(
        second = fields.second,
        minute = fields.minute,
        hour = fields.hour,
        day = fields.day,
        month = fields.month,
        week = fields.week,
        year = fields.year,
        expression = expression,
        zone = zone
    )

    fun restore(snapshot: CronSessionSnapshot) {
        fields = CronFields(snapshot.second, snapshot.minute, snapshot.hour, snapshot.day, snapshot.month, snapshot.week, snapshot.year)
        expression = snapshot.expression
        zone = snapshot.zone
        runs = emptyList()
        description = ""
        historyOpen = false
        favoritesOpen = false
        favoriteName = ""
        notice = ""
        error = ""
    }
}

@Serializable
data class DiffSessionSnapshot(
    val left: String = "MooTool\nquiet desktop tools\nold line\n",
    val right: String = "MooTool\nquiet desktop toolkit\nnew line\n",
    val mode: String = "side",
    val highlightMode: String = "both",
    val ignoreWhitespace: Boolean = false
)

class DiffSession {
    var left: String = "MooTool\nquiet desktop tools\nold line\n"
    var right: String = "MooTool\nquiet desktop toolkit\nnew line\n"
    var mode: String = "side"
    var highlightMode: String = "both"
    var ignoreWhitespace: Boolean = false
    var result: DiffResult = DiffEngine.compare(left, right, false)
    var historyOpen: Boolean = false
    var notice: String = ""
    var navIndex: Int = -1
    var compareGeneration: Long = 0

    fun snapshot(): DiffSessionSnapshot = DiffSessionSnapshot(left, right, mode, highlightMode, ignoreWhitespace)

    fun restore(snapshot: DiffSessionSnapshot) {
        left = snapshot.left
        right = snapshot.right
        mode = snapshot.mode
        highlightMode = snapshot.highlightMode
        ignoreWhitespace = snapshot.ignoreWhitespace
        result = DiffEngine.compare(left, right, ignoreWhitespace)
        historyOpen = false
        notice = ""
        navIndex = -1
        compareGeneration += 1
    }

    fun visibleSegments(): List<DiffSegment> =
        if (highlightMode == "characters") result.segments.filter { !it.wholeLine } else result.segments
}

@Serializable
data class ReformatSessionSnapshot(
    val tab: String = "text",
    val type: String = "nginx",
    val indent: Int = 4,
    val text: String = "",
    val fileName: String = "",
    val fileSource: String = "",
    val fileResult: String = ""
)

class ReformatSession {
    var tab: String = "text"
    var type: ReformatType = ReformatType.Nginx
    var indent: Int = 4
    var text: String = ReformatEngine.samples.getValue(ReformatType.Nginx)
    var fileName: String = ""
    var fileSource: String = ""
    var fileResult: String = ""
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""
    var busy: Boolean = false
    var formatGeneration: Long = 0

    fun snapshot(): ReformatSessionSnapshot = ReformatSessionSnapshot(
        tab = tab,
        type = type.name.lowercase(),
        indent = indent,
        text = text,
        fileName = fileName,
        fileSource = fileSource,
        fileResult = fileResult
    )

    fun restore(snapshot: ReformatSessionSnapshot) {
        tab = if (snapshot.tab == "file") "file" else "text"
        type = ReformatType.entries.firstOrNull { it.name.equals(snapshot.type, ignoreCase = true) } ?: ReformatType.Nginx
        indent = snapshot.indent.coerceIn(2, 6)
        text = snapshot.text
        fileName = snapshot.fileName
        fileSource = snapshot.fileSource
        fileResult = snapshot.fileResult
        historyOpen = false
        notice = ""
        error = ""
        busy = false
        formatGeneration += 1
    }

    companion object {
        fun defaultSample(type: ReformatType): String = ReformatEngine.samples.getValue(type)
    }
}

@Serializable
data class ConfigSessionSnapshot(
    val tab: String = "convert",
    val properties: String = ConfigEngine.SAMPLE_PROPERTIES,
    val yaml: String = "",
    val validateSource: String = ConfigEngine.SAMPLE_YAML,
    val validation: String = ""
)

class ConfigSession {
    var tab: String = "convert"
    var properties: String = ConfigEngine.SAMPLE_PROPERTIES
    var yaml: String = ""
    var validateSource: String = ConfigEngine.SAMPLE_YAML
    var validation: String = ""
    var valid: Boolean? = null
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): ConfigSessionSnapshot = ConfigSessionSnapshot(tab, properties, yaml, validateSource, validation)

    fun restore(snapshot: ConfigSessionSnapshot) {
        tab = if (snapshot.tab == "validate") "validate" else "convert"
        properties = snapshot.properties
        yaml = snapshot.yaml
        validateSource = snapshot.validateSource
        validation = snapshot.validation
        valid = null
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
    private var encodeSessionCache: EncodeSession? = null
    private var uaSessionCache: UaSession? = null
    private var regexSessionCache: RegexSession? = null
    private var cronSessionCache: CronSession? = null
    private var diffSessionCache: DiffSession? = null
    private var reformatSessionCache: ReformatSession? = null
    private var configSessionCache: ConfigSession? = null
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

    fun encodeSession(): EncodeSession {
        encodeSessionCache?.let { return it }
        val session = EncodeSession()
        store.load(ToolId.Encode.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<EncodeSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        encodeSessionCache = session
        return session
    }

    fun persistEncode() {
        store.save(ToolId.Encode.id, jsonCodec.encodeToString(encodeSession().snapshot()))
    }

    fun uaSession(): UaSession {
        uaSessionCache?.let { return it }
        val session = UaSession()
        store.load(ToolId.UaParse.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<UaSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        uaSessionCache = session
        return session
    }

    fun persistUa() {
        store.save(ToolId.UaParse.id, jsonCodec.encodeToString(uaSession().snapshot()))
    }

    fun regexSession(): RegexSession {
        regexSessionCache?.let { return it }
        val session = RegexSession()
        store.load(ToolId.Regex.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<RegexSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        regexSessionCache = session
        return session
    }

    fun persistRegex() {
        store.save(ToolId.Regex.id, jsonCodec.encodeToString(regexSession().snapshot()))
    }

    fun cronSession(): CronSession {
        cronSessionCache?.let { return it }
        val session = CronSession()
        store.load(ToolId.Cron.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<CronSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        cronSessionCache = session
        return session
    }

    fun persistCron() {
        store.save(ToolId.Cron.id, jsonCodec.encodeToString(cronSession().snapshot()))
    }

    fun diffSession(): DiffSession {
        diffSessionCache?.let { return it }
        val session = DiffSession()
        store.load(ToolId.TextDiff.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<DiffSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        diffSessionCache = session
        return session
    }

    fun persistDiff() {
        store.save(ToolId.TextDiff.id, jsonCodec.encodeToString(diffSession().snapshot()))
    }

    fun reformatSession(): ReformatSession {
        reformatSessionCache?.let { return it }
        val session = ReformatSession()
        store.load(ToolId.Reformat.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<ReformatSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        reformatSessionCache = session
        return session
    }

    fun persistReformat() {
        store.save(ToolId.Reformat.id, jsonCodec.encodeToString(reformatSession().snapshot()))
    }

    fun configSession(): ConfigSession {
        configSessionCache?.let { return it }
        val session = ConfigSession()
        store.load(ToolId.YmlProperties.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<ConfigSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        configSessionCache = session
        return session
    }

    fun persistConfig() {
        store.save(ToolId.YmlProperties.id, jsonCodec.encodeToString(configSession().snapshot()))
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
