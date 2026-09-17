# 产品主窗验收数据准备（next-compose）

这些脚本**只准备隔离目录与 Git 状态**，不能代替 `docs/evidence/` 中的产品窗截图。

| 脚本 | 用途 | 环境变量 |
| --- | --- | --- |
| `prepare-p7-package-smoke.sh` | P7 打包前：toolchain、`printTooling`、`desktopTest --offline`（不代替三平台安装验收） | `JAVA_HOME`（JDK 21） |
| `prepare-vault-conflict-evidence.sh` | JSON Vault 外部磁盘冲突（§A） | 读取或创建 `MOOTOOL_COMPOSE_DATA_DIR` |
| `prepare-git-merge-conflict-evidence.sh` | JSON Vault Git merge 冲突（§B） | 同上；会重建 Vault 内 `.git` |
| `prepare-editor-ime-evidence.sh` | F01/F04 系统 IME 样本文件 | 同上 |

推荐用法（**不要** `eval "$(./script)"`，输出含 `#` 注释）：

```bash
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
./scripts/prepare-vault-conflict-evidence.sh    # 仅 §A
# 或
./scripts/prepare-git-merge-conflict-evidence.sh  # 仅 §B（勿与 §A 同目录先后混用）
# 或
./scripts/prepare-editor-ime-evidence.sh

cd /path/to/next-compose
./gradlew :composeApp:runDistributable
```

登记：`docs/evidence/2026-09-17-vault-conflict-product-window/results.md`、`docs/evidence/2026-09-16-editor-manual-acceptance/results.md`。
