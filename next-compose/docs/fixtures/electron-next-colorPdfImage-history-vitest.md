# Compose 图片/PDF/调色板历史 metadata 登记

| caseId | Compose 验证 | 说明 |
| --- | --- | --- |
| color-format-legacy | `ColorHistoryMetadataTest` | 旧 `options=HEX_LOWER` |
| color-swap-pair | `ColorHistoryRestoreTest` | 输出 `#aaa / #bbb` 恢复主辅色 |
| image-asset-resolve | `ImageHistoryRestoreTest` | input 文件名匹配库内资源 |
| pdf-last-outputs | `PdfHistoryRestoreTest` | split/merge 输出路径列表 |

登记：[DIFF-536](../diff/536-f22-f24-history-mcp-slice.md)
