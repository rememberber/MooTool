import SwiftUI
import MooToolNextCore

struct JSONTreePane: View {
    let text: String
    @Binding var path: String
    @State private var structure: JSONStructure?
    @State private var source = ""
    @State private var selection: String?
    @State private var error: String?
    @State private var loading = false
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Label("JSON 结构", systemImage: "list.bullet.indent").font(.system(size: 12, weight: .medium))
                Spacer()
                if loading { ProgressView().controlSize(.mini) }
                else if let structure { Text("\(structure.nodeCount) 节点 · \(structure.maxDepth) 层").font(.system(size: 10)).foregroundStyle(.secondary) }
            }.padding(.horizontal, 13).frame(height: 37).background(.quaternary.opacity(0.25))
            Divider()
            if let error { ContentUnavailableView("无法读取 JSON", systemImage: "curlybraces", description: Text(error)) }
            else if let structure {
                List(selection: $selection) {
                    row(structure.root).tag(structure.root.id)
                    if let children = structure.root.children {
                        OutlineGroup(children, children: \.children) { node in row(node).tag(node.id) }
                    }
                }.listStyle(.inset).jsonAcceptanceControl("路径树就绪")
            } else { Spacer(); if text.isEmpty { Text("输入 JSON 后查看结构").font(.caption).foregroundStyle(.secondary) }; Spacer() }
            Divider()
            HStack(spacing: 8) {
                Text(selectedNode?.queryPath ?? "选择节点以获取查询路径").font(.system(size: 11, design: .monospaced)).foregroundStyle(.secondary).lineLimit(1).textSelection(.enabled)
                Spacer(minLength: 0)
                Button { if let selection { FilePanels.copy(selection) } } label: { Image(systemName: "link") }.help("复制 JSON Pointer（根节点为空字符串）").disabled(selection == nil)
                Button { if let selection { copyValue(selection) } } label: { Image(systemName: "doc.on.doc") }.help("复制节点 JSON").disabled(selection == nil)
            }.buttonStyle(.borderless).padding(12)
        }.frame(minWidth: 230, minHeight: 150).background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 9)).clipShape(RoundedRectangle(cornerRadius: 9)).overlay(RoundedRectangle(cornerRadius: 9).strokeBorder(.quaternary))
        .task(id: text) {
            guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { structure = nil; error = nil; selection = nil; loading = false; return }
            loading = true
            do {
                try await Task.sleep(for: .milliseconds(220))
                let value = try await Task.detached(priority: .userInitiated) { try JSONStructure(text) }.value
                try Task.checkCancellation()
                structure = value; source = text; error = nil; loading = false
                if let selection, (try? TextServices.jsonPath(source, path: selection)) == nil { self.selection = nil }
            } catch {
                guard !Task.isCancelled else { return }
                self.error = error.localizedDescription; structure = nil; selection = nil; loading = false
            }
        }
        .onChange(of: selection) { _, _ in if let node = selectedNode { path = node.queryPath } }
    }
    private var selectedNode: JSONTreeNode? {
        guard let root = structure?.root, let selection else { return nil }
        var nodes = [root]
        while let node = nodes.popLast() { if node.pointer == selection { return node }; nodes.append(contentsOf: node.children ?? []) }
        return nil
    }
    private func row(_ node: JSONTreeNode) -> some View {
        HStack(spacing: 9) {
            Image(systemName: icon(node.kind)).foregroundStyle(node.kind == "String" ? Color.green : .accentColor).frame(width: 16)
            Text(node.name.isEmpty ? "\"\"" : node.name).font(.system(size: 12, weight: .medium, design: .monospaced)).lineLimit(1)
            Text(node.summary).font(.system(size: 11, design: .monospaced)).foregroundStyle(.secondary).lineLimit(1)
            Spacer(minLength: 0)
            Text(node.kind).font(.system(size: 9)).foregroundStyle(.tertiary)
        }.padding(.vertical, 3).help(node.pointer.isEmpty ? "$" : node.pointer)
        .contextMenu {
            Button("用于路径查询") { selection = node.pointer; path = node.queryPath }
            Button("复制 JSONPath") { FilePanels.copy(node.queryPath) }
            Button("复制 JSON Pointer") { FilePanels.copy(node.pointer) }
            Button("复制节点 JSON") { copyValue(node.pointer) }
        }
    }
    private func copyValue(_ pointer: String) {
        do { FilePanels.copy(try TextServices.jsonPath(source, path: pointer)) } catch { self.error = error.localizedDescription }
    }
    private func icon(_ kind: String) -> String {
        switch kind { case "Object": return "curlybraces"; case "Array": return "square.stack"; case "String": return "textformat"; case "Boolean": return "checkmark.circle"; case "Null": return "minus.circle"; default: return "number" }
    }
}
