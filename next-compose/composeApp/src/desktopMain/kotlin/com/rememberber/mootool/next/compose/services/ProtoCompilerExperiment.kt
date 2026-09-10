package com.rememberber.mootool.next.compose.services

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class ProtoExperimentResult(
    val available: Boolean,
    val version: String?,
    val compiled: Boolean,
    val message: String
)

object ProtoCompilerExperiment {
    fun run(workDir: Path = Files.createTempDirectory("mootool-compose-proto")): ProtoExperimentResult {
        val protoc = findProtoc()
        if (protoc == null) {
            return ProtoExperimentResult(false, null, false, "protoc not bundled or on PATH; F07 will ship OS/arch binaries later")
        }
        val version = runCommand(listOf(protoc, "--version"), workDir)
        val proto = workDir.resolve("sample.proto")
        Files.writeString(
            proto,
            """
            syntax = "proto3";
            message Sample {
              string name = 1;
              repeated int32 values = 2;
              map<string, string> labels = 3;
              oneof payload { string text = 4; bytes raw = 5; }
            }
            """.trimIndent()
        )
        val descriptor = workDir.resolve("sample.desc")
        val compiled = runCommand(
            listOf(protoc, "--descriptor_set_out=${descriptor.toAbsolutePath()}", "--include_imports", proto.fileName.toString()),
            workDir
        )
        val ok = Files.exists(descriptor) && Files.size(descriptor) > 0
        return ProtoExperimentResult(true, version, ok, compiled)
    }

    private fun findProtoc(): String? {
        val fromEnv = System.getenv("PROTOC")
        if (!fromEnv.isNullOrBlank()) return fromEnv
        val candidates = listOf("protoc", "/opt/homebrew/bin/protoc", "/usr/local/bin/protoc")
        return candidates.firstOrNull { commandExists(it) }
    }

    private fun commandExists(command: String): Boolean {
        return try {
            val process = ProcessBuilder(if (command.contains('/')) listOf(command, "--version") else listOf(command, "--version"))
                .redirectErrorStream(true)
                .start()
            process.waitFor(2, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun runCommand(command: List<String>, workDir: Path): String {
        val process = ProcessBuilder(command).directory(workDir.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(8, TimeUnit.SECONDS)
        return output.trim()
    }
}
