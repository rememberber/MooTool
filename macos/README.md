# MooTool Native for macOS

This directory contains the preview skeleton for a macOS-native MooTool app built with SwiftUI. Open `Package.swift` in Xcode to work on the app as a native macOS project.

The current Java/Swing application remains the stable cross-platform implementation. This native app is intentionally small: it provides the project structure, a minimal SwiftUI shell, settings, and tests for the top-level module catalog. It does not replace the existing macOS Java DMG.

## Requirements

- macOS 14 or later
- Xcode 15 or later for tests and app development
- Swift 5.9 compatible command line tools for basic package builds

## Command Line

```bash
cd macos
swift build
```

When full Xcode is available and its license has been accepted, prefer the Xcode developer directory for tests and local launches:

```bash
cd macos
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swift test
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer swift run MooToolNative
```

If `swift test` fails because the active developer directory points to Command Line Tools, either prefix the command with `DEVELOPER_DIR` as shown above or switch the active Xcode installation with `xcode-select`.

## Xcode

1. Open `macos/Package.swift` in Xcode.
2. Select the `MooToolNative` scheme.
3. Choose My Mac as the run destination.
4. Run with Product > Run.

The preview currently launches as a Swift Package executable. It is not a signed `.app` bundle and is not part of the release DMG pipeline.

## Migration Direction

- Keep Windows and Linux on the existing Java build.
- Keep the Java macOS package available while this native app is incomplete.
- Add native modules in small PRs after the skeleton is accepted.
- Prefer importing legacy data from `~/.MooTool/MooTool.db` instead of long-term shared writes to the old database.
