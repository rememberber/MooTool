# ADR 005：数据

- 状态：采纳（P2 文件权威源）
- 日期：2026-09-08

## 决定

产品数据根由 `AppPaths` 解析，macOS 为 `~/Library/Application Support/com.rememberber.mootool.next.flutter/`。测试必须 `--data-dir`。

P2 使用原子 JSON：`settings.json`、`workspace.json`、`product.json`。Vault 正文在 workspace 中，Git 提交前导出到 `vaults/json/files/`。schemaVersion 从 1 起步，不复用 Electron schema 12。

损坏文件：保留原文，停止自动保存，展示错误，不写空默认覆盖。

Drift/SQLite 延后到历史/HTTP/收藏实体变多的 P4。当前 histories 存在 workspace 内，上限 100。

跨产品导入未做；不得自动扫描其他产品数据目录。
