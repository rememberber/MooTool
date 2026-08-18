use std::{
    fs,
    io::Write,
    path::{Path, PathBuf},
    sync::{Arc, Mutex, mpsc},
    thread,
    time::{Duration, SystemTime, UNIX_EPOCH},
};

use tauri::{PhysicalPosition, PhysicalSize, Runtime, WebviewWindow, Window};
use tempfile::NamedTempFile;

use crate::contracts::desktop::{WindowPlacement, WindowStateFile};

pub const WINDOW_STATE_FILE_NAME: &str = "window-state.json";
const PERSIST_DEBOUNCE: Duration = Duration::from_millis(300);

struct WindowStateInner {
    file_path: PathBuf,
    current: Mutex<WindowStateFile>,
}

#[derive(Clone)]
pub struct WindowStateRepository {
    inner: Arc<WindowStateInner>,
    persist_tx: mpsc::Sender<()>,
}

impl WindowStateRepository {
    pub fn open(file_path: PathBuf) -> Result<Self, String> {
        let state = load_or_recover(&file_path)?;
        let inner = Arc::new(WindowStateInner {
            file_path,
            current: Mutex::new(state),
        });
        let (persist_tx, persist_rx) = mpsc::channel();
        let worker_inner = Arc::clone(&inner);
        thread::Builder::new()
            .name("mootool-window-state".into())
            .spawn(move || persist_worker(worker_inner, persist_rx))
            .map_err(|error| format!("failed to start window state persistence worker: {error}"))?;
        Ok(Self { inner, persist_tx })
    }

    pub fn placement(&self, label: &str) -> Option<WindowPlacement> {
        self.inner
            .current
            .lock()
            .ok()
            .and_then(|state| state.windows.get(label).cloned())
    }

    pub fn snapshot(&self) -> WindowStateFile {
        self.inner
            .current
            .lock()
            .expect("window state repository poisoned")
            .clone()
    }

    pub fn remember_window<R: Runtime>(&self, window: &Window<R>) -> Result<(), String> {
        let label = window.label().to_string();
        let maximized = window
            .is_maximized()
            .map_err(|error| format!("failed to inspect {label} maximized state: {error}"))?;
        let mut state = self
            .inner
            .current
            .lock()
            .map_err(|_| "window state repository poisoned".to_string())?;

        if maximized && state.windows.contains_key(&label) {
            if let Some(current) = state.windows.get_mut(&label) {
                current.maximized = true;
            }
        } else {
            let position = window
                .outer_position()
                .map_err(|error| format!("failed to inspect {label} position: {error}"))?;
            let size = window
                .inner_size()
                .map_err(|error| format!("failed to inspect {label} size: {error}"))?;
            let placement = WindowPlacement {
                x: position.x,
                y: position.y,
                width: size.width,
                height: size.height,
                maximized,
            };
            placement.validate()?;
            state.windows.insert(label, placement);
        }
        drop(state);
        self.persist_tx
            .send(())
            .map_err(|_| "window state persistence worker stopped".to_string())
    }

    pub fn restore_window<R: Runtime>(&self, window: &WebviewWindow<R>) -> Result<bool, String> {
        let Some(placement) = self.placement(window.label()) else {
            return Ok(false);
        };
        placement.validate()?;

        window
            .set_size(PhysicalSize::new(placement.width, placement.height))
            .map_err(|error| format!("failed to restore {} size: {error}", window.label()))?;

        let monitors = window.available_monitors().map_err(|error| {
            format!(
                "failed to list monitors while restoring {}: {error}",
                window.label()
            )
        })?;
        if placement_intersects_monitors(&placement, &monitors) {
            window
                .set_position(PhysicalPosition::new(placement.x, placement.y))
                .map_err(|error| {
                    format!("failed to restore {} position: {error}", window.label())
                })?;
        } else {
            window
                .center()
                .map_err(|error| format!("failed to center {}: {error}", window.label()))?;
        }
        if placement.maximized {
            window
                .maximize()
                .map_err(|error| format!("failed to maximize {}: {error}", window.label()))?;
        }
        Ok(true)
    }

    pub fn flush(&self) -> Result<(), String> {
        persist_snapshot(&self.inner)
    }

    #[cfg(test)]
    fn store_placement(&self, label: &str, placement: WindowPlacement) -> Result<(), String> {
        placement.validate()?;
        self.inner
            .current
            .lock()
            .map_err(|_| "window state repository poisoned".to_string())?
            .windows
            .insert(label.to_string(), placement);
        Ok(())
    }
}

fn persist_worker(inner: Arc<WindowStateInner>, receiver: mpsc::Receiver<()>) {
    while receiver.recv().is_ok() {
        while receiver.recv_timeout(PERSIST_DEBOUNCE).is_ok() {}
        if let Err(error) = persist_snapshot(&inner) {
            eprintln!("MooTool Next Tauri could not persist window state: {error}");
        }
    }
}

fn persist_snapshot(inner: &WindowStateInner) -> Result<(), String> {
    let state = inner
        .current
        .lock()
        .map_err(|_| "window state repository poisoned".to_string())?
        .clone();
    state.validate()?;
    write_atomically(&inner.file_path, &state)
}

fn load_or_recover(file_path: &Path) -> Result<WindowStateFile, String> {
    if !file_path.exists() {
        return Ok(WindowStateFile::default());
    }
    let bytes = fs::read(file_path).map_err(|error| {
        format!(
            "failed to read window state file {}: {error}",
            file_path.display()
        )
    })?;
    match serde_json::from_slice::<WindowStateFile>(&bytes) {
        Ok(state) if state.validate().is_ok() => Ok(state),
        Ok(_) | Err(_) => {
            let timestamp = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs();
            let recovered_path =
                file_path.with_file_name(format!("window-state.corrupt-{timestamp}.json"));
            fs::rename(file_path, &recovered_path).map_err(|error| {
                format!(
                    "invalid window state could not be preserved as {}: {error}",
                    recovered_path.display()
                )
            })?;
            Ok(WindowStateFile::default())
        }
    }
}

fn write_atomically(file_path: &Path, state: &WindowStateFile) -> Result<(), String> {
    let parent = file_path
        .parent()
        .ok_or_else(|| "window state file has no parent directory".to_string())?;
    fs::create_dir_all(parent).map_err(|error| {
        format!(
            "failed to create window state directory {}: {error}",
            parent.display()
        )
    })?;
    let bytes = serde_json::to_vec_pretty(state)
        .map_err(|error| format!("failed to serialize window state: {error}"))?;
    let mut temporary = NamedTempFile::new_in(parent)
        .map_err(|error| format!("failed to create temporary window state: {error}"))?;
    temporary
        .write_all(&bytes)
        .and_then(|_| temporary.write_all(b"\n"))
        .and_then(|_| temporary.flush())
        .map_err(|error| format!("failed to write temporary window state: {error}"))?;
    temporary
        .as_file()
        .sync_all()
        .map_err(|error| format!("failed to sync temporary window state: {error}"))?;
    temporary
        .persist(file_path)
        .map_err(|error| format!("failed to replace window state atomically: {error}"))?;
    Ok(())
}

fn placement_intersects_monitors(
    placement: &WindowPlacement,
    monitors: &[tauri::window::Monitor],
) -> bool {
    if monitors.is_empty() {
        return false;
    }
    let left = i64::from(placement.x);
    let top = i64::from(placement.y);
    let right = left + i64::from(placement.width);
    let bottom = top + i64::from(placement.height);

    monitors.iter().any(|monitor| {
        let position = monitor.position();
        let size = monitor.size();
        let monitor_left = i64::from(position.x);
        let monitor_top = i64::from(position.y);
        let monitor_right = monitor_left + i64::from(size.width);
        let monitor_bottom = monitor_top + i64::from(size.height);
        let intersection_width = right.min(monitor_right) - left.max(monitor_left);
        let intersection_height = bottom.min(monitor_bottom) - top.max(monitor_top);
        intersection_width >= 80 && intersection_height >= 80
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn persists_and_reopens_independent_window_state() {
        let directory = TempDir::new().expect("temporary directory");
        let path = directory.path().join(WINDOW_STATE_FILE_NAME);
        let repository = WindowStateRepository::open(path.clone()).expect("open state");
        let placement = WindowPlacement {
            x: 32,
            y: 48,
            width: 1280,
            height: 800,
            maximized: true,
        };
        repository
            .store_placement("main", placement.clone())
            .expect("store placement");
        repository.flush().expect("flush state");

        let reopened = WindowStateRepository::open(path).expect("reopen state");
        assert_eq!(reopened.placement("main"), Some(placement));
    }

    #[test]
    fn preserves_corrupt_state_without_using_other_product_data() {
        let directory = TempDir::new().expect("temporary directory");
        let path = directory.path().join(WINDOW_STATE_FILE_NAME);
        fs::write(&path, b"{broken").expect("write corrupt state");

        let repository = WindowStateRepository::open(path).expect("recover state");

        assert!(repository.placement("main").is_none());
        assert!(
            fs::read_dir(directory.path())
                .expect("read state directory")
                .filter_map(Result::ok)
                .any(|entry| entry
                    .file_name()
                    .to_string_lossy()
                    .starts_with("window-state.corrupt-"))
        );
    }
}
