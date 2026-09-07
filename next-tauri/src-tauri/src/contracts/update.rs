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
#[serde(
    rename_all = "camelCase",
    rename_all_fields = "camelCase",
    tag = "event",
    content = "data"
)]
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

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn progress_event_serializes_the_frontend_field_names() {
        for content_length in [Some(16_384), None] {
            let event = ProductUpdateEvent::Progress {
                chunk_length: 1_024,
                downloaded_bytes: 4_096,
                content_length,
            };
            assert_eq!(
                serde_json::to_value(event).expect("progress event"),
                json!({
                    "event": "progress",
                    "data": {
                        "chunkLength": 1_024,
                        "downloadedBytes": 4_096,
                        "contentLength": content_length
                    }
                })
            );
        }
    }

    #[test]
    fn lifecycle_events_keep_the_frontend_tags_without_payloads() {
        for (event, tag) in [
            (ProductUpdateEvent::Started, "started"),
            (ProductUpdateEvent::Finished, "finished"),
            (ProductUpdateEvent::Cancelled, "cancelled"),
            (ProductUpdateEvent::Installed, "installed"),
        ] {
            assert_eq!(
                serde_json::to_value(event).expect("lifecycle event"),
                json!({ "event": tag })
            );
        }
    }
}
