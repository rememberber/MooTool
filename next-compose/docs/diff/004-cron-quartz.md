# DIFF-004：Cron 使用 Quartz 语义而非 JS cron-parser

- 编号：DIFF-004
- 影响：F16 Cron
- 日期：2026-09-09

## 原行为（Electron）

`cron-parser` 按 Unix 风格解析。`?` 会先改成 `*`，星期名 `SUN…SAT` 改成 `0…6`，再计算下次运行。因此「日」和「星期」可以同时为通配。年字段在解析后另行过滤。自然语言由 `cronstrue` 生成。

## 本产品行为

使用 `cron-utils` 9.2.1 的 **Quartz** 定义解析 6 字段（秒 分 时 日 月 星期）。`?`、`L`、`#`、`MON-FRI` 按 Quartz 处理，不把 `?` 改成 `*`。年字段仍按 Electron 的范围/步长规则事后过滤，并用**选定时区**的 `ZonedDateTime.year`。只接受 6/7 字段，5 字段 Unix 直接拒绝。自然语言来自 `CronDescriptor`，解析失败则不编造说明。

## 理由

规格要求默认 6/7 字段、IANA 时区、年过滤在选定时区完成，并明确不要宣称完整 Quartz 或与 Electron 逐字相同。Quartz 能保留 `?`/`L`/`#`；年过滤与 Electron 样本对齐。

## 证据

`CronEngineTest`：默认 `0 * * * * ?`、7 字段年、`MON-FRI` 在 `Asia/Shanghai` 得到 `2026-07-20 09:00:00`、`2028-01-01` 年过滤、闰日 `2028-02-29`、过去年份无下次运行、5 字段拒绝。

## 受影响范围

- 日与星期同时为 `*`（无 `?`）时 Quartz 会拒绝；Electron 因 `?`→`*` 可能算出结果。
- 自然语言措辞与 `cronstrue` 不同，但应仍能表达同一调度。
- 未实现 5 字段 Unix 模式。
