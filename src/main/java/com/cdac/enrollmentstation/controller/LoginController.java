package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(LoginController.class);
    @FXML
    private VBox onboardMsgVBox;
    @FXML
    private Button idLoginBtn;

    @FXML
    private void onlineLogin() throws IOException {
        if ("1".equals(PropertyFile.getProperty(PropertyName.INITIAL_SETUP).trim())) {
            App.setRoot("onboard_network_config");
        } else {
            App.setRoot("online_login");
        }
    }

    @FXML
    private void offlineLogin() throws IOException {
        App.setRoot("card_login");
    }

    public void initialize() {
        App.setNudLogin(false);
        if ("1".equals(PropertyFile.getProperty(PropertyName.INITIAL_SETUP).trim())) {
            idLoginBtn.setDisable(true);
            onboardMsgVBox.setManaged(true);
        }
    }

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.SEVERE, "***Unhandled exception occurred.");
    }

}
