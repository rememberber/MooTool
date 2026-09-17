package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** 对照 Electron `next/src/features/regex/regexTools.ts` `commonRegexes` 顺序与 id。 */
class RegexElectronCatalogTest {
    @Test
    fun commonPatternIdsMatchElectronCatalog() {
        val expected = listOf(
            "phone", "email", "domain", "ipv4", "account", "htmlId", "color", "jpg", "magnet",
            "chinese", "alnum", "len3to20", "letters26", "wordUnderscore", "cnEnNum", "noSpecial",
            "integer", "positiveInt", "negativeInt", "nonNegativeInt", "float",
        )
        assertEquals(expected, RegexEngine.commonRegexes.map { it.id })
        assertEquals(21, RegexEngine.commonRegexes.size)
    }
}
