package com.cdac.enrollmentstation;


import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.logging.ApplicationLogNew;
import com.cdac.enrollmentstation.util.IrisInitialize;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application implements EventHandler<WindowEvent> {
    private static final Logger LOGGER = ApplicationLogNew.getLogger(App.class);
    private static Scene scene;
    public static IrisInitialize irisInit;
    public static SecretKey skey;
    public APIServerCheck apiServerCheck = new APIServerCheck();
    public SaveEnrollmentResponse saveEnrollmentResponse;

    @Override
    public void start(Stage primaryStage) throws IOException {
        //Added for Close Button
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Platform.exit();  //Comment this line in production/deployment (Alt+f4 and close button)
        });
        scene = new Scene(loadFXML("main"), 1366, 768);
//        scene.getStylesheets().add("/css/enrollmentcss.css")
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.INFO, () -> "Caught by default Uncaught Exception Handler. Will exit now");
            throwable.printStackTrace();
        });
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Enrollment Application");
        primaryStage.setResizable(false);
        primaryStage.show();
        LOGGER.log(Level.INFO, () -> "Touch is " + (Platform.isSupported(ConditionalFeature.INPUT_TOUCH) ? "" : "not") + " supported.");
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch();
    }

    @Override
    public void handle(WindowEvent arg0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}