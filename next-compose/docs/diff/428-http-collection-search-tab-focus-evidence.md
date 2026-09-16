# DIFF-428：HTTP 集合搜索 Tab 焦点环 Compose 证据

## 背景

产品窗已有 `118-http-search-tab-focus.png`（AX 帧）。需与 [DIFF-427](427-json-vault-search-tab-focus-evidence.md) 一样，在 CI 回归 `MooCompactSearch` + modern accent 焦点环（210dp 宽对齐 HTTP 集合列）。

## 行为

- `ToolbarFocusCaptureTest.captureHttpCollectionSearchFocusRing` → `132-compose-http-collection-search-tab-focus.png`。
- JSON Vault 新建文件对话框去掉冗余 `noteOwnWrite`（`onSuccess` 已 `rebaselineAfterLocalCrud`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（534/534）
