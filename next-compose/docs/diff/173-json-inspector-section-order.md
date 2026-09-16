# DIFF-173：JSON 检查器分区顺序对齐 Electron

## 背景

Electron `JsonInspector.tsx` 分区顺序为：格式化 → 转换（含类名）→ JSONPath（输入与查询/弹层）→ **结果**。内联路径树不在侧栏，由弹层承担；compose 保留内联路径树为额外能力，但须放在结果之后。

## 行为

- **F04**：检查器拆为独立卡片：JSONPath 仅输入与按钮；**结果**卡片承载 `notice`/`status` 与路径预览；**路径浏览**卡片承载内联路径树（单击预览、双击查询）。

## 验证

- 对照 `next/src/features/json/JsonInspector.tsx` 与运行窗 `30-json.png`
- `./gradlew :composeApp:desktopTest --offline`
