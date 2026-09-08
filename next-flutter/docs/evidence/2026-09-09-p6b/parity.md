# 对照

基线：Electron 11 类设置、备份快照、留言板防休眠、HTTP 代理/超时、运行时路径。

已对齐：设置分类顺序与字段可持久化；备份复制 product/settings/workspace/vaults/images/environment/database/workspace，不含 cache；HTTP 超时与代理配置接入 HttpSender；运行时/翻译超时读取设置。

已知差异：

- 托盘、自动更新、关闭主窗口 ask/hide/quit、启动最大化只保存偏好，尚未接到 OS。
- InterfaceStyle 六种可存盘，视觉仍是 modern token。
- Git token / 代理密码写入本产品 `settings.json`（非空才写），不是独立 secret store。
- macOS 截图/取色通道返回未实现；防休眠 Swift 未在本机用 Xcode 验证。
