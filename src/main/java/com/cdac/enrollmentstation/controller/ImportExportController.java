/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;


import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.*;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class ImportExportController implements Initializable {
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

    @FXML
    private ImageView refreshicon;

    Thread exportthread = null;

    Thread importthread = null;

    Thread fetchunitthread = null;

    private String searchstatus = "false";

    //For List View
    @FXML
    private ListView<String> unitlistview;

    @FXML
    private TextField searchtext;

    List<String> unitList = new ArrayList<String>();

    // public ComboBox comboBox;
    // private ObservableList<Units> data;
    // private Integer sid;
    //AutoCompleteComboBoxListener autocompleteBox ;

    //For Application Log
    ApplicationLog appLog = new ApplicationLog();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    Handler handler;


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


    public ImportExportController() {
        //this.handler = appLog.getLogger();
        //LOGGER.addHandler(handler); 
        /*
        //Added to test the searchable Combo box
                       
                            Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                // Update UI here.
                                  try {
                                        new AutoCompleteComboBoxListener(comboUnitId);
                                        //searchstatus = "true";
                                        System.out.println("Search Status At : Initialize");
                                      } catch (Exception e) {
                                         System.out.println("Exception At AutoComplete :::" + e);
                                      }
                              }
                           });
                       
        //Added to test the searchable Combo box  
    */
    }

    @FXML
    private void fetchunitdetails() {

        //String response = fetchUnit();
        //messageStatus(response);                          
        //responseStatus(response);
        try {
//            ImportExportController ip = new ImportExportController();
            // URL arg0 = null;
            // ResourceBundle arg1 =null;
            //initialize(arg0, arg1);
            fetchunitthread = new Thread(fetchUnit);
            fetchunitthread.start();
        } catch (Exception e) {
            //System.out.println("Error in loop::" + e);            
            LOGGER.log(Level.INFO, "Error in Fetch Unit Thread::" + e);
        }
    }

    Runnable fetchUnit = new Runnable() {
        @Override
        public void run() {
            String response = "";
            response = "Fetching Unit Please Wait...";
            responseStatus(response);
            String json = "";
            try {

                String connurl = apiServerCheck.getUnitListURL();
                String connectionStatus = apiServerCheck.getStatusUnitListAPI(connurl);
                //System.out.println("connection status :" + connectionStatus);
                LOGGER.log(Level.INFO, "connection status :" + connectionStatus);

                if (!connectionStatus.contentEquals("connected")) {
                    //labelstatus.setText(connectionStatus);
                    //labelstatus.setText("Enter the Correct URL in Mafis API");
                    // Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Network Connection Issue. Check Connection and Try Again";
                    //messageStatus(response);     
                    responseStatus(response);
                    //return;
                } else {
                    try {
                        json = apiServerCheck.getUnitListAPI(connurl);
                        String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BYT TKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BXT HKL\",\"Value\":\"INSB\"},{\"Caption\":\"BXXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BYXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BDXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT YKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT KLL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT MKL\",\"Value\":\"INST\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
                        System.out.println("Output str : " + json);
                        ObjectMapper objectmapper = new ObjectMapper();
                        UnitListDetails details = objectmapper.readValue(json, UnitListDetails.class);
                        if (details.getErrorCode().equals("0")) {
                            response = "Unit Details Fetched Successfully";
                            System.out.println("If loop" + response);
                            //messageStatus(response);     
                            //responseStatus(response);
                        } else {
                            response = details.getDesc();
                            System.out.println("Else:" + response);
                            //messageStatus(response);  
                            responseStatus(response);
                            return;
                            //return response;
                        }

                        try {

                            //List View Code

                            unitlistview.setItems(null);
                            List<String> unitCaption = new ArrayList<String>();
                            for (Units unitLists : details.getUnits()) {
                                // Print all elements of ArrayList
                                System.out.println(unitLists.getCaption());
                                unitCaption.add(unitLists.getCaption());

                            }

                            ObservableList<String> items = FXCollections.observableArrayList(unitCaption).sorted();
                            // ObservableList<String> items = FXCollections.observableArrayList("Cricket", "Chess", "Kabaddy", "Badminton",    "Football", "Golf", "CoCo", "car racing").sorted();
                            unitlistview.setItems(items);

                            searchtext.textProperty().addListener(new javafx.beans.value.ChangeListener() {
                                public void changed(ObservableValue observable, Object oldVal,
                                                    Object newVal) {
                                    search((String) oldVal, (String) newVal, items);
                                }
                            });
                            int index = unitlistview.getItems().size();
                            //unitlistview.scrollTo(index);

                            unitlistview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                            unitlistview.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String old_val, String new_val) -> {
                                ObservableList<String> selectedItems = unitlistview.getSelectionModel().getSelectedItems();

                                System.out.println("Selected Items::::" + selectedItems);
                                StringBuilder builder = new StringBuilder();
                                unitList.clear();
                                for (String name : selectedItems) {
                                    builder.append(name + "\n");
                                    System.out.println("String builder:::" + builder + "" + "String::" + name);
                                    unitList.add(name);
                                    for (String uList : unitList) {
                                        System.out.println("Unit List At Refresh::" + uList);
                                    }

                                }
                                //listlabel.setText(builder.toString());

                            });

                            //List View Code


                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("error :" + e.getMessage());
                            response = "Kindly select the Values from Unit";
                            //messageStatus(response);   
                            responseStatus(response);
                            return;
                            //return response;
                        }
                    } catch (NullPointerException e) {
                        //System.out.print("NullPointerException caught");
                        LOGGER.log(Level.INFO, "NullPointerException caught at fetchunit::" + e);

                        response = "Unit List is not Available in the Server";
                        //messageStatus(response); 
                        responseStatus(response);
                        return;
                        //return response;
                    } catch (JsonProcessingException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Unit List is not Available in the Server";
                        responseStatus(response);
                        return;
                        //return response;
                    }

                }
            } catch (NullPointerException e) {
                //System.out.print("NullPointerException caught");
                LOGGER.log(Level.INFO, "NullPointerException caught at fetchUnit :" + e);
                response = "Unit List is not Available in the Server";
                //messageStatus(response);     
                responseStatus(response);
                return;
                //return response;
            }
            responseStatus(response);
            return;
        }
    };

    @FXML
    public void importData() {

        //String response = importjsonFile();
        //messageStatus(response);      
        // responseStatus(response);
        try {
            importthread = new Thread(importjsonFile);
            importthread.start();
        } catch (Exception e) {
            System.out.println("Error in loop::" + e);
            //LOGGER.log(Level.INFO, "Error in loop::"+e);
        }
    }

    Runnable importjsonFile = new Runnable() {
        @Override
        public void run() {
            String response = "";
            response = "Importing Please Wait...";
            responseStatus(response);
            String jsonurllist = "";
            String connurlUnitList = apiServerCheck.getUnitListURL();
            //String unitid = "";
            //For List View Code
            List<String> unitIds = new ArrayList<String>();
            //Units unit =new Units();
            try {

                //Deleting Import File
                 /*
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
                        //response = "Error While Deleting Import Files, Try Again";
                        //messageStatus(response);  
                    } 
                   */
                //For List View Code
                HashSet<String> unitListHash = new HashSet<String>(unitList);
                for (String uList : unitListHash) {
                    System.out.println("Unit List from Hash::" + uList);
                }
                //For List View Code

                jsonurllist = apiServerCheck.getUnitListAPI(connurlUnitList);
                String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BYT TKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BXT HKL\",\"Value\":\"INSB\"},{\"Caption\":\"BXXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BYXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BDXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT YKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT KLL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT MKL\",\"Value\":\"INST\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
                System.out.println("Output str : " + jsonurllist);
                ObjectMapper objectmapper = new ObjectMapper();
                UnitListDetails details = objectmapper.readValue(jsonurllist, UnitListDetails.class);
                if (details.getErrorCode().equals("0")) {
                    response = "Unit Details Fetched Successfully";
                    System.out.println("At Import Fetch Unit If :" + response);
                    //messageStatus(response);
                    //responseStatus(response);
                } else {
                    response = details.getDesc();
                    System.out.println("At Import Fetch Unit Else :" + response);
                    //messageStatus(response);
                    responseStatus(response);
                    return;
                    //return response;
                }

                //For List View Code
                List<Units> unit = details.getUnits();
                for (Units result1 : unit) {
                    for (String uList : unitListHash) {
                        System.out.println("Unit List3::" + uList);
                        System.out.println("Unit List3GetCaption::" + result1.getCaption());
                        if (result1.getCaption().equals(uList)) {
                            //unitid=result1.getValue();
                            unitIds.add(result1.getValue().trim());
                            //System.out.println("Value:"+unitid);
                            System.out.println("caption:" + result1.getCaption());
                        }
                    }
                }

                if (unitIds.size() == 0) {
                    response = "Kindly select the Values from Unit";
                    responseStatus(response);
                    return;
                }


                //For List View Code


            } catch (Exception e) {
                System.out.println("Exception::" + e);
                response = "Kindly select the Values from Unit";
                responseStatus(response);
                return;

            }

            try {
                //importjson=prop.getProp().getProperty("importjsonfile");
                importjson = prop.getProp().getProperty("importjsonfolder");
            } catch (IOException ex) {
                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                response = "Import Json Folder Not Exist in File.properties";
                //messageStatus(response);     
                responseStatus(response);
                return;
            }
            try {
                export = prop.getProp().getProperty("exportfolder");
            } catch (IOException ex) {
                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                response = "Export Json Folder Not Exist in File.properties";
                //messageStatus(response);     
                responseStatus(response);
                return;
            }
            System.out.println("import data");

            //Checking Connection Status
            /*
            String connurl = apiServerCheck.getARCURL();
            String arcno = "123abc";
            String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcno);
            System.out.println("connection status :" + connectionStatus);
            if (!connectionStatus.contentEquals("connected")) {
                //lblStatus.setText("System not connected to network. Connect and try again");
                response = "Network Connection Issue. Check Connection and Try Again";
                //messageStatus(response);     
                responseStatus(response);
                return;
            }*/

            try {
                String ckeckUnitid = "";
                for (String uNitLists : unitIds) {
                    System.out.println("Unit ListValue4::" + uNitLists);
                    ckeckUnitid = uNitLists;
                }
                String connurl = apiServerCheck.getDemographicURL();
                //   String unitid=unit.getValue();
                //String unitid="";
                //System.out.println("UnitID::::" + unitid);

                String connectionStatus = apiServerCheck.getDemoGraphicDetailsAPI(connurl, ckeckUnitid);


                System.out.println("Output :::::::" + connectionStatus);
                if (connectionStatus.contains("Exception")) {
                    //lblStatus.setText(connectionStatus);
                    response = connectionStatus;
                    //messageStatus(response);     
                    responseStatus(response);
                    return;
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
                    //For List View Code

                    for (String uNitLists : unitIds) {
                        System.out.println("Unit ListValue5::" + uNitLists);
                        //String json = apiServerCheck.getDemoGraphicDetailsAPI(connurl1, unitid);
                        String json = apiServerCheck.getDemoGraphicDetailsAPI(connurl1, uNitLists);
                        try {
                            checkArcDetailsList = mapper.readValue(json, ARCDetailsList.class);
                        } catch (Exception e) {
                            //response = "Exception While Read Json From Server:" + e;
                            System.out.println("Exception While Read Json From Server:" + e);
                            response = "ARC data From Server had issue";
                            responseStatus(response);
                            //return;
                        }
                        if (checkArcDetailsList.getErrorCode() == 0) {
                            System.out.println("Arc details List Error Code:" + checkArcDetailsList.getErrorCode());

                            //System.out.println("JSON response::"+json);
                            if (jsonFile.exists() && !(jsonFile.length() == 0)) {
                                FileReader reader = new FileReader(importjsonFile);
                                arcDetailsList = mapper.readValue(reader, ARCDetailsList.class);
                                //mapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
                                String postJson = mapper.writeValueAsString(arcDetailsList);
                            /*
                            //Added for writing Json on Unit files
                            FileUtils.writeStringToFile(new File(importjson+uNitLists), json);
                            ARCDetailsList arcDetailsListForEachUnit = mapper.readValue(Paths.get(importjson+uNitLists).toFile(), ARCDetailsList.class);
                            String postJsonForEachUnit = mapper.writeValueAsString(arcDetailsListForEachUnit);
                            JsonParser jParser1 = jfactory.createJsonParser(postJsonForEachUnit);
                            //Added for writing Json on Unit files */

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
                            /*
                            //Added for writing Json on Unit files
                            FileUtils.writeStringToFile(new File(importjson+uNitLists), json);
                            //Added for writing Json on Unit files*/
                            }
                        } else {
                            System.out.println("Else Arc details List Error Code:" + checkArcDetailsList.getErrorCode());
                            //response = "Exception While Import Json:" + checkArcDetailsList.getDesc();

                            response = checkArcDetailsList.getDesc();
                            responseStatus(response + " " + "Try again with Other than " + uNitLists + " " + "Unit");
                            return;
                        }
                    }//For List View Code
                } catch (Exception e) {
                    System.out.println("Exception While Import Json:" + e);
                    //response = "Exception While Import Json:" + e;
                    response = "No ARC available for Selected Unit";
                    responseStatus(response);
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
                    //messageStatus(response);     
                    responseStatus(response);
                    return;
                }

                System.out.println("Import Error Code :::" + arcDetailsList.getErrorCode());

                if (arcDetailsList.getErrorCode() == 0) {
                    //System.out.println("Import Json: "+arcDetailsList.toString());

                    try {
                        //file.write(connectionStatus);    

                        System.out.println("Successfully Copied JSON Object to File..." + arcDetailsList.getDesc());
                        response = "Successfully Imported";
                        //messageStatus(response);     
                        responseStatus(response);
                        return;
                    } catch (Exception e) {
                        System.out.println("Exception" + e);
                    }

                } else {
                    response = arcDetailsList.getDesc();
                    //messageStatus(response);     
                    responseStatus(response);
                    return;
                }

            } catch (Exception e) {
                System.out.println("Exception========" + e);
                //response = "Details not Imported From Server. Due to Network Issue";
                response = "No ARC Available for Selected Unit";
                //messageStatus(response);     
                responseStatus(response);
                return;
            }
            responseStatus(response);
            return;
        }
    };

    @FXML
    private void exportData() {

        try {
            exportthread = new Thread(exportjsonFile);
            exportthread.start();
        } catch (Exception e) {
            System.out.println("Error in loop::" + e);
            //LOGGER.log(Level.INFO, "Error in loop::"+e);
        }

        //String response = exportjsonFile();
        //messageStatus(response);  
        //responseStatus(response);
    }

    Runnable exportjsonFile = new Runnable() {
        @Override
        public void run() {
            String response = "";
            response = "Exporting Please Wait...";
            responseStatus(response);
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
                response = "Network Connection Issue. Check Connection and Try Again";
                responseStatus(response);
                return;

            }

            try {
                export = prop.getProp().getProperty("exportfolder");
            } catch (IOException ex) {
                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                //messageStatus("Export Folder Not Found on the System"); 
                response = "Export Folder Not Found on the System";
                responseStatus(response);
                return;
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
                Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
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
                    response = "Exporting Please Wait...";
                    responseStatus(response);
                    //Code for decryption and process
                    AESFileEncryptionDecryption aesDecryptFile = new AESFileEncryptionDecryption();
                    try {
                        aesDecryptFile.decryptFile(child.getAbsolutePath(), export + "/dec/" + child.getName());

                    } catch (IOException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (InvalidKeySpecException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (NoSuchPaddingException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (InvalidKeyException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (InvalidAlgorithmParameterException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (IllegalBlockSizeException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
                    } catch (BadPaddingException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        response = "Decryption Problem, Try Again";
                        responseStatus(response);
                        return;
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
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        //messageStatus("File not Found, Try Again");
                        response = "File not Found, Try Again";
                        //messageStatus(response);     
                        responseStatus(response);
                        return;
                    } catch (IOException ex) {
                        Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                        //messageStatus("File not Found, Try Again"); 
                        response = "File not Found, Try Again";
                        //messageStatus(response);     
                        responseStatus(response);
                        return;
                    }
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
                            responseStatus(decResponse);
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
                            //messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 

                            System.out.println(removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc());
                            //Uncomment Afterwards
                            //response = enrollmentResponse.getDesc();
                            response = "Biometric data exported successfully";
                            //System.out.println(" Hai test"+response);
                            //response = removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc();
                            responseStatus(response);
                            
                            
                            /*
                            response = enrollmentResponse.getDesc();
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    String response = enrollmentResponse.getDesc();
                                    //messageStatus(response);
                                    responseStatus(response);

                                    //LOGGER.log(Level.INFO, status +"Status");            
                                }
                            });*/
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
                            response = enrollmentResponse.getDesc();
                            //response = removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc();
                            //messageStatus(response);    
                            responseStatus(response);
                            child.delete();
                            aesDecryptFile.delFile(export + "/dec/" + child.getName());
                            aesDecryptFile.delFile(export + "/enc/" + child.getName());
                        } else {
                            //lblStatus.setText(child.getName()+" - "+enrollmentResponse.getDesc());
                            //messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 
                            System.out.println(removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc());
                            response = enrollmentResponse.getDesc();
                            //response = removeFileExtension(child.getName(), true) + " - " + enrollmentResponse.getDesc();
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
                responseStatus(response);
                return;

            }
            responseStatus(response);
            return;
        }
    };

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
            // String connurl = apiServerCheck.getARCURL();
            //String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, "abc123");
            System.out.println("connection status initialize :" + connectionStatus);

            if (!connectionStatus.contentEquals("connected")) {
                //    if(connectionStatus.contentEquals("connected")) {
                //labelstatus.setText(connectionStatus);
                //labelstatus.setText("Enter the Correct URL in Mafis API");
                // Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                response = "Network Connection Issue. Check Connection and Try Again";
                //messageStatus(response);   
                responseStatus(response);
                return;
            } else {
                try {

                    json = apiServerCheck.getUnitListAPI(connurl);
                    System.out.println("Output str : " + json);
                    String json1 = "{\"Units\":[{\"Caption\":\"INS DPS\",\"Value\":\"INSD\"},{\"Caption\":\"XPD GD\",\"Value\":\"INSX\"},{\"Caption\":\"ASD YPS\",\"Value\":\"INSV\"},{\"Caption\":\"BGH JKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BYT TKL\",\"Value\":\"INSZ\"},{\"Caption\":\"BXT HKL\",\"Value\":\"INSB\"},{\"Caption\":\"BXXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BYXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BDXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT HKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT YKL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT KLL\",\"Value\":\"INST\"},{\"Caption\":\"BBXTT MKL\",\"Value\":\"INST\"}],\"ErrorCode\":0,\"Desc\":\"SUCCESS\"}";
                    ObjectMapper objectmapper = new ObjectMapper();
                    UnitListDetails details = objectmapper.readValue(json, UnitListDetails.class);
                    if (details.getErrorCode().equals("0")) {
                        response = "Unit Details Fetched Successfully";
                        //messageStatus(response);   
                        responseStatus(response);
                    } else {
                        response = details.getDesc();
                        //messageStatus(response);     
                        responseStatus(response);
                        return;
                        //return response;
                    }

                    try {

                        //List View Code

                        //StringBuilder builder = new StringBuilder();
                        unitlistview.setItems(null);
                        List<String> unitCaption = new ArrayList<String>();
                        for (Units unitLists : details.getUnits()) {

                            // Print all elements of ArrayList
                            System.out.println(unitLists.getCaption());
                            unitCaption.add(unitLists.getCaption());

                        }

                        ObservableList<String> items = FXCollections.observableArrayList(unitCaption).sorted();
                        // ObservableList<String> items = FXCollections.observableArrayList("Cricket", "Chess", "Kabaddy", "Badminton",    "Football", "Golf", "CoCo", "car racing").sorted();
                        unitlistview.setItems(items);

                        searchtext.textProperty().addListener(new javafx.beans.value.ChangeListener() {
                            public void changed(ObservableValue observable, Object oldVal,
                                                Object newVal) {
                                search((String) oldVal, (String) newVal, items);

                            }
                        });
                        int index = unitlistview.getItems().size();
                        //unitlistview.scrollTo(index);

                        unitlistview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


                        unitlistview.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends String> ov, String old_val, String new_val) -> {
                            ObservableList<String> selectedItems = unitlistview.getSelectionModel().getSelectedItems();

                            System.out.println("Selected Items::::" + selectedItems);
                            StringBuilder builder = new StringBuilder();
                            unitList.clear();
                            for (String name : selectedItems) {
                                builder.append(name + "\n");
                                System.out.println("String builder:::" + builder + "" + "String::" + name);
                                unitList.add(name);
                                for (String uList : unitList) {
                                    System.out.println("Unit List1::" + uList);
                                }

                            }
                            //listlabel.setText(builder.toString());

                        });
                
                          /*
                      if(unitList.size()==0){
                           response = "Kindly select the Values from Unit";
                           responseStatus(response);
                           return;
                      }
                        */
                        for (String uList : unitList) {
                            System.out.println("Unit List2::" + uList);
                        }


                        //List View Code


                        //ObservableList<Units> units = FXCollections.observableArrayList(details.getUnits()).sorted();
                        // comboUnitId.setItems(units);

                        //Added to test the searchable Combo box
                          /*
                            Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                // Update UI here.
                                  try {
                                        new AutoCompleteComboBoxListener(comboUnitId);
                                        //searchstatus = "true";
                                        System.out.println("Search Status At : Initialize");
                                      } catch (Exception e) {
                                         System.out.println("Exception At AutoComplete :::" + e);
                                      }
                              }
                           });
                            */
                        //Added to test the searchable Combo box
                         /*
                        System.out.println("combo value : " + comboUnitId.getSelectionModel().getSelectedItem());
                        

                        comboUnitId.valueProperty().addListener((obs, oldval, newval) -> {

                            if (newval != null) //System.out.println("Selected unit: " + newval.getCaption()+ ". ID: " + newval.getValue());
                            {
                                //System.out.println("Selected unit1: " + newval);
                                LOGGER.log(Level.INFO, "Selected unit1: " + newval);
                            }
                            responseStatus("");
                        }); */

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
                    Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
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

    }

    public void search(String oldVal, String newVal, ObservableList<String> items) {
        if (oldVal != null && (newVal.length() < oldVal.length())) {
            unitlistview.setItems(items);
        }
        String value = newVal.toUpperCase();
        ObservableList<String> subentries = FXCollections.observableArrayList();
        for (Object entry : unitlistview.getItems()) {
            boolean match = true;
            String entryText = (String) entry;
            System.out.println("VALUE::::" + value);
            System.out.println("ENTRY TEXT::::" + entryText);
            if (!entryText.toUpperCase().contains(value)) {
                match = false;
                //break;
            }
            if (match) {
                subentries.add(entryText);
            }
        }
        unitlistview.setItems(subentries);
    }

}
