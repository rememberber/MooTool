# DIFF-301：Host/随手记「全部替换」成功不写 matches 状态栏

## 背景

Electron `HostTool.replaceCurrent(true)` / `QuickNoteTool.replaceCurrent(true)` 成功时仅更新正文与 `replacedCount`，无状态栏 `notice`（无匹配时仍 `toast.info(findReplace.noMatches)`）。Compose Host/随手记此前写入 `json.find.matches`；JSON 工具已在 DIFF-283 等与 Electron 一致写空 `notice`。

## 行为

- **F01 / F10**：查找条「全部替换」成功：`findReplacedCount = count`，`session.notice = ""`（不写 matches 文案）。
- 无匹配仍用 `json.notice.noMatches`；已替换计数仍由 `find.replacedPrefix` 展示。

## 验证

- 对照 `next/src/features/host/HostTool.tsx`、`next/src/features/quickNote/QuickNoteTool.tsx` `replaceCurrent`
- `./gradlew :composeApp:desktopTest --offline`
