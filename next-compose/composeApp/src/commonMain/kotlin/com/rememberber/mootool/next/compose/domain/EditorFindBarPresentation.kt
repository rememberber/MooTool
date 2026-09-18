package com.rememberber.mootool.next.compose.domain

/** 编辑器/正文查找条「查找/上一处/下一处」守卫（F01/F04/F10/F09 响应对齐 Electron 空查询禁用步进）。 */
object EditorFindBarPresentation {
    fun findQueryActionEnabled(query: String): Boolean = query.isNotBlank()

    fun findStepActionEnabled(query: String): Boolean = findQueryActionEnabled(query)
}
