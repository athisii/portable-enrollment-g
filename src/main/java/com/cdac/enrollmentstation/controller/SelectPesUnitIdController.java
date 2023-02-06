/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.model.Units;
import com.cdac.enrollmentstation.util.TestProp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class SelectPesUnitIdController implements Initializable {
    TestProp prop = new TestProp();
    //private String importjson="/usr/share/enrollment/json/import/arclistimported.json";
    private String importjson = null;

    private String curpesid = null;

    @FXML
    private Label lblStatus;


    @FXML
    private Button setpesUnitButton, showhome, adminback;

    @FXML
    private ComboBox<Units> combopesUnitId = new ComboBox<>();


    public void messageStatus(String message) {
        lblStatus.setText(message);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        // List<String> listpesunitdetails=new ArrayList<String>();  
        List<Units> listpesunitdetails = new ArrayList<>();
        Units pesunit = new Units();
        //Deleting Import File
        try {
            importjson = prop.getProp().getProperty("importjsonfolder");
            File dire = new File(importjson);
            File[] dirlisting = dire.listFiles();
            if (dirlisting.length != 0) {
                System.out.println("Inside Import directory listing");
                for (File children : dirlisting) {
                    //children.delete();
                    //filename.substring(0, index);
                    System.out.println("File Name:::" + children.getName().substring(0, children.getName().lastIndexOf(".")));
                    //listpesunitdetails.add(children.getName().substring(0, children.getName().lastIndexOf(".")));
                    pesunit.setCaption(children.getName().substring(0, children.getName().lastIndexOf(".")));
                    pesunit.setValue(children.getName().substring(0, children.getName().lastIndexOf(".")));
                    listpesunitdetails.add(pesunit);
                    //System.out.println("File Name:::"+children.getCanonicalFile());
                }

                ObservableList<Units> units = FXCollections.observableArrayList(listpesunitdetails);
                combopesUnitId.setItems(units);


            } else {
                System.out.println("No Import files");
                messageStatus("No Import files, Kindly Import Again");
            }
        } catch (IOException ex) {
            Logger.getLogger(ImportExportController.class.getName()).log(Level.SEVERE, null, ex);
            //response = "Error While Deleting Import Files, Try Again";
            //messageStatus(response);
            messageStatus("Exception:" + ex);
        }
    }


    @FXML
    private void setpesunitdetails() throws IOException {

        try {
            String currentpesunitid = combopesUnitId.getSelectionModel().getSelectedItem().toString();
            System.out.println("combo value : " + combopesUnitId.getSelectionModel().getSelectedItem());
            curpesid = prop.getProp().getProperty("curpesid");

            if (combopesUnitId.getSelectionModel().getSelectedItem().equals("") || combopesUnitId.getSelectionModel().getSelectedItem().equals("null")) {
                System.out.println("Kindly select a PES Unit");
                messageStatus("Kindly select a PES Unit");
                return;
            } else {
                //System.out.println("Selected unit: " + newval.getCaption()
                //combopesUnitId.valueProperty().addListener((obs, oldval, newval) -> {
                //if(newval != null)
                //System.out.println("Selected unit: " + newval.getCaption()
                //   + ". ID: " + newval.getValue());
                try {
                    FileUtils.writeStringToFile(new File(curpesid), currentpesunitid);
                    // String readpesunit= FileUtils.readFileToString(new File(curpesid));
                    //System.out.println("Read UniT:"+readpesunit);
                    messageStatus("PES Unit Id is Set");
                    //currentpesunitid= newval.getCaption();
                } catch (IOException ex) {
                    Logger.getLogger(SelectPesUnitIdController.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Could not set the value pes unit Id");
                    messageStatus("Could not set the PES Unit Id");
                }
                //  });


            }
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Kindly enter/select all the Values");
            messageStatus("Kindly Select a PES Unit");

            //return response;
        }


    }

    @FXML
    private void AdminBack() throws IOException {
        //System.out.println("admin_config");
        App.setRoot("admin_config");
    }

    @FXML
    private void showHome() throws IOException {
        App.setRoot("main_screen");
    }
}
