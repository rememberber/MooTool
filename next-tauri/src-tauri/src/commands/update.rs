use std::{collections::HashMap, sync::Arc, time::Duration};

use futures_util::StreamExt;
use reqwest::{Client, Url, redirect::Policy};
use serde::Deserialize;
use serde_json::Value;
use tauri::{AppHandle, ipc::Channel};
use tauri_plugin_updater::{Update, UpdaterExt};
use tokio::sync::Notify;

use crate::contracts::{
    error::{AppError, AppResult},
    update::{ProductUpdateCheck, ProductUpdateEvent, ProductUpdateStatus},
};

const PRODUCT_ID: &str = "next-tauri";
const PRODUCT_NAME: &str = "MooTool Next Tauri";
const UPDATE_REGISTRY_URL: &str =
    "https://raw.githubusercontent.com/rememberber/MooTool/master/update-manifest.json";
const UPDATE_MANIFEST_URL: &str =
    "https://github.com/rememberber/MooTool/releases/download/next-tauri-updater/latest.json";
const MAX_REGISTRY_BYTES: usize = 1024 * 1024;
const MAX_RELEASE_NOTES_BYTES: usize = 64 * 1024;

#[derive(Default)]
pub struct ProductUpdateManager {
    pending: std::sync::Mutex<Option<Update>>,
    active_cancel: std::sync::Mutex<Option<Arc<Notify>>>,
}

impl ProductUpdateManager {
    fn pending(&self) -> AppResult<Option<Update>> {
        self.pending
            .lock()
            .map(|pending| pending.clone())
            .map_err(|_| AppError::new("update_state", "Update state is unavailable", true))
    }

    fn replace_pending(&self, update: Option<Update>) -> AppResult<()> {
        *self
            .pending
            .lock()
            .map_err(|_| AppError::new("update_state", "Update state is unavailable", true))? =
            update;
        Ok(())
    }

    fn begin_install(&self) -> AppResult<Arc<Notify>> {
        let mut active = self
            .active_cancel
            .lock()
            .map_err(|_| AppError::new("update_state", "Update state is unavailable", true))?;
        if active.is_some() {
            return Err(AppError::new(
                "update_busy",
                "An update download is already running",
                true,
            ));
        }
        let cancel = Arc::new(Notify::new());
        *active = Some(cancel.clone());
        Ok(cancel)
    }

    fn finish_install(&self) {
        if let Ok(mut active) = self.active_cancel.lock() {
            *active = None;
        }
    }

    fn cancel_install(&self) -> AppResult<bool> {
        let active = self
            .active_cancel
            .lock()
            .map_err(|_| AppError::new("update_state", "Update state is unavailable", true))?;
        if let Some(cancel) = active.as_ref() {
            cancel.notify_waiters();
            Ok(true)
        } else {
            Ok(false)
        }
    }

    fn is_installing(&self) -> AppResult<bool> {
        self.active_cancel
            .lock()
            .map(|active| active.is_some())
            .map_err(|_| AppError::new("update_state", "Update state is unavailable", true))
    }
}

#[derive(Debug, Deserialize)]
struct UpdateRegistry {
    #[serde(rename = "schemaVersion")]
    schema_version: u32,
    products: HashMap<String, Value>,
}

#[derive(Debug, Deserialize)]
struct ProductRegistry {
    #[serde(rename = "displayName")]
    display_name: String,
    status: String,
    #[serde(rename = "updaterManifestUrl")]
    updater_manifest_url: Option<String>,
}

#[derive(Debug)]
enum ProductFeed {
    Inactive,
    Active(Url),
}

#[tauri::command]
pub async fn check_for_product_update(
    app: AppHandle,
    manager: tauri::State<'_, ProductUpdateManager>,
) -> AppResult<ProductUpdateCheck> {
    if manager.is_installing()? {
        return Err(AppError::new(
            "update_busy",
            "Cannot check for updates while an update is downloading",
            true,
        ));
    }

    let current_version = app.package_info().version.to_string();
    let feed = fetch_product_feed().await?;
    let ProductFeed::Active(endpoint) = feed else {
        manager.replace_pending(None)?;
        return Ok(ProductUpdateCheck::inactive(current_version));
    };

    let updater = app
        .updater_builder()
        .endpoints(vec![endpoint])
        .map_err(update_error)?
        .timeout(Duration::from_secs(30))
        .build()
        .map_err(update_error)?;
    let update = updater.check().await.map_err(update_error)?;
    let Some(update) = update else {
        manager.replace_pending(None)?;
        return Ok(ProductUpdateCheck::up_to_date(current_version));
    };

    let result = ProductUpdateCheck {
        status: ProductUpdateStatus::Available,
        current_version,
        latest_version: Some(update.version.clone()),
        release_notes: update.body.as_deref().map(limit_release_notes),
        published_at: update.date.map(|date| date.to_string()),
        release_url: update
            .raw_json
            .get("release_url")
            .and_then(Value::as_str)
            .filter(|value| {
                value
                    .starts_with("https://github.com/rememberber/MooTool/releases/tag/next-tauri-v")
            })
            .map(ToOwned::to_owned),
    };
    tracing::info!(
        product.id = PRODUCT_ID,
        update.current_version = %result.current_version,
        update.latest_version = %result.latest_version.as_deref().unwrap_or_default(),
        "Tauri product update is available"
    );
    manager.replace_pending(Some(update))?;
    Ok(result)
}

#[tauri::command]
pub async fn install_product_update(
    app: AppHandle,
    manager: tauri::State<'_, ProductUpdateManager>,
    on_event: Channel<ProductUpdateEvent>,
) -> AppResult<()> {
    let update = manager.pending()?.ok_or_else(|| {
        AppError::new(
            "update_not_pending",
            "Check for updates before starting an update download",
            false,
        )
    })?;
    let cancel = manager.begin_install()?;
    send_event(&on_event, ProductUpdateEvent::Started);

    let progress_events = on_event.clone();
    let finish_events = on_event.clone();
    let mut downloaded_bytes = 0_u64;
    let download_and_install = update.download_and_install(
        move |chunk_length, content_length| {
            downloaded_bytes = downloaded_bytes.saturating_add(chunk_length as u64);
            send_event(
                &progress_events,
                ProductUpdateEvent::Progress {
                    chunk_length,
                    downloaded_bytes,
                    content_length,
                },
            );
        },
        move || send_event(&finish_events, ProductUpdateEvent::Finished),
    );

    let outcome = tokio::select! {
        result = download_and_install => result.map_err(update_error),
        _ = cancel.notified() => Err(AppError::new("cancelled", "Update download cancelled", false)),
    };
    manager.finish_install();

    match outcome {
        Ok(()) => {
            manager.replace_pending(None)?;
            send_event(&on_event, ProductUpdateEvent::Installed);
            tracing::info!(product.id = PRODUCT_ID, "Tauri product update installed");
            commands_before_restart(&app);
            Ok(())
        }
        Err(error) if error.code == "cancelled" => {
            send_event(&on_event, ProductUpdateEvent::Cancelled);
            Err(error)
        }
        Err(error) => Err(error),
    }
}

#[tauri::command]
pub fn cancel_product_update(manager: tauri::State<'_, ProductUpdateManager>) -> AppResult<bool> {
    manager.cancel_install()
}

#[tauri::command]
pub fn relaunch_after_product_update(app: AppHandle) -> AppResult<()> {
    commands_before_restart(&app);
    app.restart()
}

fn commands_before_restart(app: &AppHandle) {
    super::desktop::flush_window_state(app);
}

async fn fetch_product_feed() -> AppResult<ProductFeed> {
    let client = Client::builder()
        .https_only(true)
        .redirect(Policy::limited(3))
        .timeout(Duration::from_secs(15))
        .build()
        .map_err(|error| update_network_error("create update registry client", error))?;
    let response = client
        .get(UPDATE_REGISTRY_URL)
        .header("Accept", "application/json")
        .send()
        .await
        .map_err(|error| update_network_error("fetch update registry", error))?
        .error_for_status()
        .map_err(|error| update_network_error("fetch update registry", error))?;
    let registry_bytes = read_limited(response, MAX_REGISTRY_BYTES).await?;
    let registry: UpdateRegistry = serde_json::from_slice(&registry_bytes).map_err(|error| {
        AppError::new(
            "update_manifest_invalid",
            format!("Tauri update registry is invalid: {error}"),
            false,
        )
    })?;
    product_feed_from_registry(registry)
}

fn product_feed_from_registry(mut registry: UpdateRegistry) -> AppResult<ProductFeed> {
    if registry.schema_version != 1 {
        return Err(AppError::new(
            "update_manifest_invalid",
            "Unsupported Tauri update registry schema",
            false,
        ));
    }
    let product_value = registry.products.remove(PRODUCT_ID).ok_or_else(|| {
        AppError::new(
            "update_product_missing",
            "The next-tauri update product is missing",
            false,
        )
    })?;
    let product: ProductRegistry = serde_json::from_value(product_value).map_err(|error| {
        AppError::new(
            "update_manifest_invalid",
            format!("The next-tauri update product is invalid: {error}"),
            false,
        )
    })?;
    if product.display_name != PRODUCT_NAME {
        return Err(AppError::new(
            "update_product_mismatch",
            "The update product identity does not match MooTool Next Tauri",
            false,
        ));
    }
    if product.status == "planned" {
        return Ok(ProductFeed::Inactive);
    }
    if product.status != "active" {
        return Err(AppError::new(
            "update_product_inactive",
            "The next-tauri update product is not active",
            false,
        ));
    }
    let endpoint = product.updater_manifest_url.ok_or_else(|| {
        AppError::new(
            "update_manifest_missing",
            "The next-tauri updater manifest URL is missing",
            false,
        )
    })?;
    if endpoint != UPDATE_MANIFEST_URL {
        return Err(AppError::new(
            "update_manifest_untrusted",
            "The next-tauri updater manifest URL is not trusted",
            false,
        ));
    }
    let endpoint = Url::parse(&endpoint).map_err(|error| {
        AppError::new(
            "update_manifest_invalid",
            format!("The next-tauri updater manifest URL is invalid: {error}"),
            false,
        )
    })?;
    Ok(ProductFeed::Active(endpoint))
}

async fn read_limited(response: reqwest::Response, limit: usize) -> AppResult<Vec<u8>> {
    if response
        .content_length()
        .is_some_and(|length| length > limit as u64)
    {
        return Err(AppError::new(
            "update_manifest_too_large",
            "The Tauri update registry is too large",
            false,
        ));
    }
    let mut bytes = Vec::new();
    let mut stream = response.bytes_stream();
    while let Some(chunk) = stream.next().await {
        let chunk = chunk.map_err(|error| update_network_error("read update registry", error))?;
        if bytes.len().saturating_add(chunk.len()) > limit {
            return Err(AppError::new(
                "update_manifest_too_large",
                "The Tauri update registry is too large",
                false,
            ));
        }
        bytes.extend_from_slice(&chunk);
    }
    Ok(bytes)
}

fn send_event(channel: &Channel<ProductUpdateEvent>, event: ProductUpdateEvent) {
    if let Err(error) = channel.send(event) {
        tracing::warn!(error = %error, "failed to publish update progress");
    }
}

fn limit_release_notes(value: &str) -> String {
    if value.len() <= MAX_RELEASE_NOTES_BYTES {
        return value.to_owned();
    }
    let mut boundary = MAX_RELEASE_NOTES_BYTES;
    while !value.is_char_boundary(boundary) {
        boundary -= 1;
    }
    format!("{}…", &value[..boundary])
}

fn update_network_error(action: &str, error: impl std::fmt::Display) -> AppError {
    AppError::new(
        "update_network",
        format!("Failed to {action}: {error}"),
        true,
    )
}

fn update_error(error: impl std::fmt::Display) -> AppError {
    AppError::new(
        "update_failed",
        format!("Tauri update operation failed: {error}"),
        true,
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    fn registry(status: &str, endpoint: Option<&str>) -> UpdateRegistry {
        let mut products = HashMap::new();
        products.insert(
            PRODUCT_ID.to_owned(),
            serde_json::json!({
                "displayName": PRODUCT_NAME,
                "status": status,
                "updaterManifestUrl": endpoint,
                "releases": []
            }),
        );
        products.insert(
            "next-electron".to_owned(),
            serde_json::json!({
                "displayName": "MooTool Next Electron",
                "status": "active",
                "updaterManifestUrl": "https://example.invalid/electron.json"
            }),
        );
        UpdateRegistry {
            schema_version: 1,
            products,
        }
    }

    #[test]
    fn planned_product_does_not_contact_an_update_endpoint() {
        assert!(matches!(
            product_feed_from_registry(registry("planned", Some(UPDATE_MANIFEST_URL)))
                .expect("planned feed"),
            ProductFeed::Inactive
        ));
    }

    #[test]
    fn active_product_accepts_only_the_tauri_channel_endpoint() {
        let feed = product_feed_from_registry(registry("active", Some(UPDATE_MANIFEST_URL)))
            .expect("active feed");
        assert!(matches!(feed, ProductFeed::Active(url) if url.as_str() == UPDATE_MANIFEST_URL));
        let error = product_feed_from_registry(registry(
            "active",
            Some("https://example.invalid/electron.json"),
        ))
        .expect_err("foreign endpoint must be rejected");
        assert_eq!(error.code, "update_manifest_untrusted");
    }

    #[test]
    fn product_identity_and_schema_are_enforced() {
        let mut wrong_schema = registry("active", Some(UPDATE_MANIFEST_URL));
        wrong_schema.schema_version = 2;
        assert_eq!(
            product_feed_from_registry(wrong_schema)
                .expect_err("schema must be rejected")
                .code,
            "update_manifest_invalid"
        );

        let mut wrong_product = registry("active", Some(UPDATE_MANIFEST_URL));
        wrong_product.products.insert(
            PRODUCT_ID.to_owned(),
            serde_json::json!({
                "displayName": "MooTool Next Electron",
                "status": "active",
                "updaterManifestUrl": UPDATE_MANIFEST_URL
            }),
        );
        assert_eq!(
            product_feed_from_registry(wrong_product)
                .expect_err("identity must be rejected")
                .code,
            "update_product_mismatch"
        );
    }

    #[test]
    fn release_notes_are_truncated_on_a_unicode_boundary() {
        let notes = "界".repeat(MAX_RELEASE_NOTES_BYTES);
        let limited = limit_release_notes(&notes);
        assert!(limited.ends_with('…'));
        assert!(limited.len() <= MAX_RELEASE_NOTES_BYTES + '…'.len_utf8());
    }
}
