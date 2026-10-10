# MooTool Next Electron

The Electron + Vite + React + TypeScript desktop edition of MooTool.

## Design Direction

The first shell follows the supplied macOS-style references:

- light, native-feeling sidebar with hidden-inset traffic lights
- quiet 1px separators and low-contrast hover states
- large central workspace with generous empty space
- rounded command/input surface as the primary interaction area
- icon-first navigation with restrained labels
- renderer kept separate from local system capabilities

## Commands

```bash
npm install
npm run dev
npm run typecheck
npm run build
```

## Developer command palette

Press **Cmd+K** on macOS or **Ctrl+K** on Windows/Linux inside the main MooTool window, or click the sidebar search button. The palette searches both tool pages and executable actions. Use arrow keys to select an entry and Enter to run it. Tool-name searches retain tool pages as the first result.

Actions preview their result before copying. Enter on **Copy result** or click the button to update the clipboard; Esc returns to search, and another Esc closes the palette. Clipboard text is read only when you execute a text action without an inline argument. Opening or searching the palette does not read the clipboard.

| Action | Inline command example |
| --- | --- |
| Generate UUID v4 | `uuid` |
| Timestamp to date in the system timezone | `timestamp 1728000000` or `时间戳 1728000000000` |
| Format / minify JSON | `json {"a":1}` / `json minify {"a":1}` |
| Base64 encode / decode UTF-8 text | `base64 encode hello` / `base64 decode aGVsbG8=` |
| URL encode / decode UTF-8 text | `url encode hello world` / `url decode hello%20world` |
| Deduplicate lines in their original order | `deduplicate` or `按行去重` (uses clipboard text) |

Text actions accept up to 100,000 characters. Invalid JSON, duplicate JSON keys, malformed Base64, invalid UTF-8 Base64 output, and malformed URL percent escapes show errors without modifying the clipboard. The input and output are kept only for the current palette session. The palette runs locally and does not send text to an AI service.

Action metadata and execution live in `src/app/actionRegistry.ts`, separately from page navigation in `toolRegistry.ts`. Implementations load on demand and reuse the existing JSON, time, encoding and Quick Note transformation functions.

### Global shortcut and tray access

Open **Settings → Keyboard Shortcuts → Global command palette** to enable a system-wide shortcut. It is **off by default**, including when upgrading existing installations. The default combination is **Cmd+Shift+Space** on macOS and **Ctrl+Shift+Space** on Windows/Linux. Enter a custom combination such as `Ctrl+Alt+Space`, then save; enabling, disabling and changing the combination take effect immediately. The settings page reports registration status, rejects invalid combinations and conflicts with built-in search/settings/editor shortcuts, and offers retry when the operating system rejects registration.

The shortcut works while MooTool runs in the background. It restores a hidden or minimized main window and opens the same command palette above any docked tool. The tray menu also provides **Command Palette…**. Disabling the shortcut, changing the combination, or quitting releases only the shortcut owned by this feature. It does not alter other apps or register a clipboard watcher.

Linux packages set `desktopName` and `linux.syncDesktopName` to align the installed desktop entry with the Electron app identity used by the Wayland global-shortcut portal. Wayland availability depends on the desktop portal and installation of that desktop entry; some desktops may show their own consent dialog. See the [Electron globalShortcut documentation](https://www.electronjs.org/docs/latest/api/global-shortcut#usage-on-linux). Windows and Linux runtime acceptance remain to be performed; the desktop tests run on macOS.

Implementation: `electron/main/globalCommandShortcut.ts` owns registration and cleanup; `src/shared/contracts/shortcuts.ts` validates combinations consistently in the renderer and main process. `tests/electron/global-shortcut.spec.ts` covers actual registration, simulated callback dispatch to restore hidden/minimized windows, registration failure/retry, persistence after restart and release on disable.

Validation: `npm run check`; `npx playwright test tests/electron/command-palette.spec.ts` covers keyboard navigation, preview/copy, clipboard isolation, invalid input, stale async results, compact layout, and light/dark themes.

## Developer translation

- **Split identifier** turns `getHTTPResponse` / `user_id` into words before translation.
- For a short English translation, **Copy name** offers camelCase, PascalCase, snake_case, UPPER_SNAKE_CASE and kebab-case. The preview shows exactly what will be copied.
- Google and Bing retain automatic translation and mutual fallback.
- **DeepL** uses your own API key: save it in **Settings → Tool Defaults → DeepL API key settings**, select DeepL, then click **Translate**. The key is stored through Electron safeStorage and read only in the main process; it is not included in translation history or ordinary settings. A Free key ending in `:fx` selects the Free endpoint; other keys select Pro. DeepL requests are explicit and never automatically fall back to another engine. Long input is split into bounded requests, so retrying a partially failed translation may consume quota again.

The engine registry and adapters live in `electron/main/translation/`; shared UI capabilities live in `src/shared/contracts/translationEngines.ts`. The implementation follows the [DeepL text API](https://developers.deepl.com/api-reference/translate/request-translation) and [authentication documentation](https://developers.deepl.com/docs/getting-started/auth). Developer naming interactions were inspired by [TranslationPlugin](https://github.com/YiiGuxing/TranslationPlugin).

## AI integration (MCP / Skill)

Available since **1.2.0**, AI integration connects local clients to MooTool's bundled runtime. Tools work with the MooTool window closed and do not require a separate Node.js installation.

| Client | MCP | Standalone Skill |
| --- | --- | --- |
| Codex | Yes | Yes |
| Claude Code | Yes | Yes |
| Cursor | Yes | Not offered by this installer |

Other clients supporting local stdio MCP can use **Copy MCP configuration**. Generated executable paths are for clients running on the same machine.

### Quick start

1. Install MooTool at a permanent location: move the macOS app into Applications, use the Windows installer, or install the Linux deb. AppImage, Windows portable and macOS App Translocation runtimes cannot be registered.
2. Open **Settings → AI integration**, choose a client and **MCP**, **Skill**, or **MCP + Skill**, then review the destinations and generated content.
3. Click **Install in one click**. MooTool tests the server and a sample tool call before writing configuration, and backs up existing files. **Test connection** also verifies the runtime on demand.
4. Restart the AI client or reload MCP / Skills, enabling the tools if prompted. Try “Use MooTool to format this JSON and sort its keys.” To use saved content, first enable the corresponding read-access switch in MooTool.

A standalone Skill includes instructions and the installed runtime command. If MCP is unavailable, it discovers schemas with `--list` and calls tools through `--call`; it supports the same tools and vault access grants.

### Available tools

| Tool | Capability |
| --- | --- |
| `mootool_json_format` | Format/minify JSON, sort keys and detect duplicate keys |
| `mootool_json_query` | JSONPath queries with script evaluation disabled |
| `mootool_encode` | Base64, URL, hexadecimal and Unicode encoding/decoding |
| `mootool_timestamp` | Timestamp/date conversion with timezone and seconds/milliseconds options |
| `mootool_diff` | Text comparison and unified diffs |
| `mootool_hash` | MD5, SHA-1, SHA-256/384/512 text digests |
| `mootool_uuid` | Generate UUID v4 values |
| `mootool_notes_search` / `mootool_notes_read` | Search and read authorized Quick Notes |
| `mootool_json_documents_search` / `mootool_json_documents_read` | Search and read authorized JSON documents |

### Read-only vault access

Document access is **off by default**. Enable **Allow reading Quick Notes** and **Allow reading JSON documents** separately in AI integration settings; the page shows each granted directory. Grants apply to all local clients using this MooTool runtime. Disabling a grant takes effect on the next call, and changing a vault location revokes access.

Search by title, relative path or content, then read a returned path with pagination. Tools exclude hidden files, symbolic links and files ignored by the vault root's `.gitignore`; they do not create or modify documents. Search returns up to 50 entries per page, reads return up to 50,000 characters per page, and files are limited to 2 MB. If search reports `truncated`, narrow the query rather than treating the results as exhaustive.

### Repair, update and uninstall

The page reports whether each integration is installed, missing, needs repair, or conflicts with user edits. After moving/updating MooTool or losing an installed Skill file, use **Repair / update** from the current app location. User-modified entries are preserved; save or adjust conflicting custom content before proceeding.

**Uninstall selected integration** removes only the managed MCP entry and/or Skill files for the chosen client and mode. Other configuration and user files remain available, and removing one client does not revoke vault grants used by other clients. Existing files are backed up before changes; incomplete changes are rolled back where possible.

See the [AI integration guide](doc/mootool-ai-integration.md) for configuration paths, limits, backup behavior, CLI details and the 1.2.0 acceptance record, including [actual Codex tool-call events](doc/verification/ai-codex-1.2.0.json).

## Releases

- Release convention: [`RELEASE_CONVENTIONS.md`](../RELEASE_CONVENTIONS.md)
- Update manifest and asset selection: [`doc/update-products-and-assets.md`](doc/update-products-and-assets.md)
- Write one source file per version under `release-notes/{version}.md` before pushing `next-electron-v{version}`.
- `.github/workflows/next-build-installers.yml` validates the tag, package version, release notes, installers, and updater metadata before publishing.

Tool pages live under `src/features/*`. Filesystem, storage, shell and OS capabilities are exposed through `electron/preload`; the independent MCP entry lives under `electron/mcp`.

### 窗口材质与顶部操作栏

“设置 → 外观 → 窗口材质”提供自动、实色、毛玻璃和 Liquid Glass。
自动模式在 macOS 26+ 尝试可选的 `electron-liquid-glass` 原生扩展，加载或挂载失败回退为 Electron vibrancy；较早 macOS 使用 vibrancy，Windows/Linux 使用实色。
系统减少透明度、高对比度或强制颜色偏好优先使用实色，并在运行期间响应变化。

材质选择在下次启动生效：原生扩展没有公开的移除接口，因此不为切换材质重建窗口，也不影响未保存编辑器和可拆卸工具视图。
侧栏通过浅色 0.4 / 深色 0.5 的背景 alpha 与平滑明度渐变显露材质，正文和按钮不降低 opacity；工具内容区和辅助窗口保持实色。主窗口和独立工具窗口保留各工具的沉浸式顶部，不额外增加标题栏；独立窗口默认显示原生红绿灯，仅在现有工具栏左侧预留安全区，关闭仍收回工具。
设置页显示当前实际材质、降级原因与待重启提示。侧栏状态规则集中在 `src/shared/styles/sidebar.css`，主题通过变量定制，避免重复覆盖。

原生扩展作为 macOS 可选依赖安装，打包时解包其原生二进制。验证：`npm run check`，以及 `npx playwright test tests/electron/window-material.spec.ts tests/electron/tool-windows.spec.ts`。

实现说明与打包验证见 [窗口材质与窗口布局](doc/window-material-and-chrome.md)。
