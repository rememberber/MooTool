# ADR-010：原生桌面体验与平台差异

> 状态：已采纳  
> 日期：2026-08-18

## 背景

MooTool Next Tauri 与 Java、Next Electron 是并行产品线，不以迁移 Electron 源码为目标；但截图、取色、剪贴板、防休眠、拖放和窗口外观应尽量保持相同的用户任务闭环。Tauri 使用系统 WebView，各平台对桌面捕获、透明窗口、全局指针和 Wayland Portal 的支持不同，因此需要固定首版行为与批准差异。

## 决策

1. 截图由 Rust 隐藏当前可见的 MooTool 窗口后捕获所有显示器。前端显示捕获快照，允许切换显示器、按物理像素框选区域、保留整屏或取消；保存区域后删除临时整屏资产。首版不创建覆盖全部显示器的透明 Overlay，也不支持一次选区跨越两个显示器。
2. 屏幕取色复用隐藏窗口后的显示器快照，提供实时色块、HEX/RGB 和像素坐标，单击确认、Esc 取消。这样不会依赖 WebView 的 EyeDropper 权限，也不会要求用户在固定倒计时内盲移指针。
3. macOS、Windows、Linux X11 的捕获通过 Tauri-owned Rust 适配器调用 `xcap`；失败必须显示可理解的权限/桌面会话错误。Linux Wayland 无法取得显示器或全局指针时明确禁用本次操作，不返回猜测颜色。Portal Overlay 作为后续平台增强，不阻塞产品线。
4. 图片与文本剪贴板统一使用 Tauri 官方 `clipboard-manager` 插件。读图/写图权限只向图片工具 WebView 开放；写文本权限只分配给提供显式复制动作的工具，不挂到全部工具 WebView，也不授予未使用的文本读取权限。拖放使用 Tauri WebView 原生路径事件，Rust 只接受 1–50 个绝对路径、非符号链接、1 字节至 20 MiB 且能真实解码的 PNG/JPEG/WebP/GIF，批量失败时回滚。单张导出使用原生保存对话框并固定真实图片扩展名；多张导出选择目录、自动避让已有同名文件，批次失败时只回滚本批已创建的文件。
5. 防显示器休眠由 Rust 维护按 owner 隔离的 token。系统工具和留言板演示模式各自持有 owner，一个页面退出不会关闭另一个页面仍需要的防休眠状态。
6. macOS 首版使用隐藏标题和原生 Overlay 标题栏，保留系统红黄绿窗口按钮；不启用 `macOSPrivateApi` 和完全透明 WebView。与 Electron 毛玻璃强度的差异属于批准差异，优先保证直接分发、输入法和工具 WebView 稳定性。
7. Windows 使用 WebView2/Tauri 的逻辑坐标和 per-monitor DPI 处理；图片文件拖放走原生物理路径，不从 DOM `File` 推断本地路径。真实多缩放显示器仍属于发布矩阵人工验收项。
8. 自动更新、签名和发布通道继续使用 Tauri 产品线独立实现，不与 Electron updater、证书或 Release 标签共用。

## 结果

- 用户在三平台获得相同的“截图后选区、可视化取色、图片剪贴板、拖放导入和防休眠”任务闭环。
- macOS 毛玻璃、跨显示器单选区和 Wayland Portal Overlay 不作为 1.0 阻塞项，差异在产品文档和错误提示中公开。
- 原生依赖与平台差异封装在 Rust/Platform API；工具页面不直接读取任意文件路径或调用外部 shell。
- P0 记录继续保留历史实测事实，但不再承担 Go/No-Go 立项职责。
