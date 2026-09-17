package com.rememberber.mootool.next.compose.ai

import io.modelcontextprotocol.spec.McpSchema

/** Electron `listMooToolTools()` + `listVaultTools()` 元数据（描述、JSON Schema、annotations）。 */
data class McpToolRegistration(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val idempotentHint: Boolean = false,
) {
    fun annotations(): McpSchema.ToolAnnotations = McpSchema.ToolAnnotations(
        null,
        true,
        false,
        if (idempotentHint) true else false,
        false,
        null,
    )
}

object McpToolCatalog {
    fun registrations(): List<McpToolRegistration> = mooToolRegistrations() + vaultToolRegistrations()

    fun mooToolRegistrations(): List<McpToolRegistration> = listOf(
        reg(
            "mootool_json_format",
            "Format or minify JSON with MooTool; optionally sort keys and reject duplicate keys.",
            """{"type":"object","properties":{"text":{"type":"string","maxLength":100000},"spaces":{"type":"integer","minimum":0,"maximum":8,"default":2},"sortKeys":{"type":"boolean","default":false},"checkDuplicateKeys":{"type":"boolean","default":true}},"required":["text"],"additionalProperties":false}""",
        ),
        reg(
            "mootool_json_query",
            "Query JSON with JSONPath. Script/filter evaluation is disabled. Returns an array of matches.",
            """{"type":"object","properties":{"text":{"type":"string","maxLength":100000},"path":{"type":"string","minLength":1,"maxLength":1000}},"required":["text","path"],"additionalProperties":false}""",
        ),
        reg(
            "mootool_encode",
            "Encode or decode UTF-8 text as Base64, URL, hexadecimal or Unicode escapes. URL also supports GB2312.",
            """{"type":"object","properties":{"text":{"type":"string","maxLength":100000},"format":{"type":"string","enum":["base64","url","hex","unicode"]},"direction":{"type":"string","enum":["encode","decode"]},"charset":{"type":"string","enum":["utf-8","gb2312"],"default":"utf-8"}},"required":["text","format","direction"],"additionalProperties":false}""",
        ),
        reg(
            "mootool_timestamp",
            "Convert a Unix timestamp to local time or yyyy-MM-dd HH:mm:ss to a timestamp using MooTool timezone rules. 13+ digit timestamps are detected as milliseconds.",
            """{"type":"object","properties":{"text":{"type":"string","maxLength":100},"direction":{"type":"string","enum":["to-local","to-timestamp"]},"unit":{"type":"string","enum":["second","millisecond"],"default":"second"},"zone":{"type":"string","maxLength":100,"default":"UTC"}},"required":["text","direction"],"additionalProperties":false}""",
        ),
        reg(
            "mootool_diff",
            "Compare two texts with MooTool and return a unified diff and line counts. Inputs are limited to 8,000 characters each.",
            """{"type":"object","properties":{"left":{"type":"string","maxLength":8000},"right":{"type":"string","maxLength":8000},"ignoreWhitespace":{"type":"boolean","default":false}},"required":["left","right"],"additionalProperties":false}""",
        ),
        reg(
            "mootool_hash",
            "Calculate a hexadecimal digest of UTF-8 text. MD5/SHA-1 are available for legacy checksums only.",
            """{"type":"object","properties":{"text":{"type":"string","maxLength":100000},"algorithm":{"type":"string","enum":["md5","sha1","sha256","sha384","sha512"],"default":"sha256"}},"required":["text"],"additionalProperties":false}""",
        ),
        reg(
            "mootool_uuid",
            "Generate random version 4 UUIDs locally.",
            """{"type":"object","properties":{"count":{"type":"integer","minimum":1,"maximum":100,"default":1}},"additionalProperties":false}""",
        ),
        reg(
            "mootool_protobuf_wire",
            "Decode protobuf wire bytes from hex or Base64 into a field tree (no .proto required).",
            """{"type":"object","properties":{"text":{"type":"string","maxLength":100000},"format":{"type":"string","enum":["hex","base64"]}},"required":["text","format"],"additionalProperties":false}""",
        ),
    )

    fun vaultToolRegistrations(): List<McpToolRegistration> {
        val searchSchema =
            """{"type":"object","properties":{"query":{"type":"string","maxLength":200,"default":""},"limit":{"type":"integer","minimum":1,"maximum":50,"default":20},"offset":{"type":"integer","minimum":0,"maximum":2000,"default":0}},"additionalProperties":false}"""
        val readSchema =
            """{"type":"object","properties":{"path":{"type":"string","minLength":1,"maxLength":1000},"offset":{"type":"integer","minimum":0,"maximum":2000000,"default":0},"length":{"type":"integer","minimum":1,"maximum":50000,"default":20000}},"required":["path"],"additionalProperties":false}"""
        return listOf(
            reg(
                "mootool_notes_search",
                "Search titles and content in MooTool Quick Notes. Requires the user to enable read access in MooTool AI integration settings. Read-only; hidden, gitignored files and symlinks are excluded. Results can be paged.",
                searchSchema,
                idempotentHint = true,
            ),
            reg(
                "mootool_notes_read",
                "Read a relative document path from MooTool Quick Notes. Requires the user to enable read access in MooTool AI integration settings. Read-only; hidden, gitignored files and symlinks are excluded. Results can be paged.",
                readSchema,
                idempotentHint = true,
            ),
            reg(
                "mootool_json_documents_search",
                "Search titles and content in MooTool JSON documents. Requires the user to enable read access in MooTool AI integration settings. Read-only; hidden, gitignored files and symlinks are excluded. Results can be paged.",
                searchSchema,
                idempotentHint = true,
            ),
            reg(
                "mootool_json_documents_read",
                "Read a relative document path from MooTool JSON documents. Requires the user to enable read access in MooTool AI integration settings. Read-only; hidden, gitignored files and symlinks are excluded. Results can be paged.",
                readSchema,
                idempotentHint = true,
            ),
        )
    }

    private fun reg(
        name: String,
        description: String,
        inputSchemaJson: String,
        idempotentHint: Boolean = false,
    ) = McpToolRegistration(name, description, inputSchemaJson, idempotentHint)
}
