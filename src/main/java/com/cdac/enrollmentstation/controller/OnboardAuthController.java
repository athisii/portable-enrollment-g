package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.MafisServerApi;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.OnboardingReqDto;
import com.cdac.enrollmentstation.dto.OnboardingResDto;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.SCENE_ROOT_ERR_MSG;

public class OnboardAuthController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(OnboardAuthController.class);

    private static final int MAX_LENGTH = 30;
    private volatile boolean isDone = false;
    @FXML
    private BorderPane rootBorderPane;
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
    public void backBtnAction() throws IOException {
        App.setRoot("onboard_network_config");
    }


    @FXML
    public void loginBtnAction() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            if (!isDone) {
                statusMsg.setText("The server is taking more time than expected. Kindly try again.");
                enableControls(backBtn, loginBtn);
            }
        }));
        timeline.setCycleCount(1);
        timeline.play();
        disableControls(backBtn, loginBtn);
        App.getThreadPool().execute(() -> authenticateUser(usernameTextField.getText(), passwordField.getText()));
    }

    private void authenticateUser(String username, String password) {
        try {
            if (AuthUtil.authenticate(username, password)) {
                // Hardware Type Mapping:
                //      PES - 1
                //      FES - 2
                OnboardingResDto onboardingResDto = MafisServerApi.fetchOnboardingDetails(new OnboardingReqDto(username, "1"));
                App.setPno(username);
                App.setEnrollmentStationIds(onboardingResDto.getDeviceSerialNos());
                PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_UNIT_CAPTION, onboardingResDto.getUnitName());
                PropertyFile.changePropertyValue(PropertyName.ENROLLMENT_STATION_UNIT_ID, onboardingResDto.getUnitCode());
                // must set on JavaFX thread.
                Platform.runLater(() -> {
                    try {
                        App.setRoot("onboard_complete");
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
        App.setPno(null);
        App.setEnrollmentStationIds(null);
        isDone = true;
        // clean up UI on failure
        clearUiControls();
        enableControls(backBtn, loginBtn);
    }

    public void initialize() {
        // disable 'enter key' on keyboard
        rootBorderPane.addEventFilter(KeyEvent.ANY, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                keyEvent.consume();
            }
        });
        // restrict the TextField Length
        usernameTextField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(usernameTextField, oldValue, newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> limitCharacters(passwordField, oldValue, newValue));

        // ease of use for operator
        usernameTextField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                passwordField.requestFocus();
                event.consume();
            }
            if (!statusMsg.getText().isBlank()) {
                statusMsg.setText("");
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

    private void clearUiControls() {
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
