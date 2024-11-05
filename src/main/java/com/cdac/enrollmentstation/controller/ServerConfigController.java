package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.CardWhitelistDetail;
import com.cdac.enrollmentstation.dto.Unit;
import com.cdac.enrollmentstation.exception.ConnectionTimeoutException;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private ImageView downArrowImageView;
    @FXML
    private ImageView upArrowImageView;
    @FXML
    private Label unitCaptionLabel;
    @FXML
    private HBox unitIdDropDownHBox;
    @FXML
    private VBox hiddenVbox;
    private List<Unit> units = new ArrayList<>();
    private List<String> sortedCaptions = new ArrayList<>();

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
    private Button backBtn;

    @FXML
    private Button homeBtn;
    private static final String WHITELISTED_CARD_FILE_PATH = PropertyFile.getProperty(PropertyName.CARD_WHITELISTED_FILE);


    @FXML
    private void homeBtnAction() throws IOException {
        if (App.getHostnameChanged() && "0".equalsIgnoreCase(PropertyFile.getProperty(PropertyName.INITIAL_SETUP))) {
            App.setHostnameChanged(false);
            disableControls(mafisUrlTextField, enrollmentStationIdTextField, fetchUnitsBtn, homeBtn, unitIdDropDownHBox, downloadWhitelistedCardBtn);
            App.getThreadPool().execute(this::rebootSystem);
        } else if (App.isNudLogin()) {
            App.setRoot("main_screen");
        } else {
            App.setRoot("login");
        }
    }

    @FXML
    private void backBtnAction() throws IOException {
        App.setRoot("admin_config");
    }

    private void saveUnitIdAndCaption(Unit unit) {
        PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_UNIT_ID, unit.getValue());
        PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_UNIT_CAPTION, unit.getCaption());
        PropertyFile.changePropertyValue(PropertyName.INITIAL_SETUP, "0"); // initial setup done.
        messageLabel.setText("Enrolment Station Unit ID updated successfully.");
    }


    @FXML
    private void fetchBtnAction() {
        if (hiddenVbox.isVisible()) {
            hiddenVbox.getChildren().remove(0, hiddenVbox.getChildren().size());
            hiddenVbox.setVisible(false);
            upArrowImageView.setVisible(false);
            downArrowImageView.setVisible(true);
        }
        homeBtn.requestFocus();
        messageLabel.setText("Fetching units...");
        disableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, unitIdDropDownHBox, fetchUnitsBtn);
        sortedCaptions = new ArrayList<>();
        units = new ArrayList<>();
        App.getThreadPool().execute(this::fetchUnits);
    }

    private void fetchUnits() {
        try {
            units = MafisServerApi.fetchAllUnits();
        } catch (GenericException ex) {
            updateUi(ex.getMessage());
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, unitIdDropDownHBox, fetchUnitsBtn);
            return;
        } catch (ConnectionTimeoutException ex) {
            Platform.runLater(() -> {
                messageLabel.setText("Connection timeout. Please try again.");
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, unitIdDropDownHBox, fetchUnitsBtn);
            });
            return;
        }

        if (units.isEmpty()) {
            updateUi("No units for selected mafis url.");
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, unitIdDropDownHBox, fetchUnitsBtn);
            return;
        }
        sortedCaptions = units.stream().map(Unit::getCaption).sorted().toList();
        updateUi("Units fetched successfully.");
        enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, unitIdDropDownHBox, fetchUnitsBtn);
    }


    private void downloadWhitelistedCardBtnAction() {
        messageLabel.setText("Downloading operators card.");
        homeBtn.requestFocus();
        disableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
        // should only download allowed personal number.
        App.getThreadPool().execute(() -> {
            List<CardWhitelistDetail> cardWhitelistDetails;
            try {
                cardWhitelistDetails = MafisServerApi.fetchWhitelistedCard();
            } catch (GenericException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
                updateUi(ex.getMessage());
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
                return;
            } catch (ConnectionTimeoutException ex) {
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
                updateUi("Connection timeout or received an unexpected value from the server.");
                return;
            }
            try {
                String cardWhitelistedString = Singleton.getObjectMapper().writeValueAsString(cardWhitelistDetails);
                Files.writeString(Paths.get(WHITELISTED_CARD_FILE_PATH), cardWhitelistedString, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                updateUi(GENERIC_ERR_MSG);
                enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
                return;
            }
            updateUi("Operators card downloaded successfully.");
            enableControls(backBtn, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
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
        if (!errorMessage.isBlank()) {
            throw new GenericException(errorMessage);
        }
        mafisUrlTextField.setText(mafisUrl);
        enrollmentStationIdTextField.setText(enrollmentStationId);
        unitCaptionLabel.setText(enrollmentStationUnitCaption);

        enrollmentStationIdTextField.textProperty().addListener((observable, oldValue, newValue) -> saveEnrollmentStationId(newValue));
        mafisUrlTextField.textProperty().addListener((observable, oldValue, newValue) -> saveMafisUrl(newValue));

        downloadWhitelistedCardBtn.setOnAction(event -> downloadWhitelistedCardBtnAction());
        unitIdDropDownHBox.setOnMouseClicked(this::toggleUnitCaptionListView);

        // Initial Setup check
        if ("1".equals(PropertyFile.getProperty(PropertyName.INITIAL_SETUP).trim())) {
            mafisUrlTextField.setDisable(false);
            enrollmentStationIdTextField.setDisable(false);
            fetchUnitsBtn.setDisable(false);
            backBtn.setManaged(false);
            backBtn.setVisible(false);
        }
    }

    private void toggleUnitCaptionListView(MouseEvent mouseEvent) {
        if (sortedCaptions.isEmpty() || units.isEmpty()) {
            return;
        }
        if (hiddenVbox.isVisible()) {
            hiddenVbox.getChildren().remove(0, hiddenVbox.getChildren().size());
            hiddenVbox.setVisible(false);
            upArrowImageView.setVisible(false);
            downArrowImageView.setVisible(true);
        } else {
            hiddenVbox.setVisible(true);
            downArrowImageView.setVisible(false);
            upArrowImageView.setVisible(true);
            TextField sarchTextField = new TextField();
            sarchTextField.setPromptText("Search");
            hiddenVbox.getChildren().add(sarchTextField);
            ListView<String> listView = new ListView<>();
            listView.setItems(FXCollections.observableArrayList(sortedCaptions));
            listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Optional<Unit> unitOptional = units.stream().filter(u -> u.getCaption().equals(newValue)).findFirst();
                    unitOptional.ifPresent(this::saveUnitIdAndCaption);
                    unitCaptionLabel.setText(newValue);
                    hiddenVbox.getChildren().remove(0, hiddenVbox.getChildren().size());
                    hiddenVbox.setVisible(false);
                    downArrowImageView.setVisible(true);
                    upArrowImageView.setVisible(false);
                }
            });
            sarchTextField.textProperty().addListener((observable, oldVal, newVal) -> searchFilter(newVal, listView));
            hiddenVbox.getChildren().add(1, listView);
        }
    }

    private void searchFilter(String value, ListView<String> listView) {
        if (value.isEmpty()) {
            listView.setItems(FXCollections.observableList(units.stream().map(Unit::getCaption).toList()));
            return;
        }
        String valueUpper = value.toUpperCase();
        listView.setItems(FXCollections.observableList(units.stream().map(Unit::getCaption).filter(caption -> caption.toUpperCase().contains(valueUpper)).toList()));
    }


    private void saveMafisUrl(String newValue) {
        if (newValue != null && !newValue.isBlank()) {
            PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, newValue);
            enableControls(backBtn, enrollmentStationIdTextField, unitIdDropDownHBox, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
            if (hiddenVbox.isVisible()) {
                hiddenVbox.setDisable(false);
            }
            updateUi("Mafis API Server Url updated successfully");
        } else {
            updateUi("Enter a valid Mafis API Server Url");
            disableControls(backBtn, homeBtn, enrollmentStationIdTextField, unitIdDropDownHBox, downloadWhitelistedCardBtn, fetchUnitsBtn);
            if (hiddenVbox.isVisible()) {
                hiddenVbox.setDisable(true);
            }
        }
    }

    private void saveEnrollmentStationId(String newValue) {
        if (newValue != null && !newValue.isBlank()) {
            PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_ID, newValue);
            enableControls(backBtn, mafisUrlTextField, unitIdDropDownHBox, homeBtn, downloadWhitelistedCardBtn, fetchUnitsBtn);
            if (hiddenVbox.isVisible()) {
                hiddenVbox.setDisable(false);
            }
            updateUi("Enrolment Station ID updated successfully");
        } else {
            updateUi("Enter a valid Enrolment Station ID");
            disableControls(backBtn, homeBtn, mafisUrlTextField, unitIdDropDownHBox, downloadWhitelistedCardBtn, fetchUnitsBtn);
            if (hiddenVbox.isVisible()) {
                hiddenVbox.setDisable(true);
            }
        }
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

    private void rebootSystem() {
        try {
            int counter = 5;
            while (counter >= 1) {
                updateUi("Rebooting system to take effect in " + counter + " second(s)...");
                Thread.sleep(1000);
                counter--;
            }
            Process process = Runtime.getRuntime().exec("reboot");
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String eline;
            while ((eline = error.readLine()) != null) {
                String finalEline = eline;
                LOGGER.log(Level.INFO, () -> "***Error: " + finalEline);
            }
            error.close();
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                LOGGER.log(Level.INFO, () -> "***Error: Process Exit Value: " + exitVal);
                updateUi(ApplicationConstant.GENERIC_ERR_MSG);
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(Level.INFO, () -> "**Error while rebooting: " + ex.getMessage());
            updateUi(ApplicationConstant.GENERIC_ERR_MSG);
        }
        disableControls(mafisUrlTextField, enrollmentStationIdTextField, fetchUnitsBtn, homeBtn, unitIdDropDownHBox, downloadWhitelistedCardBtn);
    }

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
        enableControls(backBtn, fetchUnitsBtn, homeBtn, downloadWhitelistedCardBtn);
        updateUi("Received an invalid data from the server.");
    }
}
