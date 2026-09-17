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
}
