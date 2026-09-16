# DIFF-229：JSON 路径行组件与内联路径树

## 背景

[DIFF-228](228-json-path-picker-electron-layout.md) 为 JSONPath 弹层补齐标签+路径行样式；检查器内联路径树仍用缩进标签，与弹层不一致。超大 JSON 枚举全部路径时侧栏无截断提示。

## 行为

- 抽取 `JsonPathListRow`（`JsonPathListUi.kt`），弹层与检查器共用。
- 检查器路径浏览：选中行展示等宽预览；超过 `JSON_INSPECTOR_INLINE_PATH_LIMIT`（80）时显示 `json.pathTree.truncated`。

## 验证

- `JsonPathListLimitTest`
- `./gradlew :composeApp:desktopTest --offline`
