package com.rememberber.mootool.nextfx.app;

import com.rememberber.mootool.nextfx.infrastructure.AppPaths;
import com.rememberber.mootool.nextfx.ui.shell.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class MooToolFxApplication extends Application {

    private static String[] launchArgs = new String[0];
    private AppServices services;

    public static void launchApp(String[] args) {
        launchArgs = args == null ? new String[0] : args;
        launch(MooToolFxApplication.class, launchArgs);
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(true);
        ProductIdentity identity = ProductIdentity.fromArgs(launchArgs);
        AppPaths paths = AppPaths.fromArgs(identity, launchArgs);
        services = new AppServices(identity, paths);
        MainWindow window = new MainWindow(services, stage);
        window.attach();
        var icon = MooToolFxApplication.class.getResource("/icons/icon.png");
        if (icon != null) {
            stage.getIcons().add(new Image(icon.toExternalForm()));
        }
        stage.show();
    }

    @Override
    public void stop() {
        if (services != null) {
            services.close();
        }
    }
}
