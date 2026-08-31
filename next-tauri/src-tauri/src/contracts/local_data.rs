use serde::{Deserialize, Serialize};

use super::translation::TranslationProvider;

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNote {
    pub id: String,
    pub title: String,
    pub content: String,
    #[serde(default)]
    pub tags: Vec<String>,
    #[serde(default = "default_note_color")]
    pub color: String,
    #[serde(default)]
    pub folder_path: String,
    pub pinned: bool,
    pub created_at: i64,
    pub updated_at: i64,
}

impl QuickNote {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.title.chars().count() > 256 {
            return Err("note title cannot exceed 256 characters".into());
        }
        if self.content.len() > 2 * 1024 * 1024 {
            return Err("note content cannot exceed 2 MiB".into());
        }
        if self.tags.len() > 32
            || self
                .tags
                .iter()
                .any(|tag| tag.trim().is_empty() || tag.chars().count() > 40)
        {
            return Err(
                "note tags must contain at most 32 non-empty values of 40 characters".into(),
            );
        }
        if !matches!(
            self.color.as_str(),
            "default" | "coral" | "yellow" | "green" | "blue" | "purple" | "red"
        ) {
            return Err("unsupported note color".into());
        }
        validate_note_folder_path(&self.folder_path, true)?;
        validate_timestamps(self.created_at, self.updated_at)
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteFolder {
    pub path: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl QuickNoteFolder {
    pub fn validate(&self) -> Result<(), String> {
        validate_note_folder_path(&self.path, false)?;
        validate_timestamps(self.created_at, self.updated_at)
    }
}

pub fn validate_note_folder_path(path: &str, allow_empty: bool) -> Result<(), String> {
    if path.is_empty() {
        return if allow_empty {
            Ok(())
        } else {
            Err("note folder path cannot be empty".into())
        };
    }
    if path.len() > 512
        || path.starts_with('/')
        || path.ends_with('/')
        || path.contains("//")
        || path.contains('\\')
        || path.chars().any(char::is_control)
        || path
            .split('/')
            .any(|part| part.is_empty() || part == "." || part == ".." || part.chars().count() > 80)
    {
        return Err("invalid note folder path".into());
    }
    Ok(())
}

fn default_note_color() -> String {
    "default".into()
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteAttachment {
    pub id: String,
    pub note_id: String,
    pub name: String,
    pub mime_type: String,
    pub size_bytes: u64,
    pub created_at: i64,
}

impl QuickNoteAttachment {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        validate_id(&self.note_id)?;
        if self.name.trim().is_empty()
            || self.name.chars().count() > 255
            || self.name.contains('/')
            || self.name.contains('\\')
        {
            return Err("attachment name must contain 1-255 safe characters".into());
        }
        if self.mime_type.is_empty()
            || self.mime_type.len() > 128
            || self.size_bytes > 10 * 1024 * 1024
        {
            return Err("attachment metadata exceeds local limits".into());
        }
        if self.created_at < 0 {
            return Err("invalid attachment timestamp".into());
        }
        Ok(())
    }
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteAttachmentImportRequest {
    pub id: String,
    pub note_id: String,
    pub source_path: String,
    pub created_at: i64,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BoardMessage {
    pub id: String,
    pub content: String,
    pub color: String,
    pub pinned: bool,
    pub created_at: i64,
    pub updated_at: i64,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HostProfile {
    pub id: String,
    pub name: String,
    pub content: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl HostProfile {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.name.trim().is_empty() || self.name.chars().count() > 256 {
            return Err("host profile name must contain 1-256 characters".into());
        }
        if self.content.len() > 2 * 1024 * 1024 {
            return Err("host profile cannot exceed 2 MiB".into());
        }
        validate_timestamps(self.created_at, self.updated_at)
    }
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemHostsFile {
    pub path: String,
    pub content: String,
    pub writable: bool,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslationWord {
    pub id: String,
    pub source_text: String,
    pub target_text: String,
    pub source_lang: String,
    pub target_lang: String,
    pub remark: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl TranslationWord {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.source_text.trim().is_empty() || self.source_text.chars().count() > 50_000 {
            return Err("translation word source must contain 1 to 50000 characters".into());
        }
        if self.target_text.chars().count() > 50_000 || self.remark.chars().count() > 2_000 {
            return Err("translation word content exceeds local limits".into());
        }
        validate_language_pair(&self.source_lang, &self.target_lang)?;
        validate_timestamps(self.created_at, self.updated_at)
    }
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslationHistory {
    pub id: String,
    pub source_text: String,
    pub target_text: String,
    pub source_lang: String,
    pub target_lang: String,
    pub provider: TranslationProvider,
    pub created_at: i64,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OperationHistory {
    pub id: String,
    pub tool_id: String,
    pub action: String,
    pub summary: String,
    pub status: String,
    #[serde(default)]
    pub input_text: String,
    #[serde(default)]
    pub output_text: String,
    #[serde(default = "default_metadata_json")]
    pub metadata_json: String,
    pub created_at: i64,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolFavorite {
    pub id: String,
    pub tool_id: String,
    pub name: String,
    pub payload_json: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl ToolFavorite {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.tool_id.is_empty()
            || self.tool_id.len() > 64
            || !self
                .tool_id
                .bytes()
                .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
        {
            return Err("invalid tool favorite tool ID".into());
        }
        if self.name.trim().is_empty() || self.name.chars().count() > 80 {
            return Err("tool favorite name must contain 1 to 80 characters".into());
        }
        if self.payload_json.len() > 512 * 1024 {
            return Err("tool favorite payload cannot exceed 512 KiB".into());
        }
        let payload: serde_json::Value = serde_json::from_str(&self.payload_json)
            .map_err(|_| "tool favorite payload must be valid JSON".to_string())?;
        if !payload.is_object() {
            return Err("tool favorite payload must be a JSON object".into());
        }
        validate_timestamps(self.created_at, self.updated_at)
    }
}

fn default_metadata_json() -> String {
    "{}".into()
}

impl OperationHistory {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.tool_id.is_empty()
            || self.tool_id.len() > 64
            || !self
                .tool_id
                .bytes()
                .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
        {
            return Err("invalid operation history tool ID".into());
        }
        if self.action.trim().is_empty() || self.action.chars().count() > 80 {
            return Err("operation history action must contain 1 to 80 characters".into());
        }
        if self.summary.chars().count() > 2_000 {
            return Err("operation history summary cannot exceed 2000 characters".into());
        }
        if self.input_text.len() > 512 * 1024 || self.output_text.len() > 512 * 1024 {
            return Err("operation history input and output cannot exceed 512 KiB".into());
        }
        if self.metadata_json.len() > 64 * 1024 {
            return Err("operation history metadata cannot exceed 64 KiB".into());
        }
        let metadata: serde_json::Value = serde_json::from_str(&self.metadata_json)
            .map_err(|_| "operation history metadata must be valid JSON".to_string())?;
        if !metadata.is_object() {
            return Err("operation history metadata must be a JSON object".into());
        }
        if !matches!(self.status.as_str(), "info" | "success" | "error") {
            return Err("invalid operation history status".into());
        }
        if self.created_at < 0 {
            return Err("invalid operation history timestamp".into());
        }
        Ok(())
    }
}

impl TranslationHistory {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.source_text.trim().is_empty()
            || self.source_text.chars().count() > 50_000
            || self.target_text.chars().count() > 50_000
        {
            return Err("translation history content exceeds local limits".into());
        }
        validate_language_pair(&self.source_lang, &self.target_lang)?;
        if self.created_at < 0 {
            return Err("invalid translation history timestamp".into());
        }
        Ok(())
    }
}

impl BoardMessage {
    pub fn validate(&self) -> Result<(), String> {
        validate_id(&self.id)?;
        if self.content.trim().is_empty() {
            return Err("message content cannot be empty".into());
        }
        if self.content.len() > 64 * 1024 {
            return Err("message content cannot exceed 64 KiB".into());
        }
        if !matches!(
            self.color.as_str(),
            "blue" | "green" | "yellow" | "pink" | "purple" | "gray"
        ) {
            return Err("unsupported message color".into());
        }
        validate_timestamps(self.created_at, self.updated_at)
    }
}

fn validate_id(id: &str) -> Result<(), String> {
    if id.is_empty()
        || id.len() > 128
        || !id
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'-' | b'_'))
    {
        return Err("invalid local record ID".into());
    }
    Ok(())
}

fn validate_timestamps(created_at: i64, updated_at: i64) -> Result<(), String> {
    if created_at < 0 || updated_at < created_at {
        return Err("invalid local record timestamps".into());
    }
    Ok(())
}

fn validate_language_pair(source: &str, target: &str) -> Result<(), String> {
    if source.is_empty()
        || target.is_empty()
        || source.len() > 16
        || target.len() > 16
        || source == target
    {
        return Err("invalid translation language pair".into());
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_owned_local_records() {
        let note = QuickNote {
            id: "note-1".into(),
            title: "Tauri".into(),
            content: "Independent product".into(),
            tags: vec!["desktop".into()],
            color: "blue".into(),
            folder_path: "work/tauri".into(),
            pinned: true,
            created_at: 10,
            updated_at: 11,
        };
        assert!(note.validate().is_ok());

        let mut message = BoardMessage {
            id: "message-1".into(),
            content: "Ship it".into(),
            color: "blue".into(),
            pinned: false,
            created_at: 20,
            updated_at: 20,
        };
        assert!(message.validate().is_ok());
        message.color = "electron".into();
        assert!(message.validate().is_err());
    }
}
