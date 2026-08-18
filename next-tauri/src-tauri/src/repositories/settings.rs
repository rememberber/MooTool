use std::{
    fs,
    io::Write,
    path::{Path, PathBuf},
    sync::Mutex,
    time::{SystemTime, UNIX_EPOCH},
};

use tempfile::NamedTempFile;

use crate::contracts::settings::{AppSettings, SETTINGS_SCHEMA_VERSION};

pub const SETTINGS_FILE_NAME: &str = "mootool-tauri.json";

pub struct SettingsRepository {
    file_path: PathBuf,
    current: Mutex<AppSettings>,
}

impl SettingsRepository {
    pub fn open(file_path: PathBuf) -> Result<Self, String> {
        let settings = load_or_recover(&file_path)?;
        Ok(Self {
            file_path,
            current: Mutex::new(settings),
        })
    }

    pub fn snapshot(&self) -> AppSettings {
        self.current
            .lock()
            .expect("settings repository state poisoned")
            .clone()
    }

    pub fn replace(&self, mut next: AppSettings) -> Result<AppSettings, String> {
        next.schema_version = SETTINGS_SCHEMA_VERSION;
        next.validate()?;

        let mut current = self
            .current
            .lock()
            .map_err(|_| "settings repository state poisoned".to_string())?;
        next.revision = current.revision.saturating_add(1);
        write_atomically(&self.file_path, &next)?;
        *current = next.clone();
        Ok(next)
    }

    pub fn reset(&self) -> Result<AppSettings, String> {
        let mut next = AppSettings::default();
        let mut current = self
            .current
            .lock()
            .map_err(|_| "settings repository state poisoned".to_string())?;
        next.revision = current.revision.saturating_add(1);
        write_atomically(&self.file_path, &next)?;
        *current = next.clone();
        Ok(next)
    }
}

fn load_or_recover(file_path: &Path) -> Result<AppSettings, String> {
    if !file_path.exists() {
        let settings = AppSettings::default();
        write_atomically(file_path, &settings)?;
        return Ok(settings);
    }

    let bytes = fs::read(file_path).map_err(|error| {
        format!(
            "failed to read settings file {}: {error}",
            file_path.display()
        )
    })?;
    match serde_json::from_slice::<AppSettings>(&bytes) {
        Ok(settings) if settings.schema_version <= SETTINGS_SCHEMA_VERSION => {
            let (settings, migrated) = migrate_settings(settings)?;
            settings.validate()?;
            if migrated {
                write_atomically(file_path, &settings)?;
            }
            Ok(settings)
        }
        Ok(settings) => Err(format!(
            "settings schema {} is newer than supported schema {}",
            settings.schema_version, SETTINGS_SCHEMA_VERSION
        )),
        Err(error) => {
            let recovered_path = corrupt_backup_path(file_path);
            fs::rename(file_path, &recovered_path).map_err(|rename_error| {
                format!(
                    "settings are invalid ({error}) and could not be preserved as {}: {rename_error}",
                    recovered_path.display()
                )
            })?;
            let settings = AppSettings::default();
            write_atomically(file_path, &settings)?;
            Ok(settings)
        }
    }
}

fn migrate_settings(mut settings: AppSettings) -> Result<(AppSettings, bool), String> {
    let migrated = match settings.schema_version {
        SETTINGS_SCHEMA_VERSION => false,
        0..=3 => {
            settings.schema_version = SETTINGS_SCHEMA_VERSION;
            true
        }
        version => {
            return Err(format!(
                "unsupported settings migration from schema {version}"
            ));
        }
    };
    Ok((settings, migrated))
}

fn corrupt_backup_path(file_path: &Path) -> PathBuf {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs();
    file_path.with_file_name(format!("mootool-tauri.corrupt-{timestamp}.json"))
}

fn write_atomically(file_path: &Path, settings: &AppSettings) -> Result<(), String> {
    let parent = file_path
        .parent()
        .ok_or_else(|| "settings file has no parent directory".to_string())?;
    fs::create_dir_all(parent).map_err(|error| {
        format!(
            "failed to create settings directory {}: {error}",
            parent.display()
        )
    })?;

    let bytes = serde_json::to_vec_pretty(settings)
        .map_err(|error| format!("failed to serialize settings: {error}"))?;
    let mut temporary = NamedTempFile::new_in(parent)
        .map_err(|error| format!("failed to create temporary settings file: {error}"))?;
    temporary
        .write_all(&bytes)
        .and_then(|_| temporary.write_all(b"\n"))
        .and_then(|_| temporary.flush())
        .map_err(|error| format!("failed to write temporary settings file: {error}"))?;
    temporary
        .as_file()
        .sync_all()
        .map_err(|error| format!("failed to sync temporary settings file: {error}"))?;
    temporary
        .persist(file_path)
        .map_err(|error| format!("failed to replace settings file atomically: {error}"))?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::contracts::settings::{AppLanguage, ThemePreference};
    use tempfile::TempDir;

    #[test]
    fn persists_settings_and_restores_them_after_reopen() {
        let directory = TempDir::new().expect("temporary directory");
        let path = directory.path().join(SETTINGS_FILE_NAME);
        let repository = SettingsRepository::open(path.clone()).expect("open repository");
        let mut settings = repository.snapshot();
        settings.general.language = AppLanguage::Japanese;
        settings.appearance.theme = ThemePreference::Dark;
        settings.tools.recent = vec!["json".into(), "calculator".into()];

        let saved = repository.replace(settings).expect("save settings");
        assert_eq!(saved.revision, 1);
        drop(repository);

        let reopened = SettingsRepository::open(path).expect("reopen repository");
        let restored = reopened.snapshot();
        assert_eq!(restored.general.language, AppLanguage::Japanese);
        assert_eq!(restored.appearance.theme, ThemePreference::Dark);
        assert_eq!(restored.tools.recent, ["json", "calculator"]);
    }

    #[test]
    fn preserves_invalid_json_and_recovers_defaults() {
        let directory = TempDir::new().expect("temporary directory");
        let path = directory.path().join(SETTINGS_FILE_NAME);
        fs::write(&path, b"{not-json").expect("write broken settings");

        let repository = SettingsRepository::open(path.clone()).expect("recover repository");

        assert_eq!(repository.snapshot(), AppSettings::default());
        assert!(path.exists());
        assert!(
            fs::read_dir(directory.path())
                .expect("read directory")
                .filter_map(Result::ok)
                .any(|entry| entry
                    .file_name()
                    .to_string_lossy()
                    .starts_with("mootool-tauri.corrupt-"))
        );
    }

    #[test]
    fn reset_advances_revision_and_restores_defaults() {
        let directory = TempDir::new().expect("temporary directory");
        let path = directory.path().join(SETTINGS_FILE_NAME);
        let repository = SettingsRepository::open(path).expect("open repository");
        let mut settings = repository.snapshot();
        settings.editor.font_size = 18;
        repository.replace(settings).expect("save settings");

        let reset = repository.reset().expect("reset settings");

        assert_eq!(reset.revision, 2);
        assert_eq!(reset.editor.font_size, 13);
    }

    #[test]
    fn migrates_an_older_schema_without_reading_another_product() {
        let directory = TempDir::new().expect("temporary directory");
        let path = directory.path().join(SETTINGS_FILE_NAME);
        let mut old = serde_json::to_value(AppSettings::default()).expect("serialize settings");
        old["schemaVersion"] = serde_json::json!(0);
        old["general"]["language"] = serde_json::json!("en-US");
        fs::write(
            &path,
            serde_json::to_vec_pretty(&old).expect("serialize old settings"),
        )
        .expect("write old settings");

        let repository = SettingsRepository::open(path.clone()).expect("migrate repository");

        assert_eq!(repository.snapshot().general.language, AppLanguage::English);
        assert!(repository.snapshot().general.auto_check_updates);
        let migrated: AppSettings =
            serde_json::from_slice(&fs::read(path).expect("read migrated settings"))
                .expect("parse migrated settings");
        assert_eq!(migrated.schema_version, SETTINGS_SCHEMA_VERSION);
    }
}
