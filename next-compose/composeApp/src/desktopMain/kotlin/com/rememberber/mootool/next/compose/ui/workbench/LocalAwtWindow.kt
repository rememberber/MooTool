package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.runtime.compositionLocalOf
import java.awt.Window

/** 当前 Compose `Window` 对应的 AWT 窗口；主窗与分离工具窗在根部注入。 */
val LocalAwtWindow = compositionLocalOf<Window?> { null }
