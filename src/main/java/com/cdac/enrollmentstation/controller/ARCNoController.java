/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.model.ARCDetailsList;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.service.ObjectReaderWriter;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.TouchEvent;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author boss
 */
public class ARCNoController implements Initializable {

    public ARCDetails arcDetails;

    @FXML
    private TextField txtARCno, txtARCBarcode;

    @FXML
    private Label lblStatus;

    public APIServerCheck apiServerCheck = new APIServerCheck();

    @FXML
    private Label txtName, txtRank, txtapp, txtUnit, txtFinger, txtiris, txtarcstatus, txtarcno, txtbiometricoptions;

    @FXML
    Hyperlink txtDlink;

    private SecretKey skey;

    @FXML
    private Button fingerprintcapture, barcodearcbutton;

    //private String importjson="/usr/share/enrollment/json/import/arclistimported.json";
    private String importjson = "";

    private String exportjson = "";

    //For Application Log
    ApplicationLog appLog = new ApplicationLog();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    Handler handler;

    TestProp prop = new TestProp();

    private String unitId = null;
    private String stationId = null;
    private String importjsonFile = null;
    private String curpesid = null;


    public ARCNoController() {
        //this.handler = appLog.getLogger();
    }

    @FXML
    private void showFingerPrint() throws IOException {

        //Added For Only Photo


        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        if (holder.getArcDetails().getBiometricOptions().contains("Photo") || holder.getArcDetails().getBiometricOptions().contains("photo")) {
            LOGGER.log(Level.INFO, "Going to Camera Page");
            try {
                App.setRoot("camera");
            } catch (IOException ex) {
                //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                LOGGER.log(Level.INFO, "IOException At Get Biometric Options:" + ex);
            }
        } else if (holder.getArcDetails().getBiometricOptions().contains("Biometric") || holder.getArcDetails().getBiometricOptions().contains("biometric") || holder.getArcDetails().getBiometricOptions().contains("Both") || holder.getArcDetails().getBiometricOptions().contains("both")) {

            //App.setRoot("slapscanner_1");
            //App.setRoot("slapscanner");
            //Code added by K. Karthikeyan - Start [for app crash and resume from previous data]
            String saveenrollment = null;
            saveenrollment = prop.getProp().getProperty("saveenrollment");
            if (saveenrollment.isBlank() || saveenrollment.isEmpty() || saveenrollment == null) {
                //System.out.println("The property 'saveenrollment' is empty, Please add it in file properties");
                LOGGER.log(Level.INFO, "The property 'saveenrollment' is empty, Please add it in file properties");
                return;
            }
            //String objFilePath = "/usr/share/enrollment/save/saveEnrollment.txt";
            File f = new File(saveenrollment);
            if (f.exists()) {
                //To-do
                ObjectReaderWriter objReadWrite = new ObjectReaderWriter();
                SaveEnrollmentDetails s = objReadWrite.reader();
                String prevStatus = s.getEnrollmentStatus();
                LOGGER.log(Level.INFO, "Enrollment Status :{0}", prevStatus);
                //System.out.println("Enrollment Status :"+prevStatus);
                //System.out.println("ARC No Entered : "+txtARCno.getText());
                //System.out.println("ARC No Entered : "+txtARCBarcode.getText());
                //System.out.println("Previous ARC No : "+s.getArcNo());
                LOGGER.log(Level.INFO, "Previous ARC No :{0}", s.getArcNo());

                if (s.getArcNo() != null) {

                    //System.out.println("S Get ARC NO ::"+s.getArcNo());
                    //System.out.println("Txt ARC ::"+txtarcno.getText());

                    //if(s.getArcNo().equals(txtARCno.getText())){
                    // if(s.getArcNo().equals(txtARCBarcode.getText())||s.getArcNo().equals(txtARCno.getText())){
                    if (s.getArcNo().equals(txtarcno.getText())) {
                        //if(s.getArcNo().equals(txtarcno.getText())){

                        //System.out.println("Both ARC same and proceeeding");
                        LOGGER.log(Level.INFO, "Both ARC same and proceeeding");
                        //Commented For Only Photo
                        //ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                        holder.setSaveEnrollmentDetails(s);

                        switch (prevStatus) {
                            case "FingerPrintCompleted":
                                //System.out.println("Going to Iris Capture");
                                LOGGER.log(Level.INFO, "Going to Iris Capture");
                            {
                                try {
                                    App.setRoot("iris");
                                } catch (IOException ex) {
                                    Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "IOException:" + ex);
                                }
                            }
                            break;


                            case "IrisCompleted":
                                //System.out.println("Going to Camera Capture");
                                LOGGER.log(Level.INFO, "Going to Camera Capture");
                            {
                                try {
                                    App.setRoot("camera");
                                } catch (IOException ex) {
                                    //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "IOException:" + ex);
                                }
                            }
                            break;


                            case "PhotoCompleted":
                                //System.out.println("Going to Submit Page");
                                LOGGER.log(Level.INFO, "Going to Submit Page");
                            {
                                try {
                                    App.setRoot("capturecomplete");
                                } catch (IOException ex) {
                                    //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "IOException:" + ex);
                                }
                            }
                            break;


                            case "SUCCESS":
                                //System.out.println("Going for Finger print scan");
                                LOGGER.log(Level.INFO, "Going for Finger print scan");
                            {
                                try {
                                    App.setRoot("capturecomplete");
                                } catch (IOException ex) {
                                    //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "IOException:" + ex);
                                }
                            }
                            break;


                            default:
                                //System.out.println("Going for Finger print scan");
                                LOGGER.log(Level.INFO, "Going for Finger print scan");
                            {
                                try {
                                    App.setRoot("slapscanner_1");
                                } catch (IOException ex) {
                                    //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "IOException:" + ex);
                                }
                            }
                            break;


                        }

                    } else {
                        //System.out.println("Previous file not there, Going for Finger print scan");
                        LOGGER.log(Level.INFO, "Previous file not there, Going for Finger print scan");
                        try {
                            //ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                            //System.out.println("Biometric Options- Arc Number Available in Save:"+holder.getARC().getBiometricoptions());
                            App.setRoot("slapscanner_1");
                        } catch (IOException ex) {
                            //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                            LOGGER.log(Level.INFO, "IOException:" + ex);
                        }
                    }
                } else {

                    //System.out.println("Previous Value is null, Going for Finger print scan");
                    LOGGER.log(Level.INFO, "Previous Value is null, Going for Finger print scan");
                    try {
                        //ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                        //System.out.println("Biometric Options- Arc Number Not Available in Save:"+holder.getARC().getBiometricoptions());
                        App.setRoot("slapscanner_1");
                    } catch (IOException ex) {
                        //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
                        LOGGER.log(Level.INFO, "IOException:" + ex);
                    }
                }

            } else {
                LOGGER.log(Level.INFO, "Save Enrollment File Not Exist");
                lblStatus.setText("Save Enrollment File Not Exist");
            }

            //Code added by K. Karthikeyan - finish [for app crash and resume from previous data]

        }//Added For Only Photo
        else {
            lblStatus.setText("Biometric capturing not required for given ARC Number");
            LOGGER.log(Level.INFO, "Biometric capturing not required for given ARC Number");
        }
    }

    @FXML
    private void showDlink() {
        try {
            App.setRoot("detaillink");
        } catch (IOException ex) {
            //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, "IOException:" + ex);
        }
    }


    @FXML
    private void showEnrollmentHome() {
        try {
            App.setRoot("second_screen");
        } catch (IOException ex) {
            //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, "IOException:" + ex);
        }


    }

    @FXML
    private void showHome() {
        try {
            App.setRoot("first_screen");
        } catch (IOException ex) {
            //Logger.getLogger(ARCNoController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, "IOException:" + ex);
        }


    }
    
    
    /*@FXML
    private void showARCDetails() throws IOException{ 
             
               
        try{
        
           // postJson = mapper.writeValueAsString(saveEnrollment);
            //postJson = postJson.replace("\n", "");
           //Development
           File csvFile = new File("/media/boss/DVPT/2021/Mantra/SampleDataToExportForBiometricCapture_v1.2.csv");
           //Production
           //File csvFile = new File("/etc/SampleDataToExportForBiometricCapture_v1.2.csv");
           if(csvFile.exists()) {
                String arcNo = txtARCno.getText();
                //Development
                try (BufferedReader file = new BufferedReader(new FileReader("/media/boss/DVPT/2021/Mantra/SampleDataToExportForBiometricCapture_v1.2.csv"))) {
                //Production
                //try (BufferedReader file = new BufferedReader(new FileReader("/etc/SampleDataToExportForBiometricCapture_v1.2.csv"))) {
                String line = " ";
                String input = " ";

                if(txtARCno.getText().isEmpty()){
                    lblStatus.setText("Please input ARCNo/scan barcode and try again");
                    return;
                }
                    else{
                    Boolean arcFound = false;
                    while((line = file.readLine()) != null){
                        String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                        String Name = (tokens[11]);
                        String Unit = (tokens[17]);
                        String Rank = (tokens[18]);
                        String[] Fingersleft = (tokens[52]).split(",");
                        String[] Fingersright = (tokens[53]).replaceAll("^\"|\"$", "").split(",");
                        String[] arrIris = (tokens[54].split(","));   //{}; //(tokens[54].split(","));
                        List<String> arr = new ArrayList<>();
                        Collections.addAll(arr, Fingersleft);
                        Collections.addAll(arr, Fingersright);
                        System.out.println("arr :" +arr);

                        List<String> arrIr = new ArrayList<>();
                        Collections.addAll(arrIr, arrIris);
                        System.out.println("Missing iris ::"+arrIr);
        //                
        //                String str = "AEIOU";
        //                List<Character> list = new ArrayList<Character>();
        //                
        //                for(int i =0; i < str.length(); i++){
        //                    char c = str.charAt(i);
        //                    list.add(c);
        //                    System.out.println(list);
        //                }
        //                
                        String ARCNO = (tokens[6]);

                        System.out.println(txtARCno.getText() + " " + ARCNO);
                        if(ARCNO.equals(txtARCno.getText())){
                            arcFound = true;
                            String ApplicationID = (tokens[10]);

                            String ARCStatus = (tokens[8]);

                            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();        
                            arcDetails = new ARCDetails();

                            //  setdata --pass to next controller for creating the object of ARC details class.
                            //  get the value to next controller for creating same object.
                            arcDetails.setName(tokens[11]);
                            arcDetails.setUnit(tokens[17]);
                            arcDetails.setRank(tokens[18]);
                            arcDetails.setFingers(arr);
                            arcDetails.setArcNo(ARCNO);
                            arcDetails.setApplicantID(ApplicationID);
                            arcDetails.setIris(arrIr);
                            arcDetails.setArcstatus(ARCStatus);
                            holder.setARC(arcDetails);
                            txtName.setText(arcDetails.getName());
                            txtRank.setText(arcDetails.getRank());
                            txtapp.setText(arcDetails.getApplicantID());
                            txtUnit.setText(arcDetails.getUnit());
                            txtFinger.setText(arcDetails.getFingers().toString());
                            txtiris.setText(arcDetails.getIris().toString());
                            txtDlink.setText(arcDetails.getDetailLink());
                            txtarcstatus.setText(arcDetails.getArcstatus());
                            txtarcno.setText(arcDetails.getArcNo());
                            SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
                            saveEnrollment.setArcNo(txtARCno.getText());
                            saveEnrollment.setEnrollmentStationID(apiServerCheck.getStationID());
                            saveEnrollment.setEnrollmentStationUnitID(apiServerCheck.getUnitID());
                            saveEnrollment.setFp(new HashSet<>());
                            saveEnrollment.setIris(new HashSet<>());
                            saveEnrollment.setEnrollmentStatus("ARC Details Fetched");
                            holder.setEnrollmentDetails(saveEnrollment);
                            LOGGER.log(Level.INFO, "ARCDetails added in Save Enrollment");
                        }
                        else {
                            continue;
                        }


                 //   App.setRoot("show_arcdetails");
                    }
                    if(!arcFound)
                    {
                        LOGGER.log(Level.INFO, "ARC No "+txtARCno.getText() + "not found");
                        lblStatus.setText("ARCNO not found. Please check the number and try again");
                        return;
                    }
                    else {
                        fingerprintcapture.setDisable(false);
                    }


                }
            }
         
        }
        } 
        catch(Exception e)
         {
             LOGGER.log(Level.SEVERE, "Exception while reading file / parsing data. Check if file is present. Or check if all data is available");
             e.printStackTrace();
         }
    }*/

    @FXML
    private void showARCDetails() throws IOException {
        try {
            // postJson = mapper.writeValueAsString(saveEnrollment);
            //postJson = postJson.replace("\n", "");
            //Development
            //File jsonFile = new File("/tmp/arclistimported1.json");
            //Production
            //File csvFile = new File("/etc/SampleDataToExportForBiometricCapture_v1.2.csv");
            importjson = prop.getProp().getProperty("importjsonfolder");
           /*
           curpesid=prop.getProp().getProperty("curpesid");
           String readpesunit="";
           
            readpesunit= FileUtils.readFileToString(new File(curpesid));
            if(readpesunit.trim().equals("")||readpesunit.trim().equals("null")||readpesunit.trim().isEmpty()){
                lblStatus.setText("Kindly Set the PES UnitId and Try Again");
                return;
            }
           System.out.println("Read UniT:"+readpesunit);*/
            //unitId = apiServerCheck.getUnitID();
            //stationId = apiServerCheck.getStationID();
            //importjsonFile = importjson+unitId+".json";
            //importjsonFile = importjson+readpesunit+".json";
            importjsonFile = prop.getProp().getProperty("importjsonfile");
            File jsonFile = new File(importjsonFile);
            if (jsonFile.exists()) {
                //System.out.println("reading json file as object");
                LOGGER.log(Level.INFO, "reading json file as object");
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    /*
                     //Deleting Import File   
                     ARCDetailsList arcDetailsList = null;
                     importjson=prop.getProp().getProperty("importjsonfolder");
                     String postJson = null;
                     String objimportFilePath = prop.getProp().getProperty("importjsonfile");
                        File dir = new File(importjson);
                        File[] dirlisting1 = dir.listFiles();
                            if (dirlisting1.length!=0) {
                               System.out.println("Inside Import directory listing");
                               for (File children : dirlisting1) {                            
                                   System.out.println("Child File:"+children.getName());
                                   arcDetailsList = mapper.readValue(children, ARCDetailsList.class);                              
                                   postJson = mapper.writeValueAsString(arcDetailsList);
                                   System.out.println("POSTJSON"+postJson);
                          }
                          FileUtils.writeStringToFile(new File(objimportFilePath), postJson); 
                       }
            
                       //Deleting Import File       
                    */

                    ARCDetailsList arcDetailsList = mapper.readValue(Paths.get(importjsonFile).toFile(), ARCDetailsList.class);
                    Boolean arcFound = false;
                    lblStatus.setText("");
                    if (arcDetailsList.getArcDetails().size() > 0) {
                        for (ARCDetails arcDetail : arcDetailsList.getArcDetails()) {
                            String arcNo = txtARCno.getText();
//                            System.out.println("ARC Biometric:::"+arcDetail.getBiometricoptions());
                            System.out.println("ARC Biometric:::" + arcDetail.toString());
                            //Development
                            String line = " ";
                            String input = " ";

                            if (txtARCno.getText().isEmpty()) {
                                lblStatus.setText("The ARC number is not entered. Enter ARC number and try again");
                                return;
                            } else {
                                //To check the ARC has been done Already
                                try {
                                    String exportjsonfile = "";
                                    exportjson = prop.getProp().getProperty("encexportfolder");
                                    //System.out.println("Export Json"+exportjson);
                                    LOGGER.log(Level.INFO, "Export Json:" + exportjson);
                                    File dire = new File(exportjson);
                                    File[] dirlisting = dire.listFiles();
                                    if (dirlisting.length != 0) {
                                        //System.out.println("Inside Export directory listing");
                                        LOGGER.log(Level.INFO, "Inside Export directory listing");
                                        for (File children : dirlisting) {
                                            //children.delete();
                                            //filename.substring(0, index);
                                            exportjsonfile = children.getName().substring(0, children.getName().lastIndexOf("."));
                                            //System.out.println("File Name:::::"+exportjsonfile.substring(0, exportjsonfile.lastIndexOf(".")));
                                            //System.out.println("File Name:::"+children.getName().substring(0, children.getName().lastIndexOf(".")));
                                            if (exportjsonfile.substring(0, exportjsonfile.lastIndexOf(".")).equals(txtARCno.getText())) {
                                                LOGGER.log(Level.INFO, "Entered ARCNO is Already Enrolled.Try Another ARC No");
                                                lblStatus.setText("The Biometric Already Provided Against this ARC No");
                                                fingerprintcapture.setDisable(true);
                                                return;
                                            }
                                        }
                                    } else {
                                        //System.out.println("No Export files");
                                        LOGGER.log(Level.INFO, "No Import files, Kindly Import Again");
                                        //messageStatus("No Import files, Kindly Import Again");
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "Exception:" + ex);
                                    lblStatus.setText("Exception:" + ex);
                                    //response = "Error While Deleting Import Files, Try Again";
                                    //messageStatus(response);
                                    //messageStatus("Exception:"+ex);
                                }


                                System.out.println("ARC No: " + arcDetail.getArcNo());
                                LOGGER.log(Level.INFO, "ARC No:" + arcDetail.getArcNo());
                                if (arcDetail.getArcNo().equals(txtARCno.getText())) {
                                    arcFound = true;
                                    String ApplicationID = arcDetail.getApplicantID();
                                    String ARCStatus = arcDetail.getArcStatus();

                                    ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                                    arcDetails = new ARCDetails();

                                    //  setdata --pass to next controller for creating the object of ARC details class.
                                    //  get the value to next controller for creating same object.
                                    arcDetails.setName(arcDetail.getName());
                                    arcDetails.setUnit(arcDetail.getUnit());
                                    arcDetails.setRank(arcDetail.getRank());
                                    arcDetails.setFingers(arcDetail.getFingers());
                                    arcDetails.setArcNo(arcDetail.getArcNo());
                                    arcDetails.setApplicantID(arcDetail.getApplicantID());
                                    arcDetails.setIris(arcDetail.getIris());
                                    arcDetails.setArcStatus(arcDetail.getArcStatus());
                                    //Added For Biometric Options
                                    System.out.println("ARC Name:" + arcDetail.getName());
                                    //Added For Biometric Options

                                    if (arcDetail.getBiometricOptions() == null || arcDetail.getBiometricOptions().isEmpty() || arcDetail.getBiometricOptions().contains("None") || arcDetail.getBiometricOptions().contains("none")) {
                                        lblStatus.setText("Biometric capturing not required for given ARC Number");
                                        return;
                                    }


                                    LOGGER.log(Level.INFO, "ARC BIOmetric Options::" + arcDetail.getBiometricOptions());
                                    System.out.println("Biometic Options:" + arcDetail.getBiometricOptions());
                                    arcDetails.setBiometricOptions(arcDetail.getBiometricOptions());

                                    holder.setArcDetails(arcDetails);

                                    txtName.setText(arcDetails.getName());
                                    txtRank.setText(arcDetails.getRank());
                                    txtapp.setText(arcDetails.getApplicantID());
                                    txtUnit.setText(arcDetails.getUnit());
                                    //String FingerListException = arcDetails.getFingers().toString();
                                    //String FingerListEx = FingerListException.replace("[]", "NA");
                                    //String FingerListex = FingerListException.replace("]", "");
                                    //System.out.println("Fingers List:::"+arcDetails.getFingers().toString());
                                    System.out.println("Fingers List:::" + arcDetails.getFingers().toString().substring(1, arcDetails.getFingers().toString().length() - 1));
                                    //txtFinger.setText(FingerListEx);
                                    if (arcDetails.getFingers().size() > 0) {
                                        txtFinger.setText(arcDetails.getFingers().toString().substring(1, arcDetails.getFingers().toString().length() - 1));
                                        System.out.println("greater than zero");

                                    } else {
                                        txtFinger.setText("NA");
                                        System.out.println("less than zero");
                                    }
                                    //String IrisListException = arcDetails.getIris().toString();
                                    //String IrisListEx = IrisListException.replace("[]", "NA");
                                    //txtiris.setText(IrisListEx);
                                    System.out.println("Iris List:::" + arcDetails.getIris().toString());
                                    if (arcDetails.getIris().size() > 0) {
                                        txtiris.setText(arcDetails.getIris().toString().substring(1, arcDetails.getIris().toString().length() - 1));
                                        System.out.println("greater than zero");

                                    } else {
                                        txtiris.setText("NA");
                                        System.out.println("less than zero");
                                    }
                                    //txtiris.setText(arcDetails.getIris().toString().substring(1,arcDetails.getIris().toString().length()-1));
                                    txtDlink.setText(arcDetails.getDetailLink());
                                    txtarcstatus.setText(arcDetails.getArcStatus());
                                    txtarcno.setText(arcDetails.getArcNo());
                                    txtbiometricoptions.setText(arcDetails.getBiometricOptions());
                                    SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
                                    //saveEnrollment.setArcNo(txtARCno.getText());
                                    saveEnrollment.setArcNo(arcDetails.getArcNo());
                                    //saveEnrollment.setEnrollmentStationID(apiServerCheck.getStationID());
                                    saveEnrollment.setEnrollmentStationID(stationId);
                                    //saveEnrollment.setEnrollmentStationUnitID(apiServerCheck.getUnitID());
                                    saveEnrollment.setEnrollmentStationUnitID(unitId);
                                    saveEnrollment.setFp(new HashSet<>());
                                    saveEnrollment.setIris(new HashSet<>());
                                    //Added For Biometric Options
                                    saveEnrollment.setBiometricOptions(arcDetails.getBiometricOptions());
                                    saveEnrollment.setEnrollmentStatus("ARC Details Fetched");
                                    holder.setSaveEnrollmentDetails(saveEnrollment);
                                    LOGGER.log(Level.INFO, "ARCDetails added in Save Enrollment");
                                } else {
                                    continue;
                                }
                            }
                        }
                        if (!arcFound) {
                            LOGGER.log(Level.INFO, "ARC No " + txtARCno.getText() + "not found");
                            txtName.setText("");
                            txtRank.setText("");
                            txtapp.setText("");
                            txtUnit.setText("");
                            txtFinger.setText("");
                            txtiris.setText("");
                            txtDlink.setText("");
                            txtarcstatus.setText("");
                            txtarcno.setText("");
                            lblStatus.setText("ARCNO not found. Please check the number and try again");
                            LOGGER.log(Level.INFO, "ARCNO not found. Please check the number and try again");
                            return;
                        } else {
                            fingerprintcapture.setDisable(false);
                        }
                    }
                    //   App.setRoot("show_arcdetails");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception while reading file / parsing data. Check if file is present. Or check if all data is available");
                    lblStatus.setText("Exception while reading file / parsing data, Try Import Again.");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Json File Not Found");
                lblStatus.setText("Import file not Found, Try Import Again.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception while reading file / parsing data. Check if file is present. Or check if all data is available");
            lblStatus.setText("Exception while reading file / parsing data, Try Import Again.");
            e.printStackTrace();
        }
    }
           
    
    
    /*
     @FXML
    private void showARCDetails() throws IOException{ 
         
        try{  
      
        String arcNo = txtARCno.getText(); //need to uncomment
            System.out.println("ARCNO:::"+arcNo);
        if(arcNo == null || arcNo.equals("") || arcNo.trim().equals("")){
            System.out.println("String is null, empty or blank");
            lblStatus.setText("Kindly Enter ARC Number Details");
        }
        else{  
        lblStatus.setText("");
        String connurl = apiServerCheck.getARCURL();
        String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcNo);
        System.out.println("connection status :"+connectionStatus);
        if(!connectionStatus.contentEquals("connected")) {
            lblStatus.setText(connectionStatus); 
            
        }
        else {
            
          try{  
          String ArcDetailsList = "";
          ArcDetailsList = apiServerCheck.getARCNoAPI(connurl, arcNo);  
                   
          //Object obj = JsonReader.jsonToJava(ArcDetailsList);
          //System.out.println("obj str : " +obj.toString());        
           System.out.println("ARCdetail"+ArcDetailsList);
          ObjectMapper objMapper = new ObjectMapper();
          arcDetails = objMapper.readValue(ArcDetailsList, ARCDetails.class);
          System.out.println(arcDetails.toString());   
           System.out.println("ARC Details Desc:"+arcDetails.getDesc());               
           if (arcDetails.getErrorCode().equals("0")){
               lblStatus.setText("ARC Details Fetched Successfully");               
           }else{
               lblStatus.setText(arcDetails.getDesc());  
           }
          
            if(Integer.parseInt(arcDetails.getErrorCode())== 0) {            
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
            holder.setARC(arcDetails);
            txtName.setText(arcDetails.getName());
            txtRank.setText(arcDetails.getRank());
            txtapp.setText(arcDetails.getApplicantID());
            txtUnit.setText(arcDetails.getUnit());
            txtFinger.setText(arcDetails.getFingers().toString());
            txtiris.setText(arcDetails.getIris().toString());
            txtDlink.setText(arcDetails.getDetailLink());
            txtarcstatus.setText(arcDetails.getArcstatus());
            txtarcno.setText(arcDetails.getArcNo());
            SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
            saveEnrollment.setArcNo(txtARCno.getText());
            saveEnrollment.setEnrollmentStationID(apiServerCheck.getStationID());
            saveEnrollment.setEnrollmentStationUnitID(apiServerCheck.getUnitID());
            saveEnrollment.setFp(new HashSet<>());
            saveEnrollment.setIris(new HashSet<>());
            saveEnrollment.setEnrollmentStatus("ARC Details Fetched");
            holder.setEnrollmentDetails(saveEnrollment);
            fingerprintcapture.setDisable(false);
           // show_arcdetails_next1.setDisable(false);
         //   App.setRoot("show_arcdetails");
           }
        else {
            //lblStatus.setText("Error in retriving details. Please try again");
             //emtyTextBox();
             //show_arcdetails_next1.setDisable(true);
             lblStatus.setText(arcDetails.getDesc()); 
            }
            
          }catch (JsonProcessingException ex) {
                         
                    lblStatus.setText("Invalid Data Received From the Server"); 
                      }
    
         }
            
       }
     }              
        catch(Exception e)
         {
             System.out.println(e);
         }
    }
    */

    @FXML
    public void barcodescan() {
        try {

            txtARCBarcode.requestFocus();

            ChangeListener<Scene> sceneListener = new ChangeListener<Scene>() {
                @Override
                public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                    if (newValue != null) {

                        txtARCBarcode.requestFocus();
                        txtARCBarcode.sceneProperty().removeListener(this);
                    }
                }
            };
            txtARCBarcode.sceneProperty().addListener(sceneListener);

            txtARCBarcode.setOnTouchPressed(new EventHandler<TouchEvent>() {
                @Override
                public void handle(TouchEvent event) {

                    event.consume();
                }
            });
        
        /*
        txtARCBarcode.textProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
            String oldValue, String newValue) {
                LOGGER.log(Level.INFO, "BARCODE Listener111");
                txtARCBarcode.setDisable(true);
                showDetailsBarcode();
       
        }
        });    */

            LOGGER.log(Level.INFO, "BARCODE Listener");
            txtARCBarcode.setDisable(true);
            showDetailsBarcode();
        } catch (NullPointerException e) {
            //System.out.println("Null pointer Exception at barcodescan");
            LOGGER.log(Level.INFO, "Null pointer Exception at barcodescan");

        }


    }

    public void showDetailsBarcode() {
        try {
            // postJson = mapper.writeValueAsString(saveEnrollment);
            //postJson = postJson.replace("\n", "");
            //Development
            //File jsonFile = new File("/tmp/arclistimported1.json");
            //Production
            //File csvFile = new File("/etc/SampleDataToExportForBiometricCapture_v1.2.csv");
            importjson = prop.getProp().getProperty("importjsonfolder");
           /*
           curpesid=prop.getProp().getProperty("curpesid");
           String readpesunit="";
           
            readpesunit= FileUtils.readFileToString(new File(curpesid));
            if(readpesunit.trim().equals("")||readpesunit.trim().equals("null")||readpesunit.trim().isEmpty()){
                lblStatus.setText("Kindly Set the PES UnitId and Try Again");
                return;
            }
           System.out.println("Read UniT:"+readpesunit);*/
            //unitId = apiServerCheck.getUnitID();
            //stationId = apiServerCheck.getStationID();
            //importjsonFile = importjson+unitId+".json";
            //importjsonFile = importjson+readpesunit+".json";
            importjsonFile = prop.getProp().getProperty("importjsonfile");
            File jsonFile = new File(importjsonFile);
            if (jsonFile.exists()) {
                //System.out.println("reading json file as object");
                LOGGER.log(Level.INFO, "reading json file as object");
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    /*
                     //Deleting Import File   
                     ARCDetailsList arcDetailsList = null;
                     importjson=prop.getProp().getProperty("importjsonfolder");
                     String postJson = null;
                     String objimportFilePath = prop.getProp().getProperty("importjsonfile");
                        File dir = new File(importjson);
                        File[] dirlisting1 = dir.listFiles();
                            if (dirlisting1.length!=0) {
                               System.out.println("Inside Import directory listing");
                               for (File children : dirlisting1) {                            
                                   System.out.println("Child File:"+children.getName());
                                   arcDetailsList = mapper.readValue(children, ARCDetailsList.class);                              
                                   postJson = mapper.writeValueAsString(arcDetailsList);
                                   System.out.println("POSTJSON"+postJson);
                          }
                          FileUtils.writeStringToFile(new File(objimportFilePath), postJson); 
                       }
            
                       //Deleting Import File       
                    */

                    ARCDetailsList arcDetailsList = mapper.readValue(Paths.get(importjsonFile).toFile(), ARCDetailsList.class);
                    Boolean arcFound = false;
                    lblStatus.setText("");
                    if (arcDetailsList.getArcDetails().size() > 0) {
                        for (ARCDetails arcDetail : arcDetailsList.getArcDetails()) {
                            String arcNo = txtARCBarcode.getText();
//                            System.out.println("ARC Biometric:::"+arcDetail.getBiometricoptions());
                            System.out.println("ARC Biometric:::" + arcDetail.toString());
                            //Development
                            String line = " ";
                            String input = " ";

                            if (txtARCBarcode.getText().isEmpty()) {
                                lblStatus.setText("The ARC number is not entered. Enter ARC number and try again");
                                return;
                            } else {
                                //To check the ARC has been done Already
                                try {
                                    String exportjsonfile = "";
                                    exportjson = prop.getProp().getProperty("encexportfolder");
                                    //System.out.println("Export Json"+exportjson);
                                    LOGGER.log(Level.INFO, "Export Json:" + exportjson);
                                    File dire = new File(exportjson);
                                    File[] dirlisting = dire.listFiles();
                                    if (dirlisting.length != 0) {
                                        //System.out.println("Inside Export directory listing");
                                        LOGGER.log(Level.INFO, "Inside Export directory listing");
                                        for (File children : dirlisting) {
                                            //children.delete();
                                            //filename.substring(0, index);
                                            exportjsonfile = children.getName().substring(0, children.getName().lastIndexOf("."));
                                            //System.out.println("File Name:::::"+exportjsonfile.substring(0, exportjsonfile.lastIndexOf(".")));
                                            //System.out.println("File Name:::"+children.getName().substring(0, children.getName().lastIndexOf(".")));
                                            if (exportjsonfile.substring(0, exportjsonfile.lastIndexOf(".")).equals(txtARCBarcode.getText())) {
                                                LOGGER.log(Level.INFO, "Entered ARC No is Already Enrolled.Try Another ARC No");
                                                lblStatus.setText("The Biometric Already Provided Against this ARC No");
                                                fingerprintcapture.setDisable(true);
                                                return;
                                            }
                                        }
                                    } else {
                                        //System.out.println("No Export files");
                                        LOGGER.log(Level.INFO, "No Import files, Kindly Import Again");
                                        //messageStatus("No Import files, Kindly Import Again");
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                                    LOGGER.log(Level.INFO, "Exception:" + ex);
                                    lblStatus.setText("Exception:" + ex);
                                    //response = "Error While Deleting Import Files, Try Again";
                                    //messageStatus(response);
                                    //messageStatus("Exception:"+ex);
                                }


                                System.out.println("ARC No: " + arcDetail.getArcNo());
                                LOGGER.log(Level.INFO, "ARC No:" + arcDetail.getArcNo());
                                if (arcDetail.getArcNo().equals(txtARCBarcode.getText())) {
                                    arcFound = true;
                                    String ApplicationID = arcDetail.getApplicantID();
                                    String ARCStatus = arcDetail.getArcStatus();

                                    ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                                    arcDetails = new ARCDetails();

                                    //  setdata --pass to next controller for creating the object of ARC details class.
                                    //  get the value to next controller for creating same object.
                                    arcDetails.setName(arcDetail.getName());
                                    arcDetails.setUnit(arcDetail.getUnit());
                                    arcDetails.setRank(arcDetail.getRank());
                                    arcDetails.setFingers(arcDetail.getFingers());
                                    arcDetails.setArcNo(arcDetail.getArcNo());
                                    arcDetails.setApplicantID(arcDetail.getApplicantID());
                                    arcDetails.setIris(arcDetail.getIris());
                                    arcDetails.setArcStatus(arcDetail.getArcStatus());
                                    //Added For Biometric Options
                                    System.out.println("ARC Name:" + arcDetail.getName());
                                    //Added For Biometric Options

                                    if (arcDetail.getBiometricOptions() == null || arcDetail.getBiometricOptions().isEmpty() || arcDetail.getBiometricOptions().contains("None") || arcDetail.getBiometricOptions().contains("none")) {
                                        lblStatus.setText("Biometric capturing not required for given ARC Number");
                                        return;
                                    }
                                    LOGGER.log(Level.INFO, "ARC BIOmetric Options::" + arcDetail.getBiometricOptions());
                                    System.out.println("Biometic Options:" + arcDetail.getBiometricOptions());
                                    arcDetails.setBiometricOptions(arcDetail.getBiometricOptions());

                                    holder.setArcDetails(arcDetails);

                                    txtName.setText(arcDetails.getName());
                                    txtRank.setText(arcDetails.getRank());
                                    txtapp.setText(arcDetails.getApplicantID());
                                    txtUnit.setText(arcDetails.getUnit());
                                    //String FingerListException = arcDetails.getFingers().toString();
                                    //String FingerListEx = FingerListException.replace("[]", "NA");
                                    //String FingerListex = FingerListException.replace("]", "");
                                    //System.out.println("Fingers List:::"+arcDetails.getFingers().toString());
                                    System.out.println("Fingers List:::" + arcDetails.getFingers().toString().substring(1, arcDetails.getFingers().toString().length() - 1));
                                    //txtFinger.setText(FingerListEx);
                                    if (arcDetails.getFingers().size() > 0) {
                                        txtFinger.setText(arcDetails.getFingers().toString().substring(1, arcDetails.getFingers().toString().length() - 1));
                                        System.out.println("greater than zero");

                                    } else {
                                        txtFinger.setText("NA");
                                        System.out.println("less than zero");
                                    }
                                    //String IrisListException = arcDetails.getIris().toString();
                                    //String IrisListEx = IrisListException.replace("[]", "NA");
                                    //txtiris.setText(IrisListEx);
                                    System.out.println("Iris List:::" + arcDetails.getIris().toString());
                                    if (arcDetails.getIris().size() > 0) {
                                        txtiris.setText(arcDetails.getIris().toString().substring(1, arcDetails.getIris().toString().length() - 1));
                                        System.out.println("greater than zero");

                                    } else {
                                        txtiris.setText("NA");
                                        System.out.println("less than zero");
                                    }
                                    //txtiris.setText(arcDetails.getIris().toString().substring(1,arcDetails.getIris().toString().length()-1));
                                    txtDlink.setText(arcDetails.getDetailLink());
                                    txtarcstatus.setText(arcDetails.getArcStatus());
                                    txtarcno.setText(arcDetails.getArcNo());
                                    txtbiometricoptions.setText(arcDetails.getBiometricOptions());
                                    SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
                                    //saveEnrollment.setArcNo(txtARCno.getText());
                                    saveEnrollment.setArcNo(arcDetails.getArcNo());
                                    //saveEnrollment.setEnrollmentStationID(apiServerCheck.getStationID());
                                    saveEnrollment.setEnrollmentStationID(stationId);
                                    //saveEnrollment.setEnrollmentStationUnitID(apiServerCheck.getUnitID());
                                    saveEnrollment.setEnrollmentStationUnitID(unitId);
                                    saveEnrollment.setFp(new HashSet<>());
                                    saveEnrollment.setIris(new HashSet<>());
                                    //Added For Biometric Options
                                    saveEnrollment.setBiometricOptions(arcDetails.getBiometricOptions());
                                    saveEnrollment.setEnrollmentStatus("ARC Details Fetched");
                                    holder.setSaveEnrollmentDetails(saveEnrollment);
                                    LOGGER.log(Level.INFO, "ARCDetails added in Save Enrollment");
                                    fingerprintcapture.setDisable(false);
                                    //emptyBarcode();
                                    //txtARCBarcode.setDisable(false);
                                    barcodearcbutton.requestFocus();

                                } else {
                                    continue;
                                }
                            }
                        }
                        if (!arcFound) {
                            fingerprintcapture.setDisable(false);
                            LOGGER.log(Level.INFO, "ARC No " + txtARCno.getText() + "not found");
                            txtName.setText("");
                            txtRank.setText("");
                            txtapp.setText("");
                            txtUnit.setText("");
                            txtFinger.setText("");
                            txtiris.setText("");
                            txtDlink.setText("");
                            txtarcstatus.setText("");
                            txtarcno.setText("");
                            lblStatus.setText("ARCNO not found. Please check the number and try again");
                            LOGGER.log(Level.INFO, "ARCNO not found. Please check the number and try again");
                            return;
                        } else {
                            fingerprintcapture.setDisable(false);
                        }
                    }
                    //   App.setRoot("show_arcdetails");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception while reading file / parsing data. Check if file is present. Or check if all data is available");
                    lblStatus.setText("Exception while reading file / parsing data, Try Import Again.");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Json File Not Found");
                lblStatus.setText("Import file not Found, Try Import Again.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception while reading file / parsing data. Check if file is present. Or check if all data is available");
            lblStatus.setText("Exception while reading file / parsing data, Try Import Again.");
            e.printStackTrace();
        }
    }
    
    
/*
public void showDetailsBarcode(){
        
    try{
        
            // postJson = mapper.writeValueAsString(saveEnrollment);
            //postJson = postJson.replace("\n", "");
        File csvFile = new File("/etc/SampleDataToExportForBiometricCapture_v1.2.csv");
        if(csvFile.exists()) {    
            
            String arcNo = txtARCBarcode.getText();
            try (BufferedReader file = new BufferedReader(new FileReader("/etc/SampleDataToExportForBiometricCapture_v1.2.csv"))) {
            String line = " ";
            String input = " ";
           
            if(txtARCBarcode.getText().isEmpty()){
                lblStatus.setText("Please input ARCNo/scan barcode and try again");
                return;
            }
            else{
                while((line = file.readLine()) != null){
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    String Name = (tokens[11]);
                    String Unit = (tokens[17]);
                    String Rank = (tokens[18]);
                    String[] Fingersleft = (tokens[52]).split(",");
                    String[] Fingersright = (tokens[53]).replaceAll("^\"|\"$", "").split(",");
                    String[] arrIris = (tokens[54].split(","));
                    List<String> arr = new ArrayList<>();
                    Collections.addAll(arr, Fingersleft);
                    Collections.addAll(arr, Fingersright);
                    System.out.println(arr);

                    List<String> arrIr = new ArrayList<>();
                    Collections.addAll(arrIr, arrIris);
                    System.out.println("Missing iris ::"+arrIr);
    //              
    //                String str = "AEIOU";
    //                List<Character> list = new ArrayList<Character>();
    //                
    //                for(int i =0; i < str.length(); i++){
    //                    char c = str.charAt(i);
    //                    list.add(c);
    //                    System.out.println(list);
    //                }
    //                
                    

                    String ARCNO = (tokens[6]);
                    String ApplicationID = (tokens[10]);

                    String ARCStatus = (tokens[8]);

                    ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();        
                    arcDetails = new ARCDetails();

                    //  setdata --pass to next controller for creating the object of ARC details class.
                    //  get the value to next controller for creating same object.
                    arcDetails.setName(tokens[11]);
                    arcDetails.setUnit(tokens[17]);
                    arcDetails.setRank(tokens[18]);
                    arcDetails.setFingers(arr);
                    arcDetails.setArcNo(ARCNO);
                    arcDetails.setApplicantID(ApplicationID);
                    arcDetails.setIris(arrIr);
                    arcDetails.setArcstatus(ARCStatus);
                    holder.setARC(arcDetails);
                    txtName.setText(arcDetails.getName());
                    txtRank.setText(arcDetails.getRank());
                    txtapp.setText(arcDetails.getApplicantID());
                    txtUnit.setText(arcDetails.getUnit());
                    txtFinger.setText(arcDetails.getFingers().toString());
                    txtiris.setText(arcDetails.getIris().toString());
                    txtDlink.setText(arcDetails.getDetailLink());
                    txtarcstatus.setText(arcDetails.getArcstatus());
                    txtarcno.setText(arcDetails.getArcNo());
                    SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
                    saveEnrollment.setArcNo(txtARCBarcode.getText());
                    saveEnrollment.setEnrollmentStationID(apiServerCheck.getStationID());
                    saveEnrollment.setEnrollmentStationUnitID(apiServerCheck.getUnitID());
                    saveEnrollment.setFp(new HashSet<>());
                    saveEnrollment.setIris(new HashSet<>());
                    saveEnrollment.setEnrollmentStatus("ARC Details Fetched");
                    holder.setEnrollmentDetails(saveEnrollment);
                    LOGGER.log(Level.INFO, "ARCDetails added in Save Enrollment");
                    //   App.setRoot("show_arcdetails");
                }
       
        
            }
        }     
    }
    } 
    catch(Exception e)
    {
        LOGGER.log(Level.SEVERE, "Exception while reading file / parsing data. Check if file is present. Or check if all data is available");

        System.out.println(e);
    }
          
         //   App.setRoot("show_arcdetails");
}*/


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //fingerprintcapture.setDisable(true);
        //LOGGER.addHandler(handler);
    }
}
