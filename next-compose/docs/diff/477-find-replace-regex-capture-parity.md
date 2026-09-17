# DIFF-477：查找替换正则捕获组与 Electron 对齐

## 背景

Electron `findReplace.ts` 在 `replaceAllMatches` / `replaceCurrentMatch` 中对每个匹配片段调用 `applyReplacement`，支持正则 `$1`/`$&` 与替换串中的 `\n` 转义；非正则模式保留字面 `\n`。Compose `FindReplace.replaceAll` 曾对所有命中写入同一展开串，且走 Kotlin `Regex.replace` 模板语义，导致 Host/JSON/随手记正则替换与 Electron 不一致。

## 变更

- `FindReplace.applyReplacement` + `substituteReplacementGroups`：逐匹配展开 `$n`/`$&`；非正则直接写入替换文本。
- `FindReplaceTest.mirrorsElectronFindReplaceVitestBasics` 对照 `next/src/shared/components/findReplace.test.ts`。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
