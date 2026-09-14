# MooTool Next

Electron + Vite + React + TypeScript rewrite prototype for MooTool.

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

Open **Settings → AI integration** to install MooTool tools for **Codex**, **Claude Code** or **Cursor** in one click. Codex and Claude Code support MCP, a standalone Skill, or both; Cursor supports MCP. The page previews the destinations and generated content, tests the bundled server before installing, and backs up existing configuration. A copy button provides MCP configuration for other compatible local clients.

The page also reports installation status and provides repair, update and uninstall actions. Seven tools process supplied JSON, text, timestamps, hashes and UUID arguments. Four additional tools search and read Quick Notes and JSON documents after separate opt-in grants. All eleven run locally using the application's bundled runtime, including when its window is closed. See [setup, supported packages and development verification](doc/mootool-ai-integration.md).

## Releases

- Release convention: [`RELEASE_CONVENTIONS.md`](../RELEASE_CONVENTIONS.md)
- Update manifest and asset selection: [`doc/update-products-and-assets.md`](doc/update-products-and-assets.md)
- Write one source file per version under `release-notes/{version}.md` before pushing `next-electron-v{version}`.
- `.github/workflows/next-build-installers.yml` validates the tag, package version, release notes, installers, and updater metadata before publishing.

The first implementation is a visual and architectural scaffold. Tool features should be migrated one by one under `src/features/*`, while filesystem, storage, shell, and OS integrations should be exposed through `electron/preload`.
