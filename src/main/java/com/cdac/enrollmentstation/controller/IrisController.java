/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.model.IRIS;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.util.SaveEnrollmentDetailsUtil;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author boss
 */
public class IrisController implements MIDIrisEnrollCallback, Initializable {

    private MIDIrisEnroll mIDIrisEnroll = null;
    private DeviceInfo deviceInfo = null;
    private String deviceName = null;
    private int minQuality = 30;
    private int timeout = 10000;
    private MyIcon mLeftIrisImage;
    private MyIcon mRightIrisImage;
    public int retInit;

    @FXML
    public Label lblerror;
    @FXML
    public Label labelarc;
    @FXML
    public ImageView lefticon;
    @FXML
    public ImageView righticon;

    @FXML
    public ImageView statusImage;

    @FXML
    public Button camera;

    @FXML
    private AnchorPane confirmPane;

    @FXML
    private Button captureIris;

    @FXML
    private Button showFinger;

    byte[] leftIrisImage;
    byte[] rightIrisImage;
    byte[] leftIrisTemplate;
    byte[] rightIrisTemplate;


    String irisCaptureInfo;

    Boolean isDeviceInitialized = false;
    Boolean isDeviceConnected = false;
    public Boolean irisCapturedLeft = false;

    public Boolean irisCapturedRight = false;


    Set<IRIS> irisSet = new HashSet<>();
    private static final Logger LOGGER = ApplicationLog.getLogger(IrisController.class);


    @FXML
    private void captureIris() {
        try {
            //connected device list
            captureIris.setDisable(true);
            captureIris.setText("RESCAN");
            lblerror.setText("");
            IrisSide[] irisSides = new IrisSide[1];
            if (mIDIrisEnroll.IsDeviceConnected(DeviceModel.MATISX, irisSides)) {
                LOGGER.log(Level.INFO, "mIDIrisEnroll.IsDeviceConnected function status:" + mIDIrisEnroll.IsDeviceConnected(DeviceModel.MATISX, irisSides));
                List<String> deviceListConnected = new ArrayList<String>();
                int retConnected = mIDIrisEnroll.GetConnectedDevices(deviceListConnected);
                LOGGER.log(Level.INFO, "Connected Device Status from SDK:" + retConnected);
                LOGGER.log(Level.INFO, "Device Connected List:" + deviceListConnected);
                if (retConnected != 0) {
                    lblerror.setText("Device is not connected, Kindly reconnect the IRIS Scanner");
                    LOGGER.log(Level.SEVERE, "Get Device Connected error :" + mIDIrisEnroll.GetErrorMessage(retConnected));
                    captureIris.setDisable(false);
                }
                System.out.println("connected devices :" + deviceListConnected);
                String model = "";
                //Initialize Iris Device
                if (deviceListConnected.size() > 0) {
                    model = deviceListConnected.get(0);
                }
                LOGGER.log(Level.INFO, "DEVICE NAME:" + model);
                DeviceInfo info = new DeviceInfo();

                if (isDeviceInitialized == false) {

                    retInit = mIDIrisEnroll.Init(DeviceModel.valueFor(model), info, irisSides);
                    if (retInit != 0) {
                        deviceInfo = null;
                        LOGGER.log(Level.SEVERE, "Device initialization error :" + mIDIrisEnroll.GetErrorMessage(retInit));
                        isDeviceInitialized = false;

                    } else {
                        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                        SaveEnrollmentDetails saveEnrollment = holder.getSaveEnrollmentDetails();
                        System.out.println("details : " + saveEnrollment.getArcNo());
                        saveEnrollment.setIRISScannerSerailNo(info.SerialNo);
                        saveEnrollment.setEnrollmentStatus("IRIS Capture Completed");
                        holder.setSaveEnrollmentDetails(saveEnrollment);
                        LOGGER.log(Level.INFO, "IRIS Device initialization successfull :");
                        isDeviceInitialized = true;
                    }

                }

                ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                ARCDetails a = holder.getArcDetails();

                List<String> iris = a.getIris();
                LOGGER.log(Level.INFO, iris.toString() + "Inside IrisController getIris ");
                if (iris.size() == 0) {
                    irisCaptureInfo = "RI, LI";
                    int retStartCapture = mIDIrisEnroll.StartCapture(IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_BOTH, minQuality, timeout);
                    if (retStartCapture != 0) {
                        lblerror.setText(mIDIrisEnroll.GetErrorMessage(retStartCapture));
                    } else {
                        LOGGER.log(Level.INFO, "CAPTUTRED");


                    }
                } else if (iris.size() == 1) {

                    if (iris.get(0).contains("LI")) {
                        irisCaptureInfo = "RI";
                        int retStartCapture = mIDIrisEnroll.StartCapture(IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_RIGHT, minQuality, timeout);
                        if (retStartCapture != 0) {
                            lblerror.setText(mIDIrisEnroll.GetErrorMessage(retStartCapture));
                        } else {
                            LOGGER.log(Level.INFO, "CAPTUTRED");

                        }
                    } else if (iris.get(0).contains("RI")) {
                        irisCaptureInfo = "LI";
                        int retStartCapture = mIDIrisEnroll.StartCapture(IrisSide.MIDIRIS_ENROLL_IRIS_SIDE_LEFT, minQuality, timeout);
                        if (retStartCapture != 0) {
                            lblerror.setText(mIDIrisEnroll.GetErrorMessage(retStartCapture));
                        } else {
                            System.out.println("CAPTUTRED");

                        }
                    }
                } else if (iris.size() == 2) {
                    System.out.println("IN BOTH IRIS EXCEPTION");
                    camera.setDisable(false);
                    captureIris.setDisable(true);
                    showFinger.setDisable(true);
                    lblerror.setText("IRIS Capturing not required, Kindly proceed to capture photo");
                }

            } else {
                lblerror.setText("Device is not connected, Kindly reconnect the IRIS Scanner");
                LOGGER.log(Level.INFO, "Device is not connected, Kindly reconnect the IRIS Scanner");
                captureIris.setDisable(false);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception:" + e);
            lblerror.setText("Exception:" + e);
        }

    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        try {
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
            ARCDetails a = holder.getArcDetails();
            labelarc.setText("ARC: " + a.getArcNo());

            mIDIrisEnroll = new MIDIrisEnroll(this);
            String version = mIDIrisEnroll.GetSDKVersion();
            System.out.println("sdk version :" + version);

            List<String> deviceList = new ArrayList<String>();
            int ret = mIDIrisEnroll.GetSupportedDevices(deviceList);
            System.out.println("return value :" + ret);
            if (ret != 0) {
                lblerror.setText(mIDIrisEnroll.GetErrorMessage(ret));
                LOGGER.log(Level.INFO, mIDIrisEnroll.GetErrorMessage(ret));
                return;
            }

            List<String> ls = new ArrayList<String>();
            for (String list : deviceList) {
                ls.add(list);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception:" + e);
            lblerror.setText("Exception:" + e);
        }
    }

    public class MyIcon implements Icon {

        int _Width = 0;
        int _Height = 0;
        float widthCorrection = 0;
        float heightCorrection = 0;
        int deviceWidth = 0;
        int deviceHeight = 0;

        public MyIcon(int Width, int Height) {
            this._Width = Width;
            this._Height = Height;
            m_Image = null;

            if (deviceInfo != null) {
                deviceWidth = deviceInfo.Width;
                deviceHeight = deviceInfo.Height;
                widthCorrection = ((float) _Width) / deviceWidth;
                heightCorrection = ((float) _Height) / deviceHeight;
            }
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (m_Image != null) {
                g.drawImage(m_Image, x, y, getIconWidth(), getIconHeight(), null);

                Graphics2D g2d = (Graphics2D) g;

                int quality = 60;
                try {
                    quality = minQuality;
                } catch (Exception e) {
                }

                if (qtyy >= quality) {
                    g2d.setColor(new Color(0, 100, 0)); // GREEN
                } else {
                    g2d.setColor(new Color(144, 0, 48)); // RED
                }
                g2d.setStroke(new BasicStroke(2));

//                System.out.println("Before X: " + xx + ", Y: " + yy + ", R: " + rr);
                int X = (int) ((deviceWidth - xx) * widthCorrection);
                int Y = (int) (yy * heightCorrection);
                if (Y < 0) {
                    Y = -Y;
                }
                int R = (int) (rr * (heightCorrection + widthCorrection));

                X = X - (R / 2);
                Y = Y - (R / 2);

                g2d.drawOval(X, Y, R, R);
            } else {
                g.fillRect(x, y, getIconWidth(), getIconHeight());
            }
        }

        @Override
        public int getIconWidth() {
            return _Width;
        }

        @Override
        public int getIconHeight() {
            return _Height;
        }
    }

    private java.awt.Image m_Image;
    private int qtyy;
    private int xx;
    private int yy;
    private int rr;

    @Override
    public void OnDeviceDetection(String DeviceName, IrisSide irisSide, DeviceDetection detection) {
        try {
            if (detection == DeviceDetection.CONNECTED) {
                LOGGER.log(Level.INFO, "IRIS deviedetection from SDK:" + DeviceDetection.CONNECTED);
                LOGGER.log(Level.INFO, "Connected:" + DeviceName);
                deviceName = DeviceName;
                LOGGER.log(Level.INFO, "Device Name:" + deviceName);
                isDeviceConnected = true;
            } else { //DETACHED
                LOGGER.log(Level.INFO, "IRIS deviedetection from SDK:" + DeviceDetection.CONNECTED);
                isDeviceConnected = false;
                isDeviceInitialized = false;
                mIDIrisEnroll.Uninit();
                LOGGER.log(Level.INFO, "Disconnected" + DeviceName);


            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE, "IRIS Device, Error while OnDeviceDetection ");
        }


    }

    @Override
    public void OnPreview(int ErrorCode, ImageQuality imageQuality, final ImagePara imagePara) {
        try {
            if (imagePara.LeftImageBufferLen > 0) {
                displayImage(imagePara.LeftImageBuffer, imageQuality.LeftIrisQuality,
                        imageQuality.LeftIrisX, imageQuality.LeftIrisY, imageQuality.LeftIrisR, lefticon, mLeftIrisImage);

            }

            if (imagePara.RightImageBufferLen > 0) {
                displayImage(imagePara.RightImageBuffer, imageQuality.RightIrisQuality,
                        imageQuality.RightIrisX, imageQuality.RightIrisY, imageQuality.RightIrisR, righticon, mRightIrisImage);

            }
        } catch (Exception ex) {

            ex.printStackTrace();
            LOGGER.log(Level.SEVERE, "IRIS Device, Error while OnPreview ");
        }
    }

    @Override
    public void OnComplete(int ErrorCode, ImageQuality imageQuality, final ImagePara imagePara) {
        try {
            if (imagePara == null) {
                System.out.println("stream empty");
                irisCapturedLeft = false;
                irisCapturedRight = false;
                Platform.runLater(() -> {
                    InputStream inputStream = IrisController.class.getResourceAsStream("/img/redcross.png");
                    Image image = new Image(inputStream);
                    camera.setDisable(true);
                    captureIris.setDisable(false);
                    showFinger.setDisable(false);
                    statusImage.setImage(image);
                });

                return;
            }

            if (imagePara.LeftImageBufferLen > 0) {
                irisCapturedLeft = true;
                displayImageComplete(imagePara.LeftImageBuffer, imageQuality.LeftIrisQuality,
                        imageQuality.LeftIrisX, imageQuality.LeftIrisY, imageQuality.LeftIrisR, lefticon, mLeftIrisImage);
                leftIrisImage = imagePara.LeftImageBuffer;
            }

            if (imagePara.RightImageBufferLen > 0) {
                irisCapturedRight = true;
                displayImageComplete(imagePara.RightImageBuffer, imageQuality.RightIrisQuality,
                        imageQuality.RightIrisX, imageQuality.RightIrisY, imageQuality.RightIrisR, righticon, mRightIrisImage);
                rightIrisImage = imagePara.RightImageBuffer;
            }
        } catch (Exception ex) {

            ex.printStackTrace();
            LOGGER.log(Level.SEVERE, "IRIS Device, Error while Oncomplete ");
        }
    }


    void displayImage(final byte[] buffer, final int qty, final int x, final int y, final int r, final ImageView jLabel, final MyIcon icon) throws IOException {
        new Thread(() -> {
            try {
                InputStream in = new ByteArrayInputStream(buffer);
                BufferedImage bufferedImage = ImageIO.read(in);
                System.out.println("BUFFFF" + bufferedImage.getData());

                WritableImage wr = null;
                if (bufferedImage != null) {
                    System.out.println("BUFDREA not null");
                    System.out.println("WR in display image:::" + wr);
                    wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                    PixelWriter pw = wr.getPixelWriter();
                    for (int x1 = 0; x1 < bufferedImage.getWidth(); x1++) {
                        for (int y1 = 0; y1 < bufferedImage.getHeight(); y1++) {
                            pw.setArgb(x1, y1, bufferedImage.getRGB(x1, y1));
                        }
                    }
                }

                ImageView imView = new ImageView(wr);
                System.out.println("IMAGEEEE-displayimage" + imView.getImage().toString());
                jLabel.setImage(wr);
            } catch (Exception e) {
                System.out.println("Error : " + e.getMessage());
            }
        }).start();

        try {

            ImageFormat format = ImageFormat.valueOf("K7");
            int compressionRatio = 1;
            ImagePara imagePara = new ImagePara();
            int ret = mIDIrisEnroll.GetImage(imagePara, compressionRatio, format);
            if (ret != 0) {
            } else {
                if (imagePara.LeftImageBufferLen > 0) {
                    byte[] finalLeftBuffer = new byte[imagePara.LeftImageBufferLen];
                    System.arraycopy(imagePara.LeftImageBuffer, 0, finalLeftBuffer, 0, imagePara.LeftImageBufferLen);
                    leftIrisImage = finalLeftBuffer;
                }

                if (imagePara.RightImageBufferLen > 0) {
                    byte[] finalRightBuffer = new byte[imagePara.RightImageBufferLen];
                    System.arraycopy(imagePara.RightImageBuffer, 0, finalRightBuffer, 0, imagePara.RightImageBufferLen);
                    rightIrisImage = finalRightBuffer;
                }
            }
            ImageFormat formatIso = ImageFormat.valueOf("IIR_K7_2011");
            int compressionRatioIso = 1;
            ImagePara imageParaIso = new ImagePara();
            int retIso = mIDIrisEnroll.GetImage(imageParaIso, compressionRatioIso, formatIso);
            if (retIso != 0) {
            } else {
                if (imageParaIso.LeftImageBufferLen > 0) {
                    byte[] finalLeftBuffer = new byte[imageParaIso.LeftImageBufferLen];
                    System.arraycopy(imageParaIso.LeftImageBuffer, 0, finalLeftBuffer, 0, imageParaIso.LeftImageBufferLen);
                    leftIrisTemplate = finalLeftBuffer;

                }

                if (imageParaIso.RightImageBufferLen > 0) {
                    byte[] finalRightBuffer = new byte[imageParaIso.RightImageBufferLen];
                    System.arraycopy(imageParaIso.RightImageBuffer, 0, finalRightBuffer, 0, imageParaIso.RightImageBufferLen);
                    rightIrisTemplate = finalRightBuffer;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception:" + e + ":Try Again");
        }
    }


    private void displayImageComplete(final byte[] buffer, final int qty, final int x, final int y, final int r, final ImageView jLabel, final MyIcon icon) {

        try {
            new Thread(() -> {
                try {
                    InputStream in = new ByteArrayInputStream(buffer);
                    BufferedImage bufferedImage = ImageIO.read(in);
                    System.out.println("BUFFFF" + bufferedImage.getData());

                    WritableImage wr = null;
                    if (bufferedImage != null) {
                        System.out.println("BUFDREA not null");
                        wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                        System.out.println("WR in display image complete:::" + wr);
                        PixelWriter pw = wr.getPixelWriter();
                        for (int x1 = 0; x1 < bufferedImage.getWidth(); x1++) {
                            for (int y1 = 0; y1 < bufferedImage.getHeight(); y1++) {
                                pw.setArgb(x1, y1, bufferedImage.getRGB(x1, y1));
                            }
                        }
                    }

                    ImageView imView = new ImageView(wr);
                    System.out.println("IMAGEEEE-displayimagecomplete" + imView.getImage().toString());
                    jLabel.setImage(wr);

                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.log(Level.SEVERE, "IRIS Device, Error while Displaying image ");
                }
            }).start();
            System.out.println("iris info :" + irisCaptureInfo.contains("RI"));
            System.out.println("iris info :" + irisCaptureInfo.contains("LI"));
            System.out.println(" iris left : iris right : " + irisCapturedLeft + " : " + irisCapturedRight);

            if (irisCaptureInfo.contains("RI") && irisCaptureInfo.contains("LI")) {
                if (irisCapturedRight == false) {
                    Platform.runLater(() -> {
                        InputStream inputStream = IrisController.class.getResourceAsStream("/img/redcross.png");
                        Image image = new Image(inputStream);
                        camera.setDisable(true);
                        showFinger.setDisable(false);
                        captureIris.setDisable(false);
                        statusImage.setImage(image);
                    });
                    return;
                } else if (irisCapturedLeft == false) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            InputStream inputStream = IrisController.class.getResourceAsStream("/img/redcross.png");
                            Image image = new Image(inputStream);
                            camera.setDisable(true);
                            captureIris.setDisable(false);
                            showFinger.setDisable(false);
                            statusImage.setImage(image);
                        }
                    });
                    return;
                } else if (irisCapturedLeft == true && irisCapturedRight == true) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            InputStream inputStream = IrisController.class.getResourceAsStream("/img/tickgreen.jpg");
                            Image image = new Image(inputStream);
                            camera.setDisable(false);
                            showFinger.setDisable(true);
                            captureIris.setDisable(true);
                            statusImage.setImage(image);
                        }
                    });
                }

            } else if (irisCaptureInfo.contains("RI")) {
                if (irisCapturedRight == false) {
                    Platform.runLater(() -> {
                        InputStream inputStream = IrisController.class.getResourceAsStream("/img/redcross.png");
                        Image image = new Image(inputStream);
                        captureIris.setDisable(false);
                        showFinger.setDisable(false);
                        camera.setDisable(true);
                        statusImage.setImage(image);
                    });
                    return;
                } else {
                    Platform.runLater(() -> {
                        InputStream inputStream = IrisController.class.getResourceAsStream("/img/tickgreen.jpg");
                        Image image = new Image(inputStream);
                        camera.setDisable(false);
                        showFinger.setDisable(true);
                        captureIris.setDisable(true);
                        statusImage.setImage(image);
                    });
                }
            } else if (irisCaptureInfo.contains("LI")) {
                if (irisCapturedLeft == false) {
                    Platform.runLater(() -> {
                        InputStream inputStream = IrisController.class.getResourceAsStream("/img/redcross.png");
                        Image image = new Image(inputStream);
                        camera.setDisable(true);
                        captureIris.setDisable(false);
                        showFinger.setDisable(false);
                        statusImage.setImage(image);
                    });

                    return;
                } else {
                    Platform.runLater(() -> {
                        InputStream inputStream = IrisController.class.getResourceAsStream("/img/tickgreen.jpg");
                        Image image = new Image(inputStream);
                        camera.setDisable(false);
                        showFinger.setDisable(true);
                        captureIris.setDisable(true);
                        statusImage.setImage(image);
                    });
                }
            }
            ImageFormat format = ImageFormat.valueOf("K7"); //Image Format K7 for Image
            int compressionRatio = 1;
            ImagePara imagePara = new ImagePara();
            int ret = mIDIrisEnroll.GetImage(imagePara, compressionRatio, format);
            if (ret != 0) {
            } else {
                if (imagePara.LeftImageBufferLen > 0) {
                    byte[] finalLeftBuffer = new byte[imagePara.LeftImageBufferLen];
                    System.arraycopy(imagePara.LeftImageBuffer, 0, finalLeftBuffer, 0, imagePara.LeftImageBufferLen);
                    leftIrisImage = finalLeftBuffer;
                }

                if (imagePara.RightImageBufferLen > 0) {
                    byte[] finalRightBuffer = new byte[imagePara.RightImageBufferLen];
                    System.arraycopy(imagePara.RightImageBuffer, 0, finalRightBuffer, 0, imagePara.RightImageBufferLen);
                    rightIrisImage = finalRightBuffer;
                }
            }
            ImageFormat formatIso = ImageFormat.valueOf("IIR_K7_2011");//Image Format IIR_K7_2011 for Image Template
            int compressionRatioIso = 1;
            ImagePara imageParaIso = new ImagePara();
            int retIso = mIDIrisEnroll.GetImage(imageParaIso, compressionRatioIso, formatIso);
            if (retIso != 0) {
            } else {
                if (imageParaIso.LeftImageBufferLen > 0) {
                    byte[] finalLeftBuffer = new byte[imageParaIso.LeftImageBufferLen];
                    System.arraycopy(imageParaIso.LeftImageBuffer, 0, finalLeftBuffer, 0, imageParaIso.LeftImageBufferLen);
                    leftIrisTemplate = finalLeftBuffer;

                }

                if (imageParaIso.RightImageBufferLen > 0) {
                    byte[] finalRightBuffer = new byte[imageParaIso.RightImageBufferLen];
                    System.arraycopy(imageParaIso.RightImageBuffer, 0, finalRightBuffer, 0, imageParaIso.RightImageBufferLen);
                    rightIrisTemplate = finalRightBuffer;

                }

            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception:" + e + ":Try Again");

        }
    }


    private void jbtnSaveImageActionPerformed(java.awt.event.ActionEvent evt) {
        if (deviceInfo == null) {
            return;
        }
        ImageFormat format = ImageFormat.valueOf("BMP");
        int compressionRatio = 1;
        ImagePara imagePara = new ImagePara();
        int ret = mIDIrisEnroll.GetImage(imagePara, compressionRatio, format);
        if (ret != 0) {
        } else {
            if (imagePara.LeftImageBufferLen > 0) {
                byte[] finalLeftBuffer = new byte[imagePara.LeftImageBufferLen];
                System.arraycopy(imagePara.LeftImageBuffer, 0, finalLeftBuffer, 0, imagePara.LeftImageBufferLen);
            }

            if (imagePara.RightImageBufferLen > 0) {
                byte[] finalRightBuffer = new byte[imagePara.RightImageBufferLen];
                System.arraycopy(imagePara.RightImageBuffer, 0, finalRightBuffer, 0, imagePara.RightImageBufferLen);
            }
        }
    }

    @FXML
    private void switchToSlap() throws IOException {
        showFinger.setDisable(true);
        captureIris.setDisable(true);
        camera.setDisable(true);
        confirmPane.setVisible(true);
        System.out.println("Confirm Pane Call::");

    }

    @FXML
    private void cameraCapture() throws IOException {
        if (rightIrisImage == null && leftIrisImage != null) {
            System.out.println("In Left Iris Image" + leftIrisImage);
            IRIS irisLeft = new IRIS();
            irisLeft.setPosition("LI");
            irisLeft.setImage(Base64.getEncoder().encodeToString(leftIrisImage));
            irisLeft.setTemplate(Base64.getEncoder().encodeToString(leftIrisTemplate));
            irisSet.add(irisLeft);
            System.out.println("In Camera Capture1");
        } else if (leftIrisImage == null && rightIrisImage != null) {

            System.out.println("In Left Iris Image" + rightIrisImage);
            IRIS irisRight = new IRIS();
            irisRight.setPosition("RI");
            irisRight.setImage(Base64.getEncoder().encodeToString(rightIrisImage));
            irisRight.setTemplate(Base64.getEncoder().encodeToString(rightIrisTemplate));
            irisSet.add(irisRight);
        } else if (rightIrisImage != null && leftIrisImage != null) {
            IRIS irisLeft = new IRIS();
            irisLeft.setPosition("LI");
            irisLeft.setImage(Base64.getEncoder().encodeToString(leftIrisImage));
            irisLeft.setTemplate(Base64.getEncoder().encodeToString(leftIrisTemplate));
            irisSet.add(irisLeft);

            IRIS irisRight = new IRIS();
            irisRight.setPosition("RI");
            irisRight.setImage(Base64.getEncoder().encodeToString(rightIrisImage));
            irisRight.setTemplate(Base64.getEncoder().encodeToString(rightIrisTemplate));
            irisSet.add(irisRight);
        }


        System.out.println("show Iris ::");
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails a = holder.getArcDetails();
        SaveEnrollmentDetails saveEnrollment = holder.getSaveEnrollmentDetails();
        System.out.println("details : " + saveEnrollment.getArcNo());
        saveEnrollment.setIris(irisSet);
        saveEnrollment.setEnrollmentStatus("IrisCompleted");
        holder.setSaveEnrollmentDetails(saveEnrollment);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);

        try {
            SaveEnrollmentDetailsUtil.writeToFile(saveEnrollment);
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }

        // Added For Biometric Options
        if (holder.getArcDetails().getBiometricOptions().contains("Biometric")) {
            try {
                mIDIrisEnroll.Uninit();
                App.setRoot("biometric_capture_complete");
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "IOException At Get Biometric Options:" + ex);
            }
        } else {
            try {
                mIDIrisEnroll.Uninit();
                App.setRoot("camera");
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "IOException At Get Biometric Options:" + ex);
            }
        }

    }

    @FXML
    private void goBack() {
        System.out.println("inside go back");

        try {

            Thread.sleep(1);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Error: " + ex.getMessage());
        }
        try {
            mIDIrisEnroll.Uninit();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error: " + ex.getMessage());
        }
        try {
            App.setRoot("slapscanner");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error: " + ex.getMessage());
        }

    }

    @FXML
    private void stayBack() {
        System.out.println("inside stay back");
        confirmPane.setVisible(false);

        captureIris.setDisable(false);
        showFinger.setDisable(false);

    }
}
