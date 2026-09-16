package com.rememberber.mootool.next.compose.features.http

import com.rememberber.mootool.next.compose.sessions.HttpSession
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume

class HttpFindShortcutPolicyTest {
    @Test
    fun shell_find_blocked_when_request_body_editor_focused() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)
        val session = HttpSession()
        val frame = JFrame("next-compose-http-find-policy")
        frame.contentPane.add(session.bodyEditor.scrollPane)
        frame.setSize(480, 320)
        try {
            SwingUtilities.invokeAndWait {
                frame.isVisible = true
                frame.toFront()
                frame.requestFocus()
                session.bodyEditor.area.requestFocusInWindow()
            }
            var focusOwner = false
            for (attempt in 0 until 24) {
                SwingUtilities.invokeAndWait {
                    focusOwner = session.bodyEditor.area.isFocusOwner
                    if (!focusOwner) session.bodyEditor.area.requestFocusInWindow()
                }
                if (focusOwner) break
                Thread.sleep(50)
            }
            Assume.assumeTrue("Body RSTA did not take focus in this environment", focusOwner)
            SwingUtilities.invokeAndWait {
                assertFalse(HttpFindShortcutPolicy.shouldOpenResponseFindFromShell(session))
            }
        } finally {
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }

    @Test
    fun shell_find_blocked_when_request_pane_compose_focused() {
        val session = HttpSession()
        session.requestPaneComposeFocused = true
        assertFalse(HttpFindShortcutPolicy.shouldOpenResponseFindFromShell(session))
    }

    @Test
    fun shell_find_allowed_when_body_editor_not_focused() {
        val session = HttpSession()
        SwingUtilities.invokeAndWait {
            assertTrue(HttpFindShortcutPolicy.shouldOpenResponseFindFromShell(session))
        }
    }
}
