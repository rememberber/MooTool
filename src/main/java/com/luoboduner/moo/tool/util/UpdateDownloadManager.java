package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.luoboduner.moo.tool.ui.UiConsts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 发现新版本后的静默下载与安装状态管理（对齐 Next 版 autoDownloadUpdates）。
 */
@Slf4j
public class UpdateDownloadManager {

    public enum Status {
        IDLE,
        DOWNLOADING,
        READY,
        ERROR
    }

    private static final UpdateDownloadManager INSTANCE = new UpdateDownloadManager();

    private final List<Consumer<UpdateDownloadManager>> listeners = new CopyOnWriteArrayList<>();

    @Getter
    private volatile Status status = Status.IDLE;

    @Getter
    private volatile String version;

    @Getter
    private volatile File downloadedFile;

    @Getter
    private volatile int percent;

    @Getter
    private volatile String releaseNotesHtml;

    private volatile String downloadingVersion;

    public static UpdateDownloadManager getInstance() {
        return INSTANCE;
    }

    private UpdateDownloadManager() {
    }

    public void addListener(Consumer<UpdateDownloadManager> listener) {
        listeners.add(listener);
        listener.accept(this);
    }

    public void removeListener(Consumer<UpdateDownloadManager> listener) {
        listeners.remove(listener);
    }

    /**
     * 若尚未就绪则后台静默下载安装包；已就绪则仅通知监听者刷新提示。
     */
    public void startSilentDownload(String newVersion, String releaseNotesHtml) {
        if (StringUtils.isBlank(newVersion)) {
            return;
        }
        synchronized (this) {
            if (status == Status.READY
                    && newVersion.equals(version)
                    && downloadedFile != null
                    && downloadedFile.exists()) {
                if (StringUtils.isNotBlank(releaseNotesHtml)) {
                    this.releaseNotesHtml = releaseNotesHtml;
                }
                notifyListeners();
                return;
            }
            if (status == Status.DOWNLOADING && newVersion.equals(downloadingVersion)) {
                if (StringUtils.isNotBlank(releaseNotesHtml)) {
                    this.releaseNotesHtml = releaseNotesHtml;
                }
                return;
            }
            status = Status.DOWNLOADING;
            downloadingVersion = newVersion;
            version = newVersion;
            this.releaseNotesHtml = releaseNotesHtml;
            percent = 0;
            downloadedFile = null;
        }
        notifyListeners();
        ThreadUtil.execute(() -> download(newVersion));
    }

    public void installAndExit() {
        File file = downloadedFile;
        if (file == null || !file.exists()) {
            throw new IllegalStateException("update package missing");
        }
        try {
            Desktop.getDesktop().open(file);
            System.exit(0);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void download(String newVersion) {
        try {
            String fileUrl = resolveDownloadUrl();
            if (StringUtils.isBlank(fileUrl)) {
                throw new IllegalStateException("empty download url");
            }
            String fileName = FileUtil.getName(fileUrl);
            File pendingDir = pendingDir();
            if (!pendingDir.exists() && !pendingDir.mkdirs()) {
                throw new IllegalStateException("cannot create pending-updates dir");
            }
            FileUtil.clean(pendingDir);
            File target = FileUtil.file(pendingDir, fileName);

            HttpUtil.downloadFile(fileUrl, FileUtil.touch(target), new StreamProgress() {
                @Override
                public void start() {
                    percent = 0;
                    notifyListeners();
                }

                @Override
                public void progress(long totalSize, long progressSize) {
                    if (totalSize > 0) {
                        percent = (int) Math.min(100, progressSize * 100 / totalSize);
                    }
                    notifyListeners();
                }

                @Override
                public void finish() {
                    // status updated after downloadFile returns
                }
            });

            synchronized (this) {
                if (!newVersion.equals(downloadingVersion)) {
                    return;
                }
                downloadedFile = target;
                version = newVersion;
                percent = 100;
                status = Status.READY;
            }
            log.info("静默下载更新完成: {} -> {}", newVersion, target.getAbsolutePath());
            notifyListeners();
        } catch (Exception e) {
            log.error("静默下载更新失败: {}", newVersion, e);
            synchronized (this) {
                if (newVersion.equals(downloadingVersion)) {
                    status = Status.ERROR;
                    percent = 0;
                }
            }
            notifyListeners();
        }
    }

    static String resolveDownloadUrl() {
        String downloadLinkInfo = HttpUtil.get(UiConsts.DOWNLOAD_LINK_INFO_URL);
        if (StringUtils.isEmpty(downloadLinkInfo) || downloadLinkInfo.contains("404: Not Found")) {
            throw new IllegalStateException("download links unavailable");
        }
        DocumentContext parse = JsonPath.parse(downloadLinkInfo);
        return DownloadLinkSelector.select(parse);
    }

    static File pendingDir() {
        return new File(SystemUtil.CONFIG_HOME + File.separator + "pending-updates");
    }

    private void notifyListeners() {
        if (SwingUtilities.isEventDispatchThread()) {
            fireListeners();
        } else {
            SwingUtilities.invokeLater(this::fireListeners);
        }
    }

    private void fireListeners() {
        for (Consumer<UpdateDownloadManager> listener : listeners) {
            try {
                listener.accept(this);
            } catch (Exception e) {
                log.warn("更新下载状态监听回调异常", e);
            }
        }
    }
}
