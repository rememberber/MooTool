# 对照

| 项 | 源（Electron `encodeTools.ts` / `EncodeTool.tsx`） | FX | 状态 |
| --- | --- | --- | --- |
| Unicode 往返 | `Array.from` 码点 + `\uXXXX`，补充平面拆 UTF-16 | 同语义 | 核心通过（单测含 emoji） |
| URL UTF-8 / GB2312 | iconv-lite + unreserved `%HH` | Java Charset，同一 unreserved | 核心通过；见 FX-D006 |
| Hex UTF-8 | `TextEncoder` / fatal `TextDecoder` | UTF-8 bytes + `CodingErrorAction.REPORT` | 核心通过 |
| ASCII 十进制/十六进制列表 | 码点空格分隔，hex 可混写 | 同语义 | 核心通过 |
| 四 Tab 双栏转换 | unicode/url/hex/ascii | 同 Tab 与左右栏 | 开发中（有真实转换，未拍成对截图） |
| 历史 | `extraData` JSON 恢复 Tab/方向 | schema v3 `extra_data` | 核心通过（存储单测） |
| 草稿重启 | 会话状态 | `tool_draft` encode JSON | 已接通；本轮无 UI 重启截图 |
| Base64 | 不在本工具（在 crypto） | 未做假按钮 | 通过（未宣称） |

## F15 正则

| 项 | 源（Electron `regexTools.ts` / `RegexTool.tsx`） | FX | 状态 |
| --- | --- | --- | --- |
| 捕获与位置 | `RegExp.exec` + global 循环 | `Matcher.find` 遍历 | 核心通过（Electron fixture） |
| 零宽匹配 | `lastIndex += 1` | 空匹配后 `start + 1` | 核心通过 |
| 21 常用模式 | `commonRegexes` | 同 id/pattern | 核心通过 |
| 引擎 | JS `RegExp` | Java Pattern，页脚标明 | 有意差异 |
| 超时 | 无 | 限时 CharSequence + 上限 | 开发中；worker 延期 FX-D007 |
| `\u{1F680}` | 无 `u` 时当字面量 | 非法转义报错 | FX-D008 |
| 收藏 | kind=regex，无文件夹的简单列表 | `favorite_entry` 增删/重启 | 核心通过（存储单测）；文件夹待 Cron/调色板 |
| 历史 | extraData 含 options | extra_data JSON | 已接通 |
