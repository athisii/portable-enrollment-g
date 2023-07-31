package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.ArcDetail;
import com.cdac.enrollmentstation.dto.Fp;
import com.cdac.enrollmentstation.dto.Iris;
import com.cdac.enrollmentstation.dto.SaveEnrollmentDetail;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AesFileUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailUtil;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class BiometricCaptureCompleteController {
    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(BiometricCaptureCompleteController.class);
    private static final String NOT_AVAILABLE = "Not Available";
    private static volatile boolean isEncryptedAndSaved = false;
    private static volatile boolean isDone = false;
    private static final CountDownLatch countDownLatch = new CountDownLatch(1);


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
            App.setRoot("biometric_enrollment");
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, ex.getMessage());
        }
    }

    @FXML
    private void submitBtnAction() {
        progressIndicator.setVisible(true);
        messageLabel.setText("Please wait...");
        disableControls(homeBtn, fetchArcBtn, submitBtn);
        App.getThreadPool().execute(this::submitData);
    }

    private void submitData() {
        ArcDetailsHolder holder = ArcDetailsHolder.getArcDetailsHolder();
        ArcDetail arcDetail = holder.getArcDetail();
        SaveEnrollmentDetail saveEnrollmentDetail = holder.getSaveEnrollmentDetail();

        // based on biometricOptions just do the necessary actions
        if (arcDetail.getBiometricOptions().toLowerCase().contains("biometric")) {
            saveEnrollmentDetail.setPhoto(NOT_AVAILABLE);
            saveEnrollmentDetail.setPhotoCompressed(NOT_AVAILABLE);
            saveEnrollmentDetail.setEnrollmentStatus("Success");
        } else if (arcDetail.getBiometricOptions().toLowerCase().contains("photo")) {
            // only adds photo
            try {
                addPhoto(saveEnrollmentDetail);
            } catch (GenericException ignored) {
                onErrorUpdateUiControls();
                return;
            }
            // set NA for slapscanner, iris etc.
            saveEnrollmentDetail.setIrisScannerSerialNo(NOT_AVAILABLE);
            saveEnrollmentDetail.setLeftFrScannerSerialNo(NOT_AVAILABLE);
            saveEnrollmentDetail.setRightFpScannerSerialNo(NOT_AVAILABLE);
            Set<Fp> fingerprintset = new HashSet<>(Set.of(new Fp(NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE)));
            saveEnrollmentDetail.setFp(fingerprintset);
            Set<Iris> irisSet = new HashSet<>(Set.of(new Iris(NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE)));
            saveEnrollmentDetail.setIris(irisSet);
        } else if (arcDetail.getBiometricOptions().toLowerCase().contains("both")) {
            // fingerprint and iris already added in their controllers
            // so now add only photo
            try {
                addPhoto(saveEnrollmentDetail);
            } catch (GenericException ignored) {
                onErrorUpdateUiControls();
                return;
            }
        }

        // common properties
        saveEnrollmentDetail.setEnrollmentStationId(MafisServerApi.getEnrollmentStationId());
        saveEnrollmentDetail.setEnrollmentStationUnitId(MafisServerApi.getEnrollmentStationUnitId());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        saveEnrollmentDetail.setEnrollmentDate(formatter.format(date));
        saveEnrollmentDetail.setArcStatus(arcDetail.getArcStatus());
        saveEnrollmentDetail.setUniqueId(arcDetail.getApplicantId());//For ApplicantID
        saveEnrollmentDetail.setBiometricOptions(arcDetail.getBiometricOptions());

        // saves saveEnrollmentDetail for backups
        try {
            SaveEnrollmentDetailUtil.writeToFile(saveEnrollmentDetail);
        } catch (GenericException ignored) {
            onErrorUpdateUiControls();
            return;
        }

        // converts saveEnrollmentDetail object to json string
        String jsonData;
        try {
            jsonData = Singleton.getObjectMapper().writeValueAsString(saveEnrollmentDetail);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            onErrorUpdateUiControls();
            return;
        }
        // saves locally
        startEncryptionProcess(arcDetail.getArcNo(), jsonData);
    }

    private void startEncryptionProcess(String arcNumber, String jsonData) {
        try {
            encryptAndSaveLocally(arcNumber, jsonData);
            updateUiIconOnServerResponse(true, "Applicant's Biometric Captured Successfully.");
        } catch (GenericException ex) {
            updateUiIconOnServerResponse(false, ex.getMessage());
        }
        // time for cleanup
        try {
            SaveEnrollmentDetailUtil.delete();
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            onErrorUpdateUiControls();
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
            enableControls(homeBtn, fetchArcBtn);
            submitBtn.setDisable(success);
            progressIndicator.setVisible(false);
        });
    }

    // adds photo to GLOBAL saveEnrollment object
    private void addPhoto(SaveEnrollmentDetail saveEnrollmentDetail) {
        String subPhoto = PropertyFile.getProperty(PropertyName.IMG_SUB_FILE);
        if (subPhoto == null || subPhoto.isBlank()) {
            LOGGER.log(Level.SEVERE, "No entry for '" + PropertyName.IMG_SUB_FILE + ", in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(GENERIC_ERR_MSG);
        }

        String compressPhoto = PropertyFile.getProperty(PropertyName.IMG_COMPRESS_FILE);
        if (compressPhoto == null || compressPhoto.isBlank()) {
            LOGGER.log(Level.SEVERE, "No entry for '" + PropertyName.IMG_COMPRESS_FILE + ", in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(GENERIC_ERR_MSG);
        }

        Path subPhotoPath = Paths.get(subPhoto);
        Path compressPhotoPath = Paths.get(compressPhoto);

        // check if photo files exists.
        if (!Files.exists(subPhotoPath) || !Files.exists(compressPhotoPath)) {
            LOGGER.log(Level.SEVERE, "Both or either sub photo and compress photo file not found.");
            throw new GenericException(GENERIC_ERR_MSG);
        }

        try {
            saveEnrollmentDetail.setPhoto(Base64.getEncoder().encodeToString(Files.readAllBytes(subPhotoPath)));
            saveEnrollmentDetail.setPhotoCompressed(Base64.getEncoder().encodeToString(Files.readAllBytes(compressPhotoPath)));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);

        }
        saveEnrollmentDetail.setEnrollmentStatus("PhotoCompleted");
    }


    // encrypt and save data by e-ARC number as a fine name.
    private void encryptAndSaveLocally(String arcNo, String jsonData) {
        // pass temp path to for encryption.
        String encFolderString = PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER);
        if (encFolderString == null || encFolderString.isBlank()) {
            LOGGER.log(Level.SEVERE, "No entry for '" + PropertyName.ENC_EXPORT_FOLDER + ", in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        Path encOutputPath = Paths.get(encFolderString + "/" + arcNo + ".json.enc");
        AesFileUtil.encrypt(jsonData, encOutputPath);
    }

    private void onErrorUpdateUiControls() {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            messageLabel.setText(GENERIC_ERR_MSG);
            enableControls(homeBtn, fetchArcBtn, submitBtn);
        });
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

 