package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateEngineTest {
    private val darwin = UpdateIdentity("darwin", "x64")

    @Test
    fun comparesSemverLikeElectron() {
        assertEquals(1, UpdateEngine.compareVersions("1.7.9", "1.7.8"))
        assertEquals(1, UpdateEngine.compareVersions("2.0.0-beta.2", "2.0.0-beta.1"))
        assertEquals(1, UpdateEngine.compareVersions("2.0.0-beta.10", "2.0.0-beta.2"))
        assertEquals(1, UpdateEngine.compareVersions("2.0.0", "2.0.0-beta.2"))
        assertEquals(0, UpdateEngine.compareVersions("2.0.0+build.2", "2.0.0+build.1"))
        assertEquals(0, UpdateEngine.compareVersions("v1.7", "1.7.0"))
        assertFailsWith<IllegalStateException> { UpdateEngine.compareVersions("2.0.0-beta.01", "2.0.0-beta.1") }
    }

    @Test
    fun namesComposeArtifacts() {
        assertEquals(
            "MooTool-Next-Compose-0.1.0-mac-arm64.dmg",
            UpdateEngine.artifactName("0.1.0", "darwin", "arm64", "dmg")
        )
        assertEquals(
            "MooTool-Next-Compose-0.1.0-win-x64-setup.msi",
            UpdateEngine.artifactName("0.1.0", "win32", "x64", "msi")
        )
        assertEquals(
            "MooTool-Next-Compose-0.1.0-linux-x64.deb",
            UpdateEngine.artifactName("0.1.0", "linux", "amd64", "deb")
        )
    }

    @Test
    fun missingComposeNodeIsUnpublishedNotElectron() {
        val result = UpdateEngine.check("0.1.0", darwin, MANIFEST_WITHOUT_COMPOSE)
        assertEquals(UpdateCheckStatus.Unpublished, result.status)
        assertEquals("next-compose", result.productId)
        assertNull(result.download)
        assertTrue(result.releaseUrl.contains("next-compose"))
    }

    @Test
    fun selectsCurrentPlatformAndIgnoresOlderAndPrerelease() {
        val result = UpdateEngine.check("0.1.0", darwin, MANIFEST_WITH_COMPOSE)
        assertEquals(UpdateCheckStatus.Available, result.status)
        assertEquals("0.2.0", result.latestVersion)
        assertEquals("MooTool-Next-Compose-0.2.0-mac-x64.dmg", result.download?.fileName)
        assertTrue(result.releaseNotes.contains("0.2.0"))
        assertEquals(
            UpdateCheckStatus.Latest,
            UpdateEngine.check("0.2.0", darwin, MANIFEST_WITH_COMPOSE).status
        )
        val linux = UpdateEngine.check("0.1.0", UpdateIdentity("linux", "x64"), MANIFEST_WITH_COMPOSE)
        assertEquals(UpdateCheckStatus.Available, linux.status)
        assertNull(linux.download)
        assertEquals("0.2.0", linux.latestVersion)
        assertEquals(
            UpdateCheckStatus.Unpublished,
            UpdateEngine.check("0.1.0", darwin, MANIFEST_INACTIVE_COMPOSE).status
        )
        val arm = UpdateEngine.check("0.1.0", UpdateIdentity("darwin", "arm64"), MANIFEST_WITH_COMPOSE)
        assertEquals(UpdateCheckStatus.Available, arm.status)
        assertNull(arm.download)
    }

    @Test
    fun downloadsHttpsPayloadAndVerifiesSha512() {
        val payload = "compose-update".toByteArray()
        val sha = UpdateEngine.sha512Base64(payload)
        val root = Files.createTempDirectory("mootool-compose-update-")
        try {
            val downloader = UpdateDownloader(root) { _ -> 200 to payload.inputStream() }
            val pack = UpdateDownload("MooTool-Next-Compose-0.2.0-mac-x64.dmg", "https://example.com/app.dmg", sha, payload.size.toLong())
            val path = downloader.download(pack)
            assertEquals(payload.toList(), Files.readAllBytes(path).toList())
            assertEquals(path, downloader.readyFile(pack))
            assertFailsWith<IllegalStateException> {
                downloader.download(
                    UpdateDownload("bad-size.dmg", "https://example.com/bad.dmg", sha, payload.size.toLong() + 1)
                )
            }
            assertTrue(!Files.exists(root.resolve("bad-size.dmg")))
            assertTrue(!Files.exists(root.resolve("bad-size.dmg.ready")))
            assertTrue(!Files.exists(root.resolve(".bad-size.dmg.download")))
            val otherSha = UpdateEngine.sha512Base64("other".toByteArray())
            assertFailsWith<IllegalStateException> {
                downloader.download(
                    UpdateDownload("bad-hash.dmg", "https://example.com/bad-hash.dmg", otherSha, payload.size.toLong())
                )
            }
            assertTrue(!Files.exists(root.resolve("bad-hash.dmg")))
            assertTrue(!Files.exists(root.resolve("bad-hash.dmg.ready")))
            assertFailsWith<IllegalStateException> {
                UpdateEngine.requireHttps("http://example.com/feed.json", "feed")
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    companion object {
        private val PAYLOAD_SHA = UpdateEngine.sha512Base64("compose-update".toByteArray())
        private val MANIFEST_WITHOUT_COMPOSE = """
            {"schemaVersion":1,"products":{"next-electron":{"displayName":"Electron","status":"active","releases":[]}}}
        """.trimIndent()
        private val MANIFEST_INACTIVE_COMPOSE = """
            {"schemaVersion":1,"products":{"next-compose":{"displayName":"MooTool Next Compose","status":"planned","releases":[]}}}
        """.trimIndent()
        private val MANIFEST_WITH_COMPOSE = """
            {
              "schemaVersion": 1,
              "products": {
                "next-electron": {
                  "displayName": "Electron",
                  "status": "active",
                  "releases": [{
                    "version": "9.9.9",
                    "title": "Electron 9.9.9",
                    "notes": "other product",
                    "prerelease": false,
                    "releaseUrl": "https://example.com/electron",
                    "assets": []
                  }]
                },
                "next-compose": {
                  "displayName": "MooTool Next Compose",
                  "status": "active",
                  "releases": [
                    {
                      "version": "0.1.5",
                      "title": "0.1.5",
                      "notes": "older",
                      "prerelease": false,
                      "releaseUrl": "https://example.com/compose-015",
                      "assets": []
                    },
                    {
                      "version": "0.3.0-beta.1",
                      "title": "beta",
                      "notes": "prerelease",
                      "prerelease": true,
                      "releaseUrl": "https://example.com/compose-beta",
                      "assets": []
                    },
                    {
                      "version": "0.2.0",
                      "title": "Compose 0.2.0",
                      "notes": "newer",
                      "prerelease": false,
                      "releaseUrl": "https://example.com/compose-020",
                      "assets": [
                        {
                          "platform": "darwin",
                          "architecture": "x64",
                          "packageType": "dmg",
                          "priority": 10,
                          "fileName": "MooTool-Next-Compose-0.2.0-mac-x64.dmg",
                          "url": "https://example.com/compose.dmg",
                          "sha512": "$PAYLOAD_SHA",
                          "size": 14
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }
}
