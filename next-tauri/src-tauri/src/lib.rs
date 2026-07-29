mod commands;
mod contracts;
mod state;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(state::ToolWebviewManager::default())
        .invoke_handler(tauri::generate_handler![
            commands::runtime::get_runtime_info,
            commands::tool_webview::get_tool_webview_snapshot,
            commands::tool_webview::open_tool_webview,
            commands::tool_webview::update_tool_webview_bounds,
            commands::tool_webview::set_tool_webview_visible,
            commands::tool_webview::detach_tool_webview,
            commands::tool_webview::dock_tool_webview,
            commands::tool_webview::stress_tool_webview_reparent,
            commands::tool_webview::close_tool_webview,
            commands::tool_webview::report_tool_webview_probe
        ])
        .run(tauri::generate_context!())
        .expect("failed to run MooTool Next Tauri");
}
