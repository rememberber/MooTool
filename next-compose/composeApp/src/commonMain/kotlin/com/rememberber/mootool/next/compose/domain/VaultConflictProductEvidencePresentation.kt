package com.rememberber.mootool.next.compose.domain

/**
 * Vault 外部冲突产品主窗 §A 走查常量（对齐 `prepare-vault-conflict-evidence.sh`）。
 */
object VaultConflictProductEvidencePresentation {
    const val EVIDENCE_JSON_RELATIVE_PATH = "sample.json"
    const val EVIDENCE_QUICKNOTE_RELATIVE_PATH = "sample-external.md"
    const val EVIDENCE_JSON_FIXTURE_MARKER = "vault-external-conflict"
    const val EVIDENCE_QUICKNOTE_FIXTURE_MARKER = "随手记外部冲突走查"

    val evidenceJsonInitialBody: String =
        """
        {
          "fixture": "vault-external-conflict",
          "step": "open in app then edit without saving"
        }
        """.trimIndent()

    /** 脚本步骤 3 外部改写磁盘示例（`printf '{"disk":true}'`）。 */
    const val EVIDENCE_JSON_DISK_REWRITE = """{"disk":true}"""

    val evidenceQuickNoteInitialBody: String =
        """
        ---
        title: Vault external conflict sample
        syntax: text/markdown
        ---
        $EVIDENCE_QUICKNOTE_FIXTURE_MARKER：打开后编辑不保存，再改写磁盘文件。
        """.trimIndent()

    /** 脚本步骤 3 随手记外部改写示例正文。 */
    const val EVIDENCE_QUICKNOTE_DISK_REWRITE = "disk rewrite from terminal\n"

    fun matchesEvidenceInitialJson(text: String): Boolean =
        text.contains(EVIDENCE_JSON_FIXTURE_MARKER)

    fun matchesEvidenceInitialQuickNote(text: String): Boolean =
        text.contains(EVIDENCE_QUICKNOTE_FIXTURE_MARKER)

    fun matchesEvidenceWalkthroughPath(relativePath: String): Boolean =
        relativePath == EVIDENCE_JSON_RELATIVE_PATH ||
            relativePath == EVIDENCE_QUICKNOTE_RELATIVE_PATH
}
