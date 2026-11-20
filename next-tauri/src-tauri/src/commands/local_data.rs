use std::{
    fs,
    io::Write,
    path::PathBuf,
    time::{SystemTime, UNIX_EPOCH},
};

use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
use tauri::{Emitter, Manager};

use crate::{
    contracts::error::AppResult,
    contracts::local_data::{
        BoardMessage, FavoriteFolder, FuncHistoryRecord, HostProfile, NamedFavorite, QuickNote,
        QuickNoteAttachment, QuickNoteAttachmentDataRequest, QuickNoteAttachmentImportRequest,
        QuickNoteFolder, SaveFuncHistoryInput, SaveHostProfileInput, SaveNamedFavoriteInput,
        SaveTranslationWordInput, SystemHostsFile, ToolFavorite, TranslationHistory,
        TranslationWord,
    },
    repositories::{local_data::LocalDataRepository, settings::SettingsRepository},
};

pub const LOCAL_DATA_CHANGED_EVENT: &str = "mootool://local-data-changed";

#[tauri::command]
pub fn list_quick_notes(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<Vec<QuickNote>> {
    Ok(super::quick_note_files::list_notes(
        &app,
        &repository,
        settings.snapshot().vault.hide_gitignored_files,
    )?)
}

#[tauri::command]
pub fn save_quick_note(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    note: QuickNote,
) -> AppResult<QuickNote> {
    let saved = super::quick_note_files::save_note(&app, &repository, note)?;
    emit_changed(&app, "quick-note")?;
    Ok(saved)
}

#[tauri::command]
pub fn create_quick_note(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    title: String,
    parent_path: String,
    font_name: String,
    font_size: u16,
    line_wrap: bool,
) -> AppResult<QuickNote> {
    let saved = super::quick_note_files::create_note(
        &app,
        &repository,
        &title,
        &parent_path,
        &font_name,
        font_size,
        line_wrap,
    )?;
    emit_changed(&app, "quick-note")?;
    Ok(saved)
}

#[tauri::command]
pub fn duplicate_quick_note(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    relative_path: String,
) -> AppResult<QuickNote> {
    let saved = super::quick_note_files::duplicate_note(&app, &repository, &relative_path)?;
    emit_changed(&app, "quick-note")?;
    Ok(saved)
}

#[tauri::command]
pub fn rename_quick_note_entry(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    relative_path: String,
    name: String,
) -> AppResult<QuickNote> {
    let saved =
        super::quick_note_files::rename_note_entry(&app, &repository, &relative_path, &name)?;
    emit_changed(&app, "quick-note")?;
    Ok(saved)
}

#[tauri::command]
pub fn move_quick_note_entry(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    relative_path: String,
    target_directory: String,
) -> AppResult<QuickNote> {
    let saved = super::quick_note_files::move_note_entry(
        &app,
        &repository,
        &relative_path,
        &target_directory,
    )?;
    emit_changed(&app, "quick-note")?;
    Ok(saved)
}

#[tauri::command]
pub fn open_quick_note_vault(app: tauri::AppHandle) -> AppResult<()> {
    let vault = super::quick_note_files::open_vault(&app)?;
    let path = vault.root();
    if cfg!(target_os = "windows") {
        std::process::Command::new("explorer")
            .arg(path)
            .spawn()
            .map_err(|error| format!("failed to open quick note vault: {error}"))?;
        return Ok(());
    }
    let status = if cfg!(target_os = "macos") {
        std::process::Command::new("open").arg(path).status()
    } else {
        std::process::Command::new("xdg-open").arg(path).status()
    }
    .map_err(|error| format!("failed to open quick note vault: {error}"))?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("failed to open quick note vault: {status}").into())
    }
}

#[tauri::command]
pub fn export_quick_note_files(
    app: tauri::AppHandle,
    directory: String,
    relative_paths: Vec<String>,
) -> AppResult<usize> {
    let count = super::quick_note_files::export_quick_note_files(
        &app,
        std::path::Path::new(directory.trim()),
        &relative_paths,
    )?;
    Ok(count)
}

#[tauri::command]
pub fn reveal_quick_note_entry(app: tauri::AppHandle, relative_path: String) -> AppResult<()> {
    let vault = super::quick_note_files::open_vault(&app)?;
    let resolved = if let Ok(target) = vault.resolve_entry(&relative_path) {
        target
    } else {
        let file = vault
            .note_files()?
            .into_iter()
            .find(|file| file.metadata.id == relative_path || file.relative_path == relative_path)
            .ok_or_else(|| "Vault entry does not exist".to_string())?;
        vault.resolve_entry(&file.relative_path)?
    };
    reveal_in_file_manager(&resolved)
}

fn reveal_in_file_manager(path: &std::path::Path) -> AppResult<()> {
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
        .map_err(|error| format!("failed to reveal vault entry: {error}"))?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("failed to reveal vault entry: {status}").into())
    }
}

#[tauri::command]
pub fn delete_quick_note(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = super::quick_note_files::delete_note(&app, &repository, &id)?;
    if deleted {
        emit_changed(&app, "quick-note")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_quick_note_folders(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<Vec<QuickNoteFolder>> {
    Ok(super::quick_note_files::list_folders(
        &app,
        &repository,
        settings.snapshot().vault.hide_gitignored_files,
    )?)
}

#[tauri::command]
pub fn save_quick_note_folder(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    folder: QuickNoteFolder,
) -> AppResult<QuickNoteFolder> {
    let saved = super::quick_note_files::save_folder(&app, &repository, folder)?;
    emit_changed(&app, "quick-note-folder")?;
    Ok(saved)
}

#[tauri::command]
pub fn rename_quick_note_folder(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    path: String,
    next_path: String,
    updated_at: i64,
) -> AppResult<Vec<QuickNoteFolder>> {
    let renamed =
        super::quick_note_files::rename_folder(&app, &repository, &path, &next_path, updated_at)?;
    emit_changed(&app, "quick-note-folder")?;
    Ok(renamed)
}

#[tauri::command]
pub fn delete_quick_note_folder(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    path: String,
) -> AppResult<usize> {
    let moved = super::quick_note_files::delete_folder(&app, &repository, &path, now_millis())?;
    emit_changed(&app, "quick-note-folder")?;
    Ok(moved)
}

#[tauri::command]
pub fn list_tool_favorites(
    repository: tauri::State<'_, LocalDataRepository>,
    tool_id: String,
) -> AppResult<Vec<ToolFavorite>> {
    Ok(repository.list_tool_favorites(&tool_id)?)
}

#[tauri::command]
pub fn save_tool_favorite(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    favorite: ToolFavorite,
) -> AppResult<ToolFavorite> {
    let saved = repository.save_tool_favorite(favorite)?;
    emit_changed(&app, "tool-favorite")?;
    Ok(saved)
}

#[tauri::command]
pub fn delete_tool_favorite(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_tool_favorite(&id)?;
    if deleted {
        emit_changed(&app, "tool-favorite")?;
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
    if metadata.len() > 20 * 1024 * 1024 {
        return Err("Attachment exceeds 20 MB limit".into());
    }
    let name = path
        .file_name()
        .and_then(|value| value.to_str())
        .ok_or_else(|| "attachment filename must be valid UTF-8".to_string())?
        .to_string();
    let data = fs::read(&path).map_err(|error| format!("failed to read attachment: {error}"))?;
    let extension = PathBuf::from(&name)
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default()
        .to_string();
    let saved = store_vault_attachment(
        &app,
        &repository,
        &request.id,
        &request.note_id,
        &extension,
        request.created_at,
        &data,
    )?;
    emit_changed(&app, "quick-note-attachment")?;
    Ok(saved)
}

#[tauri::command]
pub fn import_quick_note_attachment_data(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    request: QuickNoteAttachmentDataRequest,
) -> AppResult<QuickNoteAttachment> {
    if request.data_base64.len() > 28 * 1024 * 1024 {
        return Err("Attachment exceeds 20 MB limit".into());
    }
    let data = BASE64
        .decode(&request.data_base64)
        .map_err(|_| "attachment data is not valid Base64".to_string())?;
    if data.is_empty() || data.len() > 20 * 1024 * 1024 {
        return Err("Attachment exceeds 20 MB limit".into());
    }
    let expected_mime_type = attachment_mime_type(&request.name);
    if !request.mime_type.starts_with("image/") || expected_mime_type != request.mime_type {
        return Err("Unsupported image attachment".into());
    }
    validate_image_attachment_signature(&request.mime_type, &data)?;
    let extension = PathBuf::from(&request.name)
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default()
        .to_string();
    let saved = store_vault_attachment(
        &app,
        &repository,
        &request.id,
        &request.note_id,
        &extension,
        request.created_at,
        &data,
    )?;
    emit_changed(&app, "quick-note-attachment")?;
    Ok(saved)
}

fn store_vault_attachment(
    app: &tauri::AppHandle,
    repository: &LocalDataRepository,
    id: &str,
    note_id: &str,
    extension: &str,
    created_at: i64,
    data: &[u8],
) -> Result<QuickNoteAttachment, String> {
    let vault = super::quick_note_files::open_vault(app)?;
    let stored = vault.store_attachment(data, extension)?;
    let attachment = QuickNoteAttachment {
        id: id.into(),
        note_id: note_id.into(),
        name: stored.name,
        mime_type: stored.mime_type.clone(),
        size_bytes: stored.size_bytes,
        created_at,
        relative_path: stored.relative_path.clone(),
        markdown: stored.markdown,
        data_url: format!("data:{};base64,{}", stored.mime_type, BASE64.encode(data)),
    };
    match repository.save_note_attachment(attachment.clone(), data) {
        Ok(saved) => Ok(saved),
        Err(error) => {
            let _ = vault.delete_attachment(&stored.relative_path);
            Err(error)
        }
    }
}

fn validate_image_attachment_signature(mime_type: &str, data: &[u8]) -> Result<(), String> {
    let valid = match mime_type {
        "image/png" => data.starts_with(b"\x89PNG\r\n\x1a\n"),
        "image/jpeg" => data.starts_with(&[0xff, 0xd8, 0xff]),
        "image/gif" => data.starts_with(b"GIF87a") || data.starts_with(b"GIF89a"),
        "image/webp" => data.starts_with(b"RIFF") && data.get(8..12) == Some(b"WEBP"),
        "image/bmp" => data.starts_with(b"BM"),
        _ => false,
    };
    valid
        .then_some(())
        .ok_or_else(|| "image attachment bytes do not match the declared format".into())
}

#[tauri::command]
pub fn export_quick_note_attachment(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
    destination_path: String,
) -> AppResult<()> {
    let (attachment, sqlite_data) = repository.note_attachment_data(&id)?;
    let data = super::quick_note_files::open_vault(&app)
        .ok()
        .and_then(|vault| {
            vault
                .read_attachment(&format!("attachments/{}", attachment.name))
                .ok()
        })
        .unwrap_or(sqlite_data);
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
    let existing = repository.note_attachment_data(&id).ok();
    let deleted = repository.delete_note_attachment(&id)?;
    if deleted {
        if let Some((attachment, _)) = existing
            && let Ok(vault) = super::quick_note_files::open_vault(&app)
        {
            let _ = vault.delete_attachment(&format!("attachments/{}", attachment.name));
        }
        emit_changed(&app, "quick-note-attachment")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn read_quick_note_attachment_file(
    app: tauri::AppHandle,
    relative_path: String,
) -> AppResult<String> {
    let vault = super::quick_note_files::open_vault(&app)?;
    let data = vault.read_attachment(&relative_path)?;
    let extension = PathBuf::from(&relative_path)
        .extension()
        .and_then(|value| value.to_str())
        .map(|value| format!(".{value}"))
        .unwrap_or_default();
    let mime = crate::repositories::quick_note_vault::image_mime(&extension);
    Ok(format!("data:{mime};base64,{}", BASE64.encode(data)))
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
        "bmp" => "image/bmp",
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

fn now_millis() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(i64::MAX)
}

#[tauri::command]
pub fn list_func_history(
    repository: tauri::State<'_, LocalDataRepository>,
    func_type: String,
    keyword: Option<String>,
) -> AppResult<Vec<FuncHistoryRecord>> {
    Ok(repository.list_func_history(&func_type, keyword.as_deref().unwrap_or(""))?)
}

#[tauri::command]
pub fn save_func_history(
    repository: tauri::State<'_, LocalDataRepository>,
    input: SaveFuncHistoryInput,
) -> AppResult<Option<FuncHistoryRecord>> {
    Ok(repository.save_func_history(input)?)
}

#[tauri::command]
pub fn delete_func_history(
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<bool> {
    Ok(repository.delete_func_history(id)?)
}

#[tauri::command]
pub fn clear_func_history(
    repository: tauri::State<'_, LocalDataRepository>,
    func_type: String,
) -> AppResult<usize> {
    Ok(repository.clear_func_history(&func_type)?)
}

#[tauri::command]
pub fn list_favorite_folders(
    repository: tauri::State<'_, LocalDataRepository>,
    kind: String,
) -> AppResult<Vec<FavoriteFolder>> {
    Ok(repository.list_favorite_folders(&kind)?)
}

#[tauri::command]
pub fn create_favorite_folder(
    repository: tauri::State<'_, LocalDataRepository>,
    kind: String,
    title: String,
) -> AppResult<FavoriteFolder> {
    Ok(repository.create_favorite_folder(&kind, &title)?)
}

#[tauri::command]
pub fn rename_favorite_folder(
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
    title: String,
) -> AppResult<FavoriteFolder> {
    Ok(repository.rename_favorite_folder(id, &title)?)
}

#[tauri::command]
pub fn delete_favorite_folder(
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<()> {
    Ok(repository.delete_favorite_folder(id)?)
}

#[tauri::command]
pub fn list_named_favorites(
    repository: tauri::State<'_, LocalDataRepository>,
    kind: String,
    folder_id: Option<i64>,
) -> AppResult<Vec<NamedFavorite>> {
    Ok(repository.list_named_favorites(&kind, folder_id)?)
}

#[tauri::command]
pub fn save_named_favorite(
    repository: tauri::State<'_, LocalDataRepository>,
    input: SaveNamedFavoriteInput,
) -> AppResult<NamedFavorite> {
    Ok(repository.save_named_favorite(input)?)
}

#[tauri::command]
pub fn delete_named_favorite(
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<bool> {
    Ok(repository.delete_named_favorite(id)?)
}

#[tauri::command]
pub fn move_named_favorite(
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
    direction: i64,
) -> AppResult<()> {
    Ok(repository.move_named_favorite(id, direction)?)
}

#[tauri::command]
pub fn get_tool_draft(
    repository: tauri::State<'_, LocalDataRepository>,
    kind: String,
) -> AppResult<Option<String>> {
    Ok(repository.get_tool_draft(&kind)?)
}

#[tauri::command]
pub fn save_tool_draft(
    repository: tauri::State<'_, LocalDataRepository>,
    kind: String,
    value: String,
) -> AppResult<()> {
    Ok(repository.save_tool_draft(&kind, &value)?)
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
    keyword: Option<String>,
    include_content: Option<bool>,
) -> AppResult<Vec<HostProfile>> {
    Ok(repository.list_host_profiles(
        keyword.as_deref().unwrap_or(""),
        include_content.unwrap_or(true),
    )?)
}

#[tauri::command]
pub fn save_host_profile(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    profile: SaveHostProfileInput,
) -> AppResult<HostProfile> {
    let saved = repository.save_host_profile(profile)?;
    emit_changed(&app, "host")?;
    let _ = crate::commands::desktop::refresh_tray(&app);
    Ok(saved)
}

#[tauri::command]
pub fn delete_host_profile(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<bool> {
    let deleted = repository.delete_host_profile(id)?;
    if deleted {
        emit_changed(&app, "host")?;
        let _ = crate::commands::desktop::refresh_tray(&app);
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_translation_words(
    repository: tauri::State<'_, LocalDataRepository>,
    keyword: Option<String>,
) -> AppResult<Vec<TranslationWord>> {
    Ok(repository.list_translation_words(keyword.as_deref().unwrap_or(""))?)
}

#[tauri::command]
pub fn save_translation_word(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    word: SaveTranslationWordInput,
) -> AppResult<TranslationWord> {
    let saved = repository.save_translation_word(word)?;
    emit_changed(&app, "translation-word")?;
    Ok(saved)
}

#[tauri::command]
pub fn delete_translation_word(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<bool> {
    let deleted = repository.delete_translation_word(id)?;
    if deleted {
        emit_changed(&app, "translation-word")?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn list_translation_history(
    repository: tauri::State<'_, LocalDataRepository>,
    keyword: Option<String>,
) -> AppResult<Vec<TranslationHistory>> {
    Ok(repository.list_translation_history(keyword.as_deref().unwrap_or(""))?)
}

#[tauri::command]
pub fn delete_translation_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: i64,
) -> AppResult<bool> {
    let deleted = repository.delete_translation_history(id)?;
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
    active_host_id: Option<i64>,
) -> AppResult<SystemHostsFile> {
    let _ = expected_content;
    let normalized = normalize_hosts_content(&content)?;
    let path = system_hosts_path();
    if let Ok(current) = fs::read_to_string(&path) {
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
    }
    match fs::write(&path, &normalized) {
        Ok(()) => {}
        Err(error) if error.kind() == std::io::ErrorKind::PermissionDenied => {
            write_hosts_elevated(&path, &normalized)?;
        }
        Err(error) => {
            return Err(format!("failed to write system hosts {}: {error}", path.display()).into());
        }
    }
    let written = read_hosts_file(&path)?;
    crate::commands::desktop::remember_active_host(&app, active_host_id)?;
    let _ = crate::commands::desktop::refresh_tray(&app);
    Ok(written)
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

fn normalize_hosts_content(value: &str) -> Result<String, String> {
    if value.len() > 2 * 1024 * 1024 || value.contains('\0') {
        return Err("Invalid hosts content".into());
    }
    let normalized = value.replace("\r\n", "\n").replace('\r', "\n");
    Ok(format!("{}\n", normalized.trim_end_matches('\n')))
}

fn write_hosts_elevated(destination: &std::path::Path, content: &str) -> Result<(), String> {
    let temporary = std::env::temp_dir().join(format!(
        "mootool-hosts-{}-{}",
        std::process::id(),
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis()
    ));
    fs::write(&temporary, content).map_err(|error| error.to_string())?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(&temporary, fs::Permissions::from_mode(0o600))
            .map_err(|error| error.to_string())?;
    }
    let result = run_elevated_hosts_copy(&temporary, destination);
    let _ = fs::remove_file(temporary);
    result
}

fn run_elevated_hosts_copy(
    temporary: &std::path::Path,
    destination: &std::path::Path,
) -> Result<(), String> {
    let temp = temporary.to_string_lossy().to_string();
    let dest = destination.to_string_lossy().to_string();
    let mut command = if cfg!(target_os = "macos") {
        let copy = format!(
            "/bin/cp {} {} && /usr/bin/chmod 644 {} && /usr/bin/dscacheutil -flushcache",
            shell_quote(&temp),
            shell_quote(&dest),
            shell_quote(&dest)
        );
        let mut command = std::process::Command::new("/usr/bin/osascript");
        command.args([
            "-e",
            &format!(
                "do shell script {} with administrator privileges",
                apple_script_string(&copy)
            ),
        ]);
        command
    } else if cfg!(windows) {
        let script = format!(
            "Copy-Item -LiteralPath '{}' -Destination '{}' -Force",
            ps_quote(&temp),
            ps_quote(&dest)
        );
        let mut command = std::process::Command::new("powershell.exe");
        command.args([
            "-NoProfile",
            "-Command",
            &format!(
                "Start-Process powershell.exe -Verb RunAs -Wait -ArgumentList @('-NoProfile','-Command',{})",
                ps_string(&script)
            ),
        ]);
        command
    } else {
        let mut command = std::process::Command::new("pkexec");
        command.args([
            "/bin/sh",
            "-c",
            "/bin/cp \"$1\" \"$2\" && /bin/chmod 644 \"$2\"",
            "mootool",
            &temp,
            &dest,
        ]);
        command
    };
    let mut child = command.spawn().map_err(|error| error.to_string())?;
    let started = std::time::Instant::now();
    let status = loop {
        match child.try_wait().map_err(|error| error.to_string())? {
            Some(status) => break status,
            None if started.elapsed() >= std::time::Duration::from_secs(120) => {
                let _ = child.kill();
                return Err("TIMEOUT".into());
            }
            None => std::thread::sleep(std::time::Duration::from_millis(50)),
        }
    };
    if status.success() {
        Ok(())
    } else {
        Err(format!("administrator approval failed ({status})"))
    }
}

fn shell_quote(value: &str) -> String {
    format!("'{}'", value.replace('\'', "'\"'\"'"))
}

fn apple_script_string(value: &str) -> String {
    format!("\"{}\"", value.replace('\\', "\\\\").replace('"', "\\\""))
}

fn ps_quote(value: &str) -> String {
    value.replace('\'', "''")
}

fn ps_string(value: &str) -> String {
    format!("'{}'", ps_quote(value))
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

#[cfg(test)]
mod tests {
    use super::normalize_hosts_content;

    #[test]
    fn normalizes_hosts_content_like_electron() {
        assert_eq!(
            normalize_hosts_content("a\r\nb\n\n").expect("hosts"),
            "a\nb\n"
        );
        assert_eq!(
            normalize_hosts_content("127.0.0.1 localhost extra").expect("hosts"),
            "127.0.0.1 localhost extra\n"
        );
        assert_eq!(normalize_hosts_content("").expect("empty"), "\n");
        assert!(normalize_hosts_content("bad\0line").is_err());
    }
}
