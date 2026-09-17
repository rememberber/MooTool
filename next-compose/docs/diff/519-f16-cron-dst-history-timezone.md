# DIFF-519：F16 Cron DST/`#` 单测 + 历史时区 JSON + 运行行偏移格式

## 背景

DIFF-518「未做」仍列 Cron 大切片。F16 引擎已有 Quartz 6/7 字段与基础单测（DIFF-004），但缺 **DST 春令时**、**`WED#2` 序数周** 登记；历史恢复仅读 plain `options` 时区，与 Electron `extraData` JSON 不一致；下次运行行使用 `GMT±N` 而非 Luxon `ZZZZ` 风格偏移。

## 行为

### F16 引擎

- `CronEngine.formatRunLine`：输出 `yyyy-MM-dd HH:mm:ss xxx`（如 `+08:00`）。
- 单测对齐 Electron `cronTools.test.ts` 并扩展：`ja-JP` describe、`0 0 12 ? * WED#2`、美东 `0 0 2 * * ?` 跨 2024-03-10 DST（**cron-utils 跳过不存在 02:00 的 3/10，下一跳为 3/11 02:00**；Electron `cron-parser` 可能给出 3/10 03:00，差异保留在 DIFF-004）。

### F16 UI · 历史

- 解析成功写入 history `options` 为 `{"timeZone":"…"}`（`CronHistoryMetadata`，对齐 Electron `extraData`）。
- 历史恢复：`parseTimeZone` 同时接受 JSON 与 legacy plain IANA；恢复后重算 `describe` 并清空 error。

## 验证

- `CronEngineTest` / `CronHistoryMetadataTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Cron 截图、分离窗手工、`L`/`#` 全表走查、六套 CSS 皮肤、P7 安装、HTTP/PDF 大切片、设置 Vault 数值失焦链。
