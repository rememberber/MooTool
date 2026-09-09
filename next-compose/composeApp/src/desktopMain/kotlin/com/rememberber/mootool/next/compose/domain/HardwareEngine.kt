package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.ProductIdentity
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.HWDiskStore
import oshi.hardware.NetworkIF
import oshi.software.os.OSFileStore
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class HardwareTab { System, Cpu, Memory, Storage, Network }

data class HardwareItem(
    val labelKey: String,
    val value: String,
    val sensitive: Boolean = false
)

data class HardwareGroup(
    val titleKey: String,
    val items: List<HardwareItem>
)

data class HardwareSnapshot(
    val collectedAt: Instant,
    val sections: Map<HardwareTab, List<HardwareGroup>>
)

class HardwareException(val code: String, message: String) : Exception(message)

object HardwareEngine {
    private val systemInfo by lazy { SystemInfo() }

    fun mask(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.equals("unknown", ignoreCase = true) || trimmed == "n/a") return "-"
        return if (trimmed.length <= 4) trimmed else "${trimmed.take(2)}****${trimmed.takeLast(2)}"
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "-"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index += 1
        }
        return String.format(Locale.US, "%.2f %s", value, units[index])
    }

    fun formatDuration(seconds: Long): String {
        if (seconds < 0) return "-"
        val days = seconds / 86_400
        val hours = (seconds % 86_400) / 3_600
        val minutes = (seconds % 3_600) / 60
        return "${days}d ${hours}h ${minutes}m"
    }

    fun formatGHz(hertz: Long): String {
        if (hertz <= 0) return "-"
        return String.format(Locale.US, "%.2f GHz", hertz / 1_000_000_000.0)
    }

    fun displayValue(item: HardwareItem, revealSensitive: Boolean): String =
        if (item.sensitive && !revealSensitive) mask(item.value) else item.value.ifBlank { "-" }

    fun plainText(snapshot: HardwareSnapshot, tab: HardwareTab, revealSensitive: Boolean, translate: (String) -> String): String {
        val groups = snapshot.sections[tab].orEmpty()
        return groups.joinToString("\n") { group ->
            val header = "========== ${translate(group.titleKey)} =========="
            val lines = group.items.map { "${translate(it.labelKey)}: ${displayValue(it, revealSensitive)}" }
            (listOf(header) + lines + listOf("")).joinToString("\n")
        }
    }

    fun collect(loadSampleMs: Long = 200): HardwareSnapshot {
        val hardware = systemInfo.hardware
        val os = systemInfo.operatingSystem
        val processor = hardware.processor
        val memory = hardware.memory
        val computer = hardware.computerSystem
        val version = os.versionInfo
        val collectedAt = Instant.now()
        val load = cpuLoad(processor, loadSampleMs)
        val physical = runCatching { processor.physicalProcessors }.getOrDefault(emptyList())
        val performance = physical.count { it.efficiency == 0 }
        val efficiency = physical.count { it.efficiency > 0 }
        val disks = runCatching { hardware.diskStores }.getOrDefault(emptyList())
        val fileStores = runCatching { os.fileSystem.fileStores }.getOrDefault(emptyList())
        val networks = runCatching {
            hardware.networkIFs.also { ifs -> ifs.forEach { it.updateAttributes() } }
        }.getOrDefault(emptyList())
        return HardwareSnapshot(
            collectedAt = collectedAt,
            sections = mapOf(
                HardwareTab.System to listOf(
                    group(
                        "hardware.group.operatingSystem",
                        item("hardware.label.platform", os.family),
                        item("hardware.label.distribution", "${os.family} ${version.version}".trim()),
                        item("hardware.label.kernel", version.buildNumber),
                        item("hardware.label.architecture", System.getProperty("os.arch")),
                        item("hardware.label.hostName", os.networkParams.hostName),
                        HardwareItem("hardware.label.serial", computer.serialNumber.orEmpty().ifBlank { "-" }, sensitive = true),
                        item("hardware.label.manufacturer", computer.manufacturer),
                        item("hardware.label.model", computer.model),
                        item("hardware.label.uptime", formatDuration(os.systemUptime)),
                        item("hardware.label.timeZone", ZoneId.systemDefault().id)
                    ),
                    group(
                        "hardware.group.application",
                        item("hardware.label.product", ProductIdentity.DISPLAY_NAME),
                        item("hardware.label.version", ProductIdentity.VERSION),
                        item("hardware.label.javaVersion", System.getProperty("java.version")),
                        item("hardware.label.jvm", System.getProperty("java.vm.name")),
                        item("hardware.label.javaHome", System.getProperty("java.home"))
                    )
                ),
                HardwareTab.Cpu to listOf(
                    group(
                        "hardware.group.processor",
                        item("hardware.label.manufacturer", processor.processorIdentifier.vendor),
                        item("hardware.label.brand", processor.processorIdentifier.name),
                        item("hardware.label.vendor", processor.processorIdentifier.vendor),
                        item("hardware.label.family", processor.processorIdentifier.family),
                        item("hardware.label.model", processor.processorIdentifier.model),
                        item("hardware.label.physicalCores", processor.physicalProcessorCount.toString()),
                        item("hardware.label.logicalCores", processor.logicalProcessorCount.toString()),
                        item("hardware.label.performanceCores", if (physical.isEmpty()) "-" else performance.toString()),
                        item("hardware.label.efficiencyCores", if (physical.isEmpty() || efficiency == 0) "-" else efficiency.toString()),
                        item("hardware.label.baseSpeed", formatGHz(processor.maxFreq)),
                        item("hardware.label.maximumSpeed", formatGHz(processor.currentFreq.maxOrNull() ?: 0)),
                        item("hardware.label.currentLoad", load)
                    )
                ),
                HardwareTab.Memory to memoryGroup(memory),
                HardwareTab.Storage to storageGroups(disks, fileStores),
                HardwareTab.Network to networkGroups(networks)
            )
        )
    }

    private fun memoryGroup(memory: oshi.hardware.GlobalMemory): List<HardwareGroup> {
        val total = memory.total
        val available = memory.available
        val used = (total - available).coerceAtLeast(0)
        val swap = memory.virtualMemory
        return listOf(
            group(
                "hardware.group.physicalMemory",
                item("hardware.label.total", formatBytes(total)),
                item("hardware.label.used", formatBytes(used)),
                item("hardware.label.available", formatBytes(available)),
                item("hardware.label.active", "-"),
                item("hardware.label.usage", if (total > 0) String.format(Locale.US, "%.1f%%", used * 100.0 / total) else "-"),
                item("hardware.label.swapTotal", formatBytes(swap.swapTotal)),
                item("hardware.label.swapUsed", formatBytes(swap.swapUsed))
            )
        )
    }

    private fun storageGroups(disks: List<HWDiskStore>, fileStores: List<OSFileStore>): List<HardwareGroup> {
        val namedDisks = disks.map { disk ->
            HardwareGroup(
                titleKey = disk.model.ifBlank { disk.name.ifBlank { "Disk" } },
                items = listOf(
                    item("hardware.label.device", disk.name),
                    item("hardware.label.model", disk.model),
                    HardwareItem("hardware.label.serial", disk.serial.orEmpty().ifBlank { "-" }, sensitive = true),
                    item("hardware.label.capacity", formatBytes(disk.size))
                )
            )
        }
        val fsGroups = fileStores.map { store ->
            val used = (store.totalSpace - store.usableSpace).coerceAtLeast(0)
            HardwareGroup(
                titleKey = store.mount.ifBlank { store.name.ifBlank { store.type } },
                items = listOf(
                    item("hardware.label.filesystem", store.name),
                    item("hardware.label.type", store.type),
                    item("hardware.label.total", formatBytes(store.totalSpace)),
                    item("hardware.label.used", formatBytes(used)),
                    item("hardware.label.available", formatBytes(store.usableSpace)),
                    item("hardware.label.usage", if (store.totalSpace > 0)
                        String.format(Locale.US, "%.1f%%", used * 100.0 / store.totalSpace)
                    else "-")
                )
            )
        }
        return (namedDisks + fsGroups).ifEmpty {
            listOf(group("hardware.group.storage", item("hardware.label.status", "-")))
        }
    }

    private fun networkGroups(networks: List<NetworkIF>): List<HardwareGroup> {
        if (networks.isEmpty()) {
            return listOf(group("hardware.group.network", item("hardware.label.status", "-")))
        }
        return networks.map { nic ->
            val speed = nic.speed
            HardwareGroup(
                titleKey = nic.displayName.ifBlank { nic.name },
                items = listOf(
                    item("hardware.label.interface", nic.name),
                    item("hardware.label.type", nic.ifType.toString().takeIf { it != "0" } ?: "-"),
                    item("hardware.label.ipv4", nic.iPv4addr.firstOrNull()),
                    item("hardware.label.ipv6", nic.iPv6addr.firstOrNull()),
                    item("hardware.label.mac", nic.macaddr),
                    item("hardware.label.mtu", nic.mtu.takeIf { it > 0 }?.toString()),
                    item("hardware.label.speed", if (speed > 0) "${speed / 1_000_000} Mbps" else "-"),
                    item("hardware.label.status", nic.ifOperStatus?.toString() ?: "-"),
                    item("hardware.label.received", formatBytes(nic.bytesRecv)),
                    item("hardware.label.sent", formatBytes(nic.bytesSent))
                )
            )
        }
    }

    private fun cpuLoad(processor: CentralProcessor, sampleMs: Long): String {
        val previous = processor.systemCpuLoadTicks
        try {
            TimeUnit.MILLISECONDS.sleep(sampleMs.coerceIn(50, 2_000))
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return "-"
        }
        val load = processor.getSystemCpuLoadBetweenTicks(previous) * 100
        return if (load.isNaN() || load < 0) "-" else String.format(Locale.US, "%.1f%%", load)
    }

    private fun group(titleKey: String, vararg items: HardwareItem): HardwareGroup =
        HardwareGroup(titleKey, items.filter { it.value.isNotBlank() && it.value != "undefined" })

    private fun item(labelKey: String, value: String?): HardwareItem =
        HardwareItem(labelKey, if (value.isNullOrBlank()) "-" else value)
}
