use std::{
    fs,
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

use rusqlite::{Connection, OpenFlags};
use tauri::{Emitter, Manager};

use crate::{
    commands::{
        history::HISTORY_CHANGED_EVENT, local_data::LOCAL_DATA_CHANGED_EVENT,
        settings::SETTINGS_CHANGED_EVENT,
    },
    contracts::{
        backup::{
            BACKUP_FORMAT_VERSION, BACKUP_PRODUCT_ID, BackupExportResult, BackupImportResult,
            BackupManifest, DATABASE_SCHEMA_VERSION,
        },
        error::AppResult,
        local_data::OperationHistory,
        settings::{AppSettings, SETTINGS_SCHEMA_VERSION},
    },
    repositories::{
        local_data::{DATABASE_FILE_NAME, LocalDataRepository},
        settings::{SETTINGS_FILE_NAME, SettingsRepository},
        vault::VaultRepository,
    },
};

const MAX_DATABASE_BYTES: u64 = 2 * 1024 * 1024 * 1024;
const MAX_SETTINGS_BYTES: u64 = 2 * 1024 * 1024;
const MAX_IMAGE_BYTES: u64 = 20 * 1024 * 1024;
const MANIFEST_FILE_NAME: &str = "manifest.json";

#[tauri::command]
pub fn export_product_backup(
    app: tauri::AppHandle,
    local_data: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
    vault: tauri::State<'_, VaultRepository>,
    destination_directory: String,
) -> AppResult<BackupExportResult> {
    let destination = existing_directory(&destination_directory)?;
    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?;
    fs::create_dir_all(&app_data)
        .map_err(|error| format!("failed to initialize Tauri data directory: {error}"))?;
    let app_data = app_data
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize Tauri data directory: {error}"))?;
    if destination.starts_with(&app_data) {
        return Err(
            "backup destination cannot be inside the Tauri application data directory".into(),
        );
    }
    if vault
        .configured_root()?
        .is_some_and(|root| destination.starts_with(root))
    {
        return Err("backup destination cannot be inside the configured JSON Vault".into());
    }

    write_product_backup(
        &app,
        &local_data,
        &settings,
        &vault,
        &destination,
        &app_data,
    )
}

pub(crate) fn create_internal_backup(
    app: &tauri::AppHandle,
    local_data: &LocalDataRepository,
    settings: &SettingsRepository,
    vault: &VaultRepository,
    destination: &Path,
) -> AppResult<BackupExportResult> {
    fs::create_dir_all(destination)
        .map_err(|error| format!("failed to create migration backup directory: {error}"))?;
    let destination = destination
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize migration backup directory: {error}"))?;
    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?;
    fs::create_dir_all(&app_data)
        .map_err(|error| format!("failed to initialize Tauri data directory: {error}"))?;
    let app_data = app_data
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize Tauri data directory: {error}"))?;
    write_product_backup(app, local_data, settings, vault, &destination, &app_data)
}

fn write_product_backup(
    app: &tauri::AppHandle,
    local_data: &LocalDataRepository,
    settings: &SettingsRepository,
    vault: &VaultRepository,
    destination: &Path,
    app_data: &Path,
) -> AppResult<BackupExportResult> {
    let timestamp = now_millis();
    let backup_path = unique_child(
        destination,
        &format!("MooTool-Next-Tauri-backup-{timestamp}"),
    );
    let database_dir = backup_path.join("database");
    let config_dir = backup_path.join("config");
    let images_dir = backup_path.join("images");
    let vault_dir = backup_path.join("vault");
    fs::create_dir_all(&database_dir)
        .and_then(|_| fs::create_dir_all(&config_dir))
        .and_then(|_| fs::create_dir_all(&images_dir))
        .map_err(|error| format!("failed to create backup structure: {error}"))?;

    let database_path = database_dir.join(DATABASE_FILE_NAME);
    if let Err(error) = (|| {
        local_data.backup_to(&database_path)?;
        let settings_snapshot = settings.snapshot();
        let settings_bytes = serde_json::to_vec_pretty(&settings_snapshot)
            .map_err(|error| format!("failed to serialize backup settings: {error}"))?;
        fs::write(config_dir.join(SETTINGS_FILE_NAME), settings_bytes)
            .map_err(|error| format!("failed to write backup settings: {error}"))?;

        let image_assets = local_data.list_image_assets()?;
        let source_images = app_data.join("images");
        for asset in &image_assets {
            validate_backup_name(&asset.name)?;
            copy_regular_file(
                &source_images.join(&asset.name),
                &images_dir.join(&asset.name),
                MAX_IMAGE_BYTES,
            )?;
        }

        let (vault_included, vault_file_count) = export_vault(vault, &vault_dir)?;
        let manifest = BackupManifest {
            format_version: BACKUP_FORMAT_VERSION,
            product_id: BACKUP_PRODUCT_ID.into(),
            app_version: env!("CARGO_PKG_VERSION").into(),
            settings_schema_version: SETTINGS_SCHEMA_VERSION,
            database_schema_version: DATABASE_SCHEMA_VERSION,
            created_at: timestamp,
            image_count: image_assets.len(),
            vault_included,
            vault_file_count,
        };
        fs::write(
            backup_path.join(MANIFEST_FILE_NAME),
            serde_json::to_vec_pretty(&manifest)
                .map_err(|error| format!("failed to serialize backup manifest: {error}"))?,
        )
        .map_err(|error| format!("failed to write backup manifest: {error}"))?;
        Ok::<_, String>(())
    })() {
        let _ = fs::remove_dir_all(&backup_path);
        return Err(error.into());
    }

    let database_bytes = fs::metadata(&database_path)
        .map_err(|error| format!("failed to inspect backup database: {error}"))?
        .len();
    let image_count = local_data.list_image_assets()?.len();
    let exported_manifest: BackupManifest =
        read_json_file(&backup_path.join(MANIFEST_FILE_NAME), MAX_SETTINGS_BYTES)?;
    let vault_file_count = exported_manifest.vault_file_count;
    record_data_operation(
        app,
        local_data,
        settings,
        "创建备份",
        &backup_path.display().to_string(),
        "success",
    )?;
    Ok(BackupExportResult {
        backup_path: backup_path.display().to_string(),
        image_count,
        database_bytes,
        vault_file_count,
    })
}

#[tauri::command]
pub fn import_product_backup(
    app: tauri::AppHandle,
    local_data: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
    vault: tauri::State<'_, VaultRepository>,
    source_directory: String,
) -> AppResult<BackupImportResult> {
    let source = existing_directory(&source_directory)?;
    let manifest_path = source.join(MANIFEST_FILE_NAME);
    let manifest: BackupManifest = read_json_file(&manifest_path, MAX_SETTINGS_BYTES)?;
    validate_manifest(&manifest)?;
    let mut imported_settings: AppSettings = read_json_file(
        &source.join("config").join(SETTINGS_FILE_NAME),
        MAX_SETTINGS_BYTES,
    )?;
    imported_settings.schema_version = SETTINGS_SCHEMA_VERSION;
    imported_settings.validate()?;
    let source_database = source.join("database").join(DATABASE_FILE_NAME);
    let database_metadata = fs::metadata(&source_database)
        .map_err(|error| format!("backup database is missing: {error}"))?;
    if !database_metadata.is_file() || database_metadata.len() > MAX_DATABASE_BYTES {
        return Err("backup database is invalid or exceeds 2 GiB".into());
    }
    let image_entries = inspect_backup_database(&source_database)?;
    if image_entries.len() != manifest.image_count {
        return Err("backup image manifest does not match the database".into());
    }
    let vault_entries = if manifest.vault_included {
        inspect_backup_vault(&source.join("vault"))?
    } else {
        Vec::new()
    };
    if vault_entries.len() != manifest.vault_file_count {
        return Err("backup Vault manifest does not match the included JSON files".into());
    }

    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?;
    fs::create_dir_all(&app_data)
        .map_err(|error| format!("failed to initialize Tauri data directory: {error}"))?;
    let timestamp = now_millis();
    let staging = app_data.join(format!(".backup-import-{timestamp}"));
    let staging_images = staging.join("images");
    let staging_vault = staging.join("vault");
    fs::create_dir_all(&staging_images)
        .map_err(|error| format!("failed to create restore staging directory: {error}"))?;
    if manifest.vault_included {
        fs::create_dir_all(&staging_vault)
            .map_err(|error| format!("failed to create Vault restore staging: {error}"))?;
        for (relative, source_file) in &vault_entries {
            let target = staging_vault.join(relative);
            fs::create_dir_all(
                target
                    .parent()
                    .ok_or_else(|| "staged Vault path has no parent".to_string())?,
            )
            .map_err(|error| format!("failed to create staged Vault directory: {error}"))?;
            copy_regular_file(source_file, &target, MAX_IMAGE_BYTES)?;
        }
    }
    for (name, size_bytes) in &image_entries {
        validate_backup_name(name)?;
        let source_file = source.join("images").join(name);
        let target_file = staging_images.join(name);
        copy_regular_file(&source_file, &target_file, MAX_IMAGE_BYTES)?;
        let actual = fs::metadata(&target_file)
            .map_err(|error| format!("failed to inspect staged image: {error}"))?
            .len();
        if actual != *size_bytes {
            let _ = fs::remove_dir_all(&staging);
            return Err(format!("backup image size mismatch: {name}").into());
        }
    }

    let rollback_root = app_data
        .join("restore-rollbacks")
        .join(format!("restore-{timestamp}"));
    let rollback_database = rollback_root.join("database").join(DATABASE_FILE_NAME);
    let rollback_images = rollback_root.join("images");
    let previous_settings = settings.snapshot();
    let restored_vault = app_data
        .join("imported-vaults")
        .join(format!("restore-{timestamp}"));
    if manifest.vault_included {
        imported_settings.vault.root_directory =
            Some(restored_vault.to_string_lossy().into_owned());
    } else {
        imported_settings.vault.root_directory = None;
    }
    fs::create_dir_all(rollback_root.join("config"))
        .map_err(|error| format!("failed to create restore rollback directory: {error}"))?;
    local_data.backup_to(&rollback_database)?;
    fs::write(
        rollback_root.join("config").join(SETTINGS_FILE_NAME),
        serde_json::to_vec_pretty(&previous_settings)
            .map_err(|error| format!("failed to serialize rollback settings: {error}"))?,
    )
    .map_err(|error| format!("failed to write rollback settings: {error}"))?;

    let current_images = app_data.join("images");
    let mut current_images_preserved = false;
    let mut staged_images_installed = false;
    let mut staged_vault_installed = false;
    let restore_result = (|| {
        if current_images.exists() {
            fs::rename(&current_images, &rollback_images)
                .map_err(|error| format!("failed to preserve current images: {error}"))?;
            current_images_preserved = true;
        }
        fs::rename(&staging_images, &current_images)
            .map_err(|error| format!("failed to install restored images: {error}"))?;
        staged_images_installed = true;
        if manifest.vault_included {
            fs::create_dir_all(
                restored_vault
                    .parent()
                    .ok_or_else(|| "restored Vault path has no parent".to_string())?,
            )
            .map_err(|error| format!("failed to create imported Vault directory: {error}"))?;
            fs::rename(&staging_vault, &restored_vault)
                .map_err(|error| format!("failed to install restored JSON Vault: {error}"))?;
            staged_vault_installed = true;
        }
        local_data.restore_from(&source_database)?;
        settings.replace(imported_settings.clone())?;
        if manifest.vault_included {
            vault.configure(&app, restored_vault.clone())?;
        } else {
            vault.disconnect()?;
        }
        Ok::<_, String>(())
    })();

    if let Err(error) = restore_result {
        let _ = local_data.restore_from(&rollback_database);
        let _ = settings.replace(previous_settings.clone());
        if let Some(root) = previous_settings.vault.root_directory.as_ref() {
            let _ = vault.configure(&app, PathBuf::from(root));
        } else {
            let _ = vault.disconnect();
        }
        if staged_images_installed {
            let _ = fs::remove_dir_all(&current_images);
        }
        if current_images_preserved && rollback_images.exists() {
            let _ = fs::rename(&rollback_images, &current_images);
        }
        if staged_vault_installed {
            let _ = fs::remove_dir_all(&restored_vault);
        }
        let _ = fs::remove_dir_all(&staging);
        return Err(format!("backup restore failed and rollback was attempted: {error}").into());
    }
    let _ = fs::remove_dir_all(&staging);
    let restored_settings = settings.snapshot();
    app.emit(SETTINGS_CHANGED_EVENT, &restored_settings)
        .map_err(|error| format!("backup restored but settings synchronization failed: {error}"))?;
    super::desktop::sync_autostart(&app, &restored_settings)
        .map_err(|error| format!("backup restored but launch-at-login sync failed: {error}"))?;
    app.emit(LOCAL_DATA_CHANGED_EVENT, "restore")
        .map_err(|error| format!("backup restored but data synchronization failed: {error}"))?;
    record_data_operation(
        &app,
        &local_data,
        &settings,
        "恢复备份",
        &source.display().to_string(),
        "success",
    )?;
    Ok(BackupImportResult {
        source_path: source.display().to_string(),
        rollback_path: rollback_root.display().to_string(),
        image_count: image_entries.len(),
        vault_file_count: vault_entries.len(),
    })
}

fn validate_manifest(manifest: &BackupManifest) -> Result<(), String> {
    if manifest.product_id != BACKUP_PRODUCT_ID {
        return Err("backup belongs to a different product line".into());
    }
    if manifest.format_version != BACKUP_FORMAT_VERSION
        || manifest.settings_schema_version == 0
        || manifest.settings_schema_version > SETTINGS_SCHEMA_VERSION
        || !(5..=DATABASE_SCHEMA_VERSION).contains(&manifest.database_schema_version)
    {
        return Err("backup schema is not supported by this Tauri version".into());
    }
    Ok(())
}

fn inspect_backup_database(path: &Path) -> Result<Vec<(String, u64)>, String> {
    let connection = Connection::open_with_flags(path, OpenFlags::SQLITE_OPEN_READ_ONLY)
        .map_err(|error| format!("failed to inspect backup database: {error}"))?;
    let schema: u32 = connection
        .query_row("PRAGMA user_version", [], |row| row.get(0))
        .map_err(|error| format!("failed to read backup database schema: {error}"))?;
    if !(5..=DATABASE_SCHEMA_VERSION).contains(&schema) {
        return Err(format!(
            "backup database schema {schema} is not supported (expected {DATABASE_SCHEMA_VERSION})"
        ));
    }
    let quick_check: String = connection
        .query_row("PRAGMA quick_check", [], |row| row.get(0))
        .map_err(|error| format!("backup database integrity check failed: {error}"))?;
    if quick_check != "ok" {
        return Err(format!(
            "backup database integrity check failed: {quick_check}"
        ));
    }
    let mut statement = connection
        .prepare("SELECT name, size_bytes FROM image_assets ORDER BY name")
        .map_err(|error| format!("failed to inspect backup image index: {error}"))?;
    let rows = statement
        .query_map([], |row| {
            let size: i64 = row.get(1)?;
            Ok((row.get(0)?, u64::try_from(size).unwrap_or(u64::MAX)))
        })
        .map_err(|error| format!("failed to inspect backup image index: {error}"))?;
    rows.collect::<Result<Vec<_>, _>>()
        .map_err(|error| format!("failed to inspect backup image index: {error}"))
}

fn export_vault(vault: &VaultRepository, destination: &Path) -> Result<(bool, usize), String> {
    if vault.configured_root()?.is_none() {
        return Ok((false, 0));
    }
    fs::create_dir_all(destination)
        .map_err(|error| format!("failed to create backup Vault directory: {error}"))?;
    let files = vault.list_files()?;
    for file in &files {
        let relative = validate_vault_relative_path(&file.relative_path)?;
        let target = destination.join(relative);
        fs::create_dir_all(
            target
                .parent()
                .ok_or_else(|| "backup Vault path has no parent".to_string())?,
        )
        .map_err(|error| format!("failed to create backup Vault path: {error}"))?;
        let document = vault.read_document(&file.relative_path)?;
        fs::write(&target, document.content)
            .map_err(|error| format!("failed to write backup Vault document: {error}"))?;
    }
    Ok((true, files.len()))
}

fn inspect_backup_vault(root: &Path) -> Result<Vec<(PathBuf, PathBuf)>, String> {
    let metadata = fs::symlink_metadata(root)
        .map_err(|error| format!("backup Vault is unavailable: {error}"))?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err("backup Vault must be a regular directory".into());
    }
    let mut output = Vec::new();
    let mut directories = vec![(root.to_path_buf(), 0usize)];
    while let Some((directory, depth)) = directories.pop() {
        if depth > 32 {
            return Err("backup Vault nesting exceeds 32 levels".into());
        }
        for entry in fs::read_dir(&directory)
            .map_err(|error| format!("failed to inspect backup Vault: {error}"))?
        {
            let entry =
                entry.map_err(|error| format!("failed to inspect backup Vault: {error}"))?;
            let path = entry.path();
            let metadata = path
                .symlink_metadata()
                .map_err(|error| format!("failed to inspect backup Vault entry: {error}"))?;
            if metadata.file_type().is_symlink() {
                return Err("backup Vault cannot contain symbolic links".into());
            }
            if metadata.is_dir() {
                directories.push((path, depth + 1));
                continue;
            }
            if !metadata.is_file() || metadata.len() > MAX_IMAGE_BYTES {
                return Err("backup Vault contains an invalid file".into());
            }
            let relative_text = path
                .strip_prefix(root)
                .map_err(|_| "backup Vault path escaped its root".to_string())?
                .to_string_lossy()
                .replace(std::path::MAIN_SEPARATOR, "/");
            let relative = validate_vault_relative_path(&relative_text)?;
            output.push((relative, path));
            if output.len() > 10_000 {
                return Err("backup Vault contains more than 10000 files".into());
            }
        }
    }
    output.sort_by(|left, right| left.0.cmp(&right.0));
    Ok(output)
}

fn validate_vault_relative_path(value: &str) -> Result<PathBuf, String> {
    let path = Path::new(value);
    if value.is_empty()
        || value.len() > 1_024
        || !path
            .extension()
            .and_then(|value| value.to_str())
            .is_some_and(|extension| extension.eq_ignore_ascii_case("json"))
        || path
            .components()
            .any(|component| !matches!(component, std::path::Component::Normal(_)))
    {
        return Err("backup contains an invalid Vault path".into());
    }
    Ok(path.to_path_buf())
}

fn record_data_operation(
    app: &tauri::AppHandle,
    local_data: &LocalDataRepository,
    settings: &SettingsRepository,
    action: &str,
    summary: &str,
    status: &str,
) -> Result<(), String> {
    let timestamp = now_millis();
    local_data.record_operation(
        OperationHistory {
            id: format!("data-{timestamp}"),
            tool_id: "system-data".into(),
            action: action.into(),
            summary: summary.chars().take(2_000).collect(),
            status: status.into(),
            input_text: String::new(),
            output_text: String::new(),
            metadata_json: "{}".into(),
            created_at: timestamp,
        },
        settings.snapshot().data.history_limit,
    )?;
    app.emit(HISTORY_CHANGED_EVENT, ())
        .map_err(|error| format!("operation completed but history synchronization failed: {error}"))
}

fn existing_directory(value: &str) -> Result<PathBuf, String> {
    let path = PathBuf::from(value);
    if !path.is_absolute() {
        return Err("backup path must be absolute".into());
    }
    let metadata = fs::symlink_metadata(&path)
        .map_err(|error| format!("backup directory is unavailable: {error}"))?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err("backup path must be a real directory".into());
    }
    path.canonicalize()
        .map_err(|error| format!("failed to canonicalize backup directory: {error}"))
}

fn read_json_file<T: serde::de::DeserializeOwned>(
    path: &Path,
    max_bytes: u64,
) -> Result<T, String> {
    let metadata = fs::symlink_metadata(path)
        .map_err(|error| format!("backup file {} is unavailable: {error}", path.display()))?;
    if !metadata.is_file() || metadata.file_type().is_symlink() || metadata.len() > max_bytes {
        return Err(format!("backup file {} is invalid", path.display()));
    }
    serde_json::from_slice(
        &fs::read(path)
            .map_err(|error| format!("failed to read backup file {}: {error}", path.display()))?,
    )
    .map_err(|error| format!("failed to parse backup file {}: {error}", path.display()))
}

fn copy_regular_file(source: &Path, target: &Path, max_bytes: u64) -> Result<(), String> {
    let metadata = fs::symlink_metadata(source)
        .map_err(|error| format!("backup source {} is unavailable: {error}", source.display()))?;
    if !metadata.is_file() || metadata.file_type().is_symlink() || metadata.len() > max_bytes {
        return Err(format!("backup source {} is invalid", source.display()));
    }
    fs::copy(source, target)
        .map(|_| ())
        .map_err(|error| format!("failed to copy {}: {error}", source.display()))
}

fn validate_backup_name(name: &str) -> Result<(), String> {
    if name.is_empty()
        || name.chars().count() > 180
        || name == "."
        || name == ".."
        || name.starts_with('.')
        || name
            .chars()
            .any(|character| character.is_control() || matches!(character, '/' | '\\' | ':'))
    {
        return Err("backup contains an invalid image file name".into());
    }
    Ok(())
}

fn unique_child(parent: &Path, base: &str) -> PathBuf {
    let preferred = parent.join(base);
    if !preferred.exists() {
        return preferred;
    }
    for index in 2..10_000 {
        let candidate = parent.join(format!("{base}-{index}"));
        if !candidate.exists() {
            return candidate;
        }
    }
    parent.join(format!("{base}-{}", now_millis()))
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
    fn rejects_other_product_lines_and_unsafe_names() {
        let manifest = BackupManifest {
            format_version: BACKUP_FORMAT_VERSION,
            product_id: "com.rememberber.mootool.next.electron".into(),
            app_version: "1.0.0".into(),
            settings_schema_version: SETTINGS_SCHEMA_VERSION,
            database_schema_version: DATABASE_SCHEMA_VERSION,
            created_at: 1,
            image_count: 0,
            vault_included: false,
            vault_file_count: 0,
        };
        assert!(validate_manifest(&manifest).is_err());
        assert!(validate_backup_name("../electron.png").is_err());
        assert!(validate_backup_name("tauri.png").is_ok());

        let mut older_tauri = manifest;
        older_tauri.product_id = BACKUP_PRODUCT_ID.into();
        older_tauri.settings_schema_version = SETTINGS_SCHEMA_VERSION - 1;
        assert!(validate_manifest(&older_tauri).is_ok());
    }

    #[test]
    fn inspects_nested_vault_files_without_accepting_unsafe_entries() {
        let directory = tempfile::TempDir::new().expect("backup Vault");
        fs::create_dir(directory.path().join("nested")).expect("nested directory");
        fs::write(
            directory.path().join("nested/example.json"),
            "{\"ok\":true}",
        )
        .expect("JSON file");
        let entries = inspect_backup_vault(directory.path()).expect("inspect Vault");
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].0, PathBuf::from("nested/example.json"));

        fs::write(directory.path().join("unexpected.txt"), "not allowed").expect("unexpected file");
        assert!(inspect_backup_vault(directory.path()).is_err());
    }
}
