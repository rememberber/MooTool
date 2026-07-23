package com.luoboduner.moo.tool.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 监听 JSON Vault 目录外部变更，防抖后刷新列表与 Git 状态。
 */
@Slf4j
public final class JsonBeautyVaultWatcher {

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Thread watchThread;

    private JsonBeautyVaultWatcher() {
    }

    public static void start() {
        stop();
        running.set(true);
        watchThread = new Thread(JsonBeautyVaultWatcher::runLoop, "json-beauty-vault-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public static void stop() {
        running.set(false);
        JsonBeautyVaultRefreshCoordinator.cancelPendingExternalRefresh();
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }

    public static void restart() {
        start();
    }

    private static void runLoop() {
        while (running.get()) {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Path vaultPath = JsonBeautyVaultUtil.getVaultDir().toPath();
                Set<Path> registered = new HashSet<>();
                registerAllDirs(vaultPath, watchService, registered);

                while (running.get()) {
                    WatchKey key = watchService.poll(2, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                    boolean changed = false;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            changed = true;
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path name = pathEvent.context();
                        if (shouldIgnore(name.toString())) {
                            continue;
                        }
                        changed = true;
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path child = ((Path) key.watchable()).resolve(name);
                            if (Files.isDirectory(child)) {
                                registerAllDirs(child, watchService, registered);
                            }
                        }
                    }
                    if (!key.reset()) {
                        break;
                    }
                    if (changed) {
                        JsonBeautyVaultRefreshCoordinator.requestDebouncedExternalRefresh();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.debug("JSON vault watcher error: {}", e.getMessage());
                sleepQuietly(3000);
            }
        }
    }

    private static void registerAllDirs(Path root, WatchService watchService, Set<Path> registered) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        Files.walk(root)
                .filter(Files::isDirectory)
                .filter(path -> !containsGitDir(path))
                .forEach(path -> {
                    if (registered.add(path)) {
                        try {
                            path.register(watchService,
                                    StandardWatchEventKinds.ENTRY_CREATE,
                                    StandardWatchEventKinds.ENTRY_DELETE,
                                    StandardWatchEventKinds.ENTRY_MODIFY);
                        } catch (IOException ex) {
                            log.debug("Cannot watch {}: {}", path, ex.getMessage());
                        }
                    }
                });
    }

    private static boolean containsGitDir(Path path) {
        for (Path part : path) {
            if (".git".equals(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldIgnore(String name) {
        return ".git".equals(name) || name.startsWith(".");
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
