# electron-next-jsonQuickNoteQr-history-vitest

| id | product | source | compose test | notes |
| --- | --- | --- | --- | --- |
| json-history-apply | MooTool Next Electron | `JsonTool.tsx` `onApplyHistory` | `JsonHistoryRestoreTest` | 仅写回 output 正文 |
| json-path-history | Compose | JSONPath 双击历史 | `JsonHistoryRestoreTest.pathQueryRestoresResultWithoutEditorMutation` | marker `pathQuery` |
| quicknote-save-history | Electron 无通用历史 UI | F01 `HistoryBrowser` | `QuickNoteHistoryRestoreTest` | options=相对路径 |
| qrcode-generate-history | `QrCodeTool.tsx` | `extraData` + data URL output | `QrHistoryRestoreTest.generateRestoresFromDataUrlOutput` | [DIFF-538](../diff/538-json-quicknote-qr-history-git-slice.md) |
| qrcode-recognize-history | 同上 | recognize branch | `QrHistoryRestoreTest.recognizeRestoresTabAndFields` | |

登记：[DIFF-538](../diff/538-json-quicknote-qr-history-git-slice.md)
