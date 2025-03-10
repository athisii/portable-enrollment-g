
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.AuthUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.SCENE_ROOT_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class AdminAuthController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(AdminAuthController.class);

    private static final int MAX_LENGTH = 30;

    @FXML
    private Button backBtn;
    @FXML
    private Button loginBtn;
    @FXML
    private Label statusMsg;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField username;

    @FXML
    public void showHome() throws IOException {
        App.setRoot("main_screen");
    }


    @FXML
    public void loginBtnAction() {
        disableControls(backBtn, loginBtn);
        try {
            if (AuthUtil.authenticate(username.getText(), passwordField.getText())) {
                // must set on JavaFX thread.
                Platform.runLater(() -> {
                    try {
                        App.setRoot("admin_config");
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, SCENE_ROOT_ERR_MSG, ex);
                        throw new GenericException(ex.getMessage());
                    }
                });
                return;
            }
            LOGGER.log(Level.INFO, "Incorrect username or password.");
            updateUi("Wrong username or password.");
        } catch (Exception ex) {
            updateUi(ex.getMessage());
        }
        // clean up UI on failure
        clearUiControls();
        enableControls(backBtn, loginBtn);
    }

    private void clearUiControls() {
        Platform.runLater(() -> {
            username.requestFocus();
            username.setText("");
            passwordField.setText("");
        });
    }

    public void initialize() {
        // restrict the TextField Length
        username.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(username, oldValue, newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(passwordField, oldValue, newValue));

        // ease of use for operator
        username.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                passwordField.requestFocus();
                event.consume();
            }
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                loginBtnAction();
            }
        });
        // only meant for PES(as virtual keyboard not used)
        username.requestFocus();
    }

    private void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }

    private void disableControls(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(true);
        }
    }

    private void enableControls(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(false);
        }
    }

    private void updateUi(String message) {
        Platform.runLater(() -> statusMsg.setText(message));
    }


    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
        backBtn.setDisable(false);
        loginBtn.setDisable(false);
        updateUi("Something went wrong. Please try again");
    }
}
