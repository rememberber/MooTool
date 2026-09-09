# 本轮验收记录

- 阶段/条目：F17 二维码（P4）
- 本产品工作树：仅 `next-compose/`
- 依赖：`com.google.zxing:core` **3.5.4**
- 参考：Electron `qrTools.ts`、`QrCodeTool.tsx`；架构 ZXing
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest`，**75/75** 通过（QrEngine 3；此前 F14 为 72/72）
- 语义：生成 PNG（尺寸 120–2000、纠错 L/M/Q/H、可选 Logo）、保存/复制图片、文件与剪贴板识别、中文往返、历史与分离窗口；设置写入默认尺寸/纠错
- 差异：DIFF-009（历史不存 PNG 本体）
- 未测：窗口截图、生成→系统剪贴板→识别的手工往返、透明 Logo 全部平台、安装镜像
- 下一轮：P4 其余（F22 调色板、F19 留言板、F23 图片、F24 PDF）
