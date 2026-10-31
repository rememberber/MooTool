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
            VaultDocument, VaultGitChange, VaultGitCommit, VaultGitDetails, VaultGitDiffFile, VaultGitDiffInput,
            VaultGitDiffResult, VaultGitOperation, VaultGitRequest,
            VaultGitResult, VaultGitStatus, VaultSaveRequest, VaultSnapshot, VaultTrashResult,
        },
    },
    repositories::{settings::SettingsRepository, vault::VaultRepository},
};

const GIT_TIMEOUT: Duration = Duration::from_secs(120);
const MAX_GIT_OUTPUT_BYTES: usize = 1024 * 1024;
const DEFAULT_GITIGNORE: &str = ".DS_Store\n.idea/\n.vscode/\n*.tmp\n.migrated-from-db\n";
const DEFAULT_COMMIT_MESSAGE: &str = "MooTool JSON checkpoint";

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
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<VaultSnapshot> {
    vault_snapshot(
        &repository,
        settings.snapshot().vault.hide_gitignored_files,
    )
    .await
}

async fn vault_snapshot(
    repository: &VaultRepository,
    hide_ignored: bool,
) -> AppResult<VaultSnapshot> {
    let Some(root) = repository.configured_root()? else {
        return Ok(VaultSnapshot {
            root_path: None,
            files: Vec::new(),
            directories: Vec::new(),
            git: git_availability().await,
        });
    };
    let patterns = if hide_ignored {
        super::quick_note_files::gitignore_patterns(&root)
    } else {
        Vec::new()
    };
    Ok(VaultSnapshot {
        root_path: Some(root.to_string_lossy().into_owned()),
        files: repository
            .list_files()?
            .into_iter()
            .filter(|file| visible_vault_path(&file.relative_path, false, &patterns))
            .collect(),
        directories: repository
            .list_directories()?
            .into_iter()
            .filter(|path| visible_vault_path(path, true, &patterns))
            .collect(),
        git: inspect_git_status(&root).await?,
    })
}

fn visible_vault_path(relative: &str, directory: bool, patterns: &[String]) -> bool {
    if relative.split('/').any(|part| part.starts_with('.')) {
        return false;
    }
    !super::quick_note_files::path_ignored(relative, directory, patterns)
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
    vault_snapshot(&repository, saved.vault.hide_gitignored_files).await
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
pub fn export_vault_files(
    repository: tauri::State<'_, VaultRepository>,
    directory: String,
    relative_paths: Vec<String>,
) -> AppResult<usize> {
    let directory = std::path::PathBuf::from(directory.trim());
    if !directory.is_dir() {
        return Err("Vault export directory does not exist".into());
    }
    if relative_paths.is_empty() || relative_paths.len() > 500 {
        return Err("Invalid vault export".into());
    }
    let mut written = 0usize;
    let mut seen = std::collections::HashSet::new();
    for relative_path in relative_paths {
        if !seen.insert(relative_path.clone()) {
            continue;
        }
        let document = repository.read_document(&relative_path)?;
        write_exported_vault_file(&directory, &document.relative_path, &document.content)?;
        written += 1;
    }
    Ok(written)
}

#[tauri::command]
pub fn reveal_vault_root(repository: tauri::State<'_, VaultRepository>) -> AppResult<()> {
    let root = repository.root()?;
    open_directory(&root)
}

#[tauri::command]
pub fn reveal_vault_entry(
    repository: tauri::State<'_, VaultRepository>,
    relative_path: String,
) -> AppResult<()> {
    reveal_path(&repository.resolve_entry(&relative_path)?)
}

fn reveal_path(path: &std::path::Path) -> AppResult<()> {
    let target = path.to_string_lossy().to_string();
    let mut command = if cfg!(target_os = "macos") {
        let mut command = std::process::Command::new("open");
        command.args(["-R", &target]);
        command
    } else if cfg!(target_os = "windows") {
        let mut command = std::process::Command::new("explorer");
        command.arg(format!("/select,{target}"));
        command
    } else {
        let mut command = std::process::Command::new("xdg-open");
        command.arg(path.parent().unwrap_or(path));
        command
    };
    let status = command
        .status()
        .map_err(|error| format!("failed to reveal Vault entry: {error}"))?;
    if status.success() || cfg!(target_os = "windows") {
        Ok(())
    } else {
        Err(format!("failed to reveal Vault entry: {status}").into())
    }
}

fn open_directory(path: &std::path::Path) -> AppResult<()> {
    let target = path.to_string_lossy().to_string();
    let mut command = if cfg!(target_os = "macos") {
        let mut command = std::process::Command::new("open");
        command.arg(&target);
        command
    } else if cfg!(target_os = "windows") {
        let mut command = std::process::Command::new("explorer");
        command.arg(&target);
        command
    } else {
        let mut command = std::process::Command::new("xdg-open");
        command.arg(&target);
        command
    };
    let status = command
        .status()
        .map_err(|error| format!("failed to open Vault directory: {error}"))?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("failed to open Vault directory: {status}").into())
    }
}

fn write_exported_vault_file(directory: &std::path::Path, relative_path: &str, content: &str) -> Result<(), String> {
    let base = export_base_name(relative_path);
    for suffix in 0..10_000 {
        let name = if suffix == 0 {
            format!("{base}.json")
        } else {
            format!("{base}-{suffix}.json")
        };
        let target = directory.join(&name);
        match std::fs::OpenOptions::new().write(true).create_new(true).open(&target) {
            Ok(mut file) => {
                std::io::Write::write_all(&mut file, content.as_bytes())
                    .map_err(|error| format!("failed to export {}: {error}", target.display()))?;
                return Ok(());
            }
            Err(error) if error.kind() == std::io::ErrorKind::AlreadyExists => continue,
            Err(error) => return Err(format!("failed to export {}: {error}", target.display())),
        }
    }
    Err("too many export name collisions".into())
}

fn export_base_name(relative_path: &str) -> String {
    let path = relative_path.replace('\\', "/");
    let name = path.rsplit('/').next().unwrap_or(&path);
    let stem = strip_export_extension(name);
    let sanitized: String = stem
        .chars()
        .map(|character| {
            if matches!(character, '<' | '>' | ':' | '"' | '/' | '\\' | '|' | '?' | '*')
                || character.is_ascii_control()
            {
                '_'
            } else {
                character
            }
        })
        .collect();
    let sanitized = sanitized.trim();
    if sanitized.is_empty() {
        "untitled".to_string()
    } else {
        sanitized.to_string()
    }
}

fn strip_export_extension(name: &str) -> &str {
    let lower = name.to_ascii_lowercase();
    if let Some(stem) = lower.strip_suffix(".json") {
        &name[..stem.len()]
    } else if let Some(stem) = lower.strip_suffix(".txt") {
        &name[..stem.len()]
    } else {
        name
    }
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
    if !inspect_git_status(&root).await?.repository {
        return Ok(VaultGitDetails {
            diff: String::new(),
            commits: Vec::new(),
        });
    }
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
pub async fn get_vault_git_diff(
    repository: tauri::State<'_, VaultRepository>,
    input: VaultGitDiffInput,
) -> AppResult<VaultGitDiffResult> {
    Ok(diff_vault_files(&repository.root()?, &input).await?)
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
    settings: tauri::State<'_, SettingsRepository>,
    request: VaultGitRequest,
) -> AppResult<VaultGitResult> {
    validate_git_request(&request)?;
    let root = repository.root()?;
    if request.editor_dirty && request.operation == VaultGitOperation::Commit {
        return Err(
            "save or discard editor changes before running this Vault Git operation".into(),
        );
    }
    let username = settings.snapshot().vault.git_username;
    let request_id = request.request_id.clone();
    let control = manager.register(&request_id)?;
    let result = execute_git_operation(&root, &request, control, &username).await;
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
    username: &str,
) -> Result<VaultGitResult, String> {
    match request.operation {
        VaultGitOperation::Init => initialize_repository(root, control, username).await,
        VaultGitOperation::Fetch => {
            let output = run_git(root, &["fetch", "--prune", "origin"], control).await?;
            Ok(git_result(VaultGitOperation::Fetch, output))
        }
        VaultGitOperation::Pull => pull_repository(root, control).await,
        VaultGitOperation::Push => {
            let output = run_git(root, &["push", "-u", "origin", "HEAD"], control).await?;
            Ok(git_result(VaultGitOperation::Push, output))
        }
        VaultGitOperation::Commit => {
            commit_changes(
                root,
                request.message.as_deref().unwrap_or(DEFAULT_COMMIT_MESSAGE),
                username,
                control,
            )
            .await
        }
        VaultGitOperation::Discard => discard_change(root, request.path.as_deref(), control).await,
        VaultGitOperation::AbortMerge => abort_merge(root, control).await,
        VaultGitOperation::ResolveConflict => {
            resolve_conflict(
                root,
                request.path.as_deref(),
                request.strategy.as_deref(),
                control,
            )
            .await
        }
        VaultGitOperation::ContinueOperation => {
            continue_operation(root, username, control).await
        }
    }
}

async fn initialize_repository(
    root: &PathBuf,
    control: Arc<GitControl>,
    username: &str,
) -> Result<VaultGitResult, String> {
    if inspect_git_status(root).await?.repository {
        return Ok(message_result(
            VaultGitOperation::Init,
            true,
            "Git repository is already initialized",
        ));
    }
    let initialized = run_git(root, &["init"], control.clone()).await?;
    if !initialized.status.success() {
        return Ok(git_result(VaultGitOperation::Init, initialized));
    }
    let ignore = root.join(".gitignore");
    if !ignore.exists() {
        std::fs::write(&ignore, DEFAULT_GITIGNORE)
            .map_err(|error| format!("failed to write gitignore: {error}"))?;
    }
    if !inspect_git_status(root).await?.dirty {
        return Ok(git_result(VaultGitOperation::Init, initialized));
    }
    let mut committed = commit_changes(root, "Initial MooTool Vault setup", username, control).await?;
    committed.operation = VaultGitOperation::Init;
    Ok(committed)
}

async fn pull_repository(root: &PathBuf, control: Arc<GitControl>) -> Result<VaultGitResult, String> {
    if inspect_git_status(root).await?.merging {
        return Ok(message_result(
            VaultGitOperation::Pull,
            false,
            "Finish or abort the current merge/rebase before pulling",
        ));
    }
    let output = run_git(root, &["pull", "--no-rebase", "origin"], control).await?;
    Ok(git_result(VaultGitOperation::Pull, output))
}

async fn commit_changes(
    root: &PathBuf,
    message: &str,
    username: &str,
    control: Arc<GitControl>,
) -> Result<VaultGitResult, String> {
    let message: String = message.trim().chars().take(300).collect();
    if message.is_empty() {
        return Err("Commit message is required".into());
    }
    let status = inspect_git_status(root).await?;
    if !status.repository {
        return Err("Git repository is not initialized".into());
    }
    if status.merging || status.conflicts > 0 {
        return Ok(message_result(
            VaultGitOperation::Commit,
            false,
            "Finish or abort the current merge/rebase before creating a checkpoint",
        ));
    }
    if !status.dirty {
        return Ok(message_result(
            VaultGitOperation::Commit,
            true,
            "No changes to commit",
        ));
    }
    ensure_identity(root, username).await?;
    let added = run_git(root, &["add", "--all"], control.clone()).await?;
    if !added.status.success() {
        return Ok(git_result(VaultGitOperation::Commit, added));
    }
    let committed = run_git(
        root,
        &["commit", "--message", &message],
        control,
    )
    .await?;
    Ok(git_result(VaultGitOperation::Commit, committed))
}

async fn ensure_identity(root: &PathBuf, username: &str) -> Result<(), String> {
    let name = if username.trim().is_empty() {
        "MooTool".to_string()
    } else {
        username.trim().to_string()
    };
    let current_name = run_git_unmanaged(root, &["config", "--get", "user.name"]).await?;
    if !current_name.status.success() {
        let output = run_git_unmanaged(root, &["config", "user.name", &name]).await?;
        if !output.status.success() {
            return Err(bounded_output(output.stderr));
        }
    }
    let current_email = run_git_unmanaged(root, &["config", "--get", "user.email"]).await?;
    if !current_email.status.success() {
        let output = run_git_unmanaged(root, &["config", "user.email", "mootool@local"]).await?;
        if !output.status.success() {
            return Err(bounded_output(output.stderr));
        }
    }
    Ok(())
}

async fn inspect_git_status(root: &PathBuf) -> Result<VaultGitStatus, String> {
    let output = run_git_unmanaged(
        root,
        &["status", "--porcelain=v1", "-z", "--branch", "--untracked-files=all"],
    )
    .await;
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
    let (header, changes) = parse_porcelain_z(&stdout);
    let changed_files = changes.len();
    let conflicts = changes.iter().filter(|change| change.conflict).count() as u32;
    let branch = parse_branch(&header);
    let remote = run_git_unmanaged(root, &["remote", "get-url", "origin"])
        .await
        .ok()
        .filter(|output| output.status.success())
        .map(|output| bounded_output(output.stdout).trim().to_string())
        .unwrap_or_default();
    let merging = run_git_unmanaged(root, &["rev-parse", "-q", "--verify", "MERGE_HEAD"])
        .await
        .is_ok_and(|output| output.status.success());
    let rebase = rebase_in_progress(root).await;
    Ok(VaultGitStatus {
        available: true,
        repository: true,
        branch,
        dirty: changed_files > 0,
        changed_files,
        ahead: parse_counter(&header, "ahead "),
        behind: parse_counter(&header, "behind "),
        remote,
        conflicts,
        merging: merging || rebase,
        operation: if rebase {
            "rebase"
        } else if merging {
            "merge"
        } else {
            "none"
        }
        .into(),
        changes,
    })
}

async fn rebase_in_progress(root: &PathBuf) -> bool {
    for name in ["rebase-merge", "rebase-apply"] {
        let Ok(output) = run_git_unmanaged(root, &["rev-parse", "--git-path", name]).await else {
            continue;
        };
        if output.status.success() && root.join(bounded_output(output.stdout).trim()).exists() {
            return true;
        }
    }
    false
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

fn message_result(operation: VaultGitOperation, success: bool, message: &str) -> VaultGitResult {
    VaultGitResult {
        operation,
        success,
        exit_code: Some(if success { 0 } else { 1 }),
        stdout: if success { message.to_string() } else { String::new() },
        stderr: if success { String::new() } else { message.to_string() },
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
        let trimmed = message.trim();
        trimmed.is_empty() || trimmed.contains('\0')
    }) {
        return Err("Commit message is required".into());
    }
    if request.operation != VaultGitOperation::Commit && request.message.is_some() {
        return Err("a commit message is only valid for the Vault Git commit operation".into());
    }
    let path_operation = matches!(
        request.operation,
        VaultGitOperation::Discard | VaultGitOperation::ResolveConflict
    );
    if request.path.is_some() && !path_operation {
        return Err("a Git path is only valid when discarding or resolving a conflict".into());
    }
    if request.strategy.is_some() && request.operation != VaultGitOperation::ResolveConflict {
        return Err("a conflict strategy is only valid when resolving a conflict".into());
    }
    if request.operation == VaultGitOperation::ResolveConflict
        && !matches!(request.strategy.as_deref(), Some("ours" | "theirs"))
    {
        return Err("Invalid conflict strategy".into());
    }
    if path_operation {
        validate_git_relative_path(request.path.as_deref().unwrap_or(""))?;
    }
    Ok(())
}

const DIFF_PREVIEW_LIMIT: u64 = 512 * 1024;

struct DiffPreview {
    text: String,
    state: &'static str,
}

impl DiffPreview {
    fn missing() -> Self {
        Self { text: String::new(), state: "text" }
    }
    fn binary() -> Self {
        Self { text: String::new(), state: "binary" }
    }
    fn too_large() -> Self {
        Self { text: String::new(), state: "too-large" }
    }
}

async fn diff_vault_files(root: &PathBuf, input: &VaultGitDiffInput) -> Result<VaultGitDiffResult, String> {
    if let Some(commit) = input.commit.as_deref().map(str::trim).filter(|value| !value.is_empty()) {
        validate_vault_commit(commit)?;
        return commit_diff(root, commit).await;
    }
    let status = inspect_git_status(root).await?;
    if !status.repository {
        return Err("Git repository is not initialized in the Vault root".into());
    }
    let selected = input.path.as_deref().map(str::trim).filter(|value| !value.is_empty());
    if let Some(path) = selected {
        validate_git_relative_path(path)?;
    }
    let mut files = Vec::new();
    for change in status.changes {
        if selected.is_some_and(|path| path != change.path) {
            continue;
        }
        let before_path = change.original_path.clone().unwrap_or_else(|| change.path.clone());
        let before = if change.status.contains('?') {
            DiffPreview::missing()
        } else {
            read_git_blob(root, "HEAD", &before_path).await
        };
        let after = if change.status.contains('D') {
            DiffPreview::missing()
        } else {
            read_worktree_preview(root, &change.path)
        };
        files.push(build_vault_diff_file(change, before, after));
    }
    Ok(VaultGitDiffResult { files })
}

async fn commit_diff(root: &PathBuf, commit: &str) -> Result<VaultGitDiffResult, String> {
    let parent = run_git_unmanaged(root, &["rev-parse", "--verify", &format!("{commit}^")]).await?;
    let parent_ref = parent.status.success().then(|| bounded_output(parent.stdout).trim().to_string());
    let listed = if let Some(parent_ref) = &parent_ref {
        run_git_unmanaged(root, &["diff", "--name-status", "-z", "-M", "-C", parent_ref, commit]).await?
    } else {
        run_git_unmanaged(root, &["diff-tree", "--root", "--no-commit-id", "--name-status", "-z", "-r", "-M", "-C", commit]).await?
    };
    if !listed.status.success() {
        return Err(bounded_output(if listed.stderr.is_empty() { listed.stdout } else { listed.stderr }));
    }
    let mut files = Vec::new();
    for change in parse_name_status(&bounded_output(listed.stdout)) {
        let before = if let Some(parent_ref) = &parent_ref {
            read_git_blob(root, parent_ref, change.original_path.as_deref().unwrap_or(&change.path)).await
        } else {
            DiffPreview::missing()
        };
        let after = read_git_blob(root, commit, &change.path).await;
        files.push(build_vault_diff_file(change, before, after));
    }
    Ok(VaultGitDiffResult { files })
}

async fn read_git_blob(root: &PathBuf, revision: &str, path: &str) -> DiffPreview {
    let spec = format!("{revision}:{path}");
    let Ok(size_output) = run_git_unmanaged(root, &["cat-file", "-s", &spec]).await else {
        return DiffPreview::missing();
    };
    if !size_output.status.success() {
        return DiffPreview::missing();
    }
    let size: u64 = bounded_output(size_output.stdout).trim().parse().unwrap_or(0);
    if size > DIFF_PREVIEW_LIMIT {
        return DiffPreview::too_large();
    }
    let Ok(content) = run_git_unmanaged(root, &["cat-file", "blob", &spec]).await else {
        return DiffPreview::missing();
    };
    if !content.status.success() {
        return DiffPreview::missing();
    }
    preview_git_bytes(&content.stdout)
}

fn read_worktree_preview(root: &PathBuf, path: &str) -> DiffPreview {
    let target = root.join(path);
    let Ok(metadata) = std::fs::metadata(&target) else {
        return DiffPreview::missing();
    };
    if !metadata.is_file() || metadata.len() > DIFF_PREVIEW_LIMIT {
        return if metadata.is_file() { DiffPreview::too_large() } else { DiffPreview::binary() };
    }
    std::fs::read(&target).map(|bytes| preview_git_bytes(&bytes)).unwrap_or_else(|_| DiffPreview::missing())
}

fn preview_git_bytes(bytes: &[u8]) -> DiffPreview {
    if bytes.contains(&0) {
        DiffPreview::binary()
    } else {
        DiffPreview { text: String::from_utf8_lossy(bytes).into_owned(), state: "text" }
    }
}

fn build_vault_diff_file(change: VaultGitChange, before: DiffPreview, after: DiffPreview) -> VaultGitDiffFile {
    let preview = if before.state == "binary" || after.state == "binary" {
        "binary"
    } else if before.state == "too-large" || after.state == "too-large" {
        "too-large"
    } else {
        "text"
    };
    VaultGitDiffFile {
        before: if preview == "text" { before.text } else { String::new() },
        after: if preview == "text" { after.text } else { String::new() },
        preview: preview.into(),
        path: change.path,
        original_path: change.original_path,
        status: change.status,
    }
}

fn parse_name_status(output: &str) -> Vec<VaultGitChange> {
    let records: Vec<&str> = output.split('\0').collect();
    let mut changes = Vec::new();
    let mut index = 0;
    while index < records.len() {
        let record = records[index];
        index += 1;
        if record.is_empty() {
            continue;
        }
        let (status, inline_path) = record.split_once('\t').unwrap_or((record, ""));
        let renamed = status.starts_with('R') || status.starts_with('C');
        let original_path = if renamed {
            let value = if inline_path.is_empty() { take_record(&records, &mut index) } else { inline_path };
            Some(value.to_string()).filter(|value| !value.is_empty())
        } else {
            None
        };
        let path = if renamed || inline_path.is_empty() { take_record(&records, &mut index) } else { inline_path };
        if path.is_empty() {
            continue;
        }
        changes.push(VaultGitChange {
            path: path.to_string(),
            original_path,
            status: status.to_string(),
            conflict: false,
        });
    }
    changes
}

fn parse_porcelain_z(output: &str) -> (String, Vec<VaultGitChange>) {
    let mut records = output.split('\0');
    let branch_line = records.next().unwrap_or_default().to_string();
    let records: Vec<&str> = records.collect();
    let mut changes = Vec::new();
    let mut index = 0;
    while index < records.len() {
        let record = records[index];
        index += 1;
        if record.len() < 4 {
            continue;
        }
        let status = record[..2].to_string();
        let path = record[3..].to_string();
        let renamed = status.contains('R') || status.contains('C');
        let original_path = if renamed {
            let value = take_record(&records, &mut index);
            Some(value.to_string()).filter(|value| !value.is_empty())
        } else {
            None
        };
        changes.push(VaultGitChange {
            path,
            original_path,
            status: status.clone(),
            conflict: porcelain_conflict(&status),
        });
    }
    (branch_line, changes)
}

fn take_record<'a>(records: &[&'a str], index: &mut usize) -> &'a str {
    let value = records.get(*index).copied().unwrap_or("");
    if *index < records.len() {
        *index += 1;
    }
    value
}

fn validate_vault_commit(value: &str) -> Result<(), String> {
    if (7..=40).contains(&value.len()) && value.chars().all(|character| character.is_ascii_hexdigit()) {
        Ok(())
    } else {
        Err("Invalid Git commit".into())
    }
}

async fn discard_change(
    root: &PathBuf,
    path: Option<&str>,
    control: Arc<GitControl>,
) -> Result<VaultGitResult, String> {
    let path = validate_git_relative_path(path.unwrap_or(""))?.to_string();
    let status = inspect_git_status(root).await?;
    let Some(change) = status.changes.iter().find(|change| change.path == path) else {
        return Ok(message_result(
            VaultGitOperation::Discard,
            true,
            "No changes to discard",
        ));
    };
    if change.status == "??" {
        let output = run_git(root, &["clean", "-f", "--", &path], control).await?;
        return Ok(git_result(VaultGitOperation::Discard, output));
    }
    let restored = run_git(
        root,
        &["restore", "--staged", "--worktree", "--source=HEAD", "--", &path],
        control.clone(),
    )
    .await?;
    if restored.status.success() {
        return Ok(git_result(VaultGitOperation::Discard, restored));
    }
    let output = run_git(root, &["checkout", "HEAD", "--", &path], control).await?;
    Ok(git_result(VaultGitOperation::Discard, output))
}

async fn abort_merge(root: &PathBuf, control: Arc<GitControl>) -> Result<VaultGitResult, String> {
    let merge = run_git(root, &["merge", "--abort"], control.clone()).await?;
    if merge.status.success() {
        return Ok(git_result(VaultGitOperation::AbortMerge, merge));
    }
    let rebase = run_git(root, &["rebase", "--abort"], control).await?;
    Ok(git_result(VaultGitOperation::AbortMerge, rebase))
}

async fn resolve_conflict(
    root: &PathBuf,
    path: Option<&str>,
    strategy: Option<&str>,
    control: Arc<GitControl>,
) -> Result<VaultGitResult, String> {
    let path = validate_git_relative_path(path.unwrap_or(""))?.to_string();
    if !matches!(strategy, Some("ours" | "theirs")) {
        return Err("Invalid conflict strategy".into());
    }
    let strategy = strategy.unwrap_or_default();
    let status = inspect_git_status(root).await?;
    if !status
        .changes
        .iter()
        .any(|change| change.path == path && change.conflict)
    {
        return Err("Git path is not conflicted".into());
    }
    let flag = format!("--{strategy}");
    let checkout = run_git(root, &["checkout", &flag, "--", &path], control.clone()).await?;
    if !checkout.status.success() {
        return Ok(git_result(VaultGitOperation::ResolveConflict, checkout));
    }
    let added = run_git(root, &["add", "--", &path], control).await?;
    Ok(git_result(VaultGitOperation::ResolveConflict, added))
}

async fn continue_operation(
    root: &PathBuf,
    username: &str,
    control: Arc<GitControl>,
) -> Result<VaultGitResult, String> {
    let status = inspect_git_status(root).await?;
    if status.operation == "none" {
        return Ok(message_result(
            VaultGitOperation::ContinueOperation,
            false,
            "No merge or rebase is in progress",
        ));
    }
    if status.conflicts > 0 {
        return Ok(message_result(
            VaultGitOperation::ContinueOperation,
            false,
            "Resolve all conflicts before continuing",
        ));
    }
    ensure_identity(root, username).await?;
    let output = if status.operation == "rebase" {
        run_git(
            root,
            &[
                "-c",
                "core.editor=true",
                "-c",
                "commit.gpgsign=false",
                "rebase",
                "--continue",
            ],
            control,
        )
        .await?
    } else {
        run_git(
            root,
            &["-c", "commit.gpgsign=false", "commit", "--no-edit"],
            control,
        )
        .await?
    };
    Ok(git_result(VaultGitOperation::ContinueOperation, output))
}

fn porcelain_conflict(status: &str) -> bool {
    status.contains('U') || status == "AA" || status == "DD"
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
    fn reads_nul_separated_git_paths_including_renames_and_newlines() {
        let (branch, changes) = parse_porcelain_z("## main...origin/main [ahead 2, behind 1]\0 M Work/API ideas.json\0?? odd\nname.json\0UU Work/conflict.json\0R  copied.json\0copy me.json\0");
        assert_eq!(parse_branch(&branch), "main");
        assert_eq!(changes.len(), 4);
        assert_eq!(changes[1].path, "odd\nname.json");
        assert!(changes[2].conflict);
        assert_eq!(changes[3].path, "copied.json");
        assert_eq!(changes[3].original_path.as_deref(), Some("copy me.json"));
        let named = parse_name_status("M\0Work/API ideas.json\0R100\0copy me.json\0copied.json\0A\0odd\nname.json\0");
        assert_eq!(named[0].path, "Work/API ideas.json");
        assert_eq!(named[1].original_path.as_deref(), Some("copy me.json"));
        assert_eq!(named[1].path, "copied.json");
        assert_eq!(named[2].path, "odd\nname.json");
    }

    #[test]
    fn hides_dot_entries_and_gitignore_matches_from_the_vault_list() {
        assert!(!visible_vault_path(".idea", true, &[]));
        assert!(!visible_vault_path(".idea/workspace.json", false, &[]));
        let patterns = vec!["ignored.json".into(), "secret/".into(), "*.tmp".into()];
        assert!(!visible_vault_path("ignored.json", false, &patterns));
        assert!(!visible_vault_path("secret", true, &patterns));
        assert!(!visible_vault_path("secret/hidden.json", false, &patterns));
        assert!(!visible_vault_path("notes/draft.tmp", false, &patterns));
        assert!(visible_vault_path("keep.json", false, &patterns));
        assert!(visible_vault_path("notes", true, &patterns));
        assert!(visible_vault_path("ignored.json", false, &[]));
    }

    #[test]
    fn parses_git_status_headers_and_restricts_requests() {
        let header = "## main...origin/main [ahead 2, behind 3]";
        assert_eq!(parse_branch(header), "main");
        assert_eq!(parse_counter(header, "ahead "), 2);
        assert_eq!(parse_counter(header, "behind "), 3);
        assert!(porcelain_conflict("UU"));
        assert!(porcelain_conflict("AA"));
        assert!(porcelain_conflict("DD"));
        assert!(porcelain_conflict("AU"));
        assert!(!porcelain_conflict("M "));
        assert!(!porcelain_conflict("??"));
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
                path: None,
                strategy: None,
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
        assert!(!status.merging);
        assert_eq!(status.conflicts, 0);
        assert_eq!(status.operation, "none");
    }

    #[tokio::test]
    async fn reports_a_merge_and_rebase_without_counting_them_as_clean() {
        let directory = tempfile::TempDir::new().expect("Vault directory");
        let root = directory.path().to_path_buf();
        assert!(run_git_unmanaged(&root, &["init"]).await.expect("git init").status.success());
        assert!(run_git_unmanaged(&root, &["config", "user.email", "mootool@localhost"]).await.expect("email").status.success());
        assert!(run_git_unmanaged(&root, &["config", "user.name", "MooTool"]).await.expect("name").status.success());
        std::fs::write(root.join("example.json"), "{}\n").expect("Vault JSON");
        assert!(run_git_unmanaged(&root, &["add", "--all"]).await.expect("add").status.success());
        assert!(run_git_unmanaged(&root, &["commit", "--message", "Initial"]).await.expect("commit").status.success());
        let head = run_git_unmanaged(&root, &["rev-parse", "HEAD"]).await.expect("head");
        std::fs::write(root.join(".git").join("MERGE_HEAD"), &head.stdout).expect("merge head");
        std::fs::write(root.join("example.json"), "{\n  \"changed\": true\n}\n").expect("conflicted edit");
        let merging = inspect_git_status(&root).await.expect("merge status");
        assert!(merging.merging);
        assert_eq!(merging.operation, "merge");
        std::fs::remove_file(root.join(".git").join("MERGE_HEAD")).expect("clear merge");
        std::fs::create_dir(root.join(".git").join("rebase-merge")).expect("rebase directory");
        let rebasing = inspect_git_status(&root).await.expect("rebase status");
        assert!(rebasing.merging);
        assert_eq!(rebasing.operation, "rebase");
    }

    #[tokio::test]
    async fn initializes_with_the_electron_gitignore_and_skips_commits_during_a_merge() {
        let directory = tempfile::TempDir::new().expect("Vault directory");
        let root = directory.path().to_path_buf();
        std::fs::write(root.join("example.json"), "{}\n").expect("Vault JSON");
        let control = Arc::new(GitControl::default());
        let initialized = initialize_repository(&root, control.clone(), "")
            .await
            .expect("initialize");
        assert!(initialized.success, "{}", initialized.stderr);
        assert_eq!(
            std::fs::read_to_string(root.join(".gitignore")).expect("gitignore"),
            DEFAULT_GITIGNORE
        );
        let log = run_git_unmanaged(&root, &["log", "-1", "--format=%s"])
            .await
            .expect("log");
        assert_eq!(
            String::from_utf8_lossy(&log.stdout).trim(),
            "Initial MooTool Vault setup"
        );
        let again = initialize_repository(&root, control.clone(), "")
            .await
            .expect("second initialize");
        assert!(again.success);
        assert_eq!(again.stdout, "Git repository is already initialized");

        let head = run_git_unmanaged(&root, &["rev-parse", "HEAD"]).await.expect("head");
        std::fs::write(root.join(".git").join("MERGE_HEAD"), &head.stdout).expect("merge head");
        std::fs::write(root.join("example.json"), "{\"changed\":true}\n").expect("edit");
        let skipped = commit_changes(&root, "Should not commit", "", control.clone())
            .await
            .expect("merge commit");
        assert!(!skipped.success);
        assert_eq!(
            skipped.stderr,
            "Finish or abort the current merge/rebase before creating a checkpoint"
        );
        let pulled = pull_repository(&root, control).await.expect("merge pull");
        assert!(!pulled.success);
        assert_eq!(
            pulled.stderr,
            "Finish or abort the current merge/rebase before pulling"
        );
        let latest = run_git_unmanaged(&root, &["log", "-1", "--format=%s"])
            .await
            .expect("latest");
        assert_eq!(
            String::from_utf8_lossy(&latest.stdout).trim(),
            "Initial MooTool Vault setup"
        );
    }

    #[tokio::test]
    async fn aborts_a_conflicted_merge_after_refusing_to_continue() {
        let directory = tempfile::TempDir::new().expect("Vault directory");
        let root = directory.path().to_path_buf();
        let control = Arc::new(GitControl::default());
        std::fs::write(root.join("example.json"), "{\"value\":1}\n").expect("base");
        let initialized = initialize_repository(&root, control.clone(), "")
            .await
            .expect("initialize");
        assert!(initialized.success, "{}", initialized.stderr);
        let branch = run_git_unmanaged(&root, &["branch", "--show-current"])
            .await
            .expect("branch");
        let branch = String::from_utf8_lossy(&branch.stdout).trim().to_string();
        assert!(
            run_git_unmanaged(&root, &["checkout", "-b", "other"])
                .await
                .expect("other branch")
                .status
                .success()
        );
        std::fs::write(root.join("example.json"), "{\"value\":2}\n").expect("other");
        let other = commit_changes(&root, "Other side", "", control.clone())
            .await
            .expect("other commit");
        assert!(other.success, "{}", other.stderr);
        assert!(
            run_git_unmanaged(&root, &["checkout", &branch])
                .await
                .expect("checkout")
                .status
                .success()
        );
        std::fs::write(root.join("example.json"), "{\"value\":3}\n").expect("ours");
        let ours = commit_changes(&root, "Our side", "", control.clone())
            .await
            .expect("our commit");
        assert!(ours.success, "{}", ours.stderr);
        let merged = run_git_unmanaged(&root, &["merge", "other"])
            .await
            .expect("merge");
        assert!(!merged.status.success(), "the two edits should conflict");
        let status = inspect_git_status(&root).await.expect("conflict status");
        assert!(status.merging);
        assert!(status.changes.iter().any(|change| change.conflict && change.path == "example.json"));
        let continued = continue_operation(&root, "", control.clone())
            .await
            .expect("continue");
        assert!(!continued.success);
        assert_eq!(continued.stderr, "Resolve all conflicts before continuing");
        let aborted = abort_merge(&root, control.clone()).await.expect("abort");
        assert!(aborted.success, "{}", aborted.stderr);
        let after = inspect_git_status(&root).await.expect("aborted status");
        assert!(!after.merging);
        assert_eq!(after.conflicts, 0);
        assert_eq!(
            std::fs::read_to_string(root.join("example.json")).expect("restored"),
            "{\"value\":3}\n"
        );
        let idle = continue_operation(&root, "", control).await.expect("idle continue");
        assert!(!idle.success);
        assert_eq!(idle.stderr, "No merge or rebase is in progress");
    }

    #[tokio::test]
    async fn reads_working_tree_and_commit_diffs_as_before_and_after_text() {
        let directory = tempfile::TempDir::new().expect("Vault directory");
        let root = directory.path().to_path_buf();
        let control = Arc::new(GitControl::default());
        std::fs::write(root.join("example.json"), "{\"value\":1}\n").expect("base");
        let initialized = initialize_repository(&root, control.clone(), "")
            .await
            .expect("initialize");
        assert!(initialized.success, "{}", initialized.stderr);
        std::fs::write(root.join("example.json"), "{\"value\":2}\n").expect("edit");
        let working = diff_vault_files(
            &root,
            &VaultGitDiffInput { path: Some("example.json".into()), commit: None },
        )
        .await
        .expect("working diff");
        assert_eq!(working.files.len(), 1);
        assert_eq!(working.files[0].preview, "text");
        assert_eq!(working.files[0].before, "{\"value\":1}\n");
        assert_eq!(working.files[0].after, "{\"value\":2}\n");
        let committed = commit_changes(&root, "Update example", "", control)
            .await
            .expect("commit");
        assert!(committed.success, "{}", committed.stderr);
        let head = run_git_unmanaged(&root, &["rev-parse", "HEAD"]).await.expect("head");
        let hash = String::from_utf8_lossy(&head.stdout).trim().to_string();
        let historical = diff_vault_files(
            &root,
            &VaultGitDiffInput { path: None, commit: Some(hash) },
        )
        .await
        .expect("commit diff");
        assert_eq!(historical.files.len(), 1);
        assert_eq!(historical.files[0].before, "{\"value\":1}\n");
        assert_eq!(historical.files[0].after, "{\"value\":2}\n");
        let exported = tempfile::TempDir::new().expect("export directory");
        write_exported_vault_file(exported.path(), "Work/API/example.json", "{\"value\":1}\n").expect("first export");
        write_exported_vault_file(exported.path(), "other/example.json", "{\"value\":2}\n").expect("second export");
        write_exported_vault_file(exported.path(), "bad/a:b?.txt", "text\n").expect("sanitized export");
        assert_eq!(std::fs::read_to_string(exported.path().join("example.json")).expect("first"), "{\"value\":1}\n");
        assert_eq!(std::fs::read_to_string(exported.path().join("example-1.json")).expect("second"), "{\"value\":2}\n");
        assert_eq!(std::fs::read_to_string(exported.path().join("a_b_.json")).expect("sanitized"), "text\n");

        let invalid = diff_vault_files(
            &root,
            &VaultGitDiffInput { path: None, commit: Some("not-a-commit".into()) },
        )
        .await;
        assert!(invalid.is_err());
    }
}
