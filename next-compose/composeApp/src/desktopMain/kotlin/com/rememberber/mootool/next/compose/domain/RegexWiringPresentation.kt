package com.rememberber.mootool.next.compose.domain

/** F15 正则：测试/取消守卫与引擎路径（可单测；UI 仍经 [RegexWorkerClient] 进程隔离）。 */
object RegexWiringPresentation {
    fun canRunTest(pattern: String, running: Boolean): Boolean = pattern.isNotBlank() && !running

    fun showCancel(running: Boolean): Boolean = running

    sealed interface MatchOutcome {
        data class Success(val matches: List<RegexMatch>) : MatchOutcome
        data class Failure(val error: Throwable) : MatchOutcome
    }

    fun runMatch(
        pattern: String,
        source: String,
        options: RegexOptions,
        maxMatches: Int = RegexEngine.DEFAULT_MAX_MATCHES,
    ): MatchOutcome =
        runCatching { RegexEngine.match(pattern, source, options, maxMatches) }.fold(
            onSuccess = { MatchOutcome.Success(it) },
            onFailure = { MatchOutcome.Failure(it) },
        )

    fun shouldToastWorkerError(code: String): Boolean = code != "cancelled"
}
