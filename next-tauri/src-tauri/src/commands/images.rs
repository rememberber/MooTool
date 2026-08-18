use std::{
    fs,
    io::{Cursor, Write},
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

use base64::{Engine as _, engine::general_purpose::STANDARD};
use tauri::Manager;
use tauri_plugin_dialog::DialogExt;

use crate::{
    contracts::{
        error::AppResult,
        image::{ImageAsset, ImageAssetInput, ImageAssetSummary},
    },
    repositories::local_data::LocalDataRepository,
};

const MAX_IMAGE_BYTES: usize = 20 * 1024 * 1024;

#[tauri::command]
pub fn list_image_assets(
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<Vec<ImageAssetSummary>> {
    Ok(repository.list_image_assets()?)
}

#[tauri::command]
pub fn save_image_asset(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    input: ImageAssetInput,
) -> AppResult<ImageAssetSummary> {
    let (mime_type, bytes) = decode_data_url(&input.data_url)?;
    Ok(persist_image_bytes(
        &app,
        &repository,
        &input.name,
        mime_type,
        input.width,
        input.height,
        &bytes,
    )?)
}

#[tauri::command]
pub fn import_image_files(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    paths: Vec<String>,
) -> AppResult<Vec<ImageAssetSummary>> {
    if paths.is_empty() || paths.len() > 50 {
        return Err("select between 1 and 50 image files to import".into());
    }
    let mut imported = Vec::with_capacity(paths.len());
    for value in paths {
        let path = PathBuf::from(value);
        if !path.is_absolute() {
            rollback_imported_images(&app, &repository, &imported);
            return Err("dropped image paths must be absolute".into());
        }
        let metadata = match fs::symlink_metadata(&path) {
            Ok(metadata) => metadata,
            Err(error) => {
                rollback_imported_images(&app, &repository, &imported);
                return Err(format!("failed to inspect dropped image: {error}").into());
            }
        };
        if !metadata.is_file()
            || metadata.file_type().is_symlink()
            || metadata.len() == 0
            || metadata.len() > MAX_IMAGE_BYTES as u64
        {
            rollback_imported_images(&app, &repository, &imported);
            return Err("dropped image must be a regular 1 byte to 20 MiB file".into());
        }
        let bytes = match fs::read(&path) {
            Ok(bytes) => bytes,
            Err(error) => {
                rollback_imported_images(&app, &repository, &imported);
                return Err(format!("failed to read dropped image: {error}").into());
            }
        };
        let (mime_type, width, height) = match inspect_image_bytes(&bytes) {
            Ok(info) => info,
            Err(error) => {
                rollback_imported_images(&app, &repository, &imported);
                return Err(error.into());
            }
        };
        let requested_name = match path.file_name().and_then(|value| value.to_str()) {
            Some(name) => name,
            None => {
                rollback_imported_images(&app, &repository, &imported);
                return Err("dropped image name is not valid UTF-8".into());
            }
        };
        let name = match unique_image_name(&repository, requested_name, mime_type) {
            Ok(name) => name,
            Err(error) => {
                rollback_imported_images(&app, &repository, &imported);
                return Err(error.into());
            }
        };
        match persist_image_bytes(&app, &repository, &name, mime_type, width, height, &bytes) {
            Ok(summary) => imported.push(summary),
            Err(error) => {
                rollback_imported_images(&app, &repository, &imported);
                return Err(error.into());
            }
        }
    }
    Ok(imported)
}

pub(crate) fn persist_image_bytes(
    app: &tauri::AppHandle,
    repository: &LocalDataRepository,
    requested_name: &str,
    mime_type: &str,
    width: u32,
    height: u32,
    bytes: &[u8],
) -> Result<ImageAssetSummary, String> {
    if bytes.is_empty() || bytes.len() > MAX_IMAGE_BYTES {
        return Err("image must contain 1 byte to 20 MiB".into());
    }
    validate_dimensions(width, height)?;
    validate_image_signature(mime_type, bytes)?;
    let (actual_mime_type, actual_width, actual_height) = inspect_image_bytes(bytes)?;
    if actual_mime_type != mime_type || actual_width != width || actual_height != height {
        return Err("image metadata does not match the encoded image".into());
    }
    let name = normalize_image_name(requested_name, mime_type)?;
    let directory = image_directory(app)?;
    fs::create_dir_all(&directory)
        .map_err(|error| format!("failed to create image library: {error}"))?;
    let path = directory.join(&name);
    let previous_bytes = fs::read(&path).ok();
    fs::write(&path, bytes)
        .map_err(|error| format!("failed to save image {}: {error}", path.display()))?;
    let summary = ImageAssetSummary {
        name,
        mime_type: mime_type.into(),
        width,
        height,
        size_bytes: bytes.len(),
        updated_at: now_millis(),
    };
    if let Err(error) = repository.save_image_asset(&summary) {
        if let Some(previous_bytes) = previous_bytes {
            let _ = fs::write(path, previous_bytes);
        } else {
            let _ = fs::remove_file(path);
        }
        return Err(error);
    }
    Ok(summary)
}

pub(crate) fn remove_persisted_image(
    app: &tauri::AppHandle,
    repository: &LocalDataRepository,
    name: &str,
) {
    if validate_file_name(name).is_err() {
        return;
    }
    if let Ok(directory) = image_directory(app) {
        let _ = fs::remove_file(directory.join(name));
    }
    let _ = repository.delete_image_asset(name);
}

fn rollback_imported_images(
    app: &tauri::AppHandle,
    repository: &LocalDataRepository,
    imported: &[ImageAssetSummary],
) {
    for asset in imported {
        remove_persisted_image(app, repository, &asset.name);
    }
}

#[tauri::command]
pub fn read_image_asset(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    name: String,
) -> AppResult<ImageAsset> {
    validate_file_name(&name)?;
    let summary = repository
        .get_image_asset(&name)?
        .ok_or_else(|| "image asset not found".to_string())?;
    let path = image_directory(&app)?.join(&summary.name);
    let bytes = fs::read(&path)
        .map_err(|error| format!("failed to read image {}: {error}", path.display()))?;
    if bytes.len() > MAX_IMAGE_BYTES {
        return Err("stored image exceeds the 20 MiB read limit".into());
    }
    Ok(ImageAsset {
        data_url: format!(
            "data:{};base64,{}",
            summary.mime_type,
            STANDARD.encode(bytes)
        ),
        summary,
    })
}

#[tauri::command]
pub async fn export_image_assets(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    names: Vec<String>,
) -> AppResult<Option<Vec<String>>> {
    if names.is_empty() || names.len() > 50 {
        return Err("select between 1 and 50 images to export".into());
    }
    let source_directory = image_directory(&app)?;
    let mut sources = Vec::with_capacity(names.len());
    for name in names {
        validate_file_name(&name)?;
        let summary = repository
            .get_image_asset(&name)?
            .ok_or_else(|| format!("image asset not found: {name}"))?;
        let path = source_directory.join(&summary.name);
        let metadata = fs::symlink_metadata(&path)
            .map_err(|error| format!("failed to inspect stored image: {error}"))?;
        if !metadata.is_file()
            || metadata.file_type().is_symlink()
            || metadata.len() == 0
            || metadata.len() > MAX_IMAGE_BYTES as u64
        {
            return Err("stored image must be a regular 1 byte to 20 MiB file".into());
        }
        sources.push((summary, path));
    }

    if sources.len() == 1 {
        let (summary, source) = &sources[0];
        let selection = app
            .dialog()
            .file()
            .add_filter("Image", &[image_extension(&summary.mime_type)?])
            .set_file_name(&summary.name)
            .blocking_save_file();
        let Some(selection) = selection else {
            return Ok(None);
        };
        let mut target = selection
            .into_path()
            .map_err(|_| "selected export target is not a local filesystem path")?;
        enforce_image_extension(&mut target, &summary.mime_type)?;
        validate_export_file_target(&target)?;
        atomic_copy(source, &target)?;
        return Ok(Some(vec![target.display().to_string()]));
    }

    let selection = app.dialog().file().blocking_pick_folder();
    let Some(selection) = selection else {
        return Ok(None);
    };
    let destination = selection
        .into_path()
        .map_err(|_| "selected export directory is not a local filesystem path")?;
    validate_export_directory(&destination)?;
    let mut exported = Vec::with_capacity(sources.len());
    for (summary, source) in sources {
        let target = unique_export_path(&destination, &summary.name)?;
        if let Err(error) = copy_new_file(&source, &target) {
            for path in &exported {
                let _ = fs::remove_file(path);
            }
            return Err(error.into());
        }
        exported.push(target);
    }
    Ok(Some(
        exported
            .into_iter()
            .map(|path| path.display().to_string())
            .collect(),
    ))
}

#[tauri::command]
pub fn rename_image_asset(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    name: String,
    next_name: String,
) -> AppResult<ImageAssetSummary> {
    validate_file_name(&name)?;
    let current = repository
        .get_image_asset(&name)?
        .ok_or_else(|| "image asset not found".to_string())?;
    let next = normalize_image_name(&next_name, &current.mime_type)?;
    if next == name {
        return Ok(current);
    }
    if repository.get_image_asset(&next)?.is_some() {
        return Err("an image with the requested name already exists".into());
    }
    let directory = image_directory(&app)?;
    fs::rename(directory.join(&name), directory.join(&next))
        .map_err(|error| format!("failed to rename image: {error}"))?;
    match repository.rename_image_asset(&name, &next, now_millis()) {
        Ok(summary) => Ok(summary),
        Err(error) => {
            let _ = fs::rename(directory.join(&next), directory.join(&name));
            Err(error.into())
        }
    }
}

#[tauri::command]
pub fn delete_image_assets(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    names: Vec<String>,
) -> AppResult<usize> {
    if names.is_empty() || names.len() > 50 {
        return Err("select between 1 and 50 images to delete".into());
    }
    let directory = image_directory(&app)?;
    let mut deleted = 0;
    for name in names {
        validate_file_name(&name)?;
        if repository.get_image_asset(&name)?.is_none() {
            continue;
        }
        let path = directory.join(&name);
        let previous_bytes = fs::read(&path).ok();
        match fs::remove_file(&path) {
            Ok(()) => {}
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
            Err(error) => return Err(format!("failed to delete image file: {error}").into()),
        }
        match repository.delete_image_asset(&name) {
            Ok(true) => deleted += 1,
            Ok(false) => {
                if let Some(bytes) = previous_bytes {
                    let _ = fs::write(&path, bytes);
                }
            }
            Err(error) => {
                if let Some(bytes) = previous_bytes {
                    let _ = fs::write(&path, bytes);
                }
                return Err(error.into());
            }
        }
    }
    Ok(deleted)
}

fn image_directory(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    app.path()
        .app_data_dir()
        .map(|path| path.join("images"))
        .map_err(|error| format!("failed to resolve image library directory: {error}"))
}

fn decode_data_url(value: &str) -> Result<(&str, Vec<u8>), String> {
    let (header, payload) = value
        .split_once(',')
        .ok_or_else(|| "invalid image data URL".to_string())?;
    let mime_type = header
        .strip_prefix("data:")
        .and_then(|value| value.strip_suffix(";base64"))
        .ok_or_else(|| "image must use a Base64 data URL".to_string())?;
    if !matches!(
        mime_type,
        "image/png" | "image/jpeg" | "image/webp" | "image/gif"
    ) {
        return Err("supported image formats are PNG, JPEG, WebP, and GIF".into());
    }
    let estimated_size = payload.len().saturating_mul(3) / 4;
    if estimated_size > MAX_IMAGE_BYTES {
        return Err("image cannot exceed 20 MiB".into());
    }
    let bytes = STANDARD
        .decode(payload)
        .map_err(|_| "invalid image Base64 payload".to_string())?;
    if bytes.is_empty() || bytes.len() > MAX_IMAGE_BYTES {
        return Err("image must contain 1 byte to 20 MiB".into());
    }
    Ok((mime_type, bytes))
}

fn validate_image_signature(mime_type: &str, bytes: &[u8]) -> Result<(), String> {
    let valid = match mime_type {
        "image/png" => bytes.starts_with(b"\x89PNG\r\n\x1a\n"),
        "image/jpeg" => bytes.starts_with(&[0xff, 0xd8, 0xff]),
        "image/webp" => bytes.starts_with(b"RIFF") && bytes.get(8..12) == Some(b"WEBP"),
        "image/gif" => bytes.starts_with(b"GIF87a") || bytes.starts_with(b"GIF89a"),
        _ => false,
    };
    if valid {
        Ok(())
    } else {
        Err("image bytes do not match the declared format".into())
    }
}

fn inspect_image_bytes(bytes: &[u8]) -> Result<(&'static str, u32, u32), String> {
    let reader = image::ImageReader::new(Cursor::new(bytes))
        .with_guessed_format()
        .map_err(|error| format!("failed to inspect image format: {error}"))?;
    let format = reader
        .format()
        .ok_or_else(|| "image format is not supported".to_string())?;
    let mime_type = match format {
        image::ImageFormat::Png => "image/png",
        image::ImageFormat::Jpeg => "image/jpeg",
        image::ImageFormat::WebP => "image/webp",
        image::ImageFormat::Gif => "image/gif",
        _ => return Err("supported image formats are PNG, JPEG, WebP, and GIF".into()),
    };
    let (width, height) = reader
        .into_dimensions()
        .map_err(|error| format!("failed to read image dimensions: {error}"))?;
    validate_dimensions(width, height)?;
    Ok((mime_type, width, height))
}

fn unique_image_name(
    repository: &LocalDataRepository,
    requested_name: &str,
    mime_type: &str,
) -> Result<String, String> {
    let normalized = normalize_image_name(requested_name, mime_type)?;
    if repository.get_image_asset(&normalized)?.is_none() {
        return Ok(normalized);
    }
    let path = Path::new(&normalized);
    let stem = path
        .file_stem()
        .and_then(|value| value.to_str())
        .unwrap_or("Image");
    let extension = path
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or("png");
    for suffix in 2..10_000 {
        let candidate = format!("{stem}-{suffix}.{extension}");
        if repository.get_image_asset(&candidate)?.is_none() {
            return Ok(candidate);
        }
    }
    Err("could not allocate a unique image name".into())
}

fn normalize_image_name(value: &str, mime_type: &str) -> Result<String, String> {
    validate_file_name(value.trim())?;
    let expected_extension = match mime_type {
        "image/png" => "png",
        "image/jpeg" => "jpg",
        "image/webp" => "webp",
        "image/gif" => "gif",
        _ => return Err("unsupported image format".into()),
    };
    let path = Path::new(value.trim());
    let stem = path
        .file_stem()
        .and_then(|value| value.to_str())
        .ok_or_else(|| "invalid image name".to_string())?;
    let extension = path.extension().and_then(|value| value.to_str());
    if stem.is_empty() {
        return Err("image name cannot be empty".into());
    }
    Ok(
        if extension.is_some_and(|value| {
            value.eq_ignore_ascii_case(expected_extension)
                || (mime_type == "image/jpeg" && value.eq_ignore_ascii_case("jpeg"))
        }) {
            value.trim().to_string()
        } else {
            format!("{stem}.{expected_extension}")
        },
    )
}

fn validate_file_name(value: &str) -> Result<(), String> {
    if value.is_empty()
        || value.chars().count() > 180
        || value == "."
        || value == ".."
        || value.starts_with('.')
        || value
            .chars()
            .any(|character| character.is_control() || matches!(character, '/' | '\\' | ':'))
    {
        return Err("invalid image file name".into());
    }
    Ok(())
}

fn validate_dimensions(width: u32, height: u32) -> Result<(), String> {
    if width == 0 || height == 0 || width > 50_000 || height > 50_000 {
        return Err("image dimensions must be between 1 and 50000 pixels".into());
    }
    Ok(())
}

fn image_extension(mime_type: &str) -> Result<&'static str, String> {
    match mime_type {
        "image/png" => Ok("png"),
        "image/jpeg" => Ok("jpg"),
        "image/webp" => Ok("webp"),
        "image/gif" => Ok("gif"),
        _ => Err("unsupported image format".into()),
    }
}

fn enforce_image_extension(path: &mut PathBuf, mime_type: &str) -> Result<(), String> {
    let expected = image_extension(mime_type)?;
    let matches = path
        .extension()
        .and_then(|value| value.to_str())
        .is_some_and(|value| {
            value.eq_ignore_ascii_case(expected)
                || (mime_type == "image/jpeg" && value.eq_ignore_ascii_case("jpeg"))
        });
    if !matches {
        path.set_extension(expected);
    }
    Ok(())
}

fn validate_export_directory(path: &Path) -> Result<(), String> {
    if !path.is_absolute() {
        return Err("image export directory must be absolute".into());
    }
    let metadata = fs::symlink_metadata(path)
        .map_err(|error| format!("failed to inspect image export directory: {error}"))?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err("image export target must be a regular non-symlink directory".into());
    }
    Ok(())
}

fn validate_export_file_target(path: &Path) -> Result<(), String> {
    if !path.is_absolute() {
        return Err("image export target must be absolute".into());
    }
    if let Ok(metadata) = fs::symlink_metadata(path)
        && (metadata.file_type().is_symlink() || !metadata.is_file())
    {
        return Err("image export target must be a regular non-symlink file".into());
    }
    let parent = path
        .parent()
        .ok_or_else(|| "image export target has no parent directory".to_string())?;
    validate_export_directory(parent)
}

fn atomic_copy(source: &Path, target: &Path) -> Result<(), String> {
    let parent = target
        .parent()
        .ok_or_else(|| "image export target has no parent directory".to_string())?;
    let mut temporary = tempfile::NamedTempFile::new_in(parent)
        .map_err(|error| format!("failed to create temporary export file: {error}"))?;
    let bytes = fs::read(source)
        .map_err(|error| format!("failed to read stored image for export: {error}"))?;
    temporary
        .write_all(&bytes)
        .and_then(|()| temporary.flush())
        .map_err(|error| format!("failed to write temporary export image: {error}"))?;
    temporary
        .persist(target)
        .map_err(|error| format!("failed to save exported image: {}", error.error))?;
    Ok(())
}

fn unique_export_path(directory: &Path, name: &str) -> Result<PathBuf, String> {
    validate_file_name(name)?;
    let original = directory.join(name);
    if !original.exists() {
        return Ok(original);
    }
    let path = Path::new(name);
    let stem = path
        .file_stem()
        .and_then(|value| value.to_str())
        .unwrap_or("Image");
    let extension = path.extension().and_then(|value| value.to_str());
    for suffix in 2..10_000 {
        let candidate = match extension {
            Some(extension) => directory.join(format!("{stem}-{suffix}.{extension}")),
            None => directory.join(format!("{stem}-{suffix}")),
        };
        if !candidate.exists() {
            return Ok(candidate);
        }
    }
    Err("could not allocate a unique image export name".into())
}

fn copy_new_file(source: &Path, target: &Path) -> Result<(), String> {
    let bytes = fs::read(source)
        .map_err(|error| format!("failed to read stored image for export: {error}"))?;
    let mut file = fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(target)
        .map_err(|error| {
            format!(
                "failed to create image export {}: {error}",
                target.display()
            )
        })?;
    if let Err(error) = file.write_all(&bytes).and_then(|()| file.flush()) {
        drop(file);
        let _ = fs::remove_file(target);
        return Err(format!(
            "failed to export image {}: {error}",
            target.display()
        ));
    }
    Ok(())
}

fn now_millis() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(i64::MAX)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_names_signatures_and_limits() {
        assert_eq!(normalize_image_name("moo", "image/png").unwrap(), "moo.png");
        assert_eq!(
            normalize_image_name("moo.jpeg", "image/jpeg").unwrap(),
            "moo.jpeg"
        );
        assert!(normalize_image_name("../moo.png", "image/png").is_err());
        assert!(validate_image_signature("image/png", b"not png").is_err());
        assert!(validate_dimensions(0, 10).is_err());
        assert_eq!(image_extension("image/webp").unwrap(), "webp");
        let mut target = PathBuf::from("example.jpeg");
        enforce_image_extension(&mut target, "image/jpeg").unwrap();
        assert_eq!(target, PathBuf::from("example.jpeg"));
        let mut target = PathBuf::from("example.txt");
        enforce_image_extension(&mut target, "image/png").unwrap();
        assert_eq!(target, PathBuf::from("example.png"));
    }

    #[test]
    fn exports_without_overwriting_bulk_targets() {
        let directory = tempfile::TempDir::new().expect("temporary image export directory");
        let source = directory.path().join("source.png");
        fs::write(&source, b"source image bytes").unwrap();
        let existing = directory.path().join("image.png");
        fs::write(&existing, b"existing image bytes").unwrap();

        let target = unique_export_path(directory.path(), "image.png").unwrap();
        assert_eq!(target.file_name().unwrap(), "image-2.png");
        copy_new_file(&source, &target).unwrap();

        assert_eq!(fs::read(&existing).unwrap(), b"existing image bytes");
        assert_eq!(fs::read(&target).unwrap(), b"source image bytes");
        assert!(copy_new_file(&source, &target).is_err());
        assert_eq!(fs::read(&target).unwrap(), b"source image bytes");
    }
}
