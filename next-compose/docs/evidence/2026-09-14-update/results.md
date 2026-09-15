# 本轮验收记录

- 阶段/条目：A03 更新通道切片（检查 / 说明 / 下载 / SHA-512 / 打开安装包）
- 本产品工作树：仅 `next-compose/`
- 参考：`docs/data-platform-release.md` §9；Electron `updateService.ts`（electron-updater 自动安装）；Flutter 打开安装包
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **148/148**（含 UpdateEngineTest 5，0 skipped；此前 Vault 外部冲突为 143/143）
- 语义：只读 `products.next-compose`；缺失或非 active → Unpublished；稳定版忽略 prerelease；当前 OS/arch，darwin 只要 dmg；HTTPS 下载校验字节数与标准 Base64 SHA-512；成功写 `.ready`；失败不打开；`autoCheckUpdates` / `autoDownloadUpdates` 真实生效；不自动安装
- 差异：DIFF-027
- 未测：窗口截图与关于页手工操作、真实联网 feed、打开安装包、签名/公证、Windows/Linux 安装器、自动替换正在运行的应用
- 下一轮：JSON 完整检查器弹层，或 Git pull/push，或三平台发行登记
