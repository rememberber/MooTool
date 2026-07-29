use serde::Serialize;

pub const PRODUCT_ID: &str = "next-tauri";
pub const PRODUCT_NAME: &str = "MooTool Next Tauri";

#[derive(Debug, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RuntimeInfo {
    pub product_id: &'static str,
    pub product_name: &'static str,
    pub version: String,
    pub platform: &'static str,
    pub architecture: &'static str,
    pub runtime: &'static str,
}

impl RuntimeInfo {
    pub fn collect(version: impl Into<String>) -> Self {
        Self {
            product_id: PRODUCT_ID,
            product_name: PRODUCT_NAME,
            version: version.into(),
            platform: std::env::consts::OS,
            architecture: std::env::consts::ARCH,
            runtime: "tauri",
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn keeps_the_tauri_product_identity_independent() {
        let info = RuntimeInfo::collect("0.1.0");

        assert_eq!(info.product_id, "next-tauri");
        assert_eq!(info.product_name, "MooTool Next Tauri");
        assert_eq!(info.version, "0.1.0");
        assert_eq!(info.runtime, "tauri");
    }
}
