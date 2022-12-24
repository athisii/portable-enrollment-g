//package com.cdac.enrollmentstation.controller;
//
//import com.cdac.enrollmentstation.App;
//import com.cdac.enrollmentstation.api.APIServerCheck;
//import com.cdac.enrollmentstation.model.ServerConfigStorage;
//import com.cdac.enrollmentstation.model.UnitListDetails;
//import com.cdac.enrollmentstation.model.Units;
//import com.cdac.enrollmentstation.security.CryptoAES256;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.scene.control.ComboBox;
//import javafx.scene.control.Label;
//import javafx.scene.control.TextField;
//
//import java.io.*;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ResourceBundle;
//
//import static com.cdac.enrollmentstation.controller.FXHelloCVController.skey;
//
//
//public class ServerConfigController1 implements Initializable {
////    @FXML
////    private TextField UnitId;
//
//    @FXML
//    private TextField Url;
//
//    @FXML
//    private TextField SerialNO;
//
//    @FXML
//    private Label label;
//
//    @FXML
//    private ComboBox<Units> comboUnitId = new ComboBox<>();
//
//    public APIServerCheck apiServerCheck = new APIServerCheck();
//
//    public ServerConfigController1() {
//
//
//    }
//
//    @FXML
//    public void showHome() throws IOException {
//        App.setRoot("first_screen");
//    }
//
//    @FXML
//    private void saveDetails() throws IOException {
////        App.setRoot("secondary");
////        System.err.println(UnitId.getText());
////        System.err.println(Url.getText());
////        System.err.println(SerialNO.getText());
//
//        writedata();
//    }
//
//    private void writedata() {
//
//        Units unit = comboUnitId.getSelectionModel().getSelectedItem();
//        if (unit == null || Url.getText().length() == 0 || SerialNO.getText().length() == 0) {
//            //Alert alert = new Alert(AlertType.INFORMATION);
//            //alert.setTitle("WARN:");
//            //alert.setContentText("One or more fields contains null values");
//            //alert.showAndWait();
//            label.setText("One or more fields contains null values");
//            return;
//
//        }
//        String line = unit.getValue() + "," + Url.getText() + "," + SerialNO.getText();
//
//        FileWriter file_writer;
//
//        try {
//            file_writer = new FileWriter("/etc/data.txt", false);
//
//            BufferedWriter buffered_writer = new BufferedWriter(file_writer);
//
//            buffered_writer.write(line);
//            buffered_writer.flush();
//            buffered_writer.close();
//
//            //Alert Message After added data sucessfully
//            //Alert alert = new Alert(AlertType.INFORMATION);
//            //alert.setTitle("Status:");
//            //alert.setContentText("Your Data Has been added sucesfully.");
//            //alert.showAndWait();
//            //load the fxml need here!
//            //System.out.println("loading fxml!");
//            label.setText("Your data Has been added sucesfully");
//        } catch (Exception e) {
//            label.setText("Could not add the details");
//            System.out.println("Add line failed!!" + e);
//        }
//    }
//
//    @FXML
//    private void editdetails() throws Exception {
//        try (BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"))) {
//            String line = " ";
//            String input = " ";
//            while ((line = file.readLine()) != null) {
//                String[] tokens = line.split(",");
////                UnitId.setText(tokens[0]);
//                Url.setText(tokens[1]);
//                SerialNO.setText(tokens[2]);
////                UnitId.appendText(tokens[0]);
////                Url.appendText(tokens[1]);
////                SerialNO.appendText(tokens[2]);
//
//                // UnitId.getText();
//
//
////            FileWriter file_writer;
////            file_writer = new FileWriter("/home/shubham/data.txt",true);
////            BufferedWriter buffered_writer = new BufferedWriter(file_writer);
////            //buffered_writer.write(tokens[0] +","+tokens[1]+""+tokens[2]);
////            buffered_writer.flush();
////            buffered_writer.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("Problem reading file.");
//
//        }
//
//    }
//
//
//    private void ReadData() {
//        try {
//            BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"));
//            String line;
//            String input = "";
//            while ((line = file.readLine()) != null) {
//                if (line.contains(ServerConfigStorage.getUnitID())) ;
//                {
//                    line = "";
//                    System.out.println("Line deleted");
//                }
//                input = input + line + '\n';
//            }
//            FileOutputStream File = new FileOutputStream("/etc/data.txt");
//            File.write(input.getBytes());
//            file.close();
//            File.close();
//        } catch (Exception e) {
//            System.out.println("Problem reading file.");
//        }
//    }
//
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//
//
//        String response = "";
//        StringBuilder strResponse = new StringBuilder();
//        String decResponse = "";
//        String connurl = apiServerCheck.getUnitListURL();
//        String sessionkey = "";
//        String connectionStatus = apiServerCheck.getStatusUnitListAPI(connurl);
//        System.out.println("connection status :" + connectionStatus);
//        if (!connectionStatus.contentEquals("connected")) {
//            //   lblStatus.setText(connectionStatus);
//        } else {
//            try {
//                CryptoAES256 aes256 = new CryptoAES256();
//                skey = aes256.getAESKey();
//                String getuuid = aes256.generateRandomUUID();
//                getuuid = getuuid.replace("-", "");
//                System.out.println("guid : " + getuuid.length());
//
//
//                URL siteURL = new URL(connurl);
//                HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
//                con.setRequestMethod("GET");
//                con.setRequestProperty("Content-Type", "application/json; utf-8");
//                con.setRequestProperty("SessionKey", getuuid);
//                con.setRequestProperty("Accept", "application/json");
//                con.setConnectTimeout(10000);
//                con.setDoOutput(true);
//                //String arcNo = "123abc";
//                //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";
//
//
//                try (BufferedReader br = new BufferedReader(
//                        new InputStreamReader(con.getInputStream(), "utf-8"))) {
//
//                    String responseLine = null;
//                    while ((responseLine = br.readLine()) != null) {
//                        strResponse.append(responseLine.trim());
//                    }
//                }
//                sessionkey = con.getHeaderField("SessionKey");
//            } catch (Exception e) {
//
//            }
//
//            System.out.println("sess key :" + sessionkey);
//            CryptoAES256 aesdec = new CryptoAES256(sessionkey);
//            //byte[] decodedKey = Base64.getDecoder().decode(secKey);
//            // rebuild key using SecretKeySpec
//            //SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
//
//
//            decResponse = aesdec.decryptString(strResponse.toString());
//            System.out.println("response received :" + strResponse.toString());
//            System.out.println("dec response : " + decResponse);
//
//
//        }
//
//
//        ObjectMapper objectmapper = new ObjectMapper();
//
//
//        try {
//            UnitListDetails details = objectmapper.readValue(decResponse, UnitListDetails.class);
//            System.out.println(details.toString());
//            ObservableList<Units> units = FXCollections.observableArrayList(details.getUnits());
//            comboUnitId.setItems(units);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("error :" + e.getMessage());
//        }
//
//        System.out.println("combo value : " + comboUnitId.getSelectionModel().getSelectedItem());
//        comboUnitId.valueProperty().addListener((obs, oldval, newval) -> {
//            if (newval != null)
//                System.out.println("Selected unit: " + newval.getCaption()
//                        + ". ID: " + newval.getValue());
//        });
//
//
//        try (BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"))) {
//            String line = " ";
//            String input = " ";
//            while ((line = file.readLine()) != null) {
//                String[] tokens = line.split(",");
////                UnitId.setText(tokens[0]);
//                Url.setText(tokens[1]);
//                SerialNO.setText(tokens[2]);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("file reading problem. ");
//        }
//
//    }
//}
//
//
//// #522e75