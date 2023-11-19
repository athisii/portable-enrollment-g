package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class AdminConfigController extends AbstractBaseController {
    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(AdminConfigController.class);
    private static final int FINGERPRINT_LIVENESS_MAX;
    private static final int FINGERPRINT_LIVENESS_MIN;
    private final int fingerprintLivenessValue = Integer.parseInt(PropertyFile.getProperty(PropertyName.FINGERPRINT_LIVENESS_VALUE).trim());

    static {
        try {
            FINGERPRINT_LIVENESS_MAX = Integer.parseInt(PropertyFile.getProperty(PropertyName.FINGERPRINT_LIVENESS_MAX).trim());
            FINGERPRINT_LIVENESS_MIN = Integer.parseInt(PropertyFile.getProperty(PropertyName.FINGERPRINT_LIVENESS_MIN).trim());
        } catch (NumberFormatException ex) {
            throw new GenericException("Invalid max or min fingerprint liveness value. It must be a number.");
        }
    }

    @FXML
    private Button serverConfigBtn;

    @FXML
    private Label messageLabel;
    @FXML
    private TextField liveFpTextField;
    @FXML
    private Button liveFpBtn;

    @FXML
    private AnchorPane confirmPane;

    @FXML
    private ComboBox<String> cameraComboBox;


    /**
     * Automatically called by JavaFX runtime.
     */
    public void initialize() {
        cameraComboBox.getItems().addAll(ApplicationConstant.INTERNAL, ApplicationConstant.EXTERNAL);
        // Internal: value = 0; index = 0
        // External: value = 2; index = 1
        int cameraId = Integer.parseInt(PropertyFile.getProperty(PropertyName.CAMERA_ID).trim());
        // default value to display
        cameraComboBox.getSelectionModel().select(cameraId == 0 ? 0 : 1);
        cameraComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int camId = ApplicationConstant.EXTERNAL.equalsIgnoreCase(newValue) ? 2 : 0;
            PropertyFile.changePropertyValue(PropertyName.CAMERA_ID, String.valueOf(camId));
            Platform.runLater(() -> messageLabel.setText((camId == 0 ? ApplicationConstant.INTERNAL : ApplicationConstant.EXTERNAL) + " Camera Selected"));
            LOGGER.log(Level.INFO, PropertyFile.getProperty(PropertyName.CAMERA_ID));
        });
        liveFpTextField.setText(String.valueOf(fingerprintLivenessValue));
        liveFpBtn.setOnAction(event -> liveFpBtnAction());

        if (!App.isNudLogin()) {
            liveFpBtn.setDisable(true);
            serverConfigBtn.setDisable(true);
        }

    }

    private void liveFpBtnAction() {
        // check how text on edit button should be displayed
        if (liveFpTextField.isEditable()) {
            String inputValue = liveFpTextField.getText();
            String displayMessage = "Please enter a number between " + FINGERPRINT_LIVENESS_MIN + " and " + FINGERPRINT_LIVENESS_MAX;
            if (inputValue.isBlank()) {
                messageLabel.setText(displayMessage);
                return;
            }
            int number;
            try {
                number = Integer.parseInt(inputValue);
                if (number < FINGERPRINT_LIVENESS_MIN || number > FINGERPRINT_LIVENESS_MAX) {
                    throw new NumberFormatException("Invalid fingerprint value.");
                }
            } catch (NumberFormatException ex) {
                liveFpTextField.setText("");
                messageLabel.setText(displayMessage);
                return;
            }
            PropertyFile.changePropertyValue(PropertyName.FINGERPRINT_LIVENESS_VALUE, String.valueOf(number));
            liveFpBtn.setText("EDIT"); // shows as edit
            messageLabel.setText("Fingerprint liveness value updated successfully.");
        } else {
            messageLabel.setText("");
            liveFpBtn.setText("UPDATE");
        }
        // toggles the edit-ability
        liveFpTextField.setEditable(!liveFpTextField.isEditable());
    }

    @FXML
    public void closeApp() {
        LOGGER.log(Level.INFO, "Application Close Call made");
        Platform.exit();
    }

    @FXML
    public void serverConfig() throws IOException {
        App.setRoot("server_config");
    }

    @FXML
    public void licenseInfo() throws IOException {
        App.setRoot("license_info");
    }

    @FXML
    public void deviceCheck() throws IOException {
        App.setRoot("device_status");
    }


    @FXML
    public void backBtnAction() throws IOException {
        App.setRoot("main_screen");

    }

    @FXML
    public void restartSystem() {
        confirmPane.setVisible(true);
    }

    @FXML
    private void restart() {
        restartSys();
    }

    @FXML
    private void stayBack() {
        confirmPane.setVisible(false);
    }

    private void restartSys() {
        try {
            LOGGER.log(Level.INFO, "System restarting..");
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", "init 6");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            LOGGER.log(Level.INFO, () -> "Exited with error code : " + exitCode);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(Level.INFO, ex.getMessage());
        }
    }


    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
        updateUi("Something went wrong. Please try again");
    }

    private void updateUi(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }
}
