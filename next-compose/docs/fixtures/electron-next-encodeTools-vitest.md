# Electron `encodeTools.test.ts` → Compose

| Electron | Compose |
| --- | --- |
| `urlEncode` / `urlDecode` UTF-8 & GB2312 | `EncodeEngineTest.roundTripsUrlTextInUtf8AndGb2312` |
| `toUnicode` / `fromUnicode` | `EncodeEngineTest.roundTripsUnicodeAndUtf8Hex` |
| `textToHex` / `hexToText` | 同上 |
| `textToAscii` decimal/hex | `EncodeEngineTest.convertsCodePointsToDecimalOrHexLists` |
| MCP `mootool_encode` url | `MooToolMcpToolsTest.encodeUrlRoundTripViaMcp`（[DIFF-549](../diff/549-f-tools-encode-qr-css-mcp-slice.md)） |
| F13 历史 `options` JSON | `EncodeHistoryMetadataTest` / `EncodeHistoryRestoreTest` |
