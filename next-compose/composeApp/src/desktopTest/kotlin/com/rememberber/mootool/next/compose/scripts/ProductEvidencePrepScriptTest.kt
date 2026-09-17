package com.rememberber.mootool.next.compose.scripts

import com.rememberber.mootool.next.compose.domain.GitEngine
import org.junit.Assume
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductEvidencePrepScriptTest {
    @Test
    fun prepareVaultConflictScriptWritesSampleJson() {
        val composeRoot = locateNextComposeRoot()
        val dataRoot = Files.createTempDirectory("mootool-evidence-vault-conflict-")
        try {
            runPrepScript(composeRoot, "prepare-vault-conflict-evidence.sh", dataRoot)
            val sample = dataRoot.resolve("data/vaults/json/sample.json")
            assertTrue(sample.isRegularFile(), "missing sample.json at $sample")
            assertTrue(sample.toFile().readText().contains("vault-external-conflict"))
            val quickNote = dataRoot.resolve("data/vaults/quick-note/sample-external.md")
            assertTrue(quickNote.isRegularFile(), "missing quick-note sample at $quickNote")
        } finally {
            dataRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun prepareGitMergeConflictScriptLeavesUnmergedConflictJson() {
        Assume.assumeTrue("git CLI not installed", GitEngine.detect(isolateConfig = true).available)
        val composeRoot = locateNextComposeRoot()
        val dataRoot = Files.createTempDirectory("mootool-evidence-git-merge-")
        try {
            runPrepScript(composeRoot, "prepare-git-merge-conflict-evidence.sh", dataRoot)
            val vault = dataRoot.resolve("data/vaults/json")
            assertTrue(vault.resolve("conflict.json").isRegularFile())
            assertTrue(vault.resolve(".git/MERGE_HEAD").exists())
            val unmerged = git(vault, "diff", "--name-only", "--diff-filter=U").trim()
            assertEquals("conflict.json", unmerged)
        } finally {
            dataRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun prepareEditorImeScriptWritesJsonAndQuickNoteSamples() {
        val composeRoot = locateNextComposeRoot()
        val dataRoot = Files.createTempDirectory("mootool-evidence-ime-")
        try {
            runPrepScript(composeRoot, "prepare-editor-ime-evidence.sh", dataRoot)
            val jsonSample = dataRoot.resolve("data/vaults/json/ime-sample.json")
            val noteSample = dataRoot.resolve("data/vaults/quick-note/ime-sample.md")
            assertTrue(jsonSample.isRegularFile())
            assertTrue(noteSample.isRegularFile())
            assertTrue(noteSample.toFile().readText().contains("IME"))
        } finally {
            dataRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun prepareTrayScreencaptureScriptRestoresBaselinePng() {
        val composeRoot = locateNextComposeRoot()
        val dataRoot = Files.createTempDirectory("mootool-evidence-tray-")
        try {
            runPrepScript(composeRoot, "prepare-tray-screencapture-evidence.sh", dataRoot)
            val baseline =
                composeRoot.resolve(
                    "docs/evidence/2026-09-17-tray-tcc-screencapture/reference/57-color-baseline.png",
                )
            assertTrue(baseline.isRegularFile(), "missing baseline at $baseline")
        } finally {
            dataRoot.toFile().deleteRecursively()
        }
    }

    private fun runPrepScript(composeRoot: Path, scriptName: String, dataRoot: Path) {
        val script = composeRoot.resolve("scripts/$scriptName")
        assertTrue(script.isRegularFile(), "missing script $script")
        val process =
            ProcessBuilder("bash", script.toString())
                .directory(composeRoot.toFile())
                .redirectErrorStream(true)
                .apply { environment()["MOOTOOL_COMPOSE_DATA_DIR"] = dataRoot.toString() }
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        assertTrue(code == 0, "script $scriptName failed ($code): $output")
    }

    private fun git(root: Path, vararg args: String): String {
        val process =
            ProcessBuilder(listOf("git") + args)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        assertTrue(code == 0, "git ${args.joinToString(" ")} failed ($code): $output")
        return output
    }

    private fun locateNextComposeRoot(): Path {
        val candidates = listOfNotNull(
            System.getProperty("next.compose.root")?.let { Path.of(it) },
            Path.of(System.getProperty("user.dir") ?: "."),
            Path.of(System.getProperty("user.dir") ?: ".").parent,
            Path.of(System.getProperty("user.dir") ?: ".").parent?.parent,
        )
        return candidates.firstOrNull { it.resolve("scripts/prepare-vault-conflict-evidence.sh").isRegularFile() }
            ?: error(
                "Could not locate next-compose root (set -Dnext.compose.root=... or run from composeApp). " +
                    "Tried: ${candidates.joinToString()}",
            )
    }
}
