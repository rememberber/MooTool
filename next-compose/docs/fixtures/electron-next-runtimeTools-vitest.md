# Electron `runtimeTools.test.ts` 对照登记

| caseId | sourceFile | Compose 验证 |
| --- | --- | --- |
| format-node-python | `next/src/features/runtime/runtimeTools.test.ts` | `CodeRunEngineTest.parsesQuotedArgumentsWithoutAShell` + `CodeRunWiringPresentationTest.formatSourceDelegatesToEngineForNodeSample`（[DIFF-627](../diff/627-f05-coderun-format-wiring-slice.md)） |
| runtime-display-name | 同上 | `CodeRunEngineTest.displayNameMatchesElectronRuntimeTools` + `CodeRunWiringPresentationTest.displayNameMatchesElectronRuntimeTools`（[DIFF-626](../diff/626-f05-runtime-tools-vitest-parity-slice.md)、[DIFF-629](../diff/629-f05-coderun-display-cancel-wiring-slice.md)） |
| parse-quoted-args | 同上 | `CodeRunEngineTest` / `CodeRunWiringPresentationTest.parseRunArgumentsUsesEngine` |
