# DIFF-020：代码运行使用 ProcessBuilder argv 与简化格式化

- 编号：DIFF-020
- 影响：F05 代码运行
- 日期：2026-09-09

## 原行为（Electron）

`child_process.spawn` 按 argv 启动 `java`/`groovy`/`python3|python`/`node`，不拼 shell。环境只保留 PATH、HOME、JAVA_HOME 等白名单。Java 用源文件模式（`java Main.java`）。Unix 以 detached 进程组 `kill(-pid)` 杀树；Windows `taskkill /T /F`。超时先 SIGTERM，约 1.2s 后再 SIGKILL。Node 格式化走 Prettier；Python/Groovy 为空白规范化。

## 本产品行为

`ProcessBuilder` 同样按 argv 启动，白名单环境、1 MiB 源码 / 2 MiB 输出上限、超时取 `settings.network.requestTimeoutMs`（clamp 1s–120s）。杀树用 `ProcessHandle.descendants` + `kill -TERM/-KILL -pid`（Windows 仍 `taskkill /T /F`）。Java 格式化复用已有 `ReformatEngine`（JavaParser）；Python/Groovy/Node 为 Tab→4 空格并去掉行尾空白，**不引入 Prettier**。临时目录写在本产品 `cache/runtime`，结束后删除。

## 理由

架构要求进程走 argv、不拼用户 shell。产品线独立不能依赖 Electron Node/Prettier。自带 app JRE 不含 javac；Java 源文件模式需要用户本机 JDK 11+。

## 证据

`CodeRunEngineTest`：引号参数解析、public 类型文件名、JavaParser 格式化；本机 Zulu 21 `java` 打印 42、参数 `Moo Tool|--flag`、工作目录生效；坏 requestId / 超 1 MiB / 超时 / 取消无限循环 / 64 字节截断。Python 3.9.6 与 Node v24.15.0 在已安装时打印 42；本机 PATH 无 Groovy，检测为不可用，不假成功。`desktopTest` **116/116**。

## 受影响范围

- 捆绑 runtime 的 `java` 不能代替用户配置的 JDK；缺编译器时启动失败或非零退出，页面显示真实原因。
- Node 格式化不是 Prettier AST，只做空白规范化。
- Unix 默认 `ProcessBuilder` 不新建进程组，负 PID 的 `kill` 可能无效，主路径依赖 `ProcessHandle` 后代终止。
- 未测安装镜像内运行、Windows `taskkill`、无限循环关闭应用收尾的手工 UI。
