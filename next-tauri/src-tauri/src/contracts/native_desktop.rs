use serde::{Deserialize, Serialize};

use super::image::ImageAssetSummary;

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ScreenCaptureResult {
    pub assets: Vec<ImageAssetSummary>,
    pub monitor_count: usize,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ScreenColorSample {
    pub hex: String,
    pub red: u8,
    pub green: u8,
    pub blue: u8,
    pub x: i32,
    pub y: i32,
}

#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DisplaySleepStatus {
    pub active: bool,
    pub owned: bool,
}
