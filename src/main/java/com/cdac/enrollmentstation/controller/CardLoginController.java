package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.CRWaitForConnectResDto;
import com.cdac.enrollmentstation.dto.CardWhitelistDetail;
import com.cdac.enrollmentstation.exception.ConnectionTimeoutException;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.exception.NoReaderOrCardException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.Asn1CardTokenUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;
import static com.cdac.enrollmentstation.util.Asn1CardTokenUtil.*;


/**
 * FXML Controller class
 *
 * @author padmanabhanj
 */
public class CardLoginController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(CardLoginController.class);
    private static final String MANTRA_CARD_READER_NAME;
    private static final String CARD_API_SERVICE_RESTART_COMMAND;
    private static final int MAX_LENGTH = 30;
    private static int handle;
    private EnumMap<CardTokenFileType, byte[]> fileTypeToAsn1EncodedByteArrayMap; // GLOBAL data store.

    @FXML
    private Label messageLabel;
    @FXML
    private Label cardLabel;
    @FXML
    private PasswordField cardPasswordField;

    @FXML
    private Button loginBtn;
    @FXML
    private Button backBtn;

    private boolean isPnAuthCompleted;


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
        cardPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isBlank()) {
                messageLabel.setText("");
            }
            limitCharacters(cardPasswordField, oldValue, newValue);
        });
        cardPasswordField.setOnKeyReleased(event -> {
            if (event.getCode().equals(KeyCode.ENTER) && (!isPnAuthCompleted)) {
                loginBtnAction();
            }
            if (isPnAuthCompleted && cardPasswordField.getText().length() == 4) {
                cardPasswordField.setDisable(true);
                authenticateByPin(cardPasswordField.getText());
            }
            event.consume();
        });
        App.setNudLogin(false);
    }

    private void authenticateByPin(String pinCode) {
        try {
            Asn1CardTokenUtil.verifyPin(handle, pinCode);
            App.setRoot("main_screen");
        } catch (NoReaderOrCardException | GenericException ex) {
            Platform.runLater(() -> cardPasswordField.clear());
            updateUI(ex.getMessage());
            enableControls(backBtn, cardPasswordField);
        } catch (ConnectionTimeoutException ex) {
            updateUI("Something went wrong. Kindly check Card API service.");
            Platform.runLater(() -> cardPasswordField.clear());
            enableControls(backBtn, cardPasswordField);
        } catch (Exception e) {
            // by App.setRoot()
            LOGGER.log(Level.INFO, () -> "Error: " + e.getMessage());
            updateUI("Something went wrong. Kindly contact system admin.");
        }
    }

    @FXML
    private void backBtnAction() throws IOException {
        App.setRoot("login");
    }

    private void startAuthentication() {
        try {
            fileTypeToAsn1EncodedByteArrayMap = startProcedureCallForPN();
        } catch (NoReaderOrCardException | GenericException ex) {
            cardPasswordField.clear();
            updateUI(ex.getMessage());
            enableControls(backBtn, loginBtn);
            return;
        } catch (ConnectionTimeoutException ex) {
            updateUI("Something went wrong. Kindly check Card API service.");
            enableControls(backBtn, loginBtn);
            return;
        }

        try {
            if (!isPnAuthCompleted) {
                authenticateByPN(fileTypeToAsn1EncodedByteArrayMap.get(CardTokenFileType.STATIC));
            }
            isPnAuthCompleted = true;
            enableControls(backBtn);
            Platform.runLater(() -> {
                messageLabel.setText("Kindly enter the PIN to continue.");
                cardLabel.setText("    PIN:");
                cardPasswordField.clear();
            });
        } catch (Exception ex) {
            cardPasswordField.clear();
            updateUI(ex.getMessage());
            enableControls(backBtn, loginBtn);
        }
    }

    @FXML
    public void loginBtnAction() throws IllegalStateException {
        if (cardPasswordField.getText().isBlank() || cardPasswordField.getText().length() < 4) {
            messageLabel.setText("Please enter valid number.");
            return;
        }
        disableControls(backBtn, loginBtn);
        App.getThreadPool().execute(this::startAuthentication);
    }

    // throws GenericException
    // Caller must handle the exception
    private void authenticateByPN(byte[] bytes) {
        String pNumber;
        String cardNumber;
        try {
            pNumber = new String(Asn1CardTokenUtil.extractFromAsn1EncodedStaticData(bytes, CardStaticDataIndex.PN.getValue()), StandardCharsets.UTF_8);
            cardNumber = new String(Asn1CardTokenUtil.extractFromAsn1EncodedStaticData(bytes, CardStaticDataIndex.CARD_NUMBER.getValue()), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Received a null value for PN or Card Number from card.");
            throw new GenericException("Kindly place a valid card and try again.");
        }
        // gets pin code from card
        if (pNumber.isBlank() || cardNumber.isBlank()) {
            LOGGER.log(Level.SEVERE, "Received an empty value for PN or Card Number from card.");
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

        if (!pNumber.equalsIgnoreCase(cardPasswordField.getText())) {
            LOGGER.log(Level.INFO, "Personal Number and user input number does not matched.");
            throw new GenericException("Invalid Personal Number.");
        }
    }

    private boolean restartApiService() {
        try {
            Process pr = Runtime.getRuntime().exec(CARD_API_SERVICE_RESTART_COMMAND);
            int exitCode = pr.waitFor();
            LOGGER.log(Level.INFO, () -> "****EnrollmentStationServices restart exit code: " + exitCode);
            return exitCode == 0;
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GenericException(GENERIC_ERR_MSG);
        }

    }

    private EnumMap<CardTokenFileType, byte[]> startProcedureCallForPN() {
        // required to follow the procedure calls
        // deInitialize -> initialize ->[waitForConnect -> selectApp] -> readData
        CRWaitForConnectResDto crWaitForConnectResDto;
        int counter = 1;
        // restart Naval_WebServices if failed on the first WaitForConnect call.
        while (true) {
            counter--;
            Asn1CardTokenUtil.deInitialize();
            Asn1CardTokenUtil.initialize();
            try {
                Thread.sleep(SLEEP_TIME_BEFORE_WAIT_FOR_CONNECT_CALL_IN_MIL_SEC);
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "****BeforeWaitSleep: Interrupted while sleeping.");
                Thread.currentThread().interrupt();
            }

            try {
                crWaitForConnectResDto = Asn1CardTokenUtil.waitForConnect(MANTRA_CARD_READER_NAME);
                break;
                // only catch GenericException for restart
            } catch (GenericException ex) {
                if (counter == 0) {
                    LOGGER.log(Level.INFO, () -> "****Communication error occurred. Restarting EnrollmentStationServices.");
                    if (restartApiService()) {
                        try {
                            Thread.sleep(2000); // needed to sleep after restarting
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        continue; // starts from DeInitialize again.
                    } // else exit code is not zero
                    else {
                        LOGGER.log(Level.INFO, () -> "***Card: Unable to restart EnrollmentStationServices.");
                    }
                }
                LOGGER.log(Level.INFO, () -> "****Communication error occurred even after restarting EnrollmentStationServices.");
                throw new GenericException(ex.getMessage());
            }
        }
        if (crWaitForConnectResDto.getRetVal() != 0) {
            throw new GenericException("Kindly reconnect the reader and place card correctly.");
        }
        handle = crWaitForConnectResDto.getHandle();
        Asn1CardTokenUtil.selectApp(CARD_TYPE_NUMBER, crWaitForConnectResDto.getHandle());
        EnumMap<CardTokenFileType, byte[]> fileTypeByteArrayMap = new EnumMap<>(CardTokenFileType.class);
        fileTypeByteArrayMap.put(CardTokenFileType.STATIC, readBufferedData(crWaitForConnectResDto.getHandle(), CardTokenFileType.STATIC));
        return fileTypeByteArrayMap;
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

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
    }

}
