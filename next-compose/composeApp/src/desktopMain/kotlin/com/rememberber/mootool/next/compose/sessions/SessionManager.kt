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
import com.rememberber.mootool.next.compose.domain.AsymmetricAlgorithm
import com.rememberber.mootool.next.compose.domain.BaseAlgorithm
import com.rememberber.mootool.next.compose.domain.ConfigEngine
import com.rememberber.mootool.next.compose.domain.CryptoEngine
import com.rememberber.mootool.next.compose.domain.CryptoTab
import com.rememberber.mootool.next.compose.domain.DigestAlgorithm
import com.rememberber.mootool.next.compose.domain.SymmetricAlgorithm
import com.rememberber.mootool.next.compose.domain.ProtobufBinaryFormat
import com.rememberber.mootool.next.compose.domain.ProtobufEngine
import com.rememberber.mootool.next.compose.domain.BoardAlignment
import com.rememberber.mootool.next.compose.domain.BoardTheme
import com.rememberber.mootool.next.compose.domain.ColorEngine
import com.rememberber.mootool.next.compose.domain.ColorFormat
import com.rememberber.mootool.next.compose.domain.ColorThemeId
import com.rememberber.mootool.next.compose.domain.MessageBoardEngine
import com.rememberber.mootool.next.compose.domain.HardwareSnapshot
import com.rememberber.mootool.next.compose.domain.HardwareTab
import com.rememberber.mootool.next.compose.domain.NetworkAction
import com.rememberber.mootool.next.compose.domain.ImageOutputFormat
import com.rememberber.mootool.next.compose.domain.ImageOutputMode
import com.rememberber.mootool.next.compose.domain.ImageSvgDetail
import com.rememberber.mootool.next.compose.domain.ImageSvgPreset
import com.rememberber.mootool.next.compose.domain.PdfSplitRule
import com.rememberber.mootool.next.compose.domain.PdfTab
import com.rememberber.mootool.next.compose.domain.PdfTaskStatus
import com.rememberber.mootool.next.compose.domain.WatermarkFontSize
import com.rememberber.mootool.next.compose.domain.WatermarkPosition
import com.rememberber.mootool.next.compose.domain.QrEngine
import com.rememberber.mootool.next.compose.domain.QrErrorCorrection
import com.rememberber.mootool.next.compose.domain.QrTab
import com.rememberber.mootool.next.compose.domain.RgbColor
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

@Serializable
data class ProtobufSessionSnapshot(
    val tab: String = "json",
    val proto: String = ProtobufEngine.SAMPLE_PROTO,
    val messageName: String = ProtobufEngine.SAMPLE_MESSAGE,
    val format: String = "Hex",
    val json: String = ProtobufEngine.SAMPLE_JSON,
    val binary: String = "",
    val wireInput: String = "",
    val wireFormat: String = "Hex",
    val wireOutput: String = "",
    val hex: String = "",
    val base64: String = ""
)

class ProtobufSession {
    var tab: String = "json"
    var proto: String = ProtobufEngine.SAMPLE_PROTO
    var messageName: String = ProtobufEngine.SAMPLE_MESSAGE
    var format: ProtobufBinaryFormat = ProtobufBinaryFormat.Hex
    var json: String = ProtobufEngine.SAMPLE_JSON
    var binary: String = ""
    var wireInput: String = ""
    var wireFormat: ProtobufBinaryFormat = ProtobufBinaryFormat.Hex
    var wireOutput: String = ""
    var hex: String = ""
    var base64: String = ""
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): ProtobufSessionSnapshot = ProtobufSessionSnapshot(
        tab = tab,
        proto = proto,
        messageName = messageName,
        format = format.name,
        json = json,
        binary = binary,
        wireInput = wireInput,
        wireFormat = wireFormat.name,
        wireOutput = wireOutput,
        hex = hex,
        base64 = base64
    )

    fun restore(snapshot: ProtobufSessionSnapshot) {
        tab = when (snapshot.tab) {
            "wire" -> "wire"
            "convert" -> "convert"
            else -> "json"
        }
        proto = snapshot.proto
        messageName = snapshot.messageName
        format = formatOf(snapshot.format)
        json = snapshot.json
        binary = snapshot.binary
        wireInput = snapshot.wireInput
        wireFormat = formatOf(snapshot.wireFormat)
        wireOutput = snapshot.wireOutput
        hex = snapshot.hex
        base64 = snapshot.base64
        historyOpen = false
        notice = ""
        error = ""
    }

    companion object {
        fun formatOf(value: String): ProtobufBinaryFormat =
            if (value.equals("Base64", ignoreCase = true)) ProtobufBinaryFormat.Base64 else ProtobufBinaryFormat.Hex
    }
}

@Serializable
data class CryptoSessionSnapshot(
    val tab: String = "symmetric",
    val symAlgorithm: String = "AES",
    val symKey: String = CryptoEngine.SAMPLE_KEY,
    val symPlain: String = CryptoEngine.SAMPLE_PLAIN,
    val symCipher: String = "",
    val asymAlgorithm: String = "RSA",
    val publicKey: String = "",
    val privateKey: String = "",
    val asymPlain: String = CryptoEngine.SAMPLE_PLAIN,
    val asymCipher: String = "",
    val digestAlgorithm: String = "SHA256",
    val digestInput: String = CryptoEngine.SAMPLE_PLAIN,
    val digestOutput: String = "",
    val digestFileName: String = "",
    val baseAlgorithm: String = "Base64",
    val basePlain: String = CryptoEngine.SAMPLE_PLAIN,
    val baseCipher: String = "",
    val randomLength: Int = 16,
    val uuid: String = "",
    val digits: String = "",
    val randomText: String = "",
    val password: String = ""
)

class CryptoSession {
    var tab: CryptoTab = CryptoTab.Symmetric
    var symAlgorithm: SymmetricAlgorithm = SymmetricAlgorithm.AES
    var symKey: String = CryptoEngine.SAMPLE_KEY
    var symPlain: String = CryptoEngine.SAMPLE_PLAIN
    var symCipher: String = ""
    var asymAlgorithm: AsymmetricAlgorithm = AsymmetricAlgorithm.RSA
    var publicKey: String = ""
    var privateKey: String = ""
    var asymPlain: String = CryptoEngine.SAMPLE_PLAIN
    var asymCipher: String = ""
    var digestAlgorithm: DigestAlgorithm = DigestAlgorithm.SHA256
    var digestInput: String = CryptoEngine.SAMPLE_PLAIN
    var digestOutput: String = ""
    var digestFileName: String = ""
    var baseAlgorithm: BaseAlgorithm = BaseAlgorithm.Base64
    var basePlain: String = CryptoEngine.SAMPLE_PLAIN
    var baseCipher: String = ""
    var randomLength: Int = 16
    var uuid: String = ""
    var digits: String = ""
    var randomText: String = ""
    var password: String = ""
    var asymBusy: Boolean = false
    var historyOpen: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): CryptoSessionSnapshot = CryptoSessionSnapshot(
        tab = tab.name.lowercase(),
        symAlgorithm = symAlgorithm.name,
        symKey = symKey,
        symPlain = symPlain,
        symCipher = symCipher,
        asymAlgorithm = asymAlgorithm.name,
        publicKey = publicKey,
        privateKey = privateKey,
        asymPlain = asymPlain,
        asymCipher = asymCipher,
        digestAlgorithm = digestAlgorithm.name,
        digestInput = digestInput,
        digestOutput = digestOutput,
        digestFileName = digestFileName,
        baseAlgorithm = baseAlgorithm.name,
        basePlain = basePlain,
        baseCipher = baseCipher,
        randomLength = randomLength,
        uuid = uuid,
        digits = digits,
        randomText = randomText,
        password = password
    )

    fun restore(snapshot: CryptoSessionSnapshot) {
        tab = CryptoTab.entries.find { it.name.equals(snapshot.tab, ignoreCase = true) } ?: CryptoTab.Symmetric
        symAlgorithm = SymmetricAlgorithm.entries.find { it.name == snapshot.symAlgorithm } ?: SymmetricAlgorithm.AES
        symKey = snapshot.symKey
        symPlain = snapshot.symPlain
        symCipher = snapshot.symCipher
        asymAlgorithm = AsymmetricAlgorithm.entries.find { it.name == snapshot.asymAlgorithm } ?: AsymmetricAlgorithm.RSA
        publicKey = snapshot.publicKey
        privateKey = snapshot.privateKey
        asymPlain = snapshot.asymPlain
        asymCipher = snapshot.asymCipher
        digestAlgorithm = DigestAlgorithm.entries.find { it.name == snapshot.digestAlgorithm } ?: DigestAlgorithm.SHA256
        digestInput = snapshot.digestInput
        digestOutput = snapshot.digestOutput
        digestFileName = snapshot.digestFileName
        baseAlgorithm = BaseAlgorithm.entries.find { it.name == snapshot.baseAlgorithm } ?: BaseAlgorithm.Base64
        basePlain = snapshot.basePlain
        baseCipher = snapshot.baseCipher
        randomLength = snapshot.randomLength.coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)
        uuid = snapshot.uuid
        digits = snapshot.digits
        randomText = snapshot.randomText
        password = snapshot.password
        asymBusy = false
        historyOpen = false
        notice = ""
        error = ""
    }
}

@Serializable
data class QrSessionSnapshot(
    val tab: String = "generate",
    val content: String = QrEngine.SAMPLE_CONTENT,
    val size: Int = QrEngine.DEFAULT_SIZE,
    val correction: String = "M",
    val logoName: String = "",
    val logoPath: String = "",
    val recognitionName: String = "",
    val recognitionResult: String = ""
)

class QrSession {
    var tab: QrTab = QrTab.Generate
    var content: String = QrEngine.SAMPLE_CONTENT
    var size: Int = QrEngine.DEFAULT_SIZE
    var correction: QrErrorCorrection = QrErrorCorrection.M
    var logoName: String = ""
    var logoPath: String = ""
    var logoImage: java.awt.image.BufferedImage? = null
    var pngBytes: ByteArray? = null
    var recognitionName: String = ""
    var recognitionBytes: ByteArray? = null
    var recognitionResult: String = ""
    var busy: Boolean = false
    var historyTick: Long = 0
    var notice: String = ""
    var error: String = ""

    fun snapshot(): QrSessionSnapshot = QrSessionSnapshot(
        tab = tab.name.lowercase(),
        content = content,
        size = size,
        correction = correction.name,
        logoName = logoName,
        logoPath = logoPath,
        recognitionName = recognitionName,
        recognitionResult = recognitionResult
    )

    fun restore(snapshot: QrSessionSnapshot) {
        tab = QrTab.entries.find { it.name.equals(snapshot.tab, ignoreCase = true) } ?: QrTab.Generate
        content = snapshot.content
        size = QrEngine.normalizeSize(snapshot.size)
        correction = QrErrorCorrection.entries.find { it.name == snapshot.correction } ?: QrErrorCorrection.M
        logoName = snapshot.logoName
        logoPath = snapshot.logoPath
        logoImage = snapshot.logoPath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { QrEngine.readImageFile(java.nio.file.Path.of(path)) }.getOrNull()
        }
        pngBytes = null
        recognitionName = snapshot.recognitionName
        recognitionBytes = null
        recognitionResult = snapshot.recognitionResult
        busy = false
        historyTick = 0
        notice = ""
        error = ""
    }
}

@Serializable
data class ColorSessionSnapshot(
    val primaryHex: String = "#DE8F7D",
    val secondaryHex: String = "#4F83CC",
    val format: String = "HEX_UPPER",
    val code: String = "#DE8F7D",
    val theme: String = "default",
    val favoriteName: String = "",
    val favoriteFolderId: String = ""
)

class ColorSession {
    var primary: RgbColor = ColorEngine.DEFAULT_PRIMARY
    var secondary: RgbColor = ColorEngine.DEFAULT_SECONDARY
    var format: ColorFormat = ColorFormat.HEX_UPPER
    var code: String = ColorEngine.formatColor(ColorEngine.DEFAULT_PRIMARY, ColorFormat.HEX_UPPER)
    var theme: ColorThemeId = ColorThemeId.Default
    var favoriteName: String = ""
    var favoriteFolderId: String = ""
    var folderTitle: String = ""
    var historyOpen: Boolean = false
    var favoritesOpen: Boolean = false
    var saveFavoriteOpen: Boolean = false
    var picking: Boolean = false
    var historyTick: Long = 0
    var notice: String = ""
    var error: String = ""

    val primaryHex: String get() = ColorEngine.formatColor(primary, ColorFormat.HEX_UPPER)
    val secondaryHex: String get() = ColorEngine.formatColor(secondary, ColorFormat.HEX_UPPER)

    fun snapshot(): ColorSessionSnapshot = ColorSessionSnapshot(
        primaryHex = primaryHex,
        secondaryHex = secondaryHex,
        format = format.name,
        code = code,
        theme = ColorEngine.canonicalThemeId(theme),
        favoriteName = favoriteName,
        favoriteFolderId = favoriteFolderId
    )

    fun restore(snapshot: ColorSessionSnapshot) {
        primary = runCatching { ColorEngine.parseColor(snapshot.primaryHex) }.getOrDefault(ColorEngine.DEFAULT_PRIMARY)
        secondary = runCatching { ColorEngine.parseColor(snapshot.secondaryHex) }.getOrDefault(ColorEngine.DEFAULT_SECONDARY)
        format = ColorFormat.entries.find { it.name.equals(snapshot.format, ignoreCase = true) } ?: ColorFormat.HEX_UPPER
        code = snapshot.code.ifBlank { ColorEngine.formatColor(primary, format) }
        theme = ColorEngine.themeId(snapshot.theme)
        favoriteName = snapshot.favoriteName
        favoriteFolderId = snapshot.favoriteFolderId
        folderTitle = ""
        historyOpen = false
        favoritesOpen = false
        saveFavoriteOpen = false
        picking = false
        historyTick = 0
        notice = ""
        error = ""
    }
}

@Serializable
data class MessageBoardSessionSnapshot(
    val message: String = "",
    val theme: String = "sunbeam",
    val alignment: String = "center",
    val size: Int = MessageBoardEngine.DEFAULT_SIZE
)

class MessageBoardSession {
    var message: String = ""
    var theme: BoardTheme = BoardTheme.Sunbeam
    var alignment: BoardAlignment = BoardAlignment.Center
    var size: Int = MessageBoardEngine.DEFAULT_SIZE
    var restored: Boolean = false
    var presenting: Boolean = false
    var displayAwake: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): MessageBoardSessionSnapshot = MessageBoardSessionSnapshot(
        message = MessageBoardEngine.clip(message),
        theme = theme.name.lowercase(),
        alignment = alignment.name.lowercase(),
        size = MessageBoardEngine.normalizeSize(size)
    )

    fun restore(snapshot: MessageBoardSessionSnapshot) {
        message = MessageBoardEngine.clip(snapshot.message)
        theme = MessageBoardEngine.themeId(snapshot.theme)
        alignment = MessageBoardEngine.alignmentId(snapshot.alignment)
        size = MessageBoardEngine.normalizeSize(snapshot.size)
        restored = true
        presenting = false
        displayAwake = false
        notice = ""
        error = ""
    }
}

@Serializable
data class PdfSplitRowSnapshot(
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int,
    val selected: Boolean = true,
    val pageRange: String,
    val rule: String = "odd",
    val customRule: String = ""
)

@Serializable
data class PdfMergeRowSnapshot(
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int,
    val selected: Boolean = true,
    val pages: String
)

@Serializable
data class PdfSessionSnapshot(
    val tab: String = "split",
    val splitRows: List<PdfSplitRowSnapshot> = emptyList(),
    val mergeRows: List<PdfMergeRowSnapshot> = emptyList(),
    val lastOutputs: List<String> = emptyList()
)

class PdfSplitRow(
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int,
    var selected: Boolean = true,
    var pageRange: String,
    var rule: PdfSplitRule = PdfSplitRule.Odd,
    var customRule: String = "",
    var status: PdfTaskStatus = PdfTaskStatus.Ready
)

class PdfMergeRow(
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int,
    var selected: Boolean = true,
    var pages: String,
    var status: PdfTaskStatus = PdfTaskStatus.Ready
)

class PdfSession {
    var tab: PdfTab = PdfTab.Split
    var splitRows: List<PdfSplitRow> = emptyList()
    var mergeRows: List<PdfMergeRow> = emptyList()
    var lastOutputs: List<String> = emptyList()
    var helpOpen: Boolean = false
    var confirmSplit: Boolean = false
    var busy: Boolean = false
    var cancelled: Boolean = false
    var notice: String = ""
    var error: String = ""

    fun snapshot(): PdfSessionSnapshot = PdfSessionSnapshot(
        tab = tab.name.lowercase(),
        splitRows = splitRows.map {
            PdfSplitRowSnapshot(it.path, it.name, it.size, it.pageCount, it.selected, it.pageRange, it.rule.name.lowercase(), it.customRule)
        },
        mergeRows = mergeRows.map {
            PdfMergeRowSnapshot(it.path, it.name, it.size, it.pageCount, it.selected, it.pages)
        },
        lastOutputs = lastOutputs
    )

    fun restore(snapshot: PdfSessionSnapshot) {
        tab = PdfTab.entries.find { it.name.equals(snapshot.tab, ignoreCase = true) } ?: PdfTab.Split
        splitRows = snapshot.splitRows.map {
            PdfSplitRow(
                path = it.path,
                name = it.name,
                size = it.size,
                pageCount = it.pageCount,
                selected = it.selected,
                pageRange = it.pageRange,
                rule = PdfSplitRule.entries.find { rule -> rule.name.equals(it.rule, ignoreCase = true) } ?: PdfSplitRule.Odd,
                customRule = it.customRule,
                status = PdfTaskStatus.Ready
            )
        }
        mergeRows = snapshot.mergeRows.map {
            PdfMergeRow(it.path, it.name, it.size, it.pageCount, it.selected, it.pages, PdfTaskStatus.Ready)
        }
        lastOutputs = snapshot.lastOutputs
        helpOpen = false
        confirmSplit = false
        busy = false
        cancelled = false
        notice = ""
        error = ""
    }
}

@Serializable
data class ImageSessionSnapshot(
    val listVisible: Boolean = true,
    val currentName: String = "",
    val selectedNames: List<String> = emptyList(),
    val zoom: Float = 1f,
    val fit: Boolean = true,
    val lastOutputs: List<String> = emptyList()
)

class ImageSession {
    var listVisible: Boolean = true
    var currentName: String = ""
    var selectedNames: List<String> = emptyList()
    var zoom: Float = 1f
    var fit: Boolean = true
    var lastOutputs: List<String> = emptyList()
    var busy: Boolean = false
    var cancelled: Boolean = false
    var notice: String = ""
    var error: String = ""
    var base64Mode: String? = null
    var base64Text: String = ""
    var compressOpen: Boolean = false
    var watermarkOpen: Boolean = false
    var svgOpen: Boolean = false
    var renameOpen: Boolean = false
    var saveOpen: Boolean = false
    var deleteOpen: Boolean = false
    var promptValue: String = ""
    var compressQuality: Int = 80
    var compressScale: Int = 100
    var compressFormat: ImageOutputFormat = ImageOutputFormat.Auto
    var outputMode: ImageOutputMode = ImageOutputMode.Keep
    var watermarkText: String = "MooTool"
    var watermarkOpacity: Int = 50
    var watermarkColor: String = "#FFFFFF"
    var watermarkPosition: WatermarkPosition = WatermarkPosition.BottomRight
    var watermarkFont: WatermarkFontSize = WatermarkFontSize.Auto
    var watermarkDiagonal: Boolean = false
    var svgPreset: ImageSvgPreset = ImageSvgPreset.Poster
    var svgColors: Int = 16
    var svgDetail: ImageSvgDetail = ImageSvgDetail.Medium
    var svgSpeckle: Int = 8

    fun snapshot(): ImageSessionSnapshot = ImageSessionSnapshot(
        listVisible = listVisible,
        currentName = currentName,
        selectedNames = selectedNames,
        zoom = zoom,
        fit = fit,
        lastOutputs = lastOutputs
    )

    fun restore(snapshot: ImageSessionSnapshot) {
        listVisible = snapshot.listVisible
        currentName = snapshot.currentName
        selectedNames = snapshot.selectedNames
        zoom = snapshot.zoom.coerceIn(0.1f, 5f)
        fit = snapshot.fit
        lastOutputs = snapshot.lastOutputs
        busy = false
        cancelled = false
        notice = ""
        error = ""
        base64Mode = null
        compressOpen = false
        watermarkOpen = false
        svgOpen = false
        renameOpen = false
        saveOpen = false
        deleteOpen = false
    }
}

@Serializable
data class NetSessionSnapshot(
    val ipv4: String = "127.0.0.1",
    val longValue: String = "2130706433",
    val pingTarget: String = "127.0.0.1",
    val ipRange: String = "192.168.10",
    val portScanTarget: String = "127.0.0.1",
    val portSpec: String = "",
    val hostTarget: String = "localhost",
    val whoisTarget: String = "example.com",
    val output: String = "",
    val ipv4Addresses: String = "",
    val ipv6Addresses: String = ""
)

class NetSession {
    val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var ipv4: String = "127.0.0.1"
    var longValue: String = "2130706433"
    var pingTarget: String = "127.0.0.1"
    var ipRange: String = "192.168.10"
    var portScanTarget: String = "127.0.0.1"
    var portSpec: String = ""
    var hostTarget: String = "localhost"
    var whoisTarget: String = "example.com"
    var output: String = ""
    var ipv4Addresses: String = ""
    var ipv6Addresses: String = ""
    var running: NetworkAction? = null
    var error: String = ""
    var notice: String = ""
    var historyOpen: Boolean = false

    fun snapshot(): NetSessionSnapshot = NetSessionSnapshot(
        ipv4 = ipv4,
        longValue = longValue,
        pingTarget = pingTarget,
        ipRange = ipRange,
        portScanTarget = portScanTarget,
        portSpec = portSpec,
        hostTarget = hostTarget,
        whoisTarget = whoisTarget,
        output = output.take(256_000),
        ipv4Addresses = ipv4Addresses,
        ipv6Addresses = ipv6Addresses
    )

    fun restore(snapshot: NetSessionSnapshot) {
        ipv4 = snapshot.ipv4
        longValue = snapshot.longValue
        pingTarget = snapshot.pingTarget
        ipRange = snapshot.ipRange
        portScanTarget = snapshot.portScanTarget
        portSpec = snapshot.portSpec
        hostTarget = snapshot.hostTarget
        whoisTarget = snapshot.whoisTarget
        output = snapshot.output
        ipv4Addresses = snapshot.ipv4Addresses
        ipv6Addresses = snapshot.ipv6Addresses
        running = null
        error = ""
        notice = ""
        historyOpen = false
    }
}

@Serializable
data class HardwareSessionSnapshot(
    val tab: String = "system",
    val revealSensitive: Boolean = false
)

class HardwareSession {
    var tab: HardwareTab = HardwareTab.System
    var revealSensitive: Boolean = false
    var snapshot: HardwareSnapshot? = null
    var loading: Boolean = false
    var error: String = ""

    fun snapshotState(): HardwareSessionSnapshot = HardwareSessionSnapshot(
        tab = tab.name.lowercase(),
        revealSensitive = revealSensitive
    )

    fun restore(snapshot: HardwareSessionSnapshot) {
        tab = HardwareTab.entries.find { it.name.equals(snapshot.tab, ignoreCase = true) } ?: HardwareTab.System
        revealSensitive = snapshot.revealSensitive
        this.snapshot = null
        loading = false
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
    private var protobufSessionCache: ProtobufSession? = null
    private var cryptoSessionCache: CryptoSession? = null
    private var qrSessionCache: QrSession? = null
    private var colorSessionCache: ColorSession? = null
    private var messageBoardSessionCache: MessageBoardSession? = null
    private var pdfSessionCache: PdfSession? = null
    private var imageSessionCache: ImageSession? = null
    private var netSessionCache: NetSession? = null
    private var hardwareSessionCache: HardwareSession? = null
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

    fun protobufSession(): ProtobufSession {
        protobufSessionCache?.let { return it }
        val session = ProtobufSession()
        store.load(ToolId.Protobuf.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<ProtobufSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        protobufSessionCache = session
        return session
    }

    fun persistProtobuf() {
        store.save(ToolId.Protobuf.id, jsonCodec.encodeToString(protobufSession().snapshot()))
    }

    fun cryptoSession(defaultRandomLength: Int = 16): CryptoSession {
        cryptoSessionCache?.let { return it }
        val session = CryptoSession()
        val raw = store.load(ToolId.Crypto.id)
        if (raw != null) {
            runCatching { jsonCodec.decodeFromString<CryptoSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        } else {
            session.randomLength = defaultRandomLength.coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)
        }
        cryptoSessionCache = session
        return session
    }

    fun persistCrypto() {
        store.save(ToolId.Crypto.id, jsonCodec.encodeToString(cryptoSession().snapshot()))
    }

    fun qrSession(defaultSize: Int = QrEngine.DEFAULT_SIZE, defaultCorrection: String = "M"): QrSession {
        qrSessionCache?.let { return it }
        val session = QrSession()
        val raw = store.load(ToolId.QrCode.id)
        if (raw != null) {
            runCatching { jsonCodec.decodeFromString<QrSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        } else {
            session.size = QrEngine.normalizeSize(defaultSize)
            session.correction = QrErrorCorrection.entries.find { it.name == defaultCorrection } ?: QrErrorCorrection.M
        }
        qrSessionCache = session
        return session
    }

    fun persistQr() {
        store.save(ToolId.QrCode.id, jsonCodec.encodeToString(qrSession().snapshot()))
    }

    fun colorSession(): ColorSession {
        colorSessionCache?.let { return it }
        val session = ColorSession()
        store.load(ToolId.ColorBoard.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<ColorSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        colorSessionCache = session
        return session
    }

    fun persistColor() {
        store.save(ToolId.ColorBoard.id, jsonCodec.encodeToString(colorSession().snapshot()))
    }

    fun messageBoardSession(): MessageBoardSession {
        messageBoardSessionCache?.let { return it }
        val session = MessageBoardSession()
        store.load(ToolId.MessageBoard.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<MessageBoardSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        messageBoardSessionCache = session
        return session
    }

    fun persistMessageBoard() {
        store.save(ToolId.MessageBoard.id, jsonCodec.encodeToString(messageBoardSession().snapshot()))
    }

    fun pdfSession(): PdfSession {
        pdfSessionCache?.let { return it }
        val session = PdfSession()
        store.load(ToolId.Pdf.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<PdfSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        pdfSessionCache = session
        return session
    }

    fun persistPdf() {
        store.save(ToolId.Pdf.id, jsonCodec.encodeToString(pdfSession().snapshot()))
    }

    fun imageSession(): ImageSession {
        imageSessionCache?.let { return it }
        val session = ImageSession()
        store.load(ToolId.Image.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<ImageSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        imageSessionCache = session
        return session
    }

    fun persistImage() {
        store.save(ToolId.Image.id, jsonCodec.encodeToString(imageSession().snapshot()))
    }

    fun netSession(): NetSession {
        netSessionCache?.let { return it }
        val session = NetSession()
        store.load(ToolId.Net.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<NetSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        netSessionCache = session
        return session
    }

    fun persistNet() {
        store.save(ToolId.Net.id, jsonCodec.encodeToString(netSession().snapshot()))
    }

    fun cancelNetCommands() {
        netSessionCache?.commandScope?.cancel()
        netSessionCache?.running = null
    }

    fun hardwareSession(): HardwareSession {
        hardwareSessionCache?.let { return it }
        val session = HardwareSession()
        store.load(ToolId.Hardware.id)?.let { raw ->
            runCatching { jsonCodec.decodeFromString<HardwareSessionSnapshot>(raw) }
                .getOrNull()
                ?.let(session::restore)
        }
        hardwareSessionCache = session
        return session
    }

    fun persistHardware() {
        store.save(ToolId.Hardware.id, jsonCodec.encodeToString(hardwareSession().snapshotState()))
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
