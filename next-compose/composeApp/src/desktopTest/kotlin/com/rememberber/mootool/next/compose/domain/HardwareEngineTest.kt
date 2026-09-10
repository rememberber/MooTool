package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HardwareEngineTest {
    @Test
    fun masksSerialsAndFormatsUnits() {
        assertEquals("-", HardwareEngine.mask(""))
        assertEquals("-", HardwareEngine.mask("unknown"))
        assertEquals("AB12", HardwareEngine.mask("AB12"))
        assertEquals("AB****EF", HardwareEngine.mask("ABCDEF"))
        assertEquals("1.00 KB", HardwareEngine.formatBytes(1024))
        assertEquals("0.00 B", HardwareEngine.formatBytes(0))
        assertEquals("-", HardwareEngine.formatBytes(-1))
        assertEquals("1d 2h 3m", HardwareEngine.formatDuration(93780))
        assertEquals("2.00 GHz", HardwareEngine.formatGHz(2_000_000_000))
        val item = HardwareItem("hardware.label.serial", "ABCDEF", sensitive = true)
        assertEquals("AB****EF", HardwareEngine.displayValue(item, revealSensitive = false))
        assertEquals("ABCDEF", HardwareEngine.displayValue(item, revealSensitive = true))
    }

    @Test
    fun collectsLiveOsCpuAndMemoryWithoutFillingZeros() {
        val snapshot = HardwareEngine.collect(loadSampleMs = 80)
        val system = snapshot.sections.getValue(HardwareTab.System)
        val os = system.first { it.titleKey == "hardware.group.operatingSystem" }
        val host = os.items.first { it.labelKey == "hardware.label.hostName" }.value
        assertTrue(host.isNotBlank() && host != "0")
        val serial = os.items.first { it.labelKey == "hardware.label.serial" }
        assertTrue(serial.sensitive)
        assertEquals(HardwareEngine.mask(serial.value), HardwareEngine.displayValue(serial, false))
        val application = system.first { it.titleKey == "hardware.group.application" }
        assertTrue(application.items.any { it.labelKey == "hardware.label.product" && it.value.contains("Compose") })
        val cpu = snapshot.sections.getValue(HardwareTab.Cpu).first()
        val logical = cpu.items.first { it.labelKey == "hardware.label.logicalCores" }.value.toInt()
        assertTrue(logical >= 1)
        val memory = snapshot.sections.getValue(HardwareTab.Memory).first()
        assertTrue(memory.items.first { it.labelKey == "hardware.label.total" }.value != "0.00 B")
        assertTrue(memory.items.none { it.value == "0" && it.labelKey == "hardware.label.total" })
        assertTrue(snapshot.sections.getValue(HardwareTab.Storage).isNotEmpty())
        assertTrue(snapshot.sections.getValue(HardwareTab.Network).isNotEmpty())
        val text = HardwareEngine.plainText(snapshot, HardwareTab.System, revealSensitive = false) { it }
        assertTrue(text.contains("hardware.group.operatingSystem"))
        assertTrue(!text.contains(serial.value) || serial.value.length <= 4 || serial.value == "-")
    }
}
