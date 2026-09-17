# DIFF-555：F10/F13/F14/F19 引擎接线 + JSON 列编辑 wrap + Vault 底栏 + 更新/托盘 + CSS + MCP catalog + 命令盘

## 背景

DIFF-554 已做 F20/F24/F07 接线、`GitVaultRemotePresentation`、列编辑 notice 键、translate/pdf/protobuf CSS、MCP `mootool_protobuf_wire` 与 fetch/pdf 命令盘；本条**不重复** 554 的 Pdf/Protobuf/Translation 接线、Git remote 草稿、protobuf_wire MCP 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial F 工具缺口、目标未达成。

## 行为

### F14 / F13 / F10 / F19 引擎（非 metadata）

- `CryptoWiringPresentation`：对称加解密密钥有效守卫、非对称生成/还原/RSA 反向操作启用；`CryptoScreen` 接线。
- `EncodeWiringPresentation`：正向/反向转换源非空守卫；`EncodeScreen` 接线。
- `HostWiringPresentation`：应用确认/备份恢复 busy 守卫；`HostScreen` 接线。
- `MessageBoardWiringPresentation`：展示唤醒失败与 80 字裁剪；`MessageBoardScreen` 接线。

### JSON 列编辑（EditorHost 语义）

- F04 `JsonScreen`：列编辑 notice 使用 `session.wrap`（闩锁+软换行时 `quickNote.columnEdit.wrap`）；切换换行时同步 notice。

### Vault / JSON

- `JsonVaultFooterPresentation`：底栏有效路径/脏标记/复制守卫；`JsonScreen` Vault 底栏接线。

### A03 更新 / 托盘

- `UpdateAboutPresentation`：`canCheckForUpdates` / `showDownloadProgress`；关于页接线。
- `TraySyncPresentation.menuRevision` 纳入 `autoCheckUpdates` / `autoDownloadUpdates`；`Main` 托盘菜单重建触发。

### 样式（CSS 组件批次）

- `mooJsonVaultFooter` / `mooCryptoAsymActions`

### A02 / MCP catalog / 命令盘

- `McpToolCatalogTest` 登记 `mootool_protobuf_wire` 非幂等。
- `CommandSearchCatalog`：tools 增 crypto/aes/rsa 等；about 增 autocheck/installer/messageboard。

## 验证

- `CryptoWiringPresentationTest` / `EncodeWiringPresentationTest` / `HostWiringPresentationTest` / `MessageBoardWiringPresentationTest` / `JsonVaultFooterPresentationTest` / `EditorColumnEditPresentationTest` / `UpdateAboutPresentationTest` / `TraySyncPresentationTest`
- `CommandSearchCatalogTest`（aes / autocheck）/ `McpToolCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗全工具 Tab/系统 IME 手工 PNG、P7 三平台安装/公证、其余 F 工具 substantial 引擎/UI、目标未达成。
