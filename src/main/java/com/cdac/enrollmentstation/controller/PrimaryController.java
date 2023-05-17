package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrimaryController {
    @FXML
    private Button importExportBtn;
    @FXML
    private Label version;
    private static final Logger LOGGER = ApplicationLog.getLogger(PrimaryController.class);

    @FXML
    private void showEnrollmentHome() throws IOException {
        App.setRoot("biometric_enrollment");

    }

    @FXML
    public void showImportExport() throws IOException {
        App.setRoot("import_export");
    }

    @FXML
    public void onSettings() throws IOException {
        App.setRoot("admin_config");
    }

    @FXML
    public void onLogout() throws IOException {
        App.setRoot("login");
    }

    public void initialize() {
        String appVersionNumber = PropertyFile.getProperty(PropertyName.APP_VERSION_NUMBER);
        if (appVersionNumber == null || appVersionNumber.isEmpty()) {
            LOGGER.log(Level.SEVERE, () -> "No entry for '" + PropertyName.APP_VERSION_NUMBER + "' or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException("No entry for '" + PropertyName.APP_VERSION_NUMBER + "' or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        version.setText(appVersionNumber);
        if (!App.isNudLogin()) {
            importExportBtn.setDisable(true);
        }
    }


}
