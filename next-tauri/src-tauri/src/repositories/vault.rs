use std::{
    fs,
    io::Write,
    path::{Component, Path, PathBuf},
    sync::{Mutex, mpsc},
    thread,
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use notify::{Config, RecommendedWatcher, RecursiveMode, Watcher};
use sha2::{Digest, Sha256};
use tauri::Emitter;
use tempfile::NamedTempFile;

use crate::contracts::vault::{
    VaultChangedEvent, VaultDocument, VaultFileEntry, VaultSaveRequest, VaultTrashResult,
};

pub const VAULT_CHANGED_EVENT: &str = "mootool://vault-changed";
const MAX_DOCUMENT_BYTES: u64 = 20 * 1024 * 1024;
const MAX_VAULT_FILES: usize = 10_000;
const MAX_SCAN_DEPTH: usize = 32;
const WATCH_DEBOUNCE: Duration = Duration::from_millis(300);

enum WatchMessage {
    Changed,
    Stop,
}

struct VaultWatch {
    _watcher: RecommendedWatcher,
    sender: mpsc::Sender<WatchMessage>,
}

impl Drop for VaultWatch {
    fn drop(&mut self) {
        let _ = self.sender.send(WatchMessage::Stop);
    }
}

#[derive(Default)]
struct VaultState {
    root: Option<PathBuf>,
    watch: Option<VaultWatch>,
}

#[derive(Default)]
pub struct VaultRepository {
    state: Mutex<VaultState>,
}

impl VaultRepository {
    pub fn configure(&self, app: &tauri::AppHandle, root: PathBuf) -> Result<PathBuf, String> {
        let root = validate_root(&root)?;
        let watch = create_watch(app.clone(), &root)?;
        let mut state = self
            .state
            .lock()
            .map_err(|_| "vault repository state poisoned".to_string())?;
        state.watch = Some(watch);
        state.root = Some(root.clone());
        Ok(root)
    }

    pub fn disconnect(&self) -> Result<(), String> {
        let mut state = self
            .state
            .lock()
            .map_err(|_| "vault repository state poisoned".to_string())?;
        state.watch = None;
        state.root = None;
        Ok(())
    }

    pub fn root(&self) -> Result<PathBuf, String> {
        self.configured_root()?
            .ok_or_else(|| "JSON Vault is not configured".into())
    }

    pub fn configured_root(&self) -> Result<Option<PathBuf>, String> {
        Ok(self
            .state
            .lock()
            .map_err(|_| "vault repository state poisoned".to_string())?
            .root
            .clone())
    }

    pub fn list_files(&self) -> Result<Vec<VaultFileEntry>, String> {
        list_json_files(&self.root()?)
    }

    pub fn list_directories(&self) -> Result<Vec<String>, String> {
        list_directories(&self.root()?)
    }

    pub fn read_document(&self, relative_path: &str) -> Result<VaultDocument, String> {
        read_document_at(&self.root()?, relative_path)
    }

    pub fn save_document(&self, request: VaultSaveRequest) -> Result<VaultDocument, String> {
        save_document_at(&self.root()?, request)
    }

    pub fn create_directory(&self, relative_path: &str) -> Result<String, String> {
        create_directory_at(&self.root()?, relative_path)
    }

    pub fn move_entry(
        &self,
        relative_path: &str,
        destination_path: &str,
        expected_fingerprint: Option<&str>,
    ) -> Result<String, String> {
        move_entry_at(
            &self.root()?,
            relative_path,
            destination_path,
            expected_fingerprint,
        )
    }

    pub fn duplicate_document(
        &self,
        relative_path: &str,
        destination_path: &str,
        expected_fingerprint: &str,
    ) -> Result<VaultDocument, String> {
        duplicate_document_at(
            &self.root()?,
            relative_path,
            destination_path,
            expected_fingerprint,
        )
    }

    pub fn trash_document(
        &self,
        relative_path: &str,
        trash_root: &Path,
    ) -> Result<VaultTrashResult, String> {
        trash_document_at(&self.root()?, relative_path, trash_root)
    }

    pub fn trash_entry(
        &self,
        relative_path: &str,
        expected_fingerprint: Option<&str>,
        trash_root: &Path,
    ) -> Result<VaultTrashResult, String> {
        trash_entry_at(
            &self.root()?,
            relative_path,
            expected_fingerprint,
            trash_root,
        )
    }

    #[cfg(test)]
    pub(crate) fn configure_without_watcher(&self, root: PathBuf) -> Result<(), String> {
        self.state
            .lock()
            .map_err(|_| "vault repository state poisoned".to_string())?
            .root = Some(validate_root(&root)?);
        Ok(())
    }
}

fn create_watch(app: tauri::AppHandle, root: &Path) -> Result<VaultWatch, String> {
    let (sender, receiver) = mpsc::channel();
    let callback_sender = sender.clone();
    let mut watcher = RecommendedWatcher::new(
        move |result: notify::Result<notify::Event>| {
            if result.is_ok() {
                let _ = callback_sender.send(WatchMessage::Changed);
            }
        },
        Config::default(),
    )
    .map_err(|error| format!("failed to create JSON Vault watcher: {error}"))?;
    watcher
        .watch(root, RecursiveMode::Recursive)
        .map_err(|error| format!("failed to watch JSON Vault {}: {error}", root.display()))?;
    thread::Builder::new()
        .name("mootool-vault-watch".into())
        .spawn(move || watch_worker(app, receiver))
        .map_err(|error| format!("failed to start JSON Vault watch worker: {error}"))?;
    Ok(VaultWatch {
        _watcher: watcher,
        sender,
    })
}

fn watch_worker(app: tauri::AppHandle, receiver: mpsc::Receiver<WatchMessage>) {
    while let Ok(WatchMessage::Changed) = receiver.recv() {
        loop {
            match receiver.recv_timeout(WATCH_DEBOUNCE) {
                Ok(WatchMessage::Changed) => {}
                Ok(WatchMessage::Stop) | Err(mpsc::RecvTimeoutError::Disconnected) => return,
                Err(mpsc::RecvTimeoutError::Timeout) => break,
            }
        }
        let _ = app.emit(
            VAULT_CHANGED_EVENT,
            VaultChangedEvent {
                reason: "filesystem".into(),
            },
        );
    }
}

fn validate_root(root: &Path) -> Result<PathBuf, String> {
    if !root.is_absolute() {
        return Err("JSON Vault root must be an absolute path".into());
    }
    let metadata = fs::symlink_metadata(root)
        .map_err(|error| format!("JSON Vault root {} is unavailable: {error}", root.display()))?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err("JSON Vault root must be a regular directory, not a symbolic link".into());
    }
    root.canonicalize()
        .map_err(|error| format!("failed to canonicalize JSON Vault root: {error}"))
}

fn list_json_files(root: &Path) -> Result<Vec<VaultFileEntry>, String> {
    let mut files = Vec::new();
    let mut directories = vec![(root.to_path_buf(), 0usize)];
    while let Some((directory, depth)) = directories.pop() {
        if depth > MAX_SCAN_DEPTH {
            return Err("JSON Vault directory nesting exceeds 32 levels".into());
        }
        let entries = fs::read_dir(&directory).map_err(|error| {
            format!(
                "failed to read JSON Vault directory {}: {error}",
                directory.display()
            )
        })?;
        for entry in entries {
            let entry = entry.map_err(|error| format!("failed to inspect JSON Vault: {error}"))?;
            if entry.file_name() == ".git" {
                continue;
            }
            let path = entry.path();
            let metadata = path
                .symlink_metadata()
                .map_err(|error| format!("failed to inspect JSON Vault path: {error}"))?;
            if metadata.file_type().is_symlink() {
                continue;
            }
            if metadata.is_dir() {
                directories.push((path, depth + 1));
                continue;
            }
            if !metadata.is_file() || !has_json_extension(&path) {
                continue;
            }
            if metadata.len() > MAX_DOCUMENT_BYTES {
                continue;
            }
            files.push(VaultFileEntry {
                relative_path: relative_text(root, &path)?,
                size_bytes: metadata.len(),
                modified_at: modified_millis(&metadata),
            });
            if files.len() > MAX_VAULT_FILES {
                return Err("JSON Vault contains more than 10000 JSON files".into());
            }
        }
    }
    files.sort_by(|left, right| left.relative_path.cmp(&right.relative_path));
    Ok(files)
}

fn list_directories(root: &Path) -> Result<Vec<String>, String> {
    let mut result = Vec::new();
    let mut directories = vec![(root.to_path_buf(), 0usize)];
    while let Some((directory, depth)) = directories.pop() {
        if depth > MAX_SCAN_DEPTH {
            return Err("JSON Vault directory nesting exceeds 32 levels".into());
        }
        let entries = fs::read_dir(&directory).map_err(|error| {
            format!(
                "failed to read JSON Vault directory {}: {error}",
                directory.display()
            )
        })?;
        for entry in entries {
            let entry = entry.map_err(|error| format!("failed to inspect JSON Vault: {error}"))?;
            if entry.file_name() == ".git" {
                continue;
            }
            let path = entry.path();
            let metadata = path
                .symlink_metadata()
                .map_err(|error| format!("failed to inspect JSON Vault path: {error}"))?;
            if metadata.file_type().is_symlink() || !metadata.is_dir() {
                continue;
            }
            result.push(relative_text(root, &path)?);
            if result.len() > MAX_VAULT_FILES {
                return Err("JSON Vault contains more than 10000 directories".into());
            }
            directories.push((path, depth + 1));
        }
    }
    result.sort();
    Ok(result)
}

fn read_document_at(root: &Path, relative_path: &str) -> Result<VaultDocument, String> {
    let relative = normalize_relative_json_path(relative_path)?;
    let path = resolve_existing(root, &relative, false)?;
    let metadata = fs::metadata(&path)
        .map_err(|error| format!("failed to inspect JSON Vault document: {error}"))?;
    if metadata.len() > MAX_DOCUMENT_BYTES {
        return Err("JSON Vault document exceeds 20 MiB".into());
    }
    let bytes =
        fs::read(&path).map_err(|error| format!("failed to read JSON Vault document: {error}"))?;
    let content = String::from_utf8(bytes)
        .map_err(|_| "JSON Vault documents must use UTF-8 encoding".to_string())?;
    Ok(VaultDocument {
        relative_path: relative_text(Path::new(""), &relative)?,
        fingerprint: fingerprint(content.as_bytes()),
        content,
        modified_at: modified_millis(&metadata),
    })
}

fn save_document_at(root: &Path, request: VaultSaveRequest) -> Result<VaultDocument, String> {
    if request.content.len() as u64 > MAX_DOCUMENT_BYTES {
        return Err("JSON Vault document exceeds 20 MiB".into());
    }
    serde_json::from_str::<serde_json::Value>(&request.content)
        .map_err(|error| format!("JSON Vault document is not valid JSON: {error}"))?;
    let relative = normalize_relative_json_path(&request.relative_path)?;
    let parent_relative = relative.parent().unwrap_or_else(|| Path::new(""));
    let parent = resolve_existing(root, parent_relative, true)?;
    let path = parent.join(
        relative
            .file_name()
            .ok_or_else(|| "JSON Vault document name is invalid".to_string())?,
    );
    let existing = path.symlink_metadata().ok();
    if let Some(metadata) = &existing
        && (metadata.file_type().is_symlink() || !metadata.is_file())
    {
        return Err("JSON Vault document target is not a regular file".into());
    }
    match (&request.expected_fingerprint, existing.is_some()) {
        (None, true) => return Err("JSON Vault document already exists".into()),
        (Some(_), false) => return Err("JSON Vault document was removed externally".into()),
        (Some(expected), true) => {
            let current = fs::read(&path)
                .map_err(|error| format!("failed to check JSON Vault conflict: {error}"))?;
            if fingerprint(&current) != *expected {
                return Err(
                    "JSON Vault document changed outside MooTool; reload before saving".into(),
                );
            }
        }
        (None, false) => {}
    }

    let mut temporary = NamedTempFile::new_in(&parent)
        .map_err(|error| format!("failed to create temporary JSON Vault document: {error}"))?;
    temporary
        .write_all(request.content.as_bytes())
        .and_then(|_| temporary.flush())
        .map_err(|error| format!("failed to write temporary JSON Vault document: {error}"))?;
    temporary
        .as_file()
        .sync_all()
        .map_err(|error| format!("failed to sync JSON Vault document: {error}"))?;
    temporary
        .persist(&path)
        .map_err(|error| format!("failed to replace JSON Vault document atomically: {error}"))?;
    read_document_at(root, &request.relative_path)
}

fn create_directory_at(root: &Path, relative_path: &str) -> Result<String, String> {
    let relative = normalize_relative_entry_path(relative_path)?;
    let mut current = root.to_path_buf();
    let mut created = false;
    for component in relative.components() {
        let Component::Normal(value) = component else {
            return Err("JSON Vault directory path is invalid".into());
        };
        current.push(value);
        match current.symlink_metadata() {
            Ok(metadata) => {
                if metadata.file_type().is_symlink() || !metadata.is_dir() {
                    return Err("JSON Vault directory path conflicts with an existing entry".into());
                }
            }
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
                fs::create_dir(&current)
                    .map_err(|error| format!("failed to create JSON Vault directory: {error}"))?;
                created = true;
            }
            Err(error) => {
                return Err(format!("failed to inspect JSON Vault directory: {error}"));
            }
        }
        let canonical = current
            .canonicalize()
            .map_err(|error| format!("failed to canonicalize JSON Vault directory: {error}"))?;
        if !canonical.starts_with(root) {
            return Err("JSON Vault directory escapes its configured root".into());
        }
    }
    if !created {
        return Err("JSON Vault directory already exists".into());
    }
    relative_text(Path::new(""), &relative)
}

fn move_entry_at(
    root: &Path,
    relative_path: &str,
    destination_path: &str,
    expected_fingerprint: Option<&str>,
) -> Result<String, String> {
    let source_relative = normalize_relative_entry_path(relative_path)?;
    let destination_relative = normalize_relative_entry_path(destination_path)?;
    if source_relative == destination_relative {
        return Err("JSON Vault source and destination are the same".into());
    }
    let source = resolve_existing_any(root, &source_relative)?;
    let source_metadata = source
        .symlink_metadata()
        .map_err(|error| format!("failed to inspect JSON Vault source: {error}"))?;
    if source_metadata.is_file() {
        if !has_json_extension(&source_relative) || !has_json_extension(&destination_relative) {
            return Err("JSON Vault documents must use a .json extension".into());
        }
        let expected = expected_fingerprint
            .ok_or_else(|| "JSON Vault document fingerprint is required".to_string())?;
        let current = fs::read(&source)
            .map_err(|error| format!("failed to check JSON Vault conflict: {error}"))?;
        if fingerprint(&current) != expected {
            return Err("JSON Vault document changed outside MooTool; reload before moving".into());
        }
    } else if !source_metadata.is_dir() {
        return Err("JSON Vault source is not a regular file or directory".into());
    }
    let destination_parent_relative = destination_relative
        .parent()
        .unwrap_or_else(|| Path::new(""));
    let destination_parent = resolve_existing(root, destination_parent_relative, true)?;
    if source_metadata.is_dir() && destination_parent.starts_with(&source) {
        return Err("JSON Vault directory cannot be moved inside itself".into());
    }
    let destination = destination_parent.join(
        destination_relative
            .file_name()
            .ok_or_else(|| "JSON Vault destination name is invalid".to_string())?,
    );
    match destination.symlink_metadata() {
        Ok(_) => return Err("JSON Vault destination already exists".into()),
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
        Err(error) => return Err(format!("failed to inspect JSON Vault destination: {error}")),
    }
    fs::rename(&source, &destination)
        .map_err(|error| format!("failed to move JSON Vault entry: {error}"))?;
    relative_text(Path::new(""), &destination_relative)
}

fn duplicate_document_at(
    root: &Path,
    relative_path: &str,
    destination_path: &str,
    expected_fingerprint: &str,
) -> Result<VaultDocument, String> {
    let source = read_document_at(root, relative_path)?;
    if source.fingerprint != expected_fingerprint {
        return Err("JSON Vault document changed outside MooTool; reload before copying".into());
    }
    save_document_at(
        root,
        VaultSaveRequest {
            relative_path: destination_path.into(),
            content: source.content,
            expected_fingerprint: None,
        },
    )
}

fn trash_document_at(
    root: &Path,
    relative_path: &str,
    trash_root: &Path,
) -> Result<VaultTrashResult, String> {
    let relative = normalize_relative_json_path(relative_path)?;
    let source = resolve_existing(root, &relative, false)?;
    let timestamp = unix_millis();
    let target = trash_root.join(timestamp.to_string()).join(&relative);
    let parent = target
        .parent()
        .ok_or_else(|| "vault recovery path has no parent".to_string())?;
    fs::create_dir_all(parent)
        .map_err(|error| format!("failed to create Vault recovery directory: {error}"))?;
    fs::copy(&source, &target)
        .map_err(|error| format!("failed to preserve deleted Vault document: {error}"))?;
    fs::File::open(&target)
        .and_then(|file| file.sync_all())
        .map_err(|error| format!("failed to sync recovered Vault document: {error}"))?;
    fs::remove_file(&source)
        .map_err(|error| format!("failed to remove JSON Vault document: {error}"))?;
    Ok(VaultTrashResult {
        relative_path: relative_text(Path::new(""), &relative)?,
        recovery_path: target.to_string_lossy().into_owned(),
    })
}

fn trash_entry_at(
    root: &Path,
    relative_path: &str,
    expected_fingerprint: Option<&str>,
    trash_root: &Path,
) -> Result<VaultTrashResult, String> {
    let relative = normalize_relative_entry_path(relative_path)?;
    let source = resolve_existing_any(root, &relative)?;
    let metadata = source
        .symlink_metadata()
        .map_err(|error| format!("failed to inspect JSON Vault entry: {error}"))?;
    if metadata.is_file() {
        let expected = expected_fingerprint
            .ok_or_else(|| "JSON Vault document fingerprint is required".to_string())?;
        let current = fs::read(&source)
            .map_err(|error| format!("failed to check JSON Vault conflict: {error}"))?;
        if fingerprint(&current) != expected {
            return Err(
                "JSON Vault document changed outside MooTool; reload before deleting".into(),
            );
        }
        return trash_document_at(root, relative_path, trash_root);
    }
    if !metadata.is_dir() {
        return Err("JSON Vault entry is not a regular file or directory".into());
    }
    let target = trash_root.join(unix_millis().to_string()).join(&relative);
    let parent = target
        .parent()
        .ok_or_else(|| "vault recovery path has no parent".to_string())?;
    fs::create_dir_all(parent)
        .map_err(|error| format!("failed to create Vault recovery directory: {error}"))?;
    fs::rename(&source, &target).map_err(|error| {
        format!(
            "failed to move JSON Vault directory to recovery storage without data loss: {error}"
        )
    })?;
    Ok(VaultTrashResult {
        relative_path: relative_text(Path::new(""), &relative)?,
        recovery_path: target.to_string_lossy().into_owned(),
    })
}

fn normalize_relative_json_path(value: &str) -> Result<PathBuf, String> {
    let normalized = normalize_relative_entry_path(value)?;
    if !has_json_extension(&normalized) {
        return Err("JSON Vault documents must use a .json extension".into());
    }
    Ok(normalized)
}

fn normalize_relative_entry_path(value: &str) -> Result<PathBuf, String> {
    if value.is_empty() || value.len() > 1_024 {
        return Err("JSON Vault relative path is invalid".into());
    }
    let mut normalized = PathBuf::new();
    for component in Path::new(value).components() {
        match component {
            Component::Normal(value)
                if !value.is_empty() && !value.eq_ignore_ascii_case(".git") =>
            {
                normalized.push(value)
            }
            _ => return Err("JSON Vault paths cannot be absolute or contain traversal".into()),
        }
    }
    if normalized.as_os_str().is_empty() {
        return Err("JSON Vault relative path is invalid".into());
    }
    Ok(normalized)
}

fn resolve_existing_any(root: &Path, relative: &Path) -> Result<PathBuf, String> {
    let mut current = root.to_path_buf();
    for component in relative.components() {
        let Component::Normal(value) = component else {
            return Err("JSON Vault path is invalid".into());
        };
        current.push(value);
        let metadata = current.symlink_metadata().map_err(|error| {
            format!(
                "JSON Vault path {} is unavailable: {error}",
                current.display()
            )
        })?;
        if metadata.file_type().is_symlink() {
            return Err("JSON Vault paths cannot traverse symbolic links".into());
        }
    }
    let canonical = current
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize JSON Vault path: {error}"))?;
    if !canonical.starts_with(root) {
        return Err("JSON Vault path escapes its configured root".into());
    }
    Ok(canonical)
}

fn resolve_existing(root: &Path, relative: &Path, directory: bool) -> Result<PathBuf, String> {
    let mut current = root.to_path_buf();
    for component in relative.components() {
        let Component::Normal(value) = component else {
            return Err("JSON Vault path is invalid".into());
        };
        current.push(value);
        let metadata = current.symlink_metadata().map_err(|error| {
            format!(
                "JSON Vault path {} is unavailable: {error}",
                current.display()
            )
        })?;
        if metadata.file_type().is_symlink() {
            return Err("JSON Vault paths cannot traverse symbolic links".into());
        }
    }
    let metadata = current.symlink_metadata().map_err(|error| {
        format!(
            "JSON Vault path {} is unavailable: {error}",
            current.display()
        )
    })?;
    if (directory && !metadata.is_dir()) || (!directory && !metadata.is_file()) {
        return Err("JSON Vault path has an unexpected file type".into());
    }
    let canonical = current
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize JSON Vault path: {error}"))?;
    if !canonical.starts_with(root) {
        return Err("JSON Vault path escapes its configured root".into());
    }
    Ok(canonical)
}

fn relative_text(root: &Path, path: &Path) -> Result<String, String> {
    let relative = path
        .strip_prefix(root)
        .map_err(|_| "JSON Vault path is outside its configured root".to_string())?;
    Ok(relative
        .components()
        .filter_map(|component| match component {
            Component::Normal(value) => Some(value.to_string_lossy().into_owned()),
            _ => None,
        })
        .collect::<Vec<_>>()
        .join("/"))
}

fn has_json_extension(path: &Path) -> bool {
    path.extension()
        .and_then(|extension| extension.to_str())
        .is_some_and(|extension| extension.eq_ignore_ascii_case("json"))
}

fn fingerprint(bytes: &[u8]) -> String {
    let digest = Sha256::digest(bytes);
    digest.iter().map(|byte| format!("{byte:02x}")).collect()
}

fn modified_millis(metadata: &fs::Metadata) -> u64 {
    metadata
        .modified()
        .ok()
        .and_then(|modified| modified.duration_since(UNIX_EPOCH).ok())
        .map(|duration| duration.as_millis().try_into().unwrap_or(u64::MAX))
        .unwrap_or_default()
}

fn unix_millis() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn saves_reads_lists_and_recovers_json_documents() {
        let vault = tempfile::TempDir::new().expect("vault directory");
        let trash = tempfile::TempDir::new().expect("trash directory");
        let repository = VaultRepository::default();
        repository
            .configure_without_watcher(vault.path().to_path_buf())
            .expect("configure vault");
        let created = repository
            .save_document(VaultSaveRequest {
                relative_path: "example.json".into(),
                content: "{\"value\":1}".into(),
                expected_fingerprint: None,
            })
            .expect("save document");
        assert_eq!(repository.list_files().expect("list files").len(), 1);
        assert_eq!(
            repository.read_document("example.json").expect("read"),
            created
        );

        let updated = repository
            .save_document(VaultSaveRequest {
                relative_path: "example.json".into(),
                content: "{\"value\":2}".into(),
                expected_fingerprint: Some(created.fingerprint),
            })
            .expect("update document");
        assert!(updated.content.contains('2'));
        let trashed = repository
            .trash_document("example.json", trash.path())
            .expect("trash document");
        assert!(Path::new(&trashed.recovery_path).is_file());
        assert!(repository.list_files().expect("list files").is_empty());
    }

    #[test]
    fn rejects_traversal_symlinks_invalid_json_and_stale_writes() {
        let vault = tempfile::TempDir::new().expect("vault directory");
        let repository = VaultRepository::default();
        repository
            .configure_without_watcher(vault.path().to_path_buf())
            .expect("configure vault");
        assert!(repository.read_document("../outside.json").is_err());
        assert!(
            repository
                .save_document(VaultSaveRequest {
                    relative_path: "invalid.json".into(),
                    content: "not-json".into(),
                    expected_fingerprint: None,
                })
                .is_err()
        );
        fs::write(vault.path().join("conflict.json"), "{}\n").expect("seed file");
        assert!(
            repository
                .save_document(VaultSaveRequest {
                    relative_path: "conflict.json".into(),
                    content: "{\"changed\":true}".into(),
                    expected_fingerprint: Some("stale".into()),
                })
                .is_err()
        );

        #[cfg(unix)]
        {
            let outside = tempfile::TempDir::new().expect("outside directory");
            fs::write(outside.path().join("secret.json"), "{}").expect("outside JSON");
            std::os::unix::fs::symlink(outside.path(), vault.path().join("linked"))
                .expect("create symlink");
            assert!(repository.read_document("linked/secret.json").is_err());
            assert!(
                repository
                    .list_files()
                    .expect("list without symlink")
                    .iter()
                    .all(|file| !file.relative_path.starts_with("linked/"))
            );
        }
    }

    #[test]
    fn creates_lists_moves_duplicates_and_recovers_vault_entries() {
        let vault = tempfile::TempDir::new().expect("vault directory");
        let trash = tempfile::TempDir::new().expect("trash directory");
        let repository = VaultRepository::default();
        repository
            .configure_without_watcher(vault.path().to_path_buf())
            .expect("configure vault");

        assert_eq!(
            repository
                .create_directory("examples/nested")
                .expect("create nested directory"),
            "examples/nested"
        );
        assert_eq!(
            repository.list_directories().expect("list directories"),
            ["examples", "examples/nested"]
        );
        let original = repository
            .save_document(VaultSaveRequest {
                relative_path: "examples/original.json".into(),
                content: "{\"value\":1}".into(),
                expected_fingerprint: None,
            })
            .expect("save original");
        let copied = repository
            .duplicate_document(
                "examples/original.json",
                "examples/copy.json",
                &original.fingerprint,
            )
            .expect("duplicate document");
        assert_eq!(copied.content, original.content);
        assert!(
            repository
                .duplicate_document("examples/original.json", "examples/stale.json", "stale")
                .is_err()
        );
        assert_eq!(
            repository
                .move_entry(
                    "examples/copy.json",
                    "examples/nested/moved.json",
                    Some(&copied.fingerprint),
                )
                .expect("move document"),
            "examples/nested/moved.json"
        );
        assert_eq!(
            repository
                .move_entry("examples/nested", "archive", None)
                .expect("move directory"),
            "archive"
        );
        let recovered = repository
            .trash_entry("archive", None, trash.path())
            .expect("recover directory");
        assert!(
            Path::new(&recovered.recovery_path)
                .join("moved.json")
                .is_file()
        );
        assert!(!vault.path().join("archive").exists());
    }
}
