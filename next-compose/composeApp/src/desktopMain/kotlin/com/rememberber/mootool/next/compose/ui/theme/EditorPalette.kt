package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class EditorPalette(
    val background: Color,
    val foreground: Color,
    val caret: Color,
    val currentLine: Color,
    val selection: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val property: Color,
    val operator: Color,
    val function: Color,
    val literal: Color
)

fun editorPalette(dark: Boolean, colors: MooColors): EditorPalette {
    val syntax = if (dark) DarkSyntax else LightSyntax
    val line = if (dark) {
        colors.surfaceSubtle.copy(alpha = 0.85f)
    } else {
        colors.surfaceSubtle
    }
    return EditorPalette(
        background = colors.workspace,
        foreground = colors.textPrimary,
        caret = colors.textPrimary,
        currentLine = line,
        selection = colors.selected,
        keyword = syntax.keyword,
        string = syntax.string,
        number = syntax.number,
        comment = syntax.comment,
        property = syntax.property,
        operator = syntax.operator,
        function = syntax.function,
        literal = syntax.literal
    )
}

fun Color.toAwtColor(): java.awt.Color = java.awt.Color(
    red.coerceIn(0f, 1f),
    green.coerceIn(0f, 1f),
    blue.coerceIn(0f, 1f),
    alpha.coerceIn(0f, 1f)
)

private data class SyntaxColors(
    val property: Color,
    val string: Color,
    val number: Color,
    val literal: Color,
    val keyword: Color,
    val function: Color,
    val comment: Color,
    val operator: Color
)

private val LightSyntax = SyntaxColors(
    property = Color(0xFF4E6480),
    string = Color(0xFF3F755D),
    number = Color(0xFFA05F27),
    literal = Color(0xFF8A536B),
    keyword = Color(0xFF7B5792),
    function = Color(0xFF3F6F9C),
    comment = Color(0xFF7E857C),
    operator = Color(0xFF6D6D73)
)

private val DarkSyntax = SyntaxColors(
    property = Color(0xFF91A8C2),
    string = Color(0xFF82B59D),
    number = Color(0xFFD1A267),
    literal = Color(0xFFC48AA2),
    keyword = Color(0xFFC59AD9),
    function = Color(0xFF82AAD4),
    comment = Color(0xFF8C978B),
    operator = Color(0xFFB0B1B7)
)
