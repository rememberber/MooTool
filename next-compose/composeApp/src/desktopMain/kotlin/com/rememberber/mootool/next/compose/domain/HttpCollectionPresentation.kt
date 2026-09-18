package com.rememberber.mootool.next.compose.domain

/** F09 HTTP 集合侧栏与保存/cURL 对话框按钮守卫（对齐 Electron `HttpTool`）。 */
object HttpCollectionPresentation {
    fun deleteSavedActionEnabled(selectedId: String): Boolean = selectedId.isNotBlank()

    fun saveCollectionActionEnabled(saveName: String): Boolean = saveName.trim().isNotEmpty()

    fun curlImportActionEnabled(curlValue: String): Boolean = curlValue.trim().isNotEmpty()
}
