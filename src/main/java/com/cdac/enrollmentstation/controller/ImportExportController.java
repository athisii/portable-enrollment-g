/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;


import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.api.ServerAPI;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FileUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author root
 */
public class ImportExportController {
    @FXML
    public Button importDataBtn;
    @FXML
    public Button exportDataBtn;
    private List<Units> allUnits = new ArrayList<>();
    private APIServerCheck apiServerCheck = new APIServerCheck();

    public SaveEnrollmentResponse saveEnrollmentResponse;
    @FXML
    private Label messageLabel;
    TestProp prop = new TestProp();
    //private String importjson="/usr/share/enrollment/json/import/arclistimported.json"
    private String importjson = null;
    //private String export="/usr/share/enrollment/json/export"
    public SaveEnrollmentResponse enrollmentResponse;
    private String export = null;
    @FXML
    private ImageView refreshIcon;
    Thread exportthread = null;
    Thread importthread = null;
    @FXML
    private ListView<String> unitListView;
    @FXML
    private TextField searchText;
    List<String> selectedUnits = new ArrayList<>();
    private static final Logger LOGGER = ApplicationLog.getLogger(ImportExportController.class);


    public void updateUI(String message) {
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
    public void importData() {
        try {
            importthread = new Thread(importjsonFile);
            importthread.start();
        } catch (Exception e) {
            System.out.println("Error in loop::" + e);
        }
    }

    Runnable importjsonFile = () -> {
        String response = "";
        response = "Importing Please Wait...";
        updateUI(response);
        String jsonurllist = "";
        String connurlUnitList = apiServerCheck.getUnitListURL();
        List<String> unitIds = new ArrayList<>();
        try {
            HashSet<String> unitListHash = new HashSet<>(selectedUnits);
            for (String uList : unitListHash) {
                System.out.println("Unit List from Hash::" + uList);
            }
            jsonurllist = apiServerCheck.getUnitListAPI(connurlUnitList);
            System.out.println("Output str : " + jsonurllist);
            ObjectMapper objectmapper = new ObjectMapper();
            UnitListDetails details = objectmapper.readValue(jsonurllist, UnitListDetails.class);
            if (details.getErrorCode().equals("0")) {
                response = "Unit Details Fetched Successfully";
                System.out.println("At Import Fetch Unit If :" + response);
            } else {
                response = details.getDesc();
                System.out.println("At Import Fetch Unit Else :" + response);
                updateUI(response);
                return;
            }

            List<Units> unit = details.getUnits();
            for (Units result1 : unit) {
                for (String uList : unitListHash) {
                    System.out.println("Unit List3::" + uList);
                    System.out.println("Unit List3GetCaption::" + result1.getCaption());
                    if (result1.getCaption().equals(uList)) {
                        unitIds.add(result1.getValue().trim());
                        System.out.println("caption:" + result1.getCaption());
                    }
                }
            }

            if (unitIds.size() == 0) {
                response = "Kindly select the Values from Unit";
                updateUI(response);
                return;
            }


        } catch (Exception e) {
            System.out.println("Exception::" + e);
            response = "Kindly select the Values from Unit";
            updateUI(response);
            return;
        }

        try {
            importjson = prop.getProp().getProperty("importjsonfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
            response = "Import Json Folder Not Exist in File.properties";
            updateUI(response);
            return;
        }
        try {
            export = prop.getProp().getProperty("exportfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
            response = "Export Json Folder Not Exist in File.properties";
            updateUI(response);
            return;
        }
        System.out.println("import data");

        try {
            String ckeckUnitid = "";
            for (String uNitLists : unitIds) {
                System.out.println("Unit ListValue4::" + uNitLists);
                ckeckUnitid = uNitLists;
            }
            String connurl = apiServerCheck.getDemographicURL();
            String connectionStatus = apiServerCheck.getDemoGraphicDetailsAPI(connurl, ckeckUnitid);
            System.out.println("Output :::::::" + connectionStatus);
            if (connectionStatus.contains("Exception")) {
                response = connectionStatus;
                updateUI(response);
                return;
            }
            String importjsonFile = prop.getProp().getProperty("importjsonfile");
            File jsonFile = new File(importjsonFile);
            String connurl1 = apiServerCheck.getDemographicURL();
            ARCDetailsList arcDetailsList = new ARCDetailsList();
            ARCDetailsList checkArcDetailsList = new ARCDetailsList();
            ObjectMapper mapper = new ObjectMapper();

            JsonFactory jfactory = new JsonFactory();
            List<ArcDetailsMapping> mappingResult;
            try {

                for (String uNitLists : unitIds) {
                    System.out.println("Unit ListValue5::" + uNitLists);
                    String json = apiServerCheck.getDemoGraphicDetailsAPI(connurl1, uNitLists);
                    try {
                        checkArcDetailsList = mapper.readValue(json, ARCDetailsList.class);
                    } catch (Exception e) {
                        System.out.println("Exception While Read Json From Server:" + e);
                        response = "ARC data From Server had issue";
                        updateUI(response);
                    }
                    if (checkArcDetailsList.getErrorCode() == 0) {
                        System.out.println("Arc details List Error Code:" + checkArcDetailsList.getErrorCode());
                        if (jsonFile.exists() && !(jsonFile.length() == 0)) {
                            FileReader reader = new FileReader(importjsonFile);
                            arcDetailsList = mapper.readValue(reader, ARCDetailsList.class);
                            String postJson = mapper.writeValueAsString(arcDetailsList);

                            JsonParser jParser1 = jfactory.createJsonParser(json);
                            JsonParser jParser2 = jfactory.createJsonParser(postJson);
                            ARCDetailsList arcDetailsList1 = mapper.readValue(jParser1, ARCDetailsList.class);// JsonObj is Pojo for your jsonObject
                            ARCDetailsList arcDetailsList2 = mapper.readValue(jParser2, ARCDetailsList.class);
                            arcDetailsList1.getArcDetails().addAll(arcDetailsList2.getArcDetails());
                            String finaljson = mapper.writeValueAsString(arcDetailsList1);
                            FileUtils.writeStringToFile(new File(importjsonFile), finaljson);
                            System.out.println("importjsonfile  Exist");
                        } else {
                            System.out.println("importjsonfile Not Exist");
                            arcDetailsList = mapper.readValue(json, ARCDetailsList.class);
                            FileUtils.writeStringToFile(new File(importjsonFile), json);
                        }
                    } else {
                        System.out.println("Else Arc details List Error Code:" + checkArcDetailsList.getErrorCode());
                        response = checkArcDetailsList.getDesc();
                        updateUI(response + " " + "Try again with Other than " + uNitLists + " " + "Unit");
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception While Import Json:" + e);
                response = "No ARC available for Selected Unit";
                updateUI(response);
                return;
            }

            ObjectMapper objMapper = new ObjectMapper();
            try {
                FileReader reader = new FileReader(importjsonFile);
                arcDetailsList = mapper.readValue(reader, ARCDetailsList.class);
                System.out.println("response compatible with ARCDetailsList POJO");
            } catch (JsonParseException | JsonMappingException e) {
                System.out.println("response NOT compatible with ARCDetailsList POJO");
                response = "The Response From Server is not Compatible";
                updateUI(response);
                return;
            }

            System.out.println("Import Error Code :::" + arcDetailsList.getErrorCode());

            if (arcDetailsList.getErrorCode() == 0) {
                try {
                    System.out.println("Successfully Copied JSON Object to File..." + arcDetailsList.getDesc());
                    response = "Successfully Imported";
                    updateUI(response);
                    return;
                } catch (Exception e) {
                    System.out.println("Exception" + e);
                }

            } else {
                response = arcDetailsList.getDesc();
                updateUI(response);
                return;
            }

        } catch (Exception e) {
            System.out.println("Exception========" + e);
            response = "No ARC Available for Selected Unit";
            updateUI(response);
            return;
        }
        updateUI(response);
    };

    @FXML
    private void exportData() {

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

    private void fetchAllUnits() {
        try {
            List<String> unitCaptions = new ArrayList<>();
            allUnits.clear();
            ServerAPI.fetchAllUnits().stream()
                    .sorted(Comparator.comparing(Units::getCaption))
                    .forEach(unit -> {
                        unitCaptions.add(unit.getCaption());
                        allUnits.add(unit);
                    });
            Platform.runLater(() -> {
                unitListView.setItems(FXCollections.observableArrayList(unitCaptions));
                messageLabel.setText("");
                enableControls(importDataBtn, exportDataBtn);
            });
        } catch (GenericException ex) {
            allUnits = new ArrayList<>();
            Platform.runLater(() -> {
                messageLabel.setText(ex.getMessage());
                disableControls(importDataBtn, exportDataBtn);
            });
        }
    }


    public void initialize() {
        refreshIcon.setOnMouseClicked(mouseEvent -> refresh());
        searchText.textProperty().addListener((observable, oldVal, newVal) -> searchFilter(newVal));
        unitListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unitListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedUnits.clear();
            selectedUnits.addAll(new ArrayList<>(unitListView.getSelectionModel().getSelectedItems()));
        });
        //Root1234#$
        messageLabel.setText("Fetching units.....");
        disableControls(importDataBtn, exportDataBtn);
        ForkJoinPool.commonPool().execute(this::fetchAllUnits);
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
}
