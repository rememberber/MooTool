# 本轮验收记录

- 阶段/条目：F06 配置文件转换（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 依赖：`org.yaml:snakeyaml:2.3`
- 参考：Electron `configTools.ts`、`ConfigConvertTool.tsx`；路径展开对齐 Electron，冲突改为显式报错
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**62/62** 通过（ConfigEngine 6）
- 语义：Properties ↔ YAML、YAML 校验/格式化、导入导出、历史、分离窗口；标量列表逗号合并；失败保留原文
- 差异：DIFF-006（类型冲突报错；SnakeYAML 1.1 与注释不保留）
- 未测：窗口截图、锚点/自定义 tag、大文件、分离窗口手工、重启 UI
- 下一轮：F07 Protobuf；F04 Git 仍待做
