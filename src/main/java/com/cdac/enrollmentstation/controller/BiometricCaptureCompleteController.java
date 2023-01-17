/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import RealScan.FP;
import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.model.IRIS;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.service.ObjectReaderWriter;
import com.cdac.enrollmentstation.util.DeleteSavedJsonFile;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author root
 */
public class BiometricCaptureCompleteController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML
    public Label statusMessage;

    @FXML
    private ImageView statusImg;

    @FXML
    private Button submit;

    @FXML
    private Button backhome;

    @FXML
    private Button fetcharc;

    @FXML
    private ProgressIndicator progressind;

    public String finalBase64Img;

    Thread tRight = null;

    public SaveEnrollmentResponse saveEnrollmentResponse;

    public APIServerCheck apiServerCheck = new APIServerCheck();

    TestProp prop = new TestProp();

    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(BiometricCaptureCompleteController.class);

    //Thread
    Thread pi = null;


    @FXML
    private void homescreen() {

        try {
            App.setRoot("first_screen");
        } catch (IOException ex) {
            Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException");
        }
    }

    @FXML
    private void fetcharcpage() {

        try {
            App.setRoot("enrollment_arc");
        } catch (IOException ex) {
            Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException");
        }
    }


    public void messageStatus(String message) {
        statusMessage.setText(message);
    }

    public void statusMsg(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //System.out.println("Capture Status Called...");
                //LOGGER.log(Level.INFO,"Capture Status Called...");
                statusMessage.setText(message);
            }
        });
    }

    Runnable ShowProgressInd = new Runnable() {
        @Override
        public void run() {
            try {
                progressind.setVisible(true);
                statusMsg("Please wait...");

                //Removing the Old Photo while capturing Photo
                String photocapturefileprop = prop.getProp().getProperty("photocaptureimg");
                //String file1 = "/usr/share/enrollment/croppedimg/sub.png"; //changed from out.png to sub.png
                File photocapturefile = new File(photocapturefileprop);

                String fileoutput = prop.getProp().getProperty("outputfile");
                //String file1 = "/usr/share/enrollment/croppedimg/sub.png"; //changed from out.png to sub.png
                File photooutfile = new File(fileoutput);


                ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                ARCDetails a = holder.getArcDetails();
                SaveEnrollmentDetails saveEnrollment = holder.getSaveEnrollmentDetails();
                System.out.println("details : " + saveEnrollment.getArcNo());

                try {

                    //String file1 = "/usr/share/enrollment/croppedimg/out.png";
                    //Changed For sending sub.png (18-10-22)
                    //String file1 = prop.getProp().getProperty("outputfile");
                    String file1 = prop.getProp().getProperty("subfile");
                    //String file1 = "/usr/share/enrollment/croppedimg/sub.png"; //changed from out.png to sub.png
                    File outFile = new File(file1);
                    if (outFile.exists()) {
                        FileInputStream outputFile = null;

                        try {
                            outputFile = new FileInputStream(file1);
                        } catch (FileNotFoundException ex) {
                            //Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                            //messageStatus("Output File not Found");
                            statusMsg("Output File not Found");
                            LOGGER.log(Level.INFO, "Output File not Found");
                            backhome.setDisable(false);
                            fetcharc.setDisable(false);
                            progressind.setVisible(false);
                        }

                        //String fileCompressed = "/usr/share/enrollment/croppedimg/compressed.png";
                        String fileCompressed = prop.getProp().getProperty("compressfile");

                        File compressedFile = new File(fileCompressed);
                        if (compressedFile.exists()) {
                            FileInputStream outputFileCompressed = null;
                /*
                try {
                    
                } catch (FileNotFoundException ex) {
                    //Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                    messageStatus("Output File not Found");
                    LOGGER.log(Level.INFO,"Output File not Found");
                    backhome.setDisable(false);
                }*/
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
                                //Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                                //messageStatus("Photo Output File Coversion Problem");
                                statusMsg("Photo Output File Coversion Problem");
                                LOGGER.log(Level.INFO, "Photo Output File Coversion Problem");
                                backhome.setDisable(false);
                                fetcharc.setDisable(false);
                                progressind.setVisible(false);
                            }
                            //Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);


                        } else {
                            //System.out.println("Problem reading compressed  file.");
                            //messageStatus("Compressed Photo file not Exist...");
                            statusMsg("Compressed Photo file not Exist...");
                            LOGGER.log(Level.INFO, "Compressed Photo file not Exist...");
                            backhome.setDisable(false);
                            fetcharc.setDisable(false);
                            progressind.setVisible(false);
                        }
                        //this.finalBase64Img = Base64.getEncoder().encodeToString(outputFile.readAllBytes());

                    } else {
                        //System.out.println("Problem reading Out file.");
                        LOGGER.log(Level.INFO, "Photo Output file not Exist....Try Again");
                        //messageStatus("Photo Output file not Exist....");
                        statusMsg("Photo Output file not Exist....");
                        backhome.setDisable(false);
                        fetcharc.setDisable(false);
                        progressind.setVisible(false);
                        //statusMessage.setText("Problem reading Out Image file...");
                    }

                    try (BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"))) {
                        String line = file.lines().collect(Collectors.joining());
                        String input = " ";
                        String[] tokens = line.split(",");
                        saveEnrollment.setEnrollmentStationID(tokens[2]);
                        saveEnrollment.setEnrollmentStationUnitID(tokens[0]);
                        file.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        //System.out.println("Problem reading file.");
                        LOGGER.log(Level.INFO, "Problem reading UnitID from /etc/data file.");
                        backhome.setDisable(false);
                        fetcharc.setDisable(false);
                        progressind.setVisible(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        //System.out.println("Problem reading file.");
                        LOGGER.log(Level.INFO, "Problem reading UnitID from /etc/data file.");
                        backhome.setDisable(false);
                        fetcharc.setDisable(false);
                        progressind.setVisible(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //System.out.println("Problem reading file.");
                    LOGGER.log(Level.INFO, "Problem reading UnitID from /etc/data file.");
                    backhome.setDisable(false);
                    fetcharc.setDisable(false);
                    progressind.setVisible(false);
                }


                try {

                    //Removing the Old Photo while capturing Photo
                    try {
                        System.out.println("Replacing Photo Out File");
                        FileUtils.copyFile(photocapturefile, photooutfile);
                    } catch (IOException ex) {
                        Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //saveEnrollment.setEnrollmentStationID("StationID");
                    //saveEnrollment.setEnrollmentStationUnitID("UnitID");
                    saveEnrollment.setEnrollmentStatus("SUCCESS");
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(System.currentTimeMillis());
                    saveEnrollment.setEnrollmentDate(formatter.format(date));
                    saveEnrollment.setArcStatus(a.getArcStatus());
                    saveEnrollment.setUniqueID(a.getApplicantID());//For ApplicantID
                    //System.out.println("ARC STATUS::"+a.getArcstatus());
                    //System.out.println("Apllicant ID::"+a.getApplicantID());
                    LOGGER.log(Level.INFO, "ARC STATUS::", a.getArcStatus());
                    LOGGER.log(Level.INFO, "Apllicant ID::", a.getApplicantID());
                    saveEnrollment.setBiometricOptions(a.getBiometricOptions());
                    LOGGER.log(Level.INFO, "Biometric Options::", a.getBiometricOptions());
                    //Added for Biometric Options

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
                    //Added for Biometric Options


                    holder.setSaveEnrollmentDetails(saveEnrollment);


                    //Code Added by K. Karthikeyan - 18-4-22 - Start
                    ObjectReaderWriter objReadWrite = new ObjectReaderWriter();
                    objReadWrite.writer(saveEnrollment);
                    System.out.println("Save Enrollment Object write");
                    SaveEnrollmentDetails s = objReadWrite.reader();
                    System.out.println("Enrollment Status " + s.getEnrollmentStatus());
                    //Code Added by K. Karthikeyan - 18-4-22 - Finish


                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
       /* 
        Platform.runLater(new Runnable() {
                @Override public void run() {
                    System.out.println("In process1...");
                    //statusMessage.setText("In process1...");
                     messageStatus("In process....");
                     LOGGER.log(Level.INFO,"In process1...");
                }
        });*/
                    //statusMessage.setText("In process...");

                    String postJson;
                    String connurl_arc = apiServerCheck.getArcUrl();
                    String arcno = "123abc";
                    String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl_arc, arcno);
                    System.out.println("connection status :" + connectionStatus);
                    if (!connectionStatus.contentEquals("connected")) {
                        try {

                            postJson = mapper.writeValueAsString(saveEnrollment);
                            String connurl = apiServerCheck.getArcUrl();
                            //String arcno = "123abc";
                            //String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl,arcno);
                            //System.out.println("connection status :"+connectionStatus);

                            String oirgjsonfile = "/usr/share/enrollment/json/export/orig/" + saveEnrollment.getArcNo() + ".json";
                            String encryptedjsonfile = "/usr/share/enrollment/json/export/enc/" + saveEnrollment.getArcNo() + ".json.enc";
                            String decryptedjsonfile = "/usr/share/enrollment/json/export/dec/" + saveEnrollment.getArcNo() + ".json";
                            //System.out.println("post json final :"+ postJson);
                            File json = new File(oirgjsonfile);
                            FileOutputStream output = new FileOutputStream(json);
                            output.write(postJson.getBytes());
                            output.close();

                            //Encrypt the Orig Json File

                            AESFileEncryptionDecryption aesencryptfile = new AESFileEncryptionDecryption();
                            String status = "";
                            try {
                                status = aesencryptfile.encryptFile(oirgjsonfile, encryptedjsonfile);
                                //ARCDetails arcDetail = new ARCDetails();
                                //holder.setARC(arcDetail);
                                if (status.equals("Success")) {
                                    aesencryptfile.delFile(oirgjsonfile);
                                    //arcDetail.setDesc("Details Encrypted and saved locally");
                                    //messageStatus("Details Encrypted and saved locally");
                                    System.out.println("Encrypted Sucessfully:::Original file deleted0");
                                    statusMsg("Details Encrypted and saved locally");
                                    submit.setDisable(true);
                                    progressind.setVisible(false);
                                    System.out.println("Encrypted Sucessfully:::Original file deleted");
                                    //Delete Saved JSON File
                                    DeleteSavedJsonFile deleteSavedJsonFile = new DeleteSavedJsonFile();
                                    deleteSavedJsonFile.delSavedfile();
                                } else {
                                    //arcDetail.setDesc("Please Try Again...");
                                    //messageStatus("Please Try Again...");
                                    statusMsg("Please Try Again...");
                                    System.out.println("Please Try Again...");
                                    submit.setDisable(false);
                                    progressind.setVisible(false);
                                }

                            } catch (InvalidKeySpecException ex) {
                                Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                                statusMsg("Exception:" + ex);
                                submit.setDisable(false);
                                progressind.setVisible(false);

                            } catch (InvalidAlgorithmParameterException ex) {
                                Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                                statusMsg("Exception:" + ex);
                                submit.setDisable(false);
                                progressind.setVisible(false);
                            }

                        } catch (Exception e) {
                            statusMsg("Error Decrypting the JSON File" + e);
                            submit.setDisable(false);
                            progressind.setVisible(false);
                        }


                        backhome.setDisable(false);
                        fetcharc.setDisable(false);
                        progressind.setVisible(false);
                        return;
                    }


                    postJson = mapper.writeValueAsString(saveEnrollment);
                    String connurl = apiServerCheck.getEnrollmentSaveURL();
                    String decResponse = "";
                    decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
                    if (decResponse.contains("Exception:")) {
                        statusMsg("Exception: From Server, Kindly Try Again");
                        backhome.setDisable(false);
                        fetcharc.setDisable(false);
                        submit.setDisable(false);
                        progressind.setVisible(false);
                        return;
                    }
                    //saveEnrollment.setEnrollmentStatus("submitted");
                    String arcNo = saveEnrollment.getArcNo();
                    //Deleting current enrollment file
              /*
               String objFilePath = "/tmp/saveEnrollment.txt";
                File f = new File(objFilePath);
                if(f.delete()){
                    System.out.println("The enrollment object is deleted, and the ARC no is : "+arcNo);
                } else {
                    System.out.println("Failed to delete the file");
                }*/
                    ObjectMapper objMapper = new ObjectMapper();
                    saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                    //System.out.println(" save enrollment : "+saveEnrollmentResponse.toString());
                    LOGGER.log(Level.INFO, "save enrollment : " + saveEnrollmentResponse.toString());
            /*
            a.setDesc(saveEnrollmentResponse.getDesc());
            holder.setARC(a);
            System.out.println("ARC details :" +a.toString());
            
            
            Platform.runLater(new Runnable() {
            @Override public void run() {
            //System.out.println("Capture completed");
            LOGGER.log(Level.INFO,"Capture completed");
            //statusMessage.setText("Capture completed");
            backhome.setDisable(false);
            submit.setDisable(true);
            messageStatus("Capture completed");
               }
            });
                
            holder = ARCDetailsHolder.getArcDetailsHolder();
            a= holder.getARC();
            String status = a.getDesc();
            */
                    String status = saveEnrollmentResponse.getDesc();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (saveEnrollmentResponse.getErrorCode().equals("0")) {
                                //Delete Saved JSON File
                                DeleteSavedJsonFile deleteSavedJsonFile = new DeleteSavedJsonFile();
                                deleteSavedJsonFile.delSavedfile();
                                statusMsg(status);
                    /*
                    try {
                        FileUtils.copyFile(photocapturefile, photooutfile);
                    } catch (IOException ex) {
                        Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
                    }*/
                            } else {
                                statusMsg(status);
                                LOGGER.log(Level.INFO, "Status:" + status);
                                backhome.setDisable(false);
                                fetcharc.setDisable(false);
                                progressind.setVisible(false);
                    /*
                    try {
                        FileUtils.copyFile(photocapturefile, photooutfile);
                    } catch (IOException ex) {
                        Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
                    }*/
                            }
                        }
                    });


                    //if(a.getDesc().contains("refused") || a.getDesc().contains("notreachable")) {
                    if (saveEnrollmentResponse.getDesc().contains("refused") || saveEnrollmentResponse.getDesc().contains("notreachable")
                            || saveEnrollmentResponse.getDesc().contains("Exception")) {
                        InputStream inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/redcross.png");
                        Image image = new Image(inputStream);
                        statusImg.setImage(image);
                        submit.setDisable(false);
                        progressind.setVisible(false);
                    } else {
                        InputStream inputStream = BiometricCaptureCompleteController.class.getResourceAsStream("/img/tickgreen.jpg");
                        Image image = new Image(inputStream);
                        statusImg.setImage(image);
                        progressind.setVisible(false);
                        backhome.setDisable(false);
                        fetcharc.setDisable(false);
                    }

                } catch (Exception e) {
                    //System.out.println("Exception block"+e);
                    statusMessage.setText("Exception Thrown by the Server, Try Again");
                    LOGGER.log(Level.INFO, e + "Exception block");
                    backhome.setDisable(false);
                    fetcharc.setDisable(false);
                    submit.setDisable(false);
                    //messageStatus("Exception Thrown by the Server, Try Again");
                }
            } catch (Exception e) {
                System.out.println("Exception:" + e);
            }
        }
    };

    public void submitData() {

        submit.setDisable(true);

        try {
            pi = new Thread(ShowProgressInd);
            pi.start();
        } catch (Exception e) {
            //System.out.println("Error in loop::"+e);
            LOGGER.log(Level.INFO, "Error in loop::" + e);
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //this.handler = appLog.getLogger();
        //LOGGER.addHandler(handler); 
        // TODO
        //System.out.println(" init :");
        //statusMessage.setText("Please Click \'Submit\' Button and wait....");
        messageStatus("Please Click \'Submit\' Button and wait....");
        LOGGER.log(Level.INFO, "Please Click \'Submit\' Button and wait....");
    }

}

 