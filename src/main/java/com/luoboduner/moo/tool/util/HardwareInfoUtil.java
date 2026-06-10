package com.luoboduner.moo.tool.util;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 基于 OSHI 采集系统与硬件信息。
 */
public final class HardwareInfoUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private HardwareInfoUtil() {
    }

    public static String[] collectAll() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();

        return new String[]{
                collectSystemInfo(os, hal),
                collectCpuInfo(hal.getProcessor()),
                collectMemoryInfo(hal.getMemory()),
                collectDiskInfo(hal, os),
                collectNetworkInfo(hal)
        };
    }

    private static String collectSystemInfo(OperatingSystem os, HardwareAbstractionLayer hal) {
        StringBuilder sb = new StringBuilder();
        ComputerSystem cs = hal.getComputerSystem();
        OperatingSystem.OSVersionInfo versionInfo = os.getVersionInfo();

        appendSection(sb, I18n.get("hardware.section.os"));
        appendLine(sb, I18n.get("hardware.label.name"), os.toString());
        appendLine(sb, I18n.get("hardware.label.family"), os.getFamily());
        appendLine(sb, I18n.get("hardware.label.manufacturer"), os.getManufacturer());
        appendLine(sb, I18n.get("hardware.label.version"), versionInfo.getVersion());
        appendLine(sb, I18n.get("hardware.label.codeName"), nullToDash(versionInfo.getCodeName()));
        appendLine(sb, I18n.get("hardware.label.buildNumber"), nullToDash(versionInfo.getBuildNumber()));
        appendLine(sb, I18n.get("hardware.label.bootTime"), DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(os.getSystemBootTime())));
        appendLine(sb, I18n.get("hardware.label.uptime"), formatDuration(os.getSystemUptime()));
        appendLine(sb, I18n.get("hardware.label.processCount"), String.valueOf(os.getProcessCount()));
        appendLine(sb, I18n.get("hardware.label.threadCount"), String.valueOf(os.getThreadCount()));

        appendSection(sb, I18n.get("hardware.section.computer"));
        appendLine(sb, I18n.get("hardware.label.manufacturer"), nullToDash(cs.getManufacturer()));
        appendLine(sb, I18n.get("hardware.label.model"), nullToDash(cs.getModel()));
        appendLine(sb, I18n.get("hardware.label.hardwareUuid"), nullToDash(cs.getHardwareUUID()));
        appendLine(sb, I18n.get("hardware.label.serialNumber"), maskIfPresent(cs.getSerialNumber()));

        appendSection(sb, I18n.get("hardware.section.firmware"));
        appendLine(sb, I18n.get("hardware.label.manufacturer"), nullToDash(cs.getFirmware().getManufacturer()));
        appendLine(sb, I18n.get("hardware.label.name"), nullToDash(cs.getFirmware().getName()));
        appendLine(sb, I18n.get("hardware.label.version"), nullToDash(cs.getFirmware().getVersion()));
        appendLine(sb, I18n.get("hardware.label.releaseDate"), nullToDash(cs.getFirmware().getReleaseDate()));

        appendSection(sb, I18n.get("hardware.section.motherboard"));
        appendLine(sb, I18n.get("hardware.label.manufacturer"), nullToDash(cs.getBaseboard().getManufacturer()));
        appendLine(sb, I18n.get("hardware.label.model"), nullToDash(cs.getBaseboard().getModel()));
        appendLine(sb, I18n.get("hardware.label.version"), nullToDash(cs.getBaseboard().getVersion()));
        appendLine(sb, I18n.get("hardware.label.serialNumber"), maskIfPresent(cs.getBaseboard().getSerialNumber()));

        appendSection(sb, I18n.get("hardware.section.runtime"));
        appendLine(sb, I18n.get("hardware.label.javaVersion"), System.getProperty("java.version"));
        appendLine(sb, I18n.get("hardware.label.jvm"), System.getProperty("java.vm.name"));
        appendLine(sb, I18n.get("hardware.label.userHome"), System.getProperty("user.home"));
        appendLine(sb, I18n.get("hardware.label.collectedAt"), DATE_TIME_FORMATTER.format(Instant.now()));

        return sb.toString().trim();
    }

    private static String collectCpuInfo(CentralProcessor processor) {
        StringBuilder sb = new StringBuilder();
        CentralProcessor.ProcessorIdentifier id = processor.getProcessorIdentifier();

        appendSection(sb, I18n.get("hardware.section.processorId"));
        appendLine(sb, I18n.get("hardware.label.name"), id.getName());
        appendLine(sb, I18n.get("hardware.label.manufacturer"), id.getVendor());
        appendLine(sb, I18n.get("hardware.label.family"), id.getFamily());
        appendLine(sb, I18n.get("hardware.label.model"), id.getModel());
        appendLine(sb, I18n.get("hardware.label.stepping"), id.getStepping());
        appendLine(sb, I18n.get("hardware.label.identifier"), id.getIdentifier());
        appendLine(sb, I18n.get("hardware.label.microarchitecture"), nullToDash(id.getMicroarchitecture()));

        appendSection(sb, I18n.get("hardware.section.coresThreads"));
        appendLine(sb, I18n.get("hardware.label.physicalCores"), String.valueOf(processor.getPhysicalProcessorCount()));
        appendLine(sb, I18n.get("hardware.label.logicalCores"), String.valueOf(processor.getLogicalProcessorCount()));
        appendLine(sb, I18n.get("hardware.label.maxFrequency"), formatHertz(processor.getMaxFreq()));
        appendLine(sb, I18n.get("hardware.label.currentFrequency"), formatCurrentFreq(processor.getCurrentFreq()));

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        sleepQuietly(500);
        double systemLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        double[] perCpuLoad = processor.getProcessorCpuLoad(500);

        appendSection(sb, I18n.get("hardware.section.cpuUsage"));
        appendLine(sb, I18n.get("hardware.label.systemCpuUsage"), String.format(Locale.ROOT, "%.1f%%", systemLoad));
        for (int i = 0; i < perCpuLoad.length; i++) {
            appendLine(sb, I18n.format("hardware.label.logicalCoreN", i), String.format(Locale.ROOT, "%.1f%%", perCpuLoad[i] * 100));
        }

        return sb.toString().trim();
    }

    private static String collectMemoryInfo(GlobalMemory memory) {
        StringBuilder sb = new StringBuilder();
        VirtualMemory swap = memory.getVirtualMemory();

        appendSection(sb, I18n.get("hardware.section.physicalMemory"));
        appendLine(sb, I18n.get("hardware.label.total"), formatBytes(memory.getTotal()));
        appendLine(sb, I18n.get("hardware.label.used"), formatBytes(memory.getTotal() - memory.getAvailable()));
        appendLine(sb, I18n.get("hardware.label.available"), formatBytes(memory.getAvailable()));
        appendLine(sb, I18n.get("hardware.label.usage"), String.format(Locale.ROOT, "%.1f%%",
                (memory.getTotal() - memory.getAvailable()) * 100.0 / memory.getTotal()));

        appendSection(sb, I18n.get("hardware.section.swap"));
        appendLine(sb, I18n.get("hardware.label.total"), formatBytes(swap.getSwapTotal()));
        appendLine(sb, I18n.get("hardware.label.used"), formatBytes(swap.getSwapUsed()));
        appendLine(sb, I18n.get("hardware.label.swapPagesIn"), String.valueOf(swap.getSwapPagesIn()));
        appendLine(sb, I18n.get("hardware.label.swapPagesOut"), String.valueOf(swap.getSwapPagesOut()));
        appendLine(sb, I18n.get("hardware.label.virtualMax"), formatBytes(swap.getVirtualMax()));
        appendLine(sb, I18n.get("hardware.label.virtualInUse"), formatBytes(swap.getVirtualInUse()));

        appendSection(sb, I18n.get("hardware.section.memoryModules"));
        for (PhysicalMemory module : memory.getPhysicalMemory()) {
            sb.append('\n');
            appendLine(sb, "  " + I18n.get("hardware.label.slot"), nullToDash(module.getBankLabel()));
            appendLine(sb, "  " + I18n.get("hardware.label.capacity"), formatBytes(module.getCapacity()));
            appendLine(sb, "  " + I18n.get("hardware.label.type"), nullToDash(module.getMemoryType()));
            appendLine(sb, "  " + I18n.get("hardware.label.frequency"), formatHertz(module.getClockSpeed()));
            appendLine(sb, "  " + I18n.get("hardware.label.manufacturer"), nullToDash(module.getManufacturer()));
        }

        return sb.toString().trim();
    }

    private static String collectDiskInfo(HardwareAbstractionLayer hal, OperatingSystem os) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, I18n.get("hardware.section.physicalDisks"));
        for (HWDiskStore disk : hal.getDiskStores()) {
            disk.updateAttributes();
            sb.append('\n');
            appendLine(sb, "  " + I18n.get("hardware.label.name"), disk.getName());
            appendLine(sb, "  " + I18n.get("hardware.label.model"), nullToDash(disk.getModel()));
            appendLine(sb, "  " + I18n.get("hardware.label.serialNumber"), maskIfPresent(disk.getSerial()));
            appendLine(sb, "  " + I18n.get("hardware.label.capacity"), formatBytes(disk.getSize()));
            appendLine(sb, "  " + I18n.get("hardware.label.partitionCount"), String.valueOf(disk.getPartitions().size()));
            appendLine(sb, "  " + I18n.get("hardware.label.readCount"), String.valueOf(disk.getReads()));
            appendLine(sb, "  " + I18n.get("hardware.label.writeCount"), String.valueOf(disk.getWrites()));
            appendLine(sb, "  " + I18n.get("hardware.label.readBytes"), formatBytes(disk.getReadBytes()));
            appendLine(sb, "  " + I18n.get("hardware.label.writeBytes"), formatBytes(disk.getWriteBytes()));
        }

        appendSection(sb, I18n.get("hardware.section.filesystems"));
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            sb.append('\n');
            appendLine(sb, "  " + I18n.get("hardware.label.mountPoint"), fs.getMount());
            appendLine(sb, "  " + I18n.get("hardware.label.volume"), nullToDash(fs.getVolume()));
            appendLine(sb, "  " + I18n.get("hardware.label.type"), nullToDash(fs.getType()));
            appendLine(sb, "  " + I18n.get("hardware.label.description"), nullToDash(fs.getDescription()));
            appendLine(sb, "  " + I18n.get("hardware.label.totalSpace"), formatBytes(fs.getTotalSpace()));
            appendLine(sb, "  " + I18n.get("hardware.label.usableSpace"), formatBytes(fs.getUsableSpace()));
            appendLine(sb, "  " + I18n.get("hardware.label.usedSpace"), formatBytes(fs.getTotalSpace() - fs.getUsableSpace()));
            if (fs.getTotalSpace() > 0) {
                appendLine(sb, "  " + I18n.get("hardware.label.usage"), String.format(Locale.ROOT, "%.1f%%",
                        (fs.getTotalSpace() - fs.getUsableSpace()) * 100.0 / fs.getTotalSpace()));
            }
        }

        return sb.toString().trim();
    }

    private static String collectNetworkInfo(HardwareAbstractionLayer hal) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, I18n.get("hardware.section.networkInterfaces"));
        for (NetworkIF net : hal.getNetworkIFs()) {
            net.updateAttributes();
            sb.append('\n');
            appendLine(sb, "  " + I18n.get("hardware.label.name"), net.getName());
            appendLine(sb, "  " + I18n.get("hardware.label.displayName"), nullToDash(net.getDisplayName()));
            appendLine(sb, "  " + I18n.get("hardware.label.mac"), nullToDash(net.getMacaddr()));
            appendLine(sb, "  " + I18n.get("hardware.label.mtu"), String.valueOf(net.getMTU()));
            appendLine(sb, "  " + I18n.get("hardware.label.ipv4"), Arrays.toString(net.getIPv4addr()));
            appendLine(sb, "  " + I18n.get("hardware.label.ipv6"), Arrays.toString(net.getIPv6addr()));
            appendLine(sb, "  " + I18n.get("hardware.label.status"), net.isKnownVmMacAddr() ? "虚拟 MAC" : "活动");
            appendLine(sb, "  " + I18n.get("hardware.label.speed"), formatBitsPerSecond(net.getSpeed()));
            appendLine(sb, "  " + I18n.get("hardware.label.packetsRecv"), String.valueOf(net.getPacketsRecv()));
            appendLine(sb, "  " + I18n.get("hardware.label.packetsSent"), String.valueOf(net.getPacketsSent()));
            appendLine(sb, "  " + I18n.get("hardware.label.bytesRecv"), formatBytes(net.getBytesRecv()));
            appendLine(sb, "  " + I18n.get("hardware.label.bytesSent"), formatBytes(net.getBytesSent()));
        }

        return sb.toString().trim();
    }

    private static void appendSection(StringBuilder sb, String title) {
        if (!sb.isEmpty()) {
            sb.append('\n');
        }
        sb.append("========== ").append(title).append(" ==========\n");
    }

    private static void appendLine(StringBuilder sb, String key, String value) {
        sb.append(String.format(Locale.ROOT, "%-16s: %s%n", key, value));
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String maskIfPresent(String value) {
        if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value)) {
            return "-";
        }
        if (value.length() <= 4) {
            return value;
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "-";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }

    private static String formatHertz(long hertz) {
        if (hertz <= 0) {
            return "-";
        }
        if (hertz >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.2f GHz", hertz / 1_000_000_000.0);
        }
        if (hertz >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.2f MHz", hertz / 1_000_000.0);
        }
        return hertz + " Hz";
    }

    private static String formatCurrentFreq(long[] freqs) {
        if (freqs == null || freqs.length == 0) {
            return "-";
        }
        long max = Arrays.stream(freqs).max().orElse(0);
        long min = Arrays.stream(freqs).filter(f -> f > 0).min().orElse(0);
        if (max <= 0) {
            return "-";
        }
        if (min > 0 && min != max) {
            return formatHertz(min) + " ~ " + formatHertz(max);
        }
        return formatHertz(max);
    }

    private static String formatBitsPerSecond(long bps) {
        if (bps <= 0) {
            return "-";
        }
        if (bps >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.2f Gbps", bps / 1_000_000_000.0);
        }
        if (bps >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.2f Mbps", bps / 1_000_000.0);
        }
        return bps + " bps";
    }

    private static String formatDuration(long seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        if (days > 0) {
            return String.format(Locale.ROOT, "%d 天 %d 小时 %d 分钟", days, hours, minutes);
        }
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d 小时 %d 分钟", hours, minutes);
        }
        return String.format(Locale.ROOT, "%d 分钟 %d 秒", minutes, secs);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
