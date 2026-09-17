# DIFF-523：HTTP 二进制/multipart + PDF 上限 + Protobuf/Crypto fixture + 更新调度

## 背景

DIFF-522「未做」仍列 HTTP 联网/二进制大走查、PDF/加解密大切片、托盘/更新手工验收。Electron `next/` 与 Compose 在 HTTP 正文类型上均无 multipart 文件上传 Tab（见 `docs/baseline.md`）；Compose 响应侧已保留原始字节供另存，本切片补请求 NUL 正文、multipart 预览与 cURL `--data-binary` 单测，并登记 F07/F08 fixture、F24 引擎上限与 A03 自动检查 wiring。

## 行为

### F09 HTTP

- `HttpEngine.decodeBody`：`multipart/*` 按文本解码（边界可读），不按二进制摘要。
- 本机 `/raw` echo：`application/octet-stream` POST 含 NUL 字节往返。
- cURL `--data-binary` 解析与 `toCurl` 往返（对齐 `httpTools.ts` 对 `-d`/`--data-binary` 同等处理）。

### F24 PDF

- `PdfEngine.MAX_TASKS`（20）拆分/合并超限抛 `too-many`（对齐 Electron 任务规模约束）。

### F07 / F08

- `ProtobufEngineTest` 与 Electron `protobufTools.test.ts` 同 Person fixture 做 JSON 树断言。
- `docs/fixtures/electron-next-protobuf-vitest.md`、`electron-next-cryptoTools-vitest.md` 登记。

### A03 更新

- `UpdateAutoCheckScheduler` 每次 tick 读取当前 `autoDownload`（对齐 Electron 定时检查使用最新 `settings.general.autoDownloadUpdates`）。

### A03 托盘

- 行为未改：`Main.kt` `LaunchedEffect(trayEnabled, language, revision)` 与 Electron `updateTray(settings)` 在 Host 列表变更时重建菜单；仍缺权限对话框手工验收。

## 验证

- `HttpEngineTest` / `PdfEngineTest` / `ProtobufEngineTest` / `UpdateAutoCheckSchedulerTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 联网大走查与 multipart 文件上传、PDF 加密样本/UI 走查、托盘取色/截图权限对话框手工验收、加解密 UI 大切片。
