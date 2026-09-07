# MooTool Tauri 平台验收清单

本文区分自动化门禁与必须在真实桌面环境完成的人工验收，避免用单个平台的本地结果代替跨平台结论。

## 自动化门禁

每次提交由 `next-tauri-ci.yml` 在以下四种环境运行完整 TypeScript、Vitest、Rust 格式、Clippy 和 Rust 测试：

- macOS x64
- macOS arm64
- Windows x64
- Linux x64

四个平台都会构建并启动原生 Tauri 应用，遍历 25 个正式工具，验证进程隔离、工具 WebView 生命周期，并完成 100 次 detach/dock 重挂载压力循环。Linux 在 Xvfb 下运行。浏览器视觉门禁覆盖全部 25 个正式工具，使用 1440/1080 两档宽度、亮/暗主题以及中文、英文、日文组合；同时检查根节点横向溢出、重复 H1、可见工程标签和无可访问名称的按钮。

每个原生作业还会上传性能报告，包含冷启动、工具打开与重挂载、0/1/10/25 工具内存、10 MiB JSON、10,000 条 Quick Note、100 MiB SHA-256 和可执行文件大小。发布作业另行记录各安装包体积；指标定义见 [`performance-baseline.md`](./performance-baseline.md)。Linux 构建固定使用 Ubuntu 24.04，因为当前截图实现依赖的 `xcap 0.9.8` / `pipewire-rs 0.10` 需要 PipeWire 1.0 系统头文件，Ubuntu 22.04 的 PipeWire 0.3.48 无法编译该版本。`libwayshot-xcap 0.3.3` 同时使用 Rust 1.88 稳定的 let-chain 与 `slice::as_chunks_mut`，所以工程与四平台工作流统一声明 Rust 1.88 为 MSRV。

## 自动化运行记录

| 日期 | 提交 | GitHub Actions | 结果与结论 |
| --- | --- | --- | --- |
| 2026-09-04 | `344765a2` | [run 33845624852](https://github.com/rememberber/MooTool/actions/runs/33845624852) | macOS x64 的完整检查与 100 轮原生验收通过；Windows 暴露路径分隔符问题，Linux 暴露 Ubuntu 22.04/PipeWire 版本不兼容，macOS arm64 暴露同版系统字体的 1.57% 稳定栅格差异。三项均作为 RC 前修复输入，不能把本次运行记为四平台通过。 |
| 2026-09-06 | `03ac809f` | [run 33972861338](https://github.com/rememberber/MooTool/actions/runs/33972861338) | macOS x64、macOS arm64、Windows x64、Linux x64 全部通过；每个平台完成完整门禁、25/25 正式工具、会话隔离及 100 轮、200 次 reparent 状态保持。Windows 配置工具不再在 detach 时死锁，macOS arm64 QR Code 不再因异步派生输出误报状态变化。 |
| 2026-09-06 | `e45326a5` | [run 34023516533](https://github.com/rememberber/MooTool/actions/runs/34023516533) | 发布后回归在四个平台全部通过；新增校验按平台、架构与格式验证 5 个必需安装包，不再把可选 MSI/RPM 误计为发布前提，同时仍拒绝“总数足够但缺少必需格式”的清单。四个平台继续完成完整门禁和 100 轮、200 次 reparent 状态保持。 |

## RC 安装包运行记录

RC2 发布提交 `949280c1` 的[四平台 CI](https://github.com/rememberber/MooTool/actions/runs/34090663655) 全部通过，下载的四份报告均验证了 25/25 工具、会话隔离和 100 轮/200 次 reparent。合并提交 `e699cb87` 的[主干 CI](https://github.com/rememberber/MooTool/actions/runs/34090761808) 首轮在 macOS arm64 的环境变量摘要比较失败；只重跑失败作业后通过，原始失败不能因重跑成功而抹去，见下文竞态记录。

2026-09-07，[`next-tauri-v0.1.0-rc.2`](https://github.com/rememberber/MooTool/releases/tag/next-tauri-v0.1.0-rc.2) 已公开为 Pre-release。[发布构建](https://github.com/rememberber/MooTool/actions/runs/34090663749) 四平台安装包与首次启动检查全部通过，Windows NSIS 和 Linux deb 的安装、卸载也通过；下载后独立验证 13 个文件的 SHA-256、5 个安装包的 SHA-512 及 4 个更新包的 Minisign 签名，公钥与 RC1 相同。[通道提升](https://github.com/rememberber/MooTool/actions/runs/34123458315) 成功，公开通道与版本清单逐字节相同（SHA-256 `d1a1c2e42de10d9cc8b9b98d5faece4efb5487fcc93766ab7c13ab3ebb690d2a`）；根清单仅更新 Tauri 节点，RC1 标签及 13 个资产未变，仓库 Latest 仍为 Electron。

RC2 原样 macOS x64 DMG 在本机完成只读挂载、代码签名校验和隔离数据目录下的内置原生验收：25/25 工具、10 轮/20 次 reparent 通过，用时 92,862 ms；可执行文件与签名更新压缩包中的文件一致（SHA-256 `b9d9e5ad15802388158a56eddc6d6e4f49b45e1641d498b529b58b93f8e21575`），挂载卷已卸载。这不是人工交互或公开 RC1 → RC2 的升级验收。

| 日期 | 标签 / 提交 | GitHub Actions | 结果与结论 |
| --- | --- | --- | --- |
| 2026-09-06 | [`next-tauri-v0.1.0-rc.1`](https://github.com/rememberber/MooTool/releases/tag/next-tauri-v0.1.0-rc.1) / `c03ef5c0` | [构建 run 34008294955](https://github.com/rememberber/MooTool/actions/runs/34008294955)、[提升 run 34022927538](https://github.com/rememberber/MooTool/actions/runs/34022927538) | macOS x64/arm64 DMG 均完成代码签名校验、只读挂载和挂载卷首次启动；Windows x64 NSIS 完成静默安装、首次启动和静默卸载；Linux x64 AppImage 完成首次启动，deb 完成实际安装、从系统路径首次启动和卸载状态检查。四平台 updater 签名文件、规范化资产、`latest.json` 和 `next-tauri-release.json` 均已生成；五个安装包逐文件 SHA-512 校验通过。RC1 已发布为不占用仓库 Latest 的 Pre-release；提升任务第 2 次执行成功，公开 URL 全部可达，`next-tauri-updater/latest.json` 与版本 Release 中的清单逐字节一致（SHA-256 `a0aac585b23aaee0f364a8114657db31f1fd7eac1c9585271a1499761b602c5a`），根清单仅将 `products.next-tauri` 激活并登记 RC1。 |

## 本机交互运行记录

| 日期 | 构建 | 平台 | 场景 | 结果 |
| --- | --- | --- | --- | --- |
| 2026-09-04 | `0.1.0-rc.1` | macOS 26.7 x86_64 | 当前屏幕录制授权下枚举显示器并捕获画面 | `native_screen_capture_smoke` 通过 |
| 2026-09-04 | `0.1.0-rc.1` | macOS 26.7 x86_64 | 一次性账户写入、读取并删除系统 Keychain 凭据 | `native_keyring_round_trip` 通过；服务名为 `com.rememberber.mootool.next.tauri`，未覆盖真实代理密码 |
| 2026-09-04 | `0.1.0-rc.1` | macOS 26.7 x86_64 | 构建 DMG、只读挂载并从挂载卷首次启动 | 应用保持运行 8 秒后正常结束，DMG 可卸载 |
| 2026-09-06 | 公开 `next-tauri-v0.1.0-rc.1` DMG | macOS 26.7 x86_64 | 从公开 Release 重新下载、比对根清单 SHA-512、只读挂载、代码签名与内置原生验收 | SHA-512 和代码签名通过；25/25 工具、会话隔离、10 轮/20 次 reparent 状态保持均通过，总时长 101,906 ms；测试数据使用独立临时目录，挂载卷已卸载 |
| 2026-09-06 | 本机测试专用 `0.1.0-rc.0` → 公开 RC1 | macOS 26.7 x86_64 | 正式更新命令检查、部分下载取消、不重新检查直接重试、签名验证、替换安装和正式重启 | 升级流程通过；下载 1,378 字节后取消，重试安装成功；重启后的公开二进制与公开 updater 压缩包 SHA-256 相同，代码签名校验通过，含中文、日文及 emoji 的便笺保留；公开 RC1 自带原生验收完成 25/25 工具、10 轮/20 次 reparent，总时长 95,137 ms。发现进度字段命名和发布时间格式两项显示契约缺陷，不能将更新 UI 记为通过 |

RC1 是首个公开的 Tauri 版本。没有上一公开版本并不阻止升级机制测试：上述测试在隔离副本中仅降低版本号并增加自动验收入口，保持更新命令、公开通道和签名公钥不变。它直接调用原生更新命令，安装后运行的是未修改的公开 RC1，不能等同于人工操作更新 UI，也不能代替真实历史版本的数据迁移兼容性测试。macOS 的测试安装使用规范化绝对路径 `/private/tmp/...`；通过 `/tmp` 符号链接启动会触发上游可执行路径安全检查。

记录见 [`reports/update-acceptance/2026-09-06-macos-x64.json`](../reports/update-acceptance/2026-09-06-macos-x64.json)。原观察脚本按前端约定读取 `downloadedBytes`，在重启后校验时失败；后续只读校验使用已保存的原始事件确认升级结果，并保留该失败作为真实 UI 缺陷，而非将整体验收改记为通过。

### RC1 发布后发现的更新显示缺陷

- 进度事件：Rust 枚举的 `rename_all` 只转换变体名称，字段仍输出 `chunk_length`、`downloaded_bytes`、`content_length`；前端读取 camelCase，导致进度与字节数显示异常。源码修复采用 `rename_all_fields = "camelCase"`，并为已知/未知下载长度和全部生命周期事件增加序列化测试。
- 发布时间：`OffsetDateTime::to_string()` 输出的 `2026-09-05 22:31:09.0 +00:00:00` 不能被 JavaScript 正确解析；源码修复显式输出 RFC 3339，并覆盖 UTC、非零时区与缺失时间。

2026-09-07 源码回归：两项缺陷测试在修复前均失败，修复后 Rust 82 项通过、2 项交互测试保持忽略；前端 151 项通过、1 项跳过，产品/本地化/发布边界、TypeScript、Rust 格式与固定 Rust 1.88 的 Clippy 检查通过。默认 Rust 1.97 的 Clippy 会对现存文件提出额外的 `collapsible_if` 诊断；本轮未为此改动无关代码，发布检查仍使用工作流指定的 1.88。

这两项修复已随 RC2 发布，公开 RC1 安装包保持不变。后续仍需使用公开 RC1 → RC2 验证真实版本升级，并完成各平台人工更新 UI、同版本不更新及卸载检查；尚未直接执行公开二进制的同版本更新检查，不能将该项记为通过。

### RC1 依赖告警核实

2026-09-07 推送时核实到 3 条属于 `next-tauri/src-tauri/Cargo.lock` 的中等级别告警，不能将仓库中其他产品线的告警混入本产品结论：

- `time 0.3.45` 的 RFC 2822 解析存在栈耗尽风险。更新界面使用 RFC 3339，不代表依赖本身不受影响；源码已选择兼容 Rust 1.88 的首个修复版 `0.3.47`。[上游公告](https://github.com/time-rs/time/security/advisories/GHSA-r6v5-fh4h-64xc)
- `serde_with 3.17.0` 的 `KeyValueMap` 序列化空条目可能触发 panic；锁文件已更新到兼容 Rust 1.88 的首个修复版 `3.21.0`，并更新其关联宏依赖。[上游公告](https://github.com/jonasbb/serde_with/security/advisories/GHSA-7gcf-g7xr-8hxj)
- `glib 0.18.5` 的 `VariantStrIter` 存在未定义行为风险。Tauri 2.11.5 在 Linux 使用 GTK 0.18，而 GTK 0.18 依赖 GLib 0.18；公告的修复版本从 0.20 开始，不能仅替换锁文件中的版本跨越这条 API 依赖链。此次源码采用本地依赖覆盖，原样回移上游 PR #1343 的两行指针可变性修复，保留原版本、许可证和来源校验值；详见 [`vendor/README.md`](../src-tauri/vendor/README.md)。新增 Linux 专用回归测试覆盖全部受影响的迭代入口及空数组、越界、Unicode 场景，并在 CI 中强制以 Release 优化模式执行；必须以该步骤及 Linux 原生验收的成功结果作为验证证据，不能用 macOS 的测试通过代替。[RustSec 公告](https://rustsec.org/advisories/RUSTSEC-2024-0429.html)、[上游补丁](https://github.com/gtk-rs/gtk-rs-core/pull/1343)

依赖升级后使用 Rust 1.88 重跑全量 Rust 测试（82 项通过、2 项交互测试忽略）、Clippy 和格式检查，均通过。上述依赖变更已随 RC2 发布，不会修改公开 RC1 中已经锁定的依赖；不能据此宣称 RC1 的安全告警已经消除。

### RC2 发布后的环境变量会话就绪竞态

主干 CI 首轮的错误为 `variables: detach changed the reported state digest`；页面加载次数和会话 ID 检查已通过。源码首次渲染会立即上报空列表摘要，环境变量请求稍后完成时改变数量，可能越过原生验收的 1 秒稳定窗口。仅重跑失败作业后 25/25 工具及 100 轮压力测试通过，但这不足以证明竞态消失。

新增浏览器回归测试只延迟诊断传输层，仍运行真实组件与上报 hook：让请求保持未完成超过 2.5 秒，旧源码在成功和失败两条路径均过早上报空摘要，测试先失败。后续源码改为初始 `busy=true`，并在会话上报 hook 中增加默认开启的 `ready` 门禁；环境变量工具等待加载结束后才上报，其他工具保持原行为。成功与失败两条回归均通过，仍保留环境变量数量、敏感项数量及用户状态的摘要，不放宽原生比较或压力次数。

这项就绪竞态修复属于 RC2 之后的源码，**不包含在已公开的 RC2 安装包中**。不得改写 RC2 标签或资产来掩盖这一版本边界；后续源码的四平台原生验收需以相应 CI 结果单独确认。

## 真实设备验收

以下项目依赖窗口管理器、输入法、显示器或系统授权状态，不能由无头 CI 替代。每项必须记录操作系统版本、设备、显示协议、缩放比例和结果；未执行时标记为“未验证”，不能写成“通过”。

### macOS x64 与 arm64

- 100%、125%（如可用）、150%、200% 缩放下检查主窗口、设置窗口和分离工具窗口。
- 在 Retina 与非 Retina、多显示器及不同缩放混用时拖动窗口，检查边界、取色坐标和截图比例。
- 使用系统拼音、日文输入法验证 CodeMirror 组合输入、选区、查找替换和 QuickNote 自动保存。
- 首次截图、屏幕取色、剪贴板图片、文件/文件夹对话框分别验证允许、拒绝和撤销授权后的提示。
- 验证菜单栏、Dock、托盘、关闭到托盘、重新打开和自动更新重启。

### Windows x64

- 100%、125%、150%、200% DPI，含双屏不同 DPI，检查移动窗口后的缩放与点击坐标。
- 微软拼音与日文 IME 下验证组合输入、候选窗位置、选区和撤销/重做。
- 验证 WebView2 首次启动、系统代理/凭据管理器、截图、剪贴板、文件对话框和 Windows 防火墙提示。
- 验证托盘、任务栏、最小化/关闭行为、安装器升级与自动更新重启。

### Linux x64

- X11 与 Wayland 分别验证；至少覆盖 GNOME 和一个不同窗口管理器。
- 100%、125%、150%、200% 分数缩放及双屏不同缩放，检查窗口重挂载、截图和屏幕取色坐标。
- IBus/Fcitx5 中文与日文输入法下验证 CodeMirror 组合输入、候选窗和 QuickNote 自动保存。
- 验证 Secret Service 可用/锁定/缺失三种状态下的代理密码提示，确认绝不回退为明文设置。
- 验证 PipeWire/portal 屏幕授权允许、拒绝、取消，剪贴板与文件选择 portal，以及托盘兼容性。
- 分别启动 AppImage、deb（及发布时提供的 rpm），验证首次启动和自动更新边界。

## 结果记录模板

| 日期 | 构建/提交 | 平台与架构 | 桌面/显示协议 | 缩放与显示器 | 输入法 | 权限场景 | 结果 | 问题链接 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| YYYY-MM-DD | commit | Windows 11 x64 | DWM | 125% + 200% | 微软拼音 | 截图拒绝 | 未验证 | — |
