# DIFF-452：AI `NeedsRepair` 与卸载保留用户 Skill 文件

## 背景

Electron `repairs a moved runtime and missing files, then removes only managed content` 覆盖 runtime 迁移后的 `needs-repair`、缺 Skill 重装，以及卸载时保留用户自定义资源。Compose 状态机已支持，但缺可模拟 launch 变更的测试。

## 行为

- `AiIntegrationService.testLaunch`：测试覆盖 `McpLaunchResolver.clientLaunch`（模拟安装后 runtime 参数变化）。
- `mcpNeedsRepairWhenRuntimeLaunchChangesAndReinstallFixes`：旧 launch 安装 → 新 launch → `NeedsRepair` → 重装恢复 `Installed`。
- `codexSkillNeedsRepairWhenManagedFileMissing`：删除 `SKILL.md` → `NeedsRepair` → `Both` 重装。
- `uninstallPreservesUserFilesInSkillDirectory`：卸载移除托管 `SKILL.md` 与 MCP 段，保留 `my-resource.txt` 与用户 TOML 注释前缀。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（605/605）
