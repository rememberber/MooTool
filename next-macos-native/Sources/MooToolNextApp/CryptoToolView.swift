import SwiftUI
import MooToolNextCore

private enum CryptoTab: String, CaseIterable, Identifiable {
    case symmetric, asymmetric, digest, base, random
    var id: String { rawValue }
    func title(language: AppLanguage) -> String {
        switch self {
        case .symmetric: return AppLocalization.string("crypto.tab.symmetric", language: language)
        case .asymmetric: return AppLocalization.string("crypto.tab.asymmetric", language: language)
        case .digest: return AppLocalization.string("crypto.tab.digest", language: language)
        case .base: return AppLocalization.string("crypto.tab.base", language: language)
        case .random: return AppLocalization.string("crypto.tab.random", language: language)
        }
    }
}

private enum CryptoKeyPairMarker {
    static let publicKey = "MOOTOOL_PUBLIC_KEY"
    static let privateKey = "MOOTOOL_PRIVATE_KEY"
}

struct CryptoToolView: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var historyOpen = false
    @State private var randomUUID = ""
    @State private var randomDigits = ""
    @State private var randomString = ""
    @State private var randomPassword = ""
    @State private var digestFileName = ""
    @State private var randomLength = 16

    private var tab: CryptoTab {
        CryptoTab(rawValue: draft.mode) ?? .digest
    }
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }

    var body: some View {
        ToolPage(tool: Catalog.tool("crypto"), draft: draft, showsHeading: false) {
            Picker(loc("crypto.tabPicker"), selection: Binding(get: { tab }, set: { draft.mode = $0.rawValue })) {
                ForEach(CryptoTab.allCases) { Text($0.title(language: language)).tag($0) }
            }.pickerStyle(.segmented).frame(maxWidth: 520)
            Button { historyOpen = true } label: { Label(AppLocalization.string("workbench.history", language: language), systemImage: "clock.arrow.circlepath") }
            Spacer(minLength: 0)
            Button { clearTab() } label: { Image(systemName: "trash") }.help(loc("crypto.clearTab"))
        } content: {
            Group {
                switch tab {
                case .symmetric: symmetricPanel
                case .asymmetric: asymmetricPanel
                case .digest: digestPanel
                case .base: basePanel
                case .random: randomPanel
                }
            }.frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onAppear(perform: migrateLegacyDraft)
        .sheet(isPresented: $historyOpen) { HistoryView(toolID: "crypto").environment(store).environment(\.appLanguage, language) }
    }

    private let symmetricAlgorithms = ["AES-GCM", "AES-ECB", "DES-ECB", "SM4-ECB"]

    private var symmetricPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Picker(loc("crypto.algorithm"), selection: symmetricAlgorithm) {
                    ForEach(symmetricAlgorithms, id: \.self) { Text($0) }
                }.frame(width: 220)
                if symmetricAlgorithm.wrappedValue == "AES-GCM" {
                    SecureField(loc("crypto.keyHexGCM"), text: $draft.secondary).textFieldStyle(.roundedBorder).frame(maxWidth: 320)
                } else {
                    TextField(loc("crypto.keyUtf8"), text: $draft.secondary).textFieldStyle(.roundedBorder).frame(maxWidth: 320)
                }
            }
            if symmetricAlgorithm.wrappedValue == "AES-GCM" {
                cryptoIO(plainTitle: loc("crypto.plainText"), cipherTitle: loc("crypto.cipherBase64"),
                         encrypt: { symCrypt(encrypt: true) }, decrypt: { symCrypt(encrypt: false) })
            } else {
                cryptoIO(plainTitle: loc("crypto.plainText"), cipherTitle: loc("crypto.cipherHex"),
                         encrypt: { symCrypt(encrypt: true) }, decrypt: { symCrypt(encrypt: false) })
            }
        }
    }

    private var symmetricAlgorithm: Binding<String> {
        Binding(
            get: {
                symmetricAlgorithms.contains(draft.option) ? draft.option : "AES-GCM"
            },
            set: { draft.option = $0 })
    }

    private var asymmetricPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Picker(loc("crypto.algorithm"), selection: $draft.cryptoAsymmetric) {
                    Text("RSA").tag("RSA")
                    Text("SM2").tag("SM2")
                }.frame(width: 140)
                PrimaryButton(title: AppLocalization.string("tool.genKeyPair", language: language), symbol: "key") { generateAsymmetricKeys() }
                Spacer()
            }
            PersistedHSplit(toolID: "crypto", paneIndex: 0, defaultLeading: 280, minLeading: 200, maxLeading: 520) {
                EditorPane(title: draft.cryptoAsymmetric == "SM2" ? loc("crypto.publicKeySm2") : loc("crypto.publicKeyDer"), text: $draft.option, syntax: true)
            } trailing: {
                EditorPane(title: draft.cryptoAsymmetric == "SM2" ? loc("crypto.privateKeySm2") : loc("crypto.privateKeyDer"), text: $draft.secondary, syntax: true)
            }.frame(minHeight: 120, maxHeight: 180)
            cryptoIO(plainTitle: loc("crypto.plainOrSign"), cipherTitle: loc("crypto.cipherOrSignature"),
                     encrypt: { asymmetric(.encrypt) }, decrypt: { asymmetric(.decrypt) })
            HStack(spacing: 8) {
                Button(loc("crypto.sign")) { asymmetric(.sign) }
                Button(loc("crypto.verify")) { asymmetric(.verify) }
                if draft.cryptoAsymmetric == "RSA" {
                    Button(loc("crypto.privateEncrypt")) { asymmetric(.privateEncrypt) }
                    Button(loc("crypto.publicDecrypt")) { asymmetric(.publicDecrypt) }
                }
            }
        }
    }

    private var digestPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Picker(loc("crypto.algorithm"), selection: digestAlgorithm) {
                    ForEach(["MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512", "SM3", "HMAC-SHA256"], id: \.self) { Text($0) }
                }.frame(width: 200)
                if digestAlgorithm.wrappedValue == "HMAC-SHA256" {
                    SecureField(loc("crypto.hmacKey"), text: $draft.secondary).textFieldStyle(.roundedBorder).frame(maxWidth: 220)
                }
                PrimaryButton(title: AppLocalization.string("tool.digestText", language: language), symbol: "play.fill") { digestText() }
                Button(loc("crypto.fileDigest")) { digestFile() }
                if !digestFileName.isEmpty { Text(digestFileName).font(.caption).foregroundStyle(.secondary).lineLimit(1) }
            }
            PersistedHSplit(toolID: "crypto", paneIndex: 1, defaultLeading: 360, minLeading: 240, maxLeading: 720) {
                EditorPane(title: loc("tool.input"), text: $draft.input)
            } trailing: {
                EditorPane(title: loc("crypto.digestResult"), text: $draft.output, editable: false, syntax: true)
            }
        }
    }

    private var basePanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Picker(loc("crypto.algorithm"), selection: baseAlgorithm) {
                ForEach(["Base64", "Base32", "Hex"], id: \.self) { Text($0) }
            }.frame(width: 160)
            cryptoIO(plainTitle: loc("crypto.sourceText"), cipherTitle: baseAlgorithm.wrappedValue,
                     encrypt: { baseCodec(encode: true) }, decrypt: { baseCodec(encode: false) })
        }
    }

    private var randomPanel: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(loc("crypto.length")).foregroundStyle(.secondary)
                TextField("16", value: $randomLength, format: .number).textFieldStyle(.roundedBorder).frame(width: 72)
                Text(loc("crypto.lengthHint")).font(.caption).foregroundStyle(.tertiary)
            }
            randomRow(loc("crypto.random.uuid"), value: randomUUID) { generateRandom(.uuid) }
            randomRow(loc("crypto.random.digits"), value: randomDigits) { generateRandom(.digits) }
            randomRow(loc("crypto.random.string"), value: randomString) { generateRandom(.string) }
            randomRow(loc("crypto.random.password"), value: randomPassword) { generateRandom(.password) }
            randomRow(loc("crypto.random.bytes32"), value: draft.output) { generateRandom(.bytes32) }
        }.padding(.top, 4)
    }

    private func randomRow(_ title: String, value: String, generate: @escaping () -> Void) -> some View {
        HStack(spacing: 10) {
            Text(title).frame(width: 88, alignment: .leading).foregroundStyle(.secondary)
            Text(value.isEmpty ? "—" : value).font(.system(.body, design: .monospaced)).textSelection(.enabled).frame(maxWidth: .infinity, alignment: .leading)
            Button { FilePanels.copy(value) } label: { Image(systemName: "doc.on.doc") }.disabled(value.isEmpty)
            Button(loc("crypto.generate"), action: generate)
        }
    }

    private func cryptoIO(plainTitle: String, cipherTitle: String, encrypt: @escaping () -> Void, decrypt: @escaping () -> Void) -> some View {
        HSplitView {
            EditorPane(title: plainTitle, text: $draft.input)
            VStack(spacing: 10) {
                Button { encrypt() } label: { Label(loc("crypto.encryptForward"), systemImage: "arrow.right") }.buttonStyle(.borderedProminent)
                Button { decrypt() } label: { Label(loc("crypto.decryptBack"), systemImage: "arrow.left") }
            }.frame(width: 110).padding(.vertical, 40)
            EditorPane(title: cipherTitle, text: $draft.output, syntax: true)
        }
    }

    private var digestAlgorithm: Binding<String> {
        Binding(
            get: { draft.option.isEmpty ? "SHA-256" : draft.option },
            set: { draft.option = $0 }
        )
    }

    private var baseAlgorithm: Binding<String> {
        Binding(
            get: { draft.option.isEmpty ? "Base64" : draft.option },
            set: { draft.option = $0 }
        )
    }

    private func migrateLegacyDraft() {
        let digest = Set(["MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512", "SM3", "HMAC-SHA256"])
        if digest.contains(draft.mode) {
            draft.option = draft.mode
            draft.mode = CryptoTab.digest.rawValue
        } else if draft.mode.hasPrefix("AES") || draft.mode.hasPrefix("HMAC") {
            draft.option = draft.mode
            draft.mode = CryptoTab.symmetric.rawValue
        } else if draft.mode.hasPrefix("RSA") {
            draft.option = draft.mode
            draft.mode = CryptoTab.asymmetric.rawValue
        } else if draft.mode == "UUID" || draft.mode.contains("随机") {
            draft.mode = CryptoTab.random.rawValue
        } else if !CryptoTab.allCases.map(\.rawValue).contains(draft.mode) {
            if draft.mode.isEmpty { draft.mode = CryptoTab.digest.rawValue }
            if draft.option.isEmpty { draft.option = "SHA-256" }
        }
        if draft.input.isEmpty && tab != .random { draft.input = "MooTool" }
        if tab == .symmetric {
            if !symmetricAlgorithms.contains(draft.option) { draft.option = "AES-GCM" }
            if draft.secondary.isEmpty { draft.secondary = "1234567890abcdef" }
        }
    }

    private func clearTab() {
        draft.error = nil
        switch tab {
        case .symmetric: draft.input = ""; draft.output = ""
        case .asymmetric: draft.input = ""; draft.output = ""; draft.option = ""; draft.secondary = ""
        case .digest: draft.input = ""; draft.output = ""; digestFileName = ""
        case .base: draft.input = ""; draft.output = ""
        case .random: randomUUID = ""; randomDigits = ""; randomString = ""; randomPassword = ""; draft.output = ""
        }
    }

    private func symCrypt(encrypt: Bool) {
        let algorithm = symmetricAlgorithms.contains(draft.option) ? draft.option : "AES-GCM"
        if algorithm == "AES-GCM" {
            if encrypt {
                store.run("crypto") { d in try TextServices.digest(d.input, algorithm: "AES-GCM 加密", key: d.secondary) }
            } else {
                let cipher = draft.output.isEmpty ? draft.input : draft.output
                store.run("crypto") { d in try TextServices.digest(cipher, algorithm: "AES-GCM 解密", key: d.secondary) }
                Task { @MainActor in
                    await waitForCryptoRun()
                    if !draft.output.isEmpty { draft.input = draft.output }
                }
            }
            return
        }
        guard let legacy = LegacySymmetricCrypto.Algorithm(rawValue: algorithm) else { return }
        if encrypt {
            store.run("crypto") { d in try LegacySymmetricCrypto.encrypt(algorithm: legacy, plaintext: d.input, key: d.secondary) }
        } else {
            let cipher = draft.output.isEmpty ? draft.input : draft.output
            store.run("crypto") { d in try LegacySymmetricCrypto.decrypt(algorithm: legacy, cipherHex: cipher, key: d.secondary) }
            Task { @MainActor in
                await waitForCryptoRun()
                if !draft.output.isEmpty { draft.input = draft.output }
            }
        }
    }

    private enum AsymmetricOp { case encrypt, decrypt, sign, verify, privateEncrypt, publicDecrypt }

    private func asymmetric(_ op: AsymmetricOp) {
        let sm2 = draft.cryptoAsymmetric == "SM2"
        switch op {
        case .encrypt:
            store.run("crypto") {
                if sm2 { return try CryptoServices.sm2Encrypt($0.input, publicKeyBase64: $0.option) }
                return try CryptoServices.rsaEncrypt($0.input, publicKeyBase64: $0.option)
            }
        case .decrypt:
            let cipher = draft.output.isEmpty ? draft.input : draft.output
            store.run("crypto") { d in
                if sm2 { return try CryptoServices.sm2Decrypt(cipher, privateKeyBase64: d.secondary) }
                return try CryptoServices.rsaDecrypt(cipher, privateKeyBase64: d.secondary)
            }
            Task { @MainActor in
                await waitForCryptoRun()
                if !draft.output.isEmpty { draft.input = draft.output }
            }
        case .sign:
            store.run("crypto") {
                if sm2 { return try CryptoServices.sm2Sign($0.input, privateKeyBase64: $0.secondary) }
                return try CryptoServices.rsaSign($0.input, privateKeyBase64: $0.secondary)
            }
        case .verify:
            store.run("crypto") { d in
                if sm2 { return try CryptoServices.sm2Verify(d.input, signatureBase64: d.output, publicKeyBase64: d.option) }
                return try CryptoServices.rsaVerify(d.input, signatureBase64: d.output, publicKeyBase64: d.option)
            }
        case .privateEncrypt:
            store.run("crypto") { try CryptoServices.rsaPrivateEncrypt($0.input, privateKeyBase64: $0.secondary) }
        case .publicDecrypt:
            let cipher = draft.output.isEmpty ? draft.input : draft.output
            store.run("crypto") { d in try CryptoServices.rsaPublicDecrypt(cipher, publicKeyBase64: d.option) }
            Task { @MainActor in
                await waitForCryptoRun()
                if !draft.output.isEmpty { draft.input = draft.output }
            }
        }
    }

    private func generateAsymmetricKeys() {
        let sm2 = draft.cryptoAsymmetric == "SM2"
        store.run("crypto") { _ in
            let pair = sm2 ? try CryptoServices.sm2GenerateKeyPair() : try CryptoServices.rsaGenerateKeyPair()
            return "\(CryptoKeyPairMarker.publicKey):\n\(pair.publicKey)\n\n\(CryptoKeyPairMarker.privateKey):\n\(pair.privateKey)"
        }
        Task { @MainActor in
            for _ in 0..<30 where draft.busy { try? await Task.sleep(for: .milliseconds(100)) }
            parseKeyPair(from: draft.output)
        }
    }

    private func parseKeyPair(from text: String) {
        let lines = text.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        if let pubIndex = lines.firstIndex(where: { $0.hasPrefix(CryptoKeyPairMarker.publicKey) }), pubIndex + 1 < lines.count {
            draft.option = lines[pubIndex + 1].trimmingCharacters(in: .whitespaces)
        }
        if let privIndex = lines.firstIndex(where: { $0.hasPrefix(CryptoKeyPairMarker.privateKey) }), privIndex + 1 < lines.count {
            draft.secondary = lines[privIndex + 1].trimmingCharacters(in: .whitespaces)
        }
    }

    private func digestText() {
        let algorithm = digestAlgorithm.wrappedValue
        store.run("crypto") { d in try TextServices.digest(d.input, algorithm: algorithm, key: d.secondary) }
        digestFileName = ""
    }

    private func digestFile() {
        FilePanels.open(types: [.data, .item]) { urls in
            guard let url = urls.first else { return }
            do {
                let data = try Data(contentsOf: url)
                guard data.count <= 10 * 1024 * 1024 else { throw ToolError(loc("crypto.error.fileTooLarge")) }
                let algorithm = digestAlgorithm.wrappedValue
                let text = String(decoding: data, as: UTF8.self)
                draft.input = text
                digestFileName = url.lastPathComponent
                store.run("crypto") { d in try TextServices.digest(String(decoding: data, as: UTF8.self), algorithm: algorithm, key: d.secondary) }
            } catch { draft.error = error.localizedDescription }
        }
    }

    private func baseCodec(encode: Bool) {
        let format = baseAlgorithm.wrappedValue
        if encode {
            store.run("crypto") { d in try TextServices.encode(d.input, format: format, decode: false) }
        } else {
            let cipher = draft.output.isEmpty ? draft.input : draft.output
            store.run("crypto") { _ in try TextServices.encode(cipher, format: format, decode: true) }
            Task { @MainActor in
                await waitForCryptoRun()
                if !draft.output.isEmpty { draft.input = draft.output }
            }
        }
    }

    private enum RandomKind { case uuid, digits, string, password, bytes32 }

    private func generateRandom(_ kind: RandomKind) {
        let length = max(1, min(4096, randomLength))
        switch kind {
        case .uuid:
            store.run("crypto") { _ in try TextServices.digest("", algorithm: "UUID") }
            Task { @MainActor in
                for _ in 0..<20 where draft.busy { try? await Task.sleep(for: .milliseconds(50)) }
                randomUUID = draft.output
            }
        case .bytes32:
            store.run("crypto") { _ in try TextServices.digest("", algorithm: "随机 32 字节") }
            Task { @MainActor in
                for _ in 0..<20 where draft.busy { try? await Task.sleep(for: .milliseconds(50)) }
            }
        case .digits, .string, .password:
            let value = randomValue(kind: kind, length: length)
            switch kind {
            case .digits: randomDigits = value
            case .string: randomString = value
            case .password: randomPassword = value
            default: break
            }
            draft.status = kind == .password ? loc("crypto.status.randomPassword") : kind == .digits ? loc("crypto.status.randomDigits") : loc("crypto.status.randomString")
            store.record("crypto")
        default: break
        }
    }

    private func waitForCryptoRun() async {
        for _ in 0..<40 where draft.busy { try? await Task.sleep(for: .milliseconds(100)) }
    }

    private func randomValue(kind: RandomKind, length: Int) -> String {
        let digits = "0123456789"
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let symbols = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+"
        let pool = kind == .digits ? digits : kind == .password ? symbols : letters
        var generator = SystemRandomNumberGenerator()
        return String((0..<length).map { _ in pool.randomElement(using: &generator)! })
    }
}
