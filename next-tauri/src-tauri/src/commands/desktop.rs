use std::{
    collections::BTreeSet,
    path::Path,
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
        settings::{AppLanguage, AppSettings, CloseBehavior},
    },
    repositories::{
        settings::SettingsRepository,
        window_state::{WINDOW_STATE_FILE_NAME, WindowStateRepository},
    },
};

pub const CLOSE_REQUESTED_EVENT: &str = "mootool://close-requested";
const MENU_SHOW: &str = "mootool-show";
const MENU_SEARCH: &str = "mootool-search";
const MENU_SETTINGS: &str = "mootool-settings";
const MENU_HIDE: &str = "mootool-hide-to-tray";
const MENU_QUIT: &str = "mootool-quit";
const TRAY_ID: &str = "mootool-next-tauri-tray";
const AUTOSTART_ARGUMENT: &str = "--mootool-autostart";
const TRAY_ICON_BYTES: &[u8] = include_bytes!("../../icons/tray-icon.png");

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

#[derive(Clone, Copy)]
struct MenuLabels {
    show: &'static str,
    search: &'static str,
    settings: &'static str,
    hide: &'static str,
    quit: &'static str,
    edit: &'static str,
    window: &'static str,
}

fn menu_labels(language: AppLanguage) -> MenuLabels {
    match language {
        AppLanguage::SimplifiedChinese => MenuLabels {
            show: "显示 MooTool",
            search: "搜索工具…",
            settings: "设置…",
            hide: "隐藏到托盘",
            quit: "退出 MooTool Next Tauri",
            edit: "编辑",
            window: "窗口",
        },
        AppLanguage::English => MenuLabels {
            show: "Show MooTool",
            search: "Search Tools…",
            settings: "Settings…",
            hide: "Hide to Tray",
            quit: "Quit MooTool Next Tauri",
            edit: "Edit",
            window: "Window",
        },
        AppLanguage::Japanese => MenuLabels {
            show: "MooTool を表示",
            search: "ツールを検索…",
            settings: "設定…",
            hide: "トレイへ隠す",
            quit: "MooTool Next Tauri を終了",
            edit: "編集",
            window: "ウィンドウ",
        },
    }
}

fn load_tray_icon() -> Result<tauri::image::Image<'static>, String> {
    let rgba = image::load_from_memory_with_format(TRAY_ICON_BYTES, image::ImageFormat::Png)
        .map_err(|error| format!("failed to decode system tray icon: {error}"))?
        .to_rgba8();
    let (width, height) = rgba.dimensions();
    Ok(tauri::image::Image::new_owned(
        rgba.into_raw(),
        width,
        height,
    ))
}

pub fn build_application_menu(app: &AppHandle) -> tauri::Result<Menu<tauri::Wry>> {
    build_application_menu_for(app, &AppSettings::default())
}

fn build_application_menu_for(
    app: &AppHandle,
    settings_value: &AppSettings,
) -> tauri::Result<Menu<tauri::Wry>> {
    let labels = menu_labels(settings_value.general.language);
    let show = MenuItem::with_id(app, MENU_SHOW, labels.show, true, Some("CmdOrCtrl+Shift+M"))?;
    let search = MenuItem::with_id(
        app,
        MENU_SEARCH,
        labels.search,
        true,
        Some(settings_value.shortcuts.global_search.as_str()),
    )?;
    let settings = MenuItem::with_id(
        app,
        MENU_SETTINGS,
        labels.settings,
        true,
        Some(settings_value.shortcuts.settings.as_str()),
    )?;
    let quit = MenuItem::with_id(app, MENU_QUIT, labels.quit, true, Some("CmdOrCtrl+Q"))?;
    let hide = MenuItem::with_id(
        app,
        MENU_HIDE,
        labels.hide,
        settings_value.general.tray_enabled,
        Some("CmdOrCtrl+Shift+H"),
    )?;
    let application = SubmenuBuilder::new(app, "MooTool")
        .item(&show)
        .item(&search)
        .item(&settings)
        .item(&hide)
        .separator()
        .item(&quit)
        .build()?;
    let edit = SubmenuBuilder::new(app, labels.edit)
        .undo()
        .redo()
        .separator()
        .cut()
        .copy()
        .paste()
        .select_all()
        .build()?;
    let window = SubmenuBuilder::new(app, labels.window)
        .minimize()
        .maximize()
        .fullscreen()
        .build()?;
    Menu::with_items(app, &[&application, &edit, &window])
}

pub fn setup(
    app: &mut App,
    state_directory_override: Option<&Path>,
    synchronize_autostart: bool,
) -> Result<(), String> {
    let settings = app.state::<SettingsRepository>().snapshot();
    let state_path = match state_directory_override {
        Some(directory) => directory.join(WINDOW_STATE_FILE_NAME),
        None => app
            .path()
            .app_config_dir()
            .map_err(|error| format!("failed to resolve Tauri window state directory: {error}"))?
            .join(WINDOW_STATE_FILE_NAME),
    };
    let window_state = WindowStateRepository::open(state_path)?;
    if let Some(main) = app.get_webview_window("main") {
        window_state.restore_window(&main)?;
        if settings.general.start_maximized {
            main.maximize()
                .map_err(|error| format!("failed to maximize main window: {error}"))?;
        }
    }
    app.manage(window_state);

    let labels = menu_labels(settings.general.language);
    let tray_show = MenuItem::with_id(app, MENU_SHOW, labels.show, true, None::<&str>)
        .map_err(|error| format!("failed to create tray show item: {error}"))?;
    let tray_search = MenuItem::with_id(app, MENU_SEARCH, labels.search, true, None::<&str>)
        .map_err(|error| format!("failed to create tray search item: {error}"))?;
    let tray_settings = MenuItem::with_id(app, MENU_SETTINGS, labels.settings, true, None::<&str>)
        .map_err(|error| format!("failed to create tray settings item: {error}"))?;
    let tray_quit = MenuItem::with_id(app, MENU_QUIT, labels.quit, true, None::<&str>)
        .map_err(|error| format!("failed to create tray quit item: {error}"))?;
    let tray_hide = MenuItem::with_id(app, MENU_HIDE, labels.hide, true, None::<&str>)
        .map_err(|error| format!("failed to create tray hide item: {error}"))?;
    let tray_menu = Menu::with_items(
        app,
        &[
            &tray_show,
            &tray_search,
            &tray_settings,
            &tray_hide,
            &tray_quit,
        ],
    )
    .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let tray = TrayIconBuilder::with_id(TRAY_ID)
        .menu(&tray_menu)
        .icon(load_tray_icon()?)
        .icon_as_template(cfg!(target_os = "macos"))
        .tooltip("MooTool Next Tauri")
        .show_menu_on_left_click(false)
        .build(app)
        .map_err(|error| format!("failed to create system tray icon: {error}"))?;
    tray.set_visible(settings.general.tray_enabled)
        .map_err(|error| format!("failed to apply system tray visibility: {error}"))?;

    sync_desktop_preferences(app.handle(), &settings)?;
    if synchronize_autostart {
        sync_autostart(app.handle(), &settings)?;
    }
    if std::env::args().any(|argument| argument == AUTOSTART_ARGUMENT) {
        hide_product_windows(app.handle())?;
    }
    Ok(())
}

pub fn handle_menu_event(app: &AppHandle, event: MenuEvent) {
    let result = match event.id().as_ref() {
        MENU_SHOW => show_main_window(app),
        MENU_SEARCH => show_main_window(app).and_then(|_| {
            app.emit_to("main", "mootool://open-command-palette", ())
                .map_err(|error| format!("failed to open command palette: {error}"))
        }),
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

    let settings = app
        .try_state::<SettingsRepository>()
        .map(|repository| repository.snapshot())
        .unwrap_or_default();
    match settings.general.close_behavior {
        CloseBehavior::Quit => app.exit(0),
        CloseBehavior::MinimizeToTray => {
            if !settings.general.tray_enabled {
                app.exit(0);
            } else if let Err(error) = hide_product_windows(app) {
                eprintln!("MooTool Next Tauri could not minimize to tray: {error}");
            }
        }
        CloseBehavior::Ask => request_close_confirmation(app, settings.general.tray_enabled),
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

pub fn sync_desktop_preferences(app: &AppHandle, settings: &AppSettings) -> Result<(), String> {
    let menu = build_application_menu_for(app, settings)
        .map_err(|error| format!("failed to rebuild application menu: {error}"))?;
    app.set_menu(menu)
        .map_err(|error| format!("failed to apply application menu: {error}"))?;
    if let Some(tray) = app.tray_by_id(TRAY_ID) {
        let labels = menu_labels(settings.general.language);
        let show = MenuItem::with_id(app, MENU_SHOW, labels.show, true, None::<&str>)
            .map_err(|error| format!("failed to rebuild tray menu: {error}"))?;
        let search = MenuItem::with_id(app, MENU_SEARCH, labels.search, true, None::<&str>)
            .map_err(|error| format!("failed to rebuild tray menu: {error}"))?;
        let settings_item =
            MenuItem::with_id(app, MENU_SETTINGS, labels.settings, true, None::<&str>)
                .map_err(|error| format!("failed to rebuild tray menu: {error}"))?;
        let hide = MenuItem::with_id(app, MENU_HIDE, labels.hide, true, None::<&str>)
            .map_err(|error| format!("failed to rebuild tray menu: {error}"))?;
        let quit = MenuItem::with_id(app, MENU_QUIT, labels.quit, true, None::<&str>)
            .map_err(|error| format!("failed to rebuild tray menu: {error}"))?;
        let menu = Menu::with_items(app, &[&show, &search, &settings_item, &hide, &quit])
            .map_err(|error| format!("failed to rebuild tray menu: {error}"))?;
        tray.set_menu(Some(menu))
            .map_err(|error| format!("failed to apply tray menu: {error}"))?;
        tray.set_visible(settings.general.tray_enabled)
            .map_err(|error| format!("failed to apply system tray visibility: {error}"))?;
    }
    Ok(())
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

fn request_close_confirmation<R: Runtime>(app: &AppHandle<R>, can_minimize_to_tray: bool) {
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
            can_minimize_to_tray,
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

    #[test]
    fn tray_icon_preserves_high_dpi_source() {
        let icon = load_tray_icon().expect("tray icon should decode");
        assert_eq!(icon.width(), 512);
        assert_eq!(icon.height(), 512);
    }
}
