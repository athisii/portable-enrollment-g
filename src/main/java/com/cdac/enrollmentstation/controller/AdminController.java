/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.logging.ApplicationLogNew;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class AdminController {

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
    private ComboBox<String> comboBoxCamera;

    @FXML
    private Button camerabtn;


    //For Application Log
    private static final Logger LOGGER = ApplicationLogNew.getLogger(AdminController.class);
    Handler handler;

    String filepath = "/etc/file.properties";

    /**
     * Automatically called by JavaFX runtime.
     */
    public void initialize() {
        comboBoxCamera.getItems().addAll(ApplicationConstant.INTERNAL, ApplicationConstant.EXTERNAL);
        // Internal: value = 0; index = 0
        // External: value = 2; index = 1
        int cameraId = Integer.parseInt(PropertyFile.getProperty(PropertyName.CAMERA_ID));
        // default value to display
        comboBoxCamera.getSelectionModel().select(cameraId == 0 ? 0 : 1);
        comboBoxCamera.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int camId = ApplicationConstant.EXTERNAL.equalsIgnoreCase(newValue) ? 2 : 0;
            PropertyFile.changeCameraProperty(camId);
            Platform.runLater(() -> statusMsg.setText((camId == 0 ? ApplicationConstant.INTERNAL : ApplicationConstant.EXTERNAL) + " Camera Selected"));
            LOGGER.log(Level.INFO, PropertyFile.getProperty(PropertyName.CAMERA_ID));
        });

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
        //System.out.println("License Info button clicked");
        LOGGER.log(Level.INFO, "License Info button clicked");
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
        //System.out.println("Application Close Call made");
        LOGGER.log(Level.INFO, "Application Close Call made");
        Platform.exit();
        //System.out.println("Application Close Call executed");
        LOGGER.log(Level.INFO, "Application Close Call made");
    }

    @FXML
    public void logOut() {
        try {
            App.setRoot("main");
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
                //System.out.println("\nExited with error code : " + exitCode);                
                LOGGER.log(Level.INFO, "\nExited with error code : " + exitCode);
                LOGGER.log(Level.INFO, "Integrity Check Initialized");
            } catch (IOException ex) {
                Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
                LOGGER.log(Level.INFO, ex + "IOException:");
            }

        } catch (Exception e) {
            //System.out.println("com.cdac.enrollmentStation.AdminController.initialiseintegrity()"+e.getMessage());
            LOGGER.log(Level.INFO, "Exception:" + e);
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
        //System.out.println("inside stay back");
        LOGGER.log(Level.INFO, "inside stay back");
        //backBtn.setDisable(false);
        confirmPane.setVisible(false);

        //showIris.setDisable(false);
        //showCaptureStatus.setDisable(true);

    }

    private void restartSys() {
        //System.out.println("restartsystem");
        LOGGER.log(Level.INFO, "restartsystem");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", "init 6");
            Process process = null;
            try {
                process = processBuilder.start();
                int exitCode = process.waitFor();
                statusMsg.setText("System Reboot");
                LOGGER.log(Level.INFO, "System Reboot");
                //System.out.println("\nExited with error code : " + exitCode);
                LOGGER.log(Level.INFO, "\nExited with error code : " + exitCode);
            } catch (IOException ex) {
                Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
                LOGGER.log(Level.INFO, ex + "IOException:");
            }

        } catch (Exception e) {
            //System.out.println("com.cdac.enrollmentStation.AdminController.restartsystem()"+e.getMessage());
            LOGGER.log(Level.INFO, e + "Exception:");
        }
    }

}
