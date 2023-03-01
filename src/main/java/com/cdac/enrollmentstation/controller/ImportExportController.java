/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;


import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.api.ServerAPI;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.model.Units;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FXML Controller class
 *
 * @author root
 */
public class ImportExportController {
    private static final Logger LOGGER = ApplicationLog.getLogger(ImportExportController.class);
    private static final String IMPORTED_TEXT = "IMPORTED: ";
    private static final String CAPTURED_BIOMETRIC_TEXT = "CAPTURED BIOMETRIC: ";
    @FXML
    public Text importedUnitText;
    @FXML
    public Text capturedBiometricText;
    @FXML
    public Button exportDataBtn;
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
    private final List<Units> allUnits = new ArrayList<>();
    private final List<String> selectedUnits = new ArrayList<>();

    private APIServerCheck apiServerCheck = new APIServerCheck();

    private SaveEnrollmentResponse saveEnrollmentResponse;
    @FXML
    private Label messageLabel;
    private TestProp prop = new TestProp();
    //private String importjson="/usr/share/enrollment/json/import/arclistimported.json"
    private String importjson = null;
    //private String export="/usr/share/enrollment/json/export"
    private SaveEnrollmentResponse enrollmentResponse;
    private String export = null;
    @FXML
    private ImageView refreshIcon;
    private Thread exportthread = null;

    @FXML
    private ListView<String> unitListView;
    @FXML
    private TextField searchText;


    private void updateUI(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

    @FXML
    private void exportDataOld() {

        try {
            exportthread = new Thread(exportjsonFile);
            exportthread.start();
        } catch (Exception e) {
            System.out.println("Error in loop::" + e);
        }
    }

    Runnable exportjsonFile = new Runnable() {
        @Override
        public void run() {
            String response = "";
            response = "Exporting Please Wait...";
            updateUI(response);
            SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
            String postJson = "";
            String connurl = apiServerCheck.getArcUrl();
            String arcno = "123abc";
            String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcno);
            System.out.println("connection status :" + connectionStatus);

            if (!connectionStatus.contentEquals("connected")) {
                response = "Network Connection Issue. Check Connection and Try Again";
                updateUI(response);
                return;
            }

            try {
                export = prop.getProp().getProperty("exportfolder");
            } catch (IOException ex) {
                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                response = "Export Folder Not Found on the System";
                updateUI(response);
                return;
            }
            try {
                importjson = prop.getProp().getProperty("importjsonfolder");
                File dire = new File(importjson);
                File[] dirlisting = dire.listFiles();
                if (dirlisting.length != 0) {
                    System.out.println("Inside Import directory listing");
                    for (File children : dirlisting) {
                        children.delete();
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
            }
            File dir = new File(export + "/enc");
            System.out.println(export + "/enc");
            File[] directoryListing = dir.listFiles();
            System.out.println("file count" + directoryListing.length);
            if (directoryListing.length != 0) {
                System.out.println("Inside directory listing");
                for (File child : directoryListing) {
                    System.out.println("Inside directory listing - for loop");
                    response = "Exporting Please Wait...";
                    updateUI(response);
                    AESFileEncryptionDecryption aesDecryptFile = new AESFileEncryptionDecryption();
                    try {
                        aesDecryptFile.decryptFile(child.getAbsolutePath(), export + "/dec/" + child.getName());

                    } catch (IOException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (InvalidKeySpecException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (NoSuchPaddingException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (InvalidKeyException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (InvalidAlgorithmParameterException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (IllegalBlockSizeException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    } catch (BadPaddingException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        updateUI(response);
                        return;
                    }
                    FileReader file = null;
                    try {
                        file = new FileReader(export + "/dec/" + child.getName());
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT);
                        mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
                        saveEnrollment = mapper.readValue(Paths.get(export + "/dec/" + child.getName()).toFile(), SaveEnrollmentDetails.class);
                        postJson = mapper.writeValueAsString(saveEnrollment);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "File not Found, Try Again";
                        updateUI(response);
                        return;
                    } catch (IOException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "File not Found, Try Again";
                        updateUI(response);
                        return;
                    }

                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        ObjectMapper objMappersave = new ObjectMapper();
                        String decResponse = "";
                        System.out.println("Decrypted Json::" + postJson);
                        connurl = apiServerCheck.getEnrollmentSaveURL();
                        decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
                        System.out.println("dec response : " + decResponse);
                        if (decResponse.contains("Exception:")) {
                            updateUI(decResponse);
                            return;
                        }
                        saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                        System.out.println(" save enrollment : " + saveEnrollmentResponse.toString());
                        enrollmentResponse = objMappersave.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                        System.out.println(" save enrollment : " + enrollmentResponse.toString());
                        if (enrollmentResponse.getErrorCode().equals("0")) {
                            child.delete();
                            aesDecryptFile.delFile(export + "/dec/" + child.getName());
                            aesDecryptFile.delFile(export + "/enc/" + child.getName());

                            System.out.println(removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc());
                            response = "Biometric data exported successfully";
                            updateUI(response);

                        } else if (enrollmentResponse.getErrorCode().equals("-1")) {
                            response = enrollmentResponse.getDesc();

                            updateUI(response);
                            child.delete();
                            aesDecryptFile.delFile(export + "/dec/" + child.getName());
                            aesDecryptFile.delFile(export + "/enc/" + child.getName());
                        } else {

                            System.out.println(removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc());
                            response = enrollmentResponse.getDesc();

                            updateUI(response);
                        }

                    } catch (Exception e) {
                        System.out.println("Exception in Export" + e);
                        response = "Exception in Export" + e;
                        updateUI(response);
                    }
                }
                updateImportedListView();
                updateCapturedBiometric();
            } else {

                System.out.println("The Directory is empty.. No encrypted files");
                response = "No Biometric Data to Export";
                updateUI(response);
                return;

            }
            updateUI(response);
        }
    };

    @FXML
    public void home() throws IOException {
        App.setRoot("main_screen");

    }

    @FXML
    public void back() throws IOException {
        App.setRoot("main_screen");

    }

    public void refresh() {
        messageLabel.setText("Fetching units....");
        ForkJoinPool.commonPool().execute(this::fetchAllUnits);
    }

    // runs in WorkerThread
    private void fetchAllUnits() {
        try {
            List<String> unitCaptions = new ArrayList<>();
            allUnits.clear();
            // throws exception
            ServerAPI.fetchAllUnits().stream()
                    .sorted(Comparator.comparing(Units::getCaption))
                    .forEach(unit -> {
                        unitCaptions.add(unit.getCaption());
                        allUnits.add(unit);
                    });

            Platform.runLater(() -> {
                unitListView.setItems(FXCollections.observableArrayList(unitCaptions));
                messageLabel.setText("");
                enableControls(importUnitBtn, clearImportBtn, clearAllImportBtn, exportDataBtn);
            });
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            allUnits.clear();
            Platform.runLater(() -> {
                unitListView.getItems().clear();
                messageLabel.setText(ex.getMessage());
                disableControls(importUnitBtn, clearImportBtn, clearAllImportBtn, exportDataBtn);
            });
        }
    }


    public void initialize() {
        refreshIcon.setOnMouseClicked(mouseEvent -> refresh());
        importUnitBtn.setOnAction(event -> importSelectedUnits());
        clearImportBtn.setOnAction(event -> clearSingleImportedUnit());
        clearAllImportBtn.setOnAction(event -> clearAllImportedUnits());
        searchText.textProperty().addListener((observable, oldVal, newVal) -> searchFilter(newVal));
        // should not allow multiple selections of units
        // some units might not have Arc number
        // error message will get override if multiple units are selected
//        unitListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE)
        unitListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedUnits.clear();
            selectedUnits.addAll(new ArrayList<>(unitListView.getSelectionModel().getSelectedItems()));
        });
        disableControls(importUnitBtn, clearImportBtn, clearAllImportBtn, exportDataBtn);
        messageLabel.setText("Fetching units.....");
        ForkJoinPool.commonPool().execute(this::fetchAllUnits);
        ForkJoinPool.commonPool().execute(this::updateImportedListView);
        ForkJoinPool.commonPool().execute(this::updateCapturedBiometric);


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
            unitListView.setItems(FXCollections.observableList(allUnits.stream().map(Units::getCaption).collect(Collectors.toList())));
            return;
        }
        String valueUpper = value.toUpperCase();
        unitListView.setItems(FXCollections.observableList(allUnits.stream().map(Units::getCaption).filter(caption -> caption.toUpperCase().contains(valueUpper)).collect(Collectors.toList())));
    }

    private void importSelectedUnits() {
        if (selectedUnits.isEmpty()) {
            updateUI("Please select a unit");
            return;
        }
        Set<String> selectedUnitSet = new HashSet<>(selectedUnits);
        List<String> selectedUnitCodes = allUnits.stream().filter(unit -> selectedUnitSet.contains(unit.getCaption())).map(Units::getValue).collect(Collectors.toList());
        if (selectedUnitCodes.isEmpty()) {
            updateUI("Kindly, select values from unit list");
            return;
        }
        for (String unitCode : selectedUnitCodes) {
            ForkJoinPool.commonPool().execute(() -> importUnit(unitCode));
        }
        disableControls(importUnitBtn);
        messageLabel.setText("Importing unit. Please wait.......");

    }

    private void importUnit(String unitCode) {
        String unitId;
        String unitCaption;
        List<ARCDetails> arcDetailsList;
        try {
            // throws exception
            arcDetailsList = ServerAPI.fetchArcListByUnitCode(unitCode);
            if (arcDetailsList.isEmpty()) {
                updateUI("No ARC found for imported unit.");
                return;
            }
            var firstArcDetails = arcDetailsList.get(0);
            unitId = firstArcDetails.getArcNo().split("-")[0];
            unitCaption = firstArcDetails.getUnit();

        } catch (GenericException ex) {
            String exceptionMessage = ex.getMessage();
            LOGGER.log(Level.SEVERE, exceptionMessage);
            // Special case, if network connection is interrupted while importing units
            if (exceptionMessage != null && exceptionMessage.toUpperCase().contains("CONNECTION TIMEOUT")) {
                Platform.runLater(() -> {
                    unitListView.getItems().clear();
                    disableControls(importUnitBtn);
                });
                updateUI(ex.getMessage());
                return;
            }
            Platform.runLater(() -> {
                enableControls(importUnitBtn);
                messageLabel.setText(exceptionMessage);
            });
            return;
        }

        String jsonArcList;
        try {
            // throws exception
            jsonArcList = Singleton.getObjectMapper().writeValueAsString(arcDetailsList);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ERROR_MESSAGE);
            updateUI(ApplicationConstant.GENERIC_ERROR_MESSAGE);
            return;
        }

        String filePath = PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER) + "/" + unitId + "-" + unitCode + "-" + unitCaption;
        try {
            // throws exception
            Files.writeString(Path.of(filePath), jsonArcList, StandardCharsets.UTF_8);
            Platform.runLater(() -> {
                enableControls(importUnitBtn);
                messageLabel.setText("");
            });
            updateImportedListView();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ERROR_MESSAGE);
            updateUI("Something went wrong. Please try again.");
        }

    }

    private void updateImportedListView() {
        // throws exception
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            List<String> unitCaptions = importFolder
                    .filter(Files::isRegularFile)
                    .map(file -> {
                        String[] splitFileName = file.getFileName().toString().split("-");
                        if (splitFileName.length > 2) {
                            //00001-INSI-INS INDIA
                            return splitFileName[2];
                        }
                        LOGGER.log(Level.SEVERE, () -> "Malformed filename: " + file.getFileName());
                        return "";
                    }).filter(unitCaption -> !unitCaption.isBlank())
                    .collect(Collectors.toList());

            Platform.runLater(() -> {
                importedUnitListView.setItems(FXCollections.observableList(unitCaptions));
                importedUnitText.setText(IMPORTED_TEXT + unitCaptions.size());
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error updating imported list view.");
            updateUI(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
    }

    private void clearSingleImportedUnit() {
        String selectedImportedUnit = importedUnitListView.getSelectionModel().getSelectedItem();
        if (selectedImportedUnit == null || selectedImportedUnit.isBlank()) {
            return;
        }
        // throws exception
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            Optional<Path> optionalPath = importFolder
                    .filter(file -> {
                        if (Files.isRegularFile(file)) {
                            String[] splitFileName = file.getFileName().toString().split("-");
                            if (splitFileName.length > 2) {
                                //00001-INSI-INS INDIA
                                return selectedImportedUnit.equals(splitFileName[2]);
                            }
                        }
                        return false;
                    }).findFirst();
            if (optionalPath.isEmpty()) {
                return;
            }
            Files.delete(optionalPath.get());
            updateImportedListView();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting selected unit.");
            updateUI("Error deleting selected unit");
        }

    }

    private void clearAllImportedUnits() {
        try (Stream<Path> importFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.IMPORT_JSON_FOLDER)))) {
            importFolder.filter(Files::isRegularFile).forEach(this::deletePath);
            updateImportedListView();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting files");
            updateUI(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, () -> "Error deleting selected file: " + path.getFileName());
            updateUI(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
    }


    // needs to be run on WorkerThread; as the list can grow to large extent
    private void updateCapturedBiometric() {
        try (Stream<Path> encExportFolder = Files.walk(Path.of(PropertyFile.getProperty(PropertyName.ENC_EXPORT_FOLDER)))) {
            List<String> capturedArcs = new ArrayList<>();
            encExportFolder
                    .forEach(path -> {
                        if (Files.isRegularFile(path)) {
                            String[] splitFilename = path.getFileName().toString().split("\\.");
                            if (splitFilename.length > 1) {
                                capturedArcs.add(splitFilename[0]);
                            }
                        }
                    });

            Platform.runLater(() -> capturedBiometricText.setText(CAPTURED_BIOMETRIC_TEXT + capturedArcs.size()));

            if (capturedArcs.isEmpty()) {
                return;
            }
            Collections.sort(capturedArcs);
            Platform.runLater(() -> capturedArcListView.setItems(FXCollections.observableList(capturedArcs)));

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while getting the count of captured biometric data");
        }
    }
}
