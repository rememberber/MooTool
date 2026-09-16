# DIFF-320：内联路径树选中逻辑抽取

## 问题

检查器内联路径树单击/双击前段重复设置 `jsonPath`/`pathResult`/`notice`；与弹层 `applyPathPickerChoice`（不写预览）语义混在一起，不利单测与维护。

## 行为

- 新增 `JsonSession.applyInlinePathTreePreview`（`JsonInspectorResult.kt`）：单击预览写入路径 + 节点预览片段 + `pathApplied` 提示。
- 双击仍在此基础上调用 `showResult` + `queryPath`。
- `acceptance.md` F04：更正 JSONPath 弹层说明为 [DIFF-319](319-json-path-picker-choose-only.md)，并链到 Vault Git [DIFF-313](313-vault-git-panel-copy-i18n.md)～[318](318-vault-git-success-toast-only.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
