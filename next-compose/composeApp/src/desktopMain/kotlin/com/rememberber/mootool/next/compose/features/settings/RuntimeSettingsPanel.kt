package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.CodeRunEngine
import com.rememberber.mootool.next.compose.domain.CodeRunPaths
import com.rememberber.mootool.next.compose.domain.CodeRuntime
import com.rememberber.mootool.next.compose.domain.CodeRuntimeStatus
import com.rememberber.mootool.next.compose.model.RuntimeSettings
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.SettingsGroup
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RuntimeSettingsPanel(container: AppContainer) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var detecting by remember { mutableStateOf(false) }
    var statuses by remember { mutableStateOf<List<CodeRuntimeStatus>>(emptyList()) }

    fun paths(runtime: RuntimeSettings) = CodeRunPaths(
        java = runtime.javaPath,
        groovy = runtime.groovyPath,
        python = runtime.pythonPath,
        node = runtime.nodePath
    )

    fun detect() {
        scope.launch {
            detecting = true
            val next = withContext(Dispatchers.IO) { CodeRunEngine.detect(paths(settings.runtime)) }
            statuses = next
            detecting = false
        }
    }

    LaunchedEffect(
        settings.runtime.javaPath,
        settings.runtime.groovyPath,
        settings.runtime.pythonPath,
        settings.runtime.nodePath
    ) {
        detect()
    }

    SettingsGroup(container.t("settings.group.runtimes")) {
        Text(
            container.t("settings.runtime.hint"),
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            MooButton(
                container.t("settings.runtime.detect"),
                p5Toolbar = true,
                enabled = !detecting,
                onClick = { detect() }
            )
        }
        CodeRuntime.entries.forEach { runtime ->
            val status = statuses.find { it.id == runtime }
            RuntimeRow(
                container = container,
                runtime = runtime,
                path = pathFor(settings.runtime, runtime),
                status = status,
                onPathChange = { value ->
                    container.updateSettings { current ->
                        current.copy(runtime = updatePath(current.runtime, runtime, value))
                    }
                }
            )
        }
        Text(
            container.t("settings.runtime.jvmNote"),
            color = colors.warning,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RuntimeRow(
    container: AppContainer,
    runtime: CodeRuntime,
    path: String,
    status: CodeRuntimeStatus?,
    onPathChange: (String) -> Unit
) {
    val colors = MooTheme.colors
    val label = CodeRunEngine.displayName(runtime)
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.textPrimary, fontSize = 12.sp)
            Text(
                if (status?.available == true) status.version.ifBlank { status.command }
                else container.t("settings.runtime.notFound"),
                color = if (status?.available == true) colors.textSecondary else colors.textMuted,
                fontSize = 11.sp
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MooTextField(
                path,
                onPathChange,
                modifier = Modifier.weight(1f),
                placeholder = status?.command?.takeIf { it.isNotBlank() } ?: container.t("settings.runtime.auto")
            )
            MooButton(
                container.t("settings.runtime.browse"),
                p5Toolbar = true,
                onClick = {
                    val title = "$label ${container.t("settings.runtime.path")}"
                    container.chooseExecutable(title, path.ifBlank { status?.command ?: "" })?.let(onPathChange)
                }
            )
        }
    }
}

private fun pathFor(runtime: RuntimeSettings, id: CodeRuntime): String = when (id) {
    CodeRuntime.Java -> runtime.javaPath
    CodeRuntime.Groovy -> runtime.groovyPath
    CodeRuntime.Python -> runtime.pythonPath
    CodeRuntime.Node -> runtime.nodePath
}

private fun updatePath(runtime: RuntimeSettings, id: CodeRuntime, value: String): RuntimeSettings = when (id) {
    CodeRuntime.Java -> runtime.copy(javaPath = value)
    CodeRuntime.Groovy -> runtime.copy(groovyPath = value)
    CodeRuntime.Python -> runtime.copy(pythonPath = value)
    CodeRuntime.Node -> runtime.copy(nodePath = value)
}
