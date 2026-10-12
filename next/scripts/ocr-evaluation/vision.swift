import Foundation
import Vision
import ImageIO

let directory = CommandLine.arguments[1]
let sampleData = try Data(contentsOf: URL(fileURLWithPath: directory + "/samples.json"))
let samples = try JSONSerialization.jsonObject(with: sampleData) as! [[String: Any]]
let request = VNRecognizeTextRequest()
request.recognitionLevel = .accurate
request.recognitionLanguages = ["zh-Hans", "zh-Hant", "en-US"]
request.usesLanguageCorrection = false
request.minimumTextHeight = 0
let supported = try request.supportedRecognitionLanguages()
var runs: [[String: Any]] = []
for pass in 0..<4 {
    for sample in samples {
        let id = sample["id"] as! String
        let handler = VNImageRequestHandler(url: URL(fileURLWithPath: directory + "/" + id + ".png"))
        let started = DispatchTime.now().uptimeNanoseconds
        try handler.perform([request])
        let ms = Double(DispatchTime.now().uptimeNanoseconds - started) / 1_000_000
        let lines = (request.results ?? []).compactMap { $0.topCandidates(1).first?.string }
        runs.append(["id": id, "pass": pass, "ms": ms, "text": lines.joined(separator: "\n")])
        if pass == 0 { print(id, ms) }
    }
}
let output: [String: Any] = ["engine": "Apple Vision", "revision": request.revision, "languageCorrection": false, "supportedLanguages": supported, "runs": runs]
try JSONSerialization.data(withJSONObject: output, options: [.prettyPrinted, .sortedKeys]).write(to: URL(fileURLWithPath: directory + "/vision.json"))
