use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
    time::{Duration, Instant},
};

use futures_util::StreamExt;
use reqwest::{
    Method, Url,
    header::{CONTENT_TYPE, COOKIE, HeaderName, HeaderValue},
    redirect::Policy,
};
use tauri::{Emitter, WebviewUrl, WebviewWindowBuilder, ipc::Channel};
use tokio::sync::Notify;

use crate::contracts::{
    error::AppResult,
    network::{
        HttpProgressEvent, HttpRequestHistory, HttpRequestSpec, HttpResponseData,
        SaveHttpRequestInput, SavedHttpRequest, validate_http_request_payload,
    },
    settings::NetworkSettings,
};
use crate::repositories::{local_data::LocalDataRepository, settings::SettingsRepository};

const MAX_RESPONSE_BYTES: usize = 10 * 1024 * 1024;

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
    if let Err(error) = validate_request(&request) {
        let failed = http_failure(&request.url, &error, 0);
        if repository.record_http_history(&request, &failed).is_ok() {
            let _ = app.emit("mootool://local-data-changed", "http-history");
        }
        return Ok(failed);
    }
    let notify = manager.register(&request.request_id)?;
    let request_id = request.request_id.clone();
    let history_request = request.clone();
    let result = execute(request, notify, progress, settings.snapshot().network).await;
    manager.finish(&request_id);
    let response = match result {
        Ok(response) => response,
        Err(error) => http_failure(&history_request.url, &error, 0),
    };
    if let Err(error) = repository.record_http_history(&history_request, &response) {
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
    item: SaveHttpRequestInput,
) -> AppResult<SavedHttpRequest> {
    let saved = repository.save_http_request(item)?;
    let _ = app.emit("mootool://local-data-changed", "http-saved");
    Ok(saved)
}

#[tauri::command]
pub fn delete_saved_http_request(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<bool> {
    let deleted = repository.delete_http_request(id)?;
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
    id: i64,
) -> AppResult<bool> {
    let deleted = repository.delete_http_history(id)?;
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
            Policy::limited(10)
        } else {
            Policy::none()
        })
        .connect_timeout(timeout.min(Duration::from_secs(30)))
        .timeout(timeout);
    if !network.proxy_enabled {
        client_builder = client_builder.no_proxy();
    } else {
        let host = network.proxy_host.trim();
        let port: u16 = network
            .proxy_port
            .trim()
            .parse()
            .map_err(|_| "Invalid proxy settings".to_string())?;
        if host.is_empty() || !(1..=65535).contains(&port) {
            return Err("Invalid proxy settings".into());
        }
        let endpoint = if host.contains("://") {
            host.to_string()
        } else {
            format!("http://{host}:{port}")
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
    let client = client_builder
        .build()
        .map_err(|error| format!("failed to create HTTP client: {error}"))?;
    let method = Method::from_bytes(request.method.as_bytes())
        .map_err(|_| "invalid HTTP method".to_string())?;
    let prepared = prepare_http_exchange(&request)?;
    let mut builder = client.request(method, prepared.url);
    let mut has_content_type = false;
    for header in request
        .headers
        .into_iter()
        .filter(|header| header.enabled && !header.name.trim().is_empty())
    {
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
    if let Some(body) = prepared.body {
        if !has_content_type && let Some(content_type) = prepared.content_type {
            let content_type = HeaderValue::from_str(&content_type)
                .map_err(|_| "invalid HTTP body content type".to_string())?;
            builder = builder.header(CONTENT_TYPE, content_type);
        }
        builder = builder.body(body);
    }
    let _ = progress.send(HttpProgressEvent::Started);
    let response = tokio::select! {
        response = builder.send() => response.map_err(|error| format!("HTTP request failed: {error}"))?,
        _ = cancelled.notified() => return Err("ABORTED".into()),
    };
    let status = response.status().as_u16();
    let status_text = response
        .status()
        .canonical_reason()
        .unwrap_or("")
        .to_string();
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
    if let Some(length) = response.content_length()
        && length > MAX_RESPONSE_BYTES as u64
    {
        return Err("RESPONSE_TOO_LARGE".into());
    }
    let mut bytes = Vec::new();
    let mut stream = response.bytes_stream();
    loop {
        let chunk = tokio::select! {
            chunk = stream.next() => chunk,
            _ = cancelled.notified() => return Err("ABORTED".into()),
        };
        let Some(chunk) = chunk else { break };
        let chunk = chunk.map_err(|error| format!("failed to read HTTP response: {error}"))?;
        let remaining = MAX_RESPONSE_BYTES.saturating_sub(bytes.len());
        if chunk.len() > remaining {
            return Err("RESPONSE_TOO_LARGE".into());
        }
        bytes.extend_from_slice(&chunk);
        let _ = progress.send(HttpProgressEvent::Download {
            received_bytes: bytes.len(),
        });
    }
    let text = decode_http_body(&bytes, &content_type)?;
    Ok(HttpResponseData {
        status,
        final_url,
        headers,
        body_text: pretty_json(&text),
        body_base64: String::new(),
        content_type,
        size_bytes: bytes.len(),
        truncated: false,
        duration_ms: started.elapsed().as_millis(),
        status_text,
    })
}

fn decode_http_body(bytes: &[u8], content_type: &str) -> Result<String, String> {
    let label = content_type_charset(content_type).unwrap_or_else(|| "utf-8".to_string());
    let encoding = encoding_rs::Encoding::for_label(label.as_bytes())
        .ok_or_else(|| format!("Invalid charset: {label}"))?;
    Ok(encoding.decode(bytes).0.into_owned())
}

fn content_type_charset(content_type: &str) -> Option<String> {
    let lower = content_type.to_ascii_lowercase();
    let start = lower.find("charset=")? + "charset=".len();
    let rest = content_type[start..].trim();
    let end = rest.find(';').unwrap_or(rest.len());
    let label = rest[..end]
        .trim()
        .trim_matches(|character| character == '"' || character == '\'');
    if label.is_empty() {
        None
    } else {
        Some(label.to_string())
    }
}

fn classify_http_failure(message: &str) -> &'static str {
    if message.contains("RESPONSE_TOO_LARGE") {
        "RESPONSE_TOO_LARGE"
    } else if message.contains("TIMEOUT") || message.to_ascii_lowercase().contains("timed out") {
        "TIMEOUT"
    } else if message.contains("ABORTED") || message.to_ascii_lowercase().contains("cancel") {
        "ABORTED"
    } else if message.contains("invalid HTTP")
        || message.contains("Invalid proxy")
        || message.contains("Unsupported")
    {
        "INVALID_REQUEST"
    } else {
        "NETWORK"
    }
}

fn http_failure(url: &str, message: &str, duration_ms: u128) -> HttpResponseData {
    HttpResponseData {
        status: 0,
        final_url: url.to_string(),
        headers: Vec::new(),
        body_text: String::new(),
        body_base64: String::new(),
        content_type: String::new(),
        size_bytes: 0,
        truncated: false,
        duration_ms,
        status_text: classify_http_failure(message).to_string(),
    }
}

fn pretty_json(value: &str) -> String {
    match serde_json::from_str::<serde_json::Value>(value) {
        Ok(parsed) => serde_json::to_string_pretty(&parsed).unwrap_or_else(|_| value.to_string()),
        Err(_) => value.to_string(),
    }
}

struct PreparedHttpExchange {
    url: Url,
    body: Option<String>,
    content_type: Option<String>,
}

fn normalize_http_url(value: &str) -> String {
    let trimmed = value.trim();
    let lower = trimmed.to_ascii_lowercase();
    if lower.starts_with("http://") || lower.starts_with("https://") || trimmed.contains("://") {
        trimmed.to_string()
    } else {
        format!("http://{trimmed}")
    }
}

fn prepare_http_exchange(request: &HttpRequestSpec) -> Result<PreparedHttpExchange, String> {
    let mut url = Url::parse(&normalize_http_url(&request.url))
        .map_err(|error| format!("invalid HTTP URL: {error}"))?;
    if !matches!(url.scheme(), "http" | "https") || url.host_str().is_none() {
        return Err("HTTP URL must use http/https and include a host".into());
    }
    let params = request
        .params
        .iter()
        .filter(|entry| entry.enabled && !entry.name.trim().is_empty())
        .collect::<Vec<_>>();
    let method = request.method.to_ascii_uppercase();
    if matches!(method.as_str(), "GET" | "HEAD" | "OPTIONS") {
        {
            let mut pairs = url.query_pairs_mut();
            for entry in params {
                pairs.append_pair(&entry.name, &entry.value);
            }
        }
        return Ok(PreparedHttpExchange {
            url,
            body: None,
            content_type: None,
        });
    }
    if !request.body.is_empty() {
        let content_type = if request.body_type.trim().is_empty() {
            "text/plain".to_string()
        } else {
            request.body_type.trim().to_string()
        };
        return Ok(PreparedHttpExchange {
            url,
            body: Some(request.body.clone()),
            content_type: Some(content_type),
        });
    }
    if params.is_empty() {
        return Ok(PreparedHttpExchange {
            url,
            body: None,
            content_type: None,
        });
    }
    let mut form = Url::parse("http://form.invalid/").expect("form encoder");
    {
        let mut pairs = form.query_pairs_mut();
        for entry in params {
            pairs.append_pair(&entry.name, &entry.value);
        }
    }
    Ok(PreparedHttpExchange {
        url,
        body: Some(form.query().unwrap_or_default().to_string()),
        content_type: Some("application/x-www-form-urlencoded".to_string()),
    })
}

fn validate_request(request: &HttpRequestSpec) -> Result<(), String> {
    validate_http_request_payload(request)?;
    if !(1_000..=120_000).contains(&request.timeout_ms) {
        return Err("HTTP timeout must be between 1 and 120 seconds".into());
    }
    prepare_http_exchange(request).map(|_| ())
}

#[tauri::command]
pub fn open_http_response_window(app: tauri::AppHandle) -> AppResult<()> {
    let millis = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|value| value.as_millis())
        .unwrap_or(0);
    WebviewWindowBuilder::new(
        &app,
        format!("http-response-{millis}"),
        WebviewUrl::App("index.html?surface=http-response".into()),
    )
    .title("HTTP Response")
    .inner_size(1000.0, 760.0)
    .min_inner_size(560.0, 400.0)
    .center()
    .build()
    .map_err(|error| format!("failed to open HTTP response window: {error}"))?;
    Ok(())
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
            saved_id: None,
        }
    }

    #[test]
    fn builds_get_query_and_post_form_like_electron() {
        let mut get = request("example.com/api");
        get.params.push(header("q", "moo tool"));
        get.body = "ignored".into();
        let prepared = prepare_http_exchange(&get).expect("get");
        assert_eq!(prepared.url.as_str(), "http://example.com/api?q=moo+tool");
        assert!(prepared.body.is_none());

        let mut post = request("https://example.com/api");
        post.method = "POST".into();
        post.params.push(header("q", "1"));
        post.body = "{\"ok\":true}".into();
        post.body_type = "application/json".into();
        let prepared = prepare_http_exchange(&post).expect("post body");
        assert_eq!(prepared.url.as_str(), "https://example.com/api");
        assert_eq!(prepared.body.as_deref(), Some("{\"ok\":true}"));
        assert_eq!(prepared.content_type.as_deref(), Some("application/json"));

        post.body.clear();
        let prepared = prepare_http_exchange(&post).expect("post form");
        assert_eq!(prepared.body.as_deref(), Some("q=1"));
        assert_eq!(
            prepared.content_type.as_deref(),
            Some("application/x-www-form-urlencoded")
        );
    }

    fn header(name: &str, value: &str) -> crate::contracts::network::HttpHeader {
        crate::contracts::network::HttpHeader {
            id: String::new(),
            name: name.into(),
            value: value.into(),
            enabled: true,
        }
    }

    #[test]
    fn decodes_response_bodies_with_the_content_type_charset() {
        assert_eq!(
            decode_http_body(b"{\"a\":1}", "application/json").expect("utf-8"),
            "{\"a\":1}"
        );
        assert_eq!(
            decode_http_body(&[0xC4, 0xE3, 0xBA, 0xC3], "text/plain; charset=gbk").expect("gbk"),
            "你好"
        );
        assert!(decode_http_body(b"moo", "text/plain; charset=not-a-charset").is_err());
    }

    #[test]
    fn classifies_transport_failures_like_electron() {
        assert_eq!(classify_http_failure("ABORTED"), "ABORTED");
        assert_eq!(classify_http_failure("operation timed out"), "TIMEOUT");
        assert_eq!(
            classify_http_failure("RESPONSE_TOO_LARGE"),
            "RESPONSE_TOO_LARGE"
        );
        assert_eq!(
            classify_http_failure("invalid HTTP URL: relative URL"),
            "INVALID_REQUEST"
        );
        assert_eq!(classify_http_failure("connection refused"), "NETWORK");
        assert_eq!(
            http_failure("https://example.com", "ABORTED", 12).status_text,
            "ABORTED"
        );
    }

    #[test]
    fn pretty_prints_json_response_bodies() {
        assert_eq!(pretty_json("{\"a\":1}"), "{\n  \"a\": 1\n}");
        assert_eq!(pretty_json("not json"), "not json");
    }

    #[test]
    fn rejects_unsafe_or_unsupported_urls() {
        assert!(validate_request(&request("https://example.com")).is_ok());
        assert!(validate_request(&request("example.com")).is_ok());
        assert!(validate_request(&request("file:///etc/passwd")).is_err());
        assert!(validate_request(&request("https://user:pass@example.com")).is_ok());
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
