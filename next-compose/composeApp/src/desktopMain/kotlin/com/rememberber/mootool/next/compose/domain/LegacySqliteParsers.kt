package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

internal object LegacySqliteParsers {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseLegacyTime(raw: String?): Long {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return System.currentTimeMillis()
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching {
            val normalized = if (value.contains('T')) value else value.replace(' ', 'T')
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        return System.currentTimeMillis()
    }

    fun parseHttpPairs(raw: String?): List<HttpPair> {
        val array = parseJsonArray(raw)
        return array.mapIndexed { index, element ->
            val obj = element.jsonObject
            HttpPair(
                id = obj.stringOrNull("id") ?: UUID.randomUUID().toString(),
                name = obj.stringOrNull("name").orEmpty(),
                value = obj.stringOrNull("value").orEmpty(),
                enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
            )
        }
    }

    fun parseHttpCookies(raw: String?): List<HttpCookie> {
        val array = parseJsonArray(raw)
        return array.mapIndexed { index, element ->
            val obj = element.jsonObject
            HttpCookie(
                id = obj.stringOrNull("id") ?: UUID.randomUUID().toString(),
                name = obj.stringOrNull("name").orEmpty(),
                value = obj.stringOrNull("value").orEmpty(),
                domain = obj.stringOrNull("domain").orEmpty(),
                path = obj.stringOrNull("path").orEmpty().ifBlank { "/" },
                expires = obj.stringOrNull("expires").orEmpty().ifBlank { obj.stringOrNull("expiry").orEmpty() },
                enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
            )
        }
    }

    fun parseHttpMethod(raw: String?): HttpMethod =
        HttpMethod.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: HttpMethod.GET

    private fun parseJsonArray(raw: String?): JsonArray {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return JsonArray(emptyList())
        return runCatching { json.parseToJsonElement(text).jsonArray }.getOrDefault(JsonArray(emptyList()))
    }

    private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
