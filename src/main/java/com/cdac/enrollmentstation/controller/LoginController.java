package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.fxml.FXML;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(LoginController.class);

    @FXML
    private void onlineLogin() throws IOException {
        if ("1".equals(PropertyFile.getProperty(PropertyName.INITIAL_SETUP).trim())) {
            App.setRoot("hostname_ip");
        } else {
            App.setRoot("online_login");
        }
    }

    @FXML
    private void offlineLogin() throws IOException {
        App.setRoot("card_login");
    }

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.SEVERE, "***Unhandled exception occurred.");
    }

}
