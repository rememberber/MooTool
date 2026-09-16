package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.model.AppSettings

@Composable
fun IoThreePaneRow(
    container: AppContainer,
    settings: AppSettings,
    paneKey: String,
    middleRatio: Float,
    minRight: Float,
    modifier: Modifier,
    left: @Composable () -> Unit,
    middle: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    BoxWithConstraints(modifier) {
        val innerWidth = maxWidth.value
        val minLeft = 240f
        val minMiddle = 110f
        val paneHandle = 10f
        val ratioSum = 1f + middleRatio + 1f
        val defaultLeft = (innerWidth * (1f / ratioSum)).coerceIn(minLeft, innerWidth)
        val defaultMiddle = (innerWidth * (middleRatio / ratioSum)).coerceIn(minMiddle, 280f)
        val maxLeft = (innerWidth - 2f * paneHandle - minMiddle - minRight).coerceAtLeast(minLeft)
        val leftWidth = settings.layout.pane(paneKey, 0, defaultLeft.coerceIn(minLeft, maxLeft), minLeft, maxLeft)
        val maxMiddle = (innerWidth - 2f * paneHandle - leftWidth - minRight).coerceAtLeast(minMiddle)
        val middleWidth = settings.layout.pane(paneKey, 1, defaultMiddle.coerceIn(minMiddle, maxMiddle), minMiddle, maxMiddle)
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(leftWidth.dp).widthIn(min = 240.dp).fillMaxHeight()) { left() }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(paneKey, 0, leftWidth + it, 2) },
                onReset = { container.setPaneSize(paneKey, 0, defaultLeft.coerceIn(minLeft, maxLeft), 2) }
            )
            Column(Modifier.width(middleWidth.dp).widthIn(min = 110.dp).fillMaxHeight()) { middle() }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(paneKey, 1, middleWidth + it, 2) },
                onReset = { container.setPaneSize(paneKey, 1, defaultMiddle.coerceIn(minMiddle, maxMiddle), 2) }
            )
            Column(Modifier.weight(1f).widthIn(min = minRight.dp).fillMaxHeight()) { right() }
        }
    }
}

@Composable
fun IoTwoPaneRow(
    container: AppContainer,
    settings: AppSettings,
    paneKey: String,
    minLeft: Float,
    minRight: Float,
    defaultLeftFraction: Float,
    modifier: Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    BoxWithConstraints(modifier) {
        val innerWidth = maxWidth.value
        val paneHandle = 10f
        val maxLeft = (innerWidth - paneHandle - minRight).coerceAtLeast(minLeft)
        val defaultLeft = (innerWidth * defaultLeftFraction).coerceIn(minLeft, maxLeft)
        val leftWidth = settings.layout.pane(paneKey, 0, defaultLeft, minLeft, maxLeft)
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(leftWidth.dp).widthIn(min = minLeft.dp).fillMaxHeight()) { left() }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(paneKey, 0, leftWidth + it, 1) },
                onReset = { container.setPaneSize(paneKey, 0, defaultLeft, 1) }
            )
            Column(Modifier.weight(1f).widthIn(min = minRight.dp).fillMaxHeight()) { right() }
        }
    }
}
