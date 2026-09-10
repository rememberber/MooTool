package com.rememberber.mootool.nextfx.app;

import com.rememberber.mootool.nextfx.application.AppExecutors;
import com.rememberber.mootool.nextfx.application.ToolSession;
import com.rememberber.mootool.nextfx.application.ToolWindowCoordinator;
import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.features.encode.EncodeView;
import com.rememberber.mootool.nextfx.features.json.JsonView;
import com.rememberber.mootool.nextfx.features.regex.RegexView;
import com.rememberber.mootool.nextfx.infrastructure.AppPaths;
import com.rememberber.mootool.nextfx.infrastructure.SettingsStore;
import com.rememberber.mootool.nextfx.infrastructure.SqliteDatabase;
import com.rememberber.mootool.nextfx.ui.components.ComponentGallery;
import com.rememberber.mootool.nextfx.ui.i18n.Translator;
import com.rememberber.mootool.nextfx.ui.shell.HomeView;
import com.rememberber.mootool.nextfx.ui.shell.PlaceholderView;
import com.rememberber.mootool.nextfx.ui.theme.ThemeSnapshot;
import javafx.scene.Node;

import java.util.EnumMap;
import java.util.Map;

public final class AppServices implements AutoCloseable {

    private final ProductIdentity identity;
    private final AppPaths paths;
    private final AppExecutors executors;
    private final SqliteDatabase database;
    private final SettingsStore settings;
    private final Translator translator;
    private final Map<ToolId, ToolSession> sessions = new EnumMap<>(ToolId.class);
    private ThemeSnapshot theme;
    private ToolWindowCoordinator windows;

    public AppServices(ProductIdentity identity, AppPaths paths) {
        this.identity = identity;
        this.paths = paths;
        this.executors = new AppExecutors();
        this.database = new SqliteDatabase(paths);
        this.database.open();
        this.settings = new SettingsStore(paths);
        this.settings.load();
        this.translator = new Translator(this.settings.current().language());
        this.theme = resolveTheme(this.settings.current());
    }

    public ProductIdentity identity() {
        return identity;
    }

    public AppPaths paths() {
        return paths;
    }

    public AppExecutors executors() {
        return executors;
    }

    public SqliteDatabase database() {
        return database;
    }

    public SettingsStore settings() {
        return settings;
    }

    public Translator translator() {
        return translator;
    }

    public ThemeSnapshot theme() {
        return theme;
    }

    public void setWindows(ToolWindowCoordinator windows) {
        this.windows = windows;
    }

    public ToolWindowCoordinator windows() {
        return windows;
    }

    public void applySettings(SettingsStore.Settings next) {
        settings.save(next);
        translator.setLanguage(next.language());
        theme = resolveTheme(next);
    }

    public ToolSession session(ToolId toolId) {
        ToolSession existing = sessions.get(toolId);
        if (existing != null) {
            return existing;
        }
        ToolSession[] holder = new ToolSession[1];
        holder[0] = new ToolSession(toolId, () -> createView(toolId, holder[0]));
        sessions.put(toolId, holder[0]);
        return holder[0];
    }

    private Node createView(ToolId toolId, ToolSession session) {
        return switch (toolId) {
            case MOOTOOL -> new HomeView(identity, translator);
            case JSON -> new JsonView(translator, executors, database, paths, session);
            case ENCODE -> new EncodeView(translator, executors, database);
            case REGEX -> new RegexView(translator, executors, database);
            default -> new PlaceholderView(ToolRegistry.require(toolId), translator);
        };
    }

    public Node gallery() {
        return new ComponentGallery(translator);
    }

    private ThemeSnapshot resolveTheme(SettingsStore.Settings current) {
        ThemeSnapshot.Appearance appearance = switch (current.theme()) {
            case LIGHT -> ThemeSnapshot.Appearance.LIGHT;
            case DARK -> ThemeSnapshot.Appearance.DARK;
            case SYSTEM -> detectSystemDark() ? ThemeSnapshot.Appearance.DARK : ThemeSnapshot.Appearance.LIGHT;
        };
        return new ThemeSnapshot(appearance, current.accent(), current.fontSize());
    }

    private static boolean detectSystemDark() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            try {
                Process process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").start();
                String output = new String(process.getInputStream().readAllBytes());
                return output.toLowerCase().contains("dark");
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void close() {
        if (windows != null) {
            windows.closeAll();
        }
        database.close();
        executors.close();
    }
}
