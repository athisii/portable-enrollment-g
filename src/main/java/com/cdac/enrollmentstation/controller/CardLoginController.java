/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.LocalCardReaderApi;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.*;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.Asn1EncodedHexUtil;
import com.cdac.enrollmentstation.util.LocalCardReaderErrMsgUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mantra.midfingerauth.DeviceInfo;
import com.mantra.midfingerauth.MIDFingerAuth;
import com.mantra.midfingerauth.MIDFingerAuth_Callback;
import com.mantra.midfingerauth.enums.DeviceDetection;
import com.mantra.midfingerauth.enums.DeviceModel;
import com.mantra.midfingerauth.enums.TemplateFormat;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;
import static com.cdac.enrollmentstation.security.Asn1EncodedHexUtil.CardDataIndex;


/**
 * FXML Controller class
 *
 * @author padmanabhanj
 */
public class CardLoginController implements MIDFingerAuth_Callback {
    private static final String MANTRA_CARD_READER_NAME = "Mantra Reader (1.00) 00 00";
    private static final byte CARD_TYPE = 4; // Naval ID/Contractor Card value is 4
    private static final byte STATIC_TYPE = 21; // Static file -> 21
    private static final byte FINGERPRINT_TYPE = 25; // Fingerprint file -> 25
    private static final int CARD_READER_MAX_BUFFER_SIZE = 1024; // Max bytes card can handle
    private static final String CONNECTION_TIMEOUT_MSG = "Connection timeout. Please try again.";
    private static final int MAX_LENGTH = 15;
    private int jniErrorCode;


    private enum DataType {
        STATIC(STATIC_TYPE),
        FINGERPRINT(FINGERPRINT_TYPE);
        private final byte value;

        DataType(byte val) {
            value = val;
        }

        private byte getValue() {
            return value;
        }
    }


    @FXML
    private Label messageLabel;

    @FXML
    private PasswordField pinNoPasswordField;

    @FXML
    private ImageView m_FingerPrintImage;


    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(CardLoginController.class);
    private MIDFingerAuth midFingerAuth; // For MID finger jar
    private DeviceInfo deviceInfo;
    private byte[] lastCaptureTemplat;
    int fingerprintinit;
    int minQuality = 60;
    int timeout = 10000;
    int fpQuality = 96;
    String fpquality = null;
    String response = "";


    private void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }

    public void initialize() {
        /* add ChangeListner to TextField to restrict the TextField Length*/
        pinNoPasswordField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(pinNoPasswordField, oldValue, newValue));
        pinNoPasswordField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                loginBtnAction();
            }
        });

        // TODO: uncomment this when 2-factor authentication is enabled
        /*
        midFingerAuth = new MIDFingerAuth(this);
        List<String> devices = new ArrayList<>();
        jniErrorCode = midFingerAuth.GetConnectedDevices(devices);
        if (jniErrorCode != 0 || devices.isEmpty()) {
            LOGGER.log(Level.INFO, () -> midFingerAuth.GetErrorMessage(jniErrorCode));
            messageLabel.setText(GENERIC_RS_ERR_MSG);
            return;
        }
        if (!midFingerAuth.IsDeviceConnected(DeviceModel.MFS100)) {
            LOGGER.log(Level.INFO, "MFS100 device not connected");
            messageLabel.setText("Device not connected. Please connect and try again.");
            return;
        }

        deviceInfo = new DeviceInfo();
        jniErrorCode = midFingerAuth.Init(DeviceModel.MFS100, deviceInfo);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.INFO, () -> midFingerAuth.GetErrorMessage(jniErrorCode));
            LOGGER.log(Level.INFO, GENERIC_RS_ERR_MSG);
        }
        */
    }

    @FXML
    private void backBtnAction() throws IOException {
        App.setRoot("login");
    }

    @FXML
    public void loginBtnAction() throws IllegalStateException {
        if (pinNoPasswordField.getText().isBlank() || pinNoPasswordField.getText().length() < 4) {
            messageLabel.setText("Please enter valid card pin.");
            return;
        }
        EnumMap<DataType, byte[]> asn1EncodedHexByteArrayMap;

        try {
            // required to follow the procedure calls
            // deInitialize -> initialize ->[waitForConnect -> selectApp] -> readData
            asn1EncodedHexByteArrayMap = startProcedureCall();
        } catch (IllegalArgumentException | GenericException ex) {
            if (ex instanceof IllegalArgumentException) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
            }
            messageLabel.setText(GENERIC_ERR_MSG);
            return;
        }
        // connection timeout
        if (asn1EncodedHexByteArrayMap == null) {
            messageLabel.setText(CONNECTION_TIMEOUT_MSG);
            return;
        }
        // gets pin code from card
        String cardPinNumber = Asn1EncodedHexUtil.extractFromAns1EncodedHex(asn1EncodedHexByteArrayMap.get(DataType.STATIC), CardDataIndex.PIN_NUMBER);
        if (cardPinNumber == null) {
            LOGGER.log(Level.SEVERE, "Received null value from card.");
            messageLabel.setText(GENERIC_ERR_MSG);
            return;
        }
        if (!cardPinNumber.equals(pinNoPasswordField.getText())) {
            LOGGER.log(Level.INFO, "Card pin number and user input pin number do not match.");
            messageLabel.setText("Invalid card pin number.");
            return;
        }
        try {
            App.setRoot("main_screen");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }

    }

    // throws GenericException
    // Caller must handle the exception
    private EnumMap<DataType, byte[]> startProcedureCall() {
        // required to follow the procedure calls
        // deInitialize -> initialize ->[waitForConnect -> selectApp] -> readData
        CRDeInitializeResDto crDeInitializeResDto = LocalCardReaderApi.getDeInitialize();
        // connection timeout
        if (crDeInitializeResDto == null) {
            return null;
        }
        jniErrorCode = crDeInitializeResDto.getRetVal();
        // -1409286131 -> prerequisites failed error
        if (jniErrorCode != 0 && jniErrorCode != -1409286131) {
            LOGGER.log(Level.SEVERE, () -> LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRInitializeResDto crInitializeResDto = LocalCardReaderApi.getInitialize();
        // connection timeout
        if (crInitializeResDto == null) {
            return null;
        }
        jniErrorCode = crInitializeResDto.getRetVal();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRWaitForConnectReqDto(MANTRA_CARD_READER_NAME));
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }

        CRWaitForConnectResDto crWaitForConnectResDto = LocalCardReaderApi.postWaitForConnect(reqData);
        // connection timeout
        if (crWaitForConnectResDto == null) {
            return null;
        }
        jniErrorCode = crWaitForConnectResDto.getRetVal();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }

        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRSelectAppReqDto(CARD_TYPE, crWaitForConnectResDto.getHandle()));
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRSelectAppResDto crSelectAppResDto = LocalCardReaderApi.postSelectApp(reqData);
        // connection timeout
        if (crSelectAppResDto == null) {
            return null;
        }
        jniErrorCode = crSelectAppResDto.getRetVal();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }
        return readDataFromCard(crWaitForConnectResDto.getHandle(), DataType.STATIC);
    }

    // throws GenericException
    // Caller must handle the exception
    private EnumMap<DataType, byte[]> readDataFromCard(int handle, DataType dataType) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            // for reading multiple times
            boolean repeat = true;
            int offset = 0;
            while (repeat) {
                CRReadDataResDto crReadDataResDto = readBufferedDataFromCard(handle, dataType, offset, CARD_READER_MAX_BUFFER_SIZE);
                // connection timeout
                if (crReadDataResDto == null) {
                    return null;
                }
                jniErrorCode = crReadDataResDto.getRetVal();
                // if first request failed throw exception
                if (offset == 0 && jniErrorCode != 0) {
                    LOGGER.log(Level.SEVERE, () -> LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
                    throw new GenericException(GENERIC_ERR_MSG);
                }
                // consider 1st request responseLen  = 1024 bytes
                // therefore, we assumed more data is left to be read,
                // so we make a 2nd request, but all data are already read.
                // in that case we get non-zero return value.
                if (offset != 0 && jniErrorCode != 0) {
                    break;
                }

                byte[] base64DecodedBytes = Base64.getDecoder().decode(crReadDataResDto.getResponse());
                // responseLen(in bytes)
                if (base64DecodedBytes.length != crReadDataResDto.getResponseLen()) {
                    LOGGER.log(Level.SEVERE, "Number of decoded bytes and response length not equal.");
                    throw new GenericException(GENERIC_ERR_MSG);
                }
                // end the read request
                if (crReadDataResDto.getResponseLen() < CARD_READER_MAX_BUFFER_SIZE) {
                    repeat = false;
                }
                offset += CARD_READER_MAX_BUFFER_SIZE;
                byteArrayOutputStream.write(base64DecodedBytes);
            }
            EnumMap<DataType, byte[]> asn1EncodedHexByteArrayMap = new EnumMap<>(DataType.class);
            asn1EncodedHexByteArrayMap.put(dataType, byteArrayOutputStream.toByteArray());
            return asn1EncodedHexByteArrayMap;
        } catch (IOException ex) {
            // throws if exception occurs while writing to byteOutputStream
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }
    }

    // throws GenericException
    // Caller must handle the exception
    private CRReadDataResDto readBufferedDataFromCard(int handle, DataType whichData, int offset, int requestLength) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRReadDataReqDto(handle, whichData.getValue(), offset, requestLength));
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        return LocalCardReaderApi.postReadData(reqData);
    }

    public void updateUI(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }


    public String captureSingleFingerprint() {
        String response = "";
        if (fingerprintinit != 0) {
            String model = "MFS100";
            DeviceInfo info = new DeviceInfo();
            int fingerprintinit = midFingerAuth.Init(DeviceModel.valueFor(model), info);
            LOGGER.log(Level.INFO, () -> "fingerprintinit::" + fingerprintinit);
        }

        int retCapture = midFingerAuth.StartCapture(minQuality, timeout);
        LOGGER.log(Level.INFO, () -> "Start Capture Return::" + retCapture);
        if (retCapture != 0) {
            LOGGER.log(Level.INFO, () -> "Start Capture Error:" + midFingerAuth.GetErrorMessage(retCapture));
            response = "not Captured";
            return response;
        }
        return response;
    }

    @Override
    public void OnDeviceDetection(String s, DeviceDetection deviceDetection) {
        LOGGER.log(Level.INFO, "Not implemented.");
    }

    @Override
    public void OnPreview(int errorCode, int quality, final byte[] image) {
        if (errorCode != 0) {
            LOGGER.log(Level.INFO, "errorCode: " + errorCode);
            LOGGER.log(Level.INFO, "errorCode: " + midFingerAuth.GetErrorMessage(errorCode));
            return;
        }
        try {
            new Thread(() -> {
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
            }).start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void OnComplete(int errorCode, int Quality, int NFIQ) {
        if (errorCode != 0) {
            LOGGER.log(Level.INFO, "Capture:" + midFingerAuth.GetErrorMessage(errorCode));
            return;
        }
        try {
            LOGGER.log(Level.INFO, "Capture Success");
            LOGGER.log(Level.INFO, "Quality: " + Quality + ", NFIQ: " + NFIQ);
            int[] dataLen = new int[]{2500};
            byte[] data = new byte[dataLen[0]];
            lastCaptureTemplat = new byte[dataLen[0]];
            System.arraycopy(data, 0, lastCaptureTemplat, 0, dataLen[0]);
            System.out.println("Last Capture Template::::" + lastCaptureTemplat.toString());
            fingerprintMatching(lastCaptureTemplat);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(Level.INFO, "Exception" + ex);
        }
    }

    public void fingerprintMatching(byte[] fingerData) throws IOException {
        response = "";
        try {
            String fpqpath = PropertyFile.getProperty(PropertyName.FP_QUALITY);
            if (fpqpath.isBlank() || fpqpath.isEmpty() || fpqpath == null) {
                LOGGER.log(Level.INFO, "The property 'fpquality' is empty, Please add it in properties");
                return;
            }
            try (BufferedReader file = new BufferedReader(new FileReader(fpqpath))) {
                String fpq = file.lines().collect(Collectors.joining());
                fpQuality = Integer.parseInt(fpq);
                System.out.println("FP Quality is :: " + fpQuality);
                LOGGER.log(Level.INFO, "FP Quality is :: " + fpQuality);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.log(Level.INFO, "Problem reading file./usr/share/enrollment/quality/fpquality");
            }
            LOGGER.log(Level.INFO, "FP Quality is : " + fpQuality);
            int[] matchScore = new int[1];
            byte[] fmrTemplate = Base64.getDecoder().decode("Rk1SADAzMAAAAAHhAAEAAAAB0v///////////wAAAAAAAAAAAMUAxQABRQHdYEiApAED7WSAoADG0GRAcQDx0GJA4ADll2SAbQEOTGSA5gEHjWSA8ADnoWRAhACjtGRAnQFIeWSBBwDotGSA1ACOYmSBFADuumSAkgCAlWSAuAB9eWSAVgCSq2SBAgFGh2SAJgDHsmRAGwELtWSAyABg5GRADQD5oVdA2AGIAFpA6wGRfkKAqgA8fGRA2gG192SAiQEEUWSAqwDG3WRA2wDyIWRAxQEjBmSAuwEqdWSAbQDFv2SA2gC3VmSAqACem2RA+AEaIGSA4gCjzGRAeAFRcmRA/ACqzGSA6AFLB2SARQE");

            int ret = fingerprintTemplateMatching(fingerData, fmrTemplate, matchScore, TemplateFormat.FMR_V2011);
            if (ret < 0) {
                LOGGER.log(Level.INFO, "midFingerAuth.GetErrorMessage::" + midFingerAuth.GetErrorMessage(ret));
                response = "Fingerprint Template Matching Issue";
                updateUI(response);
            } else {
                int minThresold = 60;
                if (matchScore[0] >= minThresold) {
                    LOGGER.log(Level.INFO, "Finger matched with score: " + matchScore[0]);
                    response = "Fingerprint Matched";
                    updateUI(response);
                } else {
                    LOGGER.log(Level.INFO, "Finger not matched with score: " + matchScore[0]);
                    response = "Fingerprint Not Matched";
                    updateUI(response);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception: " + e);
        }


    }

    public int fingerprintTemplateMatching(byte[] fingerData, byte[] fingerprintData, int[] matchScore, TemplateFormat format) {
        return midFingerAuth.MatchTemplate(fingerData, fingerprintData, matchScore, TemplateFormat.FMR_V2011);
    }

}
