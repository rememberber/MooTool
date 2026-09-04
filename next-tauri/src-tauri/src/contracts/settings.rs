use std::collections::{BTreeMap, HashSet};

use serde::{Deserialize, Serialize};

pub const SETTINGS_SCHEMA_VERSION: u32 = 8;

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
    pub auto_download_updates: bool,
    pub start_maximized: bool,
    #[serde(default = "default_true")]
    pub tray_enabled: bool,
}

impl Default for GeneralSettings {
    fn default() -> Self {
        Self {
            language: AppLanguage::default(),
            launch_at_login: false,
            close_behavior: CloseBehavior::default(),
            auto_check_updates: true,
            auto_download_updates: false,
            start_maximized: false,
            tray_enabled: true,
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct AppearanceSettings {
    pub theme: ThemePreference,
    pub accent_color: AccentColor,
    pub font_family: String,
    pub ui_scale: u8,
}

impl Default for AppearanceSettings {
    fn default() -> Self {
        Self {
            theme: ThemePreference::default(),
            accent_color: AccentColor::default(),
            font_family: "system".into(),
            ui_scale: 100,
        }
    }
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct CustomToolGroup {
    pub id: String,
    pub name: String,
    pub tool_ids: Vec<String>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct LayoutSettings {
    pub sidebar_compact: bool,
    pub density: InterfaceDensity,
    pub custom_groups: Vec<CustomToolGroup>,
    pub pane_sizes: BTreeMap<String, u16>,
    #[serde(default = "default_true")]
    pub show_recent: bool,
    #[serde(default = "default_true")]
    pub show_group_titles: bool,
    pub hidden_tools: Vec<String>,
}

impl Default for LayoutSettings {
    fn default() -> Self {
        Self {
            sidebar_compact: false,
            density: InterfaceDensity::default(),
            custom_groups: Vec::new(),
            pane_sizes: BTreeMap::new(),
            show_recent: true,
            show_group_titles: true,
            hidden_tools: Vec::new(),
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EditorSettings {
    pub font_size: u8,
    pub tab_size: u8,
    pub word_wrap: bool,
    pub json_font_family: String,
    pub json_font_size: u8,
    pub quick_note_font_family: String,
    pub quick_note_font_size: u8,
}

impl Default for EditorSettings {
    fn default() -> Self {
        Self {
            font_size: 13,
            tab_size: 2,
            word_wrap: true,
            json_font_family: "mono".into(),
            json_font_size: 13,
            quick_note_font_family: "system".into(),
            quick_note_font_size: 13,
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct NetworkSettings {
    pub timeout_seconds: u16,
    pub proxy_mode: ProxyMode,
    pub proxy_host: String,
    pub proxy_port: u16,
    pub proxy_username: String,
    pub translation_timeout_seconds: u16,
}

impl Default for NetworkSettings {
    fn default() -> Self {
        Self {
            timeout_seconds: 30,
            proxy_mode: ProxyMode::default(),
            proxy_host: String::new(),
            proxy_port: 8080,
            proxy_username: String::new(),
            translation_timeout_seconds: 20,
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RuntimeSettings {
    pub auto_detect: bool,
    pub java_path: String,
    pub groovy_path: String,
    pub python_path: String,
    pub node_path: String,
    pub environment: BTreeMap<String, String>,
    pub drafts: BTreeMap<String, String>,
    pub options: BTreeMap<String, RuntimeRunOptionSettings>,
    pub timeout_seconds: u16,
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RuntimeRunOptionSettings {
    pub arguments_text: String,
    pub working_directory: String,
}

impl Default for RuntimeSettings {
    fn default() -> Self {
        Self {
            auto_detect: true,
            java_path: String::new(),
            groovy_path: String::new(),
            python_path: String::new(),
            node_path: String::new(),
            environment: BTreeMap::new(),
            drafts: runtime_keys()
                .map(|key| (key.into(), String::new()))
                .collect(),
            options: runtime_keys()
                .map(|key| (key.into(), RuntimeRunOptionSettings::default()))
                .collect(),
            timeout_seconds: 30,
        }
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
    pub settings: String,
}

impl Default for ShortcutSettings {
    fn default() -> Self {
        Self {
            global_search: "CommandOrControl+K".into(),
            settings: "CommandOrControl+,".into(),
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ToolSettings {
    pub favorites: Vec<String>,
    pub recent: Vec<String>,
    pub qr_code_size: u16,
    pub qr_error_correction: String,
    pub random_string_length: u16,
    pub export_directory: String,
    pub translation_provider: String,
    pub translation_source_lang: String,
    pub translation_target_lang: String,
}

impl Default for ToolSettings {
    fn default() -> Self {
        Self {
            favorites: Vec::new(),
            recent: Vec::new(),
            qr_code_size: 300,
            qr_error_correction: "M".into(),
            random_string_length: 16,
            export_directory: String::new(),
            translation_provider: "google".into(),
            translation_source_lang: "auto".into(),
            translation_target_lang: "zh-CN".into(),
        }
    }
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
        if !(10..=24).contains(&self.editor.json_font_size)
            || !(10..=24).contains(&self.editor.quick_note_font_size)
        {
            return Err("tool editor font sizes must be between 10 and 24".into());
        }
        if ![
            &self.editor.json_font_family,
            &self.editor.quick_note_font_family,
        ]
        .into_iter()
        .all(|family| family == "system" || family == "mono")
        {
            return Err("tool editor font family is invalid".into());
        }
        if ![2, 4, 8].contains(&self.editor.tab_size) {
            return Err("editor tab size must be 2, 4, or 8".into());
        }
        if !(1..=300).contains(&self.network.timeout_seconds) {
            return Err("network timeout must be between 1 and 300 seconds".into());
        }
        if !(1..=300).contains(&self.network.translation_timeout_seconds) {
            return Err("translation timeout must be between 1 and 300 seconds".into());
        }
        if self.network.proxy_port == 0
            || self.network.proxy_host.len() > 255
            || self.network.proxy_username.len() > 255
            || self.network.proxy_host.chars().any(char::is_control)
            || self.network.proxy_username.chars().any(char::is_control)
        {
            return Err("proxy settings are invalid".into());
        }
        if self.appearance.font_family != "system" && self.appearance.font_family != "mono" {
            return Err("interface font family is invalid".into());
        }
        if ![90, 100, 110].contains(&self.appearance.ui_scale) {
            return Err("interface scale must be 90, 100, or 110".into());
        }
        if !(10..=5000).contains(&self.data.history_limit) {
            return Err("history limit must be between 10 and 5000".into());
        }
        validate_custom_groups(&self.layout.custom_groups)?;
        validate_tool_ids(
            "hidden tools",
            &self.layout.hidden_tools,
            PRODUCT_TOOL_IDS.len(),
        )?;
        if self
            .layout
            .hidden_tools
            .iter()
            .any(|id| !PRODUCT_TOOL_IDS.contains(&id.as_str()))
        {
            return Err("hidden tools contains an unknown tool".into());
        }
        if self.layout.pane_sizes.len() > 64
            || self.layout.pane_sizes.iter().any(|(key, value)| {
                key.is_empty()
                    || key.len() > 64
                    || !key.bytes().all(|byte| {
                        byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-'
                    })
                    || !(120..=2000).contains(value)
            })
        {
            return Err("pane sizes contain an invalid entry".into());
        }
        if self.vault.root_directory.as_ref().is_some_and(|path| {
            path.trim().is_empty()
                || path.len() > 4_096
                || !std::path::Path::new(path).is_absolute()
        }) {
            return Err("vault root directory must be an absolute path".into());
        }
        validate_shortcut("global search", &self.shortcuts.global_search)?;
        validate_shortcut("settings", &self.shortcuts.settings)?;
        if self
            .shortcuts
            .global_search
            .eq_ignore_ascii_case(&self.shortcuts.settings)
        {
            return Err("global search and settings shortcuts must be different".into());
        }
        for path in [
            &self.runtime.java_path,
            &self.runtime.groovy_path,
            &self.runtime.python_path,
            &self.runtime.node_path,
        ] {
            if path.len() > 4_096
                || path.chars().any(char::is_control)
                || (!path.is_empty() && !std::path::Path::new(path).is_absolute())
            {
                return Err("runtime paths must be empty or absolute paths".into());
            }
        }
        if self.runtime.environment.len() > 200
            || self.runtime.environment.iter().any(|(name, value)| {
                name.is_empty()
                    || name.len() > 128
                    || !name.bytes().enumerate().all(|(index, byte)| {
                        byte == b'_'
                            || byte.is_ascii_alphabetic()
                            || (index > 0 && byte.is_ascii_digit())
                    })
                    || value.len() > 16_384
                    || value.contains('\0')
            })
        {
            return Err("runtime environment overrides are invalid".into());
        }
        validate_runtime_persistence(&self.runtime)?;
        if !(1..=300).contains(&self.runtime.timeout_seconds) {
            return Err("runtime timeout must be between 1 and 300 seconds".into());
        }
        validate_tool_ids("favorites", &self.tools.favorites, 50)?;
        validate_tool_ids("recent", &self.tools.recent, 20)?;
        validate_tool_defaults(&self.tools)?;
        Ok(())
    }
}

fn runtime_keys() -> impl Iterator<Item = &'static str> {
    ["java", "groovy", "python", "node"].into_iter()
}

fn validate_runtime_persistence(settings: &RuntimeSettings) -> Result<(), String> {
    let valid_keys = runtime_keys().collect::<HashSet<_>>();
    if settings.drafts.len() != valid_keys.len()
        || settings.options.len() != valid_keys.len()
        || settings
            .drafts
            .keys()
            .any(|key| !valid_keys.contains(key.as_str()))
        || settings
            .options
            .keys()
            .any(|key| !valid_keys.contains(key.as_str()))
        || settings
            .drafts
            .values()
            .any(|value| value.len() > 1_000_000)
        || settings.options.values().any(|option| {
            option.arguments_text.len() > 8_192
                || option.working_directory.len() > 4_096
                || option.arguments_text.contains('\0')
                || option.working_directory.chars().any(char::is_control)
        })
    {
        return Err("runtime drafts or options are invalid".into());
    }
    Ok(())
}

fn validate_tool_defaults(settings: &ToolSettings) -> Result<(), String> {
    if !(120..=2_000).contains(&settings.qr_code_size)
        || !matches!(settings.qr_error_correction.as_str(), "L" | "M" | "Q" | "H")
        || !(1..=4_096).contains(&settings.random_string_length)
        || !matches!(settings.translation_provider.as_str(), "google" | "bing")
        || settings.translation_source_lang.is_empty()
        || settings.translation_source_lang.len() > 32
        || settings.translation_target_lang.is_empty()
        || settings.translation_target_lang.len() > 32
        || settings.export_directory.len() > 4_096
        || settings.export_directory.chars().any(char::is_control)
    {
        return Err("tool defaults are invalid".into());
    }
    if !settings.export_directory.is_empty()
        && !std::path::Path::new(&settings.export_directory).is_absolute()
    {
        return Err("tool export directory must be empty or absolute".into());
    }
    Ok(())
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

fn validate_shortcut(label: &str, value: &str) -> Result<(), String> {
    if value.trim() != value
        || value.is_empty()
        || value.len() > 80
        || value.chars().any(char::is_control)
    {
        return Err(format!("{label} shortcut is invalid"));
    }
    let parts = value.split('+').collect::<Vec<_>>();
    if !(2..=5).contains(&parts.len()) || parts.iter().any(|part| part.is_empty()) {
        return Err(format!("{label} shortcut is invalid"));
    }
    let modifiers = &parts[..parts.len() - 1];
    let modifier_valid = |part: &&str| {
        matches!(
            part.to_ascii_lowercase().as_str(),
            "commandorcontrol"
                | "command"
                | "cmd"
                | "control"
                | "ctrl"
                | "alt"
                | "option"
                | "shift"
                | "super"
                | "meta"
        )
    };
    if !modifiers.iter().all(modifier_valid)
        || modifiers
            .iter()
            .map(|part| part.to_ascii_lowercase())
            .collect::<HashSet<_>>()
            .len()
            != modifiers.len()
    {
        return Err(format!("{label} shortcut has invalid modifiers"));
    }
    let key = parts[parts.len() - 1];
    let named_key = matches!(
        key.to_ascii_lowercase().as_str(),
        "space"
            | "tab"
            | "enter"
            | "escape"
            | "backspace"
            | "delete"
            | "insert"
            | "home"
            | "end"
            | "pageup"
            | "pagedown"
            | "arrowup"
            | "arrowdown"
            | "arrowleft"
            | "arrowright"
    );
    let function_key = key
        .strip_prefix('F')
        .or_else(|| key.strip_prefix('f'))
        .and_then(|number| number.parse::<u8>().ok())
        .is_some_and(|number| (1..=24).contains(&number));
    let single_key = key.chars().count() == 1
        && key.chars().all(|character| {
            character.is_ascii_alphanumeric() || ",./;'[]\\-=`".contains(character)
        });
    if !named_key && !function_key && !single_key {
        return Err(format!("{label} shortcut key is invalid"));
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

    #[test]
    fn rejects_malformed_or_conflicting_shortcuts() {
        let mut settings = AppSettings::default();
        settings.shortcuts.global_search = "K".into();
        assert_eq!(
            settings.validate(),
            Err("global search shortcut is invalid".into())
        );

        settings.shortcuts.global_search = "Control+Control+K".into();
        assert_eq!(
            settings.validate(),
            Err("global search shortcut has invalid modifiers".into())
        );

        settings.shortcuts.global_search = "Control+K".into();
        settings.shortcuts.settings = "control+k".into();
        assert_eq!(
            settings.validate(),
            Err("global search and settings shortcuts must be different".into())
        );
    }
}
