use std::{
    collections::HashMap,
    path::PathBuf,
    process::Stdio,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, Ordering},
    },
    time::Duration,
};

use tauri::{Emitter, Manager};
use tokio::{process::Command, sync::Notify, time::timeout};

use crate::{
    commands::settings::SETTINGS_CHANGED_EVENT,
    contracts::{
        error::AppResult,
        vault::{
            VaultDocument, VaultGitCommit, VaultGitDetails, VaultGitOperation, VaultGitRequest,
            VaultGitResult, VaultGitStatus, VaultSaveRequest, VaultSnapshot, VaultTrashResult,
        },
    },
    repositories::{settings::SettingsRepository, vault::VaultRepository},
};

const GIT_TIMEOUT: Duration = Duration::from_secs(120);
const MAX_GIT_OUTPUT_BYTES: usize = 1024 * 1024;

#[derive(Default)]
pub struct VaultGitManager {
    active: Mutex<HashMap<String, Arc<GitControl>>>,
}

#[derive(Default)]
struct GitControl {
    cancelled: AtomicBool,
    notify: Notify,
}

impl VaultGitManager {
    fn register(&self, request_id: &str) -> Result<Arc<GitControl>, String> {
        if request_id.is_empty()
            || request_id.len() > 128
            || !request_id
                .bytes()
                .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'-' | b'_' | b'.'))
        {
            return Err("invalid Vault Git request ID".into());
        }
        let mut active = self
            .active
            .lock()
            .map_err(|_| "Vault Git manager state poisoned")?;
        if active.contains_key(request_id) {
            return Err("Vault Git request ID is already active".into());
        }
        let control = Arc::new(GitControl::default());
        active.insert(request_id.into(), control.clone());
        Ok(control)
    }

    fn finish(&self, request_id: &str) {
        if let Ok(mut active) = self.active.lock() {
            active.remove(request_id);
        }
    }

    fn cancel(&self, request_id: &str) -> bool {
        let control = self
            .active
            .lock()
            .ok()
            .and_then(|active| active.get(request_id).cloned());
        if let Some(control) = control {
            control.cancelled.store(true, Ordering::SeqCst);
            control.notify.notify_one();
            true
        } else {
            false
        }
    }
}

#[tauri::command]
pub async fn get_vault_snapshot(
    repository: tauri::State<'_, VaultRepository>,
) -> AppResult<VaultSnapshot> {
    let Some(root) = repository.configured_root()? else {
        return Ok(VaultSnapshot {
            root_path: None,
            files: Vec::new(),
            directories: Vec::new(),
            git: git_availability().await,
        });
    };
    Ok(VaultSnapshot {
        root_path: Some(root.to_string_lossy().into_owned()),
        files: repository.list_files()?,
        directories: repository.list_directories()?,
        git: inspect_git_status(&root).await?,
    })
}

#[tauri::command]
pub async fn configure_vault(
    app: tauri::AppHandle,
    repository: tauri::State<'_, VaultRepository>,
    settings: tauri::State<'_, SettingsRepository>,
    root_directory: String,
) -> AppResult<VaultSnapshot> {
    let previous_root = repository.configured_root()?;
    let root = repository.configure(&app, PathBuf::from(root_directory))?;
    let mut next = settings.snapshot();
    next.vault.root_directory = Some(root.to_string_lossy().into_owned());
    let saved = match settings.replace(next) {
        Ok(saved) => saved,
        Err(error) => {
            if let Some(previous_root) = previous_root {
                let _ = repository.configure(&app, previous_root);
            } else {
                let _ = repository.disconnect();
            }
            return Err(error.into());
        }
    };
    app.emit(SETTINGS_CHANGED_EVENT, &saved)
        .map_err(|error| format!("Vault was configured but settings sync failed: {error}"))?;
    tracing::info!(vault.root = %crate::contracts::error::redact_for_log(&root.to_string_lossy()), "JSON Vault configured");
    get_vault_snapshot(repository).await
}

#[tauri::command]
pub fn disconnect_vault(
    app: tauri::AppHandle,
    repository: tauri::State<'_, VaultRepository>,
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<()> {
    repository.disconnect()?;
    let mut next = settings.snapshot();
    next.vault.root_directory = None;
    let saved = settings.replace(next)?;
    app.emit(SETTINGS_CHANGED_EVENT, &saved)
        .map_err(|error| format!("Vault was disconnected but settings sync failed: {error}"))?;
    tracing::info!("JSON Vault disconnected");
    Ok(())
}

#[tauri::command]
pub fn read_vault_document(
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
) -> AppResult<VaultDocument> {
    Ok(repository.read_document(&relative_path)?)
}

#[tauri::command]
pub fn save_vault_document(
    repository: tauri::State<'_, VaultRepository>,
    request: VaultSaveRequest,
) -> AppResult<VaultDocument> {
    let document = repository.save_document(request)?;
    tracing::info!(vault.document = %document.relative_path, "JSON Vault document saved");
    Ok(document)
}

#[tauri::command]
pub fn create_vault_directory(
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
) -> AppResult<String> {
    let path = repository.create_directory(&relative_path)?;
    tracing::info!(vault.directory = %path, "JSON Vault directory created");
    Ok(path)
}

#[tauri::command]
pub fn move_vault_entry(
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
    destination_path: String,
    expected_fingerprint: Option<String>,
) -> AppResult<String> {
    let path = repository.move_entry(
        &relative_path,
        &destination_path,
        expected_fingerprint.as_deref(),
    )?;
    tracing::info!(vault.entry = %relative_path, vault.destination = %path, "JSON Vault entry moved");
    Ok(path)
}

#[tauri::command]
pub fn duplicate_vault_document(
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
    destination_path: String,
    expected_fingerprint: String,
) -> AppResult<VaultDocument> {
    let document =
        repository.duplicate_document(&relative_path, &destination_path, &expected_fingerprint)?;
    tracing::info!(vault.document = %document.relative_path, "JSON Vault document duplicated");
    Ok(document)
}

#[tauri::command]
pub fn delete_vault_document(
    app: tauri::AppHandle,
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
    expected_fingerprint: String,
) -> AppResult<VaultTrashResult> {
    let current = repository.read_document(&relative_path)?;
    if current.fingerprint != expected_fingerprint {
        return Err("JSON Vault document changed outside MooTool; reload before deleting".into());
    }
    let trash_root = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Vault recovery directory: {error}"))?
        .join("vault-trash");
    let result = repository.trash_document(&relative_path, &trash_root)?;
    tracing::info!(vault.document = %result.relative_path, "JSON Vault document moved to recovery storage");
    Ok(result)
}

#[tauri::command]
pub fn delete_vault_entry(
    app: tauri::AppHandle,
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
    expected_fingerprint: Option<String>,
) -> AppResult<VaultTrashResult> {
    let trash_root = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Vault recovery directory: {error}"))?
        .join("vault-trash");
    let result =
        repository.trash_entry(&relative_path, expected_fingerprint.as_deref(), &trash_root)?;
    tracing::info!(vault.entry = %result.relative_path, "JSON Vault entry moved to recovery storage");
    Ok(result)
}

#[tauri::command]
pub async fn get_vault_git_status(
    repository: tauri::State<'_, VaultRepository>,
) -> AppResult<VaultGitStatus> {
    Ok(inspect_git_status(&repository.root()?).await?)
}

#[tauri::command]
pub async fn get_vault_git_details(
    repository: tauri::State<'_, VaultRepository>,
    relative_path: Option<String>,
) -> AppResult<VaultGitDetails> {
    let root = repository.root()?;
    let selected = relative_path
        .as_deref()
        .map(validate_git_relative_path)
        .transpose()?;
    let mut diff_arguments = vec!["diff", "--no-ext-diff", "--no-color"];
    if let Some(path) = selected {
        diff_arguments.extend(["--", path]);
    }
    let diff_output = run_git_unmanaged(&root, &diff_arguments).await?;
    if !diff_output.status.success() {
        return Err(bounded_output(diff_output.stderr).into());
    }
    let mut staged_arguments = vec!["diff", "--cached", "--no-ext-diff", "--no-color"];
    if let Some(path) = selected {
        staged_arguments.extend(["--", path]);
    }
    let staged_output = run_git_unmanaged(&root, &staged_arguments).await?;
    if !staged_output.status.success() {
        return Err(bounded_output(staged_output.stderr).into());
    }
    let mut combined_diff = Vec::new();
    if !diff_output.stdout.is_empty() {
        combined_diff.extend_from_slice(b"Unstaged changes\n\n");
        combined_diff.extend_from_slice(&diff_output.stdout);
    }
    if !staged_output.stdout.is_empty() {
        if !combined_diff.is_empty() {
            combined_diff.extend_from_slice(b"\n");
        }
        combined_diff.extend_from_slice(b"Staged changes\n\n");
        combined_diff.extend_from_slice(&staged_output.stdout);
    }
    let mut log_arguments = vec![
        "log",
        "-n",
        "50",
        "--date-order",
        "--pretty=format:%H%x1f%an%x1f%ct%x1f%s%x1e",
    ];
    if let Some(path) = selected {
        log_arguments.extend(["--", path]);
    }
    let log_output = run_git_unmanaged(&root, &log_arguments).await?;
    let commits = if log_output.status.success() {
        parse_git_commits(&String::from_utf8_lossy(&log_output.stdout))
    } else {
        Vec::new()
    };
    Ok(VaultGitDetails {
        diff: bounded_output(combined_diff),
        commits,
    })
}

#[tauri::command]
pub async fn configure_vault_git_remote(
    repository: tauri::State<'_, VaultRepository>,
    remote: String,
) -> AppResult<VaultGitStatus> {
    let root = repository.root()?;
    let remote = validate_git_remote(&remote)?;
    let current = run_git_unmanaged(&root, &["remote", "get-url", "origin"]).await?;
    let exists = current.status.success();
    let output = if remote.is_empty() {
        if exists {
            run_git_unmanaged(&root, &["remote", "remove", "origin"]).await?
        } else {
            return Ok(inspect_git_status(&root).await?);
        }
    } else if exists {
        run_git_unmanaged(&root, &["remote", "set-url", "origin", &remote]).await?
    } else {
        run_git_unmanaged(&root, &["remote", "add", "origin", &remote]).await?
    };
    if !output.status.success() {
        return Err(bounded_output(output.stderr).into());
    }
    Ok(inspect_git_status(&root).await?)
}

#[tauri::command]
pub async fn run_vault_git(
    repository: tauri::State<'_, VaultRepository>,
    manager: tauri::State<'_, VaultGitManager>,
    request: VaultGitRequest,
) -> AppResult<VaultGitResult> {
    validate_git_request(&request)?;
    let root = repository.root()?;
    if request.editor_dirty
        && matches!(
            request.operation,
            VaultGitOperation::Pull | VaultGitOperation::Commit
        )
    {
        return Err(
            "save or discard editor changes before running this Vault Git operation".into(),
        );
    }
    if request.operation == VaultGitOperation::Pull && inspect_git_status(&root).await?.dirty {
        return Err(
            "Vault Git pull is disabled while the repository has uncommitted changes".into(),
        );
    }
    let request_id = request.request_id.clone();
    let control = manager.register(&request_id)?;
    let result = execute_git_operation(&root, &request, control).await;
    manager.finish(&request_id);
    let result = result?;
    tracing::info!(
        git.operation = ?result.operation,
        git.success = result.success,
        "Vault Git operation finished"
    );
    Ok(result)
}

#[tauri::command]
pub fn cancel_vault_git(manager: tauri::State<'_, VaultGitManager>, request_id: String) -> bool {
    manager.cancel(&request_id)
}

pub fn configure_from_settings(
    app: &tauri::AppHandle,
    repository: &VaultRepository,
    settings: &crate::contracts::settings::AppSettings,
) {
    let Some(root) = settings.vault.root_directory.as_ref() else {
        return;
    };
    if let Err(error) = repository.configure(app, PathBuf::from(root)) {
        tracing::warn!(error.message = %crate::contracts::error::redact_for_log(&error), "saved JSON Vault is unavailable");
    }
}

async fn execute_git_operation(
    root: &PathBuf,
    request: &VaultGitRequest,
    control: Arc<GitControl>,
) -> Result<VaultGitResult, String> {
    let operation = request.operation;
    let output = match operation {
        VaultGitOperation::Init => run_git(root, &["init"], control).await?,
        VaultGitOperation::Pull => run_git(root, &["pull", "--ff-only"], control).await?,
        VaultGitOperation::Push => run_git(root, &["push"], control).await?,
        VaultGitOperation::Commit => {
            let added = run_git(root, &["add", "--all"], control.clone()).await?;
            if !added.status.success() {
                return Ok(git_result(operation, added));
            }
            let message = request
                .message
                .as_deref()
                .unwrap_or("Update JSON Vault from MooTool Next Tauri");
            run_git(root, &["commit", "--message", message], control).await?
        }
    };
    Ok(git_result(operation, output))
}

async fn inspect_git_status(root: &PathBuf) -> Result<VaultGitStatus, String> {
    let output = run_git_unmanaged(root, &["status", "--porcelain=v1", "--branch"]).await;
    let Ok(output) = output else {
        return Ok(git_availability().await);
    };
    if !output.status.success() {
        return Ok(VaultGitStatus {
            available: true,
            ..VaultGitStatus::default()
        });
    }
    let stdout = String::from_utf8_lossy(&output.stdout);
    let mut lines = stdout.lines();
    let header = lines.next().unwrap_or_default();
    let changed_files = lines.count();
    let branch = parse_branch(header);
    let remote = run_git_unmanaged(root, &["remote", "get-url", "origin"])
        .await
        .ok()
        .filter(|output| output.status.success())
        .map(|output| bounded_output(output.stdout).trim().to_string())
        .unwrap_or_default();
    Ok(VaultGitStatus {
        available: true,
        repository: true,
        branch,
        dirty: changed_files > 0,
        changed_files,
        ahead: parse_counter(header, "ahead "),
        behind: parse_counter(header, "behind "),
        remote,
    })
}

async fn git_availability() -> VaultGitStatus {
    let available = timeout(
        Duration::from_secs(5),
        Command::new("git")
            .arg("--version")
            .stdin(Stdio::null())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .kill_on_drop(true)
            .status(),
    )
    .await
    .ok()
    .and_then(Result::ok)
    .is_some_and(|status| status.success());
    VaultGitStatus {
        available,
        ..VaultGitStatus::default()
    }
}

async fn run_git(
    root: &PathBuf,
    arguments: &[&str],
    control: Arc<GitControl>,
) -> Result<std::process::Output, String> {
    if control.cancelled.load(Ordering::SeqCst) {
        return Err("Vault Git operation cancelled".into());
    }
    let future = git_command(root, arguments).output();
    tokio::pin!(future);
    tokio::select! {
        result = &mut future => result.map_err(|error| format!("failed to run system Git: {error}")),
        _ = control.notify.notified() => Err("Vault Git operation cancelled".into()),
        _ = tokio::time::sleep(GIT_TIMEOUT) => Err("Vault Git operation timed out".into()),
    }
}

async fn run_git_unmanaged(
    root: &PathBuf,
    arguments: &[&str],
) -> Result<std::process::Output, String> {
    timeout(
        Duration::from_secs(10),
        git_command(root, arguments).output(),
    )
    .await
    .map_err(|_| "Vault Git status timed out".to_string())?
    .map_err(|error| format!("failed to run system Git: {error}"))
}

fn git_command(root: &PathBuf, arguments: &[&str]) -> Command {
    let mut command = Command::new("git");
    command
        .args(arguments)
        .current_dir(root)
        .env("GIT_TERMINAL_PROMPT", "0")
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true);
    command
}

fn git_result(operation: VaultGitOperation, output: std::process::Output) -> VaultGitResult {
    VaultGitResult {
        operation,
        success: output.status.success(),
        exit_code: output.status.code(),
        stdout: bounded_output(output.stdout),
        stderr: bounded_output(output.stderr),
    }
}

fn bounded_output(bytes: Vec<u8>) -> String {
    let mut value =
        String::from_utf8_lossy(&bytes[..bytes.len().min(MAX_GIT_OUTPUT_BYTES)]).into_owned();
    if bytes.len() > MAX_GIT_OUTPUT_BYTES {
        value.push_str("\n… output truncated …");
    }
    value
}

fn validate_git_request(request: &VaultGitRequest) -> Result<(), String> {
    if request.message.as_ref().is_some_and(|message| {
        message.trim().is_empty() || message.len() > 160 || message.contains(['\n', '\r'])
    }) {
        return Err("Vault Git commit message must contain 1 to 160 characters".into());
    }
    if request.operation != VaultGitOperation::Commit && request.message.is_some() {
        return Err("a commit message is only valid for the Vault Git commit operation".into());
    }
    Ok(())
}

fn parse_branch(header: &str) -> String {
    let value = header.strip_prefix("## ").unwrap_or(header);
    if let Some(branch) = value.strip_prefix("No commits yet on ") {
        return branch.trim().to_string();
    }
    value
        .split("...")
        .next()
        .unwrap_or_default()
        .split_whitespace()
        .next()
        .unwrap_or_default()
        .trim()
        .to_string()
}

fn parse_counter(header: &str, marker: &str) -> u32 {
    header
        .find(marker)
        .and_then(|start| {
            header[start + marker.len()..]
                .split(|character: char| !character.is_ascii_digit())
                .next()
        })
        .and_then(|value| value.parse().ok())
        .unwrap_or_default()
}

fn validate_git_relative_path(value: &str) -> Result<&str, String> {
    if value.is_empty()
        || value.len() > 2048
        || value.starts_with('/')
        || value.contains('\\')
        || value.contains('\0')
        || value
            .split('/')
            .any(|part| part.is_empty() || part == "." || part == "..")
    {
        return Err("invalid Vault Git relative path".into());
    }
    Ok(value)
}

fn validate_git_remote(value: &str) -> Result<String, String> {
    let remote = value.trim();
    if remote.is_empty() {
        return Ok(String::new());
    }
    if remote.len() > 2048
        || remote.contains(['\r', '\n', '\0'])
        || !(remote.starts_with("https://")
            || remote.starts_with("http://")
            || remote.starts_with("ssh://")
            || remote.starts_with("git://")
            || remote.starts_with("git@")
            || remote.starts_with("file://"))
    {
        return Err("invalid Vault Git remote URL".into());
    }
    Ok(remote.to_string())
}

fn parse_git_commits(output: &str) -> Vec<VaultGitCommit> {
    output
        .split('\x1e')
        .filter_map(|record| {
            let fields = record.trim().split('\x1f').collect::<Vec<_>>();
            (fields.len() == 4).then(|| VaultGitCommit {
                hash: fields[0].to_string(),
                author: fields[1].to_string(),
                timestamp: fields[2].parse().unwrap_or_default(),
                subject: fields[3].to_string(),
            })
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_git_status_headers_and_restricts_requests() {
        let header = "## main...origin/main [ahead 2, behind 3]";
        assert_eq!(parse_branch(header), "main");
        assert_eq!(parse_counter(header, "ahead "), 2);
        assert_eq!(parse_counter(header, "behind "), 3);
        assert_eq!(
            parse_branch("## release/1.2...origin/release/1.2"),
            "release/1.2"
        );
        assert!(
            validate_git_request(&VaultGitRequest {
                request_id: "request-1".into(),
                operation: VaultGitOperation::Pull,
                message: Some("not allowed".into()),
                editor_dirty: false,
            })
            .is_err()
        );
    }

    #[test]
    fn validates_git_remotes_paths_and_commit_records() {
        assert_eq!(
            validate_git_remote(" https://example.com/mootool.git ").expect("remote"),
            "https://example.com/mootool.git"
        );
        assert!(validate_git_remote("--upload-pack=helper").is_err());
        assert!(validate_git_remote("https://example.com/repo\nmalicious").is_err());
        assert!(validate_git_relative_path("folder/example.json").is_ok());
        assert!(validate_git_relative_path("../example.json").is_err());

        let commits = parse_git_commits("abc123\x1fMoo\x1f42\x1fInitial commit\x1e");
        assert_eq!(commits.len(), 1);
        assert_eq!(commits[0].hash, "abc123");
        assert_eq!(commits[0].timestamp, 42);
        assert_eq!(commits[0].subject, "Initial commit");
    }

    #[tokio::test]
    async fn runs_git_only_inside_the_selected_vault_root() {
        let directory = tempfile::TempDir::new().expect("Vault directory");
        let root = directory.path().to_path_buf();
        let initialized = run_git_unmanaged(&root, &["init"]).await.expect("git init");
        assert!(initialized.status.success());
        std::fs::write(root.join("example.json"), "{}\n").expect("Vault JSON");
        let status = inspect_git_status(&root).await.expect("Git status");
        assert!(status.available);
        assert!(status.repository);
        assert!(status.dirty);
        assert_eq!(status.changed_files, 1);
    }
}
