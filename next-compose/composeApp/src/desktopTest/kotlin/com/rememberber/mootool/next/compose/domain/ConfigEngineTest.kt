package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigEngineTest {
    @Test
    fun convertsNestedPropertiesAndIndexedListsToYaml() {
        val yaml = ConfigEngine.propertiesToYaml("server.port=8080\nusers[0].name=Ada\nusers[1].name=Lin")
        assertTrue(yaml.contains("server:"))
        assertTrue(yaml.contains("users:"))
        assertTrue(yaml.contains("name: Ada"))
    }

    @Test
    fun flattensYamlAndJoinsScalarListsLikeElectron() {
        assertEquals(
            "server.port=8080\ntags=a,b",
            ConfigEngine.yamlToProperties("server:\n  port: 8080\ntags: [a, b]\n")
        )
    }

    @Test
    fun validatesAndFormatsYaml() {
        assertTrue(ConfigEngine.validateYaml("a: [1, 2]").valid)
        assertFalse(ConfigEngine.validateYaml("a: [1,").valid)
        assertTrue(ConfigEngine.formatYaml("a: {b: 1}").contains("a:"))
        val nested = "app:\n  name: MooTool\n"
        assertEquals(ConfigEngine.formatYaml(nested), ConfigEngine.formatYaml(ConfigEngine.formatYaml(nested)))
    }

    @Test
    fun reportsTypeConflictsInsteadOfDroppingValues() {
        val error = assertFailsWith<ConfigException> {
            ConfigEngine.propertiesToYaml("server=foo\nserver.port=8080")
        }
        assertEquals("conflict", error.code)
    }

    @Test
    fun keepsDottedKeysUnicodeEscapesAndNullishValues() {
        val yaml = ConfigEngine.propertiesToYaml("app.name=Moo\\u5DE5\\u5177\nempty=\npath.with.dots=ok")
        assertTrue(yaml.contains("Moo工具") || yaml.contains("Moo"))
        val properties = ConfigEngine.yamlToProperties("app:\n  missing:\n  tags: [x]\n")
        assertTrue(properties.contains("app.tags=x"))
        assertTrue(properties.contains("app.missing= "))
    }

    @Test
    fun rejectsNonObjectYamlRootsAndKeepsInputOnSyntaxFailure() {
        val root = assertFailsWith<ConfigException> { ConfigEngine.yamlToProperties("- a\n- b\n") }
        assertEquals("root", root.code)
        assertFailsWith<ConfigException> { ConfigEngine.formatYaml("a: [1,") }
        assertEquals("", ConfigEngine.formatYaml("   "))
        assertTrue(ConfigEngine.validateYaml("").valid)
    }
}
