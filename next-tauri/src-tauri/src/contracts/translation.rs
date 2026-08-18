use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, Deserialize, Eq, Hash, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum TranslationProvider {
    Google,
    Bing,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslationRequest {
    pub request_id: String,
    pub text: String,
    pub source_lang: String,
    pub target_lang: String,
    pub preferred_provider: TranslationProvider,
    pub timeout_ms: u64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslationResult {
    pub request_id: String,
    pub text: String,
    pub provider: TranslationProvider,
    pub fallback_used: bool,
}
