use keyring::{Entry, Error as KeyringError};
use serde::Serialize;

use crate::contracts::error::AppResult;

const CREDENTIAL_SERVICE: &str = "com.rememberber.mootool";
const PROXY_PASSWORD_ACCOUNT: &str = "network.proxy.password";
const MAX_PASSWORD_BYTES: usize = 4_096;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProxyCredentialStatus {
    stored: bool,
    secure_store: String,
}

#[tauri::command]
pub async fn get_proxy_credential_status() -> AppResult<ProxyCredentialStatus> {
    Ok(tauri::async_runtime::spawn_blocking(|| {
        let stored = match proxy_entry()?.get_password() {
            Ok(_) => true,
            Err(KeyringError::NoEntry) => false,
            Err(error) => return Err(format!("failed to read OS credential store: {error}")),
        };
        Ok(ProxyCredentialStatus {
            stored,
            secure_store: secure_store_name().into(),
        })
    })
    .await
    .map_err(|error| format!("proxy credential task failed: {error}"))??)
}

#[tauri::command]
pub async fn set_proxy_password(password: String) -> AppResult<ProxyCredentialStatus> {
    validate_password(&password)?;
    Ok(tauri::async_runtime::spawn_blocking(move || {
        let entry = proxy_entry()?;
        if password.is_empty() {
            match entry.delete_credential() {
                Ok(()) | Err(KeyringError::NoEntry) => {}
                Err(error) => return Err(format!("failed to clear OS credential: {error}")),
            }
        } else {
            entry.set_password(&password).map_err(|error| {
                format!("failed to save password in OS credential store: {error}")
            })?;
        }
        Ok(ProxyCredentialStatus {
            stored: !password.is_empty(),
            secure_store: secure_store_name().into(),
        })
    })
    .await
    .map_err(|error| format!("proxy credential task failed: {error}"))??)
}

pub async fn load_proxy_password() -> Result<Option<String>, String> {
    tauri::async_runtime::spawn_blocking(|| match proxy_entry()?.get_password() {
        Ok(password) => Ok(Some(password)),
        Err(KeyringError::NoEntry) => Ok(None),
        Err(error) => Err(format!(
            "failed to read proxy password from OS credential store: {error}"
        )),
    })
    .await
    .map_err(|error| format!("proxy credential task failed: {error}"))?
}

fn proxy_entry() -> Result<Entry, String> {
    Entry::new(CREDENTIAL_SERVICE, PROXY_PASSWORD_ACCOUNT)
        .map_err(|error| format!("OS credential store is unavailable: {error}"))
}

fn validate_password(password: &str) -> Result<(), String> {
    if password.len() > MAX_PASSWORD_BYTES {
        return Err("proxy password cannot exceed 4096 bytes".into());
    }
    Ok(())
}

fn secure_store_name() -> &'static str {
    #[cfg(target_os = "macos")]
    return "macOS Keychain";
    #[cfg(target_os = "windows")]
    return "Windows Credential Manager";
    #[cfg(target_os = "linux")]
    return "Secret Service";
    #[allow(unreachable_code)]
    "OS credential store"
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bounds_proxy_password_size_without_inspecting_contents() {
        assert!(validate_password("p@ss\nword").is_ok());
        assert!(validate_password(&"a".repeat(MAX_PASSWORD_BYTES)).is_ok());
        assert!(validate_password(&"a".repeat(MAX_PASSWORD_BYTES + 1)).is_err());
    }
}
