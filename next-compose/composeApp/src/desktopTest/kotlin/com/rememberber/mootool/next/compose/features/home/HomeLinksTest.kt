package com.rememberber.mootool.next.compose.features.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeLinksTest {
    @Test
    fun contributorsMatchElectronExternalPages() {
        assertEquals("https://gitee.com/felixnan168", HomeContributors.first { it.name == "felixnan168" }.url)
        assertEquals("https://gitee.com/L1yp", HomeContributors.first { it.name == "Lyp" }.url)
        assertEquals(6, HomeContributors.size)
        assertTrue(HomeContributors.all { it.url.startsWith("https://") })
    }

    @Test
    fun worksMatchElectronExternalPages() {
        assertEquals("https://github.com/rememberber/WePush", HomeWorks.first { it.title == "WePush" }.url)
        assertEquals("https://github.com/rememberber/MooInfo", HomeWorks.first { it.title == "MooInfo" }.url)
        assertEquals("home.wepush.desc", HomeWorks.first { it.title == "WePush" }.descKey)
    }
}
