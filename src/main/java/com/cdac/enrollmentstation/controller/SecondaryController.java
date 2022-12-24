package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import javafx.fxml.FXML;

import java.io.IOException;

public class SecondaryController {
    @FXML
    private void showARCInput() throws IOException {
        App.setRoot("enrollment_arc");
    }
}