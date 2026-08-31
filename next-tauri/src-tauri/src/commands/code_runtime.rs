use std::{
    collections::HashMap,
    path::{Path, PathBuf},
    process::Stdio,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, AtomicU32, Ordering},
    },
    time::{Duration, Instant},
};

use tauri::ipc::Channel;
use tokio::{
    io::{AsyncRead, AsyncReadExt},
    process::Command,
};

use crate::contracts::{
    code_runtime::{CodeOutputEvent, CodeRunResult, CodeRunSpec, CodeRuntimeId, CodeRuntimeStatus},
    error::AppResult,
    settings::RuntimeSettings,
};
use crate::repositories::settings::SettingsRepository;

const MAX_OUTPUT_BYTES: usize = 5 * 1024 * 1024;

#[derive(Default)]
pub struct CodeExecutionManager {
    active: Mutex<HashMap<String, Arc<ProcessControl>>>,
}

#[derive(Default)]
struct ProcessControl {
    pid: AtomicU32,
    cancelled: AtomicBool,
}

impl CodeExecutionManager {
    fn register(&self, id: &str) -> Result<Arc<ProcessControl>, String> {
        if id.is_empty() || id.len() > 128 {
            return Err("invalid code run ID".into());
        }
        let mut active = self
            .active
            .lock()
            .map_err(|_| "code execution state poisoned")?;
        if active.contains_key(id) {
            return Err("code run ID is already active".into());
        }
        let control = Arc::new(ProcessControl::default());
        active.insert(id.into(), control.clone());
        Ok(control)
    }
    fn finish(&self, id: &str) {
        if let Ok(mut active) = self.active.lock() {
            active.remove(id);
        }
    }
    fn cancel(&self, id: &str) -> bool {
        let control = self
            .active
            .lock()
            .ok()
            .and_then(|active| active.get(id).cloned());
        if let Some(control) = control {
            control.cancelled.store(true, Ordering::SeqCst);
            let pid = control.pid.load(Ordering::SeqCst);
            if pid > 0 {
                kill_process(pid);
            }
            true
        } else {
            false
        }
    }
}

#[tauri::command]
pub async fn detect_code_runtimes(
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<Vec<CodeRuntimeStatus>> {
    let runtime_settings = settings.snapshot().runtime;
    let specifications = [
        (CodeRuntimeId::Java, &["java"][..]),
        (CodeRuntimeId::Groovy, &["groovy"][..]),
        (CodeRuntimeId::Python, &["python3", "python"][..]),
        (CodeRuntimeId::Node, &["node"][..]),
    ];
    let mut output = Vec::new();
    for (id, candidates) in specifications {
        if let Some(path) = resolve_runtime(&runtime_settings, id, candidates) {
            let version = runtime_version(&path, matches!(id, CodeRuntimeId::Java)).await;
            output.push(CodeRuntimeStatus {
                id,
                available: true,
                command: path.display().to_string(),
                version,
            });
        } else {
            output.push(CodeRuntimeStatus {
                id,
                available: false,
                command: configured_runtime(&runtime_settings, id)
                    .filter(|value| !value.trim().is_empty())
                    .unwrap_or(candidates[0])
                    .into(),
                version: String::new(),
            });
        }
    }
    Ok(output)
}

#[tauri::command]
pub async fn run_code(
    manager: tauri::State<'_, CodeExecutionManager>,
    settings: tauri::State<'_, SettingsRepository>,
    spec: CodeRunSpec,
    output: Channel<CodeOutputEvent>,
) -> AppResult<CodeRunResult> {
    validate_spec(&spec)?;
    let control = manager.register(&spec.request_id)?;
    let request_id = spec.request_id.clone();
    let result = run_owned(spec, control, output, settings.snapshot().runtime).await;
    manager.finish(&request_id);
    Ok(result?)
}

#[tauri::command]
pub fn cancel_code_run(
    manager: tauri::State<'_, CodeExecutionManager>,
    request_id: String,
) -> bool {
    manager.cancel(&request_id)
}

async fn run_owned(
    spec: CodeRunSpec,
    control: Arc<ProcessControl>,
    output: Channel<CodeOutputEvent>,
    runtime_settings: RuntimeSettings,
) -> Result<CodeRunResult, String> {
    let started = Instant::now();
    let directory = tempfile::tempdir()
        .map_err(|error| format!("failed to create runtime directory: {error}"))?;
    let (program, source_path, mut arguments) = prepare_source(
        directory.path(),
        spec.runtime,
        &spec.code,
        &runtime_settings,
    )
    .await?;
    if matches!(spec.runtime, CodeRuntimeId::Java) {
        let javac = resolve_java_compiler(&runtime_settings)
            .ok_or_else(|| "javac is required to run Java source".to_string())?;
        let compile = execute_child(
            &javac,
            &[source_path.display().to_string()],
            directory.path(),
            Duration::from_millis(spec.timeout_ms),
            control.clone(),
            output.clone(),
            &runtime_settings.environment,
        )
        .await?;
        if compile.exit_code != Some(0) {
            return Ok(CodeRunResult {
                duration_ms: started.elapsed().as_millis(),
                command: compile.command,
                ..compile
            });
        }
    }
    arguments.extend(spec.arguments);
    let working = if spec.working_directory.trim().is_empty() {
        directory.path().to_path_buf()
    } else {
        PathBuf::from(spec.working_directory)
    };
    let mut result = execute_child(
        &program,
        &arguments,
        &working,
        Duration::from_millis(spec.timeout_ms),
        control,
        output,
        &runtime_settings.environment,
    )
    .await?;
    result.duration_ms = started.elapsed().as_millis();
    Ok(result)
}

async fn prepare_source(
    directory: &Path,
    runtime: CodeRuntimeId,
    code: &str,
    settings: &RuntimeSettings,
) -> Result<(PathBuf, PathBuf, Vec<String>), String> {
    let (commands, filename) = match runtime {
        CodeRuntimeId::Java => (&["java"][..], "Main.java"),
        CodeRuntimeId::Groovy => (&["groovy"][..], "main.groovy"),
        CodeRuntimeId::Python => (&["python3", "python"][..], "main.py"),
        CodeRuntimeId::Node => (&["node"][..], "main.js"),
    };
    let program = resolve_runtime(settings, runtime, commands)
        .ok_or_else(|| format!("{} runtime was not found on PATH", commands[0]))?;
    let path = directory.join(filename);
    tokio::fs::write(&path, code)
        .await
        .map_err(|error| format!("failed to write runtime source: {error}"))?;
    let arguments = if matches!(runtime, CodeRuntimeId::Java) {
        vec!["-cp".into(), directory.display().to_string(), "Main".into()]
    } else {
        vec![path.display().to_string()]
    };
    Ok((program, path, arguments))
}

async fn execute_child(
    program: &Path,
    arguments: &[String],
    working: &Path,
    timeout: Duration,
    control: Arc<ProcessControl>,
    output: Channel<CodeOutputEvent>,
    environment: &std::collections::BTreeMap<String, String>,
) -> Result<CodeRunResult, String> {
    let command_text = format!("{} {}", program.display(), arguments.join(" "));
    let mut command = Command::new(program);
    command
        .args(arguments)
        .current_dir(working)
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .envs(environment)
        .kill_on_drop(true);
    #[cfg(unix)]
    {
        use std::os::unix::process::CommandExt;
        command.as_std_mut().process_group(0);
    }
    let mut child = command
        .spawn()
        .map_err(|error| format!("failed to start runtime: {error}"))?;
    control
        .pid
        .store(child.id().unwrap_or_default(), Ordering::SeqCst);
    let stdout_task = tokio::spawn(read_stream(
        child.stdout.take().ok_or("runtime stdout unavailable")?,
        "stdout",
        output.clone(),
    ));
    let stderr_task = tokio::spawn(read_stream(
        child.stderr.take().ok_or("runtime stderr unavailable")?,
        "stderr",
        output,
    ));
    let mut timed_out = false;
    let status = match tokio::time::timeout(timeout, child.wait()).await {
        Ok(status) => status.map_err(|error| format!("failed to wait for runtime: {error}"))?,
        Err(_) => {
            timed_out = true;
            let pid = control.pid.load(Ordering::SeqCst);
            if pid > 0 {
                kill_process(pid);
            }
            child
                .wait()
                .await
                .map_err(|error| format!("failed to stop timed-out runtime: {error}"))?
        }
    };
    control.pid.store(0, Ordering::SeqCst);
    let stdout = stdout_task
        .await
        .map_err(|error| format!("stdout reader failed: {error}"))??;
    let stderr = stderr_task
        .await
        .map_err(|error| format!("stderr reader failed: {error}"))??;
    Ok(CodeRunResult {
        exit_code: status.code(),
        stdout,
        stderr,
        duration_ms: 0,
        command: command_text,
        timed_out,
        cancelled: control.cancelled.load(Ordering::SeqCst),
    })
}

async fn read_stream(
    mut reader: impl AsyncRead + Unpin,
    stream: &str,
    output: Channel<CodeOutputEvent>,
) -> Result<String, String> {
    let mut collected = Vec::new();
    let mut buffer = [0u8; 4096];
    loop {
        let count = reader
            .read(&mut buffer)
            .await
            .map_err(|error| format!("failed to read runtime {stream}: {error}"))?;
        if count == 0 {
            break;
        }
        let remaining = MAX_OUTPUT_BYTES.saturating_sub(collected.len());
        if remaining == 0 {
            break;
        }
        let take = count.min(remaining);
        collected.extend_from_slice(&buffer[..take]);
        let _ = output.send(CodeOutputEvent {
            stream: stream.into(),
            text: String::from_utf8_lossy(&buffer[..take]).into_owned(),
        });
    }
    Ok(String::from_utf8_lossy(&collected).into_owned())
}

fn validate_spec(spec: &CodeRunSpec) -> Result<(), String> {
    if spec.code.len() > 2 * 1024 * 1024
        || spec.arguments.len() > 40
        || spec.arguments.iter().any(|argument| argument.len() > 1000)
    {
        return Err("code run exceeds local input limits".into());
    }
    if !(1_000..=120_000).contains(&spec.timeout_ms) {
        return Err("runtime timeout must be between 1 and 120 seconds".into());
    }
    if !spec.working_directory.trim().is_empty() {
        let path = Path::new(&spec.working_directory);
        if !path.is_absolute() || !path.is_dir() {
            return Err("runtime working directory must be an existing absolute directory".into());
        }
    }
    Ok(())
}

fn find_command(candidates: &[&str]) -> Option<PathBuf> {
    let path = std::env::var_os("PATH")?;
    for directory in std::env::split_paths(&path) {
        for candidate in candidates {
            let executable = directory.join(candidate);
            if executable.is_file() {
                return Some(executable);
            }
            #[cfg(windows)]
            {
                let executable = directory.join(format!("{candidate}.exe"));
                if executable.is_file() {
                    return Some(executable);
                }
            }
        }
    }
    None
}

fn configured_runtime(settings: &RuntimeSettings, runtime: CodeRuntimeId) -> Option<&str> {
    let value = match runtime {
        CodeRuntimeId::Java => settings.java_path.as_str(),
        CodeRuntimeId::Groovy => settings.groovy_path.as_str(),
        CodeRuntimeId::Python => settings.python_path.as_str(),
        CodeRuntimeId::Node => settings.node_path.as_str(),
    };
    (!value.trim().is_empty()).then_some(value.trim())
}

fn resolve_runtime(
    settings: &RuntimeSettings,
    runtime: CodeRuntimeId,
    candidates: &[&str],
) -> Option<PathBuf> {
    if let Some(configured) = configured_runtime(settings, runtime) {
        let path = PathBuf::from(configured);
        return path.is_file().then_some(path);
    }
    settings
        .auto_detect
        .then(|| find_command(candidates))
        .flatten()
}

fn resolve_java_compiler(settings: &RuntimeSettings) -> Option<PathBuf> {
    if let Some(java) = configured_runtime(settings, CodeRuntimeId::Java) {
        let java = Path::new(java);
        let candidate = java.with_file_name(if cfg!(windows) { "javac.exe" } else { "javac" });
        return candidate.is_file().then_some(candidate);
    }
    settings
        .auto_detect
        .then(|| find_command(&["javac"]))
        .flatten()
}

async fn runtime_version(program: &Path, java: bool) -> String {
    let output = tokio::time::timeout(
        Duration::from_secs(3),
        Command::new(program).arg("--version").output(),
    )
    .await;
    let Ok(Ok(output)) = output else {
        return "Unknown".into();
    };
    let bytes = if java && output.stdout.is_empty() {
        output.stderr
    } else {
        output.stdout
    };
    String::from_utf8_lossy(&bytes)
        .lines()
        .next()
        .unwrap_or("Unknown")
        .trim()
        .to_string()
}

#[cfg(unix)]
fn kill_process(pid: u32) {
    let _ = std::process::Command::new("kill")
        .args(["-TERM", &format!("-{pid}")])
        .status();
}
#[cfg(windows)]
fn kill_process(pid: u32) {
    let _ = std::process::Command::new("taskkill")
        .args(["/PID", &pid.to_string(), "/T", "/F"])
        .status();
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn validates_runtime_limits() {
        let spec = CodeRunSpec {
            request_id: "one".into(),
            runtime: CodeRuntimeId::Node,
            code: "console.log(1)".into(),
            timeout_ms: 30_000,
            arguments: vec![],
            working_directory: String::new(),
        };
        assert!(validate_spec(&spec).is_ok());
    }
    #[test]
    fn only_resolves_commands_from_path() {
        assert!(find_command(&["a-command-that-does-not-exist-mootool"]).is_none());
    }
}
