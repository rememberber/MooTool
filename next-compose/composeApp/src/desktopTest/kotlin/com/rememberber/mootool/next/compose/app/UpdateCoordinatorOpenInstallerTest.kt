package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.domain.UpdateEngine
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class UpdateCoordinatorOpenInstallerTest {
    @Test
    fun openInstallerInvokesOpenerAfterDownloadReady() = runBlocking {
        val payload = "compose-installer".toByteArray()
        val sha = UpdateEngine.sha512Base64(payload)
        val manifest = manifestWithCompose(sha, payload.size.toLong())
        val root = Files.createTempDirectory("mootool-update-open-")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var opened: java.nio.file.Path? = null
        try {
            val coordinator = UpdateCoordinator(
                cacheRoot = root,
                scope = scope,
                feedUrl = "https://example.com/feed.json",
                textFetcher = { _ -> 200 to manifest },
                bytesFetcher = { _ -> 200 to ByteArrayInputStream(payload) },
                opener = { opened = it },
            )
            coordinator.check(autoDownload = true)
            for (attempt in 0 until 200) {
                val state = coordinator.state.value
                if (state.status == "ready" && state.downloaded != null) break
                delay(25)
            }
            val downloaded = coordinator.state.value.downloaded
            assertNotNull(downloaded)
            coordinator.openInstaller()
            assertEquals(downloaded, opened)
        } finally {
            scope.cancel()
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun openInstallerFailureSurfacesError() = runBlocking {
        val payload = "compose-installer-fail".toByteArray()
        val sha = UpdateEngine.sha512Base64(payload)
        val manifest = manifestWithCompose(sha, payload.size.toLong())
        val root = Files.createTempDirectory("mootool-update-open-fail-")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinator = UpdateCoordinator(
                cacheRoot = root,
                scope = scope,
                feedUrl = "https://example.com/feed.json",
                textFetcher = { _ -> 200 to manifest },
                bytesFetcher = { _ -> 200 to ByteArrayInputStream(payload) },
                opener = { throw IllegalStateException("desktop blocked") },
            )
            coordinator.check(autoDownload = true)
            for (attempt in 0 until 200) {
                if (coordinator.state.value.downloaded != null) break
                delay(25)
            }
            coordinator.openInstaller()
            assertTrue(coordinator.state.value.error.contains("desktop blocked"))
        } finally {
            scope.cancel()
            root.toFile().deleteRecursively()
        }
    }

    private fun manifestWithCompose(sha512: String, size: Long): String {
        val identity = UpdateEngine.detectIdentity()
        val packageType = when (identity.platform) {
            "darwin" -> "dmg"
            "win32" -> "msi"
            else -> "deb"
        }
        val fileName = UpdateEngine.artifactName("0.2.0", identity.platform, identity.architecture, packageType)
        return """
            {
              "schemaVersion": 1,
              "products": {
                "next-compose": {
                  "displayName": "MooTool Next Compose",
                  "status": "active",
                  "releases": [{
                    "version": "0.2.0",
                    "title": "Compose 0.2.0",
                    "notes": "newer",
                    "prerelease": false,
                    "releaseUrl": "https://example.com/compose-020",
                    "assets": [{
                      "platform": "${identity.platform}",
                      "architecture": "${identity.architecture}",
                      "packageType": "$packageType",
                      "priority": 10,
                      "fileName": "$fileName",
                      "url": "https://example.com/compose.pkg",
                      "sha512": "$sha512",
                      "size": $size
                    }]
                  }]
                }
              }
            }
        """.trimIndent()
    }
}
