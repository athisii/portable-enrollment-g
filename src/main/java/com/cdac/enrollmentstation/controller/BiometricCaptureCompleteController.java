/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.api.ServerAPI;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.service.ObjectReaderWriter;
import com.cdac.enrollmentstation.util.DeleteSavedJsonFile;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailsUtil;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class BiometricCaptureCompleteController {
    private static final Logger LOGGER = ApplicationLog.getLogger(BiometricCaptureCompleteController.class);

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

    private String finalBase64Img;

    private SaveEnrollmentResponse saveEnrollmentResponse;

    private APIServerCheck apiServerCheck = new APIServerCheck();


    //For Application Log

    //Thread
    private Thread pi = null;


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


    private void updateUI(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    @FXML
    private void submitBtnAction() {
        submitBtn.setDisable(true);
        try {
            pi = new Thread(ShowProgressInd);
            pi.start();
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, ex.getMessage());
        }

    }

    private void submitBtnNew() {
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails arcDetails = holder.getArcDetails();
        SaveEnrollmentDetails saveEnrollmentDetails = holder.getSaveEnrollmentDetails();

        if (arcDetails.getBiometricOptions().toLowerCase().contains("biometric")) {
            //set NA for photo. TODO:  Is it necessary???
            saveEnrollmentDetails.setPhoto("Not Available");
            saveEnrollmentDetails.setEnrollmentStatus("PhotoCompleted");
            saveEnrollmentDetails.setPhotoCompressed("Not Available");
            saveEnrollmentDetails.setEnrollmentStatus("Success");
        } else if (arcDetails.getBiometricOptions().toLowerCase().contains("photo")) {
            try {
                addPhoto(saveEnrollmentDetails);
            } catch (GenericException ex) {
                updateUI(ex.getMessage());
                enableControl(homeBtn, fetchArcBtn);
                Platform.runLater(() -> progressIndicator.setVisible(false));
                return;
            }
            // set NA for slapscanner, iris etc.
            saveEnrollmentDetails.setIRISScannerSerailNo("Not Available");
            saveEnrollmentDetails.setLeftFPScannerSerailNo("Not Available");
            saveEnrollmentDetails.setRightFPScannerSerailNo("Not Available");
            Set<FP> fingerprintset = new HashSet<>(Set.of(new FP("Not Available", "Not Available", "Not Available")));
            saveEnrollmentDetails.setFp(fingerprintset);
            Set<IRIS> irisSet = new HashSet<>(Set.of(new IRIS("Not Available", "Not Available", "Not Available")));
            saveEnrollmentDetails.setIris(irisSet);
        } else if (arcDetails.getBiometricOptions().toLowerCase().contains("both")) {
            try {
                addPhoto(saveEnrollmentDetails);
            } catch (GenericException ex) {
                updateUI(ex.getMessage());
                enableControl(homeBtn, fetchArcBtn);
                Platform.runLater(() -> progressIndicator.setVisible(false));
                return;
            }
        }

        // necessary properties
        saveEnrollmentDetails.setEnrollmentStationID(ServerAPI.getEnrollmentStationId());
        saveEnrollmentDetails.setEnrollmentStationUnitID(ServerAPI.getEnrollmentStationUnitId());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        saveEnrollmentDetails.setEnrollmentDate(formatter.format(date));
        saveEnrollmentDetails.setArcStatus(arcDetails.getArcStatus());
        saveEnrollmentDetails.setUniqueID(arcDetails.getApplicantID());//For ApplicantID
        saveEnrollmentDetails.setBiometricOptions(arcDetails.getBiometricOptions());

        try {
            SaveEnrollmentDetailsUtil.writeToFile(saveEnrollmentDetails);
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
            enableControl(homeBtn, fetchArcBtn);
            Platform.runLater(() -> progressIndicator.setVisible(false));
            return;
        }

        String saveEnrollmentDetailsString;
        try {
            saveEnrollmentDetailsString = Singleton.getObjectMapper().writeValueAsString(saveEnrollmentDetails);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
            enableControl(homeBtn, fetchArcBtn);
            Platform.runLater(() -> progressIndicator.setVisible(false));
            return;
        }
        //TODO: encrypt saveEnrollmentString
        String data = saveEnrollmentDetailsString; // <-- encrypt(saveEnrollmentString)

        SaveEnrollmentResponse saveEnrollmentResponse;
        try {
            saveEnrollmentResponse = ServerAPI.postEnrollment(data);
        } catch (GenericException ex) {
            if (ex.getMessage().toLowerCase().contains("timeout")) {
                //TODO:
                // encrypt and save locally
                // updateUI
            }
            return;
        }
        if (!"0".equals(saveEnrollmentResponse.getErrorCode())) {
            LOGGER.log(Level.SEVERE, "Server desc: " + saveEnrollmentResponse.getDesc());
            Platform.runLater(() -> {
                InputStream inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/redcross.png");
                Image image = new Image(Objects.requireNonNull(inputStream));
                statusImageView.setImage(image);
                messageLabel.setText(ApplicationConstant.GENERIC_ERR_MSG);
                submitBtn.setDisable(true);
                homeBtn.setDisable(false);
                fetchArcBtn.setDisable(false);
                progressIndicator.setVisible(false);
            });
            return;
        }
        // else saved remotely
        Platform.runLater(() -> {
            messageLabel.setText("Data submitted to server successfully.");
            InputStream inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/tickgreen.jpg");
            Image image = new Image(Objects.requireNonNull(inputStream));
            statusImageView.setImage(image);
            progressIndicator.setVisible(false);
            homeBtn.setDisable(false);
            fetchArcBtn.setDisable(false);
        });

        try {
            SaveEnrollmentDetailsUtil.delete();
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            updateUI(ApplicationConstant.GENERIC_ERR_MSG);
            enableControl(homeBtn, fetchArcBtn);
            Platform.runLater(() -> progressIndicator.setVisible(false));
        }
    }

    private void addPhoto(SaveEnrollmentDetails saveEnrollmentDetails) {
        // check if photo files exists.
        Path subPhotoPath = Paths.get(PropertyFile.getProperty(PropertyName.SUB_FILE));
        Path compressPhotoPath = Paths.get(PropertyFile.getProperty(PropertyName.COMPRESS_FILE));

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

    Runnable ShowProgressInd = new Runnable() {
        @Override
        public void run() {
            try {
                progressIndicator.setVisible(true);
                updateUI("Please wait...");

                //Removing the Old Photo while capturing Photo
                //String file1 = "/usr/share/enrollment/croppedimg/sub.png"; //changed from out.png to sub.png
                File photoCaptureFile = new File(PropertyFile.getProperty(PropertyName.PHOTO_CAPTURE_IMG));

                //String file1 = "/usr/share/enrollment/croppedimg/sub.png"; //changed from out.png to sub.png
                File photoOutFile = new File(PropertyFile.getProperty(PropertyName.OUTPUT_FILE));


                ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                ARCDetails arcDetails = holder.getArcDetails();
                SaveEnrollmentDetails saveEnrollment = holder.getSaveEnrollmentDetails();
                try {

                    //String file1 = "/usr/share/enrollment/croppedimg/out.png";
                    String file1 = PropertyFile.getProperty(PropertyName.SUB_FILE);
                    //String file1 = "/usr/share/enrollment/croppedimg/sub.png"; //changed from out.png to sub.png
                    File outFile = new File(file1);
                    if (outFile.exists()) {
                        FileInputStream outputFile = null;

                        try {
                            outputFile = new FileInputStream(file1);
                        } catch (FileNotFoundException ex) {
                            updateUI("Output File not Found");
                            LOGGER.log(Level.INFO, "Output File not Found");
                            homeBtn.setDisable(false);
                            fetchArcBtn.setDisable(false);
                            progressIndicator.setVisible(false);
                        }

                        //String fileCompressed = "/usr/share/enrollment/croppedimg/compressed.png";
                        String fileCompressed = PropertyFile.getProperty(PropertyName.COMPRESS_FILE);

                        File compressedFile = new File(fileCompressed);
                        if (compressedFile.exists()) {
                            FileInputStream outputFileCompressed = null;
                            try {
                                //Added for Biometric Options
                                if (holder.getArcDetails().getBiometricOptions().contains("Biometric")) {
                                    saveEnrollment.setPhoto("Not Available");
                                    saveEnrollment.setEnrollmentStatus("PhotoCompleted");
                                    saveEnrollment.setPhotoCompressed("Not Available");
                                } else {
                                    outputFileCompressed = new FileInputStream(fileCompressed);
                                    finalBase64Img = Base64.getEncoder().encodeToString(outputFile.readAllBytes());
                                    saveEnrollment.setPhoto(finalBase64Img);
                                    saveEnrollment.setEnrollmentStatus("PhotoCompleted");
                                    saveEnrollment.setPhotoCompressed(Base64.getEncoder().encodeToString(outputFileCompressed.readAllBytes()));
                                    outputFileCompressed.close();
                                }

                            } catch (IOException ex) {
                                updateUI("Photo Output File Coversion Problem");
                                LOGGER.log(Level.INFO, "Photo Output File Coversion Problem");
                                homeBtn.setDisable(false);
                                fetchArcBtn.setDisable(false);
                                progressIndicator.setVisible(false);
                            }


                        } else {
                            updateUI("Compressed Photo file not Exist...");
                            LOGGER.log(Level.INFO, "Compressed Photo file not Exist...");
                            homeBtn.setDisable(false);
                            fetchArcBtn.setDisable(false);
                            progressIndicator.setVisible(false);
                        }

                    } else {
                        LOGGER.log(Level.INFO, "Photo Output file not Exist....Try Again");
                        updateUI("Photo Output file not Exist....");
                        homeBtn.setDisable(false);
                        fetchArcBtn.setDisable(false);
                        progressIndicator.setVisible(false);
                    }
                    saveEnrollment.setEnrollmentStationID(ServerAPI.getEnrollmentStationId());
                    saveEnrollment.setEnrollmentStationUnitID(ServerAPI.getEnrollmentStationUnitId());

                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.log(Level.INFO, "Problem reading UnitID from /etc/data file.");
                    homeBtn.setDisable(false);
                    fetchArcBtn.setDisable(false);
                    progressIndicator.setVisible(false);
                }


                try {

                    try {
                        System.out.println("Replacing Photo Out File");
                        FileUtils.copyFile(photoCaptureFile, photoOutFile);
                    } catch (IOException ex) {
                        Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    saveEnrollment.setEnrollmentStatus("SUCCESS");
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(System.currentTimeMillis());
                    saveEnrollment.setEnrollmentDate(formatter.format(date));
                    saveEnrollment.setArcStatus(arcDetails.getArcStatus());
                    saveEnrollment.setUniqueID(arcDetails.getApplicantID());//For ApplicantID
                    LOGGER.log(Level.INFO, "ARC STATUS::", arcDetails.getArcStatus());
                    LOGGER.log(Level.INFO, "Apllicant ID::", arcDetails.getApplicantID());
                    saveEnrollment.setBiometricOptions(arcDetails.getBiometricOptions());
                    LOGGER.log(Level.INFO, "Biometric Options::", arcDetails.getBiometricOptions());

                    if (holder.getArcDetails().getBiometricOptions().contains("Photo")) {
                        saveEnrollment.setIRISScannerSerailNo("Not Available");
                        saveEnrollment.setLeftFPScannerSerailNo("Not Available");
                        saveEnrollment.setRightFPScannerSerailNo("Not Available");
                        Set<FP> fingerPrintSet = new HashSet<>();
                        FP fplt = new FP();
                        fplt.setPosition("Not Available");
                        fplt.setTemplate("Not Available");
                        fplt.setImage("Not Available");
                        fingerPrintSet.add(fplt);
                        saveEnrollment.setFp(fingerPrintSet);
                        Set<IRIS> irisSet = new HashSet<>();
                        IRIS iris = new IRIS();
                        iris.setPosition("Not Available");
                        iris.setImage("Not Available");
                        iris.setTemplate("Not Available");
                        irisSet.add(iris);
                        saveEnrollment.setIris(irisSet);

                    }


                    holder.setSaveEnrollmentDetails(saveEnrollment);


                    ObjectReaderWriter objReadWrite = new ObjectReaderWriter();
                    objReadWrite.writer(saveEnrollment);
                    System.out.println("Save Enrollment Object write");
                    SaveEnrollmentDetails s = objReadWrite.reader();
                    System.out.println("Enrollment Status " + s.getEnrollmentStatus());


                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);

                    String postJson;
                    String connurl_arc = apiServerCheck.getArcUrl();
                    String arcno = "123abc";
                    String connectionStatus = APIServerCheck.checkGetARCNoAPI(connurl_arc, arcno);
                    System.out.println("connection status :" + connectionStatus);
                    if (!connectionStatus.contentEquals("connected")) {
                        try {

                            postJson = mapper.writeValueAsString(saveEnrollment);
                            String connurl = apiServerCheck.getArcUrl();

                            String oirgjsonfile = "/usr/share/enrollment/json/export/orig/" + saveEnrollment.getArcNo() + ".json";
                            String encryptedjsonfile = "/usr/share/enrollment/json/export/enc/" + saveEnrollment.getArcNo() + ".json.enc";
                            String decryptedjsonfile = "/usr/share/enrollment/json/export/dec/" + saveEnrollment.getArcNo() + ".json";
                            File json = new File(oirgjsonfile);
                            FileOutputStream output = new FileOutputStream(json);
                            output.write(postJson.getBytes());
                            output.close();


                            AESFileEncryptionDecryption aesencryptfile = new AESFileEncryptionDecryption();
                            String status = "";
                            try {
                                status = aesencryptfile.encryptFile(oirgjsonfile, encryptedjsonfile);
                                if (status.equals("Success")) {
                                    aesencryptfile.delFile(oirgjsonfile);
                                    System.out.println("Encrypted Sucessfully:::Original file deleted0");
                                    updateUI("Details Encrypted and saved locally");
                                    submitBtn.setDisable(true);
                                    progressIndicator.setVisible(false);
                                    System.out.println("Encrypted Sucessfully:::Original file deleted");
                                    DeleteSavedJsonFile deleteSavedJsonFile = new DeleteSavedJsonFile();
                                    deleteSavedJsonFile.delSavedfile();
                                } else {
                                    updateUI("Please Try Again...");
                                    System.out.println("Please Try Again...");
                                    submitBtn.setDisable(false);
                                    progressIndicator.setVisible(false);
                                }

                            } catch (InvalidKeySpecException ex) {
                                Logger.getLogger(CameraController.class.getName()).log(Level.SEVERE, null, ex);
                                updateUI("Exception:" + ex);
                                submitBtn.setDisable(false);
                                progressIndicator.setVisible(false);

                            } catch (InvalidAlgorithmParameterException ex) {
                                Logger.getLogger(CameraController.class.getName()).log(Level.SEVERE, null, ex);
                                updateUI("Exception:" + ex);
                                submitBtn.setDisable(false);
                                progressIndicator.setVisible(false);
                            }

                        } catch (Exception e) {
                            updateUI("Error Decrypting the JSON File" + e);
                            submitBtn.setDisable(false);
                            progressIndicator.setVisible(false);
                        }


                        homeBtn.setDisable(false);
                        fetchArcBtn.setDisable(false);
                        progressIndicator.setVisible(false);
                        return;
                    }


                    postJson = mapper.writeValueAsString(saveEnrollment);
                    String connurl = apiServerCheck.getEnrollmentSaveURL();
                    String decResponse = "";
                    decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
                    if (decResponse.contains("Exception:")) {
                        updateUI("Exception: From Server, Kindly Try Again");
                        homeBtn.setDisable(false);
                        fetchArcBtn.setDisable(false);
                        submitBtn.setDisable(false);
                        progressIndicator.setVisible(false);
                        return;
                    }
                    String arcNo = saveEnrollment.getArcNo();

                    ObjectMapper objMapper = new ObjectMapper();
                    saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                    LOGGER.log(Level.INFO, "save enrollment : " + saveEnrollmentResponse.toString());

                    String status = saveEnrollmentResponse.getDesc();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (saveEnrollmentResponse.getErrorCode().equals("0")) {
                                DeleteSavedJsonFile deleteSavedJsonFile = new DeleteSavedJsonFile();
                                deleteSavedJsonFile.delSavedfile();
                                updateUI(status);

                            } else {
                                updateUI(status);
                                LOGGER.log(Level.INFO, "Status:" + status);
                                homeBtn.setDisable(false);
                                fetchArcBtn.setDisable(false);
                                progressIndicator.setVisible(false);

                            }
                        }
                    });


                    if (saveEnrollmentResponse.getDesc().contains("refused") || saveEnrollmentResponse.getDesc().contains("notreachable")
                            || saveEnrollmentResponse.getDesc().contains("Exception")) {
                        InputStream inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/redcross.png");
                        Image image = new Image(inputStream);
                        statusImageView.setImage(image);
                        submitBtn.setDisable(false);
                        progressIndicator.setVisible(false);
                    } else {
                        InputStream inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/tickgreen.jpg");
                        Image image = new Image(inputStream);
                        statusImageView.setImage(image);
                        progressIndicator.setVisible(false);
                        homeBtn.setDisable(false);
                        fetchArcBtn.setDisable(false);
                    }

                } catch (Exception e) {
                    messageLabel.setText("Exception Thrown by the Server, Try Again");
                    LOGGER.log(Level.INFO, e + "Exception block");
                    homeBtn.setDisable(false);
                    fetchArcBtn.setDisable(false);
                    submitBtn.setDisable(false);
                }
            } catch (Exception e) {
                System.out.println("Exception:" + e);
            }
        }
    };

    public void initialize() {
        updateUI("Please Click \'Submit\' Button and wait....");
    }

    private void disableControl(Node... nodes) {
        Platform.runLater(() -> {
            for (Node node : nodes) {
                node.setDisable(true);
            }
        });
    }

    private void enableControl(Node... nodes) {
        Platform.runLater(() -> {
            for (Node node : nodes) {
                node.setDisable(false);
            }
        });
    }

}

 