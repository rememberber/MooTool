use std::{
    collections::{HashMap, HashSet},
    fs::{self, File},
    io::Read,
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

use rusqlite::{Connection, OpenFlags};
use serde_json::Value;
use sha2::{Digest, Sha256};
use tauri::{Emitter, Manager};

use crate::{
    contracts::{
        error::{AppError, AppResult},
        image::ImageAssetSummary,
        local_data::{
            HostProfile, OperationHistory, QuickNote, TranslationHistory, TranslationWord,
        },
        product_import::{
            ProductImportCounts, ProductImportPreview, ProductImportRecords, ProductImportResult,
            ProductImportSource,
        },
        settings::{AccentColor, AppLanguage, AppSettings, ThemePreference},
        translation::TranslationProvider,
    },
    repositories::{
        local_data::LocalDataRepository, settings::SettingsRepository, vault::VaultRepository,
    },
};

const MAX_DATABASE_BYTES: u64 = 2 * 1024 * 1024 * 1024;
const MAX_SETTINGS_BYTES: u64 = 2 * 1024 * 1024;
const MAX_TEXT_BYTES: u64 = 2 * 1024 * 1024;
const MAX_IMAGE_BYTES: u64 = 20 * 1024 * 1024;
const MAX_TOTAL_FILE_BYTES: u64 = 1024 * 1024 * 1024;
const MAX_FILES: usize = 5_000;

#[derive(Debug)]
struct SourceFile {
    source: PathBuf,
    relative: PathBuf,
}

#[derive(Debug)]
struct SourceImage {
    source: PathBuf,
    preferred_name: String,
    mime_type: String,
    width: u32,
    height: u32,
    size_bytes: usize,
}

#[derive(Default)]
struct InstalledImportFiles {
    image_paths: Vec<PathBuf>,
    image_assets: Vec<ImageAssetSummary>,
    vault_path: Option<PathBuf>,
}

impl InstalledImportFiles {
    fn cleanup(&self) {
        cleanup_import_files(&self.image_paths, self.vault_path.as_deref());
    }
}

#[derive(Debug, Default)]
struct SafeSettingsPatch {
    language: Option<AppLanguage>,
    auto_check_updates: Option<bool>,
    theme: Option<ThemePreference>,
    accent_color: Option<AccentColor>,
    sidebar_compact: Option<bool>,
    editor_font_size: Option<u8>,
    history_limit: Option<u16>,
    vault_auto_commit: Option<bool>,
}

impl SafeSettingsPatch {
    fn is_empty(&self) -> bool {
        self.language.is_none()
            && self.auto_check_updates.is_none()
            && self.theme.is_none()
            && self.accent_color.is_none()
            && self.sidebar_compact.is_none()
            && self.editor_font_size.is_none()
            && self.history_limit.is_none()
            && self.vault_auto_commit.is_none()
    }

    fn apply(&self, settings: &mut AppSettings) {
        if let Some(value) = self.language {
            settings.general.language = value;
        }
        if let Some(value) = self.auto_check_updates {
            settings.general.auto_check_updates = value;
        }
        if let Some(value) = self.theme {
            settings.appearance.theme = value;
        }
        if let Some(value) = self.accent_color {
            settings.appearance.accent_color = value;
        }
        if let Some(value) = self.sidebar_compact {
            settings.layout.sidebar_compact = value;
        }
        if let Some(value) = self.editor_font_size {
            settings.editor.font_size = value;
        }
        if let Some(value) = self.history_limit {
            settings.data.history_limit = value;
        }
        if let Some(value) = self.vault_auto_commit {
            settings.vault.auto_commit = value;
        }
    }
}

struct ScannedSource {
    preview: ProductImportPreview,
    records: ProductImportRecords,
    settings_patch: SafeSettingsPatch,
    vault_files: Vec<SourceFile>,
    images: Vec<SourceImage>,
}

#[tauri::command]
pub fn preview_product_import(
    app: tauri::AppHandle,
    local_data: tauri::State<'_, LocalDataRepository>,
    source_product: ProductImportSource,
    source_directory: String,
) -> AppResult<ProductImportPreview> {
    Ok(scan_product_source(&app, &local_data, source_product, &source_directory)?.preview)
}

#[tauri::command]
pub fn run_product_import(
    app: tauri::AppHandle,
    local_data: tauri::State<'_, LocalDataRepository>,
    settings: tauri::State<'_, SettingsRepository>,
    vault: tauri::State<'_, VaultRepository>,
    source_product: ProductImportSource,
    source_directory: String,
    expected_fingerprint: String,
) -> AppResult<ProductImportResult> {
    let mut source = scan_product_source(&app, &local_data, source_product, &source_directory)?;
    if !is_valid_fingerprint(&expected_fingerprint)
        || source.preview.fingerprint != expected_fingerprint
    {
        return Err(AppError::new(
            "product_import_source_changed",
            "The selected source changed after preview; scan it again before importing",
            true,
        ));
    }
    if source.preview.already_imported {
        return Err(AppError::new(
            "product_import_duplicate",
            "This source snapshot was already imported into MooTool Next Tauri",
            false,
        ));
    }

    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?;
    fs::create_dir_all(&app_data)
        .map_err(|error| format!("failed to initialize Tauri data directory: {error}"))?;
    let timestamp = now_millis();
    let report_path = prepare_import_report(&app_data, timestamp, &source.preview)?;
    let backup = super::backup::create_internal_backup(
        &app,
        &local_data,
        &settings,
        &vault,
        &app_data.join("migration-backups"),
    )?;

    let installed = install_import_files(
        &app_data,
        source_product,
        timestamp,
        &source.vault_files,
        &source.images,
        &local_data,
    );
    let installed = installed.map_err(AppError::from)?;
    source.records.images.extend(installed.image_assets.clone());

    let previous_settings = settings.snapshot();
    let mut next_settings = previous_settings.clone();
    source.settings_patch.apply(&mut next_settings);
    let settings_changed = next_settings != previous_settings;
    let saved_settings = if settings_changed {
        match settings.replace(next_settings) {
            Ok(saved) => Some(saved),
            Err(error) => {
                installed.cleanup();
                return Err(error.into());
            }
        }
    } else {
        None
    };

    let import_result = local_data.import_product_records(
        source_product.id(),
        &source.preview.source_directory,
        &source.preview.fingerprint,
        source.records,
        saved_settings
            .as_ref()
            .unwrap_or(&previous_settings)
            .data
            .history_limit,
        timestamp,
    );
    let (mut imported, mut skipped) = match import_result {
        Ok(result) => result,
        Err(error) => {
            if settings_changed {
                let _ = settings.replace(previous_settings);
            }
            installed.cleanup();
            return Err(error.into());
        }
    };
    imported.vault_files = source.vault_files.len();
    imported.settings = usize::from(settings_changed);
    skipped.vault_files = source
        .preview
        .items
        .vault_files
        .saturating_sub(imported.vault_files);
    skipped.settings = source
        .preview
        .items
        .settings
        .saturating_sub(imported.settings);

    if let Some(saved) = saved_settings.as_ref() {
        if let Err(error) = app.emit(super::settings::SETTINGS_CHANGED_EVENT, saved) {
            tracing::warn!(error = %error, "import completed but settings event could not be emitted");
        }
    }
    if let Err(error) = app.emit(
        super::local_data::LOCAL_DATA_CHANGED_EVENT,
        "product-import",
    ) {
        tracing::warn!(error = %error, "import completed but local-data event could not be emitted");
    }

    if let Err(error) = write_import_report(
        &report_path,
        &source.preview,
        &imported,
        &skipped,
        &backup.backup_path,
        installed.vault_path.as_deref(),
    ) {
        tracing::warn!(
            error = %crate::contracts::error::redact_for_log(&error),
            "import completed but migration report could not be finalized"
        );
    }
    tracing::info!(
        source.product = source_product.id(),
        source.fingerprint = %source.preview.fingerprint,
        imported.total = imported.total(),
        "Imported data from another MooTool product line"
    );
    Ok(ProductImportResult {
        preview: source.preview,
        imported,
        skipped,
        backup_path: backup.backup_path,
        report_path: report_path.display().to_string(),
        imported_vault_path: installed.vault_path.map(|path| path.display().to_string()),
    })
}

fn scan_product_source(
    app: &tauri::AppHandle,
    local_data: &LocalDataRepository,
    source_product: ProductImportSource,
    requested_directory: &str,
) -> Result<ScannedSource, String> {
    let root = resolve_source_root(app, requested_directory)?;
    let (database_path, settings_path, quick_note_root, vault_root, image_root) =
        match source_product {
            ProductImportSource::Java => (
                root.join("MooTool.db"),
                root.join("config").join("config.setting"),
                root.join("quick-notes"),
                root.join("json-beauty"),
                root.join("images"),
            ),
            ProductImportSource::NextElectron => (
                root.join("MooToolNext.db"),
                root.join("mootool-next.json"),
                root.join("quick-notes"),
                root.join("json-vault"),
                root.join("images"),
            ),
        };
    let database_found = validate_optional_file(&database_path, MAX_DATABASE_BYTES)?;
    let database_wal_path = sqlite_sidecar_path(&database_path, "-wal");
    let database_wal_found =
        database_found && validate_optional_file(&database_wal_path, MAX_DATABASE_BYTES)?;
    let settings_found = validate_optional_file(&settings_path, MAX_SETTINGS_BYTES)?;
    let quick_note_files = collect_files(&quick_note_root, &["md", "txt", "text"], MAX_TEXT_BYTES)?;
    let vault_files = collect_files(&vault_root, &["json"], MAX_TEXT_BYTES)?;
    let image_files = collect_files(
        &image_root,
        &["png", "jpg", "jpeg", "webp", "gif"],
        MAX_IMAGE_BYTES,
    )?;
    if !database_found
        && !settings_found
        && quick_note_files.is_empty()
        && vault_files.is_empty()
        && image_files.is_empty()
    {
        return Err(format!(
            "No {} data was found in the selected directory",
            source_product.id()
        ));
    }

    for file in &vault_files {
        let bytes = read_limited_file(&file.source, MAX_TEXT_BYTES)?;
        serde_json::from_slice::<Value>(&bytes).map_err(|error| {
            format!(
                "source Vault contains invalid JSON at {}: {error}",
                file.relative.display()
            )
        })?;
    }
    let mut fingerprint_files = Vec::new();
    if database_found {
        fingerprint_files.push(SourceFile {
            source: database_path.clone(),
            relative: PathBuf::from("database"),
        });
    }
    if database_wal_found {
        fingerprint_files.push(SourceFile {
            source: database_wal_path,
            relative: PathBuf::from("database-wal"),
        });
    }
    if settings_found {
        fingerprint_files.push(SourceFile {
            source: settings_path.clone(),
            relative: PathBuf::from("settings"),
        });
    }
    fingerprint_files.extend(
        quick_note_files
            .iter()
            .map(|file| categorized_source_file("quick-notes", file)),
    );
    fingerprint_files.extend(
        vault_files
            .iter()
            .map(|file| categorized_source_file("json-vault", file)),
    );
    fingerprint_files.extend(
        image_files
            .iter()
            .map(|file| categorized_source_file("images", file)),
    );
    let fingerprint = source_fingerprint(source_product, &fingerprint_files)?;
    let base_time = now_millis();
    let mut records = if database_found {
        scan_database(&database_path, source_product, &fingerprint, base_time)?
    } else {
        ProductImportRecords::default()
    };
    append_quick_note_files(&mut records, &quick_note_files, &fingerprint, base_time)?;
    let images = scan_images(&image_files)?;
    let settings_patch = if settings_found {
        let bytes = read_limited_file(&settings_path, MAX_SETTINGS_BYTES)?;
        match source_product {
            ProductImportSource::Java => parse_java_settings(&String::from_utf8_lossy(&bytes)),
            ProductImportSource::NextElectron => parse_electron_settings(&bytes)?,
        }
    } else {
        SafeSettingsPatch::default()
    };
    if source_fingerprint(source_product, &fingerprint_files)? != fingerprint {
        return Err(
            "source changed while it was being scanned; try again after closing the source product"
                .into(),
        );
    }
    let mut counts = records.counts();
    counts.vault_files = vault_files.len();
    counts.images = images.len();
    counts.settings = usize::from(!settings_patch.is_empty());
    if counts.total() > 25_000 {
        return Err("source contains too many importable records".into());
    }
    let mut warnings = vec!["secretsSkipped".into(), "sourceRemainsReadOnly".into()];
    if !database_found {
        warnings.push("databaseNotFound".into());
    }
    if !settings_found {
        warnings.push("settingsNotFound".into());
    }
    let already_imported = local_data.has_product_import(&fingerprint)?;
    let source_directory = root.display().to_string();
    let total_items = counts.total();
    Ok(ScannedSource {
        preview: ProductImportPreview {
            source_product,
            source_directory,
            fingerprint,
            database_found,
            settings_found,
            already_imported,
            items: counts,
            total_items,
            warnings,
        },
        records,
        settings_patch,
        vault_files,
        images,
    })
}

fn resolve_source_root(app: &tauri::AppHandle, value: &str) -> Result<PathBuf, String> {
    let requested = PathBuf::from(value.trim());
    if value.trim().is_empty() || value.len() > 4_096 || !requested.is_absolute() {
        return Err("product import source must be an absolute directory".into());
    }
    let metadata = fs::symlink_metadata(&requested)
        .map_err(|error| format!("product import source does not exist: {error}"))?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err("product import source must be a real directory, not a symbolic link".into());
    }
    let root = requested
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize product import source: {error}"))?;
    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?;
    fs::create_dir_all(&app_data)
        .map_err(|error| format!("failed to initialize Tauri data directory: {error}"))?;
    let app_data = app_data
        .canonicalize()
        .map_err(|error| format!("failed to canonicalize Tauri data directory: {error}"))?;
    if root.starts_with(&app_data) || app_data.starts_with(&root) {
        return Err("another product source and Tauri data must use separate directories".into());
    }
    Ok(root)
}

fn validate_optional_file(path: &Path, maximum: u64) -> Result<bool, String> {
    let Ok(metadata) = fs::symlink_metadata(path) else {
        return Ok(false);
    };
    if !metadata.is_file() || metadata.file_type().is_symlink() || metadata.len() > maximum {
        return Err(format!(
            "source file is unsafe or too large: {}",
            path.display()
        ));
    }
    Ok(true)
}

fn collect_files(
    root: &Path,
    extensions: &[&str],
    per_file_limit: u64,
) -> Result<Vec<SourceFile>, String> {
    if !root.exists() {
        return Ok(Vec::new());
    }
    let metadata = fs::symlink_metadata(root).map_err(|error| {
        format!(
            "failed to inspect source directory {}: {error}",
            root.display()
        )
    })?;
    if !metadata.is_dir() || metadata.file_type().is_symlink() {
        return Err(format!("source directory is unsafe: {}", root.display()));
    }
    let mut result = Vec::new();
    let mut stack = vec![(root.to_path_buf(), 0_usize)];
    let mut total_bytes = 0_u64;
    while let Some((directory, depth)) = stack.pop() {
        if depth > 16 {
            return Err("source directory nesting exceeds 16 levels".into());
        }
        let mut entries = fs::read_dir(&directory)
            .map_err(|error| {
                format!(
                    "failed to read source directory {}: {error}",
                    directory.display()
                )
            })?
            .collect::<Result<Vec<_>, _>>()
            .map_err(|error| format!("failed to read source directory entry: {error}"))?;
        entries.sort_by_key(|entry| entry.file_name());
        for entry in entries {
            let path = entry.path();
            let metadata = fs::symlink_metadata(&path).map_err(|error| {
                format!("failed to inspect source entry {}: {error}", path.display())
            })?;
            if metadata.file_type().is_symlink() {
                return Err(format!(
                    "source contains a symbolic link: {}",
                    path.display()
                ));
            }
            if metadata.is_dir() {
                stack.push((path, depth + 1));
                continue;
            }
            if !metadata.is_file() {
                return Err(format!(
                    "source contains an unsupported entry: {}",
                    path.display()
                ));
            }
            let extension = path
                .extension()
                .and_then(|value| value.to_str())
                .unwrap_or_default();
            if !extensions
                .iter()
                .any(|allowed| extension.eq_ignore_ascii_case(allowed))
            {
                continue;
            }
            if metadata.len() == 0 || metadata.len() > per_file_limit {
                return Err(format!(
                    "source file is empty or too large: {}",
                    path.display()
                ));
            }
            total_bytes = total_bytes.saturating_add(metadata.len());
            if total_bytes > MAX_TOTAL_FILE_BYTES || result.len() >= MAX_FILES {
                return Err("source files exceed the import size or count limit".into());
            }
            let relative = path
                .strip_prefix(root)
                .map_err(|_| "source file escaped the selected directory".to_string())?
                .to_path_buf();
            result.push(SourceFile {
                source: path,
                relative,
            });
        }
    }
    result.sort_by(|left, right| left.relative.cmp(&right.relative));
    Ok(result)
}

fn categorized_source_file(category: &str, file: &SourceFile) -> SourceFile {
    SourceFile {
        source: file.source.clone(),
        relative: PathBuf::from(category).join(&file.relative),
    }
}

fn sqlite_sidecar_path(database_path: &Path, suffix: &str) -> PathBuf {
    let file_name = database_path
        .file_name()
        .and_then(|value| value.to_str())
        .unwrap_or("database");
    database_path.with_file_name(format!("{file_name}{suffix}"))
}

fn source_fingerprint(
    source_product: ProductImportSource,
    files: &[SourceFile],
) -> Result<String, String> {
    let mut hash = Sha256::new();
    hash.update(b"mootool-next-tauri-import-v1\0");
    hash.update(source_product.id().as_bytes());
    for file in files {
        hash.update(b"\0path\0");
        hash.update(file.relative.to_string_lossy().as_bytes());
        let mut input = File::open(&file.source).map_err(|error| {
            format!(
                "failed to open source file {}: {error}",
                file.source.display()
            )
        })?;
        let mut buffer = [0_u8; 1024 * 1024];
        loop {
            let read = input.read(&mut buffer).map_err(|error| {
                format!(
                    "failed to hash source file {}: {error}",
                    file.source.display()
                )
            })?;
            if read == 0 {
                break;
            }
            hash.update(&buffer[..read]);
        }
    }
    Ok(hex_digest(hash.finalize().as_slice()))
}

fn scan_database(
    path: &Path,
    source_product: ProductImportSource,
    fingerprint: &str,
    base_time: i64,
) -> Result<ProductImportRecords, String> {
    let connection = Connection::open_with_flags(path, OpenFlags::SQLITE_OPEN_READ_ONLY)
        .map_err(|error| format!("failed to open source database read-only: {error}"))?;
    connection
        .query_row("PRAGMA quick_check", [], |row| row.get::<_, String>(0))
        .map_err(|error| format!("failed to check source database: {error}"))
        .and_then(|result| {
            if result == "ok" {
                Ok(())
            } else {
                Err(format!("source database integrity check failed: {result}"))
            }
        })?;
    let mut records = ProductImportRecords::default();
    read_database_notes(&connection, fingerprint, base_time, &mut records)?;
    read_database_hosts(&connection, fingerprint, base_time, &mut records)?;
    read_database_translation_words(&connection, fingerprint, base_time, &mut records)?;
    read_database_translation_history(&connection, fingerprint, base_time, &mut records)?;
    read_database_operation_history(
        &connection,
        source_product,
        fingerprint,
        base_time,
        &mut records,
    )?;
    Ok(records)
}

fn table_exists(connection: &Connection, table: &str) -> Result<bool, String> {
    connection
        .query_row(
            "SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?1)",
            [table],
            |row| row.get(0),
        )
        .map_err(|error| format!("failed to inspect source database table {table}: {error}"))
}

fn read_database_notes(
    connection: &Connection,
    fingerprint: &str,
    base_time: i64,
    records: &mut ProductImportRecords,
) -> Result<(), String> {
    if !table_exists(connection, "t_quick_note")? {
        return Ok(());
    }
    let mut statement = connection
        .prepare(
            "SELECT CAST(id AS TEXT), COALESCE(name, ''), COALESCE(content, '')
             FROM t_quick_note ORDER BY id LIMIT 5000",
        )
        .map_err(|error| format!("failed to read source quick notes: {error}"))?;
    let rows = statement
        .query_map([], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
            ))
        })
        .map_err(|error| format!("failed to query source quick notes: {error}"))?;
    for (index, row) in rows.enumerate() {
        let (source_id, title, content) =
            row.map_err(|error| format!("failed to read source quick note: {error}"))?;
        records.quick_notes.push(QuickNote {
            id: imported_id(fingerprint, "note-db", &source_id),
            title: truncate_chars(title.trim(), 256),
            content: truncate_utf8_bytes(&content, 2 * 1024 * 1024),
            tags: Vec::new(),
            color: "default".into(),
            folder_path: String::new(),
            editor_font: "default".into(),
            line_height: "normal".into(),
            line_wrapping: true,
            syntax: "markdown".into(),
            pinned: false,
            created_at: base_time.saturating_add(index as i64),
            updated_at: base_time.saturating_add(index as i64),
        });
    }
    Ok(())
}

fn read_database_hosts(
    connection: &Connection,
    fingerprint: &str,
    base_time: i64,
    records: &mut ProductImportRecords,
) -> Result<(), String> {
    if !table_exists(connection, "t_host")? {
        return Ok(());
    }
    let mut statement = connection
        .prepare(
            "SELECT CAST(id AS TEXT), COALESCE(name, ''), COALESCE(content, '')
             FROM t_host ORDER BY id LIMIT 5000",
        )
        .map_err(|error| format!("failed to read source host profiles: {error}"))?;
    let rows = statement
        .query_map([], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
            ))
        })
        .map_err(|error| format!("failed to query source host profiles: {error}"))?;
    for (index, row) in rows.enumerate() {
        let (source_id, name, content) =
            row.map_err(|error| format!("failed to read source host profile: {error}"))?;
        if name.trim().is_empty() {
            continue;
        }
        records.host_profiles.push(HostProfile {
            id: imported_id(fingerprint, "host", &source_id),
            name: truncate_chars(name.trim(), 256),
            content: truncate_utf8_bytes(&content, 2 * 1024 * 1024),
            created_at: base_time.saturating_add(index as i64),
            updated_at: base_time.saturating_add(index as i64),
        });
    }
    Ok(())
}

fn read_database_translation_words(
    connection: &Connection,
    fingerprint: &str,
    base_time: i64,
    records: &mut ProductImportRecords,
) -> Result<(), String> {
    if !table_exists(connection, "t_translation_word")? {
        return Ok(());
    }
    let mut statement = connection
        .prepare(
            "SELECT CAST(id AS TEXT), COALESCE(source_text, ''), COALESCE(target_text, ''),
                    COALESCE(source_lang, 'auto'), COALESCE(target_lang, 'zh-CN'), COALESCE(remark, '')
             FROM t_translation_word ORDER BY id LIMIT 5000",
        )
        .map_err(|error| format!("failed to read source translation words: {error}"))?;
    let rows = statement
        .query_map([], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
                row.get::<_, String>(3)?,
                row.get::<_, String>(4)?,
                row.get::<_, String>(5)?,
            ))
        })
        .map_err(|error| format!("failed to query source translation words: {error}"))?;
    for (index, row) in rows.enumerate() {
        let (source_id, source_text, target_text, source_lang, target_lang, remark) =
            row.map_err(|error| format!("failed to read source translation word: {error}"))?;
        if source_text.trim().is_empty() {
            continue;
        }
        let (source_lang, target_lang) = normalize_language_pair(&source_lang, &target_lang);
        records.translation_words.push(TranslationWord {
            id: imported_id(fingerprint, "word", &source_id),
            source_text: truncate_chars(source_text.trim(), 50_000),
            target_text: truncate_chars(target_text.trim(), 50_000),
            source_lang,
            target_lang,
            remark: truncate_chars(remark.trim(), 2_000),
            created_at: base_time.saturating_add(index as i64),
            updated_at: base_time.saturating_add(index as i64),
        });
    }
    Ok(())
}

fn read_database_translation_history(
    connection: &Connection,
    fingerprint: &str,
    base_time: i64,
    records: &mut ProductImportRecords,
) -> Result<(), String> {
    if !table_exists(connection, "t_translation_history")? {
        return Ok(());
    }
    let mut statement = connection
        .prepare(
            "SELECT CAST(id AS TEXT), COALESCE(source_text, ''), COALESCE(target_text, ''),
                    COALESCE(source_lang, 'auto'), COALESCE(target_lang, 'zh-CN'),
                    COALESCE(translator_type, 'google')
             FROM t_translation_history ORDER BY id LIMIT 5000",
        )
        .map_err(|error| format!("failed to read source translation history: {error}"))?;
    let rows = statement
        .query_map([], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
                row.get::<_, String>(3)?,
                row.get::<_, String>(4)?,
                row.get::<_, String>(5)?,
            ))
        })
        .map_err(|error| format!("failed to query source translation history: {error}"))?;
    for (index, row) in rows.enumerate() {
        let (source_id, source_text, target_text, source_lang, target_lang, provider) =
            row.map_err(|error| format!("failed to read source translation history row: {error}"))?;
        if source_text.trim().is_empty() {
            continue;
        }
        let (source_lang, target_lang) = normalize_language_pair(&source_lang, &target_lang);
        records.translation_history.push(TranslationHistory {
            id: imported_id(fingerprint, "translation-history", &source_id),
            source_text: truncate_chars(source_text.trim(), 50_000),
            target_text: truncate_chars(target_text.trim(), 50_000),
            source_lang,
            target_lang,
            provider: if provider.eq_ignore_ascii_case("bing") {
                TranslationProvider::Bing
            } else {
                TranslationProvider::Google
            },
            created_at: base_time.saturating_add(index as i64),
        });
    }
    Ok(())
}

fn read_database_operation_history(
    connection: &Connection,
    source_product: ProductImportSource,
    fingerprint: &str,
    base_time: i64,
    records: &mut ProductImportRecords,
) -> Result<(), String> {
    if !table_exists(connection, "t_func_history")? {
        return Ok(());
    }
    let mut statement = connection
        .prepare(
            "SELECT CAST(id AS TEXT), COALESCE(func_type, 'import'), COALESCE(summary, '')
             FROM t_func_history ORDER BY id DESC LIMIT 5000",
        )
        .map_err(|error| format!("failed to read source operation history: {error}"))?;
    let rows = statement
        .query_map([], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
            ))
        })
        .map_err(|error| format!("failed to query source operation history: {error}"))?;
    for (index, row) in rows.enumerate() {
        let (source_id, tool_id, summary) =
            row.map_err(|error| format!("failed to read source operation: {error}"))?;
        records.operation_history.push(OperationHistory {
            id: imported_id(fingerprint, "operation", &source_id),
            tool_id: normalize_tool_id(&tool_id),
            action: format!("Imported from {}", source_product.id()),
            summary: truncate_chars(summary.trim(), 2_000),
            status: "success".into(),
            input_text: String::new(),
            output_text: String::new(),
            metadata_json: "{}".into(),
            created_at: base_time.saturating_add(index as i64),
        });
    }
    Ok(())
}

fn append_quick_note_files(
    records: &mut ProductImportRecords,
    files: &[SourceFile],
    fingerprint: &str,
    base_time: i64,
) -> Result<(), String> {
    for (index, file) in files.iter().enumerate() {
        let bytes = read_limited_file(&file.source, MAX_TEXT_BYTES)?;
        let content = String::from_utf8(bytes)
            .map_err(|_| format!("quick note is not UTF-8: {}", file.relative.display()))?;
        let title = file
            .relative
            .file_stem()
            .and_then(|value| value.to_str())
            .unwrap_or("Imported note");
        records.quick_notes.push(QuickNote {
            id: imported_id(fingerprint, "note-file", &file.relative.to_string_lossy()),
            title: truncate_chars(title, 256),
            content: truncate_utf8_bytes(&content, 2 * 1024 * 1024),
            tags: Vec::new(),
            color: "default".into(),
            folder_path: file
                .relative
                .parent()
                .and_then(|path| path.to_str())
                .unwrap_or_default()
                .replace('\\', "/"),
            editor_font: "default".into(),
            line_height: "normal".into(),
            line_wrapping: true,
            syntax: "markdown".into(),
            pinned: false,
            created_at: base_time.saturating_add(index as i64),
            updated_at: base_time.saturating_add(index as i64),
        });
    }
    Ok(())
}

fn scan_images(files: &[SourceFile]) -> Result<Vec<SourceImage>, String> {
    files
        .iter()
        .map(|file| {
            let bytes = read_limited_file(&file.source, MAX_IMAGE_BYTES)?;
            let (mime_type, width, height) = image_info(&bytes).ok_or_else(|| {
                format!(
                    "unsupported or invalid source image: {}",
                    file.relative.display()
                )
            })?;
            let preferred_name = file
                .relative
                .file_name()
                .and_then(|value| value.to_str())
                .ok_or_else(|| "source image name is not valid UTF-8".to_string())?
                .to_string();
            Ok(SourceImage {
                source: file.source.clone(),
                preferred_name,
                mime_type: mime_type.into(),
                width,
                height,
                size_bytes: bytes.len(),
            })
        })
        .collect()
}

fn install_import_files(
    app_data: &Path,
    source_product: ProductImportSource,
    timestamp: i64,
    vault_files: &[SourceFile],
    images: &[SourceImage],
    local_data: &LocalDataRepository,
) -> Result<InstalledImportFiles, String> {
    let mut installed = InstalledImportFiles::default();
    let result = (|| {
        if !vault_files.is_empty() {
            let vault_root = unique_directory(
                &app_data.join("imported-vaults"),
                &format!("{}-{timestamp}", source_product.id()),
            )?;
            fs::create_dir_all(&vault_root)
                .map_err(|error| format!("failed to create imported Vault directory: {error}"))?;
            for file in vault_files {
                let target = vault_root.join(&file.relative);
                if !target.starts_with(&vault_root) {
                    return Err("imported Vault file escaped its destination".into());
                }
                if let Some(parent) = target.parent() {
                    fs::create_dir_all(parent).map_err(|error| {
                        format!("failed to create imported Vault folder: {error}")
                    })?;
                }
                fs::copy(&file.source, &target)
                    .map_err(|error| format!("failed to copy imported Vault file: {error}"))?;
            }
            installed.vault_path = Some(vault_root);
        }

        let image_root = app_data.join("images");
        fs::create_dir_all(&image_root)
            .map_err(|error| format!("failed to create Tauri image library: {error}"))?;
        let mut used_names = local_data
            .list_image_assets()?
            .into_iter()
            .map(|image| image.name)
            .collect::<HashSet<_>>();
        for image in images {
            let name = unique_image_name(&image.preferred_name, &image.mime_type, &mut used_names);
            let target = image_root.join(&name);
            fs::copy(&image.source, &target)
                .map_err(|error| format!("failed to copy imported image: {error}"))?;
            installed.image_paths.push(target);
            installed.image_assets.push(ImageAssetSummary {
                name,
                mime_type: image.mime_type.clone(),
                width: image.width,
                height: image.height,
                size_bytes: image.size_bytes,
                updated_at: timestamp,
            });
        }
        Ok::<_, String>(())
    })();
    if let Err(error) = result {
        installed.cleanup();
        return Err(error);
    }
    Ok(installed)
}

fn cleanup_import_files(images: &[PathBuf], vault: Option<&Path>) {
    for image in images {
        let _ = fs::remove_file(image);
    }
    if let Some(vault) = vault {
        let _ = fs::remove_dir_all(vault);
    }
}

fn unique_directory(parent: &Path, base: &str) -> Result<PathBuf, String> {
    fs::create_dir_all(parent)
        .map_err(|error| format!("failed to create import directory: {error}"))?;
    for suffix in 1..10_000 {
        let name = if suffix == 1 {
            base.to_string()
        } else {
            format!("{base}-{suffix}")
        };
        let candidate = parent.join(name);
        if !candidate.exists() {
            return Ok(candidate);
        }
    }
    Err("failed to allocate a unique import directory".into())
}

fn unique_image_name(preferred: &str, mime_type: &str, used: &mut HashSet<String>) -> String {
    let extension = match mime_type {
        "image/jpeg" => "jpg",
        "image/webp" => "webp",
        "image/gif" => "gif",
        _ => "png",
    };
    let raw_stem = Path::new(preferred)
        .file_stem()
        .and_then(|value| value.to_str())
        .unwrap_or("Imported image");
    let stem = truncate_chars(
        &raw_stem
            .chars()
            .map(|character| {
                if character.is_control() || matches!(character, '/' | '\\' | ':') {
                    '_'
                } else {
                    character
                }
            })
            .collect::<String>(),
        140,
    );
    for suffix in 1..10_000 {
        let candidate = if suffix == 1 {
            format!("{stem}.{extension}")
        } else {
            format!("{stem}-{suffix}.{extension}")
        };
        if used.insert(candidate.clone()) {
            return candidate;
        }
    }
    format!("imported-image-{}.{}", now_millis(), extension)
}

fn prepare_import_report(
    app_data: &Path,
    timestamp: i64,
    preview: &ProductImportPreview,
) -> Result<PathBuf, String> {
    let directory = app_data.join("migration-reports");
    fs::create_dir_all(&directory)
        .map_err(|error| format!("failed to create migration report directory: {error}"))?;
    let path = directory.join(format!("{}-{timestamp}.json", preview.source_product.id()));
    let report = serde_json::json!({
        "schemaVersion": 1,
        "status": "prepared",
        "targetProduct": "next-tauri",
        "sourceProduct": preview.source_product,
        "sourceDirectory": preview.source_directory,
        "fingerprint": preview.fingerprint,
        "createdAt": timestamp
    });
    write_report_value(&path, &report)?;
    Ok(path)
}

fn write_import_report(
    path: &Path,
    preview: &ProductImportPreview,
    imported: &ProductImportCounts,
    skipped: &ProductImportCounts,
    backup_path: &str,
    imported_vault_path: Option<&Path>,
) -> Result<PathBuf, String> {
    let report = serde_json::json!({
        "schemaVersion": 1,
        "status": "completed",
        "targetProduct": "next-tauri",
        "sourceProduct": preview.source_product,
        "sourceDirectory": preview.source_directory,
        "fingerprint": preview.fingerprint,
        "imported": imported,
        "skipped": skipped,
        "backupPath": backup_path,
        "importedVaultPath": imported_vault_path.map(|value| value.display().to_string()),
        "warnings": preview.warnings,
        "createdAt": now_millis()
    });
    write_report_value(path, &report)?;
    Ok(path.to_path_buf())
}

fn write_report_value(path: &Path, report: &Value) -> Result<(), String> {
    let mut bytes = serde_json::to_vec_pretty(report)
        .map_err(|error| format!("failed to serialize migration report: {error}"))?;
    bytes.push(b'\n');
    fs::write(path, bytes).map_err(|error| format!("failed to write migration report: {error}"))
}

fn parse_java_settings(contents: &str) -> SafeSettingsPatch {
    let values = parse_sectioned_settings(contents);
    let get =
        |section: &str, key: &str| values.get(&format!("{section}.{key}")).map(String::as_str);
    SafeSettingsPatch {
        language: get("setting.common", "locale").and_then(parse_language),
        auto_check_updates: get("setting.common", "autoCheckUpdate").and_then(parse_bool),
        theme: get("setting.appearance", "theme").and_then(parse_theme),
        sidebar_compact: get("setting.custom", "tabCompact").and_then(parse_bool),
        editor_font_size: get("func.jsonBeauty", "jsonBeautyFontSize").and_then(parse_font_size),
        vault_auto_commit: get("func.quickNote", "quickNoteAutoGitCommit")
            .and_then(parse_bool)
            .or_else(|| get("func.jsonBeauty", "jsonBeautyAutoGitCommit").and_then(parse_bool)),
        ..SafeSettingsPatch::default()
    }
}

fn parse_sectioned_settings(contents: &str) -> HashMap<String, String> {
    let mut result = HashMap::new();
    let mut section = String::new();
    for raw_line in contents.lines() {
        let line = raw_line.trim();
        if line.is_empty() || line.starts_with('#') || line.starts_with(';') {
            continue;
        }
        if line.starts_with('[') && line.ends_with(']') {
            section = line[1..line.len() - 1].trim().to_string();
            continue;
        }
        let Some((key, value)) = line.split_once('=') else {
            continue;
        };
        let value = value.trim().trim_matches('"').trim_matches('\'');
        result.insert(format!("{}.{}", section, key.trim()), value.to_string());
    }
    result
}

fn parse_electron_settings(bytes: &[u8]) -> Result<SafeSettingsPatch, String> {
    let document: Value = serde_json::from_slice(bytes)
        .map_err(|error| format!("Electron settings JSON is invalid: {error}"))?;
    let settings = document.get("settings").unwrap_or(&document);
    Ok(SafeSettingsPatch {
        language: json_string(settings, &["general", "language"]).and_then(parse_language),
        auto_check_updates: json_bool(settings, &["general", "autoCheckUpdates"]),
        theme: json_string(settings, &["appearance", "theme"]).and_then(parse_theme),
        accent_color: json_string(settings, &["appearance", "accentColor"]).and_then(parse_accent),
        sidebar_compact: json_bool(settings, &["layout", "sidebarCompact"])
            .or_else(|| json_bool(settings, &["layout", "compactNavigation"])),
        editor_font_size: json_u64(settings, &["editor", "fontSize"])
            .or_else(|| json_u64(settings, &["editor", "jsonFontSize"]))
            .and_then(|value| u8::try_from(value).ok())
            .filter(|value| (10..=24).contains(value)),
        history_limit: json_u64(settings, &["data", "historyLimit"])
            .and_then(|value| u16::try_from(value).ok())
            .filter(|value| (10..=5000).contains(value)),
        vault_auto_commit: json_bool(settings, &["vault", "autoCommit"]),
    })
}

fn json_value<'a>(root: &'a Value, path: &[&str]) -> Option<&'a Value> {
    path.iter().try_fold(root, |value, key| value.get(*key))
}

fn json_string<'a>(root: &'a Value, path: &[&str]) -> Option<&'a str> {
    json_value(root, path)?.as_str()
}

fn json_bool(root: &Value, path: &[&str]) -> Option<bool> {
    json_value(root, path)?.as_bool()
}

fn json_u64(root: &Value, path: &[&str]) -> Option<u64> {
    json_value(root, path)?.as_u64()
}

fn parse_language(value: &str) -> Option<AppLanguage> {
    match value.to_ascii_lowercase().replace('_', "-").as_str() {
        "zh-cn" | "zh" => Some(AppLanguage::SimplifiedChinese),
        "en-us" | "en" => Some(AppLanguage::English),
        "ja-jp" | "ja" => Some(AppLanguage::Japanese),
        _ => None,
    }
}

fn parse_theme(value: &str) -> Option<ThemePreference> {
    let value = value.to_ascii_lowercase();
    if value == "system" || value.contains("follow") {
        Some(ThemePreference::System)
    } else if value == "dark" || value.contains("dark") {
        Some(ThemePreference::Dark)
    } else if value == "light" || value.contains("light") {
        Some(ThemePreference::Light)
    } else {
        None
    }
}

fn parse_accent(value: &str) -> Option<AccentColor> {
    match value.to_ascii_lowercase().as_str() {
        "blue" => Some(AccentColor::Blue),
        "indigo" => Some(AccentColor::Indigo),
        "teal" => Some(AccentColor::Teal),
        "orange" => Some(AccentColor::Orange),
        _ => None,
    }
}

fn parse_bool(value: &str) -> Option<bool> {
    match value.trim().to_ascii_lowercase().as_str() {
        "true" | "1" | "yes" | "on" => Some(true),
        "false" | "0" | "no" | "off" => Some(false),
        _ => None,
    }
}

fn parse_font_size(value: &str) -> Option<u8> {
    value.parse().ok().filter(|value| (10..=24).contains(value))
}

fn read_limited_file(path: &Path, maximum: u64) -> Result<Vec<u8>, String> {
    let metadata = fs::metadata(path)
        .map_err(|error| format!("failed to inspect source file {}: {error}", path.display()))?;
    if !metadata.is_file() || metadata.len() == 0 || metadata.len() > maximum {
        return Err(format!(
            "source file is empty or too large: {}",
            path.display()
        ));
    }
    fs::read(path)
        .map_err(|error| format!("failed to read source file {}: {error}", path.display()))
}

fn normalize_language_pair(source: &str, target: &str) -> (String, String) {
    let source = normalize_language_code(source, "auto");
    let mut target = normalize_language_code(target, "zh-CN");
    if source == target {
        target = if source == "en" { "zh-CN" } else { "en" }.into();
    }
    (source, target)
}

fn normalize_language_code(value: &str, fallback: &str) -> String {
    let value = value.trim().replace('_', "-");
    if value.is_empty()
        || value.len() > 16
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || byte == b'-')
    {
        fallback.into()
    } else {
        value
    }
}

fn normalize_tool_id(value: &str) -> String {
    match value.trim().to_ascii_lowercase().as_str() {
        "jsonbeauty" | "json-beauty" | "json" => "json".into(),
        "qrcode" | "qr-code" | "qr" => "qrcode".into(),
        "quicknote" | "quick-note" => "quick-note".into(),
        "textdiff" | "text-diff" => "text-diff".into(),
        other
            if !other.is_empty()
                && other.len() <= 64
                && other.bytes().all(|byte| {
                    byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-'
                }) =>
        {
            other.into()
        }
        _ => "import".into(),
    }
}

fn imported_id(fingerprint: &str, category: &str, source_id: &str) -> String {
    let mut hash = Sha256::new();
    hash.update(fingerprint.as_bytes());
    hash.update(b"\0");
    hash.update(category.as_bytes());
    hash.update(b"\0");
    hash.update(source_id.as_bytes());
    let digest = hex_digest(hash.finalize().as_slice());
    format!("import-{}", &digest[..32])
}

fn is_valid_fingerprint(value: &str) -> bool {
    value.len() == 64
        && value
            .bytes()
            .all(|byte| byte.is_ascii_digit() || (b'a'..=b'f').contains(&byte))
}

fn hex_digest(bytes: &[u8]) -> String {
    const HEX: &[u8; 16] = b"0123456789abcdef";
    let mut result = String::with_capacity(bytes.len() * 2);
    for byte in bytes {
        result.push(HEX[(byte >> 4) as usize] as char);
        result.push(HEX[(byte & 0x0f) as usize] as char);
    }
    result
}

fn truncate_chars(value: &str, maximum: usize) -> String {
    value.chars().take(maximum).collect()
}

fn truncate_utf8_bytes(value: &str, maximum: usize) -> String {
    if value.len() <= maximum {
        return value.to_string();
    }
    let mut end = maximum;
    while end > 0 && !value.is_char_boundary(end) {
        end -= 1;
    }
    value[..end].to_string()
}

fn image_info(bytes: &[u8]) -> Option<(&'static str, u32, u32)> {
    if bytes.starts_with(b"\x89PNG\r\n\x1a\n") && bytes.len() >= 24 {
        return Some((
            "image/png",
            u32::from_be_bytes(bytes[16..20].try_into().ok()?),
            u32::from_be_bytes(bytes[20..24].try_into().ok()?),
        ));
    }
    if (bytes.starts_with(b"GIF87a") || bytes.starts_with(b"GIF89a")) && bytes.len() >= 10 {
        return Some((
            "image/gif",
            u16::from_le_bytes(bytes[6..8].try_into().ok()?) as u32,
            u16::from_le_bytes(bytes[8..10].try_into().ok()?) as u32,
        ));
    }
    if bytes.starts_with(b"RIFF") && bytes.get(8..12) == Some(b"WEBP") {
        return webp_dimensions(bytes).map(|(width, height)| ("image/webp", width, height));
    }
    if bytes.starts_with(&[0xff, 0xd8, 0xff]) {
        return jpeg_dimensions(bytes).map(|(width, height)| ("image/jpeg", width, height));
    }
    None
}

fn jpeg_dimensions(bytes: &[u8]) -> Option<(u32, u32)> {
    let mut index = 2_usize;
    while index + 8 < bytes.len() {
        if bytes[index] != 0xff {
            index += 1;
            continue;
        }
        let marker = bytes[index + 1];
        index += 2;
        if matches!(marker, 0xd8 | 0xd9) || (0xd0..=0xd7).contains(&marker) {
            continue;
        }
        if index + 2 > bytes.len() {
            return None;
        }
        let length = u16::from_be_bytes(bytes[index..index + 2].try_into().ok()?) as usize;
        if length < 2 || index + length > bytes.len() {
            return None;
        }
        if matches!(marker, 0xc0..=0xc3 | 0xc5..=0xc7 | 0xc9..=0xcb | 0xcd..=0xcf) && length >= 7 {
            let height = u16::from_be_bytes(bytes[index + 3..index + 5].try_into().ok()?) as u32;
            let width = u16::from_be_bytes(bytes[index + 5..index + 7].try_into().ok()?) as u32;
            return Some((width, height));
        }
        index += length;
    }
    None
}

fn webp_dimensions(bytes: &[u8]) -> Option<(u32, u32)> {
    match bytes.get(12..16)? {
        b"VP8X" if bytes.len() >= 30 => {
            let width = 1 + u32::from_le_bytes([bytes[24], bytes[25], bytes[26], 0]);
            let height = 1 + u32::from_le_bytes([bytes[27], bytes[28], bytes[29], 0]);
            Some((width, height))
        }
        b"VP8 " if bytes.len() >= 30 => Some((
            u16::from_le_bytes([bytes[26], bytes[27]]) as u32 & 0x3fff,
            u16::from_le_bytes([bytes[28], bytes[29]]) as u32 & 0x3fff,
        )),
        b"VP8L" if bytes.len() >= 25 => {
            let bits = u32::from_le_bytes([bytes[21], bytes[22], bytes[23], bytes[24]]);
            Some(((bits & 0x3fff) + 1, ((bits >> 14) & 0x3fff) + 1))
        }
        _ => None,
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn scans_source_database_without_changing_a_byte() {
        let directory = tempfile::TempDir::new().expect("source product directory");
        let database_path = directory.path().join("MooTool.db");
        let connection = Connection::open(&database_path).expect("source database");
        connection
            .execute_batch(
                "CREATE TABLE t_quick_note (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    content TEXT
                 );
                 INSERT INTO t_quick_note VALUES (1, 'Source note', 'read only');",
            )
            .expect("source fixture");
        drop(connection);
        let before = fs::read(&database_path).expect("source bytes before scan");

        let records = scan_database(
            &database_path,
            ProductImportSource::Java,
            &"a".repeat(64),
            1_000,
        )
        .expect("read-only scan");

        assert_eq!(records.quick_notes.len(), 1);
        assert_eq!(records.quick_notes[0].title, "Source note");
        assert_eq!(
            fs::read(&database_path).expect("source bytes after scan"),
            before
        );
    }

    #[test]
    fn parses_only_safe_settings_and_skips_secrets() {
        let patch = parse_java_settings(
            "[setting.common]\nlocale=ja_JP\nautoCheckUpdate=false\n\
             [setting.http]\nhttpProxyPassword=secret\n\
             [setting.custom]\ntabCompact=true\n\
             [func.jsonBeauty]\njsonBeautyFontSize=16",
        );
        assert_eq!(patch.language, Some(AppLanguage::Japanese));
        assert_eq!(patch.auto_check_updates, Some(false));
        assert_eq!(patch.sidebar_compact, Some(true));
        assert_eq!(patch.editor_font_size, Some(16));

        let electron = parse_electron_settings(
            br#"{"settings":{"general":{"language":"en-US"},"appearance":{"theme":"dark"},"secrets":{"gitToken":"secret"}}}"#,
        )
        .expect("Electron settings");
        assert_eq!(electron.language, Some(AppLanguage::English));
        assert_eq!(electron.theme, Some(ThemePreference::Dark));
    }

    #[test]
    fn reads_supported_image_dimensions_without_decoding_pixels() {
        let mut png = vec![0_u8; 24];
        png[..8].copy_from_slice(b"\x89PNG\r\n\x1a\n");
        png[16..20].copy_from_slice(&640_u32.to_be_bytes());
        png[20..24].copy_from_slice(&480_u32.to_be_bytes());
        assert_eq!(image_info(&png), Some(("image/png", 640, 480)));

        let mut gif = b"GIF89a".to_vec();
        gif.extend_from_slice(&320_u16.to_le_bytes());
        gif.extend_from_slice(&200_u16.to_le_bytes());
        assert_eq!(image_info(&gif), Some(("image/gif", 320, 200)));
    }

    #[test]
    fn creates_stable_safe_record_ids_and_truncates_unicode() {
        let id = imported_id(&"a".repeat(64), "note", "1");
        assert!(id.starts_with("import-"));
        assert_eq!(id.len(), 39);
        assert_eq!(truncate_utf8_bytes("中文abc", 5), "中");
        assert!(is_valid_fingerprint(&"f".repeat(64)));
        assert!(!is_valid_fingerprint(&"F".repeat(64)));
        assert!(!is_valid_fingerprint("short"));
    }
}
