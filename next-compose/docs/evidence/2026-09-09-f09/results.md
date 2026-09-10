# 本轮验收记录

- 阶段/条目：F09 HTTP 请求（P5）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `HttpTool.tsx`、`httpTools.ts`、`networkService.buildRequestUrl`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **109/109**（含 HttpEngineTest 4；此前 F10 为 105/105）
- 语义：左集合/右 Method+URL+发送取消；Params/Headers/Cookies/Body 与响应三 Tab；冻结 GET/表单语义；cURL 只解析不执行；4xx 仍展示正文；10 MiB 上限；超时/取消；集合 JSON 持久化；历史与分离窗口
- 差异：DIFF-018（OkHttp 4.12.0、重复 Header 多值发送、本产品集合文件）
- 未测：窗口截图、真实外网、HTTPS 自签、代理认证对话框、响应另存文件、XML/HTML 高亮、安装镜像
- 下一轮：P5 其余（F20 翻译）
