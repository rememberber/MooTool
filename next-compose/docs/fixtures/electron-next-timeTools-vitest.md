# Electron `timeTools.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| epoch-shanghai | 秒级 epoch ↔ 本地时间（Asia/Shanghai） | `TimeEngineTest.timestampToLocal_*` |
| millis-utc | 毫秒 epoch ↔ UTC | `TimeEngineTest` |
| invalid-input | 非法时间戳/日期 | `TimeEngineTest` `assertFailsWith<TimeException>` |
| leap-day | 闰日往返 | `TimeEngineTest` |
| dst-america-new-york | 美东 DST 边界（与 Electron cron-parser 差异见 DIFF-519） | `TimeEngineTest.localToTimestamp_*America/New_York*` |

登记：[DIFF-531](../diff/531-tools-defaults-live-git-keywords-slice.md)
