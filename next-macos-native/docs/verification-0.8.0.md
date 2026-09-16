# 0.8.0 本机验收记录

验收日期：2026-09-14（安装包）；2026-09-16 补充 Core/Smoke 回归。环境为 macOS 26.7（25G229）、Intel x86_64、Swift 6.2.3 / Command Line Tools 26。应用最低运行版本为 macOS 14。

## 功能与布局

- **59** 组 Core 测试全部通过（含 SM2、Legacy 对称加密、RSA、工作台布局字段等）。文本对比用例对照 Electron 样例检查 UTF-16 字符范围、行增删改计数、三行上下文统一补丁、忽略空白、末尾空行、长行回退和旧工作区读取；原有格式化、附件、随手记（含选区/全文快速替换）、JSON、HTTP 与文档库用例也通过。
- 2026-09-16：`./scripts/smoke.sh` 全量通过（约 7.7 分钟），含附件缺失占位、随手记选区快速替换、JSON/文档库/格式化/文本对比与重启恢复。同日修复「无选区应全文替换」后重跑 smoke 通过（约 9.7 分钟，59 组 Core 0 失败）。
- 文本对比专项验收点击实际原生“比较”“下一处”按钮，通过 NSTextView 检查双侧高亮与定位、统一只读补丁、忽略空白和历史恢复；保存隔离工作区后启动新进程，确认正文与编辑状态恢复。
- 全量原生回归通过：86 张工具、深浅色和特殊布局窗口截图，另捕获 2 张 JSON 结果弹窗截图。附件、随手记、JSON、文档库、格式化、文本对比及重启恢复交互均通过。
- 人工核对文本对比的左右浅色、统一深色和 940px 统一视图截图。窄窗口保留侧边栏、全部工具栏操作、双编辑器、下方补丁与状态栏；验收还检查三个编辑区域没有超出窗口边界。

验收使用临时原生工作区与独立 `.acceptance` 偏好域，不访问真实数据。输入和长行限制、与 Monaco 编辑装饰的差异见 [文本对比工作区与边界](text-diff-workspace.md)。

## 安装包

- `MooTool Next Native.app` 与辅助程序均为 arm64 + x86_64 Universal。本机完成 Intel 运行；Apple Silicon 和 macOS 14 尚未做真机运行验收。
- 应用复制到仓库外后，独立 Bundle ID、签名和内嵌资源通过；从复制后的应用实际运行文本对比核心用例、四种格式化器，以及 JSON、随手记与附件备份/恢复。
- DMG 完整性、应用及辅助程序架构、产物哈希和独立应用副本均通过。使用本地 ad hoc 签名，未进行 Developer ID 签名或 Apple 公证；构建不安装应用，不修改其他产品线。

本机产物：`dist/universal/MooTool-Next-macOS-Native-0.8.0-mac-universal.dmg`，9,352,188 字节。

SHA-256：`1c35655e9ec4357a662a7f1530c2b5aa57ea8ef496a98572a05a3713daa24186`

构建时间（UTC）：2026-09-14T12:09:38.927652+00:00。

## 复现

```bash
./scripts/check-core.sh
./scripts/smoke.sh --window-capture --diff-only
./scripts/smoke.sh --window-capture
./scripts/build-app.sh --arch universal --dmg
python3 scripts/verify-package.py
```

截图、`report.json` 和 `verify-package-0.8.0.log` 位于本产品的 `dist/`；选定的文本对比截图复制到 [screenshots](screenshots/)。当前系统未接受完整 Xcode 许可，Core 检查使用与 XCTest 相同的测试主体经 Command Line Tools 运行，未声称 `swift test` 已执行。
