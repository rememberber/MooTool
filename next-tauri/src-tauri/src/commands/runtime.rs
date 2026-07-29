use crate::contracts::runtime::RuntimeInfo;

#[tauri::command]
pub fn get_runtime_info(app: tauri::AppHandle) -> RuntimeInfo {
    RuntimeInfo::collect(app.package_info().version.to_string())
}
