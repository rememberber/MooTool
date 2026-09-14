# 0.7.0 本机验收记录

验收日期：2026-09-14。环境为 macOS 26.7（25G229）、Intel x86_64、Swift 6.2.3 / Command Line Tools 26。应用最低运行版本仍为 macOS 14。

## 功能与布局

- 51 组 Core 测试全部通过，覆盖 Nginx、Java、XML、HTML 和兼容 JSON 的真实解析输出、独立辅助程序调用、UTF-8 文件与 2 MB 上限、旧原生 JSON/XML 草稿迁移及文本/文件历史恢复。原有附件、随手记、JSON、HTTP、文档库与其他核心用例回归通过。
- 格式化专项验收点击实际原生格式化按钮，并通过 NSTextView 检查文本原位更新、撤销/重做；Java 文件模式保持原文、生成右侧结果，历史恢复文件状态。隔离工作区保存后启动新应用进程，确认类型、内容、结果和编辑状态恢复。
- 完整原生回归通过：81 张工具/深浅色/窄窗口/特殊布局截图，另有 2 张 JSON 结果弹窗截图。原有随手记附件、JSON、文档库、快速切换、窗口同步及重启恢复交互均通过。格式化页另外实际捕获文本浅色/深色、文件浅色/深色和 940px 文件布局。
- 检查实际窗口截图中的页标题、文本/文件标签、类型、缩进、格式化操作、文件双栏、历史/复制/导出/清空位置；两个编辑区在窄窗口仍可用。原生版额外保留 JSON 作为旧草稿兼容类型，其余四种与 Electron 格式化页对应。

验收使用临时原生工作区与独立 `.acceptance` 偏好域，不访问真实数据。文件读取的编码、大小及导出文件名在 Core 测试验证；尚未从 Finder 完成选择文件到保存结果的整套系统面板手势验收。完整限制见 [格式化工作区与边界](reformat-workspace.md)。

## 安装包

- `MooTool Next Native.app` 与辅助程序均为 arm64 + x86_64 Universal。本机完成 Intel 运行；Apple Silicon 和 macOS 14 尚未做真机运行验收。
- 应用复制到仓库外后，独立 Bundle ID、签名和内嵌资源通过；从复制后的应用实际调用 Nginx、Java、XML、HTML 四种格式化器，以及 JSON、随手记与附件备份/恢复。辅助程序阻塞输入由自身 4 秒期限终止。
- DMG 完整性、应用及辅助程序架构、独立应用副本、可执行文件与 DMG 的 SHA-256 均通过。使用本地 ad hoc 签名，未进行 Developer ID 签名或 Apple 公证；构建不安装应用，不修改其他产品线。

本机产物：`dist/universal/MooTool-Next-macOS-Native-0.7.0-mac-universal.dmg`，9,005,652 字节。

SHA-256：`a3125a765779075106c5270105ba1eac83dbe1186e65c34c5ea4aabf0c8864dd`

构建时间（UTC）：2026-09-14T06:01:17.119337+00:00。

## 复现

```bash
./scripts/check-core.sh
./scripts/smoke.sh --window-capture --format-only
./scripts/smoke.sh --window-capture
./scripts/build-app.sh --arch universal --dmg
python3 scripts/verify-package.py
```

截图、`report.json` 和 `verify-package-0.7.0.log` 位于本产品的 `dist/`；选定的格式化界面复制到 [screenshots](screenshots/)。当前系统未接受完整 Xcode 许可，Core 检查使用与 XCTest 相同的测试主体经 Command Line Tools 运行，未声称 `swift test` 已执行。
