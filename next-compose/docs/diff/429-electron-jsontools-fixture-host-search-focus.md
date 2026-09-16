# DIFF-429：Electron jsonTools 对照登记 + Host 搜索焦点证据

## 背景

`feature-parity.md` §4 要求 fixture 登记与跨实现对照。F04 引擎已有单测，需显式链到 Electron `jsonTools.test.ts`。parity-gap 仍缺 Host 方案列表搜索的 Compose Tab 焦点回归帧。

## 行为

- `docs/fixtures/electron-next-jsonTools-vitest.md`：caseId ↔ Electron vitest ↔ `JsonEngineTest`。
- `JsonEngineTest.mirrorsElectronJsonToolsVitestBasics`、`queryPathEmptyPathMatchesElectronError`。
- `ToolbarFocusCaptureTest.captureHostProfileSearchFocusRing` → `133-compose-host-profile-search-tab-focus.png`（220dp 宽对齐 Host 方案列）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（536/536）
