/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import javafx.fxml.FXML;

import java.io.IOException;

/**
 * FXML Controller class
 *
 * @author boss
 */
public class ChooseOptionController {

    @FXML
    private void showARCInput() throws IOException {
        App.setRoot("enrollment_arc");
    }

    @FXML
    private void showBarcodeInput() throws IOException {
        App.setRoot("barcode_arc");
    }
}
