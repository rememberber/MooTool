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

        appendSection(sb, "操作系统");
        appendLine(sb, "名称", os.toString());
        appendLine(sb, "家族", os.getFamily());
        appendLine(sb, "制造商", os.getManufacturer());
        appendLine(sb, "版本", versionInfo.getVersion());
        appendLine(sb, "代号", nullToDash(versionInfo.getCodeName()));
        appendLine(sb, "构建号", nullToDash(versionInfo.getBuildNumber()));
        appendLine(sb, "启动时间", DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(os.getSystemBootTime())));
        appendLine(sb, "运行时长", formatDuration(os.getSystemUptime()));
        appendLine(sb, "进程数", String.valueOf(os.getProcessCount()));
        appendLine(sb, "线程数", String.valueOf(os.getThreadCount()));

        appendSection(sb, "计算机");
        appendLine(sb, "制造商", nullToDash(cs.getManufacturer()));
        appendLine(sb, "型号", nullToDash(cs.getModel()));
        appendLine(sb, "硬件 UUID", nullToDash(cs.getHardwareUUID()));
        appendLine(sb, "序列号", maskIfPresent(cs.getSerialNumber()));

        appendSection(sb, "固件");
        appendLine(sb, "制造商", nullToDash(cs.getFirmware().getManufacturer()));
        appendLine(sb, "名称", nullToDash(cs.getFirmware().getName()));
        appendLine(sb, "版本", nullToDash(cs.getFirmware().getVersion()));
        appendLine(sb, "发布日期", nullToDash(cs.getFirmware().getReleaseDate()));

        appendSection(sb, "主板");
        appendLine(sb, "制造商", nullToDash(cs.getBaseboard().getManufacturer()));
        appendLine(sb, "型号", nullToDash(cs.getBaseboard().getModel()));
        appendLine(sb, "版本", nullToDash(cs.getBaseboard().getVersion()));
        appendLine(sb, "序列号", maskIfPresent(cs.getBaseboard().getSerialNumber()));

        appendSection(sb, "运行时");
        appendLine(sb, "Java 版本", System.getProperty("java.version"));
        appendLine(sb, "JVM", System.getProperty("java.vm.name"));
        appendLine(sb, "用户目录", System.getProperty("user.home"));
        appendLine(sb, "采集时间", DATE_TIME_FORMATTER.format(Instant.now()));

        return sb.toString().trim();
    }

    private static String collectCpuInfo(CentralProcessor processor) {
        StringBuilder sb = new StringBuilder();
        CentralProcessor.ProcessorIdentifier id = processor.getProcessorIdentifier();

        appendSection(sb, "处理器标识");
        appendLine(sb, "名称", id.getName());
        appendLine(sb, "制造商", id.getVendor());
        appendLine(sb, "家族", id.getFamily());
        appendLine(sb, "型号", id.getModel());
        appendLine(sb, "步进", id.getStepping());
        appendLine(sb, "标识", id.getIdentifier());
        appendLine(sb, "微架构", nullToDash(id.getMicroarchitecture()));

        appendSection(sb, "核心与线程");
        appendLine(sb, "物理核心", String.valueOf(processor.getPhysicalProcessorCount()));
        appendLine(sb, "逻辑核心", String.valueOf(processor.getLogicalProcessorCount()));
        appendLine(sb, "最大频率", formatHertz(processor.getMaxFreq()));
        appendLine(sb, "当前频率", formatCurrentFreq(processor.getCurrentFreq()));

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        sleepQuietly(500);
        double systemLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        double[] perCpuLoad = processor.getProcessorCpuLoad(500);

        appendSection(sb, "CPU 使用率");
        appendLine(sb, "系统总使用率", String.format(Locale.ROOT, "%.1f%%", systemLoad));
        for (int i = 0; i < perCpuLoad.length; i++) {
            appendLine(sb, "逻辑核心 " + i, String.format(Locale.ROOT, "%.1f%%", perCpuLoad[i] * 100));
        }

        return sb.toString().trim();
    }

    private static String collectMemoryInfo(GlobalMemory memory) {
        StringBuilder sb = new StringBuilder();
        VirtualMemory swap = memory.getVirtualMemory();

        appendSection(sb, "物理内存");
        appendLine(sb, "总量", formatBytes(memory.getTotal()));
        appendLine(sb, "已用", formatBytes(memory.getTotal() - memory.getAvailable()));
        appendLine(sb, "可用", formatBytes(memory.getAvailable()));
        appendLine(sb, "使用率", String.format(Locale.ROOT, "%.1f%%",
                (memory.getTotal() - memory.getAvailable()) * 100.0 / memory.getTotal()));

        appendSection(sb, "交换区");
        appendLine(sb, "总量", formatBytes(swap.getSwapTotal()));
        appendLine(sb, "已用", formatBytes(swap.getSwapUsed()));
        appendLine(sb, "交换页入", String.valueOf(swap.getSwapPagesIn()));
        appendLine(sb, "交换页出", String.valueOf(swap.getSwapPagesOut()));
        appendLine(sb, "虚拟内存上限", formatBytes(swap.getVirtualMax()));
        appendLine(sb, "虚拟内存已用", formatBytes(swap.getVirtualInUse()));

        appendSection(sb, "内存条");
        for (PhysicalMemory module : memory.getPhysicalMemory()) {
            sb.append('\n');
            appendLine(sb, "  插槽", nullToDash(module.getBankLabel()));
            appendLine(sb, "  容量", formatBytes(module.getCapacity()));
            appendLine(sb, "  类型", nullToDash(module.getMemoryType()));
            appendLine(sb, "  频率", formatHertz(module.getClockSpeed()));
            appendLine(sb, "  制造商", nullToDash(module.getManufacturer()));
        }

        return sb.toString().trim();
    }

    private static String collectDiskInfo(HardwareAbstractionLayer hal, OperatingSystem os) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, "物理磁盘");
        for (HWDiskStore disk : hal.getDiskStores()) {
            disk.updateAttributes();
            sb.append('\n');
            appendLine(sb, "  名称", disk.getName());
            appendLine(sb, "  型号", nullToDash(disk.getModel()));
            appendLine(sb, "  序列号", maskIfPresent(disk.getSerial()));
            appendLine(sb, "  容量", formatBytes(disk.getSize()));
            appendLine(sb, "  分区数", String.valueOf(disk.getPartitions().size()));
            appendLine(sb, "  读取次数", String.valueOf(disk.getReads()));
            appendLine(sb, "  写入次数", String.valueOf(disk.getWrites()));
            appendLine(sb, "  读取量", formatBytes(disk.getReadBytes()));
            appendLine(sb, "  写入量", formatBytes(disk.getWriteBytes()));
        }

        appendSection(sb, "文件系统");
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            sb.append('\n');
            appendLine(sb, "  挂载点", fs.getMount());
            appendLine(sb, "  卷标", nullToDash(fs.getVolume()));
            appendLine(sb, "  类型", nullToDash(fs.getType()));
            appendLine(sb, "  描述", nullToDash(fs.getDescription()));
            appendLine(sb, "  总空间", formatBytes(fs.getTotalSpace()));
            appendLine(sb, "  可用空间", formatBytes(fs.getUsableSpace()));
            appendLine(sb, "  已用空间", formatBytes(fs.getTotalSpace() - fs.getUsableSpace()));
            if (fs.getTotalSpace() > 0) {
                appendLine(sb, "  使用率", String.format(Locale.ROOT, "%.1f%%",
                        (fs.getTotalSpace() - fs.getUsableSpace()) * 100.0 / fs.getTotalSpace()));
            }
        }

        return sb.toString().trim();
    }

    private static String collectNetworkInfo(HardwareAbstractionLayer hal) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, "网络接口");
        for (NetworkIF net : hal.getNetworkIFs()) {
            net.updateAttributes();
            sb.append('\n');
            appendLine(sb, "  名称", net.getName());
            appendLine(sb, "  显示名", nullToDash(net.getDisplayName()));
            appendLine(sb, "  MAC", nullToDash(net.getMacaddr()));
            appendLine(sb, "  MTU", String.valueOf(net.getMTU()));
            appendLine(sb, "  IPv4", Arrays.toString(net.getIPv4addr()));
            appendLine(sb, "  IPv6", Arrays.toString(net.getIPv6addr()));
            appendLine(sb, "  状态", net.isKnownVmMacAddr() ? "虚拟 MAC" : "活动");
            appendLine(sb, "  速率", formatBitsPerSecond(net.getSpeed()));
            appendLine(sb, "  接收包", String.valueOf(net.getPacketsRecv()));
            appendLine(sb, "  发送包", String.valueOf(net.getPacketsSent()));
            appendLine(sb, "  接收量", formatBytes(net.getBytesRecv()));
            appendLine(sb, "  发送量", formatBytes(net.getBytesSent()));
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
