package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.storage.VaultIndexRecord

object VaultSearchIndex {
    fun filter(records: List<VaultIndexRecord>, query: String, includeContent: Boolean): List<VaultEntry> {
        val needle = JsonVaultSearchPresentation.normalizeQuery(query)
        if (needle.isEmpty()) return records.map { it.entry }
        val keep = linkedSetOf<String>()
        records.forEach { record ->
            if (record.entry.directory) {
                if (pathMatches(record.entry, needle)) {
                    keep += record.entry.relativePath
                    records.forEach { child ->
                        if (child.entry.relativePath == record.entry.relativePath ||
                            child.entry.relativePath.startsWith("${record.entry.relativePath}/")
                        ) {
                            keep += child.entry.relativePath
                        }
                    }
                }
            } else if (fileMatches(record, needle, includeContent)) {
                keep += record.entry.relativePath
                var parent = VaultMove.parentDirectory(record.entry.relativePath)
                while (parent.isNotEmpty()) {
                    keep += parent
                    parent = VaultMove.parentDirectory(parent)
                }
            }
        }
        return records.map { it.entry }.filter { it.relativePath in keep }
    }

    private fun pathMatches(entry: VaultEntry, needle: String): Boolean =
        JsonVaultSearchPresentation.pathSegmentMatches(entry.relativePath, entry.name, needle)

    private fun fileMatches(record: VaultIndexRecord, needle: String, includeContent: Boolean): Boolean {
        if (pathMatches(record.entry, needle) ||
            JsonVaultSearchPresentation.titleMatches(record.title, needle)
        ) {
            return true
        }
        return JsonVaultSearchPresentation.contentMatches(record.content, needle, includeContent)
    }
}
