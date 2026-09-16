# DIFF-307：查找无匹配文案 `find.noMatches`

## 问题

Electron `findReplace.noMatches` 用于查找/替换无命中反馈。Compose 查找条复用 `json.notice.noMatches`，英文为 “No matches”，与 Electron “No matches found” 不一致，且语义键混在 JSON 工具 notice 下。

## 行为

- 新增 `find.noMatches`（zh 与 Electron 一致；en 为 “No matches found”）。
- JSON / 随手记 / Host / HTTP 响应查找条无匹配时状态栏写入 `find.noMatches`。
- 保留 `json.notice.noMatches` 供其它 JSON 场景；英文对齐为 “No matches found”。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
