# DIFF-014：系统信息使用 OSHI 采集

- 编号：DIFF-014
- 影响：F25 系统信息
- 日期：2026-09-09

## 原行为（Electron）

`systeminformation` 一次拉 OS/CPU/内存/磁盘/网卡。序列号 `mask`：长度 ≤4 原样，否则前 2 + `****` + 后 2。无通用历史。刷新为手动，不自动轮询。

## 本产品行为

OSHI **6.8.2** 读取本机 HAL/OS。字段标签与 Electron 对齐。序列号同样遮蔽，需勾选「显示序列号」后才复制明文。系统 Tab 增加「本产品运行时」组，避免把 JVM/`java.home` 写成操作系统字段。切走页面时取消进行中的采集。内存 **Active** 在 OSHI 无对应计数时显示 `-`，不填 0。

## 理由

架构指定 OSHI/JNA，Java 产品已用同一路线。不引入 Node `systeminformation`。

## 证据

`HardwareEngineTest`：mask/字节/时长 fixture；本机采集主机名、逻辑核心 ≥1、内存总量非 0、序列号敏感且默认遮蔽。`desktopTest` **92/92**。

## 受影响范围

- CPU 负载采样约 200 ms，与 Electron `currentLoad` 瞬时值不同。
- 磁盘 interface/vendor 若 OSHI 未提供则省略或 `-`。
- 未跑安装镜像内的 JNA native 加载，也未测无权限平台的每一字段。
