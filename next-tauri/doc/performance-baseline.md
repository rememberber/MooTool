# MooTool Next Tauri 性能基线

本文记录 `0.1.0-rc.1` 的可重复性能基线。数据用于后续版本比较，不代表所有设备的性能承诺；四平台 CI 会分别上传同结构的 JSON 报告。

## 采集方法

- `npm run performance:baseline` 构建 release 原生应用后，从进程创建开始计时，等待第一个正式工具完成会话上报。
- 依次打开 25 个正式工具，记录打开耗时、停靠/分离耗时和 WebView 状态保持结果。
- 在同一应用进程内分别保留 0、1、10、25 个工具 WebView，统计应用进程树常驻内存。
- 通过正式 JSON 实现验证并格式化精确 10 MiB 的有效 JSON。
- 通过正式 SQLite repository 事务导入 10,000 条 Quick Note 并完整读取。
- 通过正式流式摘要实现计算 100 MiB 文件的 SHA-256。
- `npm run release:rehearse` 构建 `.app` 与 DMG，记录未压缩应用和安装包字节数；随后从只读挂载的 DMG 启动应用并观察 8 秒。

原始报告：[`reports/performance/local.json`](../reports/performance/local.json) 和 [`reports/release-rehearsal/local.json`](../reports/release-rehearsal/local.json)。

## 首个基线结果

采集时间：2026-09-04；提交工作区版本：`0.1.0-rc.1`；环境：macOS 26.7 x86_64、Intel Core i7-1068NG7、32 GiB 内存。

| 指标 | 结果 |
| --- | ---: |
| 进程创建至首个工具会话稳定 | 7,578 ms |
| 25 个工具打开耗时中位数 / 最大值 | 1,498 / 1,714 ms |
| 分离并收回耗时中位数 | 549 ms |
| 10 轮、20 次 reparent 压力循环 | 39,551 ms，状态保持 |
| 空闲 / 1 / 10 / 25 工具进程树内存 | 106.9 / 108.2 / 110.2 / 116.8 MiB |
| 10 MiB JSON 验证 / 格式化 | 12.756 / 28.189 ms |
| 10,000 条 Quick Note 事务导入 / 完整读取 | 413 / 32 ms |
| 100 MiB SHA-256 | 114 ms |
| release 可执行文件 | 28,047,820 B（26.7 MiB） |
| `.app` / DMG | 28,568,232 / 11,089,392 B（27.2 / 10.6 MiB） |

## CI 与回归规则

- 四平台原生验收均写入 `reports/performance/<platform>.json` 并作为 `next-tauri-performance-*` artifact 上传。
- 发布工作流为每个平台写入 `reports/release/<target>.json`，与安装包一起上传，避免只比较压缩包文件名而没有体积证据。
- 当前基线先记录事实数据，不因共享 runner 波动设置硬失败阈值。连续三个版本数据齐备后再按平台制定冷启动、内存和数据处理回归预算。
- 数据为自动化场景；真实 DPI、多显示器、IME、授权提示和系统凭据行为仍按 [`platform-acceptance.md`](./platform-acceptance.md) 单独验收。

## 四平台 RC CI 基线

采集时间：2026-09-06；应用提交：`03ac809f`；版本：`0.1.0-rc.1`；来源：
[GitHub Actions run 33972861338](https://github.com/rememberber/MooTool/actions/runs/33972861338)。
四个平台均完成 25/25 工具、会话隔离和 100 轮、200 次 reparent 状态保持。

| 指标 | macOS x64 | macOS arm64 | Windows x64 | Linux x64 / Xvfb |
| --- | ---: | ---: | ---: | ---: |
| 原生验收总时长 | 117.2 s | 93.9 s | 93.2 s | 231.0 s |
| 进程创建至首个工具会话稳定 | 17,376 ms | 4,637 ms | 13,429 ms | 40,359 ms |
| 工具打开中位数 / 最大值 | 1,668 / 6,810 ms | 1,442 / 1,553 ms | 1,421 / 1,736 ms | 1,619 / 1,723 ms |
| 分离并收回中位数 | 540 ms | 517 ms | 561 ms | 5,515 ms |
| 100 轮 reparent 压力时长 | 49,409 ms | 42,038 ms | 39,582 ms | 49,307 ms |
| 空闲 / 1 / 10 / 25 工具进程树 RSS | 72.5 / 72.7 / 74.8 / 79.7 MiB | 103.6 / 104.0 / 105.6 / 107.5 MiB | 361.1 / 456.5 / 1,311.3 / 2,722.5 MiB | 20,523.6 / 33,331.0 / 124,022.3 / 242,243.7 MiB |
| 10 MiB JSON 验证 / 格式化 | 41.391 / 74.828 ms | 15.962 / 13.702 ms | 12.472 / 15.444 ms | 5.578 / 16.198 ms |
| 10,000 条 Quick Note 导入 / 读取 | 1,332 / 54 ms | 494 / 192 ms | 408 / 39 ms | 380 / 32 ms |
| 100 MiB SHA-256 | 704 ms | 395 ms | 117 ms | 92 ms |
| release 可执行文件 | 24.9 MiB | 24.0 MiB | 24.1 MiB | 33.3 MiB |

Linux 的内存列是 25 个独立 WebKit WebProcess 的进程树 RSS 求和；共享映射页会在每个进程中重复计入，
因此可能远大于 runner 的实际物理占用。该列只用于相同 Linux 采集方式下的版本趋势，不能与 macOS、
Windows 数字直接比较，也不能解释为整机已提交内存。

## RC 安装包体积

采集时间：2026-09-06；发布提交：`c03ef5c0`；来源：
[GitHub Actions run 34008294955](https://github.com/rememberber/MooTool/actions/runs/34008294955)。

| 平台与格式 | 字节数 | MiB |
| --- | ---: | ---: |
| macOS x64 DMG | 11,843,293 | 11.3 |
| macOS arm64 DMG | 11,283,086 | 10.8 |
| Windows x64 NSIS | 7,354,550 | 7.0 |
| Linux x64 AppImage | 87,894,520 | 83.8 |
| Linux x64 deb | 13,546,506 | 12.9 |

发布工作流还生成并上传 macOS `.app.tar.gz`、Windows NSIS 和 Linux AppImage 的独立 updater
签名文件，以及覆盖四个平台的 `latest.json`。Draft 中五个安装包的实际字节数与
`next-tauri-release.json` 一致，逐文件 SHA-512 校验通过。
