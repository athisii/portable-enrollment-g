package com.cdac.enrollmentstation.security;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.api.DirectoryLookup;
import com.cdac.enrollmentstation.util.PropertyFile;
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
    private static final Logger LOGGER = ApplicationLog.getLogger(AuthUtil.class);
    private static final int MAX_LENGTH = 15;

    //Suppresses default constructor for noninstantiability
    private AuthUtil() {
    }

    public static void authenticate(TextField textField, PasswordField passwordField, Label message, String fxml) {
        if (passwordField.getText().length() == 0 || textField.getText().length() == 0) {
            message.setText("Please provide the username and password");
            return;
        }

        if ("0".equals(PropertyFile.getProperty(PropertyName.ENV))) {
            // PROD environment
            LOGGER.log(Level.INFO, () -> "***** Authenticating using LDAP ********");
            try {
                DirectoryLookup.doLookup(textField.getText(), passwordField.getText());
            } catch (GenericException exception) {
                message.setText(exception.getMessage());
                passwordField.setText("");
                textField.setText("");
                textField.requestFocus();
                return;
            }
            try {
                App.setRoot(fxml);
            } catch (Exception e) {
                throw new GenericException("Error occurred while loading : " + fxml);
            }
        } else {
            // DEV environment
            LOGGER.log(Level.INFO, () -> "***** Authenticating using file ********");
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
        }


    }

    public static void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }
}
