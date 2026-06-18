package com.luoboduner.moo.tool.util;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import de.jangassen.jfa.appkit.NSApplication;
import de.jangassen.jfa.appkit.NSMenu;
import de.jangassen.jfa.appkit.NSMenuItem;
import de.jangassen.jfa.foundation.Foundation;
import de.jangassen.jfa.foundation.ID;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;

/**
 * 向 macOS 原生应用菜单（菜单栏应用名对应菜单）插入自定义项。
 * Swing 的 JMenuBar 与 Cocoa 应用菜单是两套机制，不能靠第一个 JMenu 合并进去。
 */
@Slf4j
public final class MacApplicationMenuUtil {

    private static final String HANDLER_CLASS = "MooToolCheckUpdateMenuHandler";

    private static volatile boolean handlerRegistered;
    private static volatile boolean menuInstalled;
    private static volatile NSMenuItem checkUpdateMenuItem;

    private MacApplicationMenuUtil() {
    }

    public static void installCheckForUpdatesMenu() {
        if (!SystemUtil.isMacOs() || !Foundation.isAvailable()) {
            return;
        }
        Foundation.executeOnMainThread(true, false, MacApplicationMenuUtil::installOnAppKitThread);
    }

    public static void refreshCheckForUpdatesMenuTitle() {
        if (!menuInstalled || checkUpdateMenuItem == null) {
            return;
        }
        Foundation.executeOnMainThread(true, false, () -> {
            Foundation.NSAutoreleasePool pool = new Foundation.NSAutoreleasePool();
            try {
                checkUpdateMenuItem.setTitle(I18n.get("menu.checkUpdate"));
            } catch (Exception e) {
                log.error("Failed to refresh macOS Check for Updates menu title", e);
            } finally {
                pool.drain();
            }
        });
    }

    private static void installOnAppKitThread() {
        if (menuInstalled) {
            refreshCheckForUpdatesMenuTitle();
            return;
        }

        Foundation.NSAutoreleasePool pool = new Foundation.NSAutoreleasePool();
        try {
            NSMenu mainMenu = NSApplication.sharedApplication().mainMenu();
            if (mainMenu == null || mainMenu.numberOfItems() == 0) {
                return;
            }

            NSMenuItem appMenuHolder = mainMenu.itemAtIndex(0);
            if (appMenuHolder == null || !appMenuHolder.hasSubmenu()) {
                return;
            }

            NSMenu appMenu = appMenuHolder.submenu();
            String title = I18n.get("menu.checkUpdate");
            if (findMenuItemIndex(appMenu, title) >= 0) {
                menuInstalled = true;
                return;
            }

            NSMenuItem item = createCheckUpdateMenuItem(title);
            if (item == null) {
                return;
            }

            appMenu.insertItem(item, findInsertIndex(appMenu));
            checkUpdateMenuItem = item;
            menuInstalled = true;
        } catch (Exception e) {
            log.error("Failed to install macOS Check for Updates menu item", e);
        } finally {
            pool.drain();
        }
    }

    private static int findInsertIndex(NSMenu appMenu) {
        long count = appMenu.numberOfItems();
        for (int i = 0; i < count; i++) {
            String itemTitle = appMenu.itemAtIndex(i).title();
            if (itemTitle == null) {
                continue;
            }
            if (itemTitle.contains("Preferences")
                    || itemTitle.contains("偏好设置")
                    || itemTitle.contains("環境設定")
                    || itemTitle.contains("設定")) {
                return i + 1;
            }
        }
        return Math.min(2, (int) count);
    }

    private static int findMenuItemIndex(NSMenu menu, String title) {
        long count = menu.numberOfItems();
        for (int i = 0; i < count; i++) {
            if (title.equals(menu.itemAtIndex(i).title())) {
                return i;
            }
        }
        return -1;
    }

    private static NSMenuItem createCheckUpdateMenuItem(String title) {
        ensureHandlerRegistered();
        if (!handlerRegistered) {
            return null;
        }

        ID handler = Foundation.invoke(HANDLER_CLASS, "new");
        Pointer selector = Foundation.createSelector("checkForUpdates:");
        NSMenuItem item = NSMenuItem.alloc().initWithTitle(title, selector, "");
        item.setTarget(handler);
        return item;
    }

    private static synchronized void ensureHandlerRegistered() {
        if (handlerRegistered) {
            return;
        }

        ID handlerClass = Foundation.getObjcClass(HANDLER_CLASS);
        if (ID.NIL.equals(handlerClass)) {
            handlerClass = Foundation.allocateObjcClassPair(Foundation.getObjcClass("NSObject"), HANDLER_CLASS);
            if (ID.NIL.equals(handlerClass)) {
                return;
            }

            Callback callback = new Callback() {
                @SuppressWarnings("unused")
                public void callback(ID self, Pointer sel, ID sender) {
                    SwingUtilities.invokeLater(() -> UpgradeUtil.checkUpdate(false));
                }
            };
            if (!Foundation.addMethod(handlerClass, Foundation.createSelector("checkForUpdates:"), callback, "v@:@")) {
                log.error("Failed to register macOS checkForUpdates: callback");
                return;
            }
            Foundation.registerObjcClassPair(handlerClass);
        }
        handlerRegistered = true;
    }
}
