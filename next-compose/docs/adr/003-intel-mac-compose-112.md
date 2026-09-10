# ADR-003：Intel Mac 上的 Compose 1.12.0

- 状态：已记录
- 日期：2026-09-09

## 背景

官方 Compose Multiplatform 1.12.0 平台表仅列 macOS 13 arm64。本轮开发机是 Intel Core i7（x86_64）+ Zulu JDK 21 x86_64。

## 决定

- 仍锁定 Compose 1.12.0 + Kotlin 2.2.21 + Gradle 8.14.3 + JDK 21，以便与规格候选组合一致。
- JVM 单测已在该 Intel 机器通过，说明依赖解析与 Skiko JVM 工件可用。
- 不把本机结果写成 macOS arm64 已支持。arm64 需在对应 runner 上构建。

## 后果

Intel Mac 为条件目标。若后续发现 Skiko UI 在 x86_64 运行失败，再评估降级 Compose 或仅支持 arm64。
