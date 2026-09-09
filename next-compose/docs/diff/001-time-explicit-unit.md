# DIFF-001：时间戳转换以显式单位为准

- 编号：DIFF-001
- 影响：F18 时间转换
- 日期：2026-09-09

## 原行为（Electron）

`next/src/features/time/timeTools.ts` 的 `timestampToLocal` 在输入去掉符号后长度 ≥ 13 时，把单位改成毫秒，覆盖调用方传入的秒/毫秒选择。因此 `1704067200000` + 秒 会被当成毫秒，得到 `2024-01-01 00:00:00`。

Luxon 对 DST 重叠时刻通常静默选取一个偏移；缺失时刻依赖 `isValid`，界面只提示通用“本地时间无效”。

## 本产品行为

Compose 实现**不按位数改写单位**。用户选择秒时，`1704067200000` 按秒解释，得到远离 2024 的日期。非法日期（如 2 月 31 日、非闰年 2 月 29 日）用 `java.time` `ResolverStyle.STRICT` 拒绝进位。DST 间隙返回 `dst-gap`，重叠返回 `dst-overlap`，界面分别显示，而不是只弹出通用失败。

## 理由

规格（`docs/feature-parity.md` F18）要求显式单位优先，且 DST 缺失/重复必须有明确解释。Electron 的长度启发式会让“秒”选择在 13 位输入上失效。

## 证据

`TimeEngineTest.keepsExplicitSecondUnitForThirteenDigitInput`、`explainsDstGapAndOverlap`、`rejectsInvalidDatesAndFormatsTimezoneLabels`。

## 受影响范围

- 同一串数字在 Electron 与 Compose 上可能得到不同本地时间，取决于所选单位。
- DST 重叠不再静默成功；用户需改用时间戳或避开重叠钟点。
- Java Swing 版若仍按毫秒启发式，同样与本产品不同；不在本 DIFF 中对齐 Java。
