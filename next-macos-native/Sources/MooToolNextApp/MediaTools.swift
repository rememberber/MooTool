import SwiftUI
import CoreImage.CIFilterBuiltins
import Vision
import PDFKit
import ImageIO
import UniformTypeIdentifiers
import MooToolNextCore

struct QRTool: View {
    @Bindable var draft: ToolDraft
    @State private var image: NSImage?
    @State private var png: Data?
    var body: some View {
        ToolPage(tool: Catalog.tool("qrCode"), draft: draft) {
            PrimaryButton(title: "生成二维码", symbol: "qrcode", action: generate)
            Picker("纠错", selection: $draft.mode) { ForEach(["L", "M", "Q", "H"], id: \.self) { Text($0) } }.frame(width: 120)
            Button("保存 PNG") { if let png { FilePanels.save(png, name: "qrcode.png") } }.disabled(png == nil)
            Button("识别图片…") { FilePanels.open(types: [.image]) { recognize($0[0]) } }
        } content: {
            HSplitView {
                VStack(spacing: 12) { EditorPane(title: "内容", text: $draft.input); if !draft.output.isEmpty { EditorPane(title: "识别结果", text: $draft.output, editable: false) } }
                VStack(spacing: 18) {
                    Spacer()
                    if let image { Image(nsImage: image).interpolation(.none).resizable().scaledToFit().frame(maxWidth: 320, maxHeight: 320).padding(24).background(.white, in: RoundedRectangle(cornerRadius: 16)) }
                    else { ContentUnavailableView("生成你的二维码", systemImage: "qrcode", description: Text("支持文本、网址和 Wi-Fi 配置信息")) }
                    Spacer()
                }.frame(minWidth: 260, maxWidth: .infinity, maxHeight: .infinity)
            }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "M" }; if !draft.input.isEmpty { generate() } }
    }
    private func generate() {
        do {
            let bytes = Data(draft.input.utf8)
            guard !bytes.isEmpty, bytes.count <= 2953 else { throw ToolError("二维码内容为空或超过容量。") }
            let filter = CIFilter.qrCodeGenerator(); filter.message = bytes; filter.correctionLevel = draft.mode
            guard let output = filter.outputImage else { throw ToolError("内容超过当前纠错级别的二维码容量。") }
            let scaled = output.transformed(by: CGAffineTransform(scaleX: 8, y: 8))
            guard let cg = CIContext().createCGImage(scaled, from: scaled.extent) else { throw ToolError("二维码生成失败。") }
            // Include the four-module quiet zone in the exported image, not only in the preview.
            let size = cg.width + 64
            guard let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { throw ToolError("无法创建图片。") }
            context.setFillColor(NSColor.white.cgColor); context.fill(CGRect(x: 0, y: 0, width: size, height: size)); context.interpolationQuality = .none
            context.draw(cg, in: CGRect(x: 32, y: 32, width: cg.width, height: cg.height))
            let rendered = context.makeImage()!
            png = NSBitmapImageRep(cgImage: rendered).representation(using: .png, properties: [:])
            image = NSImage(cgImage: rendered, size: NSSize(width: size, height: size)); draft.error = nil; draft.status = "\(bytes.count) bytes · 纠错 \(draft.mode)"
        } catch { draft.error = error.localizedDescription; image = nil; png = nil }
    }
    private func recognize(_ url: URL) {
        draft.busy = true; draft.error = nil
        Task {
            defer { draft.busy = false }
            do {
                let texts = try await Task.detached {
                    let request = VNDetectBarcodesRequest(); request.symbologies = [.qr]
                    try VNImageRequestHandler(url: url).perform([request])
                    return (request.results ?? []).compactMap(\.payloadStringValue)
                }.value
                guard !texts.isEmpty else { throw ToolError("图片中没有识别到二维码。") }
                draft.output = texts.joined(separator: "\n\n"); draft.status = "识别到 \(texts.count) 个二维码"
            } catch { draft.error = error.localizedDescription }
        }
    }
}

struct ColorTool: View {
    @Bindable var draft: ToolDraft
    @Environment(AppStore.self) private var store
    @State private var color = Color(red: 0.31, green: 0.51, blue: 0.80)
    @State private var sampler: NSColorSampler?
    var body: some View {
        ToolPage(tool: Catalog.tool("colorBoard"), draft: draft) {
            ColorPicker("选择颜色", selection: $color, supportsOpacity: false).frame(width: 160)
            Button("屏幕取色") { let sampler = NSColorSampler(); self.sampler = sampler; sampler.show { if let value = $0 { color = Color(nsColor: value); update() }; self.sampler = nil } }
            TextField("#4F83CC", text: $draft.input).textFieldStyle(.roundedBorder).frame(width: 120).onSubmit(parse)
            Button("应用 HEX", action: parse)
            Button("收藏颜色") { store.record("colorBoard"); if let index = store.history.firstIndex(where: { $0.toolID == "colorBoard" }) { store.history[index].favorite = true; store.scheduleSave() } }
        } content: {
            HSplitView {
                RoundedRectangle(cornerRadius: 18).fill(color).overlay {
                    VStack(spacing: 10) { Text(draft.input.uppercased()).font(.system(size: 34, weight: .medium, design: .monospaced)); Text("MooTool Color").font(.title3) }.foregroundStyle(contrastColor)
                }.padding(24).frame(minWidth: 260)
                EditorPane(title: "颜色值", text: $draft.output, editable: false)
            }
        }.onChange(of: color) { update() }.onAppear { if draft.input.isEmpty { update() } else { parse() } }
    }
    private var contrastColor: Color {
        let c = NSColor(color).usingColorSpace(.sRGB) ?? .black
        return c.redComponent * 0.299 + c.greenComponent * 0.587 + c.blueComponent * 0.114 > 0.6 ? .black : .white
    }
    private func parse() {
        let text = draft.input.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "#", with: "")
        guard text.count == 6, let value = UInt32(text, radix: 16) else { draft.error = "请输入六位 HEX，如 #4F83CC。"; return }
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
        draft.output = "HEX  \(draft.input)\n\nRGB  \(Int((r * 255).rounded())), \(Int((g * 255).rounded())), \(Int((b * 255).rounded()))\n\n" + String(format: "HSL  %.0f°, %.1f%%, %.1f%%\n\nSwiftUI\nColor(red: %.3f, green: %.3f, blue: %.3f)", hue, saturation * 100, light * 100, r, g, b)
        draft.error = nil
    }
}

struct ImageTool: View {
    @Bindable var draft: ToolDraft
    @State private var original: CGImage?
    @State private var preview: NSImage?
    @State private var width = ""
    @State private var quality = 0.85
    @State private var watermark = ""
    var body: some View {
        ToolPage(tool: Catalog.tool("image"), draft: draft) {
            PrimaryButton(title: "打开图片", symbol: "folder") { FilePanels.open(types: [.image]) { load($0[0]) } }
            Button("截图") {
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".png")
                Task {
                    defer { try? FileManager.default.removeItem(at: url) }
                    do { _ = try await ProcessRunner.run(executable: "/usr/sbin/screencapture", arguments: ["-i", "-x", url.path], timeout: 120); if FileManager.default.fileExists(atPath: url.path) { load(url) } } catch { draft.error = error.localizedDescription }
                }
            }
            Picker("格式", selection: $draft.mode) { Text("PNG").tag("PNG"); Text("JPEG").tag("JPEG"); Text("TIFF").tag("TIFF") }.frame(width: 135)
            TextField("宽度 px", text: $width).textFieldStyle(.roundedBorder).frame(width: 90)
            Button("导出", action: export).disabled(original == nil)
        } content: {
            VStack(spacing: 12) {
                HStack { Text("质量").font(.caption); Slider(value: $quality, in: 0.1...1).frame(width: 150).disabled(draft.mode != "JPEG"); Text("\(Int(quality * 100))%").font(.caption).monospacedDigit(); Spacer(); TextField("文字水印（可选）", text: $watermark).textFieldStyle(.roundedBorder).frame(width: 230) }
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(.quaternary.opacity(0.3))
                    if let preview { Image(nsImage: preview).resizable().scaledToFit().padding(24) }
                    else { ContentUnavailableView("拖入一张图片", systemImage: "photo.on.rectangle.angled", description: Text("支持 PNG、JPEG、HEIC、TIFF 等系统图像格式")) }
                }.frame(maxWidth: .infinity, maxHeight: .infinity).dropDestination(for: URL.self) { urls, _ in if let url = urls.first { load(url); return true }; return false }
            }
        }.onAppear { if draft.mode.isEmpty { draft.mode = "PNG" } }
    }
    private func load(_ url: URL) {
        do {
            guard let source = CGImageSourceCreateWithURL(url as CFURL, nil), let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
                  let w = properties[kCGImagePropertyPixelWidth] as? Int, let h = properties[kCGImagePropertyPixelHeight] as? Int, Double(w) * Double(h) <= 80_000_000,
                  let cg = CGImageSourceCreateImageAtIndex(source, 0, nil) else { throw ToolError("无法读取图片，或图片超过 8000 万像素。") }
            original = cg; preview = NSImage(cgImage: cg, size: NSSize(width: cg.width, height: cg.height)); width = String(cg.width)
            draft.status = "\(url.lastPathComponent) · \(cg.width) × \(cg.height) px"; draft.error = nil
        } catch { draft.error = error.localizedDescription }
    }
    private func export() {
        guard let original else { return }
        do {
            guard let w = Int(width), (1...16_384).contains(w) else { throw ToolError("导出宽度需为 1–16384 像素。") }
            let h = max(1, Int(Double(w) * Double(original.height) / Double(original.width)))
            guard Double(w) * Double(h) <= 80_000_000 else { throw ToolError("导出图片超过 8000 万像素。") }
            guard let context = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { throw ToolError("无法创建图片画布。") }
            if draft.mode == "JPEG" { context.setFillColor(NSColor.white.cgColor); context.fill(CGRect(x: 0, y: 0, width: w, height: h)) }
            context.interpolationQuality = .high; context.draw(original, in: CGRect(x: 0, y: 0, width: w, height: h))
            if !watermark.isEmpty {
                NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
                let font = NSFont.systemFont(ofSize: max(12, CGFloat(w) / 35), weight: .semibold)
                let shadow = NSShadow(); shadow.shadowColor = NSColor.black.withAlphaComponent(0.6); shadow.shadowBlurRadius = 3
                (watermark as NSString).draw(at: NSPoint(x: CGFloat(w) * 0.035, y: CGFloat(h) * 0.04), withAttributes: [.font: font, .foregroundColor: NSColor.white, .shadow: shadow])
                NSGraphicsContext.restoreGraphicsState()
            }
            guard let cg = context.makeImage(), let data = NSBitmapImageRep(cgImage: cg).representation(using: draft.mode == "JPEG" ? .jpeg : draft.mode == "TIFF" ? .tiff : .png, properties: [.compressionFactor: quality]) else { throw ToolError("图片导出失败。") }
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
    @State private var files: [URL] = []
    @State private var documents: [PDFDocument] = []
    @State private var combined: PDFDocument?
    @State private var showText = false
    var body: some View {
        ToolPage(tool: Catalog.tool("pdf"), draft: draft) {
            PrimaryButton(title: "添加 PDF", symbol: "plus") { FilePanels.open(types: [.pdf], multiple: true, completion: load) }
            TextField("页码：1-3,5（留空为全部）", text: $draft.option).textFieldStyle(.roundedBorder).frame(minWidth: 180, maxWidth: 270)
            Button("导出 PDF", action: export).disabled(combined == nil)
            Button(showText ? "预览 PDF" : "提取文本") { draft.output = combined?.string ?? ""; showText.toggle() }.disabled(combined == nil)
        } content: {
            HSplitView {
                VStack(alignment: .leading) {
                    Text("合并顺序").font(.caption).foregroundStyle(.secondary).padding(.horizontal, 10)
                    List(Array(files.enumerated()), id: \.offset) { index, url in
                        VStack(alignment: .leading, spacing: 7) {
                            Text(url.lastPathComponent).font(.caption).lineLimit(2)
                            HStack { Text("\(documents[index].pageCount) 页").foregroundStyle(.secondary); Spacer()
                                Button { if index > 0 { files.swapAt(index, index - 1); documents.swapAt(index, index - 1); rebuild() } } label: { Image(systemName: "arrow.up") }.disabled(index == 0)
                                Button { files.remove(at: index); documents.remove(at: index); rebuild() } label: { Image(systemName: "xmark") }
                            }.font(.caption).buttonStyle(.borderless)
                        }.padding(.vertical, 6)
                    }.listStyle(.inset)
                }.frame(minWidth: 170, idealWidth: 190, maxWidth: 230)
                if showText { EditorPane(title: "PDF 文本", text: $draft.output, editable: false) }
                else { NativePDFView(document: combined).overlay { if combined == nil { ContentUnavailableView("添加或拖入 PDF", systemImage: "doc.richtext", description: Text("按左侧顺序合并，或按页码提取页面")) } }.frame(minWidth: 300) }
            }.dropDestination(for: URL.self) { urls, _ in load(urls); return true }
        }
    }
    private func load(_ urls: [URL]) {
        do {
            var loaded: [PDFDocument] = []
            for url in urls {
                guard let document = PDFDocument(url: url), !document.isLocked, document.pageCount > 0 else { throw ToolError("无法读取 \(url.lastPathComponent)，或文件需要密码。") }; loaded.append(document)
            }
            files += urls; documents += loaded; rebuild(); draft.error = nil
        } catch { draft.error = error.localizedDescription }
    }
    private func rebuild() {
        guard !documents.isEmpty else { combined = nil; return }
        let merged = PDFDocument()
        for document in documents { for index in 0..<document.pageCount { if let page = document.page(at: index)?.copy() as? PDFPage { merged.insert(page, at: merged.pageCount) } } }
        combined = merged; draft.status = "\(documents.count) 份文件 · 共 \(merged.pageCount) 页"
    }
    private func export() {
        guard let combined else { return }
        do {
            let result = PDFDocument()
            for index in try DeveloperServices.pageIndices(draft.option, count: combined.pageCount) { if let page = combined.page(at: index)?.copy() as? PDFPage { result.insert(page, at: result.pageCount) } }
            guard let data = result.dataRepresentation() else { throw ToolError("PDF 导出失败。") }; FilePanels.save(data, name: "MooTool-document.pdf")
        } catch { draft.error = error.localizedDescription }
    }
}
