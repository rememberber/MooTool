package com.rememberber.mootool.next.compose.features.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.ui.components.loadClasspathImage
import com.rememberber.mootool.next.compose.ui.components.mooFocusOutline
import com.rememberber.mootool.next.compose.ui.components.mooWorkspaceBackground
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

data class HomeContributor(val name: String, val url: String)

data class HomeWork(val title: String, val descKey: String, val url: String)

val HomeContributors = listOf(
    HomeContributor("CassianFlorin", "https://github.com/CassianFlorin"),
    HomeContributor("felixcn", "https://github.com/felixcn"),
    HomeContributor("felixnan168", "https://gitee.com/felixnan168"),
    HomeContributor("Lyp", "https://gitee.com/L1yp"),
    HomeContributor("sunsence", "https://github.com/sunsence"),
    HomeContributor("rememberber", "https://github.com/rememberber")
)

val HomeWorks = listOf(
    HomeWork("WePush", "home.wepush.desc", "https://github.com/rememberber/WePush"),
    HomeWork("MooInfo", "home.mooinfo.desc", "https://github.com/rememberber/MooInfo")
)

@Composable
fun HomeScreen(container: AppContainer) {
    val colors = MooTheme.colors
    val scroll = rememberScrollState()
    val logo = remember { loadClasspathImage("brand/mootool-logo.png") }
    val sponsor = remember { loadClasspathImage("brand/wx-zanshang.jpg") }
    Box(Modifier.fillMaxSize().mooWorkspaceBackground()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 940.dp)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 36.dp, end = 36.dp, top = 12.dp, bottom = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                if (logo != null) {
                    HomeLink(container.t("home.website"), onClick = { container.openExternal("https://www.luoboduner.com/") }) {
                        Image(logo, contentDescription = null, modifier = Modifier.size(104.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    HomeLink("mootool.luoboduner.com", onClick = { container.openExternal("https://mootool.luoboduner.com/") }) {
                        Text(
                            "mootool.luoboduner.com",
                            color = colors.accentAction,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 5.dp)
                    ) {
                        Text(ProductIdentity.DISPLAY_NAME, color = colors.textStrong, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp)
                        Text("v${ProductIdentity.VERSION}", color = colors.textMuted, fontSize = 12.sp)
                    }
                    Text(container.t("home.tagline"), color = colors.textBody, fontSize = 15.sp, modifier = Modifier.padding(top = 5.dp))
                    Text(container.t("home.author"), color = colors.textMuted, fontSize = 12.sp)
                }
            }
            Section(container.t("home.about.title")) {
                listOf("home.about.line1", "home.about.lineDaily", "home.about.line2", "home.about.line2Note", "home.about.line3", "home.about.line4", "home.about.line5").forEach {
                    Text(container.t(it), color = colors.textBody, fontSize = 14.sp, lineHeight = 23.sp)
                }
            }
            Section(container.t("home.contributors.title")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeContributors.forEach { person ->
                        ContributorChip(person.name, onClick = { container.openExternal(person.url) })
                    }
                }
                Text(container.t("home.contributors.thanks"), color = colors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
            }
            Section(container.t("home.sponsor.title")) {
                Text(container.t("home.sponsor.prompt"), color = colors.textBody, fontSize = 14.sp, lineHeight = 22.sp)
                if (sponsor != null) {
                    Image(
                        sponsor,
                        contentDescription = container.t("home.sponsor.tip"),
                        modifier = Modifier.size(139.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
                Text(container.t("home.sponsor.tip"), color = colors.textMuted, fontSize = 12.sp)
            }
            Section(container.t("home.source.title")) {
                Link(container, "GitHub", "https://github.com/rememberber/MooTool")
                Link(container, "Gitee", "https://gitee.com/zhoubochina/MooTool")
            }
            Section(container.t("home.help.title")) {
                Link(container, container.t("home.help.issue"), "https://github.com/rememberber/MooTool/issues")
            }
            Section(container.t("home.otherWorks.title")) {
                HomeWorks.forEachIndexed { index, work ->
                    WorkRow(
                        title = work.title,
                        description = container.t(work.descKey),
                        last = index == HomeWorks.lastIndex,
                        onClick = { container.openExternal(work.url) }
                    )
                }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MooTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
        Text(
            title.uppercase(),
            color = colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun ContributorChip(name: String, onClick: () -> Unit) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    HomeLink(name, onClick = onClick, interaction = interaction) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.defaultMinSize(minHeight = 30.dp).hoverable(interaction)
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(colors.accentAction),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), color = colors.onAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(name, color = if (hovered) colors.accentAction else colors.textBody, fontSize = 13.sp)
        }
    }
}

@Composable
private fun WorkRow(title: String, description: String, last: Boolean, onClick: () -> Unit) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    HomeLink(title, onClick = onClick, interaction = interaction) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .hoverable(interaction)
                .then(if (last) Modifier else Modifier.padding(bottom = 0.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(title, color = if (hovered) colors.accentAction else colors.textBody, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = colors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
    if (!last) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
    }
}

@Composable
private fun HomeLink(
    label: String,
    onClick: () -> Unit,
    interaction: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val source = interaction ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .focusable(true, source)
            .semantics { role = Role.Button; contentDescription = label }
    ) {
        content()
    }
}

@Composable
private fun Link(container: AppContainer, label: String, url: String) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    HomeLink(label, onClick = { container.openExternal(url) }, interaction = interaction) {
        Text(
            label,
            color = if (hovered) colors.accentAction else colors.textBody,
            fontSize = 14.sp,
            modifier = Modifier.defaultMinSize(minHeight = 30.dp).padding(vertical = 6.dp).hoverable(interaction)
        )
    }
}
