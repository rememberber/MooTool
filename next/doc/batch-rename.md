# Batch Rename (Electron)

Open **Batch Rename** from the daily tools group, or use **Cmd/Ctrl+K** and search for `rename` / `重命名`.

1. Choose up to 500 regular files in the native file picker. A new selection replaces the list. Files can come from different directories; each remains in its original directory.
2. Set literal find/replace text, prefix, suffix, and optional sequential numbering. The final extension is preserved by default; `.env` is treated as a filename without an extension. Replacement text is literal, including `$` and regex metacharacters.
3. Select the files to process and click **Preview rename**. Numbering follows the displayed file order, counting selected files only. Review original names, proposed names, unchanged entries, and conflicts.
4. Click **Rename files** to apply exactly that preview. Changing rules or selection invalidates the UI preview; creating another preview invalidates the previous server token. Any conflict blocks the whole batch. Nothing is written during preview.
5. **Undo last batch** restores the latest successful batch, including after an application restart. The next successful batch replaces this undo record. A failed batch retains the preceding undo record when rollback succeeds.

## Boundaries

- Regular files only: folders, recursive traversal, symbolic links, regular-expression matching and arbitrary destination directories are not included.
- The filesystem must support hard links and the directory must be writable. Unsupported filesystems or permissions stop the operation and trigger rollback. The implementation does not fall back to copying or overwriting files.
- Names are checked conservatively for cross-platform use: case-insensitive/NFC collisions, Windows reserved names and forbidden characters, trailing dots/spaces, and the 255-byte UTF-8 filename limit are rejected. Multiple selected hard-link names referring to the same inode are rejected.
- Preview and execution verify device/inode identity. Execution also rechecks destination occupancy. The write step uses exclusive hard-link creation followed by removal of the old name; existing destination files are never deliberately replaced. Content and file identity remain unchanged by the rename operation. These checks are not a filesystem lock against other applications modifying paths concurrently.
- Failed operations attempt to restore original names. If recovery cannot finish, a pending journal remains and further previews are blocked. **Recover unfinished operation** retries restoration; conflicts are reported without replacing their files. The journal records original, destination and temporary paths plus file identities at `<userData>/file-operations/batch-rename-journal.json`. Preserve this record if recovery needs investigation. Files can temporarily remain under `.mootool-rename-<uuid>` names after an interruption.
- A corrupt recovery record stops access to that record; it is not silently discarded. External replacement/movement of a file or occupation of an original name can prevent undo. Only filenames are restored; the feature does not roll back edits to file contents.
- Undo/recovery is global to this Electron installation. Selection grants and preview tokens belong to the renderer that opened the native picker, and remain valid when its live tool view is detached or docked. Renderer requests cannot supply arbitrary rename source/destination paths.

## Implementation and verification

`src/shared/contracts/batchRename.ts` defines bounded literal naming rules. `electron/main/batchRenameService.ts` serializes selection, preview and file operations, owns file grants and tokens, and atomically replaces an application-owned journal before moving files. It stages files under unique names in their original directories to support case-only changes and destinations vacated by the same batch. The journal supports recovery of partial staging or partial completion after restart.

`src/features/batchRename/BatchRenameTool.tsx` provides the preview table and explicit execute/undo controls. The tool participates in navigation, command-palette search, custom groups and live detachable tool windows. Its file-writing operations are not exposed as quick text actions or through MCP.

Validation uses temporary fixtures only:

- `npm run check` — type checking, the full unit suite, and production build.
- `electron/main/batchRenameService.test.ts` — owner/token isolation, name conflicts, replaced sources, case-only renames, selected destination chains, post-preflight target collisions, rollback, restart recovery, unsupported hard links and undo conflicts.
- `tests/electron/batch-rename.spec.ts` — UI preview/invalidation, execution/undo, conflicts, source replacement, restart undo, and native detached view access/layout.

Runtime verification has been performed on macOS. Windows/Linux filesystem and desktop acceptance remain pending.
