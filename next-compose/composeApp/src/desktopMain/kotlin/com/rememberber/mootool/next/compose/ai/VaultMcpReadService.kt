package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.domain.GitIgnoreMatcher
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

class VaultMcpReadService(private val accessFile: Path?) {
    private val maxBytes = 2_000_000L

    fun search(kind: VaultMcpKind, query: String, limit: Int, offset: Int): VaultMcpSearchResult {
        val root = root(kind)
        val matcher = matcher(root)
        val found = mutableListOf<VaultMcpSearchEntry>()
        var visited = 0
        var bytes = 0L
        var truncated = false
        val needle = query.lowercase()
        fun walk(directory: Path, depth: Int) {
            if (depth > 24) {
                truncated = true
                return
            }
            val realDir = runCatching { directory.toRealPath() }.getOrNull() ?: return
            if (!inside(root, realDir)) return
            val entries = runCatching {
                Files.list(directory).use { stream ->
                    stream.sorted(compareBy { it.fileName.toString() }).toList()
                }
            }.getOrElse { return }
            for (target in entries) {
                if (++visited > 4000 || bytes > 10_000_000 || found.size > offset + limit) {
                    truncated = true
                    return
                }
                if (target.fileName.toString().startsWith(".") || Files.isSymbolicLink(target)) continue
                val relative = root.relativize(target).toString().replace('\\', '/')
                val ignored = matcher.ignores(relative, target.isDirectory())
                if (ignored) continue
                if (target.isDirectory()) {
                    walk(target, depth + 1)
                } else if (target.isRegularFile() && allowed(kind, target)) {
                    try {
                        val document = document(root, kind, relative, target)
                        bytes += document.bytes
                        val haystack = "${document.title}\n$relative\n${document.content}".lowercase()
                        if (needle.isEmpty() || haystack.contains(needle)) {
                            val index = document.content.lowercase().indexOf(needle)
                            val excerptStart = if (needle.isEmpty()) 0 else maxOf(0, index - 60)
                            val excerpt = document.content.substring(
                                excerptStart,
                                minOf(document.content.length, excerptStart + 240)
                            )
                            found += VaultMcpSearchEntry(
                                path = relative,
                                title = document.title,
                                excerpt = excerpt,
                                modifiedAt = document.modifiedAt
                            )
                        }
                    } catch (_: Exception) {
                        // skip unreadable files during search
                    }
                }
            }
        }
        walk(root, 0)
        val hasMore = found.size > offset + limit
        return VaultMcpSearchResult(
            entries = found.drop(offset).take(limit),
            nextOffset = if (hasMore) offset + limit else null,
            truncated = truncated
        )
    }

    fun read(kind: VaultMcpKind, path: String, offset: Int, length: Int): VaultMcpReadResult {
        val root = root(kind)
        val normalized = normalizePath(path)
        val matcher = matcher(root)
        if (matcher.ignores(normalized, false)) {
            throw IllegalArgumentException("Gitignored documents are not exposed to AI")
        }
        val target = resolveUnderRoot(root, normalized)
        val document = document(root, kind, normalized, target)
        val end = minOf(document.content.length, offset + length)
        return VaultMcpReadResult(
            path = normalized,
            title = document.title,
            content = document.content.substring(offset, end),
            modifiedAt = document.modifiedAt,
            totalCharacters = document.content.length,
            nextOffset = if (end < document.content.length) end else null
        )
    }

    private fun root(kind: VaultMcpKind): Path {
        val access = VaultMcpAccess.read(accessFile)
        val raw = when (kind) {
            VaultMcpKind.Notes -> access.notes
            VaultMcpKind.Json -> access.json
        } ?: throw IllegalArgumentException(
            "Read access to ${kind.label()} is disabled. Enable it in MooTool Settings → AI integration."
        )
        val path = Path.of(raw).normalize()
        if (!path.isAbsolute) {
            throw IllegalArgumentException("The granted vault moved. Grant access again in MooTool settings.")
        }
        val real = runCatching { path.toRealPath() }.getOrElse {
            throw IllegalArgumentException("The granted vault moved. Grant access again in MooTool settings.")
        }
        // 对齐 Electron `realpath(path) !== path`：授权路径须为真实目录，不能是符号链接别名。
        if (real != path) {
            throw IllegalArgumentException("The granted vault moved. Grant access again in MooTool settings.")
        }
        if (!real.isDirectory()) {
            throw IllegalArgumentException("The granted vault moved. Grant access again in MooTool settings.")
        }
        return real
    }

    private fun matcher(root: Path): GitIgnoreMatcher {
        val file = root.resolve(".gitignore")
        if (!file.exists()) return GitIgnoreMatcher.parse("")
        if (!file.isRegularFile() || Files.isSymbolicLink(file)) {
            throw IllegalArgumentException("Cannot safely read the vault .gitignore; use a regular file under 100 KB")
        }
        if (Files.size(file) > 100_000) {
            throw IllegalArgumentException("Cannot safely read the vault .gitignore; use a regular file under 100 KB")
        }
        val bytes = runCatching { Files.readAllBytes(file) }.getOrElse {
            throw IllegalArgumentException("Cannot safely read the vault .gitignore; use a regular file under 100 KB")
        }
        val text = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrElse {
            throw IllegalArgumentException("Cannot safely read the vault .gitignore; use a regular file under 100 KB")
        }
        return GitIgnoreMatcher.parse(text)
    }

    private fun document(root: Path, kind: VaultMcpKind, relative: String, file: Path): VaultDocument {
        val normalized = normalizePath(relative)
        if (!allowed(kind, file)) throw IllegalArgumentException("Unsupported document type")
        val canonical = file.toRealPath(LinkOption.NOFOLLOW_LINKS)
        if (!inside(root, canonical)) throw IllegalArgumentException("Document must be inside the granted vault")
        if (!canonical.isRegularFile() || Files.isSymbolicLink(canonical)) {
            throw IllegalArgumentException("Unsupported document type")
        }
        val size = Files.size(canonical)
        if (size > maxBytes) throw IllegalArgumentException("Document exceeds the 2 MB read limit")
        val raw = canonical.readText(Charsets.UTF_8)
        if (raw.toByteArray(Charsets.UTF_8).size > maxBytes) {
            throw IllegalArgumentException("Document exceeds the 2 MB read limit")
        }
        val modifiedAt = Files.getLastModifiedTime(canonical).toInstant().toString()
        if (kind == VaultMcpKind.Notes) {
            val parsed = NoteFrontmatter.parse(raw, file.nameWithoutExtension, modifiedAt, modifiedAt)
            return VaultDocument(
                content = parsed.content,
                title = parsed.metadata.title,
                modifiedAt = parsed.metadata.modifiedAt.ifBlank { modifiedAt },
                bytes = raw.length
            )
        }
        return VaultDocument(
            content = raw,
            title = file.name,
            modifiedAt = modifiedAt,
            bytes = raw.length
        )
    }

    private fun resolveUnderRoot(root: Path, relative: String): Path {
        var target = root
        for (part in relative.split('/')) {
            if (part.isEmpty()) throw IllegalArgumentException("Use a relative document path returned by MooTool search")
            target = target.resolve(part)
            if (Files.isSymbolicLink(target)) throw IllegalArgumentException("Symbolic links are not exposed to AI")
        }
        return target
    }

    private fun allowed(kind: VaultMcpKind, file: Path): Boolean {
        val ext = ".${file.extension.lowercase()}"
        return when (kind) {
            VaultMcpKind.Json -> ext == ".json"
            VaultMcpKind.Notes -> NoteFrontmatter.noteExtensions.any { ".${it.lowercase()}" == ext }
        }
    }

    private fun normalizePath(path: String): String {
        val parts = path.split('/')
        if (path.isEmpty() || path.contains('\\') || path.contains(':') || path.contains('\u0000') ||
            parts.any { it.isEmpty() || it.startsWith('.') }
        ) {
            throw IllegalArgumentException("Use a relative document path returned by MooTool search")
        }
        if (Path.of(path).isAbsolute) {
            throw IllegalArgumentException("Use a relative document path returned by MooTool search")
        }
        return parts.joinToString("/")
    }

    private fun inside(root: Path, path: Path): Boolean {
        val rootReal = runCatching { root.toRealPath() }.getOrNull() ?: return false
        val target = runCatching { path.toRealPath() }.getOrNull() ?: return false
        return target.startsWith(rootReal)
    }

    private data class VaultDocument(
        val content: String,
        val title: String,
        val modifiedAt: String,
        val bytes: Int
    )
}

private fun VaultMcpKind.label(): String = when (this) {
    VaultMcpKind.Notes -> "notes"
    VaultMcpKind.Json -> "json"
}

@Serializable
data class VaultMcpSearchEntry(
    val path: String,
    val title: String,
    val excerpt: String,
    val modifiedAt: String
)

@Serializable
data class VaultMcpSearchResult(
    val entries: List<VaultMcpSearchEntry>,
    val nextOffset: Int?,
    val truncated: Boolean
)

@Serializable
data class VaultMcpReadResult(
    val path: String,
    val title: String,
    val content: String,
    val modifiedAt: String,
    val totalCharacters: Int,
    val nextOffset: Int?
)
