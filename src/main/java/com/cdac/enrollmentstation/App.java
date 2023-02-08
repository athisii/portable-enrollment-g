package com.cdac.enrollmentstation;


import com.cdac.enrollmentstation.logging.ApplicationLog;
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

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application implements EventHandler<WindowEvent> {
    private static final Logger LOGGER = ApplicationLog.getLogger(App.class);
    private static Scene scene;

    @Override
    public void start(Stage primaryStage) throws IOException {
        //Added for Close Button
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Platform.exit();  //Comment this line in production/deployment (Alt+f4 and close button)
        });
        scene = new Scene(loadFXML("import_export"), 1366, 768);
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
        return FXMLLoader.load(Objects.requireNonNull(App.class.getResource("/fxml/" + fxml + ".fxml")));
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