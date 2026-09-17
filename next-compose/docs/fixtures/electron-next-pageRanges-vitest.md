# Electron `pageRanges.test.ts` ↔ Compose F24

| Case | Electron | Compose |
| --- | --- | --- |
| token order + dedupe | `parsePageSelection` | `PdfEngine.parsePageSelection`（[DIFF-546](../diff/546-translate-http-pdf-style-f19-slice.md)） |
| odd/even/custom split | `selectSplitPages` | `PdfEngine.selectSplitPages` |

登记：`PdfEngineTest.parsesRangesAndKeepsSourceOrder`；导入 toast 见 `PdfImportPresentationTest`。
