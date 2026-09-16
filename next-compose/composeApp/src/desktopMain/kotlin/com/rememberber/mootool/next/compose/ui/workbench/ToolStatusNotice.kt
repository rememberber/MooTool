package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.sessions.CodeRunSession
import com.rememberber.mootool.next.compose.sessions.ColorSession
import com.rememberber.mootool.next.compose.sessions.CryptoSession
import com.rememberber.mootool.next.compose.sessions.HttpSession
import com.rememberber.mootool.next.compose.sessions.ImageSession
import com.rememberber.mootool.next.compose.sessions.MessageBoardSession
import com.rememberber.mootool.next.compose.sessions.PdfSession
import com.rememberber.mootool.next.compose.sessions.ProtobufSession
import com.rememberber.mootool.next.compose.sessions.TimeSession
import com.rememberber.mootool.next.compose.sessions.VariablesSession

/**
 * 用户编辑主输入时清空状态栏 [notice]（对齐 JSON DIFF-227 / Electron 编辑 onChange 清空提示）。
 */
internal fun applyUserEditClearingStatusNotice(
    notice: () -> String,
    setNotice: (String) -> Unit,
    edit: () -> Unit,
) {
    edit()
    if (notice().isNotEmpty()) setNotice("")
}

internal fun CryptoSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun ProtobufSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun HttpSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun TimeSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun ColorSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun VariablesSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun PdfSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun MessageBoardSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

internal fun ImageSession.onUserInput(edit: () -> Unit) =
    applyUserEditClearingStatusNotice({ notice }, { notice = it }, edit)

/** 代码运行无状态栏 notice，用户改源码时清除上次运行/校验错误提示。 */
internal fun CodeRunSession.clearErrorOnUserEdit(edit: () -> Unit) {
    edit()
    if (error.isNotEmpty()) error = ""
}
