package com.rememberber.mootool.next.compose.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.ui.components.loadClasspathImage
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun HomeScreen(container: AppContainer) {
    val colors = MooTheme.colors
    val scroll = rememberScrollState()
    val logo = remember { loadClasspathImage("brand/mootool-logo.png") }
    val sponsor = remember { loadClasspathImage("brand/wx-zanshang.jpg") }
    Box(Modifier.fillMaxSize().background(colors.workspace)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (logo != null) {
                    Image(logo, contentDescription = container.t("home.website"), modifier = Modifier.size(104.dp).clickable {
                        container.openExternal("https://www.luoboduner.com/")
                    })
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("mootool.luoboduner.com", color = colors.accent, modifier = Modifier.clickable {
                        container.openExternal("https://mootool.luoboduner.com/")
                    })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ProductIdentity.DISPLAY_NAME, color = colors.textPrimary, fontSize = 22.sp)
                        Text("v${ProductIdentity.VERSION}", color = colors.textSecondary)
                    }
                    Text(container.t("home.tagline"), color = colors.textSecondary)
                    Text(container.t("home.author"), color = colors.textSecondary)
                }
            }
            Section(container.t("home.about.title")) {
                listOf("home.about.line1", "home.about.lineDaily", "home.about.line2", "home.about.line2Note", "home.about.line3", "home.about.line4", "home.about.line5").forEach {
                    Text(container.t(it), color = colors.textPrimary, fontSize = 13.sp)
                }
            }
            Section(container.t("home.contributors.title")) {
                val people = listOf(
                    "CassianFlorin" to "https://github.com/CassianFlorin",
                    "felixcn" to "https://github.com/felixcn",
                    "felixnan168" to "https://github.com/felixnan168",
                    "Lyp" to "https://github.com/rememberber",
                    "sunsence" to "https://github.com/sunsence",
                    "rememberber" to "https://github.com/rememberber"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    people.forEach { (name, url) ->
                        Text(name, color = colors.accent, modifier = Modifier.clickable { container.openExternal(url) }.padding(6.dp))
                    }
                }
                Text(container.t("home.contributors.thanks"), color = colors.textSecondary)
            }
            Section(container.t("home.sponsor.title")) {
                Text(container.t("home.sponsor.prompt"), color = colors.textPrimary)
                if (sponsor != null) {
                    Image(sponsor, contentDescription = container.t("home.sponsor.tip"), modifier = Modifier.heightIn(max = 220.dp))
                }
                Text(container.t("home.sponsor.tip"), color = colors.textSecondary)
            }
            Section(container.t("home.source.title")) {
                Link(container, "GitHub", "https://github.com/rememberber/MooTool")
                Link(container, "Gitee", "https://gitee.com/zhoubochina/MooTool")
            }
            Section(container.t("home.help.title")) {
                Link(container, container.t("home.help.issue"), "https://github.com/rememberber/MooTool/issues")
            }
            Section(container.t("home.otherWorks.title")) {
                Link(container, container.t("home.wepush"), "https://github.com/rememberber/WePush")
                Link(container, container.t("home.mooinfo"), "https://github.com/rememberber/MooInfo")
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = MooTheme.colors.textPrimary, fontSize = 16.sp)
        content()
    }
}

@Composable
private fun Link(container: AppContainer, label: String, url: String) {
    Text(label, color = MooTheme.colors.accent, modifier = Modifier.clickable { container.openExternal(url) }.padding(vertical = 2.dp))
}
