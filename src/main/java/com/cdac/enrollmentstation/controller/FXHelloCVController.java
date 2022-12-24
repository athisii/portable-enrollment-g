package com.cdac.enrollmentstation.controller;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.api.APIServerCheck;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.util.TestProp;
import com.cdac.enrollmentstation.util.Utils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a> (minor fixes)
 * @version 2.0 (2016-09-17)
 * @since 1.0 (2013-10-20)
 */
public class FXHelloCVController implements Initializable {
    // the FXML button
    @FXML
    private Button button;
    // the FXML image view
    @FXML
    private ImageView currentFrame;

    @FXML
    private ImageView msgicon;

    @FXML
    private ImageView croppedFrame;

    @FXML
    private ImageView iconFrame;

    @FXML
    private Label message;

    @FXML
    private Label labelarccam;

    @FXML
    private AnchorPane confirmPane;

    @FXML
    private Button showIris;

    String finalBase64Img;

    public static SecretKey skey;

    @FXML
    private Button showCaptureStatus;

    public SaveEnrollmentResponse saveEnrollmentResponse;

    public APIServerCheck apiServerCheck = new APIServerCheck();

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the id of the camera to be used

    int fcount = 0;
    int validflag = 0;
    TestProp prop = new TestProp();

    private static int cameraId = 0;

    @FXML
    private Label confirmpanelabel;

    @FXML
    private Slider camslider;

    @FXML
    private ImageView brightness;

    //@FXML
    // private TextField panetxt;

    //For Application Log
    ApplicationLog appLog = new ApplicationLog();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    Handler handler;

    /**
     * The action triggered by pushing the button on the GUI
     *
     * @param event the push button event
     */
    @FXML
    protected void startCamera(ActionEvent event) {
        if (!this.cameraActive) {

            // start the video capture
            String camid = null;
            try {
                camid = prop.getProp().getProperty("cameraid");
            } catch (IOException ex) {
                //Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                LOGGER.log(Level.INFO, "Camera Id not in File Properties");
            }
            try {
                System.out.println("Replacing Photo Out File");
                //Removing the Old Photo while capturing Photo
                String photocapturefileprop = prop.getProp().getProperty("photocaptureimg");
                File photocapturefile = new File(photocapturefileprop);

                String fileoutput = prop.getProp().getProperty("outputfile");
                File photooutfile = new File(fileoutput);
                FileUtils.copyFile(photocapturefile, photooutfile);
            } catch (IOException ex) {
                Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
            }
            //this.capture.open(cameraId);
            LOGGER.log(Level.INFO, "CamID" + camid);
            int camidint = Integer.parseInt(camid);
            LOGGER.log(Level.INFO, "CamIDint" + camidint);
            LOGGER.log(Level.INFO, "CameraID::" + cameraId);
            //this.capture.open(cameraId);
            this.capture.open(camidint);
            //message.setText("Valid Image");
            // is the video stream available?
            if (this.capture.isOpened()) {
                this.cameraActive = true;
                this.fcount = 0;
                validflag = 0;
                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run() {
                        // effectively grab and process a single frame
                        Mat frame = grabFrame();
                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame);
                        updateImageView(currentFrame, imageToShow);
                        LOGGER.log(Level.INFO, "Called thread");
                        //System.out.println("Called thread");
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // update the button content
                this.button.setText("Stop Camera");
                //message.setVisible(Boolean.FALSE);

            } else {
                // log the error
                LOGGER.log(Level.INFO, "Impossible to open the camera connection...");
                message.setText("Kindly Check the Camera Connection, And Try Again");
                //System.err.println("Impossible to open the camera connection...");
            }
        } else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.button.setText("Start Camera");
            //message.setText("Valid Image");
            //message.setVisible(Boolean.TRUE);

            // stop the timer
            this.stopAcquisition();
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Mat} to show
     */
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
                    String inputfile = prop.getProp().getProperty("inputfile");
                    //String file2 = "/usr/share/enrollment/images/input.jpg";
                    if (inputfile.isBlank() || inputfile.isEmpty() || inputfile == null) {
                        //System.out.println("The property 'inputfile' is empty, Please add it in properties");
                        LOGGER.log(Level.INFO, "The property 'inputfile' is empty, Please add it in properties");
                        return null;
                    }
                    Imgcodecs imageCodecs = new Imgcodecs();
                    imageCodecs.imwrite(inputfile, frame);
                    this.fcount++;
                    //System.out.println("Frame count="+(this.fcount ));
                    LOGGER.log(Level.INFO, (this.fcount) + "Frame count=");
                    //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                    if (this.fcount > 35) {
                        Imgproc.rectangle(frame,                    //Matrix obj of the image
                                new Point(150, 100),        //p1
                                new Point(450, 450),       //p2
                                new Scalar(0, 0, 255),     //Scalar object for color
                                5                          //Thickness of the line
                        );
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                message.setText("Move your face to fit in REDBOX");
                            }
                        });
                    }
                                        /*PythonInterpreter interpreter = new PythonInterpreter();
                                        interpreter.execfile("/home/boss/HeadPoseEstimation-WHENet/test.py");
                                        String cmd = "hello()";
                                        PyObject returnFromPython = interpreter.eval(cmd);
                                        System.out.println("Return value from Python: " + returnFromPython);
                                        */
                                        /*interpreter.exec("import sys\nsys.path.append('/home/hari/Desktop/Capture')\nimport hellojy");
                                        // execute a function that takes a string and returns a string
                                        PyObject someFunc = interpreter.get("hellofun");
                                        PyObject result = someFunc.__call__(new PyString("Hello"));
                                        String realResult = (String) result.__tojava__(String.class);
                                        System.out.println(realResult); */
                    Runtime rt = Runtime.getRuntime();
                    String cmdString;
                    Process pr;
                    if (this.fcount > 60) {
                        //String cmdString0="/home/boss/HeadPoseEstimation-WHENet/WHENet.sh";
                        //Process pr0 = rt.exec(cmdString0);
                        //String[] cmdString ={"sh" , "WHENet.sh", "/home/boss/HeadPoseEstimation-WHENet"};       //exit value 127
                        //String[] cmdString ={"sh" , "WHENet.sh"};       //exit value 2
                        //+frame.toString();\
                        //String[] cmdString ={"bash","-c","/home/boss/HeadPoseEstimation-WHENet/WHENet.sh"}; //exit value 2-working with exit value 126
                        //String cmdString ="python /home/boss/HeadPoseEstimation-WHENet/demo_video_M1.py";  //exit value 1
                        //cmdString ="python3 /usr/share/enrollment/python/cap.py";
                        cmdString = prop.getProp().getProperty("capcommand");
                        if (cmdString.isBlank() || cmdString.isEmpty() || cmdString == null) {
                            //System.out.println("The property 'capcommand' is empty, Please add it in properties");
                            LOGGER.log(Level.INFO, "The property 'capcommand' is empty, Please add it in properties");
                            return null;
                        }

                        System.out.println("cmdstr = " + cmdString);
                        pr = rt.exec(cmdString);
                        System.out.println("After rt exec");
                    } else {
                        //String cmdString0 = "python3 /usr/share/enrollment/python/webcam.py";   //working
                        //System.out.println("before cmdstr 0 ");
                        //String cmdString0[]= {"sh" ,"/usr/share/enrollment/python/face_code.sh"};
                        String cmdString0 = prop.getProp().getProperty("webcamcommand");
                        if (cmdString0.isBlank() || cmdString0.isEmpty() || cmdString0 == null) {
                            //System.out.println("The property 'webcamcommand' is empty, Please add it in properties");
                            LOGGER.log(Level.INFO, "The property 'webcamcommand' is empty, Please add it in properties");
                            return null;
                        }
                        System.out.println(cmdString0);
                        pr = rt.exec(cmdString0);
                        System.out.println("After rt exec cmdString0");
                    }


                    BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    //System.out.println("buffered input stream :"+input.readLine());
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    //System.out.println("buffered error stream "+error.readLine());
                    String line = null;
                    String eline = null;
                    while ((eline = error.readLine()) != null) {
                        System.out.println(eline);
                    }
                    while ((line = input.readLine()) != null) {
                        System.out.println(line);
                                        /*
                                        String croppedpath;
                                        if(line.contains("Path="))
                                                {
                                                croppedpath=line.substring(line.indexOf("Path=") + 6 , line.length());
                                                System.out.println("Cropped path="+croppedpath);
                                                }
                                        */
                        String croppedm;
                        if (line.contains("Message=")) {
                            if (this.fcount < 35) {
                                System.out.println("Inside Message ");
                                croppedm = line.substring(line.indexOf("Message=") + 9, line.length());
                                System.out.println("Cropped Message=" + croppedm);
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        message.setText(croppedm);

                                        if (croppedm.contains("Face Going out of frame")) {
                                            Image imageToShow_tick = new Image("/facecode/outofframecolor.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face CLOCK")) {
                                            Image imageToShow_tick = new Image("/facecode/clockcolor.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face ANTI")) {
                                            Image imageToShow_tick = new Image("/facecode/anticlockcolor.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face RIGHT")) {
                                            Image imageToShow_tick = new Image("/facecode/rightrotatecolor.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("ROTATE Face LEFT")) {
                                            Image imageToShow_tick = new Image("/facecode/leftrotatecolor.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("CHIN DOWN")) {
                                            Image imageToShow_tick = new Image("/facecode/chindowncolored.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                        if (croppedm.contains("CHIN UP")) {
                                            Image imageToShow_tick = new Image("/facecode/chinupcolor.png");
                                            updateImageView(msgicon, imageToShow_tick);
                                        }
                                    }
                                });
                                if (croppedm.contains("Single")) {
                                    this.stopAcquisition();
                                }
                            }

//                                                String filered = "/home/boss/NetBeansProjects/src/main/resources/facecode/camera.png"; 
//                                                Mat matrix_red = imageCodecs.imread(filered);
                            //Image imageToShow_tick = Utils.mat2Image(matrix_red);
                            Image imageToShow_tick = new Image("/facecode/camera.png");
                            updateImageView(iconFrame, imageToShow_tick);
                        }
                        if (line.contains("Valid")) {
                            //this.updatemessage("Valid Image
                            if (this.fcount < 60) {
                                String subfile = prop.getProp().getProperty("subfile");

                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        message.setText("Valid Image");
                                        //UPDATED 18 Oct 2022
                                        validflag = 1;
                                        System.out.println("Set validflag");
                                    }
                                });
//                                                String filetick = "/home/boss/NetBeansProjects/src/main/resources/facecode/tickgreen.jpg"; 
//                                                Mat matrix_tick = imageCodecs.imread(filetick);
//                                                Image imageToShow_tick = Utils.mat2Image(matrix_tick);
                                Image imageToShow_tick = new Image("/facecode/tickgreen.jpg");

                                updateImageView(iconFrame, imageToShow_tick);
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        showCaptureStatus.setDisable(false);
                                        showIris.setDisable(true);
                                        button.setDisable(false);
                                    }
                                });
                            } else {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        message.setText("Face cropped along RED Box. If not valid-repeat process");
                                        //message.setText("Valid Image");
                                    }
                                });
//                                                String filebrown = "/home/boss/NetBeansProjects/src/main/resources/facecode/brownquestion.jpeg"; 
//                                                Mat matrix_brown = imageCodecs.imread(filebrown);
//                                                Image imageToShow_tick = Utils.mat2Image(matrix_brown);
                                Image imageToShow_tick = new Image("/facecode/brownquestion.jpeg");
                                //Image imageToShow_tick = new Image("/facecode/tickgreen.jpg");
                                updateImageView(iconFrame, imageToShow_tick);
                            }
                            this.setClosed();
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    cameraActive = false;

                                    button.setText("Start Camera");
                                }
                            });
                        }
                    }
                    //System.out.println("Aftere webcam.py");
                    System.out.println("</OUTPUT>");
                    int exitVal = pr.waitFor();
                    //System.out.println("Process exitValue: " + exitVal);
                    LOGGER.log(Level.INFO, exitVal + "Process exitValue: ");
                    if (exitVal == 0) {
                        //String file1 = "/usr/share/enrollment/croppedimg/out.png";
                        //Commented on 191022
                        String subfile = prop.getProp().getProperty("subfile");
                        String outputfile = prop.getProp().getProperty("outputfile");

                        if (outputfile.isBlank() || outputfile.isEmpty() || outputfile == null) {
                            // System.out.println("The property 'outputfile' is empty, Please add it in properties");
                            LOGGER.log(Level.INFO, "The property 'outputfile' is empty, Please add it in properties");
                            return null;
                        }     //Commented on 191022
                        //System.out.println("Process exitValue if : " + exitVal);
                        LOGGER.log(Level.INFO, exitVal + ": Process exitValue if ");
                        Mat matrix_cropped = null;
                        if (validflag == 1) {
                            System.out.println("Subfile" + subfile);
                            File file = new File(subfile);
                            Image image = new Image(file.toURI().toString());
                            croppedFrame.setImage(image);
                            //camslider.setVisible(true);
                            //brightness.setVisible(true);
                            //adjustbrightness();
                        } else {
                            //UPDATED 18 Oct 2022
                            //matrix_cropped = imageCodecs.imread(subfile,4);
                            //Image imageToShow_crop = Utils.mat2Image(matrix_cropped);
                            //updateImageView(croppedFrame, imageToShow_crop);
                            File file = new File(outputfile);
                            Image image = new Image(file.toURI().toString());
                            croppedFrame.setImage(image);
                            System.out.println("Outfile");
                        }

                        //message.setText("Valid Image");
                    }

                    input.close();
                    error.close();
                }

            } catch (Exception e) {
                // log the error

                //System.err.println("Exception during the image elaboration: " + e);
                LOGGER.log(Level.INFO, e.toString() + ": Exception during the image elaboration ");
            }
        }

        return frame;
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
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
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed() {
        this.stopAcquisition();
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
    public void showCaptureStatus() {
        try {
            App.setRoot("capturecomplete");
        } catch (IOException ex) {
            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
   /* 
    @FXML
    private void showCaptureStatus() throws IOException, FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
         System.out.println("show Capture Status ::");
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails a= holder.getARC();
        SaveEnrollmentDetails saveEnrollment = holder.getenrollmentDetails();
        System.out.println("details : " + saveEnrollment.getArcNo());
        String file1 = "/usr/share/enrollment/croppedimg/out.png"; 
        File outFile = new File(file1);
        if(outFile.exists()) {
            FileInputStream outputFile=new FileInputStream(file1);
            String fileCompressed = "/usr/share/enrollment/croppedimg/compressed.png"; 
            File compressedFile = new File(fileCompressed);
            if(compressedFile.exists()) {
                FileInputStream outputFileCompressed=new FileInputStream(fileCompressed);
                this.finalBase64Img = Base64.getEncoder().encodeToString(outputFile.readAllBytes());
                saveEnrollment.setPhoto(finalBase64Img);
                saveEnrollment.setPhotoCompressed(Base64.getEncoder().encodeToString(outputFileCompressed.readAllBytes()));
                outputFileCompressed.close();
            }
            else {

                System.out.println("Problem reading out  file.");
            }
            //this.finalBase64Img = Base64.getEncoder().encodeToString(outputFile.readAllBytes());
            outputFile.close();
            
        }
        else {

            System.out.println("Problem reading compressed file.");
        }
        try (BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"))) {
            String line = " ";
            String input = " ";
            while((line = file.readLine()) != null){
                String[] tokens = line.split(",");
                saveEnrollment.setEnrollmentStationID(tokens[2]);
                saveEnrollment.setEnrollmentStationUnitID(tokens[0]);

            }
            file.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Problem reading file.");
                

            }
        
//        saveEnrollment.setEnrollmentStationID("StationID");
//        saveEnrollment.setEnrollmentStationUnitID("UnitID");
        saveEnrollment.setEnrollmentStatus("SUCCESS");
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        saveEnrollment.setEnrollmentDate(formatter.format(date));
        saveEnrollment.setArcStatus(a.getArcstatus());
        saveEnrollment.setUniqueID(a.getApplicantID());//For ApplicantID
        
        holder.setEnrollmentDetails(saveEnrollment);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
        
        
        String postJson;
            try {
                postJson = mapper.writeValueAsString(saveEnrollment);
           
       
                String connurl = apiServerCheck.getARCURL();
                String arcno = "123abc"; 
                String connectionStatus = apiServerCheck.checkGetARCNoAPI(connurl,arcno);        
                System.out.println("connection status :"+connectionStatus);
                if(!connectionStatus.contentEquals("connected")) {
                    String oirgjsonfile = "/usr/share/enrollment/json/export/orig/"+saveEnrollment.getArcNo()+".json";
                    String encryptedjsonfile = "/usr/share/enrollment/json/export/enc/"+saveEnrollment.getArcNo()+".json.enc";
                    String decryptedjsonfile = "/usr/share/enrollment/json/export/dec/"+saveEnrollment.getArcNo()+".json";
                    System.out.println("post json final :"+ postJson);
                    File json = new File(oirgjsonfile);
                    FileOutputStream output = new FileOutputStream(json);
                    output.write(postJson.getBytes());
                    output.close();
            
                //Encrypt the Orig Json File
            
                AESFileEncryptionDecryption aesencryptfile = new AESFileEncryptionDecryption();
                String status = "";
                        try {
                            status = aesencryptfile.encryptFile(oirgjsonfile, encryptedjsonfile);
                            ARCDetails arcDetail = new ARCDetails();            
                            holder.setARC(arcDetail);
                            if(status.equals("Success")){
                                aesencryptfile.delFile(oirgjsonfile);
                                arcDetail.setDesc("Details Encrypted and saved locally");
                                System.out.println("Encrypted Sucessfully:::Original file deleted");
                            } else {
                                arcDetail.setDesc("Please Try Again...");
                                System.out.println("Please Try Again...");                            
                            }                    

                        } catch (InvalidKeySpecException ex) {
                            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);                            
                        } catch (InvalidAlgorithmParameterException ex) {
                            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                        }
            
                    // Uncomment and Enable this when PKI encryption support is provided locally,
                    /*
                      PKIUtil pkiUtil = new PKIUtil();
                      // If the network is not connected, Encrypt JSON using PKI private key and save locally
                      String encryptedJSON = pkiUtil.encrypt(postJson);
                      File json = new File("/usr/share/enrollment/json/"+saveEnrollment.getArcNo());
                      FileOutputStream output = new FileOutputStream(json);
                      output.write(encryptedJSON.getBytes());
                      output.close();*/
      /*      
            App.setRoot("capturecomplete");
        } 
        else {        
        try{
            String decResponse = "";
            connurl = apiServerCheck.getEnrollmentSaveURL();
            decResponse = apiServerCheck.getEnrollmentSaveAPI(connurl, postJson);
            System.out.println("dec response : "+ decResponse);           

            ObjectMapper objMapper = new ObjectMapper();
            saveEnrollmentResponse = objMapper.readValue(decResponse.toString(), saveEnrollmentResponse.class);
            System.out.println(" save enrollment : "+saveEnrollmentResponse.toString());
            //ARCDetails arcDetail = new ARCDetails();
             a.setDesc(saveEnrollmentResponse.getDesc());
             holder.setARC(a);
             System.out.println("ARC details :" +a.toString());
             App.setRoot("capturecomplete");

            }
            catch(Exception e)
            {
                System.out.println("Exception block"+e);               
            }
            }
                } catch (JsonProcessingException ex) {
                   // Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                   System.out.println("JSON Exception block"+ex);      
                }

          //  App.setRoot("capturecomplete");
        } */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LOGGER.log(Level.INFO, "Reached Camera page");
        showCaptureStatus.setDisable(true);
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails a = holder.getArcDetails();
        labelarccam.setText("ARC: " + a.getArcNo());
        camslider.setVisible(false);
        brightness.setVisible(false);
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
          /*
              // For Biometric Options
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
          
            if(holder.getARC().getBiometricoptions().contains("Photo")){  
                    confirmPane.setVisible(false); 
                    showIris.setDisable(false);
                    showCaptureStatus.setDisable(true);
                    message.setText("Biometric Options contains Photo, Cannot Go Back");
                   
            }else{
                        try {
                   App.setRoot("iris");
               } catch (IOException ex) {
                   Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                   LOGGER.log(Level.INFO, ex + "IOException");
               }
            }
          /*
              // For Biometric Options
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
          
            if(holder.getARC().getBiometricoptions().contains("Photo")){  
                    confirmPane.setVisible(false); 
                    showIris.setDisable(false);
                    showCaptureStatus.setDisable(true);
                    message.setText("Biometric Options contains Photo, Cannot Go Back");
                   
            }else{
                        try {
                   App.setRoot("iris");
               } catch (IOException ex) {
                   Logger.getLogger(FXHelloCVController_bak.class.getName()).log(Level.SEVERE, null, ex);
                   LOGGER.log(Level.INFO, ex + "IOException");
               }
            }
          /*
              // For Biometric Options
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
          
            if(holder.getARC().getBiometricoptions().contains("Photo")){  
                    confirmPane.setVisible(false); 
                    showIris.setDisable(false);
                    showCaptureStatus.setDisable(true);
                    message.setText("Biometric Options contains Photo, Cannot Go Back");
                   
            }else{
                        try {
                   App.setRoot("iris");
               } catch (IOException ex) {
                   Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                   LOGGER.log(Level.INFO, ex + "IOException");
               }
            }
          /*
              // For Biometric Options
            ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
          
            if(holder.getARC().getBiometricoptions().contains("Photo")){  
                    confirmPane.setVisible(false); 
                    showIris.setDisable(false);
                    showCaptureStatus.setDisable(true);
                    message.setText("Biometric Options contains Photo, Cannot Go Back");
                   
            }else{
                        try {
                   App.setRoot("iris");
               } catch (IOException ex) {
                   Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                   LOGGER.log(Level.INFO, ex + "IOException");
               }
            }
            */
    }

    @FXML
    private void stayBack() {
        System.out.println("inside stay back");
        //backBtn.setDisable(false);
        confirmPane.setVisible(false);

        showIris.setDisable(false);
        showCaptureStatus.setDisable(true);

    }

    /*
    private void adjustbrightness(){
        
        try {
            System.out.println("905");
                String subfile = prop.getProp().getProperty("subfile");
                File file = new File(subfile);
                RescaleOp rescaleOp = new RescaleOp(1.2f, 15, null);
                BufferedImage bimag = ImageIO.read(file);
                rescaleOp.filter(bimag, bimag);  
                ImageIO.write(bimag, subfile, file);
                Image image = new Image(file.toURI().toString());
                croppedFrame.setImage(image);
                System.out.println("914");
            } catch (Exception ex) {
                Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("918"+ex);
            }
    }*/
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
            String subfile = prop.getProp().getProperty("subfile");
            File file = new File(subfile);
            RescaleOp rescaleOp = new RescaleOp(Float.valueOf(sliderdec), 15 * Float.valueOf(sliderdec), null);
            BufferedImage bimag = ImageIO.read(file);
            rescaleOp.filter(bimag, bimag);
            ImageIO.write(bimag, subfile, file);
            Image image = new Image(file.toURI().toString());
            croppedFrame.setImage(image);
            System.out.println("914");
        } catch (Exception ex) {
            Logger.getLogger(FXHelloCVController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("918" + ex);
        }

    }


}
