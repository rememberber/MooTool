# DIFF-565：F15 Worker↔runMatch 闭环 + CSS 批次 + Vault MCP read/list stdio + 帧 159 + P7 Win/Linux 说明

## 背景

DIFF-564 已登记 F15 `RegexWiringPresentation.runMatch`（注明 UI 仍经 `RegexWorkerClient`）、runtime/host/qr/git-resolve CSS、Vault MCP `listTools` 四项与 json search offset、Compose 帧 `158`；本条**不重复** 564 run* 批量链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### F15 正则（Worker 与 Presentation 同源）

- `RegexWorker` 子进程匹配改经 `RegexWiringPresentation.runMatch`（与单测 `runMatch` 同引擎路径；UI 仍 `RegexWorkerClient` 进程隔离/超时/取消）。
- `UaRegexTimeWiringPresentationTest`：`runMatch` 与 `RegexEngine.match` 对齐、非法模式 `Failure`。
- `RegexScreen`：`mooRegexResultsPane` / `mooRegexMatchCard`（对齐 `.regex-results` / `article`）。

### 样式（CSS 组件批次，非 564）

- `mooRegexResultsPane` / `mooRegexMatchCard`（F15 结果侧栏）。
- `mooMessageBoardFormatRow`（F19 `.message-board-format-row` 字号滑条行）。

### Vault MCP stdio（read 分页 + list/read 联调）

- `AiIntegrationVaultMcpConnectionTest.subprocessNotesReadHonorsOffsetAndLength`：`mootool_notes_read` offset/length 分页（**非** 563 json read offset / 564 search offset 链）。
- `subprocessListToolsIncludesVaultReadToolsWithNotesGrant`：stdio `listTools` 含 notes search/read 且 `mootool_notes_read` 可读（**非** 564 空 access `listTools` 链）。

### Compose 证据

- `RegexMatchCaptureTest` → `159-compose-regex-test-tab-focus.png`（非产品主窗）。

### P7 Win/Linux（文档/脚本 only）

- `scripts/prepare-p7-win-linux-build.sh`：Windows/Linux 本机构建命令清单（不代替安装验收）。
- `docs/evidence/2026-09-17-p7-win-linux-build/results.md` 登记未执行项。

## 验证

- `UaRegexTimeWiringPresentationTest` / `RegexWorkerClientTest` / `AiIntegrationVaultMcpConnectionTest` / `RegexMatchCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 F15 手工 PNG、P7 三平台安装/公证、F 工具 UI 仍不经 Presentation 直调 worker 以外的 substantial 缺口、目标未达成。
