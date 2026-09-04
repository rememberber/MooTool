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
