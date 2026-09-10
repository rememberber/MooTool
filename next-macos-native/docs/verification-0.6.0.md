# 0.6.0 本机验收记录

验收日期：2026-09-09。环境为 macOS 26.7（25G224）、Intel x86_64、Swift 6.2.3 / Command Line Tools 26。

## 功能与布局

- 48 组核心测试全部通过：图片实际格式、大小、像素、内容摘要、路径与符号链接校验；UTF-16 选区和换行规则；附件副本、完整导出、同名保护；便携备份跨目录恢复、错误备份不覆盖旧工作区；无附件旧工作区兼容。原有 JSON、随手记、HTTP、文档库及其他核心用例回归通过。
- 原生附件交互通过：打开并取消真实选图面板、独立测试剪贴板粘贴、选区替换、自动切换 Markdown、撤销/重做、连续粘贴排队、多文件顺序插入、切换文档时丢弃过期结果、复制后删除原文档仍保留图片、图片弹窗、缺失图片占位及跨目录恢复。
- 完整回归生成 76 个界面/主题截图及 2 张 JSON 结果弹窗截图；JSON 格式化、转换、应用结果、撤销/重做，文档库切换、光标/滚动、跨窗口编辑、草稿及批量导入回归通过。
- 修正分栏尺寸后，另外重跑 15 个随手记布局场景，并复跑随手记与附件全部原生交互。实际窗口鼠标事件验证分隔条拖动及编辑/预览切换后的比例恢复；实际宽度断言防止长附件路径挤占预览区。图片水平边界检查通过。
- 检查深浅色图片分栏、长图、窄窗口、缺失占位、工具栏、快速替换、查找栏和 Markdown 表格截图。编辑器在三种视图间切换后保持同一实例与撤销记录。
- 独立的新应用进程验证文档、文件夹、选择、编辑设置和工作区恢复，并重新读取、校验和解码全部已登记附件。

文件拖放覆盖原生编辑器的文件插入回调，未进行从 Finder 开始的完整拖动手势验收。截图不能代替全部交互验收；图片缩放弹窗打开/关闭已检查，未逐项验收所有缩放比例。

所有验收使用临时原生工作区及独立偏好域；剪贴板图片测试使用专用命名剪贴板，不读取或修改真实笔记和系统剪贴板。

## 安装包

- 主程序与 JSON 辅助程序均为 arm64 + x86_64 Universal。Apple Silicon 与 macOS 14 未做真机运行验收。
- 应用复制到仓库外后，独立 Bundle ID、嵌入资源、签名、真实 JSON/随手记辅助程序调用、附件备份恢复和图片解码全部通过。
- 辅助程序阻塞输入时由自身 4 秒计时终止；DMG 完整性、可执行文件和 DMG 的 SHA-256 校验通过。
- 使用本地 ad hoc 签名，尚未完成 Developer ID 签名和 Apple 公证。
- 构建期间源码输入保持不变。构建不会安装应用或修改其他产品的数据。

产物：`MooTool-Next-macOS-Native-0.6.0-mac-universal.dmg`，8238355 字节。

SHA-256：`b919edf128a0b54b078e91b61b1b8895522fdcfc17c02170f328c2a7a78ff0b4`

构建时间（UTC）：2026-09-09T03:54:20.913568+00:00。

## 复现与记录

```bash
./scripts/check-core.sh
./scripts/smoke.sh --window-capture
./scripts/smoke.sh --window-capture --note-layouts-only
./scripts/smoke.sh --notes-only
./scripts/build-app.sh --arch universal --dmg
python3 scripts/verify-package.py
```

本产品 `dist/` 保存 `check-0.6.0.log`、`smoke-0.6.0.log`、`note-layouts-0.6.0.log`、`notes-0.6.0.log`、`package-0.6.0.log`、`verify-package-0.6.0.log` 与 `source-hashes-0.6.0.json`。`acceptance/report.json` 对应最近一次截图运行，15 个随手记布局的报告另存为 `note-layouts-report-0.6.0.json`；截图保留于 `acceptance/`，选定的界面截图复制到 [screenshots](screenshots/)。

仍未对齐的能力和数据边界见 [图片附件与备份](note-attachments.md)、[随手记工作区](quick-note-workspace.md) 和 [功能对齐清单](parity.md)。
