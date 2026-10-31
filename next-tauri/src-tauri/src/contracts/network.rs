use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpHeader {
    #[serde(default)]
    pub id: String,
    pub name: String,
    pub value: String,
    pub enabled: bool,
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpCookie {
    #[serde(default)]
    pub id: String,
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
    #[serde(default)]
    pub saved_id: Option<i64>,
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
    #[serde(default)]
    pub status_text: String,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SaveHttpRequestInput {
    #[serde(default)]
    pub id: Option<i64>,
    pub name: String,
    pub method: String,
    pub url: String,
    #[serde(default)]
    pub params: Vec<HttpHeader>,
    #[serde(default)]
    pub headers: Vec<HttpHeader>,
    #[serde(default)]
    pub cookies: Vec<HttpCookie>,
    pub body: String,
    #[serde(default = "default_body_type")]
    pub body_type: String,
    #[serde(default)]
    pub response_body: String,
    #[serde(default)]
    pub response_headers: String,
    #[serde(default)]
    pub response_cookies: String,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SavedHttpRequest {
    pub id: i64,
    pub name: String,
    pub method: String,
    pub url: String,
    pub params: Vec<HttpHeader>,
    pub headers: Vec<HttpHeader>,
    pub cookies: Vec<HttpCookie>,
    pub body: String,
    pub body_type: String,
    pub response_body: String,
    pub response_headers: String,
    pub response_cookies: String,
    pub create_time: String,
    pub modified_time: String,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HttpRequestHistory {
    pub id: i64,
    pub request_id_value: Option<i64>,
    pub title: String,
    pub name: String,
    pub method: String,
    pub url: String,
    pub params: Vec<HttpHeader>,
    pub headers: Vec<HttpHeader>,
    pub cookies: Vec<HttpCookie>,
    pub body: String,
    pub body_type: String,
    pub response_body: String,
    pub response_headers: String,
    pub response_cookies: String,
    pub status: String,
    pub cost_time: i64,
    pub create_time: String,
    pub modified_time: String,
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

pub fn validate_saved_http_request(input: &SaveHttpRequestInput) -> Result<(), String> {
    if !matches!(input.method.as_str(), "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "HEAD" | "OPTIONS") {
        return Err("Unsupported HTTP method".into());
    }
    if utf16_len(input.name.trim()) > 200
        || utf16_len(&input.url) > 8_192
        || utf16_len(&input.body) > 5 * 1024 * 1024
        || utf16_len(&input.body_type) > 200
        || utf16_len(&input.response_body) > 10 * 1024 * 1024
        || utf16_len(&input.response_headers) > 2 * 1024 * 1024
        || utf16_len(&input.response_cookies) > 2 * 1024 * 1024
    {
        return Err("Invalid string value".into());
    }
    Ok(())
}

fn utf16_len(value: &str) -> usize {
    value.encode_utf16().count()
}

fn default_body_type() -> String {
    "application/json".into()
}
