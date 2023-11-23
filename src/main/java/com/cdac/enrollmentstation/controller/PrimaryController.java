package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrimaryController extends AbstractBaseController {
    @FXML
    private Button importExportBtn;
    private static final Logger LOGGER = ApplicationLog.getLogger(PrimaryController.class);

    public void initialize() {
        if (!App.isNudLogin()) {
            importExportBtn.setDisable(true);
        }
    }

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

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.SEVERE, "***Unhandled exception occurred.");
    }
}
