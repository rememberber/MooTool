package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteVaultTest {
    @Test
    fun createsJavaCompatibleFrontmatterAndSupportsNestedCrud() {
        val vault = vault()
        vault.createDirectory("Work")
        val note = vault.createNote("API ideas", parentPath = "Work", fontName = "PingFang SC", fontSize = 15)
        val saved = vault.saveNote(note.relativePath, "# Hello\nneedle", note.metadata.copy(color = "coral"))
        assertEquals("Work/API ideas.txt", saved.relativePath)
        val raw = vault.root().resolve(saved.relativePath).readText()
        assertTrue(raw.contains("font_name: PingFang SC"))
        assertTrue(raw.contains("font_size: \"15\""))
        assertTrue(raw.contains("line_spacing: \"1.0\""))
        assertTrue(raw.contains("color: coral"))
        assertEquals("# Hello\nneedle", vault.readNote(saved.relativePath).content)
        assertEquals("coral", vault.list().first { it.relativePath == saved.relativePath }.color)
        val listed = vault.list().first { it.relativePath == saved.relativePath }
        assertTrue(listed.createdAt.isNotBlank())
        assertTrue(listed.modifiedAt.isNotBlank())
        assertTrue(vault.list("needle", includeContent = true).any { it.relativePath == saved.relativePath })
        assertTrue(vault.list("needle", includeContent = false).none { it.relativePath == saved.relativePath })

        val renamed = vault.rename(saved.relativePath, "API notes")
        assertEquals("Work/API notes.txt", renamed)
        val duplicate = vault.duplicate(renamed)
        assertEquals("Work/API notes Copy.txt", duplicate.relativePath)
        val moved = vault.move(duplicate.relativePath, "")
        assertEquals("API notes Copy.txt", moved)
        vault.delete(moved)
        assertFails { vault.readNote(moved) }
    }

    @Test
    fun readsLegacyFilesAndHonorsGitignore() {
        val vault = vault()
        vault.root().resolve("Private").createDirectories()
        vault.root().resolve("Legacy.txt").writeText("---\ntitle: Legacy title\nsyntax: text/markdown\nline_spacing: \"1.6\"\nline_wrap: \"0\"\n---\nlegacy body")
        vault.root().resolve("Private").resolve("Secret.txt").writeText("secret")
        vault.root().resolve(".gitignore").writeText("Private/\n")
        val note = vault.readNote("Legacy.txt")
        assertEquals("legacy body", note.content)
        assertEquals("Legacy title", note.metadata.title)
        assertEquals(1.6, note.metadata.lineSpacing)
        assertFalse(note.metadata.lineWrap)
        assertTrue(vault.list(hideIgnored = true).none { it.relativePath.startsWith("Private") })
        assertTrue(vault.list(hideIgnored = false).any { it.relativePath == "Private/Secret.txt" })
    }

    @Test
    fun renamesNoteFilesToMatchSyntaxAndDeletesFoldersRecursively() {
        val vault = vault()
        val note = vault.createNote("Configuration")
        val markdown = vault.saveNote(note.relativePath, "# Configuration", note.metadata.copy(syntax = "text/markdown"))
        assertEquals("Configuration.md", markdown.relativePath)
        assertFalse(vault.root().resolve("Configuration.txt").toFile().exists())
        vault.createDirectory("Folder")
        vault.createNote("Note", parentPath = "Folder")
        vault.delete("Folder")
        assertFalse(vault.root().resolve("Folder").toFile().exists())
    }

    private fun vault(): NoteVault {
        val root = createTempDirectory("mootool-compose-notes-")
        return NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
    }
}

class JsonVaultTest {
    @Test
    fun supportsFolderCrudSearchAndGitignore() {
        val root = createTempDirectory("mootool-compose-json-")
        val vault = JsonVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        vault.createDirectory("drafts")
        vault.createDirectory("archive")
        vault.createFile("drafts/request.json", """{"ok":true}""")
        val renamed = vault.rename("drafts/request.json", "response.json")
        assertEquals("drafts/response.json", renamed)
        val moved = vault.move(renamed, "archive")
        assertEquals("archive/response.json", moved)
        val copy = vault.duplicate(moved)
        assertEquals("archive/response Copy.json", copy)
        vault.write("archive/content-only.json", """{"marker":"needle-value"}""")
        assertTrue(vault.list("by-name", includeContent = false).isEmpty())
        vault.createFile("requests/by-name.json", """{"marker":"ordinary"}""")
        assertTrue(vault.list("by-name", includeContent = false).any { it.name == "by-name.json" })
        assertTrue(vault.list("needle-value", includeContent = false).none { it.name == "content-only.json" })
        assertTrue(vault.list("NEEDLE-VALUE", includeContent = true).any { it.name == "content-only.json" })
        vault.root().resolve(".gitignore").writeText("ignored.json\n")
        vault.root().resolve("ignored.json").writeText("{}")
        assertTrue(vault.list(hideIgnored = true).none { it.name == "ignored.json" })
        assertTrue(vault.list(hideIgnored = false).any { it.name == "ignored.json" })
    }
}
