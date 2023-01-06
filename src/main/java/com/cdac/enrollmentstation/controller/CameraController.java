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

    private static final String INPUT_FILE;
    private static final String CAP_COMMAND;
    private static final String WEBCAM_COMMAND;
    private static final String SUB_FILE;
    private static final String OUTPUT_FILE;

    static {
        try {
            INPUT_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.INPUT_FILE));
            CAP_COMMAND = requireNonBlank(PropertyFile.getProperty(PropertyName.CAP_COMMAND));
            WEBCAM_COMMAND = requireNonBlank(PropertyFile.getProperty(PropertyName.WEBCAM_COMMAND));
            SUB_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.SUB_FILE));
            OUTPUT_FILE = requireNonBlank(PropertyFile.getProperty(PropertyName.OUTPUT_FILE));
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, "Some property values are missing or blank");
            throw new GenericException("Some property values are missing or blank");
        }
    }

    @FXML
    public ImageView croppedFrame;
    @FXML

    public ImageView iconFrame;
    @FXML

    public AnchorPane confirmPane;
    @FXML
    private ImageView liveFrame;
    @FXML
    public Button confirmNoBtn;
    @FXML
    public Button confirmYesBtn;
    @FXML
    public Label labelarccam;
    @FXML
    public ImageView msgicon;
    @FXML

    public Button showCaptureStatus;
    @FXML

    public Button showIris;
    @FXML
    private Label message;
    @FXML
    private Button button;
    private boolean cameraActive = false;
    private boolean stopLive = false;
    private int cameraId = 0; // should get it from properties file
    VideoCapture capture;

    private int fCount = 0;
    int validflag = 0;
    @FXML
    private Slider camslider;

    private ScheduledExecutorService timer;
    @FXML
    private Label confirmpanelabel;

    // automatically called by JavaFx runtime.
    public void initialize() {
        try {
            checkRequiredPropertyValues();
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, () -> "Some property values are missing. " + ex.getMessage());
            message.setText(ex.getMessage());
            return;
        }
        // set action for button click
        button.setOnAction(this::startCamera);


    }

    public void startCamera(ActionEvent actionEvent) {
        // if active then stop it
        if (this.cameraActive) {
            this.cameraActive = false;
            Platform.runLater(() -> button.setText("Start Camera"));
            // stop the camera and thread executor
            this.stopAcquisition();

        } else {
            capture = new VideoCapture(-1);
            if (!capture.isOpened()) {
                LOGGER.log(Level.SEVERE, () -> "Unable to open camera.");
                throw new GenericException("Unable to start the camera. Please try again");  // controls return immediately
            }
            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.cameraActive = true;
            Platform.runLater(() -> {
                this.button.setText("Stop Camera");
                this.button.setDisable(true);
            });
            // thread for showing live image
            ForkJoinPool.commonPool().execute(this::liveImageThread);
            // thread for capturing photo
            ForkJoinPool.commonPool().execute(this::capturePhotoThread);

        }
    }

    private void capturePhotoThread() {
        // Display count down on UI
        // should run on worker thread
        // should get hold by photo capturing thread
        final AtomicInteger countdown = new AtomicInteger(5);
        while (countdown.get() > 0) {
            try {
                Platform.runLater(() -> message.setText(countdown.get() + ""));
                Thread.sleep(1000);
                countdown.decrementAndGet();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        this.button.setDisable(false);
        Platform.runLater(() -> this.button.setText("Stop Camera"));
        timer.scheduleAtFixedRate(this::grabFrame, 0, 33, TimeUnit.MILLISECONDS);
    }

    private void liveImageThread() {
        Mat matrix = new Mat();
        while (!stopLive) {
            boolean read = capture.read(matrix);
            if (!read) {
                LOGGER.log(Level.INFO, () -> "Failed to capture the photo.");
                return;
            }
            updateImageView(liveFrame, Utils.mat2Image(matrix));
        }
        if (capture.isOpened()) {
            capture.release();
        }
        stopLive = false;
    }


    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    private void startCapture(ActionEvent actionEvent) {


    }

    private Mat grabFrame() {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    Imgcodecs.imwrite(INPUT_FILE, frame);
                    this.fCount++;
                    LOGGER.log(Level.INFO, " Frame count = " + this.fCount);
                    if (this.fCount > 35) {
                        Imgproc.rectangle(frame,                    //Matrix obj of the image
                                new Point(150, 100),        //p1
                                new Point(450, 450),       //p2
                                new Scalar(0, 0, 255),     //Scalar object for color
                                5                          //Thickness of the line
                        );
                        Platform.runLater(() -> message.setText("Move your face to fit in REDBOX"));
                    }
                    Runtime rt = Runtime.getRuntime();
                    Process pr;
                    if (this.fCount > 60) {
                        //CAP_COMMAND ="python3 /usr/share/enrollment/python/cap.py"
                        pr = rt.exec(CAP_COMMAND);

                    } else {
                        //WEBCAM_COMMAND = "python3 /usr/share/enrollment/python/webcam.py"
                        pr = rt.exec(WEBCAM_COMMAND);
                    }


                    BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line;
                    String eline;
                    while ((eline = error.readLine()) != null) {
                        System.out.println(eline);
                    }
                    error.close();
                    Image imageToShowTick;
                    while ((line = input.readLine()) != null) {
                        String croppedm;
                        if (line.contains("Message=")) {
                            if (this.fCount < 35) {
                                croppedm = line.substring(line.indexOf("Message=") + 9, line.length());
                                LOGGER.log(Level.INFO, croppedm + "Cropped Message=");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        message.setText(croppedm);
                                        Image imageToShow_tick;
                                        if (croppedm.contains("Face Going out of frame")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/outofframecolor.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face CLOCK")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/clockcolor.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face ANTI")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/anticlockcolor.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face RIGHT")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/rightrotatecolor.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face LEFT")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/leftrotatecolor.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("CHIN DOWN")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/chindowncolored.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("CHIN UP")) {
                                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/chinupcolor.png");
                                            imageToShow_tick = new Image(inputStream);
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                    }
                                });
                                if (croppedm.contains("Single")) {
                                    this.stopAcquisition();
                                }
                            }
                            InputStream inputStream = this.getClass().getResourceAsStream("/facecode/camera.png");
                            imageToShowTick = new Image(inputStream);
                            updateImageView(iconFrame, imageToShowTick);
                        }
                        if (line.contains("Valid")) {
                            if (this.fCount < 60) {
                                Platform.runLater(() -> {
                                    message.setText("Valid Image");
                                    validflag = 1;
                                });
                                InputStream inputStream = this.getClass().getResourceAsStream("/facecode/tickgreen.jpg");
                                imageToShowTick = new Image(inputStream);

                                updateImageView(iconFrame, imageToShowTick);
                                Platform.runLater(() -> {
                                    showCaptureStatus.setDisable(false);
                                    showIris.setDisable(true);
                                    button.setDisable(false);
                                });
                            } else {
                                Platform.runLater(() -> message.setText("Face cropped along RED Box. If not valid-repeat process"));
                                InputStream inputStream = this.getClass().getResourceAsStream("/facecode/brownquestion.png");
                                imageToShowTick = new Image(inputStream);
                                updateImageView(iconFrame, imageToShowTick);
                            }
                            this.setClosed();
                            Platform.runLater(() -> {
                                cameraActive = false;

                                button.setText("Start Camera");
                            });
                        }
                    }
                    input.close();
                    int exitVal = pr.waitFor();
                    LOGGER.log(Level.INFO, exitVal + "Process exitValue: ");
                    if (exitVal == 0) {
                        LOGGER.log(Level.INFO, exitVal + ": Process exitValue if ");
                        if (validflag == 1) {
                            File file = new File(SUB_FILE);
                            Image image = new Image(file.toURI().toString());
                            croppedFrame.setImage(image);
                        } else {
                            File file = new File(OUTPUT_FILE);
                            Image image = new Image(file.toURI().toString());
                            croppedFrame.setImage(image);
                        }
                    }


                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, e + ": Exception during the image elaboration ");
            }
        }
        return frame;
    }

    protected void setClosed() {
        this.stopAcquisition();
    }

    private static String requireNonBlank(String str) {
        if (str == null || str.isBlank()) {
            throw new GenericException("Property value is null or blank");
        }
        return str;
    }

    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                //System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
                LOGGER.log(Level.INFO, e + ": Exception in stopping the frame capture, trying to release the camera now... ");
            }
        }

        if (this.capture.isOpened()) {
            // release the camera
            this.capture.release();
        }
        this.cameraActive = false;
    }


    // TODO - check all required property values before initializing the camera.

    private void checkRequiredPropertyValues() {


    }



    @FXML
    private void showIris() throws IOException {

        /*
         ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
         // Added For Biometric Options
          if(holder.getARC().getBiometricoptions().contains("Photo")){
                 App.setRoot("enrollment_arc");
               }else{
                    confirmPane.setVisible(true);
               }
        */
        confirmPane.setVisible(true);
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        // Added For Biometric Options
        if (holder.getArcDetails().getBiometricOptions().contains("Photo")) {
            confirmpanelabel.setText("Click 'Yes' to FetchArc or Click 'No' to Capture photo");
            //panetxt.setDisable(true);
            // App.setRoot("enrollment_arc");
        } else {
            confirmpanelabel.setText("Click 'Yes' to Scan Iris or Click 'No' to Capture photo");
        }
        //App.setRoot("iris");
    }
    @FXML
    private void camSlider() {

        System.out.println("inside Cam Slider");
        System.out.println("CAm Slider Value:" + String.valueOf((int) camslider.getValue()));
        //String sliderValue = String.valueOf();
        //System.out.println("CAm Slider Value1:"+sliderValue);
        String sliderdec = new DecimalFormat("##.#").format((float) camslider.getValue());
        System.out.println("CAm Slider Value2:" + sliderdec);
        try {
            System.out.println("905");
            //String subfile = prop.getProp().getProperty("subfile");
            File file = new File(SUB_FILE);
            RescaleOp rescaleOp = new RescaleOp(Float.valueOf(sliderdec), 15 * Float.valueOf(sliderdec), null);
            BufferedImage bimag = ImageIO.read(file);
            rescaleOp.filter(bimag, bimag);
            ImageIO.write(bimag, SUB_FILE, file);
            Image image = new Image(file.toURI().toString());
            croppedFrame.setImage(image);
            System.out.println("914");
        } catch (Exception ex) {
            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("918" + ex);
        }
    }

    @FXML
    public void showCaptureStatus() {
        try {
            System.out.println("444");
            App.setRoot("capturecomplete");
        } catch (IOException ex) {
            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void goBack() {
        System.out.println("inside go back");

        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            Logger.getLogger(IrisController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "InterruptedException");
        }
        try {

            //Commented For Biometric Options
            //App.setRoot("iris");
            // For Biometric Options
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();

            if (holder.getArcDetails().getBiometricOptions().contains("Photo")) {

                App.setRoot("enrollment_arc");
            } else {

                App.setRoot("iris");

            }
            // For Biometric Options
        } catch (IOException ex) {
            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.INFO, ex + "IOException");
        }
    }

    @FXML
    private void stayBack() {
        System.out.println("inside stay back");
        //backBtn.setDisable(false);
        confirmPane.setVisible(false);

        showIris.setDisable(false);
        showCaptureStatus.setDisable(true);

    }
}
