package com.rememberber.mootool.next.compose.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class NetworkAction {
    Interfaces, Connections, Ping, PingRange, PortScan, FlushDns, Resolve, Whois
}

enum class NetworkErrorCode {
    ABORTED, TIMEOUT, PERMISSION, UNSUPPORTED, COMMAND_FAILED, INVALID_TARGET
}

data class LocalAddressSnapshot(
    val ipv4: List<String>,
    val ipv6: List<String>
)

data class NetworkCommandResult(
    val action: NetworkAction,
    val output: String,
    val durationMs: Long,
    val errorCode: NetworkErrorCode? = null
)

class NetException(val code: NetworkErrorCode, message: String) : Exception(message)

class NetConvertException(message: String) : Exception(message)

class NetCommandHandle {
    private val cancelled = AtomicBoolean(false)
    private val processes = CopyOnWriteArrayList<Process>()
    private val sockets = CopyOnWriteArrayList<Socket>()

    fun cancel() {
        cancelled.set(true)
        processes.forEach { runCatching { it.destroyForcibly() } }
        sockets.forEach { runCatching { it.close() } }
    }

    fun isCancelled(): Boolean = cancelled.get()

    internal fun register(process: Process) {
        processes.add(process)
        if (cancelled.get()) runCatching { process.destroyForcibly() }
    }

    internal fun register(socket: Socket) {
        sockets.add(socket)
        if (cancelled.get()) runCatching { socket.close() }
    }
}

object NetEngine {
    const val MAX_OUTPUT_BYTES = 2 * 1024 * 1024
    const val MAX_CUSTOM_PORTS = 4096
    const val IP_RANGE_CONCURRENCY = 32
    const val PORT_SCAN_CONCURRENCY = 96

    val commonPorts: Map<Int, String> = linkedMapOf(
        20 to "ftp-data", 21 to "ftp", 22 to "ssh", 23 to "telnet", 25 to "smtp", 53 to "dns",
        67 to "dhcp", 68 to "dhcp", 69 to "tftp", 80 to "http", 110 to "pop3", 123 to "ntp",
        135 to "msrpc", 139 to "netbios", 143 to "imap", 161 to "snmp", 389 to "ldap", 443 to "https",
        445 to "smb", 465 to "smtps", 587 to "smtp-submission", 631 to "ipp", 636 to "ldaps",
        873 to "rsync", 993 to "imaps", 995 to "pop3s", 1080 to "socks", 1433 to "mssql",
        1521 to "oracle", 2049 to "nfs", 2181 to "zookeeper", 2375 to "docker", 3000 to "http-alt",
        3306 to "mysql", 3389 to "rdp", 5432 to "postgresql", 5601 to "kibana", 5672 to "amqp",
        5900 to "vnc", 6379 to "redis", 8080 to "http-alt", 8081 to "http-alt", 8443 to "https-alt",
        8888 to "http-alt", 9092 to "kafka", 9200 to "elasticsearch", 9300 to "elasticsearch",
        11211 to "memcached", 27017 to "mongodb"
    )

    fun ipv4ToLong(value: String): Long {
        val parts = value.trim().split('.')
        if (parts.size != 4 || parts.any { !it.matches(Regex("""^\d{1,3}$""")) || it.toInt() > 255 }) {
            throw NetConvertException("Invalid IPv4 address")
        }
        return parts.fold(0L) { acc, part -> acc * 256 + part.toInt() }
    }

    fun longToIpv4(value: String): String {
        val number = value.trim().toDoubleOrNull()
            ?: throw NetConvertException("Invalid IPv4 number")
        if (number % 1.0 != 0.0 || number < 0 || number > 0xffffffffL.toDouble()) {
            throw NetConvertException("Invalid IPv4 number")
        }
        return longToIpv4(number.toLong())
    }

    fun longToIpv4(value: Long): String {
        if (value < 0 || value > 0xffffffffL) throw NetConvertException("Invalid IPv4 number")
        return listOf(24, 16, 8, 0).joinToString(".") { shift ->
            ((value shr shift) and 0xffL).toString()
        }
    }

    fun parseIpv4Range(value: String?): List<String> {
        val match = value?.trim()?.let { IPV4_RANGE.matchEntire(it) }
            ?: throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        val octets = match.destructured.toList().map { it.toInt() }
        if (octets.any { it > 255 }) throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        val prefix = octets.joinToString(".")
        return (1..254).map { "$prefix.$it" }
    }

    fun parsePortSpec(value: String?): List<Int> {
        val input = value?.trim().orEmpty()
        if (input.isEmpty()) return commonPorts.keys.toList()
        val ports = linkedSetOf<Int>()
        for (rawToken in input.split(',')) {
            val token = rawToken.trim()
            val range = PORT_RANGE.matchEntire(token)
            if (range != null) {
                val start = validPort(range.groupValues[1])
                val end = validPort(range.groupValues[2])
                if (start > end || end - start + 1 > MAX_CUSTOM_PORTS) {
                    throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
                }
                for (port in start..end) ports.add(port)
            } else {
                ports.add(validPort(token))
            }
            if (ports.size > MAX_CUSTOM_PORTS) throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        }
        return ports.sorted()
    }

    fun localAddresses(): LocalAddressSnapshot {
        val ipv4 = linkedSetOf<String>()
        val ipv6 = linkedSetOf<String>()
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().forEach { nic ->
            nic.inetAddresses.toList().forEach { address ->
                when (address) {
                    is Inet4Address -> ipv4.add(address.hostAddress)
                    is Inet6Address -> ipv6.add(address.hostAddress)
                }
            }
        }
        return LocalAddressSnapshot(ipv4.sorted(), ipv6.sorted())
    }

    fun interfacesCommandLabel(): String = when (osFamily()) {
        OsFamily.Windows -> "ipconfig /all"
        OsFamily.Mac -> "ifconfig"
        OsFamily.Linux -> "ip address"
    }

    fun clampTimeout(value: Int): Int = value.coerceIn(1_000, 120_000)

    suspend fun run(
        action: NetworkAction,
        target: String? = null,
        ports: String? = null,
        timeoutMs: Int = 30_000,
        handle: NetCommandHandle = NetCommandHandle(),
        onChunk: (String) -> Unit = {}
    ): NetworkCommandResult {
        val started = System.currentTimeMillis()
        val timeout = clampTimeout(timeoutMs)
        return withContext(Dispatchers.IO) {
            try {
                val output = execute(action, target, ports, timeout, handle, onChunk)
                val text = output.trim()
                NetworkCommandResult(action, text, System.currentTimeMillis() - started)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val classified = classify(error)
                NetworkCommandResult(
                    action = action,
                    output = error.message?.takeIf { it.isNotBlank() } ?: classified.name,
                    durationMs = System.currentTimeMillis() - started,
                    errorCode = classified
                )
            }
        }
    }

    private suspend fun execute(
        action: NetworkAction,
        target: String?,
        ports: String?,
        timeoutMs: Int,
        handle: NetCommandHandle,
        onChunk: (String) -> Unit
    ): String {
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        return when (action) {
            NetworkAction.Resolve -> resolve(target, handle)
            NetworkAction.Whois -> queryWhois(normalizeWhoisTarget(target), timeoutMs, handle, onChunk)
            NetworkAction.PingRange -> scanIpRange(target, timeoutMs, handle)
            NetworkAction.PortScan -> scanPorts(normalizeHostTarget(target), ports, timeoutMs, handle)
            NetworkAction.Interfaces, NetworkAction.Connections, NetworkAction.Ping, NetworkAction.FlushDns -> {
                val command = networkCommand(action, target)
                spawn(command.first, command.second, timeoutMs, handle, onChunk)
            }
        }
    }

    internal fun normalizeHostTarget(value: String?): String {
        val target = value?.trim().orEmpty()
        if (target.isEmpty() || target.length > 253 || !HOST_PATTERN.matches(target)) {
            throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        }
        return target
    }

    internal fun normalizeWhoisTarget(value: String?): String {
        val target = value?.trim()?.lowercase().orEmpty()
        if (target.isEmpty() || target.length > 253 || !WHOIS_PATTERN.matches(target)) {
            throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        }
        return target
    }

    private fun resolve(target: String?, handle: NetCommandHandle): String {
        val host = normalizeHostTarget(target)
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (error: UnknownHostException) {
            throw NetException(NetworkErrorCode.COMMAND_FAILED, error.message ?: host)
        }
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        if (addresses.isEmpty()) throw NetException(NetworkErrorCode.COMMAND_FAILED, "No addresses")
        return addresses.joinToString("\n") { address ->
            val family = if (address is Inet6Address) "IPv6" else "IPv4"
            "${address.hostAddress}\t$family"
        }
    }

    private suspend fun scanIpRange(value: String?, timeoutMs: Int, handle: NetCommandHandle): String {
        val addresses = parseIpv4Range(value)
        val deadline = System.currentTimeMillis() + timeoutMs
        val reachable = concurrentMap(addresses, IP_RANGE_CONCURRENCY, handle, deadline) { address ->
            pingOnce(address, minOf(timeoutMs, 800), handle)
        }.filter { it.reachable }.map { it.address }
        return buildString {
            append("Reachable hosts: ${reachable.size} / ${addresses.size}\n\n")
            if (reachable.isEmpty()) append("No reachable host found") else append(reachable.joinToString("\n"))
        }
    }

    private suspend fun scanPorts(host: String, portSpec: String?, timeoutMs: Int, handle: NetCommandHandle): String {
        val ports = parsePortSpec(portSpec)
        val deadline = System.currentTimeMillis() + timeoutMs
        val results = concurrentMap(ports, PORT_SCAN_CONCURRENCY, handle, deadline) { port ->
            port to probePort(host, port, minOf(timeoutMs, 500), handle)
        }
        val openPorts = results.filter { it.second }.map { it.first }
        val lines = openPorts.map { port ->
            val service = commonPorts[port]
            "$port/tcp open${if (service != null) " $service" else ""}"
        }
        return buildString {
            append("Open TCP ports on $host: ${openPorts.size} / ${ports.size}\n\n")
            if (lines.isEmpty()) append("No open TCP port found") else append(lines.joinToString("\n"))
        }
    }

    private fun pingOnce(address: String, timeoutMs: Int, handle: NetCommandHandle): PingOnceResult {
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        val command = pingOnceCommand(address, timeoutMs)
        return try {
            val process = startProcess(command.first, command.second)
            handle.register(process)
            drainQuietly(process)
            val finished = process.waitFor((timeoutMs + 1_000).toLong(), TimeUnit.MILLISECONDS)
            if (handle.isCancelled()) {
                process.destroyForcibly()
                throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
            }
            if (!finished) {
                process.destroyForcibly()
                PingOnceResult(address, false)
            } else {
                PingOnceResult(address, process.exitValue() == 0)
            }
        } catch (error: NetException) {
            throw error
        } catch (_: Exception) {
            PingOnceResult(address, false)
        }
    }

    private fun probePort(host: String, port: Int, timeoutMs: Int, handle: NetCommandHandle): Boolean {
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        val socket = Socket()
        handle.register(socket)
        return try {
            socket.connect(InetSocketAddress(host, port), timeoutMs.coerceAtLeast(100))
            true
        } catch (_: Exception) {
            if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
            false
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun queryWhois(
        target: String,
        timeoutMs: Int,
        handle: NetCommandHandle,
        onChunk: (String) -> Unit
    ): String {
        val first = whoisServerQuery("whois.iana.org", target, timeoutMs, handle, onChunk)
        val referral = WHOIS_REFERRAL.find(first)?.groupValues?.get(1)
        if (referral.isNullOrBlank() || referral == "whois.iana.org") return first
        return whoisServerQuery(referral, target, timeoutMs, handle, onChunk)
    }

    private fun whoisServerQuery(
        server: String,
        target: String,
        timeoutMs: Int,
        handle: NetCommandHandle,
        onChunk: (String) -> Unit
    ): String {
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        val socket = Socket()
        handle.register(socket)
        return try {
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(server, 43), timeoutMs)
            socket.getOutputStream().write("$target\r\n".toByteArray(Charsets.US_ASCII))
            socket.getOutputStream().flush()
            val buffer = StringBuilder()
            val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val chars = CharArray(4096)
            while (true) {
                if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
                val n = reader.read(chars)
                if (n < 0) break
                val chunk = String(chars, 0, n)
                buffer.append(chunk)
                if (buffer.length > MAX_OUTPUT_BYTES) {
                    throw NetException(NetworkErrorCode.COMMAND_FAILED, "WHOIS response exceeds 2 MB")
                }
                onChunk(chunk)
            }
            buffer.toString().trim()
        } catch (error: NetException) {
            throw error
        } catch (error: java.net.SocketTimeoutException) {
            throw NetException(NetworkErrorCode.TIMEOUT, "TIMEOUT")
        } catch (error: IOException) {
            throw NetException(classifyIo(error), error.message ?: server)
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun spawn(
        file: String,
        args: List<String>,
        timeoutMs: Int,
        handle: NetCommandHandle,
        onChunk: (String) -> Unit
    ): String {
        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        val process = startProcess(file, args)
        handle.register(process)
        val charset = nativeCharset()
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val overflow = AtomicBoolean(false)
        val stdoutThread = Thread({
            readStream(process.inputStream, charset, stdout, stderr, overflow, process, onChunk)
        }, "net-stdout").apply { isDaemon = true; start() }
        val stderrThread = Thread({
            readStream(process.errorStream, charset, stderr, stdout, overflow, process, onChunk)
        }, "net-stderr").apply { isDaemon = true; start() }
        val finished = process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        if (handle.isCancelled()) {
            process.destroyForcibly()
            joinReaders(stdoutThread, stderrThread)
            throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
        }
        if (!finished) {
            process.destroyForcibly()
            joinReaders(stdoutThread, stderrThread)
            throw NetException(NetworkErrorCode.TIMEOUT, "TIMEOUT")
        }
        joinReaders(stdoutThread, stderrThread)
        if (overflow.get()) throw NetException(NetworkErrorCode.COMMAND_FAILED, "Command output exceeds 2 MB")
        val code = process.exitValue()
        if (code != 0) {
            val message = stderr.toString().trim().ifBlank { stdout.toString().trim() }
                .ifBlank { "$file exited with code $code" }
            throw NetException(NetworkErrorCode.COMMAND_FAILED, message)
        }
        return (stdout.ifBlank { stderr }).toString()
    }

    private fun readStream(
        stream: java.io.InputStream,
        charset: Charset,
        self: StringBuilder,
        other: StringBuilder,
        overflow: AtomicBoolean,
        process: Process,
        onChunk: (String) -> Unit
    ) {
        stream.bufferedReader(charset).use { reader ->
            val buf = CharArray(4096)
            while (true) {
                val n = reader.read(buf)
                if (n < 0) break
                val chunk = String(buf, 0, n)
                synchronized(self) {
                    if (self.length + other.length + chunk.length > MAX_OUTPUT_BYTES) {
                        overflow.set(true)
                        process.destroyForcibly()
                        return
                    }
                    self.append(chunk)
                }
                onChunk(chunk)
            }
        }
    }

    private fun startProcess(file: String, args: List<String>): Process {
        return try {
            ProcessBuilder(listOf(file) + args).start()
        } catch (error: IOException) {
            throw NetException(classifyIo(error), error.message ?: file)
        }
    }

    private fun drainQuietly(process: Process) {
        Thread({ process.inputStream.use { it.copyTo(NullOutput) } }, "net-drain-out").apply { isDaemon = true; start() }
        Thread({ process.errorStream.use { it.copyTo(NullOutput) } }, "net-drain-err").apply { isDaemon = true; start() }
    }

    private fun joinReaders(vararg threads: Thread) {
        threads.forEach { runCatching { it.join(2_000) } }
    }

    private fun networkCommand(action: NetworkAction, target: String?): Pair<String, List<String>> {
        return when (action) {
            NetworkAction.Interfaces -> when (osFamily()) {
                OsFamily.Windows -> "ipconfig.exe" to listOf("/all")
                OsFamily.Mac -> "/sbin/ifconfig" to emptyList()
                OsFamily.Linux -> "ip" to listOf("-details", "address")
            }
            NetworkAction.Connections -> when (osFamily()) {
                OsFamily.Windows -> "netstat.exe" to listOf("-ano")
                else -> "netstat" to listOf("-nat")
            }
            NetworkAction.Ping -> {
                val host = normalizeHostTarget(target)
                when (osFamily()) {
                    OsFamily.Windows -> "ping.exe" to listOf("-n", "4", host)
                    else -> "/sbin/ping" to listOf("-c", "4", host)
                }
            }
            NetworkAction.FlushDns -> when (osFamily()) {
                OsFamily.Windows -> "ipconfig.exe" to listOf("/flushdns")
                OsFamily.Mac -> "/usr/bin/dscacheutil" to listOf("-flushcache")
                OsFamily.Linux -> "resolvectl" to listOf("flush-caches")
            }
            else -> throw NetException(NetworkErrorCode.UNSUPPORTED, "UNSUPPORTED")
        }
    }

    private fun pingOnceCommand(host: String, timeoutMs: Int): Pair<String, List<String>> = when (osFamily()) {
        OsFamily.Windows -> "ping.exe" to listOf("-n", "1", "-w", timeoutMs.toString(), host)
        OsFamily.Mac -> "/sbin/ping" to listOf("-c", "1", "-W", timeoutMs.toString(), host)
        OsFamily.Linux -> "ping" to listOf("-c", "1", "-W", maxOf(1, (timeoutMs + 999) / 1000).toString(), host)
    }

    private fun validPort(value: String): Int {
        if (!value.matches(Regex("""^\d{1,5}$"""))) throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        val port = value.toInt()
        if (port < 1 || port > 65535) throw NetException(NetworkErrorCode.INVALID_TARGET, "INVALID_TARGET")
        return port
    }

    private fun classify(error: Throwable): NetworkErrorCode {
        if (error is NetException) return error.code
        val message = error.message.orEmpty()
        return when {
            message.contains("ABORTED") -> NetworkErrorCode.ABORTED
            message.contains("TIMEOUT") -> NetworkErrorCode.TIMEOUT
            message.contains("INVALID_TARGET") -> NetworkErrorCode.INVALID_TARGET
            else -> classifyIo(error)
        }
    }

    private fun classifyIo(error: Throwable): NetworkErrorCode {
        val message = (error.message.orEmpty() + " " + error.javaClass.simpleName).lowercase()
        return when {
            "enoent" in message || "error=2" in message || "no such file" in message || "cannot find" in message ->
                NetworkErrorCode.UNSUPPORTED
            "eacces" in message || "eperm" in message || "error=13" in message || "permission" in message ->
                NetworkErrorCode.PERMISSION
            else -> NetworkErrorCode.COMMAND_FAILED
        }
    }

    internal fun osFamily(): OsFamily {
        val name = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            name.contains("win") -> OsFamily.Windows
            name.contains("mac") || name.contains("darwin") -> OsFamily.Mac
            else -> OsFamily.Linux
        }
    }

    internal fun nativeCharset(): Charset {
        val name = System.getProperty("sun.jnu.encoding") ?: Charset.defaultCharset().name()
        return runCatching { Charset.forName(name) }.getOrElse { Charset.defaultCharset() }
    }

    private suspend fun <T, R> concurrentMap(
        values: List<T>,
        concurrency: Int,
        handle: NetCommandHandle,
        deadlineMs: Long,
        mapper: (T) -> R
    ): List<R> {
        if (values.isEmpty()) return emptyList()
        val results = arrayOfNulls<Any?>(values.size)
        val next = AtomicInteger(0)
        coroutineScope {
            repeat(minOf(concurrency, values.size)) {
                launch {
                    while (true) {
                        if (handle.isCancelled()) throw NetException(NetworkErrorCode.ABORTED, "ABORTED")
                        if (System.currentTimeMillis() >= deadlineMs) throw NetException(NetworkErrorCode.TIMEOUT, "TIMEOUT")
                        val index = next.getAndIncrement()
                        if (index >= values.size) return@launch
                        results[index] = mapper(values[index])
                    }
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
    }

    private data class PingOnceResult(val address: String, val reachable: Boolean)

    enum class OsFamily { Windows, Mac, Linux }

    private val IPV4_RANGE = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.?$""")
    private val PORT_RANGE = Regex("""^(\d{1,5})\s*-\s*(\d{1,5})$""")
    private val HOST_PATTERN = Regex("""^[a-zA-Z0-9.:_-]+$""")
    private val WHOIS_PATTERN = Regex("""^[a-z0-9.-]+$""")
    private val WHOIS_REFERRAL = Regex("""^(?:refer|whois):\s*(\S+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    private object NullOutput : java.io.OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray, off: Int, len: Int) {}
    }
}
