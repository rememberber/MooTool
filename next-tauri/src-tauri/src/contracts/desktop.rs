use std::collections::BTreeMap;

use serde::{Deserialize, Serialize};

pub const WINDOW_STATE_SCHEMA_VERSION: u32 = 1;

#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum CloseDecision {
    Cancel,
    MinimizeToTray,
    Quit,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WindowPlacement {
    pub x: i32,
    pub y: i32,
    pub width: u32,
    pub height: u32,
    pub maximized: bool,
}

impl WindowPlacement {
    pub fn validate(&self) -> Result<(), String> {
        if !(480..=16_384).contains(&self.width) || !(360..=16_384).contains(&self.height) {
            return Err("window dimensions are outside the supported range".into());
        }
        if self.x.unsigned_abs() > 131_072 || self.y.unsigned_abs() > 131_072 {
            return Err("window position is outside the supported range".into());
        }
        Ok(())
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct WindowStateFile {
    pub schema_version: u32,
    pub windows: BTreeMap<String, WindowPlacement>,
}

impl Default for WindowStateFile {
    fn default() -> Self {
        Self {
            schema_version: WINDOW_STATE_SCHEMA_VERSION,
            windows: BTreeMap::new(),
        }
    }
}

impl WindowStateFile {
    pub fn validate(&self) -> Result<(), String> {
        if self.schema_version != WINDOW_STATE_SCHEMA_VERSION {
            return Err(format!(
                "unsupported window state schema {}, expected {}",
                self.schema_version, WINDOW_STATE_SCHEMA_VERSION
            ));
        }
        if self.windows.len() > 64 {
            return Err("window state contains too many windows".into());
        }
        for (label, placement) in &self.windows {
            if !valid_window_label(label) {
                return Err(format!("invalid window label in state: {label}"));
            }
            placement.validate()?;
        }
        Ok(())
    }
}

fn valid_window_label(label: &str) -> bool {
    !label.is_empty()
        && label.len() <= 80
        && label.bytes().all(|byte| {
            byte.is_ascii_lowercase() || byte.is_ascii_digit() || matches!(byte, b'-' | b'_' | b'.')
        })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_owned_window_state() {
        let mut state = WindowStateFile::default();
        state.windows.insert(
            "main".into(),
            WindowPlacement {
                x: -120,
                y: 40,
                width: 1440,
                height: 900,
                maximized: false,
            },
        );
        assert!(state.validate().is_ok());

        state.windows.insert(
            "../../electron".into(),
            WindowPlacement {
                x: 0,
                y: 0,
                width: 800,
                height: 600,
                maximized: false,
            },
        );
        assert!(state.validate().is_err());
    }
}
