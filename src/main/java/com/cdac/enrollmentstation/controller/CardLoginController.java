package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.LocalCardReaderApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.*;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.CardWhitelistDetail;
import com.cdac.enrollmentstation.util.Asn1Util;
import com.cdac.enrollmentstation.util.LocalCardReaderErrMsgUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mantra.midfingerauth.DeviceInfo;
import com.mantra.midfingerauth.MIDFingerAuth;
import com.mantra.midfingerauth.MIDFingerAuth_Callback;
import com.mantra.midfingerauth.enums.DeviceDetection;
import com.mantra.midfingerauth.enums.DeviceModel;
import com.mantra.midfingerauth.enums.TemplateFormat;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;
import static com.cdac.enrollmentstation.util.Asn1Util.CardStaticDataIndex;


/**
 * FXML Controller class
 *
 * @author padmanabhanj
 */
public class CardLoginController implements MIDFingerAuth_Callback {
    private static final Logger LOGGER = ApplicationLog.getLogger(CardLoginController.class);
    private boolean twoFactorAuthEnabled;
    private static final String MANTRA_CARD_READER_NAME;
    private static final String CARD_API_SERVICE_RESTART_COMMAND;
    private static final byte CARD_TYPE = 4; // Naval ID/Contractor Card value is 4
    private static final byte STATIC_TYPE = 21; // Static file -> 21
    private static final byte FINGERPRINT_TYPE = 25; // Fingerprint file -> 25
    private static final int CARD_READER_MAX_BUFFER_SIZE = 1024; // Max bytes card can handle
    private static final int MAX_LENGTH = 30;
    private static final int SLEEP_TIME_IN_MIL_SEC = 500;
    private int jniErrorCode;
    private EnumMap<DataType, byte[]> asn1EncodedHexByteArrayMap; // GLOBAL data store.

    private enum DataType {
        STATIC(STATIC_TYPE), FINGERPRINT(FINGERPRINT_TYPE);
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
    private Label cardPinLabel;
    @FXML
    private PasswordField pNoPasswordField;
    @FXML
    private ImageView iconOrFpImageView;
    @FXML
    private Button loginBtn;
    @FXML
    private Button backBtn;

    private boolean isPinAuthCompleted;

    //***********************Fingerprint***************************//
    private MIDFingerAuth midFingerAuth; // For MID finger jar
    private DeviceInfo deviceInfo;
    private boolean isDeviceInitialized;
    private static final int MIN_QUALITY = 60;
    private static final int FINGERPRINT_CAPTURE_TIMEOUT_IN_SEC = 10;

    //***********************Fingerprint***************************//


    static {
        try {
            MANTRA_CARD_READER_NAME = PropertyFile.getProperty(PropertyName.CARD_API_CARD_READER_NAME).trim();
            CARD_API_SERVICE_RESTART_COMMAND = PropertyFile.getProperty(PropertyName.CARD_API_SERVICE_RESTART_COMMAND).trim();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, () -> "No entry for '" + PropertyName.CARD_API_CARD_READER_NAME + "/" + PropertyName.CARD_API_SERVICE_RESTART_COMMAND + "' in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(ex.getMessage());
        }
    }


    private void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }

    public void initialize() {
        /* adds changeListener to restrict the text length */
        pNoPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isBlank()) {
                messageLabel.setText("");
            }
            limitCharacters(pNoPasswordField, oldValue, newValue);
        });
        pNoPasswordField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                loginBtnAction();
            }
            event.consume();
        });
        try {
            twoFactorAuthEnabled = Boolean.parseBoolean(PropertyFile.getProperty(PropertyName.TWO_FACTOR_AUTH_ENABLED).trim());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, () -> "Not a number or no entry for '" + PropertyName.TWO_FACTOR_AUTH_ENABLED + "' in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(ex.getMessage());
        }
        if (twoFactorAuthEnabled) {
            midFingerAuth = new MIDFingerAuth(this);
            initFpReader();
        }
        App.setNudLogin(false);
    }

    private boolean initFpReader() {
        List<String> devices = new ArrayList<>();
        jniErrorCode = midFingerAuth.GetConnectedDevices(devices);
        if (jniErrorCode != 0 || devices.isEmpty()) {
            LOGGER.log(Level.INFO, () -> midFingerAuth.GetErrorMessage(jniErrorCode));
            updateUI("Single fingerprint reader not connected.");
            return false;
        }
        if (!midFingerAuth.IsDeviceConnected(DeviceModel.valueFor(devices.get(0)))) {
            LOGGER.log(Level.INFO, "Fingerprint reader not connected");
            updateUI("Device not connected. Please connect and try again.");
            return false;
        }

        deviceInfo = new DeviceInfo();
        jniErrorCode = midFingerAuth.Init(DeviceModel.valueFor(devices.get(0)), deviceInfo);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.INFO, () -> midFingerAuth.GetErrorMessage(jniErrorCode));
            updateUI("Single fingerprint reader not initialized.");
            return false;
        }
        isDeviceInitialized = true;
        return true;
    }

    @FXML
    private void backBtnAction() throws IOException {
        App.setRoot("login");
    }

    private void startAuthentication() {
        // required to follow the procedure calls to read data from card.
        // deInitialize -> initialize ->[waitForConnect -> selectApp] -> readData
        try {
            asn1EncodedHexByteArrayMap = startProcedureCall();
        } catch (GenericException ex) {
            pNoPasswordField.clear();
            updateUI(ex.getMessage());
            enableControls(backBtn, loginBtn);
            return;
        }

        // connection timeout
        if (asn1EncodedHexByteArrayMap == null) {
            updateUI("Something went wrong. Kindly check Card API service.");
            enableControls(backBtn, loginBtn);
            return;
        }
        if (!twoFactorAuthEnabled) {
            try {
                authenticateByPN();
                App.setRoot("main_screen");
            } catch (Exception ex) {
                if (ex instanceof IOException) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                    throw new GenericException(GENERIC_ERR_MSG);
                }
                pNoPasswordField.clear();
                updateUI(ex.getMessage());
                enableControls(backBtn, loginBtn);
            }
            return;
        }
        // if reached here, then two-factor auth is enabled.
        try {
            if (!isPinAuthCompleted) {
                authenticateByPN();
                isPinAuthCompleted = true;
            }
            cardPinLabel.setVisible(false);
            pNoPasswordField.setVisible(false);
            // loads next page by OnComplete callback if authenticated successfully.
            captureFingerprint();
        } catch (GenericException ex) {
            pNoPasswordField.clear();
            updateUI(ex.getMessage());
            enableControls(backBtn, loginBtn);
        }
    }

    @FXML
    public void loginBtnAction() throws IllegalStateException {
        if (pNoPasswordField.getText().isBlank() || pNoPasswordField.getText().length() < 4) {
            messageLabel.setText("Please enter valid card pin.");
            return;
        }
        disableControls(backBtn, loginBtn);
        App.getThreadPool().execute(this::startAuthentication);
    }


    // throws GenericException
    // Caller must handle the exception
    private void authenticateByPN() {
        // gets pin code from card
        String pNumber = Asn1Util.extractFromStaticAns1Encoded(asn1EncodedHexByteArrayMap.get(DataType.STATIC), CardStaticDataIndex.PN);
        String cardNumber = Asn1Util.extractFromStaticAns1Encoded(asn1EncodedHexByteArrayMap.get(DataType.STATIC), CardStaticDataIndex.CARD_NUMBER);
        if (pNumber == null || pNumber.isBlank() || cardNumber == null || cardNumber.isBlank()) {
            LOGGER.log(Level.SEVERE, "Received a null or empty value for PN or Card Number from card.");
            throw new GenericException("Kindly place a valid card and try again.");
        }
        String cardHotlistedFilePathString = PropertyFile.getProperty(PropertyName.CARD_WHITELISTED_FILE);
        if (cardHotlistedFilePathString == null || cardHotlistedFilePathString.isBlank()) {
            throw new GenericException("'+" + PropertyName.CARD_WHITELISTED_FILE + "' is empty or not found in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        List<CardWhitelistDetail> cardWhitelistDetails;
        Path path = Paths.get(cardHotlistedFilePathString);
        try {
            cardWhitelistDetails = Singleton.getObjectMapper().readValue(Files.readAllBytes(path), new TypeReference<>() {
            });
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            try {
                Files.writeString(path, "[]");
                cardWhitelistDetails = new ArrayList<>();
            } catch (IOException ex1) {
                throw new GenericException(ex1.getMessage());
            }
        }

        Optional<CardWhitelistDetail> whitelistedCardDetailOptional = cardWhitelistDetails.stream().filter(ele -> pNumber.equalsIgnoreCase(ele.getPNo())).findFirst();

        String unauthorizedMessage = "You are unauthorized to access the system.";
        CardWhitelistDetail whitelistedCardDetail = whitelistedCardDetailOptional.orElseGet(() -> {
            LOGGER.log(Level.INFO, "No Personal/Card Number found in the downloaded list.");
            throw new GenericException(unauthorizedMessage);
        });

        if (whitelistedCardDetail.getCardNo() == null) {
            LOGGER.log(Level.INFO, "Found null card number in downloaded cards.");
            throw new GenericException(unauthorizedMessage);
        }

        if (!whitelistedCardDetail.getCardNo().equalsIgnoreCase(cardNumber)) {
            LOGGER.log(Level.INFO, "Card number not matched with the downloaded card.");
            throw new GenericException(unauthorizedMessage);
        }

        if (!pNumber.equalsIgnoreCase(pNoPasswordField.getText())) {
            LOGGER.log(Level.INFO, "Personal Number and user input number does not matched.");
            throw new GenericException("Invalid Personal Number.");
        }
    }

    private boolean restartApiService() {
        try {
            Process pr = Runtime.getRuntime().exec(CARD_API_SERVICE_RESTART_COMMAND);
            int exitCode = pr.waitFor();
            LOGGER.log(Level.INFO, () -> "****Naval_WebServices restart exit code: " + exitCode);
            return exitCode == 0;
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GenericException(GENERIC_ERR_MSG);
        }

    }

    // throws GenericException
    // Caller must handle the exception
    private EnumMap<DataType, byte[]> startProcedureCall() {
        // required to follow the procedure calls
        // deInitialize -> initialize ->[waitForConnect -> selectApp] -> readData
        String reqData;
        CRWaitForConnectResDto crWaitForConnectResDto;
        int counter = 1;
        // restart Naval_WebServices if failed on the first WaitForConnect call.
        while (true) {
            counter--;
            CRDeInitializeResDto crDeInitializeResDto = LocalCardReaderApi.getDeInitialize();
            // connection timeout
            if (crDeInitializeResDto == null) {
                return null;
            }
            jniErrorCode = crDeInitializeResDto.getRetVal();
            // -1409286131 -> prerequisites failed error
            if (jniErrorCode != 0 && jniErrorCode != -1409286131) {
                LOGGER.log(Level.SEVERE, () -> "****DeInitializeErrorCode: " + jniErrorCode);
                throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            }
            CRInitializeResDto crInitializeResDto = LocalCardReaderApi.getInitialize();
            // connection timeout
            if (crInitializeResDto == null) {
                return null;
            }
            jniErrorCode = crInitializeResDto.getRetVal();
            if (jniErrorCode != 0) {
                LOGGER.log(Level.SEVERE, () -> "****InitializeErrorCode: " + jniErrorCode);
                throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            }
            try {
                Thread.sleep(SLEEP_TIME_IN_MIL_SEC);
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Interrupted while sleeping.");
                Thread.currentThread().interrupt();
            }

            try {
                reqData = Singleton.getObjectMapper().writeValueAsString(new CRWaitForConnectReqDto(MANTRA_CARD_READER_NAME));
            } catch (JsonProcessingException ex) {
                LOGGER.log(Level.SEVERE, ex::getMessage);
                throw new GenericException(GENERIC_ERR_MSG);
            }

            crWaitForConnectResDto = LocalCardReaderApi.postWaitForConnect(reqData);
            // connection timeout
            if (crWaitForConnectResDto == null) {
                return null;
            }
            jniErrorCode = crWaitForConnectResDto.getRetVal();

            if (jniErrorCode != 0) {
                LOGGER.log(Level.SEVERE, () -> "****WaitForConnectErrorCode: " + jniErrorCode);
                if (jniErrorCode == -1090519029) { // custom message when press 'login' without reader connected
                    throw new GenericException("No card reader detected or unsupported reader name.");
                }
                if (counter == 0) {
                    LOGGER.log(Level.INFO, () -> "****Communication error occurred. Restarting Naval_WebServices.");
                    if (restartApiService()) {
                        try {
                            Thread.sleep(1000); // needed to sleep after restarting
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        continue; // starts from DeInitialize again.
                    } // else exit code is not zero
                }
                throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
            }
            try {
                reqData = Singleton.getObjectMapper().writeValueAsString(new CRSelectAppReqDto(CARD_TYPE, crWaitForConnectResDto.getHandle()));
            } catch (JsonProcessingException ex) {
                LOGGER.log(Level.SEVERE, ex::getMessage);
                throw new GenericException(GENERIC_ERR_MSG);
            }
            break;
        }

        if (crWaitForConnectResDto.getRetVal() != 0) {
            throw new GenericException("Kindly reconnect the reader and place card correctly.");
        }

        CRSelectAppResDto crSelectAppResDto = LocalCardReaderApi.postSelectApp(reqData);
        // connection timeout
        if (crSelectAppResDto == null) {
            return null;
        }
        jniErrorCode = crSelectAppResDto.getRetVal();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> "****SelectAppErrorCode: " + jniErrorCode);
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
        EnumMap<DataType, byte[]> ans1EncodedHexByteArrayMap = new EnumMap<>(DataType.class);
        ans1EncodedHexByteArrayMap.put(DataType.STATIC, readBufferedData(crWaitForConnectResDto.getHandle(), DataType.STATIC));
        if (twoFactorAuthEnabled) {
            ans1EncodedHexByteArrayMap.put(DataType.FINGERPRINT, readBufferedData(crWaitForConnectResDto.getHandle(), DataType.FINGERPRINT));
        }
        return ans1EncodedHexByteArrayMap;
    }

    // throws GenericException
    // Caller must handle the exception readBufferedDataFromCard
    private byte[] readBufferedData(int handle, DataType dataType) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            // for reading multiple times
            boolean repeat = true;
            int offset = 0;
            while (repeat) {
                CRReadDataResDto crReadDataResDto = readData(handle, dataType, offset, CARD_READER_MAX_BUFFER_SIZE);
                // connection timeout
                if (crReadDataResDto == null) {
                    return null;
                }
                jniErrorCode = crReadDataResDto.getRetVal();
                // if first request failed throw exception
                if (offset == 0 && jniErrorCode != 0) {
                    LOGGER.log(Level.SEVERE, () -> "****ReadDataErrorCode: " + jniErrorCode);
                    throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
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
            return byteArrayOutputStream.toByteArray();
        } catch (Exception ex) {
            // throws if exception occurs while writing to byteOutputStream
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }
    }

    // throws GenericException
    // Caller must handle the exception
    private CRReadDataResDto readData(int handle, DataType whichData, int offset, int requestLength) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRReadDataReqDto(handle, whichData.getValue(), offset, requestLength));
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        return LocalCardReaderApi.postReadData(reqData);
    }


    private void captureFingerprint() {
        if (!isDeviceInitialized && (!initFpReader())) {
            //message updated by initFpReader()
            return;
        }
        jniErrorCode = midFingerAuth.StartCapture(MIN_QUALITY, (int) TimeUnit.SECONDS.toMillis(FINGERPRINT_CAPTURE_TIMEOUT_IN_SEC));
        if (jniErrorCode != 0) {
            LOGGER.log(Level.INFO, () -> midFingerAuth.GetErrorMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }
    }

    @Override
    public void OnDeviceDetection(String s, DeviceDetection deviceDetection) {
        if (DeviceDetection.DISCONNECTED == deviceDetection) {
            LOGGER.log(Level.INFO, "Fingerprint scanner disconnected.");
            updateUI("Fingerprint scanner disconnected.");
            midFingerAuth.Uninit();
            isDeviceInitialized = false;
        }
    }

    @Override
    public void OnPreview(int errorCode, int quality, final byte[] imageData) {
        if (errorCode != 0 || imageData == null) {
            LOGGER.log(Level.SEVERE, () -> midFingerAuth.GetErrorMessage(errorCode));
            loginBtn.setDisable(false);
            return;
        }
        InputStream inputStream = new ByteArrayInputStream(imageData);
        Image image = new Image(inputStream, iconOrFpImageView.getFitWidth(), iconOrFpImageView.getFitHeight(), true, false);
        iconOrFpImageView.setImage(image);
    }

    @Override
    public void OnComplete(int errorCode, int quality, int nFIQ) {
        if (errorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midFingerAuth.GetErrorMessage(errorCode));
            updateUI("Fingerprint quality too poor. Please try again.");
            loginBtn.setDisable(false);
            return;
        }
        int dataLen = 2500;  // as is. but can also be used from OnPreview callback
        byte[] template = new byte[dataLen];
        int[] templateLen = {dataLen};
        jniErrorCode = midFingerAuth.GetTemplate(template, templateLen, TemplateFormat.FMR_V2011);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midFingerAuth.GetErrorMessage(errorCode));
            updateUI(GENERIC_ERR_MSG);
            loginBtn.setDisable(false);
            return;
        }
        try {
            matchFingerprintTemplate(template);
            App.setRoot("main_screen");
        } catch (GenericException | IOException ex) {
            if (ex instanceof IOException) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
                throw new GenericException(GENERIC_ERR_MSG);
            }
            updateUI(ex.getMessage());
            loginBtn.setDisable(false);
        }
    }

    public void matchFingerprintTemplate(byte[] template) {
        int fpMatchMinThreshold;
        try {
            fpMatchMinThreshold = Integer.parseInt(PropertyFile.getProperty(PropertyName.FP_MATCH_MIN_THRESHOLD).trim());
        } catch (NumberFormatException | GenericException ex) {
            LOGGER.log(Level.SEVERE, () -> "Not a number or no entry for '" + PropertyName.FP_MATCH_MIN_THRESHOLD + "' in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        int[] matchScore = new int[1];
        byte[] fingerprintBytes = asn1EncodedHexByteArrayMap.get(DataType.FINGERPRINT);
        if (fingerprintBytes == null) {
            LOGGER.log(Level.SEVERE, () -> "Read null fingerprint value from card.");
            throw new GenericException(GENERIC_ERR_MSG);
        }

        jniErrorCode = midFingerAuth.MatchTemplate(template, fingerprintBytes, matchScore, TemplateFormat.FMR_V2011);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midFingerAuth.GetErrorMessage(jniErrorCode));
            throw new GenericException(GENERIC_ERR_MSG);
        }
        if (matchScore[0] < fpMatchMinThreshold) {
            LOGGER.log(Level.SEVERE, "Fingerprint not matched.");
            throw new GenericException("Fingerprint not matched. Please try again.");
        }
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

    public void updateUI(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }
}
