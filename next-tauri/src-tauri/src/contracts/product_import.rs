use serde::{Deserialize, Serialize};

use super::{
    image::ImageAssetSummary,
    local_data::{HostProfile, OperationHistory, QuickNote, TranslationHistory, TranslationWord},
};

#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum ProductImportSource {
    Java,
    NextElectron,
}

impl ProductImportSource {
    pub fn id(self) -> &'static str {
        match self {
            Self::Java => "java",
            Self::NextElectron => "next-electron",
        }
    }
}

#[derive(Clone, Debug, Default, Deserialize, PartialEq, Serialize)]
#[serde(default, rename_all = "camelCase")]
pub struct ProductImportCounts {
    pub quick_notes: usize,
    pub host_profiles: usize,
    pub translation_words: usize,
    pub translation_history: usize,
    pub operation_history: usize,
    pub vault_files: usize,
    pub images: usize,
    pub settings: usize,
}

impl ProductImportCounts {
    pub fn total(&self) -> usize {
        self.quick_notes
            + self.host_profiles
            + self.translation_words
            + self.translation_history
            + self.operation_history
            + self.vault_files
            + self.images
            + self.settings
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProductImportPreview {
    pub source_product: ProductImportSource,
    pub source_directory: String,
    pub fingerprint: String,
    pub database_found: bool,
    pub settings_found: bool,
    pub already_imported: bool,
    pub items: ProductImportCounts,
    pub total_items: usize,
    pub warnings: Vec<String>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProductImportResult {
    pub preview: ProductImportPreview,
    pub imported: ProductImportCounts,
    pub skipped: ProductImportCounts,
    pub backup_path: String,
    pub report_path: String,
    pub imported_vault_path: Option<String>,
}

#[derive(Debug, Default)]
pub struct ProductImportRecords {
    pub quick_notes: Vec<QuickNote>,
    pub host_profiles: Vec<HostProfile>,
    pub translation_words: Vec<TranslationWord>,
    pub translation_history: Vec<TranslationHistory>,
    pub operation_history: Vec<OperationHistory>,
    pub images: Vec<ImageAssetSummary>,
}

impl ProductImportRecords {
    pub fn counts(&self) -> ProductImportCounts {
        ProductImportCounts {
            quick_notes: self.quick_notes.len(),
            host_profiles: self.host_profiles.len(),
            translation_words: self.translation_words.len(),
            translation_history: self.translation_history.len(),
            operation_history: self.operation_history.len(),
            images: self.images.len(),
            ..ProductImportCounts::default()
        }
    }
}
