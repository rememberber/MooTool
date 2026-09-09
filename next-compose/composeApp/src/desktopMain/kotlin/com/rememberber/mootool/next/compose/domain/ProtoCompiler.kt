package com.rememberber.mootool.next.compose.domain

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.util.JsonFormat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet
import java.util.concurrent.TimeUnit

internal object ProtoCompiler {
    const val VERSION = "4.29.3"
    const val MAX_PROTO_BYTES = 1_048_576
    const val TIMEOUT_MS = 8_000L

    private val lock = Any()
    private var cachedExecutable: Path? = null

    fun descriptorFor(protoContent: String, messageName: String): Descriptors.Descriptor {
        if (protoContent.trim().isEmpty()) throw ProtobufException("empty-proto", ".proto definition is required")
        if (messageName.trim().isEmpty()) throw ProtobufException("empty-message", "Message name is required")
        if (protoContent.length > MAX_PROTO_BYTES) {
            throw ProtobufException("too-large", "Proto definition exceeds ${MAX_PROTO_BYTES} bytes")
        }
        val set = DescriptorProtos.FileDescriptorSet.parseFrom(compile(protoContent))
        return findMessage(set, messageName.trim())
    }

    private fun compile(protoContent: String): ByteArray {
        val workDir = Files.createTempDirectory("next-compose-proto")
        try {
            val protoFile = workDir.resolve("message.proto")
            Files.writeString(protoFile, protoContent)
            val descriptorFile = workDir.resolve("descriptor.pb")
            val command = listOf(
                resolveExecutable().toAbsolutePath().toString(),
                "--proto_path=${workDir.toAbsolutePath()}",
                "--descriptor_set_out=${descriptorFile.toAbsolutePath()}",
                "--include_imports",
                protoFile.fileName.toString()
            )
            val process = ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw ProtobufException("compile", "protoc timed out after ${TIMEOUT_MS}ms")
            }
            if (process.exitValue() != 0) {
                throw ProtobufException("compile", output.ifBlank { "protoc failed with exit ${process.exitValue()}" })
            }
            if (!Files.exists(descriptorFile) || Files.size(descriptorFile) == 0L) {
                throw ProtobufException("compile", "protoc produced an empty descriptor set")
            }
            return Files.readAllBytes(descriptorFile)
        } finally {
            workDir.toFile().deleteRecursively()
        }
    }

    private fun findMessage(set: DescriptorProtos.FileDescriptorSet, messageName: String): Descriptors.Descriptor {
        val files = LinkedHashMap<String, Descriptors.FileDescriptor>()
        for (file in set.fileList) {
            val deps = Array(file.dependencyCount) { index ->
                files[file.getDependency(index)]
                    ?: throw ProtobufException("compile", "Missing import '${file.getDependency(index)}'")
            }
            val descriptor = Descriptors.FileDescriptor.buildFrom(file, deps)
            files[file.name] = descriptor
        }
        val simple = messageName.substringAfterLast('.')
        for (file in files.values) {
            search(file.messageTypes, messageName, simple)?.let { return it }
        }
        throw ProtobufException("missing-message", "Message not found: $messageName")
    }

    private fun search(
        types: List<Descriptors.Descriptor>,
        messageName: String,
        simple: String
    ): Descriptors.Descriptor? {
        for (type in types) {
            if (type.name == messageName || type.fullName == messageName || type.name == simple &&
                (messageName == type.name || messageName.endsWith(".${type.name}"))
            ) {
                return type
            }
            search(type.nestedTypes, messageName, simple)?.let { return it }
        }
        return null
    }

    private fun resolveExecutable(): Path = synchronized(lock) {
        cachedExecutable?.takeIf { Files.isRegularFile(it) }?.let { return it }
        val resource = bundledResourceName()
        val stream = ProtoCompiler::class.java.getResourceAsStream(resource)
            ?: throw ProtobufException("compiler", "Bundled protoc not found ($resource)")
        val dir = Path.of(System.getProperty("java.io.tmpdir"), "next-compose-protoc-$VERSION")
        Files.createDirectories(dir)
        val windows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
        val exe = dir.resolve(if (windows) "protoc.exe" else "protoc")
        stream.use { Files.copy(it, exe, StandardCopyOption.REPLACE_EXISTING) }
        if (!windows) {
            runCatching {
                Files.setPosixFilePermissions(
                    exe,
                    EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE
                    )
                )
            }
        }
        cachedExecutable = exe
        exe
    }

    private fun bundledResourceName(): String {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val arch = System.getProperty("os.arch").orEmpty().lowercase()
        val classifier = when {
            os.contains("mac") && (arch == "aarch64" || arch == "arm64") -> "osx-aarch_64"
            os.contains("mac") -> "osx-x86_64"
            os.contains("win") -> "windows-x86_64"
            arch == "aarch64" || arch == "arm64" -> "linux-aarch_64"
            else -> "linux-x86_64"
        }
        return "/helpers/protoc-$classifier"
    }
}

internal val protobufJsonPrinter: JsonFormat.Printer =
    JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames()

internal val protobufJsonParser: JsonFormat.Parser =
    JsonFormat.parser().ignoringUnknownFields()

internal fun parseDynamic(descriptor: Descriptors.Descriptor, json: String): DynamicMessage {
    val builder = DynamicMessage.newBuilder(descriptor)
    try {
        protobufJsonParser.merge(json, builder)
    } catch (error: InvalidProtocolBufferException) {
        throw ProtobufException("json", error.message ?: "Invalid JSON for message")
    } catch (error: IllegalArgumentException) {
        throw ProtobufException("json", error.message ?: "Invalid JSON for message")
    }
    return builder.build()
}
