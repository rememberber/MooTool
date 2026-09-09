package com.rememberber.mootool.next.compose.domain

interface WakeSession {
    fun isAlive(): Boolean
    fun stop()
}

class DisplayWakeLock(
    private val startSession: () -> WakeSession? = { OsDisplayWake.start()?.let(::ProcessWakeSession) }
) {
    private val holders = linkedSetOf<String>()
    private var session: WakeSession? = null

    @Synchronized
    fun acquire(token: String): Boolean {
        if (token.isBlank()) return isActive()
        holders += token
        if (session?.isAlive() != true) {
            session = startSession()
        }
        return isActive()
    }

    @Synchronized
    fun release(token: String) {
        holders.remove(token)
        if (holders.isEmpty()) stopSession()
    }

    @Synchronized
    fun releaseAll() {
        holders.clear()
        stopSession()
    }

    @Synchronized
    fun isActive(): Boolean = holders.isNotEmpty() && session?.isAlive() == true

    @Synchronized
    fun holderCount(): Int = holders.size

    private fun stopSession() {
        session?.stop()
        session = null
    }
}

class ProcessWakeSession(private val process: Process) : WakeSession {
    override fun isAlive(): Boolean = process.isAlive
    override fun stop() {
        process.destroy()
        runCatching { process.waitFor() }
    }
}

object OsDisplayWake {
    fun start(): Process? {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            "mac" in os || "darwin" in os -> listOf("caffeinate", "-d")
            "win" in os -> listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", WINDOWS_SCRIPT)
            else -> listOf(
                "systemd-inhibit",
                "--what=idle:sleep",
                "--who=MooTool Next Compose",
                "--why=Message board presentation",
                "--mode=block",
                "sleep",
                "infinity"
            )
        }
        return runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }.getOrNull()
            ?.takeIf { it.isAlive }
    }

    private const val WINDOWS_SCRIPT =
        "Add-Type @'\nusing System;\nusing System.Runtime.InteropServices;\npublic class MooWake {\n  [DllImport(\"kernel32.dll\")] public static extern uint SetThreadExecutionState(uint flags);\n}\n'@\n[void][MooWake]::SetThreadExecutionState(2147483651)\nwhile (\$true) { Start-Sleep -Seconds 30; [void][MooWake]::SetThreadExecutionState(2147483651) }\n"
}
