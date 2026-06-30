public enum MooToolModuleCatalog {
    public static let previewModules: [MooToolModule] = [
        MooToolModule(id: "quick-note", title: "随手记", symbolName: "note.text", status: .preview),
        MooToolModule(id: "json", title: "JSON", symbolName: "curlybraces", status: .preview),
        MooToolModule(id: "time", title: "时间转换", symbolName: "clock", status: .preview),
        MooToolModule(id: "encoding", title: "编码转换", symbolName: "arrow.left.arrow.right", status: .preview),
        MooToolModule(id: "qr-code", title: "二维码", symbolName: "qrcode", status: .preview),
        MooToolModule(id: "http", title: "HTTP", symbolName: "network", status: .preview),
        MooToolModule(id: "host", title: "Host", symbolName: "server.rack", status: .preview),
        MooToolModule(id: "regex", title: "正则", symbolName: "text.magnifyingglass", status: .preview),
        MooToolModule(id: "text-diff", title: "文本对比", symbolName: "doc.on.doc", status: .preview)
    ]

    public static var defaultSelection: MooToolModule? {
        previewModules.first
    }
}
