import Foundation

public enum LayoutPaneSizes {
    public static func pane(_ map: [String: [Double]], toolId: String, index: Int, default defaultValue: Double, min: Double, max: Double) -> Double {
        let stored = map[toolId]?.getOrNil(index)
        if stored == nil || stored! <= 0 { return defaultValue.clamped(to: min...max) }
        return stored!.clamped(to: min...max)
    }

    public static func withPane(_ map: [String: [Double]], toolId: String, index: Int, value: Double, slots: Int) -> [String: [Double]] {
        var existing = map[toolId] ?? []
        while existing.count < max(slots, index + 1) { existing.append(0) }
        guard index < existing.count else { return map }
        existing[index] = value
        return map.merging([toolId: existing]) { _, new in new }
    }
}

private extension Array {
    func getOrNil(_ index: Int) -> Element? {
        guard index >= 0, index < count else { return nil }
        return self[index]
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
