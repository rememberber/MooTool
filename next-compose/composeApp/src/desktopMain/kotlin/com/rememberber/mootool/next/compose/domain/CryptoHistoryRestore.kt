package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CryptoSession

object CryptoHistoryRestore {
    fun apply(session: CryptoSession, item: HistoryRecord) {
        val meta = CryptoHistoryMetadata.decode(item.options)
        val tab = meta?.tab.orEmpty()
        val operation = meta?.operation.orEmpty()
        session.tab = when (tab) {
            "asymmetric" -> CryptoTab.Asymmetric
            "digest" -> CryptoTab.Digest
            "base" -> CryptoTab.Base
            "random" -> CryptoTab.Random
            else -> CryptoTab.Symmetric
        }
        when (session.tab) {
            CryptoTab.Symmetric -> {
                if (operation == "decrypt") {
                    session.symCipher = item.input
                    session.symPlain = item.output
                } else {
                    session.symPlain = item.input
                    session.symCipher = item.output
                }
                SymmetricAlgorithm.entries.find { it.name == meta?.algorithm }?.let { session.symAlgorithm = it }
            }
            CryptoTab.Asymmetric -> {
                if (operation.contains("Decrypt", ignoreCase = true)) {
                    session.asymCipher = item.input
                    session.asymPlain = item.output
                } else {
                    session.asymPlain = item.input
                    session.asymCipher = item.output
                }
                AsymmetricAlgorithm.entries.find { it.name == meta?.algorithm }?.let { session.asymAlgorithm = it }
            }
            CryptoTab.Digest -> {
                session.digestInput = item.input
                session.digestOutput = item.output
                DigestAlgorithm.entries.find { digestLabel(it) == meta?.algorithm }?.let { session.digestAlgorithm = it }
            }
            CryptoTab.Base -> {
                if (operation == "decode") {
                    session.baseCipher = item.input
                    session.basePlain = item.output
                } else {
                    session.basePlain = item.input
                    session.baseCipher = item.output
                }
                BaseAlgorithm.entries.find { it.name == meta?.algorithm }?.let { session.baseAlgorithm = it }
            }
            CryptoTab.Random -> when (operation) {
                "uuid" -> session.uuid = item.output
                "digits" -> session.digits = item.output
                "string" -> session.randomText = item.output
                "password" -> session.password = item.output
            }
        }
        session.error = ""
    }

    private fun digestLabel(algorithm: DigestAlgorithm): String = when (algorithm) {
        DigestAlgorithm.MD5 -> "MD5"
        DigestAlgorithm.SHA1 -> "SHA-1"
        DigestAlgorithm.SHA256 -> "SHA-256"
        DigestAlgorithm.SHA384 -> "SHA-384"
        DigestAlgorithm.SHA512 -> "SHA-512"
        DigestAlgorithm.SM3 -> "SM3"
    }
}
