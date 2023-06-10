package com.cdac.enrollmentstation;


import com.cdac.enrollmentstation.logging.ApplicationLog;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.opencv.core.Core;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
    private static final Logger LOGGER = ApplicationLog.getLogger(App.class);
    private static volatile boolean isNudLogin;
    private static Scene scene;
    // GLOBAL THREAD POOL for the application.
    private static final ExecutorService executorService;

    static {
        int processorCount = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(Math.min(processorCount, 3));
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        //Added for Close Button
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Platform.exit();  //Comment this line in production/deployment (Alt+f4 and close button)
        });
        scene = new Scene(loadFXML("login"), 1366, 768);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.INFO, () -> "Caught by default Uncaught Exception Handler. Will exit now");
            LOGGER.log(Level.INFO, () -> "Caused: " + throwable.getCause());
            LOGGER.log(Level.INFO, () -> "Message: " + throwable.getMessage());
            throwable.printStackTrace();
        });
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Enrolment Application");
        primaryStage.setResizable(false);
        primaryStage.show();
        LOGGER.log(Level.INFO, () -> "Touch is " + (Platform.isSupported(ConditionalFeature.INPUT_TOUCH) ? "" : "not") + " supported.");
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        return FXMLLoader.load(Objects.requireNonNull(App.class.getResource("/fxml/" + fxml + ".fxml")));
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch();
    }

    public static ExecutorService getThreadPool() {
        return executorService;
    }

    public static void setNudLogin(boolean nudLogin) {
        isNudLogin = nudLogin;
    }

    public static boolean isNudLogin() {
        return isNudLogin;
    }
}