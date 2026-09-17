# Electron `httpTools.test.ts` → Compose 对照

| 场景 | Electron `httpTools.test.ts` | Compose |
| --- | --- | --- |
| curl-parse-json | `parses method, headers, cookies and JSON body` | `HttpEngineTest.parsesCurlAndRoundTripsImportantFields` |
| curl-round-trip | `round trips the important request fields` | `HttpEngineTest.curlRoundTripMatchesElectronHttpToolsAcceptAndBody`（[DIFF-520](../diff/520-vault-numeric-git-http-curl.md)） |
| binary-post | （Compose 本机 echo，Electron 无等价 vitest） | `HttpEngineTest.postBodyWithEmbeddedNullBytesRoundTripsOnLocalServer`（[DIFF-523](../diff/523-http-pdf-crypto-tray-update-slice.md)） |
| multipart-preview | （Compose 扩展；Electron 响应仍 UTF-8 解码） | `HttpEngineTest.decodeBodyTreatsMultipartResponsesAsText` |
| curl-data-binary | （与 `-d` 同等 token 分支） | `HttpEngineTest.parseCurlDataBinaryPreservesPayloadAndDefaultsToPost` |
| curl-data-urlencode | （Electron 解析不 decode；Compose 按 URL 解码正文，见 [DIFF-524](../diff/524-http-crypto-pdf-tray-slice.md)） | `HttpEngineTest.parseCurlDataUrlencodeDecodesBodyAndSetsFormContentType` |
| cookie-echo | （Compose 本机 echo） | `HttpEngineTest.localServerEchoesRequestCookiesAndResponseSetCookie`（[DIFF-525](../diff/525-crypto-pdf-http-network-slice.md)） |
