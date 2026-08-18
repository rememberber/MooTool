use serde::Serialize;

pub type AppResult<T> = Result<T, AppError>;

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AppError {
    pub code: String,
    pub message: String,
    pub retryable: bool,
}

impl AppError {
    pub fn new(code: impl Into<String>, message: impl Into<String>, retryable: bool) -> Self {
        Self {
            code: code.into(),
            message: message.into(),
            retryable,
        }
    }

    pub fn from_message(message: impl Into<String>) -> Self {
        let message = message.into();
        let lower = message.to_ascii_lowercase();
        let (code, retryable) = if lower.contains("cancel") || lower.contains("取消") {
            ("cancelled", false)
        } else if lower.contains("timeout") || lower.contains("timed out") || lower.contains("超时")
        {
            ("timeout", true)
        } else if lower.contains("permission")
            || lower.contains("not permitted")
            || lower.contains("access denied")
            || lower.contains("权限")
        {
            ("permission_denied", false)
        } else if lower.contains("not found")
            || lower.contains("unavailable")
            || lower.contains("不存在")
        {
            ("not_found", false)
        } else if lower.contains("conflict")
            || lower.contains("changed since")
            || lower.contains("冲突")
        {
            ("conflict", true)
        } else if lower.contains("network")
            || lower.contains("request failed")
            || lower.contains("dns")
            || lower.contains("连接")
        {
            ("network", true)
        } else if lower.contains("database")
            || lower.contains("sqlite")
            || lower.contains("failed to read")
            || lower.contains("failed to write")
            || lower.contains("failed to create")
        {
            ("storage", true)
        } else if lower.contains("invalid")
            || lower.contains("must be")
            || lower.contains("too many")
            || lower.contains("too large")
            || lower.contains("unsupported")
        {
            ("validation", false)
        } else {
            ("internal", false)
        };
        let error = Self::new(code, message, retryable);
        tracing::error!(
            error.code = %error.code,
            error.retryable = error.retryable,
            error.message = %redact_for_log(&error.message),
            "Tauri command failed"
        );
        error
    }
}

impl From<String> for AppError {
    fn from(value: String) -> Self {
        Self::from_message(value)
    }
}

impl From<&str> for AppError {
    fn from(value: &str) -> Self {
        Self::from_message(value)
    }
}

impl std::fmt::Display for AppError {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str(&self.message)
    }
}

impl std::error::Error for AppError {}

pub fn redact_for_log(value: &str) -> String {
    let mut result = value.replace(['\n', '\r'], " ");
    if let Ok(home) = std::env::var("HOME") {
        if !home.is_empty() {
            result = result.replace(&home, "~");
        }
    }
    for marker in [
        "authorization=",
        "authorization:",
        "password=",
        "passwd=",
        "token=",
        "secret=",
        "api_key=",
        "apikey=",
    ] {
        result = redact_after_marker(&result, marker);
    }
    if result.len() > 8_192 {
        let mut boundary = 8_192;
        while !result.is_char_boundary(boundary) {
            boundary -= 1;
        }
        result.truncate(boundary);
        result.push('…');
    }
    result
}

fn redact_after_marker(value: &str, marker: &str) -> String {
    let mut redacted = value.to_string();
    let mut search_from = 0;
    loop {
        let lower = redacted.to_ascii_lowercase();
        let Some(relative_start) = lower[search_from..].find(marker) else {
            break;
        };
        let marker_end = search_from + relative_start + marker.len();
        let leading = redacted[marker_end..]
            .chars()
            .take_while(|character| character.is_whitespace() || matches!(character, '\'' | '"'))
            .map(char::len_utf8)
            .sum::<usize>();
        let secret_start = marker_end + leading;
        let authorization = marker.starts_with("authorization");
        let secret_length = redacted[secret_start..]
            .find(|character: char| {
                matches!(character, '&' | ';' | ',' | '\'' | '"')
                    || (!authorization && character.is_whitespace())
            })
            .unwrap_or(redacted.len() - secret_start);
        if secret_length == 0 {
            search_from = secret_start;
            continue;
        }
        redacted.replace_range(secret_start..secret_start + secret_length, "<redacted>");
        search_from = secret_start + "<redacted>".len();
    }
    redacted
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classifies_errors_for_the_ipc_boundary() {
        assert_eq!(AppError::from_message("request timed out").code, "timeout");
        assert_eq!(
            AppError::from_message("editor font size must be between 10 and 24").code,
            "validation"
        );
        assert_eq!(
            AppError::from_message("permission denied while writing hosts").code,
            "permission_denied"
        );
    }

    #[test]
    fn redacts_secret_values_and_home_paths_from_logs() {
        let home = std::env::var("HOME").unwrap_or_default();
        let value = format!("failed at {home}/data?token=abc123&mode=test password=hunter2");
        let redacted = redact_for_log(&value);
        assert!(!redacted.contains("abc123"));
        assert!(!redacted.contains("hunter2"));
        if !home.is_empty() {
            assert!(!redacted.contains(&home));
        }
    }

    #[test]
    fn redacts_authorization_headers_and_truncates_unicode_safely() {
        let redacted = redact_for_log(&format!(
            "Authorization: Bearer highly-secret, payload={}",
            "界".repeat(4_000)
        ));
        assert!(!redacted.contains("highly-secret"));
        assert!(redacted.contains("Authorization: <redacted>"));
        assert!(redacted.len() <= 8_195);
    }
}
