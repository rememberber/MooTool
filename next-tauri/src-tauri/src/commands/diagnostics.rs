use std::{
    fs,
    path::{Path, PathBuf},
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use serde::Serialize;
use sysinfo::{Pid, System};
use tauri::{AppHandle, Manager, Runtime, State};
use tracing_subscriber::EnvFilter;

use crate::{
    contracts::{
        backup::BACKUP_PRODUCT_ID,
        diagnostics::{
            DiagnosticsExportResult, EnvironmentVariable, FrontendErrorReport, SystemSnapshot,
        },
        error::{AppResult, redact_for_log},
        runtime::{PRODUCT_ID, PRODUCT_NAME},
    },
    repositories::{settings::SettingsRepository, window_state::WindowStateRepository},
};

const LOG_FILE_PREFIX: &str = "mootool-next-tauri.ndjson";
const LOG_RETENTION: Duration = Duration::from_secs(14 * 24 * 60 * 60);
const MAX_LOG_FILE_BYTES: u64 = 20 * 1024 * 1024;
const MAX_LOG_BUNDLE_BYTES: u64 = 100 * 1024 * 1024;

pub struct LoggingGuard {
    _guard: tracing_appender::non_blocking::WorkerGuard,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct DiagnosticsManifest<'a> {
    format_version: u32,
    product_id: &'a str,
    bundle_id: &'a str,
    product_name: &'a str,
    app_version: &'a str,
    platform: &'a str,
    architecture: &'a str,
    created_at: u64,
    log_file_count: usize,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct DiagnosticsSnapshot {
    system: SystemSnapshot,
    settings: crate::contracts::settings::AppSettings,
    window_state: crate::contracts::desktop::WindowStateFile,
}

pub fn initialize_logging<R: Runtime>(app: &tauri::App<R>) -> Result<LoggingGuard, String> {
    let log_directory = app
        .path()
        .app_log_dir()
        .map_err(|error| format!("failed to resolve application log directory: {error}"))?;
    fs::create_dir_all(&log_directory).map_err(|error| {
        format!(
            "failed to create application log directory {}: {error}",
            log_directory.display()
        )
    })?;
    cleanup_expired_logs(&log_directory);

    let appender = tracing_appender::rolling::daily(&log_directory, LOG_FILE_PREFIX);
    let (writer, guard) = tracing_appender::non_blocking(appender);
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("mootool_next_tauri_lib=info,warn"));
    let subscriber = tracing_subscriber::fmt()
        .with_env_filter(filter)
        .json()
        .with_ansi(false)
        .with_target(true)
        .with_writer(writer)
        .finish();
    tracing::subscriber::set_global_default(subscriber)
        .map_err(|error| format!("failed to initialize structured logging: {error}"))?;

    let previous_hook = std::panic::take_hook();
    std::panic::set_hook(Box::new(move |panic_info| {
        let location = panic_info
            .location()
            .map(|location| format!("{}:{}", location.file(), location.line()))
            .unwrap_or_else(|| "unknown".into());
        let message = panic_info
            .payload()
            .downcast_ref::<&str>()
            .map(|message| (*message).to_string())
            .or_else(|| panic_info.payload().downcast_ref::<String>().cloned())
            .unwrap_or_else(|| "non-string panic payload".into());
        tracing::error!(
            panic.location = %redact_for_log(&location),
            panic.message = %redact_for_log(&message),
            "application panic"
        );
        previous_hook(panic_info);
    }));
    tracing::info!(
        product.id = PRODUCT_ID,
        product.version = app.package_info().version.to_string(),
        platform = std::env::consts::OS,
        architecture = std::env::consts::ARCH,
        "application logging initialized"
    );
    Ok(LoggingGuard { _guard: guard })
}

#[tauri::command]
pub fn report_frontend_error(report: FrontendErrorReport) -> AppResult<()> {
    report.validate()?;
    tracing::error!(
        error.source = "frontend",
        error.code = %report.code,
        error.context = %redact_for_log(&report.context),
        error.retryable = report.retryable,
        error.message = %redact_for_log(&report.message),
        error.stack = %redact_for_log(report.stack.as_deref().unwrap_or("")),
        "frontend operation failed"
    );
    Ok(())
}

#[tauri::command]
pub fn export_diagnostics_bundle<R: Runtime>(
    app: AppHandle<R>,
    settings: State<'_, SettingsRepository>,
    window_state: State<'_, WindowStateRepository>,
    destination_directory: String,
) -> AppResult<DiagnosticsExportResult> {
    let destination = validate_export_directory(&destination_directory)?;
    let created_at = unix_timestamp();
    let bundle_directory = unique_bundle_directory(&destination, created_at);
    fs::create_dir(&bundle_directory).map_err(|error| {
        format!(
            "failed to create diagnostics directory {}: {error}",
            bundle_directory.display()
        )
    })?;

    let mut system = collect_system_snapshot();
    system.host_name = "<redacted>".into();
    let mut diagnostic_settings = settings.snapshot();
    if diagnostic_settings.vault.root_directory.is_some() {
        diagnostic_settings.vault.root_directory = Some("<redacted>".into());
    }
    write_json(
        &bundle_directory.join("diagnostics.json"),
        &DiagnosticsSnapshot {
            system,
            settings: diagnostic_settings,
            window_state: window_state.snapshot(),
        },
    )?;

    let log_directory = app
        .path()
        .app_log_dir()
        .map_err(|error| format!("failed to resolve application log directory: {error}"))?;
    let copied_logs = copy_logs(&log_directory, &bundle_directory.join("logs"))?;
    let app_version = app.package_info().version.to_string();
    let manifest = DiagnosticsManifest {
        format_version: 1,
        product_id: PRODUCT_ID,
        bundle_id: BACKUP_PRODUCT_ID,
        product_name: PRODUCT_NAME,
        app_version: &app_version,
        platform: std::env::consts::OS,
        architecture: std::env::consts::ARCH,
        created_at,
        log_file_count: copied_logs,
    };
    write_json(&bundle_directory.join("manifest.json"), &manifest)?;

    let bundle_path = bundle_directory.to_string_lossy().into_owned();
    tracing::info!(
        diagnostics.path = %redact_for_log(&bundle_path),
        diagnostics.log_file_count = copied_logs,
        "diagnostics bundle exported"
    );
    Ok(DiagnosticsExportResult {
        bundle_path,
        log_file_count: copied_logs,
        created_at,
    })
}

#[tauri::command]
pub fn get_environment_variables(reveal_sensitive: bool) -> Vec<EnvironmentVariable> {
    let mut variables = std::env::vars()
        .map(|(name, value)| {
            let sensitive = is_sensitive_name(&name);
            EnvironmentVariable {
                name,
                value: if sensitive && !reveal_sensitive {
                    "••••••••".into()
                } else {
                    value
                },
                sensitive,
            }
        })
        .collect::<Vec<_>>();
    variables.sort_by(|left, right| left.name.cmp(&right.name));
    variables
}

#[tauri::command]
pub fn get_system_snapshot() -> SystemSnapshot {
    collect_system_snapshot()
}

fn collect_system_snapshot() -> SystemSnapshot {
    let mut system = System::new_all();
    system.refresh_all();
    let process_memory_bytes = system
        .process(Pid::from_u32(std::process::id()))
        .map(|process| process.memory())
        .unwrap_or_default();
    SystemSnapshot {
        os_name: System::name().unwrap_or_else(|| std::env::consts::OS.into()),
        os_version: System::os_version().unwrap_or_else(|| "Unknown".into()),
        kernel_version: System::kernel_version().unwrap_or_else(|| "Unknown".into()),
        host_name: System::host_name().unwrap_or_else(|| "Unknown".into()),
        architecture: std::env::consts::ARCH.into(),
        cpu_brand: system
            .cpus()
            .first()
            .map(|cpu| cpu.brand().trim().to_string())
            .filter(|brand| !brand.is_empty())
            .unwrap_or_else(|| "Unknown".into()),
        physical_cores: System::physical_core_count().unwrap_or_default(),
        logical_cores: system.cpus().len(),
        total_memory_bytes: system.total_memory(),
        available_memory_bytes: system.available_memory(),
        process_memory_bytes,
        uptime_seconds: System::uptime(),
    }
}

fn validate_export_directory(value: &str) -> Result<PathBuf, String> {
    let candidate = PathBuf::from(value);
    if !candidate.is_absolute() {
        return Err("diagnostics export directory must be an absolute path".into());
    }
    let canonical = candidate.canonicalize().map_err(|error| {
        format!(
            "diagnostics export directory {} is unavailable: {error}",
            candidate.display()
        )
    })?;
    if !canonical.is_dir() {
        return Err("diagnostics export destination must be a directory".into());
    }
    Ok(canonical)
}

fn unique_bundle_directory(parent: &Path, created_at: u64) -> PathBuf {
    let base_name = format!("MooTool-Next-Tauri-diagnostics-{created_at}");
    let initial = parent.join(&base_name);
    if !initial.exists() {
        return initial;
    }
    for suffix in 1..=999 {
        let candidate = parent.join(format!("{base_name}-{suffix}"));
        if !candidate.exists() {
            return candidate;
        }
    }
    parent.join(format!("{base_name}-{}", std::process::id()))
}

fn copy_logs(source: &Path, destination: &Path) -> Result<usize, String> {
    if !source.is_dir() {
        return Ok(0);
    }
    let mut entries = fs::read_dir(source)
        .map_err(|error| format!("failed to read application logs: {error}"))?
        .filter_map(Result::ok)
        .collect::<Vec<_>>();
    entries.sort_by_key(|entry| entry.file_name());
    let mut copied = 0;
    let mut copied_bytes: u64 = 0;
    for entry in entries {
        let name = entry.file_name();
        let name_text = name.to_string_lossy();
        if !name_text.starts_with(LOG_FILE_PREFIX) {
            continue;
        }
        let metadata = entry
            .path()
            .symlink_metadata()
            .map_err(|error| format!("failed to inspect application log: {error}"))?;
        if !metadata.file_type().is_file()
            || metadata.len() > MAX_LOG_FILE_BYTES
            || copied_bytes.saturating_add(metadata.len()) > MAX_LOG_BUNDLE_BYTES
        {
            continue;
        }
        if copied == 0 {
            fs::create_dir(destination)
                .map_err(|error| format!("failed to create diagnostics logs directory: {error}"))?;
        }
        fs::copy(entry.path(), destination.join(name))
            .map_err(|error| format!("failed to copy application log into diagnostics: {error}"))?;
        copied += 1;
        copied_bytes += metadata.len();
    }
    Ok(copied)
}

fn write_json(path: &Path, value: &impl Serialize) -> Result<(), String> {
    let mut bytes = serde_json::to_vec_pretty(value)
        .map_err(|error| format!("failed to serialize diagnostics: {error}"))?;
    bytes.push(b'\n');
    fs::write(path, bytes).map_err(|error| {
        format!(
            "failed to write diagnostics file {}: {error}",
            path.display()
        )
    })
}

fn cleanup_expired_logs(directory: &Path) {
    let Ok(entries) = fs::read_dir(directory) else {
        return;
    };
    let now = SystemTime::now();
    for entry in entries.filter_map(Result::ok) {
        let name = entry.file_name();
        if !name.to_string_lossy().starts_with(LOG_FILE_PREFIX) {
            continue;
        }
        let Ok(metadata) = entry.path().symlink_metadata() else {
            continue;
        };
        let expired = metadata.file_type().is_file()
            && metadata
                .modified()
                .ok()
                .and_then(|modified| now.duration_since(modified).ok())
                .is_some_and(|age| age > LOG_RETENTION);
        if expired {
            let _ = fs::remove_file(entry.path());
        }
    }
}

fn unix_timestamp() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
}

fn is_sensitive_name(name: &str) -> bool {
    let upper = name.to_ascii_uppercase();
    [
        "TOKEN",
        "SECRET",
        "PASSWORD",
        "PASSWD",
        "API_KEY",
        "PRIVATE_KEY",
        "CREDENTIAL",
        "COOKIE",
        "AUTH",
        "DSN",
    ]
    .iter()
    .any(|needle| upper.contains(needle))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classifies_secret_like_environment_names() {
        assert!(is_sensitive_name("GITHUB_TOKEN"));
        assert!(is_sensitive_name("database_password"));
        assert!(is_sensitive_name("SENTRY_DSN"));
        assert!(!is_sensitive_name("PATH"));
        assert!(!is_sensitive_name("JAVA_HOME"));
    }

    #[test]
    fn returns_a_cross_platform_system_snapshot() {
        let snapshot = get_system_snapshot();
        assert!(!snapshot.os_name.is_empty());
        assert!(!snapshot.architecture.is_empty());
        assert!(snapshot.logical_cores > 0);
        assert!(snapshot.total_memory_bytes >= snapshot.available_memory_bytes);
    }

    #[test]
    fn validates_frontend_error_reports() {
        let valid = FrontendErrorReport {
            code: "network".into(),
            message: "request failed".into(),
            context: "translation.submit".into(),
            retryable: true,
            stack: None,
        };
        assert!(valid.validate().is_ok());

        let invalid = FrontendErrorReport {
            code: "Bad Code".into(),
            ..valid
        };
        assert!(invalid.validate().is_err());
    }

    #[test]
    fn accepts_only_absolute_existing_export_directories() {
        let directory = tempfile::TempDir::new().expect("temporary directory");
        assert_eq!(
            validate_export_directory(directory.path().to_str().expect("utf-8 path"))
                .expect("valid directory"),
            directory
                .path()
                .canonicalize()
                .expect("canonical directory")
        );
        assert!(validate_export_directory("relative/path").is_err());
    }

    #[test]
    fn copies_only_owned_regular_logs_with_size_limits() {
        let source = tempfile::TempDir::new().expect("source directory");
        let output = tempfile::TempDir::new().expect("output directory");
        fs::write(
            source.path().join("mootool-next-tauri.ndjson.2026-08-16"),
            b"{}\n",
        )
        .expect("owned log");
        fs::write(source.path().join("another-product.log"), b"private").expect("foreign log");
        fs::File::create(source.path().join("mootool-next-tauri.ndjson.oversized"))
            .and_then(|file| file.set_len(MAX_LOG_FILE_BYTES + 1))
            .expect("oversized log");

        let destination = output.path().join("logs");
        assert_eq!(
            copy_logs(source.path(), &destination).expect("copy logs"),
            1
        );
        assert!(
            destination
                .join("mootool-next-tauri.ndjson.2026-08-16")
                .is_file()
        );
        assert!(!destination.join("another-product.log").exists());
        assert!(
            !destination
                .join("mootool-next-tauri.ndjson.oversized")
                .exists()
        );
    }
}
