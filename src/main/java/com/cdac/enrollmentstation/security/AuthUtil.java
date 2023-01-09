package com.cdac.enrollmentstation.security;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLogNew;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 09/01/23
 */
public class AuthUtil {
    private static final Logger LOGGER = ApplicationLogNew.getLogger(AuthUtil.class);
    private static final int MAX_LENGTH = 15;

    //Suppresses default constructor for noninstantiability
    private AuthUtil() {
    }

    // TODO - based on dev or prod should authenticate
    public static void authenticate(TextField textField, PasswordField passwordField, Label message, String fxml) {
        if (passwordField.getText().length() == 0 || textField.getText().length() == 0) {
            message.setText("Please provide the username and password");
            return;
        }

        // if Dev
        try (BufferedReader file = new BufferedReader(new FileReader("/etc/adminpwd"))) {
            String line = file.readLine();
            if (line != null) {
                if (line.equals(passwordField.getText())) {
                    App.setRoot(fxml);
                } else {
                    message.setText("Username or password is wrong");
                    passwordField.setText("");
                    textField.setText("");
                    textField.requestFocus();
                }
            }
        } catch (Exception e) {
            throw new GenericException("Error reading file.");
        }

        LOGGER.log(Level.INFO, () -> "***** Authenticating using LDAP ********");
        // else Prod
        /*
        try {
            String status;
            status = DirectoryLookup.doLookup(textField.getText(), passwordField.getText());
            if (!"true".equals(status)) {
                message.setText(status);
                passwordField.setText("");
                textField.setText("");
                textField.requestFocus();
                return;
            }
            App.setRoot(fxml);
        } catch (Exception e) {
            throw new GenericException("Error occurred while loading : " + fxml);
        }
         */
    }

    public static void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }
}
