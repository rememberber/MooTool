import SwiftUI
import CoreImage.CIFilterBuiltins
import Vision
import PDFKit
import ImageIO
import UniformTypeIdentifiers
import MooToolNextCore

struct QRTool: View {
    @Bindable var draft: ToolDraft
    @Environment(\.appLanguage) private var language
    @State private var image: NSImage?
    @State private var png: Data?
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    var body: some View {
        ToolPage(tool: Catalog.tool("qrCode"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.generateQr", language: language), symbol: "qrcode", action: generate)
            Picker(loc("qr.correction"), selection: $draft.mode) { ForEach(["L", "M", "Q", "H"], id: \.self) { Text($0) } }.frame(width: 120)
            Button(loc("qr.savePng")) { if let png { FilePanels.save(png, name: "qrcode.png") } }.disabled(png == nil)
            Button(loc("qr.recognizeImage")) { FilePanels.open(types: [.image]) { recognize($0[0]) } }
        } content: {
            PersistedHSplit(toolID: "qrCode", defaultLeading: 360, minLeading: 260, maxLeading: 640) {
                VStack(spacing: 12) {
                    EditorPane(title: loc("tool.input"), text: $draft.input)
                    if !draft.output.isEmpty { EditorPane(title: loc("qr.recognizeResult"), text: $draft.output, editable: false) }
                }
            } trailing: {
                VStack(spacing: 18) {
                    Spacer()
                    if let image { Image(nsImage: image).interpolation(.none).resizable().scaledToFit().frame(maxWidth: 320, maxHeight: 320).padding(24).background(.white, in: RoundedRectangle(cornerRadius: 16)) }
                    else { ContentUnavailableView(loc("qr.empty.title"), systemImage: "qrcode", description: Text(loc("qr.empty.description"))) }
                    Spacer()
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "M" }; if !draft.input.isEmpty { generate() } }
    }
    private func generate() {
        do {
            let bytes = Data(draft.input.utf8)
            guard !bytes.isEmpty, bytes.count <= 2953 else { throw ToolError(loc("qr.error.emptyOrCapacity")) }
            let filter = CIFilter.qrCodeGenerator(); filter.message = bytes; filter.correctionLevel = draft.mode
            guard let output = filter.outputImage else { throw ToolError(loc("qr.error.overCapacity")) }
            let scaled = output.transformed(by: CGAffineTransform(scaleX: 8, y: 8))
            guard let cg = CIContext().createCGImage(scaled, from: scaled.extent) else { throw ToolError(loc("qr.error.generateFailed")) }
            // Include the four-module quiet zone in the exported image, not only in the preview.
            let size = cg.width + 64
            guard let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { throw ToolError(loc("qr.error.createImage")) }
            context.setFillColor(NSColor.white.cgColor); context.fill(CGRect(x: 0, y: 0, width: size, height: size)); context.interpolationQuality = .none
            context.draw(cg, in: CGRect(x: 32, y: 32, width: cg.width, height: cg.height))
            let rendered = context.makeImage()!
            png = NSBitmapImageRep(cgImage: rendered).representation(using: .png, properties: [:])
            image = NSImage(cgImage: rendered, size: NSSize(width: size, height: size))
            draft.error = nil
            draft.status = locf("qr.status.bytes", bytes.count, draft.mode)
        } catch { draft.error = error.localizedDescription; image = nil; png = nil }
    }
    private func recognize(_ url: URL) {
        draft.busy = true; draft.error = nil
        let lang = language
        Task {
            defer { draft.busy = false }
            do {
                let texts = try await Task.detached {
                    let request = VNDetectBarcodesRequest(); request.symbologies = [.qr]
                    try VNImageRequestHandler(url: url).perform([request])
                    return (request.results ?? []).compactMap(\.payloadStringValue)
                }.value
                guard !texts.isEmpty else { throw ToolError(AppLocalization.string("qr.error.noBarcode", language: lang)) }
                draft.output = texts.joined(separator: "\n\n")
                draft.status = locf("qr.status.found", texts.count)
            } catch { draft.error = error.localizedDescription }
        }
    }
}

struct ColorTool: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    @State private var color = Color(red: 0.31, green: 0.51, blue: 0.80)
    @State private var sampler: NSColorSampler?
    @State private var favoritesOpen = false
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    var body: some View {
        ToolPage(tool: Catalog.tool("colorBoard"), draft: draft) {
            ColorPicker(AppLocalization.string("color.choose", language: language), selection: $color, supportsOpacity: false).frame(width: 160)
            Button(AppLocalization.string("color.screenPick", language: language)) { let sampler = NSColorSampler(); self.sampler = sampler; sampler.show { if let value = $0 { color = Color(nsColor: value); update() }; self.sampler = nil } }
            TextField("#4F83CC", text: $draft.input).textFieldStyle(.roundedBorder).frame(width: 120).onSubmit(parse)
            Button(AppLocalization.string("color.applyHex", language: language), action: parse)
            Button(AppLocalization.string("tool.favorites", language: language), systemImage: "star") { favoritesOpen = true }
        } content: {
            PersistedHSplit(toolID: "colorBoard", defaultLeading: 320, minLeading: 220, maxLeading: 560) {
                RoundedRectangle(cornerRadius: 18).fill(color).overlay {
                    VStack(spacing: 10) { Text(draft.input.uppercased()).font(.system(size: 34, weight: .medium, design: .monospaced)); Text(loc("color.preview.title")).font(.title3) }.foregroundStyle(contrastColor)
                }.padding(24)
            } trailing: {
                EditorPane(title: AppLocalization.string("color.values", language: language), text: $draft.output, editable: false)
            }
        }.onChange(of: color) { update() }.onAppear { if draft.input.isEmpty { update() } else { parse() } }
        .sheet(isPresented: $favoritesOpen) {
            ToolFavoritesSheet(kind: .color, currentValue: draft.input) { hex in
                draft.input = hex
                parse()
            }.environment(store)
        }
    }
    private var contrastColor: Color {
        let c = NSColor(color).usingColorSpace(.sRGB) ?? .black
        return c.redComponent * 0.299 + c.greenComponent * 0.587 + c.blueComponent * 0.114 > 0.6 ? .black : .white
    }
    private func parse() {
        let text = draft.input.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "#", with: "")
        guard text.count == 6, let value = UInt32(text, radix: 16) else {
            draft.error = AppLocalization.string("color.hexInvalid", language: language)
            return
        }
        color = Color(red: Double((value >> 16) & 255) / 255, green: Double((value >> 8) & 255) / 255, blue: Double(value & 255) / 255); update()
    }
    private func update() {
        guard let c = NSColor(color).usingColorSpace(.sRGB) else { return }
        let r = c.redComponent, g = c.greenComponent, b = c.blueComponent
        let high = max(r, g, b), low = min(r, g, b), delta = high - low, light = (high + low) / 2
        var hue = 0.0
        if delta > 0 { hue = high == r ? ((g - b) / delta).truncatingRemainder(dividingBy: 6) : high == g ? (b - r) / delta + 2 : (r - g) / delta + 4; hue *= 60; if hue < 0 { hue += 360 } }
        let saturation = delta == 0 ? 0 : delta / (1 - abs(2 * light - 1))
        draft.input = String(format: "#%02X%02X%02X", Int((r * 255).rounded()), Int((g * 255).rounded()), Int((b * 255).rounded()))
        let ri = Int((r * 255).rounded()), gi = Int((g * 255).rounded()), bi = Int((b * 255).rounded())
        draft.output = "\(loc("color.label.hex"))  \(draft.input)\n\n\(loc("color.label.rgb"))  \(ri), \(gi), \(bi)\n\n"
            + String(format: "\(loc("color.label.hsl"))  %.0f°, %.1f%%, %.1f%%\n\n\(loc("color.label.swiftUI"))\nColor(red: %.3f, green: %.3f, blue: %.3f)", hue, saturation * 100, light * 100, r, g, b)
        draft.error = nil
    }
}

struct ImageTool: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    @State private var original: CGImage?
    @State private var preview: NSImage?
    @State private var sourcePath = ""
    @State private var width = ""
    @State private var quality = 0.85
    @State private var watermark = ""
    var body: some View {
        ToolPage(tool: Catalog.tool("image"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.openImage", language: language), symbol: "folder") { FilePanels.open(types: [.image]) { load($0[0]) } }
            Button(loc("image.screenshot")) {
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".png")
                Task {
                    defer { try? FileManager.default.removeItem(at: url) }
                    do { _ = try await ProcessRunner.run(executable: "/usr/sbin/screencapture", arguments: ["-i", "-x", url.path], timeout: 120); if FileManager.default.fileExists(atPath: url.path) { load(url) } else { draft.error = loc("image.error.screenshotFailed") } } catch { draft.error = loc("image.error.screenshotFailed") }
                }
            }
            Picker(loc("image.format"), selection: $draft.mode) { Text("PNG").tag("PNG"); Text("JPEG").tag("JPEG"); Text("TIFF").tag("TIFF") }.frame(width: 135)
            TextField(loc("image.widthPlaceholder"), text: $width).textFieldStyle(.roundedBorder).frame(width: 90).onChange(of: width) { persistMedia() }
            Button(loc("image.export"), action: export).disabled(original == nil)
        } content: {
            PersistedHSplit(toolID: "image", defaultLeading: 360, minLeading: 240, maxLeading: 720) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(.quaternary.opacity(0.3))
                    if let preview { Image(nsImage: preview).resizable().scaledToFit().padding(24) }
                    else { ContentUnavailableView(loc("image.empty.title"), systemImage: "photo.on.rectangle.angled", description: Text(loc("image.empty.description"))) }
                }.frame(maxWidth: .infinity, maxHeight: .infinity).dropDestination(for: URL.self) { urls, _ in if let url = urls.first { load(url); return true }; return false }
            } trailing: {
                VStack(alignment: .leading, spacing: 14) {
                    GroupBox(loc("image.exportGroup")) {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text(loc("image.quality")).font(.caption)
                                Slider(value: $quality, in: 0.1...1).disabled(draft.mode != "JPEG").onChange(of: quality) { persistMedia() }
                                Text("\(Int(quality * 100))%").font(.caption).monospacedDigit().frame(width: 36, alignment: .trailing)
                            }
                            TextField(loc("image.watermarkPlaceholder"), text: $watermark).textFieldStyle(.roundedBorder).onChange(of: watermark) { persistMedia() }
                        }.padding(4)
                    }
                    if !draft.status.isEmpty {
                        Text(draft.status).font(.caption).foregroundStyle(.secondary).textSelection(.enabled)
                    }
                    Spacer(minLength: 0)
                }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading).padding(.vertical, 4)
            }
        }.onAppear {
            if draft.mode.isEmpty { draft.mode = "PNG" }
            restoreMedia()
        }
    }
    private func restoreMedia() {
        guard let media = draft.media else { return }
        if !media.exportWidth.isEmpty { width = media.exportWidth }
        quality = media.jpegQuality
        watermark = media.watermark
        if let path = media.filePaths.first, path != sourcePath, FileManager.default.fileExists(atPath: path) {
            load(URL(fileURLWithPath: path), persist: false)
        }
    }
    private func persistMedia() {
        var state = draft.media ?? MediaWorkspaceState()
        state.exportWidth = width
        state.jpegQuality = quality
        state.watermark = watermark
        state.filePaths = sourcePath.isEmpty ? [] : [sourcePath]
        draft.media = state
        store.scheduleSave()
    }
    private func load(_ url: URL, persist: Bool = true) {
        do {
            guard let source = CGImageSourceCreateWithURL(url as CFURL, nil), let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
                  let w = properties[kCGImagePropertyPixelWidth] as? Int, let h = properties[kCGImagePropertyPixelHeight] as? Int, Double(w) * Double(h) <= 80_000_000,
                  let cg = CGImageSourceCreateImageAtIndex(source, 0, nil) else { throw ToolError(loc("image.error.load")) }
            original = cg; preview = NSImage(cgImage: cg, size: NSSize(width: cg.width, height: cg.height)); width = String(cg.width)
            sourcePath = url.path
            draft.status = "\(url.lastPathComponent) · \(cg.width) × \(cg.height) px"; draft.error = nil
            if persist { persistMedia() }
        } catch { draft.error = error.localizedDescription }
    }
    private func export() {
        guard let original else { return }
        do {
            guard let w = Int(width), (1...16_384).contains(w) else { throw ToolError(loc("image.error.exportWidth")) }
            let h = max(1, Int(Double(w) * Double(original.height) / Double(original.width)))
            guard Double(w) * Double(h) <= 80_000_000 else { throw ToolError(loc("image.error.exportTooLarge")) }
            guard let context = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { throw ToolError(loc("image.error.canvas")) }
            if draft.mode == "JPEG" { context.setFillColor(NSColor.white.cgColor); context.fill(CGRect(x: 0, y: 0, width: w, height: h)) }
            context.interpolationQuality = .high; context.draw(original, in: CGRect(x: 0, y: 0, width: w, height: h))
            if !watermark.isEmpty {
                NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
                let font = NSFont.systemFont(ofSize: max(12, CGFloat(w) / 35), weight: .semibold)
                let shadow = NSShadow(); shadow.shadowColor = NSColor.black.withAlphaComponent(0.6); shadow.shadowBlurRadius = 3
                (watermark as NSString).draw(at: NSPoint(x: CGFloat(w) * 0.035, y: CGFloat(h) * 0.04), withAttributes: [.font: font, .foregroundColor: NSColor.white, .shadow: shadow])
                NSGraphicsContext.restoreGraphicsState()
            }
            guard let cg = context.makeImage(), let data = NSBitmapImageRep(cgImage: cg).representation(using: draft.mode == "JPEG" ? .jpeg : draft.mode == "TIFF" ? .tiff : .png, properties: [.compressionFactor: quality]) else { throw ToolError(loc("image.error.exportFailed")) }
            FilePanels.save(data, name: "MooTool-image." + (draft.mode == "JPEG" ? "jpg" : draft.mode.lowercased()))
        } catch { draft.error = error.localizedDescription }
    }
}

struct NativePDFView: NSViewRepresentable {
    let document: PDFDocument?
    func makeNSView(context: Context) -> PDFView { let view = PDFView(); view.autoScales = true; view.displayMode = .singlePageContinuous; return view }
    func updateNSView(_ view: PDFView, context: Context) { if view.document !== document { view.document = document } }
}
struct PDFTool: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @Environment(\.appLanguage) private var language
    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func locf(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: AppLocalization.string(key, language: language), arguments: arguments)
    }
    @State private var files: [URL] = []
    @State private var documents: [PDFDocument] = []
    @State private var combined: PDFDocument?
    @State private var showText = false
    var body: some View {
        ToolPage(tool: Catalog.tool("pdf"), draft: draft) {
            PrimaryButton(title: AppLocalization.string("tool.addPdf", language: language), symbol: "plus") { FilePanels.open(types: [.pdf], multiple: true) { load($0) } }
            TextField(loc("pdf.pagesPlaceholder"), text: $draft.option).textFieldStyle(.roundedBorder).frame(minWidth: 180, maxWidth: 270)
            Button(loc("pdf.export"), action: export).disabled(combined == nil)
            Button(showText ? loc("pdf.preview") : loc("pdf.extractText")) {
                draft.output = combined?.string ?? ""
                showText.toggle()
                persistMedia()
            }.disabled(combined == nil)
        } content: {
            PersistedHSplit(toolID: "pdf", defaultLeading: 200, minLeading: 160, maxLeading: 320) {
                VStack(alignment: .leading) {
                    Text(loc("pdf.mergeOrder")).font(.caption).foregroundStyle(.secondary).padding(.horizontal, 10)
                    List(Array(files.enumerated()), id: \.offset) { index, url in
                        VStack(alignment: .leading, spacing: 7) {
                            Text(url.lastPathComponent).font(.caption).lineLimit(2)
                            HStack { Text(locf("pdf.pageCount", documents[index].pageCount)).foregroundStyle(.secondary); Spacer()
                                Button { if index > 0 { files.swapAt(index, index - 1); documents.swapAt(index, index - 1); rebuild() } } label: { Image(systemName: "arrow.up") }.disabled(index == 0)
                                Button { files.remove(at: index); documents.remove(at: index); rebuild() } label: { Image(systemName: "xmark") }
                            }.font(.caption).buttonStyle(.borderless)
                        }.padding(.vertical, 6)
                    }.listStyle(.inset)
                }
            } trailing: {
                Group {
                    if showText { EditorPane(title: loc("pdf.textTitle"), text: $draft.output, editable: false) }
                    else { NativePDFView(document: combined).overlay { if combined == nil { ContentUnavailableView(loc("pdf.empty.title"), systemImage: "doc.richtext", description: Text(loc("pdf.empty.description"))) } } }
                }.frame(minWidth: 300)
            }.dropDestination(for: URL.self) { urls, _ in appendURLs(urls, persist: true); return true }
        }.onAppear { restoreMedia() }
    }
    private func restoreMedia() {
        guard let media = draft.media else { return }
        showText = media.pdfShowText
        let paths = media.filePaths.filter { FileManager.default.fileExists(atPath: $0) }
        guard !paths.isEmpty, files.isEmpty else { return }
        appendURLs(paths.map { URL(fileURLWithPath: $0) }, persist: false)
    }
    private func persistMedia() {
        var state = draft.media ?? MediaWorkspaceState()
        state.filePaths = files.map(\.path)
        state.pdfShowText = showText
        draft.media = state
        store.scheduleSave()
    }
    private func load(_ urls: [URL]) {
        appendURLs(urls, persist: true)
    }
    private func appendURLs(_ urls: [URL], persist: Bool) {
        do {
            var loaded: [PDFDocument] = []
            for url in urls {
                guard let document = PDFDocument(url: url), !document.isLocked, document.pageCount > 0 else { throw ToolError(String(format: loc("pdf.error.load"), url.lastPathComponent)) }; loaded.append(document)
            }
            files += urls; documents += loaded; rebuild(); draft.error = nil
            if persist { persistMedia() }
        } catch { draft.error = error.localizedDescription }
    }
    private func rebuild() {
        guard !documents.isEmpty else { combined = nil; persistMedia(); return }
        let merged = PDFDocument()
        for document in documents { for index in 0..<document.pageCount { if let page = document.page(at: index)?.copy() as? PDFPage { merged.insert(page, at: merged.pageCount) } } }
        combined = merged; draft.status = locf("pdf.status.summary", documents.count, merged.pageCount)
        persistMedia()
    }
    private func export() {
        guard let combined else { return }
        do {
            let result = PDFDocument()
            for index in try pageIndices(draft.option, count: combined.pageCount) { if let page = combined.page(at: index)?.copy() as? PDFPage { result.insert(page, at: result.pageCount) } }
            guard let data = result.dataRepresentation() else { throw ToolError(loc("pdf.error.exportFailed")) }; FilePanels.save(data, name: "MooTool-document.pdf")
        } catch { draft.error = error.localizedDescription }
    }
    private func pageIndices(_ text: String, count: Int) throws -> [Int] {
        if text.trimmingCharacters(in: .whitespaces).isEmpty { return Array(0..<count) }
        var result: [Int] = []
        for part in text.split(separator: ",", omittingEmptySubsequences: false) {
            let bounds = part.trimmingCharacters(in: .whitespaces).split(separator: "-", omittingEmptySubsequences: false)
            guard (1...2).contains(bounds.count), let start = Int(bounds[0]), let end = Int(bounds.last!), start > 0, end >= start, end <= count else {
                throw ToolError(locf("pdf.error.pageRange", count))
            }
            result += (start...end).map { $0 - 1 }
        }
        return result
    }
}
