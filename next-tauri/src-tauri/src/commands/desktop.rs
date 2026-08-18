use std::{
    collections::BTreeSet,
    sync::{
        Mutex,
        atomic::{AtomicBool, Ordering},
    },
};

use serde::Serialize;
use tauri::{
    App, AppHandle, Emitter, Manager, Runtime, Window, WindowEvent,
    menu::{Menu, MenuEvent, MenuItem, SubmenuBuilder},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
};
use tauri_plugin_autostart::ManagerExt;

use crate::{
    contracts::{
        desktop::CloseDecision,
        error::AppResult,
        settings::{AppSettings, CloseBehavior},
    },
    repositories::{
        settings::SettingsRepository,
        window_state::{WINDOW_STATE_FILE_NAME, WindowStateRepository},
    },
};

pub const CLOSE_REQUESTED_EVENT: &str = "mootool://close-requested";
const MENU_SHOW: &str = "mootool-show";
const MENU_SETTINGS: &str = "mootool-settings";
const MENU_HIDE: &str = "mootool-hide-to-tray";
const MENU_QUIT: &str = "mootool-quit";
const TRAY_ID: &str = "mootool-next-tauri-tray";
const AUTOSTART_ARGUMENT: &str = "--mootool-autostart";

#[derive(Default)]
pub struct DesktopLifecycle {
    close_prompt_active: AtomicBool,
    hidden_windows: Mutex<BTreeSet<String>>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct CloseRequestPayload {
    can_minimize_to_tray: bool,
}

pub fn build_application_menu(app: &AppHandle) -> tauri::Result<Menu<tauri::Wry>> {
    let show = MenuItem::with_id(
        app,
        MENU_SHOW,
        "Show MooTool",
        true,
        Some("CmdOrCtrl+Shift+M"),
    )?;
    let settings = MenuItem::with_id(app, MENU_SETTINGS, "Settings…", true, Some("CmdOrCtrl+,"))?;
    let quit = MenuItem::with_id(
        app,
        MENU_QUIT,
        "Quit MooTool Next Tauri",
        true,
        Some("CmdOrCtrl+Q"),
    )?;
    let hide = MenuItem::with_id(
        app,
        MENU_HIDE,
        "Hide to Tray",
        true,
        Some("CmdOrCtrl+Shift+H"),
    )?;
    let application = SubmenuBuilder::new(app, "MooTool")
        .item(&show)
        .item(&settings)
        .item(&hide)
        .separator()
        .item(&quit)
        .build()?;
    let edit = SubmenuBuilder::new(app, "Edit")
        .undo()
        .redo()
        .separator()
        .cut()
        .copy()
        .paste()
        .select_all()
        .build()?;
    let window = SubmenuBuilder::new(app, "Window")
        .minimize()
        .maximize()
        .fullscreen()
        .build()?;
    Menu::with_items(app, &[&application, &edit, &window])
}

pub fn setup(app: &mut App) -> Result<(), String> {
    let state_path = app
        .path()
        .app_config_dir()
        .map_err(|error| format!("failed to resolve Tauri window state directory: {error}"))?
        .join(WINDOW_STATE_FILE_NAME);
    let window_state = WindowStateRepository::open(state_path)?;
    if let Some(main) = app.get_webview_window("main") {
        window_state.restore_window(&main)?;
    }
    app.manage(window_state);

    let tray_show = MenuItem::with_id(app, MENU_SHOW, "Show MooTool", true, None::<&str>)
        .map_err(|error| format!("failed to create tray show item: {error}"))?;
    let tray_settings = MenuItem::with_id(app, MENU_SETTINGS, "Settings…", true, None::<&str>)
        .map_err(|error| format!("failed to create tray settings item: {error}"))?;
    let tray_quit = MenuItem::with_id(
        app,
        MENU_QUIT,
        "Quit MooTool Next Tauri",
        true,
        None::<&str>,
    )
    .map_err(|error| format!("failed to create tray quit item: {error}"))?;
    let tray_hide = MenuItem::with_id(app, MENU_HIDE, "Hide to Tray", true, None::<&str>)
        .map_err(|error| format!("failed to create tray hide item: {error}"))?;
    let tray_menu = Menu::with_items(app, &[&tray_show, &tray_settings, &tray_hide, &tray_quit])
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let mut tray = TrayIconBuilder::with_id(TRAY_ID)
        .menu(&tray_menu)
        .tooltip("MooTool Next Tauri")
        .show_menu_on_left_click(false);
    if let Some(icon) = app.default_window_icon() {
        tray = tray.icon(icon.clone());
    }
    tray.build(app)
        .map_err(|error| format!("failed to create system tray icon: {error}"))?;

    let settings = app.state::<SettingsRepository>().snapshot();
    sync_autostart(app.handle(), &settings)?;
    if std::env::args().any(|argument| argument == AUTOSTART_ARGUMENT) {
        hide_product_windows(app.handle())?;
    }
    Ok(())
}

pub fn handle_menu_event(app: &AppHandle, event: MenuEvent) {
    let result = match event.id().as_ref() {
        MENU_SHOW => show_main_window(app),
        MENU_SETTINGS => {
            super::settings::open_settings_window(app.clone()).map_err(|error| error.to_string())
        }
        MENU_HIDE => hide_product_windows(app),
        MENU_QUIT => {
            app.exit(0);
            Ok(())
        }
        _ => Ok(()),
    };
    if let Err(error) = result {
        eprintln!("MooTool Next Tauri menu action failed: {error}");
    }
}

pub fn handle_tray_event(app: &AppHandle, event: TrayIconEvent) {
    let should_show = matches!(
        event,
        TrayIconEvent::Click {
            button: MouseButton::Left,
            button_state: MouseButtonState::Up,
            ..
        } | TrayIconEvent::DoubleClick {
            button: MouseButton::Left,
            ..
        }
    );
    if should_show && let Err(error) = show_main_window(app) {
        eprintln!("MooTool Next Tauri tray restore failed: {error}");
    }
}

pub fn handle_window_event<R: Runtime>(window: &Window<R>, event: &WindowEvent) {
    let app = window.app_handle();
    if matches!(
        event,
        WindowEvent::Moved(_) | WindowEvent::Resized(_) | WindowEvent::ScaleFactorChanged { .. }
    ) && let Some(repository) = app.try_state::<WindowStateRepository>()
        && let Err(error) = repository.remember_window(window)
    {
        eprintln!("MooTool Next Tauri window state capture failed: {error}");
    }

    if window.label() != "main" {
        return;
    }
    let WindowEvent::CloseRequested { api, .. } = event else {
        return;
    };
    api.prevent_close();
    if let Some(repository) = app.try_state::<WindowStateRepository>() {
        if let Err(error) = repository
            .remember_window(window)
            .and_then(|_| repository.flush())
        {
            eprintln!("MooTool Next Tauri final window state save failed: {error}");
        }
    }

    let behavior = app
        .try_state::<SettingsRepository>()
        .map(|repository| repository.snapshot().general.close_behavior)
        .unwrap_or_default();
    match behavior {
        CloseBehavior::Quit => app.exit(0),
        CloseBehavior::MinimizeToTray => {
            if let Err(error) = hide_product_windows(app) {
                eprintln!("MooTool Next Tauri could not minimize to tray: {error}");
            }
        }
        CloseBehavior::Ask => request_close_confirmation(app),
    }
}

pub fn flush_window_state(app: &AppHandle) {
    if let Some(repository) = app.try_state::<WindowStateRepository>()
        && let Err(error) = repository.flush()
    {
        eprintln!("MooTool Next Tauri could not flush window state: {error}");
    }
}

#[tauri::command]
pub fn resolve_close_request(
    app: AppHandle,
    lifecycle: tauri::State<'_, DesktopLifecycle>,
    decision: CloseDecision,
) -> AppResult<()> {
    lifecycle
        .close_prompt_active
        .store(false, Ordering::Release);
    match decision {
        CloseDecision::Cancel => Ok(()),
        CloseDecision::MinimizeToTray => Ok(hide_product_windows(&app)?),
        CloseDecision::Quit => {
            app.exit(0);
            Ok(())
        }
    }
}

pub fn sync_autostart(app: &AppHandle, settings: &AppSettings) -> Result<(), String> {
    let manager = app.autolaunch();
    let enabled = manager
        .is_enabled()
        .map_err(|error| format!("failed to inspect launch-at-login state: {error}"))?;
    match (settings.general.launch_at_login, enabled) {
        (true, false) => manager
            .enable()
            .map_err(|error| format!("failed to enable launch at login: {error}")),
        (false, true) => manager
            .disable()
            .map_err(|error| format!("failed to disable launch at login: {error}")),
        _ => Ok(()),
    }
}

pub fn show_main_window(app: &AppHandle) -> Result<(), String> {
    #[cfg(target_os = "macos")]
    app.show()
        .map_err(|error| format!("failed to show macOS application: {error}"))?;
    if let Some(lifecycle) = app.try_state::<DesktopLifecycle>() {
        let labels = std::mem::take(
            &mut *lifecycle
                .hidden_windows
                .lock()
                .map_err(|_| "desktop lifecycle state poisoned".to_string())?,
        );
        for label in labels {
            if let Some(window) = app.get_webview_window(&label) {
                window
                    .show()
                    .map_err(|error| format!("failed to restore window {label}: {error}"))?;
            }
        }
    }
    let main = app
        .get_webview_window("main")
        .ok_or_else(|| "main window is unavailable".to_string())?;
    main.show()
        .and_then(|_| main.unminimize())
        .and_then(|_| main.set_focus())
        .map_err(|error| format!("failed to show main window: {error}"))
}

fn request_close_confirmation<R: Runtime>(app: &AppHandle<R>) {
    let Some(lifecycle) = app.try_state::<DesktopLifecycle>() else {
        return;
    };
    if lifecycle
        .close_prompt_active
        .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
        .is_err()
    {
        return;
    }
    if let Err(error) = app.emit_to(
        "main",
        CLOSE_REQUESTED_EVENT,
        CloseRequestPayload {
            can_minimize_to_tray: true,
        },
    ) {
        lifecycle
            .close_prompt_active
            .store(false, Ordering::Release);
        eprintln!("MooTool Next Tauri could not request close confirmation: {error}");
    }
}

fn hide_product_windows<R: Runtime>(app: &AppHandle<R>) -> Result<(), String> {
    let lifecycle = app
        .try_state::<DesktopLifecycle>()
        .ok_or_else(|| "desktop lifecycle state is unavailable".to_string())?;
    let mut hidden = BTreeSet::new();
    for (label, window) in app.webview_windows() {
        if window.is_visible().unwrap_or(false) {
            window
                .hide()
                .map_err(|error| format!("failed to hide window {label}: {error}"))?;
            hidden.insert(label);
        }
    }
    #[cfg(target_os = "macos")]
    app.hide()
        .map_err(|error| format!("failed to hide macOS application: {error}"))?;
    *lifecycle
        .hidden_windows
        .lock()
        .map_err(|_| "desktop lifecycle state poisoned".to_string())? = hidden;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn close_prompt_is_single_flight() {
        let lifecycle = DesktopLifecycle::default();
        assert!(
            lifecycle
                .close_prompt_active
                .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
                .is_ok()
        );
        assert!(
            lifecycle
                .close_prompt_active
                .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
                .is_err()
        );
        lifecycle
            .close_prompt_active
            .store(false, Ordering::Release);
        assert!(!lifecycle.close_prompt_active.load(Ordering::Acquire));
    }
}
