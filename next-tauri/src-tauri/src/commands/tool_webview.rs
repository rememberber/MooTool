use std::path::PathBuf;

use tauri::{
    AppHandle, LogicalPosition, LogicalSize, Manager, State, Webview, WebviewUrl,
    webview::{PageLoadEvent, WebviewBuilder},
    window::WindowBuilder,
};

use crate::{
    contracts::{
        error::AppResult,
        tool_webview::{
            ManagedToolId, ToolSessionReport, ToolWebviewBounds, ToolWebviewPlacement,
            ToolWebviewSnapshot,
        },
    },
    state::ToolWebviewManager,
};

const MAIN_WINDOW_LABEL: &str = "main";
const SHELL_WEBVIEW_LABEL: &str = "main";

pub(crate) const PRODUCT_TOOLS: [ManagedToolId; 25] = [
    ManagedToolId::Calculator,
    ManagedToolId::Color,
    ManagedToolId::Config,
    ManagedToolId::Cron,
    ManagedToolId::Crypto,
    ManagedToolId::Host,
    ManagedToolId::Http,
    ManagedToolId::Image,
    ManagedToolId::Encode,
    ManagedToolId::Json,
    ManagedToolId::MessageBoard,
    ManagedToolId::Network,
    ManagedToolId::Pdf,
    ManagedToolId::Protobuf,
    ManagedToolId::QuickNote,
    ManagedToolId::Qrcode,
    ManagedToolId::Reformat,
    ManagedToolId::Regex,
    ManagedToolId::Runtime,
    ManagedToolId::Timestamp,
    ManagedToolId::TextDiff,
    ManagedToolId::Translation,
    ManagedToolId::Ua,
    ManagedToolId::Variables,
    ManagedToolId::System,
];

#[tauri::command]
pub async fn get_tool_webview_snapshot(
    caller: Webview,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn open_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    open_tool_webview_owned(&app, state.inner(), tool_id, bounds)
}

pub(crate) fn open_tool_webview_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
) -> AppResult<ToolWebviewSnapshot> {
    let bounds = bounds.validate()?;
    if app.get_webview(tool_id.webview_label()).is_some() {
        return Ok(state.snapshot(tool_id));
    }

    let main = main_window(app)?;
    state.begin_open(tool_id, bounds);
    let builder = WebviewBuilder::new(
        tool_id.webview_label(),
        WebviewUrl::App(PathBuf::from(tool_id.app_path())),
    )
    .on_page_load(move |webview, payload| {
        if payload.event() == PageLoadEvent::Finished {
            webview
                .app_handle()
                .state::<ToolWebviewManager>()
                .record_page_load(tool_id);
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
            state.mark_open_failed(tool_id);
            return Err(format!("failed to create tool WebView: {error}").into());
        }
    };
    webview
        .set_auto_resize(false)
        .map_err(|error| format!("failed to configure tool WebView resize: {error}"))?;
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn update_tool_webview_bounds(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    update_tool_webview_bounds_owned(&app, state.inner(), tool_id, bounds)
}

pub(crate) fn update_tool_webview_bounds_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
) -> AppResult<ToolWebviewSnapshot> {
    let bounds = bounds.validate()?;
    let snapshot = state.snapshot(tool_id);
    if snapshot.exists && snapshot.placement == ToolWebviewPlacement::Docked {
        let webview = tool_webview(app, tool_id)?;
        apply_docked_bounds(&webview, bounds)?;
        state.update_bounds(tool_id, bounds);
    }
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn set_tool_webview_visible(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
    visible: bool,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    set_tool_webview_visible_owned(&app, state.inner(), tool_id, visible)
}

pub(crate) fn set_tool_webview_visible_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
    visible: bool,
) -> AppResult<ToolWebviewSnapshot> {
    let snapshot = state.snapshot(tool_id);
    if snapshot.exists && snapshot.placement == ToolWebviewPlacement::Docked {
        let webview = tool_webview(app, tool_id)?;
        if visible {
            webview
                .show()
                .map_err(|error| format!("failed to show tool WebView: {error}"))?;
        } else {
            webview
                .hide()
                .map_err(|error| format!("failed to hide tool WebView: {error}"))?;
        }
        state.mark_visible(tool_id, visible);
    }
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn detach_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    detach_tool_webview_owned(&app, state.inner(), tool_id)
}

pub(crate) fn detach_tool_webview_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
) -> AppResult<ToolWebviewSnapshot> {
    let snapshot = state.snapshot(tool_id);
    if !snapshot.exists {
        return Err("tool WebView has not been created".into());
    }
    if snapshot.placement == ToolWebviewPlacement::Detached {
        return Ok(snapshot);
    }

    let detached = ensure_detached_window(app, tool_id)?;
    let webview = tool_webview(app, tool_id)?;
    detached
        .set_size(
            webview
                .size()
                .map_err(|error| format!("failed to read tool WebView size: {error}"))?,
        )
        .map_err(|error| format!("failed to match detached window size: {error}"))?;
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

    state.record_reparent_operations(tool_id, 1);
    state.mark_detached(tool_id);
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn dock_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    dock_tool_webview_owned(&app, state.inner(), tool_id, bounds)
}

pub(crate) fn dock_tool_webview_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
) -> AppResult<ToolWebviewSnapshot> {
    let bounds = bounds.validate()?;
    let snapshot = state.snapshot(tool_id);
    if !snapshot.exists {
        return Err("tool WebView has not been created".into());
    }
    if snapshot.placement == ToolWebviewPlacement::Docked {
        let webview = tool_webview(app, tool_id)?;
        apply_docked_bounds(&webview, bounds)?;
        state.mark_docked(tool_id, bounds);
        return Ok(state.snapshot(tool_id));
    }

    let main = main_window(app)?;
    let webview = tool_webview(app, tool_id)?;
    webview
        .set_auto_resize(false)
        .map_err(|error| format!("failed to disable detached auto-resize: {error}"))?;
    webview
        .reparent(&main)
        .map_err(|error| format!("failed to dock tool WebView: {error}"))?;
    apply_docked_bounds(&webview, bounds)?;
    if let Some(detached) = app.get_window(tool_id.detached_window_label()) {
        detached
            .destroy()
            .map_err(|error| format!("failed to destroy detached tool window: {error}"))?;
    }

    state.record_reparent_operations(tool_id, 1);
    state.mark_docked(tool_id, bounds);
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn stress_tool_webview_reparent(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
    cycles: u32,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    stress_tool_webview_reparent_owned(&app, state.inner(), tool_id, bounds, cycles)
}

pub(crate) fn stress_tool_webview_reparent_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
    bounds: ToolWebviewBounds,
    cycles: u32,
) -> AppResult<ToolWebviewSnapshot> {
    let bounds = bounds.validate()?;
    if !(1..=500).contains(&cycles) {
        return Err("stress cycles must be between 1 and 500".into());
    }
    let before = state.snapshot(tool_id);
    if !before.exists {
        return Err("tool WebView has not been created".into());
    }

    let main = main_window(app)?;
    let detached = ensure_detached_window(app, tool_id)?;
    let webview = tool_webview(app, tool_id)?;

    if before.placement == ToolWebviewPlacement::Detached {
        webview
            .set_auto_resize(false)
            .map_err(|error| format!("failed to disable detached auto-resize: {error}"))?;
        webview
            .reparent(&main)
            .map_err(|error| format!("failed to prepare docked stress state: {error}"))?;
        apply_docked_bounds(&webview, bounds)?;
        state.record_reparent_operations(tool_id, 1);
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
    state.record_reparent_operations(tool_id, cycles.saturating_mul(2));
    state.mark_docked(tool_id, bounds);
    state.finish_stress(
        tool_id,
        cycles,
        before.page_loads,
        before.session_id,
        before.state_digest,
    );
    webview
        .set_focus()
        .map_err(|error| format!("failed to focus tool WebView after stress: {error}"))?;
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn close_tool_webview(
    caller: Webview,
    app: AppHandle,
    state: State<'_, ToolWebviewManager>,
    tool_id: ManagedToolId,
) -> AppResult<ToolWebviewSnapshot> {
    require_shell(&caller)?;
    close_tool_webview_owned(&app, state.inner(), tool_id)
}

pub(crate) fn close_tool_webview_owned(
    app: &AppHandle,
    state: &ToolWebviewManager,
    tool_id: ManagedToolId,
) -> AppResult<ToolWebviewSnapshot> {
    if let Some(webview) = app.get_webview(tool_id.webview_label()) {
        webview
            .close()
            .map_err(|error| format!("failed to close tool WebView: {error}"))?;
    }
    if let Some(detached) = app.get_window(tool_id.detached_window_label()) {
        detached
            .destroy()
            .map_err(|error| format!("failed to destroy detached tool window: {error}"))?;
    }
    state.mark_closed(tool_id);
    Ok(state.snapshot(tool_id))
}

#[tauri::command]
pub async fn report_tool_webview_session(
    caller: Webview,
    state: State<'_, ToolWebviewManager>,
    report: ToolSessionReport,
) -> AppResult<ToolWebviewSnapshot> {
    let tool_id = ManagedToolId::from_webview_label(caller.label())
        .ok_or_else(|| "session state can only be reported by an owned tool WebView".to_string())?;
    if report.session_id.trim().is_empty() || report.session_id.len() > 128 {
        return Err("invalid tool WebView session ID".into());
    }
    if report.state_digest.len() > 100_000 {
        return Err("tool WebView state digest is too large".into());
    }
    if report.state_summary.len() > 512 {
        return Err("tool WebView state summary is too large".into());
    }
    state.report_session(tool_id, report);
    Ok(state.snapshot(tool_id))
}

impl ManagedToolId {
    pub(crate) fn as_str(self) -> &'static str {
        match self {
            Self::Calculator => "calculator",
            Self::Color => "color",
            Self::Config => "config",
            Self::Cron => "cron",
            Self::Crypto => "crypto",
            Self::Host => "host",
            Self::Http => "http",
            Self::Image => "image",
            Self::Encode => "encode",
            Self::EditorLab => "editor-lab",
            Self::Json => "json",
            Self::MessageBoard => "message-board",
            Self::Network => "network",
            Self::Pdf => "pdf",
            Self::Protobuf => "protobuf",
            Self::QuickNote => "quick-note",
            Self::Qrcode => "qrcode",
            Self::Reformat => "reformat",
            Self::Regex => "regex",
            Self::Runtime => "runtime",
            Self::Timestamp => "timestamp",
            Self::TextDiff => "text-diff",
            Self::Translation => "translation",
            Self::Ua => "ua",
            Self::Variables => "variables",
            Self::System => "system",
            Self::WebviewProbe => "webview-probe",
        }
    }

    pub(crate) fn webview_label(self) -> &'static str {
        match self {
            Self::Calculator => "tool-calculator",
            Self::Color => "tool-color",
            Self::Config => "tool-config",
            Self::Cron => "tool-cron",
            Self::Crypto => "tool-crypto",
            Self::Host => "tool-host",
            Self::Http => "tool-http",
            Self::Image => "tool-image",
            Self::Encode => "tool-encode",
            Self::EditorLab => "tool-editor-lab",
            Self::Json => "tool-json",
            Self::MessageBoard => "tool-message-board",
            Self::Network => "tool-network",
            Self::Pdf => "tool-pdf",
            Self::Protobuf => "tool-protobuf",
            Self::QuickNote => "tool-quick-note",
            Self::Qrcode => "tool-qrcode",
            Self::Reformat => "tool-reformat",
            Self::Regex => "tool-regex",
            Self::Runtime => "tool-runtime",
            Self::Timestamp => "tool-timestamp",
            Self::TextDiff => "tool-text-diff",
            Self::Translation => "tool-translation",
            Self::Ua => "tool-ua",
            Self::Variables => "tool-variables",
            Self::System => "tool-system",
            Self::WebviewProbe => "p0-tool-probe",
        }
    }

    fn detached_window_label(self) -> &'static str {
        match self {
            Self::Calculator => "tool-detached-calculator",
            Self::Color => "tool-detached-color",
            Self::Config => "tool-detached-config",
            Self::Cron => "tool-detached-cron",
            Self::Crypto => "tool-detached-crypto",
            Self::Host => "tool-detached-host",
            Self::Http => "tool-detached-http",
            Self::Image => "tool-detached-image",
            Self::Encode => "tool-detached-encode",
            Self::EditorLab => "tool-detached-editor-lab",
            Self::Json => "tool-detached-json",
            Self::MessageBoard => "tool-detached-message-board",
            Self::Network => "tool-detached-network",
            Self::Pdf => "tool-detached-pdf",
            Self::Protobuf => "tool-detached-protobuf",
            Self::QuickNote => "tool-detached-quick-note",
            Self::Qrcode => "tool-detached-qrcode",
            Self::Reformat => "tool-detached-reformat",
            Self::Regex => "tool-detached-regex",
            Self::Runtime => "tool-detached-runtime",
            Self::Timestamp => "tool-detached-timestamp",
            Self::TextDiff => "tool-detached-text-diff",
            Self::Translation => "tool-detached-translation",
            Self::Ua => "tool-detached-ua",
            Self::Variables => "tool-detached-variables",
            Self::System => "tool-detached-system",
            Self::WebviewProbe => "tool-detached-webview-probe",
        }
    }

    fn app_path(self) -> &'static str {
        match self {
            Self::Calculator => "index.html?surface=calculator",
            Self::Color => "index.html?surface=color",
            Self::Config => "index.html?surface=config",
            Self::Cron => "index.html?surface=cron",
            Self::Crypto => "index.html?surface=crypto",
            Self::Host => "index.html?surface=host",
            Self::Http => "index.html?surface=http",
            Self::Image => "index.html?surface=image",
            Self::Encode => "index.html?surface=encode",
            Self::EditorLab => "index.html?surface=editor-lab",
            Self::Json => "index.html?surface=json",
            Self::MessageBoard => "index.html?surface=message-board",
            Self::Network => "index.html?surface=network",
            Self::Pdf => "index.html?surface=pdf",
            Self::Protobuf => "index.html?surface=protobuf",
            Self::QuickNote => "index.html?surface=quick-note",
            Self::Qrcode => "index.html?surface=qrcode",
            Self::Reformat => "index.html?surface=reformat",
            Self::Regex => "index.html?surface=regex",
            Self::Runtime => "index.html?surface=runtime",
            Self::Timestamp => "index.html?surface=timestamp",
            Self::TextDiff => "index.html?surface=text-diff",
            Self::Translation => "index.html?surface=translation",
            Self::Ua => "index.html?surface=ua",
            Self::Variables => "index.html?surface=variables",
            Self::System => "index.html?surface=system",
            Self::WebviewProbe => "index.html?surface=tool-probe",
        }
    }

    fn detached_window_title(self) -> &'static str {
        match self {
            Self::Calculator => "MooTool Calculator",
            Self::Color => "MooTool Color Palette",
            Self::Config => "MooTool YAML / Properties",
            Self::Cron => "MooTool Cron",
            Self::Crypto => "MooTool Crypto & Random",
            Self::Host => "MooTool Host",
            Self::Http => "MooTool HTTP Client",
            Self::Image => "MooTool Image Tools",
            Self::Encode => "MooTool Encode & Decode",
            Self::EditorLab => "MooTool CodeMirror Lab",
            Self::Json => "MooTool JSON",
            Self::MessageBoard => "MooTool Message Board",
            Self::Network => "MooTool Network & IP",
            Self::Pdf => "MooTool PDF",
            Self::Protobuf => "MooTool Protobuf",
            Self::QuickNote => "MooTool Quick Note",
            Self::Qrcode => "MooTool QR Code",
            Self::Reformat => "MooTool Reformat",
            Self::Regex => "MooTool Regex",
            Self::Runtime => "MooTool Code Runner",
            Self::Timestamp => "MooTool Time Convert",
            Self::TextDiff => "MooTool Text Diff",
            Self::Translation => "MooTool Translation",
            Self::Ua => "MooTool User-Agent Analyzer",
            Self::Variables => "MooTool Environment Variables",
            Self::System => "MooTool Hardware & System",
            Self::WebviewProbe => "MooTool WebView Reparent Probe",
        }
    }

    fn from_webview_label(label: &str) -> Option<Self> {
        match label {
            "tool-calculator" => Some(Self::Calculator),
            "tool-color" => Some(Self::Color),
            "tool-config" => Some(Self::Config),
            "tool-cron" => Some(Self::Cron),
            "tool-crypto" => Some(Self::Crypto),
            "tool-host" => Some(Self::Host),
            "tool-http" => Some(Self::Http),
            "tool-image" => Some(Self::Image),
            "tool-encode" => Some(Self::Encode),
            "tool-editor-lab" => Some(Self::EditorLab),
            "tool-json" => Some(Self::Json),
            "tool-message-board" => Some(Self::MessageBoard),
            "tool-network" => Some(Self::Network),
            "tool-pdf" => Some(Self::Pdf),
            "tool-protobuf" => Some(Self::Protobuf),
            "tool-quick-note" => Some(Self::QuickNote),
            "tool-qrcode" => Some(Self::Qrcode),
            "tool-reformat" => Some(Self::Reformat),
            "tool-regex" => Some(Self::Regex),
            "tool-runtime" => Some(Self::Runtime),
            "tool-timestamp" => Some(Self::Timestamp),
            "tool-text-diff" => Some(Self::TextDiff),
            "tool-translation" => Some(Self::Translation),
            "tool-ua" => Some(Self::Ua),
            "tool-variables" => Some(Self::Variables),
            "tool-system" => Some(Self::System),
            "p0-tool-probe" => Some(Self::WebviewProbe),
            _ => None,
        }
    }
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

fn tool_webview(app: &AppHandle, tool_id: ManagedToolId) -> Result<Webview, String> {
    app.get_webview(tool_id.webview_label())
        .ok_or_else(|| "tool WebView is not available".into())
}

fn ensure_detached_window(
    app: &AppHandle,
    tool_id: ManagedToolId,
) -> Result<tauri::Window, String> {
    if let Some(window) = app.get_window(tool_id.detached_window_label()) {
        return Ok(window);
    }

    WindowBuilder::new(app, tool_id.detached_window_label())
        .title(tool_id.detached_window_title())
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
