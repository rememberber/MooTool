# 对照

基线：Electron `runtimeTools.ts` / `runtimeExecutionService.ts` / `networkService.ts` / `VariablesTool.tsx` / `HardwareTool.tsx`。

已对齐：`parseRuntimeArguments` 引号/转义 fixtures；Python 格式化 tab→4 空格；翻译分段与 `googleLanguage`；IPv4 与 WHOIS refer 跟随。

已知差异：

- Node 格式化不是 Prettier，只做空白规范化。
- 翻译仅 Google gtx；Bing、单词本、500ms 自动翻译未做。
- 用户环境变量只写入本产品 `environment/user.json`，不改 OS / launchctl / 注册表。
- 系统信息不是 systeminformation 全量；无 CPU%、序列号等字段时不填样例。
- Host 系统写入、HTTP 代理 UI 仍未做。
- 本机缺完整 Xcode，未跑 `flutter run -d macos`。
