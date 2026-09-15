package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.ProductIdentity
import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

enum class UpdateCheckStatus { Latest, Available, Unpublished }

data class UpdateIdentity(
    val platform: String,
    val architecture: String,
    val productId: String = ProductIdentity.PRODUCT_ID
)

data class UpdateAsset(
    val platform: String,
    val architecture: String,
    val packageType: String,
    val fileName: String,
    val url: String,
    val sha512: String,
    val size: Long,
    val priority: Int
)

data class UpdateRelease(
    val version: String,
    val title: String,
    val notes: String,
    val prerelease: Boolean,
    val releaseUrl: String,
    val assets: List<UpdateAsset>
)

data class UpdateDownload(
    val fileName: String,
    val url: String,
    val sha512: String,
    val size: Long
)

data class UpdateCheckResult(
    val status: UpdateCheckStatus,
    val productId: String,
    val productName: String,
    val currentVersion: String,
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val platform: String,
    val architecture: String,
    val download: UpdateDownload? = null,
    val message: String = ""
)

fun interface UpdateTextFetcher {
    fun fetch(url: String): Pair<Int, String>
}

object UpdateEngine {
    const val DEFAULT_FEED = "https://raw.githubusercontent.com/rememberber/MooTool/master/update-manifest.json"
    const val DEFAULT_RELEASES = "https://github.com/rememberber/MooTool/releases?q=next-compose"
    const val MAX_MANIFEST_BYTES = 2 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true }
    private val sha512Pattern = Regex("^[A-Za-z0-9+/]{86}==$")

    fun detectIdentity(
        osName: String = System.getProperty("os.name").orEmpty(),
        osArch: String = System.getProperty("os.arch").orEmpty()
    ): UpdateIdentity {
        val os = osName.lowercase()
        val platform = when {
            os.contains("mac") -> "darwin"
            os.contains("win") -> "win32"
            else -> "linux"
        }
        return UpdateIdentity(platform, normalizeArchitecture(osArch))
    }

    fun normalizeArchitecture(value: String): String {
        val arch = value.trim().lowercase()
        return if (arch.contains("aarch64") || arch == "arm64") "arm64" else "x64"
    }

    fun artifactName(version: String, platform: String, architecture: String, packageType: String): String {
        val ver = normalizeVersion(version)
        val arch = normalizeArchitecture(architecture)
        return when (platform.trim().lowercase() to packageType.trim().lowercase()) {
            "darwin" to "dmg" -> "MooTool-Next-Compose-$ver-mac-$arch.dmg"
            "win32" to "msi" -> "MooTool-Next-Compose-$ver-win-$arch-setup.msi"
            "linux" to "deb" -> "MooTool-Next-Compose-$ver-linux-$arch.deb"
            else -> error("Unsupported Compose package $platform/$packageType")
        }
    }

    fun compareVersions(left: String, right: String): Int {
        val a = parseVersion(left)
        val b = parseVersion(right)
        for (index in 0 until 3) {
            if (a.numbers[index] != b.numbers[index]) return if (a.numbers[index] > b.numbers[index]) 1 else -1
        }
        if (a.prerelease.isEmpty() && b.prerelease.isEmpty()) return 0
        if (a.prerelease.isEmpty()) return 1
        if (b.prerelease.isEmpty()) return -1
        val max = maxOf(a.prerelease.size, b.prerelease.size)
        for (index in 0 until max) {
            val leftPart = a.prerelease.getOrNull(index)
            val rightPart = b.prerelease.getOrNull(index)
            if (leftPart == null) return -1
            if (rightPart == null) return 1
            if (leftPart == rightPart) continue
            val leftNumeric = leftPart.all { it.isDigit() }
            val rightNumeric = rightPart.all { it.isDigit() }
            if (leftNumeric && rightNumeric) {
                val ln = leftPart.toLong()
                val rn = rightPart.toLong()
                if (ln != rn) return if (ln > rn) 1 else -1
                continue
            }
            if (leftNumeric != rightNumeric) return if (leftNumeric) -1 else 1
            return if (leftPart > rightPart) 1 else -1
        }
        return 0
    }

    fun normalizeVersion(raw: String): String {
        val parsed = parseVersion(raw)
        val core = parsed.numbers.joinToString(".")
        return if (parsed.prerelease.isEmpty()) core else "$core-${parsed.prerelease.joinToString(".")}"
    }

    fun requireHttps(url: String, kind: String): String {
        val trimmed = url.trim()
        check(trimmed.startsWith("https://") && !trimmed.contains(' ')) { "Update $kind URL must use HTTPS" }
        val host = trimmed.removePrefix("https://").substringBefore('/').substringBefore('?')
        check(host.isNotBlank()) { "Update $kind URL must use HTTPS" }
        return trimmed
    }

    fun check(
        currentVersion: String,
        identity: UpdateIdentity,
        rawManifest: String,
        productId: String = ProductIdentity.PRODUCT_ID
    ): UpdateCheckResult {
        check(identity.productId == ProductIdentity.PRODUCT_ID && productId == ProductIdentity.PRODUCT_ID) {
            "Update productId is compiled to ${ProductIdentity.PRODUCT_ID}"
        }
        check(rawManifest.isNotEmpty() && rawManifest.length <= MAX_MANIFEST_BYTES) { "Invalid update response" }
        val current = normalizeVersion(currentVersion)
        val product = parseProduct(rawManifest, productId)
        if (product == null) {
            return unpublished(current, identity, ProductIdentity.DISPLAY_NAME)
        }
        val allowPrerelease = parseVersion(current).prerelease.isNotEmpty()
        val eligible = product.releases.filter { allowPrerelease || !it.prerelease }
        if (eligible.isEmpty()) return unpublished(current, identity, product.displayName)
        val latest = eligible.reduce { left, right ->
            if (compareVersions(right.version, left.version) > 0) right else left
        }
        val available = compareVersions(latest.version, current) > 0
        return UpdateCheckResult(
            status = if (available) UpdateCheckStatus.Available else UpdateCheckStatus.Latest,
            productId = productId,
            productName = product.displayName,
            currentVersion = current,
            latestVersion = latest.version,
            releaseUrl = latest.releaseUrl,
            releaseNotes = if (available) releaseNotesAfter(eligible, current) else "",
            platform = identity.platform,
            architecture = identity.architecture,
            download = if (available) selectAsset(latest.assets, identity) else null
        )
    }

    fun fetchAndCheck(
        currentVersion: String,
        identity: UpdateIdentity,
        feedUrl: String,
        fetcher: UpdateTextFetcher
    ): UpdateCheckResult {
        val feed = requireHttps(feedUrl, "feed")
        val (code, body) = fetcher.fetch(feed)
        check(code in 200..299) { "Update server returned HTTP $code" }
        return check(currentVersion, identity, body)
    }

    fun sha512Base64(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(bytes))

    fun selectAsset(assets: List<UpdateAsset>, identity: UpdateIdentity): UpdateDownload? {
        val platform = identity.platform.trim().lowercase()
        val architecture = normalizeArchitecture(identity.architecture)
        val matches = assets.filter { asset ->
            asset.platform == platform &&
                (asset.architecture == architecture || asset.architecture == "universal") &&
                (platform != "darwin" || asset.packageType == "dmg")
        }.sortedWith(
            compareBy<UpdateAsset> { if (it.architecture == architecture) 0 else 1 }
                .thenBy { it.priority }
                .thenBy { packagePreference(platform, it.packageType) }
        )
        return matches.firstOrNull()?.let { UpdateDownload(it.fileName, it.url, it.sha512, it.size) }
    }

    private data class ParsedProduct(val displayName: String, val releases: List<UpdateRelease>)
    private data class ParsedVersion(val numbers: List<Int>, val prerelease: List<String>)

    private fun unpublished(current: String, identity: UpdateIdentity, name: String) = UpdateCheckResult(
        status = UpdateCheckStatus.Unpublished,
        productId = ProductIdentity.PRODUCT_ID,
        productName = name,
        currentVersion = current,
        latestVersion = current,
        releaseUrl = DEFAULT_RELEASES,
        releaseNotes = "",
        platform = identity.platform,
        architecture = identity.architecture,
        message = "unpublished"
    )

    private fun parseProduct(raw: String, productId: String): ParsedProduct? {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrElse { error("Update response is not valid JSON") }
        check(root["schemaVersion"]?.jsonPrimitive?.int == 1) { "Unsupported update manifest" }
        val products = root["products"]?.jsonObject ?: error("Unsupported update manifest")
        val node = products[productId] ?: return null
        val product = node.jsonObject
        if (product["status"]?.jsonPrimitive?.contentOrNull != "active") return null
        val displayName = product["displayName"]?.jsonPrimitive?.contentOrNull?.take(120) ?: error("Invalid update product: $productId")
        val releases = (product["releases"] as? JsonArray)?.map { parseRelease(it.jsonObject) } ?: error("Invalid update product: $productId")
        check(releases.map { it.version }.toSet().size == releases.size) { "Update product has duplicate release versions: $productId" }
        return ParsedProduct(displayName, releases)
    }

    private fun parseRelease(map: JsonObject): UpdateRelease {
        val version = normalizeVersion(map["version"]?.jsonPrimitive?.contentOrNull ?: error("Invalid release in update manifest"))
        val prerelease = parseVersion(version).prerelease.isNotEmpty()
        map["prerelease"]?.jsonPrimitive?.boolean?.let { flagged ->
            check(flagged == prerelease) { "Prerelease flag does not match version $version" }
        }
        val assets = (map["assets"] as? JsonArray)?.map { parseAsset(it.jsonObject) } ?: emptyList()
        return UpdateRelease(
            version = version,
            title = map["title"]?.jsonPrimitive?.contentOrNull.orEmpty().take(300),
            notes = map["notes"]?.jsonPrimitive?.contentOrNull.orEmpty().take(5_000),
            prerelease = prerelease,
            releaseUrl = requireHttps(map["releaseUrl"]?.jsonPrimitive?.contentOrNull ?: error("Invalid release in update manifest"), "release"),
            assets = assets
        )
    }

    private fun parseAsset(map: JsonObject): UpdateAsset {
        val fileName = map["fileName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        check(fileName.isNotEmpty() && fileName.length <= 240 && '/' !in fileName && '\\' !in fileName) { "Invalid update asset file name" }
        val sha512 = map["sha512"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        check(sha512Pattern.matches(sha512)) { "Invalid update asset SHA-512" }
        val size = map["size"]?.jsonPrimitive?.long ?: error("Invalid asset in update manifest")
        check(size > 0) { "Invalid update asset size" }
        return UpdateAsset(
            platform = map["platform"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase() ?: error("Invalid asset"),
            architecture = normalizeArchitecture(map["architecture"]?.jsonPrimitive?.contentOrNull ?: error("Invalid asset")),
            packageType = map["packageType"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase() ?: error("Invalid asset"),
            fileName = fileName,
            url = requireHttps(map["url"]?.jsonPrimitive?.contentOrNull ?: error("Invalid asset"), "asset"),
            sha512 = sha512,
            size = size,
            priority = map["priority"]?.jsonPrimitive?.int ?: 100
        )
    }

    private fun parseVersion(raw: String): ParsedVersion {
        var value = raw.trim()
        if (value.startsWith("v") || value.startsWith("V")) value = value.substring(1)
        val plus = value.indexOf('+')
        if (plus >= 0) value = value.substring(0, plus)
        val dash = value.indexOf('-')
        val core = if (dash >= 0) value.substring(0, dash) else value
        val pre = if (dash >= 0) value.substring(dash + 1) else ""
        val parts = core.split('.')
        check(parts.isNotEmpty() && parts.size <= 3 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) { "Invalid version: $raw" }
        val numbers = IntArray(3)
        parts.forEachIndexed { index, part -> numbers[index] = part.toInt() }
        val prerelease = if (pre.isEmpty()) emptyList() else pre.split('.').map { token ->
            check(token.isNotEmpty()) { "Invalid version: $raw" }
            check(!(token.all(Char::isDigit) && token.length > 1 && token.startsWith('0'))) { "Invalid version: $raw" }
            token
        }
        return ParsedVersion(numbers.toList(), prerelease)
    }

    private fun packagePreference(platform: String, packageType: String): Int {
        val order = when (platform) {
            "darwin" -> listOf("dmg", "pkg")
            "win32" -> listOf("nsis", "msi", "portable", "zip")
            "linux" -> listOf("appimage", "deb", "rpm", "tar.gz")
            else -> emptyList()
        }
        val index = order.indexOf(packageType)
        return if (index < 0) 100 else index
    }

    private fun releaseNotesAfter(releases: List<UpdateRelease>, currentVersion: String): String {
        val newer = releases.filter { compareVersions(it.version, currentVersion) > 0 }
            .sortedWith { left, right -> compareVersions(right.version, left.version) }
        val selected = mutableListOf<String>()
        var length = 0
        for (release in newer) {
            val heading = if (release.title.contains(release.version)) release.title
            else listOf(release.version, release.title).filter { it.isNotBlank() }.joinToString(" — ")
            val section = listOf(heading, release.notes).filter { it.isNotBlank() }.joinToString("\n").take(12_000)
            val added = section.length + if (selected.isNotEmpty()) 2 else 0
            if (selected.isNotEmpty() && length + added > 12_000) break
            selected.add(0, section)
            length += added
        }
        return selected.joinToString("\n\n")
    }
}
