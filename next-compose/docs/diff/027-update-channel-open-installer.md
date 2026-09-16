# DIFF-027：更新通道只读 next-compose，校验后打开安装包

- 编号：DIFF-027
- 影响：A01 关于/通用、A03 更新
- 日期：2026-09-14

## 原行为（Electron）

`updateService.ts` 读取根 `update-manifest.json` 的 `products.next-electron`，比较 SemVer 后用 `electron-updater` 下载并在支持的平台上自动安装。节点缺失或非 active 会按错误处理，而不是当成“本产品尚未发布”。

Flutter `next-flutter` 更接近本产品：下载校验后打开安装包，不自动替换正在运行的应用。

## 本产品行为

`UpdateEngine` **只读** `products.next-compose`。不提供切换成 Electron/Java 更新的设置。根清单尚未登记该节点、或 `status != active` 时结果为 `Unpublished`：打开本产品 release 页，**不**下载其他产品或平台包。

稳定版忽略 prerelease。只选当前 OS/arch；darwin 只要 `dmg`。下载走 HTTPS 到本产品 `cache/updates`，校验字节数与标准 Base64 SHA-512，成功后写 `{fileName}.ready`。校验失败删除临时文件，不打开。可取消。完整流程是检查→展示说明→下载→校验→**打开安装包**。界面不声称自动安装完成，也不调用 Electron updater。

`autoCheckUpdates`（默认 true）在启动后后台检查（2.5s 首次、每小时重复，见 [DIFF-409](409-update-auto-check-schedule.md)）；`autoDownloadUpdates`（默认 false）可自动下载，仍需用户点打开。Feed 默认 GitHub raw 清单，可用 `MOOTOOL_COMPOSE_UPDATE_FEED_URL` 覆盖。HTTPS 重定向允许，跳到 http 则拒绝。

本轮不往仓库根 `update-manifest.json` 预填 `next-compose` 节点。签名/公证、退出后自动替换安装、Windows/Linux 安装器实装仍未做。

## 理由

规格要求独立产品节点与手动打开安装包。资产尚未上传时，把缺失节点当成 Unpublished 比抛错或误下 Electron 包更安全。Compose Desktop 没有与 electron-updater 等价的受控自动替换。

## 证据

`UpdateEngineTest`：SemVer、包名、缺失/非 active 节点 Unpublished、只读 next-compose、darwin dmg 选包、无匹配包 download=null、本地下载 size/SHA-512 成功与失败不落正式文件。`desktopTest` **148/148**（5 项更新测试均执行）。无运行截图；真实联网检查与打开安装包未手工验收。

## 受影响范围

- 与 Electron 比：不自动安装；节点未登记是 Unpublished 而不是抛错。
- 与根清单当前状态：`next-compose` 尚未发布，启动检查预期为 Unpublished。
- Git pull/push、完整 JSON 检查器弹层仍未做。
