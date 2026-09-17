# Electron `qrTools.test.ts` ↔ Compose 对照

| Electron vitest | Compose 单测 |
| --- | --- |
| `generates a PNG data URL at all correction levels` | `QrEngineTest.roundTripsContentAtEveryCorrectionLevel` |
| `normalizes QR sizes to the settings boundary`（`20→120`、`360.4→360`、`9999→2000`） | `QrEngineTest.normalizesSizeAndRejectsEmptyOrBrokenImages`（含 `normalizeSize(360.4)`，[DIFF-526](../diff/526-http-pdf-merge-smoke-p7-slice.md)） |

来源：`next/src/features/qrcode/qrTools.test.ts`。
