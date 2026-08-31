use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentVariable {
    pub name: String,
    pub value: String,
    pub sensitive: bool,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemSnapshot {
    pub os_name: String,
    pub os_version: String,
    pub kernel_version: String,
    pub host_name: String,
    pub architecture: String,
    pub cpu_brand: String,
    pub physical_cores: usize,
    pub logical_cores: usize,
    pub total_memory_bytes: u64,
    pub available_memory_bytes: u64,
    pub process_memory_bytes: u64,
    pub uptime_seconds: u64,
    pub cpu_usage_percent: f32,
    pub cpu_frequency_mhz: u64,
    pub total_swap_bytes: u64,
    pub used_swap_bytes: u64,
    pub disks: Vec<SystemDisk>,
    pub network_interfaces: Vec<SystemNetworkInterface>,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemDisk {
    pub name: String,
    pub mount_point: String,
    pub file_system: String,
    pub total_bytes: u64,
    pub available_bytes: u64,
    pub removable: bool,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemNetworkInterface {
    pub name: String,
    pub addresses: Vec<String>,
    pub mac_address: String,
    pub received_bytes: u64,
    pub transmitted_bytes: u64,
}

#[derive(Clone, Debug, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct FrontendErrorReport {
    pub code: String,
    pub message: String,
    pub context: String,
    pub retryable: bool,
    pub stack: Option<String>,
}

impl FrontendErrorReport {
    pub fn validate(&self) -> Result<(), String> {
        if self.code.is_empty()
            || self.code.len() > 80
            || !self
                .code
                .bytes()
                .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'_')
        {
            return Err("frontend error code is invalid".into());
        }
        if self.message.trim().is_empty() || self.message.len() > 16_384 {
            return Err("frontend error message is invalid".into());
        }
        if self.context.trim().is_empty() || self.context.len() > 160 {
            return Err("frontend error context is invalid".into());
        }
        if self
            .stack
            .as_ref()
            .is_some_and(|stack| stack.len() > 32_768)
        {
            return Err("frontend error stack is too large".into());
        }
        Ok(())
    }
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsExportResult {
    pub bundle_path: String,
    pub log_file_count: usize,
    pub created_at: u64,
}
