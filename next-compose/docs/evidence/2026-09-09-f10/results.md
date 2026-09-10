# 本轮验收记录

- 阶段/条目：F10 Host（P5）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `HostTool.tsx`、`systemService.normalizeHostsContent` / `writeHosts`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **105/105**（含 HostEngineTest 5；此前 F08 为 100/100）
- 语义：左方案列表/右编辑；方案 CRUD、复制、导入导出、查找替换；保存只写 `data/hosts/profiles.json`；应用到系统前 diff/备份/指纹冲突；无权限保持原文件；备份可恢复；历史与分离窗口
- 差异：DIFF-017（本产品方案存储、模板注释、并发指纹校验；条目校验比 Electron 更严）
- 未测：窗口截图、真实 `/etc/hosts`、osascript/pkexec 管理员对话框、Windows hosts、安装镜像、DNS 刷新是否真正生效
- 下一轮：P5 其余（F09 HTTP、F20 翻译）
