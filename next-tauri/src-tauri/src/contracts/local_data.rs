use serde::{Deserialize, Serialize};

use super::translation::TranslationProvider;

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNote {
    pub id: String,
    pub title: String,
    pub content: String,
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
        validate_timestamps(self.created_at, self.updated_at)
    }
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
    pub created_at: i64,
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
