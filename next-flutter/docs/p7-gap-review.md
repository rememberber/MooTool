# P7 全量差异审查（2026-09-09）

对照 Electron 1.1.4 与 `feature-parity.md`。这是审查，不是把未完成项改成“视觉改进”。

## 已有真实路径

- 26 个入口可搜索；JSON、本地工具、随手记、HTTP/Host/Net、运行时/翻译/系统信息、留言板/PDF/图片库有领域实现。
- 六种界面风格独立 token；关闭策略/托盘/剪贴板/截图草稿/取色有 DesktopHost。
- 更新检查只读 `next-flutter`；下载 SHA-512；手动打开安装包；未签名。

## 尚未实现（用户仍可用 Electron 或等待后续）

| ID | 缺口 | 用户如何完成原任务 |
| --- | --- | --- |
| 多窗口 | 同 engine 占位，不是第二 Flutter engine | 不要依赖分离窗口隔离崩溃；继续在主窗口工作 |
| F01 | 拖放树、Git watcher、5MiB 冲突 UI | 用 Git 服务手动提交；大笔记先拆分 |
| F04 | Vault Git 冲突 UI | 用外部 Git 处理冲突再打开 |
| F07 | proto 嵌套/map/oneof | 用 protoc 或 Electron |
| F10 | 系统 hosts 提权写入 | 用系统编辑器改 hosts |
| F14 | SM4/RSA/SM2 | 用 OpenSSL 或其他产品 |
| F16 | Cron `L`/`#` | 用标准五/六字段表达式 |
| F17 | QR 文件/剪贴板识别 | 用外部解码器 |
| F20 | Bing、单词本、自动翻译 | 用 Google gtx 或浏览器 |
| F23 | ScreenCaptureKit 多屏、拖拽选区 overlay、vtracer | 截主屏后裁剪；复杂矢量化用专用工具 |
| F24 | 任意加密 PDF / pdfium | SimplePdf 明文拆合 |
| F25 | CPU% | 看系统监视器 |
| A03 | 实机安装/升级/卸载、五产品并存实测 | 等 CI 产物后人工验收 |
| 发行 | 本机无 Xcode，未产出 DMG；未写 update-manifest 节点 | 在有 Xcode 的机器或 CI 打包 |

## 明确产品决定

- 不签名。
- 不把 Flutter Release 设为仓库 Latest。
- 不自动接管其他产品数据。
