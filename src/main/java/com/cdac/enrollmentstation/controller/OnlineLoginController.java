package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.UserResDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.AuthUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.SCENE_ROOT_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */
public class OnlineLoginController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(OnlineLoginController.class);
    private static final int MAX_LENGTH = 30;
    private volatile boolean isDone = false;

    @FXML
    private Button backBtn;
    @FXML
    private Button loginBtn;

    @FXML
    private Label statusMsg;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField usernameTextField;


    @FXML
    private void backBtnAction() throws IOException {
        App.setRoot("login");
    }

    @FXML
    private void loginBtnAction() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            if (!isDone) {
                statusMsg.setText("The server is taking more time than expected. Kindly try again.");
                enableControls(backBtn, loginBtn);
            }
        }));
        timeline.setCycleCount(1);
        timeline.play();
        disableControls(backBtn, loginBtn);
        App.getThreadPool().execute(this::authenticateUser);
    }

    private void authenticateUser() {
        try {
            if (!"admin".equalsIgnoreCase(usernameTextField.getText())) {
                // Hardware Type Mapping:
                // PES - 1
                // FES - 2
                MafisServerApi.validateUserCategory(new UserResDto(usernameTextField.getText(), PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID), "1", PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID)));
                LOGGER.info("Done validating user category.");
            }
            if (AuthUtil.authenticate(usernameTextField.getText(), passwordField.getText())) {
                // must set on JavaFX thread.
                Platform.runLater(() -> {
                    try {
                        App.setNudLogin(true);
                        App.setRoot("main_screen");
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, SCENE_ROOT_ERR_MSG, ex);
                        throw new GenericException(ex.getMessage());
                    }
                });
                isDone = true;
                return;
            }
            LOGGER.log(Level.INFO, "Incorrect username or password.");
            updateUi("Wrong username or password.");
        } catch (Exception ex) {
            updateUi(ex.getMessage());
        }
        isDone = true;
        // clean up UI on failure
        clearPasswordField();
        enableControls(backBtn, loginBtn);
    }

    public void initialize() {
        //restrict the TextField Length
        usernameTextField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(usernameTextField, oldValue, newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(passwordField, oldValue, newValue));
        usernameTextField.setOnKeyPressed(event -> {
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
    }

    private void limitCharacters(TextField textField, String oldValue, String newValue) {
        if (newValue.length() > MAX_LENGTH) {
            textField.setText(oldValue);
        }
    }

    private void updateUi(String message) {
        Platform.runLater(() -> statusMsg.setText(message));
    }

    private void clearPasswordField() {
        Platform.runLater(() -> {
            usernameTextField.requestFocus();
            usernameTextField.setText("");
            passwordField.setText("");
        });
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

    @Override
    public void onUncaughtException() {
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
        backBtn.setDisable(false);
        loginBtn.setDisable(false);
        updateUi("Something went wrong. Please try again");
    }
}
