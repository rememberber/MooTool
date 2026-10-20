use std::{
    process::Stdio,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, AtomicU32, Ordering},
    },
    time::Duration,
};

use serde::{Deserialize, Serialize};
use tauri::State;
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    process::Command,
};

use crate::{
    commands::code_runtime::resolve_runtime,
    contracts::{code_runtime::CodeRuntimeId, error::AppResult, settings::RuntimeSettings},
    repositories::settings::SettingsRepository,
};

const JAVA_SOURCE: &str = r#"import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.charset.StandardCharsets;
class MooToolRegex {
 static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
 static String encode(String s) { return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8)); }
 public static void main(String[] args) throws Exception {
  BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
  try {
   String pattern = decode(in.readLine()), source = decode(in.readLine());
   int flags = Integer.parseInt(in.readLine());
   boolean global = Boolean.parseBoolean(in.readLine());
   Matcher matcher = Pattern.compile(pattern, flags).matcher(source);
   int count = 0, size = 0;
   while (matcher.find()) {
    if (count++ >= 5000) { System.out.println("LIMIT"); return; }
    StringBuilder row = new StringBuilder("M\t" + matcher.start() + "\t" + encode(matcher.group()));
    for (int i = 1; i <= matcher.groupCount(); i++) row.append("\t").append(encode(matcher.group(i)));
    size += row.length();
    if (size > 1500000) { System.out.println("LIMIT"); return; }
    System.out.println(row);
    if (!global) break;
   }
  } catch (Exception e) { System.out.println("ERROR\t" + encode(e.getMessage())); }
 }
}"#;

const MAX_STDIN_BYTES: usize = 2_000_000;
const MAX_STDOUT_BYTES: usize = 2 * 1024 * 1024;

#[derive(Default)]
pub struct JavaRegexManager {
    active: Mutex<Option<ActiveJavaRegex>>,
}

struct ActiveJavaRegex {
    request_id: String,
    pid: Arc<AtomicU32>,
    cancelled: Arc<AtomicBool>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JavaRegexRequest {
    request_id: String,
    stdin: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct JavaRegexRun {
    stdout: String,
    stderr: String,
    exit_code: Option<i32>,
    timed_out: bool,
    cancelled: bool,
}

#[tauri::command]
pub async fn match_java_regex(
    manager: State<'_, JavaRegexManager>,
    settings: State<'_, SettingsRepository>,
    request: JavaRegexRequest,
) -> AppResult<JavaRegexRun> {
    validate_request(&request)?;
    let pid = Arc::new(AtomicU32::new(0));
    let cancelled = Arc::new(AtomicBool::new(false));
    manager.start(&request.request_id, pid.clone(), cancelled.clone())?;
    let result = run_java(
        &settings.snapshot().runtime,
        &request.stdin,
        pid,
        cancelled,
    )
    .await;
    manager.finish(&request.request_id);
    Ok(result?)
}

#[tauri::command]
pub fn cancel_java_regex(manager: State<'_, JavaRegexManager>, request_id: String) -> bool {
    manager.cancel(&request_id)
}

fn validate_request(request: &JavaRegexRequest) -> Result<(), String> {
    if request.request_id.is_empty()
        || request.request_id.len() > 128
        || !request
            .request_id
            .chars()
            .all(|char| char.is_ascii_alphanumeric() || char == '-')
    {
        return Err("invalid Java regex request".into());
    }
    if request.stdin.len() > MAX_STDIN_BYTES || request.stdin.contains('\0') {
        return Err("Invalid regex input (pattern ≤ 16000, text ≤ 1000000 characters)".into());
    }
    Ok(())
}

async fn run_java(
    settings: &RuntimeSettings,
    stdin: &str,
    pid: Arc<AtomicU32>,
    cancelled: Arc<AtomicBool>,
) -> Result<JavaRegexRun, String> {
    let java = resolve_runtime(settings, CodeRuntimeId::Java, &["java"])
        .ok_or_else(|| "JDK 11+ is required".to_string())?;
    let directory = tempfile::tempdir()
        .map_err(|error| format!("failed to create Java regex directory: {error}"))?;
    let file = directory.path().join("MooToolRegex.java");
    tokio::fs::write(&file, JAVA_SOURCE)
        .await
        .map_err(|error| format!("failed to write Java regex source: {error}"))?;
    if cancelled.load(Ordering::SeqCst) {
        return Ok(cancelled_run());
    }
    let mut command = Command::new(&java);
    command
        .arg("-Xmx128m")
        .arg(&file)
        .current_dir(directory.path())
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true);
    #[cfg(unix)]
    {
        use std::os::unix::process::CommandExt;
        command.as_std_mut().process_group(0);
    }
    let mut child = command
        .spawn()
        .map_err(|error| format!("JDK 11+ is required: {error}"))?;
    pid.store(child.id().unwrap_or_default(), Ordering::SeqCst);
    if let Some(mut input) = child.stdin.take() {
        let _ = input.write_all(stdin.as_bytes()).await;
        let _ = input.shutdown().await;
    }
    let stdout_task = tokio::spawn(read_limited(
        child.stdout.take().ok_or("Java regex stdout unavailable")?,
    ));
    let stderr_task = tokio::spawn(read_limited(
        child.stderr.take().ok_or("Java regex stderr unavailable")?,
    ));
    let mut timed_out = false;
    let status = match tokio::time::timeout(Duration::from_secs(5), child.wait()).await {
        Ok(status) => status.map_err(|error| format!("failed to wait for Java regex: {error}"))?,
        Err(_) => {
            timed_out = true;
            kill_process(pid.load(Ordering::SeqCst));
            child
                .wait()
                .await
                .map_err(|error| format!("failed to stop timed-out Java regex: {error}"))?
        }
    };
    pid.store(0, Ordering::SeqCst);
    let stdout = stdout_task
        .await
        .map_err(|error| format!("Java regex stdout failed: {error}"))??;
    let stderr = stderr_task
        .await
        .map_err(|error| format!("Java regex stderr failed: {error}"))??;
    Ok(JavaRegexRun {
        stdout,
        stderr,
        exit_code: status.code(),
        timed_out,
        cancelled: cancelled.load(Ordering::SeqCst),
    })
}

fn cancelled_run() -> JavaRegexRun {
    JavaRegexRun {
        stdout: String::new(),
        stderr: String::new(),
        exit_code: None,
        timed_out: false,
        cancelled: true,
    }
}

async fn read_limited(mut reader: impl tokio::io::AsyncRead + Unpin) -> Result<String, String> {
    let mut collected = Vec::new();
    let mut buffer = [0u8; 4096];
    loop {
        let count = reader
            .read(&mut buffer)
            .await
            .map_err(|error| format!("failed to read Java regex output: {error}"))?;
        if count == 0 || collected.len() >= MAX_STDOUT_BYTES {
            break;
        }
        let take = count.min(MAX_STDOUT_BYTES.saturating_sub(collected.len()));
        collected.extend_from_slice(&buffer[..take]);
    }
    Ok(String::from_utf8_lossy(&collected).into_owned())
}

impl JavaRegexManager {
    fn start(
        &self,
        request_id: &str,
        pid: Arc<AtomicU32>,
        cancelled: Arc<AtomicBool>,
    ) -> Result<(), String> {
        let mut active = self
            .active
            .lock()
            .map_err(|_| "Java regex state poisoned".to_string())?;
        if let Some(previous) = active.take() {
            previous.cancelled.store(true, Ordering::SeqCst);
            kill_process(previous.pid.load(Ordering::SeqCst));
        }
        active.replace(ActiveJavaRegex {
            request_id: request_id.into(),
            pid,
            cancelled,
        });
        Ok(())
    }

    fn finish(&self, request_id: &str) {
        if let Ok(mut active) = self.active.lock() {
            if active
                .as_ref()
                .is_some_and(|current| current.request_id == request_id)
            {
                active.take();
            }
        }
    }

    fn cancel(&self, request_id: &str) -> bool {
        let current = self
            .active
            .lock()
            .ok()
            .and_then(|active| active.as_ref().map(|current| current.request_id.clone()));
        if current.as_deref() != Some(request_id) && !request_id.is_empty() {
            return false;
        }
        if let Ok(active) = self.active.lock() {
            if let Some(current) = active.as_ref() {
                if request_id.is_empty() || current.request_id == request_id {
                    current.cancelled.store(true, Ordering::SeqCst);
                    kill_process(current.pid.load(Ordering::SeqCst));
                    return true;
                }
            }
        }
        false
    }
}

fn kill_process(pid: u32) {
    if pid == 0 {
        return;
    }
    #[cfg(unix)]
    {
        let _ = std::process::Command::new("kill")
            .args(["-KILL", &format!("-{pid}")])
            .status();
    }
    #[cfg(windows)]
    {
        let _ = std::process::Command::new("taskkill")
            .args(["/PID", &pid.to_string(), "/T", "/F"])
            .status();
    }
}
