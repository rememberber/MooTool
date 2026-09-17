import Foundation

public struct VaultGitChange: Equatable, Sendable {
    public var path: String
    public var originalPath: String?
    public var status: String
    public var conflict: Bool
}

public struct VaultGitStatus: Equatable, Sendable {
    public var available = false
    public var repository = false
    public var branch = ""
    public var remote = ""
    public var ahead = 0
    public var behind = 0
    public var changes: [VaultGitChange] = []
    public var conflicts = 0
    public var merging = false
    public var operation = "none"
}

public struct VaultGitCommit: Equatable, Sendable {
    public var hash: String
    public var shortHash: String
    public var author: String
    public var date: String
    public var message: String
}

public enum VaultGitAction: String, Sendable {
    case initRepo = "init"
    case configureRemote
    case commit
    case fetch
    case pull
    case push
    case discard
    case abortMerge
    case resolveConflict
    case continueOperation
}

public enum VaultGitConflictStrategy: String, Sendable {
    case ours, theirs
}

public struct VaultGitActionInput: Sendable {
    public var action: VaultGitAction
    public var message: String?
    public var remote: String?
    public var path: String?
    public var strategy: VaultGitConflictStrategy?
    public init(action: VaultGitAction, message: String? = nil, remote: String? = nil, path: String? = nil, strategy: VaultGitConflictStrategy? = nil) {
        self.action = action; self.message = message; self.remote = remote; self.path = path; self.strategy = strategy
    }
}

public struct VaultGitActionResult: Equatable, Sendable {
    public var success: Bool
    public var message: String
    public init(success: Bool, message: String) { self.success = success; self.message = message }
}

public struct VaultGitDiffFile: Equatable, Sendable {
    public var path: String
    public var before: String
    public var after: String
    public var kind: String
}

public struct VaultGitDiffResult: Equatable, Sendable {
    public var files: [VaultGitDiffFile]
}

public struct VaultGitCredentials: Sendable {
    public var username: String?
    public var token: String?
    public init(username: String? = nil, token: String? = nil) { self.username = username; self.token = token }
}

public final class VaultGitService: @unchecked Sendable {
    private let rootDirectory: URL
    private let credentials: VaultGitCredentials
    private let language: AppLanguage
    private let queue = DispatchQueue(label: "com.rememberber.mootool.vault-git")

    public init(rootDirectory: URL, credentials: VaultGitCredentials = VaultGitCredentials(), language: AppLanguage = AppLocalization.preferredLanguage()) {
        self.rootDirectory = rootDirectory; self.credentials = credentials; self.language = language
    }

    private func loc(_ key: String) -> String { AppLocalization.string(key, language: language) }
    private func err(_ key: String) -> ToolError { ToolError(loc(key)) }
    private func actionResult(_ success: Bool, _ key: String) -> VaultGitActionResult { VaultGitActionResult(success: success, message: loc(key)) }

    public func status() throws -> VaultGitStatus {
        try queue.sync { try queryStatus() }
    }

    private func queryStatus() throws -> VaultGitStatus {
            try FileManager.default.createDirectory(at: rootDirectory, withIntermediateDirectories: true)
            guard run(["--version"]).exitCode == 0 else { return emptyStatus(false) }
            let top = run(["rev-parse", "--show-toplevel"])
            guard top.exitCode == 0 else { return emptyStatus(true) }
            let resolvedRoot = rootDirectory.resolvingSymlinksInPath().path
            let resolvedTop = URL(fileURLWithPath: top.stdout.trimmingCharacters(in: .whitespacesAndNewlines)).resolvingSymlinksInPath().path
            guard resolvedRoot == resolvedTop else { return emptyStatus(true) }
            let porcelain = run(["status", "--porcelain=v1", "-z", "--branch", "--untracked-files=all"])
            let parsed = parsePorcelain(porcelain.stdout)
            let branch = parseBranch(parsed.branchLine)
            let ahead = captureInt("ahead (\\d+)", in: parsed.branchLine)
            let behind = captureInt("behind (\\d+)", in: parsed.branchLine)
            let remote = run(["remote", "get-url", "origin"])
            let merge = run(["rev-parse", "-q", "--verify", "MERGE_HEAD"])
            let rebaseInProgress = gitPathExists("rebase-merge") || gitPathExists("rebase-apply")
            let operation = rebaseInProgress ? "rebase" : (merge.exitCode == 0 ? "merge" : "none")
            let changes = parsed.changes
            return VaultGitStatus(available: true, repository: true, branch: branch,
                                  remote: remote.exitCode == 0 ? remote.stdout.trimmingCharacters(in: .whitespacesAndNewlines) : "",
                                  ahead: ahead, behind: behind, changes: changes,
                                  conflicts: changes.filter(\.conflict).count, merging: operation != "none", operation: operation)
    }

    public func diff(path: String? = nil, commit: String? = nil) throws -> VaultGitDiffResult {
        try queue.sync {
            guard try queryStatus().repository else { throw err("git.error.repoNotInitialized") }
            if let commit {
                guard commit.range(of: #"^[0-9a-f]{7,40}$"#, options: .regularExpression) != nil else { throw err("git.error.invalidCommitHash") }
                return VaultGitDiffResult(files: try commitDiffFiles(commit, path: path))
            }
            let current = try queryStatus()
            let changes = path.map { value in current.changes.filter { $0.path == value } } ?? current.changes
            return VaultGitDiffResult(files: changes.map { change in
                let before = readBlobPreview(ref: "HEAD", path: change.originalPath ?? change.path)
                let after = readWorkingPreview(change.path)
                return buildDiffFile(path: change.path, before: before, after: after)
            })
        }
    }

    public func history(limit: Int = 50) throws -> [VaultGitCommit] {
        try queue.sync {
            guard try queryStatus().repository else { return [] }
            let result = run(["log", "--max-count=\(min(100, max(1, limit)))", "--pretty=format:%H%x1f%h%x1f%an%x1f%aI%x1f%s"])
            guard result.exitCode == 0 else { return [] }
            return result.stdout.split(separator: "\n").compactMap { line in
                let parts = line.split(separator: "\u{1f}", omittingEmptySubsequences: false)
                guard parts.count >= 5 else { return nil }
                return VaultGitCommit(hash: String(parts[0]), shortHash: String(parts[1]), author: String(parts[2]),
                                      date: String(parts[3]), message: String(parts[4]))
            }
        }
    }

    public func automaticCheckpoint(_ message: String) throws -> VaultGitActionResult {
        return try queue.sync { () throws -> VaultGitActionResult in
            var gitStatus = try queryStatus()
            if !gitStatus.available { return actionResult(true, "git.result.skippedGitUnavailable") }
            if !gitStatus.repository {
                let initialized = try initialize()
                if !initialized.success { return initialized }
                gitStatus = try queryStatus()
            }
            if gitStatus.merging || gitStatus.conflicts > 0 {
                return actionResult(true, "git.result.skippedMergeInProgress")
            }
            let shouldPush = !gitStatus.remote.isEmpty && (!gitStatus.changes.isEmpty || gitStatus.ahead > 0)
            var checkpointResult = actionResult(true, "git.result.nothingToCommit")
            if !gitStatus.changes.isEmpty {
                checkpointResult = try commit(message)
                if !checkpointResult.success { return checkpointResult }
            }
            if shouldPush {
                return commandResult(run(["push", "-u", "origin", "HEAD"], authenticated: true), successMessage: loc("git.result.pushDone"))
            }
            return checkpointResult
        }
    }

    public func perform(_ input: VaultGitActionInput) throws -> VaultGitActionResult {
        try queue.sync {
            try FileManager.default.createDirectory(at: rootDirectory, withIntermediateDirectories: true)
            let repoReady = try queryStatus().repository
            if input.action != .initRepo && !repoReady {
                return actionResult(false, "git.result.repoDirectoryNotInitialized")
            }
            switch input.action {
            case .initRepo: return try initialize()
            case .configureRemote: return try configureRemote(input.remote)
            case .commit: return try commit(input.message)
            case .fetch: return commandResult(run(["fetch", "--prune", "origin"]), successMessage: loc("git.result.fetchDone"))
            case .pull: return try pull()
            case .push: return commandResult(run(["push", "-u", "origin", "HEAD"], authenticated: true), successMessage: loc("git.result.pushDone"))
            case .discard: return try discard(input.path)
            case .abortMerge: return try abortMerge()
            case .resolveConflict: return try resolveConflict(input.path, strategy: input.strategy)
            case .continueOperation: return try continueOperation()
            }
        }
    }

    private func pull() throws -> VaultGitActionResult {
        let current = try queryStatus()
        if current.merging { return actionResult(false, "git.result.finishMergeBeforePull") }
        return commandResult(run(["pull", "--no-rebase", "origin"], authenticated: true), successMessage: loc("git.result.pullDone"))
    }

    private func abortMerge() throws -> VaultGitActionResult {
        let merge = run(["merge", "--abort"])
        if merge.exitCode == 0 { return VaultGitActionResult(success: true, message: merge.stdout.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? loc("git.result.mergeAborted") : merge.stdout.trimmingCharacters(in: .whitespacesAndNewlines)) }
        let rebase = run(["rebase", "--abort"])
        return rebase.exitCode == 0
            ? VaultGitActionResult(success: true, message: rebase.stdout.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? loc("git.result.rebaseAborted") : rebase.stdout.trimmingCharacters(in: .whitespacesAndNewlines))
            : commandFailure(rebase)
    }

    private func resolveConflict(_ value: String?, strategy: VaultGitConflictStrategy?) throws -> VaultGitActionResult {
        let path = try normalizeGitPath(value)
        guard let strategy else { throw err("git.error.chooseConflictStrategy") }
        let current = try queryStatus()
        guard current.changes.contains(where: { $0.path == path && $0.conflict }) else {
            throw err("git.error.noConflictAtPath")
        }
        let checkout = run(["checkout", "--\(strategy.rawValue)", "--", path])
        guard checkout.exitCode == 0 else { return commandFailure(checkout) }
        return commandResult(run(["add", "--", path]), successMessage: loc("git.result.conflictMarkedResolved"))
    }

    private func continueOperation() throws -> VaultGitActionResult {
        let current = try queryStatus()
        guard current.repository, current.operation != "none" else {
            return actionResult(false, "git.result.noMergeInProgress")
        }
        if current.conflicts > 0 { return actionResult(false, "git.result.resolveConflictsFirst") }
        try ensureIdentity()
        if current.operation == "rebase" {
            return commandResult(run(["-c", "core.editor=true", "-c", "commit.gpgsign=false", "rebase", "--continue"]), successMessage: loc("git.result.rebaseContinued"))
        }
        return commandResult(run(["-c", "commit.gpgsign=false", "commit", "--no-edit"]), successMessage: loc("git.result.mergeContinued"))
    }

    private func gitPathExists(_ name: String) -> Bool {
        let resolved = run(["rev-parse", "--git-path", name])
        guard resolved.exitCode == 0 else { return false }
        let relative = resolved.stdout.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !relative.isEmpty else { return false }
        let path = rootDirectory.appendingPathComponent(relative).path
        return FileManager.default.fileExists(atPath: path)
    }

    private func discard(_ value: String?) throws -> VaultGitActionResult {
        let path = try normalizeGitPath(value)
        let current = try queryStatus()
        guard let change = current.changes.first(where: { $0.path == path }) else {
            return actionResult(true, "git.result.nothingToDiscard")
        }
        if change.status == "??" || change.status.hasSuffix("?") {
            return commandResult(run(["clean", "-f", "--", path]), successMessage: loc("git.result.untrackedRemoved"))
        }
        let paths = [change.originalPath, change.path].compactMap { $0 }.filter { !$0.isEmpty }
        var restored = run(["restore", "--staged", "--worktree", "--source=HEAD", "--"] + paths)
        if restored.exitCode != 0 {
            _ = run(["reset", "HEAD", "--"] + paths)
            restored = run(["checkout", "HEAD", "--"] + paths)
        }
        if restored.exitCode != 0 && change.status.contains("A") {
            return commandResult(run(["clean", "-f", "--", path]), successMessage: loc("git.result.discarded"))
        }
        return restored.exitCode == 0 ? actionResult(true, "git.result.discarded") : commandFailure(restored)
    }

    private func commitDiffFiles(_ commit: String, path: String?) throws -> [VaultGitDiffFile] {
        let parent = run(["rev-parse", "--verify", "\(commit)^"])
        let parentHash = parent.exitCode == 0 ? parent.stdout.trimmingCharacters(in: .whitespacesAndNewlines) : nil
        var args: [String]
        if let parentHash {
            args = ["diff", "--name-status", "-M", parentHash, commit]
        } else {
            args = ["diff-tree", "--root", "--no-commit-id", "--name-status", "-r", "-M", commit]
        }
        if let path { args += ["--", path] }
        let listing = run(args)
        guard listing.exitCode == 0 else { throw err("git.error.readCommitDiffFailed") }
        return parseNameStatus(listing.stdout).compactMap { item in
            let before = parentHash == nil ? missingPreview() : readBlobPreview(ref: parentHash!, path: item.originalPath ?? item.path)
            let after = readBlobPreview(ref: commit, path: item.path)
            return buildDiffFile(path: item.path, before: before, after: after)
        }
    }

    private struct NameStatusItem { var path: String; var originalPath: String? }

    private func parseNameStatus(_ output: String) -> [NameStatusItem] {
        output.split(separator: "\n").compactMap { line in
            let parts = line.split(separator: "\t", omittingEmptySubsequences: false).map(String.init)
            guard parts.count >= 2 else { return nil }
            let status = parts[0]
            if status.hasPrefix("R") || status.hasPrefix("C"), parts.count >= 3 {
                return NameStatusItem(path: parts[2], originalPath: parts[1])
            }
            return NameStatusItem(path: parts[1], originalPath: nil)
        }
    }

    private struct ContentPreview { var text: String; var kind: String }

    private let maxDiffPreviewBytes = 512 * 1024

    private func readBlobPreview(ref: String, path: String) -> ContentPreview {
        let spec = "\(ref):\(path)"
        let size = run(["cat-file", "-s", spec])
        guard size.exitCode == 0, let bytes = Int(size.stdout.trimmingCharacters(in: .whitespacesAndNewlines)), bytes <= maxDiffPreviewBytes else {
            return size.exitCode != 0 ? missingPreview() : ContentPreview(text: "", kind: "too-large")
        }
        let content = run(["cat-file", "blob", spec])
        guard content.exitCode == 0 else { return missingPreview() }
        if content.stdout.contains("\0") { return ContentPreview(text: "", kind: "binary") }
        return ContentPreview(text: content.stdout, kind: "text")
    }

    private func readWorkingPreview(_ path: String) -> ContentPreview {
        let file = rootDirectory.appendingPathComponent(path)
        guard let values = try? file.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey]), values.isRegularFile == true else {
            return missingPreview()
        }
        if let size = values.fileSize, size > maxDiffPreviewBytes { return ContentPreview(text: "", kind: "too-large") }
        guard let data = try? Data(contentsOf: file) else { return missingPreview() }
        if data.contains(0) { return ContentPreview(text: "", kind: "binary") }
        return ContentPreview(text: String(data: data, encoding: .utf8) ?? "", kind: "text")
    }

    private func missingPreview() -> ContentPreview { ContentPreview(text: "", kind: "missing") }

    private func buildDiffFile(path: String, before: ContentPreview, after: ContentPreview) -> VaultGitDiffFile {
        let kind: String
        if before.kind == "too-large" || after.kind == "too-large" { kind = "too-large" }
        else if before.kind == "binary" || after.kind == "binary" { kind = "binary" }
        else { kind = "text" }
        return VaultGitDiffFile(path: path, before: before.text, after: after.text, kind: kind)
    }

    private func normalizeGitPath(_ value: String?) throws -> String {
        let path = (value ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !path.isEmpty, path.count <= 512, !path.hasPrefix("/"), !path.contains("\0"),
              !path.split(separator: "/").contains(where: { $0 == ".." || $0 == "." || $0.isEmpty }) else {
            throw err("git.error.invalidPath")
        }
        return path.replacingOccurrences(of: "\\", with: "/")
    }

    private func initialize() throws -> VaultGitActionResult {
        if try queryStatus().repository { return actionResult(true, "git.result.repoAlreadyExists") }
        let initResult = run(["init"])
        guard initResult.exitCode == 0 else { return commandFailure(initResult) }
        let ignore = rootDirectory.appendingPathComponent(".gitignore")
        if !FileManager.default.fileExists(atPath: ignore.path) {
            try defaultGitignore.write(to: ignore, atomically: true, encoding: .utf8)
        }
        let status = try queryStatus()
        if status.changes.isEmpty { return VaultGitActionResult(success: true, message: initResult.stdout.trimmingCharacters(in: .whitespacesAndNewlines)) }
        return try commit("Initial MooTool Vault setup")
    }

    private func configureRemote(_ value: String?) throws -> VaultGitActionResult {
        let remote = try normalizeRemote(value)
        if remote.isEmpty {
            let result = run(["remote", "remove", "origin"])
            if result.exitCode != 0 && !result.stderr.contains("No such remote") {
                return commandFailure(result)
            }
            return actionResult(true, "git.result.remoteRemoved")
        }
        let existing = run(["remote", "get-url", "origin"])
        if existing.exitCode == 0 {
            return commandResult(run(["remote", "set-url", "origin", remote]), successMessage: loc("git.result.remoteUpdated"))
        }
        return commandResult(run(["remote", "add", "origin", remote]), successMessage: loc("git.result.remoteSaved"))
    }

    private func commit(_ message: String?) throws -> VaultGitActionResult {
        let text = (message ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return actionResult(false, "git.result.enterCommitMessage") }
        let current = try queryStatus()
        if current.merging || current.conflicts > 0 {
            return actionResult(false, "git.result.finishMergeBeforeCheckpoint")
        }
        if current.changes.isEmpty { return actionResult(true, "git.result.nothingToCommit") }
        try ensureIdentity()
        let add = run(["add", "-A"])
        guard add.exitCode == 0 else { return commandFailure(add) }
        let result = run(["commit", "-m", text])
        guard result.exitCode == 0 else { return commandFailure(result) }
        return VaultGitActionResult(success: true, message: result.stdout.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    private func ensureIdentity() throws {
        if run(["config", "--get", "user.name"]).exitCode != 0 {
            let name = credentials.username?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false
                ? credentials.username!.trimmingCharacters(in: .whitespacesAndNewlines) : "MooTool"
            guard run(["config", "user.name", name]).exitCode == 0 else { throw err("git.error.configUserNameFailed") }
        }
        if run(["config", "--get", "user.email"]).exitCode != 0 {
            let user = credentials.username?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let email = user.isEmpty ? "mootool@local" : user + "@mootool.local"
            guard run(["config", "user.email", email]).exitCode == 0 else { throw err("git.error.configUserEmailFailed") }
        }
    }

    private struct CommandResult { var stdout: String; var stderr: String; var exitCode: Int32 }

    private func run(_ args: [String], authenticated: Bool = false) -> CommandResult {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/git")
        process.arguments = args
        process.currentDirectoryURL = rootDirectory
        var environment = ProcessInfo.processInfo.environment
        environment["GIT_TERMINAL_PROMPT"] = "0"
        if authenticated, let token = credentials.token, !token.isEmpty {
            environment["GIT_ASKPASS"] = "/usr/bin/false"
            if let username = credentials.username { environment["MOOTOOL_GIT_USERNAME"] = username }
            environment["MOOTOOL_GIT_TOKEN"] = token
        }
        process.environment = environment
        let stdout = Pipe(), stderr = Pipe()
        process.standardOutput = stdout; process.standardError = stderr
        do { try process.run() } catch { return CommandResult(stdout: "", stderr: error.localizedDescription, exitCode: 1) }
        process.waitUntilExit()
        return CommandResult(stdout: String(data: stdout.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8) ?? "",
                             stderr: String(data: stderr.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8) ?? "",
                             exitCode: process.terminationStatus)
    }

    private let defaultGitignore = """
.DS_Store
.idea/
.vscode/
*.tmp
.migrated-from-db
"""

    private func emptyStatus(_ available: Bool) -> VaultGitStatus {
        VaultGitStatus(available: available)
    }

    private func commandResult(_ result: CommandResult, successMessage: String) -> VaultGitActionResult {
        result.exitCode == 0 ? VaultGitActionResult(success: true, message: successMessage) : commandFailure(result)
    }

    private func commandFailure(_ result: CommandResult) -> VaultGitActionResult {
        VaultGitActionResult(success: false, message: result.stderr.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? result.stdout.trimmingCharacters(in: .whitespacesAndNewlines) : result.stderr.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    private func parsePorcelain(_ output: String) -> (branchLine: String, changes: [VaultGitChange]) {
        var records = output.split(separator: "\0", omittingEmptySubsequences: false).map(String.init)
        let branchLine = records.isEmpty ? "" : records.removeFirst()
        var changes: [VaultGitChange] = []
        var index = 0
        while index < records.count {
            let record = records[index]
            index += 1
            guard record.count >= 4 else { continue }
            let status = String(record.prefix(2))
            var path = String(record.dropFirst(3))
            var originalPath: String?
            if status.contains("R") || status.contains("C") {
                originalPath = path
                if index < records.count { path = records[index]; index += 1 }
            }
            let conflict = status.contains("U") || status == "AA" || status == "DD"
            changes.append(VaultGitChange(path: path, originalPath: originalPath, status: status, conflict: conflict))
        }
        return (branchLine, changes)
    }

    private func parseBranch(_ line: String) -> String {
        let value = line.replacingOccurrences(of: "## ", with: "")
        if value.hasPrefix("No commits yet on ") || value.hasPrefix("Initial commit on ") {
            return value.split(separator: " ").last.map(String.init) ?? "HEAD"
        }
        if value.hasPrefix("HEAD ") { return "HEAD" }
        return value.components(separatedBy: "...").first?.split(separator: " ").first.map(String.init) ?? "HEAD"
    }

    private func captureInt(_ pattern: String, in text: String) -> Int {
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
              match.numberOfRanges > 1,
              let range = Range(match.range(at: 1), in: text) else { return 0 }
        return Int(text[range]) ?? 0
    }

    private func normalizeRemote(_ value: String?) throws -> String {
        let remote = (value ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if remote.isEmpty { return "" }
        guard remote.count <= 2048, !remote.contains(where: { $0.isNewline }),
              remote.range(of: #"^(https?://|ssh://|git://|git@|file://)"#, options: .regularExpression) != nil else {
            throw err("git.error.invalidRemote")
        }
        return remote
    }
}
