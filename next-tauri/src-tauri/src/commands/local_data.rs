use std::{
    fs,
    io::Write,
    path::PathBuf,
    time::{SystemTime, UNIX_EPOCH},
};

use tauri::{Emitter, Manager};

use crate::{
    contracts::error::AppResult,
    contracts::local_data::{
        BoardMessage, HostProfile, QuickNote, QuickNoteAttachment,
        QuickNoteAttachmentImportRequest, SystemHostsFile, TranslationHistory, TranslationWord,
    },
    repositories::local_data::LocalDataRepository,
};

pub const LOCAL_DATA_CHANGED_EVENT: &str = "mootool://local-data-changed";

#[tauri::command]
pub fn list_quick_notes(
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<Vec<QuickNote>> {
    Ok(repository.list_notes()?)
}

#[tauri::command]
pub fn save_quick_note(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    note: QuickNote,
) -> AppResult<QuickNote> {
    let saved = repository.save_note(note)?;
    emit_changed(&app, "quick-note")?;
    Ok(saved)
}

#[tauri::command]
pub fn delete_quick_note(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_note(&id)?;
    if deleted {
        emit_changed(&app, "quick-note")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_quick_note_attachments(
    repository: tauri::State<'_, LocalDataRepository>,
    note_id: String,
) -> AppResult<Vec<QuickNoteAttachment>> {
    Ok(repository.list_note_attachments(&note_id)?)
}

#[tauri::command]
pub fn import_quick_note_attachment(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    request: QuickNoteAttachmentImportRequest,
) -> AppResult<QuickNoteAttachment> {
    let path = PathBuf::from(&request.source_path);
    let metadata = fs::symlink_metadata(&path)
        .map_err(|error| format!("failed to inspect attachment source: {error}"))?;
    if !metadata.is_file() || metadata.file_type().is_symlink() {
        return Err("attachment source must be a regular file".into());
    }
    if metadata.len() > 10 * 1024 * 1024 {
        return Err("attachment cannot exceed 10 MiB".into());
    }
    let name = path
        .file_name()
        .and_then(|value| value.to_str())
        .ok_or_else(|| "attachment filename must be valid UTF-8".to_string())?
        .to_string();
    let data = fs::read(&path).map_err(|error| format!("failed to read attachment: {error}"))?;
    let attachment = QuickNoteAttachment {
        id: request.id,
        note_id: request.note_id,
        mime_type: attachment_mime_type(&name).into(),
        name,
        size_bytes: data.len() as u64,
        created_at: request.created_at,
    };
    let saved = repository.save_note_attachment(attachment, &data)?;
    emit_changed(&app, "quick-note-attachment")?;
    Ok(saved)
}

#[tauri::command]
pub fn export_quick_note_attachment(
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
    destination_path: String,
) -> AppResult<()> {
    let (_, data) = repository.note_attachment_data(&id)?;
    let destination = PathBuf::from(destination_path);
    let parent = destination
        .parent()
        .ok_or_else(|| "attachment destination has no parent directory".to_string())?;
    if let Ok(metadata) = fs::symlink_metadata(&destination)
        && metadata.file_type().is_symlink()
    {
        return Err("attachment destination cannot be a symbolic link".into());
    }
    let mut temporary = tempfile::NamedTempFile::new_in(parent)
        .map_err(|error| format!("failed to create temporary attachment export: {error}"))?;
    temporary
        .write_all(&data)
        .and_then(|_| temporary.as_file().sync_all())
        .map_err(|error| format!("failed to write attachment export: {error}"))?;
    temporary
        .persist(&destination)
        .map_err(|error| format!("failed to finish attachment export: {}", error.error))?;
    Ok(())
}

#[tauri::command]
pub fn delete_quick_note_attachment(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_note_attachment(&id)?;
    if deleted {
        emit_changed(&app, "quick-note-attachment")?;
    }
    Ok(deleted)
}

fn attachment_mime_type(name: &str) -> &'static str {
    match PathBuf::from(name)
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default()
        .to_ascii_lowercase()
        .as_str()
    {
        "png" => "image/png",
        "jpg" | "jpeg" => "image/jpeg",
        "gif" => "image/gif",
        "webp" => "image/webp",
        "svg" => "image/svg+xml",
        "pdf" => "application/pdf",
        "json" => "application/json",
        "md" | "txt" | "log" => "text/plain",
        "csv" => "text/csv",
        "zip" => "application/zip",
        _ => "application/octet-stream",
    }
}

#[tauri::command]
pub fn list_board_messages(
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<Vec<BoardMessage>> {
    Ok(repository.list_messages()?)
}

#[tauri::command]
pub fn save_board_message(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    message: BoardMessage,
) -> AppResult<BoardMessage> {
    let saved = repository.save_message(message)?;
    emit_changed(&app, "message-board")?;
    Ok(saved)
}

#[tauri::command]
pub fn delete_board_message(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_message(&id)?;
    if deleted {
        emit_changed(&app, "message-board")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_host_profiles(
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<Vec<HostProfile>> {
    Ok(repository.list_host_profiles()?)
}

#[tauri::command]
pub fn save_host_profile(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    profile: HostProfile,
) -> AppResult<HostProfile> {
    let saved = repository.save_host_profile(profile)?;
    emit_changed(&app, "host")?;
    Ok(saved)
}

#[tauri::command]
pub fn delete_host_profile(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_host_profile(&id)?;
    if deleted {
        emit_changed(&app, "host")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_translation_words(
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<Vec<TranslationWord>> {
    Ok(repository.list_translation_words()?)
}

#[tauri::command]
pub fn save_translation_word(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    word: TranslationWord,
) -> AppResult<TranslationWord> {
    let saved = repository.save_translation_word(word)?;
    emit_changed(&app, "translation-word")?;
    Ok(saved)
}

#[tauri::command]
pub fn delete_translation_word(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_translation_word(&id)?;
    if deleted {
        emit_changed(&app, "translation-word")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_translation_history(
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<Vec<TranslationHistory>> {
    Ok(repository.list_translation_history()?)
}

#[tauri::command]
pub fn delete_translation_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_translation_history(&id)?;
    if deleted {
        emit_changed(&app, "translation-history")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn clear_translation_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<usize> {
    let deleted = repository.clear_translation_history()?;
    if deleted > 0 {
        emit_changed(&app, "translation-history")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn read_system_hosts() -> AppResult<SystemHostsFile> {
    Ok(read_hosts_file(&system_hosts_path())?)
}

#[tauri::command]
pub fn write_system_hosts(
    app: tauri::AppHandle,
    content: String,
    expected_content: String,
) -> AppResult<SystemHostsFile> {
    if content.len() > 2 * 1024 * 1024 {
        return Err("system hosts content cannot exceed 2 MiB".into());
    }
    validate_hosts_content(&content)?;
    let path = system_hosts_path();
    let current = fs::read_to_string(&path)
        .map_err(|error| format!("failed to read system hosts {}: {error}", path.display()))?;
    if current != expected_content {
        return Err("system hosts changed outside MooTool; refresh before applying".into());
    }
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs();
    let backup_dir = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve backup directory: {error}"))?
        .join("hosts-backups");
    fs::create_dir_all(&backup_dir)
        .map_err(|error| format!("failed to create hosts backup directory: {error}"))?;
    fs::write(backup_dir.join(format!("hosts-{timestamp}.bak")), &current)
        .map_err(|error| format!("failed to back up system hosts: {error}"))?;
    fs::write(&path, content)
        .map_err(|error| format!("failed to write system hosts {}: {error}", path.display()))?;
    Ok(read_hosts_file(&path)?)
}

#[tauri::command]
pub async fn resolve_host(host: String) -> AppResult<Vec<String>> {
    let value = host.trim().trim_end_matches('.');
    if value.is_empty()
        || value.len() > 253
        || value.contains('/')
        || value.contains(':')
        || value.chars().any(char::is_whitespace)
    {
        return Err("invalid DNS host name".into());
    }
    let mut addresses = tokio::net::lookup_host((value, 0))
        .await
        .map_err(|error| format!("DNS lookup failed: {error}"))?
        .map(|address| address.ip().to_string())
        .collect::<Vec<_>>();
    addresses.sort();
    addresses.dedup();
    Ok(addresses)
}

fn read_hosts_file(path: &PathBuf) -> Result<SystemHostsFile, String> {
    let content = fs::read_to_string(path)
        .map_err(|error| format!("failed to read system hosts {}: {error}", path.display()))?;
    let writable = fs::OpenOptions::new().write(true).open(path).is_ok();
    Ok(SystemHostsFile {
        path: path.display().to_string(),
        content,
        writable,
    })
}

fn validate_hosts_content(content: &str) -> Result<(), String> {
    for (index, line) in content.lines().enumerate() {
        let value = line.split('#').next().unwrap_or_default().trim();
        if value.is_empty() {
            continue;
        }
        let columns = value.split_whitespace().collect::<Vec<_>>();
        if columns.len() < 2 || columns[0].parse::<std::net::IpAddr>().is_err() {
            return Err(format!("invalid hosts entry on line {}", index + 1));
        }
    }
    Ok(())
}

#[cfg(target_os = "windows")]
fn system_hosts_path() -> PathBuf {
    PathBuf::from(std::env::var("SystemRoot").unwrap_or_else(|_| "C:\\Windows".into()))
        .join("System32/drivers/etc/hosts")
}

#[cfg(not(target_os = "windows"))]
fn system_hosts_path() -> PathBuf {
    PathBuf::from("/etc/hosts")
}

fn emit_changed(app: &tauri::AppHandle, kind: &str) -> Result<(), String> {
    app.emit(LOCAL_DATA_CHANGED_EVENT, kind)
        .map_err(|error| format!("local data was saved but synchronization failed: {error}"))
}
