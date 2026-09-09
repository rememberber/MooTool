# DIFF-002：UA 引擎字段由规则推断

- 编号：DIFF-002
- 影响：F12 UA 分析
- 日期：2026-09-09

## 原行为（Electron）

`ua-parser-js` 自带 browser / engine / os / device 四套规则。Chrome 预设通常得到 engine `Blink`。

## 本产品行为

使用同源的 `uap-java` 1.6.1（`uap-core` 的 UA/OS/Device 规则）。该库**没有**独立 engine 表。引擎与版本由浏览器族与 UA 字符串推断：Chrome/Edge/Opera/Chromium → Blink；Firefox → Gecko；Safari → WebKit；其余为 `Unknown`。

浏览器名称做了归一：`Mobile Safari` → `Safari`；`Chrome Mobile` → `Chrome`；`iPhone OS`/`iPadOS` → `iOS`。空字段与 `Other` 显示为 `Unknown`。

## 理由

规格要求真实维护的 UA 规则，并用 fixtures 归一库名称差异，而不是手写全套 UA 正则。engine 为界面必要字段，因此在适配层补齐，而不是换一个更重、名称更发散的解析器。

## 证据

`UaEngineTest`：Chrome/Windows/Blink、Safari/iPhone/WebKit、Firefox/Gecko、Googlebot、空输入、未知 UA。

## 受影响范围

个别冷门浏览器的 engine 可能与 Electron `ua-parser-js` 不一致；Chrome/Firefox/Safari 预设应对齐。
