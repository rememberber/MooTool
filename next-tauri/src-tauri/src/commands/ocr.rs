use std::{
    io::{Cursor, Write},
    path::PathBuf,
    process::{Command, Stdio},
    sync::Mutex,
    thread,
    time::{Duration, Instant},
};

use base64::{Engine as _, engine::general_purpose::STANDARD};
use image::ImageFormat;
use serde::{Deserialize, Serialize};

use crate::contracts::error::AppResult;

const LANGUAGES: &[&str] = &["zh-Hans", "zh-Hant", "en-US", "ja-JP"];
const MAX_DATA_URL_CHARS: usize = 48 * 1024 * 1024;
const MAX_PNG_BYTES: usize = 32 * 1024 * 1024;
const MAX_SIDE: u32 = 16_384;
const MAX_PIXELS: u64 = 32_000_000;

struct ActiveTask {
    request_id: String,
    pid: u32,
    timed_out: bool,
    cancelled: bool,
}

static ACTIVE: Mutex<Option<ActiveTask>> = Mutex::new(None);

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OcrStatus {
    pub available: bool,
    pub engine: Option<String>,
    pub languages: Vec<String>,
    pub reason: Option<String>,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OcrRequest {
    pub request_id: String,
    pub language: String,
    pub source: OcrSource,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OcrSource {
    pub name: String,
    pub data_url: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OcrLine {
    pub text: String,
    pub confidence: f32,
    #[serde(rename = "box")]
    pub bounds: OcrBox,
}

#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct OcrBox {
    pub x: f64,
    pub y: f64,
    pub width: f64,
    pub height: f64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OcrResult {
    pub request_id: String,
    pub source_name: String,
    pub engine: String,
    pub language: String,
    pub width: u32,
    pub height: u32,
    pub text: String,
    pub lines: Vec<OcrLine>,
    pub duration_ms: u64,
}

#[derive(Deserialize)]
struct HelperStatus {
    languages: Vec<String>,
}

#[derive(Deserialize)]
struct HelperOutput {
    width: u32,
    height: u32,
    lines: Vec<HelperLine>,
}

#[derive(Deserialize)]
struct HelperLine {
    text: String,
    confidence: f32,
    #[serde(rename = "box")]
    bounds: OcrBox,
}

#[derive(Deserialize)]
struct HelperError {
    error: String,
}

#[tauri::command]
pub fn image_text_recognition_status() -> OcrStatus {
    recognition_status()
}

#[tauri::command]
pub async fn recognize_image_text(request: OcrRequest) -> AppResult<OcrResult> {
    tauri::async_runtime::spawn_blocking(move || recognize(request))
        .await
        .map_err(|error| error.to_string())?
}

#[tauri::command]
pub fn cancel_image_text_recognition(request_id: String) -> bool {
    let Some(pid) = ({
        let mut active = ACTIVE.lock().unwrap_or_else(|error| error.into_inner());
        let matches = active.as_ref().is_some_and(|task| task.request_id == request_id);
        if matches {
            if let Some(task) = active.as_mut() {
                task.cancelled = true;
            }
            active.as_ref().map(|task| task.pid)
        } else {
            None
        }
    }) else {
        return false;
    };
    let _ = Command::new("kill").args(["-KILL", &pid.to_string()]).status();
    true
}

fn recognition_status() -> OcrStatus {
    if !cfg!(target_os = "macos") {
        return unavailable("platform");
    }
    let helper = helper_path();
    if !helper.is_file() {
        return unavailable("component");
    }
    let output = Command::new(&helper).arg("--status").output();
    let Ok(output) = output else {
        return unavailable("component");
    };
    if !output.status.success() {
        return unavailable("system");
    }
    let Ok(status) = serde_json::from_slice::<HelperStatus>(&output.stdout) else {
        return unavailable("component");
    };
    let languages = LANGUAGES.iter().filter(|language| status.languages.iter().any(|item| item == *language)).map(|language| (*language).to_string()).collect::<Vec<_>>();
    if languages.is_empty() {
        return unavailable("system");
    }
    let mut offered = vec!["auto".to_string()];
    offered.extend(languages);
    OcrStatus { available: true, engine: Some("apple-vision".into()), languages: offered, reason: None }
}

fn unavailable(reason: &str) -> OcrStatus {
    OcrStatus { available: false, engine: None, languages: Vec::new(), reason: Some(reason.into()) }
}

fn recognize(request: OcrRequest) -> AppResult<OcrResult> {
    validate_request(&request)?;
    let status = recognition_status();
    if !status.available {
        return Err("image.ocr.error.unavailable".into());
    }
    if !status.languages.iter().any(|language| language == &request.language) {
        return Err("image.ocr.error.language".into());
    }
    {
        let active = ACTIVE.lock().unwrap_or_else(|error| error.into_inner());
        if active.is_some() {
            return Err("image.ocr.error.busy".into());
        }
    }
    let started = Instant::now();
    let (width, height, png) = decode_png(&request.source.data_url)?;
    let directory = std::env::temp_dir().join(format!("mootool-ocr-{}", request.request_id));
    let path = directory.join("snapshot.png");
    let result = (|| {
        std::fs::create_dir_all(&directory).map_err(|_| "image.ocr.error.failed".to_string())?;
        std::fs::write(&path, &png).map_err(|_| "image.ocr.error.failed".to_string())?;
        run_helper(&request, &path, width, height, started)
    })();
    let _ = std::fs::remove_dir_all(&directory);
    result.map_err(Into::into)
}

fn run_helper(request: &OcrRequest, path: &std::path::Path, width: u32, height: u32, started: Instant) -> Result<OcrResult, String> {
    let helper = helper_path();
    let mut child = Command::new(&helper)
        .arg("--recognize")
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|_| "image.ocr.error.unavailable".to_string())?;
    let pid = child.id();
    {
        let mut active = ACTIVE.lock().unwrap_or_else(|error| error.into_inner());
        if active.is_some() {
            let _ = child.kill();
            return Err("image.ocr.error.busy".into());
        }
        *active = Some(ActiveTask { request_id: request.request_id.clone(), pid, timed_out: false, cancelled: false });
    }
    let request_id = request.request_id.clone();
    thread::spawn(move || {
        thread::sleep(Duration::from_secs(120));
        let mut active = ACTIVE.lock().unwrap_or_else(|error| error.into_inner());
        if active.as_ref().is_some_and(|task| task.request_id == request_id && task.pid == pid) {
            if let Some(task) = active.as_mut() {
                task.timed_out = true;
            }
            let _ = Command::new("kill").args(["-KILL", &pid.to_string()]).status();
        }
    });
    let payload = serde_json::json!({ "path": path, "language": request.language });
    if let Some(mut stdin) = child.stdin.take() {
        let _ = stdin.write_all(payload.to_string().as_bytes());
    }
    let output = child.wait_with_output().map_err(|_| "image.ocr.error.failed".to_string());
    let flags = {
        let mut active = ACTIVE.lock().unwrap_or_else(|error| error.into_inner());
        let flags = active.as_ref().filter(|task| task.request_id == request.request_id).map(|task| (task.cancelled, task.timed_out));
        if active.as_ref().is_some_and(|task| task.request_id == request.request_id) {
            *active = None;
        }
        flags.unwrap_or((false, false))
    };
    let output = output?;
    if flags.0 {
        return Err("image.ocr.error.cancelled".into());
    }
    if flags.1 {
        return Err("image.ocr.error.timeout".into());
    }
    if !output.status.success() {
        let code = serde_json::from_slice::<HelperError>(&output.stdout).map(|error| error.error).unwrap_or_else(|_| "failed".into());
        return Err(format!("image.ocr.error.{code}"));
    }
    let parsed = serde_json::from_slice::<HelperOutput>(&output.stdout).map_err(|_| "image.ocr.error.failed".to_string())?;
    if parsed.width != width || parsed.height != height || parsed.lines.len() > 2_000 {
        return Err("image.ocr.error.failed".into());
    }
    let lines = parsed.lines.into_iter().map(|line| OcrLine { text: line.text, confidence: line.confidence, bounds: line.bounds }).collect::<Vec<_>>();
    if lines.iter().map(|line| line.text.chars().count() + 1).sum::<usize>() > 100_000 {
        return Err("image.ocr.error.tooLarge".into());
    }
    Ok(OcrResult {
        request_id: request.request_id.clone(),
        source_name: request.source.name.clone(),
        engine: "apple-vision".into(),
        language: request.language.clone(),
        width,
        height,
        text: lines.iter().map(|line| line.text.as_str()).collect::<Vec<_>>().join("\n"),
        lines,
        duration_ms: started.elapsed().as_millis() as u64,
    })
}

fn validate_request(request: &OcrRequest) -> Result<(), String> {
    if !request.request_id.chars().all(|char| char.is_ascii_alphanumeric() || char == '-' || char == '_') || request.request_id.is_empty() || request.request_id.len() > 64 {
        return Err("image.ocr.error.input".into());
    }
    if request.language != "auto" && !LANGUAGES.contains(&request.language.as_str()) {
        return Err("image.ocr.error.language".into());
    }
    let name = request.source.name.trim();
    if name.is_empty() || name.len() > 160 || name.chars().any(|char| char == '/' || char == '\\' || char.is_control()) {
        return Err("image.ocr.error.input".into());
    }
    if request.source.data_url.len() > MAX_DATA_URL_CHARS {
        return Err("image.ocr.error.tooLarge".into());
    }
    Ok(())
}

fn decode_png(data_url: &str) -> Result<(u32, u32, Vec<u8>), String> {
    let (header, encoded) = data_url.split_once(',').ok_or_else(|| "image.ocr.error.input".to_string())?;
    if !header.starts_with("data:image/") || !header.contains(";base64") {
        return Err("image.ocr.error.input".into());
    }
    let bytes = STANDARD.decode(encoded.trim()).map_err(|_| "image.ocr.error.input".to_string())?;
    let image = image::load_from_memory(&bytes).map_err(|_| "image.ocr.error.input".to_string())?;
    let width = image.width();
    let height = image.height();
    if width == 0 || height == 0 || width > MAX_SIDE || height > MAX_SIDE || u64::from(width) * u64::from(height) > MAX_PIXELS {
        return Err("image.ocr.error.tooLarge".into());
    }
    let mut png = Cursor::new(Vec::new());
    image.write_to(&mut png, ImageFormat::Png).map_err(|_| "image.ocr.error.input".to_string())?;
    let bytes = png.into_inner();
    if bytes.len() > MAX_PNG_BYTES {
        return Err("image.ocr.error.tooLarge".into());
    }
    Ok((width, height, bytes))
}

fn helper_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("native/mootool-ocr")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_a_path_like_image_name() {
        let request = OcrRequest {
            request_id: "sample-id".into(),
            language: "auto".into(),
            source: OcrSource { name: "/tmp/secret.png".into(), data_url: "data:image/png;base64,YQ==".into() },
        };
        assert_eq!(validate_request(&request).unwrap_err(), "image.ocr.error.input");
    }

    #[test]
    fn vision_helper_reads_a_blank_png_when_present() {
        let helper = helper_path();
        if !helper.is_file() {
            return;
        }
        let status = recognition_status();
        assert!(status.available, "{:?}", status.reason);
        assert_eq!(status.engine.as_deref(), Some("apple-vision"));
        assert!(status.languages.iter().any(|language| language == "en-US"));
        let image = image::RgbaImage::from_pixel(80, 40, image::Rgba([255, 255, 255, 255]));
        let mut png = Cursor::new(Vec::new());
        image::DynamicImage::ImageRgba8(image).write_to(&mut png, ImageFormat::Png).expect("png");
        let data_url = format!("data:image/png;base64,{}", STANDARD.encode(png.get_ref()));
        let result = recognize(OcrRequest {
            request_id: "blank-image".into(),
            language: "auto".into(),
            source: OcrSource { name: "blank.png".into(), data_url },
        }).expect("recognize");
        assert_eq!(result.width, 80);
        assert_eq!(result.height, 40);
        assert!(result.text.is_empty());
    }
}
