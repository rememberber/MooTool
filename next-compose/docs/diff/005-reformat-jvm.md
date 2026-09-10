# DIFF-005：格式化使用 JVM 解析器而非 Prettier

- 编号：DIFF-005
- 影响：F03 格式化
- 日期：2026-09-09

## 原行为（Electron）

`prettier/standalone`：Java 走 `prettier-plugin-java`，XML 走 `@prettier/plugin-xml`（`xmlWhitespaceSensitivity: preserve`），HTML 走 `prettier/plugins/html`。语法错误由 Prettier 抛出。Nginx 为自写 tokenizer，不经 Prettier。

## 本产品行为

- Nginx：移植 Electron `tokenizeNginx` / `formatNginx`，引号、注释、转义、块层级一致。
- Java：`javaparser-core` 3.26.4 解析 + PrettyPrinter，缩进 1–8 空格生效；语法错误抛出行列，不改原文。
- XML：JAXP 解析（禁用 DTD/外部实体）+ 自写缩进；元素子节点缩进，混合文本节点保持同行；`DOCTYPE` 拒绝。
- HTML：Jsoup 解析后按同样规则缩进。Jsoup 对残缺 HTML 会修复树，因此**不把修复当失败**；无法像 Prettier 那样对所有畸形 HTML 给出语法错误行列。

不要求使用者安装 Node，也不用通用花括号换行冒充多语言 formatter。

## 理由

架构要求独立声明的 Java 语法处理器和专用 HTML/XML/Nginx 实现，禁止把 Electron Prettier 当运行时依赖。JavaParser 能按用户选择的缩进重排，并在解析失败时定位。

## 证据

`ReformatEngineTest`：Electron Nginx fixture 逐字一致；Java 样本含 `class Demo` 与 `System.out.println("moo");` 且二次格式化幂等；XML `<item` 缩进与文本节点 `moo 文本`；HTML 含 `<strong>Moo</strong>`；非法 Java/XML 抛 `syntax` 且带行号。

## 受影响范围

- Java 换行/空行风格与 prettier-plugin-java 不同，语义由 AST 重印决定。
- 畸形 HTML 可能被 Jsoup 修好后输出，而不是保留原文并报错。
- 含 `DOCTYPE` 的 XML 被拒绝（防 XXE），Electron Prettier 可能仍格式化。
