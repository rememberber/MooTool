# 产品主窗验收数据准备（next-compose）

这些脚本**只准备隔离目录与 Git 状态**，不能代替 `docs/evidence/` 中的产品窗截图。

| 脚本 | 用途 | 环境变量 |
| --- | --- | --- |
| `prepare-p7-package-smoke.sh` | P7 打包前：toolchain、`printTooling`、`verifyNativePackageMetadata`、`desktopTest --offline`；`MOOTOOL_P7_BUILD_DIST=1` 时额外跑本机 `packageDistributionForCurrentOS`（Windows/Linux 安装包须在对应 OS 构建） | `JAVA_HOME`（JDK 21）、可选 `MOOTOOL_P7_BUILD_DIST=1` |
| `prepare-vault-git-settings-evidence.sh` | 设置 Vault/Git 字段 + JSON Vault Git 面板走查（file:// bare remote） | 读取或创建 `MOOTOOL_COMPOSE_DATA_DIR` |
| `prepare-vault-conflict-evidence.sh` | JSON Vault 外部磁盘冲突（§A） | 读取或创建 `MOOTOOL_COMPOSE_DATA_DIR` |
| `prepare-git-merge-conflict-evidence.sh` | JSON Vault Git merge 冲突（§B） | 同上；会重建 Vault 内 `.git` |
| `prepare-editor-ime-evidence.sh` | F01/F04 系统 IME 样本文件 | 同上 |
| `prepare-tray-screencapture-evidence.sh` | 托盘/屏幕录制 TCC 手工走查说明 + 恢复基线 PNG | 同上 |
| `verify-product-evidence-prep.sh` | 非交互校验上述准备脚本（无 GUI） | 无 |
| `lib/product-evidence-common.sh` | 共享目录解析、merge 冲突断言、可选 HTTP 公网 smoke 提示 | — |

推荐用法（输出均为 `#` 注释 + `export MOOTOOL_COMPOSE_DATA_DIR=…` 单行，**不要** `eval "$(./script)"`）：

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

CI/本机无 GUI：`./scripts/verify-product-evidence-prep.sh` 或 `ProductEvidencePrepScriptTest`（见 [DIFF-513](../docs/diff/513-product-evidence-prep-verify.md)）。

可选 F09 公网 smoke（仅 httpbin/localhost，默认 `desktopTest --offline` **不跑**）：`MOOTOOL_HTTP_PUBLIC_SMOKE=1` + `HttpEngineTest.optionalHttpBinPublicGetSmoke`（见 [DIFF-526](../docs/diff/526-http-pdf-merge-smoke-p7-slice.md)）。multipart POST：`MOOTOOL_HTTP_MULTIPART_SMOKE=1` + `optionalHttpBinMultipartPostSmoke`（见 [DIFF-528](../docs/diff/528-http-multipart-editor-tray-git-slice.md)）。

登记：`docs/evidence/2026-09-17-vault-conflict-product-window/results.md`、`docs/evidence/2026-09-16-editor-manual-acceptance/results.md`、`docs/evidence/2026-09-17-tray-tcc-screencapture/results.md`。
