/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.event.ChangeListener;
import com.cdac.enrollmentstation.service.DirectoryLookup;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author root
 */
public class OnlineLoginController implements Initializable {
    @FXML
    public Label statusMsg;

    @FXML
    public PasswordField adminpwd;

    @FXML
    private TextField user;

    @FXML
    public void showHome() throws IOException {
        App.setRoot("main");
    }


    @FXML
    public void serverConfig() {

        try (BufferedReader file = new BufferedReader(new FileReader("/etc/adminpwd"))) {
            String line = " ";
            String input = " ";
            while ((line = file.readLine()) != null) {
                if (adminpwd.getText().length() == 0 || user.getText().length() == 0) {
                    statusMsg.setText("Please provide the admin password");
                } else {
                    //byte[] decodedpass = Base64.getDecoder().decode(line);                    
                    //String decodedPassword = new String(decodedpass);
                    //System.out.println("Password2:::"+decodedPassword);
                    if (adminpwd.getText().equals(line)) {
                        App.setRoot("first_screen");
                    } else {
                        statusMsg.setText("Password did not match");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error reading file ");
        }

        try {
            DirectoryLookup ldap = new DirectoryLookup();
            System.out.println("server config ");
            String status = "";
            System.out.println("username : pwd : obj : " + user.getText() + " : " + adminpwd.getText());
            if (adminpwd.getText().length() == 0 || user.getText().length() == 0) {
                System.err.println("Length equals zero1");
                status = "Please provide the admin username/password";

            } else {
                status = ldap.doLookup(user.getText(), adminpwd.getText());
            }
            System.out.println("status : " + status);
            if (status.contains("true")) {
                App.setRoot("first_screen");
            } else {
                statusMsg.setText(status);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        int maxLength = 15;
        /* add ChangeListner to TextField to restrict the TextField Length*/
        user.textProperty().addListener(new ChangeListener(user, maxLength));
        adminpwd.textProperty().addListener(new ChangeListener(adminpwd, maxLength));
    }

}
