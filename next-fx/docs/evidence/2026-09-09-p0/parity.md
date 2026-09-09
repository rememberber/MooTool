# 对照

| Case / 子项 | 源观察 | FX 结果 | 状态 |
| --- | --- | --- | --- |
| 26 Tool ID 与分组顺序 | Electron `toolRegistry.ts` | `ToolRegistryTest` | 通过（注册表） |
| JSON 格式化/压缩往返 | `JSON.stringify` 空白可能不同 | 压缩后与输入等价；pretty 空白不强制逐字节相同 | 通过（算法）；UI 待 P2 |
| 大整数 `9007199254740993` | Electron `JSON.parse` 丢精度 | Jackson BigInteger 保留 | 通过，FX-D001 |
| 重复 key | 源 DuplicateKeyParser | Map 折叠前检测 | 通过（算法） |
| JSONPath `$.store.books[1].title` | jsonpath-plus | Jayway 返回 `"Two"` | 通过；filter/union 待 P2，FX-D002 |
| 3MiB JSON | 源可处理大文档 | 单元测试格式化不丢 key | 通过（算法）；UI 忙态待 P2 |
| 窗口默认 1440×920 / 最小 1080×720 | Electron BrowserWindow | `MainWindow` 常量一致 | 代码对齐；视觉未截图 |
| 工具窗口关闭收回 | 不销毁 WebContentsView | `ToolWindowCoordinator` 转移同一 Node | 代码存在；50 次转移未测 |
| 系统标题栏 | Electron 一体化 chrome | `StageStyle.DECORATED` | FX-D003 |
| 产品数据隔离 | 各自 Application Support | `AppPaths` + `product.json` | 单元测试通过；release smoke 写本产品目录 |
| E01 IME | 真实桌面 | 未测 | 待测 |
| E03 窗口转移 | 真实多窗口 | 未测桌面 | 待测 |
| E21 无系统 Java | 干净用户 | 本机 PATH 精简后进程可活；机器仍有 `/usr/bin/java` stub | 部分 |
