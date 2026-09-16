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
    @State private var regexFlags = ""
    @State private var favoritesOpen = false
    private var tool: Tool { Catalog.tool(id) }
    var body: some View {
        ToolPage(tool: tool, draft: draft, showsHeading: id != "json") {
            controls
        } content: {
            VStack(spacing: 12) {
                if id == "regex" {
                    HStack { Text("表达式").foregroundStyle(.secondary); TextField("例如 (\\w+)@(\\w+)", text: $draft.secondary).font(.system(.body, design: .monospaced)); TextField("标志 ims", text: $regexFlags).frame(width: 70) }.textFieldStyle(.roundedBorder)
                    if draft.mode == "替换" { TextField("替换模板，例如 $1", text: $draft.option).textFieldStyle(.roundedBorder) }
                }
                if id == "crypto" {
                    if draft.mode.hasPrefix("RSA") && draft.mode != "RSA 生成密钥对" {
                        TextField("RSA 密钥（Base64 DER，与 Electron 导出格式兼容）", text: $draft.secondary).textFieldStyle(.roundedBorder)
                    }
                    if draft.mode == "RSA 验签" {
                        TextField("签名（Base64）", text: $draft.option).textFieldStyle(.roundedBorder)
                    }
                    if draft.mode.contains("AES") || draft.mode.contains("HMAC") {
                        SecureField(draft.mode.contains("AES") ? "Hex 密钥 · 16 / 24 / 32 字节" : "HMAC 密钥（UTF-8）", text: $draft.secondary).textFieldStyle(.roundedBorder)
                    }
                }
                PersistedHSplit(toolID: id, defaultLeading: id == "json" ? 420 : 360, minLeading: id == "json" ? 280 : 200, maxLeading: 900) {
                    EditorPane(title: inputTitle, text: $draft.input, syntax: id == "json" || id == "ymlProperties", persistence: id == "json" ? store.editorPersistence(id) : nil)
                } trailing: {
                    if id == "json" && draft.json?.showsTree == true {
                        JSONTreePane(text: draft.input, path: $draft.option)
                    } else {
                        EditorPane(title: "结果", text: $draft.output, editable: false, syntax: ["json", "uaParse", "ymlProperties", "protobuf", "regex"].contains(id), persistence: id == "json" ? store.editorPersistence(id, output: true) : nil)
                    }
                }
            }
        }.onAppear { if !modes.isEmpty && !modes.contains(draft.mode) { draft.mode = modes[0] }; if id == "timeConvert" && draft.option.isEmpty { draft.option = TimeZone.current.identifier } }
        .sheet(isPresented: $favoritesOpen) {
            ToolFavoritesSheet(kind: .regex, currentValue: draft.secondary) { pattern in
                draft.secondary = pattern
            }.environment(store)
        }
    }
    private var inputTitle: String {
        if id == "variables" { return "运行变量 · KEY=value（仅用于原生版代码运行）" }
        return id == "host" ? "Hosts 配置草稿" : "输入"
    }
    private var modes: [String] {
        switch id {
        case "encode": return ["Base64", "Base32", "URL", "Hex", "Unicode", "HTML"]
        case "crypto": return ["SHA-256", "SHA-384", "SHA-512", "MD5", "SHA-1", "SM3", "HMAC-SHA256", "AES-GCM 加密", "AES-GCM 解密", "RSA 生成密钥对", "RSA 公钥加密", "RSA 私钥解密", "RSA 签名", "RSA 验签", "UUID", "随机 32 字节"]
        case "regex": return ["匹配", "替换"]
        case "ymlProperties": return ["YAML → JSON", "JSON → YAML", "YAML → Properties", "Properties → YAML", "JSON → Properties", "Properties → JSON"]
        case "protobuf": return ["Hex", "Base64"]
        case "calculator": return ["表达式", "十进制 → 其他进制", "十六进制 → 十进制", "二进制 → 十进制"]
        default: return []
        }
    }
    @ViewBuilder private var controls: some View {
        if !modes.isEmpty { Picker("模式", selection: $draft.mode) { ForEach(modes, id: \.self) { Text($0) } }.labelsHidden().frame(width: id == "crypto" ? 200 : id == "ymlProperties" ? 185 : 145) }
        switch id {
        case "json":
            PrimaryButton(title: "格式化", symbol: "text.alignleft") { execute("format") }
            Button("压缩") { execute("minify") }
            Menu {
                Picker("缩进", selection: jsonOption(\.indent)) { Text("2 个空格").tag(2); Text("4 个空格").tag(4) }
                Toggle("按键名排序", isOn: jsonOption(\.sortKeys))
                Divider()
                Button("转义为 JSON 字符串") { execute("escape") }
                Button("还原 JSON 字符串") { execute("unescape") }
                Button("交换输入与结果") { swap(&draft.input, &draft.output) }
            } label: { Image(systemName: "slider.horizontal.3") }.help("格式与转换选项")
            Toggle(isOn: jsonOption(\.showsTree)) { Image(systemName: "list.bullet.indent") }.toggleStyle(.button).help("显示 JSON 结构树")
            TextField("$.key[0] 或 /key/0", text: $draft.option).textFieldStyle(.roundedBorder).frame(minWidth: 130, idealWidth: 180, maxWidth: 230)
            Button("查询") { execute("query") }
        case "encode":
            PrimaryButton(title: "编码") { execute("encode") }; Button("解码") { execute("decode") }
            Button { swap(&draft.input, &draft.output) } label: { Image(systemName: "arrow.left.arrow.right") }.help("交换输入和结果")
        case "timeConvert":
            PrimaryButton(title: "转换") { execute() }
            Button("此刻") { draft.input = String(Int64(Date().timeIntervalSince1970 * 1000)); execute() }
            Picker("时区", selection: $draft.option) {
                ForEach(Array(Set([TimeZone.current.identifier, "UTC", "Asia/Shanghai", "Asia/Tokyo", "America/New_York", "America/Los_Angeles", "Europe/London"])).sorted(), id: \.self) { Text($0) }
            }.frame(width: 240)
        case "variables":
            PrimaryButton(title: "查看当前环境", symbol: "arrow.clockwise") { execute() }
            Button("导出 .env") { FilePanels.saveText(draft.input, name: ".env") }
        case "cron":
            PrimaryButton(title: "预览执行时间", symbol: "calendar") { execute() }
            Menu("常用表达式") { ForEach(["*/5 * * * *", "0 9 * * 1-5", "0 0 1 * *"], id: \.self) { expression in Button(expression) { draft.input = expression; execute() } } }
        case "regex":
            PrimaryButton(title: draft.mode) { execute() }
            Button("收藏", systemImage: "star") { favoritesOpen = true }
        default: PrimaryButton(title: "运行") { execute() }
        }
        Button("示例") { draft.input = example; if id == "regex" { draft.secondary = #"([\w.]+)@([\w.]+)"# }; if id == "textDiff" { draft.secondary = "MooTool\nNative for macOS\nHello, SwiftUI" } }
        Spacer(minLength: 0)
        Button { draft.input = ""; draft.output = ""; draft.error = nil } label: { Image(systemName: "trash") }.help("清空输入和结果")
    }
    private var example: String {
        switch id {
        case "json": return "{\"name\":\"MooTool\",\"native\":true,\"tools\":[\"JSON\",\"HTTP\",\"随手记\"]}"
        case "regex": return "hello@mootool.dev\ncontact@example.com"
        case "cron": return "*/15 * * * *"
        case "calculator": return "sqrt(144) + 2^3 * sin(pi / 2)"
        case "protobuf": return "08 96 01 12 07 4d 6f 6f 54 6f 6f 6c"
        case "ymlProperties": return draft.mode.hasPrefix("JSON") ? "{\"app\":{\"name\":\"MooTool\",\"port\":8080}}" : draft.mode.hasPrefix("Properties") ? "app.name=MooTool\napp.port=8080" : "app:\n  name: MooTool\n  port: 8080"
        case "uaParse": return "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        case "timeConvert": return "2026-09-05 12:00:00"
        case "host": return "127.0.0.1 localhost\n::1 localhost\n# Development\n127.0.0.1 api.local"
        case "variables": return "APP_ENV=development\nPORT=8080"
        default: return "Hello, MooTool! 你好，世界。"
        }
    }
    private func execute(_ action: String = "run") {
        if id == "json" { var options = draft.json ?? JSONOptions(); options.showsTree = false; draft.json = options }
        let id = id, flags = regexFlags
        store.run(id) { d in
            switch id {
            case "json":
                if action == "query" { return try TextServices.jsonPath(d.input, path: d.option) }
                if action == "escape" { return try TextServices.serialize(d.input, pretty: false) }
                if action == "unescape" {
                    guard let text = try JSONSerialization.jsonObject(with: Data(d.input.utf8), options: [.fragmentsAllowed]) as? String else { throw ToolError("输入需为 JSON 字符串，例如 \"hello\\nworld\"。") }; return text
                }
                return try TextServices.json(d.input, pretty: action != "minify", sorted: d.json?.sortKeys ?? true, indent: d.json?.indent ?? 2)
            case "encode": return try TextServices.encode(d.input, format: d.mode, decode: action == "decode")
            case "crypto":
                switch d.mode {
                case "RSA 生成密钥对":
                    let pair = try CryptoServices.rsaGenerateKeyPair()
                    return "公钥 (Base64 DER):\n\(pair.publicKey)\n\n私钥 (Base64 DER):\n\(pair.privateKey)"
                case "RSA 公钥加密": return try CryptoServices.rsaEncrypt(d.input, publicKeyBase64: d.secondary)
                case "RSA 私钥解密": return try CryptoServices.rsaDecrypt(d.input, privateKeyBase64: d.secondary)
                case "RSA 签名": return try CryptoServices.rsaSign(d.input, privateKeyBase64: d.secondary)
                case "RSA 验签": return try CryptoServices.rsaVerify(d.input, signatureBase64: d.option, publicKeyBase64: d.secondary)
                default: return try TextServices.digest(d.input, algorithm: d.mode, key: d.secondary)
                }
            case "regex": return try TextServices.regex(d.input, pattern: d.secondary, replacement: d.mode == "替换" ? d.option : nil, flags: flags)
            case "timeConvert": return try DeveloperServices.timestamp(d.input, zone: d.option)
            case "ymlProperties": let pair = d.mode.components(separatedBy: " → "); guard pair.count == 2 else { throw ToolError("请选择转换格式。") }; return try TextServices.config(d.input, from: pair[0], to: pair[1])
            case "protobuf": return try DeveloperServices.protobuf(d.input, base64: d.mode == "Base64")
            case "uaParse": return try DeveloperServices.userAgent(d.input)
            case "calculator":
                if d.mode == "表达式" { var calc = try Calculator(d.input); return String(format: "%.15g", try calc.evaluate()) }
                let radix = d.mode.hasPrefix("十六") ? 16 : d.mode.hasPrefix("二进") ? 2 : 10
                guard let value = Int64(d.input.trimmingCharacters(in: .whitespacesAndNewlines), radix: radix) else { throw ToolError("请输入有效的 64 位整数。") }
                return "DEC  \(value)\nHEX  \(String(value, radix: 16).uppercased())\nOCT  \(String(value, radix: 8))\nBIN  \(String(value, radix: 2))"
            case "cron": return try CronExpression(d.input).next(after: Date()).enumerated().map { "\($0.offset + 1).  \($0.element.formatted(date: .complete, time: .standard))" }.joined(separator: "\n")
            case "variables": return ProcessInfo.processInfo.environment.keys.sorted().map { "\($0)=\(ProcessInfo.processInfo.environment[$0]!)" }.joined(separator: "\n")
            case "host": return try HostWorkspace.validateContent(d.input)
            default: throw ToolError("没有找到此工具。")
            }
        }
    }
    private func jsonOption<Value>(_ keyPath: WritableKeyPath<JSONOptions, Value>) -> Binding<Value> {
        Binding(get: { (draft.json ?? JSONOptions())[keyPath: keyPath] }, set: { value in var options = draft.json ?? JSONOptions(); options[keyPath: keyPath] = value; draft.json = options })
    }
}
