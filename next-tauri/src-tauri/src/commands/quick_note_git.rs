use std::{fs, path::Path, process::Command};

use serde::{Deserialize, Serialize};

use crate::contracts::error::AppResult;

const DEFAULT_GITIGNORE: &str = ".DS_Store\n.idea/\n.vscode/\n*.tmp\n.migrated-from-db\n";

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitChange {
    pub path: String,
    pub original_path: Option<String>,
    pub status: String,
    pub conflict: bool,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitStatus {
    pub available: bool,
    pub repository: bool,
    pub branch: String,
    pub remote: String,
    pub ahead: u32,
    pub behind: u32,
    pub changes: Vec<QuickNoteGitChange>,
    pub conflicts: u32,
    pub merging: bool,
    pub operation: String,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitCommit {
    pub hash: String,
    pub short_hash: String,
    pub author: String,
    pub date: String,
    pub message: String,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitActionInput {
    pub action: String,
    pub message: Option<String>,
    pub remote: Option<String>,
    pub path: Option<String>,
    pub strategy: Option<String>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitActionResult {
    pub success: bool,
    pub message: String,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitDiffInput {
    pub path: Option<String>,
    pub commit: Option<String>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitDiffFile {
    pub path: String,
    pub original_path: Option<String>,
    pub status: String,
    pub before: String,
    pub after: String,
    pub preview: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct QuickNoteGitDiffResult {
    pub files: Vec<QuickNoteGitDiffFile>,
}

impl Default for QuickNoteGitStatus {
    fn default() -> Self {
        Self {
            available: false,
            repository: false,
            branch: String::new(),
            remote: String::new(),
            ahead: 0,
            behind: 0,
            changes: Vec::new(),
            conflicts: 0,
            merging: false,
            operation: "none".into(),
        }
    }
}

#[tauri::command]
pub fn quick_note_git_status(app: tauri::AppHandle) -> AppResult<QuickNoteGitStatus> {
    let vault = super::quick_note_files::open_vault(&app)?;
    Ok(git_status(vault.root())?)
}

#[tauri::command]
pub fn quick_note_git_history(app: tauri::AppHandle) -> AppResult<Vec<QuickNoteGitCommit>> {
    let vault = super::quick_note_files::open_vault(&app)?;
    let root = vault.root();
    if !git_status(root)?.repository {
        return Ok(Vec::new());
    }
    let output = run_git(root, &["log", "-n", "40", "--pretty=format:%H%x1f%h%x1f%an%x1f%aI%x1f%s"])?;
    if !output.status.success() {
        return Ok(Vec::new());
    }
    Ok(parse_history(&String::from_utf8_lossy(&output.stdout)))
}

#[tauri::command]
pub fn quick_note_git_diff(app: tauri::AppHandle, input: QuickNoteGitDiffInput) -> AppResult<QuickNoteGitDiffResult> {
    let vault = super::quick_note_files::open_vault(&app)?;
    Ok(diff_files(vault.root(), &input)?)
}

fn diff_files(root: &Path, input: &QuickNoteGitDiffInput) -> Result<QuickNoteGitDiffResult, String> {
    if let Some(commit) = input.commit.as_deref().map(str::trim).filter(|value| !value.is_empty()) {
        validate_commit(commit)?;
        let parent = run_git(root, &["rev-parse", "--verify", &format!("{commit}^")])?;
        let parent_ref = parent.status.success().then(|| String::from_utf8_lossy(&parent.stdout).trim().to_string());
        let listed = if let Some(parent_ref) = &parent_ref {
            run_git(root, &["diff", "--name-status", "-M", parent_ref, commit])?
        } else {
            run_git(root, &["diff-tree", "--root", "--no-commit-id", "--name-status", "-r", "-M", commit])?
        };
        if !listed.status.success() {
            return Err(command_text(&listed));
        }
        let changes = parse_name_status(&String::from_utf8_lossy(&listed.stdout));
        let mut files = Vec::new();
        for change in changes {
            let before = if let Some(parent_ref) = &parent_ref {
                read_blob(root, parent_ref, change.original_path.as_deref().unwrap_or(&change.path))
            } else {
                ContentPreview::missing()
            };
            let after = read_blob(root, commit, &change.path);
            files.push(build_diff_file(change, before, after));
        }
        return Ok(QuickNoteGitDiffResult { files });
    }
    if let Some(path) = input.path.as_deref().map(str::trim).filter(|value| !value.is_empty()) {
        let path = validate_git_path(path)?;
        let status = git_status(root)?;
        let mut files = Vec::new();
        for change in status.changes.into_iter().filter(|change| change.path == path) {
            let before_path = change.original_path.clone().unwrap_or_else(|| change.path.clone());
            let before = if change.status.contains('?') { ContentPreview::missing() } else { read_blob(root, "HEAD", &before_path) };
            let after = if change.status.contains('D') { ContentPreview::missing() } else { read_worktree(root, &change.path) };
            files.push(build_diff_file(change, before, after));
        }
        return Ok(QuickNoteGitDiffResult { files });
    }
    Ok(QuickNoteGitDiffResult { files: Vec::new() })
}

struct ContentPreview {
    text: String,
    state: &'static str,
}

impl ContentPreview {
    fn missing() -> Self { Self { text: String::new(), state: "text" } }
    fn binary() -> Self { Self { text: String::new(), state: "binary" } }
    fn too_large() -> Self { Self { text: String::new(), state: "too-large" } }
}

fn read_blob(root: &Path, revision: &str, path: &str) -> ContentPreview {
    let spec = format!("{revision}:{path}");
    let Ok(size_output) = run_git(root, &["cat-file", "-s", &spec]) else { return ContentPreview::missing() };
    if !size_output.status.success() { return ContentPreview::missing() }
    let size: u64 = String::from_utf8_lossy(&size_output.stdout).trim().parse().unwrap_or(0);
    if size > 512 * 1024 { return ContentPreview::too_large() }
    let Ok(content) = run_git(root, &["cat-file", "blob", &spec]) else { return ContentPreview::missing() };
    if !content.status.success() { return ContentPreview::missing() }
    preview_bytes(&content.stdout)
}

fn read_worktree(root: &Path, path: &str) -> ContentPreview {
    let target = root.join(path);
    let Ok(metadata) = fs::metadata(&target) else { return ContentPreview::missing() };
    if !metadata.is_file() { return ContentPreview::binary() }
    if metadata.len() > 512 * 1024 { return ContentPreview::too_large() }
    fs::read(&target).map(|bytes| preview_bytes(&bytes)).unwrap_or_else(|_| ContentPreview::missing())
}

fn preview_bytes(bytes: &[u8]) -> ContentPreview {
    if bytes.contains(&0) { ContentPreview::binary() } else { ContentPreview { text: String::from_utf8_lossy(bytes).into_owned(), state: "text" } }
}

fn build_diff_file(change: QuickNoteGitChange, before: ContentPreview, after: ContentPreview) -> QuickNoteGitDiffFile {
    let preview = if before.state == "binary" || after.state == "binary" {
        "binary"
    } else if before.state == "too-large" || after.state == "too-large" {
        "too-large"
    } else {
        "text"
    };
    QuickNoteGitDiffFile {
        before: if preview == "text" { before.text } else { String::new() },
        after: if preview == "text" { after.text } else { String::new() },
        preview: preview.into(),
        path: change.path,
        original_path: change.original_path,
        status: change.status,
    }
}

fn parse_name_status(output: &str) -> Vec<QuickNoteGitChange> {
    output.lines().filter_map(|line| {
        let mut parts = line.split('\t');
        let status = parts.next()?.trim().to_string();
        let first = parts.next()?.to_string();
        let second = parts.next().map(str::to_string);
        let (original_path, path) = if status.starts_with('R') || status.starts_with('C') {
            (Some(first), second.unwrap_or_default())
        } else {
            (None, first)
        };
        Some(QuickNoteGitChange { path, original_path, status, conflict: false })
    }).collect()
}

#[tauri::command]
pub fn quick_note_git_action(
    app: tauri::AppHandle,
    settings: tauri::State<'_, crate::repositories::settings::SettingsRepository>,
    input: QuickNoteGitActionInput,
) -> AppResult<QuickNoteGitActionResult> {
    let vault = super::quick_note_files::open_vault(&app)?;
    let username = settings.snapshot().vault.git_username;
    let runtime = app.path().app_data_dir().ok().map(|path| path.join("runtime"));
    Ok(perform_action(vault.root(), &input, &username, runtime.as_deref())?)
}

fn perform_action(root: &Path, input: &QuickNoteGitActionInput, username: &str, runtime: Option<&Path>) -> Result<QuickNoteGitActionResult, String> {
    if input.action != "init" && !git_status(root)?.repository {
        return Ok(failure("Git repository is not initialized in the Vault root"));
    }
    match input.action.as_str() {
        "init" => initialize(root),
        "configure-remote" => configure_remote(root, input.remote.as_deref().unwrap_or("")),
        "commit" => commit(root, input.message.as_deref(), username),
        "fetch" => command_result_auth(root, &["fetch", "--prune", "origin"], username, runtime),
        "pull" => pull(root, username, runtime),
        "push" => command_result_auth(root, &["push", "-u", "origin", "HEAD"], username, runtime),
        "discard" => discard(root, input.path.as_deref()),
        "abort-merge" => abort_merge(root),
        "resolve-conflict" => resolve_conflict(root, input.path.as_deref(), input.strategy.as_deref()),
        "continue-operation" => continue_operation(root, username),
        _ => Err("unsupported quick note git action".into()),
    }
}

fn initialize(root: &Path) -> Result<QuickNoteGitActionResult, String> {
    if git_status(root)?.repository {
        return Ok(success("Git repository is already initialized"));
    }
    let initialized = run_git(root, &["init"])?;
    if !initialized.status.success() {
        return Ok(failure_output(&initialized));
    }
    let ignore = root.join(".gitignore");
    if !ignore.exists() {
        fs::write(&ignore, DEFAULT_GITIGNORE).map_err(|error| format!("failed to write gitignore: {error}"))?;
    }
    let status = git_status(root)?;
    if status.changes.is_empty() {
        return Ok(success(command_text(&initialized).trim()));
    }
    commit(root, Some("Initial MooTool Vault setup"), "")
}

fn configure_remote(root: &Path, remote: &str) -> Result<QuickNoteGitActionResult, String> {
    let remote = validate_remote(remote)?;
    let exists = run_git(root, &["remote", "get-url", "origin"])?.status.success();
    if remote.is_empty() {
        return if exists { command_result(root, &["remote", "remove", "origin"]) } else { Ok(success("Remote is already removed")) };
    }
    if exists {
        command_result(root, &["remote", "set-url", "origin", &remote])
    } else {
        command_result(root, &["remote", "add", "origin", &remote])
    }
}

fn commit(root: &Path, message: Option<&str>, username: &str) -> Result<QuickNoteGitActionResult, String> {
    let message = message.unwrap_or("").trim();
    if message.is_empty() || message.len() > 300 || message.contains(['\n', '\r']) {
        return Err("Commit message is required".into());
    }
    let status = git_status(root)?;
    if status.merging || status.conflicts > 0 {
        return Ok(failure("Finish or abort the current merge/rebase before creating a checkpoint"));
    }
    if status.changes.is_empty() {
        return Ok(success("No changes to commit"));
    }
    ensure_identity(root, username)?;
    let added = run_git(root, &["add", "--all"])?;
    if !added.status.success() {
        return Ok(failure_output(&added));
    }
    command_result(root, &["-c", "commit.gpgsign=false", "commit", "-m", message])
}

fn pull(root: &Path, username: &str, runtime: Option<&Path>) -> Result<QuickNoteGitActionResult, String> {
    let status = git_status(root)?;
    if status.merging {
        return Ok(failure("Finish or abort the current merge/rebase before pulling"));
    }
    command_result_auth(root, &["pull", "--no-rebase", "origin"], username, runtime)
}

fn discard(root: &Path, path: Option<&str>) -> Result<QuickNoteGitActionResult, String> {
    let path = validate_git_path(path.unwrap_or(""))?;
    let status = git_status(root)?;
    let Some(change) = status.changes.iter().find(|change| change.path == path) else {
        return Ok(success("No changes to discard"));
    };
    if change.status == "??" {
        return command_result(root, &["clean", "-f", "--", path]);
    }
    let restored = run_git(root, &["restore", "--staged", "--worktree", "--source=HEAD", "--", path])?;
    if restored.status.success() {
        return Ok(success(command_text(&restored).trim()));
    }
    command_result(root, &["checkout", "HEAD", "--", path])
}

fn abort_merge(root: &Path) -> Result<QuickNoteGitActionResult, String> {
    let merge = run_git(root, &["merge", "--abort"])?;
    if merge.status.success() {
        return Ok(success(command_text(&merge).trim()));
    }
    command_result(root, &["rebase", "--abort"])
}

fn resolve_conflict(root: &Path, path: Option<&str>, strategy: Option<&str>) -> Result<QuickNoteGitActionResult, String> {
    let path = validate_git_path(path.unwrap_or(""))?;
    let strategy = match strategy {
        Some("ours" | "theirs") => strategy.unwrap_or_default(),
        _ => return Err("Invalid conflict strategy".into()),
    };
    let status = git_status(root)?;
    if !status.changes.iter().any(|change| change.path == path && change.conflict) {
        return Err("Git path is not conflicted".into());
    }
    let flag = format!("--{strategy}");
    let checkout = run_git(root, &["checkout", &flag, "--", path])?;
    if !checkout.status.success() {
        return Ok(failure_output(&checkout));
    }
    command_result(root, &["add", "--", path])
}

fn continue_operation(root: &Path, username: &str) -> Result<QuickNoteGitActionResult, String> {
    let status = git_status(root)?;
    if status.operation == "none" {
        return Ok(failure("No merge or rebase is in progress"));
    }
    if status.conflicts > 0 {
        return Ok(failure("Resolve all conflicts before continuing"));
    }
    ensure_identity(root, username)?;
    if status.operation == "rebase" {
        command_result(root, &["-c", "core.editor=true", "-c", "commit.gpgsign=false", "rebase", "--continue"])
    } else {
        command_result(root, &["-c", "commit.gpgsign=false", "commit", "--no-edit"])
    }
}

fn git_status(root: &Path) -> Result<QuickNoteGitStatus, String> {
    if !git_available() {
        return Ok(QuickNoteGitStatus::default());
    }
    let top = run_git(root, &["rev-parse", "--show-toplevel"])?;
    if !top.status.success() {
        return Ok(QuickNoteGitStatus { available: true, ..QuickNoteGitStatus::default() });
    }
    let root_real = root.canonicalize().unwrap_or_else(|_| root.to_path_buf());
    let top_real = Path::new(String::from_utf8_lossy(&top.stdout).trim()).canonicalize().unwrap_or_default();
    if root_real != top_real {
        return Ok(QuickNoteGitStatus { available: true, ..QuickNoteGitStatus::default() });
    }
    let porcelain = run_git(root, &["status", "--porcelain=v1", "--branch", "--untracked-files=all"])?;
    let (branch_line, changes) = parse_porcelain(&String::from_utf8_lossy(&porcelain.stdout));
    let remote = run_git(root, &["remote", "get-url", "origin"])
        .ok()
        .filter(|output| output.status.success())
        .map(|output| String::from_utf8_lossy(&output.stdout).trim().to_string())
        .unwrap_or_default();
    let merging = run_git(root, &["rev-parse", "-q", "--verify", "MERGE_HEAD"])?.status.success();
    let rebase = rebase_in_progress(root);
    let conflicts = changes.iter().filter(|change| change.conflict).count() as u32;
    Ok(QuickNoteGitStatus {
        available: true,
        repository: true,
        branch: parse_branch(&branch_line),
        remote,
        ahead: parse_counter(&branch_line, "ahead "),
        behind: parse_counter(&branch_line, "behind "),
        changes,
        conflicts,
        merging: merging || rebase,
        operation: if rebase { "rebase" } else if merging { "merge" } else { "none" }.into(),
    })
}

fn rebase_in_progress(root: &Path) -> bool {
    for name in ["rebase-merge", "rebase-apply"] {
        let Ok(output) = run_git(root, &["rev-parse", "--git-path", name]) else { continue };
        if output.status.success() && root.join(String::from_utf8_lossy(&output.stdout).trim()).exists() {
            return true;
        }
    }
    false
}

fn ensure_identity(root: &Path, username: &str) -> Result<(), String> {
    let name = if username.trim().is_empty() { "MooTool" } else { username.trim() };
    if !run_git(root, &["config", "--get", "user.name"])?.status.success() {
        let output = run_git(root, &["config", "user.name", name])?;
        if !output.status.success() {
            return Err(command_text(&output));
        }
    }
    if !run_git(root, &["config", "--get", "user.email"])?.status.success() {
        let output = run_git(root, &["config", "user.email", "mootool@localhost"])?;
        if !output.status.success() {
            return Err(command_text(&output));
        }
    }
    Ok(())
}

fn git_available() -> bool {
    Command::new("git").arg("--version").output().is_ok_and(|output| output.status.success())
}

fn run_git(root: &Path, args: &[&str]) -> Result<std::process::Output, String> {
    run_git_with_env(root, args, &[])
}

fn run_git_with_env(root: &Path, args: &[&str], env: &[(&str, &str)]) -> Result<std::process::Output, String> {
    let mut command = Command::new("git");
    command
        .args(args)
        .current_dir(root)
        .env("GIT_TERMINAL_PROMPT", "0")
        .stdin(std::process::Stdio::null());
    for (key, value) in env {
        command.env(key, value);
    }
    command.output().map_err(|error| format!("failed to run system Git: {error}"))
}

fn command_result(root: &Path, args: &[&str]) -> Result<QuickNoteGitActionResult, String> {
    finish_git(run_git(root, args)?)
}

fn command_result_auth(root: &Path, args: &[&str], username: &str, runtime: Option<&Path>) -> Result<QuickNoteGitActionResult, String> {
    let token = crate::commands::secure_credentials::load_git_token()?.unwrap_or_default();
    if token.is_empty() {
        return command_result(root, args);
    }
    let Some(runtime) = runtime else { return command_result(root, args); };
    let askpass = prepare_askpass(runtime)?;
    let askpass = askpass.to_string_lossy().into_owned();
    finish_git(run_git_with_env(root, args, &[
        ("GIT_ASKPASS", askpass.as_str()),
        ("MOOTOOL_GIT_USERNAME", username),
        ("MOOTOOL_GIT_TOKEN", token.as_str()),
    ])?)
}

fn prepare_askpass(directory: &Path) -> Result<std::path::PathBuf, String> {
    fs::create_dir_all(directory).map_err(|error| format!("failed to create git askpass directory: {error}"))?;
    let windows = cfg!(target_os = "windows");
    let path = directory.join(if windows { "mootool-git-askpass.cmd" } else { "mootool-git-askpass.sh" });
    let script = if windows {
        "@echo off\r\necho %1 | findstr /I \"Username\" >nul\r\nif %errorlevel%==0 (echo %MOOTOOL_GIT_USERNAME%) else (echo %MOOTOOL_GIT_TOKEN%)\r\n"
    } else {
        "#!/bin/sh\ncase \"$1\" in\n  *Username*) printf \"%s\" \"$MOOTOOL_GIT_USERNAME\" ;;\n  *) printf \"%s\" \"$MOOTOOL_GIT_TOKEN\" ;;\nesac\n"
    };
    fs::write(&path, script).map_err(|error| format!("failed to write git askpass: {error}"))?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(&path, fs::Permissions::from_mode(0o700))
            .map_err(|error| format!("failed to make git askpass executable: {error}"))?;
    }
    Ok(path)
}

fn finish_git(output: std::process::Output) -> Result<QuickNoteGitActionResult, String> {
    if output.status.success() { Ok(success(command_text(&output).trim())) } else { Ok(failure_output(&output)) }
}

fn success(message: &str) -> QuickNoteGitActionResult {
    QuickNoteGitActionResult { success: true, message: if message.is_empty() { "Done".into() } else { message.into() } }
}

fn failure(message: &str) -> QuickNoteGitActionResult {
    QuickNoteGitActionResult { success: false, message: message.into() }
}

fn failure_output(output: &std::process::Output) -> QuickNoteGitActionResult {
    let text = command_text(output);
    failure(text.trim())
}

fn command_text(output: &std::process::Output) -> String {
    let mut text = String::from_utf8_lossy(&output.stdout).into_owned();
    let stderr = String::from_utf8_lossy(&output.stderr);
    if !stderr.trim().is_empty() {
        if !text.is_empty() { text.push('\n'); }
        text.push_str(stderr.trim());
    }
    if text.len() > 1024 * 1024 {
        text.truncate(1024 * 1024);
        text.push_str("\n… output truncated …");
    }
    text
}

fn parse_porcelain(output: &str) -> (String, Vec<QuickNoteGitChange>) {
    let mut lines = output.lines();
    let branch_line = lines.next().unwrap_or_default().to_string();
    let changes = lines.filter(|line| line.len() >= 4).map(|line| {
        let status = line[..2].to_string();
        let raw = line[3..].to_string();
        let (original_path, path) = raw.split_once(" -> ").map(|(from, to)| (Some(from.to_string()), to.to_string())).unwrap_or((None, raw));
        let conflict = status.contains('U') || status == "AA" || status == "DD";
        QuickNoteGitChange { path, original_path, status, conflict }
    }).collect();
    (branch_line, changes)
}

fn parse_history(output: &str) -> Vec<QuickNoteGitCommit> {
    output.lines().filter_map(|line| {
        let mut parts = line.split('\u{1f}');
        Some(QuickNoteGitCommit {
            hash: parts.next()?.into(),
            short_hash: parts.next()?.into(),
            author: parts.next()?.into(),
            date: parts.next()?.into(),
            message: parts.next()?.into(),
        })
    }).collect()
}

fn parse_branch(header: &str) -> String {
    let value = header.strip_prefix("## ").unwrap_or(header);
    if let Some(branch) = value.strip_prefix("No commits yet on ") {
        return branch.trim().to_string();
    }
    value.split("...").next().unwrap_or_default().split_whitespace().next().unwrap_or_default().trim().to_string()
}

fn parse_counter(header: &str, marker: &str) -> u32 {
    header.find(marker).and_then(|start| header[start + marker.len()..].split(|character: char| !character.is_ascii_digit()).next()).and_then(|value| value.parse().ok()).unwrap_or_default()
}

fn validate_git_path(value: &str) -> Result<&str, String> {
    if value.is_empty() || value.len() > 2048 || value.starts_with('/') || value.contains('\\') || value.contains('\0') || value.split('/').any(|part| part.is_empty() || part == "." || part == "..") {
        return Err("invalid quick note git path".into());
    }
    Ok(value)
}

fn validate_commit(value: &str) -> Result<(), String> {
    if value.len() < 4 || value.len() > 64 || !value.chars().all(|character| character.is_ascii_hexdigit()) {
        return Err("invalid quick note git commit".into());
    }
    Ok(())
}

fn validate_remote(value: &str) -> Result<String, String> {
    let remote = value.trim();
    if remote.is_empty() {
        return Ok(String::new());
    }
    if remote.len() > 2048 || remote.contains(['\r', '\n', '\0']) || !(remote.starts_with("https://") || remote.starts_with("http://") || remote.starts_with("ssh://") || remote.starts_with("git://") || remote.starts_with("git@") || remote.starts_with("file://")) {
        return Err("invalid quick note git remote".into());
    }
    Ok(remote.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_branch_changes_and_conflicts() {
        let (branch, changes) = parse_porcelain("## main...origin/main [ahead 2, behind 1]\n M Work/API ideas.md\n?? attachments/image.png\nUU Work/conflict.md\nR  old.md -> new.md\n");
        assert_eq!(parse_branch(&branch), "main");
        assert_eq!(parse_counter(&branch, "ahead "), 2);
        assert_eq!(parse_counter(&branch, "behind "), 1);
        assert_eq!(changes.len(), 4);
        assert!(changes[2].conflict);
        assert_eq!(changes[3].original_path.as_deref(), Some("old.md"));
        assert_eq!(changes[3].path, "new.md");
    }

    #[test]
    fn parses_commit_name_status() {
        let changes = parse_name_status("M\tWork/API ideas.md\nR100\told.md\tnew.md\n");
        assert_eq!(changes[0].path, "Work/API ideas.md");
        assert_eq!(changes[1].original_path.as_deref(), Some("old.md"));
        assert_eq!(changes[1].path, "new.md");
    }
}
