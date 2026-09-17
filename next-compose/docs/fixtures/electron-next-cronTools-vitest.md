# Electron `next` · `cronTools.test.ts` → Compose 登记

| Vitest 用例 | Electron 源 | Compose 断言 |
| --- | --- | --- |
| builds-and-splits | `cronTools.test.ts` | `CronEngineTest.buildsAndSplitsQuartzStyleExpressions` |
| upcoming-runs-timezone | 同上 | `CronEngineTest.calculatesWeekdayRunsInTimezone` |
| year-field | 同上 | `CronEngineTest.filtersOptionalYearAndLeapDay` |
| describe-language | 同上 | `CronEngineTest.describesLanguageAndRejectsImpossibleSchedules`（含 `ja-JP`，[DIFF-519](../diff/519-f16-cron-dst-history-timezone.md)） |
| nth-weekday-dst | — | `CronEngineTest.nthWeekdayAndDstSpringForward`（`WED#2` + 美东 DST，[DIFF-519](../diff/519-f16-cron-dst-history-timezone.md)） |
| history-timezone-json | `CronTool` `extraData.timeZone` | `CronHistoryMetadataTest` + `CronHistoryRestore`（[DIFF-519](../diff/519-f16-cron-dst-history-timezone.md)、[DIFF-535](../diff/535-f08-session-f-tools-history-git-slice.md)） |
