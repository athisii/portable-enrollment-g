package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = ApplicationLog.getLogger(LoginController.class);

    @FXML
    private Label version;

    public void initialize() {
        String appVersionNumber = PropertyFile.getProperty(PropertyName.APP_VERSION_NUMBER);
        if (appVersionNumber == null || appVersionNumber.isEmpty()) {
            LOGGER.log(Level.SEVERE, () -> "No entry for '" + PropertyName.APP_VERSION_NUMBER + "' or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException("No entry for '" + PropertyName.APP_VERSION_NUMBER + "' or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        version.setText(appVersionNumber);
    }

    @FXML
    private void onlineLogin() throws IOException {
        App.setRoot("online_login");

    }

    @FXML
    private void offlineLogin() throws IOException {
        App.setRoot("card_login");
    }


}
