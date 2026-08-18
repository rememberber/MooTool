use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, Deserialize, Eq, Hash, PartialEq, Serialize)]
#[serde(rename_all = "kebab-case")]
pub enum ManagedToolId {
    Calculator,
    Color,
    Config,
    Cron,
    Crypto,
    Host,
    Http,
    Image,
    Encode,
    EditorLab,
    Json,
    MessageBoard,
    Network,
    Pdf,
    Protobuf,
    QuickNote,
    Qrcode,
    Reformat,
    Regex,
    Runtime,
    Timestamp,
    TextDiff,
    Translation,
    Ua,
    Variables,
    System,
    WebviewProbe,
}

#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolWebviewBounds {
    pub x: f64,
    pub y: f64,
    pub width: f64,
    pub height: f64,
}

impl ToolWebviewBounds {
    pub fn validate(self) -> Result<Self, String> {
        if !self.x.is_finite()
            || !self.y.is_finite()
            || !self.width.is_finite()
            || !self.height.is_finite()
        {
            return Err("tool WebView bounds must contain finite values".into());
        }
        if self.width < 320.0 || self.height < 240.0 {
            return Err("tool WebView bounds must be at least 320 × 240".into());
        }
        Ok(self)
    }
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum ToolWebviewPlacement {
    #[default]
    Closed,
    Docked,
    Detached,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolSessionReport {
    pub session_id: String,
    pub state_revision: u64,
    pub state_digest: String,
    pub state_summary: String,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolWebviewSnapshot {
    pub tool_id: ManagedToolId,
    pub exists: bool,
    pub visible: bool,
    pub placement: ToolWebviewPlacement,
    pub reparent_operations: u32,
    pub page_loads: u32,
    pub session_id: Option<String>,
    pub state_revision: u64,
    pub state_digest: String,
    pub state_summary: String,
    pub last_stress_cycles: u32,
    pub last_stress_passed: Option<bool>,
}
