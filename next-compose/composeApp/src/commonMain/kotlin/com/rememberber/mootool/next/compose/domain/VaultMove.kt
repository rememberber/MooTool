package com.rememberber.mootool.next.compose.domain

object VaultMove {
    fun parentDirectory(relativePath: String): String {
        val index = relativePath.lastIndexOf('/')
        return if (index <= 0) "" else relativePath.substring(0, index)
    }

    fun canMoveToDirectory(relativePath: String, targetDirectory: String): Boolean {
        val source = relativePath.trim('/').trim()
        val target = targetDirectory.trim('/').trim()
        if (source.isEmpty()) return false
        val currentDirectory = parentDirectory(source)
        return currentDirectory != target &&
            source != target &&
            (target.isEmpty() || !target.startsWith("$source/"))
    }

    fun retargetAfterMove(current: String, from: String, next: String): String = when {
        current == from -> next
        current.startsWith("$from/") -> next + current.removePrefix(from)
        else -> current
    }
}
