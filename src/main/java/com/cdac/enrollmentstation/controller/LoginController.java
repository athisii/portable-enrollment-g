package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import javafx.fxml.FXML;

import java.io.IOException;

public class LoginController {

    @FXML
    private void onlineLogin() throws IOException {
        App.setRoot("onlinelogin");

    }

    @FXML
    private void offlineLogin() throws IOException {
        App.setRoot("cardauthentication");
    }


}
