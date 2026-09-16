package com.rememberber.mootool.next.compose.features.json

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPathListLimitTest {
    @Test
    fun inspector_inline_path_limit_is_stable() {
        assertEquals(80, JSON_INSPECTOR_INLINE_PATH_LIMIT)
    }
}
