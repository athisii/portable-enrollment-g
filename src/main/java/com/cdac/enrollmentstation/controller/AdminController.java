/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class AdminController implements Initializable {

    @FXML
    public Label statusMsg;

    @FXML
    private AnchorPane confirmPane;

    @FXML
    private Button confirmYesBtn;

    @FXML
    private Button confirmNoBtn;

    @FXML
    private Label confirmpanelabel;

    @FXML
    private ComboBox combocamera;

    @FXML
    private Button camerabtn;


    //For Application Log
    ApplicationLog appLog = new ApplicationLog();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    Handler handler;

    String filepath = "/etc/file.properties";

    public AdminController() {
        //this.handler = appLog.getLogger();
        //LOGGER.addHandler(handler); 
    }


    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        String cameralist[] = {"Internal", "External"};
        ObservableList oblist = FXCollections.observableArrayList(cameralist);
        combocamera.setItems(oblist);
        selectedCombo();
        //combocamera.getSelectionModel().select(0);

    }

    @FXML
    public void serverconfig() {
        try {
            App.setRoot("server_config");
        } catch (IOException ex) {
            Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException:");
        }
    }

    @FXML
    public void licenseInfo() {
        System.out.println("License Info button clicked");
        try {
            App.setRoot("license_info");
        } catch (IOException ex) {
            Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException:");
        }

    }

    @FXML
    public void devicecheck() {
        try {
            App.setRoot("device_status");
        } catch (IOException ex) {
            Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException:");
        }
    }

    @FXML
    public void closeApp() {
        System.out.println("Application Close Call made");
        Platform.exit();
        System.out.println("Application Close Call executed");
    }

    @FXML
    public void logOut() {
        try {
            App.setRoot("enterpassword");
        } catch (IOException ex) {
            Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException:");
        }
    }

    @FXML
    public void initialiseintegrity() {
        //System.out.println("initialiseintegrity");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", "echo \"true\" | sudo tee /etc/baseline");
            Process process = null;
            try {
                process = processBuilder.start();
                int exitCode = process.waitFor();
                statusMsg.setText("Integrity Check Initialized");
                System.out.println("\nExited with error code : " + exitCode);
                LOGGER.log(Level.INFO, "Integrity Check Initialized");
            } catch (IOException ex) {
                Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
                LOGGER.log(Level.INFO, ex + "IOException:");
            }

        } catch (Exception e) {
            System.out.println("com.cdac.enrollmentStation.AdminController.initialiseintegrity()" + e.getMessage());
            LOGGER.log(Level.INFO, e + "Exception:");
        }
        // System.out.println("initialiseintegrity1");

    }

    @FXML
    public void restartsystem() {

        confirmPane.setVisible(true);

    }

    @FXML
    private void restart() {
        restartSys();
    }

    @FXML
    private void stayBack() {
        System.out.println("inside stay back");
        //backBtn.setDisable(false);
        confirmPane.setVisible(false);

        //showIris.setDisable(false);
        //showCaptureStatus.setDisable(true);

    }

    private void restartSys() {
        System.out.println("restartsystem");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", "init 6");
            Process process = null;
            try {
                process = processBuilder.start();
                int exitCode = process.waitFor();
                statusMsg.setText("System Reboot");
                LOGGER.log(Level.INFO, "System Reboot");
                System.out.println("\nExited with error code : " + exitCode);
            } catch (IOException ex) {
                Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
                LOGGER.log(Level.INFO, ex + "IOException:");
            }

        } catch (Exception e) {
            //System.out.println("com.cdac.enrollmentStation.AdminController.restartsystem()"+e.getMessage());
            LOGGER.log(Level.INFO, e + "Exception:");
        }
        System.out.println("restartsystem1");

    }


    @FXML
    private void clickCamera() {


        String selectedcamera = (String) combocamera.getSelectionModel().getSelectedItem();
        System.out.println("Selected Camera:" + selectedcamera);
        if (selectedcamera.contains("External")) {
            modifyPropertiesFile(filepath, "cameraid=0", "cameraid=2");
            System.out.println("In External");
            statusMsg.setText("Camera Selection Updated");

        } else {
            modifyPropertiesFile(filepath, "cameraid=2", "cameraid=0");
            System.out.println("In Internal");
            statusMsg.setText("Camera Selection Updated");

        }
    }

    static void modifyPropertiesFile(String filePath, String oldString, String newString) {
        File fileToBeModified = new File(filePath);

        String oldContent = "";

        BufferedReader reader = null;

        FileWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(fileToBeModified));

            //Reading all the lines of input text file into oldContent

            String line = reader.readLine();

            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();

                line = reader.readLine();
            }

            //Replacing oldString with newString in the oldContent

            String newContent = oldContent.replaceAll(oldString, newString);

            //Rewriting the input text file with newContent

            writer = new FileWriter(fileToBeModified);

            writer.write(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //Closing the resources

                reader.close();

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void selectedCombo() {

        File propfile = new File(filepath);
        try {
            FileReader profileread = new FileReader(propfile);
            BufferedReader profbuffread = new BufferedReader(profileread);
            StringBuffer profbuffer = new StringBuffer();
            String line;
            int count = 0;
            try {
                while ((line = profbuffread.readLine()) != null) {
                    if (line.contains("cameraid=0")) {
                        System.out.println("In Cameraid 0");
                        combocamera.getSelectionModel().select(0);
                        count = 1;
                    }


                }
                if (count != 1) {
                    combocamera.getSelectionModel().select(1);
                }
            } catch (IOException ex) {
                Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

}
