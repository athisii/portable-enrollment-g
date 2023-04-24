/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.exception.GenericException;
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
public class OnlineLoginController {
    private static final int MAX_LENGTH = 30;

    @FXML
    private Label statusMsg;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField textField;

    @FXML
    public void showHome() throws IOException {
        App.setRoot("login");
    }


    @FXML
    public void showPrimaryScreen() {
        try {
            if (AuthUtil.authenticate(textField.getText(), passwordField.getText())) {
                App.setRoot("main_screen");
                return;
            }
            statusMsg.setText("Wrong username or password.");
        } catch (GenericException | IOException ex) {
            statusMsg.setText(ex.getMessage());
        }
        // clean up UI on failure
        textField.requestFocus();
        textField.setText("");
        passwordField.setText("");

    }

    public void initialize() {
        //restrict the TextField Length
        textField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(textField, oldValue, newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(passwordField, oldValue, newValue));
        textField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                passwordField.requestFocus();
                event.consume();
            }
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                showPrimaryScreen();
            }
        });
    }
    private void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }

}
