package com.cdac.enrollmentstation.controller;


import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.ArcDetail;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResDto;
import com.cdac.enrollmentstation.dto.Unit;
import com.cdac.enrollmentstation.exception.ConnectionTimeoutException;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.AesFileUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class ImportExportController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(ImportExportController.class);
    private static final String TIMEOUT_ERR_MSG = "Connection timeout. Please try again.";

    private static final String IMPORTED_TEXT = "IMPORTED: ";
    private static final String CAPTURED_BIOMETRIC_TEXT = "CAPTURED BIOMETRIC: ";
    @FXML
    private Button homeBtn;
    @FXML
    private Label importedUnitText;
    @FXML
    private Label capturedBiometricText;
    @FXML
    private Button exportBtn;
    @FXML
    private Button backBtn;
    @FXML
    private Button importUnitBtn;
    @FXML
    private ListView<String> importedUnitListView;
    @FXML
    private Button clearImportBtn;
    @FXML
    private Button clearAllImportBtn;
    @FXML
    private ListView<String> capturedArcListView;
    private final List<Unit> allUnits = new ArrayList<>();
    private final List<String> selectedUnits = new ArrayList<>();

    @FXML
    private Label messageLabel;
    @FXML
    private ImageView refreshIcon;

    @FXML
    private ListView<String> unitListView;
    @FXML
    private TextField searchText;


    public void initialize() {
        refreshIcon.setOnMouseClicked(mouseEvent -> refresh());
        importUnitBtn.setOnAction(event -> importSelectedUnits());
        clearImportBtn.setOnAction(event -> clearSingleImportedUnit());
        clearAllImportBtn.setOnAction(event -> clearAllImportedUnits());
        exportBtn.setOnAction(event -> exportBtnAction());

        searchText.textProperty().addListener((observable, oldVal, newVal) -> searchFilter(newVal));
        // should not allow multiple selections of units
        // some units might not have Arc number
        // error message will get override if multiple units are selected
        unitListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedUnits.clear();
            selectedUnits.addAll(new ArrayList<>(unitListView.getSelectionModel().getSelectedItems()));
        });
        disableControls(importUnitBtn, clearImportBtn, clearAllImportBtn, exportBtn);
        messageLabel.setText("Fetching units.....");
        App.getThreadPool().execute(this::fetchAllUnits);
        App.getThreadPool().execute(this::updateImportedListView);
        App.getThreadPool().execute(this::updateCapturedBiometric);
        capturedArcListView.addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);
    }

    private void exportBtnAction() {
        messageLabel.setText("Exporting. Please Wait...");
        disableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
        App.getThreadPool().execute(this::exportData);
    }

    private void exportData() {
        List<Path> encryptedArcPaths;
        try {
            encryptedArcPaths = getEncryptedArcPaths();
        } catch (GenericException ex) {
            updateUI(ex.getMessage());
            enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
            return;
        }
        if (encryptedArcPaths.isEmpty()) {
            updateUI("No e-ARC to be exported.");
            enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
            return;
        }
        decryptAndSendToServer(encryptedArcPaths);
    }

    private List<Path> getEncryptedArcPaths() {
        String encFolderString = PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER);
        if (encFolderString.isBlank()) {
            LOGGER.log(Level.SEVERE, "Entry for '" + PropertyName.ENC_EXPORT_FOLDER + "' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        Path encFolderPath = Paths.get(encFolderString);

        List<Path> encryptedArcPaths;
        // collects encrypted arc files to List
        try (Stream<Path> encFolderStream = Files.walk(encFolderPath)) {
            encryptedArcPaths = encFolderStream.filter(path -> {
                if (Files.isRegularFile(path)) {
                    String[] splitFilename = path.getFileName().toString().split("\\.");
                    // return only this format -> 00-A-AA.json.enc
                    return splitFilename.length == 3;
                }
                return false;
            }).collect(Collectors.toList());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }
        return encryptedArcPaths;
    }


    @FXML
    public void home() throws IOException {
        App.setRoot("main_screen");

    }

    private void decryptAndSendToServer(List<Path> paths) {
        String decryptedJsonData;
        for (Path path : paths) {
            String arcNumber = path.getFileName().toString().split("\\.")[0];
            updateUI("Exporting e-ARC: " + arcNumber);
            try {
                decryptedJsonData = AesFileUtil.decrypt(path);
            } catch (GenericException ignored) {
                LOGGER.log(Level.SEVERE, () -> "Error decrypting arc: " + arcNumber);
                try {
                    Files.delete(path);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage());
                    updateUI(GENERIC_ERR_MSG);
                    enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
                    return;
                }
                continue;
            }

            SaveEnrollmentResDto saveEnrollmentResDto;
            LOGGER.log(Level.INFO, () -> "***Sending biometric data to the server for e-ARC:" + arcNumber);
            try {
                saveEnrollmentResDto = MafisServerApi.postEnrollment(decryptedJsonData);
            } catch (GenericException ex) {
                updateUI(ex.getMessage());
                enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
                continue;
            } catch (ConnectionTimeoutException ex) {
                updateUI(TIMEOUT_ERR_MSG);
                enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
                return;
            }
            LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + saveEnrollmentResDto.getErrorCode());
            if (saveEnrollmentResDto.getErrorCode() != 0) {
                String errorMessage = saveEnrollmentResDto.getDesc().toLowerCase();
                LOGGER.log(Level.SEVERE, () -> "Error Desc: " + errorMessage);
                if (saveEnrollmentResDto.getErrorCode() == -1) {
                    continue;
                }
            }
            try {
                Files.delete(path);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
                updateUI(GENERIC_ERR_MSG);
                enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
                return;
            }
        }

        List<Path> encryptedArcPaths;
        try {
            encryptedArcPaths = getEncryptedArcPaths();
        } catch (GenericException ex) {
            updateUI(ex.getMessage());
            enableControls(importUnitBtn, backBtn, homeBtn, clearImportBtn, clearAllImportBtn, exportBtn);
            return;
        }
        updateCapturedBiometric();
        if (encryptedArcPaths.isEmpty()) {
            clearAllImportedUnits();
            updateUI("Record(s) exported successfully.");
        } else {
            updateUI("Unable to export all captured biometrics. Kindly try again.");
            enableControls(exportBtn, clearImportBtn, clearAllImportBtn);
        }
        enableControls(homeBtn, backBtn, importUnitBtn);
    }

    @FXML
    public void back() throws IOException {
        App.setRoot("main_screen");

    }

    public void refresh() {
        messageLabel.setText("Fetching units....");
        disableControls(importUnitBtn);
        App.getThreadPool().execute(this::fetchAllUnits);
    }

    // runs in WorkerThread
    private void fetchAllUnits() {
        List<String> unitCaptions = new ArrayList<>();
        allUnits.clear();
        Platform.runLater(() -> {
            unitListView.getItems().clear();
        });
        List<Unit> units;

        try {
            units = MafisServerApi.fetchAllUnits();
        } catch (GenericException ex) {
            allUnits.clear();
            disableControls(importUnitBtn);
            Platform.runLater(() -> {
                unitListView.getItems().clear();
                messageLabel.setText(ex.getMessage());
            });
            return;
        } catch (ConnectionTimeoutException ex) {
            disableControls(importUnitBtn);
            Platform.runLater(() -> {
                unitListView.getItems().clear();
                messageLabel.setText(TIMEOUT_ERR_MSG);
            });
            return;
        }
        units.stream().sorted(Comparator.comparing(Unit::getCaption)).forEach(unit -> {
            unitCaptions.add(unit.getCaption());
            allUnits.add(unit);
        });

        if (allUnits.isEmpty()) {
            updateUI("No units available.");
        } else {
            enableControls(importUnitBtn);
        }

        Platform.runLater(() -> {
            unitListView.setItems(FXCollections.observableArrayList(unitCaptions));
            messageLabel.setText("");
        });
    }


    private void disableControls(Node... nodes) {
        for (var node : nodes) {
            node.setDisable(true);
        }
    }

    private void enableControls(Node... nodes) {
        for (var node : nodes) {
            node.setDisable(false);
        }
    }

    private void searchFilter(String value) {
        if (value.isEmpty()) {
            unitListView.setItems(FXCollections.observableList(allUnits.stream().map(Unit::getCaption).collect(Collectors.toList())));
            return;
        }
        String valueUpper = value.toUpperCase();
        unitListView.setItems(FXCollections.observableList(allUnits.stream().map(Unit::getCaption).filter(caption -> caption.toUpperCase().contains(valueUpper)).collect(Collectors.toList())));
    }

    private void importSelectedUnits() {
        if (selectedUnits.isEmpty()) {
            updateUI("Please select a unit");
            return;
        }
        Set<String> selectedUnitSet = new HashSet<>(selectedUnits);
        List<String> selectedUnitCodes = allUnits.stream().filter(unit -> selectedUnitSet.contains(unit.getCaption())).map(Unit::getValue).collect(Collectors.toList());
        if (selectedUnitCodes.isEmpty()) {
            updateUI("Kindly, select values from unit list");
            return;
        }
        for (String unitCode : selectedUnitCodes) {
            App.getThreadPool().execute(() -> importUnit(unitCode));
        }
        disableControls(importUnitBtn, clearImportBtn, clearAllImportBtn, exportBtn, homeBtn, backBtn);
        messageLabel.setText("Importing unit. Please wait.......");

    }

    public void updateUIViews() {
        updateImportedListView();
        updateCapturedBiometric();
    }

    private void importUnit(String unitCode) {
        String unitId;
        String unitCaption;
        List<ArcDetail> arcDetails;
        try {
            arcDetails = MafisServerApi.fetchArcsByUnitCode(unitCode);
        } catch (GenericException ex) {
            updateUIViews(); // since all buttons are disabled
            enableControls(importUnitBtn, homeBtn, backBtn); // since all buttons are disabled
            updateUI(ex.getMessage());
            return;
        } catch (ConnectionTimeoutException ex) {
            enableControls(homeBtn, backBtn);  // since all buttons are disabled
            updateUIViews(); // since all buttons are disabled
            Platform.runLater(() -> {
                unitListView.getItems().clear();
                disableControls(importUnitBtn);
                messageLabel.setText(TIMEOUT_ERR_MSG);
            });
            return;
        }
        if (arcDetails == null || arcDetails.isEmpty()) {
            enableControls(importUnitBtn, homeBtn, backBtn); // since all buttons are disabled
            updateUIViews(); // since all buttons are disabled
            updateUI("No e-ARC found for imported unit.");
            return;
        }
        enableControls(importUnitBtn, homeBtn, backBtn); // since all buttons are disabled

        var firstArcDetails = arcDetails.get(0);
        unitId = firstArcDetails.getArcNo().split("-")[0];
        unitCaption = firstArcDetails.getUnit().replaceAll("[^a-zA-Z0-9]", "");
        unitCode = unitCode.replaceAll("[^a-zA-Z0-9]", "");
        String jsonArcList;
        try {
            // throws exception
            jsonArcList = Singleton.getObjectMapper().writeValueAsString(arcDetails);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            updateUI(GENERIC_ERR_MSG);
            updateUIViews(); // since all buttons are disabled
            return;
        }

        String filePath = PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER) + "/" + unitId + "-" + unitCode + "-" + unitCaption;
        try {
            // throws exception
            Files.writeString(Path.of(filePath), jsonArcList, StandardCharsets.UTF_8);
            updateUI("Unit imported successfully.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            updateUI(GENERIC_ERR_MSG);
        }
        updateUIViews(); // since all buttons are disabled
    }

    private void updateImportedListView() {
        // throws exception
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            List<String> unitCaptions = importFolder.filter(Files::isRegularFile)
                    .map(file -> {
                        List<ArcDetail> arcDetails;
                        try {
                            arcDetails = Singleton.getObjectMapper().readValue(Files.readAllBytes(file), new TypeReference<>() {
                            });
                        } catch (IOException ex) {
                            LOGGER.log(Level.INFO, () -> "***Error: " + ex.getMessage());
                            return "";
                        }

                        if (arcDetails.isEmpty()) {
                            LOGGER.log(Level.INFO, () -> "***No e-arcs found for file" + file);
                            return "";
                        }
                        return arcDetails.get(0).getUnit();
                    })
                    .filter(unitCaption -> !unitCaption.isBlank())
                    .sorted()
                    .collect(Collectors.toList());

            if (unitCaptions.isEmpty()) {
                disableControls(clearImportBtn, clearAllImportBtn);
            } else {
                enableControls(clearImportBtn, clearAllImportBtn);
            }
            Platform.runLater(() -> {
                importedUnitListView.setItems(FXCollections.observableList(unitCaptions));
                importedUnitText.setText(IMPORTED_TEXT + unitCaptions.size());
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error updating imported list view.");
            updateUI(GENERIC_ERR_MSG);
        }
    }

    private void clearSingleImportedUnit() {
        String selectedImportedUnit = importedUnitListView.getSelectionModel().getSelectedItem();
        if (selectedImportedUnit == null || selectedImportedUnit.isBlank()) {
            updateUI("Select an imported unit to clear.");
            return;
        }
        // throws exception
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            Optional<Path> optionalPath = importFolder.filter(file -> {
                if (Files.isRegularFile(file)) {
                    String[] splitFileName = file.getFileName().toString().split("-");
                    if (splitFileName.length > 2) {
                        //00001-INSI-INS INDIA
                        return selectedImportedUnit.replaceAll("[^a-zA-Z0-9]", "").equals(splitFileName[2]);
                    }
                }
                return false;
            }).findFirst();
            if (optionalPath.isEmpty()) {
                updateUI("");
                return;
            }
            Files.delete(optionalPath.get());
            updateImportedListView();
            updateUI("");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting selected unit.");
            updateUI(GENERIC_ERR_MSG);
        }

    }

    private void clearAllImportedUnits() {
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            importFolder.filter(Files::isRegularFile).forEach(this::deletePath);
            updateImportedListView();
            updateUI("");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting files");
            updateUI(GENERIC_ERR_MSG);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, () -> "Error deleting selected file: " + path.getFileName());
            updateUI(GENERIC_ERR_MSG);
        }
    }


    // needs to be run on WorkerThread; as the list can grow to large extent
    private void updateCapturedBiometric() {
        try (Stream<Path> encExportFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER)))) {
            List<String> capturedArcs = new ArrayList<>();

            encExportFolder.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String[] splitFilename = path.getFileName().toString().split("\\.");
                    if (splitFilename.length == 3) {
                        capturedArcs.add(splitFilename[0]);
                    }
                }
            });

            Collections.sort(capturedArcs);

            if (capturedArcs.isEmpty()) {
                disableControls(exportBtn);
            } else {
                enableControls(exportBtn);
            }
            Platform.runLater(() -> {
                capturedBiometricText.setText(CAPTURED_BIOMETRIC_TEXT + capturedArcs.size());
                capturedArcListView.setItems(FXCollections.observableList(capturedArcs));
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while getting the count of captured biometric data");
            updateUI(GENERIC_ERR_MSG);
        }
    }

    private void updateUI(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.SEVERE, "***Unhandled exception occurred.");
    }

}
