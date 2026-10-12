import Foundation
import Vision
import ImageIO

struct Input: Decodable { let path: String; let language: String }
struct Box: Encodable { let x: Double; let y: Double; let width: Double; let height: Double }
struct Line: Encodable { let text: String; let confidence: Float; let box: Box }
struct Output: Encodable { let width: Int; let height: Int; let lines: [Line] }
struct Status: Encodable { let languages: [String]; let revision: Int }
enum Failure: String, Error { case input, tooLarge, language, failed }

func write<T: Encodable>(_ value: T) throws {
    FileHandle.standardOutput.write(try JSONEncoder().encode(value))
}

do {
    let request = VNRecognizeTextRequest()
    request.recognitionLevel = .accurate
    request.usesLanguageCorrection = false
    request.minimumTextHeight = 0
    let supported = try request.supportedRecognitionLanguages()
    if CommandLine.arguments.dropFirst().first == "--status" {
        try write(Status(languages: supported, revision: request.revision))
    } else if CommandLine.arguments.dropFirst().first == "--recognize" {
        let data = FileHandle.standardInput.readDataToEndOfFile()
        guard data.count <= 16384 else { throw Failure.input }
        let input = try JSONDecoder().decode(Input.self, from: data)
        let requested = input.language == "auto" ? ["zh-Hans", "zh-Hant", "en-US"] : [input.language]
        let languages = requested.filter { supported.contains($0) }
        guard !languages.isEmpty, input.language == "auto" || languages.count == requested.count else { throw Failure.language }
        request.recognitionLanguages = languages
        guard let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: input.path) as CFURL, nil),
              let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else { throw Failure.input }
        let width = image.width, height = image.height
        guard width > 0, height > 0, width <= 16384, height <= 16384, width * height <= 32000000 else { throw Failure.tooLarge }
        try VNImageRequestHandler(url: URL(fileURLWithPath: input.path), orientation: .up).perform([request])
        let lines = (request.results ?? []).compactMap { observation -> Line? in
            guard let candidate = observation.topCandidates(1).first else { return nil }
            let bounds = observation.boundingBox
            return Line(text: candidate.string, confidence: candidate.confidence,
                        box: Box(x: bounds.minX * Double(width), y: (1 - bounds.maxY) * Double(height),
                                 width: bounds.width * Double(width), height: bounds.height * Double(height)))
        }
        guard lines.count <= 2000, lines.reduce(0, { $0 + $1.text.utf16.count }) <= 100000 else { throw Failure.tooLarge }
        try write(Output(width: width, height: height, lines: lines))
    } else { throw Failure.input }
} catch {
    let code = (error as? Failure)?.rawValue ?? "failed"
    // Keep native diagnostics and temporary image paths out of the UI protocol.
    FileHandle.standardOutput.write(Data("{\"error\":\"\(code)\"}".utf8))
    exit(1)
}
