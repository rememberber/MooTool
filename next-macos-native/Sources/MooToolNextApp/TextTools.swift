import SwiftUI
import MooToolNextCore

struct ToolRouter: View {
    let id: String
    @Environment(AppStore.self) private var store
    var body: some View {
        Group {
            switch id {
            case "mootool": HomeView()
            case "quickNote", "json": DocumentsTool(id: id, draft: store.draft(id))
            case "reformat": ReformatWorkspace(draft: store.draft(id))
            case "textDiff": TextDiffWorkspace(draft: store.draft(id))
            case "http": HTTPTool(draft: store.draft(id))
            case "host": HostWorkspace(draft: store.draft(id))
            case "net": NetToolView(draft: store.draft(id))
            case "java", "hardware": SystemTool(id: id, draft: store.draft(id))
            case "qrCode": QRTool(draft: store.draft(id))
            case "colorBoard": ColorTool(draft: store.draft(id))
            case "image": ImageTool(draft: store.draft(id))
            case "pdf": PDFTool(draft: store.draft(id))
            case "messageBoard": MessageBoardTool(draft: store.draft(id))
            case "translation": TranslationTool(draft: store.draft(id))
            case "cron": CronToolView(draft: store.draft(id))
            case "timeConvert": TimeConvertView(draft: store.draft(id))
            case "crypto": CryptoToolView(draft: store.draft(id))
            default: TextTool(id: id, draft: store.draft(id))
            }
        }.id(id)
    }
}

struct TextTool: View {
    let id: String
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var regexFlags = ""
    @State private var favoritesOpen = false
    private static let regexModes = ["match", "replace"]
    private static let calculatorModes = ["expr", "decToRadix", "hexToDec", "binToDec"]
    private static let configModes = ["yamlToJson", "jsonToYaml", "yamlToProps", "propsToYaml", "jsonToProps", "propsToJson"]
    private static let digestModes = ["SHA-256", "SHA-384", "SHA-512", "MD5", "SHA-1", "SM3", "HMAC-SHA256", "UUID"]
    private static let cryptoModes = digestModes + ["aesGcmEncrypt", "aesGcmDecrypt", "rsaGen", "rsaPublicEncrypt", "rsaPrivateDecrypt", "rsaSign", "rsaVerify", "random32"]
    private var tool: Tool { Catalog.tool(id) }
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func loc(_ key: String, _ args: [String: String]) -> String {
        args.reduce(loc(key)) { partial, pair in partial.replacingOccurrences(of: "{\(pair.key)}", with: pair.value) }
    }
    private func regexModeLabel(_ mode: String) -> String {
        mode == "replace" || mode == "替换" ? loc("regex.mode.replace") : loc("regex.mode.match")
    }
    private func regexModeIsReplace(_ mode: String) -> Bool { mode == "replace" || mode == "替换" }
    private func calculatorModeLabel(_ mode: String) -> String {
        switch mode {
        case "decToRadix", "十进制 → 其他进制": return loc("calculator.mode.decToRadix")
        case "hexToDec", "十六进制 → 十进制": return loc("calculator.mode.hexToDec")
        case "binToDec", "二进制 → 十进制": return loc("calculator.mode.binToDec")
        default: return loc("calculator.mode.expr")
        }
    }
    private func calculatorModeIsExpr(_ mode: String) -> Bool { mode == "expr" || mode == "表达式" }
    private func calculatorInputRadix(_ mode: String) -> Int {
        switch mode {
        case "hexToDec", "十六进制 → 十进制": return 16
        case "binToDec", "二进制 → 十进制": return 2
        default: return 10
        }
    }
    private func configModeLabel(_ mode: String) -> String {
        let key: String
        switch mode {
        case "yamlToJson", "YAML → JSON": key = "config.mode.yamlToJson"
        case "jsonToYaml", "JSON → YAML": key = "config.mode.jsonToYaml"
        case "yamlToProps", "YAML → Properties": key = "config.mode.yamlToProps"
        case "propsToYaml", "Properties → YAML": key = "config.mode.propsToYaml"
        case "jsonToProps", "JSON → Properties": key = "config.mode.jsonToProps"
        case "propsToJson", "Properties → JSON": key = "config.mode.propsToJson"
        default: return mode
        }
        return loc(key)
    }
    private func configConversionPair(_ mode: String) -> (String, String)? {
        switch mode {
        case "yamlToJson", "YAML → JSON": return ("YAML", "JSON")
        case "jsonToYaml", "JSON → YAML": return ("JSON", "YAML")
        case "yamlToProps", "YAML → Properties": return ("YAML", "Properties")
        case "propsToYaml", "Properties → YAML": return ("Properties", "YAML")
        case "jsonToProps", "JSON → Properties": return ("JSON", "Properties")
        case "propsToJson", "Properties → JSON": return ("Properties", "JSON")
        default: return nil
        }
    }
    private func cryptoModeLabel(_ mode: String) -> String {
        switch mode {
        case "aesGcmEncrypt", "AES-GCM 加密": return loc("textCrypto.mode.aesGcmEncrypt")
        case "aesGcmDecrypt", "AES-GCM 解密": return loc("textCrypto.mode.aesGcmDecrypt")
        case "rsaGen", "RSA 生成密钥对": return loc("textCrypto.mode.rsaGen")
        case "rsaPublicEncrypt", "RSA 公钥加密": return loc("textCrypto.mode.rsaPublicEncrypt")
        case "rsaPrivateDecrypt", "RSA 私钥解密": return loc("textCrypto.mode.rsaPrivateDecrypt")
        case "rsaSign", "RSA 签名": return loc("textCrypto.mode.rsaSign")
        case "rsaVerify", "RSA 验签": return loc("textCrypto.mode.rsaVerify")
        case "random32", "随机 32 字节": return loc("textCrypto.mode.random32")
        default: return mode
        }
    }
    private func cryptoEngineAlgorithm(_ mode: String) -> String {
        switch mode {
        case "aesGcmEncrypt", "AES-GCM 加密": return "AES-GCM 加密"
        case "aesGcmDecrypt", "AES-GCM 解密": return "AES-GCM 解密"
        case "random32", "随机 32 字节": return "随机 32 字节"
        default: return mode
        }
    }
    private func cryptoModeIsRSA(_ mode: String) -> Bool {
        mode.hasPrefix("RSA") || mode.hasPrefix("rsa")
    }
    private func cryptoModeUsesAES(_ mode: String) -> Bool {
        mode.contains("AES") || mode == "aesGcmEncrypt" || mode == "aesGcmDecrypt"
    }
    private func modeLabel(_ mode: String) -> String {
        if id == "regex" { return regexModeLabel(mode) }
        if id == "calculator" { return calculatorModeLabel(mode) }
        if id == "ymlProperties" { return configModeLabel(mode) }
        if id == "crypto" { return cryptoModeLabel(mode) }
        return mode
    }
    var body: some View {
        ToolPage(tool: tool, draft: draft, showsHeading: id != "json") {
            controls
        } content: {
            VStack(spacing: 12) {
                if id == "regex" {
                    HStack {
                        Text(loc("regex.expression")).foregroundStyle(.secondary)
                        TextField(loc("regex.patternPlaceholder"), text: $draft.secondary).font(.system(.body, design: .monospaced))
                        TextField(loc("regex.flagsPlaceholder"), text: $regexFlags).frame(width: 70)
                    }.textFieldStyle(.roundedBorder)
                    if regexModeIsReplace(draft.mode) { TextField(loc("regex.replaceTemplate"), text: $draft.option).textFieldStyle(.roundedBorder) }
                }
                if id == "crypto" {
                    if cryptoModeIsRSA(draft.mode) && draft.mode != "rsaGen" && draft.mode != "RSA 生成密钥对" {
                        TextField(loc("textCrypto.rsaKeyDer"), text: $draft.secondary).textFieldStyle(.roundedBorder)
                    }
                    if draft.mode == "rsaVerify" || draft.mode == "RSA 验签" {
                        TextField(loc("textCrypto.signatureBase64"), text: $draft.option).textFieldStyle(.roundedBorder)
                    }
                    if cryptoModeUsesAES(draft.mode) || draft.mode == "HMAC-SHA256" {
                        SecureField(
                            cryptoModeUsesAES(draft.mode) ? loc("crypto.keyHexGCM") : loc("crypto.hmacKey"),
                            text: $draft.secondary
                        ).textFieldStyle(.roundedBorder)
                    }
                }
                PersistedHSplit(toolID: id, defaultLeading: id == "json" ? 420 : 360, minLeading: id == "json" ? 280 : 200, maxLeading: 900) {
                    EditorPane(title: inputTitle, text: $draft.input, syntax: id == "json" || id == "ymlProperties", persistence: id == "json" ? store.editorPersistence(id) : nil)
                } trailing: {
                    if id == "json" && draft.json?.showsTree == true {
                        JSONTreePane(text: draft.input, path: $draft.option)
                    } else {
                        EditorPane(title: AppLocalization.string("tool.output", language: language), text: $draft.output, editable: false, syntax: ["json", "uaParse", "ymlProperties", "protobuf", "regex"].contains(id), persistence: id == "json" ? store.editorPersistence(id, output: true) : nil)
                    }
                }
            }
        }.onAppear {
            if id == "regex" {
                if draft.mode == "匹配" { draft.mode = "match" }
                if draft.mode == "替换" { draft.mode = "replace" }
            }
            if id == "calculator" {
                switch draft.mode {
                case "表达式": draft.mode = "expr"
                case "十进制 → 其他进制": draft.mode = "decToRadix"
                case "十六进制 → 十进制": draft.mode = "hexToDec"
                case "二进制 → 十进制": draft.mode = "binToDec"
                default: break
                }
            }
            if id == "ymlProperties" {
                switch draft.mode {
                case "YAML → JSON": draft.mode = "yamlToJson"
                case "JSON → YAML": draft.mode = "jsonToYaml"
                case "YAML → Properties": draft.mode = "yamlToProps"
                case "Properties → YAML": draft.mode = "propsToYaml"
                case "JSON → Properties": draft.mode = "jsonToProps"
                case "Properties → JSON": draft.mode = "propsToJson"
                default: break
                }
            }
            if id == "crypto" {
                switch draft.mode {
                case "AES-GCM 加密": draft.mode = "aesGcmEncrypt"
                case "AES-GCM 解密": draft.mode = "aesGcmDecrypt"
                case "RSA 生成密钥对": draft.mode = "rsaGen"
                case "RSA 公钥加密": draft.mode = "rsaPublicEncrypt"
                case "RSA 私钥解密": draft.mode = "rsaPrivateDecrypt"
                case "RSA 签名": draft.mode = "rsaSign"
                case "RSA 验签": draft.mode = "rsaVerify"
                case "随机 32 字节": draft.mode = "random32"
                default: break
                }
            }
            if !modes.isEmpty && !modes.contains(draft.mode) { draft.mode = modes[0] }
            if id == "timeConvert" && draft.option.isEmpty { draft.option = TimeZone.current.identifier }
        }
        .sheet(isPresented: $favoritesOpen) {
            ToolFavoritesSheet(kind: .regex, currentValue: draft.secondary) { pattern in
                draft.secondary = pattern
            }.environment(store)
        }
    }
    private var inputTitle: String {
        if id == "variables" { return loc("variables.inputTitle") }
        if id == "host" { return loc("host.draftTitle") }
        return AppLocalization.string("tool.input", language: language)
    }
    private var modes: [String] {
        switch id {
        case "encode": return ["Base64", "Base32", "URL", "Hex", "Unicode", "HTML"]
        case "crypto": return Self.cryptoModes
        case "regex": return Self.regexModes
        case "ymlProperties": return Self.configModes
        case "protobuf": return ["Hex", "Base64"]
        case "calculator": return Self.calculatorModes
        default: return []
        }
    }
    @ViewBuilder private var controls: some View {
        if !modes.isEmpty { Picker(loc("tool.mode"), selection: $draft.mode) { ForEach(modes, id: \.self) { Text(modeLabel($0)).tag($0) } }.labelsHidden().frame(width: id == "crypto" ? 200 : id == "ymlProperties" ? 185 : 145) }
        switch id {
        case "json":
            PrimaryButton(title: loc("json.action.format"), symbol: "text.alignleft") { execute("format") }
            Button(loc("json.action.compress")) { execute("minify") }
            Menu {
                Picker(loc("json.format.indent"), selection: jsonOption(\.indent)) {
                    Text(loc("json.format.indentTwoSpaces")).tag(2)
                    Text(loc("json.format.indentFourSpaces")).tag(4)
                }
                Toggle(loc("json.format.sortKeys"), isOn: jsonOption(\.sortKeys))
                Divider()
                Button(loc("json.legacy.escapeString")) { execute("escape") }
                Button(loc("json.legacy.unescapeString")) { execute("unescape") }
                Button(loc("tool.swapIO")) { swap(&draft.input, &draft.output) }
            } label: { Image(systemName: "slider.horizontal.3") }.help(loc("json.legacy.formatOptions"))
            Toggle(isOn: jsonOption(\.showsTree)) { Image(systemName: "list.bullet.indent") }.toggleStyle(.button).help(loc("json.legacy.showTree"))
            TextField(loc("json.legacy.pathPlaceholder"), text: $draft.option).textFieldStyle(.roundedBorder).frame(minWidth: 130, idealWidth: 180, maxWidth: 230)
            Button(loc("json.path.query")) { execute("query") }
        case "encode":
            PrimaryButton(title: AppLocalization.string("tool.encode", language: language)) { execute("encode") }
            Button(AppLocalization.string("tool.decode", language: language)) { execute("decode") }
            Button { swap(&draft.input, &draft.output) } label: { Image(systemName: "arrow.left.arrow.right") }.help(loc("tool.swapIO"))
        case "timeConvert":
            PrimaryButton(title: AppLocalization.string("tool.convert", language: language)) { execute() }
            Button(AppLocalization.string("tool.now", language: language)) { draft.input = String(Int64(Date().timeIntervalSince1970 * 1000)); execute() }
            Picker(loc("timeConvert.timezone"), selection: $draft.option) {
                ForEach(Array(Set([TimeZone.current.identifier, "UTC", "Asia/Shanghai", "Asia/Tokyo", "America/New_York", "America/Los_Angeles", "Europe/London"])).sorted(), id: \.self) { Text($0) }
            }.frame(width: 240)
        case "variables":
            PrimaryButton(title: AppLocalization.string("tool.refreshEnv", language: language), symbol: "arrow.clockwise") { execute() }
            Button(loc("variables.exportEnv")) { FilePanels.saveText(draft.input, name: ".env") }
        case "cron":
            PrimaryButton(title: AppLocalization.string("tool.cronPreview", language: language), symbol: "calendar") { execute() }
            Menu(loc("cron.preset")) { ForEach(["*/5 * * * *", "0 9 * * 1-5", "0 0 1 * *"], id: \.self) { expression in Button(expression) { draft.input = expression; execute() } } }
        case "regex":
            PrimaryButton(title: regexModeLabel(draft.mode)) { execute() }
            Button(AppLocalization.string("tool.favorites", language: language), systemImage: "star") { favoritesOpen = true }
        default: PrimaryButton(title: AppLocalization.string("tool.run", language: language)) { execute() }
        }
        Button(AppLocalization.string("tool.example", language: language)) { draft.input = example; if id == "regex" { draft.secondary = #"([\w.]+)@([\w.]+)"# }; if id == "textDiff" { draft.secondary = "MooTool\nNative for macOS\nHello, SwiftUI" } }
        Spacer(minLength: 0)
        Button { draft.input = ""; draft.output = ""; draft.error = nil } label: { Image(systemName: "trash") }.help(AppLocalization.string("tool.clear", language: language))
    }
    private var example: String {
        switch id {
        case "json": return "{\"name\":\"MooTool\",\"native\":true,\"tools\":[\"JSON\",\"HTTP\",\"随手记\"]}"
        case "regex": return "hello@mootool.dev\ncontact@example.com"
        case "cron": return "*/15 * * * *"
        case "calculator": return "sqrt(144) + 2^3 * sin(pi / 2)"
        case "protobuf": return "08 96 01 12 07 4d 6f 6f 54 6f 6f 6c"
        case "ymlProperties":
            let mode = draft.mode
            if mode.hasPrefix("json") || mode.hasPrefix("JSON") { return "{\"app\":{\"name\":\"MooTool\",\"port\":8080}}" }
            if mode.contains("props") || mode.hasPrefix("Properties") { return "app.name=MooTool\napp.port=8080" }
            return "app:\n  name: MooTool\n  port: 8080"
        case "uaParse": return "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        case "timeConvert": return "2026-09-05 12:00:00"
        case "host": return "127.0.0.1 localhost\n::1 localhost\n# Development\n127.0.0.1 api.local"
        case "variables": return "APP_ENV=development\nPORT=8080"
        default: return "Hello, MooTool! 你好，世界。"
        }
    }
    private func execute(_ action: String = "run") {
        if id == "json" { var options = draft.json ?? JSONOptions(); options.showsTree = false; draft.json = options }
        let id = id, flags = regexFlags, lang = language
        store.run(id) { d in
            switch id {
            case "json":
                if action == "query" { return try TextServices.jsonPath(d.input, path: d.option, language: lang) }
                if action == "escape" { return try TextServices.serialize(d.input, pretty: false) }
                if action == "unescape" {
                    guard let text = try JSONSerialization.jsonObject(with: Data(d.input.utf8), options: [.fragmentsAllowed]) as? String else {
                        throw ToolError(AppLocalization.string("json.legacy.unescapeNeedsString", language: lang))
                    }
                    return text
                }
                return try TextServices.json(d.input, pretty: action != "minify", sorted: d.json?.sortKeys ?? true, indent: d.json?.indent ?? 2, language: lang)
            case "encode": return try TextServices.encode(d.input, format: d.mode, decode: action == "decode", language: lang)
            case "crypto":
                switch d.mode {
                case "rsaGen", "RSA 生成密钥对":
                    let pair = try CryptoServices.rsaGenerateKeyPair()
                    return AppLocalization.string("textCrypto.rsaGenOutput", language: lang)
                        .replacingOccurrences(of: "{public}", with: pair.publicKey)
                        .replacingOccurrences(of: "{private}", with: pair.privateKey)
                case "rsaPublicEncrypt", "RSA 公钥加密": return try CryptoServices.rsaEncrypt(d.input, publicKeyBase64: d.secondary)
                case "rsaPrivateDecrypt", "RSA 私钥解密": return try CryptoServices.rsaDecrypt(d.input, privateKeyBase64: d.secondary)
                case "rsaSign", "RSA 签名": return try CryptoServices.rsaSign(d.input, privateKeyBase64: d.secondary)
                case "rsaVerify", "RSA 验签": return try CryptoServices.rsaVerify(d.input, signatureBase64: d.option, publicKeyBase64: d.secondary)
                default:
                    return try TextServices.digest(d.input, algorithm: cryptoEngineAlgorithm(d.mode), key: d.secondary, language: lang)
                }
            case "regex":
                let isReplace = d.mode == "replace" || d.mode == "替换"
                return try TextServices.regex(d.input, pattern: d.secondary, replacement: isReplace ? d.option : nil, flags: flags, language: lang)
            case "timeConvert": return try DeveloperServices.timestamp(d.input, zone: d.option, language: lang)
            case "ymlProperties":
                guard let pair = configConversionPair(d.mode) else { throw ToolError(AppLocalization.string("config.error.pickFormat", language: lang)) }
                return try TextServices.config(d.input, from: pair.0, to: pair.1, language: lang)
            case "protobuf": return try DeveloperServices.protobuf(d.input, base64: d.mode == "Base64", language: lang)
            case "uaParse": return try DeveloperServices.userAgent(d.input)
            case "calculator":
                if calculatorModeIsExpr(d.mode) { var calc = try Calculator(d.input, language: lang); return String(format: "%.15g", try calc.evaluate()) }
                let radix = calculatorInputRadix(d.mode)
                guard let value = Int64(d.input.trimmingCharacters(in: .whitespacesAndNewlines), radix: radix) else {
                    throw ToolError(AppLocalization.string("calculator.error.int64", language: lang))
                }
                return "DEC  \(value)\nHEX  \(String(value, radix: 16).uppercased())\nOCT  \(String(value, radix: 8))\nBIN  \(String(value, radix: 2))"
            case "cron":
                return try CronExpression(d.input, language: lang).next(after: Date(), language: lang).enumerated()
                    .map { "\($0.offset + 1).  \($0.element.formatted(date: .complete, time: .standard))" }.joined(separator: "\n")
            case "variables": return ProcessInfo.processInfo.environment.keys.sorted().map { "\($0)=\(ProcessInfo.processInfo.environment[$0]!)" }.joined(separator: "\n")
            case "host": return try HostWorkspace.validateContent(d.input, language: lang)
            default: throw ToolError(AppLocalization.string("tool.error.unknownTool", language: lang))
            }
        }
    }
    private func jsonOption<Value>(_ keyPath: WritableKeyPath<JSONOptions, Value>) -> Binding<Value> {
        Binding(get: { (draft.json ?? JSONOptions())[keyPath: keyPath] }, set: { value in var options = draft.json ?? JSONOptions(); options[keyPath: keyPath] = value; draft.json = options })
    }
}
