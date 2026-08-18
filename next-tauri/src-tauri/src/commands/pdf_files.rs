use std::{
    collections::{HashMap, HashSet},
    fs,
    io::{Read, Write},
    path::{Path, PathBuf},
    sync::{
        Mutex,
        atomic::{AtomicU64, Ordering},
    },
    time::{Duration, Instant, SystemTime, UNIX_EPOCH},
};

use serde::Serialize;
use tauri_plugin_dialog::DialogExt;

use crate::contracts::error::AppResult;

const MAX_FILES: usize = 20;
const MAX_FILE_BYTES: usize = 200 * 1024 * 1024;
const MAX_TOTAL_BYTES: usize = 500 * 1024 * 1024;
const MAX_CHUNK_BYTES: usize = 1024 * 1024;
const SESSION_TTL: Duration = Duration::from_secs(15 * 60);

#[derive(Default)]
pub struct PdfExportManager {
    sessions: Mutex<HashMap<String, PdfExportSession>>,
    next_id: AtomicU64,
}

struct PdfExportSession {
    created_at: Instant,
    total_written: usize,
    files: Vec<PdfExportFile>,
}

struct PdfExportFile {
    target: PathBuf,
    temporary: tempfile::NamedTempFile,
    written: usize,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PdfExportPlan {
    session_id: String,
    target_paths: Vec<String>,
}

#[tauri::command]
pub async fn begin_pdf_export(
    app: tauri::AppHandle,
    manager: tauri::State<'_, PdfExportManager>,
    names: Vec<String>,
) -> AppResult<Option<PdfExportPlan>> {
    if names.is_empty() || names.len() > MAX_FILES {
        return Err("select between 1 and 20 PDF outputs".into());
    }
    for name in &names {
        validate_pdf_name(name)?;
    }

    let targets = if names.len() == 1 {
        let selection = app
            .dialog()
            .file()
            .add_filter("PDF document", &["pdf"])
            .set_file_name(&names[0])
            .blocking_save_file();
        let Some(selection) = selection else {
            return Ok(None);
        };
        let mut target = selection
            .into_path()
            .map_err(|_| "selected PDF export target is not a local filesystem path")?;
        target.set_extension("pdf");
        validate_export_file_target(&target)?;
        vec![target]
    } else {
        let selection = app.dialog().file().blocking_pick_folder();
        let Some(selection) = selection else {
            return Ok(None);
        };
        let directory = selection
            .into_path()
            .map_err(|_| "selected PDF export directory is not a local filesystem path")?;
        validate_export_directory(&directory)?;
        allocate_export_targets(&directory, &names)?
    };

    let mut files = Vec::with_capacity(targets.len());
    for target in &targets {
        let parent = target
            .parent()
            .ok_or_else(|| "PDF export target has no parent directory".to_string())?;
        let temporary = tempfile::NamedTempFile::new_in(parent)
            .map_err(|error| format!("failed to create temporary PDF export file: {error}"))?;
        files.push(PdfExportFile {
            target: target.clone(),
            temporary,
            written: 0,
        });
    }

    let mut sessions = manager
        .sessions
        .lock()
        .map_err(|_| "PDF export manager lock is poisoned")?;
    sessions.retain(|_, session| session.created_at.elapsed() < SESSION_TTL);
    if sessions.len() >= 4 {
        return Err("too many PDF export sessions are active".into());
    }
    let sequence = manager.next_id.fetch_add(1, Ordering::Relaxed);
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis();
    let session_id = format!("pdf-export-{timestamp}-{sequence}");
    sessions.insert(
        session_id.clone(),
        PdfExportSession {
            created_at: Instant::now(),
            total_written: 0,
            files,
        },
    );
    Ok(Some(PdfExportPlan {
        session_id,
        target_paths: targets
            .into_iter()
            .map(|path| path.display().to_string())
            .collect(),
    }))
}

#[tauri::command]
pub fn write_pdf_export_chunk(
    manager: tauri::State<'_, PdfExportManager>,
    session_id: String,
    file_index: usize,
    offset: usize,
    chunk: Vec<u8>,
) -> AppResult<usize> {
    if chunk.is_empty() || chunk.len() > MAX_CHUNK_BYTES {
        return Err("PDF export chunks must contain 1 byte to 1 MiB".into());
    }
    let mut sessions = manager
        .sessions
        .lock()
        .map_err(|_| "PDF export manager lock is poisoned")?;
    let session = sessions
        .get_mut(&session_id)
        .ok_or_else(|| "PDF export session is unavailable or expired".to_string())?;
    let file = session
        .files
        .get_mut(file_index)
        .ok_or_else(|| "PDF export file index is out of range".to_string())?;
    if file.written != offset {
        return Err("PDF export chunk offset does not match the written length".into());
    }
    let file_length = file
        .written
        .checked_add(chunk.len())
        .ok_or_else(|| "PDF export size overflow".to_string())?;
    let total_length = session
        .total_written
        .checked_add(chunk.len())
        .ok_or_else(|| "PDF export size overflow".to_string())?;
    if file_length > MAX_FILE_BYTES || total_length > MAX_TOTAL_BYTES {
        return Err("PDF output exceeds the 200 MiB file or 500 MiB batch limit".into());
    }
    file.temporary
        .write_all(&chunk)
        .map_err(|error| format!("failed to write PDF export chunk: {error}"))?;
    file.written = file_length;
    session.total_written = total_length;
    Ok(file.written)
}

#[tauri::command]
pub fn finish_pdf_export(
    manager: tauri::State<'_, PdfExportManager>,
    session_id: String,
) -> AppResult<Vec<String>> {
    let session = manager
        .sessions
        .lock()
        .map_err(|_| "PDF export manager lock is poisoned")?
        .remove(&session_id)
        .ok_or_else(|| "PDF export session is unavailable or expired".to_string())?;
    finish_session(session).map_err(Into::into)
}

#[tauri::command]
pub fn cancel_pdf_export(
    manager: tauri::State<'_, PdfExportManager>,
    session_id: String,
) -> AppResult<bool> {
    Ok(manager
        .sessions
        .lock()
        .map_err(|_| "PDF export manager lock is poisoned")?
        .remove(&session_id)
        .is_some())
}

fn finish_session(mut session: PdfExportSession) -> Result<Vec<String>, String> {
    if session.files.iter().any(|file| file.written == 0) {
        return Err("each PDF export file must contain data".into());
    }
    for file in &mut session.files {
        file.temporary
            .as_file_mut()
            .flush()
            .and_then(|()| file.temporary.as_file().sync_all())
            .map_err(|error| format!("failed to flush PDF export: {error}"))?;
        validate_pdf_header(file.temporary.path())?;
    }

    let allow_overwrite = session.files.len() == 1;
    let mut exported = Vec::with_capacity(session.files.len());
    for file in session.files {
        let target = file.target.clone();
        if let Err(error) = persist_pdf(file.temporary, &target, allow_overwrite) {
            for path in &exported {
                let _ = fs::remove_file(path);
            }
            return Err(error);
        }
        exported.push(target);
    }
    Ok(exported
        .into_iter()
        .map(|path| path.display().to_string())
        .collect())
}

fn persist_pdf(
    temporary: tempfile::NamedTempFile,
    target: &Path,
    allow_overwrite: bool,
) -> Result<(), String> {
    validate_export_file_target(target)?;
    if !allow_overwrite || !target.exists() {
        return temporary
            .persist_noclobber(target)
            .map(|_| ())
            .map_err(|error| format!("failed to save exported PDF: {}", error.error));
    }

    let parent = target
        .parent()
        .ok_or_else(|| "PDF export target has no parent directory".to_string())?;
    let backup_holder = tempfile::NamedTempFile::new_in(parent)
        .map_err(|error| format!("failed to prepare PDF overwrite backup: {error}"))?;
    let backup = backup_holder
        .into_temp_path()
        .keep()
        .map_err(|error| format!("failed to prepare PDF overwrite backup: {}", error.error))?;
    fs::remove_file(&backup)
        .map_err(|error| format!("failed to prepare PDF overwrite backup: {error}"))?;
    fs::rename(target, &backup)
        .map_err(|error| format!("failed to preserve existing PDF before overwrite: {error}"))?;
    match temporary.persist_noclobber(target) {
        Ok(_) => {
            let _ = fs::remove_file(backup);
            Ok(())
        }
        Err(error) => {
            let _ = fs::rename(&backup, target);
            Err(format!("failed to save exported PDF: {}", error.error))
        }
    }
}

fn validate_pdf_header(path: &Path) -> Result<(), String> {
    let mut file = fs::File::open(path)
        .map_err(|error| format!("failed to inspect generated PDF: {error}"))?;
    let mut prefix = [0_u8; 1024];
    let length = file
        .read(&mut prefix)
        .map_err(|error| format!("failed to inspect generated PDF: {error}"))?;
    if prefix[..length].windows(5).any(|value| value == b"%PDF-") {
        Ok(())
    } else {
        Err("generated output does not contain a PDF header".into())
    }
}

fn validate_pdf_name(value: &str) -> Result<(), String> {
    if value.trim().is_empty()
        || value.chars().count() > 180
        || value.starts_with('.')
        || !value.to_ascii_lowercase().ends_with(".pdf")
        || value
            .chars()
            .any(|character| character.is_control() || matches!(character, '/' | '\\' | ':'))
    {
        return Err("invalid PDF export filename".into());
    }
    Ok(())
}

fn validate_export_directory(path: &Path) -> Result<(), String> {
    if !path.is_absolute() {
        return Err("PDF export directory must be absolute".into());
    }
    let metadata = fs::symlink_metadata(path)
        .map_err(|error| format!("failed to inspect PDF export directory: {error}"))?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err("PDF export target must be a regular non-symlink directory".into());
    }
    Ok(())
}

fn validate_export_file_target(path: &Path) -> Result<(), String> {
    if !path.is_absolute() {
        return Err("PDF export target must be absolute".into());
    }
    if let Ok(metadata) = fs::symlink_metadata(path)
        && (metadata.file_type().is_symlink() || !metadata.is_file())
    {
        return Err("PDF export target must be a regular non-symlink file".into());
    }
    let parent = path
        .parent()
        .ok_or_else(|| "PDF export target has no parent directory".to_string())?;
    validate_export_directory(parent)
}

fn allocate_export_targets(directory: &Path, names: &[String]) -> Result<Vec<PathBuf>, String> {
    let mut reserved = HashSet::new();
    let mut targets = Vec::with_capacity(names.len());
    for name in names {
        let path = Path::new(name);
        let stem = path
            .file_stem()
            .and_then(|value| value.to_str())
            .unwrap_or("MooTool-output");
        let mut selected = None;
        for suffix in 1..10_000 {
            let filename = if suffix == 1 {
                format!("{stem}.pdf")
            } else {
                format!("{stem}-{suffix}.pdf")
            };
            let candidate = directory.join(filename);
            if !candidate.exists() && reserved.insert(candidate.clone()) {
                selected = Some(candidate);
                break;
            }
        }
        targets.push(
            selected.ok_or_else(|| "could not allocate a unique PDF export name".to_string())?,
        );
    }
    Ok(targets)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_names_and_allocates_distinct_targets() {
        assert!(validate_pdf_name("report.pdf").is_ok());
        assert!(validate_pdf_name("../report.pdf").is_err());
        assert!(validate_pdf_name("report.txt").is_err());
        let directory = tempfile::TempDir::new().unwrap();
        fs::write(directory.path().join("report.pdf"), b"existing").unwrap();
        let targets = allocate_export_targets(
            directory.path(),
            &["report.pdf".into(), "report.pdf".into()],
        )
        .unwrap();
        assert_eq!(targets[0].file_name().unwrap(), "report-2.pdf");
        assert_eq!(targets[1].file_name().unwrap(), "report-3.pdf");
    }

    #[test]
    fn finishes_a_pdf_session_and_replaces_an_explicit_single_target() {
        let directory = tempfile::TempDir::new().unwrap();
        let target = directory.path().join("output.pdf");
        fs::write(&target, b"%PDF-1.4\nold").unwrap();
        let mut temporary = tempfile::NamedTempFile::new_in(directory.path()).unwrap();
        temporary.write_all(b"%PDF-1.7\nbody").unwrap();
        let session = PdfExportSession {
            created_at: Instant::now(),
            total_written: 13,
            files: vec![PdfExportFile {
                target: target.clone(),
                temporary,
                written: 13,
            }],
        };
        let paths = finish_session(session).unwrap();
        assert_eq!(paths, vec![target.display().to_string()]);
        assert_eq!(fs::read(target).unwrap(), b"%PDF-1.7\nbody");
    }
}
