package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.RuntimeSettings

/** F05 设置 → 检测/运行守卫（可单测，对齐 Electron 运行台可用性语义）。 */
object CodeRunWiringPresentation {
    fun pathsFrom(settings: RuntimeSettings): CodeRunPaths =
        CodeRunPaths(settings.javaPath, settings.groovyPath, settings.pythonPath, settings.nodePath)

    fun availableCount(statuses: List<CodeRuntimeStatus>): Int = statuses.count { it.available }

    fun showConfigureBanner(status: CodeRuntimeStatus?): Boolean = status?.available == false

    fun canRun(status: CodeRuntimeStatus?): Boolean = status?.available != false
}
