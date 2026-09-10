# DIFF-003：正则引擎使用 Java Pattern

- 编号：DIFF-003
- 影响：F15 正则
- 日期：2026-09-09

## 原行为（Electron）

`next/src/features/regex/regexTools.ts` 用 JavaScript `RegExp`。`global` 是 `g` flag；`ignoreCase`/`multiline`/`dotAll` 对应 `i`/`m`/`s`。空模式会按 JS 规则产生零宽匹配。灾难性回溯会卡住渲染进程，没有独立超时。

## 本产品行为

默认引擎是 **Java Pattern**，界面显示「引擎：Java Pattern」。`global` **不是** Java flag，由 `Matcher.find` 循环实现；零宽匹配把搜索起点前进 1，避免死循环。忽略大小写同时打开 `CASE_INSENSITIVE` 与 `UNICODE_CASE`。空模式不匹配，避免对整段文本产生大量零宽结果。命名组使用 Java `(?<name>…)`，结果里额外展示命名捕获（Electron 当前只展示序号组）。匹配在独立 JVM worker 中执行，默认 2s / 10000 条上限，超时 `destroyForcibly`；worker 起不来时界面报错，**不回退到本进程匹配**。

## 理由

规格要求 Java Pattern 为首选，并必须能终止灾难性回溯。不能宣称与 JS `RegExp` 完全兼容。21 条常用模式从 `regexTools.ts` 原样复制；其中 `htmlId` 的 lookbehind 在 Java 可用。

## 证据

`RegexEngineTest`：捕获组、零宽 `(?=a)`、命名组、emoji UTF-16 下标、非法 `(`、dotAll、空模式、htmlId lookbehind。`RegexWorkerClientTest`：正常匹配与 `(.*a){28}` 超时杀死（Java 21 对经典 `(a+)+$` 已能很快拒绝，改用仍会指数回溯的计数重复）。`StorageIsolationTest.regexFavoritesPersistAcrossStoreInstances`。

## 受影响范围

- JS 独有或语义不同的写法（例如某些 Unicode 属性转义、`\w` 在未开 Unicode 类时的差异）可能与 Electron 结果不同。
- 空模式在 Electron 可能产生零宽匹配，本产品返回 0 条。
- 超时/上限是本产品新增保护，Electron 没有对等行为。
