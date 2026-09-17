# Electron `httpTools.test.ts` → Compose 对照

| 场景 | Electron `httpTools.test.ts` | Compose |
| --- | --- | --- |
| curl-parse-json | `parses method, headers, cookies and JSON body` | `HttpEngineTest.parsesCurlAndRoundTripsImportantFields` |
| curl-round-trip | `round trips the important request fields` | `HttpEngineTest.curlRoundTripMatchesElectronHttpToolsAcceptAndBody`（[DIFF-520](../diff/520-vault-numeric-git-http-curl.md)） |
