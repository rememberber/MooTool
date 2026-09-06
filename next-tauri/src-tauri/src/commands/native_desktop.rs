use std::{collections::HashSet, sync::Mutex};

use image::{ExtendedColorType, ImageEncoder, codecs::png::PngEncoder};
use keepawake::KeepAwake;
use mouse_position::mouse_position::Mouse;
use tauri::Manager;
use tokio::sync::Mutex as AsyncMutex;
use xcap::Monitor;

use crate::{
    contracts::{
        error::AppResult,
        native_desktop::{DisplaySleepStatus, ScreenCaptureResult, ScreenColorSample},
    },
    repositories::local_data::LocalDataRepository,
};

const MAX_CAPTURED_IMAGE_BYTES: usize = 20 * 1024 * 1024;
const MAX_MONITORS: usize = 16;

#[derive(Default)]
pub struct NativeDesktopManager {
    display_sleep: Mutex<DisplaySleepState>,
    capture: AsyncMutex<()>,
}

#[derive(Default)]
struct DisplaySleepState {
    token: Option<KeepAwake>,
    owners: HashSet<String>,
}

#[derive(Debug)]
struct HiddenWindow {
    label: String,
    focused: bool,
}

struct CapturedMonitor {
    display_id: u32,
    width: u32,
    height: u32,
    png: Vec<u8>,
}

#[tauri::command]
pub fn get_display_sleep_status(
    manager: tauri::State<'_, NativeDesktopManager>,
    owner: String,
) -> AppResult<DisplaySleepStatus> {
    validate_sleep_owner(&owner)?;
    let guard = manager
        .display_sleep
        .lock()
        .map_err(|_| "display sleep state is unavailable")?;
    Ok(DisplaySleepStatus {
        active: guard.token.is_some(),
        owned: guard.owners.contains(&owner),
    })
}

#[tauri::command]
pub fn set_display_sleep_prevention(
    manager: tauri::State<'_, NativeDesktopManager>,
    owner: String,
    enabled: bool,
) -> AppResult<DisplaySleepStatus> {
    validate_sleep_owner(&owner)?;
    let mut guard = manager
        .display_sleep
        .lock()
        .map_err(|_| "display sleep state is unavailable")?;
    if enabled && !guard.owners.contains(&owner) {
        guard.owners.insert(owner.clone());
    } else if !enabled {
        guard.owners.remove(&owner);
    }
    if !guard.owners.is_empty() && guard.token.is_none() {
        let token = keepawake::Builder::default()
            .display(true)
            .idle(true)
            .reason("MooTool display mode")
            .app_name("MooTool Next Tauri")
            .app_reverse_domain("com.rememberber.mootool.next.tauri")
            .create()
            .map_err(|error| {
                guard.owners.remove(&owner);
                format!("failed to prevent display sleep: {error}")
            })?;
        guard.token = Some(token);
    } else if guard.owners.is_empty() {
        guard.token = None;
    }
    Ok(DisplaySleepStatus {
        active: guard.token.is_some(),
        owned: guard.owners.contains(&owner),
    })
}

#[tauri::command]
pub async fn capture_display_images(
    app: tauri::AppHandle,
    manager: tauri::State<'_, NativeDesktopManager>,
    delay_ms: u64,
) -> AppResult<ScreenCaptureResult> {
    validate_delay(delay_ms, 100, 2_000)?;
    let _capture_guard = manager.capture.lock().await;
    let hidden = hide_visible_windows(&app)?;
    tokio::time::sleep(std::time::Duration::from_millis(delay_ms)).await;
    let captured = tauri::async_runtime::spawn_blocking(capture_all_monitors)
        .await
        .map_err(|error| format!("screen capture worker failed: {error}"));
    restore_windows(&app, &hidden);
    let captured = captured??;
    if captured
        .iter()
        .any(|capture| capture.png.len() > MAX_CAPTURED_IMAGE_BYTES)
    {
        return Err("a captured display exceeds the 20 MiB image-library limit".into());
    }

    let repository = app.state::<LocalDataRepository>();
    let timestamp = now_millis();
    let mut assets = Vec::with_capacity(captured.len());
    for (index, capture) in captured.into_iter().enumerate() {
        let name = format!(
            "Screenshot-{timestamp}-{}-{}.png",
            capture.display_id,
            index + 1
        );
        match super::images::persist_image_bytes(
            &app,
            &repository,
            &name,
            "image/png",
            capture.width,
            capture.height,
            &capture.png,
        ) {
            Ok(asset) => assets.push(asset),
            Err(error) => {
                for asset in &assets {
                    super::images::remove_persisted_image(&app, &repository, &asset.name);
                }
                return Err(error.into());
            }
        }
    }
    let monitor_count = assets.len();
    Ok(ScreenCaptureResult {
        assets,
        monitor_count,
    })
}

#[tauri::command]
pub async fn sample_screen_color(
    app: tauri::AppHandle,
    manager: tauri::State<'_, NativeDesktopManager>,
    delay_ms: u64,
) -> AppResult<ScreenColorSample> {
    validate_delay(delay_ms, 500, 5_000)?;
    let _capture_guard = manager.capture.lock().await;
    let hidden = hide_visible_windows(&app)?;
    tokio::time::sleep(std::time::Duration::from_millis(delay_ms)).await;
    let sampled = tauri::async_runtime::spawn_blocking(sample_color_at_pointer)
        .await
        .map_err(|error| format!("screen color worker failed: {error}"));
    restore_windows(&app, &hidden);
    Ok(sampled??)
}

fn capture_all_monitors() -> Result<Vec<CapturedMonitor>, String> {
    let mut monitors = Monitor::all().map_err(screen_capture_error)?;
    if monitors.is_empty() || monitors.len() > MAX_MONITORS {
        return Err(format!(
            "screen capture requires between 1 and {MAX_MONITORS} displays"
        ));
    }
    monitors.sort_by_key(|monitor| {
        (
            monitor.y().unwrap_or(i32::MAX),
            monitor.x().unwrap_or(i32::MAX),
        )
    });
    monitors
        .into_iter()
        .map(|monitor| {
            let display_id = monitor.id().map_err(screen_capture_error)?;
            let frame = monitor.capture_image().map_err(screen_capture_error)?;
            let width = frame.width();
            let height = frame.height();
            let mut png = Vec::new();
            PngEncoder::new(&mut png)
                .write_image(frame.as_raw(), width, height, ExtendedColorType::Rgba8)
                .map_err(|error| format!("failed to encode display capture: {error}"))?;
            Ok(CapturedMonitor {
                display_id,
                width,
                height,
                png,
            })
        })
        .collect()
}

fn sample_color_at_pointer() -> Result<ScreenColorSample, String> {
    let (x, y) = match Mouse::get_mouse_position() {
        Mouse::Position { x, y } => (x, y),
        Mouse::Error => {
            return Err(
                "global pointer position is unavailable; Wayland may require an X11 session".into(),
            );
        }
    };
    let monitor = Monitor::from_point(x, y).map_err(screen_capture_error)?;
    let monitor_x = monitor.x().map_err(screen_capture_error)?;
    let monitor_y = monitor.y().map_err(screen_capture_error)?;
    let width = monitor.width().map_err(screen_capture_error)?;
    let height = monitor.height().map_err(screen_capture_error)?;
    let local_x = x
        .saturating_sub(monitor_x)
        .clamp(0, width.saturating_sub(1) as i32) as u32;
    let local_y = y
        .saturating_sub(monitor_y)
        .clamp(0, height.saturating_sub(1) as i32) as u32;
    let frame = monitor
        .capture_region(local_x, local_y, 1, 1)
        .map_err(screen_capture_error)?;
    let pixel = frame
        .pixels()
        .next()
        .ok_or_else(|| "screen color capture returned no pixels".to_string())?;
    Ok(color_sample(x, y, pixel.0))
}

fn color_sample(x: i32, y: i32, pixel: [u8; 4]) -> ScreenColorSample {
    let [red, green, blue, _alpha] = pixel;
    ScreenColorSample {
        hex: format!("#{red:02X}{green:02X}{blue:02X}"),
        red,
        green,
        blue,
        x,
        y,
    }
}

fn hide_visible_windows(app: &tauri::AppHandle) -> Result<Vec<HiddenWindow>, String> {
    let mut hidden = Vec::new();
    for (label, window) in app.webview_windows() {
        if window
            .is_visible()
            .map_err(|error| format!("failed to inspect window visibility: {error}"))?
        {
            let focused = window.is_focused().unwrap_or(false);
            if let Err(error) = window.hide() {
                restore_windows(app, &hidden);
                return Err(format!(
                    "failed to hide MooTool before screen capture: {error}"
                ));
            }
            hidden.push(HiddenWindow { label, focused });
        }
    }
    Ok(hidden)
}

fn restore_windows(app: &tauri::AppHandle, hidden: &[HiddenWindow]) {
    for item in hidden {
        if let Some(window) = app.get_webview_window(&item.label) {
            if let Err(error) = window.show() {
                tracing::warn!(error = %error, window = %item.label, "failed to restore window after screen capture");
            }
        }
    }
    if let Some(item) = hidden.iter().find(|item| item.focused) {
        if let Some(window) = app.get_webview_window(&item.label) {
            let _ = window.set_focus();
        }
    }
}

fn validate_delay(value: u64, minimum: u64, maximum: u64) -> Result<(), String> {
    if (minimum..=maximum).contains(&value) {
        Ok(())
    } else {
        Err(format!(
            "screen capture delay must be between {minimum} and {maximum} milliseconds"
        ))
    }
}

fn validate_sleep_owner(value: &str) -> Result<(), String> {
    if (1..=64).contains(&value.len())
        && value
            .bytes()
            .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
    {
        Ok(())
    } else {
        Err("display sleep owner must be a lowercase product identifier".into())
    }
}

fn screen_capture_error(error: impl std::fmt::Display) -> String {
    format!(
        "native screen capture failed: {error}; verify screen-recording permission and the current desktop session"
    )
}

fn now_millis() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(i64::MAX)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn formats_sampled_color_and_validates_capture_delays() {
        assert_eq!(
            color_sample(-20, 30, [0x2f, 0x6f, 0xed, 0xff]),
            ScreenColorSample {
                hex: "#2F6FED".into(),
                red: 0x2f,
                green: 0x6f,
                blue: 0xed,
                x: -20,
                y: 30,
            }
        );
        assert!(validate_delay(500, 500, 5_000).is_ok());
        assert!(validate_delay(499, 500, 5_000).is_err());
        assert!(validate_sleep_owner("message-board-presentation").is_ok());
        assert!(validate_sleep_owner("Other Product").is_err());
    }

    #[test]
    #[ignore = "requires an interactive desktop session and screen-recording permission"]
    fn native_screen_capture_smoke() {
        let captures = capture_all_monitors().expect("capture attached displays");
        assert!(!captures.is_empty());
        assert!(captures.iter().all(|capture| {
            capture.width > 0 && capture.height > 0 && capture.png.starts_with(b"\x89PNG\r\n\x1a\n")
        }));
        let sample = sample_color_at_pointer().expect("sample color under pointer");
        assert_eq!(sample.hex.len(), 7);
    }
}
