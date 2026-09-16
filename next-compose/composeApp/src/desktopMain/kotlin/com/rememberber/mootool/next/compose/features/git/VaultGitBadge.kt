package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.GitEngine
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

internal fun formatVaultGitBadgeCount(changeCount: Int): String? = when {
    changeCount <= 0 -> null
    changeCount > 99 -> "99+"
    else -> changeCount.toString()
}

fun gitActionMenuLabel(baseLabel: String, changeCount: Int): String {
    val badge = formatVaultGitBadgeCount(changeCount)
    return if (badge != null) "$baseLabel ($badge)" else baseLabel
}

@Composable
fun rememberVaultGitChangeCount(root: Path, refreshKey: Any = Unit): Int {
    var count by remember(root) { mutableIntStateOf(0) }
    LaunchedEffect(root, refreshKey) {
        suspend fun load() {
            count = withContext(Dispatchers.IO) {
                val status = GitEngine.status(root)
                if (status.available && status.repository) status.changes.size else 0
            }
        }
        load()
        while (isActive) {
            delay(5_000)
            load()
        }
    }
    return count
}

@Composable
fun GitActionButton(
    label: String,
    changeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    p5Toolbar: Boolean = false,
) {
    val colors = MooTheme.colors
    Box(modifier) {
        MooButton(label, onClick = onClick, p5Toolbar = p5Toolbar)
        formatVaultGitBadgeCount(changeCount)?.let { badgeText ->
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentAction)
                    .border(1.5.dp, colors.toolbar, RoundedCornerShape(8.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    badgeText,
                    color = colors.onAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

