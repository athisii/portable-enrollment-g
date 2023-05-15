
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
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class AdminAuthController {
    private static final int MAX_LENGTH = 30;
    @FXML
    private Label statusMsg;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField username;

    @FXML
    public void showHome() throws IOException {
        App.setRoot("main_screen");
    }


    @FXML
    public void serverConfig() {
        try {
            if (AuthUtil.authenticate(username.getText(), passwordField.getText())) {
                App.setRoot("admin_config");
                return;
            }
            statusMsg.setText("Wrong username or password.");
        } catch (GenericException | IOException ex) {
            statusMsg.setText(ex.getMessage());
        }
        // clean up UI on failure
        username.requestFocus();
        username.setText("");
        passwordField.setText("");
    }

    public void initialize() {
        // restrict the TextField Length
        username.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(username, oldValue, newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(passwordField, oldValue, newValue));

        // ease of use for operator
        username.setOnKeyPressed(event -> {
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
        // only meant for PES(as virtual keyboard not used)
        username.requestFocus();
    }

    private void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }

}
