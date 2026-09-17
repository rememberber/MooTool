package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.NetworkSettings

/** HTTP/翻译发送时使用的代理配置（trim + 合法端口，对齐 DIFF-514 设置规范化）。 */
fun NetworkSettings.toHttpProxyConfig(): HttpProxyConfig {
    if (!proxyEnabled) return HttpProxyConfig()
    return HttpProxyConfig(
        enabled = true,
        host = proxyHost.trim(),
        port = SettingsNetworkNormalize.sanitizeProxyPort(proxyPort),
        username = proxyUsername.trim(),
        password = proxyPassword.trim(),
    )
}
