use std::sync::Mutex;

use crate::contracts::tool_webview::{
    ToolProbeReport, ToolWebviewBounds, ToolWebviewPlacement, ToolWebviewSnapshot,
};

#[derive(Default)]
pub struct ToolWebviewManager {
    inner: Mutex<ToolWebviewRuntime>,
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
    counter: i64,
    draft: String,
    last_stress_cycles: u32,
    last_stress_passed: Option<bool>,
    stress_expected_page_loads: u32,
    stress_expected_session_id: Option<String>,
}

impl ToolWebviewManager {
    pub fn begin_open(&self, bounds: ToolWebviewBounds) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        *state = ToolWebviewRuntime {
            exists: true,
            visible: true,
            placement: ToolWebviewPlacement::Docked,
            bounds: Some(bounds),
            draft: "state survives reparent".into(),
            ..Default::default()
        };
    }

    pub fn mark_open_failed(&self) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        *state = ToolWebviewRuntime::default();
    }

    pub fn mark_docked(&self, bounds: ToolWebviewBounds) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        state.exists = true;
        state.visible = true;
        state.placement = ToolWebviewPlacement::Docked;
        state.bounds = Some(bounds);
    }

    pub fn mark_detached(&self) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        state.exists = true;
        state.visible = true;
        state.placement = ToolWebviewPlacement::Detached;
    }

    pub fn mark_visible(&self, visible: bool) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        state.visible = visible;
    }

    pub fn update_bounds(&self, bounds: ToolWebviewBounds) {
        self.inner
            .lock()
            .expect("tool WebView state poisoned")
            .bounds = Some(bounds);
    }

    pub fn mark_closed(&self) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        state.exists = false;
        state.visible = false;
        state.placement = ToolWebviewPlacement::Closed;
        state.bounds = None;
    }

    pub fn record_reparent_operations(&self, operations: u32) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        state.reparent_operations = state.reparent_operations.saturating_add(operations);
    }

    pub fn record_page_load(&self) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        state.page_loads = state.page_loads.saturating_add(1);
        if state.last_stress_cycles > 0 && state.page_loads > state.stress_expected_page_loads {
            state.last_stress_passed = Some(false);
        }
    }

    pub fn report_probe(&self, report: ToolProbeReport) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        if let Some(expected) = &state.stress_expected_session_id {
            if expected != &report.session_id {
                state.last_stress_passed = Some(false);
            }
        }
        state.session_id = Some(report.session_id);
        state.counter = report.counter;
        state.draft = report.draft;
    }

    pub fn finish_stress(
        &self,
        cycles: u32,
        expected_page_loads: u32,
        expected_session_id: Option<String>,
    ) {
        let mut state = self.inner.lock().expect("tool WebView state poisoned");
        let passed = state.page_loads == expected_page_loads
            && state.session_id == expected_session_id
            && expected_session_id.is_some();
        state.last_stress_cycles = cycles;
        state.last_stress_passed = Some(passed);
        state.stress_expected_page_loads = expected_page_loads;
        state.stress_expected_session_id = expected_session_id;
    }

    pub fn snapshot(&self) -> ToolWebviewSnapshot {
        let state = self.inner.lock().expect("tool WebView state poisoned");
        ToolWebviewSnapshot {
            exists: state.exists,
            visible: state.visible,
            placement: state.placement,
            reparent_operations: state.reparent_operations,
            page_loads: state.page_loads,
            session_id: state.session_id.clone(),
            counter: state.counter,
            draft: state.draft.clone(),
            last_stress_cycles: state.last_stress_cycles,
            last_stress_passed: state.last_stress_passed,
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

    #[test]
    fn tracks_probe_state_across_successful_reparent_stress() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(bounds());
        manager.record_page_load();
        manager.report_probe(ToolProbeReport {
            session_id: "session-a".into(),
            counter: 9,
            draft: "preserve me".into(),
        });
        manager.record_reparent_operations(200);
        manager.finish_stress(100, 1, Some("session-a".into()));

        let snapshot = manager.snapshot();
        assert_eq!(snapshot.reparent_operations, 200);
        assert_eq!(snapshot.counter, 9);
        assert_eq!(snapshot.draft, "preserve me");
        assert_eq!(snapshot.last_stress_passed, Some(true));
    }

    #[test]
    fn marks_stress_failed_when_the_page_loads_again() {
        let manager = ToolWebviewManager::default();
        manager.begin_open(bounds());
        manager.record_page_load();
        manager.report_probe(ToolProbeReport {
            session_id: "session-a".into(),
            counter: 1,
            draft: "before".into(),
        });
        manager.finish_stress(100, 1, Some("session-a".into()));
        manager.record_page_load();
        manager.report_probe(ToolProbeReport {
            session_id: "session-b".into(),
            counter: 0,
            draft: "after".into(),
        });

        assert_eq!(manager.snapshot().last_stress_passed, Some(false));
    }
}
