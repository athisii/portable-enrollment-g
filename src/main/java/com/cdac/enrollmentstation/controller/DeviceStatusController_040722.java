package com.cdac.enrollmentstation.controller;


import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;

public class DeviceStatusController_040722 implements Initializable {
    public APIServerCheck apiServerCheck = new APIServerCheck();
    @FXML
    public ImageView irisstatus;
    @FXML
    public ImageView slapstatus;
    @FXML
    public ImageView camerastatus;
    @FXML
    public ImageView barcodestatus;
    @FXML
    public ImageView mafisurl;
    @FXML
    Label statusMsg;


    @FXML
    public void showDeviceStatus() throws IOException {


        System.out.println("inside show device status");
        Image redcross = new Image("/haar_facedetection/redcross.png");
        Image greentick = new Image("/haar_facedetection/tickgreen.jpg");


        try {
            File iris_file = new File("/etc/fingerprint_iris.txt");
            if (!iris_file.exists()) {
                statusMsg.setText("fingerprint_iris.txt does not exist");
                return;
            }
            Scanner readIrisFile = new Scanner(iris_file);
            while (readIrisFile.hasNextLine()) {
                String iris_file_data = readIrisFile.nextLine();
                System.out.println(iris_file_data);
                if (iris_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Iris Connected");
                    irisstatus.setImage(greentick);

                } else {
                    System.out.println("Iris Not Connected");
                    irisstatus.setImage(redcross);

                }
            }
            readIrisFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading IrisDevice File.");
            e.printStackTrace();
        }

        try {
            File slapscan_file = new File("/etc/fingerprint_realscan.txt");
            if (!slapscan_file.exists()) {
                statusMsg.setText("fingerprint_realscan.txt does not exist");
                return;
            }
            Scanner readSlapFile = new Scanner(slapscan_file);
            while (readSlapFile.hasNextLine()) {
                String slap_file_data = readSlapFile.nextLine();
                System.out.println(slap_file_data);
                if (slap_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Slap Scanner Connected");
                    slapstatus.setImage(greentick);

                } else {
                    System.out.println("Slap Scanner Not Connected");
                    slapstatus.setImage(redcross);

                }
            }
            readSlapFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occur reading SlapScanner File.");
            e.printStackTrace();
        }

        //Camera
        try {
            File camera_file = new File("/etc/fingerprint_camera.txt");
            if (!camera_file.exists()) {
                statusMsg.setText("fingerprint_camera.txt does not exist");
                return;
            }
            Scanner readCameraFile = new Scanner(camera_file);
            while (readCameraFile.hasNextLine()) {
                String camera_file_data = readCameraFile.nextLine();
                System.out.println(camera_file_data);
                if (camera_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Camera Connected");
                    camerastatus.setImage(greentick);

                } else {
                    System.out.println("Camera Not Connected");
                    camerastatus.setImage(redcross);

                }
            }
            readCameraFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading Camera File.");
            e.printStackTrace();
        }

        //Barcode Scanner
        try {
            File barcode_file = new File("/etc/fingerprint_barcode.txt");
            if (!barcode_file.exists()) {
                statusMsg.setText("fingerprint_barcode.txt does not exist");
                return;
            }
            Scanner readBarcodeFile = new Scanner(barcode_file);
            while (readBarcodeFile.hasNextLine()) {
                String barcode_file_data = readBarcodeFile.nextLine();
                System.out.println(barcode_file_data);
                if (barcode_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Barcode Connected");
                    barcodestatus.setImage(greentick);

                } else {
                    System.out.println("Barcode Not Connected");
                    barcodestatus.setImage(redcross);

                }
            }
            readBarcodeFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading Barcode File.");
            e.printStackTrace();
        }


        //URL Connectivity

        try {

            String arcNo = "123abc";
            String connurl = apiServerCheck.getARCURL();
            String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcNo);

            if (!connectionStatus.contentEquals("connected")) {
                System.out.println("mafisurl not Connected");
                mafisurl.setImage(redcross);
            } else {
                System.out.println("mafisurl Connected");
                mafisurl.setImage(greentick);
            }
        } catch (Exception e) {
            System.out.println("Exception:: " + e);
        }


    }

    @FXML
    public void showDeviceStatusPrevious() throws IOException {
        App.setRoot("first_screen");

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        Image redcross = new Image("/haar_facedetection/redcross.png");
        Image greentick = new Image("/haar_facedetection/tickgreen.jpg");


        try {
            File iris_file = new File("/etc/fingerprint_iris.txt");
            if (!iris_file.exists()) {
                statusMsg.setText("fingerprint_iris.txt does not exist");
                return;
            }
            Scanner readIrisFile = new Scanner(iris_file);
            while (readIrisFile.hasNextLine()) {
                String iris_file_data = readIrisFile.nextLine();
                System.out.println(iris_file_data);
                if (iris_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Iris Connected");
                    irisstatus.setImage(greentick);

                } else {
                    System.out.println("Iris Not Connected");
                    irisstatus.setImage(redcross);

                }
            }
            readIrisFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading IrisDevice File.");
            e.printStackTrace();
        }

        try {
            File slapscan_file = new File("/etc/fingerprint_realscan.txt");
            if (!slapscan_file.exists()) {
                statusMsg.setText("fingerprint_realscan.txt does not exist");
                return;
            }
            Scanner readSlapFile = new Scanner(slapscan_file);
            while (readSlapFile.hasNextLine()) {
                String slap_file_data = readSlapFile.nextLine();
                System.out.println(slap_file_data);
                if (slap_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Slap Scanner Connected");
                    slapstatus.setImage(greentick);

                } else {
                    System.out.println("Slap Scanner Not Connected");
                    slapstatus.setImage(redcross);

                }
            }
            readSlapFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occur reading SlapScanner File.");
            e.printStackTrace();
        }

        //Camera
        try {
            File camera_file = new File("/etc/fingerprint_camera.txt");
            if (!camera_file.exists()) {
                statusMsg.setText("fingerprint_camera.txt does not exist");
                return;
            }
            Scanner readCameraFile = new Scanner(camera_file);
            while (readCameraFile.hasNextLine()) {
                String camera_file_data = readCameraFile.nextLine();
                System.out.println(camera_file_data);
                if (camera_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Camera Connected");
                    camerastatus.setImage(greentick);

                } else {
                    System.out.println("Camera Not Connected");
                    camerastatus.setImage(redcross);

                }
            }
            readCameraFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading Camera File.");
            e.printStackTrace();
        }

        //Barcode Scanner
        try {
            File barcode_file = new File("/etc/fingerprint_barcode.txt");
            if (!barcode_file.exists()) {
                statusMsg.setText("fingerprint_barcode.txt does not exist");
                return;
            }
            Scanner readBarcodeFile = new Scanner(barcode_file);
            while (readBarcodeFile.hasNextLine()) {
                String barcode_file_data = readBarcodeFile.nextLine();
                System.out.println(barcode_file_data);
                if (barcode_file_data.equalsIgnoreCase("yes")) {
                    System.out.println("Barcode Connected");
                    barcodestatus.setImage(greentick);

                } else {
                    System.out.println("Barcode Not Connected");
                    barcodestatus.setImage(redcross);

                }
            }
            readBarcodeFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading Barcode File.");
            e.printStackTrace();
        }

        //URL Connectivity

        try {

            String arcNo = "123abc";
            String connurl = apiServerCheck.getARCURL();
            String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl, arcNo);

            if (!connectionStatus.contentEquals("connected")) {
                System.out.println("mafisurl not Connected");
                mafisurl.setImage(redcross);
            } else {
                System.out.println("mafisurl Connected");
                mafisurl.setImage(greentick);
            }
        } catch (Exception e) {
            System.out.println("Exception:: " + e);
        }

    }

}
