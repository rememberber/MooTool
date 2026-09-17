# Electron `codeEditorLanguage.test.ts` → Compose 对照

| 场景 | Electron | Compose |
| --- | --- | --- |
| mime-json-markdown | `resolveTextCodeEditorLanguage` MIME（`application/json`、`text/markdown` 等） | `TextCodeEditorLanguageTest.mapsMimeTypesUsedByQuickNoteAndHttpBodies`（[DIFF-541](../diff/541-git-diff-update-tray-http-slice.md)） |
| short-names-fallback | `node`/`py`/`yml`/未知 → 语言或 `text` | `TextCodeEditorLanguageTest.acceptsShortRuntimeNamesAndFallsBackToText` |
| git-diff-path | `VaultGitDiffView` 按路径扩展名选 CodeMirror 语言 | `TextCodeEditorLanguages.resolveFromPath` + `GitDiffPresentation.languageForFile` / `rstaSyntaxForFile`（[DIFF-542](../diff/542-vault-git-diff-editorformat-conflict-slice.md) EditorHost） |
| format-js-python | `formatCodeEditorContent` JS 单线样本 / Python tab | `CodeEditorSurfaceFormatEngineTest` + `DocumentFormatEngine`（[DIFF-542](../diff/542-vault-git-diff-editorformat-conflict-slice.md)） |
