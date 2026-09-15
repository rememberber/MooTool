# DIFF-031：JSONPath 完整语义、历史搜索、检查器双击与安装身份展示

- 编号：DIFF-031
- 影响：F04 JSONPath/检查器、A02 历史/收藏/搜索、F09 HTTP 响应另存、A01 快捷键帮助、A03 安装身份 UI、编辑器窗口级列选择
- 日期：2026-09-14

## 原行为（Electron）

JSONPath 使用 jsonpath-plus（`wrap: false`）：单值不包数组，缺失为 `undefined`。路径树单击预览、双击采用路径。命令搜索可点击打开。历史支持搜索/删除/清空。HTTP 规格要求响应可另存。设置展示快捷键与安装身份。

## 本产品行为

- Jayway JSONPath 继续禁用任意 JS。`queryPath` 不再把真实空数组结果当成 `undefined`：filter 无匹配返回 `[]`，缺失仍可为 `undefined` 或明确错误。非法路径抛 `JsonException`。覆盖 filter / slice / union / `$..` / `$['foo.bar']`。
- 检查器路径树单击写入路径并显示 preview；双击执行查询。路径选择弹层双击采用路径。
- 命令搜索结果可点击打开工具。
- 共享 `HistoryBrowser`：搜索、详情、恢复、删除单条、清空。已接到 JSON / HTTP / 编码 / 正则 / Cron。
- Regex/Cron 收藏增加分组与查询；调色板收藏可按名称/色值搜索。
- HTTP 当前响应 Tab 可另存为文件；取消文件对话框不写盘。
- 设置 Shortcuts 列出平台快捷键；About 展示 bundle / UpgradeCode / Linux 包名与卸载隔离路径。
- 主窗口最小尺寸与持久化下限放到 960×640，配合已有 `<960dp` 折叠辅助栏。默认仍 1440×920。
- 列编辑增加真实 `JFrame` 上的拖选与 IME 提交写入列选择；headless 时 Assume 跳过。这不是手工窗口验收。

## 理由

规格要求 JSONPath 不能只做 `$.a.b`；历史必须能搜能删；命令搜索必须完整键盘+点击；安装身份必须对用户可见。空 filter 返回 `[]` 是为了让查询结果可消费，与把空数组文档 `$` 误报 `undefined` 区分。

## 证据

`JsonEngineTest.queryPathSupportsFilterSliceUnionEscapeAndRejectsScripts`、`StorageIsolationTest` 历史搜索/删除与正则分组查询、`EditorBufferColumnEditTest` 窗口组件拖选。desktopTest 见 `docs/evidence/2026-09-14-jsonpath-history/`。窗口截图、IME 手工、三平台安装未测。

## 受影响范围

- JSONPath 空匹配从 `undefined` 改为 `[]`（filter/slice 无命中）。缺失路径仍可能是 `undefined`。
- 历史删除按 `id` 删本产品 SQLite 行，不清空其他工具。
- 最小窗口从 1080×720 放宽到 960×640；1080×720 仍是必须完整可用尺寸。
