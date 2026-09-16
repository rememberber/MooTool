import Foundation

public struct VaultGitCheckpointSchedulerOptions: Sendable {
    public var enabled: @Sendable () -> Bool
    public var hasUnsavedEditorChanges: @Sendable () async -> Bool
    public var idleMilliseconds: @Sendable () -> Int
    public var inactiveMilliseconds: @Sendable () -> Int
    public var checkpoint: @Sendable (String) async -> VaultGitActionResult
    public var now: @Sendable () -> TimeInterval
    public var tickMilliseconds: Int

    public init(
        enabled: @escaping @Sendable () -> Bool,
        hasUnsavedEditorChanges: @escaping @Sendable () async -> Bool,
        idleMilliseconds: @escaping @Sendable () -> Int,
        inactiveMilliseconds: @escaping @Sendable () -> Int,
        checkpoint: @escaping @Sendable (String) async -> VaultGitActionResult,
        now: @escaping @Sendable () -> TimeInterval = { Date().timeIntervalSince1970 * 1000 },
        tickMilliseconds: Int = 5_000
    ) {
        self.enabled = enabled
        self.hasUnsavedEditorChanges = hasUnsavedEditorChanges
        self.idleMilliseconds = idleMilliseconds
        self.inactiveMilliseconds = inactiveMilliseconds
        self.checkpoint = checkpoint
        self.now = now
        self.tickMilliseconds = tickMilliseconds
    }
}

public final class VaultGitCheckpointScheduler: @unchecked Sendable {
    private let options: VaultGitCheckpointSchedulerOptions
    private let queue = DispatchQueue(label: "com.rememberber.mootool.vault-checkpoint")
    private var timer: DispatchSourceTimer?
    private var lastActivityAt: TimeInterval = 0
    private var windowDeactivatedAt: TimeInterval = 0
    private var lastIdleCheckpointForActivity: TimeInterval = -1
    private var lastInactiveCheckpointAt: TimeInterval = -1
    private var message = "Automatic Vault checkpoint"
    private var running = false

    public init(options: VaultGitCheckpointSchedulerOptions) {
        self.options = options
    }

    public func start() {
        queue.sync {
            guard timer == nil else { return }
            let source = DispatchSource.makeTimerSource(queue: queue)
            source.schedule(deadline: .now() + .milliseconds(options.tickMilliseconds), repeating: .milliseconds(options.tickMilliseconds))
            source.setEventHandler { [weak self] in self?.evaluate() }
            source.resume()
            timer = source
        }
    }

    public func stop() {
        queue.sync {
            timer?.cancel()
            timer = nil
        }
    }

    public func recordActivity(_ message: String) {
        queue.sync {
            lastActivityAt = options.now()
            self.message = message
        }
    }

    public func setWindowActive(_ active: Bool) {
        queue.sync {
            windowDeactivatedAt = active ? 0 : options.now()
        }
    }

    @discardableResult
    public func evaluate() -> Bool {
        if running || !options.enabled() { return false }
        if lastActivityAt == 0 && windowDeactivatedAt == 0 { return false }

        let now = options.now()
        let idleReady = lastActivityAt > 0
            && now - lastActivityAt >= TimeInterval(options.idleMilliseconds())
            && lastIdleCheckpointForActivity < lastActivityAt
        let inactiveReady = windowDeactivatedAt > 0
            && now - windowDeactivatedAt >= TimeInterval(options.inactiveMilliseconds())
            && lastInactiveCheckpointAt < windowDeactivatedAt
        if !idleReady && !inactiveReady { return false }

        let activitySnapshot = lastActivityAt
        let inactiveSnapshot = windowDeactivatedAt
        let checkpointMessage = message
        running = true
        Task {
            if await options.hasUnsavedEditorChanges() {
                queue.sync { running = false }
                return
            }
            let result = await options.checkpoint(checkpointMessage)
            queue.sync {
                defer { running = false }
                guard result.success else { return }
                if idleReady { lastIdleCheckpointForActivity = activitySnapshot }
                if inactiveReady { lastInactiveCheckpointAt = inactiveSnapshot }
            }
        }
        return true
    }
}
