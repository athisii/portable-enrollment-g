package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.CardHotlistApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.CardHotlistDetail;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class AdminConfigController {
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
    private Button downloadBtn;
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
        downloadBtn.setOnAction(event -> downloadBtnAction());

        if (!App.isNudLogin()) {
            downloadBtn.setDisable(true);
            liveFpBtn.setDisable(true);
            serverConfigBtn.setDisable(true);
        }

    }

    private void updateUi(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    private void disableControl(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(true);
        }
    }

    private void enableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }

    private void downloadBtnAction() {
        String hotlistedCardFilePath = PropertyFile.getProperty(PropertyName.CARD_HOTLISTED_FILE);
        if (hotlistedCardFilePath.isBlank()) {
            LOGGER.log(Level.SEVERE, () -> PropertyName.CARD_HOTLISTED_FILE + " is empty or no entry found in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(PropertyName.CARD_HOTLISTED_FILE + " is empty or no entry found in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        disableControl(downloadBtn);
        App.getThreadPool().execute(() -> {
            List<CardHotlistDetail> cardHotlistDetails;
            try {
                cardHotlistDetails = CardHotlistApi.fetchHotlistedCard();
            } catch (GenericException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
                updateUi(GENERIC_ERR_MSG);
                enableControls(downloadBtn);
                return;
            }

            if (cardHotlistDetails == null) {
                enableControls(downloadBtn);
                updateUi("Connection timeout or received an unexpected value from server.");
                return;
            }
            try {
                String cardHotlistString = Singleton.getObjectMapper().writeValueAsString(cardHotlistDetails);
                Files.writeString(Paths.get(hotlistedCardFilePath), cardHotlistString, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                updateUi(GENERIC_ERR_MSG);
                enableControls(downloadBtn);
            }
            updateUi("Downloaded hotlisted cards successfully.");
            enableControls(downloadBtn);
        });
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


}
