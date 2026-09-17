# DIFF-487：JSON 校验结构摘要（节点数与深度）

## 背景

`feature-parity.md` F04 要求校验含「结构摘要」。Electron 状态栏仅展示根类型（`json.valid.ok`），Tauri `analyzeJson` 已提供节点/键/深度指标。Compose 需在保留自定义解析（大整数、重复键原文检测）的前提下，在有效 JSON 的状态栏与检查器回退文案中展示结构摘要。

## 行为

- `JsonEngine.summarizeStructure` / `analyzeStructure`：遍历 `parsePreserving` 结果，统计 `nodes`、`keys`、`maxDepth`。
- `JsonEngine.validate` 成功时使用 `json.valid.summary`（`{type}`、`{nodes}`、`{depth}`），替代仅根类型的 `json.valid.ok`。
- 三语：`json.valid.summary`（zh/en；ja 继承 en）。

## 验证

- `JsonEngineTest.analyzeStructure_matchesTauriFixture`（对照 next-tauri `jsonTools.test.ts`）
- `JsonEngineTest.reportsIdleValidAndInvalid`
- `./gradlew :composeApp:desktopTest --offline`
