package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailsUtil;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class BiometricEnrollmentController {
    private String tempArc;

    @FXML
    public Button continueBtn;
    @FXML

    public Button backBtn;
    @FXML
    public Button showArcBtn;

    @FXML
    private TextField arcNumberTextField;

    @FXML
    private Label messageLabel;

    @FXML
    private Label txtName;
    @FXML
    private Label txtRank;
    @FXML
    private Label txtApp;
    @FXML
    private Label txtUnit;
    @FXML
    private Label txtFinger;
    @FXML
    private Label txtIris;
    @FXML
    private Label txtArcStatus;

    @FXML
    private Label txtBiometricOptions;
    private static final Logger LOGGER = ApplicationLog.getLogger(BiometricEnrollmentController.class);

    public void initialize() {
        backBtn.setOnAction(event -> back());
        showArcBtn.setOnAction(event -> showArcBtnAction());
        continueBtn.setOnAction(event -> continueBtnAction());

        arcNumberTextField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                showArcBtnAction();
            }
        });
    }

    private void showArcBtnAction() {
        tempArc = arcNumberTextField.getText().trim();
        if (isMalformedArc()) {
            messageLabel.setText("Kindly enter the valid format for e-ARC number.");
            return;
        }
        disableControls(showArcBtn, backBtn, continueBtn);
        // fetches e-ARC in worker thread.
        App.getThreadPool().execute(this::showArcDetails);
        messageLabel.setText("Fetching details for e-ARC: " + tempArc + ". Kindly wait...");
    }

    private void continueBtnAction() {
        ARCDetails arcDetails = ARCDetailsHolder.getArcDetailsHolder().getArcDetails();

        if (arcDetails.getBiometricOptions().trim().equalsIgnoreCase("photo")) {
            try {
                App.setRoot("camera");
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, ex.getMessage());
            }
        } else if (arcDetails.getBiometricOptions().trim().equalsIgnoreCase("both") || arcDetails.getBiometricOptions().trim().equalsIgnoreCase("biometric")) {
            setNextScreen();
        } else {
            messageLabel.setText("Biometric capturing not required for e-ARC number: " + tempArc);
            LOGGER.log(Level.INFO, "Biometric capturing not required for given e-ARC Number");
        }

    }

    private void setNextScreen() {
        SaveEnrollmentDetails saveEnrollmentDetails;
        try {
            saveEnrollmentDetails = SaveEnrollmentDetailsUtil.readFromFile();
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            messageLabel.setText(ApplicationConstant.GENERIC_ERR_MSG);
            return;
        }

        // different e-ARC number is entered.
        if (saveEnrollmentDetails.getArcNo() == null || !ARCDetailsHolder.getArcDetailsHolder().getArcDetails().getArcNo().equals(saveEnrollmentDetails.getArcNo())) {
            try {
                App.setRoot("slap_scanner");
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
            }
        } else {
            // same e-ARC number is entered as the one saved in saveEnrollment.txt file.
            ARCDetailsHolder.getArcDetailsHolder().setSaveEnrollmentDetails(saveEnrollmentDetails);
            changeScreenBasedOnEnrollmentStatus();
        }

    }

    private void changeScreenBasedOnEnrollmentStatus() {
        switch (ARCDetailsHolder.getArcDetailsHolder().getSaveEnrollmentDetails().getEnrollmentStatus()) {
            case "FingerPrintCompleted":
                try {
                    App.setRoot("iris");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                }
                break;

            case "IrisCompleted":
                try {
                    App.setRoot("camera");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                }
                break;
            case "PhotoCompleted":
            case "SUCCESS":
                try {
                    App.setRoot("biometric_capture_complete");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                }
                break;
            default:
                try {
                    App.setRoot("slap_scanner");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                }
                break;
        }
    }

    @FXML
    private void back() {
        try {
            App.setRoot("main_screen");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, () -> "Error loading fxml: " + ex.getMessage());
        }
    }

    private void showArcDetails() {
        boolean alreadyCaptured;
        ARCDetails arcDetails;
        // check if already captured
        Future<Boolean> alreadyCapturedFuture = App.getThreadPool().submit(this::checkIfAlreadyCaptured);

        // check if e-ARC number exists
        Future<ARCDetails> arcDetailsFuture = App.getThreadPool().submit(this::checkIfArcNumberExist);

        // waits for both the worker threads to finish
        try {
            alreadyCaptured = alreadyCapturedFuture.get();
            arcDetails = arcDetailsFuture.get();
        } catch (InterruptedException e) {
            enableControls(backBtn, showArcBtn);
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while getting value from worker thread.");
            enableControls(backBtn, showArcBtn);
            updateUiDynamicLabelText(null);
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
            return;
        }

        if (alreadyCaptured) {
            LOGGER.log(Level.INFO, () -> tempArc + " is already enrolled.");
            enableControls(backBtn, showArcBtn);
            updateUiDynamicLabelText(null);
            updateUI("Biometric already provided for e-ARC number: " + tempArc);
            return;
        }


        if (arcDetails == null) {
            LOGGER.log(Level.INFO, () -> "Details not found for e-ARC number: " + tempArc);
            enableControls(backBtn, showArcBtn);
            updateUiDynamicLabelText(null);
            updateUI("Details not found for e-ARC number: " + tempArc);
            return;
        }

        if (arcDetails.getBiometricOptions() == null || arcDetails.getBiometricOptions().isBlank() || arcDetails.getBiometricOptions().trim().equalsIgnoreCase("none") || arcDetails.getBiometricOptions().trim().equalsIgnoreCase("no")) {
            LOGGER.log(Level.INFO, () -> "Biometric capturing not required for e-ARC: " + tempArc);
            enableControls(backBtn, showArcBtn);
            updateUiDynamicLabelText(arcDetails);
            updateUI("Biometric capturing not required for e-ARC: " + tempArc);
            return;
        }
        updateUiDynamicLabelText(arcDetails);
        updateUI("Details fetched successfully for e-ARC: " + tempArc);


        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        holder.setArcDetails(arcDetails);
        SaveEnrollmentDetails saveEnrollmentDetails = new SaveEnrollmentDetails();

        try {
            saveEnrollmentDetails.setEnrollmentStationUnitID(MafisServerApi.getEnrollmentStationUnitId());
            saveEnrollmentDetails.setEnrollmentStationID(MafisServerApi.getEnrollmentStationId());
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            enableControls(backBtn, showArcBtn);
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
            return;
        }

        saveEnrollmentDetails.setArcNo(arcDetails.getArcNo());
        saveEnrollmentDetails.setBiometricOptions(arcDetails.getBiometricOptions());
        holder.setSaveEnrollmentDetails(saveEnrollmentDetails);
        enableControls(showArcBtn, backBtn, continueBtn);
    }


    private ARCDetails checkIfArcNumberExist() {
        // throws exception
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            Optional<Path> optionalPath = importFolder
                    .filter(path -> {
                        if (Files.isRegularFile(path)) {
                            // 00001-INSI-INS INDIA --> filename format
                            // 00001-A-AA21 --> e-ARC format
                            return arcNumberTextField.getText().split("-")[0].equals(path.getFileName().toString().split("-")[0]);
                        }
                        return false;
                    }).findFirst();

            if (optionalPath.isEmpty()) {
                return null;
            }
            // throws exception
            String jsonString = Files.readString(optionalPath.get(), StandardCharsets.UTF_8);
            // throws exception
            List<ARCDetails> arcList = Singleton.getObjectMapper().readValue(jsonString, new TypeReference<>() {
            });

            for (ARCDetails arcDetail : arcList) {
                if (arcNumberTextField.getText().equals(arcDetail.getArcNo())) {
                    return arcDetail;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while searching e-ARC number in import folder");
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
        }
        //e-ARC number not found
        return null;
    }

    private boolean checkIfAlreadyCaptured() {
        // throws exception
        try (Stream<Path> encExportFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER)))) {
            Optional<Path> optionalPath = encExportFolder
                    .filter(path -> {
                        if (Files.isRegularFile(path)) {
                            // 00001-A-AA02.json.enc -- filename format
                            return arcNumberTextField.getText().equals(path.getFileName().toString().split("\\.")[0]);
                        }
                        return false;
                    }).findFirst();
            return optionalPath.isPresent();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while checking if already captured");
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
            return false;
        }
    }

    private void updateUiDynamicLabelText(ARCDetails arcDetails) {
        Platform.runLater(() -> {
            if (arcDetails == null) {
                messageLabel.setText("Details not found for entered e-ARC number.");
                clearLabelText(txtName, txtRank, txtApp, txtUnit, txtFinger, txtIris, txtBiometricOptions, txtArcStatus);
                return;
            }
            txtName.setText(arcDetails.getName());
            txtRank.setText(arcDetails.getRank());
            txtApp.setText(arcDetails.getApplicantID());
            txtUnit.setText(arcDetails.getUnit());
            if (arcDetails.getFingers().isEmpty()) {
                txtFinger.setText("NA");
            } else {
                txtFinger.setText(String.join(",", arcDetails.getFingers()));
            }
            if (arcDetails.getIris().isEmpty()) {
                txtIris.setText("NA");
            } else {
                txtIris.setText(String.join(",", arcDetails.getIris()));
            }
            txtBiometricOptions.setText(arcDetails.getBiometricOptions());
            txtArcStatus.setText(arcDetails.getArcStatus());
        });
    }

    private void clearLabelText(Label... labels) {
        for (Label label : labels) {
            label.setText("");
        }
    }

    private boolean isMalformedArc() {
        // 00001-A-AA01
        return tempArc.split("-").length != 3;
    }

    private void updateUI(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    private void disableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(true);
        }
    }

    private void enableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }
}
