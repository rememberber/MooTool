# Electron `jsonTools.test.ts` 对照登记

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| format-compress | MooTool Next Electron | `next/src/features/json/jsonTools.test.ts` | `JsonEngineTest.mirrorsElectronJsonToolsVitestBasics` |
| escape-roundtrip | 同上 | 同上 | `JsonEngineTest.escapesAndRestoresJsonStrings` |
| validate-idle-valid-error | 同上 | 同上 | `JsonEngineTest.reportsIdleValidAndInvalid` |
| validate-structure-summary | next-tauri | `next-tauri/src/features/json/jsonTools.test.ts` `validates input and reports structural metrics` | `JsonEngineTest.analyzeStructure_matchesTauriFixture`（[DIFF-487](../diff/487-json-validate-structure-summary.md)） |
| inspector-structure-panel | next-tauri | `JsonToolSurface.tsx` `json-analysis`（`analyzeJson` + `findDuplicateJsonKeys` + UTF-8） | `JsonInspectorStructureTest` + 检查器 `JsonInspectorStructurePanel`（[DIFF-488](../diff/488-json-inspector-structure-panel.md)） |
| inspector-schema-path-sync | next-tauri / macOS | `JsonToolSurface.tsx` `path.picker`；`JSONTreePane` 复制路径 | `JsonEngineTest.inferJsonSchema_*` + `JsonInspectorPathUiTest` + 弹层 `jsonPathPickerSelectionForOpen`（[DIFF-489](../diff/489-json-inspector-schema-path-sync.md)） |
| sort-duplicate-keys | 同上 | 同上 | `JsonEngineTest.sortsKeysAndDetectsDuplicates` |
| xml-roundtrip | 同上 | 同上 | `JsonEngineTest.mirrorsElectronJsonToolsVitestBasics` / `convertsJsonAndXml` |
| path-query-enumerate | 同上 | 同上 | `mirrorsElectronJsonToolsVitestBasics` / `queriesAndEnumeratesPaths` |
| list-paths-enumerate | 同上 | `listJsonPaths` | `JsonEngineTest.listPathsMatchesElectronJsonToolsEnumerate`（[DIFF-516](../diff/516-command-settings-json-host-git.md)） |
| swap-java-bean | 同上 | 同上 | `swapsKeysAndConvertsJavaBean` |
| empty-jsonpath | 同上 | `queryJsonPath` | `JsonEngineTest.queryPathEmptyPathMatchesElectronError` |

Compose 额外覆盖：filter/slice/union/`$..`/脚本拒绝、3 MiB format、大整数字面量（见 `JsonEngineTest` 其它用例）；MCP `spaces:0` 紧凑见 `formatAdvancedSpacesZeroMinifiesLikeElectronMcp`（[DIFF-459](../diff/459-ai-mcp-test-connection-json-spaces-zero.md)，对照 `mcp/server.test.ts`）。

路径选择器预览见 [DIFF-430](../diff/430-json-path-picker-preview-format.md)（`JsonPathNodePreviewTest`，非 `queryPath` 查询结果格式）。

| path-picker-choose | Electron UI | `JsonPathPicker.tsx` `onDoubleClick` / 主按钮 | `JsonInspectorResultTest.applyPathPickerChoice_*`；弹层 UI 见 `JsonPathPickerDialogInteractionTest`（[DIFF-457](../diff/457-json-path-picker-dialog-extract.md)、[DIFF-458](../diff/458-json-path-picker-double-tap.md)） |
