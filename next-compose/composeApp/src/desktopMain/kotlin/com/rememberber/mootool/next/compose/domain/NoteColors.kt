package com.rememberber.mootool.next.compose.domain

object NoteColors {
    val swatches: List<Pair<String, Int>> = listOf(
        "default" to 0xFF8A8A8A.toInt(),
        "coral" to 0xFFD97868.toInt(),
        "yellow" to 0xFFC99535.toInt(),
        "green" to 0xFF4E9275.toInt(),
        "blue" to 0xFF4F83CC.toInt(),
        "purple" to 0xFF8A72B5.toInt(),
        "red" to 0xFFC96761.toInt()
    )

    fun normalize(id: String): String {
        val key = id.trim().lowercase()
        return swatches.firstOrNull { it.first == key }?.first ?: "default"
    }

    fun argb(id: String): Int = swatches.firstOrNull { it.first == normalize(id) }?.second ?: swatches.first().second

    fun tintsTree(id: String): Boolean = normalize(id) != "default"
}
