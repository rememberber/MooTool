use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultFileEntry {
    pub relative_path: String,
    pub size_bytes: u64,
    pub modified_at: u64,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultDocument {
    pub relative_path: String,
    pub content: String,
    pub fingerprint: String,
    pub modified_at: u64,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultSaveRequest {
    pub relative_path: String,
    pub content: String,
    pub expected_fingerprint: Option<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultTrashResult {
    pub relative_path: String,
    pub recovery_path: String,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultSnapshot {
    pub root_path: Option<String>,
    pub files: Vec<VaultFileEntry>,
    pub directories: Vec<String>,
    pub git: VaultGitStatus,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultGitStatus {
    pub available: bool,
    pub repository: bool,
    pub branch: String,
    pub dirty: bool,
    pub changed_files: usize,
    pub ahead: u32,
    pub behind: u32,
    pub remote: String,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultGitCommit {
    pub hash: String,
    pub author: String,
    pub timestamp: i64,
    pub subject: String,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultGitDetails {
    pub diff: String,
    pub commits: Vec<VaultGitCommit>,
}

#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum VaultGitOperation {
    Init,
    Pull,
    Commit,
    Push,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultGitRequest {
    pub request_id: String,
    pub operation: VaultGitOperation,
    pub message: Option<String>,
    pub editor_dirty: bool,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultGitResult {
    pub operation: VaultGitOperation,
    pub success: bool,
    pub exit_code: Option<i32>,
    pub stdout: String,
    pub stderr: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct VaultChangedEvent {
    pub reason: String,
}
