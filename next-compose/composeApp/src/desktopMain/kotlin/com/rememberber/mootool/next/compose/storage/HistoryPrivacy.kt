package com.rememberber.mootool.next.compose.storage

data class HistoryPayload(
    val input: String,
    val output: String
)

object HistoryPrivacy {
    const val REDACTED = "[redacted]"

    private val secretCryptoOps = setOf(
        "privateDecrypt",
        "privateEncrypt",
        "sign",
        "publicDecrypt"
    )

    fun crypto(operation: String, input: String, output: String): HistoryPayload {
        val hideInput = operation in secretCryptoOps || looksLikePrivateKey(input)
        val hideOutput = operation == "password" || looksLikePrivateKey(output)
        return HistoryPayload(
            input = if (hideInput) REDACTED else input,
            output = if (hideOutput) REDACTED else output
        )
    }

    fun httpUrl(url: String): String =
        url.replace(Regex("://([^/@:\\s]+):([^/@\\s]+)@"), "://$1:$REDACTED@")

    fun looksLikePrivateKey(text: String): Boolean {
        val upper = text.uppercase()
        return upper.contains("BEGIN ") &&
            (upper.contains("PRIVATE KEY") || upper.contains("RSA PRIVATE") || upper.contains("EC PRIVATE"))
    }
}
