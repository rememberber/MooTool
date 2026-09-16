package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.rememberber.mootool.next.compose.domain.SystemFonts
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun FontSelect(
    value: String,
    ariaLabel: String,
    onChange: (String) -> Unit,
    labels: Map<String, String> = emptyMap(),
    emptyLabel: String? = null,
    enabled: Boolean = true,
    searchPlaceholder: String = ""
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val fonts = remember(value) { SystemFonts.list(value) }
    val display = SystemFonts.displayName(value, labels, emptyLabel).ifBlank { ariaLabel }
    val colors = MooTheme.colors
    Box {
        MooButton(
            "$ariaLabel · $display",
            enabled = enabled,
            onClick = {
                query = ""
                open = !open
            }
        )
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 40),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier
                        .width(280.dp)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .background(colors.surfaceCard, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.borderSoft, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (emptyLabel != null) {
                        Text(
                            emptyLabel,
                            color = if (value.isBlank()) colors.accent else colors.textPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().mooFocusClickable {
                                onChange("")
                                open = false
                            }.padding(6.dp)
                        )
                    }
                    MooTextField(query, { query = it }, placeholder = searchPlaceholder.ifBlank { ariaLabel })
                    val filtered = fonts.filter { query.isBlank() || it.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.fillMaxWidth().height(240.dp)) {
                        items(filtered, key = { it }) { font ->
                            val selected = font == value
                            Text(
                                labels[font] ?: font,
                                color = if (selected) colors.accent else colors.textPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().mooFocusClickable {
                                    onChange(font)
                                    open = false
                                }.padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
