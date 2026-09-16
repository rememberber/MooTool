# DIFF-283：JSON 查找无匹配提示

## 对照 Electron

`findAround` / `replaceAllMatches` 无命中时 `toast.info(findReplace.noMatches)`；Compose JSON 此前上/下条静默失败，「全部替换」在 0 处时仍显示「0 处匹配」。

## 行为

- `jumpFind`：查询非空且无下一/上一处时写入状态栏 `json.notice.noMatches`（与 Host/随手记一致）。
- 「全部替换」：`count == 0` 时同样提示无匹配，不写入编辑器。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
