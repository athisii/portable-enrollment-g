package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.CardWhitelistDetail;
import com.cdac.enrollmentstation.dto.UserReqDto;
import com.cdac.enrollmentstation.exception.ConnectionTimeoutException;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */

public class ServerConfigController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(ServerConfigController.class);
    @FXML
    private Label serialNoOfDevice;
    @FXML
    private Label unitCaptionLabel;

    @FXML
    private Button downloadWhitelistedCardBtn;

    @FXML
    private TextField mafisUrlTextField;

    @FXML
    private Label enrollmentStationIdTextField;


    @FXML
    private Label messageLabel;

    @FXML
    private Button validateBtn;


    @FXML
    private Button backBtn;

    @FXML
    private Button homeBtn;
    private static final String WHITELISTED_CARD_FILE_PATH = PropertyFile.getProperty(PropertyName.CARD_WHITELISTED_FILE);


    @FXML
    private void homeBtnAction() throws IOException {
        App.setRoot("main_screen");
    }

    @FXML
    private void backBtnAction() throws IOException {
        App.setRoot("admin_config");
    }

    @FXML
    private void validateBtnAction() {
        homeBtn.requestFocus();
        messageLabel.setText("Validating MAFIS URL...");
        disableControls(backBtn, homeBtn, validateBtn, mafisUrlTextField, downloadWhitelistedCardBtn);
        App.getThreadPool().execute(() -> validateServer(mafisUrlTextField.getText()));
    }

    private void validateServer(String mafisUrl) {
        String oldMafisUrl = PropertyFile.getProperty(PropertyName.MAFIS_API_URL);
        PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, mafisUrl);
        try {
            // Hardware Type Mapping:
            //      PES - 1
            //      FES - 2
            MafisServerApi.validateUserCategory(new UserReqDto(App.getPno(), PropertyFile.getProperty(PropertyName.DEVICE_SERIAL_NO), "2", PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID)));
            LOGGER.info("Done validating user category.");
        } catch (GenericException ex) {
            updateUi(ex.getMessage());
            enableControls(backBtn, homeBtn, validateBtn, mafisUrlTextField, downloadWhitelistedCardBtn);
            PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, oldMafisUrl);
            Platform.runLater(() -> mafisUrlTextField.setText(oldMafisUrl));
            return;
        } catch (ConnectionTimeoutException ex) {
            Platform.runLater(() -> {
                messageLabel.setText("Connection timeout. Please try again.");
                enableControls(backBtn, homeBtn, validateBtn, mafisUrlTextField, downloadWhitelistedCardBtn);
                mafisUrlTextField.setText(oldMafisUrl);
            });
            PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, oldMafisUrl);
            return;
        }
        updateUi("MAFIS URL updated successfully.");
        enableControls(backBtn, homeBtn, validateBtn, mafisUrlTextField, downloadWhitelistedCardBtn);
    }

    private void downloadWhitelistedCardBtnAction() {
        messageLabel.setText("Downloading operators card.");
        homeBtn.requestFocus();
        disableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, validateBtn, mafisUrlTextField);
        // should only download allowed personal number.
        App.getThreadPool().execute(() -> {
            List<CardWhitelistDetail> cardWhitelistDetails;
            try {
                cardWhitelistDetails = MafisServerApi.fetchWhitelistedCard();
            } catch (GenericException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
                updateUi(ex.getMessage());
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, validateBtn, mafisUrlTextField);
                return;
            } catch (ConnectionTimeoutException ex) {
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, validateBtn, mafisUrlTextField);
                updateUi("Connection timeout or received an unexpected value from the server.");
                return;
            }
            try {
                String cardWhitelistedString = Singleton.getObjectMapper().writeValueAsString(cardWhitelistDetails);
                Files.writeString(Paths.get(WHITELISTED_CARD_FILE_PATH), cardWhitelistedString, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                updateUi(GENERIC_ERR_MSG);
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, validateBtn, mafisUrlTextField);
                return;
            }
            updateUi("Operators card downloaded successfully.");
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, validateBtn, mafisUrlTextField);
        });
    }

    // calls automatically by JavaFX runtime
    public void initialize() {
        String commonText = " is required in " + ApplicationConstant.DEFAULT_PROPERTY_FILE + ".";
        String errorMessage = "";
        String whitelistedCardUrl = PropertyFile.getProperty(PropertyName.CARD_API_WHITELISTED_URL);
        String mafisUrl = PropertyFile.getProperty(PropertyName.MAFIS_API_URL);
        String enrollmentStationId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID);
        String enrollmentStationUnitId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID);
        String enrollmentStationUnitCaption = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_CAPTION);
        String deviceSerialNumber = PropertyFile.getProperty(PropertyName.DEVICE_SERIAL_NO);
        if (whitelistedCardUrl.isBlank()) {
            errorMessage += PropertyName.CARD_API_WHITELISTED_URL + commonText;
        }
        if (mafisUrl.isBlank()) {
            errorMessage += PropertyName.MAFIS_API_URL + commonText;
        }
        if (enrollmentStationId.isBlank()) {
            errorMessage += "\n" + PropertyName.ENROLLMENT_STATION_ID + commonText;
        }
        if (enrollmentStationUnitId.isBlank()) {
            errorMessage += "\n" + PropertyName.ENROLLMENT_STATION_UNIT_ID + commonText;
        }
        if (enrollmentStationUnitCaption.isBlank()) {
            errorMessage += "\n" + PropertyName.ENROLLMENT_STATION_UNIT_CAPTION + commonText;
        }
        if (deviceSerialNumber.isBlank()) {
            errorMessage += "\n" + PropertyName.DEVICE_SERIAL_NO + commonText;
        }
        if (!errorMessage.isBlank()) {
            throw new GenericException(errorMessage);
        }
        mafisUrlTextField.setText(mafisUrl);
        enrollmentStationIdTextField.setText(enrollmentStationId);
        unitCaptionLabel.setText(enrollmentStationUnitCaption);
        serialNoOfDevice.setText(deviceSerialNumber);
        downloadWhitelistedCardBtn.setOnAction(event -> downloadWhitelistedCardBtnAction());

        // hides in prod
//        if ("0".equals(PropertyFile.getProperty(PropertyName.ENV))) {
//            validateBtn.setManaged(false);
//            mafisUrlTextField.setDisable(false);
//            mafisUrlTextField.setEditable(false);
//        }
    }

    private void updateUi(String message) {
        Platform.runLater((() -> messageLabel.setText(message)));
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

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
        enableControls(backBtn, validateBtn, homeBtn, downloadWhitelistedCardBtn, mafisUrlTextField);
        updateUi("Received an invalid data from the server.");
    }
}
