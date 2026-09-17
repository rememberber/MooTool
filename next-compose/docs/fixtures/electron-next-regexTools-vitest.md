# Electron `regexTools.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| capture-groups-global | `(moo)(\\d+)` 全局捕获组位置 | `RegexEngineTest.returnsMatchPositionsAndCaptureGroups` |
| zero-width-global | `(?=a)` 零宽不死循环 | `RegexEngineTest.handlesZeroWidthGlobalExpressionsWithoutLooping` |
| catalog-21 | `commonRegexes` 长度 21 | `RegexElectronCatalogTest` + `RegexEngineTest.keepsJavaCommonPatternCatalogAndNamedGroups` |

登记：[DIFF-532](../diff/532-f15-f18-history-restore-time-roundtrip.md)
