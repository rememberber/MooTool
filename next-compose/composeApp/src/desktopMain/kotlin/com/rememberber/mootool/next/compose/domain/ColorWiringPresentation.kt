package com.rememberber.mootool.next.compose.domain

/** F22 调色板：工具栏操作启用守卫与引擎路径（可单测）。 */
object ColorWiringPresentation {
    fun canScreenPick(picking: Boolean): Boolean = !picking

    fun canApplyCode(code: String): Boolean = code.trim().isNotEmpty()

    fun canFavorite(favoriteFolderId: String): Boolean = favoriteFolderId.isNotBlank()

    fun runFormatColor(primary: RgbColor, format: ColorFormat): String =
        ColorEngine.formatColor(primary, format)

    fun shouldToastOperationFailure(error: Throwable): Boolean = true

    fun shouldToastErrorMessage(): Boolean = true
}
