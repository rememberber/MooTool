# Compose Host/UA/格式化/运行台历史登记

| caseId | Compose 验证 | 说明 |
| --- | --- | --- |
| host-apply-content | `HostHistoryRestoreTest` | apply 历史恢复正文 |
| ua-parse-json | `UaHistoryRestoreTest` | output JSON + `UaParse` marker |
| reformat-pipe | `ReformatHistoryMetadataTest` | `type\|tab\|indent\|file` |
| reformat-text-restore | `ReformatHistoryRestoreTest` | 文本 Tab 恢复 output |
| runtime-options-json | `CodeRunHistoryMetadataTest` | arguments/workingDirectory/runtime |
| runtime-tab-restore | `CodeRunHistoryRestoreTest` | Node Tab + 源码 |

登记：[DIFF-537](../diff/537-host-ua-reformat-runtime-history-slice.md)
