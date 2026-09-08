# 对照

基线：Electron `httpTools.ts` / `networkService.ts` / `netTools.ts`。

已对齐：cURL 解析 fixtures、IPv4 `127.0.0.1` ↔ `2130706433`、重复 Query、真实 POST body、超时与 10MiB 上限错误码。

已知差异：

- HTTP 代理 UI 未接；cURL `-u`/`--proxy` 拒绝导入而不是静默丢认证。
- Host 方案仅存本产品目录；应用到系统需提权，本轮不标记成功。
- WHOIS、翻译、环境变量、代码运行未做。
- 本机缺完整 Xcode，未跑 `flutter run -d macos`。
