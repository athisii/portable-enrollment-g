package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.ServerAPI;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.Unit;
import com.cdac.enrollmentstation.util.PropertyFile;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class ServerConfigController {
    private static final Logger LOGGER = ApplicationLog.getLogger(ServerConfigController.class);
    private Unit unit;
    private List<Unit> units;

    @FXML
    private TextField mafisUrlTextField;

    @FXML
    private TextField enrollmentStationIdTextField;


    @FXML
    private Label labelStatus;

    @FXML
    private Button fetchUnitsBtn;


    @FXML
    private ComboBox<String> enrollmentStationUnitIdsComboBox;
    @FXML
    private Button backBtn;

    @FXML
    private Button editBtn;
    @FXML
    private Button updateUnitBtn;
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

    @FXML
    private void updateBtnAction() {
        if (mafisUrlTextField.getText().isBlank() || isMalformedUrl(mafisUrlTextField.getText())) {
            labelStatus.setText("Invalid mafis url.");
            return;
        }
        if (enrollmentStationIdTextField.getText().isBlank()) {
            labelStatus.setText("Enrollment station id is empty.");
            return;
        }

        if (unit == null) {
            labelStatus.setText("Kindly select a unit.");
            return;
        }

        PropertyFile.changePropertyValue(PropertyName.MAFIS_API_URL, mafisUrlTextField.getText());
        PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_ID, enrollmentStationIdTextField.getText());
        PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_UNIT_ID, unit.getValue());
        labelStatus.setText("Updated successfully.");

        disableControls(updateUnitBtn);

    }


    @FXML
    private void fetchBtnAction() {
        if (isMalformedUrl(mafisUrlTextField.getText())) {
            labelStatus.setText(("Not a valid url."));
            return;
        }
        labelStatus.setText("Fetching units...");
        disableControls(backBtn, homeBtn, editBtn, updateUnitBtn, fetchUnitsBtn);
        ForkJoinPool.commonPool().execute(this::fetchUnits);

    }

    private void fetchUnits() {
        try {
            units = ServerAPI.fetchAllUnits();
            if (units.isEmpty()) {
                updateUI("No units for selected mafis url.");
                return;
            }
            List<String> captions = units.stream().map(Unit::getCaption).collect(Collectors.toList());
            enrollmentStationUnitIdsComboBox.setItems(FXCollections.observableArrayList(captions));
            enrollmentStationUnitIdsComboBox.getSelectionModel().select(0);
            updateUI("Units fetched successfully.");
        } catch (GenericException ex) {
            updateUI("Connection timeout. Failed to connect to server.");
        }
        enableControls(backBtn, homeBtn, editBtn, updateUnitBtn, fetchUnitsBtn);

    }

    // calls automatically by JavaFX runtime
    public void initialize() {
        String commonText = " is required in " + ApplicationConstant.DEFAULT_PROPERTY_FILE + ".";
        String errorMessage = "";
        String mafisUrl = PropertyFile.getProperty(PropertyName.MAFIS_API_URL);
        String enrollmentStationId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID);
        String enrollmentStationUnitId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID);
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
        System.out.println(mafisUrl);
        System.out.println(enrollmentStationId);
        mafisUrlTextField.setText(mafisUrl);
        enrollmentStationIdTextField.setText(enrollmentStationId);
        enrollmentStationUnitIdsComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // sometimes old and new value will be null.
            if (oldValue != null && newValue != null) {
                if (!newValue.equals(oldValue)) {
                    Optional<Unit> unitOptional = units.stream().filter(u -> u.getCaption().equals(newValue)).findFirst();
                    unitOptional.ifPresent(u -> unit = u);
                    updateUI(newValue + " unit selected.");
                }

            }
            disableControls(fetchUnitsBtn);
        });

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
        Platform.runLater((() -> labelStatus.setText(message)));
    }


    private void disableControls(Node... nodes) {
        Platform.runLater((() -> {
            for (Node node : nodes) {
                node.setDisable(true);
            }
        }));
    }

    private void enableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }
}
