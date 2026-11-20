use std::{
    collections::BTreeSet,
    fs,
    path::Path,
    sync::{
        Mutex,
        atomic::{AtomicBool, Ordering},
    },
};

use serde::{Deserialize, Serialize};
use tauri::{
    App, AppHandle, Emitter, Manager, Runtime, Window, WindowEvent,
    menu::{CheckMenuItem, Menu, MenuEvent, MenuItem, PredefinedMenuItem, SubmenuBuilder},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
};
use tauri_plugin_autostart::ManagerExt;
use tauri_plugin_dialog::{DialogExt, MessageDialogKind};

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
const RUN_COMMAND_ACTION_EVENT: &str = "mootool://run-command-action";
const OPEN_TOOL_EVENT: &str = "mootool://open-tool";
const START_SCREEN_ACTION_EVENT: &str = "mootool://start-screen-action";
const MENU_SHOW: &str = "mootool-show";
const MENU_SEARCH: &str = "mootool-search";
const MENU_SETTINGS: &str = "mootool-settings";
const MENU_HIDE: &str = "mootool-hide-to-tray";
const MENU_QUIT: &str = "mootool-quit";
const MENU_COLOR: &str = "mootool-tray-color";
const MENU_SCREENSHOT: &str = "mootool-tray-screenshot";
const MENU_TRANSLATION: &str = "mootool-tray-translation";
const TRAY_COMMAND_PREFIX: &str = "mootool-command:";
const TRAY_HOST_PREFIX: &str = "mootool-host:";
const ACTIVE_HOST_FILE_NAME: &str = "active-host-id";
const TRAY_NOTICE_EVENT: &str = "mootool://tray-notice";
const TRAY_ACTION_IDS: &[&str] = &[
    "uuid",
    "timestamp",
    "json-format",
    "json-minify",
    "base64-encode",
    "base64-decode",
    "url-encode",
    "url-decode",
    "deduplicate-lines",
    "clipboard-text",
    "clipboard-html",
    "html-markdown",
    "table-json",
];
const TRAY_ID: &str = "mootool-next-tauri-tray";
const AUTOSTART_ARGUMENT: &str = "--mootool-autostart";
const TRAY_ICON_BYTES: &[u8] = include_bytes!("../../icons/tray-icon.png");

#[derive(Default)]
pub struct TrayMenuState {
    actions: Mutex<Vec<TrayQuickAction>>,
}

#[derive(Clone)]
struct TrayQuickAction {
    id: String,
    label: String,
}

#[derive(Deserialize)]
pub struct TrayQuickActionInput {
    id: String,
    label: String,
}

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
    tray_open: &'static str,
    tray_palette: &'static str,
    tray_actions: &'static str,
    tray_color: &'static str,
    tray_screenshot: &'static str,
    tray_translation: &'static str,
    tray_quit: &'static str,
    tray_host_switched: &'static str,
    tray_host_failed: &'static str,
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
            tray_open: "打开 MooTool",
            tray_palette: "命令面板…",
            tray_actions: "快捷动作",
            tray_color: "取色器",
            tray_screenshot: "截图",
            tray_translation: "翻译",
            tray_quit: "退出 MooTool",
            tray_host_switched: "Host 已切换：{name}",
            tray_host_failed: "Host 切换失败",
        },
        AppLanguage::English => MenuLabels {
            show: "Show MooTool",
            search: "Search Tools…",
            settings: "Settings…",
            hide: "Hide to Tray",
            quit: "Quit MooTool Next Tauri",
            edit: "Edit",
            window: "Window",
            tray_open: "Open MooTool",
            tray_palette: "Command Palette…",
            tray_actions: "Quick Actions",
            tray_color: "Color Picker",
            tray_screenshot: "Capture",
            tray_translation: "Translation",
            tray_quit: "Quit MooTool",
            tray_host_switched: "Host switched: {name}",
            tray_host_failed: "Host switch failed",
        },
        AppLanguage::Japanese => MenuLabels {
            show: "MooTool を表示",
            search: "ツールを検索…",
            settings: "設定…",
            hide: "トレイへ隠す",
            quit: "MooTool Next Tauri を終了",
            edit: "編集",
            window: "ウィンドウ",
            tray_open: "MooTool を開く",
            tray_palette: "コマンドパレット…",
            tray_actions: "クイックアクション",
            tray_color: "スポイト",
            tray_screenshot: "スクリーンショット",
            tray_translation: "翻訳",
            tray_quit: "MooTool を終了",
            tray_host_switched: "Host を切り替えました：{name}",
            tray_host_failed: "Host の切り替えに失敗しました",
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

fn sanitize_menu_label(value: &str) -> String {
    let label: String = value
        .chars()
        .filter(|character| !character.is_control())
        .take(80)
        .collect();
    label.trim().to_string()
}

fn default_tray_actions(language: AppLanguage) -> Vec<TrayQuickAction> {
    let labels: [(&str, &str); 3] = match language {
        AppLanguage::SimplifiedChinese => [
            ("uuid", "生成 UUID"),
            ("json-format", "格式化 JSON"),
            ("base64-decode", "Base64 解码"),
        ],
        AppLanguage::English => [
            ("uuid", "Generate UUID"),
            ("json-format", "Format JSON"),
            ("base64-decode", "Base64 decode"),
        ],
        AppLanguage::Japanese => [
            ("uuid", "UUID を生成"),
            ("json-format", "JSON を整形"),
            ("base64-decode", "Base64 を復号"),
        ],
    };
    labels
        .into_iter()
        .map(|(id, label)| TrayQuickAction {
            id: id.to_string(),
            label: label.to_string(),
        })
        .collect()
}

fn normalize_tray_actions(
    actions: Vec<TrayQuickActionInput>,
    language: AppLanguage,
) -> Vec<TrayQuickAction> {
    let mut result = Vec::new();
    for action in actions {
        let Some(id) = TRAY_ACTION_IDS
            .iter()
            .copied()
            .find(|item| *item == action.id)
        else {
            continue;
        };
        if result.iter().any(|item: &TrayQuickAction| item.id == id) {
            continue;
        }
        let label = sanitize_menu_label(&action.label);
        if label.is_empty() {
            continue;
        }
        result.push(TrayQuickAction {
            id: id.to_string(),
            label,
        });
        if result.len() == TRAY_ACTION_IDS.len() {
            break;
        }
    }
    if result.is_empty() {
        default_tray_actions(language)
    } else {
        result
    }
}

fn current_tray_actions(
    app: &AppHandle,
    language: AppLanguage,
) -> Result<Vec<TrayQuickAction>, String> {
    let Some(state) = app.try_state::<TrayMenuState>() else {
        return Ok(default_tray_actions(language));
    };
    let actions = state
        .actions
        .lock()
        .map_err(|_| "tray menu state is unavailable".to_string())?;
    if actions.is_empty() {
        Ok(default_tray_actions(language))
    } else {
        Ok(actions.clone())
    }
}

struct TrayHostItem {
    id: i64,
    label: String,
    checked: bool,
}

pub fn matching_host_id(stored: Option<i64>, ids: &[i64]) -> Option<i64> {
    stored.filter(|id| *id > 0 && ids.contains(id))
}

fn active_host_path(app: &AppHandle) -> Result<std::path::PathBuf, String> {
    Ok(app
        .path()
        .app_config_dir()
        .map_err(|error| format!("failed to resolve active host directory: {error}"))?
        .join(ACTIVE_HOST_FILE_NAME))
}

fn read_active_host_id(app: &AppHandle) -> Option<i64> {
    let path = active_host_path(app).ok()?;
    let text = fs::read_to_string(path).ok()?;
    text.trim().parse::<i64>().ok().filter(|id| *id > 0)
}

pub fn remember_active_host(app: &AppHandle, id: Option<i64>) -> Result<(), String> {
    let path = active_host_path(app)?;
    match id.filter(|value| *value > 0) {
        Some(id) => {
            if let Some(parent) = path.parent() {
                fs::create_dir_all(parent)
                    .map_err(|error| format!("failed to create active host directory: {error}"))?;
            }
            fs::write(path, id.to_string())
                .map_err(|error| format!("failed to remember active host: {error}"))
        }
        None => {
            if path.exists() {
                fs::remove_file(path)
                    .map_err(|error| format!("failed to clear active host: {error}"))?;
            }
            Ok(())
        }
    }
}

fn tray_hosts(app: &AppHandle) -> Vec<TrayHostItem> {
    let Some(repository) = app.try_state::<crate::repositories::local_data::LocalDataRepository>()
    else {
        return Vec::new();
    };
    let Ok(profiles) = repository.list_host_profiles("", false) else {
        return Vec::new();
    };
    let ids = profiles
        .iter()
        .map(|profile| profile.id)
        .collect::<Vec<_>>();
    let active = matching_host_id(read_active_host_id(app), &ids);
    if read_active_host_id(app) != active {
        let _ = remember_active_host(app, active);
    }
    profiles
        .into_iter()
        .filter_map(|profile| {
            let label = sanitize_menu_label(&profile.name);
            if label.is_empty() {
                None
            } else {
                Some(TrayHostItem {
                    id: profile.id,
                    label,
                    checked: active == Some(profile.id),
                })
            }
        })
        .collect()
}

fn show_host_switch_error(app: &AppHandle, message: &str) {
    let language = app
        .try_state::<SettingsRepository>()
        .map(|repository| repository.snapshot().general.language)
        .unwrap_or_default();
    app.dialog()
        .message(message)
        .title(menu_labels(language).tray_host_failed)
        .kind(MessageDialogKind::Error)
        .blocking_show();
}

fn switch_host_from_tray(app: &AppHandle, host_id: i64) -> Result<(), String> {
    let repository = app
        .try_state::<crate::repositories::local_data::LocalDataRepository>()
        .ok_or_else(|| "host profiles are unavailable".to_string())?;
    let profiles = repository.list_host_profiles("", true)?;
    let Some(profile) = profiles.into_iter().find(|item| item.id == host_id) else {
        remember_active_host(app, None)?;
        let settings = app.state::<SettingsRepository>().snapshot();
        return apply_tray_menu(app, &settings);
    };
    let current =
        crate::commands::local_data::read_system_hosts().map_err(|error| error.message)?;
    let name = profile.name.clone();
    crate::commands::local_data::write_system_hosts(
        app.clone(),
        profile.content,
        current.content,
        Some(profile.id),
    )
    .map_err(|error| error.message)?;
    let language = app
        .state::<SettingsRepository>()
        .snapshot()
        .general
        .language;
    let body = menu_labels(language)
        .tray_host_switched
        .replace("{name}", &name);
    app.emit_to("main", TRAY_NOTICE_EVENT, body)
        .map_err(|error| format!("failed to report host switch: {error}"))?;
    Ok(())
}

pub fn refresh_tray(app: &AppHandle) -> Result<(), String> {
    let Some(repository) = app.try_state::<SettingsRepository>() else {
        return Ok(());
    };
    apply_tray_menu(app, &repository.snapshot())
}

fn build_tray_menu(
    app: &AppHandle,
    language: AppLanguage,
    actions: &[TrayQuickAction],
) -> Result<Menu<tauri::Wry>, String> {
    let labels = menu_labels(language);
    let open = MenuItem::with_id(app, MENU_SHOW, labels.tray_open, true, None::<&str>)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let palette = MenuItem::with_id(app, MENU_SEARCH, labels.tray_palette, true, None::<&str>)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let mut command_items = Vec::with_capacity(actions.len());
    for action in actions {
        command_items.push(
            MenuItem::with_id(
                app,
                format!("{TRAY_COMMAND_PREFIX}{}", action.id),
                &action.label,
                true,
                None::<&str>,
            )
            .map_err(|error| format!("failed to create tray menu: {error}"))?,
        );
    }
    let mut quick_actions = SubmenuBuilder::new(app, labels.tray_actions);
    for item in &command_items {
        quick_actions = quick_actions.item(item);
    }
    let quick_actions = quick_actions
        .build()
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let settings = MenuItem::with_id(app, MENU_SETTINGS, labels.settings, true, None::<&str>)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let separator = PredefinedMenuItem::separator(app)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let color = MenuItem::with_id(app, MENU_COLOR, labels.tray_color, true, None::<&str>)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let screenshot = MenuItem::with_id(
        app,
        MENU_SCREENSHOT,
        labels.tray_screenshot,
        true,
        None::<&str>,
    )
    .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let translation = MenuItem::with_id(
        app,
        MENU_TRANSLATION,
        labels.tray_translation,
        true,
        None::<&str>,
    )
    .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let separator_after_tools = PredefinedMenuItem::separator(app)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let hosts = tray_hosts(app);
    let mut host_items = Vec::with_capacity(hosts.len());
    for host in &hosts {
        host_items.push(
            CheckMenuItem::with_id(
                app,
                format!("{TRAY_HOST_PREFIX}{}", host.id),
                &host.label,
                true,
                host.checked,
                None::<&str>,
            )
            .map_err(|error| format!("failed to create tray menu: {error}"))?,
        );
    }
    let separator_after_hosts = PredefinedMenuItem::separator(app)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let quit = MenuItem::with_id(app, MENU_QUIT, labels.tray_quit, true, None::<&str>)
        .map_err(|error| format!("failed to create tray menu: {error}"))?;
    let mut entries: Vec<&dyn tauri::menu::IsMenuItem<tauri::Wry>> = vec![
        &open,
        &palette,
        &quick_actions,
        &settings,
        &separator,
        &color,
        &screenshot,
        &translation,
        &separator_after_tools,
    ];
    for item in &host_items {
        entries.push(item);
    }
    if !host_items.is_empty() {
        entries.push(&separator_after_hosts);
    }
    entries.push(&quit);
    Menu::with_items(app, &entries).map_err(|error| format!("failed to create tray menu: {error}"))
}

fn apply_tray_menu(app: &AppHandle, settings: &AppSettings) -> Result<(), String> {
    let Some(tray) = app.tray_by_id(TRAY_ID) else {
        return Ok(());
    };
    let actions = current_tray_actions(app, settings.general.language)?;
    let menu = build_tray_menu(app, settings.general.language, &actions)?;
    tray.set_menu(Some(menu))
        .map_err(|error| format!("failed to apply tray menu: {error}"))?;
    tray.set_tooltip(Some("MooTool"))
        .map_err(|error| format!("failed to apply tray tooltip: {error}"))?;
    tray.set_visible(settings.general.tray_enabled)
        .map_err(|error| format!("failed to apply system tray visibility: {error}"))
}

#[tauri::command]
pub fn sync_tray_quick_actions(
    app: AppHandle,
    state: tauri::State<'_, TrayMenuState>,
    actions: Vec<TrayQuickActionInput>,
) -> AppResult<()> {
    let settings = app.state::<SettingsRepository>().snapshot();
    let normalized = normalize_tray_actions(actions, settings.general.language);
    *state
        .actions
        .lock()
        .map_err(|_| "tray menu state is unavailable")? = normalized;
    apply_tray_menu(&app, &settings)?;
    Ok(())
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

    let tray_actions = current_tray_actions(app.handle(), settings.general.language)?;
    let tray_menu = build_tray_menu(app.handle(), settings.general.language, &tray_actions)?;
    let tray = TrayIconBuilder::with_id(TRAY_ID)
        .menu(&tray_menu)
        .icon(load_tray_icon()?)
        .icon_as_template(cfg!(target_os = "macos"))
        .tooltip("MooTool")
        .show_menu_on_left_click(false)
        .build(app)
        .map_err(|error| format!("failed to create system tray icon: {error}"))?;
    tray.set_visible(settings.general.tray_enabled)
        .map_err(|error| format!("failed to apply system tray visibility: {error}"))?;

    sync_desktop_preferences(app.handle(), &settings)?;
    super::global_shortcut::sync_global_shortcut(app.handle(), &settings);
    if synchronize_autostart {
        sync_autostart(app.handle(), &settings)?;
    }
    if std::env::args().any(|argument| argument == AUTOSTART_ARGUMENT) {
        hide_product_windows(app.handle())?;
    }
    Ok(())
}

pub fn tray_screen_action(menu_id: &str) -> Option<&'static str> {
    match menu_id {
        MENU_COLOR => Some("color"),
        MENU_SCREENSHOT => Some("capture"),
        _ => None,
    }
}

fn emit_open_tool(app: &AppHandle, tool_id: &str) -> Result<(), String> {
    app.emit_to("main", OPEN_TOOL_EVENT, tool_id.to_string())
        .map_err(|error| format!("failed to open {tool_id}: {error}"))
}

pub fn handle_menu_event(app: &AppHandle, event: MenuEvent) {
    let id = event.id().as_ref().to_string();
    let result = if let Some(action_id) = id.strip_prefix(TRAY_COMMAND_PREFIX) {
        let action_id = action_id.to_string();
        show_main_window(app).and_then(|_| {
            if TRAY_ACTION_IDS.contains(&action_id.as_str()) {
                app.emit_to("main", RUN_COMMAND_ACTION_EVENT, action_id)
                    .map_err(|error| format!("failed to run tray command: {error}"))
            } else {
                Ok(())
            }
        })
    } else {
        match id.as_str() {
            MENU_SHOW => show_main_window(app),
            MENU_SEARCH => show_main_window(app).and_then(|_| {
                app.emit_to("main", "mootool://open-command-palette", ())
                    .map_err(|error| format!("failed to open command palette: {error}"))
            }),
            MENU_SETTINGS => super::settings::open_settings_window(app.clone())
                .map_err(|error| error.to_string()),
            MENU_COLOR | MENU_SCREENSHOT => show_main_window(app).and_then(|_| {
                let Some(action) = tray_screen_action(&id) else {
                    return Ok(());
                };
                app.emit_to("main", START_SCREEN_ACTION_EVENT, action)
                    .map_err(|error| format!("failed to start screen action: {error}"))
            }),
            MENU_TRANSLATION => {
                show_main_window(app).and_then(|_| emit_open_tool(app, "translation"))
            }
            MENU_HIDE => hide_product_windows(app),
            _ if id.starts_with(TRAY_HOST_PREFIX) => {
                match id[TRAY_HOST_PREFIX.len()..].parse::<i64>() {
                    Ok(host_id) if host_id > 0 => match switch_host_from_tray(app, host_id) {
                        Ok(()) => Ok(()),
                        Err(error) => {
                            show_host_switch_error(app, &error);
                            Err(error)
                        }
                    },
                    _ => Ok(()),
                }
            }
            MENU_QUIT => {
                app.exit(0);
                Ok(())
            }
            _ => Ok(()),
        }
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
    if let Some(repository) = app.try_state::<WindowStateRepository>()
        && let Err(error) = repository
            .remember_window(window)
            .and_then(|_| repository.flush())
    {
        eprintln!("MooTool Next Tauri final window state save failed: {error}");
    }

    let settings = app
        .try_state::<SettingsRepository>()
        .map(|repository| repository.snapshot())
        .unwrap_or_default();
    match settings.general.close_behavior {
        CloseBehavior::Quit => app.exit(0),
        CloseBehavior::Hide => {
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
        CloseDecision::Hide => Ok(hide_product_windows(&app)?),
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
    apply_tray_menu(app, settings)?;
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
    fn tray_actions_keep_pinned_order_and_fall_back() {
        let pinned = normalize_tray_actions(
            vec![
                TrayQuickActionInput {
                    id: "base64-decode".into(),
                    label: "解码".into(),
                },
                TrayQuickActionInput {
                    id: "nope".into(),
                    label: "忽略".into(),
                },
                TrayQuickActionInput {
                    id: "base64-decode".into(),
                    label: "重复".into(),
                },
                TrayQuickActionInput {
                    id: "timestamp".into(),
                    label: " 时间戳 ".into(),
                },
            ],
            AppLanguage::SimplifiedChinese,
        );
        assert_eq!(
            pinned
                .iter()
                .map(|item| item.id.as_str())
                .collect::<Vec<_>>(),
            ["base64-decode", "timestamp"]
        );
        assert_eq!(pinned[1].label, "时间戳");
        assert_eq!(tray_screen_action(MENU_COLOR), Some("color"));
        assert_eq!(tray_screen_action(MENU_SCREENSHOT), Some("capture"));
        assert_eq!(tray_screen_action(MENU_TRANSLATION), None);
        assert_eq!(matching_host_id(Some(4), &[9, 4, 2]), Some(4));
        assert_eq!(matching_host_id(Some(7), &[9, 4]), None);
        assert_eq!(matching_host_id(Some(0), &[0]), None);
        assert_eq!(
            menu_labels(AppLanguage::SimplifiedChinese)
                .tray_host_switched
                .replace("{name}", "办公室"),
            "Host 已切换：办公室"
        );
        assert_eq!(
            menu_labels(AppLanguage::English).tray_host_failed,
            "Host switch failed"
        );
        assert_eq!(
            menu_labels(AppLanguage::Japanese).tray_host_switched,
            "Host を切り替えました：{name}"
        );
        let fallback = normalize_tray_actions(Vec::new(), AppLanguage::English);
        assert_eq!(
            fallback
                .iter()
                .map(|item| item.id.as_str())
                .collect::<Vec<_>>(),
            ["uuid", "json-format", "base64-decode"]
        );
    }

    #[test]
    fn tray_icon_preserves_high_dpi_source() {
        let icon = load_tray_icon().expect("tray icon should decode");
        assert_eq!(icon.width(), 512);
        assert_eq!(icon.height(), 512);
    }
}
