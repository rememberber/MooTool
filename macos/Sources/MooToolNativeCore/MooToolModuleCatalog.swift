public enum MooToolModuleCatalog {
    public static let previewModules: [MooToolModule] = [
        MooToolModule(id: "quick-note", title: "随手记", symbolName: "note.text", status: .planned),
        MooToolModule(id: "json", title: "JSON", symbolName: "curlybraces", status: .planned),
        MooToolModule(id: "time", title: "时间转换", symbolName: "clock", status: .planned),
        MooToolModule(id: "encoding", title: "编码转换", symbolName: "arrow.left.arrow.right", status: .planned),
        MooToolModule(id: "qr-code", title: "二维码", symbolName: "qrcode", status: .planned),
        MooToolModule(id: "http", title: "HTTP", symbolName: "network", status: .planned),
        MooToolModule(id: "host", title: "Host", symbolName: "server.rack", status: .planned),
        MooToolModule(id: "regex", title: "正则", symbolName: "text.magnifyingglass", status: .planned),
        MooToolModule(id: "text-diff", title: "文本对比", symbolName: "doc.on.doc", status: .planned)
    ]

    public static var defaultSelection: MooToolModule? {
        previewModules.first
    }
}
