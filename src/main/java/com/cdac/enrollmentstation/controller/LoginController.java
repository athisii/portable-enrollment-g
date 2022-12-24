package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private TextField irisInit;
    @FXML
    private TextField cameraInit;
    @FXML
    private TextField slapInit;

    @FXML
    private Label version;

    private String versionno = "1.1";

    @FXML
    private void onlineLogin() throws IOException {

        // App.setRoot("second_screen");
        App.setRoot("onlinelogin");

    }

    @FXML
    private void offlineLogin() throws IOException {
        //App.setRoot("list_contract");

        App.setRoot("cardauthentication");

    }


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        //version.setText(versionno);
    }


}
