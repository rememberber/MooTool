package com.rememberber.mootool.next.compose.domain

/** F22 调色板：工具栏操作启用守卫。 */
object ColorWiringPresentation {
    fun canScreenPick(picking: Boolean): Boolean = !picking

    fun canApplyCode(code: String): Boolean = code.trim().isNotEmpty()

    fun canFavorite(favoriteFolderId: String): Boolean = favoriteFolderId.isNotBlank()
}
