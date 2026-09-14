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
