# 本轮验收记录

- 阶段/条目：F11 网络/IP（P5）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `NetTool.tsx`、`netTools.ts`、`systemService` 网络命令
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **97/97**（含 NetEngineTest 5；此前 F25 为 92/92）
- 语义：左输出/右功能区；IPv4↔Long（0/最大值/越界/非法段）；localhost 解析保留 IPv4/IPv6；本机地址；argv 启动 ping/ifconfig/netstat；流式输出可停止；本机端口扫描见 open；失败不含示例 IP；历史与分离窗口
- 差异：DIFF-015（`InetAddress` vs Node lookup；子进程用 `sun.jnu.encoding`；Compose 流式输出）
- 未测：窗口截图、WHOIS 在线、中文 Windows ping 编码、安装镜像、无网环境逐项
- 下一轮：P5 其余（F08 环境变量、F09 HTTP、F10 Host、F20 翻译）
