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

The first implementation is a visual and architectural scaffold. Tool features should be migrated one by one under `src/features/*`, while filesystem, storage, shell, and OS integrations should be exposed through `electron/preload`.
