/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.CardReaderAPI;
import com.cdac.enrollmentstation.api.LocalCardReaderApi;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.*;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;


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
    public Button showContractDetails;

    @FXML
    public Button show_home_token;


    @FXML
    private Label messageLabel;

    @FXML
    private PasswordField pinNoPasswordField;

    @FXML
    private ImageView m_FingerPrintImage;


    CardReaderAPI cardReaderAPI = new CardReaderAPI();
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
        EnumMap<DataType, String> hexadecimalDataMap;
        try {
            // required to follow the procedure calls
            // deInitialize -> initialize ->[waitForConnect -> selectApp] -> readData
            hexadecimalDataMap = startProcedureCall();
        } catch (IllegalArgumentException | GenericException ex) {
            if (ex instanceof IllegalArgumentException) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
            }
            messageLabel.setText(GENERIC_ERR_MSG);
            return;
        }
        // connection timeout
        if (hexadecimalDataMap == null) {
            messageLabel.setText(CONNECTION_TIMEOUT_MSG);
            return;
        }
        String cardPinNumber = extractCardPinNumberFromAsn(hexadecimalDataMap.get(DataType.STATIC));
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
        }

    }

    // throws GenericException due to ObjectMapper.
    private EnumMap<DataType, String> startProcedureCall() {
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
        return readDataFromCard(crWaitForConnectResDto.getHandle());
    }

    private EnumMap<DataType, String> readDataFromCard(int handle) {
        StringBuilder hexadecimalStaticData = new StringBuilder();
        /* ** Will use when 2-factor authentication is enabled. *** */
        // StringBuilder hexadecimalFingerprintData = new StringBuilder()

        // (read data 1024 bytes) * 3 times
        // TODO: need to know how many bytes are written while writing data to card.
        for (int i = 0; i < 3; i++) {
            hexadecimalStaticData.append(readBufferedDataFromCard(handle, DataType.STATIC, i * CARD_READER_MAX_BUFFER_SIZE, CARD_READER_MAX_BUFFER_SIZE));
            //hexadecimalFingerprintData.append(readBufferedDataFromCard(handle, DataType.FINGERPRINT, i * CARD_READER_MAX_BUFFER_SIZE, CARD_READER_MAX_BUFFER_SIZE))
        }
        EnumMap<DataType, String> hexadecimalDataMap = new EnumMap<>(DataType.class);
        hexadecimalDataMap.put(DataType.STATIC, hexadecimalStaticData.toString());
        //hexadecimalDataMap.put(DataType.FINGERPRINT, hexadecimalFingerprintData.toString())
        return hexadecimalDataMap;
    }

    private String extractCardPinNumberFromAsn(String hexadecimalData) {
        // contractorId = hextoasn.getContractorIdfromASN(hexadecimalData);
        // contractorName = hextoasn.getContractorNamefromASN(hexadecimalData);
        // can return null
        throw new AssertionError("Not implemented.");
    }

    private String readBufferedDataFromCard(int handle, DataType whichData, int offset, int requestLength) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRReadDataReqDto(handle, whichData.getValue(), offset, requestLength));
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRReadDataResDto crReadDataResDto = LocalCardReaderApi.postReadData(reqData);
        // connection timeout
        if (crReadDataResDto == null) {
            return null;
        }
        jniErrorCode = crReadDataResDto.getRetVal();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }
        byte[] decodedBytes = Base64.getDecoder().decode(crReadDataResDto.getResponse());
        // TODO: must find antother way to do it.
        return DatatypeConverter.printHexBinary(decodedBytes);
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
    public void OnDeviceDetection(String arg0, DeviceDetection arg1) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            int ret = fingerprintTemplateMatching(fingerData, fmrTemplate, matchScore, TemplateFormat.FMR_V2011);
            if (ret < 0) {
                LOGGER.log(Level.INFO, "midFingerAuth.GetErrorMessage::" + midFingerAuth.GetErrorMessage(ret));
                response = "Fingerprint Template Matching Issue";
                updateUI(response);
                return;
            } else {
                int minThresold = 60;
                if (matchScore[0] >= minThresold) {
                    LOGGER.log(Level.INFO, "Finger matched with score: " + matchScore[0]);
                    response = "Fingerprint Matched";
                    updateUI(response);
                    return;
                } else {
                    LOGGER.log(Level.INFO, "Finger not matched with score: " + matchScore[0]);
                    response = "Fingerprint Not Matched";
                    updateUI(response);
                    return;
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
