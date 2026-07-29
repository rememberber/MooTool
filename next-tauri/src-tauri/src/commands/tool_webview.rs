use tauri::{
    AppHandle, LogicalPosition, LogicalSize, Manager, State, Webview, WebviewUrl,
    webview::{PageLoadEvent, WebviewBuilder},
    window::WindowBuilder,
};

use crate::{
    contracts::tool_webview::{
        ToolProbeReport, ToolWebviewBounds, ToolWebviewPlacement, ToolWebviewSnapshot,
    },
    state::ToolWebviewManager,
};

const MAIN_WINDOW_LABEL: &str = "main";
const SHELL_WEBVIEW_LABEL: &str = "main";
const TOOL_WEBVIEW_LABEL: &str = "p0-tool-probe";
const DETACHED_WINDOW_LABEL: &str = "p0-tool-detached";

#[tauri::command]
pub async fn get_tool_webview_snapshot(
    caller: Webview,
    state: State<'_, ToolWebviewManager>,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn open_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    bounds: ToolWebviewBounds,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    let bounds = bounds.validate()?;
    if app.get_webview(TOOL_WEBVIEW_LABEL).is_some() {
        return Ok(state.snapshot());
    }

    let main = main_window(&app)?;
    state.begin_open(bounds);
    let builder = WebviewBuilder::new(
        TOOL_WEBVIEW_LABEL,
        WebviewUrl::App("index.html?surface=tool-probe".into()),
    )
    .on_page_load(|webview, payload| {
        if payload.event() == PageLoadEvent::Finished {
            webview
                .app_handle()
                .state::<ToolWebviewManager>()
                .record_page_load();
        }
    });

    let result = main.add_child(
        builder,
        LogicalPosition::new(bounds.x, bounds.y),
        LogicalSize::new(bounds.width, bounds.height),
    );
    let webview = match result {
        Ok(webview) => webview,
        Err(error) => {
            state.mark_open_failed();
            return Err(format!("failed to create tool WebView: {error}"));
        }
    };
    webview
        .set_auto_resize(false)
        .map_err(|error| format!("failed to configure tool WebView resize: {error}"))?;
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn update_tool_webview_bounds(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    bounds: ToolWebviewBounds,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    let bounds = bounds.validate()?;
    let snapshot = state.snapshot();
    if snapshot.exists && snapshot.placement == ToolWebviewPlacement::Docked {
        let webview = tool_webview(&app)?;
        apply_docked_bounds(&webview, bounds)?;
        state.update_bounds(bounds);
    }
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn set_tool_webview_visible(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    visible: bool,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    let snapshot = state.snapshot();
    if snapshot.exists && snapshot.placement == ToolWebviewPlacement::Docked {
        let webview = tool_webview(&app)?;
        if visible {
            webview
                .show()
                .map_err(|error| format!("failed to show tool WebView: {error}"))?;
        } else {
            webview
                .hide()
                .map_err(|error| format!("failed to hide tool WebView: {error}"))?;
        }
        state.mark_visible(visible);
    }
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn detach_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    let snapshot = state.snapshot();
    if !snapshot.exists {
        return Err("tool WebView has not been created".into());
    }
    if snapshot.placement == ToolWebviewPlacement::Detached {
        return Ok(snapshot);
    }

    let detached = ensure_detached_window(&app)?;
    let webview = tool_webview(&app)?;
    webview
        .reparent(&detached)
        .map_err(|error| format!("failed to detach tool WebView: {error}"))?;
    fill_window(&webview, &detached)?;
    detached
        .show()
        .map_err(|error| format!("failed to show detached tool window: {error}"))?;
    detached
        .set_focus()
        .map_err(|error| format!("failed to focus detached tool window: {error}"))?;

    state.record_reparent_operations(1);
    state.mark_detached();
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn dock_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    bounds: ToolWebviewBounds,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    let bounds = bounds.validate()?;
    let snapshot = state.snapshot();
    if !snapshot.exists {
        return Err("tool WebView has not been created".into());
    }
    if snapshot.placement == ToolWebviewPlacement::Docked {
        let webview = tool_webview(&app)?;
        apply_docked_bounds(&webview, bounds)?;
        state.mark_docked(bounds);
        return Ok(state.snapshot());
    }

    let main = main_window(&app)?;
    let webview = tool_webview(&app)?;
    webview
        .set_auto_resize(false)
        .map_err(|error| format!("failed to disable detached auto-resize: {error}"))?;
    webview
        .reparent(&main)
        .map_err(|error| format!("failed to dock tool WebView: {error}"))?;
    apply_docked_bounds(&webview, bounds)?;
    if let Some(detached) = app.get_window(DETACHED_WINDOW_LABEL) {
        detached
            .destroy()
            .map_err(|error| format!("failed to destroy detached tool window: {error}"))?;
    }

    state.record_reparent_operations(1);
    state.mark_docked(bounds);
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn stress_tool_webview_reparent(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    bounds: ToolWebviewBounds,
    cycles: u32,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    let bounds = bounds.validate()?;
    if !(1..=500).contains(&cycles) {
        return Err("stress cycles must be between 1 and 500".into());
    }
    let before = state.snapshot();
    if !before.exists {
        return Err("tool WebView has not been created".into());
    }

    let main = main_window(&app)?;
    let detached = ensure_detached_window(&app)?;
    let webview = tool_webview(&app)?;

    if before.placement == ToolWebviewPlacement::Detached {
        webview
            .set_auto_resize(false)
            .map_err(|error| format!("failed to disable detached auto-resize: {error}"))?;
        webview
            .reparent(&main)
            .map_err(|error| format!("failed to prepare docked stress state: {error}"))?;
        apply_docked_bounds(&webview, bounds)?;
        state.record_reparent_operations(1);
    }

    for cycle in 0..cycles {
        webview
            .reparent(&detached)
            .map_err(|error| format!("detach failed during stress cycle {}: {error}", cycle + 1))?;
        fill_window(&webview, &detached)?;
        webview.set_auto_resize(false).map_err(|error| {
            format!(
                "auto-resize reset failed during stress cycle {}: {error}",
                cycle + 1
            )
        })?;
        webview
            .reparent(&main)
            .map_err(|error| format!("dock failed during stress cycle {}: {error}", cycle + 1))?;
        apply_docked_bounds(&webview, bounds)?;
    }

    detached
        .destroy()
        .map_err(|error| format!("failed to destroy stress window: {error}"))?;
    state.record_reparent_operations(cycles.saturating_mul(2));
    state.mark_docked(bounds);
    state.finish_stress(cycles, before.page_loads, before.session_id);
    webview
        .set_focus()
        .map_err(|error| format!("failed to focus tool WebView after stress: {error}"))?;
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn close_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
) -> Result<ToolWebviewSnapshot, String> {
    require_shell(&caller)?;
    if let Some(webview) = app.get_webview(TOOL_WEBVIEW_LABEL) {
        webview
            .close()
            .map_err(|error| format!("failed to close tool WebView: {error}"))?;
    }
    if let Some(detached) = app.get_window(DETACHED_WINDOW_LABEL) {
        detached
            .destroy()
            .map_err(|error| format!("failed to destroy detached tool window: {error}"))?;
    }
    state.mark_closed();
    Ok(state.snapshot())
}

#[tauri::command]
pub async fn report_tool_webview_probe(
    caller: Webview,
    state: State<'_, ToolWebviewManager>,
    report: ToolProbeReport,
) -> Result<ToolWebviewSnapshot, String> {
    if caller.label() != TOOL_WEBVIEW_LABEL {
        return Err("probe state can only be reported by the owned tool WebView".into());
    }
    if report.session_id.trim().is_empty() || report.session_id.len() > 128 {
        return Err("invalid tool WebView session ID".into());
    }
    if report.draft.len() > 10_000 {
        return Err("tool WebView probe draft is too large".into());
    }
    state.report_probe(report);
    Ok(state.snapshot())
}

fn require_shell(caller: &Webview) -> Result<(), String> {
    if caller.label() == SHELL_WEBVIEW_LABEL {
        Ok(())
    } else {
        Err("tool WebView lifecycle commands are restricted to the main shell".into())
    }
}

fn main_window(app: &AppHandle) -> Result<tauri::Window, String> {
    app.get_window(MAIN_WINDOW_LABEL)
        .ok_or_else(|| "main window is not available".into())
}

fn tool_webview(app: &AppHandle) -> Result<Webview, String> {
    app.get_webview(TOOL_WEBVIEW_LABEL)
        .ok_or_else(|| "tool WebView is not available".into())
}

fn ensure_detached_window(app: &AppHandle) -> Result<tauri::Window, String> {
    if let Some(window) = app.get_window(DETACHED_WINDOW_LABEL) {
        return Ok(window);
    }

    WindowBuilder::new(app, DETACHED_WINDOW_LABEL)
        .title("MooTool WebView Reparent Probe")
        .inner_size(900.0, 680.0)
        .min_inner_size(520.0, 400.0)
        .center()
        .visible(false)
        .closable(false)
        .build()
        .map_err(|error| format!("failed to create detached tool window: {error}"))
}

fn fill_window(webview: &Webview, window: &tauri::Window) -> Result<(), String> {
    webview
        .set_position(LogicalPosition::new(0.0, 0.0))
        .map_err(|error| format!("failed to position detached tool WebView: {error}"))?;
    webview
        .set_size(
            window
                .inner_size()
                .map_err(|error| format!("failed to read detached tool window size: {error}"))?,
        )
        .map_err(|error| format!("failed to size detached tool WebView: {error}"))?;
    webview
        .set_auto_resize(true)
        .map_err(|error| format!("failed to enable detached auto-resize: {error}"))
}

fn apply_docked_bounds(webview: &Webview, bounds: ToolWebviewBounds) -> Result<(), String> {
    webview
        .set_auto_resize(false)
        .map_err(|error| format!("failed to disable docked auto-resize: {error}"))?;
    webview
        .set_position(LogicalPosition::new(bounds.x, bounds.y))
        .map_err(|error| format!("failed to position docked tool WebView: {error}"))?;
    webview
        .set_size(LogicalSize::new(bounds.width, bounds.height))
        .map_err(|error| format!("failed to size docked tool WebView: {error}"))?;
    webview
        .show()
        .map_err(|error| format!("failed to show docked tool WebView: {error}"))
}
