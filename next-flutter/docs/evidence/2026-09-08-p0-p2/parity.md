# 与 Electron 1.1.4 的差异

- Flutter 使用自有 JSON/XML/JSONPath 实现，不嵌入 Electron 或 jsonpath-plus JS。过滤器只支持 `@.field op literal`，不执行任意 JS。
- 大整数与 Dart `jsonDecode` 相同，超过 JS 安全整数时两边都可能失真。
- 分离窗口在 P2 为同一 engine 的会话转移，不是独立 NSWindow。
- 次要外观预设未做，避免选择后仍是 modern。
- JSON 样例文案为 Flutter 产品，不是 Electron 技术栈。
- 未完成工具显示明确“尚未实现”，没有假成功按钮。
