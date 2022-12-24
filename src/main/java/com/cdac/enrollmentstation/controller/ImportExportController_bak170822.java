/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.event.AutoCompleteComboBoxListener;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class ImportExportController_bak170822 implements Initializable {
    /**
     * Initializes the controller class.
     */

    private APIServerCheck apiServerCheck = new APIServerCheck();

    public ARCDetailsList arcDetailsListResponse;

    public SaveEnrollmentResponse saveEnrollmentResponse;

    @FXML
    private Label lblStatus;

    @FXML
    private Button exporthome;

    @FXML
    private Button exportback;

    public static SecretKey skey;

    TestProp prop = new TestProp();

    //private String importjson="/usr/share/enrollment/json/import/arclistimported.json";
    private String importjson = null;


    private String export = null;

    public SaveEnrollmentResponse enrollmentResponse;
    //private String export="/usr/share/enrollment/json/export";


    @FXML
    private Button FetchUnitButton;

    @FXML
    // private ComboBox<Units> comboUnitId = new ComboBox<>();
    private ComboBox comboUnitId = new ComboBox<>();


    // public ComboBox comboBox;
    // private ObservableList<Units> data;
    // private Integer sid;
    //AutoCompleteComboBoxListener autocompleteBox ;


    public void messageStatus(String message) {
        lblStatus.setText(message);
    }

    public void responseStatus(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                lblStatus.setText(message);
            }
        });
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
        lblStatus.setText("");
        String response = importjsonFile();
        //messageStatus(response);
        responseStatus(response);
    }

    @FXML
    private void fetchunitdetails() {

        String response = fetchUnit();
        //messageStatus(response);
        responseStatus(response);
    }


    public String fetchUnit() {

        String response = "";
        String json = "";
        try {
            String connurl = apiServerCheck.getUnitListURL();
            String connectionStatus = apiServerCheck.getStatusUnitListAPI(connurl);
            System.out.println("connection status :" + connectionStatus);

            if (!connectionStatus.contentEquals("connected")) {
                //labelstatus.setText(connectionStatus);
                //labelstatus.setText("Enter the Correct URL in Mafis API");
                // Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                response = "Network Connection Timed out From Server.Kindly Try Again";
                //messageStatus(response);
                return response;
            } else {
                try {
                    json = apiServerCheck.getUnitListAPI(connurl);
                    String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BYT TKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BXT HKL\",\"Value\":\"INSZ\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
                    System.out.println("Output str : " + json);
                    ObjectMapper objectmapper = new ObjectMapper();
                    UnitListDetails details = objectmapper.readValue(json1, UnitListDetails.class);
                    if (details.getErrorCode().equals("0")) {
                        response = "Unit Details Fetched Successfully";
                        //messageStatus(response);
                        responseStatus(response);
                    } else {
                        response = details.getDesc();
                        //messageStatus(response);
                        responseStatus(response);
                        //return response;
                    }


                    try {
                        try {
                            new AutoCompleteComboBoxListener(comboUnitId);
                        } catch (Exception e) {
                            System.out.println("Exception At AutoComplete :::" + e);
                        }

                        ObservableList<Units> units = FXCollections.observableArrayList(details.getUnits()).sorted();
                        comboUnitId.setItems(units);

                        System.out.println("combo value : " + comboUnitId.getSelectionModel().getSelectedItem());
                        comboUnitId.valueProperty().addListener((obs, oldval, newval) -> {
                            if (newval != null)
                                //System.out.println("Selected unit1: " + newval.getCaption() + ". ID: " + newval.getValue());
                                System.out.println("Selected unit1: " + newval);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("error :" + e.getMessage());
                        response = "Kindly select the Values from Unit";
                        //messageStatus(response);
                        responseStatus(response);
                        //return response;
                    }
                } catch (NullPointerException e) {
                    System.out.print("NullPointerException caught");

                    response = "Unit List is not Available in the Server";
                    //messageStatus(response);
                    responseStatus(response);
                    //return response;
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Unit List is not Available in the Server";
                    messageStatus(response);
                    //return response;
                }

            }
        } catch (NullPointerException e) {
            System.out.print("NullPointerException caught");
            response = "Unit List is not Available in the Server";
            //messageStatus(response);
            responseStatus(response);
            //return response;
        }
        return response;
        //messageStatus(response);     
    }


    public String importjsonFile() {

        String response = "";
        String jsonurllist = "";
        String connurlUnitList = apiServerCheck.getUnitListURL();
        String unitid = "";

        //Units unit =new Units();
        try {
            String selectedUnit = comboUnitId.getSelectionModel().getSelectedItem().toString();
            if (selectedUnit == null || selectedUnit.isEmpty()) {
                response = "Kindly select the Values from Unit";
                //messageStatus(response);
                return response;
            }

            jsonurllist = apiServerCheck.getUnitListAPI(connurlUnitList);
            String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BYT TKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BXT HKL\",\"Value\":\"INSZ\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
            System.out.println("Output str : " + jsonurllist);
            ObjectMapper objectmapper = new ObjectMapper();
            UnitListDetails details = objectmapper.readValue(json1, UnitListDetails.class);
            if (details.getErrorCode().equals("0")) {
                response = "Unit Details Fetched Successfully";
                //messageStatus(response);
                responseStatus(response);
            } else {
                response = details.getDesc();
                //messageStatus(response);
                responseStatus(response);
                //return response;
            }


            System.out.println("Unit1:" + comboUnitId.getSelectionModel().getSelectedItem());


            System.out.println("Selected Unit::::" + selectedUnit);
            List<Units> unit = details.getUnits();
            for (Units result : unit) {
                if (result.getCaption().equals(selectedUnit)) {
                    unitid = result.getValue();
                    System.out.println("UNITIDDD Value:" + unitid);
                    System.out.println("UNIT caption:" + result.getCaption());
                }
            }
            // System.out.println("Unit1:"+unit.getCaption()+" "+selected_unit.toString());
            // unit = comboUnitId.getSelectionModel().getSelectedItem();
            if (unitid == null || unitid.isEmpty()) {
                response = "Kindly select the Values from Unit";
                //messageStatus(response);
                return response;
            }

        } catch (Exception e) {
            System.out.println("Exception::" + e);
            response = "Kindly select the Values from Unit";
            return response;

        }


        try {
            //importjson=prop.getProp().getProperty("importjsonfile");
            importjson = prop.getProp().getProperty("importjsonfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
            response = "Json File Not Exist in File.properties";
            //messageStatus(response);
            return response;
        }
        try {
            export = prop.getProp().getProperty("exportfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
            response = "Json File Not Exist in File.properties";
            //messageStatus(response);
            return response;
        }
        System.out.println("import data");

        //Checking Connection Status
        String connurl = apiServerCheck.getARCURL();
        String arcno = "123abc";
        String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcno);
        System.out.println("connection status :" + connectionStatus);
        if (!connectionStatus.contentEquals("connected")) {
            //lblStatus.setText("System not connected to network. Connect and try again");
            response = "Network Connection Timed out From Server, Kindly Try Again";
            //messageStatus(response);
            return response;
        }


        try {

            connurl = apiServerCheck.getDemographicURL();
            //   String unitid=unit.getValue();
            //String unitid="";
            System.out.println("UnitID::::" + unitid);

            connectionStatus = apiServerCheck.getDemoGraphicDetailsAPI(connurl, unitid);

            System.out.println("Output :::::::" + connectionStatus);
            if (connectionStatus.contains("Exception")) {
                //lblStatus.setText(connectionStatus);
                response = connectionStatus;
                //messageStatus(response);
                return response;
            }


            //Import into single file
            String importjsonFile = prop.getProp().getProperty("importjsonfile");
            File jsonFile = new File(importjsonFile);
            String connurl1 = apiServerCheck.getDemographicURL();
            ARCDetailsList arcDetailsList = new ARCDetailsList();
            ARCDetailsList checkArcDetailsList = new ARCDetailsList();
            ObjectMapper mapper = new ObjectMapper();

            JsonFactory jfactory = new JsonFactory();
            //ObjectReaderWriter objReadWrite = new ObjectReaderWriter();


            List<ArcDetailsMapping> mappingResult;


            //String finaljson="";

            try {
                String json = apiServerCheck.getDemoGraphicDetailsAPI(connurl1, unitid);
                try {
                    checkArcDetailsList = mapper.readValue(json, ARCDetailsList.class);
                } catch (Exception e) {
                    response = "Exception While Read Json From Server:" + e;
                    return response;
                }
                if (checkArcDetailsList.getErrorCode() == 0) {
                    System.out.println("Arc details List Error Code:" + checkArcDetailsList.getErrorCode());

                    //System.out.println("JSON response::"+json);
                    if (jsonFile.exists() && !(jsonFile.length() == 0)) {
                        FileReader reader = new FileReader(importjsonFile);
                        arcDetailsList = mapper.readValue(reader, ARCDetailsList.class);
                        //mapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
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
                        //jsonFile.delete();
                        System.out.println("importjsonfile Not Exist");
                        //FileReader reader = new FileReader(importjsonFile);
                        arcDetailsList = mapper.readValue(json, ARCDetailsList.class);
                        //System.out.println("JSON String"+json);
                        FileUtils.writeStringToFile(new File(importjsonFile), json);

                    }
                } else {
                    System.out.println("Else Arc details List Error Code:" + checkArcDetailsList.getErrorCode());
                    response = "Exception While Import Json:" + checkArcDetailsList.getDesc();
                    return response;
                }

            } catch (Exception e) {
                System.out.println("Exception While Import Json:" + e);
                response = "Exception While Import Json:" + e;
                return response;
            }


            ObjectMapper objMapper = new ObjectMapper();
            try {
                FileReader reader = new FileReader(importjsonFile);
                arcDetailsList = mapper.readValue(reader, ARCDetailsList.class);
                System.out.println("response compatible with ARCDetailsList POJO");
            } catch (JsonParseException | JsonMappingException e) {
                System.out.println("response NOT compatible with ARCDetailsList POJO");
                response = "The Response From Server is not Compatible";
                //messageStatus(response);     
                return response;
            }

            System.out.println("Import Error Code :::" + arcDetailsList.getErrorCode());


            if (arcDetailsList.getErrorCode() == 0) {
                //System.out.println("Import Json: "+arcDetailsList.toString());

                try {
                    //file.write(connectionStatus);

                    System.out.println("Successfully Copied JSON Object to File..." + arcDetailsList.getDesc());
                    response = "Successfully Imported";
                    //messageStatus(response);
                    return response;
                } catch (Exception e) {
                    System.out.println("Exception" + e);
                }


            } else {
                response = arcDetailsList.getDesc();
                //messageStatus(response);
                return response;
            }


        } catch (Exception e) {
            System.out.println("Exception========" + e);
            response = "Details not Imported From Server. Due to Network Issue";
            //messageStatus(response);
            return response;
        }
        return response;
    }

    @FXML
    private void exportData() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                lblStatus.setText("In Export ");
            }
        });
        System.out.println("Empty Message Status");
        String response = exportjsonFile();
        //messageStatus(response);  
        responseStatus(response);
    }

    public String exportjsonFile() {
        String response = "";
        SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
        String postJson = "";
        String connurl = apiServerCheck.getARCURL();
        String arcno = "123abc";
        String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcno);
        System.out.println("connection status :" + connectionStatus);

        if (!connectionStatus.contentEquals("connected")) {
            //lblStatus.setText("System not connected to network. Connect and try again");
            //messageStatus("System not connected to network. Connect and try again"); 
            //return;
            response = "Network Connection Timed out From Server, Kindly Try Again";
            //messageStatus(response);     
            return response;

        }

        try {
            export = prop.getProp().getProperty("exportfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
            //messageStatus("Export Folder Not Found on the System"); 
            response = "Export Folder Not Found on the System";
            //messageStatus(response);     
            return response;
            //lblStatus.setText("Export Folder Not Found on the System");
        }
        //Deleting Import File
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
            Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
            //response = "Error While Deleting Import Files, Try Again";
            //messageStatus(response);
        }

        //For Production
        File dir = new File(export + "/enc");
        System.out.println(export + "/enc");
        File[] directoryListing = dir.listFiles();
        System.out.println("file count" + directoryListing.length);
        if (directoryListing.length != 0) {
            System.out.println("Inside directory listing");
            for (File child : directoryListing) {
                System.out.println("Inside directory listing - for loop");
                //Code for decryption and process
                AESFileEncryptionDecryption aesDecryptFile = new AESFileEncryptionDecryption();
                try {
                    aesDecryptFile.decryptFile(child.getAbsolutePath(), export + "/dec/" + child.getName());

                } catch (IOException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (InvalidKeySpecException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (NoSuchPaddingException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (InvalidKeyException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (InvalidAlgorithmParameterException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (IllegalBlockSizeException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                } catch (BadPaddingException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Decryption Problem, Try Again";
                    return response;
                }

                //System.out.println("file name:"+child.getName());
                FileReader file = null;
                try {
                    file = new FileReader(export + "/dec/" + child.getName());
                    //file = new FileReader(export+"/dec/"+"AB.json.enc");
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
                    saveEnrollment = mapper.readValue(Paths.get(export + "/dec/" + child.getName()).toFile(), SaveEnrollmentDetails.class);
                    //saveEnrollment.setEnrollmentStationUnitID("");
                    postJson = mapper.writeValueAsString(saveEnrollment);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    //messageStatus("File not Found, Try Again");
                    response = "File not Found, Try Again";
                    //messageStatus(response);
                    return response;
                } catch (IOException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    //messageStatus("File not Found, Try Again");
                    response = "File not Found, Try Again";
                    //messageStatus(response);
                    return response;
                }
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);

                try {
                    ObjectMapper objMapper = new ObjectMapper();
                    ObjectMapper objMappersave = new ObjectMapper();
                    String decResponse = "";
                    System.out.println("Decrypted Json::" + postJson);
                    connurl = apiServerCheck.getEnrollmentSaveURL();
                    decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
                    System.out.println("dec response : " + decResponse);
                    if (decResponse.contains("Exception:")) {
                        // messageStatus(decResponse);    
                        return decResponse;
                    }
                    saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                    System.out.println(" save enrollment : " + saveEnrollmentResponse.toString());
                    enrollmentResponse = objMappersave.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                    System.out.println(" save enrollment : " + enrollmentResponse.toString());
                    if (enrollmentResponse.getErrorCode().equals("0")) {
                        child.delete();
                        aesDecryptFile.delFile(export + "/dec/" + child.getName());
                        aesDecryptFile.delFile(export + "/enc/" + child.getName());
                        //messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 

                        System.out.println(removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc());
                        //Uncomment Afterwards
                        //response = child.getName()+" - "+enrollmentResponse.getDesc();
                        response = enrollmentResponse.getDesc();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                String response = enrollmentResponse.getDesc();
                                //messageStatus(response);
                                responseStatus(response);

                                //LOGGER.log(Level.INFO, status +"Status");
                            }
                        });
                        /*
                          try {
                            importjson=prop.getProp().getProperty("importjsonfolder");
                             File dire = new File(importjson);
                             File[] dirlisting = dire.listFiles();
                             if (dirlisting.length!=0) {
                                System.out.println("Inside Import directory listing");
                                for (File children : dirlisting) {
                                    children.delete();
                                }
                               }
                             } catch (IOException ex) {
                                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                                //response = "Error While Deleting Import Files, Try Again";
                                //messageStatus(response);  
                             }*/
                        //return response;                       


                    } else if (enrollmentResponse.getErrorCode().equals("-1")) {
                        response = removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc();
                        //messageStatus(response);
                        responseStatus(response);
                        child.delete();
                        aesDecryptFile.delFile(export + "/dec/" + child.getName());
                        aesDecryptFile.delFile(export + "/enc/" + child.getName());
                    } else {
                        //lblStatus.setText(child.getName()+" - "+enrollmentResponse.getDesc());
                        //messageStatus(child.getName()+" - "+enrollmentResponse.getDesc());
                        System.out.println(removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc());
                        response = removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc();
                        //messageStatus(response);
                        responseStatus(response);
                        //return response;
                    }

                } catch (Exception e) {
                    System.out.println("Exception in Export" + e);
                    //messageStatus("Exception in Export"+e);
                    response = "Exception in Export" + e;
                    //messageStatus(response);     
                    responseStatus(response);
                    //return response; 
                }
            }
        } else {
            //lblStatus.setText("No Biometric Data to Export");
            //messageStatus("No Biometric Data to Export");
            System.out.println("The Directory is empty.. No encrypted files");
            response = "No Biometric Data to Export";
            //messageStatus(response);     
            return response;

        }
        return response;
    }

    @FXML
    public void showImportExportHome() throws IOException {
        App.setRoot("first_screen");

    }

    @FXML
    public void showImportExportBack() throws IOException {
        App.setRoot("first_screen");

    }

    @FXML
    public void mergejson() throws IOException {
        // App.setRoot("first_screen");

        String meesage = MergeJson();
        System.out.println("----" + meesage);
    }


    private String MergeJson() {

        try {
            String importjsonFile = prop.getProp().getProperty("importjsonfile");
            File jsonFile = new File(importjsonFile);
            String connurl = apiServerCheck.getDemographicURL();
            ARCDetailsList arcDetailsList = new ARCDetailsList();
            ObjectMapper mapper = new ObjectMapper();

            JsonFactory jfactory = new JsonFactory();
            //ObjectReaderWriter objReadWrite = new ObjectReaderWriter();           


            List<ArcDetailsMapping> mappingResult;


            //String unitid=unit.getValue();
            String unitid = "INSD";
            //String unitid="";
            //System.out.println("UnitID::::"+unitid);
            String json = apiServerCheck.getDemoGraphicDetailsAPI(connurl, unitid);
            if (jsonFile.exists()) {
                FileReader reader = new FileReader(importjsonFile);
                arcDetailsList = mapper.readValue(reader, ARCDetailsList.class);
                //mapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
                String postJson = mapper.writeValueAsString(arcDetailsList);
                JsonParser jParser1 = jfactory.createJsonParser(json);
                JsonParser jParser2 = jfactory.createJsonParser(postJson);
                ARCDetailsList arcDetailsList1 = mapper.readValue(jParser1, ARCDetailsList.class);// JsonObj is Pojo for your jsonObject
                ARCDetailsList arcDetailsList2 = mapper.readValue(jParser2, ARCDetailsList.class);
                arcDetailsList1.getArcDetails().addAll(arcDetailsList2.getArcDetails());

                String finaljson = mapper.writeValueAsString(arcDetailsList1);
                FileUtils.writeStringToFile(new File(importjsonFile), finaljson);
            } else {
                System.out.println("importjsonfile Not Exist");
                arcDetailsList = mapper.readValue(json, ARCDetailsList.class);
                System.out.println("JSON String" + json);
                FileUtils.writeStringToFile(new File(importjsonFile), json);
            }
        } catch (Exception e) {
            System.out.println("Exception While Import Json:" + e);
        }


        return null;
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        String response = "";
        String json = "";
        try {
            String connurl = apiServerCheck.getUnitListURL();
            String connectionStatus = apiServerCheck.getStatusUnitListAPI(connurl);
            System.out.println("connection status :" + connectionStatus);


            if (!connectionStatus.contentEquals("connected")) {
                //    if(connectionStatus.contentEquals("connected")) {
                //labelstatus.setText(connectionStatus);
                //labelstatus.setText("Enter the Correct URL in Mafis API");
                // Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                response = "Network Connection Timed out From Server, Kindly Try Again.";
                //messageStatus(response);
                responseStatus(response);
                //return response;
            } else {
                try {
                    json = apiServerCheck.getUnitListAPI(connurl);
                    System.out.println("Output str : " + json);
                    String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BYT TKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BXT HKL\",\"Value\":\"INSZ\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
                    ObjectMapper objectmapper = new ObjectMapper();
                    UnitListDetails details = objectmapper.readValue(json1, UnitListDetails.class);
                    if (details.getErrorCode().equals("0")) {
                        response = "Unit Details Fetched Successfully";
                        //messageStatus(response);
                        responseStatus(response);
                    } else {
                        response = details.getDesc();
                        //messageStatus(response);
                        responseStatus(response);

                        //return response;
                    }


                    try {

                        ObservableList<Units> units = FXCollections.observableArrayList(details.getUnits()).sorted();
                        comboUnitId.setItems(units);
                        //comboUnitId.setEditable(true);
                        //AutoCompleteComboBoxListener autoComplete = new AutoCompleteComboBoxListener<Units>(comboUnitId);

                        //Added to test the searchable Combo box
                        //comboUnitId.setEditable(true);
                /*
                comboUnitId.setOnKeyPressed(new EventHandler<KeyEvent>() {

                        @Override
                        public void handle(KeyEvent t) {
                            comboUnitId.hide();
                        }
                    });
                 */
                        //Added to test the searchable Combo box

                        try {
                            new AutoCompleteComboBoxListener(comboUnitId);
                        } catch (Exception e) {
                            System.out.println("Exception At AutoComplete :::" + e);
                        }


                        //Added to test the searchable Combo box


                        System.out.println("combo value : " + comboUnitId.getSelectionModel().getSelectedItem());

                        comboUnitId.valueProperty().addListener((obs, oldval, newval) -> {

                            if (newval != null)
                                //System.out.println("Selected unit: " + newval.getCaption()+ ". ID: " + newval.getValue());
                                System.out.println("Selected unit1: " + newval);
                            responseStatus("");
                        });


                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("error :" + e.getMessage());
                        response = "Kindly select the Values From Unit";
                        //messageStatus(response);
                        responseStatus(response);
                        //return response;
                    }
                } catch (NullPointerException e) {
                    System.out.print("NullPointerException caught");

                    response = "Unit List is not Available in the Server";
                    //messageStatus(response);
                    responseStatus(response);
                    //return response;
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(ImportExportController_bak170822.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Unit List is not Available in the Server";
                    //messageStatus(response);     
                    responseStatus(response);
                    //return response;
                }

            }
        } catch (NullPointerException e) {
            System.out.print("NullPointerException caught");
            response = "Unit List is not Available in the Server";
            //messageStatus(response);
            responseStatus(response);
            //return response;
        }
        //return response;
        //messageStatus(response);     
    }


}
