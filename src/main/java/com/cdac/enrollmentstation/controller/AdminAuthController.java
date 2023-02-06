/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.security.AuthUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.io.IOException;

/**
 * FXML Controller class
 *
 * @author root
 */
public class AdminAuthController {
    @FXML
    private Label statusMsg;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField textField;

    @FXML
    public void showHome() throws IOException {
        App.setRoot("main_screen");
    }


    @FXML
    public void serverConfig() {
        AuthUtil.authenticate(textField, passwordField, statusMsg, "admin_config");
    }

    public void initialize() {
        // restrict the TextField Length
        textField.textProperty().addListener((observable, oldValue, newValue) -> AuthUtil.limitCharacters(textField, oldValue, newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> AuthUtil.limitCharacters(passwordField, oldValue, newValue));

        // ease of use for operator
        textField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                passwordField.requestFocus();
                event.consume();
            }
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                serverConfig();
            }
        });
    }

}
