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
| public-get | （可选 httpbin，`MOOTOOL_HTTP_PUBLIC_SMOKE=1`） | `HttpEngineTest.optionalHttpBinPublicGetSmoke`（[DIFF-526](../diff/526-http-pdf-merge-smoke-p7-slice.md)，默认 CI 跳过；本机通过见 [DIFF-527](../diff/527-tray-permission-pdf-outline-http-slice.md)） |
| multipart-prepare | （Compose 手工 Body；无 Electron multipart 文件 Tab） | `HttpEngineTest.preparePostsMultipartBodyUnmodifiedWhenContentTypeHeaderPresent`（[DIFF-527](../diff/527-tray-permission-pdf-outline-http-slice.md)） |
| multipart-build | （Electron 无文件 Tab；程序化字段） | `HttpEngine.buildMultipartFormData` + `HttpEngineTest.buildMultipartFormDataIncludesTextAndFileParts`（[DIFF-528](../diff/528-http-multipart-editor-tray-git-slice.md)） |
| public-multipart-post | （可选 httpbin，`MOOTOOL_HTTP_MULTIPART_SMOKE=1`） | `HttpEngineTest.optionalHttpBinMultipartPostSmoke`（[DIFF-528](../diff/528-http-multipart-editor-tray-git-slice.md)，默认 CI 跳过） |
| format-body | `formatCodeEditorContent`（Quick Note/HTTP Body，`codeEditorFormatting.test.ts`） | `HttpEngine.formatBody` → `DocumentFormatEngine.format`（[DIFF-541](../diff/541-git-diff-update-tray-http-slice.md)）；`HttpEngineTest.formatBodyUsesDocumentFormatEngineForXmlAndJson` |
| format-body-js | `text/javascript` 单线 `const value={ready:true}` | `HttpEngineTest.formatBodyFormatsJavascriptLikeElectron`（[DIFF-542](../diff/542-vault-git-diff-editorformat-conflict-slice.md)） |
