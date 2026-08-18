use tauri::Emitter;

use crate::{
    contracts::{error::AppResult, local_data::OperationHistory},
    repositories::local_data::LocalDataRepository,
};

pub const HISTORY_CHANGED_EVENT: &str = "mootool://operation-history-changed";

#[tauri::command]
pub fn record_operation_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    entry: OperationHistory,
    history_limit: u16,
) -> AppResult<OperationHistory> {
    let saved = repository.record_operation(entry, history_limit)?;
    app.emit(HISTORY_CHANGED_EVENT, ())
        .map_err(|error| format!("history was saved but synchronization failed: {error}"))?;
    Ok(saved)
}

#[tauri::command]
pub fn list_operation_history(
    repository: tauri::State<'_, LocalDataRepository>,
    limit: u16,
) -> AppResult<Vec<OperationHistory>> {
    Ok(repository.list_operations(limit)?)
}

#[tauri::command]
pub fn delete_operation_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
    id: String,
) -> AppResult<bool> {
    let deleted = repository.delete_operation(&id)?;
    if deleted {
        app.emit(HISTORY_CHANGED_EVENT, ())
            .map_err(|error| format!("history was deleted but synchronization failed: {error}"))?;
    }
    Ok(deleted)
}

#[tauri::command]
pub fn clear_operation_history(
    app: tauri::AppHandle,
    repository: tauri::State<'_, LocalDataRepository>,
) -> AppResult<usize> {
    let deleted = repository.clear_operations()?;
    if deleted > 0 {
        app.emit(HISTORY_CHANGED_EVENT, ())
            .map_err(|error| format!("history was cleared but synchronization failed: {error}"))?;
    }
    Ok(deleted)
}
