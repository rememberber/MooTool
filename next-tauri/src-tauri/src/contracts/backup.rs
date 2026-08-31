use serde::{Deserialize, Serialize};

pub const BACKUP_FORMAT_VERSION: u32 = 2;
pub const BACKUP_PRODUCT_ID: &str = "com.rememberber.mootool.next.tauri";
pub const DATABASE_SCHEMA_VERSION: u32 = 11;

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BackupManifest {
    pub format_version: u32,
    pub product_id: String,
    pub app_version: String,
    pub settings_schema_version: u32,
    pub database_schema_version: u32,
    pub created_at: i64,
    pub image_count: usize,
    pub vault_included: bool,
    pub vault_file_count: usize,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BackupExportResult {
    pub backup_path: String,
    pub image_count: usize,
    pub database_bytes: u64,
    pub vault_file_count: usize,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BackupImportResult {
    pub source_path: String,
    pub rollback_path: String,
    pub image_count: usize,
    pub vault_file_count: usize,
}
