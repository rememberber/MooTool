# JSON 工作区与独立解析引擎

0.4.0 的界面由 SwiftUI / AppKit 实现。工具栏、主编辑器、可折叠检查器、查找替换栏、路径选择和转换结果弹窗，对照 Electron 的 `JsonTool.tsx`、`JsonToolbar.tsx`、`JsonInspector.tsx`、`JsonToolDialogs.tsx` 实现。

## 操作语义

- 格式化、压缩、高级格式化、键值互换和转义直接更新主编辑器，可撤销/重做，并保存到当前文档。
- JSONPath、JSON → XML、JSON → JavaBean 在结果弹窗中显示，可复制、导出、使用结果；使用结果前不会替换正文。
- XML / JavaBean → JSON 先输入来源内容，再转换并替换正文。
- 查找替换沿用 JavaScript 正则语义，支持大小写、全词、正则、前后导航、捕获组和换行替换；高亮使用 NSTextView 临时属性，不写入文档。
- 路径选择器填入 JSONPath；右键仍可复制 JSON Pointer。字面路径保留数字、引号、方括号和换行等特殊键名，复杂表达式继续交给 JSONPath-Plus。额外支持严格 JSON Pointer 查询。
- 窄编辑区将部分工具栏操作放入更多菜单，检查器通过弹出面板打开；界面采用 macOS 的菜单、分栏和弹窗。

## 依赖与产品边界

查询采用 [JSONPath-Plus](https://jsonpath-plus.github.io/JSONPath/docs/ts/index.html) 10.4.0，XML 转换采用 [fast-xml-parser](https://github.com/NaturalIntelligence/fast-xml-parser) 5.10.1。浏览器发行文件及许可副本全部位于本产品的 `Sources/MooToolNextCore/Resources/`，具体依赖版本及两个发行文件的 SHA-256 记录在 `parser-versions.json`。

`JSONTools.js` 是 MooTool 对应纯算法的独立副本，在本产品内维护，包含 Java 字段名/类名冲突处理等原生版修正。构建和运行不读取 `next` 源码或 `node_modules`，不要求 Node.js、Electron、浏览器或网络服务。JSON 数据不会发送到远端。

系统 [JavaScriptCore](https://developer.apple.com/documentation/javascriptcore/jscontext) 在随应用打包的 `MooToolJSONWorker` 中执行这些算法。用户输入通过私有临时文件中的 JSON 请求传入，作为函数参数读取；不拼接为脚本源码，不向脚本提供原生文件、网络或进程对象。JSONPath 使用 `eval: safe`。

辅助进程与主应用一同构建为对应架构、分别签名，并使用应用内部资源。每次调用结束删除临时目录，超时或取消终止该次辅助进程。关闭无关窗口不会取消另一窗口的操作；切换文档、恢复工作区或修改正文后，旧结果不会自动覆盖新内容。

## 能力边界

- 单次正文最多 10 MB，请求/结果最多 16 MB，JSON 最多 10 万节点、128 层；结构树和查询/查找结果最多 2 万项。父进程默认 3 秒超时；辅助程序自身另设 4 秒终止计时，即使主应用异常退出也会停止耗时表达式。
- JSON 数字按 JavaScript 的 Number 语义处理，与 Electron 一致；不提供任意精度数字保证。
- XML 使用严格语法校验，不接收 DTD/实体声明；XML → JSON 的属性使用 `@_` 前缀，重复元素为数组，值的数值/布尔推断沿用解析库。
- JavaBean → JSON 解析字段声明并生成示例值，不执行 Java，也不做完整 Java 编译器级类型分析。JSON → JavaBean 生成字段和嵌套类，包含 List 导入、关键字和命名冲突处理；混合类型数组使用 Object。类名与 String、List 等所用类型冲突时追加数字后缀。
- 窗口间同步正文；撤销记录属于当前编辑器，其他窗口写入正文时会清理本窗口可能已过期的撤销记录。重启不恢复撤销栈。
- Monaco 的全部快捷操作、折叠和编辑装饰尚未移植；文档库 Git、实际文件监听和 Finder 定位仍不在这一版范围内。

## 验证

核心测试对照 Electron 的输入输出用例，并覆盖通配符、递归、筛选、切片、联合查询、特殊键路径、重复键、XML 拒绝项、Java 命名、Unicode 查找替换、工作区兼容，以及辅助进程超时和取消。

本机另用 `javac` 实际编译辅助进程生成的 Java 文件，覆盖关键字、字段重名、嵌套类型、混合数组和 String/List/Object 命名冲突。

原生验收发送格式化快捷键并操作实际编辑器，验证原地修改、自动保存、撤销/重做、结果弹窗、使用结果、输入转换及文档切换；再通过新进程检查工作区恢复。界面截图包含检查器、查找栏、路径选择器、结果弹窗及深浅色/窄窗口。截图成功不等于所有菜单和系统对话框均已做端到端验证。
