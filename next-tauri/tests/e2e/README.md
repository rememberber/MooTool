# Tauri UI verification

`npm run test:e2e` runs deterministic Chromium interaction and screenshot checks against the Vite surface. Use `npm run test:e2e:update` only after intentionally reviewing layout changes.

`npm run test:native-smoke` builds the real Tauri binary and verifies that it remains alive through startup. Use `npm run test:native-smoke -- --skip-build` to reuse an existing release binary.

`npm run test:native-acceptance` builds and launches the unsigned native application in an explicit acceptance mode. On macOS this exercises real WKWebViews for all 25 product tools: load and session reporting, bounds, hide/show, detach/dock without reload, close, cross-tool session isolation, and Calculator reparent stress. Use `npm run test:native-acceptance -- --skip-build --cycles=100` to reuse a release binary and increase the stress cycle count. The runner isolates configuration, window state, database, and logs under a temporary directory, skips launch-at-login synchronization, and removes the directory on completion, so normal application data and system startup state are not read or changed.

Release candidates should run the web suite and native startup smoke on macOS, Windows, and Linux. The macOS CI gate additionally runs native acceptance to cover WKWebView lifecycle behavior that Chromium preview cannot prove. Windows WebView2 and Linux WebKitGTK acceptance remain part of the platform release matrix.
