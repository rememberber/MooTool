# Electron `hardwareTools.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| mask-serial | 序列号默认遮蔽 | `HardwareEngineTest` `mask` / `displayValue` |
| format-bytes | 存储容量展示 | `HardwareEngineTest.formatBytes_*` |
| format-duration | 运行时长 | `HardwareEngineTest.formatDuration_*` |
| collect-sample | OSHI 采集快照结构 | `HardwareEngineTest.collect_*` |

登记：[DIFF-531](../diff/531-tools-defaults-live-git-keywords-slice.md)
