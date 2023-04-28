package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

public class PrimaryController {
    @FXML
    private Label version;

    private static final String VERSION_NO = "1.0";


    @FXML
    private void showEnrollmentHome() throws IOException {
        App.setRoot("enrollment_arc");

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
        version.setText(VERSION_NO);
    }


}
