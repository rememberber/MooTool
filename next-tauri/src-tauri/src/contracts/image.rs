use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageAssetInput {
    pub name: String,
    pub data_url: String,
    pub width: u32,
    pub height: u32,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageAssetSummary {
    pub name: String,
    pub mime_type: String,
    pub width: u32,
    pub height: u32,
    pub size_bytes: usize,
    pub updated_at: i64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageAsset {
    #[serde(flatten)]
    pub summary: ImageAssetSummary,
    pub data_url: String,
}

#[derive(Clone, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageVectorizeOptions {
    pub preset: String,
    pub color_count: u8,
    pub detail: String,
    pub filter_speckle: usize,
}
