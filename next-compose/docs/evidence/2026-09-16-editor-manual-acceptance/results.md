# F01/F04 编辑器列编辑与系统 IME 手工验收说明（2026-09-16）

## 已自动化证据（不能代替系统 IME）

| 项 | 证据 | 说明 |
| --- | --- | --- |
| 列编辑引擎 + Alt 拖选 | `desktopTest` 中 `ColumnEditEngine` / `EditorHost` 相关用例 | 逻辑行、矩形选区 |
| JFrame 派发 + IME 提交 | `docs/evidence/2026-09-15-inspector-screencapture/windows/70-column-edit-jframe.png` | 测试窗内预编辑「中」提交；**非产品主窗** |
| 产品窗列选高亮 | `115-json-column-edit-product.png` | Alt 拖选，非写入 |
| 列选 ASCII 写入 | `122-json-column-type.png`、`125-quicknote-column-latch.png` | 键入 `x`，**不是**系统输入法 |
| 5 MiB setText | `docs/evidence/2026-09-09-p0-p1/` 等 | 大文档性能 |

## 待本机手工（通过后才可标 F04/F01 编辑器项已验收）

隔离数据目录（推荐 [DIFF-464](../../diff/464-vault-access-schema-editor-ime-prep.md)）：

```bash
cd /path/to/next-compose
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-ime-evidence-XXXX)"
./scripts/prepare-editor-ime-evidence.sh
./gradlew :composeApp:runDistributable
```

1. **macOS 系统输入法**（如拼音）：在 JSON / 随手记 `EditorHost` 聚焦，输入中文预编辑，确认预编辑不写入磁盘、提交后写入正确、快捷键在预编辑时让路（见 DIFF-064）。
2. **列编辑 + IME**：列选或闩锁后使用 IME 提交，确认多行同一逻辑列写入一致。
3. **分离/收回窗口**：列选与 undo 栈在拆出窗与主窗切换后仍保留（抽样）。

## 执行记录

- 本机 2026-09-16：**未执行**上述手工项；`acceptance.md` 中 F04/F01/P0 仍标记列编辑/IME 待验收。
