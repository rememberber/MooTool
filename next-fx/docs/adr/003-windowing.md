# ADR 003 — Windowing

Status: Accepted  
Date: 2026-09-09

## Context

Electron keeps one WebContentsView per tool and moves it between the main window and a tool window. JavaFX Nodes cannot have two parents. Home must not detach. Closing a tool window should dock, not dispose, the session.

## Decision

- Main Stage uses `StageStyle.DECORATED` (JavaFX 26 `EXTENDED`/`HeaderBar` remains preview).
- Default size 1440×920, minimum 1080×720. Tool windows default 1100×760, minimum 760×560.
- `ToolWindowCoordinator` transfers the existing tool Node: remove from dock `StackPane`, place as center of a new `Scene`/`Stage`, reverse on close.
- Home (`mootool`) is not detachable. Re-clicking a detached tool focuses that Stage.
- `Platform.setImplicitExit(true)` for P0 because tray restore is not implemented; quit closes SQLite and executors in `Application.stop()`.
- Window bounds and active tool id persist in this product's SQLite `window_state` table.

## Consequences

Custom chrome, snap layouts, and tray-hidden background mode are not claimed. Multi-screen clamp currently uses the primary visual bounds only. Closing the main window while a tool Stage is open still exits the process in P0.
