package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultEntry
import java.time.Instant

object VaultSort {
    const val NAME = "name"
    const val MODIFIED = "modified"
    const val CREATED = "created"

    fun normalize(sort: String, allowCreated: Boolean): String = when (sort.trim().lowercase()) {
        MODIFIED -> MODIFIED
        CREATED -> if (allowCreated) CREATED else NAME
        else -> NAME
    }

    fun compare(left: VaultEntry, right: VaultEntry, sort: String): Int {
        if (left.directory != right.directory) return if (left.directory) -1 else 1
        if (left.directory || sort == NAME) return left.name.compareTo(right.name)
        val leftStamp = if (sort == CREATED) left.createdAt else left.modifiedAt
        val rightStamp = if (sort == CREATED) right.createdAt else right.modifiedAt
        val byTime = compareNewestFirst(leftStamp, rightStamp)
        return if (byTime != 0) byTime else left.name.compareTo(right.name)
    }

    private fun compareNewestFirst(left: String, right: String): Int {
        val leftTime = parseInstant(left)
        val rightTime = parseInstant(right)
        return when {
            leftTime != null && rightTime != null -> rightTime.compareTo(leftTime)
            leftTime == null && rightTime == null -> right.compareTo(left)
            leftTime == null -> 1
            else -> -1
        }
    }

    private fun parseInstant(value: String): Instant? =
        value.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
