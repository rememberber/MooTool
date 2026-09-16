# DIFF-228：JSONPath 选择器对齐 Electron

## 背景

对照 `next/src/features/json/JsonPathPicker.tsx` 与 `.path-picker` / `.path-node` 样式，Compose 弹层此前仅显示缩进标签、640 宽、底栏为「关闭」，且再次打开时选中项可能不跟当前 `jsonPath` 同步。

## 行为

- 弹层 **760×480**，内容区 **min 400** 高、外框与左右分栏（约 0.9 / 1.1）+ 竖分隔线。
- 树行：**标签 + 次要路径**（10sp muted、省略），左内边距 `10 + depth×18` dp，选中行 `control` 底。
- 预览区：10sp 标签 + 11sp 等宽路径/预览。
- 底栏：**取消**（`common.cancel`）+ **使用该路径**；无匹配节点时禁用确认。
- 打开时 `jsonPathPickerInitialSelection` 同步选中项。

## 验证

- `JsonPathPickerSelectionTest`
- `./gradlew :composeApp:desktopTest --offline`
