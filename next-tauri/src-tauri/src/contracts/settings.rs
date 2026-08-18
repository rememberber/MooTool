use std::collections::HashSet;

use serde::{Deserialize, Serialize};

pub const SETTINGS_SCHEMA_VERSION: u32 = 4;

const PRODUCT_TOOL_IDS: [&str; 25] = [
    "quick-note",
    "text-diff",
    "reformat",
    "json",
    "config",
    "runtime",
    "protobuf",
    "variables",
    "http",
    "host",
    "network",
    "ua",
    "encode",
    "crypto",
    "regex",
    "cron",
    "qrcode",
    "timestamp",
    "message-board",
    "translation",
    "calculator",
    "color",
    "image",
    "pdf",
    "system",
];

const fn default_true() -> bool {
    true
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
pub enum AppLanguage {
    #[default]
    #[serde(rename = "zh-CN")]
    SimplifiedChinese,
    #[serde(rename = "en-US")]
    English,
    #[serde(rename = "ja-JP")]
    Japanese,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum CloseBehavior {
    #[default]
    Ask,
    MinimizeToTray,
    Quit,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum ThemePreference {
    #[default]
    System,
    Light,
    Dark,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum AccentColor {
    #[default]
    Blue,
    Indigo,
    Teal,
    Orange,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum InterfaceDensity {
    Compact,
    #[default]
    Comfortable,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum ProxyMode {
    #[default]
    System,
    Direct,
    Manual,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct GeneralSettings {
    pub language: AppLanguage,
    pub launch_at_login: bool,
    pub close_behavior: CloseBehavior,
    #[serde(default = "default_true")]
    pub auto_check_updates: bool,
}

impl Default for GeneralSettings {
    fn default() -> Self {
        Self {
            language: AppLanguage::default(),
            launch_at_login: false,
            close_behavior: CloseBehavior::default(),
            auto_check_updates: true,
        }
    }
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct AppearanceSettings {
    pub theme: ThemePreference,
    pub accent_color: AccentColor,
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct CustomToolGroup {
    pub id: String,
    pub name: String,
    pub tool_ids: Vec<String>,
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct LayoutSettings {
    pub sidebar_compact: bool,
    pub density: InterfaceDensity,
    pub custom_groups: Vec<CustomToolGroup>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EditorSettings {
    pub font_size: u8,
    pub tab_size: u8,
    pub word_wrap: bool,
}

impl Default for EditorSettings {
    fn default() -> Self {
        Self {
            font_size: 13,
            tab_size: 2,
            word_wrap: true,
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct NetworkSettings {
    pub timeout_seconds: u16,
    pub proxy_mode: ProxyMode,
}

impl Default for NetworkSettings {
    fn default() -> Self {
        Self {
            timeout_seconds: 30,
            proxy_mode: ProxyMode::default(),
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RuntimeSettings {
    pub auto_detect: bool,
}

impl Default for RuntimeSettings {
    fn default() -> Self {
        Self { auto_detect: true }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct DataSettings {
    pub history_limit: u16,
}

impl Default for DataSettings {
    fn default() -> Self {
        Self { history_limit: 500 }
    }
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct VaultSettings {
    pub auto_commit: bool,
    pub root_directory: Option<String>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ShortcutSettings {
    pub global_search: String,
}

impl Default for ShortcutSettings {
    fn default() -> Self {
        Self {
            global_search: "CommandOrControl+K".into(),
        }
    }
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ToolSettings {
    pub favorites: Vec<String>,
    pub recent: Vec<String>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct AppSettings {
    pub schema_version: u32,
    pub revision: u64,
    pub general: GeneralSettings,
    pub appearance: AppearanceSettings,
    pub layout: LayoutSettings,
    pub editor: EditorSettings,
    pub network: NetworkSettings,
    pub runtime: RuntimeSettings,
    pub data: DataSettings,
    pub vault: VaultSettings,
    pub shortcuts: ShortcutSettings,
    pub tools: ToolSettings,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            schema_version: SETTINGS_SCHEMA_VERSION,
            revision: 0,
            general: GeneralSettings::default(),
            appearance: AppearanceSettings::default(),
            layout: LayoutSettings::default(),
            editor: EditorSettings::default(),
            network: NetworkSettings::default(),
            runtime: RuntimeSettings::default(),
            data: DataSettings::default(),
            vault: VaultSettings::default(),
            shortcuts: ShortcutSettings::default(),
            tools: ToolSettings::default(),
        }
    }
}

impl AppSettings {
    pub fn validate(&self) -> Result<(), String> {
        if self.schema_version != SETTINGS_SCHEMA_VERSION {
            return Err(format!(
                "unsupported settings schema {}, expected {}",
                self.schema_version, SETTINGS_SCHEMA_VERSION
            ));
        }
        if !(10..=24).contains(&self.editor.font_size) {
            return Err("editor font size must be between 10 and 24".into());
        }
        if ![2, 4, 8].contains(&self.editor.tab_size) {
            return Err("editor tab size must be 2, 4, or 8".into());
        }
        if !(1..=300).contains(&self.network.timeout_seconds) {
            return Err("network timeout must be between 1 and 300 seconds".into());
        }
        if !(10..=5000).contains(&self.data.history_limit) {
            return Err("history limit must be between 10 and 5000".into());
        }
        validate_custom_groups(&self.layout.custom_groups)?;
        if self.vault.root_directory.as_ref().is_some_and(|path| {
            path.trim().is_empty()
                || path.len() > 4_096
                || !std::path::Path::new(path).is_absolute()
        }) {
            return Err("vault root directory must be an absolute path".into());
        }
        if self.shortcuts.global_search.trim().is_empty() || self.shortcuts.global_search.len() > 80
        {
            return Err("global search shortcut is invalid".into());
        }
        validate_tool_ids("favorites", &self.tools.favorites, 50)?;
        validate_tool_ids("recent", &self.tools.recent, 20)?;
        Ok(())
    }
}

fn validate_tool_ids(label: &str, ids: &[String], limit: usize) -> Result<(), String> {
    if ids.len() > limit {
        return Err(format!("{label} contains too many tool ids"));
    }
    if ids.iter().any(|id| {
        id.is_empty()
            || id.len() > 64
            || !id
                .bytes()
                .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
    }) {
        return Err(format!("{label} contains an invalid tool id"));
    }
    Ok(())
}

fn validate_custom_groups(groups: &[CustomToolGroup]) -> Result<(), String> {
    if groups.len() > 12 {
        return Err("custom groups cannot contain more than 12 groups".into());
    }
    let mut group_ids = HashSet::new();
    for group in groups {
        if group.id.is_empty()
            || group.id.len() > 64
            || !group
                .id
                .bytes()
                .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
            || !group_ids.insert(group.id.as_str())
        {
            return Err("custom group id is invalid or duplicated".into());
        }
        let name = group.name.trim();
        if name.is_empty() || name.chars().count() > 40 || name.chars().any(char::is_control) {
            return Err("custom group name must contain 1 to 40 safe characters".into());
        }
        if group.tool_ids.is_empty() || group.tool_ids.len() > PRODUCT_TOOL_IDS.len() {
            return Err("custom group must contain between 1 and 25 tools".into());
        }
        let mut tool_ids = HashSet::new();
        if group.tool_ids.iter().any(|tool_id| {
            !PRODUCT_TOOL_IDS.contains(&tool_id.as_str()) || !tool_ids.insert(tool_id.as_str())
        }) {
            return Err("custom group contains an unknown or duplicated tool".into());
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn defaults_cover_the_versioned_product_schema() {
        let settings = AppSettings::default();

        assert_eq!(settings.schema_version, SETTINGS_SCHEMA_VERSION);
        assert_eq!(settings.general.language, AppLanguage::SimplifiedChinese);
        assert!(settings.general.auto_check_updates);
        assert_eq!(settings.appearance.theme, ThemePreference::System);
        assert!(settings.layout.custom_groups.is_empty());
        assert_eq!(settings.editor.tab_size, 2);
        assert_eq!(settings.data.history_limit, 500);
        assert!(settings.validate().is_ok());
    }

    #[test]
    fn rejects_values_outside_owned_ranges() {
        let mut settings = AppSettings::default();
        settings.editor.font_size = 40;
        assert_eq!(
            settings.validate(),
            Err("editor font size must be between 10 and 24".into())
        );

        settings.editor.font_size = 13;
        settings.tools.recent = vec!["../electron".into()];
        assert_eq!(
            settings.validate(),
            Err("recent contains an invalid tool id".into())
        );

        settings.tools.recent.clear();
        settings.layout.custom_groups = vec![CustomToolGroup {
            id: "daily".into(),
            name: "Daily".into(),
            tool_ids: vec!["calculator".into(), "unknown-tool".into()],
        }];
        assert_eq!(
            settings.validate(),
            Err("custom group contains an unknown or duplicated tool".into())
        );
    }
}
