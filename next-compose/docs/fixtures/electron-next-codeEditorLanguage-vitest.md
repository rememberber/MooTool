# Electron `codeEditorLanguage.test.ts` → Compose 对照

| 场景 | Electron | Compose |
| --- | --- | --- |
| mime-json-markdown | `resolveTextCodeEditorLanguage` MIME（`application/json`、`text/markdown` 等） | `TextCodeEditorLanguageTest.mapsMimeTypesUsedByQuickNoteAndHttpBodies`（[DIFF-541](../diff/541-git-diff-update-tray-http-slice.md)） |
| short-names-fallback | `node`/`py`/`yml`/未知 → 语言或 `text` | `TextCodeEditorLanguageTest.acceptsShortRuntimeNamesAndFallsBackToText` |
| git-diff-path | `VaultGitDiffView` 按路径扩展名选 CodeMirror 语言 | `TextCodeEditorLanguages.resolveFromPath` + `GitDiffPresentation.languageForFile` / `rstaSyntaxForFile`（[DIFF-542](../diff/542-vault-git-diff-editorformat-conflict-slice.md) EditorHost） |
| format-js-python | `formatCodeEditorContent` JS 单线样本 / Python tab | `CodeEditorSurfaceFormatEngineTest` + `DocumentFormatEngine`（[DIFF-542](../diff/542-vault-git-diff-editorformat-conflict-slice.md)） |
| format-ts-surface | TS 仍走 Prettier `typescript` parser（Electron）；Compose 无 Node | `CodeEditorSurfaceFormatEngineTest.formatsTypescriptLikeJavascriptSurface`（[DIFF-543](../diff/543-git-diff-decoration-f-tools-p7-slice.md)） |
| format-markdown-incremental | `formatCodeEditorContent` → Prettier `plugins/markdown`（列表/标题/引用/围栏样本） | `CodeEditorSurfaceFormatEngineTest.formatsMarkdownLikePrettierSamples` 等 + `DocumentFormatEngine.format(..., "text/markdown", ...)`（[DIFF-544](../diff/544-document-format-markdown-slice.md)；非 AST/reflow） |
| git-diff-decoration | Vault Git diff 高亮 | `GitDiffDecorationTest` + `GitDiffPresentation.diffHighlightMode`（[DIFF-543](../diff/543-git-diff-decoration-f-tools-p7-slice.md)） |
