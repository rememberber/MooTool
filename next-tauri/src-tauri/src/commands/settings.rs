use tauri::{Emitter, Manager, WebviewUrl, WebviewWindowBuilder};

use crate::{
    contracts::{error::AppResult, settings::AppSettings},
    repositories::settings::SettingsRepository,
};

pub const SETTINGS_CHANGED_EVENT: &str = "mootool://settings-changed";

#[tauri::command]
pub fn get_settings(repository: tauri::State<'_, SettingsRepository>) -> AppSettings {
    repository.snapshot()
}

#[tauri::command]
pub fn update_settings(
    app: tauri::AppHandle,
    repository: tauri::State<'_, SettingsRepository>,
    settings: AppSettings,
) -> AppResult<AppSettings> {
    let saved = repository.replace(settings)?;
    app.emit(SETTINGS_CHANGED_EVENT, &saved)
        .map_err(|error| format!("settings were saved but synchronization failed: {error}"))?;
    super::desktop::sync_autostart(&app, &saved)
        .map_err(|error| format!("settings were saved but launch-at-login sync failed: {error}"))?;
    Ok(saved)
}

#[tauri::command]
pub fn reset_settings(
    app: tauri::AppHandle,
    repository: tauri::State<'_, SettingsRepository>,
) -> AppResult<AppSettings> {
    let saved = repository.reset()?;
    app.emit(SETTINGS_CHANGED_EVENT, &saved)
        .map_err(|error| format!("settings were reset but synchronization failed: {error}"))?;
    super::desktop::sync_autostart(&app, &saved)
        .map_err(|error| format!("settings were reset but launch-at-login sync failed: {error}"))?;
    Ok(saved)
}

#[tauri::command]
pub fn open_settings_window(app: tauri::AppHandle) -> AppResult<()> {
    if let Some(window) = app.get_webview_window("settings") {
        window
            .show()
            .and_then(|_| window.set_focus())
            .map_err(|error| format!("failed to focus settings window: {error}"))?;
        return Ok(());
    }

    WebviewWindowBuilder::new(
        &app,
        "settings",
        WebviewUrl::App("index.html?surface=settings".into()),
    )
    .title("MooTool Next Tauri Settings")
    .title_bar_style(tauri::TitleBarStyle::Overlay)
    .hidden_title(true)
    .inner_size(760.0, 720.0)
    .min_inner_size(660.0, 600.0)
    .center()
    .build()
    .map_err(|error| format!("failed to open settings window: {error}"))?;
    Ok(())
}
