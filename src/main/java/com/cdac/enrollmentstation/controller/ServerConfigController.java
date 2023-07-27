package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.CardWhitelistApi;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.ConnectionTimeoutException;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.CardWhitelistDetail;
import com.cdac.enrollmentstation.model.Unit;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */

public class ServerConfigController {
    private static final Logger LOGGER = ApplicationLog.getLogger(ServerConfigController.class);
    private List<Unit> units;
    @FXML
    private TextField whitelistedCardUrlTextField;
    @FXML
    private Button downloadWhitelistedCardBtn;

    @FXML
    private TextField mafisUrlTextField;

    @FXML
    private TextField enrollmentStationIdTextField;


    @FXML
    private Label messageLabel;

    @FXML
    private Button fetchUnitsBtn;


    @FXML
    private ComboBox<String> enrollmentStationUnitIdsComboBox;
    @FXML
    private Button backBtn;

    @FXML
    private Button homeBtn;


    @FXML
    public void homeBtnAction() throws IOException {
        App.setRoot("main_screen");
    }

    @FXML
    public void backBtnAction() throws IOException {
        App.setRoot("admin_config");
    }


    @FXML
    private void editBtnAction() {
        updateUI("");
        enableControls(mafisUrlTextField, enrollmentStationIdTextField, enrollmentStationUnitIdsComboBox, fetchUnitsBtn);
    }


    private void saveToFile(Unit unit) {
        if (mafisUrlTextField.getText().isBlank() || isMalformedUrl(mafisUrlTextField.getText())) {
            messageLabel.setText("Invalid mafis url.");
            return;
        }
        if (enrollmentStationIdTextField.getText().isBlank()) {
            messageLabel.setText("Enrolment station id is empty.");
            return;
        }

        PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, mafisUrlTextField.getText());
        PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_ID, enrollmentStationIdTextField.getText());
        PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_UNIT_ID, unit.getValue());
        messageLabel.setText("Updated successfully.");

    }


    @FXML
    private void fetchBtnAction() {
        if (isMalformedUrl(mafisUrlTextField.getText())) {
            messageLabel.setText(("Not a valid url."));
            return;
        }
        homeBtn.requestFocus();
        messageLabel.setText("Fetching units...");
        disableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
        App.getThreadPool().execute(this::fetchUnits);

    }

    private void downloadWhitelistedCardBtnAction() {
        if (isMalformedUrl(whitelistedCardUrlTextField.getText())) {
            messageLabel.setText(("Not a valid url."));
            return;
        }
        String whitelistedCardFilePath = PropertyFile.getProperty(PropertyName.CARD_WHITELISTED_FILE);
        if (whitelistedCardFilePath.isBlank()) {
            LOGGER.log(Level.SEVERE, () -> PropertyName.CARD_WHITELISTED_FILE + " is empty or no entry found in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(PropertyName.CARD_WHITELISTED_FILE + " is empty or no entry found in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        messageLabel.setText("Downloading operators card.");
        homeBtn.requestFocus();
        disableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
        // should only download allowed personal number.
        App.getThreadPool().execute(() -> {
            List<CardWhitelistDetail> cardWhitelistDetails;
            try {
                cardWhitelistDetails = CardWhitelistApi.fetchWhitelistedCard();
            } catch (GenericException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
                updateUI(ex.getMessage());
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
                return;
            } catch (ConnectionTimeoutException ex) {
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
                updateUI("Connection timeout or received an unexpected value from server.");
                return;
            }
            try {
                String cardWhitelistedString = Singleton.getObjectMapper().writeValueAsString(cardWhitelistDetails);
                Files.writeString(Paths.get(whitelistedCardFilePath), cardWhitelistedString, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                updateUI(GENERIC_ERR_MSG);
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
            }
            updateUI("Operators card downloaded successfully.");
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
        });
    }

    private void fetchUnits() {
        try {
            units = MafisServerApi.fetchAllUnits();
        } catch (GenericException ex) {
            updateUI(ex.getMessage());
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
            return;
        } catch (ConnectionTimeoutException ex) {
            Platform.runLater(() -> {
                messageLabel.setText("Connection timeout. Please try again.");
                enrollmentStationUnitIdsComboBox.getItems().clear();
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
            });
            return;
        }

        if (units.isEmpty()) {
            updateUI("No units for selected mafis url.");
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
            return;
        }
        List<String> captions = units.stream().map(Unit::getCaption).collect(Collectors.toList());
        Platform.runLater(() -> enrollmentStationUnitIdsComboBox.setItems(FXCollections.observableArrayList(captions)));
        updateUI("Units fetched successfully.");
        enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);

    }

    // calls automatically by JavaFX runtime
    public void initialize() {
        String commonText = " is required in " + ApplicationConstant.DEFAULT_PROPERTY_FILE + ".";
        String errorMessage = "";
        String whitelistedCardUrl = PropertyFile.getProperty(PropertyName.CARD_API_WHITELISTED_URL);
        String mafisUrl = PropertyFile.getProperty(PropertyName.MAFIS_API_URL);
        String enrollmentStationId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID);
        String enrollmentStationUnitId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID);
        if (whitelistedCardUrl == null || whitelistedCardUrl.isBlank()) {
            errorMessage += PropertyName.CARD_API_WHITELISTED_URL + commonText;
        }
        if (mafisUrl == null || mafisUrl.isBlank()) {
            errorMessage += PropertyName.MAFIS_API_URL + commonText;
        }
        if (enrollmentStationId == null || enrollmentStationId.isBlank()) {
            errorMessage += "\n" + PropertyName.ENROLLMENT_STATION_ID + commonText;
        }
        if (enrollmentStationUnitId == null || enrollmentStationUnitId.isBlank()) {
            errorMessage += "\n" + PropertyName.ENROLLMENT_STATION_UNIT_ID + commonText;
        }
        if (!errorMessage.isBlank()) {
            throw new GenericException(errorMessage);
        }
        whitelistedCardUrlTextField.setText(whitelistedCardUrl);
        mafisUrlTextField.setText(mafisUrl);
        enrollmentStationIdTextField.setText(enrollmentStationId);
        enrollmentStationUnitIdsComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // sometimes old and new value will be null.
            if (newValue != null) {
                Optional<Unit> unitOptional = units.stream().filter(u -> u.getCaption().equals(newValue)).findFirst();
                unitOptional.ifPresent(this::saveToFile);
            }
        });
        // very important, fetchApi returns data based on previously saved url.
        mafisUrlTextField.setOnKeyReleased(event -> {
            String url = mafisUrlTextField.getText();
            if (!url.isBlank() && !isMalformedUrl(url)) {
                PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, url);
            }
        });

        whitelistedCardUrlTextField.setOnKeyReleased(event -> {
            String url = whitelistedCardUrlTextField.getText();
            if (!url.isBlank() && !isMalformedUrl(url)) {
                PropertyFile.changePropertyValue(PropertyName.CARD_API_WHITELISTED_URL, url);
            }
        });
        downloadWhitelistedCardBtn.setOnAction(event -> downloadWhitelistedCardBtnAction());
    }

    public boolean isMalformedUrl(String url) {
        try {
            new URL(url).toURI();
            return false;
        } catch (MalformedURLException | URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            return true;
        }
    }

    private void updateUI(String message) {
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
}
