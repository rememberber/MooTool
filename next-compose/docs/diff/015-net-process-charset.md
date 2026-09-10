# DIFF-015：网络命令使用 JVM 进程与 InetAddress

- 编号：DIFF-015
- 影响：F11 网络/IP
- 日期：2026-09-09

## 原行为（Electron）

`child_process.spawn(file, args)` 传 argv，不走 shell。DNS 用 Node `dns.promises.lookup({ all: true })`。命令 stdout 按 Buffer 默认 UTF-8 解码。界面先显示「正在执行…」，进程结束后一次性替换输出。

## 本产品行为

`ProcessBuilder` 同样按 argv 启动 `ifconfig`/`ping`/`netstat`/`dscacheutil` 等。DNS 用 `InetAddress.getAllByName`，A/AAAA 都保留，不把 IPv6 送进 IPv4 Long 转换。子进程输出按 `sun.jnu.encoding`（否则 JVM 默认字符集）解码，避免中文 Windows ping 结果乱码。spawn 类命令向左侧面板流式追加，可停止；ping 段扫描与端口扫描结束时仍输出与 Electron 相同的汇总文本。

## 理由

架构把网络/进程放在 IO 执行域，不能引入 Node。平台 ping 输出编码与 DNS 记录顺序属于运行时差异，不能用示例 IP 填失败结果。

## 证据

`NetEngineTest`：IPv4 `127.0.0.1` ↔ `2130706433`、0/最大值/越界/非法段；网段与端口解析；`localhost` 解析保留回环；非法主机名为 `INVALID_TARGET`；未知域名失败不含示例 IP；ping `127.0.0.1` 可取消；本机监听端口扫描为 open。`desktopTest` **97/97**。

## 受影响范围

- DNS 记录顺序、是否同时返回 IPv4/IPv6 与系统解析器有关，不保证与 Node lookup 逐行相同。
- macOS `/sbin/ping -c 4` 与 Linux `ping` 超时单位仍按 Electron 分支（Darwin `-W` 毫秒，Linux 秒）。
- WHOIS 依赖 `whois.iana.org:43`，离线时显示真实连接/超时错误。
- 刷新 DNS（`dscacheutil -flushcache` 等）在无权限时显示系统拒绝原因，不假装成功。
