package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LicenceInfoController implements Initializable {
    //    @FXML
//    private TextField UnitId;
    @FXML
    private TextField finScannerInfo;

    @FXML
    private TextField cardReaderInfo;


    @FXML
    private Button FetchLicDetails;

    @FXML
    private Button gohome;



    //For Application Log
    private static final Logger LOGGER = ApplicationLog.getLogger(LicenceInfoController.class);

    public LicenceInfoController() {
        //this.handler = appLog.getLogger();
        //LOGGER.addHandler(handler); 
    }


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    public void showHome() throws IOException {
        App.setRoot("main_screen");
    }

    @FXML
    public void goHome() {
        try {
            App.setRoot("admin_config");
        } catch (IOException ex) {
            Logger.getLogger(LicenceInfoController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, "IOException:" + ex);
        }
    }


    @FXML
    private void fetchLicenceDetails() throws IOException {


        System.out.println("In fetch Licence");
        ProcessBuilder processBuilder = new ProcessBuilder();
        // -- Linux --

        // Run a shell command
        processBuilder.command("bash", "-c", "curl -X GET http://localhost:8088/N_getSystemInfo");

        try {
            cardReaderInfo.setText("");
            Process process = processBuilder.start();
            System.out.println("System Info" + process);
            // blocked :(
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                cardReaderInfo.setText(line);
            }
            //System.out.println("LINEEE:::"+line);

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        processBuilder.command("bash", "-c", "/usr/share/enrollment/ansi/license_manager_cli -p");

        try {
            finScannerInfo.setText("");
            Process process = processBuilder.start();
            System.out.println("System Info" + process);
            // blocked :(
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                finScannerInfo.setText(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
        
      