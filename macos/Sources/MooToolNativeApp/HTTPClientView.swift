import MooToolNativeCore
import SwiftUI

struct HTTPClientView: View {
    @State private var method = "GET"
    @State private var urlText = "https://httpbin.org/get"
    @State private var headersText = ""
    @State private var bodyText = ""
    @State private var responseText = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let methods = ["GET", "POST", "PUT", "PATCH", "DELETE"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Picker("Method", selection: $method) {
                    ForEach(methods, id: \.self) { method in
                        Text(method).tag(method)
                    }
                }
                .frame(width: 120)

                TextField("URL", text: $urlText)
                    .textFieldStyle(.roundedBorder)
                    .font(.system(.body, design: .monospaced))

                Button {
                    send()
                } label: {
                    Label(isLoading ? "请求中" : "发送", systemImage: "paperplane")
                }
                .disabled(isLoading)
            }

            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Headers")
                        .font(.headline)
                    TextEditor(text: $headersText)
                        .font(.system(.body, design: .monospaced))
                        .frame(minHeight: 110)
                        .overlay {
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(.separator)
                        }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Body")
                        .font(.headline)
                    TextEditor(text: $bodyText)
                        .font(.system(.body, design: .monospaced))
                        .frame(minHeight: 110)
                        .overlay {
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(.separator)
                        }
                }
            }

            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle")
                    .foregroundStyle(.red)
            }

            Text("Response")
                .font(.headline)

            TextEditor(text: $responseText)
                .font(.system(.body, design: .monospaced))
                .frame(minHeight: 220)
                .overlay {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator)
                }
        }
    }

    private func send() {
        isLoading = true
        errorMessage = nil
        responseText = ""

        Task {
            do {
                let request = try HTTPRequestBuilder.build(
                    method: method,
                    urlText: urlText,
                    headersText: headersText,
                    bodyText: bodyText
                )
                let (data, response) = try await URLSession.shared.data(for: request)
                let httpResponse = response as? HTTPURLResponse
                let body = String(data: data, encoding: .utf8) ?? "<\(data.count) bytes>"
                let statusLine = "Status: \(httpResponse?.statusCode ?? 0)"
                let headers = httpResponse?.allHeaderFields
                    .map { "\($0.key): \($0.value)" }
                    .sorted()
                    .joined(separator: "\n") ?? ""

                await MainActor.run {
                    responseText = [statusLine, headers, "", body].joined(separator: "\n")
                    isLoading = false
                }
            } catch HTTPRequestBuilderError.invalidURL {
                await MainActor.run {
                    errorMessage = "URL 无效"
                    isLoading = false
                }
            } catch HTTPRequestBuilderError.invalidHeader {
                await MainActor.run {
                    errorMessage = "Header 格式应为 Key: Value"
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }
}
