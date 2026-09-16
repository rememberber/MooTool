# DIFF-454：JSON 检查器路径树 80 条截断 + AI 并发安装守卫

## 背景

F04 检查器内联路径树应对齐 Electron 大量节点时的截断提示（80 条）。AI 安装与 Electron 一样在 `installing` 期间拒绝第二次 `install`。

## 行为

- `jsonInspectorVisiblePathEntries` / `jsonInspectorPathTreeShowsTruncationHint`（`JsonPathListUi.kt`），`JsonScreen` 检查器路径树复用。
- `JsonInspectorPathTreeLimitTest`：100 条路径仅展示 80 条并显示截断条件。
- `AiIntegrationServiceTest.installRejectsConcurrentInstallation`：`connectionVerifier` 阻塞首装时第二次 `install` 抛错。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（610/610）
