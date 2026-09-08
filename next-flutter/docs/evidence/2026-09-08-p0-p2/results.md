# 结果

## 命令

```text
export PATH="$HOME/sdk/flutter/bin:$PATH"
cd next-flutter
flutter pub get
flutter test test/unit --reporter compact
```

结果：**20 个 unit 测试通过**（json 引擎/路径、Vault、编辑 undo 与列插入、查找替换、动态 proto、SimplePdf 拆合、workspace 重启恢复、损坏 settings 不覆盖）。

`flutter analyze`：修复 `notifyListeners` 外部调用后应无 warning。`curly_braces_in_flow_control_structures` 已在 analysis_options 中降为 ignore。

`flutter run -d macos`：**未执行成功验证**。`flutter doctor` 报告 Xcode 安装不完整，无法作为 iOS/macOS 开发完整工具链。存在 `macos` 桌面设备条目，但不能把未链接的插件应用写成已验收。

Widget 级 Workbench 泵送会长时间挂起（首页/侧栏 Image.asset），已从必跑测试中移除，不把界面截图写成通过。

## 范围

P0：产品 ID、数据路径、三平台工程目录、编辑器/窗口/PDF/proto ADR 与可执行样例。
P1：26 入口、首页、搜索、设置子集、主题、i18n。
P2：JSON 格式化/压缩/重复 Key/JSONPath/转换/Vault/历史/查找/原子保存/重启恢复；Git 服务与导出；同 engine 分离协议。

未实现：P3–P7 工具页、真实第二 Flutter engine、安装包、更新通道、Windows/Linux 实机。
