use std::{
    collections::HashSet,
    fs,
    path::{Path, PathBuf},
    time::{Duration, Instant, SystemTime, UNIX_EPOCH},
};

use serde::Serialize;
use sysinfo::{Pid, System};
use tauri::{AppHandle, Manager};
use tokio::time::sleep;

use crate::{
    commands::tool_webview::{
        PRODUCT_TOOLS, close_tool_webview_owned, detach_tool_webview_owned,
        dock_tool_webview_owned, open_tool_webview_owned, set_tool_webview_visible_owned,
        stress_tool_webview_reparent_owned, update_tool_webview_bounds_owned,
    },
    contracts::tool_webview::{
        ManagedToolId, ToolWebviewBounds, ToolWebviewPlacement, ToolWebviewSnapshot,
    },
    contracts::{local_data::QuickNote, product_import::ProductImportRecords},
    repositories::local_data::LocalDataRepository,
    state::ToolWebviewManager,
};

const RESULT_ENV: &str = "MOOTOOL_NATIVE_ACCEPTANCE_RESULT";
const DATA_ENV: &str = "MOOTOOL_NATIVE_ACCEPTANCE_DATA";
const CYCLES_ENV: &str = "MOOTOOL_NATIVE_ACCEPTANCE_CYCLES";
const SPAWN_EPOCH_ENV: &str = "MOOTOOL_NATIVE_ACCEPTANCE_SPAWN_EPOCH_MS";
const SESSION_TIMEOUT: Duration = Duration::from_secs(15);
const SESSION_STABILITY: Duration = Duration::from_millis(750);
const CLOSE_TIMEOUT: Duration = Duration::from_secs(5);
const QUICK_NOTE_BENCHMARK_COUNT: usize = 10_000;
const DIGEST_BENCHMARK_BYTES: u64 = 100 * 1024 * 1024;

#[derive(Clone)]
pub struct NativeAcceptanceConfig {
    pub result_path: PathBuf,
    pub data_root: PathBuf,
    stress_cycles: u32,
    spawn_epoch_ms: Option<u128>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct NativeAcceptanceReport {
    schema_version: u32,
    passed: bool,
    platform: &'static str,
    architecture: &'static str,
    duration_ms: u128,
    stress_cycles: u32,
    tools: Vec<ToolAcceptanceResult>,
    isolation: CheckResult,
    stress: CheckResult,
    performance: PerformanceReport,
    failures: Vec<String>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ToolAcceptanceResult {
    tool_id: &'static str,
    passed: bool,
    page_loads: u32,
    session_id: Option<String>,
    reparent_operations: u32,
    open_duration_ms: u128,
    detach_dock_duration_ms: u128,
    failure: Option<String>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct CheckResult {
    passed: bool,
    detail: String,
}

#[derive(Default, Serialize)]
#[serde(rename_all = "camelCase")]
struct PerformanceReport {
    acceptance_start_to_first_tool_ready_ms: u128,
    median_tool_open_ms: u128,
    maximum_tool_open_ms: u128,
    median_detach_dock_ms: u128,
    stress_duration_ms: u128,
    memory: MemoryScalingReport,
    quick_note: QuickNotePerformanceReport,
    digest_100_mib_ms: u128,
}

#[derive(Default, Serialize)]
#[serde(rename_all = "camelCase")]
struct MemoryScalingReport {
    idle_bytes: u64,
    one_tool_bytes: u64,
    ten_tools_bytes: u64,
    all_tools_bytes: u64,
}

#[derive(Default, Serialize)]
#[serde(rename_all = "camelCase")]
struct QuickNotePerformanceReport {
    records: usize,
    seed_ms: u128,
    list_ms: u128,
}

pub fn configuration_from_environment() -> Result<Option<NativeAcceptanceConfig>, String> {
    let result_path = std::env::var_os(RESULT_ENV).map(PathBuf::from);
    let data_root = std::env::var_os(DATA_ENV).map(PathBuf::from);
    match (result_path, data_root) {
        (None, None) => Ok(None),
        (Some(result_path), Some(data_root)) => {
            validate_absolute_path(RESULT_ENV, &result_path)?;
            validate_absolute_path(DATA_ENV, &data_root)?;
            let stress_cycles = std::env::var(CYCLES_ENV)
                .ok()
                .map(|value| {
                    value
                        .parse::<u32>()
                        .map_err(|_| format!("{CYCLES_ENV} must be an integer between 1 and 500"))
                })
                .transpose()?
                .unwrap_or(10);
            if !(1..=500).contains(&stress_cycles) {
                return Err(format!("{CYCLES_ENV} must be between 1 and 500"));
            }
            let spawn_epoch_ms = std::env::var(SPAWN_EPOCH_ENV)
                .ok()
                .map(|value| {
                    value.parse::<u128>().map_err(|_| {
                        format!("{SPAWN_EPOCH_ENV} must be a Unix epoch in milliseconds")
                    })
                })
                .transpose()?;
            Ok(Some(NativeAcceptanceConfig {
                result_path,
                data_root,
                stress_cycles,
                spawn_epoch_ms,
            }))
        }
        _ => Err(format!(
            "{RESULT_ENV} and {DATA_ENV} must be provided together for native acceptance"
        )),
    }
}

pub fn start(app: AppHandle, config: NativeAcceptanceConfig) {
    tauri::async_runtime::spawn(async move {
        let started_at = Instant::now();
        let report = run_acceptance(&app, &config, started_at).await;
        let exit_code = if report.passed { 0 } else { 1 };
        if let Err(error) = write_report(&config.result_path, &report) {
            eprintln!("native acceptance could not write its report: {error}");
            app.exit(2);
            return;
        }
        app.exit(exit_code);
    });
}

async fn run_acceptance(
    app: &AppHandle,
    config: &NativeAcceptanceConfig,
    started_at: Instant,
) -> NativeAcceptanceReport {
    sleep(Duration::from_millis(1_500)).await;
    cleanup_all_tool_webviews(app).await;

    let mut tools = Vec::with_capacity(PRODUCT_TOOLS.len());
    let mut failures = Vec::new();
    let mut start_to_first_tool_ready_ms = 0;
    for tool_id in PRODUCT_TOOLS {
        match exercise_tool(app, tool_id).await {
            Ok(result) => {
                if start_to_first_tool_ready_ms == 0 {
                    start_to_first_tool_ready_ms = config
                        .spawn_epoch_ms
                        .and_then(elapsed_since_epoch_ms)
                        .unwrap_or_else(|| started_at.elapsed().as_millis());
                }
                tools.push(result);
            }
            Err(error) => {
                let message = format!("{}: {error}", tool_id.as_str());
                failures.push(message.clone());
                let snapshot = app.state::<ToolWebviewManager>().snapshot(tool_id);
                tools.push(ToolAcceptanceResult {
                    tool_id: tool_id.as_str(),
                    passed: false,
                    page_loads: snapshot.page_loads,
                    session_id: snapshot.session_id,
                    reparent_operations: snapshot.reparent_operations,
                    open_duration_ms: 0,
                    detach_dock_duration_ms: 0,
                    failure: Some(error),
                });
                close_and_wait(app, tool_id).await;
            }
        }
    }

    let isolation = match exercise_isolation(app).await {
        Ok(detail) => CheckResult {
            passed: true,
            detail,
        },
        Err(error) => {
            failures.push(format!("isolation: {error}"));
            CheckResult {
                passed: false,
                detail: error,
            }
        }
    };
    cleanup_all_tool_webviews(app).await;

    let stress_started_at = Instant::now();
    let stress = match exercise_stress(app, config.stress_cycles).await {
        Ok(detail) => CheckResult {
            passed: true,
            detail,
        },
        Err(error) => {
            failures.push(format!("stress: {error}"));
            CheckResult {
                passed: false,
                detail: error,
            }
        }
    };
    cleanup_all_tool_webviews(app).await;

    let memory = match measure_memory_scaling(app).await {
        Ok(report) => report,
        Err(error) => {
            failures.push(format!("performance memory: {error}"));
            MemoryScalingReport::default()
        }
    };
    cleanup_all_tool_webviews(app).await;

    let (quick_note, digest_100_mib_ms) = match measure_data_performance(app, config) {
        Ok(report) => report,
        Err(error) => {
            failures.push(format!("performance data: {error}"));
            (QuickNotePerformanceReport::default(), 0)
        }
    };
    let open_durations = tools
        .iter()
        .filter(|tool| tool.passed)
        .map(|tool| tool.open_duration_ms)
        .collect::<Vec<_>>();
    let detach_dock_durations = tools
        .iter()
        .filter(|tool| tool.passed)
        .map(|tool| tool.detach_dock_duration_ms)
        .collect::<Vec<_>>();
    let performance = PerformanceReport {
        acceptance_start_to_first_tool_ready_ms: start_to_first_tool_ready_ms,
        median_tool_open_ms: median(&open_durations),
        maximum_tool_open_ms: open_durations.iter().copied().max().unwrap_or_default(),
        median_detach_dock_ms: median(&detach_dock_durations),
        stress_duration_ms: stress_started_at.elapsed().as_millis(),
        memory,
        quick_note,
        digest_100_mib_ms,
    };

    NativeAcceptanceReport {
        schema_version: 2,
        passed: failures.is_empty(),
        platform: std::env::consts::OS,
        architecture: std::env::consts::ARCH,
        duration_ms: started_at.elapsed().as_millis(),
        stress_cycles: config.stress_cycles,
        tools,
        isolation,
        stress,
        performance,
        failures,
    }
}

async fn exercise_tool(
    app: &AppHandle,
    tool_id: ManagedToolId,
) -> Result<ToolAcceptanceResult, String> {
    let state = app.state::<ToolWebviewManager>();
    let initial_bounds = acceptance_bounds();
    let open_started_at = Instant::now();
    open_tool_webview_owned(app, state.inner(), tool_id, initial_bounds)
        .map_err(|error| error.to_string())?;
    let initial = wait_for_session(state.inner(), tool_id).await?;
    let open_duration_ms = open_started_at.elapsed().as_millis();

    let resized_bounds = ToolWebviewBounds {
        x: initial_bounds.x + 8.0,
        y: initial_bounds.y + 8.0,
        width: initial_bounds.width - 16.0,
        height: initial_bounds.height - 16.0,
    };
    update_tool_webview_bounds_owned(app, state.inner(), tool_id, resized_bounds)
        .map_err(|error| error.to_string())?;
    let hidden = set_tool_webview_visible_owned(app, state.inner(), tool_id, false)
        .map_err(|error| error.to_string())?;
    require(!hidden.visible, "hide did not update native visibility")?;
    let shown = set_tool_webview_visible_owned(app, state.inner(), tool_id, true)
        .map_err(|error| error.to_string())?;
    require(shown.visible, "show did not update native visibility")?;

    let detach_dock_started_at = Instant::now();
    let detached = detach_tool_webview_owned(app, state.inner(), tool_id)
        .map_err(|error| error.to_string())?;
    require(
        detached.placement == ToolWebviewPlacement::Detached,
        "detach did not move the WebView to a native window",
    )?;
    sleep(Duration::from_millis(250)).await;
    assert_session_preserved(&initial, &state.snapshot(tool_id), "detach")?;

    let docked = dock_tool_webview_owned(app, state.inner(), tool_id, initial_bounds)
        .map_err(|error| error.to_string())?;
    require(
        docked.placement == ToolWebviewPlacement::Docked,
        "dock did not return the WebView to the shell",
    )?;
    sleep(Duration::from_millis(250)).await;
    let final_snapshot = state.snapshot(tool_id);
    assert_session_preserved(&initial, &final_snapshot, "dock")?;
    require(
        final_snapshot.reparent_operations == 2,
        "detach/dock operation count was not 2",
    )?;

    close_and_wait(app, tool_id).await;
    let closed = state.snapshot(tool_id);
    require(
        !closed.exists && closed.placement == ToolWebviewPlacement::Closed,
        "close did not clear the native WebView session",
    )?;

    Ok(ToolAcceptanceResult {
        tool_id: tool_id.as_str(),
        passed: true,
        page_loads: final_snapshot.page_loads,
        session_id: final_snapshot.session_id,
        reparent_operations: final_snapshot.reparent_operations,
        open_duration_ms,
        detach_dock_duration_ms: detach_dock_started_at.elapsed().as_millis(),
        failure: None,
    })
}

async fn measure_memory_scaling(app: &AppHandle) -> Result<MemoryScalingReport, String> {
    sleep(Duration::from_millis(750)).await;
    let idle_bytes = process_tree_memory_bytes()?;
    let mut one_tool_bytes = 0;
    let mut ten_tools_bytes = 0;
    let mut all_tools_bytes = 0;
    let bounds = acceptance_bounds();
    for (index, tool_id) in PRODUCT_TOOLS.into_iter().enumerate() {
        let state = app.state::<ToolWebviewManager>();
        open_tool_webview_owned(app, state.inner(), tool_id, bounds)
            .map_err(|error| error.to_string())?;
        wait_for_session(state.inner(), tool_id).await?;
        let opened = index + 1;
        if opened == 1 {
            one_tool_bytes = process_tree_memory_bytes()?;
        } else if opened == 10 {
            ten_tools_bytes = process_tree_memory_bytes()?;
        } else if opened == PRODUCT_TOOLS.len() {
            all_tools_bytes = process_tree_memory_bytes()?;
        }
    }
    require(
        one_tool_bytes > 0,
        "could not measure one-tool process memory",
    )?;
    require(
        ten_tools_bytes > 0,
        "could not measure ten-tool process memory",
    )?;
    require(
        all_tools_bytes > 0,
        "could not measure all-tool process memory",
    )?;
    Ok(MemoryScalingReport {
        idle_bytes,
        one_tool_bytes,
        ten_tools_bytes,
        all_tools_bytes,
    })
}

fn process_tree_memory_bytes() -> Result<u64, String> {
    let root = Pid::from_u32(std::process::id());
    let mut system = System::new_all();
    system.refresh_all();
    let mut owned = HashSet::from([root]);
    loop {
        let previous = owned.len();
        for (pid, process) in system.processes() {
            if process
                .parent()
                .is_some_and(|parent| owned.contains(&parent))
            {
                owned.insert(*pid);
            }
        }
        if owned.len() == previous {
            break;
        }
    }
    let bytes = owned
        .iter()
        .filter_map(|pid| system.process(*pid))
        .map(|process| process.memory())
        .sum();
    require(bytes > 0, "process tree memory was unavailable")?;
    Ok(bytes)
}

fn measure_data_performance(
    app: &AppHandle,
    config: &NativeAcceptanceConfig,
) -> Result<(QuickNotePerformanceReport, u128), String> {
    let notes = (0..QUICK_NOTE_BENCHMARK_COUNT)
        .map(|index| QuickNote {
            id: format!("performance-note-{index:05}"),
            title: format!("Performance note {index}"),
            content: "Quick Note performance baseline content".repeat(4),
            tags: vec!["performance".into()],
            color: "default".into(),
            folder_path: format!("Performance/{:02}", index % 100),
            editor_font: "default".into(),
            line_height: "normal".into(),
            line_wrapping: true,
            syntax: "markdown".into(),
            pinned: index % 50 == 0,
            created_at: index as i64,
            updated_at: index as i64,
        })
        .collect();
    let repository = app.state::<LocalDataRepository>();
    let seed_started_at = Instant::now();
    repository.import_product_records(
        "performance",
        "native-acceptance-performance",
        &"b".repeat(64),
        ProductImportRecords {
            quick_notes: notes,
            ..ProductImportRecords::default()
        },
        500,
        QUICK_NOTE_BENCHMARK_COUNT as i64,
    )?;
    let seed_ms = seed_started_at.elapsed().as_millis();
    let list_started_at = Instant::now();
    let records = repository.list_notes()?.len();
    let list_ms = list_started_at.elapsed().as_millis();
    require(
        records >= QUICK_NOTE_BENCHMARK_COUNT,
        "Quick Note benchmark did not return every seeded record",
    )?;

    let performance_directory = config.data_root.join("performance");
    fs::create_dir_all(&performance_directory)
        .map_err(|error| format!("failed to create performance directory: {error}"))?;
    let digest_path = performance_directory.join("digest-100-mib.bin");
    let digest_file = fs::File::create(&digest_path)
        .map_err(|error| format!("failed to create digest benchmark file: {error}"))?;
    digest_file
        .set_len(DIGEST_BENCHMARK_BYTES)
        .map_err(|error| format!("failed to size digest benchmark file: {error}"))?;
    let digest_started_at = Instant::now();
    let digest = super::user_files::digest_path(&digest_path, "sha256")?;
    let digest_100_mib_ms = digest_started_at.elapsed().as_millis();
    require(digest.len() == 64, "100 MiB SHA-256 digest was incomplete")?;
    Ok((
        QuickNotePerformanceReport {
            records,
            seed_ms,
            list_ms,
        },
        digest_100_mib_ms,
    ))
}

fn median(values: &[u128]) -> u128 {
    if values.is_empty() {
        return 0;
    }
    let mut sorted = values.to_vec();
    sorted.sort_unstable();
    sorted[sorted.len() / 2]
}

fn elapsed_since_epoch_ms(started_at_ms: u128) -> Option<u128> {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .ok()
        .map(|duration| duration.as_millis().saturating_sub(started_at_ms))
}

async fn exercise_isolation(app: &AppHandle) -> Result<String, String> {
    let state = app.state::<ToolWebviewManager>();
    let bounds = acceptance_bounds();
    for tool_id in [ManagedToolId::Calculator, ManagedToolId::Json] {
        open_tool_webview_owned(app, state.inner(), tool_id, bounds)
            .map_err(|error| error.to_string())?;
    }
    let calculator = wait_for_session(state.inner(), ManagedToolId::Calculator).await?;
    let json = wait_for_session(state.inner(), ManagedToolId::Json).await?;
    require(
        calculator.session_id != json.session_id,
        "calculator and JSON shared a session ID",
    )?;

    set_tool_webview_visible_owned(app, state.inner(), ManagedToolId::Calculator, false)
        .map_err(|error| error.to_string())?;
    let json_after_hide = state.snapshot(ManagedToolId::Json);
    assert_session_preserved(&json, &json_after_hide, "cross-tool hide")?;
    require(json_after_hide.visible, "hiding calculator also hid JSON")?;

    detach_tool_webview_owned(app, state.inner(), ManagedToolId::Calculator)
        .map_err(|error| error.to_string())?;
    sleep(Duration::from_millis(250)).await;
    let json_after_detach = state.snapshot(ManagedToolId::Json);
    assert_session_preserved(&json, &json_after_detach, "cross-tool detach")?;
    require(
        json_after_detach.placement == ToolWebviewPlacement::Docked,
        "detaching calculator also changed JSON placement",
    )?;
    dock_tool_webview_owned(app, state.inner(), ManagedToolId::Calculator, bounds)
        .map_err(|error| error.to_string())?;
    Ok(format!(
        "isolated sessions {} and {}",
        calculator.session_id.unwrap_or_default(),
        json.session_id.unwrap_or_default()
    ))
}

async fn exercise_stress(app: &AppHandle, cycles: u32) -> Result<String, String> {
    let tool_id = ManagedToolId::Calculator;
    let state = app.state::<ToolWebviewManager>();
    let bounds = acceptance_bounds();
    open_tool_webview_owned(app, state.inner(), tool_id, bounds)
        .map_err(|error| error.to_string())?;
    let before = wait_for_session(state.inner(), tool_id).await?;
    stress_tool_webview_reparent_owned(app, state.inner(), tool_id, bounds, cycles)
        .map_err(|error| error.to_string())?;
    sleep(Duration::from_millis(750)).await;
    let after = state.snapshot(tool_id);
    assert_session_preserved(&before, &after, "stress")?;
    require(
        after.last_stress_passed == Some(true),
        "native reparent stress reported a session reload or state change",
    )?;
    require(
        after.last_stress_cycles == cycles,
        "native reparent stress cycle count did not match",
    )?;
    Ok(format!(
        "{cycles} cycles, {} reparent operations, session preserved",
        after.reparent_operations
    ))
}

async fn wait_for_session(
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
) -> Result<ToolWebviewSnapshot, String> {
    let started_at = Instant::now();
    let mut stable_since = Instant::now();
    let mut stable_snapshot: Option<ToolWebviewSnapshot> = None;
    loop {
        let snapshot = state.snapshot(tool_id);
        if snapshot.page_loads > 0 && snapshot.session_id.is_some() {
            let unchanged = stable_snapshot.as_ref().is_some_and(|previous| {
                previous.page_loads == snapshot.page_loads
                    && previous.session_id == snapshot.session_id
                    && previous.state_digest == snapshot.state_digest
            });
            if unchanged {
                if stable_since.elapsed() >= SESSION_STABILITY {
                    return Ok(snapshot);
                }
            } else {
                stable_since = Instant::now();
                stable_snapshot = Some(snapshot.clone());
            }
        }
        if started_at.elapsed() >= SESSION_TIMEOUT {
            return Err(format!(
                "timed out waiting for page load and session report (loads {}, session {})",
                snapshot.page_loads,
                snapshot.session_id.is_some()
            ));
        }
        sleep(Duration::from_millis(100)).await;
    }
}

fn assert_session_preserved(
    before: &ToolWebviewSnapshot,
    after: &ToolWebviewSnapshot,
    operation: &str,
) -> Result<(), String> {
    require(
        after.page_loads == before.page_loads,
        &format!("{operation} reloaded the page"),
    )?;
    require(
        after.session_id == before.session_id,
        &format!("{operation} replaced the session ID"),
    )?;
    require(
        after.state_digest == before.state_digest,
        &format!("{operation} changed the reported state digest"),
    )
}

async fn cleanup_all_tool_webviews(app: &AppHandle) {
    for tool_id in PRODUCT_TOOLS
        .into_iter()
        .chain([ManagedToolId::EditorLab, ManagedToolId::WebviewProbe])
    {
        close_and_wait(app, tool_id).await;
    }
}

async fn close_and_wait(app: &AppHandle, tool_id: ManagedToolId) {
    let state = app.state::<ToolWebviewManager>();
    let _ = close_tool_webview_owned(app, state.inner(), tool_id);
    let started_at = Instant::now();
    while app.get_webview(tool_id.webview_label()).is_some() && started_at.elapsed() < CLOSE_TIMEOUT
    {
        sleep(Duration::from_millis(50)).await;
    }
}

fn acceptance_bounds() -> ToolWebviewBounds {
    ToolWebviewBounds {
        x: 280.0,
        y: 64.0,
        width: 1_080.0,
        height: 760.0,
    }
}

fn require(condition: bool, message: &str) -> Result<(), String> {
    condition.then_some(()).ok_or_else(|| message.to_string())
}

fn validate_absolute_path(name: &str, path: &Path) -> Result<(), String> {
    if !path.is_absolute() {
        return Err(format!("{name} must be an absolute path"));
    }
    Ok(())
}

fn write_report(path: &Path, report: &NativeAcceptanceReport) -> Result<(), String> {
    let parent = path
        .parent()
        .ok_or_else(|| "native acceptance result path has no parent directory".to_string())?;
    fs::create_dir_all(parent).map_err(|error| {
        format!(
            "failed to create native acceptance report directory {}: {error}",
            parent.display()
        )
    })?;
    let mut contents = serde_json::to_vec_pretty(report)
        .map_err(|error| format!("failed to serialize native acceptance report: {error}"))?;
    contents.push(b'\n');
    fs::write(path, contents).map_err(|error| {
        format!(
            "failed to write native acceptance report {}: {error}",
            path.display()
        )
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn requires_absolute_acceptance_paths() {
        assert!(validate_absolute_path(RESULT_ENV, Path::new("result.json")).is_err());
        assert!(validate_absolute_path(RESULT_ENV, Path::new("/tmp/result.json")).is_ok());
    }

    #[test]
    fn product_acceptance_list_excludes_engineering_surfaces() {
        assert_eq!(PRODUCT_TOOLS.len(), 25);
        assert!(!PRODUCT_TOOLS.contains(&ManagedToolId::EditorLab));
        assert!(!PRODUCT_TOOLS.contains(&ManagedToolId::WebviewProbe));
    }

    #[test]
    fn calculates_stable_performance_medians() {
        assert_eq!(median(&[]), 0);
        assert_eq!(median(&[30, 10, 20]), 20);
        assert_eq!(median(&[40, 10, 30, 20]), 30);
    }
}
