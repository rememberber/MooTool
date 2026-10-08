import SwiftUI
import Observation
import MooToolNextCore

struct NativeFeedbackItem: Identifiable {
    enum Tone { case info, success, error }
    let id = UUID()
    let message: String
    let tone: Tone
    var symbol: String {
        switch tone {
        case .info: return "info.circle"
        case .success: return "checkmark.circle"
        case .error: return "exclamationmark.circle"
        }
    }
    var color: Color {
        switch tone {
        case .info: return .blue
        case .success: return .green
        case .error: return .red
        }
    }
}

@Observable @MainActor
final class NativeFeedbackQueue {
    private(set) var items: [NativeFeedbackItem] = []
    private var lastSeen: UUID?

    func reset(to item: NativeFeedbackItem?) { items = []; lastSeen = item?.id }
    func receive(_ item: NativeFeedbackItem?, busy: Bool) {
        guard !busy, let item, item.id != lastSeen else { return }
        lastSeen = item.id
        items = Array((items + [item]).suffix(4))
    }
    func dismiss(_ id: UUID) { items.removeAll { $0.id == id } }
}

struct NativeToolFeedback: ViewModifier {
    let draft: ToolDraft
    @State private var queue = NativeFeedbackQueue()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content.overlay(alignment: .bottomTrailing) {
            NativeFeedbackViewport(queue: queue)
                .padding(22)
        }
        .onAppear { queue.reset(to: draft.feedback) }
        .onDisappear { queue.reset(to: draft.feedback) }
        .onChange(of: draft.feedback?.id) { _, _ in queue.receive(draft.feedback, busy: draft.busy) }
        .onChange(of: draft.busy) { _, busy in queue.receive(draft.feedback, busy: busy) }
        .onChange(of: draft.documentID) { _, _ in queue.reset(to: draft.feedback) }
        .onChange(of: queue.items.last?.id) { _, id in
            guard let id, let item = queue.items.first(where: { $0.id == id }) else { return }
            NSAccessibility.post(element: NSApp, notification: .announcementRequested,
                userInfo: [.announcement: item.message, .priority: NSAccessibilityPriorityLevel.medium.rawValue])
        }
        .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: queue.items.map(\.id))
    }
}

// Acceptance previews inject preferences without changing system-wide accessibility settings.
struct NativeFeedbackAccessibility {
    var reduceTransparency = false
    var highContrast = false
}

struct NativeFeedbackViewport: View {
    let queue: NativeFeedbackQueue
    var forceLegacyMaterial = false
    var autoDismiss = true
    var accessibilityOverride: NativeFeedbackAccessibility?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 10) {
            ForEach(queue.items) { item in
                NativeFeedbackCard(item: item, forceLegacyMaterial: forceLegacyMaterial, accessibilityOverride: accessibilityOverride) { queue.dismiss(item.id) }
                    .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity))
                    .task {
                        guard autoDismiss else { return }
                        do { try await Task.sleep(for: .milliseconds(3200)) }
                        catch { return }
                        guard !Task.isCancelled else { return }
                        queue.dismiss(item.id)
                    }
            }
        }
        .frame(maxWidth: 360)
        .fixedSize(horizontal: false, vertical: true)
    }
}

struct NativeFeedbackCard: View {
    let item: NativeFeedbackItem
    var forceLegacyMaterial = false
    var accessibilityOverride: NativeFeedbackAccessibility?
    let dismiss: () -> Void
    @Environment(\.appLanguage) private var language
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var contrast

    private var shape: RoundedRectangle { RoundedRectangle(cornerRadius: 16, style: .continuous) }
    private var solid: Bool {
        (accessibilityOverride?.reduceTransparency ?? reduceTransparency) ||
        (accessibilityOverride?.highContrast ?? (contrast == .increased))
    }

    var body: some View {
        surface
            .shadow(color: .black.opacity(solid ? 0 : 0.12), radius: 12, y: 4)
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier("native.feedback.\(item.id)")
            .feedbackAcceptanceAnchor("card.\(item.id)")
    }

    private var contents: some View {
        HStack(alignment: .center, spacing: 10) {
            Image(systemName: item.symbol).foregroundStyle(item.color).font(.system(size: 17))
                .accessibilityLabel(AppLocalization.string(item.tone == .error ? "feedback.error" : item.tone == .success ? "feedback.success" : "feedback.info", language: language))
            Text(item.message).font(.system(size: 12)).foregroundStyle(.primary).lineLimit(4)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: dismiss) { Image(systemName: "xmark").font(.system(size: 11)).frame(width: 28, height: 28).contentShape(Rectangle()) }
                .buttonStyle(.plain)
                .accessibilityLabel(AppLocalization.string("common.close", language: language))
                .help(AppLocalization.string("common.close", language: language))
                .feedbackAcceptanceAnchor("dismiss.\(item.id)")
        }
        .padding(.leading, 13).padding(.trailing, 9).padding(.vertical, 9)
        .frame(minHeight: 48)
    }

    @ViewBuilder private var surface: some View {
        if solid {
            contents.background(Color(nsColor: .windowBackgroundColor), in: shape)
                .overlay(shape.strokeBorder(Color.primary.opacity(0.35)))
        } else if #available(macOS 26.0, *), !forceLegacyMaterial {
            // Apply after laying out the complete card: one native glass surface.
            contents.background {
                Color.clear.glassEffect(.regular, in: shape)
            }
        } else {
            contents.background(.regularMaterial, in: shape)
                .overlay(shape.strokeBorder(Color.primary.opacity(0.12)))
        }
    }
}

private struct FeedbackAcceptanceAnchor: NSViewRepresentable {
    let marker: String
    func makeNSView(context: Context) -> NSView { let view = NSView(); view.identifier = NSUserInterfaceItemIdentifier("feedback." + marker); return view }
    func updateNSView(_ nsView: NSView, context: Context) {}
}
private extension View {
    @ViewBuilder func feedbackAcceptanceAnchor(_ marker: String) -> some View {
        if CommandLine.arguments.contains("--smoke-test") { self.background(FeedbackAcceptanceAnchor(marker: marker)) }
        else { self }
    }
}
