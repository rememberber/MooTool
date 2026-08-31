# Tauri UI verification

`npm run test:e2e` runs deterministic Chromium interaction and screenshot checks against the Vite surface. Use `npm run test:e2e:update` only after intentionally reviewing layout changes.

`npm run test:native-smoke` builds the real Tauri binary and verifies that it remains alive through startup. Use `npm run test:native-smoke -- --skip-build` to reuse an existing release binary.

Release candidates should run both commands on macOS, Windows, and Linux. The web suite catches shared React/CSS regressions; the native smoke catches platform bootstrap, plugin, menu, tray, capability, and WebView initialization failures.
