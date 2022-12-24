/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.model.ARCDetailsList;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class ImportExportController1 {
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
    private String importjson=null;
    
    
    private String export=null;
    
    public SaveEnrollmentResponse enrollmentResponse;
    //private String export="/usr/share/enrollment/json/export";
    
    /*
    @FXML
    private Button FetchUnitButton;
    
    @FXML
    private ComboBox<Units> comboUnitId = new ComboBox<>();
    */
   
    
        
    public void messageStatus(String message){
        lblStatus.setText(message);
    }



    @FXML
    public void importData() {
        
       String response = importjsonFile();
       messageStatus(response);      
    }
    /*
      @FXML
    private void fetchunitdetails() {
        
       String response = fetchUnit();
       messageStatus(response);     
           
        
       
    }*/
  
  /*  
    public String fetchUnit(){

        String response="";
        String json = "";
        try{
        String connurl = apiServerCheck.getUnitListURL();        
        String connectionStatus = apiServerCheck.getStatusUnitListAPI(connurl);
        System.out.println("connection status :"+connectionStatus);
        
        if(!connectionStatus.contentEquals("connected")) {
            //labelstatus.setText(connectionStatus);
            //labelstatus.setText("Enter the Correct URL in Mafis API");
           // Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Enter the Correct URL in Mafis API";
                    return response;
        }
        else {
            try {
            json = apiServerCheck.getUnitListAPI(connurl);
            System.out.println("Output str : "+json);
            ObjectMapper objectmapper = new ObjectMapper();
            UnitListDetails details = objectmapper.readValue(json, UnitListDetails.class);  
            
          
          
             try{
                ObservableList<Units> units = FXCollections.observableArrayList(details.getUnits());
                comboUnitId.setItems(units);

                System.out.println("combo value : "+comboUnitId.getSelectionModel().getSelectedItem());
                comboUnitId.valueProperty().addListener((obs, oldval, newval) -> {
                if(newval != null)
                System.out.println("Selected unit: " + newval.getCaption()
                    + ". ID: " + newval.getValue());
                  });

                } catch (Exception e) {
                e.printStackTrace();
                System.out.println("error :"+e.getMessage());                
                response = "Kindly enter/select all the Values";
                return response;
                } 
            }
            catch (NullPointerException e)
            {
                System.out.print("NullPointerException caught");
                
                response = "Unit Id List is not Available in the Server";
                return response;
            }  catch (JsonProcessingException ex) {
                    Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);                     
                    response = "Unit Id List is not Available in the Server";
                    return response;
                }      
        
      }
        }catch(NullPointerException e){
                System.out.print("NullPointerException caught");
                response ="Unit Id List is not Available in the Server";
                return response;
        }
        return response;
    }
    
    */
    
    public String importjsonFile(){
        
             
        
                String response="";
                /*
                Units unit =null;
                try{
                        unit = comboUnitId.getSelectionModel().getSelectedItem();
                        if(unit == null) {
                           response = "Kindly enter/select all the Values from  UnitId";
                           return response;         
                        } 
        
                   }catch (Exception e)
                     {
                      System.out.println("Exception::"+e);                     
                      
                     }*/
                
                 try {
                    importjson=prop.getProp().getProperty("importjsonfile");
                    //  importjson=prop.getProp().getProperty("importjsonfolder");
                } catch (IOException ex) {
                    Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Json File Not Exist in File.properties";
                    return response;
                }
                try {
                    export=prop.getProp().getProperty("exportfolder");
                } catch (IOException ex) {
                    Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);
                    response = "Json File Not Exist in File.properties";
                    return response;            
                }
                System.out.println("import data");
          
                 //Checking Connection Status
                 String connurl = apiServerCheck.getARCURL();
                 String arcno = "123abc";
                 String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl,arcno);   
                 System.out.println("connection status :"+connectionStatus);
                 if(!connectionStatus.contentEquals("connected")) {
                     //lblStatus.setText("System not connected to network. Connect and try again");
                     response = "System not connected to network. Connect and try again";
                     return response;                       
                 }
          
         
            try {
                
                connurl = apiServerCheck.getDemographicURL();
                //String unitid=unit.getValue();
                String unitid="";
                //System.out.println("UnitID::::"+unitid);
                connectionStatus = apiServerCheck.getDemoGraphicDetailsAPI(connurl, unitid);
                
                System.out.println("Output :::::::"+connectionStatus);
                if(connectionStatus.contains("Exception")){
                    //lblStatus.setText(connectionStatus);
                     response = connectionStatus;
                     return response;                      
                }
               
                //Production
                FileWriter file = new FileWriter(importjson);
                //Development
                //FileWriter file = new FileWriter("/tmp/arclistimported.json");
                System.out.println("conn::"+connectionStatus);
                ObjectMapper objMapper = new ObjectMapper();
                try{
                objMapper.readValue(connectionStatus, ARCDetailsList.class);
                System.out.println("response compatible with ARCDetailsList POJO");
                } catch (JsonParseException | JsonMappingException e) {
                System.out.println("response NOT compatible with ARCDetailsList POJO");
                response = "The Response From Server is not Compatible";
                return response;                   
                }                
                arcDetailsListResponse = objMapper.readValue(connectionStatus, ARCDetailsList.class);
                System.out.println("Import Error Code :::"+arcDetailsListResponse.getErrorCode());               
                       
                if (arcDetailsListResponse.getErrorCode()==0){                   
                   System.out.println("Import Json: "+arcDetailsListResponse.toString());
                        try {
                        file.write(connectionStatus);                      
                        System.out.println("Successfully Copied JSON Object to File..."+arcDetailsListResponse);                      
                        response = "Successfully Imported";
                        return response;                           
                            } 
                        catch (IOException e) {
                             e.printStackTrace();                             
                              response = "Error While Importing the File";
                              return response;  
                           } 
                        finally {
                                try {
                                    file.flush();
                                    file.close();
                                } 
                        catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();                                
                                response = "Error While Flushing the File";
                                return response;  
                                }
                            }

                    }else{                        
                         response = arcDetailsListResponse.getDesc();
                         return response;                          
                    }
               
               
            } catch (Exception e){
                System.out.println("Exception"+e);                
                response = "Details not Imported From Server. Due to Network Issue";
                return response; 
            }
    
    }

    @FXML
    private void exportData() {
        
        String reponse = exportjsonFile();
        messageStatus(reponse);      
        
        /*
        SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
        String postJson = "";
        String connurl = apiServerCheck.getARCURL();
        String arcno = "123abc";      
        String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl,arcno);   
        System.out.println("connection status :"+connectionStatus);
        if(!connectionStatus.contentEquals("connected")) {
            //lblStatus.setText("System not connected to network. Connect and try again");
            messageStatus("System not connected to network. Connect and try again"); 
            return;
        }
        
        try {
            export=prop.getProp().getProperty("exportfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
            messageStatus("Export Folder Not Found on the System"); 
            //lblStatus.setText("Export Folder Not Found on the System");
        }
        //For Production
        File dir = new File(export+"/enc");
        System.out.println(export+"/enc");             
        File[] directoryListing = dir.listFiles();
        System.out.println("file count"+directoryListing.length);
        if (directoryListing.length!=0) {
          System.out.println("Inside directory listing");
          for (File child : directoryListing) {
                System.out.println("Inside directory listing - for loop");
                //Code for decryption and process
                AESFileEncryptionDecryption aesDecryptFile = new AESFileEncryptionDecryption();
                  try {
                      aesDecryptFile.decryptFile(child.getAbsolutePath(),export+"/dec/"+child.getName());

                  } catch (IOException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (NoSuchAlgorithmException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (InvalidKeySpecException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (NoSuchPaddingException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (InvalidKeyException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (InvalidAlgorithmParameterException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (IllegalBlockSizeException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  } catch (BadPaddingException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("Decryption Problem, Try Again"); 
                  }

                //System.out.println("file name:"+child.getName());
                FileReader file=null;
                    try {                        
                        file = new FileReader(export+"/dec/"+child.getName());
                        //file = new FileReader(export+"/dec/"+"AB.json.enc");
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT);
                        mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
                        saveEnrollment = mapper.readValue(Paths.get(export+"/dec/"+child.getName()).toFile(), SaveEnrollmentDetails.class);
                        postJson = mapper.writeValueAsString(saveEnrollment);
                    } catch (FileNotFoundException ex) {
                      Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                      messageStatus("File not Found, Try Again"); 
                      } catch (IOException ex) {
                          Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
                          messageStatus("File not Found, Try Again"); 
                      }
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);
        
                try {
                    ObjectMapper objMapper = new ObjectMapper();
                    ObjectMapper objMappersave = new ObjectMapper();
                    String decResponse = "";
                    connurl = apiServerCheck.getEnrollmentSaveURL();           
                    decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
                    System.out.println("dec response : "+ decResponse);           
                    saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), saveEnrollmentResponse.class);
                    System.out.println(" save enrollment : "+saveEnrollmentResponse.toString());          
                    enrollmentResponse = objMappersave.readValue(decResponse.toString(), saveEnrollmentResponse.class);
                    System.out.println(" save enrollment : "+enrollmentResponse.toString());
                    if(enrollmentResponse.getErrorCode().equals("0")) {
                        child.delete();
                        aesDecryptFile.delFile(export+"/dec/"+child.getName());
                        aesDecryptFile.delFile(export+"/enc/"+child.getName());
                        messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 
                        //lblStatus.setText(child.getName()+" - "+enrollmentResponse.getDesc());
                        System.out.println(child.getName()+" - "+enrollmentResponse.getDesc());
                        } else {
                            //lblStatus.setText(child.getName()+" - "+enrollmentResponse.getDesc());
                            messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 
                            System.out.println(child.getName()+" - "+enrollmentResponse.getDesc());
                        }

                } 
                catch(Exception e)
                {
                    System.out.println("Exception in Export"+e);
                    messageStatus("Exception in Export"+e);
                }
        }
        } else {
            //lblStatus.setText("No Biometric Data to Export");
            messageStatus("No Biometric Data to Export");
            System.out.println("The Directory is empty.. No encrypted files");
        }*/
    }
    
    public String exportjsonFile(){
        String response="";
        SaveEnrollmentDetails saveEnrollment = new SaveEnrollmentDetails();
        String postJson = "";
        String connurl = apiServerCheck.getARCURL();
        String arcno = "123abc";      
        String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl,arcno);   
        System.out.println("connection status :"+connectionStatus);
        if(!connectionStatus.contentEquals("connected")) {
            //lblStatus.setText("System not connected to network. Connect and try again");
            //messageStatus("System not connected to network. Connect and try again"); 
            //return;
            response = "System not connected to network. Connect and try again";
            return response;     
            
        }
        
        try {
            export=prop.getProp().getProperty("exportfolder");
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);
            //messageStatus("Export Folder Not Found on the System"); 
            response = "Export Folder Not Found on the System";
            return response; 
            //lblStatus.setText("Export Folder Not Found on the System");
        }
        //For Production
        File dir = new File(export+"/enc");
        System.out.println(export+"/enc");             
        File[] directoryListing = dir.listFiles();
        System.out.println("file count"+directoryListing.length);
        if (directoryListing.length!=0) {
          System.out.println("Inside directory listing");
          for (File child : directoryListing) {
                System.out.println("Inside directory listing - for loop");
                //Code for decryption and process
                AESFileEncryptionDecryption aesDecryptFile = new AESFileEncryptionDecryption();
                  try {
                      aesDecryptFile.decryptFile(child.getAbsolutePath(),export+"/dec/"+child.getName());

                  } catch (IOException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (NoSuchAlgorithmException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (InvalidKeySpecException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (NoSuchPaddingException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (InvalidKeyException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (InvalidAlgorithmParameterException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (IllegalBlockSizeException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  } catch (BadPaddingException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);                      
                      response = "Decryption Problem, Try Again";
                      return response; 
                  }

                //System.out.println("file name:"+child.getName());
                FileReader file=null;
                    try {                        
                        file = new FileReader(export+"/dec/"+child.getName());
                        //file = new FileReader(export+"/dec/"+"AB.json.enc");
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT);
                        mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
                        saveEnrollment = mapper.readValue(Paths.get(export+"/dec/"+child.getName()).toFile(), SaveEnrollmentDetails.class);
                        postJson = mapper.writeValueAsString(saveEnrollment);
                    } catch (FileNotFoundException ex) {
                      Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);
                      //messageStatus("File not Found, Try Again");
                      response = "File not Found, Try Again";
                      return response;
                      } catch (IOException ex) {
                          Logger.getLogger(ImportExportController1.class.getName()).log(Level.SEVERE, null, ex);
                          //messageStatus("File not Found, Try Again"); 
                          response = "File not Found, Try Again";
                          return response;
                      }
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);
                //BufferedReader reader = new BufferedReader(file);        
                //System.out.println("POSTJSON:::"+postJson);
        
                try {
                    ObjectMapper objMapper = new ObjectMapper();
                    ObjectMapper objMappersave = new ObjectMapper();
                    String decResponse = "";
                    connurl = apiServerCheck.getEnrollmentSaveURL();           
                    decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
                    System.out.println("dec response : "+ decResponse);           
                    saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                    System.out.println(" save enrollment : "+saveEnrollmentResponse.toString());          
                    enrollmentResponse = objMappersave.readValue(decResponse.toString(), SaveEnrollmentResponse.class);
                    System.out.println(" save enrollment : "+enrollmentResponse.toString());
                    if(enrollmentResponse.getErrorCode().equals("0")) {
                        child.delete();
                        aesDecryptFile.delFile(export+"/dec/"+child.getName());
                        aesDecryptFile.delFile(export+"/enc/"+child.getName());
                        //messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 
                        System.out.println(child.getName()+" - "+enrollmentResponse.getDesc());
                        //Uncomment Afterwards
                        //response = child.getName()+" - "+enrollmentResponse.getDesc();
                        response=enrollmentResponse.getDesc();
                        return response;                       
                        
                        } else {
                            //lblStatus.setText(child.getName()+" - "+enrollmentResponse.getDesc());
                            //messageStatus(child.getName()+" - "+enrollmentResponse.getDesc()); 
                            System.out.println(child.getName()+" - "+enrollmentResponse.getDesc());
                            response = child.getName()+" - "+enrollmentResponse.getDesc();
                            return response; 
                        }

                } 
                catch(Exception e)
                {
                    System.out.println("Exception in Export"+e);
                    //messageStatus("Exception in Export"+e);
                    response = "Exception in Export"+e;
                    return response; 
                }
        }
        } else {
            //lblStatus.setText("No Biometric Data to Export");
            //messageStatus("No Biometric Data to Export");
            System.out.println("The Directory is empty.. No encrypted files");
            response = "No Biometric Data to Export";
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
    
}
