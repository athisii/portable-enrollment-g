package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.SaveEnrollmentDetail;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.jna.Touchpad;
import com.cdac.enrollmentstation.jna.TouchpadEvent;
import com.cdac.enrollmentstation.jna.TouchpadLib;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ArcDetailsHolder;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailUtil;
import com.twelvemonkeys.image.ResampleOp;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.SCENE_ROOT_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 05/11/23
 */
public class SignatureController extends AbstractBaseController {
    private static final Logger LOGGER = ApplicationLog.getLogger(SignatureController.class);
    // 5x27mm aspect ratio
    private static final int PADDING = 3;
    private static final int RAW_WIDTH = 319;
    private static final int RAW_HEIGHT = 59;
    private static final int COMPRESSED_WIDTH = 159;
    private static final int COMPRESSED_HEIGHT = 29;//5*7
    private static final String IMG_SIGNATURE_FILE;
    private static final String IMG_SIGNATURE_COMPRESSED_FILE;
    private volatile boolean capture;
    private volatile boolean firstPaint;
    private final AtomicInteger concurrentWorkerCount = new AtomicInteger(0);
    private static final TouchpadLib TOUCHPAD_LIB = TouchpadLib.INSTANCE;
    private double scaledFactor;
    private static final int TOUCHPAD_MAX_WIDTH;
    private static final int TOUCHPAD_MAX_HEIGHT;
    private static final String TOUCHPAD_DEVICE;
    private static final String DEFAULT_MESSAGE = "Kindly sign on the touchpad and click 'SAVE SIGNATURE' button to proceed.";

    static {
        try {
            String command = "cat /proc/bus/input/devices | egrep -B 2 -A 10 -i '.*touchpad.*' | grep event | awk '{print $3}'";
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
            String eventNumber;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                eventNumber = reader.readLine();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || eventNumber == null) {
                throw new GenericException("Either process not exited normally or no device input with name 'Touchpad' found.");
            }

            TOUCHPAD_DEVICE = "/dev/input/" + eventNumber;
            Touchpad touchpad = new Touchpad(TOUCHPAD_DEVICE);
            var touchpadInfo = touchpad.getDeviceInfo();
            TOUCHPAD_MAX_WIDTH = touchpadInfo.width;
            TOUCHPAD_MAX_HEIGHT = touchpadInfo.height;
            LOGGER.log(Level.INFO, () -> "Touchpad device name:" + touchpad.getDeviceName() + ";width:" + TOUCHPAD_MAX_WIDTH + ";height:" + TOUCHPAD_MAX_HEIGHT);
            touchpad.closeDevice(); // just destroy this touchpad instance after getting info about device
            IMG_SIGNATURE_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.IMG_SIGNATURE_FILE), PropertyName.IMG_SIGNATURE_FILE);
            IMG_SIGNATURE_COMPRESSED_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.IMG_SIGNATURE_COMPRESSED_FILE), PropertyName.IMG_SIGNATURE_COMPRESSED_FILE);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GenericException(ex.getMessage());
        }
    }

    @FXML
    private ImageView previewSignatureImageView;

    @FXML
    private Label confirmPaneLbl;
    @FXML
    private Button backBtn;

    @FXML
    private Label messageLabel;

    @FXML
    private Button clearBtn;
    @FXML
    private Button saveSignatureBtn;

    @FXML
    private VBox confirmVbox;
    @FXML
    private Button confirmYesBtn;
    @FXML
    private Button confirmNoBtn;
    @FXML
    private Label arcLbl;
    @FXML
    private BorderPane borderPane;
    @FXML
    private ImageView recordGifImgView;
    @FXML
    private ImageView writingGifImgView;
    @FXML
    private ImageView stopGifImgView;


    @FXML
    private VBox vBoxCanvasContainer;

    @FXML
    private Canvas canvas;
    private boolean isSigned;
    private double lastX;
    private double lastY;

    // For bounding box
    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE;
    private double maxY = Double.MIN_VALUE;

    private GraphicsContext gc;
    private static final Robot robot = new Robot();


    public void initialize() {
        clearBtn.setOnAction(event -> clearBtnAction());
        saveSignatureBtn.setOnAction(this::saveSignatureBtnAction);
        confirmNoBtn.setOnAction(this::confirmNo);
        confirmYesBtn.setOnAction(this::confirmYes);
        backBtn.setOnAction(this::backBtnAction);

        App.getThreadPool().execute(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> {
                /*
                  Height aspect ratio canvas and touchpad
                  scaled factor: 250/2176 = 0.1147
              */
                scaledFactor = vBoxCanvasContainer.getPrefHeight() / TOUCHPAD_MAX_HEIGHT;
                double canvasWidth = TOUCHPAD_MAX_WIDTH * scaledFactor;
                double canvasHeight = TOUCHPAD_MAX_HEIGHT * scaledFactor;
                canvas = new Canvas(canvasWidth, canvasHeight);
                vBoxCanvasContainer.getChildren().add(canvas);
                gc = canvas.getGraphicsContext2D();
                gc.setLineWidth(3);
            });

        });

        arcLbl.setText("e-ARC: " + ArcDetailsHolder.getArcDetailsHolder().getArcDetail().getArcNo());
    }

    private void showPreview(double minX, double minY, double maxX, double maxY) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage writableImage = canvas.snapshot(params, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
        int width = (int) Math.min(maxX - minX, canvas.getWidth() - minX);
        int height = (int) Math.min(maxY - minY, canvas.getHeight() - minY);

        int bMinX = (int) minX;
        int bMinY = (int) minY;

        if (bMinX > PADDING) {
            bMinX = bMinX - PADDING;
            width = width + PADDING;
        }
        if (bMinY > PADDING) {
            bMinY = bMinY - PADDING;
            height = height + PADDING;
        }
        if (maxX + PADDING < canvas.getWidth()) {
            width = width + PADDING;
        }
        if (maxY + PADDING < canvas.getHeight()) {
            height = height + PADDING;
        }
        if (width > 0 && height > 0) {
            BufferedImage imageBoundedBox = bufferedImage.getSubimage(bMinX, bMinY, width, height);
            BufferedImage resizedImage = resizeImage(width, height, imageBoundedBox, RAW_WIDTH, RAW_HEIGHT);
            previewSignatureImageView.setImage(SwingFXUtils.toFXImage(resizedImage, null));
        }
    }

    private void backBtnAction(ActionEvent event) {
        disableControls(backBtn, clearBtn, saveSignatureBtn);
        confirmVbox.setVisible(true);
        if ("biometric".equalsIgnoreCase(ArcDetailsHolder.getArcDetailsHolder().getArcDetail().getBiometricOptions().trim())) {
            confirmPaneLbl.setText("Click 'Yes' to Iris Scan or Click 'No' Capture Signature");
        } else {
            confirmPaneLbl.setText("Click 'Yes' to Capture Photo or Click 'No' Capture Signature");
        }
    }

    private void confirmYes(ActionEvent actionEvent) {
        confirmVbox.setVisible(false);
        try {
            if ("biometric".equalsIgnoreCase(ArcDetailsHolder.getArcDetailsHolder().getArcDetail().getBiometricOptions().trim())) {
                App.setRoot("iris");
                return;
            }
            App.setRoot("camera");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, SCENE_ROOT_ERR_MSG, ex);
        }
    }

    private void confirmNo(ActionEvent actionEvent) {
        confirmVbox.setVisible(false);
        enableControls(backBtn, clearBtn, saveSignatureBtn);
    }

    private void clearBtnAction() {
        messageLabel.setText("Kindly sign inside the black box and click 'SAVE SIGNATURE' button to proceed.");
        isSigned = false;
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;
        previewSignatureImageView.setImage(null);
    }

    private void saveSignatureBtnAction(ActionEvent event) {
        if (!isSigned) {
            messageLabel.setText("Kindly provide the signature. ");
            return;
        }
        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage writableImage = canvas.snapshot(params, null);
            BufferedImage image = SwingFXUtils.fromFXImage(writableImage, null);
            int width = (int) (maxX - minX);
            int height = (int) (maxY - minY);

            // just a check to ensure valid/big signature is provided.
            if (width < 20 || height < 20) {
                messageLabel.setText("Kindly provide a valid or larger signature.");
                return;
            }

            if (minX > PADDING) {
                minX = minX - PADDING;
                width = width + PADDING;
            }
            if (minY > PADDING) {
                minY = minY - PADDING;
                height = height + PADDING;
            }
            if (maxX + PADDING < canvas.getWidth()) {
                width = width + PADDING;
            }
            if (maxY + PADDING < canvas.getHeight()) {
                height = height + PADDING;
            }

            BufferedImage imageBoundedBox = image.getSubimage((int) minX, (int) minY, width, height);
            BufferedImage resizedImage = resizeImage(width, height, imageBoundedBox, RAW_WIDTH, RAW_HEIGHT);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", byteArrayOutputStream);
            byteArrayOutputStream.close();
            byte[] data = byteArrayOutputStream.toByteArray();

            if (data.length < 1024) {
                LOGGER.log(Level.WARNING, () -> "Signature byte size: " + data.length);
                messageLabel.setText("Kindly provide a valid or larger signature.");
                return;
            }

            // Convert the resized BufferedImage to WritableImage
            WritableImage wImage = new WritableImage(resizedImage.getWidth(), resizedImage.getHeight());
            wImage = SwingFXUtils.toFXImage(resizedImage, wImage);
            // Create a Canvas and draw the image on it
            double rectWidthMid = RAW_WIDTH / (double) 2;
            double rectHeightMid = RAW_HEIGHT / (double) 2;
            Canvas imageCanvas = new Canvas(RAW_WIDTH, RAW_HEIGHT);
            imageCanvas.getGraphicsContext2D().drawImage(wImage, rectWidthMid - (resizedImage.getWidth() / (double) 2), rectHeightMid - (resizedImage.getHeight() / (double) 2));

            SnapshotParameters imageParams = new SnapshotParameters();
            imageParams.setFill(Color.TRANSPARENT);
            wImage = imageCanvas.snapshot(imageParams, null);
            BufferedImage finalImage = SwingFXUtils.fromFXImage(wImage, null);

            Path signaturePath = Paths.get(IMG_SIGNATURE_FILE);
            ImageIO.write(finalImage, "png", signaturePath.toFile());

            // for compressed image
            BufferedImage resizedImageCompressed = resizeImage(width, height, imageBoundedBox, COMPRESSED_WIDTH, COMPRESSED_HEIGHT);
            // Convert the resized BufferedImage to WritableImage
            wImage = new WritableImage(resizedImageCompressed.getWidth(), resizedImageCompressed.getHeight());
            wImage = SwingFXUtils.toFXImage(resizedImageCompressed, wImage);
            // Create a Canvas and draw the image on it
            rectWidthMid = COMPRESSED_WIDTH / (double) 2;
            rectHeightMid = COMPRESSED_HEIGHT / (double) 2;
            Canvas imageCompressedCanvas = new Canvas(COMPRESSED_WIDTH, COMPRESSED_HEIGHT);
            imageCompressedCanvas.getGraphicsContext2D().drawImage(wImage, rectWidthMid - (resizedImageCompressed.getWidth() / (double) 2), rectHeightMid - (resizedImageCompressed.getHeight() / (double) 2));

            imageParams = new SnapshotParameters();
            imageParams.setFill(Color.TRANSPARENT);
            wImage = imageCompressedCanvas.snapshot(imageParams, null);
            BufferedImage finalImageCompressed = SwingFXUtils.fromFXImage(wImage, null);


            Path signatureCompressedPath = Paths.get(IMG_SIGNATURE_COMPRESSED_FILE);
            ImageIO.write(finalImageCompressed, "png", signatureCompressedPath.toFile());

            SaveEnrollmentDetail saveEnrollmentDetail = ArcDetailsHolder.getArcDetailsHolder().getSaveEnrollmentDetail();
            saveEnrollmentDetail.setSignatureRequired(true);
            saveEnrollmentDetail.setSignature(Base64.getEncoder().encodeToString(Files.readAllBytes(signaturePath)));
            saveEnrollmentDetail.setSignatureCompressed(Base64.getEncoder().encodeToString(Files.readAllBytes(signatureCompressedPath)));
            saveEnrollmentDetail.setEnrollmentStatus("SignatureCompleted");
            SaveEnrollmentDetailUtil.writeToFile(saveEnrollmentDetail);
            App.setRoot("biometric_capture_complete");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, SCENE_ROOT_ERR_MSG, ex);
            messageLabel.setText(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }

    private BufferedImage resizeImage(int width, int height, BufferedImage boundedBox, int requiredWidth, int requiredHeight) {
        int[] widthHeight;
        if (height > RAW_HEIGHT) {
            if (width > RAW_WIDTH) {
                widthHeight = shrinkUntilBothFit(width, height, requiredWidth, requiredHeight);
                width = widthHeight[0];
                height = widthHeight[1];
            } else if (width < RAW_WIDTH) {
                widthHeight = shrinkUntilHeightFit(width, height, requiredHeight);
                width = widthHeight[0];
                height = widthHeight[1];
            }
        } else if (height < RAW_HEIGHT) {
            if (width > RAW_WIDTH) {
                widthHeight = shrinkUntilWidthFit(width, height, requiredWidth);
                width = widthHeight[0];
                height = widthHeight[1];
            } else if (width < RAW_WIDTH) {
                widthHeight = enlargeBothUntilFit(width, height, requiredWidth, requiredHeight);
                width = widthHeight[0];
                height = widthHeight[1];
            }
        }
        BufferedImageOp boundedBoxResampleOp = new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS);
        boundedBox = boundedBoxResampleOp.filter(boundedBox, null);
        return boundedBox;
    }

    private int[] shrinkUntilBothFit(double w, double h, int requiredWidth, int requiredHeight) {
        double hRem = requiredHeight / h;
        double width = w * hRem;
        double height = requiredHeight;

        if (width > requiredWidth) {
            double wRem = requiredWidth / width;
            height = requiredHeight * wRem;
        }
        return new int[]{(int) width, (int) height};
    }

    private int[] shrinkUntilHeightFit(double w, double h, int requiredHeight) {
        double hRem = requiredHeight / h;
        double width = w * hRem;
        return new int[]{(int) width, requiredHeight};
    }

    private int[] shrinkUntilWidthFit(double w, double h, int requiredWidth) {
        double wRem = requiredWidth / w;
        double height = h * wRem;
        return new int[]{requiredWidth, (int) height};
    }

    private int[] enlargeBothUntilFit(double w, double h, int requiredWidth, int requiredHeight) {
        double width = w;
        double height = h;
        double counter = 1;

        while (width < requiredWidth && height < requiredHeight) {
            width = w * counter;
            height = h * counter;
            counter += 0.1;
        }
        return new int[]{(int) width, (int) height};
    }


    private void updateUi(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    private void disableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(true);
        }
    }

    private void enableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }

    @Override
    public void onUncaughtException() {
        capture = false;
        concurrentWorkerCount.set(0);
        LOGGER.log(Level.INFO, "***Unhandled exception occurred.");
        enableControls(backBtn);
        Platform.runLater(this::clearBtnAction);
        updateUi("Something went wrong. Kindly try again.");
    }

    private static String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            String errorMessage = propertyName + " value is null or blank in " + ApplicationConstant.DEFAULT_PROPERTY_FILE;
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new GenericException(errorMessage);
        }
        return value;
    }

    public void keyTypeAction(KeyEvent keyEvent) {
        if ("R".equalsIgnoreCase(keyEvent.getCharacter())) {
            if (capture) {
                //another worker is still running.
                return;
            }
            capture = true;
            recordGifImgView.setVisible(true);
            Touchpad touchpad = new Touchpad(TOUCHPAD_DEVICE);
            if (!touchpad.isInitialized()) {
                LOGGER.log(Level.SEVERE, "Touchpad initialization failed with code");
                messageLabel.setText("Touchpad initialization failed with code");
                return;
            }
            messageLabel.setText(DEFAULT_MESSAGE);
            disableControls(backBtn, clearBtn, saveSignatureBtn);
            concurrentWorkerCount.getAndIncrement();
            stopGifImgView.setVisible(false);
            App.getThreadPool().execute(() -> startRecordingTouchpadEvent(touchpad));
        }
        if ("S".equalsIgnoreCase(keyEvent.getCharacter())) {
            capture = false;
            recordGifImgView.setVisible(false);
            messageLabel.setText(DEFAULT_MESSAGE);
            enableControls(backBtn, clearBtn, saveSignatureBtn);
            stopGifImgView.setVisible(true);
        }
        if ("C".equalsIgnoreCase(keyEvent.getCharacter())) {
            if (!capture) {
                gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
                minX = Double.MAX_VALUE;
                minY = Double.MAX_VALUE;
                maxX = Double.MIN_VALUE;
                maxY = Double.MIN_VALUE;
                messageLabel.setText(DEFAULT_MESSAGE);
                enableControls(backBtn, clearBtn, saveSignatureBtn);
                previewSignatureImageView.setImage(null);
            } else {
                messageLabel.setText("You must stop the recording to clear the canvas.");
            }
        }
    }

    private void startRecordingTouchpadEvent(Touchpad touchpad) {
        if (touchpad.isClosed()) {
            LOGGER.log(Level.SEVERE, "Touchpad is already closed.");
            return;
        }
        firstPaint = false;
        TouchpadEvent touchpadEvent;
        double x = 0;
        double y = 0;
        while (capture) {
            touchpadEvent = touchpad.getNextEvent();  // blocking operation (wait for next event)
            // don't execute the last event.
            if (!capture) {
                concurrentWorkerCount.getAndDecrement();
                // when last event is collected, shutdown
                touchpad.closeDevice();
                return;
            }
            // disable concurrent paint
            if (concurrentWorkerCount.get() > 1) {
                concurrentWorkerCount.getAndDecrement();
                touchpad.closeDevice();
                return;
            }
            if (touchpadEvent.code == TOUCHPAD_LIB.getXCode()) {
                /*
                  Width aspect ratio canvas and touchpad
                  scaled width: 401/3495 == 0.1147 (width_scaled)
                  x = x * width_scaled
                */

                x = scaledFactor * touchpadEvent.x;
                if (minX > x) {
                    minX = x;
                }
                if (maxX < x) {
                    maxX = x;
                }
            } else if (touchpadEvent.code == TOUCHPAD_LIB.getYCode()) {
                /*
                   Height aspect ratio canvas and touchpad
                   scaled height: 250/2176 = 0.1149 (height_scaled)
                   y = y * height_scaled
                */
                y = scaledFactor * touchpadEvent.y;
                if (minY > y) {
                    minY = y;
                }
                if (maxY < y) {
                    maxY = y;
                }
            } else if (touchpadEvent.code == TOUCHPAD_LIB.getResetCode()) {
                // filter same previous number
                if (touchpadEvent.touch == TOUCHPAD_LIB.isTouched() && !(x == lastX && y == lastY)) {
                    // thread safety
                    double finalX = x;
                    double finalY = y;
                    double finalLastX = lastX;
                    double finalLastY = lastY;
                    boolean finalFirstPaint = firstPaint;
                    // thread safety
                    Platform.runLater(() -> {
                        Point2D point2D = canvas.localToScreen(0, 0);
                        robot.mouseMove(point2D.getX() + finalX, point2D.getY() + finalY);
                        paintCanvas(finalX, finalY, finalFirstPaint, finalLastX, finalLastY);
                    });
                    lastX = x;
                    lastY = y;
                    isSigned = true;
                }
            } else if (touchpadEvent.code == TOUCHPAD_LIB.getTouchCode()) {
                if (touchpadEvent.touch == TOUCHPAD_LIB.isTouched()) {
                    firstPaint = true;
                    Platform.runLater(() -> writingGifImgView.setVisible(true));
                } else {
                    lastX = -1;
                    lastY = -1;
                    firstPaint = false;
                    double finalMinX = minX;
                    double finalMinY = minY;
                    double finalMaxX = maxX;
                    double finalMaxY = maxY;
                    Platform.runLater(() -> {
                        writingGifImgView.setVisible(false);
                        showPreview(finalMinX, finalMinY, finalMaxX, finalMaxY);
                    });
                }
            }
        }
        // when last event is collected, shutdown
        concurrentWorkerCount.getAndDecrement();
        touchpad.closeDevice();
    }

    void paintCanvas(double x, double y, boolean isFirstPaintArg, double lastX, double lastY) {
        gc.beginPath();
        if (isFirstPaintArg) {
            gc.moveTo(x, y);
            firstPaint = false;
        } else {
            gc.moveTo(lastX, lastY);
        }
        gc.lineTo(x, y);
        gc.stroke();
    }
}
