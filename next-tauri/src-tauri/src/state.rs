use std::{collections::HashMap, sync::Mutex};

use crate::contracts::tool_webview::{
    ManagedToolId, ToolSessionReport, ToolWebviewBounds, ToolWebviewPlacement, ToolWebviewSnapshot,
};

#[derive(Default)]
pub struct ToolWebviewManager {
    inner: Mutex<HashMap<ManagedToolId, ToolWebviewRuntime>>,
}

#[derive(Debug, Default)]
struct ToolWebviewRuntime {
    exists: bool,
    visible: bool,
    placement: ToolWebviewPlacement,
    bounds: Option<ToolWebviewBounds>,
    reparent_operations: u32,
    page_loads: u32,
    session_id: Option<String>,
    state_revision: u64,
    state_digest: String,
    state_summary: String,
    last_stress_cycles: u32,
    last_stress_passed: Option<bool>,
    stress_expected_page_loads: u32,
    stress_expected_session_id: Option<String>,
    stress_expected_state_digest: String,
}

impl ToolWebviewManager {
    pub fn begin_open(&self, tool_id: ManagedToolId, bounds: ToolWebviewBounds) {
        self.inner
            .lock()
            .expect("tool WebView state poisoned")
            .insert(
                tool_id,
                ToolWebviewRuntime {
                    exists: true,
                    visible: true,
                    placement: ToolWebviewPlacement::Docked,
                    bounds: Some(bounds),
                    ..Default::default()
                },
            );
    }

    pub fn mark_open_failed(&self, tool_id: ManagedToolId) {
        self.inner
            .lock()
            .expect("tool WebView state poisoned")
            .remove(&tool_id);
    }

    pub fn mark_docked(&self, tool_id: ManagedToolId, bounds: ToolWebviewBounds) {
        let mut sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.entry(tool_id).or_default();
        state.exists = true;
        state.visible = true;
        state.placement = ToolWebviewPlacement::Docked;
        state.bounds = Some(bounds);
    }

    pub fn mark_detached(&self, tool_id: ManagedToolId) {
        let mut sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.entry(tool_id).or_default();
        state.exists = true;
        state.visible = true;
        state.placement = ToolWebviewPlacement::Detached;
    }

    pub fn mark_visible(&self, tool_id: ManagedToolId, visible: bool) {
        self.inner
            .lock()
            .expect("tool WebView state poisoned")
            .entry(tool_id)
            .or_default()
            .visible = visible;
    }

    pub fn update_bounds(&self, tool_id: ManagedToolId, bounds: ToolWebviewBounds) {
        self.inner
            .lock()
            .expect("tool WebView state poisoned")
            .entry(tool_id)
            .or_default()
            .bounds = Some(bounds);
    }

    pub fn mark_closed(&self, tool_id: ManagedToolId) {
        self.inner
            .lock()
            .expect("tool WebView state poisoned")
            .remove(&tool_id);
    }

    pub fn record_reparent_operations(&self, tool_id: ManagedToolId, operations: u32) {
        let mut sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.entry(tool_id).or_default();
        state.reparent_operations = state.reparent_operations.saturating_add(operations);
    }

    pub fn record_page_load(&self, tool_id: ManagedToolId) {
        let mut sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.entry(tool_id).or_default();
        state.page_loads = state.page_loads.saturating_add(1);
        if state.last_stress_cycles > 0 && state.page_loads > state.stress_expected_page_loads {
            state.last_stress_passed = Some(false);
        }
    }

    pub fn report_session(&self, tool_id: ManagedToolId, report: ToolSessionReport) {
        let mut sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.entry(tool_id).or_default();

        if state.session_id.as_deref() == Some(report.session_id.as_str())
            && report.state_revision < state.state_revision
        {
            return;
        }
        if let Some(expected) = &state.stress_expected_session_id {
            if expected != &report.session_id
                || state.stress_expected_state_digest != report.state_digest
            {
                state.last_stress_passed = Some(false);
            }
        }

        state.session_id = Some(report.session_id);
        state.state_revision = report.state_revision;
        state.state_digest = report.state_digest;
        state.state_summary = report.state_summary;
    }

    pub fn finish_stress(
        &self,
        tool_id: ManagedToolId,
        cycles: u32,
        expected_page_loads: u32,
        expected_session_id: Option<String>,
        expected_state_digest: String,
    ) {
        let mut sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.entry(tool_id).or_default();
        let passed = state.page_loads == expected_page_loads
            && state.session_id == expected_session_id
            && state.state_digest == expected_state_digest
            && expected_session_id.is_some();
        state.last_stress_cycles = cycles;
        state.last_stress_passed = Some(passed);
        state.stress_expected_page_loads = expected_page_loads;
        state.stress_expected_session_id = expected_session_id;
        state.stress_expected_state_digest = expected_state_digest;
    }

    pub fn snapshot(&self, tool_id: ManagedToolId) -> ToolWebviewSnapshot {
        let sessions = self.inner.lock().expect("tool WebView state poisoned");
        let state = sessions.get(&tool_id);
        ToolWebviewSnapshot {
            tool_id,
            exists: state.is_some_and(|state| state.exists),
            visible: state.is_some_and(|state| state.visible),
            placement: state.map_or(ToolWebviewPlacement::Closed, |state| state.placement),
            reparent_operations: state.map_or(0, |state| state.reparent_operations),
            page_loads: state.map_or(0, |state| state.page_loads),
            session_id: state.and_then(|state| state.session_id.clone()),
            state_revision: state.map_or(0, |state| state.state_revision),
            state_digest: state.map_or_else(String::new, |state| state.state_digest.clone()),
            state_summary: state.map_or_else(String::new, |state| state.state_summary.clone()),
            last_stress_cycles: state.map_or(0, |state| state.last_stress_cycles),
            last_stress_passed: state.and_then(|state| state.last_stress_passed),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn bounds() -> ToolWebviewBounds {
        ToolWebviewBounds {
            x: 300.0,
            y: 120.0,
            width: 800.0,
            height: 600.0,
        }
    }

    fn report(session_id: &str, revision: u64, digest: &str) -> ToolSessionReport {
        ToolSessionReport {
            session_id: session_id.into(),
            state_revision: revision,
            state_digest: digest.into(),
            state_summary: format!("revision {revision}"),
        }
    }

    #[test]
    fn tracks_calculator_state_across_successful_reparent_stress() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(ManagedToolId::Calculator, bounds());
        manager.record_page_load(ManagedToolId::Calculator);
        manager.report_session(
            ManagedToolId::Calculator,
            report("calculator-a", 9, "9*9=81"),
        );
        manager.record_reparent_operations(ManagedToolId::Calculator, 200);
        manager.finish_stress(
            ManagedToolId::Calculator,
            100,
            1,
            Some("calculator-a".into()),
            "9*9=81".into(),
        );

        let snapshot = manager.snapshot(ManagedToolId::Calculator);
        assert_eq!(snapshot.reparent_operations, 200);
        assert_eq!(snapshot.state_revision, 9);
        assert_eq!(snapshot.state_digest, "9*9=81");
        assert_eq!(snapshot.last_stress_passed, Some(true));
    }

    #[test]
    fn isolates_sessions_by_tool_id() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(ManagedToolId::Calculator, bounds());
        manager.begin_open(ManagedToolId::Color, bounds());
        manager.begin_open(ManagedToolId::Config, bounds());
        manager.begin_open(ManagedToolId::Cron, bounds());
        manager.begin_open(ManagedToolId::Crypto, bounds());
        manager.begin_open(ManagedToolId::Host, bounds());
        manager.begin_open(ManagedToolId::Http, bounds());
        manager.begin_open(ManagedToolId::Image, bounds());
        manager.begin_open(ManagedToolId::Encode, bounds());
        manager.begin_open(ManagedToolId::EditorLab, bounds());
        manager.begin_open(ManagedToolId::Json, bounds());
        manager.begin_open(ManagedToolId::MessageBoard, bounds());
        manager.begin_open(ManagedToolId::Network, bounds());
        manager.begin_open(ManagedToolId::Pdf, bounds());
        manager.begin_open(ManagedToolId::Protobuf, bounds());
        manager.begin_open(ManagedToolId::QuickNote, bounds());
        manager.begin_open(ManagedToolId::Qrcode, bounds());
        manager.begin_open(ManagedToolId::Reformat, bounds());
        manager.begin_open(ManagedToolId::Regex, bounds());
        manager.begin_open(ManagedToolId::Runtime, bounds());
        manager.begin_open(ManagedToolId::Timestamp, bounds());
        manager.begin_open(ManagedToolId::TextDiff, bounds());
        manager.begin_open(ManagedToolId::Translation, bounds());
        manager.begin_open(ManagedToolId::Ua, bounds());
        manager.begin_open(ManagedToolId::Variables, bounds());
        manager.begin_open(ManagedToolId::System, bounds());
        manager.begin_open(ManagedToolId::WebviewProbe, bounds());
        manager.report_session(
            ManagedToolId::Calculator,
            report("calculator-a", 2, "calculator"),
        );
        manager.report_session(ManagedToolId::Color, report("color-a", 3, "color"));
        manager.report_session(ManagedToolId::Config, report("config-a", 3, "config"));
        manager.report_session(ManagedToolId::Cron, report("cron-a", 4, "cron"));
        manager.report_session(ManagedToolId::Crypto, report("crypto-a", 5, "crypto"));
        manager.report_session(ManagedToolId::Host, report("host-a", 4, "host"));
        manager.report_session(ManagedToolId::Http, report("http-a", 5, "http"));
        manager.report_session(ManagedToolId::Image, report("image-a", 7, "image"));
        manager.report_session(ManagedToolId::Encode, report("encode-a", 3, "encode"));
        manager.report_session(ManagedToolId::EditorLab, report("editor-a", 5, "editor"));
        manager.report_session(ManagedToolId::Json, report("json-a", 6, "json"));
        manager.report_session(
            ManagedToolId::MessageBoard,
            report("message-board-a", 7, "message-board"),
        );
        manager.report_session(ManagedToolId::Network, report("network-a", 8, "network"));
        manager.report_session(ManagedToolId::Pdf, report("pdf-a", 6, "pdf"));
        manager.report_session(ManagedToolId::Protobuf, report("protobuf-a", 7, "protobuf"));
        manager.report_session(
            ManagedToolId::QuickNote,
            report("quick-note-a", 8, "quick-note"),
        );
        manager.report_session(ManagedToolId::Qrcode, report("qrcode-a", 8, "qrcode"));
        manager.report_session(ManagedToolId::Reformat, report("reformat-a", 7, "reformat"));
        manager.report_session(ManagedToolId::Regex, report("regex-a", 8, "regex"));
        manager.report_session(ManagedToolId::Runtime, report("runtime-a", 6, "runtime"));
        manager.report_session(
            ManagedToolId::Timestamp,
            report("timestamp-a", 9, "timestamp"),
        );
        manager.report_session(ManagedToolId::TextDiff, report("diff-a", 7, "diff"));
        manager.report_session(
            ManagedToolId::Translation,
            report("translation-a", 9, "translation"),
        );
        manager.report_session(ManagedToolId::Ua, report("ua-a", 10, "ua"));
        manager.report_session(
            ManagedToolId::Variables,
            report("variables-a", 11, "variables"),
        );
        manager.report_session(ManagedToolId::System, report("system-a", 12, "system"));
        manager.report_session(ManagedToolId::WebviewProbe, report("probe-a", 8, "probe"));

        assert_eq!(
            manager.snapshot(ManagedToolId::Calculator).state_digest,
            "calculator"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Color).state_digest, "color");
        assert_eq!(
            manager.snapshot(ManagedToolId::Config).state_digest,
            "config"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Cron).state_digest, "cron");
        assert_eq!(
            manager.snapshot(ManagedToolId::Crypto).state_digest,
            "crypto"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Host).state_digest, "host");
        assert_eq!(manager.snapshot(ManagedToolId::Http).state_digest, "http");
        assert_eq!(manager.snapshot(ManagedToolId::Image).state_digest, "image");
        assert_eq!(
            manager.snapshot(ManagedToolId::Encode).state_digest,
            "encode"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::EditorLab).state_digest,
            "editor"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Json).state_digest, "json");
        assert_eq!(
            manager.snapshot(ManagedToolId::MessageBoard).state_digest,
            "message-board"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::Network).state_digest,
            "network"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Pdf).state_digest, "pdf");
        assert_eq!(
            manager.snapshot(ManagedToolId::Protobuf).state_digest,
            "protobuf"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::QuickNote).state_digest,
            "quick-note"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::Qrcode).state_digest,
            "qrcode"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::Reformat).state_digest,
            "reformat"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Regex).state_digest, "regex");
        assert_eq!(
            manager.snapshot(ManagedToolId::Runtime).state_digest,
            "runtime"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::Timestamp).state_digest,
            "timestamp"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::TextDiff).state_digest,
            "diff"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::Translation).state_digest,
            "translation"
        );
        assert_eq!(manager.snapshot(ManagedToolId::Ua).state_digest, "ua");
        assert_eq!(
            manager.snapshot(ManagedToolId::Variables).state_digest,
            "variables"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::System).state_digest,
            "system"
        );
        assert_eq!(
            manager.snapshot(ManagedToolId::WebviewProbe).state_digest,
            "probe"
        );
    }

    #[test]
    fn ignores_stale_reports_from_the_same_session() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(ManagedToolId::Calculator, bounds());
        manager.report_session(ManagedToolId::Calculator, report("calculator-a", 3, "new"));
        manager.report_session(
            ManagedToolId::Calculator,
            report("calculator-a", 2, "stale"),
        );

        assert_eq!(
            manager.snapshot(ManagedToolId::Calculator).state_digest,
            "new"
        );
    }

    #[test]
    fn marks_stress_failed_when_the_page_loads_again() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(ManagedToolId::Calculator, bounds());
        manager.record_page_load(ManagedToolId::Calculator);
        manager.report_session(
            ManagedToolId::Calculator,
            report("calculator-a", 1, "before"),
        );
        manager.finish_stress(
            ManagedToolId::Calculator,
            100,
            1,
            Some("calculator-a".into()),
            "before".into(),
        );
        manager.record_page_load(ManagedToolId::Calculator);
        manager.report_session(
            ManagedToolId::Calculator,
            report("calculator-b", 1, "after"),
        );

        assert_eq!(
            manager
                .snapshot(ManagedToolId::Calculator)
                .last_stress_passed,
            Some(false)
        );
    }

    #[test]
    fn closing_a_tool_clears_its_session_metadata() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(ManagedToolId::Calculator, bounds());
        manager.record_page_load(ManagedToolId::Calculator);
        manager.report_session(
            ManagedToolId::Calculator,
            report("calculator-a", 4, "2*(3+4)=14"),
        );

        manager.mark_closed(ManagedToolId::Calculator);

        let snapshot = manager.snapshot(ManagedToolId::Calculator);
        assert!(!snapshot.exists);
        assert_eq!(snapshot.placement, ToolWebviewPlacement::Closed);
        assert_eq!(snapshot.page_loads, 0);
        assert_eq!(snapshot.session_id, None);
        assert!(snapshot.state_digest.is_empty());
    }
}
