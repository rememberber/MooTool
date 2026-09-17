package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooGhostButton
import com.rememberber.mootool.next.compose.ui.components.mooFocusOutline
import com.rememberber.mootool.next.compose.ui.components.MooCompactSearch
import com.rememberber.mootool.next.compose.ui.components.MooSelect
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.AccentSwatches
import com.rememberber.mootool.next.compose.ui.components.SettingsNavItem
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose 场景下 Tab 焦点外描边（非产品主窗、不能代替系统 IME）。
 * 与 `116-json-tab-focus.png` 等 AX 产品窗帧互补，供 CI 回归 modern 焦点环 token。
 */
class ToolbarFocusCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureCommandSearchAndJsonCopyFocusRings() = runDesktopComposeUiTest(width = 720, height = 400) {
        val zh = Translator(AppLanguage.ZhCN)
        val searchFocus = FocusRequester()
        val copyFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.workspace)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        Modifier
                            .width(620.dp)
                            .mooDialogSurface(MooTheme.dimens.commandRadius)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MooTextField(
                                "",
                                {},
                                modifier = Modifier.weight(1f),
                                placeholder = zh.t("app.search.placeholder"),
                                fieldModifier = Modifier
                                    .focusRequester(searchFocus)
                                    .testTag("commandSearchField")
                            )
                        }
                    }
                    MooButton(
                        zh.t("json.action.copy"),
                        onClick = {},
                        modifier = Modifier.focusRequester(copyFocus)
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0

        runOnIdle { searchFocus.requestFocus() }
        waitForIdle()
        val searchImage = onRoot().captureToImage().toAwtImage()
        val searchFile = File(dir, "127-compose-command-search-tab-focus.png")
        assertTrue(ImageIO.write(searchImage, "png", searchFile))
        assertTrue(searchFile.length() > 500)

        runOnIdle { copyFocus.requestFocus() }
        waitForIdle()
        val copyImage = onRoot().captureToImage().toAwtImage()
        val copyFile = File(dir, "128-compose-json-copy-tab-focus.png")
        assertTrue(ImageIO.write(copyImage, "png", copyFile))
        val copyHits = countRingPixels(copyImage, expected)
        assertTrue(copyHits >= 8, "json copy focus ring hits=$copyHits")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureJsonVaultSearchFocusRing() = runDesktopComposeUiTest(width = 420, height = 160) {
        val zh = Translator(AppLanguage.ZhCN)
        val searchFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                MooCompactSearch(
                    "",
                    {},
                    placeholder = zh.t("app.search.placeholder"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(colors.workspace),
                    fieldModifier = Modifier
                        .focusRequester(searchFocus)
                        .testTag("jsonVaultSearchField"),
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { searchFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "131-compose-json-vault-search-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "json vault search focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureHttpCollectionSearchFocusRing() = runDesktopComposeUiTest(width = 280, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val searchFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                MooCompactSearch(
                    "",
                    {},
                    placeholder = zh.t("common.search"),
                    modifier = Modifier
                        .width(210.dp)
                        .padding(16.dp)
                        .background(colors.workspace),
                    fieldModifier = Modifier
                        .focusRequester(searchFocus)
                        .testTag("httpCollectionSearchField"),
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { searchFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "132-compose-http-collection-search-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "http collection search focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureHostProfileSearchFocusRing() = runDesktopComposeUiTest(width = 280, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val searchFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                MooCompactSearch(
                    "",
                    {},
                    placeholder = zh.t("common.search"),
                    modifier = Modifier
                        .width(220.dp)
                        .padding(16.dp)
                        .background(colors.workspace),
                    fieldModifier = Modifier
                        .focusRequester(searchFocus)
                        .testTag("hostProfileSearchField"),
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { searchFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "133-compose-host-profile-search-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "host profile search focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSettingsAppearanceNavFocusRing() = runDesktopComposeUiTest(width = 280, height = 96) {
        val zh = Translator(AppLanguage.ZhCN)
        val navFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                SettingsNavItem(
                    zh.t("settings.appearance"),
                    selected = true,
                    onClick = {},
                    modifier = Modifier
                        .background(colors.workspace)
                        .padding(16.dp)
                        .focusRequester(navFocus),
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { navFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "134-compose-settings-appearance-nav-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "settings appearance nav focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSettingsLanguageSelectFocusRing() = runDesktopComposeUiTest(width = 360, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val triggerFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(Modifier.background(colors.workspace).padding(16.dp)) {
                    MooSelect(
                        options = AppLanguage.entries.map { it.code to zh.t("settings.language.${it.code}") },
                        value = AppLanguage.ZhCN.code,
                        onChange = {},
                        triggerModifier = Modifier.focusRequester(triggerFocus),
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { triggerFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "135-compose-settings-language-select-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "settings language select focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSettingsRuntimeCategoryNavIconFocusRing() = runDesktopComposeUiTest(width = 300, height = 96) {
        val zh = Translator(AppLanguage.ZhCN)
        val navFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                SettingsNavItem(
                    label = zh.t("settings.category.runtime"),
                    selected = false,
                    onClick = {},
                    icon = ">_",
                    modifier = Modifier
                        .background(colors.workspace)
                        .padding(16.dp)
                        .focusRequester(navFocus),
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { navFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "154-compose-settings-runtime-category-nav-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "settings runtime category nav icon focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSidebarLanguageChipFocusRing() = runDesktopComposeUiTest(width = 120, height = 80) {
        val chipFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val shape = RoundedCornerShape(6.dp)
                Text(
                    "中",
                    color = colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(colors.workspace)
                        .mooFocusOutline(focused, shape)
                        .clip(shape)
                        .border(
                            if (focused) 2.dp else 0.dp,
                            if (focused) colors.focusRing else Color.Transparent,
                            shape,
                        )
                        .focusRequester(chipFocus)
                        .focusable(true, interaction)
                        .clickable(interactionSource = interaction, indication = null, onClick = {})
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .semantics { role = Role.Button },
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { chipFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "136-compose-sidebar-language-chip-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "sidebar language chip focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSidebarSearchGhostFocusRing() = runDesktopComposeUiTest(width = 96, height = 96) {
        val searchFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                MooGhostButton(
                    "search",
                    onClick = {},
                    modifier = Modifier
                        .padding(16.dp)
                        .background(colors.workspace)
                        .focusRequester(searchFocus),
                ) {
                    Text("⌕", color = colors.textMuted, fontSize = 13.sp)
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { searchFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "137-compose-sidebar-search-ghost-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "sidebar search ghost focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureCommandPaletteResultRowFocusRing() = runDesktopComposeUiTest(width = 360, height = 88) {
        val zh = Translator(AppLanguage.ZhCN)
        val rowFocus = FocusRequester()
        val tool = ToolRegistry.byId.getValue(ToolId.Json)
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val shape = RoundedCornerShape(MooTheme.dimens.navRadius)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(colors.workspace)
                        .mooFocusOutline(focused, shape)
                        .clip(shape)
                        .background(colors.sidebarItemBrush(true, card = false, hovered = false))
                        .border(2.dp, colors.focusRing, shape)
                        .focusRequester(rowFocus)
                        .focusable(true, interaction)
                        .clickable(interactionSource = interaction, indication = null, onClick = {})
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        zh.t(tool.titleKey),
                        color = colors.navSelectedContent(),
                        fontSize = 13.sp,
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { rowFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "140-compose-command-palette-result-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "command palette result row focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureCommandPaletteCloseButtonFocusRing() = runDesktopComposeUiTest(width = 200, height = 72) {
        val zh = Translator(AppLanguage.ZhCN)
        val closeFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val shape = RoundedCornerShape(MooTheme.dimens.navRadius)
                Text(
                    "×",
                    color = colors.textSecondary,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(colors.workspace)
                        .focusRequester(closeFocus)
                        .mooFocusOutline(focused, shape)
                        .clip(shape)
                        .border(2.dp, colors.focusRing, shape)
                        .focusable(true, interaction)
                        .clickable(interactionSource = interaction, indication = null, onClick = {})
                        .padding(6.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = zh.t("app.search.close")
                        },
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { closeFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "149-compose-command-palette-close-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "command palette close button focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureCommandPaletteSearchRowTabChrome() = runDesktopComposeUiTest(width = 420, height = 56) {
        val zh = Translator(AppLanguage.ZhCN)
        val closeFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                val closeInteraction = remember { MutableInteractionSource() }
                val closeFocused by closeInteraction.collectIsFocusedAsState()
                val shape = RoundedCornerShape(MooTheme.dimens.commandRadius)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(colors.workspace)
                        .clip(shape)
                        .background(colors.toolbar)
                        .border(1.dp, colors.borderSoft, shape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MooTextField(
                        "",
                        {},
                        modifier = Modifier.weight(1f),
                        placeholder = zh.t("app.search.placeholder"),
                    )
                    Text(
                        "×",
                        color = colors.textSecondary,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .focusRequester(closeFocus)
                            .mooFocusOutline(closeFocused, RoundedCornerShape(MooTheme.dimens.navRadius))
                            .clip(RoundedCornerShape(MooTheme.dimens.navRadius))
                            .border(2.dp, colors.focusRing, RoundedCornerShape(MooTheme.dimens.navRadius))
                            .focusable(true, closeInteraction)
                            .clickable(interactionSource = closeInteraction, indication = null, onClick = {})
                            .padding(6.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = zh.t("app.search.close")
                            },
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { closeFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "151-compose-command-palette-search-row-tab.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "command palette search row close focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSidebarNavItemFocusRing() = runDesktopComposeUiTest(width = 240, height = 72) {
        val zh = Translator(AppLanguage.ZhCN)
        val navFocus = FocusRequester()
        val tool = ToolRegistry.byId.getValue(ToolId.Json)
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val shape = RoundedCornerShape(MooTheme.dimens.navRadius)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(colors.sidebar)
                        .mooFocusOutline(focused, shape)
                        .clip(shape)
                        .background(colors.sidebarItemBrush(true, card = false, hovered = false))
                        .border(1.dp, colors.navItemBorder(true, hovered = false), shape)
                        .focusRequester(navFocus)
                        .focusable(true, interaction)
                        .clickable(interactionSource = interaction, indication = null, onClick = {})
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        zh.t(tool.titleKey),
                        color = colors.navSelectedContent(),
                        fontSize = 13.sp,
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { navFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "138-compose-sidebar-nav-item-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "sidebar nav item focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureSettingsAccentSwatchFocusRing() = runDesktopComposeUiTest(width = 200, height = 96) {
        val swatchFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                AccentSwatches(
                    "blue",
                    {},
                    firstSwatchModifier = Modifier
                        .padding(16.dp)
                        .background(colors.workspace)
                        .focusRequester(swatchFocus),
                )
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { swatchFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "139-compose-settings-accent-swatch-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "settings accent swatch focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureHttpSendAndTranslationNowFocusRings() = runDesktopComposeUiTest(width = 520, height = 280) {
        val zh = Translator(AppLanguage.ZhCN)
        val sendFocus = FocusRequester()
        val translateFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.workspace)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MooButton(
                        zh.t("http.send"),
                        prominent = true,
                        p5Toolbar = true,
                        onClick = {},
                        modifier = Modifier.focusRequester(sendFocus)
                    )
                    MooButton(
                        zh.t("translation.now"),
                        prominent = true,
                        p5Toolbar = true,
                        onClick = {},
                        modifier = Modifier.focusRequester(translateFocus)
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0

        runOnIdle { sendFocus.requestFocus() }
        waitForIdle()
        val sendFile = File(dir, "129-compose-http-send-tab-focus.png")
        assertTrue(ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", sendFile))
        assertTrue(countRingPixels(ImageIO.read(sendFile), expected) >= 8, "http send focus ring")

        runOnIdle { translateFocus.requestFocus() }
        waitForIdle()
        val translateFile = File(dir, "130-compose-translation-now-tab-focus.png")
        assertTrue(ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", translateFile))
        assertTrue(countRingPixels(ImageIO.read(translateFile), expected) >= 8, "translation now focus ring")
    }

    private fun countRingPixels(image: java.awt.image.BufferedImage, rgb: Int): Int {
        var hits = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) and 0x00FFFFFF == rgb) hits++
            }
        }
        return hits
    }
}
