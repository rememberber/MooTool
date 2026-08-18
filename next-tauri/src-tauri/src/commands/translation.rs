use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use reqwest::{Client, Url, redirect::Policy};
use serde_json::Value;
use tokio::sync::Notify;

use crate::{
    contracts::{
        error::AppResult,
        local_data::TranslationHistory,
        translation::{TranslationProvider, TranslationRequest, TranslationResult},
    },
    repositories::local_data::LocalDataRepository,
};

const MAX_TRANSLATION_CHARS: usize = 50_000;
const GOOGLE_CHUNK_CHARS: usize = 1_800;
const TRANSLATION_USER_AGENT: &str =
    "Mozilla/5.0 (MooTool Next Tauri) AppleWebKit/537.36 Safari/537.36";

#[derive(Clone, Debug)]
struct BingSession {
    ig: String,
    key: String,
    token: String,
    request_count: u32,
}

#[derive(Default)]
pub struct TranslationManager {
    active: Mutex<HashMap<String, Arc<Notify>>>,
    bing_session: tokio::sync::Mutex<Option<BingSession>>,
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
}

#[tauri::command]
pub async fn translate_text(
    manager: tauri::State<'_, TranslationManager>,
    repository: tauri::State<'_, LocalDataRepository>,
    request: TranslationRequest,
) -> AppResult<TranslationResult> {
    validate_request(&request)?;
    let cancelled = manager.register(&request.request_id)?;
    let request_id = request.request_id.clone();
    let source_text = request.text.clone();
    let source_lang = request.source_lang.clone();
    let target_lang = request.target_lang.clone();
    let result = execute_translation(&manager, &request, cancelled).await;
    manager.finish(&request_id);
    let result = result?;
    let timestamp = now_millis();
    let history_suffix = request_id
        .chars()
        .filter(|character| character.is_ascii_alphanumeric() || matches!(character, '-' | '_'))
        .take(80)
        .collect::<String>();
    repository.save_translation_history(TranslationHistory {
        id: format!("{timestamp}-{history_suffix}"),
        source_text,
        target_text: result.text.clone(),
        source_lang,
        target_lang,
        provider: result.provider,
        created_at: timestamp,
    })?;
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
) -> Result<TranslationResult, String> {
    let client = Client::builder()
        .connect_timeout(Duration::from_secs(5))
        .timeout(Duration::from_millis(request.timeout_ms))
        .redirect(Policy::limited(3))
        .user_agent(TRANSLATION_USER_AGENT)
        .build()
        .map_err(|error| format!("failed to create translation client: {error}"))?;
    let providers = match request.preferred_provider {
        TranslationProvider::Google => [TranslationProvider::Google, TranslationProvider::Bing],
        TranslationProvider::Bing => [TranslationProvider::Bing, TranslationProvider::Google],
    };
    let mut last_error = String::new();
    for provider in providers {
        let translated = match provider {
            TranslationProvider::Google => {
                translate_google(&client, request, cancelled.clone()).await
            }
            TranslationProvider::Bing => {
                translate_bing(manager, &client, request, cancelled.clone()).await
            }
        };
        match translated {
            Ok(text) => {
                return Ok(TranslationResult {
                    request_id: request.request_id.clone(),
                    text,
                    provider,
                    fallback_used: provider != request.preferred_provider,
                });
            }
            Err(error) if error == "translation request cancelled" => return Err(error),
            Err(error) => last_error = error,
        }
    }
    Err(if last_error.is_empty() {
        "all translation providers failed".into()
    } else {
        last_error
    })
}

async fn translate_google(
    client: &Client,
    request: &TranslationRequest,
    cancelled: Arc<Notify>,
) -> Result<String, String> {
    let chunks = split_text(&request.text, GOOGLE_CHUNK_CHARS)?;
    let mut output = String::new();
    for chunk in chunks {
        let mut url = Url::parse("https://translate.googleapis.com/translate_a/single")
            .map_err(|error| format!("invalid Google translation endpoint: {error}"))?;
        url.query_pairs_mut()
            .append_pair("client", "gtx")
            .append_pair("sl", google_language(&request.source_lang))
            .append_pair("tl", google_language(&request.target_lang))
            .append_pair("dt", "t")
            .append_pair("q", &chunk);
        let response = tokio::select! {
            response = client.get(url).send() => response.map_err(|error| format!("Google translation failed: {error}"))?,
            _ = cancelled.notified() => return Err("translation request cancelled".into()),
        };
        if !response.status().is_success() {
            return Err(format!("Google translation HTTP {}", response.status()));
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
            return Err("Google returned no translation".into());
        }
        output.push_str(&translated);
    }
    Ok(output)
}

async fn translate_bing(
    manager: &TranslationManager,
    client: &Client,
    request: &TranslationRequest,
    cancelled: Arc<Notify>,
) -> Result<String, String> {
    let mut session_guard = manager.bing_session.lock().await;
    if session_guard.is_none() {
        *session_guard = Some(fetch_bing_session(client, cancelled.clone()).await?);
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
        return Err(format!("Bing translation HTTP {}", response.status()));
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
    if values.len() < 2 {
        return Err("Bing session token unavailable".into());
    }
    Ok(BingSession {
        ig: ig.into(),
        key: values[0].trim_matches('"').into(),
        token: values[1].trim_matches('"').into(),
        request_count: 0,
    })
}

fn validate_request(request: &TranslationRequest) -> Result<(), String> {
    if request.text.trim().is_empty() || request.text.chars().count() > MAX_TRANSLATION_CHARS {
        return Err("translation text must contain 1 to 50000 characters".into());
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

fn split_text(text: &str, max_chars: usize) -> Result<Vec<String>, String> {
    if max_chars == 0 {
        return Err("translation chunk size must be positive".into());
    }
    let mut chunks = Vec::new();
    let mut current = String::new();
    for character in text.chars() {
        current.push(character);
        if current.chars().count() >= max_chars {
            chunks.push(std::mem::take(&mut current));
        }
    }
    if !current.is_empty() {
        chunks.push(current);
    }
    Ok(chunks)
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
        "vie" => "vi",
        other => other,
    }
}

fn bing_language(value: &str, source: bool) -> &str {
    match value {
        "auto" if source => "auto-detect",
        "zh-CN" => "zh-Hans",
        "cht" => "zh-Hant",
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

fn now_millis() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(i64::MAX)
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
        assert!(split_text(source, 0).is_err());
    }

    #[test]
    fn maps_provider_language_codes() {
        assert_eq!(google_language("jp"), "ja");
        assert_eq!(bing_language("auto", true), "auto-detect");
        assert_eq!(bing_language("zh-CN", false), "zh-Hans");
    }
}
