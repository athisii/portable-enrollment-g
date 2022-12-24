/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author root
 */
public class AdminController_040722 implements Initializable {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    public void serverconfig() throws IOException {
        App.setRoot("server_config");
    }

    @FXML
    public void licenseInfo() throws IOException {
        System.out.println("License Info button clicked");
        App.setRoot("license_info");

    }

    @FXML
    public void devicecheck() throws IOException {
        App.setRoot("device_status");
    }

    @FXML
    public void closeApp() throws IOException {
        System.out.println("Application Close Call made");
        Platform.exit();
        System.out.println("Application Close Call executed");
    }

    @FXML
    public void on_pesunitid() throws IOException {
        System.out.println("pes_unitid Info button clicked");
        App.setRoot("pes_unitid");
    }

}
