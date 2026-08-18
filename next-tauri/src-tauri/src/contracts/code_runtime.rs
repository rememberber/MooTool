use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, Deserialize, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum CodeRuntimeId {
    Java,
    Groovy,
    Python,
    Node,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CodeRuntimeStatus {
    pub id: CodeRuntimeId,
    pub available: bool,
    pub command: String,
    pub version: String,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CodeRunSpec {
    pub request_id: String,
    pub runtime: CodeRuntimeId,
    pub code: String,
    pub timeout_ms: u64,
    pub arguments: Vec<String>,
    pub working_directory: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CodeOutputEvent {
    pub stream: String,
    pub text: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CodeRunResult {
    pub exit_code: Option<i32>,
    pub stdout: String,
    pub stderr: String,
    pub duration_ms: u128,
    pub command: String,
    pub timed_out: bool,
    pub cancelled: bool,
}
