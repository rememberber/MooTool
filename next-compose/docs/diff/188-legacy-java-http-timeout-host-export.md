# DIFF-188：Java HTTP 超时与 Host 导出目录

## 背景

Java `config.setting` 含 `setting.http.httpTimeoutMs`（全局 HTTP 超时）与 `func.host.hostExportPath`（Host 工具导出目录，与随手记/JSON/图片导出路径同类）。

## 行为

- `applyPatch`：`httpTimeoutMs` → `network.requestTimeoutMs`（1_000～120_000）；`hostExportPath` 并入导出目录候选链（绝对路径时写入 `tools.exportDirectory`，优先级低于 quickNote/json/image）。
- `applySessionPatches`：`httpTimeoutMs` → `HttpSession.timeoutMs`（`HttpEngine.clampTimeout`）并持久化，迁移后 HTTP 工具内超时与 Java 一致。

## 证据

- `LegacyJavaSettingsTest.applyPatchMapsHttpTimeoutAndHostExportDirectory`
- `LegacyJavaSettingsTest.applySessionPatchesMapsHttpTimeoutToHttpSession`
