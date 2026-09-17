package com.rememberber.mootool.next.compose.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.FindReplaceOptions
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.mooFindBarBackground
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import javax.swing.SwingUtilities

@Composable
internal fun EditorFindOnlyBar(
    container: AppContainer,
    editor: EditorBuffer,
    findQuery: String,
    onFindQueryChange: (String) -> Unit,
    findOptions: FindReplaceOptions,
    onFindOptionsChange: (FindReplaceOptions) -> Unit,
    onClose: () -> Unit,
    onChanged: () -> Unit,
    placeholderKey: String = "json.find.placeholder",
) {
    val matches = FindReplace.findAll(editor.text, findQuery, findOptions)
    val findFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        findFocus.requestFocus()
    }
    fun step(forward: Boolean) {
        if (findQuery.isBlank()) return
        onEdt {
            val match = RstaFindNavigation.jump(editor, findQuery, findOptions, forward)
            if (match == null) {
                container.toastFindNoMatches()
            }
            onChanged()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mooFindBarBackground()
            .onFindBarRowKeys(
                onPrevious = { step(false) },
                onNext = { step(true) },
                onClose = onClose,
            )
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MooTextField(
            findQuery,
            onFindQueryChange,
            modifier = Modifier.width(220.dp),
            placeholder = container.t(placeholderKey),
            fieldModifier = Modifier
                .focusRequester(findFocus)
                .onFindQueryEnterKey { step(true) },
        )
        MooButton(
            container.t("find.find"),
            enabled = findQuery.isNotBlank(),
            onClick = { step(true) },
        )
        MooButton(container.t("find.matchCase") + ": ${findOptions.matchCase}", onClick = {
            onFindOptionsChange(findOptions.copy(matchCase = !findOptions.matchCase))
            onChanged()
        })
        MooButton(container.t("find.wholeWord") + ": ${findOptions.wholeWord}", onClick = {
            onFindOptionsChange(findOptions.copy(wholeWord = !findOptions.wholeWord))
            onChanged()
        })
        MooButton(container.t("find.regex") + ": ${findOptions.regex}", onClick = {
            onFindOptionsChange(findOptions.copy(regex = !findOptions.regex))
            onChanged()
        })
        Text(
            "${container.t("find.foundPrefix")} ${matches.size}",
            color = MooTheme.colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("find.previous"), onClick = { step(false) })
        MooButton(container.t("find.next"), onClick = { step(true) })
        MooButton(container.t("common.action.close"), onClick = onClose)
    }
}

private fun onEdt(block: () -> Unit) {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}
