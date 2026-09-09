import Foundation
import Darwin
import MooToolNextCore

// The parent normally cancels at three seconds. This independent deadline also
// stops a pathological expression if the main application exits unexpectedly.
_ = signal(SIGALRM, SIG_DFL)
alarm(4)

do {
    let input = try FileHandle.standardInput.read(upToCount: 16 * 1024 * 1024 + 1) ?? Data()
    guard input.count <= 16 * 1024 * 1024 else { throw ToolError("输入超过 16 MB。") }
    let request = try JSONDecoder().decode(JSONEngineRequest.self, from: input)
    let reply = try JSONEngine.evaluateLocally(request)
    try FileHandle.standardOutput.write(contentsOf: JSONEncoder().encode(reply))
} catch {
    if let data = try? JSONSerialization.data(withJSONObject: ["error": error.localizedDescription]) { try? FileHandle.standardOutput.write(contentsOf: data) }
}
