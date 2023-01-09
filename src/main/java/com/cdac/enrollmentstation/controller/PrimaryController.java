package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PrimaryController implements Initializable {
    @FXML
    private TextField irisInit;
    @FXML
    private TextField cameraInit;
    @FXML
    private TextField slapInit;
    @FXML
    private Label statusmsg;
    @FXML
    private Label version;

    private String versionno = "1.0";

    private APIServerCheck apiServerCheck = new APIServerCheck();

    public void responseStatus(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                statusmsg.setText(message);
            }
        });
    }

    @FXML
    private void showEnrollmentHome() throws IOException {

        // App.setRoot("second_screen");
        App.setRoot("enrollment_arc");

    }

    @FXML
    private void showContract() throws IOException {
        //App.setRoot("list_contract");

        App.setRoot("n_showtoken_1");

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
            return;
        } else {
            App.setRoot("import_export");
        }
    }

    @FXML
    public void deviceStatus() throws IOException {
        //Initialize IRIS

//        mIDIrisEnroll = new MIDIrisEnroll(this);
//        String version = mIDIrisEnroll.GetSDKVersion();
//        System.out.println("sdk version :"+ version); 
//     

    }

    @FXML
    public void OnSettings() throws IOException {
//        System.out.println("In onsettings");
        App.setRoot("enterpassword");
//        System.out.println("In onsettings1");
    }

    @FXML
    public void OnLogout() throws IOException {
        App.setRoot("main");
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        version.setText(versionno);
    }


}
