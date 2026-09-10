# 本轮验收记录

- 阶段/条目：F20 翻译（P5）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `TranslationTool.tsx`、`networkService.translate`、`translationLanguageCodes`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **112/112**（含 TranslationEngineTest 3；此前 F09 为 109/109）。HttpEngine 超时分类改为优先 TIMEOUT（OkHttp callTimeout 会先 cancel）
- 语义：翻译/单词本/历史三 Tab；源/目标语言、Google/Bing、交换、自动 500ms debounce 与手动立即发；过期响应不覆盖；回填不重复请求；分段/并发保序、fallback 与冷却、取消/超时；单词本 CRUD/搜索/重译；历史 JSON 持久化；分离窗口
- 差异：DIFF-019（OkHttp 4.12.0、本产品 `data/translation/*.json`、无通用历史）
- 未测：窗口截图、真实 Google/Bing 联网、代理认证对话框、安装镜像
- 下一轮：P5 引擎层已闭环；下一刀为 P6（随手记/Git/代码运行/备份）或补截图与真实联网
