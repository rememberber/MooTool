use std::sync::Mutex;

use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager, Wry};
use tauri_plugin_global_shortcut::{GlobalShortcut, GlobalShortcutExt};

use crate::contracts::settings::AppSettings;

pub const GLOBAL_SHORTCUT_STATUS_EVENT: &str = "mootool://global-shortcut-status";

static ACTIVE_ACCELERATOR: Mutex<Option<String>> = Mutex::new(None);
static STATUS: Mutex<GlobalShortcutStatus> = Mutex::new(GlobalShortcutStatus {
    state: GlobalShortcutState::Disabled,
    accelerator: String::new(),
});

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum GlobalShortcutState {
    Disabled,
    Registered,
    Invalid,
    Conflict,
    Unavailable,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize)]
pub struct GlobalShortcutStatus {
    pub state: GlobalShortcutState,
    pub accelerator: String,
}

const SPECIAL_KEYS: [&str; 15] = [
    "Space", "Return", "Tab", "Backspace", "Delete", "Insert", "Home", "End", "PageUp",
    "PageDown", "Up", "Down", "Left", "Right", "Escape",
];
const MODIFIER_ORDER: [&str; 5] = ["Command", "Control", "Alt", "Shift", "Super"];
const RESERVED_KEYS: [&str; 12] = [
    "K", ",", "C", "V", "X", "A", "Z", "F", "S", "Return", "Shift+F", "Shift+Z",
];

pub fn sync_global_shortcut(app: &AppHandle, settings: &AppSettings) {
    let status = apply_global_shortcut(app, settings);
    *STATUS.lock().unwrap_or_else(|error| error.into_inner()) = status.clone();
    if let Err(error) = app.emit(GLOBAL_SHORTCUT_STATUS_EVENT, &status) {
        eprintln!("MooTool Next Tauri could not publish global shortcut status: {error}");
    }
}

#[tauri::command]
pub fn get_global_shortcut_status() -> GlobalShortcutStatus {
    STATUS.lock().unwrap_or_else(|error| error.into_inner()).clone()
}

#[tauri::command]
pub fn retry_global_shortcut(
    app: AppHandle,
    repository: tauri::State<'_, crate::repositories::settings::SettingsRepository>,
) -> GlobalShortcutStatus {
    sync_global_shortcut(&app, &repository.snapshot());
    get_global_shortcut_status()
}

fn apply_global_shortcut(app: &AppHandle, settings: &AppSettings) -> GlobalShortcutStatus {
    let configured = settings.shortcuts.global_search.clone();
    let enabled = settings.shortcuts.global_search_enabled;
    let validation = validate_global_shortcut(&configured, host_platform());
    let has_plugin = app.try_state::<GlobalShortcut<Wry>>().is_some();
    let mut active = ACTIVE_ACCELERATOR
        .lock()
        .unwrap_or_else(|error| error.into_inner());
    let same_shortcut = enabled
        && validation.state == GlobalShortcutState::Registered
        && active.as_deref() == Some(validation.accelerator.as_str())
        && has_plugin
        && app
            .global_shortcut()
            .is_registered(validation.accelerator.as_str());
    if same_shortcut {
        return validation;
    }
    if let Some(previous) = active.take()
        && has_plugin
        && let Err(error) = app.global_shortcut().unregister(previous.as_str())
    {
        eprintln!("MooTool Next Tauri could not unregister global shortcut: {error}");
    }
    if !enabled {
        return GlobalShortcutStatus {
            state: GlobalShortcutState::Disabled,
            accelerator: configured,
        };
    }
    if validation.state != GlobalShortcutState::Registered || !has_plugin {
        return if validation.state == GlobalShortcutState::Registered {
            GlobalShortcutStatus {
                state: GlobalShortcutState::Unavailable,
                accelerator: validation.accelerator,
            }
        } else {
            validation
        };
    }
    match app.global_shortcut().register(validation.accelerator.as_str()) {
        Ok(()) => {
            *active = Some(validation.accelerator.clone());
            validation
        }
        Err(error) => {
            eprintln!("MooTool Next Tauri could not register global shortcut: {error}");
            GlobalShortcutStatus {
                state: GlobalShortcutState::Unavailable,
                accelerator: validation.accelerator,
            }
        }
    }
}

pub fn validate_global_shortcut(value: &str, platform: &str) -> GlobalShortcutStatus {
    let Some(accelerator) = normalize_shortcut(value, platform) else {
        return GlobalShortcutStatus {
            state: GlobalShortcutState::Invalid,
            accelerator: value.to_string(),
        };
    };
    let reserved = RESERVED_KEYS.iter().any(|keys| {
        normalize_shortcut(&format!("CommandOrControl+{keys}"), platform).as_deref()
            == Some(accelerator.as_str())
    });
    let in_app_search = accelerator.ends_with("+K")
        && accelerator
            .split('+')
            .any(|part| matches!(part, "Control" | "Command" | "Super"));
    GlobalShortcutStatus {
        state: if reserved || in_app_search {
            GlobalShortcutState::Conflict
        } else {
            GlobalShortcutState::Registered
        },
        accelerator,
    }
}

fn normalize_shortcut(value: &str, platform: &str) -> Option<String> {
    if value.len() > 160 {
        return None;
    }
    let mut parts = value
        .split('+')
        .map(str::trim)
        .filter(|part| !part.is_empty())
        .collect::<Vec<_>>();
    if parts.len() < 2 || parts.len() != value.split('+').count() {
        return None;
    }
    let raw_key = parts.pop()?;
    let key = canonical_key(raw_key)?;
    let mut modifiers = Vec::with_capacity(parts.len());
    for part in parts {
        let modifier = canonical_modifier(part)?;
        let modifier = match modifier {
            "CommandOrControl" => {
                if platform == "darwin" {
                    "Command"
                } else {
                    "Control"
                }
            }
            "Super" if platform == "darwin" => "Command",
            other => other,
        };
        modifiers.push(modifier);
    }
    if modifiers.len() != modifiers.iter().collect::<std::collections::BTreeSet<_>>().len()
        || !modifiers.iter().any(|modifier| *modifier != "Shift")
        || (platform != "darwin" && modifiers.contains(&"Command"))
    {
        return None;
    }
    modifiers.sort_by_key(|modifier| {
        MODIFIER_ORDER
            .iter()
            .position(|candidate| candidate == modifier)
            .unwrap_or(usize::MAX)
    });
    modifiers.push(key.as_str());
    Some(modifiers.join("+"))
}

fn canonical_modifier(part: &str) -> Option<&'static str> {
    match part.to_ascii_lowercase().as_str() {
        "commandorcontrol" | "cmdorctrl" => Some("CommandOrControl"),
        "command" | "cmd" => Some("Command"),
        "control" | "ctrl" => Some("Control"),
        "alt" | "option" => Some("Alt"),
        "shift" => Some("Shift"),
        "super" | "win" | "meta" => Some("Super"),
        _ => None,
    }
}

fn canonical_key(raw: &str) -> Option<String> {
    let mut characters = raw.chars();
    let Some(character) = characters.next() else {
        return None;
    };
    if characters.next().is_none() && character.is_ascii_alphanumeric() {
        return Some(character.to_ascii_uppercase().to_string());
    }
    if let Some(number) = raw
        .strip_prefix('F')
        .or_else(|| raw.strip_prefix('f'))
        .filter(|number| {
            !number.is_empty()
                && !number.starts_with('0')
                && number.chars().all(|character| character.is_ascii_digit())
        })
    {
        let value = number.parse::<u8>().ok()?;
        if (1..=24).contains(&value) {
            return Some(format!("F{value}"));
        }
    }
    if let Some(key) = SPECIAL_KEYS
        .iter()
        .find(|key| key.eq_ignore_ascii_case(raw))
    {
        return Some((*key).to_string());
    }
    if characters_consumed(raw)
        && matches!(
            raw,
            "," | "." | "/" | ";" | "[" | "]" | "\\" | "'" | "`" | "=" | "-"
        )
    {
        return Some(raw.to_string());
    }
    None
}

fn characters_consumed(value: &str) -> bool {
    value.chars().nth(1).is_none()
}

fn host_platform() -> &'static str {
    if cfg!(target_os = "macos") {
        "darwin"
    } else {
        "other"
    }
}

#[cfg(test)]
mod tests {
    use super::{GlobalShortcutState, validate_global_shortcut};

    #[test]
    fn normalizes_the_electron_default_global_shortcut() {
        let darwin = validate_global_shortcut("CommandOrControl+Shift+Space", "darwin");
        assert_eq!(darwin.state, GlobalShortcutState::Registered);
        assert_eq!(darwin.accelerator, "Command+Shift+Space");
        assert_eq!(
            validate_global_shortcut("cmdorctrl+shift+space", "win32").accelerator,
            "Control+Shift+Space"
        );
        assert_eq!(
            validate_global_shortcut("Shift+Alt+P", "linux").accelerator,
            "Alt+Shift+P"
        );
        assert_eq!(
            validate_global_shortcut("Super+P", "darwin").accelerator,
            "Command+P"
        );
    }

    #[test]
    fn rejects_reserved_invalid_and_conflicting_shortcuts() {
        assert_eq!(
            validate_global_shortcut("CommandOrControl+K", "darwin").state,
            GlobalShortcutState::Conflict
        );
        assert_eq!(
            validate_global_shortcut("CommandOrControl+,", "linux").state,
            GlobalShortcutState::Conflict
        );
        assert_eq!(
            validate_global_shortcut("CommandOrControl+Shift+Z", "darwin").state,
            GlobalShortcutState::Conflict
        );
        assert_eq!(
            validate_global_shortcut("Control+Alt+K", "linux").state,
            GlobalShortcutState::Conflict
        );
        assert_eq!(
            validate_global_shortcut("Shift+A", "darwin").state,
            GlobalShortcutState::Invalid
        );
        assert_eq!(
            validate_global_shortcut("Command+Shift+P", "win32").state,
            GlobalShortcutState::Invalid
        );
        assert_eq!(
            validate_global_shortcut("Control+Control+K", "linux").state,
            GlobalShortcutState::Invalid
        );
        assert_eq!(
            validate_global_shortcut("", "darwin").state,
            GlobalShortcutState::Invalid
        );
    }
}
