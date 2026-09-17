package com.rememberber.mootool.next.compose.features.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.toHttpProxyConfig
import com.rememberber.mootool.next.compose.domain.TranslationAutoPresentation
import com.rememberber.mootool.next.compose.domain.TranslationEngine
import com.rememberber.mootool.next.compose.domain.TranslationHistoryRestore
import com.rememberber.mootool.next.compose.domain.TranslationErrorCode
import com.rememberber.mootool.next.compose.domain.TranslationInput
import com.rememberber.mootool.next.compose.domain.TranslationProvider
import com.rememberber.mootool.next.compose.domain.TranslationTab
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.TranslationSession
import com.rememberber.mootool.next.compose.storage.TranslationHistoryItem
import com.rememberber.mootool.next.compose.storage.TranslationWord
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooCompactSearch
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooTranslationHistoryArticle
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.OnToolLeaveUnlessDetached
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun TranslationScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.translationSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Translation) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var words by remember { mutableStateOf(emptyList<TranslationWord>()) }
    var historyItems by remember { mutableStateOf(emptyList<TranslationHistoryItem>()) }
    val sourceLang = settings.tools.translationSourceLang
    val targetLang = settings.tools.translationTargetLang
    val provider = TranslationEngine.parseProvider(settings.tools.translationProvider)
    val timeoutMs = TranslationEngine.clampTimeout(settings.network.translationTimeoutMs)

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistTranslation()
    }

    fun reloadWords() {
        words = container.translations.listWords(session.wordQuery)
    }

    fun reloadHistory() {
        historyItems = container.translations.listHistory(session.historyQuery)
    }

    fun cancelActive() {
        val active = session.requestId
        session.requestId = ""
        session.translating = false
        if (active.isNotBlank()) TranslationEngine.cancel(active)
    }

    fun send(text: String, expectedSeq: Int) {
        if (expectedSeq != session.sequence) return
        if (text.isEmpty()) {
            session.target = ""
            session.providerUsed = ""
            session.fallbackUsed = false
            session.translating = false
            persist()
            return
        }
        val requestId = "translation-${UUID.randomUUID()}"
        session.requestId = requestId
        session.translating = true
        session.error = ""
        persist()
        val proxy = settings.network.toHttpProxyConfig()
        val input = TranslationInput(
            requestId = requestId,
            text = text,
            sourceLang = sourceLang,
            targetLang = targetLang,
            preferredProvider = provider,
            timeoutMs = timeoutMs
        )
        scope.launch(Dispatchers.IO) {
            val result = TranslationEngine.translate(input, proxy)
            withContext(Dispatchers.Main) {
                if (session.sequence != expectedSeq || session.requestId != requestId) return@withContext
                session.translating = false
                session.requestId = ""
                if (result.ok) {
                    session.target = result.text
                    session.providerUsed = result.provider
                    session.fallbackUsed = result.fallbackUsed
                    session.notice = buildNotice(container, result.provider, result.fallbackUsed)
                    session.error = ""
                    container.translations.saveHistory(text, result.text, sourceLang, targetLang, result.provider)
                } else if (result.errorCode != TranslationErrorCode.ABORTED) {
                    session.error = messageFor(container, result.errorCode, result.statusText)
                    session.notice = ""
                }
                persist()
            }
        }
    }

    fun translateNow(text: String = session.source) {
        session.sequence += 1
        cancelActive()
        send(text, session.sequence)
    }

    fun prepareForRetranslation() {
        session.restoredSource = null
        session.sequence += 1
        cancelActive()
        persist()
    }

    OnToolLeaveUnlessDetached(container, ToolId.Translation) {
        session.sequence += 1
        cancelActive()
    }

    LaunchedEffect(session.source, session.autoEnabled, sourceLang, targetLang, provider) {
        val seq = ++session.sequence
        val previous = session.requestId
        if (previous.isNotBlank()) TranslationEngine.cancel(previous)
        session.requestId = ""
        if (session.source.isEmpty()) {
            session.target = ""
            session.providerUsed = ""
            session.fallbackUsed = false
            session.translating = false
            persist()
            return@LaunchedEffect
        }
        if (TranslationAutoPresentation.skipAutoTranslate(session.restoredSource, session.source, session.autoEnabled)) {
            return@LaunchedEffect
        }
        delay(TranslationAutoPresentation.AUTO_DEBOUNCE_MS)
        if (seq != session.sequence) return@LaunchedEffect
        send(session.source, seq)
    }

    LaunchedEffect(session.wordQuery, session.tab, revision) {
        if (session.tab == TranslationTab.Words) reloadWords()
    }
    LaunchedEffect(session.historyQuery, session.tab, revision) {
        if (session.tab == TranslationTab.History) reloadHistory()
    }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        reloadWords()
        reloadHistory()
        if (session.selectedWordId.isNotBlank()) {
            val ids = container.translations.listWords(session.wordQuery).map { it.id }
            if (session.selectedWordId !in ids) session.selectedWordId = ""
        }
        persist()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("translation.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (session.error.isNotEmpty()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            else if (session.notice.isNotEmpty()) Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Translation) })
                }
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .mooToolShell(p5 = true, endBorder = false)
        ) {
        MooToolTabsRow {
            TranslationTab.entries.forEach { tab ->
                MooToolTab(container.t(tabKey(tab)), selected = session.tab == tab, onClick = {
                    session.tab = tab
                    persist()
                })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        when (session.tab) {
            TranslationTab.Translate -> TranslatePane(
                container = container,
                session = session,
                overflow = overflow,
                sourceLang = sourceLang,
                targetLang = targetLang,
                provider = provider,
                onSourceLang = { value ->
                    prepareForRetranslation()
                    val next = TranslationEngine.nextSourceLanguages(value, targetLang)
                    container.updateSettings { current ->
                        current.copy(tools = current.tools.copy(translationSourceLang = next.first, translationTargetLang = next.second))
                    }
                },
                onTargetLang = { value ->
                    prepareForRetranslation()
                    val next = TranslationEngine.nextTargetLanguage(value, sourceLang)
                    container.updateSettings { current ->
                        current.copy(tools = current.tools.copy(translationTargetLang = next))
                    }
                },
                onProvider = { value ->
                    prepareForRetranslation()
                    container.updateSettings { current ->
                        current.copy(tools = current.tools.copy(translationProvider = value.name.lowercase()))
                    }
                },
                onSource = { value ->
                    prepareForRetranslation()
                    applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                        session.source = value.take(TranslationEngine.MAX_TEXT_UNITS)
                    }
                    persist()
                },
                onExchange = {
                    val next = TranslationEngine.exchangedLanguages(sourceLang, targetLang)
                    session.sequence += 1
                    cancelActive()
                    container.updateSettings { current ->
                        current.copy(tools = current.tools.copy(translationSourceLang = next.first, translationTargetLang = next.second))
                    }
                    val previousSource = session.source
                    session.restoredSource = null
                    session.source = session.target
                    session.target = previousSource
                    persist()
                },
                onCopy = {
                    if (session.target.isNotEmpty()) {
                        if (container.copyText(session.target)) {
                            session.notice = container.t("common.copied")
                        } else {
                            session.notice = container.t("json.notice.copyFailed")
                        }
                        persist()
                    }
                },
                onSaveWord = {
                    if (session.source.isBlank()) return@TranslatePane
                    runCatching {
                        container.translations.saveWord(null, session.source, session.target, sourceLang, targetLang, "")
                        session.notice = container.t("translation.savedWord")
                        container.toastSuccess(container.t("translation.savedWord"))
                    }.onFailure { session.error = it.message ?: container.t("translation.error.generic") }
                    persist()
                },
                onClear = {
                    prepareForRetranslation()
                    session.source = ""
                    session.target = ""
                    session.providerUsed = ""
                    session.fallbackUsed = false
                    session.notice = ""
                    persist()
                },
                onManual = { translateNow() },
                onChanged = { persist() }
            )
            TranslationTab.Words -> WordBookPane(
                container = container,
                session = session,
                words = words,
                onReload = { reloadWords(); persist() },
                onApply = { word ->
                    session.sequence += 1
                    cancelActive()
                    session.restoredSource = word.sourceText
                    session.source = word.sourceText
                    session.target = word.targetText
                    session.tab = TranslationTab.Translate
                    container.updateSettings { current ->
                        current.copy(tools = current.tools.copy(translationSourceLang = word.sourceLang, translationTargetLang = word.targetLang))
                    }
                    persist()
                },
                onRetranslate = { word ->
                    val requestId = "word-${UUID.randomUUID()}"
                    val proxy = settings.network.toHttpProxyConfig()
                    scope.launch(Dispatchers.IO) {
                        val result = TranslationEngine.translate(
                            TranslationInput(requestId, word.sourceText, word.sourceLang, word.targetLang, provider, timeoutMs),
                            proxy
                        )
                        withContext(Dispatchers.Main) {
                            if (!result.ok) {
                                if (result.errorCode != TranslationErrorCode.ABORTED) {
                                    session.error = messageFor(container, result.errorCode, result.statusText)
                                    persist()
                                }
                                return@withContext
                            }
                            val saved = container.translations.saveWord(
                                word.id, word.sourceText, result.text, word.sourceLang, word.targetLang, word.remark
                            )
                            container.translations.saveHistory(word.sourceText, result.text, word.sourceLang, word.targetLang, result.provider)
                            session.selectedWordId = saved.id
                            session.wordTarget = saved.targetText
                            reloadWords()
                            persist()
                        }
                    }
                }
            )
            TranslationTab.History -> HistoryPane(
                container = container,
                session = session,
                items = historyItems,
                onReload = { reloadHistory(); persist() },
                onApply = { item ->
                    cancelActive()
                    TranslationHistoryRestore.applyToSession(session, item)
                    session.tab = TranslationTab.Translate
                    container.updateSettings { current ->
                        TranslationHistoryRestore.patchSettingsProvider(
                            TranslationHistoryRestore.patchSettingsLanguages(current, item),
                            item,
                        )
                    }
                    persist()
                }
            )
        }
        }
        }
    }
    }
}

private const val TRANSLATION_EDITOR_PANE_KEY = "translation-editor"

@Composable
private fun TranslatePane(
    container: AppContainer,
    session: TranslationSession,
    overflow: Boolean,
    sourceLang: String,
    targetLang: String,
    provider: TranslationProvider,
    onSourceLang: (String) -> Unit,
    onTargetLang: (String) -> Unit,
    onProvider: (TranslationProvider) -> Unit,
    onSource: (String) -> Unit,
    onExchange: () -> Unit,
    onCopy: () -> Unit,
    onSaveWord: () -> Unit,
    onClear: () -> Unit,
    onManual: () -> Unit,
    onChanged: () -> Unit
) {
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    var providerOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).mooToolbarBackground()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(container.t("translation.sourceLanguage"), color = colors.textMuted, fontSize = 10.sp)
            MooButton(
                container.t("translation.lang.$sourceLang"),
                onClick = { session.languagePicker = "source"; onChanged() },
                modifier = Modifier.widthIn(min = 130.dp),
                p5Toolbar = true
            )
            MooButton(container.t("translation.exchange"), onClick = onExchange, p5Toolbar = true)
            Text(container.t("translation.targetLanguage"), color = colors.textMuted, fontSize = 10.sp)
            MooButton(
                container.t("translation.lang.$targetLang"),
                onClick = { session.languagePicker = "target"; onChanged() },
                modifier = Modifier.widthIn(min = 130.dp),
                p5Toolbar = true
            )
            Spacer(Modifier.weight(1f))
            Box {
                MooButton(
                    if (provider == TranslationProvider.Bing) "Bing" else "Google",
                    onClick = { providerOpen = true },
                    modifier = Modifier.widthIn(min = 130.dp),
                    p5Toolbar = true
                )
                MooMenu(expanded = providerOpen, onDismissRequest = { providerOpen = false }) {
                    MooMenuItem(onClick = {
                        providerOpen = false
                        onProvider(TranslationProvider.Google)
                    }) { Text("Google") }
                    MooMenuItem(onClick = {
                        providerOpen = false
                        onProvider(TranslationProvider.Bing)
                    }) { Text("Bing") }
                }
            }
            MooButton(
                container.t("translation.auto"),
                primary = session.autoEnabled,
                onClick = {
                    session.autoEnabled = !session.autoEnabled
                    onChanged()
                },
                p5Toolbar = true
            )
            MooButton(container.t("translation.now"), prominent = true, onClick = onManual, p5Toolbar = true)
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = listOf(
                    OverflowAction(container.t("translation.copy"), enabled = session.target.isNotEmpty(), onClick = onCopy),
                    OverflowAction(container.t("translation.saveWord"), enabled = session.source.isNotBlank(), onClick = onSaveWord),
                    OverflowAction(container.t("common.action.clear"), onClick = onClear)
                )
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val minPane = 280f
            val paneHandle = 10f
            val maxSource = (maxWidth.value - paneHandle - minPane).coerceAtLeast(minPane)
            val defaultSource = (maxWidth.value * 0.5f).coerceIn(minPane, maxSource)
            val sourceWidth = settings.layout.pane(TRANSLATION_EDITOR_PANE_KEY, 0, defaultSource, minPane, maxSource)
            Row(Modifier.fillMaxSize()) {
            MooTextField(
                session.source,
                onSource,
                modifier = Modifier.width(sourceWidth.dp).widthIn(min = 280.dp).fillMaxHeight(),
                placeholder = container.t("translation.sourcePlaceholder"),
                singleLine = false,
                borderless = true
            )
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(TRANSLATION_EDITOR_PANE_KEY, 0, sourceWidth + it, 1) },
                onReset = { container.setPaneSize(TRANSLATION_EDITOR_PANE_KEY, 0, defaultSource, 1) }
            )
            Column(Modifier.weight(1f).widthIn(min = 280.dp).fillMaxHeight()) {
                SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
                    MooTextField(
                        if (session.translating) container.t("translation.translating") else session.target,
                        {},
                        modifier = Modifier.fillMaxSize(),
                        placeholder = container.t("translation.targetPlaceholder"),
                        singleLine = false,
                        borderless = true
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                Row(
                    Modifier.fillMaxWidth().height(34.dp).mooToolbarBackground().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val providerLabel = when (session.providerUsed) {
                        "google" -> "Google"
                        "bing" -> "Bing"
                        else -> ""
                    }
                    Text(
                        if (providerLabel.isEmpty()) "" else providerLabel + if (session.fallbackUsed) " · ${container.t("translation.fallback")}" else "",
                        color = colors.textMuted,
                        fontSize = 10.sp
                    )
                    Text("${session.source.length} / ${TranslationEngine.MAX_TEXT_UNITS}", color = colors.textMuted, fontSize = 10.sp)
                }
            }
            }
        }
    }
    if (session.languagePicker.isNotEmpty()) {
        val forSource = session.languagePicker == "source"
        MooOverlay(onDismiss = { session.languagePicker = ""; onChanged() }) {
            Column(
                Modifier.width(360.dp).height(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    container.t(if (forSource) "translation.sourceLanguage" else "translation.targetLanguage"),
                    color = colors.textPrimary
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(TranslationEngine.LANGUAGE_CODES.filter { forSource || it != "auto" }) { code ->
                        Text(
                            container.t("translation.lang.$code"),
                            color = colors.textPrimary,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .background(if (code == if (forSource) sourceLang else targetLang) colors.selected else colors.workspace)
                                .mooFocusClickable(shape = RoundedCornerShape(6.dp)) {
                                    if (forSource) onSourceLang(code) else onTargetLang(code)
                                    session.languagePicker = ""
                                    onChanged()
                                }
                                .padding(8.dp)
                        )
                    }
                }
                MooButton(container.t("common.close"), onClick = { session.languagePicker = ""; onChanged() })
            }
        }
    }
}

@Composable
private fun WordBookPane(
    container: AppContainer,
    session: TranslationSession,
    words: List<TranslationWord>,
    onReload: () -> Unit,
    onApply: (TranslationWord) -> Unit,
    onRetranslate: (TranslationWord) -> Unit
) {
    val colors = MooTheme.colors
    Row(Modifier.fillMaxSize()) {
        val listWidth = container.settings.value.layout.pane(ToolId.Translation.id, 0, 220f, 180f, 320f)
        Column(
            Modifier.width(listWidth.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MooCompactSearch(session.wordQuery, { session.wordQuery = it; onReload() }, placeholder = container.t("translation.searchWords"))
            if (words.isEmpty()) {
                Text(container.t("translation.wordEmpty"), color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(words, key = { it.id }) { word ->
                        val interaction = remember(word.id) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        val active = word.id == session.selectedWordId
                        val shape = RoundedCornerShape(5.dp)
                        Column(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (active || hovered) colors.control else Color.Transparent)
                                .hoverable(interaction)
                                .mooFocusClickable(shape = shape) {
                                    session.selectedWordId = word.id
                                    session.wordSource = word.sourceText
                                    session.wordTarget = word.targetText
                                    session.wordRemark = word.remark
                                    session.wordSourceLang = word.sourceLang
                                    session.wordTargetLang = word.targetLang
                                    onReload()
                                }
                                .padding(8.dp)
                        ) {
                            Text(word.sourceText.ifBlank { "—" }, color = colors.textBody, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(word.targetText, color = colors.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MooButton(container.t("common.new"), onClick = {
                    session.selectedWordId = ""
                    session.wordSource = ""
                    session.wordTarget = ""
                    session.wordRemark = ""
                    session.wordSourceLang = "auto"
                    session.wordTargetLang = "zh-CN"
                    onReload()
                })
                MooButton(
                    container.t("common.delete"),
                    enabled = session.selectedWordId.isNotBlank(),
                    onClick = { session.deleteWordConfirm = true; onReload() }
                )
            }
        }
        VerticalPaneHandle(
            onDelta = { container.setPaneSize(ToolId.Translation.id, 0, listWidth + it, 1) },
            onReset = { container.setPaneSize(ToolId.Translation.id, 0, 220f, 1) }
        )
        Column(
            Modifier.weight(1f).fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${container.t("translation.lang.${session.wordSourceLang}")} → ${container.t("translation.lang.${session.wordTargetLang}")}",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
                val current = words.firstOrNull { it.id == session.selectedWordId }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    MooButton(container.t("translation.apply"), enabled = current != null, onClick = { current?.let(onApply) })
                    MooButton(container.t("translation.retranslate"), enabled = current != null && current.sourceText.isNotBlank(), onClick = { current?.let(onRetranslate) })
                }
            }
                MooTextField(
                    session.wordSource,
                    { session.wordSource = it; onReload() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = container.t("translation.sourcePlaceholder"),
                    singleLine = false
                )
                MooTextField(
                    session.wordTarget,
                    { session.wordTarget = it; onReload() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = container.t("translation.targetPlaceholder"),
                    singleLine = false
                )
                MooTextField(session.wordRemark, { session.wordRemark = it; onReload() }, placeholder = container.t("translation.remark"), dense = true)
                MooButton(container.t("common.save"), prominent = true, onClick = {
                    runCatching {
                        val saved = container.translations.saveWord(
                            session.selectedWordId.ifBlank { null },
                            session.wordSource,
                            session.wordTarget,
                            session.wordSourceLang,
                            session.wordTargetLang,
                            session.wordRemark
                        )
                        session.selectedWordId = saved.id
                        session.notice = container.t("common.save")
                        container.toastSuccess(container.t("common.save"))
                    }.onFailure { session.error = it.message ?: container.t("translation.error.generic") }
                    onReload()
                }, modifier = Modifier.align(Alignment.End))
        }
    }
    if (session.deleteWordConfirm) {
        MooOverlay(onDismiss = { session.deleteWordConfirm = false; onReload() }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t("translation.confirmDeleteWord"), color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.delete"), danger = true, onClick = {
                        if (session.selectedWordId.isNotBlank()) container.translations.deleteWord(session.selectedWordId)
                        session.selectedWordId = ""
                        session.wordSource = ""
                        session.wordTarget = ""
                        session.wordRemark = ""
                        session.deleteWordConfirm = false
                        onReload()
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.deleteWordConfirm = false; onReload() })
                }
            }
        }
    }
}

@Composable
private fun HistoryPane(
    container: AppContainer,
    session: TranslationSession,
    items: List<TranslationHistoryItem>,
    onReload: () -> Unit,
    onApply: (TranslationHistoryItem) -> Unit
) {
    val colors = MooTheme.colors
    val stamp = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()) }
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MooCompactSearch(
                session.historyQuery,
                { session.historyQuery = it; onReload() },
                modifier = Modifier.weight(1f),
                placeholder = container.t("translation.searchHistory")
            )
            MooButton(
                container.t("translation.clearHistory"),
                enabled = items.isNotEmpty(),
                onClick = { session.clearHistoryConfirm = true; onReload() }
            )
        }
        if (items.isEmpty()) {
            Text(container.t("translation.historyEmpty"), color = colors.textMuted, fontSize = 11.sp)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items, key = { it.id }) { item ->
                    val interaction = remember(item.id) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .mooTranslationHistoryArticle(hovered = hovered)
                            .hoverable(interaction)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f).mooFocusClickable { onApply(item) }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "${container.t("translation.lang.${item.sourceLang}")} → ${container.t("translation.lang.${item.targetLang}")}",
                                    color = colors.textBody,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    stamp.format(Instant.ofEpochMilli(item.createdAt)),
                                    color = colors.textMuted,
                                    fontSize = 9.sp
                                )
                            }
                            Text(item.sourceText, color = colors.textMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.targetText, color = colors.textMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        MooButton(container.t("common.delete"), onClick = {
                            container.translations.deleteHistory(item.id)
                            onReload()
                        })
                    }
                }
            }
        }
    }
    if (session.clearHistoryConfirm) {
        MooOverlay(onDismiss = { session.clearHistoryConfirm = false; onReload() }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(container.t("translation.confirmClearHistory"), color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("translation.clearHistory"), prominent = true, onClick = {
                        container.translations.clearHistory()
                        session.clearHistoryConfirm = false
                        onReload()
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.clearHistoryConfirm = false; onReload() })
                }
            }
        }
    }
}

private fun tabKey(tab: TranslationTab): String = when (tab) {
    TranslationTab.Translate -> "translation.tab.translate"
    TranslationTab.Words -> "translation.tab.words"
    TranslationTab.History -> "translation.tab.history"
}

private fun buildNotice(container: AppContainer, provider: String, fallback: Boolean): String {
    val name = if (provider == "bing") "Bing" else "Google"
    return if (fallback) "$name · ${container.t("translation.fallback")}" else name
}

private fun messageFor(container: AppContainer, code: TranslationErrorCode?, fallback: String): String {
    if (code == null) return fallback.ifBlank { container.t("translation.error.generic") }
    val key = "translation.error.${code.name}"
    val localized = container.t(key)
    return if (localized == key) fallback.ifBlank { container.t("translation.error.generic") } else localized
}
