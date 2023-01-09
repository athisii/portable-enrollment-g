package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLogNew;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Utils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 26/12/22
 */
public class CameraController {
    private static final Logger LOGGER = ApplicationLogNew.getLogger(CameraController.class);
    private static final int COUNTDOWN = 5;
    private static final int THREAD_EXEC_PERIOD = 50; // in milliseconds
    private static final int THRESHOLD_FOR_RED_BOX = 10;
    private static final int IMAGE_CAPTURE_LIMIT = 30;
    private static final String INPUT_FILE;
    private static final String WEBCAM_COMMAND;
    private static final String SUB_FILE;

    private static final Image OUT_OF_FRAME_IMAGE;
    private static final Image NO_MASK_IMAGE;
    private static final Image NO_GLASSES_IMAGE;
    private static final Image CLOCK_COLOR_IMAGE;
    private static final Image ANTI_CLOCK_COLOR_IMAGE;
    private static final Image RIGHT_ROTATE_COLOR_IMAGE;
    private static final Image LEFT_ROTATE_COLOR_IMAGE;
    private static final Image CHIN_DOWN_COLORED_IMAGE;
    private static final Image CHIN_UP_COLOR_IMAGE;
    private static final Image TICK_GREEN_IMAGE;

    static {
        try {
            INPUT_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.INPUT_FILE));
            WEBCAM_COMMAND = requireNonBlank(PropertyFile.getProperty(PropertyName.WEBCAM_COMMAND));
            SUB_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.SUB_FILE));
            // loads --> /com/cdac/enrollmentstation/img/
            NO_MASK_IMAGE = loadFileFromImgDirectory("nomask.png");
            NO_GLASSES_IMAGE = loadFileFromImgDirectory("no-goggles.jpg");
            // loads --> /facecode
            OUT_OF_FRAME_IMAGE = loadFileFromFaceCodeDirectory("outofframecolor.png");
            CLOCK_COLOR_IMAGE = loadFileFromFaceCodeDirectory("clockcolor.png");
            ANTI_CLOCK_COLOR_IMAGE = loadFileFromFaceCodeDirectory("anticlockcolor.png");
            RIGHT_ROTATE_COLOR_IMAGE = loadFileFromFaceCodeDirectory("rightrotatecolor.png");
            LEFT_ROTATE_COLOR_IMAGE = loadFileFromFaceCodeDirectory("leftrotatecolor.png");
            CHIN_DOWN_COLORED_IMAGE = loadFileFromFaceCodeDirectory("chindowncolored.png");
            CHIN_UP_COLOR_IMAGE = loadFileFromFaceCodeDirectory("chinupcolor.png");
            TICK_GREEN_IMAGE = loadFileFromFaceCodeDirectory("tickgreen.jpg");

        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException("Some property values are missing or blank or some files are missing in facecode directory");
        }
    }

    @FXML
    private ImageView brightness;

    @FXML
    private ImageView sunGlassIcon;

    @FXML
    private ImageView resultImageView;
    @FXML

    private ImageView iconFrame;
    @FXML

    private AnchorPane confirmPane;
    @FXML
    private Label confirmPaneLbl;
    @FXML
    private ImageView liveImageView;
    @FXML
    private Button confirmNoBtn;
    @FXML
    private Button confirmYesBtn;
    @FXML
    private Label arcLbl;
    @FXML
    private ImageView msgIcon;
    @FXML

    private Button savePhotoBtn;
    @FXML

    private Button backBtn;
    @FXML
    private Label message;
    @FXML
    private Button startStopCameraBtn;
    private volatile boolean cameraActive = false;
    private volatile boolean stopLive = false;
    //    private static final int CAMERA_ID = Integer.parseInt(PropertyFile.getProperty(PropertyName.CAMERA_ID))
    private volatile int cameraId; // default 0
    private VideoCapture videoCapture;

    private volatile int imageCaptureCount = 0;
    private volatile boolean validImage = false;
    @FXML
    private Slider camSlider;

    private ScheduledExecutorService timer;

    // automatically called by JavaFx runtime.
    public void initialize() {
        cameraId = Integer.parseInt(PropertyFile.getProperty(PropertyName.CAMERA_ID).trim());
        // set action for button click
        startStopCameraBtn.setOnAction(this::startCamera);
        savePhotoBtn.setOnAction(this::savePhoto);
        savePhotoBtn.setDisable(!validImage);
        backBtn.setOnAction(this::back);
        confirmNoBtn.setOnAction(this::confirmNo);
        confirmYesBtn.setOnAction(this::confirmYes);
        arcLbl.setText("ARC: " + ARCDetailsHolder.getArcDetailsHolder().getArcDetails().getArcNo());
        //TODO - not working for now
        //camSlider.setVisible(false);
        //brightness.setVisible(false);
    }

    private void confirmYes(ActionEvent actionEvent) {
        confirmPane.setVisible(false);
        savePhotoBtn.setDisable(!validImage);
        enableControls(startStopCameraBtn, backBtn);
        try {
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
            if (holder.getArcDetails().getIris().size() > 1) {
                // should go back to fingerprint scanner page
                App.setRoot("slapscanner_1");
            } else {
                App.setRoot("iris");

            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, ex::getMessage);
        }

    }

    private void confirmNo(ActionEvent actionEvent) {
        confirmPane.setVisible(false);
        savePhotoBtn.setDisable(!validImage);
        enableControls(startStopCameraBtn, backBtn);
    }

    // action for back button
    private void back(ActionEvent actionEvent) {
        confirmPane.setVisible(true);
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        confirmPaneLbl.setText("Click 'Yes' to Scan Iris or Click 'No' to Capture photo");
        // exceptions in both eyes
        if (holder.getArcDetails().getIris().size() > 1) {
            confirmPaneLbl.setText("Click 'Yes' to Scan Fingers or Click 'No' to Capture photo");
        }
        disableControls(startStopCameraBtn, backBtn, savePhotoBtn);
    }

    // action for save photo button
    private void savePhoto(ActionEvent actionEvent) {
        LOGGER.log(Level.INFO, () -> "***********savePhoto*************");
        try {
            App.setRoot("capturecomplete");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
        }
    }

    // action for start/stop button
    public void startCamera(ActionEvent actionEvent) {
        imageCaptureCount = 0;
        validImage = false;
        // if active then stop it
        if (cameraActive) {
            cameraActive = false;
            stopLive = true;
            startStopCameraBtn.setText("Start Camera");
            backBtn.setDisable(false);

            updateImageView(sunGlassIcon, NO_GLASSES_IMAGE);
            updateImageView(iconFrame, NO_MASK_IMAGE);
            updateImageView(msgIcon, null);
            updateImageView(resultImageView, null);
            // stop the camera and thread executor
            stopAcquisition();

        } else {
            // active status to be used by worker thread
            this.cameraActive = true;
            videoCapture = new VideoCapture(cameraId);
            requireCameraOpened();
            this.timer = Executors.newSingleThreadScheduledExecutor();
            //disable controls during the countdown
            this.startStopCameraBtn.setText("Stop Camera");
            disableControls(startStopCameraBtn, backBtn, savePhotoBtn);
            // clears icons
            updateImageView(sunGlassIcon, null);
            updateImageView(iconFrame, null);
            updateImageView(msgIcon, null);
            updateImageView(resultImageView, null);
            // thread for showing live image
            ForkJoinPool.commonPool().execute(this::liveImageThread);
            // thread for capturing photo
            ForkJoinPool.commonPool().execute(this::capturePhotoThread);
        }
    }

    private void capturePhotoThread() {
        // Display count down on UI
        // should run on worker thread
        final AtomicInteger countdown = new AtomicInteger(COUNTDOWN);
        while (countdown.get() > 0) {
            try {
                Platform.runLater(() -> message.setText(countdown.get() + ""));
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                countdown.decrementAndGet();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        // enable controls after countdown
        Platform.runLater(() -> {
            this.startStopCameraBtn.setText("Stop Camera");
            this.startStopCameraBtn.setDisable(false);
            this.message.setText("");
        });
        timer.scheduleAtFixedRate(this::grabFrame, 0, THREAD_EXEC_PERIOD, TimeUnit.MILLISECONDS);
    }

    private void liveImageThread() {
        Mat matrix = new Mat();
        while (!stopLive) {
            boolean read = videoCapture.read(matrix);
            if (!read) {
                // only log when camera is active
                if (cameraActive) {
                    LOGGER.log(Level.INFO, () -> "Failed to capture the photo.");
                }
                return;
            }
            if (imageCaptureCount > THRESHOLD_FOR_RED_BOX) {
                Imgproc.rectangle(matrix,                   //Matrix obj of the image
                        new Point(150, 100),           //p1
                        new Point(450, 450),           //p2
                        new Scalar(0, 0, 255),              //Scalar object for color
                        5                                   //Thickness of the line
                );
                Platform.runLater(() -> message.setText("Move your face to fit in REDBOX"));
            }
            updateImageView(liveImageView, Utils.mat2Image(matrix));
        }
        if (videoCapture.isOpened()) {
            videoCapture.release();
        }
        stopLive = false;
    }


    private void updateImageView(ImageView view, Image image) {
        Platform.runLater(() -> view.setImage(image));
    }


    private void grabFrame() {
        if (!cameraActive) {
            LOGGER.log(Level.INFO, () -> "***************Valid Image already captured. Just stop the current task******************");
            return;
        }
        requireCameraOpened();
        Mat matrix = new Mat();
        // read the current matrix
        this.videoCapture.read(matrix);
        // if the matrix is empty, return
        if (matrix.empty()) {
            LOGGER.log(Level.INFO, () -> "Read empty matrix");
            return;
        }
        // writes to /usr/share/enrollment/images/input.jpeg
        Imgcodecs.imwrite(INPUT_FILE, matrix);
        this.imageCaptureCount++;
        // stop camera after IMAGE_CAPTURE_LIMIT shots
        if (imageCaptureCount > IMAGE_CAPTURE_LIMIT) {
            stopAcquisition();
            updateImageView(sunGlassIcon, NO_GLASSES_IMAGE);
            updateImageView(iconFrame, NO_MASK_IMAGE);
            updateImageView(msgIcon, null);
            Platform.runLater(() -> {
                message.setText("Capture threshold reached. Start camera again.");
                startStopCameraBtn.setText("Start Camera");
            });
            return;
        }
        try {
            //WEBCAM_COMMAND = "python3 /usr/share/enrollment/python/webcam.py"
            Process pr = Runtime.getRuntime().exec(WEBCAM_COMMAND);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String eline;
            while ((eline = error.readLine()) != null) {
                LOGGER.log(Level.SEVERE, eline);
            }
            // TODO - make sure unnecessary print is removed in python scripts
            String line = input.readLine();
            error.close();
            input.close();
            if (line == null) {
                LOGGER.log(Level.SEVERE, () -> "Read null value from outputStream of Process");
                return;
            }
            if (line.contains("Message=")) {
                String subString = line.substring("Message= ".length());
                Platform.runLater(() -> message.setText(subString));
                hintMessage(subString);
            }
            if (line.contains("Valid")) {
                validImage = true;
                stopLive = true;
                cameraActive = false;
            }
            int exitVal = pr.waitFor();
            LOGGER.log(Level.INFO, () -> "Process Exit Value: " + exitVal);
            if (exitVal == 0 && validImage) {
                Image image = new Image(Files.newInputStream(Paths.get(SUB_FILE)));
                resultImageView.setImage(image);
                updateImageView(iconFrame, TICK_GREEN_IMAGE);
                updateImageView(msgIcon, null);
                updateImageView(sunGlassIcon, null);
                Platform.runLater(() -> {
                    // camSlider.setVisible(true)
                    // brightness.setVisible(true)
                    message.setText("Valid image. Restart Camera if photo is not good.");
                    startStopCameraBtn.setText("Restart Camera");
                    enableControls(startStopCameraBtn, backBtn, savePhotoBtn);
                });
                stopAcquisition();
            }
        } catch (Exception ex) {
            Platform.runLater(() -> message.setText("Something went wrong while capturing photo."));
            LOGGER.log(Level.SEVERE, ex::getMessage);
            Thread.currentThread().interrupt();
        }

    }

    private void hintMessage(String message) {
        if (message.contains("Face Going out of matrix")) {
            updateImageView(msgIcon, OUT_OF_FRAME_IMAGE);
        } else if (message.contains("ROTATE Face CLOCK")) {
            updateImageView(msgIcon, CLOCK_COLOR_IMAGE);
        } else if (message.contains("ROTATE Face ANTI")) {
            updateImageView(msgIcon, ANTI_CLOCK_COLOR_IMAGE);
        } else if (message.contains("ROTATE Face RIGHT")) {
            updateImageView(msgIcon, RIGHT_ROTATE_COLOR_IMAGE);
        } else if (message.contains("ROTATE Face LEFT")) {
            updateImageView(msgIcon, LEFT_ROTATE_COLOR_IMAGE);
        } else if (message.contains("CHIN DOWN")) {
            updateImageView(msgIcon, CHIN_DOWN_COLORED_IMAGE);
        } else if (message.contains("CHIN UP")) {
            updateImageView(msgIcon, CHIN_UP_COLOR_IMAGE);
        }
    }

    private void requireCameraOpened() {
        if (!videoCapture.isOpened()) {
            LOGGER.log(Level.SEVERE, () -> "Unable to open camera.");
            Platform.runLater(() -> message.setText("Unable to start camera. Please check camera"));
            throw new GenericException("Unable to start the camera. Please try again");  // controls return immediately
        }
    }

    private static String requireNonBlank(String str) {
        if (str == null || str.isBlank()) {
            throw new GenericException("Property value is null or blank");
        }
        return str;
    }

    private static Image loadFileFromFaceCodeDirectory(String filename) {
        InputStream inputStream = CameraController.class.getResourceAsStream("/facecode/" + filename);
        if (inputStream == null) {
            throw new GenericException(filename + " not found in '/facecode/' directory");
        }
        return new Image(inputStream);
    }

    private static Image loadFileFromImgDirectory(String filename) {
        InputStream inputStream = CameraController.class.getResourceAsStream("/com/cdac/enrollmentstation/img/" + filename);
        if (inputStream == null) {
            throw new GenericException(filename + " not found in '/com/cdac/enrollmentstation/img/' directory");
        }
        return new Image(inputStream);
    }

    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(THREAD_EXEC_PERIOD, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.INFO, ex::getMessage);
                Thread.currentThread().interrupt();
            }
        }
        if (this.videoCapture.isOpened()) {
            this.videoCapture.release();
        }
        this.cameraActive = false;
    }

    @FXML
    private void camSlider() {
        System.out.println("inside Cam Slider");
        System.out.println("CAm Slider Value:" + (int) camSlider.getValue());
        String sliderdec = new DecimalFormat("##.#").format((float) camSlider.getValue());
        System.out.println("CAm Slider Value2:" + sliderdec);
        try {
            System.out.println("905");
            File file = new File(SUB_FILE);
            RescaleOp rescaleOp = new RescaleOp(Float.valueOf(sliderdec), 15 * Float.valueOf(sliderdec), null);
            BufferedImage bimag = ImageIO.read(file);
            rescaleOp.filter(bimag, bimag);
            ImageIO.write(bimag, SUB_FILE, file);
            Image image = new Image(file.toURI().toString());
            resultImageView.setImage(image);
            System.out.println("914");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
        }
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
}
