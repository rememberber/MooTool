use std::{fs, path::PathBuf, time::SystemTime};

use tauri::Manager;

use crate::{
    contracts::local_data::{QuickNote, QuickNoteFolder},
    repositories::{
        local_data::LocalDataRepository,
        quick_note_vault::{QuickNoteFile, QuickNoteMetadata, QuickNoteVault},
    },
};

const IMPORT_MARKER: &str = ".mootool-sqlite-imported";

pub fn open_vault(app: &tauri::AppHandle) -> Result<QuickNoteVault, String> {
    let root = if let Some(config) = commands_acceptance_root(app) {
        config.join("quick-notes")
    } else {
        app.path()
            .app_data_dir()
            .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?
            .join("quick-notes")
    };
    QuickNoteVault::open(root)
}

fn commands_acceptance_root(app: &tauri::AppHandle) -> Option<PathBuf> {
    app.try_state::<crate::commands::native_acceptance::NativeAcceptanceConfig>()
        .map(|config| config.data_root.clone())
}

pub fn list_notes(app: &tauri::AppHandle, sqlite: &LocalDataRepository, hide_ignored: bool) -> Result<Vec<QuickNote>, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let patterns = if hide_ignored { gitignore_patterns(vault.root()) } else { Vec::new() };
    let mut notes = vault
        .note_files()?
        .into_iter()
        .filter(|file| !hide_ignored || !path_ignored(&file.relative_path, false, &patterns))
        .map(note_from_file)
        .collect::<Vec<_>>();
    notes.sort_by(|left, right| right.pinned.cmp(&left.pinned).then(right.updated_at.cmp(&left.updated_at)));
    Ok(notes)
}

pub fn save_note(app: &tauri::AppHandle, sqlite: &LocalDataRepository, note: QuickNote) -> Result<QuickNote, String> {
    note.validate()?;
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let previous = vault
        .note_files()?
        .into_iter()
        .find(|file| file.metadata.id == note.id);
    let previous_attachments = previous.as_ref().map(|file| attachment_references(&file.content)).unwrap_or_default();
    let relative = saved_note_path(&note, previous.as_ref().map(|file| file.relative_path.as_str()));
    if let Some(previous) = &previous {
        if previous.relative_path != relative && vault.root().join(&relative).exists() {
            return Err("An entry with that name already exists".into());
        }
        if previous.relative_path != relative {
            let _ = vault.delete_entry(&previous.relative_path);
        }
    }
    let saved = vault.save(&QuickNoteFile {
        relative_path: relative,
        content: note.content.clone(),
        metadata: metadata_from_note(&note),
    })?;
    let mapped = note_from_file(saved);
    let current_attachments = attachment_references(&note.content);
    let removed = previous_attachments
        .into_iter()
        .filter(|path| !current_attachments.iter().any(|current| current == path))
        .collect::<Vec<_>>();
    remove_unreferenced_attachments(&vault, &removed)?;
    sqlite.save_note(mapped.clone())?;
    Ok(mapped)
}

pub fn create_note(
    app: &tauri::AppHandle,
    sqlite: &LocalDataRepository,
    title: &str,
    parent: &str,
    font_name: &str,
    font_size: u16,
    line_wrap: bool,
) -> Result<QuickNote, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let saved = vault.create(title, parent, font_name, font_size, line_wrap)?;
    let mapped = note_from_file(saved);
    sqlite.save_note(mapped.clone())?;
    Ok(mapped)
}

pub fn duplicate_note(app: &tauri::AppHandle, sqlite: &LocalDataRepository, relative_path: &str) -> Result<QuickNote, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let saved = vault.duplicate(relative_path)?;
    let mapped = note_from_file(saved);
    sqlite.save_note(mapped.clone())?;
    Ok(mapped)
}

pub fn rename_note_entry(
    app: &tauri::AppHandle,
    sqlite: &LocalDataRepository,
    relative_path: &str,
    name: &str,
) -> Result<QuickNote, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let next_path = vault.rename_entry(relative_path, name)?;
    let mapped = note_from_file(vault.read(&next_path)?);
    sqlite.save_note(mapped.clone())?;
    Ok(mapped)
}

pub fn move_note_entry(
    app: &tauri::AppHandle,
    sqlite: &LocalDataRepository,
    relative_path: &str,
    target_directory: &str,
) -> Result<QuickNote, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let next_path = vault.move_entry(relative_path, target_directory)?;
    let mapped = note_from_file(vault.read(&next_path)?);
    sqlite.save_note(mapped.clone())?;
    Ok(mapped)
}

pub fn delete_note(app: &tauri::AppHandle, sqlite: &LocalDataRepository, id: &str) -> Result<bool, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let existing = vault.note_files()?.into_iter().find(|file| file.metadata.id == id);
    let removed_file = existing.is_some();
    let attachments = existing.as_ref().map(|file| attachment_references(&file.content)).unwrap_or_default();
    if let Some(existing) = existing {
        vault.delete_entry(&existing.relative_path)?;
    }
    remove_unreferenced_attachments(&vault, &attachments)?;
    Ok(sqlite.delete_note(id)? || removed_file)
}

pub fn list_folders(app: &tauri::AppHandle, sqlite: &LocalDataRepository, hide_ignored: bool) -> Result<Vec<QuickNoteFolder>, String> {
    let vault = open_vault(app)?;
    import_sqlite_notes(&vault, sqlite)?;
    let patterns = if hide_ignored { gitignore_patterns(vault.root()) } else { Vec::new() };
    let now = now_millis();
    Ok(vault
        .directories()?
        .into_iter()
        .filter(|path| !hide_ignored || !path_ignored(path, true, &patterns))
        .map(|path| QuickNoteFolder {
            path,
            created_at: now,
            updated_at: now,
        })
        .collect())
}

pub fn save_folder(app: &tauri::AppHandle, sqlite: &LocalDataRepository, folder: QuickNoteFolder) -> Result<QuickNoteFolder, String> {
    folder.validate()?;
    let vault = open_vault(app)?;
    vault.create_folder(&folder.path)?;
    sqlite.save_note_folder(folder.clone())?;
    Ok(folder)
}

pub fn rename_folder(
    app: &tauri::AppHandle,
    sqlite: &LocalDataRepository,
    path: &str,
    next_path: &str,
    updated_at: i64,
) -> Result<Vec<QuickNoteFolder>, String> {
    let vault = open_vault(app)?;
    let source = vault.root().join(path);
    let destination = vault.root().join(next_path);
    if source.exists() {
        if let Some(parent) = destination.parent() {
            fs::create_dir_all(parent).map_err(|error| format!("failed to create renamed folder parent: {error}"))?;
        }
        fs::rename(&source, &destination).map_err(|error| format!("failed to rename note folder: {error}"))?;
    }
    sqlite.rename_note_folder(path, next_path, updated_at)
}

pub fn delete_folder(app: &tauri::AppHandle, sqlite: &LocalDataRepository, path: &str, updated_at: i64) -> Result<usize, String> {
    let vault = open_vault(app)?;
    let files = vault.note_files()?;
    let mut attachments = Vec::new();
    let mut removed = 0usize;
    for file in &files {
        let folder = parent_folder(&file.relative_path);
        if folder == path || folder.starts_with(&format!("{path}/")) {
            removed += 1;
            attachments.extend(attachment_references(&file.content));
        }
    }
    let directory = vault.root().join(path);
    if directory.exists() {
        fs::remove_dir_all(&directory).map_err(|error| format!("failed to delete note folder: {error}"))?;
    }
    let deleted = sqlite.delete_note_folder(path, updated_at)?;
    remove_unreferenced_attachments(&vault, &attachments)?;
    Ok(removed.max(deleted))
}

fn attachment_references(content: &str) -> Vec<String> {
    let mut paths = Vec::new();
    let mut rest = content;
    while let Some(start) = rest.find("attachments/") {
        let candidate = &rest[start..];
        let end = candidate
            .find(|character: char| !character.is_ascii_alphanumeric() && character != '/' && character != '_' && character != '.' && character != '-')
            .unwrap_or(candidate.len());
        let path = candidate[..end].trim_end_matches('.');
        if path.matches('/').count() == 1 && path.len() > "attachments/".len() {
            paths.push(path.to_string());
        }
        rest = &candidate[end.max(1)..];
    }
    paths
}

fn remove_unreferenced_attachments(vault: &QuickNoteVault, candidates: &[String]) -> Result<(), String> {
    if candidates.is_empty() {
        return Ok(());
    }
    let mut referenced = std::collections::HashSet::new();
    for file in vault.note_files()? {
        referenced.extend(attachment_references(&file.content));
    }
    for candidate in candidates {
        if referenced.contains(candidate) {
            continue;
        }
        let _ = vault.delete_attachment(candidate);
    }
    Ok(())
}

fn import_sqlite_notes(vault: &QuickNoteVault, sqlite: &LocalDataRepository) -> Result<(), String> {
    let marker = vault.root().join(IMPORT_MARKER);
    if marker.exists() {
        return Ok(());
    }
    for folder in sqlite.list_note_folders()? {
        vault.create_folder(&folder.path)?;
    }
    for note in sqlite.list_notes()? {
        vault.save(&QuickNoteFile {
            relative_path: note_path(&note),
            content: note.content.clone(),
            metadata: metadata_from_note(&note),
        })?;
    }
    fs::write(marker, b"imported\n").map_err(|error| format!("failed to record note import: {error}"))
}

fn note_from_file(file: QuickNoteFile) -> QuickNote {
    let folder_path = parent_folder(&file.relative_path);
    QuickNote {
        id: if file.metadata.id.is_empty() { file.relative_path.clone() } else { file.metadata.id },
        title: file.metadata.title,
        content: file.content,
        tags: file.metadata.tags,
        color: file.metadata.color,
        folder_path,
        relative_path: file.relative_path,
        editor_font: file.metadata.editor_font,
        line_height: line_height_for_spacing(file.metadata.line_spacing),
        font_size: file.metadata.font_size,
        font_name: file.metadata.font_name,
        line_spacing: file.metadata.line_spacing,
        line_wrapping: file.metadata.line_wrap,
        syntax: file_syntax(&file.metadata.syntax),
        pinned: file.metadata.pinned,
        created_at: parse_millis(&file.metadata.created_at),
        updated_at: parse_millis(&file.metadata.modified_at),
    }
}

fn metadata_from_note(note: &QuickNote) -> QuickNoteMetadata {
    QuickNoteMetadata {
        id: note.id.clone(),
        title: note.title.clone(),
        style: String::new(),
        syntax: file_syntax(&note.syntax),
        font_name: note.font_name.clone(),
        font_size: note.font_size,
        line_spacing: note.line_spacing,
        color: note.color.clone(),
        line_wrap: note.line_wrapping,
        created_at: format_millis(note.created_at),
        modified_at: format_millis(note.updated_at),
        tags: note.tags.clone(),
        pinned: note.pinned,
        editor_font: note.editor_font.clone(),
        line_height: note.line_height.clone(),
    }
}

fn line_height_for_spacing(spacing: f32) -> String {
    if spacing <= 1.05 {
        "compact"
    } else if spacing <= 1.45 {
        "normal"
    } else {
        "relaxed"
    }
    .into()
}

fn note_path(note: &QuickNote) -> String {
    let extension = extension_for_syntax(&note.syntax);
    let title = safe_title(&note.title);
    let file_name = format!("{title}.{extension}");
    if note.folder_path.is_empty() { file_name } else { format!("{}/{}", note.folder_path, file_name) }
}

fn saved_note_path(note: &QuickNote, previous: Option<&str>) -> String {
    let extension = extension_for_syntax(&note.syntax);
    let existing = if note.relative_path.is_empty() { previous.unwrap_or("") } else { note.relative_path.as_str() };
    if existing.is_empty() {
        return note_path(note);
    }
    let leaf = existing.rsplit_once('/').map(|(_, name)| name).unwrap_or(existing);
    let parent = existing.rsplit_once('/').map(|(directory, _)| directory).unwrap_or("");
    let placed = if note.folder_path == parent {
        existing.to_string()
    } else if note.folder_path.is_empty() {
        leaf.to_string()
    } else {
        format!("{}/{leaf}", note.folder_path)
    };
    with_syntax_extension(&placed, extension)
}

fn with_syntax_extension(path: &str, extension: &str) -> String {
    let (directory, leaf) = match path.rsplit_once('/') {
        Some((directory, leaf)) => (Some(directory), leaf),
        None => (None, path),
    };
    let stem = leaf.rsplit_once('.').map(|(stem, _)| stem).filter(|stem| !stem.is_empty()).unwrap_or(leaf);
    let file_name = format!("{stem}.{extension}");
    match directory {
        Some(directory) => format!("{directory}/{file_name}"),
        None => file_name,
    }
}

fn safe_title(title: &str) -> String {
    let sanitized: String = title
        .trim()
        .chars()
        .map(|char| if matches!(char, '\\' | '/' | ':' | '*' | '?' | '"' | '<' | '>' | '|') { '_' } else { char })
        .collect();
    let sanitized = sanitized.trim();
    if sanitized.is_empty() { "note".to_string() } else { sanitized.to_string() }
}

pub fn export_quick_note_files(app: &tauri::AppHandle, directory: &std::path::Path, relative_paths: &[String]) -> Result<usize, String> {
    if !directory.is_dir() {
        return Err("Quick Note export directory does not exist".into());
    }
    if relative_paths.is_empty() || relative_paths.len() > 500 {
        return Err("Invalid vault export".into());
    }
    let vault = open_vault(app)?;
    let mut written = 0usize;
    let mut seen = std::collections::HashSet::new();
    for relative_path in relative_paths {
        if !seen.insert(relative_path.clone()) {
            continue;
        }
        let file = vault.read(relative_path)?;
        write_exported_quick_note(directory, &file.relative_path, &file.content)?;
        written += 1;
    }
    Ok(written)
}

pub fn write_exported_quick_note(directory: &std::path::Path, relative_path: &str, content: &str) -> Result<(), String> {
    let base = quick_note_export_base(relative_path);
    for suffix in 0..10_000 {
        let name = if suffix == 0 {
            format!("{base}.txt")
        } else {
            format!("{base}-{suffix}.txt")
        };
        let target = directory.join(&name);
        match fs::OpenOptions::new().write(true).create_new(true).open(&target) {
            Ok(mut file) => {
                use std::io::Write;
                file.write_all(content.as_bytes())
                    .map_err(|error| format!("failed to export {}: {error}", target.display()))?;
                return Ok(());
            }
            Err(error) if error.kind() == std::io::ErrorKind::AlreadyExists => continue,
            Err(error) => return Err(format!("failed to export {}: {error}", target.display())),
        }
    }
    Err("too many export name collisions".into())
}

fn quick_note_export_base(relative_path: &str) -> String {
    let path = relative_path.replace('\\', "/").replace('/', "-");
    let stem = strip_note_export_extension(&path);
    let sanitized: String = stem
        .chars()
        .map(|character| {
            if matches!(character, '<' | '>' | ':' | '"' | '/' | '\\' | '|' | '?' | '*') || character.is_ascii_control() {
                '_'
            } else {
                character
            }
        })
        .collect();
    let sanitized = sanitized.trim();
    if sanitized.is_empty() { "untitled".to_string() } else { sanitized.to_string() }
}

fn strip_note_export_extension(name: &str) -> &str {
    let lower = name.to_ascii_lowercase();
    if let Some(stem) = lower.strip_suffix(".json").or_else(|| lower.strip_suffix(".txt")) {
        &name[..stem.len()]
    } else {
        name
    }
}

pub fn gitignore_patterns(root: &std::path::Path) -> Vec<String> {
    fs::read_to_string(root.join(".gitignore"))
        .unwrap_or_default()
        .lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(str::to_string)
        .collect()
}

pub fn path_ignored(relative: &str, directory: bool, patterns: &[String]) -> bool {
    patterns.iter().any(|pattern| gitignore_match(relative, directory, pattern))
}

fn gitignore_match(relative: &str, directory: bool, pattern: &str) -> bool {
    let directory_pattern = pattern.ends_with('/');
    let pattern = pattern.trim_end_matches('/');
    if directory_pattern {
        return relative == pattern || relative.starts_with(&format!("{pattern}/"));
    }
    if !pattern.contains('/') {
        return relative.split('/').any(|part| glob_match(part, pattern));
    }
    if directory {
        return glob_match(relative, pattern) || relative.split('/').any(|part| glob_match(part, pattern.rsplit('/').next().unwrap_or(pattern)));
    }
    glob_match(relative, pattern)
}

fn glob_match(text: &str, pattern: &str) -> bool {
    fn rec(text: &[u8], pattern: &[u8]) -> bool {
        if pattern.is_empty() { return text.is_empty(); }
        if pattern.len() >= 2 && pattern[0] == b'*' && pattern[1] == b'*' {
            let rest = pattern[2..].strip_prefix(b"/").unwrap_or(&pattern[2..]);
            return (0..=text.len()).any(|index| rec(&text[index..], rest));
        }
        if pattern[0] == b'*' {
            return (0..=text.iter().position(|byte| *byte == b'/').unwrap_or(text.len()))
                .any(|index| rec(&text[index..], &pattern[1..]));
        }
        if text.is_empty() { return false; }
        if pattern[0] == b'?' || pattern[0] == text[0] {
            return rec(&text[1..], &pattern[1..]);
        }
        false
    }
    rec(text.as_bytes(), pattern.as_bytes())
}

fn parent_folder(relative_path: &str) -> String {
    match relative_path.rsplit_once('/') {
        Some((folder, _)) => folder.to_string(),
        None => String::new(),
    }
}

fn extension_for_syntax(syntax: &str) -> &'static str {
    match syntax {
        "markdown" | "text/markdown" => "md",
        "json" | "application/json" => "json",
        "java" | "text/java" => "java",
        "javascript" | "text/javascript" => "js",
        "typescript" | "text/typescript" => "ts",
        "python" | "text/python" => "py",
        "html" | "text/html" => "html",
        "xml" | "text/xml" => "xml",
        "yaml" | "text/yaml" => "yaml",
        "sql" | "text/sql" => "sql",
        _ => "txt",
    }
}

fn file_syntax(syntax: &str) -> String {
    match syntax {
        "markdown" | "text/markdown" => "text/markdown".into(),
        "json" | "application/json" => "application/json".into(),
        "java" | "text/java" => "text/java".into(),
        "javascript" | "text/javascript" => "text/javascript".into(),
        "typescript" | "text/typescript" => "text/typescript".into(),
        "python" | "text/python" => "text/python".into(),
        "html" | "text/html" => "text/html".into(),
        "xml" | "text/xml" => "text/xml".into(),
        "yaml" | "text/yaml" => "text/yaml".into(),
        "sql" | "text/sql" => "text/sql".into(),
        _ => "text/plain".into(),
    }
}

fn format_millis(millis: i64) -> String {
    let seconds = millis.div_euclid(1000);
    let fraction = millis.rem_euclid(1000) as u32;
    let Ok(time) = time::OffsetDateTime::from_unix_timestamp(seconds) else {
        return "1970-01-01T00:00:00.000Z".into();
    };
    let (year, month, day) = time.date().to_calendar_date();
    format!(
        "{year:04}-{:02}-{day:02}T{:02}:{:02}:{:02}.{fraction:03}Z",
        u8::from(month),
        time.hour(),
        time.minute(),
        time.second()
    )
}

fn parse_millis(value: &str) -> i64 {
    if value.len() >= 19 && value.as_bytes().get(4) == Some(&b'-') {
        let year = value[0..4].parse().unwrap_or(1970);
        let month = value[5..7].parse::<u8>().unwrap_or(1);
        let day = value[8..10].parse().unwrap_or(1);
        let hour = value[11..13].parse().unwrap_or(0);
        let minute = value[14..16].parse().unwrap_or(0);
        let second = value[17..19].parse().unwrap_or(0);
        if let (Ok(month), Ok(clock)) = (time::Month::try_from(month), time::Time::from_hms(hour, minute, second)) {
            if let Ok(date) = time::Date::from_calendar_date(year, month, day) {
                return date.with_time(clock).assume_utc().unix_timestamp() * 1000;
            }
        }
    }
    value.parse::<i64>().unwrap_or(0).saturating_mul(if value.len() <= 10 { 1000 } else { 1 })
}

fn now_millis() -> i64 {
    SystemTime::now()
        .duration_since(SystemTime::UNIX_EPOCH)
        .map(|duration| duration.as_millis() as i64)
        .unwrap_or(0)
}

#[cfg(test)]
fn sample_note(title: &str, folder: &str, path: &str, syntax: &str) -> crate::contracts::local_data::QuickNote {
    crate::contracts::local_data::QuickNote {
        id: "note".into(),
        title: title.into(),
        content: String::new(),
        tags: Vec::new(),
        color: "default".into(),
        folder_path: folder.into(),
        relative_path: path.into(),
        editor_font: "default".into(),
        line_height: "normal".into(),
        font_size: 14,
        font_name: String::new(),
        line_spacing: 1.2,
        line_wrapping: true,
        syntax: syntax.into(),
        pinned: false,
        created_at: 0,
        updated_at: 0,
    }
}

#[cfg(test)]
mod tests {
    use super::{attachment_references, extension_for_syntax, file_syntax, path_ignored, remove_unreferenced_attachments, sample_note, saved_note_path, write_exported_quick_note};
    use crate::repositories::quick_note_vault::{QuickNoteFile, QuickNoteVault};

    #[test]
    fn keeps_the_existing_file_name_when_the_title_changes() {
        let renamed = sample_note("Completely different", "Work", "Work/API ideas.txt", "plain");
        assert_eq!(saved_note_path(&renamed, Some("Work/API ideas.txt")), "Work/API ideas.txt");
        let markdown = sample_note("Completely different", "Work", "Work/API ideas.txt", "markdown");
        assert_eq!(saved_note_path(&markdown, Some("Work/API ideas.txt")), "Work/API ideas.md");
        let moved = sample_note("Completely different", "Archive", "Work/API ideas.txt", "plain");
        assert_eq!(saved_note_path(&moved, Some("Work/API ideas.txt")), "Archive/API ideas.txt");
        let fresh = sample_note("a/b", "Work", "", "plain");
        assert_eq!(saved_note_path(&fresh, None), "Work/a_b.txt");
    }

    #[test]
    fn hides_gitignored_notes_and_directories() {
        let patterns = vec!["*.tmp".into(), ".idea/".into(), "secret.md".into()];
        assert!(path_ignored("notes/draft.tmp", false, &patterns));
        assert!(path_ignored(".idea", true, &patterns));
        assert!(path_ignored(".idea/workspace.xml", false, &patterns));
        assert!(path_ignored("secret.md", false, &patterns));
        assert!(!path_ignored("Work/API ideas.md", false, &patterns));
    }

    #[test]
    fn removes_an_attachment_only_after_its_last_note_is_gone() {
        let directory = tempfile::tempdir().expect("vault");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("open");
        let stored = vault.store_attachment(b"png-bytes", ".png").expect("attachment");
        let first = vault.create("Picture", "", "ui-monospace", 14, true).expect("first");
        let saved = vault
            .save(&QuickNoteFile { content: stored.markdown.clone(), ..first })
            .expect("save first");
        let second = vault.create("Other", "", "ui-monospace", 14, true).expect("second");
        vault
            .save(&QuickNoteFile { content: stored.markdown.clone(), ..second })
            .expect("save second");
        vault.delete_entry(&saved.relative_path).expect("delete first");
        remove_unreferenced_attachments(&vault, &[stored.relative_path.clone()]).expect("keep shared");
        assert!(vault.read_attachment(&stored.relative_path).is_ok());
        let remaining = vault.note_files().expect("remaining");
        vault.delete_entry(&remaining[0].relative_path).expect("delete last");
        remove_unreferenced_attachments(&vault, &[stored.relative_path.clone()]).expect("cleanup");
        assert!(vault.read_attachment(&stored.relative_path).is_err());
    }

    #[test]
    fn reads_note_attachment_references() {
        assert_eq!(
            attachment_references("![image](attachments/a.png)\nattachments/b.jpg."),
            vec!["attachments/a.png".to_string(), "attachments/b.jpg".to_string()]
        );
    }

    #[test]
    fn uses_electron_extensions_for_note_syntax() {
        assert_eq!(extension_for_syntax("text/markdown"), "md");
        assert_eq!(extension_for_syntax("java"), "java");
        assert_eq!(extension_for_syntax("typescript"), "ts");
        assert_eq!(extension_for_syntax("sql"), "sql");
        assert_eq!(extension_for_syntax("plain"), "txt");
        assert_eq!(file_syntax("python"), "text/python");
        assert_eq!(file_syntax("text/python"), "text/python");
        assert_eq!(file_syntax("sql"), "text/sql");
    }

    #[test]
    fn exports_note_bodies_with_electron_file_names() {
        let source = tempfile::tempdir().expect("vault");
        let vault = QuickNoteVault::open(source.path().to_path_buf()).expect("open");
        vault.create_folder("Work").expect("folder");
        let note = vault.create("API ideas", "Work", "ui-monospace", 14, true).expect("create");
        let saved = vault
            .save(&QuickNoteFile { content: "# Hello\nneedle".into(), ..note })
            .expect("save");
        let exported = tempfile::tempdir().expect("export");
        write_exported_quick_note(exported.path(), &saved.relative_path, &vault.read(&saved.relative_path).expect("read").content).expect("export note");
        write_exported_quick_note(exported.path(), "notes/example.md", "markdown body").expect("markdown");
        write_exported_quick_note(exported.path(), "notes/example.JSON", "json body").expect("json");
        write_exported_quick_note(exported.path(), "bad/a:b?.txt", "sanitized").expect("sanitized");
        write_exported_quick_note(exported.path(), "Work-API ideas.txt", "collision").expect("collision");
        assert_eq!(std::fs::read_to_string(exported.path().join("Work-API ideas.txt")).expect("txt body"), "# Hello\nneedle");
        assert!(!std::fs::read_to_string(exported.path().join("Work-API ideas.txt")).expect("body").contains("font_name"));
        assert_eq!(std::fs::read_to_string(exported.path().join("notes-example.md.txt")).expect("md"), "markdown body");
        assert_eq!(std::fs::read_to_string(exported.path().join("notes-example.txt")).expect("json name"), "json body");
        assert_eq!(std::fs::read_to_string(exported.path().join("bad-a_b_.txt")).expect("sanitized name"), "sanitized");
        assert_eq!(std::fs::read_to_string(exported.path().join("Work-API ideas-1.txt")).expect("collision"), "collision");
    }
}
