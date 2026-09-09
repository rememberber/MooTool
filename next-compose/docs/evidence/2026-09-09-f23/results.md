# 本轮验收记录

- 阶段/条目：F23 图片助手（P4）
- 本产品工作树：仅 `next-compose/`
- 依赖：ImageIO/Java2D；内嵌 ImageTracer.java 1.1.2（Unlicense）
- 参考：Electron `ImageTool.tsx`、`imageTools.ts`、`imageRepository.ts`、`imageVectorizationService.ts`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **90/90**（含 ImageEngineTest 3）
- 语义：左图片库/中画布/上操作/下缩放；导入文件/剪贴板/Base64；压缩与水印真实写出；SVG 真 path；区域截图拒绝全黑；会话与分离窗口；历史
- 差异：DIFF-013（ImageTracer.java vs vtracer；拖选截图无缩放手柄）
- 未测：窗口截图、多屏/录屏权限对话框、WebP、超 16MP、安装镜像
- 下一轮：P5 网络/系统，或用户指定的下一工具
