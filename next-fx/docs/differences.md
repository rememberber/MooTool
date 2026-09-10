# FX 主动差异

编号规则：`FX-Dxxx`。延期不是已完成。

## FX-D001 大整数 JSON 字面量

- 类型：行为修正
- 源行为：Electron `JSON.parse` 把 `9007199254740993` 变成 IEEE double，再 `JSON.stringify` 会丢精度。
- 本版行为：Jackson `JsonNode` 使用 `BigInteger`/`BigDecimal`，格式化/压缩保留该整数。
- 原因：功能规格要求 JSON 文本编辑不先转 Double。
- 验证：`JsonEngineTest.preservesIntegerLargerThanIeeeDouble`、`formatsThreeMegabyteObjectWithoutDroppingKeys`
- 后续：P2 在 UI 状态栏标明“已保留超出 JS 安全整数的字面量”。

## FX-D002 JSONPath 方言

- 类型：技术等价
- 源行为：jsonpath-plus（JS）。
- 本版行为：Jayway JSONPath 2.9.0 + Jackson `JsonNode`。
- 原因：不在 JavaFX 进程里跑 JS。
- 验证：`$.store.books[1].title` 返回 `"Two"`。filter/union/slice 仍待 P2 fixtures。
- 后续：不支持的语法必须报错，不能静默空结果冒充成功（当前 `SUPPRESS_EXCEPTIONS` 对缺失路径返回 `undefined`，与源码 `wrap:false` 缺失接近）。

## FX-D003 窗口装饰

- 类型：平台限制
- 源行为：Electron 一体化标题栏。
- 本版行为：P0 使用系统 `DECORATED` 标题栏。
- 原因：JavaFX 26 `EXTENDED`/`HeaderBar` 仍为 preview。
- 验证：主窗口可系统缩放/移动。
- 后续：P1 实验 HeaderBar，失败则保留本差异。

## FX-D004 安装器数字版本

- 类型：平台限制
- 源行为：Electron 可用 `0.1.0` 作为应用版本。
- 本版行为：`pom.xml` 产品版本仍为 `0.1.0-SNAPSHOT`；jpackage `--app-version` 映射为 `1.0.0`（`packaging/version-mapping.txt`）。
- 原因：JDK 25 macOS jpackage 拒绝首位为 `0` 的 `app-version`。
- 验证：`dist/MooTool Next FX.app` 的 `CFBundleShortVersionString` 为 `1.0.0`；jar 内 `version.properties` 仍为 `0.1.0-SNAPSHOT`。
- 后续：正式发 1.x 时取消映射或改为同一数字。

## FX-D005 六风格未实现

- 类型：延期
- 源行为：modern/quiet/hero/smartisan/miui-v5/claude。
- 本版行为：P1 仅 modern 浅/深 + 六强调色。其他风格在设置里说明原因，没有无效果下拉项。
- 原因：P6 才要求完整风格。
- 验证：外观页文案 `settings.pending.style`。
- 后续：P6 为每种风格提供独立 Token，禁止六名一色。

## FX-D006 URL 编码字符集实现

- 类型：技术等价
- 源行为：Electron `iconv-lite` 的 `utf-8` / `gb2312`，只对 RFC 3986 unreserved 以外的字节做 `%HH`。
- 本版行为：Java `StandardCharsets.UTF_8` 与 `Charset.forName("GB2312")`，同一套 unreserved 规则。
- 原因：不在 JavaFX 进程里引入 iconv-lite/Node。
- 验证：`EncodeEngineTest.roundTripsUrlTextInUtf8AndGb2312`；UTF-8 fixture `%E4%BD%A0%E5%A5%BD%20a%2Fb`。
- 后续：若发现生僻 GB 码位与 iconv-lite 不一致，补差异样本，不以静默替换冒充成功。

## FX-D007 正则超时不是独立 worker

- 类型：延期 / 技术等价
- 源行为：Electron 正则在渲染进程跑 JS `RegExp`，没有独立 worker。
- 本版行为：Java `Pattern` + 限时 `CharSequence`（默认 2s）和 10000 条匹配上限；UI 标明引擎为 Java Pattern。`global` 是 `Matcher.find` 遍历，不是 Java flag。
- 原因：Java `Pattern` 不响应 thread interrupt；限时 CharSequence 能在回溯中真正停下。规格仍要求独立 worker 进程。
- 验证：`RegexEngineTest.timesOutCatastrophicBacktracking`。
- 后续：独立 regex worker 进程、结果流式回传。当前不宣称灾难性回溯已按 P7 进程隔离完成。

## FX-D008 JS `\u{…}` 与 Java Pattern

- 类型：平台限制
- 源行为：无 `u` 旗标时 JS 把 `\u{1F680}` 当成 `u{1F680}` 字面量。
- 本版行为：同一输入对 Java `Pattern` 是非法转义，报 `regex.invalid`，不静默改写成字面量。
- 原因：默认引擎是 Java Pattern，差异必须可见。
- 验证：`RegexEngineTest.recordsJsStyleUnicodeBraceEscapeAsJavaSyntaxError`。
- 后续：若提供 JS 引擎模式，单独标明，不混进默认 Java 结果。
