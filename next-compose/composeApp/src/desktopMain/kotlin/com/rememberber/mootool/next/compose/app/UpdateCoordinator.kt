package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.domain.UpdateBytesFetcher
import com.rememberber.mootool.next.compose.domain.UpdateCancelled
import com.rememberber.mootool.next.compose.domain.UpdateCheckResult
import com.rememberber.mootool.next.compose.domain.UpdateCheckStatus
import com.rememberber.mootool.next.compose.domain.UpdateDownloader
import com.rememberber.mootool.next.compose.domain.UpdateEngine
import com.rememberber.mootool.next.compose.domain.UpdateProgress
import com.rememberber.mootool.next.compose.domain.UpdateTextFetcher
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UpdateUiState(
    val busy: Boolean = false,
    val status: String = "idle",
    val message: String = "",
    val error: String = "",
    val result: UpdateCheckResult? = null,
    val progress: UpdateProgress? = null,
    val downloaded: Path? = null
)

class UpdateCoordinator(
    private val cacheRoot: Path,
    private val scope: CoroutineScope,
    private val feedUrl: String = System.getenv("MOOTOOL_COMPOSE_UPDATE_FEED_URL")?.trim().orEmpty()
        .ifBlank { UpdateEngine.DEFAULT_FEED },
    private val textFetcher: UpdateTextFetcher = defaultTextFetcher,
    private val bytesFetcher: UpdateBytesFetcher = defaultBytesFetcher,
    private val opener: (Path) -> Unit = defaultOpener
) {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state
    private val downloader = UpdateDownloader(cacheRoot.resolve("updates"), bytesFetcher)
    private val job = AtomicReference<Job?>(null)

    fun check(currentVersion: String = ProductIdentity.VERSION, autoDownload: Boolean = false) {
        replaceJob {
            _state.value = _state.value.copy(busy = true, error = "", message = "", status = "checking")
            runCatching {
                withContext(Dispatchers.IO) {
                    UpdateEngine.fetchAndCheck(currentVersion, UpdateEngine.detectIdentity(), feedUrl, textFetcher)
                }
            }.onSuccess { result ->
                val ready = result.download?.let { downloader.readyFile(it) }
                _state.value = UpdateUiState(
                    busy = false,
                    status = result.status.name.lowercase(),
                    message = result.message,
                    result = result,
                    downloaded = ready
                )
                if (autoDownload && result.status == UpdateCheckStatus.Available && result.download != null && ready == null) {
                    performDownload()
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(busy = false, status = "error", error = error.message ?: "Update check failed")
            }
        }
    }

    fun download() {
        replaceJob { performDownload() }
    }

    private suspend fun performDownload() {
        val result = _state.value.result ?: return
        val pack = result.download ?: return
        _state.value = _state.value.copy(busy = true, error = "", status = "downloading", progress = UpdateProgress(0, pack.size))
        runCatching {
            withContext(Dispatchers.IO) {
                downloader.download(pack) { progress ->
                    _state.value = _state.value.copy(progress = progress)
                }
            }
        }.onSuccess { path ->
            _state.value = _state.value.copy(busy = false, status = "ready", downloaded = path, progress = UpdateProgress(pack.size, pack.size))
        }.onFailure { error ->
            if (error is UpdateCancelled) {
                _state.value = _state.value.copy(busy = false, status = "cancelled", error = "")
            } else {
                _state.value = _state.value.copy(busy = false, status = "error", error = error.message ?: "Update download failed")
            }
        }
    }

    fun cancel() {
        downloader.cancel()
        job.get()?.cancel()
        _state.value = _state.value.copy(busy = false, status = "cancelled")
    }

    fun openInstaller() {
        val path = _state.value.downloaded ?: return
        runCatching { opener(path) }
            .onFailure { _state.value = _state.value.copy(error = it.message ?: "Unable to open installer") }
    }

    fun openReleasePage() {
        val url = _state.value.result?.releaseUrl ?: UpdateEngine.DEFAULT_RELEASES
        runCatching { Desktop.getDesktop().browse(URI(url)) }
    }

    private fun replaceJob(block: suspend CoroutineScope.() -> Unit) {
        job.getAndSet(scope.launch { block() })?.cancel()
    }

    companion object {
        private val client: OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .addNetworkInterceptor { chain ->
                check(chain.request().url.isHttps) { "Update URL must use HTTPS" }
                chain.proceed(chain.request())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val defaultTextFetcher = UpdateTextFetcher { url ->
            UpdateEngine.requireHttps(url, "feed")
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            response.use { http ->
                val body = http.body?.string().orEmpty()
                http.code to body
            }
        }

        val defaultBytesFetcher = UpdateBytesFetcher { url ->
            UpdateEngine.requireHttps(url, "download")
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = response.body
            if (body == null || response.code !in 200..299) {
                val code = response.code
                response.close()
                return@UpdateBytesFetcher code to java.io.ByteArrayInputStream(ByteArray(0))
            }
            response.code to body.byteStream()
        }

        val defaultOpener: (Path) -> Unit = { path -> Desktop.getDesktop().open(path.toFile()) }
    }
}
