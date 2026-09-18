package com.rememberber.mootool.next.compose.domain

/** F11 端口扫描等命令启动前校验（可单测，对齐 `NetEngine.parsePortSpec`）。 */
object NetWiringPresentation {
    sealed interface PortScanStart {
        data class Ready(val target: String, val portSpec: String) : PortScanStart
        data class Blocked(val errorCode: NetworkErrorCode) : PortScanStart
    }

    sealed interface HostCommandStart {
        data class Ready(val target: String) : HostCommandStart
        data class Blocked(val errorCode: NetworkErrorCode) : HostCommandStart
    }

    fun hostCommandStart(raw: String, normalize: (String?) -> String): HostCommandStart {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return HostCommandStart.Blocked(NetworkErrorCode.INVALID_TARGET)
        return try {
            HostCommandStart.Ready(normalize(trimmed))
        } catch (error: NetException) {
            HostCommandStart.Blocked(error.code)
        }
    }

    fun pingStart(target: String): HostCommandStart =
        hostCommandStart(target, NetEngine::normalizeHostTarget)

    fun resolveStart(target: String): HostCommandStart =
        hostCommandStart(target, NetEngine::normalizeHostTarget)

    fun whoisStart(target: String): HostCommandStart =
        hostCommandStart(target, NetEngine::normalizeWhoisTarget)

    fun ipRangeStart(target: String): HostCommandStart =
        hostCommandStart(target, NetEngine::normalizeHostTarget)

    fun stopEnabled(running: Boolean): Boolean = running

    fun outputActionsEnabled(outputNotBlank: Boolean): Boolean = outputNotBlank

    fun runCommandEnabled(idle: Boolean, startReady: Boolean): Boolean = idle && startReady

    /** 无在途网络任务时可触发 flush DNS / 刷新本机地址等瞬时操作。 */
    fun idleUtilityActionEnabled(running: Boolean): Boolean = !running

    fun flushDnsActionEnabled(running: Boolean): Boolean = idleUtilityActionEnabled(running)

    fun refreshLocalAddressesActionEnabled(running: Boolean): Boolean = idleUtilityActionEnabled(running)

    fun ipv4ToLongActionEnabled(ipv4: String): Boolean =
        runCatching {
            NetEngine.ipv4ToLong(ipv4.trim())
            true
        }.getOrDefault(false)

    fun longToIpv4ActionEnabled(longText: String): Boolean =
        runCatching {
            NetEngine.longToIpv4(longText.trim())
            true
        }.getOrDefault(false)

    fun runLocalAddresses(): LocalAddressSnapshot = NetEngine.localAddresses()

    fun portScanStart(target: String, portSpec: String): PortScanStart {
        val trimmedTarget = target.trim()
        val trimmedPorts = portSpec.trim()
        if (trimmedTarget.isEmpty()) return PortScanStart.Blocked(NetworkErrorCode.INVALID_TARGET)
        if (trimmedPorts.isEmpty()) return PortScanStart.Blocked(NetworkErrorCode.INVALID_TARGET)
        return try {
            NetEngine.parsePortSpec(trimmedPorts)
            PortScanStart.Ready(trimmedTarget, trimmedPorts)
        } catch (error: NetException) {
            PortScanStart.Blocked(error.code)
        }
    }

    fun shouldToastNetworkError(errorCode: NetworkErrorCode?): Boolean =
        errorCode != null && errorCode != NetworkErrorCode.ABORTED

    fun shouldToastLocalFailure(): Boolean = true
}
