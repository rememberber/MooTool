package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.sessions.CodeRunSessionSnapshot

/** Aligns with Electron `normalizeRuntimeOptions` / `normalizeRuntimeDrafts` in `settings.ts`. */
object CodeRunRuntimeOptionsNormalize {
    const val MAX_ARGUMENTS_CHARS = 2_000
    const val MAX_WORKING_DIRECTORY_CHARS = 1_000

    fun normalizeArguments(value: String): String =
        if (value.length <= MAX_ARGUMENTS_CHARS) value else value.take(MAX_ARGUMENTS_CHARS)

    fun normalizeWorkingDirectory(value: String): String =
        if (value.length <= MAX_WORKING_DIRECTORY_CHARS) value else value.take(MAX_WORKING_DIRECTORY_CHARS)

    fun normalizeSnapshot(snapshot: CodeRunSessionSnapshot): CodeRunSessionSnapshot {
        val limit = CodeRunEngine.MAX_CODE_BYTES
        return snapshot.copy(
            javaCode = snapshot.javaCode.take(limit),
            groovyCode = snapshot.groovyCode.take(limit),
            pythonCode = snapshot.pythonCode.take(limit),
            nodeCode = snapshot.nodeCode.take(limit),
            javaArguments = normalizeArguments(snapshot.javaArguments),
            groovyArguments = normalizeArguments(snapshot.groovyArguments),
            pythonArguments = normalizeArguments(snapshot.pythonArguments),
            nodeArguments = normalizeArguments(snapshot.nodeArguments),
            javaWorkingDirectory = normalizeWorkingDirectory(snapshot.javaWorkingDirectory),
            groovyWorkingDirectory = normalizeWorkingDirectory(snapshot.groovyWorkingDirectory),
            pythonWorkingDirectory = normalizeWorkingDirectory(snapshot.pythonWorkingDirectory),
            nodeWorkingDirectory = normalizeWorkingDirectory(snapshot.nodeWorkingDirectory),
        )
    }
}
