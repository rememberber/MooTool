package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

object LegacyImportOptions {
    private val json = Json { ignoreUnknownKeys = true }

    const val DEDUPE_KEY_FIELD = "importDedupeKey"

    fun withDedupeKey(dedupeKey: String, options: String = ""): String {
        if (dedupeKey.isBlank()) return options
        val base = runCatching { json.parseToJsonElement(options).jsonObject }.getOrNull() ?: JsonObject(emptyMap())
        return json.encodeToString(
            buildJsonObject {
                base.forEach { (key, value) -> put(key, value) }
                put(DEDUPE_KEY_FIELD, JsonPrimitive(dedupeKey))
            }
        )
    }

    fun readDedupeKey(options: String): String? {
        if (options.isBlank()) return null
        return runCatching {
            json.parseToJsonElement(options).jsonObject[DEDUPE_KEY_FIELD]?.let { element ->
                element.toString().trim('"')
            }
        }.getOrNull()
    }
}
