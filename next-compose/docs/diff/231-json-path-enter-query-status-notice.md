# DIFF-231：JSONPath Enter 查询与状态栏 notice 显隐

## 背景

检查器 JSONPath 输入仅能通过按钮查询；桌面工具常见在路径框按 Enter 触发与「查询」相同的动作。JSON/随手记状态栏在 `notice` 为空时仍渲染占位 meta，与 Electron 侧栏结果区「无文案则不占行」的体验不一致。

## 行为

- 抽取 `queryJsonPathInspector`，供检查器「查询」按钮与路径框 Enter（尊重 IME 预编辑拦截）共用。
- JSON、随手记状态栏仅在 `notice` 非空时展示右侧提示。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
