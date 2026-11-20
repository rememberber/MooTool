use std::path::PathBuf;

use tauri::{Emitter, Manager, WebviewUrl, WebviewWindowBuilder};

use crate::{
    contracts::{error::AppResult, settings::AppSettings},
    repositories::settings::SettingsRepository,
};

pub const SETTINGS_CHANGED_EVENT: &str = "mootool://settings-changed";

fn reopen_data_directory(
    app: &tauri::AppHandle,
    previous: &AppSettings,
    saved: &AppSettings,
) -> Result<(), String> {
    if previous.data.directory.trim() == saved.data.directory.trim()
        || app
            .try_state::<crate::commands::native_acceptance::NativeAcceptanceConfig>()
            .is_some()
    {
        return Ok(());
    }
    let Some(local_data) = app.try_state::<crate::repositories::local_data::LocalDataRepository>()
    else {
        return Ok(());
    };
    local_data.reopen(database_file(app, saved)?)?;
    crate::commands::desktop::remember_active_host(app, None)?;
    crate::commands::desktop::refresh_tray(app)?;
    if let Some(repository) = app.try_state::<crate::repositories::vault::VaultRepository>() {
        crate::commands::vault::configure_from_settings(app, &repository, saved, None);
    }
    Ok(())
}

pub fn database_file(app: &tauri::AppHandle, settings: &AppSettings) -> Result<PathBuf, String> {
    Ok(data_directory(app, settings)?.join(crate::repositories::local_data::DATABASE_FILE_NAME))
}

pub fn images_directory(app: &tauri::AppHandle, settings: &AppSettings) -> Result<PathBuf, String> {
    Ok(data_directory(app, settings)?.join("images"))
}

pub fn data_directory(app: &tauri::AppHandle, settings: &AppSettings) -> Result<PathBuf, String> {
    let configured = settings.data.directory.trim();
    if configured.is_empty() {
        app.path()
            .app_data_dir()
            .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))
    } else {
        Ok(PathBuf::from(configured))
    }
}

pub fn configured_export_directory(repository: &SettingsRepository) -> Option<PathBuf> {
    let value = repository.snapshot().tools.export_directory;
    if value.is_empty() {
        return None;
    }
    let path = PathBuf::from(value);
    path.is_dir().then_some(path)
}

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
    let previous = repository.snapshot();
    let saved = repository.replace(settings)?;
    if let Err(error) = reopen_data_directory(&app, &previous, &saved) {
        let _ = repository.replace(previous);
        return Err(error.into());
    }
    app.emit(SETTINGS_CHANGED_EVENT, &saved)
        .map_err(|error| format!("settings were saved but synchronization failed: {error}"))?;
    super::desktop::sync_autostart(&app, &saved)
        .map_err(|error| format!("settings were saved but launch-at-login sync failed: {error}"))?;
    super::desktop::sync_desktop_preferences(&app, &saved).map_err(|error| {
        format!("settings were saved but desktop preferences sync failed: {error}")
    })?;
    super::global_shortcut::sync_global_shortcut(&app, &saved);
    sync_json_vault(&app, &saved);
    Ok(saved)
}

#[tauri::command]
pub fn reset_settings(
    app: tauri::AppHandle,
    repository: tauri::State<'_, SettingsRepository>,
) -> AppResult<AppSettings> {
    let previous = repository.snapshot();
    let saved = repository.reset()?;
    if let Err(error) = reopen_data_directory(&app, &previous, &saved) {
        let _ = repository.replace(previous);
        return Err(error.into());
    }
    app.emit(SETTINGS_CHANGED_EVENT, &saved)
        .map_err(|error| format!("settings were reset but synchronization failed: {error}"))?;
    super::desktop::sync_autostart(&app, &saved)
        .map_err(|error| format!("settings were reset but launch-at-login sync failed: {error}"))?;
    super::desktop::sync_desktop_preferences(&app, &saved).map_err(|error| {
        format!("settings were reset but desktop preferences sync failed: {error}")
    })?;
    super::global_shortcut::sync_global_shortcut(&app, &saved);
    sync_json_vault(&app, &saved);
    Ok(saved)
}

fn sync_json_vault(app: &tauri::AppHandle, settings: &AppSettings) {
    if let Some(repository) = app.try_state::<crate::repositories::vault::VaultRepository>() {
        crate::commands::vault::configure_from_settings(app, &repository, settings, None);
    }
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

    let builder = WebviewWindowBuilder::new(
        &app,
        "settings",
        WebviewUrl::App("index.html?surface=settings".into()),
    )
    .title("MooTool Next Tauri Settings");

    #[cfg(target_os = "macos")]
    let builder = builder
        .title_bar_style(tauri::TitleBarStyle::Overlay)
        .hidden_title(true);

    builder
        .inner_size(760.0, 720.0)
        .min_inner_size(660.0, 600.0)
        .center()
        .build()
        .map_err(|error| format!("failed to open settings window: {error}"))?;
    Ok(())
}
