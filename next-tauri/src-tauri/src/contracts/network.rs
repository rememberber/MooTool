use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpHeader {
    pub name: String,
    pub value: String,
    pub enabled: bool,
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpCookie {
    pub name: String,
    pub value: String,
    pub domain: String,
    pub path: String,
    pub expires: String,
    pub enabled: bool,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpRequestSpec {
    pub request_id: String,
    #[serde(default)]
    pub name: String,
    pub method: String,
    pub url: String,
    #[serde(default)]
    pub params: Vec<HttpHeader>,
    pub headers: Vec<HttpHeader>,
    #[serde(default)]
    pub cookies: Vec<HttpCookie>,
    pub body: String,
    #[serde(default = "default_body_type")]
    pub body_type: String,
    pub timeout_ms: u64,
    pub follow_redirects: bool,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpResponseData {
    pub status: u16,
    pub final_url: String,
    pub headers: Vec<(String, String)>,
    pub body_text: String,
    pub body_base64: String,
    pub content_type: String,
    pub size_bytes: usize,
    pub truncated: bool,
    pub duration_ms: u128,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SavedHttpRequest {
    pub id: String,
    pub name: String,
    pub request: HttpRequestSpec,
    pub response: Option<HttpResponseData>,
    pub created_at: i64,
    pub updated_at: i64,
}

impl SavedHttpRequest {
    pub fn validate(&self) -> Result<(), String> {
        validate_record_id(&self.id)?;
        if self.name.trim().is_empty() || self.name.chars().count() > 256 {
            return Err("saved HTTP request name must contain 1 to 256 characters".into());
        }
        if self.created_at < 0 || self.updated_at < self.created_at {
            return Err("invalid saved HTTP request timestamps".into());
        }
        validate_http_request_payload(&self.request)
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpRequestHistory {
    pub id: String,
    pub request: HttpRequestSpec,
    pub response: HttpResponseData,
    pub created_at: i64,
}

impl HttpRequestHistory {
    pub fn validate(&self) -> Result<(), String> {
        validate_record_id(&self.id)?;
        if self.created_at < 0 {
            return Err("invalid HTTP history timestamp".into());
        }
        validate_http_request_payload(&self.request)
    }
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase", tag = "kind")]
pub enum HttpProgressEvent {
    Started,
    Headers { status: u16 },
    Download { received_bytes: usize },
}

pub fn validate_http_request_payload(request: &HttpRequestSpec) -> Result<(), String> {
    if request.request_id.is_empty() || request.request_id.len() > 128 {
        return Err("invalid HTTP request ID".into());
    }
    if request.name.chars().count() > 256
        || request.url.len() > 16 * 1024
        || request.params.len() > 200
        || request.headers.len() > 200
        || request.cookies.len() > 200
        || request.body.len() > 5 * 1024 * 1024
        || request.body_type.len() > 128
    {
        return Err("HTTP request exceeds local limits".into());
    }
    for entry in request.params.iter().chain(&request.headers) {
        if entry.name.len() > 8 * 1024
            || entry.value.len() > 64 * 1024
            || entry.name.chars().any(char::is_control)
        {
            return Err("HTTP request entry exceeds local limits".into());
        }
    }
    for cookie in &request.cookies {
        if cookie.name.len() > 4 * 1024
            || cookie.value.len() > 64 * 1024
            || cookie.domain.len() > 4 * 1024
            || cookie.path.len() > 4 * 1024
            || cookie.expires.len() > 256
            || cookie.name.chars().any(char::is_control)
        {
            return Err("HTTP cookie exceeds local limits".into());
        }
    }
    Ok(())
}

fn validate_record_id(value: &str) -> Result<(), String> {
    if value.is_empty()
        || value.len() > 128
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'-' | b'_'))
    {
        return Err("invalid HTTP local record ID".into());
    }
    Ok(())
}

fn default_body_type() -> String {
    "application/json".into()
}
