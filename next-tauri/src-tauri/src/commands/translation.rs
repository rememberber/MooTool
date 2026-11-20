use std::{
    collections::HashMap,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, AtomicUsize, Ordering},
    },
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use reqwest::{Client, Url, redirect::Policy};
use serde_json::Value;
use tokio::sync::Notify;

use crate::{
    contracts::{
        error::AppResult,
        local_data::SaveTranslationHistoryInput,
        settings::NetworkSettings,
        translation::{TranslationProvider, TranslationRequest, TranslationResult},
    },
    repositories::{local_data::LocalDataRepository, settings::SettingsRepository},
};

const GOOGLE_CHUNK_CHARS: usize = 1_800;
const GOOGLE_CHUNK_CONCURRENCY: usize = 3;
const PROVIDER_COOLDOWN_MS: u64 = 10 * 60 * 1_000;
const TRANSLATION_USER_AGENT: &str =
    "Mozilla/5.0 (MooTool Next) AppleWebKit/537.36 Chrome/138 Safari/537.36";

#[derive(Clone, Debug)]
struct BingSession {
    ig: String,
    key: String,
    token: String,
    expires_at: u64,
    request_count: u32,
}

#[derive(Default)]
pub struct TranslationManager {
    active: Mutex<HashMap<String, Arc<Notify>>>,
    bing_session: tokio::sync::Mutex<Option<BingSession>>,
    provider_cooldown_until: Mutex<HashMap<TranslationProvider, u64>>,
}

impl TranslationManager {
    fn register(&self, request_id: &str) -> Result<Arc<Notify>, String> {
        if request_id.is_empty() || request_id.len() > 128 {
            return Err("invalid translation request ID".into());
        }
        let mut active = self
            .active
            .lock()
            .map_err(|_| "translation manager state poisoned")?;
        if active.contains_key(request_id) {
            return Err("translation request ID is already active".into());
        }
        let cancelled = Arc::new(Notify::new());
        active.insert(request_id.into(), cancelled.clone());
        Ok(cancelled)
    }

    fn finish(&self, request_id: &str) {
        if let Ok(mut active) = self.active.lock() {
            active.remove(request_id);
        }
    }

    fn cancel(&self, request_id: &str) -> bool {
        let cancelled = self
            .active
            .lock()
            .ok()
            .and_then(|active| active.get(request_id).cloned());
        if let Some(cancelled) = cancelled {
            cancelled.notify_one();
            true
        } else {
            false
        }
    }

    fn should_skip_provider(&self, provider: TranslationProvider, now: u64) -> bool {
        self.provider_cooldown_until
            .lock()
            .ok()
            .and_then(|guard| guard.get(&provider).copied())
            .is_some_and(|until| until > now)
    }

    fn mark_provider_failure(&self, provider: TranslationProvider, now: u64) {
        if let Ok(mut guard) = self.provider_cooldown_until.lock() {
            guard.insert(provider, now.saturating_add(PROVIDER_COOLDOWN_MS));
        }
    }

    fn mark_provider_success(&self, provider: TranslationProvider) {
        if let Ok(mut guard) = self.provider_cooldown_until.lock() {
            guard.remove(&provider);
        }
    }
}

#[tauri::command]
pub async fn translate_text(
    manager: tauri::State<'_, TranslationManager>,
    repository: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
    request: TranslationRequest,
) -> AppResult<TranslationResult> {
    validate_request(&request)?;
    let cancelled = manager.register(&request.request_id)?;
    let request_id = request.request_id.clone();
    let source_text = request.text.clone();
    let source_lang = request.source_lang.clone();
    let target_lang = request.target_lang.clone();
    let result =
        execute_translation(&manager, &request, cancelled, &settings.snapshot().network).await;
    manager.finish(&request_id);
    let result = result?;
    if let Err(error) = repository.save_translation_history(SaveTranslationHistoryInput {
        source_text,
        target_text: result.text.clone(),
        source_lang,
        target_lang,
        translator_type: result.provider,
    }) {
        eprintln!("Failed to save translation history: {error}");
    }
    Ok(result)
}

#[tauri::command]
pub fn cancel_translation(
    manager: tauri::State<'_, TranslationManager>,
    request_id: String,
) -> bool {
    manager.cancel(&request_id)
}

async fn execute_translation(
    manager: &TranslationManager,
    request: &TranslationRequest,
    cancelled: Arc<Notify>,
    network: &NetworkSettings,
) -> Result<TranslationResult, String> {
    let mut client_builder = Client::builder()
        .connect_timeout(Duration::from_secs(5))
        .timeout(Duration::from_millis(request.timeout_ms))
        .redirect(Policy::limited(3))
        .user_agent(TRANSLATION_USER_AGENT);
    if network.proxy_enabled {
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
        .map_err(|error| format!("failed to create translation client: {error}"))?;
    let providers = translation_provider_order(
        request.preferred_provider,
        manager.should_skip_provider(request.preferred_provider, unix_millis()),
    );
    let mut last_error = String::new();
    for provider in providers {
        let translated = match provider {
            TranslationProvider::Google => {
                translate_google(&client, request, cancelled.clone()).await
            }
            TranslationProvider::Bing => {
                translate_bing(manager, &client, request, cancelled.clone()).await
            }
            TranslationProvider::DeepL => {
                translate_deepl(&client, request, cancelled.clone()).await
            }
        };
        match translated {
            Ok(text) => {
                manager.mark_provider_success(provider);
                return Ok(TranslationResult {
                    request_id: request.request_id.clone(),
                    text,
                    provider,
                    fallback_used: provider != request.preferred_provider,
                });
            }
            Err(error) if error == "translation request cancelled" => return Err(error),
            Err(error) => {
                manager.mark_provider_failure(provider, unix_millis());
                last_error = error;
            }
        }
    }
    Err(if last_error.is_empty() {
        "all translation providers failed".into()
    } else {
        last_error
    })
}

async fn translate_deepl(
    client: &Client,
    request: &TranslationRequest,
    cancelled: Arc<Notify>,
) -> Result<String, String> {
    let key = super::secure_credentials::load_deepl_api_key()?.unwrap_or_default();
    let key = key.trim();
    if key.is_empty() {
        return Err("DEEPL_KEY_REQUIRED".into());
    }
    let source = deepl_language(&request.source_lang, true)?;
    let target = deepl_language(&request.target_lang, false)?;
    let host = if key.ends_with(":fx") {
        "https://api-free.deepl.com"
    } else {
        "https://api.deepl.com"
    };
    let chunks = split_text(&request.text, 12_000)?;
    let mut output = String::new();
    for chunk in chunks {
        let mut body = serde_json::json!({
            "text": [chunk],
            "target_lang": target,
            "preserve_formatting": true
        });
        if let Some(source) = source.as_deref() {
            body["source_lang"] = Value::String(source.to_string());
        }
        let response = tokio::select! {
            response = client.post(format!("{host}/v2/translate")).header("Authorization", format!("DeepL-Auth-Key {key}")).json(&body).send() => response.map_err(|error| format!("DeepL translation failed: {error}"))?,
            _ = cancelled.notified() => return Err("translation request cancelled".into()),
        };
        let status = response.status();
        if status.as_u16() == 401 || status.as_u16() == 403 {
            return Err("DEEPL_INVALID_KEY".into());
        }
        if status.as_u16() == 456 {
            return Err("DEEPL_QUOTA_EXCEEDED".into());
        }
        if status.as_u16() == 429 {
            return Err("DEEPL_RATE_LIMITED".into());
        }
        if !status.is_success() {
            return Err(format!("DeepL HTTP {status}"));
        }
        let payload = response
            .json::<Value>()
            .await
            .map_err(|error| format!("invalid DeepL translation response: {error}"))?;
        let translated = payload
            .pointer("/translations/0/text")
            .and_then(Value::as_str)
            .ok_or_else(|| "DEEPL_EMPTY_RESPONSE".to_string())?;
        if translated.is_empty() {
            return Err("DEEPL_EMPTY_RESPONSE".into());
        }
        output.push_str(translated);
    }
    Ok(output)
}

fn deepl_language(code: &str, source: bool) -> Result<Option<String>, String> {
    if code == "yue" || code == "wyw" || (!source && code == "auto") {
        return Err("DEEPL_UNSUPPORTED_LANGUAGE".into());
    }
    if code == "auto" {
        return Ok(None);
    }
    let mapped = if code == "zh-CN" {
        if source { "ZH" } else { "ZH-HANS" }
    } else if code == "cht" {
        if source { "ZH" } else { "ZH-HANT" }
    } else {
        match code {
            "jp" => "JA",
            "kor" => "KO",
            "fra" => "FR",
            "spa" => "ES",
            "ara" => "AR",
            "bul" => "BG",
            "est" => "ET",
            "dan" => "DA",
            "fin" => "FI",
            "rom" => "RO",
            "slo" => "SL",
            "swe" => "SV",
            "vie" => "VI",
            other => return Ok(Some(other.to_uppercase())),
        }
    };
    Ok(Some(mapped.into()))
}

async fn translate_google(
    client: &Client,
    request: &TranslationRequest,
    cancelled: Arc<Notify>,
) -> Result<String, String> {
    let chunks = split_text(&request.text, GOOGLE_CHUNK_CHARS)?;
    if chunks.len() <= 1 {
        return translate_google_chunk(
            client,
            chunks.first().map(String::as_str).unwrap_or(""),
            &request.source_lang,
            &request.target_lang,
            cancelled.as_ref(),
        )
        .await;
    }
    let stop = Arc::new(StopFlag::new());
    let parent = cancelled.clone();
    let bridge_stop = stop.clone();
    let bridge = tokio::spawn(async move {
        parent.notified().await;
        bridge_stop.cancel();
    });
    let next = Arc::new(AtomicUsize::new(0));
    let results = Arc::new(Mutex::new(vec![String::new(); chunks.len()]));
    let failure = Arc::new(Mutex::new(None::<String>));
    let mut workers = Vec::with_capacity(GOOGLE_CHUNK_CONCURRENCY.min(chunks.len()));
    for _ in 0..GOOGLE_CHUNK_CONCURRENCY.min(chunks.len()) {
        let chunks = chunks.clone();
        let next = next.clone();
        let results = results.clone();
        let failure = failure.clone();
        let client = client.clone();
        let source = request.source_lang.clone();
        let target = request.target_lang.clone();
        let stop = stop.clone();
        workers.push(tokio::spawn(async move {
            loop {
                if failure
                    .lock()
                    .ok()
                    .and_then(|guard| guard.clone())
                    .is_some()
                {
                    break;
                }
                let index = next.fetch_add(1, Ordering::Relaxed);
                if index >= chunks.len() {
                    break;
                }
                match translate_google_chunk(
                    &client,
                    &chunks[index],
                    &source,
                    &target,
                    stop.as_ref(),
                )
                .await
                {
                    Ok(text) => {
                        if let Ok(mut guard) = results.lock() {
                            guard[index] = text;
                        }
                    }
                    Err(message) => {
                        if let Ok(mut guard) = failure.lock()
                            && guard.is_none()
                        {
                            *guard = Some(message);
                        }
                        break;
                    }
                }
            }
        }));
    }
    for worker in workers {
        if let Err(error) = worker.await {
            bridge.abort();
            return Err(format!("Google translation failed: {error}"));
        }
    }
    bridge.abort();
    if let Some(message) = failure.lock().ok().and_then(|guard| guard.clone()) {
        return Err(message);
    }
    results
        .lock()
        .map(|guard| guard.concat())
        .map_err(|_| "Google translation failed".to_string())
}

async fn translate_google_chunk(
    client: &Client,
    chunk: &str,
    source_lang: &str,
    target_lang: &str,
    cancelled: &impl WaitForCancel,
) -> Result<String, String> {
    let mut url = Url::parse("https://translate.googleapis.com/translate_a/single")
        .map_err(|error| format!("invalid Google translation endpoint: {error}"))?;
    url.query_pairs_mut()
        .append_pair("client", "gtx")
        .append_pair("sl", google_language(source_lang))
        .append_pair("tl", google_language(target_lang))
        .append_pair("dt", "t")
        .append_pair("q", chunk);
    let response = tokio::select! {
        response = client.get(url).send() => response.map_err(|error| format!("Google translation failed: {error}"))?,
        _ = cancelled.cancelled() => return Err("translation request cancelled".into()),
    };
    if !response.status().is_success() {
        return Err(format!("Google HTTP {}", response.status()));
    }
    let payload = response
        .json::<Value>()
        .await
        .map_err(|error| format!("invalid Google translation response: {error}"))?;
    let segments = payload
        .get(0)
        .and_then(Value::as_array)
        .ok_or_else(|| "Google returned no translation".to_string())?;
    let translated = segments
        .iter()
        .filter_map(|segment| segment.get(0).and_then(Value::as_str))
        .collect::<String>();
    if translated.is_empty() {
        Err("Google returned no translation".into())
    } else {
        Ok(translated)
    }
}

trait WaitForCancel {
    fn cancelled(&self) -> impl std::future::Future<Output = ()> + '_;
}

impl WaitForCancel for Notify {
    fn cancelled(&self) -> impl std::future::Future<Output = ()> + '_ {
        self.notified()
    }
}

struct StopFlag {
    flag: AtomicBool,
    notify: Notify,
}

impl StopFlag {
    fn new() -> Self {
        Self {
            flag: AtomicBool::new(false),
            notify: Notify::new(),
        }
    }

    fn cancel(&self) {
        self.flag.store(true, Ordering::Release);
        self.notify.notify_waiters();
    }
}

impl WaitForCancel for StopFlag {
    async fn cancelled(&self) {
        let notified = self.notify.notified();
        if self.flag.load(Ordering::Acquire) {
            return;
        }
        notified.await;
    }
}

async fn translate_bing(
    manager: &TranslationManager,
    client: &Client,
    request: &TranslationRequest,
    cancelled: Arc<Notify>,
) -> Result<String, String> {
    let mut session_guard = manager.bing_session.lock().await;
    let now = unix_millis();
    if session_guard
        .as_ref()
        .is_none_or(|session| session.expires_at <= now)
    {
        *session_guard = Some(fetch_bing_session(client, cancelled.clone(), now).await?);
    }
    let session = session_guard
        .as_mut()
        .ok_or_else(|| "Bing session unavailable".to_string())?;
    session.request_count = session.request_count.saturating_add(2);
    let endpoint = format!(
        "https://cn.bing.com/ttranslatev3?isVertical=1&IG={}&IID=translator.5026.{}",
        session.ig, session.request_count
    );
    let form = [
        ("fromLang", bing_language(&request.source_lang, true)),
        ("to", bing_language(&request.target_lang, false)),
        ("text", request.text.as_str()),
        ("token", session.token.as_str()),
        ("key", session.key.as_str()),
        ("tryFetchingGenderDebiasedTranslations", "true"),
    ];
    let response = tokio::select! {
        response = client.post(endpoint)
            .header("origin", "https://cn.bing.com")
            .header("referer", "https://cn.bing.com/translator")
            .form(&form)
            .send() => response.map_err(|error| format!("Bing translation failed: {error}"))?,
        _ = cancelled.notified() => return Err("translation request cancelled".into()),
    };
    if !response.status().is_success() {
        *session_guard = None;
        return Err(format!("Bing HTTP {}", response.status()));
    }
    let payload = response
        .json::<Value>()
        .await
        .map_err(|error| format!("invalid Bing translation response: {error}"))?;
    payload
        .get(0)
        .and_then(|value| value.get("translations"))
        .and_then(|value| value.get(0))
        .and_then(|value| value.get("text"))
        .and_then(Value::as_str)
        .filter(|value| !value.is_empty())
        .map(str::to_string)
        .ok_or_else(|| "Bing returned no translation".to_string())
}

async fn fetch_bing_session(
    client: &Client,
    cancelled: Arc<Notify>,
    now: u64,
) -> Result<BingSession, String> {
    let response = tokio::select! {
        response = client.get("https://cn.bing.com/translator").send() => response.map_err(|error| format!("Bing session failed: {error}"))?,
        _ = cancelled.notified() => return Err("translation request cancelled".into()),
    };
    if !response.status().is_success() {
        return Err(format!("Bing session HTTP {}", response.status()));
    }
    let html = response
        .text()
        .await
        .map_err(|error| format!("failed to read Bing session: {error}"))?;
    let ig = extract_between(&html, "IG:\"", "\"")
        .filter(|value| value.len() == 32)
        .ok_or_else(|| "Bing session IG unavailable".to_string())?;
    let helper = extract_between(&html, "params_AbusePreventionHelper = [", "]")
        .or_else(|| extract_between(&html, "params_AbusePreventionHelper=[", "]"))
        .ok_or_else(|| "Bing session token unavailable".to_string())?;
    let values = helper.split(',').map(str::trim).collect::<Vec<_>>();
    let lifetime = values
        .get(2)
        .map(|value| value.trim_matches('"'))
        .and_then(|value| value.parse::<u64>().ok())
        .ok_or_else(|| "Bing session token unavailable".to_string())?;
    Ok(BingSession {
        ig: ig.into(),
        key: values[0].trim_matches('"').into(),
        token: values[1].trim_matches('"').into(),
        expires_at: bing_expires_at(lifetime, now),
        request_count: 0,
    })
}

fn unix_millis() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_millis() as u64)
        .unwrap_or(0)
}

fn bing_expires_at(lifetime_ms: u64, now_ms: u64) -> u64 {
    now_ms.saturating_add(lifetime_ms).saturating_sub(60_000)
}

fn translation_provider_order(
    preferred: TranslationProvider,
    skip_preferred: bool,
) -> Vec<TranslationProvider> {
    if preferred == TranslationProvider::DeepL {
        return vec![TranslationProvider::DeepL];
    }
    let alternate = if preferred == TranslationProvider::Google {
        TranslationProvider::Bing
    } else {
        TranslationProvider::Google
    };
    if skip_preferred {
        vec![alternate, preferred]
    } else {
        vec![preferred, alternate]
    }
}

fn validate_request(request: &TranslationRequest) -> Result<(), String> {
    if request.text.trim().is_empty() {
        return Err("translation text must not be empty".into());
    }
    if !(1_000..=60_000).contains(&request.timeout_ms) {
        return Err("translation timeout must be between 1 and 60 seconds".into());
    }
    if !is_language(&request.source_lang, true)
        || !is_language(&request.target_lang, false)
        || request.source_lang == request.target_lang
    {
        return Err("invalid translation language selection".into());
    }
    Ok(())
}

fn split_text(text: &str, max_length: usize) -> Result<Vec<String>, String> {
    if max_length < 2 {
        return Err("Invalid translation chunk size".into());
    }
    let units: Vec<u16> = text.encode_utf16().collect();
    if units.len() <= max_length {
        return Ok(vec![text.to_string()]);
    }
    let mut chunks = Vec::new();
    let mut offset = 0;
    while offset < units.len() {
        let mut end = (offset + max_length).min(units.len());
        if end < units.len() {
            let minimum = offset + max_length / 2;
            let mut cursor = end;
            while cursor > minimum {
                if is_translation_break(units[cursor - 1]) {
                    end = cursor;
                    break;
                }
                cursor -= 1;
            }
            if end > 0 && end < units.len() {
                let previous = units[end - 1];
                let next = units[end];
                if (0xD800..=0xDBFF).contains(&previous) && (0xDC00..=0xDFFF).contains(&next) {
                    end -= 1;
                }
            }
        }
        if end <= offset {
            return Err("Invalid translation chunk size".into());
        }
        chunks.push(String::from_utf16_lossy(&units[offset..end]));
        offset = end;
    }
    Ok(chunks)
}

fn is_translation_break(unit: u16) -> bool {
    matches!(
        unit,
        0x09 | 0x0A
            | 0x0B
            | 0x0C
            | 0x0D
            | 0x20
            | 0x2E
            | 0x21
            | 0x3F
            | 0x3002
            | 0xFF01
            | 0xFF1F
            | 0x2C
            | 0xFF0C
            | 0x3B
            | 0xFF1B
            | 0x3A
            | 0xFF1A
    ) || char::from_u32(u32::from(unit)).is_some_and(char::is_whitespace)
}

fn is_language(value: &str, include_auto: bool) -> bool {
    (include_auto && value == "auto")
        || matches!(
            value,
            "zh-CN"
                | "cht"
                | "en"
                | "yue"
                | "wyw"
                | "jp"
                | "kor"
                | "fra"
                | "spa"
                | "th"
                | "ara"
                | "ru"
                | "pt"
                | "de"
                | "it"
                | "el"
                | "nl"
                | "pl"
                | "bul"
                | "est"
                | "dan"
                | "fin"
                | "cs"
                | "rom"
                | "slo"
                | "swe"
                | "hu"
                | "vie"
        )
}

fn google_language(value: &str) -> &str {
    match value {
        "zh-CN" => "zh-CN",
        "cht" => "zh-TW",
        "jp" => "ja",
        "kor" => "ko",
        "fra" => "fr",
        "spa" => "es",
        "ara" => "ar",
        "bul" => "bg",
        "est" => "et",
        "dan" => "da",
        "fin" => "fi",
        "rom" => "ro",
        "slo" => "sl",
        "swe" => "sv",
        "wyw" => "lzh",
        "vie" => "vi",
        "" => "auto",
        other => other,
    }
}

fn bing_language(value: &str, source: bool) -> &str {
    match value {
        "" | "auto" => {
            if source {
                "auto-detect"
            } else {
                "zh-Hans"
            }
        }
        "zh-CN" => "zh-Hans",
        "cht" => "zh-Hant",
        "wyw" => "lzh",
        "jp" => "ja",
        "kor" => "ko",
        "fra" => "fr",
        "spa" => "es",
        "ara" => "ar",
        "bul" => "bg",
        "est" => "et",
        "dan" => "da",
        "fin" => "fi",
        "rom" => "ro",
        "slo" => "sl",
        "swe" => "sv",
        "vie" => "vi",
        other => other,
    }
}

fn extract_between<'a>(value: &'a str, start: &str, end: &str) -> Option<&'a str> {
    let rest = value.split_once(start)?.1;
    Some(rest.split_once(end)?.0)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn request(text: &str) -> TranslationRequest {
        TranslationRequest {
            request_id: "translation-1".into(),
            text: text.into(),
            source_lang: "auto".into(),
            target_lang: "zh-CN".into(),
            preferred_provider: TranslationProvider::Google,
            timeout_ms: 10_000,
        }
    }

    #[test]
    fn validates_and_splits_unicode_without_losing_content() {
        assert!(validate_request(&request("hello 世界")).is_ok());
        let source = "你🙂好abc";
        assert_eq!(split_text(source, 2).unwrap().concat(), source);
        assert_eq!(
            split_text("hello world", 8).unwrap(),
            vec!["hello ".to_string(), "world".to_string()]
        );
        assert!(split_text(source, 0).is_err());
    }

    #[test]
    fn maps_provider_language_codes() {
        assert_eq!(google_language("jp"), "ja");
        assert_eq!(google_language("wyw"), "lzh");
        assert_eq!(google_language(""), "auto");
        assert_eq!(bing_language("auto", true), "auto-detect");
        assert_eq!(bing_language("auto", false), "zh-Hans");
        assert_eq!(bing_language("wyw", false), "lzh");
        assert_eq!(bing_language("zh-CN", false), "zh-Hans");
        assert_eq!(bing_expires_at(120_000, 1_000), 61_000);
        assert_eq!(
            translation_provider_order(TranslationProvider::Google, false),
            vec![TranslationProvider::Google, TranslationProvider::Bing]
        );
        assert_eq!(
            translation_provider_order(TranslationProvider::Google, true),
            vec![TranslationProvider::Bing, TranslationProvider::Google]
        );
        assert_eq!(
            translation_provider_order(TranslationProvider::DeepL, true),
            vec![TranslationProvider::DeepL]
        );
        let manager = TranslationManager::default();
        assert!(!manager.should_skip_provider(TranslationProvider::Google, 1_000));
        manager.mark_provider_failure(TranslationProvider::Google, 1_000);
        assert!(manager.should_skip_provider(
            TranslationProvider::Google,
            1_000 + PROVIDER_COOLDOWN_MS - 1
        ));
        assert!(
            !manager
                .should_skip_provider(TranslationProvider::Google, 1_000 + PROVIDER_COOLDOWN_MS)
        );
        manager.mark_provider_success(TranslationProvider::Google);
        assert!(!manager.should_skip_provider(TranslationProvider::Google, 1_000));
    }
}
