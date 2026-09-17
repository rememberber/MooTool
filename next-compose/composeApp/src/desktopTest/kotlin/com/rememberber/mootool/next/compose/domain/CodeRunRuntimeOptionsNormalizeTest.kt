package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.CodeRunSessionSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeRunRuntimeOptionsNormalizeTest {
    @Test
    fun truncatesArgumentsAndWorkingDirectoryLikeElectronNormalizeRuntimeOptions() {
        val longArgs = "a".repeat(CodeRunRuntimeOptionsNormalize.MAX_ARGUMENTS_CHARS + 50)
        val longDir = "/tmp/" + "b".repeat(CodeRunRuntimeOptionsNormalize.MAX_WORKING_DIRECTORY_CHARS)
        val normalized = CodeRunRuntimeOptionsNormalize.normalizeSnapshot(
            CodeRunSessionSnapshot(
                javaArguments = longArgs,
                javaWorkingDirectory = longDir,
            ),
        )
        assertEquals(CodeRunRuntimeOptionsNormalize.MAX_ARGUMENTS_CHARS, normalized.javaArguments.length)
        assertEquals(longArgs.take(CodeRunRuntimeOptionsNormalize.MAX_ARGUMENTS_CHARS), normalized.javaArguments)
        assertEquals(CodeRunRuntimeOptionsNormalize.MAX_WORKING_DIRECTORY_CHARS, normalized.javaWorkingDirectory.length)
        assertEquals(longDir.take(CodeRunRuntimeOptionsNormalize.MAX_WORKING_DIRECTORY_CHARS), normalized.javaWorkingDirectory)
    }

    @Test
    fun truncatesDraftCodeToMaxCodeBytes() {
        val huge = "x".repeat(CodeRunEngine.MAX_CODE_BYTES + 100)
        val normalized = CodeRunRuntimeOptionsNormalize.normalizeSnapshot(
            CodeRunSessionSnapshot(pythonCode = huge),
        )
        assertEquals(CodeRunEngine.MAX_CODE_BYTES, normalized.pythonCode.length)
    }
}
