# 源码基线

| 项目 | 取值 |
| --- | --- |
| Electron 对照 | `next/` 1.1.4，工具 ID / 分组 / 默认窗口尺寸来自 `toolRegistry.ts`、`index.ts`、`toolWindowManager.ts` |
| JSON 算法对照 | `next/src/features/json/` 与 `jsonTools.ts` 的格式化/压缩/重复 key/JSONPath |
| 本产品身份 | `productId=next-fx`，显示名 `MooTool Next FX`，Bundle ID `com.rememberber.mootool.next.fx`，Windows UpgradeCode `500dc26c-8050-4b1e-b7c6-691e1229bc00` |
| Maven 坐标 | `com.rememberber.mootool:mootool-next-fx:0.1.0-SNAPSHOT` |
| Java 包 | `com.rememberber.mootool.nextfx` |
| 编辑器 | RichTextFX 0.11.7 `CodeArea`（ADR 002） |
| JSON 库 | Jackson 2.19.2 + Jayway JSONPath 2.9.0（ADR 004） |
| 数据库 | sqlite-jdbc 3.53.2.1，schema v1 |
| 打包 | jlink（保留 `bin/java`）+ jpackage `app-image`；macOS 安装器数字版本映射为 `1.0.0`（见 `packaging/version-mapping.txt`） |
