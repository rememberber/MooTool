# Electron `networkService.integration.test.ts` ↔ Compose F20

| Case | Electron | Compose |
| --- | --- | --- |
| split long text | `splitTranslationText` | `TranslationEngine.splitTranslationText` |
| provider order + cooldown | `translationProviderOrder` | `TranslationEngine.translationProviderOrder` |
| restore 不重发 | UI restore | `TranslationAutoPresentation.skipAutoTranslate` + `TranslationHistoryRestore` |
| 过期响应丢弃 | in-flight seq/id | `TranslationResponsePresentation`（[DIFF-547](../diff/547-http-translation-net-style-git-slice.md)） |

登记：`TranslationEngineTest`；自动 debounce 见 `TranslationAutoPresentationTest`（[DIFF-546](../diff/546-translate-http-pdf-style-f19-slice.md)）；在途结果见 `TranslationResponsePresentationTest`（[DIFF-547](../diff/547-http-translation-net-style-git-slice.md)）。
