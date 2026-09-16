import Foundation

/// Maps Electron normalized pane ratios to native absolute widths (Compose-compatible keys).
public enum ElectronPaneSizeImport {
    private static let referenceWidth = 1320.0

    public static func mergeElectronIntoNative(_ electron: [String: [Double]], current: [String: [Double]]) -> [String: [Double]] {
        guard !electron.isEmpty else { return current }
        var result = current

        if let ratios = electron["json-three-pane"], ratios.count >= 3, let total = positiveSum(ratios) {
            let vault = (ratios[0] / total * referenceWidth).clamped(to: 200...320)
            let inspector = (ratios[2] / total * referenceWidth).clamped(to: 240...340)
            result["json"] = [vault, inspector]
        }
        if let ratios = electron["json-two-pane"], ratios.count >= 2, let total = positiveSum(ratios) {
            let vault = (ratios[0] / total * referenceWidth).clamped(to: 200...320)
            let inspector = existingSlot(result: result, current: current, paneKey: "json", index: 1, fallback: 280)
            result["json"] = [vault, inspector]
        }

        mapTwoColumn(electron["http-workspace"], paneKey: "http", minFirst: 180, maxFirst: 320, defaultSecond: 220, into: &result, current: current)
        mapTwoColumn(electron["host-workspace"], paneKey: "host", minFirst: 170, maxFirst: 360, defaultSecond: 220, into: &result, current: current)
        mapTwoColumn(electron["image-library"], paneKey: "image", minFirst: 180, maxFirst: 360, defaultSecond: 240, into: &result, current: current)
        mapTwoColumn(electron["translation-words"], paneKey: "translation", minFirst: 170, maxFirst: 360, defaultSecond: 220, into: &result, current: current)
        mapTwoColumn(electron["ua-parser"], paneKey: "uaParse", minFirst: 300, maxFirst: 480, defaultSecond: 360, into: &result, current: current)
        mapTwoColumn(electron["text-diff"], paneKey: "textDiff", minFirst: 240, maxFirst: 900, defaultSecond: 240, into: &result, current: current)
        mapTwoColumn(electron["network-workspace"], paneKey: "net", minFirst: 340, maxFirst: 900, defaultSecond: 320, into: &result, current: current)
        mapTwoColumn(electron["regex-test"], paneKey: "regex", minFirst: 320, maxFirst: 900, defaultSecond: 290, into: &result, current: current)
        mapTwoColumn(electron["reformat-file"], paneKey: "reformat", minFirst: 260, maxFirst: 900, defaultSecond: 260, into: &result, current: current)
        mapTwoColumn(electron["calculator-panels"], paneKey: "calculator", minFirst: 360, maxFirst: 900, defaultSecond: 320, into: &result, current: current)
        mapTwoColumn(electron["cron-builder"], paneKey: "cron", minFirst: 460, maxFirst: 900, defaultSecond: 340, into: &result, current: current)
        mapIoThree(electron["encode-panes"], paneKey: "encode", minLeft: 240, maxLeft: 700, minMiddle: 120, maxMiddle: 280, into: &result)
        mapIoThree(electron["config-convert"], paneKey: "ymlProperties", minLeft: 240, maxLeft: 700, minMiddle: 110, maxMiddle: 280, into: &result)
        mapIoThree(electron["config-validate"], paneKey: "ymlProperties", minLeft: 240, maxLeft: 700, minMiddle: 110, maxMiddle: 280, into: &result)
        mapTwoColumn(electron["color-board"], paneKey: "colorBoard", minFirst: 240, maxFirst: 700, defaultSecond: 420, into: &result, current: current)
        mapIoThree(electron["protobuf-wire"], paneKey: "protobuf", minLeft: 240, maxLeft: 700, minMiddle: 110, maxMiddle: 280, into: &result)
        mapIoThree(electron["protobuf-convert"], paneKey: "protobuf", minLeft: 240, maxLeft: 700, minMiddle: 110, maxMiddle: 280, into: &result)
        mapIoThree(electron["crypto-symmetric"], paneKey: "crypto", minLeft: 240, maxLeft: 700, minMiddle: 110, maxMiddle: 280, into: &result)
        mapIoThree(electron["crypto-base"], paneKey: "crypto", minLeft: 240, maxLeft: 700, minMiddle: 110, maxMiddle: 280, into: &result)
        mapTwoColumn(electron["crypto-key-pair"], paneKey: "crypto", minFirst: 260, maxFirst: 900, defaultSecond: 260, into: &result, current: current)
        mapTwoColumn(electron["runtime-editor-output"], paneKey: "java", minFirst: 300, maxFirst: 900, defaultSecond: 300, into: &result, current: current)
        mapTwoColumn(electron["quick-note-editor-preview"], paneKey: "quick-note-editor-preview", minFirst: 260, maxFirst: 900, defaultSecond: 260, into: &result, current: current)
        mapTwoColumn(electron["qrcode-generate"], paneKey: "qrCode", minFirst: 300, maxFirst: 900, defaultSecond: 280, into: &result, current: current)
        mapTwoColumn(electron["qrcode-recognize"], paneKey: "qrCode", minFirst: 300, maxFirst: 900, defaultSecond: 280, into: &result, current: current)

        if let ratios = electron["quick-note-tree-replace"], ratios.count >= 3, let total = positiveSum(ratios) {
            let vault = (ratios[0] / total * referenceWidth).clamped(to: 200...320)
            let replace = (ratios[2] / total * referenceWidth).clamped(to: 200...340)
            result["quick-note-tree-replace"] = [vault, replace]
        }
        if let ratios = electron["quick-note-tree-no-replace"], ratios.count >= 2, let total = positiveSum(ratios) {
            let vault = (ratios[0] / total * referenceWidth).clamped(to: 200...320)
            let replace = existingSlot(result: result, current: current, paneKey: "quick-note-tree-no-replace", index: 1, fallback: 240)
            result["quick-note-tree-no-replace"] = [vault, replace]
        }
        if let ratios = electron["quick-note-no-tree-replace"], ratios.count >= 2, let total = positiveSum(ratios) {
            let editor = (ratios[0] / total * referenceWidth).clamped(to: 320...900)
            let replace = (ratios[1] / total * referenceWidth).clamped(to: 200...340)
            result["quick-note-no-tree-replace"] = [editor, replace]
        }

        return result
    }

    private static func positiveSum(_ ratios: [Double]) -> Double? {
        let total = ratios.reduce(0, +)
        return total > 0 ? total : nil
    }

    private static func mapIoThree(_ ratios: [Double]?, paneKey: String, minLeft: Double, maxLeft: Double, minMiddle: Double, maxMiddle: Double, into result: inout [String: [Double]]) {
        guard let ratios, ratios.count >= 3, let total = positiveSum(ratios) else { return }
        let left = (ratios[0] / total * referenceWidth).clamped(to: minLeft...maxLeft)
        let middle = (ratios[1] / total * referenceWidth).clamped(to: minMiddle...maxMiddle)
        result[paneKey] = [left, middle]
    }

    private static func mapTwoColumn(_ ratios: [Double]?, paneKey: String, minFirst: Double, maxFirst: Double, defaultSecond: Double, into result: inout [String: [Double]], current: [String: [Double]]) {
        guard let ratios, ratios.count >= 2, let total = positiveSum(ratios) else { return }
        let first = (ratios[0] / total * referenceWidth).clamped(to: minFirst...maxFirst)
        let second = existingSlot(result: result, current: current, paneKey: paneKey, index: 1, fallback: defaultSecond)
        result[paneKey] = [first, second]
    }

    private static func existingSlot(result: [String: [Double]], current: [String: [Double]], paneKey: String, index: Int, fallback: Double) -> Double {
        if let list = result[paneKey], index >= 0, index < list.count { return list[index] }
        if let list = current[paneKey], index >= 0, index < list.count { return list[index] }
        return fallback
    }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double { min(max(self, range.lowerBound), range.upperBound) }
}
