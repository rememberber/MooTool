# Vendored Rust patches

## `tauri-runtime-wry` 2.11.4

This is the crates.io source for `tauri-runtime-wry` 2.11.4 with one focused
change in `WebviewDispatcher::reparent`: copy the current window ID and release
its mutex before waiting for the event-loop reply.

Without the patch, a concurrent IPC reply can deadlock with WebView reparenting
on Windows and macOS. The native acceptance suite reproduced it while detaching
the Config tool on Windows.

Upstream tracking issue: <https://github.com/tauri-apps/tauri/issues/15489>

Remove the `[patch.crates-io]` override and this directory once a released
`tauri-runtime-wry` contains the upstream fix.
