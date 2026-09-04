use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
    time::{Duration, Instant},
};

use base64::{Engine as _, engine::general_purpose::STANDARD};
use futures_util::StreamExt;
use reqwest::{
    Method, Url,
    header::{CONTENT_TYPE, COOKIE, HeaderName, HeaderValue},
    redirect::Policy,
};
use tauri::{Emitter, ipc::Channel};
use tokio::sync::Notify;

use crate::contracts::{
    error::AppResult,
    network::{
        HttpProgressEvent, HttpRequestHistory, HttpRequestSpec, HttpResponseData, SavedHttpRequest,
        validate_http_request_payload,
    },
    settings::{NetworkSettings, ProxyMode},
};
use crate::repositories::{local_data::LocalDataRepository, settings::SettingsRepository};

const MAX_RESPONSE_BYTES: usize = 5 * 1024 * 1024;

#[derive(Default)]
pub struct HttpRequestManager {
    active: Mutex<HashMap<String, Arc<Notify>>>,
}

impl HttpRequestManager {
    fn register(&self, request_id: &str) -> Result<Arc<Notify>, String> {
        if request_id.is_empty() || request_id.len() > 128 {
            return Err("invalid HTTP request ID".into());
        }
        let mut active = self
            .active
            .lock()
            .map_err(|_| "HTTP manager state poisoned")?;
        if active.contains_key(request_id) {
            return Err("HTTP request ID is already active".into());
        }
        let notify = Arc::new(Notify::new());
        active.insert(request_id.into(), notify.clone());
        Ok(notify)
    }

    fn finish(&self, request_id: &str) {
        if let Ok(mut active) = self.active.lock() {
            active.remove(request_id);
        }
    }

    fn cancel(&self, request_id: &str) -> bool {
        let notify = self
            .active
            .lock()
            .ok()
            .and_then(|active| active.get(request_id).cloned());
        if let Some(notify) = notify {
            // `notify_one` stores a permit when cancellation races the first
            // `notified()` poll, so an immediately cancelled request cannot
            // accidentally continue to the network.
            notify.notify_one();
            true
        } else {
            false
        }
    }
}

#[tauri::command]
pub async fn execute_http_request(
    app: tauri::AppHandle,
    manager: tauri::State<'_, HttpRequestManager>,
    repository: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
    request: HttpRequestSpec,
    progress: Channel<HttpProgressEvent>,
) -> AppResult<HttpResponseData> {
    validate_request(&request)?;
    let notify = manager.register(&request.request_id)?;
    let request_id = request.request_id.clone();
    let history_request = request.clone();
    let result = execute(request, notify, progress, settings.snapshot().network).await;
    manager.finish(&request_id);
    let response = result?;
    let history = HttpRequestHistory {
        id: request_id,
        request: history_request,
        response: response.clone(),
        created_at: now_millis(),
    };
    if let Err(error) = repository.record_http_history(history) {
        tracing::warn!(error = %error, "HTTP request completed but history could not be saved");
    } else if let Err(error) = app.emit("mootool://local-data-changed", "http-history") {
        tracing::warn!(error = %error, "HTTP history saved but synchronization event failed");
    }
    Ok(response)
}

#[tauri::command]
pub fn cancel_http_request(
    manager: tauri::State<'_, HttpRequestManager>,
    request_id: String,
) -> bool {
    manager.cancel(&request_id)
}

#[tauri::command]
pub fn list_saved_http_requests(
    repository: tauri::State<'_, LocalDataRepository>,
    query: String,
) -> AppResult<Vec<SavedHttpRequest>> {
    Ok(repository.list_http_requests(&query)?)
}

#[tauri::command]
pub fn save_http_request(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    item: SavedHttpRequest,
) -> AppResult<SavedHttpRequest> {
    let saved = repository.save_http_request(item)?;
    let _ = app.emit("mootool://local-data-changed", "http-saved");
    Ok(saved)
}

#[tauri::command]
pub fn delete_saved_http_request(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_http_request(&id)?;
    if deleted {
        let _ = app.emit("mootool://local-data-changed", "http-saved");
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_http_request_history(
    repository: tauri::State<'_, LocalDataRepository>,
    query: String,
) -> AppResult<Vec<HttpRequestHistory>> {
    Ok(repository.list_http_history(&query)?)
}

#[tauri::command]
pub fn delete_http_request_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_http_history(&id)?;
    if deleted {
        let _ = app.emit("mootool://local-data-changed", "http-history");
    }
    Ok(deleted)
}

#[tauri::command]
pub fn clear_http_request_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<usize> {
    let deleted = repository.clear_http_history()?;
    if deleted > 0 {
        let _ = app.emit("mootool://local-data-changed", "http-history");
    }
    Ok(deleted)
}

async fn execute(
    request: HttpRequestSpec,
    cancelled: Arc<Notify>,
    progress: Channel<HttpProgressEvent>,
    network: NetworkSettings,
) -> Result<HttpResponseData, String> {
    let started = Instant::now();
    let timeout = Duration::from_millis(request.timeout_ms);
    let mut client_builder = reqwest::Client::builder()
        .redirect(if request.follow_redirects {
            Policy::limited(5)
        } else {
            Policy::none()
        })
        .connect_timeout(timeout.min(Duration::from_secs(30)))
        .timeout(timeout)
        .user_agent("MooTool-Next-Tauri/0.1");
    match network.proxy_mode {
        ProxyMode::System => {}
        ProxyMode::Direct => client_builder = client_builder.no_proxy(),
        ProxyMode::Manual => {
            let host = network.proxy_host.trim();
            if host.is_empty() {
                return Err("manual proxy host is required".into());
            }
            let endpoint = if host.contains("://") {
                host.to_string()
            } else {
                format!("http://{host}:{}", network.proxy_port)
            };
            let mut proxy = reqwest::Proxy::all(&endpoint)
                .map_err(|error| format!("invalid proxy endpoint: {error}"))?;
            if !network.proxy_username.trim().is_empty() {
                let password = super::secure_credentials::load_proxy_password()
                    .await?
                    .unwrap_or_default();
                proxy = proxy.basic_auth(network.proxy_username.trim(), &password);
            }
            client_builder = client_builder.proxy(proxy);
        }
    }
    let client = client_builder
        .build()
        .map_err(|error| format!("failed to create HTTP client: {error}"))?;
    let method = Method::from_bytes(request.method.as_bytes())
        .map_err(|_| "invalid HTTP method".to_string())?;
    let mut url = Url::parse(&request.url).map_err(|error| format!("invalid HTTP URL: {error}"))?;
    {
        let mut pairs = url.query_pairs_mut();
        for entry in request
            .params
            .iter()
            .filter(|entry| entry.enabled && !entry.name.trim().is_empty())
        {
            pairs.append_pair(entry.name.trim(), &entry.value);
        }
    }
    let mut builder = client.request(method, url);
    let mut has_content_type = false;
    for header in request.headers.into_iter().filter(|header| header.enabled) {
        let name = HeaderName::from_bytes(header.name.trim().as_bytes())
            .map_err(|_| format!("invalid HTTP header name: {}", header.name))?;
        let value = HeaderValue::from_str(&header.value)
            .map_err(|_| format!("invalid value for HTTP header {}", header.name))?;
        has_content_type |= name == CONTENT_TYPE;
        builder = builder.header(name, value);
    }
    let cookies = request
        .cookies
        .into_iter()
        .filter(|cookie| cookie.enabled && !cookie.name.trim().is_empty())
        .map(|cookie| format!("{}={}", cookie.name.trim(), cookie.value))
        .collect::<Vec<_>>()
        .join("; ");
    if !cookies.is_empty() {
        builder = builder.header(COOKIE, cookies);
    }
    if !request.body.is_empty() {
        if !has_content_type && !request.body_type.trim().is_empty() {
            let content_type = HeaderValue::from_str(request.body_type.trim())
                .map_err(|_| "invalid HTTP body content type".to_string())?;
            builder = builder.header(CONTENT_TYPE, content_type);
        }
        builder = builder.body(request.body);
    }
    let _ = progress.send(HttpProgressEvent::Started);
    let response = tokio::select! {
        response = builder.send() => response.map_err(|error| format!("HTTP request failed: {error}"))?,
        _ = cancelled.notified() => return Err("HTTP request cancelled".into()),
    };
    let status = response.status().as_u16();
    let final_url = response.url().to_string();
    let content_type = response
        .headers()
        .get(reqwest::header::CONTENT_TYPE)
        .and_then(|value| value.to_str().ok())
        .unwrap_or_default()
        .to_string();
    let headers = response
        .headers()
        .iter()
        .map(|(name, value)| {
            (
                name.to_string(),
                value.to_str().unwrap_or("<binary>").to_string(),
            )
        })
        .collect::<Vec<_>>();
    let _ = progress.send(HttpProgressEvent::Headers { status });
    let mut bytes = Vec::new();
    let mut stream = response.bytes_stream();
    let mut truncated = false;
    loop {
        let chunk = tokio::select! {
            chunk = stream.next() => chunk,
            _ = cancelled.notified() => return Err("HTTP request cancelled".into()),
        };
        let Some(chunk) = chunk else { break };
        let chunk = chunk.map_err(|error| format!("failed to read HTTP response: {error}"))?;
        let remaining = MAX_RESPONSE_BYTES.saturating_sub(bytes.len());
        if chunk.len() > remaining {
            bytes.extend_from_slice(&chunk[..remaining]);
            truncated = true;
            break;
        }
        bytes.extend_from_slice(&chunk);
        let _ = progress.send(HttpProgressEvent::Download {
            received_bytes: bytes.len(),
        });
    }
    let textual = content_type.starts_with("text/")
        || content_type.contains("json")
        || content_type.contains("xml")
        || content_type.contains("javascript")
        || std::str::from_utf8(&bytes).is_ok();
    Ok(HttpResponseData {
        status,
        final_url,
        headers,
        body_text: if textual {
            String::from_utf8_lossy(&bytes).into_owned()
        } else {
            String::new()
        },
        body_base64: if textual {
            String::new()
        } else {
            STANDARD.encode(&bytes)
        },
        content_type,
        size_bytes: bytes.len(),
        truncated,
        duration_ms: started.elapsed().as_millis(),
    })
}

fn validate_request(request: &HttpRequestSpec) -> Result<(), String> {
    validate_http_request_payload(request)?;
    if !(1_000..=120_000).contains(&request.timeout_ms) {
        return Err("HTTP timeout must be between 1 and 120 seconds".into());
    }
    let url = Url::parse(&request.url).map_err(|error| format!("invalid HTTP URL: {error}"))?;
    if !matches!(url.scheme(), "http" | "https")
        || url.host_str().is_none()
        || !url.username().is_empty()
        || url.password().is_some()
    {
        return Err(
            "HTTP URL must use http/https, include a host, and omit embedded credentials".into(),
        );
    }
    Ok(())
}

fn now_millis() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(i64::MAX)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn request(url: &str) -> HttpRequestSpec {
        HttpRequestSpec {
            request_id: "request-1".into(),
            name: String::new(),
            method: "GET".into(),
            url: url.into(),
            params: vec![],
            headers: vec![],
            cookies: vec![],
            body: String::new(),
            body_type: "application/json".into(),
            timeout_ms: 30_000,
            follow_redirects: true,
        }
    }

    #[test]
    fn rejects_unsafe_or_unsupported_urls() {
        assert!(validate_request(&request("https://example.com")).is_ok());
        assert!(validate_request(&request("file:///etc/passwd")).is_err());
        assert!(validate_request(&request("https://user:pass@example.com")).is_err());
    }

    #[test]
    fn tracks_and_cancels_owned_request_ids() {
        let manager = HttpRequestManager::default();
        assert!(manager.register("one").is_ok());
        assert!(manager.register("one").is_err());
        assert!(manager.cancel("one"));
        manager.finish("one");
        assert!(!manager.cancel("one"));
    }
}
