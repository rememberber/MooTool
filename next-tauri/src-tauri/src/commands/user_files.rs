use std::{fs, io::Read, path::Path};

use serde::Serialize;
use sha2::{Digest, Sha256, Sha384, Sha512};
use tauri_plugin_dialog::DialogExt;

use crate::contracts::error::AppResult;

const MAX_TEXT_BYTES: u64 = 5 * 1024 * 1024;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserTextFile {
    name: String,
    path: String,
    content: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserFileDigest {
    name: String,
    path: String,
    digest: String,
}

#[tauri::command]
pub async fn digest_user_file(
    app: tauri::AppHandle,
    algorithm: String,
) -> AppResult<Option<UserFileDigest>> {
    if !matches!(algorithm.as_str(), "sha256" | "sha384" | "sha512") {
        return Err("file digest supports SHA-256, SHA-384, or SHA-512".into());
    }
    let selection = app.dialog().file().blocking_pick_file();
    let Some(selection) = selection else {
        return Ok(None);
    };
    let path = selection
        .into_path()
        .map_err(|_| "selected digest file is not a local filesystem path")?;
    let metadata = fs::symlink_metadata(&path)
        .map_err(|error| format!("failed to inspect digest file: {error}"))?;
    if !metadata.is_file()
        || metadata.file_type().is_symlink()
        || metadata.len() > 2 * 1024 * 1024 * 1024
    {
        return Err("digest source must be a regular non-symlink file up to 2 GiB".into());
    }
    let digest = digest_path(&path, &algorithm)?;
    let name = path
        .file_name()
        .and_then(|value| value.to_str())
        .ok_or_else(|| "digest filename must be valid UTF-8".to_string())?
        .to_string();
    Ok(Some(UserFileDigest {
        name,
        path: path.display().to_string(),
        digest,
    }))
}

fn digest_path(path: &Path, algorithm: &str) -> Result<String, String> {
    let mut file =
        fs::File::open(path).map_err(|error| format!("failed to open digest file: {error}"))?;
    let mut buffer = [0_u8; 64 * 1024];
    macro_rules! hash_file {
        ($hasher:expr) => {{
            let mut hasher = $hasher;
            loop {
                let count = file
                    .read(&mut buffer)
                    .map_err(|error| format!("failed to read digest file: {error}"))?;
                if count == 0 {
                    break;
                }
                hasher.update(&buffer[..count]);
            }
            format!("{:x}", hasher.finalize())
        }};
    }
    Ok(match algorithm {
        "sha256" => hash_file!(Sha256::new()),
        "sha384" => hash_file!(Sha384::new()),
        "sha512" => hash_file!(Sha512::new()),
        _ => return Err("unsupported file digest algorithm".into()),
    })
}

#[tauri::command]
pub async fn pick_text_file(app: tauri::AppHandle) -> AppResult<Option<UserTextFile>> {
    let selection = app
        .dialog()
        .file()
        .add_filter(
            "Text files",
            &["txt", "hosts", "conf", "md", "json", "yaml", "yml", "svg"],
        )
        .blocking_pick_file();
    let Some(selection) = selection else {
        return Ok(None);
    };
    let path = selection
        .into_path()
        .map_err(|_| "selected text file is not a local filesystem path")?;
    let metadata = fs::symlink_metadata(&path)
        .map_err(|error| format!("failed to inspect selected text file: {error}"))?;
    if !metadata.is_file() || metadata.file_type().is_symlink() || metadata.len() > MAX_TEXT_BYTES {
        return Err("selected text file must be a regular UTF-8 file up to 5 MiB".into());
    }
    let content = fs::read_to_string(&path)
        .map_err(|error| format!("failed to read selected UTF-8 text file: {error}"))?;
    let name = path
        .file_name()
        .and_then(|value| value.to_str())
        .ok_or_else(|| "selected text filename is not valid UTF-8".to_string())?;
    Ok(Some(UserTextFile {
        name: name.into(),
        path: path.display().to_string(),
        content,
    }))
}

#[tauri::command]
pub async fn export_text_file(
    app: tauri::AppHandle,
    default_name: String,
    content: String,
) -> AppResult<Option<String>> {
    validate_default_name(&default_name)?;
    if content.len() as u64 > MAX_TEXT_BYTES {
        return Err("exported text cannot exceed 5 MiB".into());
    }
    let selection = app
        .dialog()
        .file()
        .add_filter(
            "Text files",
            &["txt", "hosts", "conf", "md", "json", "yaml", "yml", "svg"],
        )
        .set_file_name(&default_name)
        .blocking_save_file();
    let Some(selection) = selection else {
        return Ok(None);
    };
    let path = selection
        .into_path()
        .map_err(|_| "selected export target is not a local filesystem path")?;
    validate_export_target(&path)?;
    fs::write(&path, content)
        .map_err(|error| format!("failed to export text file {}: {error}", path.display()))?;
    Ok(Some(path.display().to_string()))
}

fn validate_default_name(value: &str) -> Result<(), String> {
    if value.trim().is_empty()
        || value.chars().count() > 180
        || value.starts_with('.')
        || value
            .chars()
            .any(|character| character.is_control() || matches!(character, '/' | '\\' | ':'))
    {
        return Err("invalid default export filename".into());
    }
    Ok(())
}

fn validate_export_target(path: &Path) -> Result<(), String> {
    if !path.is_absolute() {
        return Err("export target must be an absolute path".into());
    }
    if let Ok(metadata) = fs::symlink_metadata(path)
        && (metadata.file_type().is_symlink() || !metadata.is_file())
    {
        return Err("export target must be a regular non-symlink file".into());
    }
    let parent = path
        .parent()
        .ok_or_else(|| "export target has no parent directory".to_string())?;
    let metadata = fs::metadata(parent)
        .map_err(|error| format!("failed to inspect export directory: {error}"))?;
    if !metadata.is_dir() {
        return Err("export target parent must be a directory".into());
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_safe_default_export_names() {
        assert!(validate_default_name("hosts.txt").is_ok());
        assert!(validate_default_name("../hosts").is_err());
        assert!(validate_default_name("C:hosts").is_err());
    }

    #[test]
    fn digests_files_with_streaming_sha2() {
        let directory = tempfile::TempDir::new().expect("digest directory");
        let path = directory.path().join("sample.bin");
        fs::write(&path, b"abc").expect("digest fixture");
        assert_eq!(
            digest_path(&path, "sha256").expect("digest"),
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
    }
}
