use std::collections::{BTreeMap, HashSet};

use serde::{Deserialize, Deserializer, Serialize};

pub const SETTINGS_SCHEMA_VERSION: u32 = 12;

const PRODUCT_TOOL_IDS: [&str; 27] = [
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
    "file-preview",
    "batch-rename",
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
    #[serde(alias = "minimizeToTray")]
    Hide,
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
#[serde(rename_all = "kebab-case")]
pub enum InterfaceStyle {
    #[default]
    Modern,
    Quiet,
    Hero,
    Smartisan,
    MiuiV5,
    Claude,
}

fn default_interface_font_size() -> u8 {
    13
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum AccentColor {
    #[default]
    Blue,
    Indigo,
    Teal,
    Orange,
    Yellow,
    Coral,
    Green,
    Red,
    Purple,
}

#[derive(Clone, Copy, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum NavigationStyle {
    #[default]
    Classic,
    Card,
    Grouped,
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
    #[serde(default = "default_true")]
    pub escape_hides_window: bool,
}

impl Default for GeneralSettings {
    fn default() -> Self {
        Self {
            language: AppLanguage::default(),
            launch_at_login: false,
            close_behavior: CloseBehavior::default(),
            auto_check_updates: true,
            auto_download_updates: true,
            start_maximized: false,
            tray_enabled: true,
            escape_hides_window: true,
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct AppearanceSettings {
    pub theme: ThemePreference,
    #[serde(default)]
    pub interface_style: InterfaceStyle,
    #[serde(default = "default_true")]
    pub unified_background: bool,
    pub accent_color: AccentColor,
    #[serde(default = "default_interface_font_family", deserialize_with = "deserialize_interface_font_family")]
    pub font_family: String,
    #[serde(default = "default_interface_font_size")]
    pub font_size: u8,
}

impl Default for AppearanceSettings {
    fn default() -> Self {
        Self {
            theme: ThemePreference::default(),
            interface_style: InterfaceStyle::default(),
            unified_background: true,
            accent_color: AccentColor::default(),
            font_family: "system-ui".into(),
            font_size: default_interface_font_size(),
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

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LayoutSettings {
    pub hide_navigation_titles: bool,
    pub navigation_style: NavigationStyle,
    pub compact_navigation: bool,
    pub show_separators: bool,
    pub custom_groups: Vec<CustomToolGroup>,
    pub pane_sizes: BTreeMap<String, Vec<f64>>,
    pub show_recent: bool,
    pub hidden_navigation_tool_ids: Vec<String>,
}

#[derive(Deserialize)]
#[serde(default, rename_all = "camelCase")]
struct LayoutSettingsFile {
    hide_navigation_titles: Option<bool>,
    sidebar_compact: Option<bool>,
    navigation_style: NavigationStyle,
    compact_navigation: bool,
    show_separators: bool,
    custom_groups: Vec<CustomToolGroup>,
    #[serde(default, deserialize_with = "deserialize_pane_sizes")]
    pane_sizes: BTreeMap<String, Vec<f64>>,
    show_recent: bool,
    hidden_navigation_tool_ids: Option<Vec<String>>,
    hidden_tools: Option<Vec<String>>,
}

impl Default for LayoutSettingsFile {
    fn default() -> Self {
        Self {
            hide_navigation_titles: None,
            sidebar_compact: None,
            navigation_style: NavigationStyle::default(),
            compact_navigation: false,
            show_separators: true,
            custom_groups: Vec::new(),
            pane_sizes: BTreeMap::new(),
            show_recent: false,
            hidden_navigation_tool_ids: None,
            hidden_tools: None,
        }
    }
}

impl<'de> Deserialize<'de> for LayoutSettings {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let file = LayoutSettingsFile::deserialize(deserializer)?;
        Ok(Self {
            hide_navigation_titles: file.hide_navigation_titles.or(file.sidebar_compact).unwrap_or(false),
            navigation_style: file.navigation_style,
            compact_navigation: file.compact_navigation,
            show_separators: file.show_separators,
            custom_groups: file.custom_groups,
            pane_sizes: file.pane_sizes,
            show_recent: file.show_recent,
            hidden_navigation_tool_ids: file
                .hidden_navigation_tool_ids
                .or(file.hidden_tools)
                .unwrap_or_default(),
        })
    }
}

impl Default for LayoutSettings {
    fn default() -> Self {
        Self {
            hide_navigation_titles: false,
            navigation_style: NavigationStyle::default(),
            compact_navigation: false,
            show_separators: true,
            custom_groups: Vec::new(),
            pane_sizes: BTreeMap::new(),
            show_recent: false,
            hidden_navigation_tool_ids: Vec::new(),
        }
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct EditorSettings {
    #[serde(rename = "softWrap", alias = "wordWrap", default = "default_true")]
    pub soft_wrap: bool,
    #[serde(
        rename = "jsonFontName",
        alias = "jsonFontFamily",
        default = "default_editor_font_name",
        deserialize_with = "deserialize_editor_font_name"
    )]
    pub json_font_name: String,
    pub json_font_size: u8,
    #[serde(
        rename = "quickNoteFontName",
        alias = "quickNoteFontFamily",
        default = "default_editor_font_name",
        deserialize_with = "deserialize_editor_font_name"
    )]
    pub quick_note_font_name: String,
    pub quick_note_font_size: u8,
    pub sql_dialect: String,
}

impl Default for EditorSettings {
    fn default() -> Self {
        Self {
            soft_wrap: true,
            json_font_name: "ui-monospace".into(),
            json_font_size: 14,
            quick_note_font_name: "ui-monospace".into(),
            quick_note_font_size: 14,
            sql_dialect: "Standard SQL".into(),
        }
    }
}

fn default_editor_font_name() -> String {
    "ui-monospace".into()
}

fn deserialize_editor_font_name<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    Ok(normalize_editor_font_name(&String::deserialize(deserializer)?))
}

fn normalize_editor_font_name(value: &str) -> String {
    let trimmed: String = value.trim().chars().take(120).collect();
    match trimmed.as_str() {
        "" | "mono" => "ui-monospace".into(),
        "system" => "system-ui".into(),
        _ => trimmed,
    }
}

fn pane_size_key_valid(key: &str) -> bool {
    let count = key.chars().count();
    (1..=64).contains(&count)
        && key.chars().all(|char| char.is_ascii_alphanumeric() || char == '-')
}

fn deserialize_pane_sizes<'de, D>(deserializer: D) -> Result<BTreeMap<String, Vec<f64>>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = serde_json::Value::deserialize(deserializer)?;
    let mut sizes = BTreeMap::new();
    let Some(object) = value.as_object() else {
        return Ok(sizes);
    };
    for (key, entry) in object.iter().take(64) {
        if !pane_size_key_valid(key) {
            continue;
        }
        let Some(list) = entry.as_array() else {
            continue;
        };
        if !(2..=4).contains(&list.len()) {
            continue;
        }
        let numbers: Vec<f64> = list.iter().filter_map(serde_json::Value::as_f64).collect();
        if numbers.len() != list.len() || numbers.iter().any(|size| !size.is_finite() || *size <= 0.0) {
            continue;
        }
        let total: f64 = numbers.iter().sum();
        sizes.insert(
            key.clone(),
            numbers
                .iter()
                .map(|size| (size / total * 1_000_000.0).round() / 1_000_000.0)
                .collect(),
        );
    }
    Ok(sizes)
}

fn default_interface_font_family() -> String {
    "system-ui".into()
}

fn deserialize_interface_font_family<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    Ok(normalize_interface_font_family(&String::deserialize(
        deserializer,
    )?))
}

fn normalize_interface_font_family(value: &str) -> String {
    let trimmed: String = value.trim().chars().take(120).collect();
    match trimmed.as_str() {
        "" | "system" => "system-ui".into(),
        "mono" => "ui-monospace".into(),
        _ => trimmed,
    }
}

fn font_name_valid(value: &str) -> bool {
    !value.is_empty() && value.chars().count() <= 120 && !value.chars().any(char::is_control)
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkSettings {
    pub request_timeout_ms: u32,
    pub proxy_enabled: bool,
    pub proxy_host: String,
    pub proxy_port: String,
    pub proxy_username: String,
    pub translation_timeout_ms: u32,
}

#[derive(Deserialize)]
#[serde(default, rename_all = "camelCase")]
struct NetworkSettingsFile {
    request_timeout_ms: Option<u64>,
    translation_timeout_ms: Option<u64>,
    timeout_seconds: Option<u64>,
    translation_timeout_seconds: Option<u64>,
    proxy_enabled: Option<bool>,
    proxy_mode: Option<String>,
    proxy_host: String,
    proxy_port: serde_json::Value,
    proxy_username: String,
}

impl Default for NetworkSettingsFile {
    fn default() -> Self {
        Self {
            request_timeout_ms: None,
            translation_timeout_ms: None,
            timeout_seconds: None,
            translation_timeout_seconds: None,
            proxy_enabled: None,
            proxy_mode: None,
            proxy_host: String::new(),
            proxy_port: serde_json::Value::String(String::new()),
            proxy_username: String::new(),
        }
    }
}

fn normalize_timeout_ms(milliseconds: Option<u64>, seconds: Option<u64>, default_ms: u64) -> u32 {
    let raw = milliseconds.unwrap_or_else(|| seconds.map(|value| value.saturating_mul(1_000)).unwrap_or(default_ms));
    raw.clamp(1_000, 120_000) as u32
}

impl<'de> Deserialize<'de> for NetworkSettings {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let file = NetworkSettingsFile::deserialize(deserializer)?;
        let proxy_enabled = file.proxy_enabled.unwrap_or(file.proxy_mode.as_deref() == Some("manual"));
        let proxy_port = match file.proxy_port {
            serde_json::Value::String(text) => text.trim().chars().take(16).collect(),
            serde_json::Value::Number(number) if proxy_enabled || number.as_u64() != Some(8080) => number.to_string(),
            _ => String::new(),
        };
        Ok(Self {
            request_timeout_ms: normalize_timeout_ms(file.request_timeout_ms, file.timeout_seconds, 30_000),
            proxy_enabled,
            proxy_host: file.proxy_host,
            proxy_port,
            proxy_username: file.proxy_username,
            translation_timeout_ms: normalize_timeout_ms(file.translation_timeout_ms, file.translation_timeout_seconds, 15_000),
        })
    }
}

impl Default for NetworkSettings {
    fn default() -> Self {
        Self {
            request_timeout_ms: 30_000,
            proxy_enabled: false,
            proxy_host: String::new(),
            proxy_port: String::new(),
            proxy_username: String::new(),
            translation_timeout_ms: 15_000,
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
        }
    }
}

pub const OPERATION_HISTORY_LIMIT: u16 = 500;

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct DataSettings {}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct VaultSettings {
    pub auto_commit: bool,
    pub root_directory: Option<String>,
    pub git_remote: String,
    pub git_username: String,
    pub auto_commit_idle_seconds: u32,
    pub auto_commit_inactive_seconds: u32,
    pub auto_pull_minutes: u32,
    pub hide_gitignored_files: bool,
    pub quick_note_tree_expand_mode: VaultTreeExpandMode,
    pub json_tree_expand_mode: VaultTreeExpandMode,
}

#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum VaultTreeExpandMode {
    ExpandAll,
    CollapseAll,
}

impl Default for VaultTreeExpandMode {
    fn default() -> Self {
        Self::ExpandAll
    }
}

impl Default for VaultSettings {
    fn default() -> Self {
        Self {
            auto_commit: false,
            root_directory: None,
            git_remote: String::new(),
            git_username: String::new(),
            auto_commit_idle_seconds: 30,
            auto_commit_inactive_seconds: 120,
            auto_pull_minutes: 0,
            hide_gitignored_files: true,
            quick_note_tree_expand_mode: VaultTreeExpandMode::ExpandAll,
            json_tree_expand_mode: VaultTreeExpandMode::ExpandAll,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ShortcutSettings {
    pub search: String,
    pub settings: String,
    pub global_search_enabled: bool,
    pub global_search: String,
}

#[derive(Deserialize)]
#[serde(default, rename_all = "camelCase")]
struct ShortcutSettingsFile {
    search: Option<String>,
    settings: Option<String>,
    global_search_enabled: Option<bool>,
    global_search: Option<String>,
}

impl Default for ShortcutSettingsFile {
    fn default() -> Self {
        Self {
            search: None,
            settings: None,
            global_search_enabled: None,
            global_search: None,
        }
    }
}

impl<'de> Deserialize<'de> for ShortcutSettings {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let file = ShortcutSettingsFile::deserialize(deserializer)?;
        let legacy_palette = file.global_search_enabled.is_none();
        let search = file.search.clone().unwrap_or_else(|| {
            if legacy_palette {
                file.global_search.clone().unwrap_or_else(|| "CommandOrControl+K".into())
            } else {
                "CommandOrControl+K".into()
            }
        });
        Ok(Self {
            search,
            settings: file.settings.unwrap_or_else(|| "CommandOrControl+,".into()),
            global_search_enabled: file.global_search_enabled.unwrap_or(false),
            global_search: if legacy_palette {
                "CommandOrControl+Shift+Space".into()
            } else {
                file.global_search.unwrap_or_else(|| "CommandOrControl+Shift+Space".into())
            },
        })
    }
}

impl Default for ShortcutSettings {
    fn default() -> Self {
        Self {
            search: "CommandOrControl+K".into(),
            settings: "CommandOrControl+,".into(),
            global_search_enabled: false,
            global_search: "CommandOrControl+Shift+Space".into(),
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
        if !(10..=24).contains(&self.editor.json_font_size)
            || !(10..=24).contains(&self.editor.quick_note_font_size)
        {
            return Err("tool editor font sizes must be between 10 and 24".into());
        }
        if !font_name_valid(&self.editor.json_font_name)
            || !font_name_valid(&self.editor.quick_note_font_name)
        {
            return Err("tool editor font name is invalid".into());
        }
        if ![
            "Standard SQL",
            "MySQL",
            "MariaDB",
            "PostgreSQL",
            "Oracle PL/SQL",
            "SQL Server Transact-SQL",
            "IBM DB2",
            "Couchbase N1QL",
            "Amazon Redshift",
            "Spark",
        ]
        .contains(&self.editor.sql_dialect.as_str())
        {
            return Err("unsupported SQL dialect".into());
        }
        if !(1_000..=120_000).contains(&self.network.request_timeout_ms) {
            return Err("network timeout must be between 1000 and 120000 milliseconds".into());
        }
        if !(1_000..=120_000).contains(&self.network.translation_timeout_ms) {
            return Err("translation timeout must be between 1000 and 120000 milliseconds".into());
        }
        if self.network.proxy_port.chars().count() > 16
            || self.network.proxy_host.len() > 255
            || self.network.proxy_username.len() > 255
            || self.network.proxy_host.chars().any(char::is_control)
            || self.network.proxy_port.chars().any(char::is_control)
            || self.network.proxy_username.chars().any(char::is_control)
        {
            return Err("proxy settings are invalid".into());
        }
        if !font_name_valid(&self.appearance.font_family) {
            return Err("interface font family is invalid".into());
        }
        if !(12..=18).contains(&self.appearance.font_size) {
            return Err("interface font size must be between 12 and 18".into());
        }
        validate_custom_groups(&self.layout.custom_groups)?;
        validate_tool_ids(
            "hidden tools",
            &self.layout.hidden_navigation_tool_ids,
            PRODUCT_TOOL_IDS.len(),
        )?;
        if self
            .layout
            .hidden_navigation_tool_ids
            .iter()
            .any(|id| !PRODUCT_TOOL_IDS.contains(&id.as_str()))
        {
            return Err("hidden tools contains an unknown tool".into());
        }
        if self.layout.pane_sizes.len() > 64
            || self.layout.pane_sizes.iter().any(|(key, sizes)| {
                !pane_size_key_valid(key)
                    || !(2..=4).contains(&sizes.len())
                    || sizes.iter().any(|size| !size.is_finite() || *size <= 0.0)
            })
        {
            return Err("pane sizes contain an invalid entry".into());
        }
        if !(5..=3600).contains(&self.vault.auto_commit_idle_seconds)
            || !(5..=3600).contains(&self.vault.auto_commit_inactive_seconds)
            || self.vault.auto_pull_minutes > 1440
        {
            return Err("vault git schedule is outside the supported range".into());
        }
        if self.vault.git_remote.len() > 2048
            || self.vault.git_username.len() > 120
            || self.vault.git_remote.chars().any(char::is_control)
            || self.vault.git_username.chars().any(char::is_control)
            || !(self.vault.git_remote.is_empty()
                || self.vault.git_remote.starts_with("https://")
                || self.vault.git_remote.starts_with("http://")
                || self.vault.git_remote.starts_with("ssh://")
                || self.vault.git_remote.starts_with("git://")
                || self.vault.git_remote.starts_with("git@")
                || self.vault.git_remote.starts_with("file://"))
        {
            return Err("vault git remote or username is invalid".into());
        }
        if self.vault.root_directory.as_ref().is_some_and(|path| {
            path.trim().is_empty()
                || path.len() > 4_096
                || !std::path::Path::new(path).is_absolute()
        }) {
            return Err("vault root directory must be an absolute path".into());
        }
        validate_shortcut("search", &self.shortcuts.search)?;
        validate_shortcut("global search", &self.shortcuts.global_search)?;
        validate_shortcut("settings", &self.shortcuts.settings)?;
        if self.shortcuts.search.eq_ignore_ascii_case(&self.shortcuts.settings)
            || self.shortcuts.global_search.eq_ignore_ascii_case(&self.shortcuts.settings)
            || self.shortcuts.search.eq_ignore_ascii_case(&self.shortcuts.global_search)
        {
            return Err("search, global search, and settings shortcuts must be different".into());
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
        || !matches!(settings.translation_provider.as_str(), "google" | "bing" | "deepl")
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
            return Err(format!("custom group must contain between 1 and {} tools", PRODUCT_TOOL_IDS.len()));
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
        assert!(settings.general.escape_hides_window);
        assert_eq!(settings.appearance.theme, ThemePreference::System);
        assert_eq!(settings.appearance.interface_style, InterfaceStyle::Modern);
        assert!(settings.appearance.unified_background);
        assert_eq!(settings.appearance.font_size, 13);
        assert!(settings.layout.custom_groups.is_empty());
        assert_eq!(settings.layout.navigation_style, NavigationStyle::Classic);
        assert!(settings.layout.show_separators);
        assert!(!settings.layout.show_recent);
        assert!(settings.general.auto_download_updates);
        assert_eq!(settings.editor.json_font_size, 14);
        assert_eq!(settings.editor.quick_note_font_name, "ui-monospace");
        let legacy: EditorSettings = serde_json::from_str(
            r#"{"jsonFontFamily":"mono","quickNoteFontFamily":"system"}"#,
        )
        .expect("editor");
        assert_eq!(legacy.json_font_name, "ui-monospace");
        assert_eq!(legacy.quick_note_font_name, "system-ui");
        let stored = serde_json::to_value(&legacy).expect("json");
        assert_eq!(stored["jsonFontName"], "ui-monospace");
        assert!(stored.get("jsonFontFamily").is_none());
        let panes: LayoutSettings = serde_json::from_str(
            r#"{"paneSizes":{"json":[200,600,200],"legacy":260,"../unsafe":[1,1]}}"#,
        )
        .expect("layout");
        assert_eq!(panes.pane_sizes.get("json").expect("json"), &vec![0.2, 0.6, 0.2]);
        assert!(!panes.pane_sizes.contains_key("legacy"));
        assert!(!panes.pane_sizes.contains_key("../unsafe"));
        let manual: NetworkSettings = serde_json::from_str(r#"{"proxyMode":"manual","proxyPort":7890}"#).expect("proxy");
        assert!(manual.proxy_enabled);
        assert_eq!(manual.proxy_port, "7890");
        let system: NetworkSettings = serde_json::from_str(r#"{"proxyMode":"system","proxyPort":8080}"#).expect("proxy");
        assert!(!system.proxy_enabled);
        assert_eq!(system.proxy_port, "");
        let stored_proxy = serde_json::to_value(NetworkSettings::default()).expect("proxy json");
        assert_eq!(stored_proxy["proxyEnabled"], false);
        assert_eq!(stored_proxy["proxyPort"], "");
        let interface_font: AppearanceSettings = serde_json::from_str(r#"{"fontFamily":"mono"}"#).expect("font");
        assert_eq!(interface_font.font_family, "ui-monospace");
        let system_font: AppearanceSettings = serde_json::from_str(r#"{"fontFamily":"system"}"#).expect("font");
        assert_eq!(system_font.font_family, "system-ui");
        assert_eq!(AppearanceSettings::default().font_family, "system-ui");
        let legacy_scale: AppearanceSettings = serde_json::from_str(r#"{"uiScale":90}"#).expect("scale");
        assert!(serde_json::to_value(legacy_scale).expect("appearance")["uiScale"].is_null());
        assert_eq!(settings.editor.quick_note_font_size, 14);
        assert_eq!(settings.network.translation_timeout_ms, 15_000);
        let legacy_timeout: NetworkSettings = serde_json::from_str(r#"{"timeoutSeconds":45,"translationTimeoutSeconds":20}"#).expect("timeout");
        assert_eq!(legacy_timeout.request_timeout_ms, 45_000);
        assert_eq!(legacy_timeout.translation_timeout_ms, 20_000);
        let clamped_timeout: NetworkSettings = serde_json::from_str(r#"{"requestTimeoutMs":50,"translationTimeoutMs":999999}"#).expect("timeout");
        assert_eq!(clamped_timeout.request_timeout_ms, 1_000);
        assert_eq!(clamped_timeout.translation_timeout_ms, 120_000);
        let legacy_layout: LayoutSettings = serde_json::from_str(r#"{"sidebarCompact":true,"hiddenTools":["json","missing"]}"#).expect("layout");
        assert!(legacy_layout.hide_navigation_titles);
        assert_eq!(legacy_layout.hidden_navigation_tool_ids, vec!["json".to_string(), "missing".to_string()]);
        let stored_layout = serde_json::to_value(LayoutSettings::default()).expect("layout json");
        assert_eq!(stored_layout["hideNavigationTitles"], false);
        assert!(stored_layout.get("density").is_none());
        assert!(stored_layout.get("sidebarCompact").is_none());
        assert!(stored_layout.get("showGroupTitles").is_none());
        assert!(stored_layout.get("hiddenNavigationToolIds").is_some());
        assert!(settings.editor.soft_wrap);
        let legacy_wrap: EditorSettings = serde_json::from_str(r#"{"wordWrap":false,"fontSize":18,"tabSize":8}"#).expect("editor");
        assert!(!legacy_wrap.soft_wrap);
        let stored_editor = serde_json::to_value(EditorSettings::default()).expect("editor json");
        assert_eq!(stored_editor["softWrap"], true);
        assert!(stored_editor.get("wordWrap").is_none());
        assert!(stored_editor.get("fontSize").is_none());
        assert!(stored_editor.get("tabSize").is_none());
        let legacy_shortcuts: ShortcutSettings = serde_json::from_str(r#"{"globalSearch":"CommandOrControl+K","settings":"CommandOrControl+,"}"#).expect("shortcuts");
        assert_eq!(legacy_shortcuts.search, "CommandOrControl+K");
        assert!(!legacy_shortcuts.global_search_enabled);
        assert_eq!(legacy_shortcuts.global_search, "CommandOrControl+Shift+Space");
        let stored_shortcuts = serde_json::to_value(ShortcutSettings::default()).expect("shortcuts json");
        assert_eq!(stored_shortcuts["search"], "CommandOrControl+K");
        assert_eq!(stored_shortcuts["globalSearch"], "CommandOrControl+Shift+Space");
        assert_eq!(stored_shortcuts["globalSearchEnabled"], false);
        let legacy_data: DataSettings = serde_json::from_str(r#"{"historyLimit":42}"#).expect("data");
        let stored_data = serde_json::to_value(legacy_data).expect("data json");
        assert!(stored_data.get("historyLimit").is_none());
        assert!(serde_json::to_value(DataSettings::default()).expect("default data").get("historyLimit").is_none());
        assert_eq!(settings.vault.quick_note_tree_expand_mode, VaultTreeExpandMode::ExpandAll);
        assert_eq!(settings.vault.json_tree_expand_mode, VaultTreeExpandMode::ExpandAll);
        assert!(settings.validate().is_ok());
    }

    #[test]
    fn rejects_values_outside_owned_ranges() {
        let mut settings = AppSettings::default();
        settings.editor.json_font_size = 40;
        assert_eq!(
            settings.validate(),
            Err("tool editor font sizes must be between 10 and 24".into())
        );

        settings.editor.json_font_size = 14;
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
            Err("search, global search, and settings shortcuts must be different".into())
        );
    }

    #[test]
    fn missing_escape_hides_window_stays_enabled() {
        let settings: GeneralSettings = serde_json::from_str(r#"{"language":"en-US"}"#).expect("settings");
        assert!(settings.escape_hides_window);
        let disabled: GeneralSettings = serde_json::from_str(r#"{"escapeHidesWindow":false}"#).expect("settings");
        assert!(!disabled.escape_hides_window);
        let hidden: GeneralSettings = serde_json::from_str(r#"{"closeBehavior":"minimizeToTray"}"#).expect("settings");
        assert_eq!(hidden.close_behavior, CloseBehavior::Hide);
        let stored = serde_json::to_string(&CloseBehavior::Hide).expect("json");
        assert_eq!(stored, "\"hide\"");
    }
}
