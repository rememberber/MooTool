use serde::Serialize;

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub enum ProductUpdateStatus {
    Available,
    UpToDate,
    Inactive,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ProductUpdateCheck {
    pub status: ProductUpdateStatus,
    pub current_version: String,
    pub latest_version: Option<String>,
    pub release_notes: Option<String>,
    pub published_at: Option<String>,
    pub release_url: Option<String>,
}

impl ProductUpdateCheck {
    pub fn inactive(current_version: String) -> Self {
        Self {
            status: ProductUpdateStatus::Inactive,
            current_version,
            latest_version: None,
            release_notes: None,
            published_at: None,
            release_url: None,
        }
    }

    pub fn up_to_date(current_version: String) -> Self {
        Self {
            status: ProductUpdateStatus::UpToDate,
            current_version,
            latest_version: None,
            release_notes: None,
            published_at: None,
            release_url: None,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase", tag = "event", content = "data")]
pub enum ProductUpdateEvent {
    Started,
    Progress {
        chunk_length: usize,
        downloaded_bytes: u64,
        content_length: Option<u64>,
    },
    Finished,
    Cancelled,
    Installed,
}
