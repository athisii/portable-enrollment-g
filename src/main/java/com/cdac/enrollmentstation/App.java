package com.cdac.enrollmentstation;


import com.cdac.enrollmentstation.controller.AbstractBaseController;
import com.cdac.enrollmentstation.dto.OnboardingUnitDetail;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.DisplayUtil;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
    private static final Logger LOGGER = ApplicationLog.getLogger(App.class);
    private static AbstractBaseController controller;
    private static volatile boolean isNudLogin = false;
    private static volatile boolean hostnameChanged = false;
    private static Scene scene;
    // GLOBAL THREAD POOL for the application.
    private static final ExecutorService executorService;
    private static volatile String pno;
    private static List<OnboardingUnitDetail> onboardingUnitDetails;

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
            System.exit(0);
        });
        scene = new Scene(loadFXML("login"), DisplayUtil.SCREEN_WIDTH, DisplayUtil.SCREEN_HEIGHT);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(getCssFileName())).toExternalForm());
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "**Uncaught Exception Error: ", throwable);
            controller.onUncaughtException();
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
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        Parent parent = fxmlLoader.load();
        controller = fxmlLoader.getController();
        return parent;
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch();
    }

    public static void setHostnameChanged(boolean value) {
        App.hostnameChanged = value;
    }

    public static boolean getHostnameChanged() {
        return App.hostnameChanged;
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

    public static void setPno(String pno) {
        App.pno = pno;
    }

    public static String getPno() {
        return App.pno;
    }

    public static void setOnboardingUnitDetails(List<OnboardingUnitDetail> onboardingUnitDetails) {
        App.onboardingUnitDetails = onboardingUnitDetails != null ? onboardingUnitDetails.stream().sorted(Comparator.comparing(OnboardingUnitDetail::getSerialNo)).toList() : null;
    }

    public static List<OnboardingUnitDetail> getOnboardingUnitDetails() {
        return App.onboardingUnitDetails;
    }

    public static String getCssFileName() {
        // if width >= 1920 and height >= 1080
        if (DisplayUtil.SCREEN_WIDTH >= DisplayUtil.SCREEN_HD[0] && DisplayUtil.SCREEN_HEIGHT >= DisplayUtil.SCREEN_HD[1]) {
            return "/style/screen_fhd.css";
        }
        return "/style/base.css";
    }
}