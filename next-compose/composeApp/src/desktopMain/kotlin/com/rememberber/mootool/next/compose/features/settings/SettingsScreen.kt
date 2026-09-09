package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.CloseBehavior
import com.rememberber.mootool.next.compose.model.InterfaceStyle
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

private enum class SettingsCategory { General, Appearance, Layout, Editor, Network, Data, Vault, Runtime, Tools, Shortcuts, About }

@Composable
fun SettingsScreen(container: AppContainer) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    var category by remember { mutableStateOf(SettingsCategory.General) }
    Row(Modifier.fillMaxSize().background(colors.workspace)) {
        Column(Modifier.width(200.dp).background(colors.sidebar).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCategory.entries.forEach { item ->
                val selected = item == category
                Text(
                    container.t("settings.${item.name.lowercase()}"),
                    color = if (selected) colors.accent else colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().clickable { category = item }.padding(8.dp)
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(container.t("settings.title"), fontSize = 18.sp, color = colors.textPrimary)
            when (category) {
                SettingsCategory.General -> {
                    Label(container.t("settings.language"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { language ->
                            MooButton(language.code, primary = settings.general.language == language.code, onClick = {
                                container.updateSettings { it.copy(general = it.general.copy(language = language.code)) }
                            })
                        }
                    }
                    Label(container.t("settings.closeBehavior"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CloseBehavior.entries.forEach { behavior ->
                            MooButton(behavior.name.lowercase(), primary = settings.general.closeBehavior == behavior.name.lowercase(), onClick = {
                                container.updateSettings { it.copy(general = it.general.copy(closeBehavior = behavior.name.lowercase())) }
                            })
                        }
                    }
                    MooButton(
                        container.t("settings.tray") + ": " + settings.general.trayEnabled,
                        onClick = { container.updateSettings { it.copy(general = it.general.copy(trayEnabled = !it.general.trayEnabled)) } }
                    )
                }
                SettingsCategory.Appearance -> {
                    Label(container.t("settings.theme"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system", "light", "dark").forEach { theme ->
                            MooButton(container.t("settings.theme.$theme"), primary = settings.appearance.theme == theme, onClick = {
                                container.updateSettings { it.copy(appearance = it.appearance.copy(theme = theme)) }
                            })
                        }
                    }
                    Label(container.t("settings.style"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InterfaceStyle.entries.forEach { style ->
                            val id = style.name.lowercase().replace("miuiv5", "miui-v5")
                            MooButton(
                                style.name,
                                primary = settings.appearance.interfaceStyle == id || (style == InterfaceStyle.Modern && settings.appearance.interfaceStyle == "modern"),
                                enabled = style == InterfaceStyle.Modern,
                                onClick = {
                                    container.updateSettings { it.copy(appearance = it.appearance.copy(interfaceStyle = "modern")) }
                                }
                            )
                        }
                    }
                    Text(container.t("settings.style.unsupported"), color = colors.warning, fontSize = 12.sp)
                }
                SettingsCategory.Layout -> {
                    Toggle(container, container.t("settings.nav.recent"), settings.layout.showRecent) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(showRecent = !current.layout.showRecent)) }
                    }
                    Toggle(container, container.t("settings.nav.separators"), settings.layout.showSeparators) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(showSeparators = !current.layout.showSeparators)) }
                    }
                    Toggle(container, container.t("settings.nav.collapsed"), settings.layout.hideNavigationTitles) {
                        container.updateSettings { current -> current.copy(layout = current.layout.copy(hideNavigationTitles = !current.layout.hideNavigationTitles)) }
                    }
                }
                SettingsCategory.Editor -> {
                    Toggle(container, container.t("settings.softWrap"), settings.editor.softWrap) {
                        container.updateSettings { current -> current.copy(editor = current.editor.copy(softWrap = !current.editor.softWrap)) }
                    }
                    Label(container.t("settings.jsonFontSize") + ": ${settings.editor.jsonFontSize}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(12, 13, 14, 16, 18).forEach { size ->
                            MooButton(size.toString(), primary = settings.editor.jsonFontSize == size, onClick = {
                                container.updateSettings { it.copy(editor = it.editor.copy(jsonFontSize = size)) }
                            })
                        }
                    }
                }
                SettingsCategory.Data -> {
                    Label(container.t("settings.dataPath"))
                    Text(container.directories.dataRoot.toString(), color = colors.textSecondary)
                    MooButton(container.t("settings.openData"), onClick = { container.openDirectory(container.directories.dataRoot) })
                }
                SettingsCategory.About -> {
                    Text(ProductIdentity.DISPLAY_NAME, fontSize = 16.sp, color = colors.textPrimary)
                    Text("v${ProductIdentity.VERSION}", color = colors.textSecondary)
                    Text(ProductIdentity.APPLICATION_ID, color = colors.textSecondary)
                    Text(container.t("settings.style.unsupported"), color = colors.textSecondary, fontSize = 12.sp)
                }
                SettingsCategory.Runtime -> {
                    Text(container.t("settings.runtime.hint"), color = colors.textSecondary, fontSize = 12.sp)
                    Label("Java")
                    MooTextField(settings.runtime.javaPath, {
                        container.updateSettings { current -> current.copy(runtime = current.runtime.copy(javaPath = it)) }
                    }, placeholder = container.t("settings.runtime.auto"))
                    Label("Groovy")
                    MooTextField(settings.runtime.groovyPath, {
                        container.updateSettings { current -> current.copy(runtime = current.runtime.copy(groovyPath = it)) }
                    }, placeholder = container.t("settings.runtime.auto"))
                    Label("Python")
                    MooTextField(settings.runtime.pythonPath, {
                        container.updateSettings { current -> current.copy(runtime = current.runtime.copy(pythonPath = it)) }
                    }, placeholder = container.t("settings.runtime.auto"))
                    Label("Node.js")
                    MooTextField(settings.runtime.nodePath, {
                        container.updateSettings { current -> current.copy(runtime = current.runtime.copy(nodePath = it)) }
                    }, placeholder = container.t("settings.runtime.auto"))
                    Text(container.t("settings.runtime.jvmNote"), color = colors.warning, fontSize = 12.sp)
                }
                SettingsCategory.Network, SettingsCategory.Vault, SettingsCategory.Tools, SettingsCategory.Shortcuts -> {
                    Text(container.t("common.notImplemented"), color = colors.warning)
                    Text(container.t("app.tool.unimplemented"), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
}

@Composable
private fun Toggle(container: AppContainer, label: String, value: Boolean, onClick: () -> Unit) {
    MooButton("$label: $value", onClick = onClick)
}
