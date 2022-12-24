/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.util.TestProp;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author root
 */
public class BiometricCaptureCompleteController_1 implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Text statusMessage;

    @FXML
    private ImageView statusImg;

    TestProp prop = new TestProp();

    //For Application Log
    ApplicationLog appLog = new ApplicationLog();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    Handler handler;

    @FXML
    private void homescreen() throws IOException {

        App.setRoot("first_screen");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO

        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails a = holder.getArcDetails();
        statusMessage.setText(a.getDesc());
        //System.out.println(a.getArcstatus().contains("refused") + " " + a.getArcstatus().contains("notreachable"));
        if (a.getDesc().contains("refused") || a.getDesc().contains("notreachable")) {
            Image image = new Image("/haar_facedetection/redcross.png");
            statusImg.setImage(image);
        } else {
            Image image = new Image("/haar_facedetection/tickgreen.jpg");
            statusImg.setImage(image);
        }


    }

}
