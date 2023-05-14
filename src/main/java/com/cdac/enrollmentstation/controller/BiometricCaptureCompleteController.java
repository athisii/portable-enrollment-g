package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AesFileUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailsUtil;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class BiometricCaptureCompleteController {
    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(BiometricCaptureCompleteController.class);
    private static final String NOT_AVAILABLE = "Not Available";


    @FXML
    private Label messageLabel;

    @FXML
    private ImageView statusImageView;

    @FXML
    private Button submitBtn;

    @FXML
    private Button homeBtn;

    @FXML
    private Button fetchArcBtn;

    @FXML
    private ProgressIndicator progressIndicator;

    // calls automatically by JavaFx runtime
    public void initialize() {
        // better sets button actions here
        messageLabel.setText("Please click SUBMIT button and wait....");
    }

    @FXML
    private void homeBtnAction() {
        try {
            App.setRoot("main_screen");
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, ex.getMessage());
        }
    }

    @FXML
    private void fetchArcBtnAction() {
        try {
            App.setRoot("enrollment_arc");
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, ex.getMessage());
        }
    }


    @FXML
    private void submitBtnAction() {
        submitBtn.setDisable(true);
        progressIndicator.setVisible(true);
        messageLabel.setText("Please wait...");
        ForkJoinPool.commonPool().execute(this::submitData);
    }

    private void submitData() {
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails arcDetails = holder.getArcDetails();
        SaveEnrollmentDetails saveEnrollmentDetails = holder.getSaveEnrollmentDetails();

        // based on biometricOptions just do the necessary actions
        if (arcDetails.getBiometricOptions().toLowerCase().contains("biometric")) {
            saveEnrollmentDetails.setPhoto(NOT_AVAILABLE);
            saveEnrollmentDetails.setPhotoCompressed(NOT_AVAILABLE);
            saveEnrollmentDetails.setEnrollmentStatus("Success");
        } else if (arcDetails.getBiometricOptions().toLowerCase().contains("photo")) {
            // only adds photo
            try {
                addPhoto(saveEnrollmentDetails);
            } catch (GenericException ignored) {
                onErrorUpdateUiControls();
                return;
            }
            // set NA for slapscanner, iris etc.
            saveEnrollmentDetails.setIRISScannerSerailNo(NOT_AVAILABLE);
            saveEnrollmentDetails.setLeftFPScannerSerailNo(NOT_AVAILABLE);
            saveEnrollmentDetails.setRightFPScannerSerailNo(NOT_AVAILABLE);
            Set<FP> fingerprintset = new HashSet<>(Set.of(new FP(NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE)));
            saveEnrollmentDetails.setFp(fingerprintset);
            Set<IRIS> irisSet = new HashSet<>(Set.of(new IRIS(NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE)));
            saveEnrollmentDetails.setIris(irisSet);
        } else if (arcDetails.getBiometricOptions().toLowerCase().contains("both")) {
            // fingerprint and iris already added in their controllers
            // so now add only photo
            try {
                addPhoto(saveEnrollmentDetails);
            } catch (GenericException ignored) {
                onErrorUpdateUiControls();
                return;
            }
        }

        // common properties
        saveEnrollmentDetails.setEnrollmentStationID(MafisServerApi.getEnrollmentStationId());
        saveEnrollmentDetails.setEnrollmentStationUnitID(MafisServerApi.getEnrollmentStationUnitId());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        saveEnrollmentDetails.setEnrollmentDate(formatter.format(date));
        saveEnrollmentDetails.setArcStatus(arcDetails.getArcStatus());
        saveEnrollmentDetails.setUniqueID(arcDetails.getApplicantID());//For ApplicantID
        saveEnrollmentDetails.setBiometricOptions(arcDetails.getBiometricOptions());

        // saves saveEnrollmentDetails for backups
        try {
            SaveEnrollmentDetailsUtil.writeToFile(saveEnrollmentDetails);
        } catch (GenericException ignored) {
            onErrorUpdateUiControls();
            return;
        }

        // converts saveEnrollmentDetails object to json string
        String jsonData;
        try {
            jsonData = Singleton.getObjectMapper().writeValueAsString(saveEnrollmentDetails);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            onErrorUpdateUiControls();
            return;
        }
        // starts another thread for encrypting data to avoid wasting cpu time when API call fails.
        // but this encrypted data file must be DELETED if API call succeeds
        ForkJoinTask<Boolean> encryptionProcessFuture = ForkJoinPool.commonPool().submit(() -> {
            LOGGER.log(Level.INFO, () -> "EncryptingThread: " + Thread.currentThread().getName());
            return startEncryptionProcess(arcDetails.getArcNo(), jsonData);
        });

        SaveEnrollmentResDto saveEnrollmentResDto;
        // try submitting to the server.
        try {
            saveEnrollmentResDto = MafisServerApi.postEnrollment(jsonData);
        } catch (GenericException ignored) {
            onErrorUpdateUiControls();
            return;
        }

        // connection timeout error
        // saves the data locally
        if (saveEnrollmentResDto == null) {
            try {
                boolean result = encryptionProcessFuture.get();
                // encrypted successfully
                if (result) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Record saved successfully.");
                        submitBtn.setDisable(true);
                        progressIndicator.setVisible(false);
                        homeBtn.setDisable(false);
                        fetchArcBtn.setDisable(false);
                    });
                    try {
                        SaveEnrollmentDetailsUtil.delete();
                    } catch (GenericException ignored) {
                        onErrorUpdateUiControls();
                    }
                    return;
                }
            } catch (InterruptedException | ExecutionException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.log(Level.SEVERE, ex.getMessage());
            }
            onErrorUpdateUiControls();
            return;
        }

        // checks for error response
        if (!"0".equals(saveEnrollmentResDto.getErrorCode())) {
            LOGGER.log(Level.SEVERE, () -> "Server desc: " + saveEnrollmentResDto.getDesc());
            // runs on main thread
            updateUiIconOnServerResponse(false, saveEnrollmentResDto.getDesc());
        } else {
            // else saved successfully on the server
            // runs on main thread
            updateUiIconOnServerResponse(true, "Record submitted to server successfully.");
        }

        // time for cleanup
        try {
            SaveEnrollmentDetailsUtil.delete();
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            onErrorUpdateUiControls();
        }

        // deletes encrypted file saved by worker thread.
        try {
            boolean result = encryptionProcessFuture.get();
            if (result) {
                Files.delete(Paths.get(PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER) + "/" + arcDetails.getArcNo() + ".json.enc"));
            }
        } catch (InterruptedException | ExecutionException | IOException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(Level.SEVERE, ex.getMessage());
            onErrorUpdateUiControls();
        }

    }

    private boolean startEncryptionProcess(String arcNumber, String jsonData) {
        try {
            encryptAndSaveLocally(arcNumber, jsonData);
            return true;
        } catch (GenericException ex) {
            onErrorUpdateUiControls();
            return false;
        }
    }

    private void updateUiIconOnServerResponse(boolean success, String message) {
        Platform.runLater(() -> {
            InputStream inputStream;
            if (success) {
                inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/tick_green.jpg");
            } else {
                inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/red_cross.png");
            }
            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, "Image not found for updating the UI image.");
                //TODO: continue for now
            }
            messageLabel.setText(message);
            statusImageView.setImage(new Image(inputStream));
            submitBtn.setDisable(true);
            homeBtn.setDisable(false);
            fetchArcBtn.setDisable(false);
            progressIndicator.setVisible(false);
        });
    }

    // adds photo to GLOBAL saveEnrollment object
    private void addPhoto(SaveEnrollmentDetails saveEnrollmentDetails) {
        String subPhoto = PropertyFile.getProperty(PropertyName.IMG_SUB_FILE);
        if (subPhoto == null || subPhoto.isBlank()) {
            LOGGER.log(Level.SEVERE, "No entry for '" + PropertyName.IMG_SUB_FILE + ", in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

        String compressPhoto = PropertyFile.getProperty(PropertyName.IMG_COMPRESS_FILE);
        if (compressPhoto == null || compressPhoto.isBlank()) {
            LOGGER.log(Level.SEVERE, "No entry for '" + PropertyName.IMG_COMPRESS_FILE + ", in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

        Path subPhotoPath = Paths.get(subPhoto);
        Path compressPhotoPath = Paths.get(compressPhoto);

        // check if photo files exists.
        if (!Files.exists(subPhotoPath) || !Files.exists(compressPhotoPath)) {
            LOGGER.log(Level.SEVERE, "Both or either sub photo and compress photo file not found.");
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

        try {
            saveEnrollmentDetails.setPhoto(Base64.getEncoder().encodeToString(Files.readAllBytes(subPhotoPath)));
            saveEnrollmentDetails.setPhotoCompressed(Base64.getEncoder().encodeToString(Files.readAllBytes(compressPhotoPath)));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);

        }
        saveEnrollmentDetails.setEnrollmentStatus("PhotoCompleted");
    }


    // encrypt and save data by e-ARC number as a fine name.
    private void encryptAndSaveLocally(String arcNo, String jsonData) {
        // pass temp path to for encryption.
        String encFolderString = PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER);
        if (encFolderString == null || encFolderString.isBlank()) {
            LOGGER.log(Level.SEVERE, "No entry for '" + PropertyName.ENC_EXPORT_FOLDER + ", in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        Path encOutputPath = Paths.get(encFolderString + "/" + arcNo + ".json.enc");
        AesFileUtil.encrypt(jsonData, encOutputPath);
        AesFileUtil.removeCipherFromThreadLocal();
    }

    private void onErrorUpdateUiControls() {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            messageLabel.setText(ApplicationConstant.GENERIC_ERR_MSG);
            homeBtn.setDisable(false);
            fetchArcBtn.setDisable(false);
        });
    }

}

 