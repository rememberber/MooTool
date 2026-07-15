import CoreImage
import CoreImage.CIFilterBuiltins
import MooToolNativeCore
import SwiftUI

struct QRCodeGeneratorView: View {
    @State private var input = "https://mootool.app"
    @State private var errorMessage: String?

    private let context = CIContext()

    private var payload: QRCodePayload? {
        do {
            return try QRCodePayloadService.prepare(input)
        } catch {
            return nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("内容")
                .font(.headline)

            TextEditor(text: $input)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 110)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }
                .onChange(of: input) {
                    errorMessage = nil
                }

            HStack(alignment: .top, spacing: 22) {
                qrPreview

                VStack(alignment: .leading, spacing: 10) {
                    if let payload {
                        Text("字节数：\(payload.byteCount)")
                            .foregroundStyle(.secondary)
                        Button {
                            copy(payload.text)
                        } label: {
                            Label("复制内容", systemImage: "doc.on.doc")
                        }
                    } else {
                        Label(errorMessage ?? "请输入二维码内容", systemImage: "exclamationmark.triangle")
                            .foregroundStyle(.red)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var qrPreview: some View {
        if let payload, let image = makeQRCode(text: payload.text) {
            Image(nsImage: image)
                .interpolation(.none)
                .resizable()
                .frame(width: 220, height: 220)
                .padding(18)
                .background(.white, in: RoundedRectangle(cornerRadius: 8))
        } else {
            ContentUnavailableView("暂无二维码", systemImage: "qrcode")
                .frame(width: 260, height: 260)
        }
    }

    private func makeQRCode(text: String) -> NSImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"

        guard let outputImage = filter.outputImage else {
            return nil
        }

        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }
        return NSImage(cgImage: cgImage, size: NSSize(width: 220, height: 220))
    }

    private func copy(_ value: String) {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(value, forType: .string)
    }
}
