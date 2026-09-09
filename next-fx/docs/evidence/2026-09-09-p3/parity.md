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
