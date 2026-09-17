# Electron `timeTools.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| epoch-shanghai | 秒级 epoch ↔ 本地时间（Asia/Shanghai） | `TimeEngineTest.timestampToLocal_*` |
| millis-utc | 毫秒 epoch ↔ UTC | `TimeEngineTest` |
| invalid-input | 非法时间戳/日期 | `TimeEngineTest` `assertFailsWith<TimeException>` |
| leap-day | 闰日往返 | `TimeEngineTest` |
| dst-america-new-york | 美东 DST 边界（与 Electron cron-parser 差异见 DIFF-519） | `TimeEngineTest.localToTimestamp_*America/New_York*` |

| local-roundtrip | 非法本地时间（Luxon 格式化 round-trip） | `TimeEngineTest.rejectsLocalTimeThatDoesNotRoundTripFormat` |
| history-options | 历史 zone/unit 恢复 | `TimeHistoryRestoreTest` |

登记：[DIFF-532](../diff/532-f15-f18-history-restore-time-roundtrip.md)（DIFF-531 初版登记仍有效）
