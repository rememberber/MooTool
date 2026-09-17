package com.rememberber.mootool.next.compose.domain

/** F08 环境变量工具栏/对话框启用守卫（可单测）。 */
object EnvWiringPresentation {
    fun refreshEnabled(loading: Boolean, saving: Boolean): Boolean = !loading && !saving

    fun exportEnabled(hasSnapshot: Boolean): Boolean = hasSnapshot

    fun addVariableEnabled(canEdit: Boolean, saving: Boolean): Boolean = canEdit && !saving

    fun saveEditorEnabled(trimmedKey: String, saving: Boolean): Boolean =
        trimmedKey.isNotEmpty() && !saving
}
