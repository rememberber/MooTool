import CoreServices
import Foundation
import MooToolNextCore

private let fileEventFlag = UInt32(kFSEventStreamCreateFlagFileEvents)

@MainActor
final class VaultFilesystemWatcher {
    private var streams: [FSEventStreamRef] = []
    private var debounce: Task<Void, Never>?
    private let workspace: URL
    private let onChange: () -> Void

    init(workspace: URL, onChange: @escaping () -> Void) {
        self.workspace = workspace
        self.onChange = onChange
    }

    func start() {
        stop()
        for toolID in ["json", "quickNote"] {
            let path = VaultFilesystemSync.root(toolID: toolID, workspace: workspace).path
            try? FileManager.default.createDirectory(atPath: path, withIntermediateDirectories: true)
            var context = FSEventStreamContext(version: 0, info: Unmanaged.passUnretained(self).toOpaque(),
                                               retain: nil, release: nil, copyDescription: nil)
            let paths = [path] as CFArray
            guard let stream = FSEventStreamCreate(nil, eventCallback, &context, paths, FSEventStreamEventId(kFSEventStreamEventIdSinceNow), 0.2, fileEventFlag) else { continue }
            FSEventStreamSetDispatchQueue(stream, DispatchQueue.main)
            FSEventStreamStart(stream)
            streams.append(stream)
        }
    }

    func stop() {
        debounce?.cancel(); debounce = nil
        for stream in streams {
            FSEventStreamStop(stream)
            FSEventStreamInvalidate(stream)
            FSEventStreamRelease(stream)
        }
        streams.removeAll()
    }

    func noteFilesystemEvent() {
        debounce?.cancel()
        debounce = Task {
            try? await Task.sleep(for: .milliseconds(180))
            guard !Task.isCancelled else { return }
            onChange()
        }
    }
}

private func eventCallback(_ stream: ConstFSEventStreamRef, clientInfo: UnsafeMutableRawPointer?, numEvents: Int,
                           eventPaths: UnsafeMutableRawPointer, eventFlags: UnsafePointer<FSEventStreamEventFlags>,
                           eventIds: UnsafePointer<FSEventStreamEventId>) {
    guard let clientInfo else { return }
    let watcher = Unmanaged<VaultFilesystemWatcher>.fromOpaque(clientInfo).takeUnretainedValue()
    Task { @MainActor in watcher.noteFilesystemEvent() }
}
