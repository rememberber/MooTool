package com.rememberber.mootool.next.compose.domain

enum class ColorThemeId { Default, Theme1, Theme2, Theme3, Theme4, Theme5, China }

enum class ColorFormat { HEX_UPPER, HEX_LOWER, RGB }

enum class ColorOperation { Invert, Intersect, Add, Difference, Average }

data class RgbColor(val r: Int, val g: Int, val b: Int)

data class ColorTheme(
    val id: ColorThemeId,
    val main: List<String>,
    val shades: List<List<String>>
)

class ColorException(val code: String, message: String) : RuntimeException(message)

object ColorEngine {
    val DEFAULT_PRIMARY: RgbColor = RgbColor(222, 143, 125)
    val DEFAULT_SECONDARY: RgbColor = RgbColor(79, 131, 204)

    val STANDARD_COLORS: List<String> = listOf(
        "#C00000",
        "#FF0000",
        "#FFC000",
        "#FFFF00",
        "#92D050",
        "#00B050",
        "#00B0F0",
        "#0070C0",
        "#002060",
        "#7030A0"
    )

    val THEMES: List<ColorTheme> = listOf(
        ColorTheme(
            id = ColorThemeId.Default,
            main = listOf(
                "#000000",
                "#FFFFFF",
                "#880015",
                "#ED1C24",
                "#FF7F27",
                "#FFF200",
                "#22B14C",
                "#00A2E8",
                "#3F48CC",
                "#A349A4"
            ),
            shades = listOf(
                listOf("#808080", "#595959", "#404040", "#262626", "#0D0D0D"),
                listOf("#F2F2F2", "#D9D9D9", "#BFBFBF", "#A6A6A6", "#808080"),
                listOf("#E7B9C0", "#CF7C89", "#B84A5B", "#660010", "#44000A"),
                listOf("#FBCFD0", "#F8A1A4", "#F47378", "#B21016", "#77070B"),
                listOf("#FFE5D4", "#FFCCA9", "#FFB27D", "#BF5B16", "#803A0A"),
                listOf("#FFFCCC", "#FFFA99", "#FFF766", "#BFB500", "#807900"),
                listOf("#C8EFD4", "#98E0AD", "#6BD089", "#138535", "#085820"),
                listOf("#C8EBFA", "#94D8F6", "#60C5F1", "#007AAE", "#005174"),
                listOf("#D3D5F5", "#AAAEEB", "#8389E0", "#232B99", "#101566"),
                listOf("#EDD3ED", "#DAAADB", "#C785C8", "#7A297B", "#511252")
            )
        ),
        ColorTheme(
            id = ColorThemeId.Theme1,
            main = listOf(
                "#FFFFFF",
                "#000000",
                "#1F497D",
                "#EEECE1",
                "#4F81BD",
                "#C0504D",
                "#9BBB59",
                "#8064A2",
                "#4BACC6",
                "#F79646"
            ),
            shades = listOf(
                listOf("#F2F2F2", "#D9D9D9", "#BFBFBF", "#A6A6A6", "#808080"),
                listOf("#808080", "#595959", "#404040", "#262626", "#0D0D0D"),
                listOf("#C3D2E5", "#8EA9CB", "#6185B1", "#11345E", "#08203E"),
                listOf("#D6D1B6", "#B2AA7E", "#776D38", "#3C350E", "#181502"),
                listOf("#D6E3F2", "#B0C8E5", "#8CAED7", "#2D598E", "#14365F"),
                listOf("#F2D6D5", "#E6B0AF", "#D98D8B", "#902E2B", "#601513"),
                listOf("#E9F1D8", "#D4E4B4", "#C0D693", "#6F8C32", "#465D16"),
                listOf("#E2DAEC", "#C8B9DA", "#AE99C7", "#553879", "#321951"),
                listOf("#D6EEF4", "#AEDCE8", "#8BCCDD", "#2A7E95", "#135263"),
                listOf("#FDE9D9", "#FCD4B4", "#FABF8F", "#B96927", "#7C4212")
            )
        ),
        ColorTheme(
            id = ColorThemeId.Theme2,
            main = listOf(
                "#FFFFFF",
                "#000000",
                "#69676D",
                "#C9C2D1",
                "#CEB966",
                "#9CB084",
                "#6BB1C9",
                "#6585CF",
                "#7E6BC9",
                "#A379BB"
            ),
            shades = listOf(
                listOf("#F2F2F2", "#D9D9D9", "#BFBFBF", "#A6A6A6", "#808080"),
                listOf("#808080", "#595959", "#404040", "#262626", "#0D0D0D"),
                listOf("#E0E0E2", "#C2C1C5", "#A3A1A7", "#423A52", "#231A36"),
                listOf("#AB9DBC", "#846D9D", "#4A3068", "#1F0C34", "#0B0215"),
                listOf("#F5F0DC", "#EBE1BC", "#E2D49E", "#9B873A", "#67571A"),
                listOf("#EAEFE3", "#D5DFC9", "#C2D0B1", "#6A844A", "#3F5821"),
                listOf("#DDEEF4", "#BDDEE9", "#A0CFDF", "#3C8097", "#1B5164"),
                listOf("#DCE3F5", "#BCCAEC", "#9DB2E2", "#39569B", "#193168"),
                listOf("#E2DDF4", "#C6BDE9", "#ADA0DF", "#4F3C97", "#291B64"),
                listOf("#EBE0F1", "#D8C4E4", "#C6A9D6", "#72448C", "#461E5D")
            )
        ),
        ColorTheme(
            id = ColorThemeId.Theme3,
            main = listOf(
                "#FFFFFF",
                "#000000",
                "#323232",
                "#E3DED1",
                "#F07F09",
                "#9F2936",
                "#1B587C",
                "#4E8542",
                "#604878",
                "#C19859"
            ),
            shades = listOf(
                listOf("#F2F2F2", "#D9D9D9", "#BFBFBF", "#A6A6A6", "#808080"),
                listOf("#808080", "#595959", "#404040", "#262626", "#0D0D0D"),
                listOf("#D6D6D6", "#ADADAD", "#848484", "#262626", "#191919"),
                listOf("#CCC2A9", "#AA9B75", "#716034", "#392D0D", "#171102"),
                listOf("#FCE4CB", "#F9CA99", "#F6B168", "#B45E05", "#783E02"),
                listOf("#ECC9CD", "#D999A0", "#C56D77", "#771722", "#500A12"),
                listOf("#C1D8E5", "#8BB3CB", "#5D91B0", "#0F405D", "#07293E"),
                listOf("#D4E7D0", "#ACCEA4", "#89B67F", "#306425", "#1A4311"),
                listOf("#DBD2E4", "#B9A9C9", "#9984AE", "#41285A", "#27123C"),
                listOf("#F3E9D9", "#E6D2B4", "#DABE94", "#916C32", "#604316")
            )
        ),
        ColorTheme(
            id = ColorThemeId.Theme4,
            main = listOf(
                "#FFFFFF",
                "#000000",
                "#646B86",
                "#C5D1D7",
                "#D16349",
                "#CCB400",
                "#8CADAE",
                "#8C7B70",
                "#8FB08C",
                "#D19049"
            ),
            shades = listOf(
                listOf("#F2F2F2", "#D9D9D9", "#BFBFBF", "#A6A6A6", "#808080"),
                listOf("#808080", "#595959", "#404040", "#262626", "#0D0D0D"),
                listOf("#DBDEE7", "#BABECF", "#9AA0B6", "#394265", "#192243"),
                listOf("#9FB6C1", "#6F90A1", "#31586C", "#0C2836", "#020F16"),
                listOf("#F6DCD6", "#EDBBAF", "#E39B8A", "#9D3F29", "#682312"),
                listOf("#F5EFC4", "#EBE08D", "#E0D05A", "#998700", "#665A00"),
                listOf("#E6EFEF", "#CEDEDF", "#B6CDCE", "#4E8082", "#235557"),
                listOf("#E8E2DF", "#D1C7C0", "#BAACA4", "#69503F", "#462D1C"),
                listOf("#E6EFE5", "#CEDFCD", "#B9D0B6", "#53844F", "#275823"),
                listOf("#F6E7D6", "#EDD0AF", "#E3B98A", "#9D6629", "#683F12")
            )
        ),
        ColorTheme(
            id = ColorThemeId.Theme5,
            main = listOf(
                "#FFFFFF",
                "#000000",
                "#464646",
                "#DEF5FA",
                "#2DA2BF",
                "#DA1F28",
                "#EB641B",
                "#39639D",
                "#474B78",
                "#7D3C4A"
            ),
            shades = listOf(
                listOf("#F2F2F2", "#D9D9D9", "#BFBFBF", "#A6A6A6", "#808080"),
                listOf("#808080", "#595959", "#404040", "#262626", "#0D0D0D"),
                listOf("#DADADA", "#B5B5B5", "#909090", "#353535", "#232323"),
                listOf("#B4D9E1", "#7DB0BB", "#37717D", "#0E353E", "#021519"),
                listOf("#CDEBF2", "#9FD7E5", "#75C5D9", "#19788F", "#0B4F60"),
                listOf("#F8CDD0", "#F09EA2", "#E97177", "#A31118", "#6D080D"),
                listOf("#FBDECF", "#F7BEA0", "#F39F72", "#B0480F", "#752D07"),
                listOf("#CDDAEB", "#A1B8D8", "#7999C4", "#204476", "#0E294F"),
                listOf("#D1D3E4", "#A8ABC9", "#8387AE", "#282C5A", "#12153C"),
                listOf("#E5CDD2", "#CBA1AA", "#B17A86", "#5E222F", "#3E0F19")
            )
        ),
        ColorTheme(
            id = ColorThemeId.China,
            main = listOf(
                "#FFFEF9",
                "#3D3B4F",
                "#9D2933",
                "#FF461F",
                "#C91F37",
                "#CA6924",
                "#F0C239",
                "#789262",
                "#177CB0",
                "#815463"
            ),
            shades = listOf(
                listOf("#FFFEFD", "#FFFEF9", "#FFF8EF", "#FFF2E5", "#FFE8D5"),
                listOf("#D5D4DC", "#ACABB9", "#828190", "#59576C", "#3D3B4F"),
                listOf("#EBD4D6", "#D7A9AD", "#C37E84", "#AF535C", "#7A1E26"),
                listOf("#FFE2D9", "#FFBFAD", "#FF9C81", "#FF7955", "#CC2E08"),
                listOf("#F2D0D4", "#E5A1A9", "#D8727E", "#B83D4D", "#8F1525"),
                listOf("#F5E6D9", "#EBCCB3", "#E1B28D", "#C48542", "#8F5118"),
                listOf("#FCF3D4", "#F8E7A9", "#F4DB7E", "#D9AE2A", "#A67E15"),
                listOf("#E5EDE0", "#CBDBC1", "#B1C9A2", "#93A97E", "#5A7348"),
                listOf("#D5EBF4", "#ABDAEA", "#81C9E0", "#45A8CC", "#0E5F85"),
                listOf("#E5D8DD", "#CBACB8", "#B18099", "#946B7B", "#5C3445")
            )
        )
    )

    fun theme(id: ColorThemeId): ColorTheme = THEMES.first { it.id == id }

    fun themeId(value: String): ColorThemeId = when (value.trim().lowercase()) {
        "default" -> ColorThemeId.Default
        "theme1" -> ColorThemeId.Theme1
        "theme2" -> ColorThemeId.Theme2
        "theme3" -> ColorThemeId.Theme3
        "theme4" -> ColorThemeId.Theme4
        "theme5" -> ColorThemeId.Theme5
        "china" -> ColorThemeId.China
        else -> ColorThemeId.entries.find { it.name.equals(value, ignoreCase = true) } ?: ColorThemeId.Default
    }

    fun extractHex(text: String): String? =
        Regex("#[0-9a-fA-F]{6}").find(text)?.value?.uppercase()

    fun parseColor(input: String): RgbColor {
        val value = input.trim().replace('，', ',')
        if (value.contains(',')) {
            val stripped = value.replace(Regex("^rgba?\\(|\\)$", RegexOption.IGNORE_CASE), "")
            val parts = stripped.split(',').map { it.trim() }.mapNotNull { it.toIntOrNull() }
            if (parts.size < 3 || parts.take(3).any { it !in 0..255 }) {
                throw ColorException("invalid-rgb", "RGB values must be integers from 0 to 255")
            }
            return RgbColor(parts[0], parts[1], parts[2])
        }
        val hex = value.removePrefix("#")
        if (!Regex("^(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$").matches(hex)) {
            throw ColorException("invalid-hex", "Color must be #RGB, #RRGGBB or R,G,B")
        }
        val expanded = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        return RgbColor(
            r = expanded.substring(0, 2).toInt(16),
            g = expanded.substring(2, 4).toInt(16),
            b = expanded.substring(4, 6).toInt(16)
        )
    }

    fun formatColor(color: RgbColor, format: ColorFormat): String {
        if (format == ColorFormat.RGB) return "${color.r}, ${color.g}, ${color.b}"
        val hex = "#" + listOf(color.r, color.g, color.b).joinToString("") { clamp(it).toString(16).padStart(2, '0') }
        return if (format == ColorFormat.HEX_UPPER) hex.uppercase() else hex.lowercase()
    }

    fun apply(operation: ColorOperation, primary: RgbColor, secondary: RgbColor): RgbColor = when (operation) {
        ColorOperation.Invert -> RgbColor(255 - primary.r, 255 - primary.g, 255 - primary.b)
        ColorOperation.Intersect -> RgbColor(
            primary.r * secondary.r / 255,
            primary.g * secondary.g / 255,
            primary.b * secondary.b / 255
        )
        ColorOperation.Add -> RgbColor(clamp(primary.r + secondary.r), clamp(primary.g + secondary.g), clamp(primary.b + secondary.b))
        ColorOperation.Difference -> RgbColor(
            kotlin.math.abs(primary.r - secondary.r),
            kotlin.math.abs(primary.g - secondary.g),
            kotlin.math.abs(primary.b - secondary.b)
        )
        ColorOperation.Average -> RgbColor(
            (primary.r + secondary.r) / 2,
            (primary.g + secondary.g) / 2,
            (primary.b + secondary.b) / 2
        )
    }

    fun bestTextColor(color: RgbColor): String {
        val luminance = (0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b) / 255.0
        return if (luminance > 0.55) "#000000" else "#FFFFFF"
    }

    fun rgbToHex(r: Int, g: Int, b: Int): String =
        "#" + listOf(r, g, b).joinToString("") { clamp(it).toString(16).padStart(2, '0') }.uppercase()

    fun sampleRgba(pixels: ByteArray, width: Int, height: Int, x: Double, y: Double): RgbColor? {
        if (width <= 0 || height <= 0 || pixels.size < width * height * 4) return null
        val pixelX = kotlin.math.floor(x).toInt().coerceIn(0, width - 1)
        val pixelY = kotlin.math.floor(y).toInt().coerceIn(0, height - 1)
        val offset = (pixelY * width + pixelX) * 4
        return RgbColor(pixels[offset].toInt() and 0xff, pixels[offset + 1].toInt() and 0xff, pixels[offset + 2].toInt() and 0xff)
    }

    fun canonicalThemesJson(): String = buildString {
        append('[')
        THEMES.forEachIndexed { index, theme ->
            if (index > 0) append(',')
            append("{\"id\":\"")
            append(idJson(theme.id))
            append("\",\"main\":")
            append(theme.main.joinToString(",", "[", "]") { "\"$it\"" })
            append(",\"shades\":")
            append(
                theme.shades.joinToString(",", "[", "]") { column ->
                    column.joinToString(",", "[", "]") { "\"$it\"" }
                }
            )
            append('}')
        }
        append(']')
    }

    fun canonicalStandardJson(): String = STANDARD_COLORS.joinToString(",", "[", "]") { "\"$it\"" }

    fun canonicalThemeId(id: ColorThemeId): String = idJson(id)

    private fun idJson(id: ColorThemeId): String = when (id) {
        ColorThemeId.Default -> "default"
        ColorThemeId.Theme1 -> "theme1"
        ColorThemeId.Theme2 -> "theme2"
        ColorThemeId.Theme3 -> "theme3"
        ColorThemeId.Theme4 -> "theme4"
        ColorThemeId.Theme5 -> "theme5"
        ColorThemeId.China -> "china"
    }

    private fun clamp(value: Int): Int = value.coerceIn(0, 255)
}
