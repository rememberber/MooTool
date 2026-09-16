package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonPathEntry
import com.rememberber.mootool.next.compose.domain.JsonStatus
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.ui.components.MooCard
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * JSON 检查器内联路径树双击查询后的结果区 Compose 场景帧（非产品主窗）。
 */
class JsonInspectorCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureInlinePathDoubleTapQueryResult() = runDesktopComposeUiTest(width = 360, height = 280) {
        val zh = Translator(AppLanguage.ZhCN)
        val jsonT = JsonTranslator { key, params -> zh.t(key, params) }
        val input = """{"store":{"books":[{"title":"One"}]}}"""
        val session = JsonSession()
        session.editor.setText(input, recordUndo = false)
        val path = "$.store.books[0].title"
        val pathApplied = zh.t("json.notice.pathApplied")
        val pathTitle = zh.t("json.panel.jsonPath")
        session.performInlinePathTreeDoubleTapQuery(
            input = input,
            path = path,
            previewFallback = "",
            pathAppliedNotice = pathApplied,
            queryPanelTitle = pathTitle,
            translator = jsonT,
        )
        val status = JsonEngine.validate(input, jsonT)
        val display = jsonInspectorResultDisplay(
            pathResult = session.pathResult,
            notice = session.notice,
            status = status,
            pathAppliedNotice = pathApplied,
            jsonPathPanelTitle = pathTitle,
        )
        val entry = JsonEngine.listPaths(input, jsonT).first { it.path == path }
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(colors.workspace)
                        .padding(12.dp)
                        .width(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MooCard(Modifier.fillMaxWidth()) {
                        Text(zh.t("json.panel.result"), color = colors.textPrimary, fontSize = 12.sp)
                        Text(
                            display.text,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    MooCard(Modifier.fillMaxWidth()) {
                        Text(zh.t("json.panel.pathTree"), color = colors.textPrimary, fontSize = 12.sp)
                        JsonPathListRow(
                            entry = entry,
                            selected = true,
                            showInlinePreview = true,
                            inlinePreviewText = "One",
                            onTap = {},
                        )
                    }
                }
            }
        }
        waitForIdle()
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val file = File(dir, "147-compose-json-inspector-inline-path-query.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(display.text.contains("One"), display.text)
        assertTrue(file.length() > 2_000, "png should have content")
    }
}
