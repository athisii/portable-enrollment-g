/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.CardReaderAPI;
import com.cdac.enrollmentstation.api.CardReaderAPIURLs;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AuthUtil;
import com.cdac.enrollmentstation.security.HextoASNFormat;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mantra.midfingerauth.DeviceInfo;
import com.mantra.midfingerauth.MIDFingerAuth;
import com.mantra.midfingerauth.MIDFingerAuth_Callback;
import com.mantra.midfingerauth.enums.DeviceDetection;
import com.mantra.midfingerauth.enums.DeviceModel;
import com.mantra.midfingerauth.enums.TemplateFormat;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * FXML Controller class
 *
 * @author padmanabhanj
 */
public class CardLoginController implements MIDFingerAuth_Callback {

    /**
     * Initializes the controller class.
     */

    @FXML
    public Button showContractDetails;

    @FXML
    public Button show_home_token;


    @FXML
    private Label statusMsg;

    @FXML
    private PasswordField usertxt;

    @FXML
    private ImageView m_FingerPrintImage;


    CardReaderAPIURLs CardReaderAPIURLs = new CardReaderAPIURLs();
    CardReaderAPI cardReaderAPI = new CardReaderAPI();
    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(CardLoginController.class);
    private MIDFingerAuth midFingerAuth = null; // For MID finger jar
    private DeviceInfo deviceInfo = null;
    private byte[] lastCaptureTemplat;
    int fingerprintinit;
    int minQuality = 60;
    int timeout = 10000;
    int fpQuality = 96;
    String fpquality = null;
    String response = "";

    public CardLoginController() {
        //this.handler = appLog.getLogger();
        //LOGGER.addHandler(handler); 


        //MIDfinger
        midFingerAuth = new MIDFingerAuth(this);
        String version = midFingerAuth.GetSDKVersion();
        try {
            LOGGER.log(Level.INFO, "JAVA_VERSION:" + System.getProperty("java.version"));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception:" + e);
        }
        //MIDfinger
        List<String> deviceList = new ArrayList<String>();
        int ret = midFingerAuth.GetConnectedDevices(deviceList);
        if (ret != 0) {
            LOGGER.log(Level.INFO, "supported devices:" + midFingerAuth.GetErrorMessage(ret));
            return;
        }
        for (int i = 0; i < deviceList.size(); i++) {
            LOGGER.log(Level.INFO, "deviceList:" + deviceList.get(i));
        }

        String model = "MFS100";
        boolean isDeviceConnected = midFingerAuth.IsDeviceConnected(DeviceModel.valueFor(deviceList.get(0)));
        if (isDeviceConnected) {
            LOGGER.log(Level.INFO, "Device is connected...");
        } else {
            LOGGER.log(Level.INFO, "Device is not connected...");
            return;
        }

        DeviceInfo info = new DeviceInfo();
        int fingerprintinit = midFingerAuth.Init(DeviceModel.valueFor(model), info);
        if (fingerprintinit != 0) {
            LOGGER.log(Level.INFO, "Device Initialization not success" + midFingerAuth.GetErrorMessage(fingerprintinit));
            return;
        }

        if (fingerprintinit == 0) {
            deviceInfo = info;
            LOGGER.log(Level.INFO, "Width: " + String.valueOf(deviceInfo.Width));
            LOGGER.log(Level.INFO, "Height: " + String.valueOf(deviceInfo.Height));
            return;
        } else {
            LOGGER.log(Level.INFO, "Error Single Fingerprint reader not initialized");
            return;
        }
    }

    public void initialize() {
        /* add ChangeListner to TextField to restrict the TextField Length*/
        usertxt.textProperty().addListener((observable, oldValue, newValue) -> AuthUtil.limitCharacters(usertxt, oldValue, newValue));
        // TODO - need to implement
        /*
        usertxt.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                // TODO - should call 'login button' action here too
                event.consume();
            }
        });
         */
    }

    @FXML
    private void showHome() throws IOException {
        //App.setRoot("main_screen");
        App.setRoot("login");

    }

    public void messageStatus(String message) {
        statusMsg.setText(message);
    }

    public void responseStatus(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                statusMsg.setText(message);
            }
        });
    }


    @FXML
    public void readCardDetails() throws MalformedURLException, IOException, IllegalStateException {
        String response = "";
    /* 
    if(usertxt.getText().isEmpty()){
        response="Kindly Enter the Card Pin";       
        LOGGER.log(Level.INFO, "Inside User Text");
        messageStatus(response);         
        return;
    }*/

        response = readCard();
        if (response.equals("success")) {
            //lblcarderror.setText("Kindly Check the CardReader Api Service");
            // App.setRoot("list_contract");
            //App.setRoot("main_screen");
            return;
        } else {
            //messageStatus(response);
            responseStatus(response);
            return;
        }

    }

    @FXML
    public void CaptureActionPerformed() {
        String response = "";
     
   /* if(usertxt.getText().isEmpty()){
        response="Kindly Enter the Card Pin";
        System.out.println("Inside User Text");
        messageStatus(response);         
        return;
     }*/

        response = CaptureSingleFingerprint();
        if (response.equals("success")) {
            //lblcarderror.setText("Kindly Check the CardReader Api Service");
            // App.setRoot("list_contract");
            //App.setRoot("main_screen");
            return;
        } else {
            messageStatus(response);
        }

    }

    public String CaptureSingleFingerprint() {
        String response = "";
        if (fingerprintinit != 0) {
            String model = "MFS100";
            DeviceInfo info = new DeviceInfo();
            int fingerprintinit = midFingerAuth.Init(DeviceModel.valueFor(model), info);
            //System.out.println("FingerPri"+fingerprintinit);
            LOGGER.log(Level.INFO, "fingerprintinit::" + fingerprintinit);
        }

        int retCapture = midFingerAuth.StartCapture(minQuality, timeout);
        LOGGER.log(Level.INFO, "Start Capture Return::" + retCapture);
        if (retCapture != 0) {
            LOGGER.log(Level.INFO, "Start Capture Error:" + midFingerAuth.GetErrorMessage(retCapture));
            //messageStatus("Start Capture Error:"+midFingerAuth.GetErrorMessage(retCapture));
            response = "not Captured";
            return response;
        }
        return response;
    }

    public StringBuffer getReadCardValue(byte[] whichdata, int offset, int reqlength, int addlength, int handleValue) {

        StringBuffer responsebuffer = new StringBuffer("");
        String response;
        ArrayList<String> responseReadDataFromNavalcard = new ArrayList<String>();
        for (int i = 0; i <= 2; i++) {
            // System.out.println("Inside For Loop");
            responseReadDataFromNavalcard.add(cardReaderAPI.readDataFromNaval(handleValue, whichdata, offset, reqlength));
            offset = offset + addlength;
        }

        ArrayList<String> responseString = new ArrayList<String>();
        //String decodedResponseString = "";
        StringBuffer decodedResponseString = new StringBuffer("");
        byte[] decodedDatafromNaval;
        String decodedStringFromNaval;
        ObjectMapper objReadDataFromNaval = new ObjectMapper();
        CardReaderReadData readDataFromNaval = new CardReaderReadData();

        for (String responseReadDataArray : responseReadDataFromNavalcard) {
            System.out.println(responseReadDataArray);
            try {
                //Commented on 270522 for code review
                //ObjectMapper objReadDataFromNaval = new ObjectMapper();
                //CardReaderReadData readDataFromNaval = objReadDataFromNaval.readValue(responseReadDataArray, CardReaderReadData.class);
                readDataFromNaval = objReadDataFromNaval.readValue(responseReadDataArray, CardReaderReadData.class);
            } catch (JsonProcessingException ex) {
                Logger.getLogger(CardLoginController.class.getName()).log(Level.SEVERE, null, ex);
                response = "JSON Prossessing Error CardReaderReadData";

                return responsebuffer;
            }

            if (readDataFromNaval.getRetVal() == 0) {
                LOGGER.log(Level.INFO, "Read Data from card Connect succes");
                response = "success";
                //Decode Data from Naval Card
                decodedDatafromNaval = Base64.getDecoder().decode(readDataFromNaval.getResponse());

                decodedStringFromNaval = DatatypeConverter.printHexBinary(decodedDatafromNaval);
                //decodedResponseString = decodedResponseString+decodedStringFromNaval;
                decodedResponseString.append(decodedStringFromNaval);
            } else {
                LOGGER.log(Level.INFO, "Read Data from card Connect Failure");
                //lblcarderror.setText("Error While Reading the Card, Try with other Card");
                //response = "Error While Reading the Card, Try with other Card";
                return decodedResponseString;
            }

        }


        return decodedResponseString;
    }


    public String readCard() {
        CardReaderInitialize cardReaderInitialize;
        CardReaderWaitForConnect waitForConnect;
        CardReaderSelectApp selectApp;
        String response = "";
        String MantraCardReader = "Mantra Reader (1.00) 00 00";
        String ACSCardReader = "ACS ACR1281 1S Dual Reader 00 01";

        //String[] listofreaders = cardReaderAPI.listofreaders();
        /*
        if(listofreaders == null ){
            response = "Kindly Check the Card Service running or not";
            //lblcarderror.setText("Kindly Check the Card Service running or not");
            return response;
        }
        
        if(listofreaders.length < 1 ){
            //lblcarderror.setText("Kindly Check the Both Card readers connected");
            response = "Kindly Check the Card is Connected to the Device";
            return response;
        }
        */
        String dintializeresponse = cardReaderAPI.deInitialize();
        System.out.println("dintializeresponse::" + dintializeresponse);
        //if (dintializeresponse.equals("")){
        if (dintializeresponse.isEmpty() || dintializeresponse.contains("Exception")) {
            response = "Kindly Check the CardReader Api Service";
            //lblcarderror.setText("Kindly Check the CardReader Api Service");
            return response;
        }
        String responseinit = cardReaderAPI.initialize();
        if (responseinit.equals("")) {
            //lblcarderror.setText("Kindly Check the CardReader Api Service");
            response = "Kindly Check the CardReader Api Service";
            return response;
        }
        ObjectMapper objMapper = new ObjectMapper();

        try {
            cardReaderInitialize = objMapper.readValue(responseinit, CardReaderInitialize.class);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(CardLoginController.class.getName()).log(Level.SEVERE, null, ex);
            response = "JSON Prossessing Error cardReaderInitialize";
            return response;

        }
        System.out.println("card init" + cardReaderInitialize.toString());
        if (cardReaderInitialize.getRetVal() == 0) {
            //String waitConnStatus = cardReaderAPI.getWaitConnectStatus(listofreaders[0].trim());
            //String waitConnStatus = cardReaderAPI.getWaitConnectStatus(ACSCardReader);
            String waitConnStatus = cardReaderAPI.getWaitConnectStatus(MantraCardReader);
            System.out.println("connection status :" + waitConnStatus);
            if (!waitConnStatus.contentEquals("connected")) {
                //lblcarderror.setText(connectionStatus);
                response = "Kindly Check the CardReader Api Service";
                return response;
                //lblcarderror.setText("Kindly Check the CardReader Api Service");;
                //return;
            } else {
                //String responseWaitConnect = cardReaderAPI.getWaitConnect(listofreaders[0].trim());
                //String responseWaitConnect = cardReaderAPI.getWaitConnect(ACSCardReader);
                String responseWaitConnect = cardReaderAPI.getWaitConnect(MantraCardReader);
                System.out.println("response Wait For Connect " + responseWaitConnect);

                ObjectMapper objMapperWaitConn = new ObjectMapper();

                try {
                    waitForConnect = objMapperWaitConn.readValue(responseWaitConnect, CardReaderWaitForConnect.class);
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(CardLoginController.class.getName()).log(Level.SEVERE, null, ex);
                    response = "JSON Prossessing Error CardReaderWaitforConnect";
                    return response;
                }
                if (waitForConnect.getRetVal() == 0) {
                    // System.out.println("Wait for conect succes");
                    LOGGER.log(Level.INFO, "Wait for conect succes");

                    //Get CSN and handle Value
                    //base 64 encoded bytes
                    String csnValue = waitForConnect.getCsn();
                    int handleValue = waitForConnect.getHandle();
                    LOGGER.log(Level.INFO, "CSN Value:" + csnValue);
                    LOGGER.log(Level.INFO, "Handle Value:" + handleValue);
                    HextoASNFormat hextoasn = new HextoASNFormat();
                    String decodedCsnValue = hextoasn.getDecodedCSN(csnValue);
                    System.out.println("Decoded Csn Value::::" + decodedCsnValue);
                    System.out.println("CSN Value::::" + csnValue);
                    System.out.println("Handle Value::::" + handleValue);

                    //Naval ID/Contractor Card value is 4 , For Token the value is 5
                    byte[] cardtype = {4};


                    String responseSelectApp = cardReaderAPI.getSelectApp(cardtype, handleValue);
                    ObjectMapper objMapperSelectApp = new ObjectMapper();

                    try {
                        selectApp = objMapperSelectApp.readValue(responseSelectApp, CardReaderSelectApp.class);
                    } catch (JsonProcessingException ex) {
                        Logger.getLogger(CardLoginController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "JSON Prossessing Error CardReaderSelectApp";
                        return response;
                    }

                    if (selectApp.getRetVal() == 0) {
                        System.out.println("Select App Connect succes");
                     
                       /*
                       //Card Reading 
                       byte[] whichdata = {21}; //static data
                       int offset = 0;
                       //int reqlength = 122;                                 
                       //int addlength = 122;
                       int reqlength = 1024;                                 
                       int addlength = 1024;
                       ArrayList<String> responseReadDataFromNavalcard = new ArrayList<String>();
                       for(int i=0;i<=2;i++){      
                            // System.out.println("Inside For Loop");                        
                            responseReadDataFromNavalcard.add(cardReaderAPI.readDataFromNaval(handleValue, whichdata, offset, reqlength));                                     
                            offset = offset+addlength;                                    
                       }

                       ArrayList<String> responseString = new ArrayList<String>();
                       //String decodedResponseString = "";
                       StringBuffer decodedResponseString = new StringBuffer("");
                       byte[] decodedDatafromNaval;
                       String decodedStringFromNaval;
                       ObjectMapper objReadDataFromNaval = new ObjectMapper();
                       CardReaderReadData readDataFromNaval = new CardReaderReadData();
                       for(String responseReadDataArray: responseReadDataFromNavalcard ) {
                           System.out.println(responseReadDataArray);
                           try {
                               //Commented on 270522 for code review
                               //ObjectMapper objReadDataFromNaval = new ObjectMapper();
                               //CardReaderReadData readDataFromNaval = objReadDataFromNaval.readValue(responseReadDataArray, CardReaderReadData.class);
                               readDataFromNaval = objReadDataFromNaval.readValue(responseReadDataArray, CardReaderReadData.class);
                           } catch (JsonProcessingException ex) {
                               Logger.getLogger(CardAuthenticationController.class.getName()).log(Level.SEVERE, null, ex);
                               response = "JSON Prossessing Error CardReaderReadData";
                               return response;
                           }

                            if(readDataFromNaval.getRetval() == 0){     
                                 System.out.println("Read Data from card Connect succes");
                               //Decode Data from Naval Card
                              decodedDatafromNaval = Base64.getDecoder().decode(readDataFromNaval.response);    
                              
                              decodedStringFromNaval = DatatypeConverter.printHexBinary(decodedDatafromNaval);
                              //decodedResponseString = decodedResponseString+decodedStringFromNaval; 
                              decodedResponseString.append(decodedStringFromNaval);
                            }else{
                                System.out.println("Read Data from card Connect Failure");
                                //lblcarderror.setText("Error While Reading the Card, Try with other Card");                                         
                                //response = "Error While Reading the Card, Try with other Card";
                                //return response;
                            }

                       }
                       */
                        StringBuffer decodedResponseStringForStaticDetails = new StringBuffer("");
                        StringBuffer decodedResponseStringForFingerprintDetails = new StringBuffer("");
                        byte[] whichdata_static = {21}; //static data
                        byte[] whichdata_fingerprint = {25}; //fingerprint data
                        int offset = 0;
                        //int reqlength = 122;
                        //int addlength = 122;
                        int reqlength = 1024;
                        int addlength = 1024;
                        decodedResponseStringForStaticDetails = getReadCardValue(whichdata_static, offset, reqlength, addlength, handleValue);
                        decodedResponseStringForFingerprintDetails = getReadCardValue(whichdata_fingerprint, offset, reqlength, addlength, handleValue);
                        String contractorId = "";
                        String contractorName = "";
                        if (decodedResponseStringForStaticDetails.length() > 0) {
                            System.out.println("DECODED RESPONSE STRING STATIC::::" + decodedResponseStringForStaticDetails);
                            System.out.println("DECODED RESPONSE STRING FINGERPRINT::::" + decodedResponseStringForFingerprintDetails);
                            contractorId = hextoasn.getContractorIdfromASN(decodedResponseStringForStaticDetails.toString());
                            contractorName = hextoasn.getContractorNamefromASN(decodedResponseStringForStaticDetails.toString());
                            System.out.println("Contractor ID:::::" + contractorId);
                            System.out.println("Contractor Unit Id:::::" + contractorName);
                        } else {
                            response = "Error While Reading the Card, Try with other Card";
                            return response;
                        }
                      /*
                       if(decodedResponseStringForFingerprintDetails.length() > 0){
                          System.out.println("DECODED RESPONSE STRING::::"+decodedResponseStringForFingerprintDetails);
                          contractorId = hextoasn.getFingerprintfromASN(decodedResponseStringForFingerprintDetails.toString()); 
                       //contractorName = hextoasn.getContractorNamefromASN(decodedResponseStringForFingerprintDetails.toString()); 
                       System.out.println("Contractor ID11111:::::"+contractorId); 
                      }else{
                               response = "Error While Reading the Card, Try with other Card";
                               return response;
                      }*/
                      
                      /*
                       System.out.println("DECODED RESPONSE STRING::::"+decodedResponseString);
                       String contractorId = hextoasn.getContractorIdfromASN(decodedResponseString.toString()); 
                       String contractorName = hextoasn.getContractorNamefromASN(decodedResponseString.toString()); 
                       System.out.println("Contractor ID:::::"+contractorId); */

                        //Set the Contractor Id and Card Serial Number (CSN)
                        ContractorDetail contractorDetail = new ContractorDetail();
                        contractorDetail.setContractorId(contractorId.trim());
                        contractorDetail.setContractorName(contractorName);
                        //contactdetail.setSerial_no(decodedCsnValue.toLowerCase());
                        contractorDetail.setSerialNo(decodedCsnValue);
                        contractorDetail.setCardReaderHandle(handleValue);

                        //Details details = Details.getdetails();
                        //details.setContractdetail(contactdetail);
                        System.out.println("Details from Show Token:::" + contractorDetail.getContractorId());
                        System.out.println("Details from Show Token:::" + contractorDetail.getSerialNo());


                        String contractorID = contractorDetail.getContractorId();
                        String serialNo = contractorDetail.getSerialNo();
                        if (contractorID != null && !contractorID.isEmpty()) {
                            //App.setRoot("list_contract");
                            System.out.println("Inside Card read Details");
                            if (usertxt.getText().isEmpty()) {
                                response = "Kindly Enter the Card PNo";
                                LOGGER.log(Level.INFO, "Inside User Text");
                                messageStatus(response);
                                return response;
                            }
                            if (contractorID.equals(usertxt.getText())) {
                                try {
                                    System.out.println("Equals ContractID");
                                    response = "success";
                                    App.setRoot("main_screen");
                                    return response;
                                   /* To Check UnitId against the Card
                                    if(contractorName.equals(apiServerCheck.getUnitID().trim())){
                                        response="success";
                                        App.setRoot("main_screen");
                                        return response;
                                    }else{
                                        response="Invalid Unit, Try with Valid Card";                                 
                                        return response;
                                    }*/

                                } catch (IOException ex) {
                                    Logger.getLogger(CardLoginController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                return response;
                            } else {
                                System.out.println("Kindly enter Correct PNo");
                                response = "Kindly enter Correct PNo";
                                //messageStatus(response);  
                                return response;
                            }

                        } else {
                            System.out.println("P No  is Null");
                            //lblcarderror.setText("Contract ID or Serial Number is Empty, Try Again with proper Card");
                            response = "Kindly place the valid card on the reader and try again";
                            return response;
                            //return;
                        }
                    } else {
                        System.out.println("Select App  Failure");
                        response = "Kindly place the card on the reader and try again";
                        return response;
                        //lblcarderror.setText("Kindly Place the Valid Card in the Card Reader and Try Again");
                        //return;
                    }
                } else {
                    System.out.println("Wait for connect Failure");
                    response = "Kindly place the card on the reader and try again";
                    return response;
                    //lblcarderror.setText("Kindly Place the Card in the Card Reader and Try Again");
                    //return;
                }
            }
        } else {
            System.out.println("Initialize Card Failed");
            //lblcarderror.setText("Initialize Card Failed");
            response = "Initialize Card Failed";

            String responseDeInitialize = cardReaderAPI.deInitialize();
            ObjectMapper objMapperDeInitialize = new ObjectMapper();
            CardReaderDeInitialize cardReaderDeInitialize = new CardReaderDeInitialize();
            try {
                cardReaderDeInitialize = objMapperDeInitialize.readValue(responseDeInitialize, CardReaderDeInitialize.class);
            } catch (JsonProcessingException ex) {
                Logger.getLogger(CardLoginController.class.getName()).log(Level.SEVERE, null, ex);
            }


        }
        return response;


    }


    public void fingerprintMatching(byte[] fingerData) throws IOException {
        response = "";
        try {
            String fpqpath = PropertyFile.getProperty(PropertyName.FP_QUALITY);
            if (fpqpath.isBlank() || fpqpath.isEmpty() || fpqpath == null) {
                //System.out.println("The property 'fpquality' is empty, Please add it in properties");
                LOGGER.log(Level.INFO, "The property 'fpquality' is empty, Please add it in properties");
                return;
            }
            try (BufferedReader file = new BufferedReader(new FileReader(fpqpath))) {
                String input = " ";
                String fpq = file.lines().collect(Collectors.joining());
                fpQuality = Integer.parseInt(fpq);
                System.out.println("FP Quality is :: " + fpQuality);
                LOGGER.log(Level.INFO, "FP Quality is :: " + fpQuality);
                file.close();
            } catch (Exception e) {
                e.printStackTrace();
                //System.out.println("Problem reading file./usr/share/enrollment/quality/fpquality");
                LOGGER.log(Level.INFO, "Problem reading file./usr/share/enrollment/quality/fpquality");
            }
            //fpQuality=50;
            LOGGER.log(Level.INFO, "FP Quality is : " + fpQuality);
            int[] matchScore = new int[1];
            //byte[] fmrTemplate = Base64.getDecoder().decode("Rk1SADAzMAAAAADrAAEAAAAA3P///////////wAAAAAAAAAAAMUAxQABIQGZYB9AkQDepmSAXgDdHmSAygD/s2RAlAE30ElA2AEO1WRAuACW/2RA7AEeY0RAlQFmZ0FA2QCJ+GSAXQFrAx5AqgAtikxAdwDToGRAcQEhsGRAdQCynGSA0QDXiGRAlAFERy9AygCiiGRAgAFifjGAngB1k2RAkgFyeBRAVQB1GGRAXgECpl9AfAEoxGSASADnpGSAcQE7vGRANADkH2RA7wDTbWRAXAFZF0WAPgCPmWSAtAFvZBFASABxmlsAAA==");
            byte[] fmrTemplate = Base64.getDecoder().decode("Rk1SADAzMAAAAAHhAAEAAAAB0v///////////wAAAAAAAAAAAMUAxQABRQHdYEiApAED7WSAoADG0GRAcQDx0GJA4ADll2SAbQEOTGSA5gEHjWSA8ADnoWRAhACjtGRAnQFIeWSBBwDotGSA1ACOYmSBFADuumSAkgCAlWSAuAB9eWSAVgCSq2SBAgFGh2SAJgDHsmRAGwELtWSAyABg5GRADQD5oVdA2AGIAFpA6wGRfkKAqgA8fGRA2gG192SAiQEEUWSAqwDG3WRA2wDyIWRAxQEjBmSAuwEqdWSAbQDFv2SA2gC3VmSAqACem2RA+AEaIGSA4gCjzGRAeAFRcmRA/ACqzGSA6AFLB2SARQE");

            //int ret = FingerprintTemplateMatching(fingerData, Base64.getDecoder().decode(fmrTemplate), matchScore, TemplateFormat.FMR_V2011);
            int ret = FingerprintTemplateMatching(fingerData, fmrTemplate, matchScore, TemplateFormat.FMR_V2011);
            if (ret < 0) {
                LOGGER.log(Level.INFO, "midFingerAuth.GetErrorMessage::" + midFingerAuth.GetErrorMessage(ret));
                response = "Fingerprint Template Matching Issue";
                responseStatus(response);
                return;
            } else {
                int minThresold = 60;
                if (matchScore[0] >= minThresold) {
                    LOGGER.log(Level.INFO, "Finger matched with score: " + matchScore[0]);
                    response = "Fingerprint Matched";
                    responseStatus(response);
                    return;
                } else {
                    LOGGER.log(Level.INFO, "Finger not matched with score: " + matchScore[0]);
                    response = "Fingerprint Not Matched";
                    responseStatus(response);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception: " + e);
        }


    }

    public int FingerprintTemplateMatching(byte[] fingerData, byte[] fingerprintData, int[] matchScore, TemplateFormat format) {
        int ret = midFingerAuth.MatchTemplate(fingerData, fingerprintData, matchScore, TemplateFormat.FMR_V2011);
        return ret;
    }

    @Override
    public void OnDeviceDetection(String arg0, DeviceDetection arg1) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void OnPreview(int errorCode, int quality, final byte[] image) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if (errorCode != 0) {
            //System.out.println("errorCode: " + errorCode);
            LOGGER.log(Level.INFO, "errorCode: " + errorCode);
            //System.out.println("errorCode: " + midFingerAuth.GetErrorMessage(errorCode));   
            LOGGER.log(Level.INFO, "errorCode: " + midFingerAuth.GetErrorMessage(errorCode));
            return;
        }
        try {
            // jlbPreviewQuality.setText("Quality: " + quality);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream in = new ByteArrayInputStream(image);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        WritableImage wr = null;
                        if (bufferedImage != null) {
                            System.out.println("BUFDREA not null");
                            wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                            PixelWriter pw = wr.getPixelWriter();
                            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                                    pw.setArgb(x, y, bufferedImage.getRGB(x, y));
                                }
                            }
                        }

                        ImageView imView = new ImageView(wr);
                        System.out.println("IMAGEEEEEE" + imView.getImage().toString());
                        m_FingerPrintImage.setImage(wr);

                    } catch (Exception e) {
                    }
                }
            }).start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void OnComplete(int errorCode, int Quality, int NFIQ) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if (errorCode != 0) {
            LOGGER.log(Level.INFO, "Capture:" + midFingerAuth.GetErrorMessage(errorCode));
            return;
        }
        try {
            LOGGER.log(Level.INFO, "Capture Success");
            LOGGER.log(Level.INFO, "Quality: " + Quality + ", NFIQ: " + NFIQ);
            //String fingertemplate2011="Rk1SADAzMAAAAADrAAEAAAAA3P///////////wAAAAAAAAAAAMUAxQABIQGZYB9AkQDepmSAXgDdHmSAygD/s2RAlAE30ElA2AEO1WRAuACW/2RA7AEeY0RAlQFmZ0FA2QCJ+GSAXQFrAx5AqgAtikxAdwDToGRAcQEhsGRAdQCynGSA0QDXiGRAlAFERy9AygCiiGRAgAFifjGAngB1k2RAkgFyeBRAVQB1GGRAXgECpl9AfAEoxGSASADnpGSAcQE7vGRANADkH2RA7wDTbWRAXAFZF0WAPgCPmWSAtAFvZBFASABxmlsAAA==";                    
            int[] dataLen = new int[]{2500};
            byte[] data = new byte[dataLen[0]];

            int ret = midFingerAuth.GetTemplate(data, dataLen, TemplateFormat.FMR_V2011);
            lastCaptureTemplat = new byte[dataLen[0]];
            System.arraycopy(data, 0, lastCaptureTemplat, 0, dataLen[0]);
            System.out.println("Last Capture Template::::" + lastCaptureTemplat.toString());
            fingerprintMatching(lastCaptureTemplat);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(Level.INFO, "Exception" + ex);
        }
    }

}
