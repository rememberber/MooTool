# 本轮验收记录

- 阶段/条目：F13 编码解码（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `encodeTools.ts`、`EncodeTool.tsx`
- 已执行：`./gradlew :composeApp:desktopTest`，**29/29** 通过（EncodeEngine 5）
- 语义：Unicode 按码点转 `\uXXXX`（增补平面拆成代理对）；URL 按 RFC 未保留字节百分号编码，UTF-8/GB2312；`+` 解码为空格；UTF-8 Hex 严格解码；ASCII 为十进制/十六进制码点列表。GB2312 不可映射字符报错，不写成 `?`。非法 Hex / 截断 UTF-8 报错并保留原文。
- jlink 增加 `jdk.charsets`，否则安装镜像没有 GB2312。本轮未重跑 `createDistributable`
- 未测：窗口截图、分离窗口手工、重启 UI、损坏 GB2312 多字节截断的手工核对
- 下一轮：F12 UA 或 F15 正则等其余 P3 工具；F04 Git 仍待做
