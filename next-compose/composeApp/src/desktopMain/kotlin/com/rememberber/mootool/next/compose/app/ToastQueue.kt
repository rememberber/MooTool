package com.rememberber.mootool.next.compose.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class ToastTone { Info, Success, Error }

data class ToastItem(
    val id: Long,
    val message: String,
    val tone: ToastTone,
    val durationMs: Long = 3200
)

class ToastQueue(private val limit: Int = 4) {
    private val _items = MutableStateFlow<List<ToastItem>>(emptyList())
    val items: StateFlow<List<ToastItem>> = _items
    private var nextId = 1L

    fun show(message: String, tone: ToastTone = ToastTone.Info, durationMs: Long = 3200): Long {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return -1L
        val id = nextId++
        _items.update { current ->
            (current + ToastItem(id, trimmed, tone, durationMs)).takeLast(limit)
        }
        return id
    }

    fun success(message: String, durationMs: Long = 3200) = show(message, ToastTone.Success, durationMs)

    fun error(message: String, durationMs: Long = 3200) = show(message, ToastTone.Error, durationMs)

    fun info(message: String, durationMs: Long = 3200) = show(message, ToastTone.Info, durationMs)

    fun dismiss(id: Long) {
        _items.update { it.filterNot { item -> item.id == id } }
    }
}
