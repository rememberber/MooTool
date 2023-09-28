/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.jthemedetecor;

import com.jthemedetecor.util.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Used for detecting the dark theme on a Linux (GNOME/GTK) system.
 * Tested on Ubuntu.
 *
 * @author Daniel Gyorffy
 */
class GnomeThemeDetector extends OsThemeDetector {

    private static final Logger logger = LoggerFactory.getLogger(GnomeThemeDetector.class);

    private static final String MONITORING_CMD = "gsettings monitor org.gnome.desktop.interface gtk-theme";
    private static final String GET_CMD = "gsettings get org.gnome.desktop.interface gtk-theme";

    private final Set<Consumer<Boolean>> listeners = new ConcurrentHashSet<>();
    private final Pattern darkThemeNamePattern = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE);

    private volatile DetectorThread detectorThread;

    @Override
    public boolean isDark() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(GET_CMD);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String readLine = reader.readLine();
                if (readLine != null) {
                    return isDarkTheme(readLine);
                }
            }
        } catch (IOException e) {
            logger.error("Couldn't detect Linux OS theme", e);
        }
        return false;
    }

    private boolean isDarkTheme(String gtkTheme) {
        return darkThemeNamePattern.matcher(gtkTheme).matches();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public synchronized void registerListener(@NotNull Consumer<Boolean> darkThemeListener) {
        Objects.requireNonNull(darkThemeListener);
        final boolean listenerAdded = listeners.add(darkThemeListener);
        final boolean singleListener = listenerAdded && listeners.size() == 1;
        final DetectorThread currentDetectorThread = detectorThread;
        final boolean threadInterrupted = currentDetectorThread != null && currentDetectorThread.isInterrupted();

        if (singleListener || threadInterrupted) {
            final DetectorThread newDetectorThread = new DetectorThread(this);
            this.detectorThread = newDetectorThread;
            newDetectorThread.start();
        }
    }

    @Override
    public synchronized void removeListener(@Nullable Consumer<Boolean> darkThemeListener) {
        listeners.remove(darkThemeListener);
        if (listeners.isEmpty()) {
            this.detectorThread.interrupt();
            this.detectorThread = null;
        }
    }

    /**
     * Thread implementation for detecting the actually changed theme
     */
    private static final class DetectorThread extends Thread {

        private final GnomeThemeDetector detector;
        private boolean lastValue;

        DetectorThread(@NotNull GnomeThemeDetector detector) {
            this.detector = detector;
            this.lastValue = detector.isDark();
            this.setName("GTK Theme Detector Thread");
            this.setDaemon(true);
            this.setPriority(Thread.NORM_PRIORITY - 1);
        }

        @Override
        public void run() {
            try {
                Runtime runtime = Runtime.getRuntime();
                Process monitoringProcess = runtime.exec(MONITORING_CMD);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(monitoringProcess.getInputStream()))) {
                    while (!this.isInterrupted()) {
                        //Expected input = gtk-theme: '$GtkThemeName'
                        String readLine = reader.readLine();
                        String[] keyValue = readLine.split("\\s");
                        String value = keyValue[1];
                        boolean currentDetection = detector.isDarkTheme(value);
                        logger.debug("Theme changed detection, dark: {}", currentDetection);
                        if (currentDetection != lastValue) {
                            lastValue = currentDetection;
                            for (Consumer<Boolean> listener : detector.listeners) {
                                try {
                                    listener.accept(currentDetection);
                                } catch (RuntimeException e) {
                                    logger.error("Caught exception during listener notifying ", e);
                                }
                            }
                        }
                    }
                    logger.debug("ThemeDetectorThread has been interrupted!");
                    if (monitoringProcess.isAlive()) {
                        monitoringProcess.destroy();
                        logger.debug("Monitoring process has been destroyed!");
                    }
                }
            } catch (IOException e) {
                logger.error("Couldn't start monitoring process ", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.error("Couldn't parse command line output", e);
            }
        }
    }
}
