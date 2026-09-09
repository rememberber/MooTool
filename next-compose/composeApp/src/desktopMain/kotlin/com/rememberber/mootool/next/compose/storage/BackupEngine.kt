package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.app.ProductIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

enum class BackupErrorCode { INVALID_ARCHIVE, WRONG_PRODUCT, HASH_MISMATCH, PATH_ESCAPE, IO }

class BackupException(val code: BackupErrorCode, message: String) : RuntimeException(message)

@Serializable
data class BackupFileEntry(
    val path: String,
    val sha256: String,
    val size: Long
)

@Serializable
data class BackupManifest(
    val productId: String,
    val appVersion: String,
    val schemaVersion: Int,
    val createdAt: String,
    val files: List<BackupFileEntry>
)

data class BackupExportResult(
    val zipPath: Path,
    val manifest: BackupManifest
)

data class BackupRestoreResult(
    val safetyBackup: Path,
    val restored: List<String>
)

object BackupEngine {
    const val MANIFEST_NAME = "manifest.json"
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
    private val skippedDataNames = setOf("backups", "cache")

    fun defaultZipName(): String = "MooTool-Next-Compose-backup-${stamp.format(Instant.now())}.zip"

    fun export(directories: AppDirectories, zipPath: Path, database: AppDatabase? = null): BackupExportResult {
        if (zipPath.exists()) {
            throw BackupException(BackupErrorCode.IO, "Backup file already exists: ${zipPath.fileName}")
        }
        zipPath.parent?.createDirectories()
        checkDestination(directories, zipPath)
        database?.checkpoint()
        val sources = collectSources(directories)
        val entries = ArrayList<BackupFileEntry>()
        ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
            sources.forEach { source ->
                val digest = sha256AndCopy(source.bytes()) { bytes ->
                    zip.putNextEntry(ZipEntry(source.zipPath))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                entries += BackupFileEntry(source.zipPath, digest, source.size)
            }
            val manifest = BackupManifest(
                productId = ProductIdentity.PRODUCT_ID,
                appVersion = ProductIdentity.VERSION,
                schemaVersion = ProductIdentity.SCHEMA_VERSION,
                createdAt = Instant.now().toString(),
                files = entries
            )
            val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
            zip.putNextEntry(ZipEntry(MANIFEST_NAME))
            zip.write(manifestBytes)
            zip.closeEntry()
            return BackupExportResult(zipPath, manifest.copy(files = entries))
        }
    }

    fun preview(zipPath: Path): BackupManifest {
        ZipFile(zipPath.toFile()).use { zip ->
            Collections.list(zip.entries()).forEach { entry -> normalizeZipPath(entry.name) }
            val manifestEntry = zip.getEntry(MANIFEST_NAME)
                ?: throw BackupException(BackupErrorCode.INVALID_ARCHIVE, "Backup is missing manifest.json")
            val manifest = json.decodeFromString<BackupManifest>(zip.getInputStream(manifestEntry).readBytes().toString(Charsets.UTF_8))
            if (manifest.productId != ProductIdentity.PRODUCT_ID) {
                throw BackupException(BackupErrorCode.WRONG_PRODUCT, "Backup productId is ${manifest.productId}")
            }
            Collections.list(zip.entries()).forEach { entry ->
                val name = normalizeZipPath(entry.name)
                if (name != MANIFEST_NAME && !entry.isDirectory) {
                    val listed = manifest.files.firstOrNull { it.path == name }
                        ?: throw BackupException(BackupErrorCode.INVALID_ARCHIVE, "Unlisted file: $name")
                    val actual = sha256(zip.getInputStream(entry))
                    if (!actual.equals(listed.sha256, ignoreCase = true)) {
                        throw BackupException(BackupErrorCode.HASH_MISMATCH, "Checksum mismatch: $name")
                    }
                }
            }
            return manifest
        }
    }

    fun restore(zipPath: Path, directories: AppDirectories, database: AppDatabase? = null): BackupRestoreResult {
        val manifest = preview(zipPath)
        checkDestination(directories, zipPath)
        database?.checkpoint()
        database?.close()
        val safety = directories.backups.resolve("pre-restore-${stamp.format(Instant.now())}").apply { createDirectories() }
        copyLiveSnapshot(directories, safety)
        val staging = Files.createTempDirectory("mootool-compose-restore-")
        try {
            extractValidated(zipPath, staging, manifest)
            applyStaging(staging, directories)
            return BackupRestoreResult(safety, manifest.files.map { it.path })
        } catch (error: Exception) {
            runCatching { applyStaging(safety, directories, fromSafety = true) }
            if (error is BackupException) throw error
            throw BackupException(BackupErrorCode.IO, error.message ?: "Restore failed")
        } finally {
            runCatching { staging.toFile().deleteRecursively() }
            runCatching { database?.reopen() }
        }
    }

    private data class SourceFile(val zipPath: String, val file: Path, val size: Long) {
        fun bytes(): ByteArray = Files.readAllBytes(file)
    }

    private fun collectSources(directories: AppDirectories): List<SourceFile> {
        val files = ArrayList<SourceFile>()
        addIfRegular(files, directories.settingsFile, "config/${ProductIdentity.SETTINGS_FILE}")
        val bootstrap = directories.bootstrapFile
        addIfRegular(files, bootstrap, "config/${bootstrap.fileName}")
        if (directories.dataRoot.exists()) {
            Files.walk(directories.dataRoot).use { stream ->
                stream.filter { it.isRegularFile() && !it.isSymbolicLink() }.forEach { file ->
                    val relative = file.relativeTo(directories.dataRoot).invariantSeparatorsPathString
                    val top = relative.substringBefore('/', relative)
                    if (top in skippedDataNames) return@forEach
                    files += SourceFile("data/$relative", file, Files.size(file))
                }
            }
        }
        return files.sortedBy { it.zipPath }
    }

    private fun addIfRegular(files: MutableList<SourceFile>, file: Path, zipPath: String) {
        if (file.isRegularFile() && !file.isSymbolicLink()) {
            files += SourceFile(zipPath, file, Files.size(file))
        }
    }

    private fun extractValidated(zipPath: Path, staging: Path, manifest: BackupManifest) {
        ZipFile(zipPath.toFile()).use { zip ->
            Collections.list(zip.entries()).forEach { entry ->
                if (entry.isDirectory) return@forEach
                val name = normalizeZipPath(entry.name)
                if (name == MANIFEST_NAME) {
                    val target = resolveInside(staging, name)
                    target.parent.createDirectories()
                    Files.copy(zip.getInputStream(entry), target, StandardCopyOption.REPLACE_EXISTING)
                    return@forEach
                }
                val listed = manifest.files.firstOrNull { it.path == name }
                    ?: throw BackupException(BackupErrorCode.INVALID_ARCHIVE, "Unlisted file: $name")
                val target = resolveInside(staging, name)
                target.parent.createDirectories()
                Files.copy(zip.getInputStream(entry), target, StandardCopyOption.REPLACE_EXISTING)
                val actual = sha256(Files.newInputStream(target))
                if (!actual.equals(listed.sha256, ignoreCase = true) || Files.size(target) != listed.size) {
                    throw BackupException(BackupErrorCode.HASH_MISMATCH, "Checksum mismatch: $name")
                }
            }
        }
    }

    private fun applyStaging(staging: Path, directories: AppDirectories, fromSafety: Boolean = false) {
        val settingsSource = if (fromSafety) staging.resolve(ProductIdentity.SETTINGS_FILE) else staging.resolve("config").resolve(ProductIdentity.SETTINGS_FILE)
        if (settingsSource.exists()) {
            directories.settingsFile.parent.createDirectories()
            Files.copy(settingsSource, directories.settingsFile, StandardCopyOption.REPLACE_EXISTING)
        }
        val bootstrapName = directories.bootstrapFile.fileName.toString()
        val bootstrapSource = if (fromSafety) staging.resolve(bootstrapName) else staging.resolve("config").resolve(bootstrapName)
        if (bootstrapSource.exists()) {
            directories.bootstrapFile.parent.createDirectories()
            Files.copy(bootstrapSource, directories.bootstrapFile, StandardCopyOption.REPLACE_EXISTING)
        }
        val dataSource = if (fromSafety) staging.resolve("data") else staging.resolve("data")
        if (fromSafety) {
            val safetyData = staging.resolve("data")
            if (safetyData.exists()) replaceDirectory(safetyData, directories.dataRoot)
        } else if (dataSource.exists()) {
            replaceDirectory(dataSource, directories.dataRoot)
        }
    }

    private fun copyLiveSnapshot(directories: AppDirectories, safety: Path) {
        if (directories.settingsFile.exists()) {
            Files.copy(directories.settingsFile, safety.resolve(ProductIdentity.SETTINGS_FILE), StandardCopyOption.REPLACE_EXISTING)
        }
        if (directories.bootstrapFile.exists()) {
            Files.copy(directories.bootstrapFile, safety.resolve(directories.bootstrapFile.fileName), StandardCopyOption.REPLACE_EXISTING)
        }
        val dataSafety = safety.resolve("data")
        if (directories.dataRoot.exists()) {
            copyDirectorySkipping(directories.dataRoot, dataSafety)
        }
    }

    private fun copyDirectorySkipping(source: Path, destination: Path) {
        Files.walk(source).use { stream ->
            stream.forEach { path ->
                val relative = source.relativize(path)
                val top = relative.invariantSeparatorsPathString.substringBefore('/', relative.invariantSeparatorsPathString)
                if (relative.pathString.isEmpty()) return@forEach
                if (top in skippedDataNames) return@forEach
                val target = destination.resolve(relative.toString())
                if (path.isDirectory()) {
                    target.createDirectories()
                } else if (path.isRegularFile() && !path.isSymbolicLink()) {
                    target.parent.createDirectories()
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun replaceDirectory(source: Path, destination: Path) {
        if (destination.exists()) {
            Files.walk(destination).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { path ->
                    if (path == destination) return@forEach
                    val relative = destination.relativize(path).invariantSeparatorsPathString
                    val top = relative.substringBefore('/', relative)
                    if (top in skippedDataNames) return@forEach
                    Files.deleteIfExists(path)
                }
            }
        }
        destination.createDirectories()
        Files.walk(source).use { stream ->
            stream.forEach { path ->
                val relative = source.relativize(path)
                if (relative.pathString.isEmpty()) return@forEach
                val target = destination.resolve(relative.toString())
                if (path.isDirectory()) {
                    target.createDirectories()
                } else if (path.isRegularFile()) {
                    target.parent.createDirectories()
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun normalizeZipPath(raw: String): String {
        val name = raw.replace('\\', '/').trimStart('/')
        if (name.isEmpty() || name.contains("..") || name.startsWith("/") || name.contains(':')) {
            throw BackupException(BackupErrorCode.PATH_ESCAPE, "Illegal zip path: $raw")
        }
        return name
    }

    private fun resolveInside(root: Path, relative: String): Path {
        val normalized = normalizeZipPath(relative)
        val target = root.resolve(normalized).normalize()
        val base = root.toAbsolutePath().normalize()
        if (!target.toAbsolutePath().normalize().startsWith(base)) {
            throw BackupException(BackupErrorCode.PATH_ESCAPE, "Illegal zip path: $relative")
        }
        return target
    }

    private fun checkDestination(directories: AppDirectories, zipPath: Path) {
        val zip = zipPath.toAbsolutePath().normalize()
        listOf(directories.dataRoot, directories.configRoot, directories.cacheRoot).forEach { root ->
            val base = root.toAbsolutePath().normalize()
            if (zip.startsWith(base)) {
                throw BackupException(BackupErrorCode.PATH_ESCAPE, "Backup destination cannot be inside a source directory")
            }
        }
    }

    private fun sha256AndCopy(bytes: ByteArray, write: (ByteArray) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        write(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        stream.use { input ->
            val buffer = ByteArray(16_384)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
