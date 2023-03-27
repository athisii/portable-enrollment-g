/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.model.IRIS;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailsUtil;
import com.mantra.midirisenroll.DeviceInfo;
import com.mantra.midirisenroll.MIDIrisEnroll;
import com.mantra.midirisenroll.MIDIrisEnrollCallback;
import com.mantra.midirisenroll.enums.DeviceDetection;
import com.mantra.midirisenroll.enums.DeviceModel;
import com.mantra.midirisenroll.enums.ImageFormat;
import com.mantra.midirisenroll.enums.IrisSide;
import com.mantra.midirisenroll.model.ImagePara;
import com.mantra.midirisenroll.model.ImageQuality;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_IRIS_ERR_MSG;
import static com.cdac.enrollmentstation.model.ARCDetailsHolder.getArcDetailsHolder;

/**
 * FXML Controller class
 *
 * @author boss
 */
public class IrisController implements MIDIrisEnrollCallback {
    private static final Logger LOGGER = ApplicationLog.getLogger(IrisController.class);
    private static final int IMAGE_COMPRESSION_RATIO = 1;
    private static final int TEMPLATE_COMPRESSION_RATIO = 1;
    private static final ImageFormat IMAGE_FORMAT = ImageFormat.K7;
    private static final ImageFormat TEMPLATE_FORMAT = ImageFormat.IIR_K7_2011;

    private static final int MIN_QUALITY = 30;
    private static final int CAPTURE_TIMEOUT = 10000;
    private static final String CAPTURE_SUCCESS_MESSAGE = "Iris captured successfully.";

    private static final String DEVICE_NOT_CONNECTED = "Iris scanner not connected. Kindly connect it and try again.";
    private Image failureImage;
    private Image successImage;
    private IrisType irisTypeToCapture;
    private int jniErrorCode;
    private IrisSide irisSideToCapture;
    private boolean displayLeftIris;
    private boolean displayRightIris;
    private DeviceInfo deviceInfo;

    private MIDIrisEnroll midIrisEnroll;

    @FXML
    private Label messageLabel;
    @FXML
    private Label arcLabel;
    @FXML
    private ImageView leftIrisImageView;
    @FXML
    private ImageView rightIrisImageView;

    @FXML
    private ImageView statusImageView;

    @FXML
    private Button capturePhotoBtn;

    @FXML
    private AnchorPane confirmPane;

    @FXML
    private Button captureIrisBtn;

    @FXML
    private Button backBtn;

    private boolean isDeviceInitialized;
    private boolean isDeviceConnected;
    private boolean isIrisCompleted;
    private final Set<IRIS> irisSet = new HashSet<>();

    private enum IrisType {
        LEFT, RIGHT, BOTH, NONE
    }

    private IrisType getIrisToScan(List<String> irisExceptions) {
        Set<IrisType> mIrisSet = new HashSet<>(Set.of(IrisType.RIGHT, IrisType.LEFT));
        irisExceptions.forEach(irisException -> {
            if ("RI".equalsIgnoreCase(irisException)) {
                mIrisSet.remove(IrisType.RIGHT);
            }
            if ("LI".equalsIgnoreCase(irisException)) {
                mIrisSet.remove(IrisType.LEFT);
            }
        });
        if (mIrisSet.isEmpty()) {
            return IrisType.NONE;
        }
        if (mIrisSet.size() == 1) {
            return mIrisSet.stream().findFirst().orElseThrow(() -> new GenericException("Something went wrong while streaming the elements."));
        }
        return IrisType.BOTH;

    }

    public void initialize() {
        // loads failure and success images from FS.
        InputStream inputStream = IrisController.class.getResourceAsStream("/img/redcross.png");
        if (inputStream == null) {
            LOGGER.log(Level.SEVERE, "Received a null inputStream stream while loading failure image from file system.");
            messageLabel.setText(GENERIC_IRIS_ERR_MSG);
            return;
        }
        failureImage = new Image(inputStream, statusImageView.getFitWidth(), statusImageView.getFitHeight(), true, false);
        inputStream = IrisController.class.getResourceAsStream("/img/tickgreen.jpg");
        if (inputStream == null) {
            LOGGER.log(Level.SEVERE, "Received a null inputStream stream while loading success image from file system.");
            messageLabel.setText(GENERIC_IRIS_ERR_MSG);
            return;
        }
        successImage = new Image(inputStream, statusImageView.getFitWidth(), statusImageView.getFitHeight(), true, false);

        // registers callbacks
        midIrisEnroll = new MIDIrisEnroll(this);
        List<String> devices = new ArrayList<>();
        jniErrorCode = midIrisEnroll.GetSupportedDevices(devices);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.INFO, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
            messageLabel.setText(GENERIC_IRIS_ERR_MSG);
            return;
        }
        if (devices.isEmpty()) {
            LOGGER.log(Level.INFO, "Number of supported devices is 0.");
            messageLabel.setText(GENERIC_IRIS_ERR_MSG);
            return;
        }
        // TODO: uncomment this
        /*
        arcLabel.setText("ARC: " + getArcDetailsHolder().getArcDetails().getArcNo());
        irisTypeToScan = getIrisToScan(getArcDetailsHolder().getArcDetails().getIris());
        if (IrisType.NONE == irisTypeToScan) {
            capturePhotoBtn.setDisable(false);
            captureIrisBtn.setDisable(true);
            backBtn.setDisable(true);
            messageLabel.setText("Iris capturing not required. Kindly proceed to capture photo.");
            return;
        }

        if (IrisType.LEFT == irisTypeToScan) {
            displayLeftIris = true;
            irisSideToCapture = IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_LEFT;
        } else if (IrisType.RIGHT == irisTypeToScan) {
            displayRightIris = true;
            irisSideToCapture = IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_RIGHT;
        } else {
            displayRightIris = true;
            displayLeftIris = true;
            irisSideToCapture = IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_BOTH;
        }
        */


        //TODO: remove this
        irisTypeToCapture = getIrisToScan(List.of());
        if (IrisType.NONE == irisTypeToCapture) {
            capturePhotoBtn.setDisable(false);
            captureIrisBtn.setDisable(true);
            backBtn.setDisable(false);
            messageLabel.setText("Iris capturing not required. Kindly proceed to capture photo.");
            return;
        }

        if (IrisType.LEFT == irisTypeToCapture) {
            displayLeftIris = true;
            irisSideToCapture = IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_LEFT;
        } else if (IrisType.RIGHT == irisTypeToCapture) {
            displayRightIris = true;
            irisSideToCapture = IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_RIGHT;
        } else {
            displayRightIris = true;
            displayLeftIris = true;
            irisSideToCapture = IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_BOTH;
        }
    }

    @Override
    public void OnDeviceDetection(String deviceName, IrisSide irisSide, DeviceDetection detection) {
        if (DeviceDetection.CONNECTED == detection) {
            LOGGER.log(Level.INFO, () -> "Connected device name: " + deviceName);
            isDeviceConnected = true;
        } else {
            LOGGER.log(Level.INFO, () -> "Disconnected device name: " + deviceName);
            midIrisEnroll.Uninit();
            isDeviceConnected = false;
            isDeviceInitialized = false;
            Platform.runLater(() -> messageLabel.setText("Iris scanner disconnected."));
        }
    }


    @FXML
    private void captureIrisBtnAction() {
        captureIrisBtn.setDisable(true);
        captureIrisBtn.setText("RESCAN");
        messageLabel.setText("");
        IrisSide[] irisSide = new IrisSide[1];

        if (!midIrisEnroll.IsDeviceConnected(DeviceModel.MATISX, irisSide)) {
            LOGGER.log(Level.SEVERE, () -> "Iris scanner not connected.");
            messageLabel.setText(GENERIC_IRIS_ERR_MSG);
            captureIrisBtn.setDisable(false);
            return;
        }
        List<String> devices = new ArrayList<>();
        jniErrorCode = midIrisEnroll.GetConnectedDevices(devices);
        if (jniErrorCode != 0 || devices.isEmpty()) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
            messageLabel.setText(DEVICE_NOT_CONNECTED);
            captureIrisBtn.setDisable(false);
            return;
        }
        String model = devices.get(0);
        deviceInfo = new DeviceInfo();

        if (!isDeviceInitialized) {
            jniErrorCode = midIrisEnroll.Init(DeviceModel.valueFor(model), deviceInfo, irisSide);
            if (jniErrorCode != 0) {
                LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
                deviceInfo = null;
                isDeviceInitialized = false;
                messageLabel.setText(GENERIC_IRIS_ERR_MSG);
                captureIrisBtn.setDisable(false);
                return;
            }
            isDeviceInitialized = true;
        }

        jniErrorCode = midIrisEnroll.StartCapture(irisSideToCapture, MIN_QUALITY, CAPTURE_TIMEOUT);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
            messageLabel.setText(GENERIC_IRIS_ERR_MSG);
            backBtn.setDisable(false);
            captureIrisBtn.setDisable(false);
        }
    }

    private void updateUiImage(byte[] imageData, ImageView imageView) {
        Image image = new Image(new ByteArrayInputStream(imageData), imageView.getFitWidth(), imageView.getFitHeight(), true, false);
        imageView.setImage(image);
    }

    @Override
    public void OnPreview(int errorCode, ImageQuality imageQuality, final ImagePara imagePara) {
        if (errorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(errorCode));
            Platform.runLater(() -> {
                captureIrisBtn.setDisable(false);
                backBtn.setDisable(false);
            });
            return;
        }

        Platform.runLater(() -> {
            if (displayLeftIris && imagePara.LeftImageBufferLen > 0) {
                updateUiImage(imagePara.LeftImageBuffer, leftIrisImageView);
            }
            if (displayRightIris && imagePara.RightImageBufferLen > 0) {
                updateUiImage(imagePara.RightImageBuffer, rightIrisImageView);
            }
        });
    }

    // TODO: need to check again
    // TODO: parameters are not used.
    @Override
    public void OnComplete(int errorCode, ImageQuality imageQuality, ImagePara imagePara) {
        if (errorCode != 0 || imagePara == null) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(errorCode));
            updateUIOnFailureOrSuccess(false, "Quality too poor. Please try again.");
            return;
        }
        // empties previously added items.
        irisSet.clear();
        // TODO: need to check this
        ImagePara imageData = new ImagePara();
        jniErrorCode = midIrisEnroll.GetImage(imageData, IMAGE_COMPRESSION_RATIO, IMAGE_FORMAT);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
            updateUIOnFailureOrSuccess(false, "Quality too poor. Please try again.");
            return;
        }
        // validates received iris exceptions in ArcDetails and captured iris.
        boolean leftImageResult = displayLeftIris && imageData.LeftImageBufferLen > 0;
        boolean rightImageResult = displayRightIris && imageData.RightImageBufferLen > 0;

        // TODO: need to check this
        ImagePara templateData = new ImagePara();
        jniErrorCode = midIrisEnroll.GetImage(templateData, TEMPLATE_COMPRESSION_RATIO, TEMPLATE_FORMAT);
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
            updateUIOnFailureOrSuccess(false, "Quality too poor. Please try again.");
            return;
        }

        boolean leftTemplateResult = displayLeftIris && templateData.LeftImageBufferLen > 0;
        boolean rightTemplateResult = displayRightIris && templateData.RightImageBufferLen > 0;

        if (IrisType.BOTH == irisTypeToCapture) {
            if (leftImageResult && rightImageResult && leftTemplateResult && rightTemplateResult) {
                irisSet.add(getBase64EncodedIris("LI", imageData.LeftImageBuffer, templateData.LeftImageBuffer));
                irisSet.add(getBase64EncodedIris("RI", imageData.RightImageBuffer, templateData.RightImageBuffer));
                updateUIOnFailureOrSuccess(true, CAPTURE_SUCCESS_MESSAGE);
                isIrisCompleted = true;
                return;
            }
        } else if (IrisType.LEFT == irisTypeToCapture) {
            if (leftImageResult && leftTemplateResult) {
                irisSet.add(getBase64EncodedIris("LI", imageData.LeftImageBuffer, templateData.LeftImageBuffer));
                updateUIOnFailureOrSuccess(true, CAPTURE_SUCCESS_MESSAGE);
                isIrisCompleted = true;
                return;
            }
        } else if (IrisType.RIGHT == irisTypeToCapture && rightImageResult && rightTemplateResult) {
            irisSet.add(getBase64EncodedIris("RI", imageData.RightImageBuffer, templateData.RightImageBuffer));
            updateUIOnFailureOrSuccess(true, CAPTURE_SUCCESS_MESSAGE);
            isIrisCompleted = true;
            return;
        }
        // if control reaches here, something went wrong.
        updateUIOnFailureOrSuccess(false, "Quality too poor. Please try again.");
        captureIrisBtn.setDisable(false);
        backBtn.setDisable(false);
        isIrisCompleted = true;
    }

    private IRIS getBase64EncodedIris(String position, byte[] image, byte[] template) {
        IRIS iris = new IRIS();
        iris.setPosition(position);
        iris.setImage(Base64.getEncoder().encodeToString(image));
        iris.setTemplate(Base64.getEncoder().encodeToString(template));
        return iris;
    }


    @FXML
    private void backBtnAction() {
        backBtn.setDisable(true);
        captureIrisBtn.setDisable(true);
        capturePhotoBtn.setDisable(true);
        confirmPane.setVisible(true);
    }

    @FXML
    private void cameraCapture() {
        ARCDetailsHolder holder = getArcDetailsHolder();
        SaveEnrollmentDetails saveEnrollmentDetails = holder.getSaveEnrollmentDetails();
        saveEnrollmentDetails.setIRISScannerSerailNo(deviceInfo.SerialNo);
        saveEnrollmentDetails.setIris(irisSet);
        saveEnrollmentDetails.setEnrollmentStatus("IrisCompleted");
        holder.setSaveEnrollmentDetails(saveEnrollmentDetails);

        try {
            SaveEnrollmentDetailsUtil.writeToFile(saveEnrollmentDetails);
        } catch (GenericException ex) {
            updateUIOnFailureOrSuccess(false, ex.getMessage());
            return;
        }
        jniErrorCode = midIrisEnroll.Uninit();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
        }

        // Added For Biometric Options
        if (holder.getArcDetails().getBiometricOptions().trim().equalsIgnoreCase("Biometric")) {
            try {
                App.setRoot("biometric_capture_complete");
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, ex::getMessage);
            }
            return;
        }
        try {
            App.setRoot("camera");
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, ex::getMessage);
        }


    }

    @FXML
    private void goBack() {
        jniErrorCode = midIrisEnroll.Uninit();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> midIrisEnroll.GetErrorMessage(jniErrorCode));
        }
        try {
            App.setRoot("slapscanner");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
        }

    }

    @FXML
    private void stayBack() {
        confirmPane.setVisible(false);
        captureIrisBtn.setDisable(false);
        backBtn.setDisable(false);
        capturePhotoBtn.setDisable(!isIrisCompleted);
    }

    private void updateUIOnFailureOrSuccess(boolean status, String message) {
        Platform.runLater(() -> {
            messageLabel.setText(message);
            captureIrisBtn.setDisable(false);
            backBtn.setDisable(false);
            capturePhotoBtn.setDisable(!status);
            statusImageView.setImage(status ? successImage : failureImage);
            isIrisCompleted = status;
        });
    }
}
