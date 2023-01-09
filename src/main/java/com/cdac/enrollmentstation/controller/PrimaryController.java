package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class PrimaryController {
    @FXML
    private TextField irisInit;
    @FXML
    private TextField cameraInit;
    @FXML
    private TextField slapInit;
    @FXML
    private Label statusMsg;
    @FXML
    private Label version;

    private static final String versionNo = "1.0";

    private APIServerCheck apiServerCheck = new APIServerCheck();

    public void responseStatus(String message) {
        Platform.runLater(() -> statusMsg.setText(message));
    }

    @FXML
    private void showEnrollmentHome() throws IOException {
        App.setRoot("enrollment_arc");

    }

    @FXML
    public void showPasswdScreen() throws IOException {
        App.setRoot("enterpassword");
    }

    @FXML
    public void showImportExport() throws IOException {
        String response = "";
        //String connurl = apiServerCheck.getARCURL();
        //String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, "abc123");
        String connurl = apiServerCheck.getUnitListURL();
        String connectionStatus = apiServerCheck.getStatusUnitListAPI(connurl);
        System.out.println("connection status :" + connectionStatus);

        if (!connectionStatus.contentEquals("connected")) {
            //labelstatus.setText(connectionStatus);
            //labelstatus.setText("Enter the Correct URL in Mafis API");
            // Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
            response = "Network Connection Issue. Check Connection and Try Again";
            //messageStatus(response);
            responseStatus(response);
        } else {
            App.setRoot("import_export");
        }
    }

    @FXML
    public void onSettings() throws IOException {
        App.setRoot("admin-auth");
    }

    @FXML
    public void onLogout() throws IOException {
        App.setRoot("main");
    }

    public void initialize() {
        version.setText(versionNo);
    }


}
