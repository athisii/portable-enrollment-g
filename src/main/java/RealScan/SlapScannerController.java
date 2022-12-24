package RealScan;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsHolder;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;
import com.cdac.enrollmentstation.service.ObjectReaderWriter;
import com.cdac.enrollmentstation.util.TestProp;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.innovatrics.commons.img.RawGrayscaleImage;
import com.innovatrics.iengine.ansiiso.AnsiIso;
import com.innovatrics.iengine.ansiiso.AnsiIsoException;
import com.innovatrics.iengine.ansiiso.AnsiIsoImageFormatEnum;
import com.innovatrics.iengine.ansiiso.IEngineTemplateFormat;
import com.mantra.IMAGE_FORMAT;
import com.mantra.Utility;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlapScannerController implements Initializable {
    StringBuffer deviceName;
    javafx.scene.image.Image img = null;

    private final Object syncImg = new Object();

    Set<FP> fingerPrintSet = new HashSet<>();

    List<Integer> captureModesCompleted = new ArrayList<Integer>();

    // SaveEnrollmentDetails saveEnrollment;

    @FXML
    public Label statusField;

    @FXML
    public Label displayarclabel;

    @FXML
    public ImageView imageLeft;

    @FXML
    public ImageView imageRight;

    @FXML
    public ImageView imageThumb;


    @FXML
    public ImageView preview;


    @FXML
    public ImageView LTCanvas;

    @FXML
    public ImageView LICanvas;

    @FXML
    public ImageView LMCanvas;

    @FXML
    public ImageView LRCanvas;

    @FXML
    public ImageView LLCanvas;

    @FXML
    public ImageView RTCanvas;

    @FXML
    public ImageView RICanvas;

    @FXML
    public ImageView RMCanvas;

    @FXML
    public ImageView RRCanvas;

    @FXML
    public ImageView RLCanvas;

    @FXML
    public Button showIrisBtn;

    @FXML
    public Button backBtn;

    @FXML
    public Button scan;

    @FXML
    public Button leftScan;

    @FXML
    public Button rightScan;

    @FXML
    public Button thumbScan;

    @FXML
    public AnchorPane confirmPane;

    @FXML
    public Button confirmYesBtn;

    @FXML
    public Button confirmNoBtn;

    TestProp prop = new TestProp();

    public List seqTargetList = new java.util.Vector<>();

    public ARCDetails arcDetails;

    int leftmissingfingers = 0;

    int rightmissingfingers = 0;

    int thumbmissingfingers = 0;

    String whichdevice;

    String[] clearImagePanel = {"", "", ""};


    //EnrollmentDetailsHolder enrollmentDetailsHolder = null;

    public SlapScannerController() {
        // this.enrollmentDetailsHolder = new EnrollmentDetailsHolder();
        // this.handler = appLog.getLogger();
    }

    Thread t = null;

    Thread tRight = null;

    Thread tThumb = null;

    Thread ttemplateconvert = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        //LOGGER.addHandler(handler);
        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        ARCDetails a = holder.getArcDetails();
        displayarclabel.setText("ARC: " + a.getArcNo());
        for (int i = 0; i < segFPImages.length; i++) {
            segFPImages[i] = new segFP();
            segFPImages[i].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
            segFPImages[i].image = null;
            segFPImages[i].rawdata = null;
            segFPImages[i].width = 0;
            segFPImages[i].height = 0;
        }
        showIrisBtn.setDisable(true);

        try {
            initISOSDK();
//            System.out.println("");
            //To change body of generated methods, choose Tools | Templates.
        } catch (IOException ex) {
            Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public class segFP {
        //public int slapType;
        public int fingerPosition;
        public javafx.scene.image.Image image;
        public byte[] rawdata;
        public int width;
        public int height;
    }

    @FXML
    public AnchorPane AnchorPane;

    segFP[] segFPImages = new segFP[10];

    String[] pFingerMsg = {"None", "Right Thumb", "Right Index", "Right Middle", "Right Ring", "Right Little", "Left Thumb", "Left Index", "Left Middle", "Left Ring", "Left Little"};

    int numOfDevice;
    int deviceHandle;

    //int deviceHandle1;
    Boolean isDeviceInitialized = false;
    //byte[] imageBuffer = new byte[2250 * 2250 + 1078];

    List selectDevice = new ArrayList();
    int[] modeLedTypes = new int[]{RealScan_JNI.RS_LED_MODE_ALL, RealScan_JNI.RS_LED_MODE_LEFT_FINGER4, RealScan_JNI.RS_LED_MODE_RIGHT_FINGER4, RealScan_JNI.RS_LED_MODE_TWO_THUMB, RealScan_JNI.RS_LED_MODE_ROLL, RealScan_JNI.RS_LED_POWER};
    int[] fingerLedTypes = new int[]{RealScan_JNI.RS_FINGER_ALL, RealScan_JNI.RS_FINGER_LEFT_LITTLE, RealScan_JNI.RS_FINGER_LEFT_RING, RealScan_JNI.RS_FINGER_LEFT_MIDDLE, RealScan_JNI.RS_FINGER_LEFT_INDEX, RealScan_JNI.RS_FINGER_LEFT_THUMB, RealScan_JNI.RS_FINGER_RIGHT_THUMB, RealScan_JNI.RS_FINGER_RIGHT_INDEX, RealScan_JNI.RS_FINGER_RIGHT_MIDDLE, RealScan_JNI.RS_FINGER_RIGHT_RING, RealScan_JNI.RS_FINGER_RIGHT_LITTLE, RealScan_JNI.RS_FINGER_TWO_THUMB, RealScan_JNI.RS_FINGER_LEFT_FOUR, RealScan_JNI.RS_FINGER_RIGHT_FOUR};
    int[] ledColors = new int[]{RealScan_JNI.RS_LED_GREEN, RealScan_JNI.RS_LED_RED, RealScan_JNI.RS_LED_YELLOW};
    int[] beepTypes = new int[]{RealScan_JNI.RS_BEEP_PATTERN_NONE, RealScan_JNI.RS_BEEP_PATTERN_1, RealScan_JNI.RS_BEEP_PATTERN_2};
    int[] rollProfileOptions = new int[]{RealScan_JNI.RS_ROLL_PROFILE_LOW, RealScan_JNI.RS_ROLL_PROFILE_NORMAL, RealScan_JNI.RS_ROLL_PROFILE_HIGH};
    int[] rollDirectionOptions = new int[]{RealScan_JNI.RS_ROLL_DIR_L2R, RealScan_JNI.RS_ROLL_DIR_R2L, RealScan_JNI.RS_ROLL_DIR_AUTO, RealScan_JNI.RS_ROLL_DIR_AUTO_M};
    int[] sensitivityOptions = new int[]{RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, RealScan_JNI.RS_AUTO_SENSITIVITY_HIGH, RealScan_JNI.RS_AUTO_SENSITIVITY_HIGHER, RealScan_JNI.RS_AUTO_SENSITIVITY_DISABLED};
    int[] fontSizeOptions = new int[]{8, 10, 12, 14, 16, 18, 20, 24, 28};
    long[] overlayColorOptions = new long[]{0x00000000, 0x000000ff, 0x0000ff00, 0x00ff0000};
    int[] keyCodeOptions = new int[]{RealScan_JNI.RS_REALSCANF_NO_KEY, RealScan_JNI.RS_REALSCANF_UP_KEY, RealScan_JNI.RS_REALSCANF_DOWN_KEY, RealScan_JNI.RS_REALSCANF_LEFT_KEY, RealScan_JNI.RS_REALSCANF_RIGHT_KEY, RealScan_JNI.RS_REALSCANF_PLAY_KEY, RealScan_JNI.RS_REALSCANF_STOP_KEY, RealScan_JNI.RS_REALSCANF_FOOTSWITCH, RealScan_JNI.RS_REALSCANF_ALL_KEYS};

    int[] captureModes = new int[]{RealScan_JNI.RS_CAPTURE_DISABLED, RealScan_JNI.RS_CAPTURE_ROLL_FINGER, RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER, RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS, RealScan_JNI.RS_CAPTURE_FLAT_LEFT_FOUR_FINGERS, RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_FOUR_FINGERS, RealScan_JNI.RS_CAPTURE_FLAT_LEFT_PALM, RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_PALM, RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER_EX, RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS_EX, RealScan_JNI.RS_CAPTURE_FLAT_LEFT_SIDE_PALM, RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_SIDE_PALM, RealScan_JNI.RS_CAPTURE_FLAT_LEFT_WRITERS_PALM, RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_WRITERS_PALM, RealScan_JNI.RS_CAPTURE_FLAT_MANUAL};
    int[] captureDirections = new int[]{RealScan_JNI.RS_CAPTURE_DIRECTION_DEFAULT, RealScan_JNI.RS_CAPTURE_DIRECTION_LEFT, RealScan_JNI.RS_CAPTURE_DIRECTION_RIGHT};

    boolean bIsCaptureModeSelected = false;
    int nCaptureMode = -1;

    int nCustomCaptX;
    int nCustomCaptY;
    int nCustomCaptWidth;
    int nCustomCaptHeight;
    int keyCode;
    int nFingerCount = 0;
    int nSlapType = 0;

    byte[][] bSeqCheckTargetImages = new byte[5][];
    int[] nSeqCheckTargetWidths = new int[5];
    int[] nSeqCheckTargetHeights = new int[5];
    int[] nSeqCheckTargetSlapTypes = new int[5];
    int nNumOfTargetFingers = 0;
    int nNumOfTargets;
    Vector listData = new Vector();

    boolean bIsPrevStarted = false;

    enum PrevMode {
        directDraw, prevCallbackDraw, advPrevCallbackDraw,
    }

    PrevMode _selectedPrevMode;

    AnsiIso ansiISO = new AnsiIso();

    ApplicationLog appLog = new ApplicationLog();
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    Handler handler;


    public void statusMsg(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //System.out.println("Capture Status Called...");
                //LOGGER.log(Level.INFO,"Capture Status Called...");
                statusField.setText(message);

            }
        });
    }


    Runnable DoTemplateConversion = new Runnable() {
        @Override
        public void run() {
            try {
                statusMsg("Please Wait...");
                System.out.println("Inside show Iris function");
                int result = RealScan_JNI.RS_SUCCESS;
                RealScan_JNI.RSDeviceInfo deviceInfoT = new RealScan_JNI.RSDeviceInfo();

                result = RealScan_JNI.RS_GetDeviceInfo(deviceHandle, deviceInfoT);
                if (result != RealScan_JNI.RS_SUCCESS) {
                    String errStr = RealScan_JNI.RS_GetErrString(result);
                    //statusField.setText(errStr);
                    statusMsg(errStr);
                    return;
                }

                RealScan_JNI.RSDeviceInfo deviceInfoT1 = new RealScan_JNI.RSDeviceInfo();
        
        /*result = RealScan_JNI.RS_GetDeviceInfo(deviceHandle1, deviceInfoT1);
        if (result != RealScan_JNI.RS_SUCCESS) {
            String errStr = RealScan_JNI.RS_GetErrString(result);
            statusField.setText(errStr);            
            return;
        }*/

                RealScan_JNI.RS_ExitDevice(deviceHandle);
                //RealScan_JNI.RS_ExitDevice(deviceHandle1);

                // statusField.setText("The device is disconnected successfully");
                ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
                ARCDetails a = holder.getArcDetails();
                SaveEnrollmentDetails saveEnrollment = holder.getSaveEnrollmentDetails();
                saveEnrollment.setLeftFPScannerSerialNo(deviceInfoT.deviceID);
                saveEnrollment.setRightFPScannerSerialNo(deviceInfoT.deviceID);

                //saveEnrollment.setRightFPScannerSerailNo("Not Available");
                System.out.println("details : " + saveEnrollment.getArcNo());
                if (segFPImages.length < 10) {
                    //statusField.setText("Error while segmenting, Please try again");
                    statusMsg("Error while segmenting, Please try again");
                    LOGGER.log(Level.SEVERE, "Error while segmenting fingers, Please try again");
                    scan.setDisable(false);
                    backBtn.setDisable(false);
                    return;
                }
                System.out.println("Segment Images Length:::" + segFPImages.length);
                fingerPrintSet.clear();//for clearing fingerprintset
                for (int i = 0; i < segFPImages.length; i++) {
                    // segFPImages[i] = new segFP();
                    //segFPImages[i].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
                    System.out.println("raw data " + i + " " + segFPImages[i].rawdata + "  " + segFPImages[i].fingerPosition);
                    if (segFPImages[i].rawdata != null) {
                        RawGrayscaleImage rawGreyScaleImg = new RawGrayscaleImage(segFPImages[i].width, segFPImages[i].height, segFPImages[i].rawdata);


                        try {
                            //Create Template(FMR)
                            //String ansiVersion = ansiISO.getVersionString();
                            //System.out.println("ANSI Version:"+ansiVersion);
                            //byte[] imgTemplate = ansiISO.isoCreateTemplate(rawGreyScaleImg);
                            //byte[] isoTemplate = ansiISO.ansiConvertToISO(imgTemplate);
                            byte[] isoTemplate = ansiISO.isoCreateTemplate(rawGreyScaleImg);
                            byte[] isoTemplate2011 = ansiISO.iengineConvertTemplate(IEngineTemplateFormat.ISO_TEMPLATE, isoTemplate, IEngineTemplateFormat.ISO_TEMPLATE_V30);

                            //Create Image(FIR)
                            byte[] image = ansiISO.convertRawToImage(rawGreyScaleImg, AnsiIsoImageFormatEnum.JPEG2K);
                            byte[] data = image;

                            byte[] outImage = new byte[data.length + 2000];
                            int[] outImageLen = new int[1];
                            outImageLen[0] = data.length + 2000;

                            int ret = Utility.ImageConvert(data, data.length, outImage, outImageLen, IMAGE_FORMAT.IENGINE_FORMAT_JPEG2K_TO_FIR_JPEG2000_V2005.getValue(), 354, 296);
                            if (ret == Utility.UNKNOWN_ERROR) {
                                //statusField.setText("UNKNOWN_ERROR DURING IMAGE CONVERSION");
                                statusMsg("UNKNOWN_ERROR DURING IMAGE CONVERSION");
                                return;
                            } else if (ret == Utility.UNSUPPORTED_IMAGE_FORMAT) {
                                //statusField.setText("UNSUPPORTED_IMAGE_FORMAT DURING IMAGE CONVERSION");
                                statusMsg("UNSUPPORTED_IMAGE_FORMAT DURING IMAGE CONVERSION");
                                return;
                            } else if (ret == Utility.OBJECT_CANNOT_BE_NULL_OR_EMPTY) {
                                //statusField.setText("OBJECT_CANNOT_BE_NULL_OR_EMPTY DURING IMAGE CONVERSION");
                                statusMsg("OBJECT_CANNOT_BE_NULL_OR_EMPTY DURING IMAGE CONVERSION");
                                return;
                            }
                            System.out.println("ImageConvert Ret: " + ret);
                            if (ret == 0) {
                                byte[] finalOutData = new byte[outImageLen[0]];
                                System.arraycopy(outImage, 0, finalOutData, 0, outImageLen[0]);
                        /*
                        System.out.println("rawGreyScaleImg:"+i+":"+rawGreyScaleImg);
                        System.out.println("isoTemplate2011:"+i+":"+isoTemplate2011);
                        //System.out.println("imgTemplate:"+i+":"+imgTemplate);
                        
                        //Printing isoTemplate and imgTemplate 
                        OutputStream os = new FileOutputStream("/home/boss/fingerprint/isoTemplate2011-"+i);
                       // Starts writing the bytes in it
                        os.write(isoTemplate2011);                  
                        OutputStream os1 = new FileOutputStream("/home/boss/fingerprint/outImage-"+i);  
                        // Starts writing the bytes in it
                        os1.write(outImage);
                        
                        //OutputStream os2 = new FileOutputStream("/home/boss/fingerprint/image-"+i);  
                        // Starts writing the bytes in it
                        //os2.write(image);
                        
                        System.out.println("Successfully"+ " byte inserted");
                        InputStream in = new ByteArrayInputStream(isoTemplate2011);
                        System.out.println("isoTemplate2011::"+Base64.getEncoder().encodeToString(isoTemplate2011));
                        //System.out.println("imgTemplate::"+Base64.getEncoder().encodeToString(imgTemplate));
                        */

                                // System.out.println("BUFFFF"+bufferedImage.getData());
                                //saveEnrollmentDetails(saveEnrollment,segFPImages[i].fingerPosition, isoTemplate2011, imgTemplate);
                                // FingerPosition , Image Format ( jpeg 2000 ), IsoTemplate2011
                                //System.err.println("Finger Position::::"+segFPImages[i].fingerPosition);


                                saveEnrollmentDetails(saveEnrollment, segFPImages[i].fingerPosition, outImage, isoTemplate2011);
                                // WriteImageFile("jpeg_2005_fir.bin", finalOutData);
                            }
                        } catch (AnsiIsoException e) {
                            //statusField.setText("Fingerprint quality check failed. Please retry again");
                            statusMsg("Fingerprint quality check failed. Please retry again");
                            LOGGER.log(Level.SEVERE, "Fingerprint quality check failed and ISO create failed with exception ");
                            scan.setDisable(false);
                            backBtn.setDisable(false);
                            return;
                        }
                    }

                    //To be uncommented
                    //saveEnrollmentDetails(saveEnrollment,segFPImages[i].fingerPosition, segFPImages[i].rawdata, isoTemplate);

                }
                System.out.println("arc no in slap :" + saveEnrollment.getArcNo());
                System.out.println("Missing fingers count:" + arcDetails.getFingers().size() + " -> " + arcDetails.getFingers().toString());

                System.out.println("No., of FINGER PRINTS Available after Scan::" + fingerPrintSet.size());

                int totalfingers = 10 - arcDetails.getFingers().size();
                System.out.println("Fingers to be captured (total - missing) " + totalfingers);

                if (totalfingers == 0 && fingerPrintSet.size() == 0) {
                    LOGGER.log(Level.INFO, "No Fingerprint to scan, proceed to IRIS");
                    //statusField.setText("No Fingerprint to scan, proceed to IRIS ");
                    statusMsg("No Fingerprint to scan, proceed to IRIS ");
                } else if (fingerPrintSet.size() < totalfingers) {
                    //statusField.setText("Fingers Quality Issue, Please Capture Again");
                    statusMsg("Fingers Quality Issue, Please Capture Again");
                    LOGGER.log(Level.SEVERE, "fingers captured are less than fingers needed to be captured");
                    scan.setDisable(false);
                    backBtn.setDisable(false);
                    return;
                } else if (fingerPrintSet.size() > totalfingers) {
                    //statusField.setText("Fingers Quality Issue, Please Capture Again");
                    statusMsg("Fingers Quality Issue, Please Capture Again");
                    LOGGER.log(Level.SEVERE, "fingers captured are more than fingers needed to be captured" + fingerPrintSet.size() + " " + totalfingers);
                    scan.setDisable(false);
                    backBtn.setDisable(false);
                    return;
                }

                System.err.println("\nPrinting Finger-print set iteratively\n============================================");
                int k = 1;
                for (FP s : fingerPrintSet) {
                    System.out.println(k + ". FingerPrintSET :" + s.toString());
                    k += 1;
                }

                saveEnrollment.setFp(fingerPrintSet);
                //saveEnrollment.setEnrollmentStatus("FingerPrint Capture Completed");
                saveEnrollment.setEnrollmentStatus("FingerPrintCompleted");
                System.out.println("Finger print capture completed");

                holder.setSaveEnrollmentDetails(saveEnrollment);
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);

                //Enable Capture Iris
                statusMsg("Proceed Next to Capture IRIS");

                showIrisBtn.setDisable(false);
                scan.setDisable(false);


                //Enable Capture Iris
                String postJson;
                try {
                    postJson = mapper.writeValueAsString(saveEnrollment);
                    //Code Added by K. Karthikeyan - 18-4-22 - Start
                    ObjectReaderWriter objReadWrite = new ObjectReaderWriter();
                    objReadWrite.writer(saveEnrollment);
                    System.out.println("Save Enrollment Object write");
                    SaveEnrollmentDetails s = objReadWrite.reader();
                    System.out.println("Enrollment Status " + s.getEnrollmentStatus());
                    //Code Added by K. Karthikeyan - 18-4-22 - Finish
                    //     System.out.println("post json slap :"+ postJson);
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
                    //statusField.setText("Fetched Details From Server has Error");
                    statusMsg("Fetched Details From Server has Error");
                }


            } catch (Exception ex) {
                //statusField.setText("Fingerprint quality check failed. Please retry again");
                statusMsg("Fingerprint quality check failed. Please retry again");
                LOGGER.log(Level.SEVERE, "Fingerprint quality check and ISO create failed with exception ");
                ex.printStackTrace();
                thumbScan.setDisable(false);
                backBtn.setDisable(false);
                showIrisBtn.setDisable(true);
                return;
            }
        }
    };

    @FXML
    private void showIris() throws IOException {
        App.setRoot("iris");
    }


//    @FXML
//    private void showIris() throws IOException {
//          
//        try {
//        System.out.println("Inside show Iris function");
//        int result = RealScan_JNI.RS_SUCCESS;
//        RealScan_JNI.RSDeviceInfo deviceInfoT = new RealScan_JNI.RSDeviceInfo();
//        
//        result = RealScan_JNI.RS_GetDeviceInfo(deviceHandle, deviceInfoT);
//        if (result != RealScan_JNI.RS_SUCCESS) {
//            String errStr = RealScan_JNI.RS_GetErrString(result);
//            statusField.setText(errStr);                     
//            return;
//        }
//        
//        RealScan_JNI.RSDeviceInfo deviceInfoT1 = new RealScan_JNI.RSDeviceInfo();
//        
//        /*result = RealScan_JNI.RS_GetDeviceInfo(deviceHandle1, deviceInfoT1);
//        if (result != RealScan_JNI.RS_SUCCESS) {
//            String errStr = RealScan_JNI.RS_GetErrString(result);
//            statusField.setText(errStr);            
//            return;
//        }*/
//      
//      
//        RealScan_JNI.RS_ExitDevice(deviceHandle);
//        //RealScan_JNI.RS_ExitDevice(deviceHandle1);
//        
//        // statusField.setText("The device is disconnected successfully");
//        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
//        ARCDetails a= holder.getARC();
//        SaveEnrollmentDetails saveEnrollment = holder.getenrollmentDetails();
//        saveEnrollment.setLeftFPScannerSerailNo(deviceInfoT.deviceID);
//        saveEnrollment.setRightFPScannerSerailNo(deviceInfoT.deviceID);
//        
//        //saveEnrollment.setRightFPScannerSerailNo("Not Available");
//        System.out.println("details : " + saveEnrollment.getArcNo());
//        if(segFPImages.length < 10) {
//            statusField.setText("Error while segmenting, Please try again");
//            LOGGER.log(Level.SEVERE, "Error while segmenting fingers, Please try again");
//            scan.setDisable(false);
//            backBtn.setDisable(false);
//            return;
//        }
//        System.out.println("Segment Images Length:::"+segFPImages.length);
//        fingerPrintSet.clear();//for clearing fingerprintset
//        for (int i = 0; i < segFPImages.length; i++) {
//           // segFPImages[i] = new segFP();
//            //segFPImages[i].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
//            System.out.println("raw data "+ i + " "+segFPImages[i].rawdata +"  " + segFPImages[i].fingerPosition );
//            if(segFPImages[i].rawdata !=null) {
//                RawGrayscaleImage rawGreyScaleImg = new RawGrayscaleImage(segFPImages[i].width,segFPImages[i].height, segFPImages[i].rawdata);
//
//                System.out.println("RAW Scale Image::: "+rawGreyScaleImg);
//                try {
//                    //Create Template(FMR)
//                    byte[] imgTemplate = ansiISO.isoCreateTemplate(rawGreyScaleImg);                                      
//                    byte[] isoTemplate = ansiISO.ansiConvertToISO(imgTemplate);
//                    byte[] isoTemplate2011 = ansiISO.iengineConvertTemplate(IEngineTemplateFormat.ISO_TEMPLATE, isoTemplate, IEngineTemplateFormat.ISO_TEMPLATE_V30);
//                    
//                    //Create Image(FIR)
//                    byte[] image = ansiISO.convertRawToImage(rawGreyScaleImg, AnsiIsoImageFormatEnum.JPEG2K);
//                    byte[] data = image;
//
//                    byte[] outImage = new byte[data.length + 2000];
//                    int[] outImageLen = new int[1];
//                    outImageLen[0] = data.length + 2000;
//
//                    int ret = Utility.ImageConvert(data, data.length, outImage, outImageLen, IMAGE_FORMAT.IENGINE_FORMAT_JPEG2K_TO_FIR_JPEG2000_V2005.getValue(), 354, 296);
//                    if(ret==Utility.UNKNOWN_ERROR) { 
//                        statusField.setText("UNKNOWN_ERROR DURING IMAGE CONVERSION");
//                        return;
//                    }
//                    else if(ret==Utility.UNSUPPORTED_IMAGE_FORMAT) {
//                        statusField.setText("UNSUPPORTED_IMAGE_FORMAT DURING IMAGE CONVERSION");
//                        return;
//                    }
//                    else if(ret==Utility.OBJECT_CANNOT_BE_NULL_OR_EMPTY) {
//                        statusField.setText("OBJECT_CANNOT_BE_NULL_OR_EMPTY DURING IMAGE CONVERSION");
//                        return;
//                    }
//                    System.out.println("ImageConvert Ret: " + ret);
//                    if (ret == 0) {
//                        byte[] finalOutData = new byte[outImageLen[0]];
//                        System.arraycopy(outImage, 0, finalOutData, 0, outImageLen[0]);
//                        /*
//                        System.out.println("rawGreyScaleImg:"+i+":"+rawGreyScaleImg);
//                        System.out.println("isoTemplate2011:"+i+":"+isoTemplate2011);
//                        System.out.println("imgTemplate:"+i+":"+imgTemplate);
//                        
//                        //Printing isoTemplate and imgTemplate 
//                        OutputStream os = new FileOutputStream("/home/boss/isoTemplate2011-"+i);
//                       // Starts writing the bytes in it
//                        os.write(isoTemplate2011);                  
//                        OutputStream os1 = new FileOutputStream("/home/boss/outImage-"+i);  
//                        // Starts writing the bytes in it
//                        os1.write(outImage);
//                        
//                        OutputStream os2 = new FileOutputStream("/home/boss/image-"+i);  
//                        // Starts writing the bytes in it
//                        os2.write(image);
//                        
//                        System.out.println("Successfully"+ " byte inserted");
//                        InputStream in = new ByteArrayInputStream(isoTemplate2011);
//                        System.out.println("isoTemplate2011::"+Base64.getEncoder().encodeToString(isoTemplate2011));
//                        System.out.println("imgTemplate::"+Base64.getEncoder().encodeToString(imgTemplate));
//                        */
//                       
//                   // System.out.println("BUFFFF"+bufferedImage.getData());
//                        //saveEnrollmentDetails(saveEnrollment,segFPImages[i].fingerPosition, isoTemplate2011, imgTemplate);
//                        // FingerPosition , Image Format ( jpeg 2000 ), IsoTemplate2011
//    
//                        System.err.println("Finger Position::::"+segFPImages[i].fingerPosition);
//                        
//                        
//                         saveEnrollmentDetails(saveEnrollment, segFPImages[i].fingerPosition, outImage, isoTemplate2011);
//                       // WriteImageFile("jpeg_2005_fir.bin", finalOutData);
//                    }
//                } catch (AnsiIsoException e) {
//                   statusField.setText("Fingerprint quality check failed. Please retry again");
//                   LOGGER.log(Level.SEVERE, "Fingerprint quality check failed and ISO create failed with exception ");
//                   scan.setDisable(false);
//                   backBtn.setDisable(false);
//                   return;
//                } 
//            }
//            
//            //To be uncommented
//            //saveEnrollmentDetails(saveEnrollment,segFPImages[i].fingerPosition, segFPImages[i].rawdata, isoTemplate);
//            
//        }
//                System.out.println("arc no in slap :" + saveEnrollment.getArcNo());            
//                System.out.println("Missing fingers count:"+arcDetails.getFingers().size()+" -> "+arcDetails.getFingers().toString());         
//
//                System.out.println("No., of FINGER PRINTS Available after Scan::"+fingerPrintSet.size());
//
//                int totalfingers = 10 - arcDetails.getFingers().size();
//                System.out.println("Fingers to be captured (total - missing) "+totalfingers);
//
//                    if (totalfingers == 0 && fingerPrintSet.size() == 0){
//                        LOGGER.log(Level.INFO,"No Fingerprint to scan, proceed to IRIS");                        
//                        statusField.setText("No Fingerprint to scan, proceed to IRIS ");
//                    }
//                    else if(fingerPrintSet.size() < totalfingers) {          
//                    statusField.setText("Fingers Quality Issue, Please Capture Again");
//                    LOGGER.log(Level.SEVERE,"fingers captured are less than fingers needed to be captured");
//                    scan.setDisable(false);
//                    backBtn.setDisable(false);
//                    return;            
//                    } else if (fingerPrintSet.size() > totalfingers){
//                    statusField.setText("Fingers Quality Issue, Please Capture Again");
//                    LOGGER.log(Level.SEVERE, "fingers captured are more than fingers needed to be captured"+fingerPrintSet.size()+" "+totalfingers);
//                    scan.setDisable(false);
//                    backBtn.setDisable(false);
//                    return;
//                    }
//        
//                System.err.println("\nPrinting Finger-print set iteratively\n============================================");
//                int k=1;
//                for (FP s : fingerPrintSet) {
//                    System.out.println(k+". FingerPrintSET :"+s.toString());
//                    k+=1;
//                }
//        
//            saveEnrollment.setFp(fingerPrintSet);
//            //saveEnrollment.setEnrollmentStatus("FingerPrint Capture Completed");
//            saveEnrollment.setEnrollmentStatus("FingerPrintCompleted");
//            System.out.println("Finger print capture completed");
//        
//            holder.setEnrollmentDetails(saveEnrollment);
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.enable(SerializationFeature.INDENT_OUTPUT);
//            mapper.setBase64Variant(Base64Variants.MIME_NO_LINEFEEDS);
//
//            String postJson;
//            try {
//                postJson = mapper.writeValueAsString(saveEnrollment);
//                //Code Added by K. Karthikeyan - 18-4-22 - Start
//                ObjectReaderWriter objReadWrite = new ObjectReaderWriter();
//                objReadWrite.writer(saveEnrollment);
//                System.out.println("Save Enrollment Object write");
//                SaveEnrollmentDetails s = objReadWrite.reader();
//                System.out.println("Enrollment Status "+s.getEnrollmentStatus());
//                //Code Added by K. Karthikeyan - 18-4-22 - Finish
//            //     System.out.println("post json slap :"+ postJson);
//            } catch (JsonProcessingException ex) {
//                Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
//                statusField.setText("Fetched Details From Server has Error");
//            }
//
//        
//        }
//        catch (Exception ex){
//           statusField.setText("Fingerprint quality check failed. Please retry again");
//           LOGGER.log(Level.SEVERE, "Fingerprint quality check and ISO create failed with exception ");
//           ex.printStackTrace();
//           thumbScan.setDisable(false);
//           backBtn.setDisable(false);
//           showIrisBtn.setDisable(true);           
//           return;
//        }
//        com.cdac.enrollmentStation.App.setRoot("iris");
//    } 

    @FXML
    private void showARCInput() throws IOException {

        confirmPane.setVisible(true);
        backBtn.setDisable(true);
        scan.setDisable(true);

    }


    @FXML
    private void goBack() throws IOException {
        System.out.println("inside go back");
        backBtn.setDisable(false);
        scan.setDisable(false);
        statusField.setText("");

        try {
            //System.out.println("device 1 -"+RealScan_JNI.RS_ExitDevice(deviceHandle));
            //RealScan_JNI.RS_ExitDevice(deviceHandle1);
            //System.out.println("device 2 -"+RealScan_JNI.RS_ExitDevice(deviceHandle1));
           /*Platform.runLater(new Runnable() {
           @Override public void run() {
                statusField.setText("Please wait...");
                }
            });     */
            //System.out.println("All devices -"+RealScan_JNI.RS_ExitAllDevices());

            //Thread.sleep(5000);
            /*if(t!=null && t.isAlive()){
                t.stop();
                System.out.println("Inside t");
            }*/
            //Thread.sleep(5000); 
            /*if(tRight!=null && tRight.isAlive()){
                tRight.stop();
                System.out.println("Inside tRight");
            }*/
            //Thread.sleep(5000);
            /*if(tThumb!=null && tThumb.isAlive()){
                tThumb.stop();
                System.out.println("Inside tThumb");
            }*/
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
            LOGGER.log(Level.SEVERE, "Error: " + ex.getMessage());
        }
        //System.out.println("All devices -"+RealScan_JNI.RS_ExitAllDevices());
        App.setRoot("enrollment_arc");
    }

    @FXML
    private void stayBack() throws IOException {
        System.out.println("inside stay back");
        backBtn.setDisable(false);
        scan.setDisable(false);
        confirmPane.setVisible(false);
    }


    private void setText(String message) {
        statusField.setText(message);
    }

    @FXML
    private void captureSlap() throws IOException {
        //disable right and thumb button
        //System.out.println("Inside Capture Slap method");
        scan.setDisable(true);
        rightScan.setDisable(true);
        thumbScan.setDisable(true);
        backBtn.setDisable(true);
        //System.out.println("Inside Capture Slap method");
        captureModesCompleted.clear();
        //Empty the preview Image View
        preview.imageProperty().set(null);
        scan.setText("RESCAN");
        try {
            if (isDeviceInitialized) {
                RealScan_JNI.RS_ExitDevice(deviceHandle);
                for (int i = 0; i < segFPImages.length; i++) {
                    segFPImages[i] = new segFP();
                    segFPImages[i].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
                    segFPImages[i].image = null;
                    segFPImages[i].rawdata = null;
                    segFPImages[i].width = 0;
                    segFPImages[i].height = 0;
                }
            }
            LOGGER.log(Level.INFO, "Fingerprints array initialized");
//            RealScanTest test = new RealScanTest();
//            RealScan_JNI jni = new RealScan_JNI(test, true);
//            RealScan_JNI.RS_JNI_Init(jni);
            System.out.println("init ::");
            LOGGER.log(Level.INFO, "Real Scan JNI initialized");

            int sdkresult = RealScan_JNI.RS_SUCCESS;

            int numOfDevice = RealScan_JNI.RS_InitSDK("", 0);

            if (RealScan_JNI.RS_GetLastError() == RealScan_JNI.RS_SUCCESS || RealScan_JNI.RS_GetLastError() == RealScan_JNI.RS_ERR_SDK_ALREADY_INITIALIZED) {
                statusField.setText("Kindly connect the Device, if not connected");
                LOGGER.log(Level.INFO, "SDK initialized successfully");
                backBtn.setDisable(false);
                scan.setDisable(false);
            }

            deviceName = new StringBuffer("Device ");
            int i = 0;
            for (; i < numOfDevice; i++) {
                System.out.println("device :" + i);
            }


            if (i <= 0) {
                //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                LOGGER.log(Level.INFO, "Device is not detected. Check device connection");
                backBtn.setDisable(false);
                scan.setDisable(false);
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Exception ::::", e);
            statusField.setText("Exception ::::" + e);
        }

        try {
            int deviceResult = RealScan_JNI.RS_SUCCESS;
            int deviceResult1 = RealScan_JNI.RS_SUCCESS;

            try {
                deviceHandle = RealScan_JNI.RS_InitDevice(0);
            } catch (Exception e) {
                System.out.println("Device Initialization Error" + e);
                statusField.setText("Device Initialization Error");
            }
            //deviceHandle1 = RealScan_JNI.RS_InitDevice(1);

            if (RealScan_JNI.RS_GetLastError() != RealScan_JNI.RS_SUCCESS && RealScan_JNI.RS_GetLastError() != RealScan_JNI.RS_ERR_DEVICE_ALREADY_INITIALIZED) {
                String errStr = RealScan_JNI.RS_GetErrString(RealScan_JNI.RS_GetLastError());
                System.out.println("init device : " + errStr);
                statusField.setText("Kindly Check the device connection");
                LOGGER.log(Level.INFO, "Device is not initalized");
                backBtn.setDisable(false);
                scan.setDisable(false);
                isDeviceInitialized = false;
                return;
            }
            LOGGER.log(Level.INFO, "Device is initialized successfully");
            RealScan_JNI.RSDeviceInfo deviceInfoT = new RealScan_JNI.RSDeviceInfo();


            deviceResult = RealScan_JNI.RS_GetDeviceInfo(deviceHandle, deviceInfoT);
            //deviceResult1 = RealScan_JNI.RS_GetDeviceInfo(deviceHandle1, deviceInfoT);

            if (deviceResult != RealScan_JNI.RS_SUCCESS) {
                String errStr = RealScan_JNI.RS_GetErrString(deviceResult);
                System.out.println("device result :" + errStr);
                statusField.setText("device result :" + errStr);
                backBtn.setDisable(false);
                scan.setDisable(false);
                isDeviceInitialized = false;
                return;
            }
        
        /*if (deviceResult1 != RealScan_JNI.RS_SUCCESS) {
            String errStr = RealScan_JNI.RS_GetErrString(deviceResult1);
            System.out.println("device result 1:" +errStr);
            isDeviceInitialized = false;
            return;
        }*/
            isDeviceInitialized = true;

        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Device Info Error:::::");
            statusField.setText("Getting Device InFo Error, Kindly Reconnect the Device Again");
        }

        //Enabled For Testing 161221
//        int minFingerResult = RealScan_JNI.RS_SetMinimumFinger(deviceHandle, 3);
//        System.out.println("minimum finger result:"+ minFingerResult);
//        if(RealScan_JNI.RS_GetLastError() != RealScan_JNI.RS_SUCCESS){
//            String errStr = RealScan_JNI.RS_GetErrString(RealScan_JNI.RS_GetLastError());
//            statusField.setText(errStr);
//            System.out.println("error while setting minimum fingers :" + errStr);
//        }
//        else {
//            System.out.println("Setting minimum finger count is done successfully done!!");
//            statusField.setText("Setting minimum finger count is done successfully done!!");
//        }

        ARCDetailsHolder holder = ARCDetailsHolder.getArcDetailsHolder();
        arcDetails = holder.getArcDetails();

        List<String> missingfingers = arcDetails.getFingers();
        //List<String> missingfingers = new ArrayList<>();
        //missingfingers.add("LR");        
        //missingfingers.add("LL");
        //missingfingers.add("LT");
        //missingfingers.add("LM");
        //missingfingers.add("LI");
        //missingfingers.add("RR");        
        //missingfingers.add("RL");
        //missingfingers.add("RT");
        //missingfingers.add("RM");
        //missingfingers.add("RI");
        arcDetails.setFingers(missingfingers);
        System.out.println("MISSING FINGERS:::" + arcDetails.getFingers());
        // Thread t = new Thread(DoSequenceCapture);
        t = new Thread(DoSequenceCapture);
        t.start();
        //Platform.runLater(DoSequenceCapture);                      
    }

    @FXML
    private void captureRightSlap() {
//        Thread t = new Thread(DoSequenceCaptureRight);
//        t.start(); 
//        if(tRight !=null)
//            tRight.stop();
        tRight = new Thread(DoSequenceCaptureRight);
        tRight.start();
    }

    @FXML
    private void captureThumb() {
//        Thread t = new Thread(DoSequenceCaptureThumb);
//        t.start(); 
//         if(tThumb !=null)
//            tThumb.stop();
        tThumb = new Thread(DoSequenceCaptureThumb);
        tThumb.start();
    }

    Runnable PlaceLeft = new Runnable() {

        @Override
        public void run() {
            setText("Place Left 4 fingers on the sensor");
        }
    };

    Runnable PlaceRight = new Runnable() {

        @Override
        public void run() {
            setText("Place Right 4 fingers on the sensor");
        }
    };


    Runnable DoSequenceCapture = new Runnable() {
        @Override
        public void run() {
            try {
                //System.out.println("Inside do sequ capture");
                scan.setDisable(true);
                showIrisBtn.setDisable(true); //Disable the Iris button
                backBtn.setDisable(true);
                //System.out.println("Inside do sequ capture");

                // 1) variables initialize
                int res = RealScan_JNI.RS_SUCCESS;
                String resString;
                RealScan_JNI.RSImageInfo imageInfo = new RealScan_JNI.RSImageInfo();
                int numOfFingers = 0;
                //Initialization of slapInfoArray and imageInfoArray
                RealScan_JNI.RSSlapInfo[] slapInfoArray = new RealScan_JNI.RSSlapInfo[4];
                for (int i = 0; i < 4; i++) {
                    slapInfoArray[i] = new RealScan_JNI.RSSlapInfo();
                }
                RealScan_JNI.RSImageInfo[] imageInfoArray = new RealScan_JNI.RSImageInfo[4];
                for (int i = 0; i < 4; i++) {
                    imageInfoArray[i] = new RealScan_JNI.RSImageInfo();
                }
                captureModesCompleted.clear(); //Clearing all the captured modes

                clearSegmentedImagePanels(clearImagePanel); //clearing the segmented image panels
                clearSeqCheckTargetImages(); //clearing the seqcheck target images


                // 2) scanning left four fingers
                try {
                    if (leftfingerstoscan() == 1) { //fingers to scan is one
                        res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        //int res1 = RealScan_JNI.RS_SetCaptureMode(deviceHandle1, RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        if (res != RealScan_JNI.RS_SUCCESS) {
                            LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                                    statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                                    //rightScan.setDisable(false);
                                    backBtn.setDisable(false);
                                    return;
                                }
                            });
                        }

                    } else { //More than one fingers to scan
                        res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_LEFT_FOUR_FINGERS, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        if (res != RealScan_JNI.RS_SUCCESS) {
                            LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                                    statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                                    backBtn.setDisable(false);
                                    return;
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.out.println("exception caught :" + e);
                    statusField.setText("Exception caught :" + e);
                }

                if (leftfingerstoscan() >= 1) {    //Enable For Single Fingerprint Capture
                    int minFingerResult = RealScan_JNI.RS_SetMinimumFinger(deviceHandle, leftfingerstoscan());
                    System.out.println("minimum finger result:" + minFingerResult);
                    if (RealScan_JNI.RS_GetLastError() != RealScan_JNI.RS_SUCCESS) {
                        String errStr = RealScan_JNI.RS_GetErrString(RealScan_JNI.RS_GetLastError());
                        statusField.setText(errStr);
                        scan.setDisable(false);
                        backBtn.setDisable(false);
                        LOGGER.log(Level.SEVERE, "Error while setting minimum fingers LEFT HAND :" + errStr);
                        System.out.println("error while setting minimum fingers :" + errStr);
                    }

                    System.out.println("Place left " + leftFingersToScan(arcDetails.getFingers()) + " fingers in sensor loc-1");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Place your " + leftFingersToScan(arcDetails.getFingers()) + " left fingers on the sensor ");
                            scan.setDisable(true);
                            backBtn.setDisable(true);
                        }
                    });

                    //A finger is on the sensor, Please remove the finger after the beep sound
                    res = RealScan_JNI.RS_TakeImageData(deviceHandle, 20000, imageInfo);//A finger is on the sensor, Please remove the finger after the beep sound
                    if (res != RealScan_JNI.RS_SUCCESS) {
                        final String strMsg = "A finger is on the sensor, Please remove the finger after the beep sound";
                        String errStr = RealScan_JNI.RS_GetErrString(res);
                        if (errStr.toLowerCase().contains("sensor is too dirty ")) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    statusField.setText(strMsg);
                                    scan.setDisable(false);
                                    backBtn.setDisable(false);
                                }
                            });
                        } else {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("Message1::::::" + errStr);
                                    statusField.setText(errStr + ", Kindly Rescan again.");
                                    scan.setDisable(false);
                                    //rightScan.setDisable(false);
                                    backBtn.setDisable(false);
                                }
                            });
                        }
                        scan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    if (imageInfo.pbyImgBuf == null || imageInfo.imageWidth == 0 || imageInfo.imageHeight == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Image is empty! Try Left hand again");
                            }
                        });
                        scan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    //To set the Image Panel
                    setImagePanel(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight);
                    int slapType;//left slap
                    int fingerType = 0;
                    int[] missingFingerArr = new int[]{0, 0, 0, 0};
                    int n = 0;
                    int[] captureMode_result = RealScan_JNI.RS_GetCaptureMode(deviceHandle);
                    System.out.println("CAPTURE MODE::::" + captureMode_result[0]);
                    slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;

                    switch (captureMode_result[0]) {
                        case RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER:
                            slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;
                            System.out.println("Single Finger Left");
                            fingerType = fingertypeleft();  //It will return Single finger type
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER_EX:
                        case RealScan_JNI.RS_CAPTURE_ROLL_FINGER:
                        case RealScan_JNI.RS_CAPTURE_ROLL_FINGER_EX:
                            slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;
                            System.out.println("Roll Finger");
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS:
                        case RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS_EX:
                            slapType = RealScan_JNI.RS_SLAP_TWO_THUMB;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_LEFT_FOUR_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_LEFT_FOUR;
                            if (arcDetails.getFingers().contains("LL"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_LITTLE;
                            if (arcDetails.getFingers().contains("LR"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_RING;
                            if (arcDetails.getFingers().contains("LM"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_MIDDLE;
                            if (arcDetails.getFingers().contains("LI"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_INDEX;
                            fingerType = RealScan_JNI.RS_FGP_LEFT_LITTLE;
                            System.out.println("Left Finger");
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_FOUR_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_RIGHT_FOUR;
                            if (arcDetails.getFingers().contains("RI"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_INDEX;
                            if (arcDetails.getFingers().contains("RM"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_MIDDLE;
                            if (arcDetails.getFingers().contains("RR"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_RING;
                            if (arcDetails.getFingers().contains("RL"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_LITTLE;
                            fingerType = RealScan_JNI.RS_FGP_RIGHT_INDEX;
                            break;
                        default:
                            statusField.setText("Cannot segment in this mode");
                            return;
                    }


                    // 3) save target image(left) of sequence check
                    //numOfFingers = RealScan_JNI.RS_Segment(imageInfo, RealScan_JNI.RS_SLAP_LEFT_FOUR, numOfFingers, slapInfoArray, imageInfoArray);
                    numOfFingers = RealScan_JNI.RS_Segment(imageInfo, slapType, numOfFingers, slapInfoArray, imageInfoArray);

                    //Finger Counts Mismatch

                    System.out.println("numOf--Fingers:::" + numOfFingers);
                    System.out.println("leftfingerstoscan:::" + leftfingerstoscan());
                    if (numOfFingers != leftfingerstoscan()) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Fingers counts not Matched. Try Left hand again");
                                LOGGER.log(Level.SEVERE, "Fingers counts not Matched. Try Left hand again");
                                scan.setDisable(false);
                                backBtn.setDisable(false);
                            }
                        });

                        return;
                    }

                    System.out.println("NUM OF FING:::" + numOfFingers);
                    if (numOfFingers < 0) {
                        statusField.setText("Segmentation failed");
                        LOGGER.log(Level.SEVERE, "Left Hand Segmentation failed");
                        scan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    StringBuilder segResult = new StringBuilder();
                    int j = 0;
                    for (int i = 0; i < numOfFingers; i++) {
                        if (slapInfoArray[i].fingerType == RealScan_JNI.RS_FGP_UNKNOWN) {
                            System.out.println("fgp unknown:::" + slapInfoArray[i].fingerType);
                            if (slapType == RealScan_JNI.RS_SLAP_LEFT_FOUR) {
                                System.out.println("fgp unknown:::" + slapInfoArray[i].fingerType);
                                while (fingerType == missingFingerArr[j]) {
                                    fingerType--;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType--;
                            } else if (slapType == RealScan_JNI.RS_SLAP_RIGHT_FOUR) {
                                while (fingerType == missingFingerArr[j]) {
                                    fingerType++;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType++;
                            }//Added on 080722
                            else if (slapType == RealScan_JNI.RS_SLAP_ONE_FINGER) {
                                slapInfoArray[i].fingerType = fingertypeleft();
                                System.out.println("Finger Type Left Single: image quality" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality);
                            } else {
                                statusField.setText("SlapType is Not set to Left Four or Left Single");
                                LOGGER.log(Level.SEVERE, "Finger Type Left Single: image quality" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality);
                                //System.out.println("Finger Type Left Single: image quality"+ slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality );
                            }
                        }
                        segResult.append("[" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality + "]");
                        System.out.println("Finger Type : image quality" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality);
                    }

                    saveSeqCheckTargetImages(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight, RealScan_JNI.RS_SLAP_LEFT_FOUR, numOfFingers);
                    try {
                        if (leftfingerstoscan() == 1) {// finger to scan is one
                            setSegmentedImagePanel(RealScan_JNI.RS_SLAP_ONE_FINGER, slapInfoArray, imageInfoArray, numOfFingers, "left");
                        } else {//more than one fingers to scan
                            setSegmentedImagePanel(RealScan_JNI.RS_SLAP_LEFT_FOUR, slapInfoArray, imageInfoArray, numOfFingers, "left");
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Image Quality too poor. Try Left hand again");
                                LOGGER.log(Level.SEVERE, "Image Quality too poor. Try Left hand again");
                                scan.setDisable(false);
                                backBtn.setDisable(false);
                            }
                        });
                        scan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Left fingers captured successfully");
                            scan.setDisable(true);
                            leftScan.setDisable(true);
                            rightScan.setDisable(true);
                            thumbScan.setDisable(true);
                            backBtn.setDisable(true);

                            if (captureModesCompleted.contains(1)) {
                                captureModesCompleted.remove(1);
                            }

                            captureModesCompleted.add(1);
                            System.out.println("capture modes completed is :" + captureModesCompleted.size());
                            if (captureModesCompleted.size() == 3) {
                                showIrisBtn.setDisable(false);
                                System.out.println("first block");
                            }
                        }
                    });
                    System.out.println("Left fingers captured successfully1 ");
                    LOGGER.log(Level.INFO, "Left fingers captured successfully1 ");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    System.out.println("before calling sequence capture right");
                    tRight = new Thread(DoSequenceCaptureRight);
                    tRight.start();
                    System.out.println("After calling sequence capture right");

                } else { //Left Hand is not available
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Left fingers not available to capture");
                            System.out.println("Proceed to capture right fingers");
                            scan.setDisable(true);
                            leftScan.setDisable(true);
                            rightScan.setDisable(false);
                            thumbScan.setDisable(true);
                            if (captureModesCompleted.contains(1)) {
                                captureModesCompleted.remove(1);
                            }
                            captureModesCompleted.add(1);
                            System.out.println("capture size left :" + captureModesCompleted.size());
                            if (captureModesCompleted.size() == 3) {
                                showIrisBtn.setDisable(false);
                                System.out.println("first block");

                            }
                        }
                    });

                    System.out.println("before calling sequence capture right at Else");
                    tRight = new Thread(DoSequenceCaptureRight);
                    tRight.start();
                    System.out.println("After calling sequence capture right at Else");
                }

            } catch (Exception e) {
                statusField.setText("Exception:" + e);
            }
            //Commented by p not in portable enrollment
//            slapInfoArray = new RealScan_JNI.RSSlapInfo[4];
//            
//            for (int i = 0; i < 4; i++) {
//                slapInfoArray[i] = new RealScan_JNI.RSSlapInfo();
//            }
//            imageInfoArray = new RealScan_JNI.RSImageInfo[4];
//            for(int i=0;i<4;i++)
//            {
//                imageInfoArray[i] = new RealScan_JNI.RSImageInfo();
//            }    

            // 4) scanning right four fingers           
        }


    };

    Runnable DoSequenceCaptureRight = new Runnable() {
        @Override
        public void run() {
            try {
                System.out.println("Inside sequence capture right");
                showIrisBtn.setDisable(true);
                thumbScan.setDisable(true);
                backBtn.setDisable(true);
                rightScan.setDisable(true);

                // 1) variables initialize
                int res = RealScan_JNI.RS_SUCCESS;
                String resString;
                RealScan_JNI.RSImageInfo imageInfo = new RealScan_JNI.RSImageInfo();
                int numOfFingers = 0;
                rightmissingfingers = 0;
                thumbmissingfingers = 0;
                RealScan_JNI.RSSlapInfo[] slapInfoArray = new RealScan_JNI.RSSlapInfo[4];
                for (int i = 0; i < 4; i++) {
                    slapInfoArray[i] = new RealScan_JNI.RSSlapInfo();
                }

                RealScan_JNI.RSImageInfo[] imageInfoArray = new RealScan_JNI.RSImageInfo[4];
                for (int i = 0; i < 4; i++) {
                    imageInfoArray[i] = new RealScan_JNI.RSImageInfo();
                }

                //Commented for clearing left panel 231221
                // clearSegmentedImagePanels();
                // clearSeqCheckTargetImages();
                //clearSegmentedImagePanelsCheck("Right");
                //clearSeqCheckTargetImages();
                slapInfoArray = new RealScan_JNI.RSSlapInfo[4];
                for (int i = 0; i < 4; i++) {
                    slapInfoArray[i] = new RealScan_JNI.RSSlapInfo();
                }

                imageInfoArray = new RealScan_JNI.RSImageInfo[4];
                for (int i = 0; i < 4; i++) {
                    imageInfoArray[i] = new RealScan_JNI.RSImageInfo();
                }
                // 4) scanning right four
                try {
                    if (rightfingerstoscan() == 1) {
                        res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        if (res != RealScan_JNI.RS_SUCCESS) {
                            LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                                    statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                                    rightScan.setDisable(false);
                                    backBtn.setDisable(false);
                                    return;
                                }
                            });
                        }
                    } else {
                        res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_FOUR_FINGERS, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        if (res != RealScan_JNI.RS_SUCCESS) {
                            LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                                    statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                                    rightScan.setDisable(false);
                                    backBtn.setDisable(false);
                                    return;
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception : " + e);
                }

                if (rightfingerstoscan() >= 1) {    //Enable For Single Fingerprint Capture
                    int minFingerResult = RealScan_JNI.RS_SetMinimumFinger(deviceHandle, rightfingerstoscan());
                    if (RealScan_JNI.RS_GetLastError() != RealScan_JNI.RS_SUCCESS) {
                        String errStr = RealScan_JNI.RS_GetErrString(RealScan_JNI.RS_GetLastError());
                        statusField.setText(errStr);
                        LOGGER.log(Level.SEVERE, "Error while setting minimum fingers LEFT HAND :" + errStr);
                        System.out.println("error while setting minimum fingers :" + errStr);
                        rightScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Place your " + rightFingersToScan(arcDetails.getFingers()) + " right fingers on the sensor");
                        }
                    });

                    res = RealScan_JNI.RS_TakeImageData(deviceHandle, 20000, imageInfo);
                    if (res != RealScan_JNI.RS_SUCCESS) {
                        final String strMsg = "A finger is on the sensor, Please remove the finger after the beep sound";
                        String errStr = RealScan_JNI.RS_GetErrString(res);
                        if (errStr.toLowerCase().contains("sensor is too dirty")) {
                            //strMsg = "A finger is on the sensor, Please remove the finger after the beep sound";
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    statusField.setText(strMsg);
                                    rightScan.setDisable(false);
                                    backBtn.setDisable(false);
                                }
                            });
                        } else {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    statusField.setText(errStr + ", Kindly Rescan again.");
                                    System.out.println("Message2::::: : " + strMsg);
                                    rightScan.setDisable(false);
                                    backBtn.setDisable(false);
                                }
                            });
                        }
                        rightScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    if (imageInfo.pbyImgBuf == null || imageInfo.imageWidth == 0 || imageInfo.imageHeight == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Image is empty! Try right hand again");
                            }
                        });
                        rightScan.setDisable(false);
                        return;
                    }

                    setImagePanel(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight);
                    int slapType;
                    int fingerType = 0;
                    int[] missingFingerArr = new int[]{0, 0, 0, 0};

                    int n = 0;
                    int[] captureMode_result = RealScan_JNI.RS_GetCaptureMode(deviceHandle);
                    switch (captureMode_result[0]) {
                        case RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER:
                            System.out.println("Single Finger Right");
                            slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;
                            fingerType = fingertyperight();
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER_EX:
                        case RealScan_JNI.RS_CAPTURE_ROLL_FINGER:
                        case RealScan_JNI.RS_CAPTURE_ROLL_FINGER_EX:
                            slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS:
                        case RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS_EX:
                            slapType = RealScan_JNI.RS_SLAP_TWO_THUMB;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_LEFT_FOUR_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_LEFT_FOUR;
                            if (arcDetails.getFingers().contains("LL"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_LITTLE;
                            if (arcDetails.getFingers().contains("LR"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_RING;
                            if (arcDetails.getFingers().contains("LM"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_MIDDLE;
                            if (arcDetails.getFingers().contains("LI"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_INDEX;
                            fingerType = RealScan_JNI.RS_FGP_LEFT_LITTLE;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_FOUR_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_RIGHT_FOUR;
                            if (arcDetails.getFingers().contains("RI"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_INDEX;
                            if (arcDetails.getFingers().contains("RM"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_MIDDLE;
                            if (arcDetails.getFingers().contains("RR"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_RING;
                            if (arcDetails.getFingers().contains("RL"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_LITTLE;
                            fingerType = RealScan_JNI.RS_FGP_RIGHT_INDEX;
                            break;

                        default:
                            statusField.setText("Cannot segment in this mode");
                            rightScan.setDisable(false);
                            return;
                    }

                    // 5) do sequence check & save target image(right) of sequence check
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Sequence Checking is running now...");
                        }
                    });

                    //numOfFingers = RealScan_JNI.RS_Segment(imageInfo, RealScan_JNI.RS_SLAP_RIGHT_FOUR, numOfFingers, slapInfoArray, imageInfoArray);
                    numOfFingers = RealScan_JNI.RS_Segment(imageInfo, slapType, numOfFingers, slapInfoArray, imageInfoArray);
                    //Finger counts Mismatch
                    System.out.println("numOf--Fingers:::" + numOfFingers);
                    System.out.println("rightfingerstoscan:::" + rightfingerstoscan());
                    if (numOfFingers != rightfingerstoscan()) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Fingers counts not Matched. Try Right hand again");
                                LOGGER.log(Level.SEVERE, "Fingers counts not Matched. Try Right hand again");
                                rightScan.setDisable(false);
                                backBtn.setDisable(false);
                            }
                        });

                        return;
                    }
                    resString = sequenceCheckProcess2(imageInfoArray, numOfFingers, "right");
                    if (resString.contains("There is no target image")) {
                        System.out.println("ResTring:::" + resString);
                        rightScan.setDisable(false);
                        return;

                    } else if (!resString.contains("There isn't matched finger")) {
                        System.out.println("ResTring:::" + resString);
                        rightScan.setDisable(false);
                        return;
                    }
                    System.out.println("ResTring:::" + resString);

                    for (int i = 0; i < pFingerMsg.length; i++) {
                        if (resString.equals(pFingerMsg[i])) {
                            statusField.setText(pFingerMsg[i] + " finger is already scanned!");
                            rightScan.setDisable(false);
                            return;
                        }
                    }
                    if (numOfFingers < 0) {
                        statusField.setText("Segmentation failed");
                        LOGGER.log(Level.SEVERE, "Right Hand Segmentation failed");
                        rightScan.setDisable(false);
                        return;
                    }

                    StringBuilder segResult = new StringBuilder();
                    int j = 0;
                    for (int i = 0; i < numOfFingers; i++) {
                        if (slapInfoArray[i].fingerType == RealScan_JNI.RS_FGP_UNKNOWN) {
                            if (slapType == RealScan_JNI.RS_SLAP_LEFT_FOUR) {
                                while (fingerType == missingFingerArr[j]) {
                                    fingerType--;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType--;
                            } else if (slapType == RealScan_JNI.RS_SLAP_RIGHT_FOUR) {
                                while (fingerType == missingFingerArr[j]) {
                                    fingerType++;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType++;
                            }//Added on 080722
                            else if (slapType == RealScan_JNI.RS_SLAP_ONE_FINGER) {
                                slapInfoArray[i].fingerType = fingertyperight();
                                System.out.println("Finger Type Right Single: image quality" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality);
                            } else {
                                statusField.setText("SlapType is Not set to Right Four or Right Single");
                                LOGGER.log(Level.SEVERE, "Finger Type Left Single: image quality" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality);
                                System.out.println("Finger Type Left Single: image quality" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality);
                            }
                        }
                        segResult.append("[" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality + "]");
                    }
                    saveSeqCheckTargetImages(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight, RealScan_JNI.RS_SLAP_RIGHT_FOUR, numOfFingers);


                    try {
                        if (rightfingerstoscan() == 1) {
                            setSegmentedImagePanel(RealScan_JNI.RS_SLAP_ONE_FINGER, slapInfoArray, imageInfoArray, numOfFingers, "right");
                        } else {
                            if (numOfFingers == 0 || rightfingerstoscan() > numOfFingers) { //Important to avoid jump to next section even if error in current section
                                System.out.println("numOfFinters either 0 or less than fingestoscan. Num of Finger actual" + numOfFingers + " Fingesto scan" + rightfingerstoscan());
                                rightScan.setDisable(false);
                                return;
                            }
                            setSegmentedImagePanel(RealScan_JNI.RS_SLAP_RIGHT_FOUR, slapInfoArray, imageInfoArray, numOfFingers, "right");
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Image Quality too poor. Try right hand again");
                                LOGGER.log(Level.SEVERE, "Image Quality too poor. Try right hand again");
                            }
                        });
                        rightScan.setDisable(false);
                        return;
                    }


                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Right fingers captured successfully");
                            LOGGER.log(Level.INFO, "Right fingers captured successfully");
                            rightScan.setDisable(true);// Added on 080922
                            if (captureModesCompleted.contains(2)) {
                                captureModesCompleted.remove(2);
                            }
                            captureModesCompleted.add(2);
                            System.out.println("capture size right :" + captureModesCompleted.size());
                            if (captureModesCompleted.size() == 3) {
                                showIrisBtn.setDisable(false);
                            }
                        }
                    });

                    //captureModesCompleted.add(2);
                    System.out.println("Right fingers captured successfully2 ");
                    LOGGER.log(Level.INFO, "Right fingers captured successfully2 ");
                    rightScan.setDisable(true);

                    System.out.println("before calling sequence thumb");
                    tThumb = new Thread(DoSequenceCaptureThumb);
                    tThumb.start();
                    System.out.println("After calling sequence thumb");

                } else { //Right hand is not available
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            rightScan.setDisable(true);
                            statusField.setText("Right fingers not available to capture");
                            System.out.println("Proceed to capture thumb");
                            thumbScan.setDisable(false);
                            if (captureModesCompleted.contains(2)) {
                                captureModesCompleted.remove(2);
                            }
                            captureModesCompleted.add(2);
                            System.out.println("capture size right :" + captureModesCompleted.size());
                            if (captureModesCompleted.size() == 3) {
                                showIrisBtn.setDisable(false);
                                System.out.println("second block");
                            }
                        }
                    });
                    System.out.println("before calling sequence thumb");
                    tThumb = new Thread(DoSequenceCaptureThumb);
                    tThumb.start();
                    System.out.println("After calling sequence thumb");
                }

                if (!captureModesCompleted.contains(2)) {
                    rightScan.setDisable(false);
                }

            } catch (Exception e) {
                statusField.setText("Exception:" + e);
            }
        }
    };

    Runnable DoSequenceCaptureLeft = new Runnable() {
        @Override
        public void run() {
            // 1) variables initialize
            thumbScan.setDisable(true);
            backBtn.setDisable(true);
            int res = RealScan_JNI.RS_SUCCESS;
            String resString;
            RealScan_JNI.RSImageInfo imageInfo = new RealScan_JNI.RSImageInfo();
            int numOfFingers = 0;
            RealScan_JNI.RSSlapInfo[] slapInfoArray = new RealScan_JNI.RSSlapInfo[4];

            for (int i = 0; i < 4; i++) {
                slapInfoArray[i] = new RealScan_JNI.RSSlapInfo();
            }
            RealScan_JNI.RSImageInfo[] imageInfoArray = new RealScan_JNI.RSImageInfo[4];
            for (int i = 0; i < 4; i++) {
                imageInfoArray[i] = new RealScan_JNI.RSImageInfo();
            }

            // clearSegmentedImagePanels();
            // clearSeqCheckTargetImages();

            try {
                res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_LEFT_FOUR_FINGERS, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                if (res != RealScan_JNI.RS_SUCCESS) {
                    statusField.setText("capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                    return;
                }
            } catch (Exception e) {
                System.out.println("Exception : " + e);
            }

            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    statusField.setText("Place your " + leftFingersToScan(arcDetails.getFingers()) + " left fingers on the sensor ");
                    //statusField.setText("Place left "+leftFingersToScan(arcDetails.getFingers()).toString()+"fingers in sensor");
                    //System.out.println("Place left "+leftFingersToScan(arcDetails.getFingers()).toString()+"fingers in sensor loc-2");
                }
            });


            res = RealScan_JNI.RS_TakeImageData(deviceHandle, 20000, imageInfo);
            if (res != RealScan_JNI.RS_SUCCESS) {
                String errStr = RealScan_JNI.RS_GetErrString(res);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setText(errStr + ", Kindly Rescan again.");
                    }
                });
                return;
            }

            if (imageInfo.pbyImgBuf == null || imageInfo.imageWidth == 0 || imageInfo.imageHeight == 0) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setText("Image is empty!");
                    }
                });
                return;
            }

            setImagePanel(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight);

            // 3) save target image(left) of sequence check
            numOfFingers = RealScan_JNI.RS_Segment(imageInfo, RealScan_JNI.RS_SLAP_LEFT_FOUR, numOfFingers, slapInfoArray, imageInfoArray);
            saveSeqCheckTargetImages(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight, RealScan_JNI.RS_SLAP_LEFT_FOUR, 4);
            setSegmentedImagePanel(RealScan_JNI.RS_SLAP_LEFT_FOUR, slapInfoArray, imageInfoArray, numOfFingers, "left");
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    statusField.setText("Left fingers captured successfully");
                    if (captureModesCompleted.contains(1)) {
                        captureModesCompleted.remove(1);

                    }
                    captureModesCompleted.add(1);
                    if (captureModesCompleted.size() == 3) {
                        System.out.println("first block");
                        showIrisBtn.setDisable(false);

                    }
                }
            });


            System.out.println("Left fingers captured successfully2 ");

        }
    };

    Runnable DoSequenceCaptureThumb = new Runnable() {
        @Override
        public void run() {
            try {
                // 1) variables initialize
                thumbScan.setDisable(true);
                backBtn.setDisable(true);
                int res = RealScan_JNI.RS_SUCCESS;
                RealScan_JNI.RSImageInfo imageInfo = new RealScan_JNI.RSImageInfo();
                int numOfFingers = 0;
                RealScan_JNI.RSSlapInfo[] slapInfoArray = new RealScan_JNI.RSSlapInfo[4];
                String resString;
                for (int i = 0; i < 4; i++) {
                    slapInfoArray[i] = new RealScan_JNI.RSSlapInfo();
                }
                RealScan_JNI.RSImageInfo[] imageInfoArray = new RealScan_JNI.RSImageInfo[4];
                for (int i = 0; i < 4; i++) {
                    imageInfoArray[i] = new RealScan_JNI.RSImageInfo();
                }
                LTCanvas.setImage(null);
                RTCanvas.setImage(null);
                // added on 21/04/22
                try {
                    if (thumbfingerstoscan() == 1) {
                        res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        if (res != RealScan_JNI.RS_SUCCESS) {
                            LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                                    statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                                    backBtn.setDisable(false);
                                    thumbScan.setDisable(false);
                                    return;
                                }
                            });
                        }
                    } else {
                        res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                        if (res != RealScan_JNI.RS_SUCCESS) {
                            LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //statusField.setText("Device Connection Error, Kindly GoBack and Capture Again");
                                    statusField.setText("Biometric Data capturing Device is not Connected, Kindly connect and try again.");
                                    backBtn.setDisable(false);
                                    thumbScan.setDisable(false);
                                    return;
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception : " + e);
                }

                //Commented on 21/04/22
            /*
            try{ 
                res = RealScan_JNI.RS_SetCaptureMode(deviceHandle, RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS, RealScan_JNI.RS_AUTO_SENSITIVITY_NORMAL, true);
                if(res != RealScan_JNI.RS_SUCCESS) {
                    statusField.setText("capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")");
                    LOGGER.log(Level.SEVERE, "capture mode setting failed(error : " + RealScan_JNI.RS_GetErrString(res) + ")"); 
                    thumbScan.setDisable(false);
                    return;
                }
            }catch(Exception e){
                System.out.println("Exception : "+e);
            }*/

                int thumbmissingfingers = 0;
                if (arcDetails.getFingers().contains("LT")) thumbmissingfingers++;
                if (arcDetails.getFingers().contains("RT")) thumbmissingfingers++;

                int fingerstoscanthumb = 2 - thumbmissingfingers;
                System.out.println("thumb fingers3 :" + fingerstoscanthumb);
                if (fingerstoscanthumb >= 1 && fingerstoscanthumb <= 2) {
                    int minFingerResult = RealScan_JNI.RS_SetMinimumFinger(deviceHandle, fingerstoscanthumb);
                    if (RealScan_JNI.RS_GetLastError() != RealScan_JNI.RS_SUCCESS) {
                        String errStr = RealScan_JNI.RS_GetErrString(RealScan_JNI.RS_GetLastError());
                        System.out.println("error while setting minimum fingers :" + errStr);
                        statusField.setText(errStr);
                        backBtn.setDisable(false);
                        thumbScan.setDisable(false);
                        //LOGGER.log(Level.SEVERE, "Error while setting minimum fingers LEFT HAND :" + errStr);
                        System.out.println("error while setting minimum fingers :" + errStr);
                    }

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            //statusField.setText("Place your two thumbs on the sensor");
                            statusField.setText("Place your " + thumbFingersToScan(arcDetails.getFingers()) + " thumb fingers in the middle of scanner ");

                        }
                    });

                    res = RealScan_JNI.RS_TakeImageData(deviceHandle, 20000, imageInfo);
                    if (res != RealScan_JNI.RS_SUCCESS) {
                        String errStr = RealScan_JNI.RS_GetErrString(res);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText(errStr + ", Kindly Rescan again.");
                                System.out.println("error while setting minimum fingers new:" + errStr);
                                backBtn.setDisable(false);
                                thumbScan.setDisable(false);
                            }
                        });
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    if (imageInfo.pbyImgBuf == null || imageInfo.imageWidth == 0 || imageInfo.imageHeight == 0) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Image is empty! Try thumb again");
                            }
                        });
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    // To Set the image panel
                    setImagePanel(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight);
                    int slapType;//left slap
                    int fingerType = 0;
                    int[] missingFingerArr = new int[]{0, 0};
                    int n = 0;
                    int[] captureMode_result = RealScan_JNI.RS_GetCaptureMode(deviceHandle);
                    switch (captureMode_result[0]) {
                        case RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER:
                            System.out.println("Single Finger Thumb");
                            slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;
                            fingerType = fingertypethumb();
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_SINGLE_FINGER_EX:
                        case RealScan_JNI.RS_CAPTURE_ROLL_FINGER:
                        case RealScan_JNI.RS_CAPTURE_ROLL_FINGER_EX:
                            slapType = RealScan_JNI.RS_SLAP_ONE_FINGER;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_TWO_THUMB;
                            if (arcDetails.getFingers().contains("LT"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_THUMB;
                            if (arcDetails.getFingers().contains("RT"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_THUMB;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS_EX:
                            slapType = RealScan_JNI.RS_SLAP_TWO_THUMB;
                            if (arcDetails.getFingers().contains("LT")) {
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_THUMB;

                            }
                            if (arcDetails.getFingers().contains("RT")) {
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_THUMB;

                            }
                            //Changed on 21/04/22
                            //fingerType = RealScan_JNI.RS_FGP_RIGHT_THUMB;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_LEFT_FOUR_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_LEFT_FOUR;
                            if (arcDetails.getFingers().contains("LL"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_LITTLE;
                            if (arcDetails.getFingers().contains("LR"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_RING;
                            if (arcDetails.getFingers().contains("LM"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_MIDDLE;
                            if (arcDetails.getFingers().contains("LI"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_LEFT_INDEX;
                            fingerType = RealScan_JNI.RS_FGP_LEFT_LITTLE;
                            break;
                        case RealScan_JNI.RS_CAPTURE_FLAT_RIGHT_FOUR_FINGERS:
                            slapType = RealScan_JNI.RS_SLAP_RIGHT_FOUR;
                            if (arcDetails.getFingers().contains("RI"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_INDEX;
                            if (arcDetails.getFingers().contains("RM"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_MIDDLE;
                            if (arcDetails.getFingers().contains("RR"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_RING;
                            if (arcDetails.getFingers().contains("RL"))
                                missingFingerArr[n++] = RealScan_JNI.RS_FGP_RIGHT_LITTLE;
                            fingerType = RealScan_JNI.RS_FGP_RIGHT_INDEX;
                            break;

                        default:
                            statusField.setText("Cannot segment in this mode");
                            thumbScan.setDisable(false);
                            return;
                    }
                    // 7) do sequence check & save target image(right) of sequence check
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Sequence Checking is running now...");
                        }
                    });
                    numOfFingers = RealScan_JNI.RS_Segment(imageInfo, RealScan_JNI.RS_SLAP_TWO_THUMB, numOfFingers, slapInfoArray, imageInfoArray);
                    //Finger Counts Mismatch

                    System.out.println("numOf--Fingers:::" + numOfFingers);
                    System.out.println("thumbfingerstoscan:::" + thumbfingerstoscan());
                    if (numOfFingers != thumbfingerstoscan()) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Fingers counts not Matched. Try Thumb Fingers again");
                                LOGGER.log(Level.SEVERE, "Fingers counts not Matched. Try Thumb Fingers again");
                                thumbScan.setDisable(false);
                                backBtn.setDisable(false);
                            }
                        });

                        return;
                    }

                    resString = sequenceCheckProcess2(imageInfoArray, numOfFingers, "thumb");

                    if (resString.contains("There is no target image")) {
                        System.out.println("ResTring:::" + resString);
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    } else if (!resString.contains("There isn't matched finger")) {
                        System.out.println("ResTring:::" + resString);
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }
                    System.out.println("ResTring:::" + resString);

                    for (int i = 0; i < pFingerMsg.length; i++) {
                        if (resString.equals(pFingerMsg[i])) {
                            statusField.setText(pFingerMsg[i] + " finger is already scanned!");
                            thumbScan.setDisable(false);
                            backBtn.setDisable(false);
                            return;
                        }
                    }
                    if (numOfFingers < 0) {
                        statusField.setText("Segmentation failed");
                        LOGGER.log(Level.SEVERE, "Thumbs Segmentation failed");
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }
                    System.out.println("NO OF FINGERS:::::" + numOfFingers);

                    StringBuilder segResult = new StringBuilder();
                    int j = 0;
                    //Commented on 21/04/22
                    //fingerType = RealScan_JNI.RS_FGP_RIGHT_THUMB;
                    System.out.println("FINGER TYPE:::::" + fingerType);
                    for (int i = 0; i < numOfFingers; i++) {
                        if (slapInfoArray[i].fingerType == RealScan_JNI.RS_FGP_UNKNOWN) {
                            System.out.println("inside if");
                            if (slapType == RealScan_JNI.RS_SLAP_LEFT_FOUR) {
                                while (fingerType == missingFingerArr[j]) {
                                    fingerType--;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType--;
                            } else if (slapType == RealScan_JNI.RS_SLAP_RIGHT_FOUR) {
                                while (fingerType == missingFingerArr[j]) {
                                    fingerType++;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType++;
                            } else if (slapType == RealScan_JNI.RS_SLAP_TWO_THUMB) {
                                //System.out.println("finger thumb " + fingerType + " " +missingFingerArr[j]);
                                while (fingerType == missingFingerArr[j]) {
                                    //System.out.println("inside missing finger thumb");
                                    fingerType++;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType++;
                            } else if (slapType == RealScan_JNI.RS_SLAP_ONE_FINGER) {
                                System.out.println("finger Type and missing finger-- thumb " + fingerType + " " + missingFingerArr[j]);
                                while (fingerType == missingFingerArr[j]) {
                                    System.out.println("inside missing finger thumb");
                                    fingerType++;
                                    j++;
                                }
                                slapInfoArray[i].fingerType = fingerType++;
                            }
                        } else {
                            //System.out.println("seg result else:" + segResult + " " + numOfFingers);
                        }
                        segResult.append("[" + slapInfoArray[i].fingerType + ":" + slapInfoArray[i].imageQuality + "]");
                    }
                    System.out.println("seg result :" + segResult + " " + numOfFingers);
                    saveSeqCheckTargetImages(imageInfo.pbyImgBuf, imageInfo.imageWidth, imageInfo.imageHeight, RealScan_JNI.RS_SLAP_TWO_THUMB, numOfFingers);

                    try {
                        setSegmentedImagePanel(RealScan_JNI.RS_CAPTURE_FLAT_TWO_FINGERS, slapInfoArray, imageInfoArray, numOfFingers, "thumb");
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                statusField.setText("Could not segment images. Try thumb again");
                                LOGGER.log(Level.SEVERE, "Thumb Segmentation failed");
                            }
                        });
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                        return;
                    }

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Thumbs captured successfully");
                            LOGGER.log(Level.INFO, "thumbs captured successfully");
                            if (captureModesCompleted.contains(3)) {
                                captureModesCompleted.remove(3);
                            }
                            captureModesCompleted.add(3);
                            System.out.println("capture size thumb :" + captureModesCompleted.size());
                            if (captureModesCompleted.size() == 3) {
                                showIrisBtn.setDisable(true);
                                scan.setDisable(true);
                                scan.setDisable(true);
                                leftScan.setDisable(true);
                                rightScan.setDisable(true);
                                thumbScan.setDisable(true);
                                backBtn.setDisable(true);
                            }
                        }
                    });

                    System.out.println("thumbs captured successfully3");
                    LOGGER.log(Level.INFO, "thumbs captured successfully3");
                    statusMsg("Thumb Capture Success");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    LOGGER.log(Level.INFO, "before calling Template Conversion if");
                    //Template Conversion
                    ttemplateconvert = new Thread(DoTemplateConversion);
                    ttemplateconvert.start();
                    //Template Conversion
                    LOGGER.log(Level.INFO, "After calling Template Conversion if");
                } else {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            // statusField.setText("Thumbs capture Not required, Proceed For Iris");
                            statusField.setText("Thumb capture not required, proceed next to Capture IRIS");
                            scan.setDisable(true);
                            showIrisBtn.setDisable(true);
                            if (captureModesCompleted.contains(3)) {
                                captureModesCompleted.remove(3);
                            }
                            captureModesCompleted.add(3);
                            System.out.println("capture size thumb :" + captureModesCompleted.size());
                            if (captureModesCompleted.size() == 3) {
                                showIrisBtn.setDisable(false);
                                scan.setDisable(true);
                                leftScan.setDisable(true);
                                rightScan.setDisable(true);
                                thumbScan.setDisable(true);
                                backBtn.setDisable(true);
                            }
                        }
                    });
                    System.out.println("Thumb capture Not required, enabling iris capture");
                    LOGGER.log(Level.INFO, "Thumbs capture Not required, enabling iris capture");
                    statusMsg("Thumb Capture Not Required");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SlapScannerController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    LOGGER.log(Level.INFO, "before calling Template Conversion else");
                    //Template Conversion
                    ttemplateconvert = new Thread(DoTemplateConversion);
                    ttemplateconvert.start();
                    //Template Conversion
                    LOGGER.log(Level.INFO, "After calling Template Conversion else");
                    //showIrisBtn.setDisable(false);
                }
            } catch (Exception e) {
                statusField.setText("Exception:" + e);
            }
        }
    };


    //New Implementation
    public void clearSegmentedImagePanels(String[] clearImagePanel) {
        //String[] clearImagePanel = {"l","r","t"};


        if (clearImagePanel.toString().contains("l")) {
            for (int i = 7; i <= 10; i++) {
                segFPImages[i - 1].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
                segFPImages[i - 1].image = null;
                segFPImages[i - 1].rawdata = null;
                segFPImages[i - 1].width = 0;
                segFPImages[i - 1].height = 0;
            }
            LICanvas.setImage(null);
            LMCanvas.setImage(null);
            LRCanvas.setImage(null);
            LLCanvas.setImage(null);
        }

        if (clearImagePanel.toString().contains("r")) {
            for (int i = 2; i <= 5; i++) {
                segFPImages[i - 1].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
                segFPImages[i - 1].image = null;
                segFPImages[i - 1].rawdata = null;
                segFPImages[i - 1].width = 0;
                segFPImages[i - 1].height = 0;
            }
            RICanvas.setImage(null);
            RMCanvas.setImage(null);
            RRCanvas.setImage(null);
            RLCanvas.setImage(null);
        }

        if (clearImagePanel.toString().contains("t")) {
            //RT
            segFPImages[0].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
            segFPImages[0].image = null;
            segFPImages[0].rawdata = null;
            segFPImages[0].width = 0;
            segFPImages[0].height = 0;
            RTCanvas.setImage(null);

            //LT
            segFPImages[5].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
            segFPImages[5].image = null;
            segFPImages[5].rawdata = null;
            segFPImages[5].width = 0;
            segFPImages[5].height = 0;
            LTCanvas.setImage(null);

        }

    }

    //This method is not in use currently, instead public void clearSegmentedImagePanels(String[] clearImagePanel) used
    public void clearSegmentedImagePanels() {
        for (int i = 0; i < segFPImages.length; i++) {
            segFPImages[i].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
            segFPImages[i].image = null;
            segFPImages[i].rawdata = null;
            segFPImages[i].width = 0;
            segFPImages[i].height = 0;
        }

        //LTCanvas.main_img = null;
        LTCanvas.setImage(null);
        //LICanvas.main_img = null;
        LICanvas.setImage(null);
        //LMCanvas.main_img = null;
        LMCanvas.setImage(null);
        //LRCanvas.main_img = null;
        LRCanvas.setImage(null);
        //LLCanvas.main_img = null;
        LLCanvas.setImage(null);

        //RTCanvas.main_img = null;
        RTCanvas.setImage(null);
        //RICanvas.main_img = null;
        RICanvas.setImage(null);
        //RMCanvas.main_img = null;
        RMCanvas.setImage(null);
        //RRCanvas.main_img = null;
        RRCanvas.setImage(null);
        //RLCanvas.main_img = null;
        RLCanvas.setImage(null);
    }

    public void clearSegmentedImagePanelsCheck(String Hand) {
        for (int i = 0; i < segFPImages.length; i++) {
            segFPImages[i].fingerPosition = RealScan_JNI.RS_FGP_UNKNOWN;
            segFPImages[i].image = null;
            segFPImages[i].rawdata = null;
            segFPImages[i].width = 0;
            segFPImages[i].height = 0;
        }
        if (Hand.contains("Left")) {
            //LTCanvas.main_img = null;
            LTCanvas.setImage(null);
            //LICanvas.main_img = null;
            LICanvas.setImage(null);
            //LMCanvas.main_img = null;
            LMCanvas.setImage(null);
            //LRCanvas.main_img = null;
            LRCanvas.setImage(null);
            //LLCanvas.main_img = null;
            LLCanvas.setImage(null);
        } else {
            //RTCanvas.main_img = null;
            RTCanvas.setImage(null);
            //RICanvas.main_img = null;
            RICanvas.setImage(null);
            //RMCanvas.main_img = null;
            RMCanvas.setImage(null);
            //RRCanvas.main_img = null;
            RRCanvas.setImage(null);
            //RLCanvas.main_img = null;
            RLCanvas.setImage(null);
        }
    }

    public void clearSeqCheckTargetImages() {
        nNumOfTargets = 0;
        nNumOfTargetFingers = 0;

        for (int i = 0; i < 5; i++) {
            if (bSeqCheckTargetImages[i] != null) {
                bSeqCheckTargetImages[i] = null;
            }
            nSeqCheckTargetWidths[i] = 0;
            nSeqCheckTargetHeights[i] = 0;
            nSeqCheckTargetSlapTypes[i] = 0;
        }
    }


    public void setImagePanel(byte[] imgByte, int imageWidth, int imageHeight) {
        int nWidth = imageWidth;
        int nHeight = imageHeight;
        if (nWidth % 4 != 0) nWidth -= nWidth % 4;
        byte[] bData = new byte[nWidth * nHeight];
        for (int i = 0; i < nHeight; i++) {
            for (int j = 0; j < nWidth; j++)
                bData[i * nWidth + j] = imgByte[i * imageWidth + j];
        }

        byte[] imageBuffer = new byte[nWidth * nHeight + 1078];

        int result = RealScan_JNI.RS_SaveBitmapMem(bData, nWidth, nHeight, imageBuffer);
        if (result == RealScan_JNI.RS_SUCCESS) {
            try {
                synchronized (syncImg) {
                    InputStream in = new ByteArrayInputStream(imageBuffer);
                    BufferedImage bufferedImage = ImageIO.read(in);
                    //System.out.println("BUFFFF"+bufferedImage.getData());
//                    icon.setImage(bufferedImage, qty, x, y, r);

                    WritableImage wr = null;
                    if (bufferedImage != null) {
                        // System.out.println("BUFDREA not null");
                        wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                        PixelWriter pw = wr.getPixelWriter();
                        for (int x = 0; x < bufferedImage.getWidth(); x++) {
                            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                                pw.setArgb(x, y, bufferedImage.getRGB(x, y));
                            }
                        }
                    }
                    img = wr;
                }
            } catch (java.io.IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to set image in image panel");
                e.printStackTrace();
            }

            Platform.runLater(showPrevData);
        }
    }

    Runnable showPrevData = new Runnable() {
        public void run() {
            synchronized (syncImg) {
                preview.setImage(img);
            }
            preview.setImage(img);

        }
    };

    public void setSegmentedImagePanel(int slapType, RealScan_JNI.RSSlapInfo[] slapInfo, RealScan_JNI.RSImageInfo[] imageInfo, int numOfFingers, String device) throws ArrayIndexOutOfBoundsException {

        whichdevice = device;
        System.out.println("slapType 123: SlapInfo : ImageInfo : numFingers" + slapType + " : " + slapInfo.toString() + " : " + imageInfo.toString() + " : " + numOfFingers);

        if (slapType == RealScan_JNI.RS_SLAP_ONE_FINGER) { //For Single Finger
            for (int i = 0; i < numOfFingers; i++) {
                System.out.println("width : height : " + i + " finger :  " + imageInfo[i].imageWidth + " : " + imageInfo[i].imageHeight + "Finger type:" + slapInfo[i].fingerType);
                System.out.println("setSegmentedImagePanel for Single Finger");
                System.out.println("width : height : " + i + " finger :  " + imageInfo[i].imageWidth + " : " + imageInfo[i].imageHeight);
                int idx = slapInfo[i].fingerType - 1;
                /*    
                int idx = whichdevice.contains("left")?fingertypeleft():fingertyperight();//slapInfo[i].fingerType;//Added For Single Finger
                if(idx==10){ //IDX value for left little is 10 
                   idx = 9; // Added for Left Little
                }
                if(idx==5){ //IDX value for left little is 10 
                   idx = 4; // Added for Right Little
                }*/

                System.err.println("IDX Value in set Segment for single finger:" + idx);
                if (idx < 0) {
                    System.out.println("width : height : " + idx + " finger :  " + imageInfo[i].imageWidth + " : " + imageInfo[i].imageHeight);
                    //continue;
                }
                System.err.println("before width");
                System.out.println("Image Info Length:" + imageInfo.length);
                int nWidth = imageInfo[i].imageWidth;
                System.err.println("after width" + imageInfo.toString());
                int nHeight = imageInfo[i].imageHeight;
                System.err.println("before");
                System.out.println("segFP : length : " + segFPImages[idx] + " " + segFPImages.length);
                segFPImages[idx].width = nWidth;
                segFPImages[idx].height = nHeight;
                segFPImages[idx].rawdata = new byte[nWidth * nHeight];
                System.arraycopy(imageInfo[i].pbyImgBuf, 0, segFPImages[idx].rawdata, 0, nWidth * nHeight);

                if (nWidth % 4 != 0) nWidth -= nWidth % 4;
                byte[] bData = new byte[nWidth * nHeight];
                byte[] bmpBuf = new byte[nWidth * nHeight + 1078];

                for (int n = 0; n < nHeight; n++) {
                    for (int m = 0; m < nWidth; m++) {
                        bData[n * nWidth + m] = imageInfo[i].pbyImgBuf[n * imageInfo[i].imageWidth + m];
                    }
                }

                //String strIrisLeft = Base64.getEncoder().encodeToString(finalLeftBuffer);
                int result = RealScan_JNI.RS_SaveBitmapMem(bData, nWidth, nHeight, bmpBuf);
                if (result == RealScan_JNI.RS_SUCCESS) {
                    try {
                        segFPImages[idx].fingerPosition = slapInfo[i].fingerType;
                        InputStream in = new ByteArrayInputStream(bmpBuf);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        // System.out.println("BUFFFF"+bufferedImage.getData());
                        //icon.setImage(bufferedImage, qty, x, y, r);

                        WritableImage wr = null;
                        if (bufferedImage != null) {
                            //  System.out.println("BUFDREA not null");
                            wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                            PixelWriter pw = wr.getPixelWriter();
                            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                                    pw.setArgb(x, y, bufferedImage.getRGB(x, y));
                                }
                            }
                        }
                        segFPImages[idx].image = wr;
                        if (wr == null) {
                            System.err.println("Null value for i = " + i);
                        }

                        System.out.println("SLAP Single Finger::" + "raw data " + i + " raw data image " + segFPImages[i].rawdata + " slap type " + segFPImages[i].fingerPosition);

                        //  System.out.println("raw data "+ i + " raw data image "+segFPImages[i].rawdata +" slap type " + segFPImages[i].fingerPosition );
                        //segFPImages[idx].image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bmpBuf));

                    } catch (java.io.IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to set segmented image in image panel");
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }

                if (!whichdevice.equals("left")) {
                    clearImagePanel[0] = "l";
                    clearImagePanel[1] = "";
                    clearImagePanel[2] = "";
                }
                if (!whichdevice.equals("right")) {
                    clearImagePanel[0] = "l";
                    clearImagePanel[1] = "r";
                    clearImagePanel[2] = "";
                }

                if (!whichdevice.equals("thumb")) {
                    clearImagePanel[0] = "l";
                    clearImagePanel[1] = "r";
                    clearImagePanel[2] = "t";
                }

                //enabled in portable enrollment
                Platform.runLater(showSegmentFP);
            }

        } else {
            System.out.println("numofFingers in setsegmentedpanel - four finger" + numOfFingers);
            for (int i = 0; i < numOfFingers; i++) {
                System.out.println("setSegmentedImagePanel");
                System.out.println("width : height : " + i + " finger :  " + imageInfo[i].imageWidth + " : " + imageInfo[i].imageHeight + "Finger type:" + slapInfo[i].fingerType);
                int idx = slapInfo[i].fingerType - 1;
                if (idx < 0) {
                    System.out.println("width : height : " + idx + " finger :  " + imageInfo[i].imageWidth + " : " + imageInfo[i].imageHeight);
                    continue;
                }
                System.err.println("before width");
                System.out.println("Image Info Length:" + imageInfo.length);
                int nWidth = imageInfo[i].imageWidth;
                System.err.println("after width" + imageInfo.toString());
                int nHeight = imageInfo[i].imageHeight;
                System.err.println("before");
                //System.out.println("segFP : length : "+ segFPImages[idx] + " "+ segFPImages.length);
                segFPImages[idx].width = nWidth;
                segFPImages[idx].height = nHeight;
                segFPImages[idx].rawdata = new byte[nWidth * nHeight];
                System.arraycopy(imageInfo[i].pbyImgBuf, 0, segFPImages[idx].rawdata, 0, nWidth * nHeight);

                if (nWidth % 4 != 0) nWidth -= nWidth % 4;
                byte[] bData = new byte[nWidth * nHeight];
                byte[] bmpBuf = new byte[nWidth * nHeight + 1078];

                for (int n = 0; n < nHeight; n++) {
                    for (int m = 0; m < nWidth; m++) {
                        bData[n * nWidth + m] = imageInfo[i].pbyImgBuf[n * imageInfo[i].imageWidth + m];
                    }
                }

                //String strIrisLeft = Base64.getEncoder().encodeToString(finalLeftBuffer);
                int result = RealScan_JNI.RS_SaveBitmapMem(bData, nWidth, nHeight, bmpBuf);
                if (result == RealScan_JNI.RS_SUCCESS) {
                    try {
                        segFPImages[idx].fingerPosition = slapInfo[i].fingerType;
                        InputStream in = new ByteArrayInputStream(bmpBuf);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        // System.out.println("BUFFFF"+bufferedImage.getData());
                        //                    icon.setImage(bufferedImage, qty, x, y, r);

                        WritableImage wr = null;
                        if (bufferedImage != null) {
                            //  System.out.println("BUFDREA not null");
                            wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
                            PixelWriter pw = wr.getPixelWriter();
                            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                                    pw.setArgb(x, y, bufferedImage.getRGB(x, y));
                                }
                            }
                        }
                        segFPImages[idx].image = wr;
                        if (wr == null) {
                            System.err.println("Null value for i = " + i);
                        }

                        //  System.out.println("raw data "+ i + " raw data image "+segFPImages[i].rawdata +" slap type " + segFPImages[i].fingerPosition );
                        //segFPImages[idx].image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bmpBuf));

                    } catch (java.io.IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to set segmented image in image panel");
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }

                if (!whichdevice.equals("left")) {
                    clearImagePanel[0] = "l";
                    clearImagePanel[1] = "";
                    clearImagePanel[2] = "";
                }
                if (!whichdevice.equals("right")) {
                    clearImagePanel[0] = "l";
                    clearImagePanel[1] = "r";
                    clearImagePanel[2] = "";
                }

                if (!whichdevice.equals("thumb")) {
                    clearImagePanel[0] = "l";
                    clearImagePanel[1] = "r";
                    clearImagePanel[2] = "t";
                }


                //enabled in portable enrollment
                Platform.runLater(showSegmentFP);
            }
        }
    }

    Runnable showSegmentFP = new Runnable() {
        public void run() {
            int fingerstoscan = 0;
            if (whichdevice.contains("left")) { //To check the device in left
                fingerstoscan = leftfingerstoscan();
                System.out.println("Fingers To scan Left:" + fingerstoscan);
            } else if (whichdevice.contains("right")) {
                fingerstoscan = rightfingerstoscan();
                System.out.println("Fingers To scan Right:" + fingerstoscan);
            }
            System.out.println("Fingers to scan in Showsegment:" + fingerstoscan);
            if (fingerstoscan == 1) {
                System.out.println("show fingerprint fp :" + segFPImages.length);
                int fingerType = 0;
                System.out.println("Show segment For Single Finger");

                for (int i = 0; i < segFPImages.length; i++) {
                    String value = segFPImages[i].image != null ? "has value" : "null";
                    System.out.println("i=" + i + "slaptype" + segFPImages[i].fingerPosition + "value" + value);
                    if (segFPImages[i].image != null) {
                        if (whichdevice.contains("left")) {
                            fingerType = fingertypeleft(); // For Single Finger
                            System.out.println("FingerType FOr Left:" + fingerType);
                        } else if (whichdevice.contains("right")) {
                            fingerType = fingertyperight(); // For Single Finger
                            System.out.println("FingerType FOr Right:" + fingerType);
                        }
                        //segFPImages[i].fingerPosition=fingerType;
                        //segFPImages[i].fingerPosition=i;
                        switch (segFPImages[i].fingerPosition) {
                            case RealScan_JNI.RS_FGP_LEFT_INDEX:
                                //LICanvas.main_img = segFPImages[i].image;
                                LICanvas.setImage(segFPImages[i].image);
                                break;
                            case RealScan_JNI.RS_FGP_LEFT_MIDDLE:
                                //LMCanvas.main_img = segFPImages[i].image;
                                LMCanvas.setImage(segFPImages[i].image);
                                break;
                            case RealScan_JNI.RS_FGP_LEFT_RING:
                                // LRCanvas.main_img = segFPImages[i].image;
                                LRCanvas.setImage(segFPImages[i].image);
                                break;
                            case RealScan_JNI.RS_FGP_LEFT_LITTLE:
                                System.out.println("Setting left little Image:" + i);
                                //LLCanvas.main_img = segFPImages[i].image;
                                LLCanvas.setImage(segFPImages[i].image);
                                break;
                            case RealScan_JNI.RS_FGP_RIGHT_THUMB:
                                System.out.println("Setting right thumb Image" + whichdevice);
                                //RTCanvas.main_img = segFPImages[i].image;
                                if (whichdevice.equals("thumb")) {
                                    RTCanvas.setImage(segFPImages[i].image);
                                }
                                break;
                            case RealScan_JNI.RS_FGP_RIGHT_INDEX:
                                //RICanvas.main_img = segFPImages[i].image;  
                                if (RICanvas.getImage() == null) {
                                    RICanvas.setImage(segFPImages[i].image);
                                }
                                break;
                            case RealScan_JNI.RS_FGP_RIGHT_MIDDLE:
                                //RMCanvas.main_img = segFPImages[i].image;
                                if (RMCanvas.getImage() == null) {
                                    RMCanvas.setImage(segFPImages[i].image);
                                }
                                break;
                            case RealScan_JNI.RS_FGP_RIGHT_RING:
                                //RRCanvas.main_img = segFPImages[i].image;  
                                if (RICanvas.getImage() == null) {
                                    RRCanvas.setImage(segFPImages[i].image);
                                }
                                break;
                            case RealScan_JNI.RS_FGP_RIGHT_LITTLE:
                                //RLCanvas.main_img = segFPImages[i].image;   
                                if (RICanvas.getImage() == null) {
                                    RLCanvas.setImage(segFPImages[i].image);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }

            } else {

                System.out.println("show fingerprint fp :" + segFPImages.length);
                for (int i = 0; i < segFPImages.length; i++) {
                    String value = segFPImages[i].image != null ? "has value" : "null";
                    System.out.println("i=" + i + "slaptype" + segFPImages[i].fingerPosition + "value" + value);
                    if (segFPImages[i].image != null) {
                        switch (segFPImages[i].fingerPosition) {
                            case RealScan_JNI.RS_FGP_LEFT_THUMB:
                                System.out.println("Setting Left thumb Image" + whichdevice);
                                //LTCanvas.main_img = segFPImages[i].image;
                                if (whichdevice.equals("thumb")) {
                                    LTCanvas.setImage(segFPImages[i].image);
                                }
                                break;

                            case RealScan_JNI.RS_FGP_LEFT_INDEX:
                                //LICanvas.main_img = segFPImages[i].image;
                                LICanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_LEFT_MIDDLE:
                                //LMCanvas.main_img = segFPImages[i].image;
                                LMCanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_LEFT_RING:
                                // LRCanvas.main_img = segFPImages[i].image;
                                LRCanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_LEFT_LITTLE:
                                //LLCanvas.main_img = segFPImages[i].image;
                                LLCanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_RIGHT_THUMB:
                                //RTCanvas.main_img = segFPImages[i].image;
                                System.out.println("Setting Left thumb Image" + whichdevice);
                                if (whichdevice.equals("thumb")) {
                                    RTCanvas.setImage(segFPImages[i].image);
                                }
                                break;

                            case RealScan_JNI.RS_FGP_RIGHT_INDEX:
                                //RICanvas.main_img = segFPImages[i].image;
                                RICanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_RIGHT_MIDDLE:
                                //RMCanvas.main_img = segFPImages[i].image;
                                RMCanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_RIGHT_RING:
                                //RRCanvas.main_img = segFPImages[i].image;
                                RRCanvas.setImage(segFPImages[i].image);
                                break;

                            case RealScan_JNI.RS_FGP_RIGHT_LITTLE:
                                //RLCanvas.main_img = segFPImages[i].image;
                                RLCanvas.setImage(segFPImages[i].image);
                                break;

                            default:
                                break;
                        }
                    }
                }
            }
        }
    };

    //public void saveEnrollmentDetails(SaveEnrollmentDetails saveEnrollment, int fingerPosition, byte[] bData, byte[] bmpData) {
    public void saveEnrollmentDetails(SaveEnrollmentDetails saveEnrollment, int fingerPosition, byte[] image, byte[] template2011) {
        System.out.println("slap type in save :" + fingerPosition);
        switch (fingerPosition) {
            case RealScan_JNI.RS_FGP_LEFT_THUMB:
                //LTCanvas.main_img = segFPImages[i].image;
                FP fplt = new FP();
                fplt.setPosition("LT");
                fplt.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fplt.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fplt);
                //System.out.println("fp add ");
                break;
            case RealScan_JNI.RS_FGP_LEFT_INDEX:
                //LICanvas.main_img = segFPImages[i].image;
                FP fpli = new FP();
                fpli.setPosition("LI");
                fpli.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fpli.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fpli);
                break;
            case RealScan_JNI.RS_FGP_LEFT_MIDDLE:
                //LMCanvas.main_img = segFPImages[i].image;
                FP fplm = new FP();
                fplm.setPosition("LM");
                fplm.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fplm.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fplm);
                break;
            case RealScan_JNI.RS_FGP_LEFT_RING:
                // LRCanvas.main_img = segFPImages[i].image;
                FP fplr = new FP();
                fplr.setPosition("LR");
                fplr.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fplr.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fplr);
                break;
            case RealScan_JNI.RS_FGP_LEFT_LITTLE:
                //LLCanvas.main_img = segFPImages[i].image;
                FP fpll = new FP();
                fpll.setPosition("LL");
                fpll.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fpll.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fpll);
                break;
            case RealScan_JNI.RS_FGP_RIGHT_THUMB:
                //RTCanvas.main_img = segFPImages[i].image;
                FP fprt = new FP();
                fprt.setPosition("RT");
                fprt.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fprt.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fprt);
                break;
            case RealScan_JNI.RS_FGP_RIGHT_INDEX:
                //RICanvas.main_img = segFPImages[i].image;
                FP fpri = new FP();
                fpri.setPosition("RI");
                fpri.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fpri.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fpri);
                break;
            case RealScan_JNI.RS_FGP_RIGHT_MIDDLE:
                //RMCanvas.main_img = segFPImages[i].image;
                FP fprm = new FP();
                fprm.setPosition("RM");
                fprm.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fprm.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fprm);
                break;
            case RealScan_JNI.RS_FGP_RIGHT_RING:
                //RRCanvas.main_img = segFPImages[i].image;
                FP fprr = new FP();
                fprr.setPosition("RR");
                fprr.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fprr.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fprr);
                break;
            case RealScan_JNI.RS_FGP_RIGHT_LITTLE:
                //RLCanvas.main_img = segFPImages[i].image;
                FP fprl = new FP();
                fprl.setPosition("RL");
                fprl.setTemplate(Base64.getEncoder().encodeToString(template2011));
                fprl.setImage(Base64.getEncoder().encodeToString(image));
                fingerPrintSet.add(fprl);
                break;
            default:
                break;
        }

    }

    public void saveSeqCheckTargetImages(byte[] imgByte, int imageWidth, int imageHeight, int slapType, int fingerCount) {
        if (slapType < RealScan_JNI.RS_SLAP_ONE_FINGER) {
            boolean bIsOverlapped = false;
            int nOverlappedIndex = nNumOfTargets;
            for (int i = 0; i < nNumOfTargets; i++) {
                if (nSeqCheckTargetSlapTypes[i] == slapType) {
                    bIsOverlapped = true;
                    nOverlappedIndex = i;
                    break;
                }
            }

            if (bIsOverlapped == true) {
                bSeqCheckTargetImages[nOverlappedIndex] = imgByte;
                nSeqCheckTargetWidths[nOverlappedIndex] = imageWidth;
                nSeqCheckTargetHeights[nOverlappedIndex] = imageHeight;
            } else {
                bSeqCheckTargetImages[nNumOfTargets] = new byte[imageWidth * imageHeight];
                System.arraycopy(imgByte, 0, bSeqCheckTargetImages[nNumOfTargets], 0, imageWidth * imageHeight);
                nSeqCheckTargetWidths[nNumOfTargets] = imageWidth;
                nSeqCheckTargetHeights[nNumOfTargets] = imageHeight;
                nSeqCheckTargetSlapTypes[nNumOfTargets] = slapType;
                System.out.println("slaptype : " + slapType);
                switch (slapType) {
                    case RealScan_JNI.RS_SLAP_TWO_THUMB:
                        listData.addElement("TWO THUMBS");
                        break;
                    case RealScan_JNI.RS_SLAP_LEFT_FOUR:
                        listData.addElement("LEFT FOUR FINGERS");
                        break;
                    case RealScan_JNI.RS_SLAP_RIGHT_FOUR:
                        listData.addElement("RIGHT FOUR FINGERS");
                        break;
                }
                seqTargetList.add(listData);

                nNumOfTargets++;
                System.err.println("nNumofTargets = " + nNumOfTargets);
                nNumOfTargetFingers += fingerCount;
            }
        }
        System.out.println("nNumOfTargetFingers : " + nNumOfTargetFingers);
    }

    public void sequenceCheckProcess(byte[] imageData, int imageWidth, int imageHeight) {
        int seqCheckResult = -1;
        int nSecuLev = 5;    // (0~7)
        int numOfSlaps = seqTargetList.size();

        if (numOfSlaps == 0) statusField.setText("There is no target image");
        else {
            boolean bIsMatched = false;
            for (int i = 0; i < numOfSlaps; i++) {
                RealScan_JNI.RSImageInfo fingerImageInfo = new RealScan_JNI.RSImageInfo();
                fingerImageInfo.pbyImgBuf = new byte[imageWidth * imageHeight];
                fingerImageInfo.pbyImgBuf = imageData;
                fingerImageInfo.imageWidth = imageWidth;
                fingerImageInfo.imageHeight = imageHeight;

                seqCheckResult = RealScan_JNI.RS_SequenceCheck(1, fingerImageInfo, bSeqCheckTargetImages[i], nSeqCheckTargetWidths[i], nSeqCheckTargetHeights[i], nSeqCheckTargetSlapTypes[i], nSecuLev);

                if (seqCheckResult > 0) {
                    String seqRetMsg = "This finger is the " + pFingerMsg[seqCheckResult] + " finger";
                    statusField.setText(seqRetMsg);
                    bIsMatched = true;
                    break;
                }
            }

            if (!bIsMatched) statusField.setText("There isn't matched finger");
        }
        LOGGER.log(Level.INFO, "Sequence check process completed");
    }

    public String sequenceCheckProcess2(RealScan_JNI.RSImageInfo[] imageInfo, int numOfFingers, String finger) {
        LOGGER.log(Level.INFO, "Sequence check process2 started");
        String retString = "There is no target image";

        int seqCheckResult = -1;
        int seqCheckResult1 = -1;
        int nSecuLev = 5;    // (0~7)
        /*
        if(nNumOfTargets == 0){
            System.out.println("nNumOfTargets is 0 :"+nNumOfTargets);
            return retString;
        }*/

        System.out.println("numOfFingers is  :" + numOfFingers + " nNumOfTargets is 0 :" + nNumOfTargets);
        boolean bIsMatched = false;
        for (int i = 0; i < nNumOfTargets; i++) {
            for (int j = 0; j < numOfFingers; j++) {
                RealScan_JNI.RSImageInfo fingerImageInfo = new RealScan_JNI.RSImageInfo();
                int nWidth = imageInfo[j].imageWidth;
                int nHeight = imageInfo[j].imageHeight;
                fingerImageInfo.pbyImgBuf = new byte[nWidth * nHeight];
                fingerImageInfo.pbyImgBuf = imageInfo[j].pbyImgBuf;
                fingerImageInfo.imageWidth = nWidth;
                fingerImageInfo.imageHeight = nHeight;

                seqCheckResult = RealScan_JNI.RS_SequenceCheck(1, fingerImageInfo, bSeqCheckTargetImages[i], nSeqCheckTargetWidths[i], nSeqCheckTargetHeights[i], nSeqCheckTargetSlapTypes[i], nSecuLev);
                System.out.println("i:" + i + " j:" + j + " seqCheckResult" + seqCheckResult + "Slap:" + nSeqCheckTargetSlapTypes[i] + "noofTarget:" + nNumOfTargets);
                if (seqCheckResult > 0) {
                    retString = pFingerMsg[seqCheckResult];
                    bIsMatched = true;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            statusField.setText("Kindly Place the Fingers and Try Again");
                        }
                    });
                    if (finger.contains("right")) {
                        rightScan.setDisable(false);
                        backBtn.setDisable(false);
                    } else {
                        thumbScan.setDisable(false);
                        backBtn.setDisable(false);
                    }
                    break;
                }
            }
        }

        if (!bIsMatched) retString = "There isn't matched finger"; //If they are placing fingers are not unique
        LOGGER.log(Level.INFO, "Sequence check process2 completed");
        //System.out.println("Return String::"+retString);
        return retString;
    }

    public void initISOSDK() throws IOException {
        String fileName = "/etc/licence/iengine.lic";
        ClassLoader classLoader = getClass().getClassLoader();

        File file = new File(fileName);
        if (!file.exists()) {
            statusField.setText("Licence file is not found");
            LOGGER.log(Level.SEVERE, "Licence file is not found");
            return;
        }

        //File is found
        System.out.println("Licence File Found : " + file.exists());

        //Read File Content
        String content = new String(Files.readAllBytes(file.toPath()));
        System.out.println("content :" + content);
        String hardwareID = ansiISO.getHardwareId().toString();
        System.out.println("hardware ID :" + hardwareID);
        try {
            ansiISO.setLicenseContent(Files.readAllBytes(file.toPath()), Files.readAllBytes(file.toPath()).length);
            ansiISO.init();
        } catch (RuntimeException e) {
            System.out.println("Runtime exception" + e.toString());
            System.out.println("Runtime exception" + e.toString());
            String[] tokens = e.toString().split(":");
            System.out.println("Runtime exception" + tokens[1] + " " + tokens[2]);
            statusField.setText("Error : " + tokens[2]);
        }
    }

    public int leftfingerstoscan() {
        int leftmissingfingers = 0;
        if (arcDetails.getFingers().contains("LI")) leftmissingfingers++;
        if (arcDetails.getFingers().contains("LM")) leftmissingfingers++;
        if (arcDetails.getFingers().contains("LR")) leftmissingfingers++;
        if (arcDetails.getFingers().contains("LL")) leftmissingfingers++;
        int fingerstoscan = 4 - leftmissingfingers;
        return fingerstoscan;
    }

    public int rightfingerstoscan() {
        int rightmissingfingers = 0;
        if (arcDetails.getFingers().contains("RI")) rightmissingfingers++;
        if (arcDetails.getFingers().contains("RM")) rightmissingfingers++;
        if (arcDetails.getFingers().contains("RR")) rightmissingfingers++;
        if (arcDetails.getFingers().contains("RL")) rightmissingfingers++;
        int fingerstoscan = 4 - rightmissingfingers;
        return fingerstoscan;
    }

    public int thumbfingerstoscan() {
        int thumbmissingfingers = 0;
        if (arcDetails.getFingers().contains("LT")) thumbmissingfingers++;
        if (arcDetails.getFingers().contains("RT")) thumbmissingfingers++;
        int fingerstoscan = 2 - thumbmissingfingers;
        return fingerstoscan;
    }


    public int fingertypeleft() {
        int fingerType = 0;
        if (!arcDetails.getFingers().contains("LL")) {
            fingerType = RealScan_JNI.RS_FGP_LEFT_LITTLE;
        }
        if (!arcDetails.getFingers().contains("LR")) {
            fingerType = RealScan_JNI.RS_FGP_LEFT_RING;
        }
        if (!arcDetails.getFingers().contains("LM")) {
            fingerType = RealScan_JNI.RS_FGP_LEFT_MIDDLE;
        }
        if (!arcDetails.getFingers().contains("LI")) {
            fingerType = RealScan_JNI.RS_FGP_LEFT_INDEX;
        }

        return fingerType;
    }

    public int fingertyperight() {
        int fingerType = 0;

        if (!arcDetails.getFingers().contains("RL")) {
            fingerType = RealScan_JNI.RS_FGP_RIGHT_LITTLE;
        }
        if (!arcDetails.getFingers().contains("RR")) {
            fingerType = RealScan_JNI.RS_FGP_RIGHT_RING;
        }
        if (!arcDetails.getFingers().contains("RM")) {
            fingerType = RealScan_JNI.RS_FGP_RIGHT_MIDDLE;
        }
        if (!arcDetails.getFingers().contains("RI")) {
            fingerType = RealScan_JNI.RS_FGP_RIGHT_INDEX;
        }

        return fingerType;
    }

    public int fingertypethumb() {
        int fingerType = 0;

        if (!arcDetails.getFingers().contains("RT")) {
            fingerType = RealScan_JNI.RS_FGP_RIGHT_THUMB;
        }
        if (!arcDetails.getFingers().contains("LT")) {
            fingerType = RealScan_JNI.RS_FGP_LEFT_THUMB;
        }
        return fingerType;
    }

    public String leftFingersToScan(List<String> missingFingers) {
        if (missingFingers.size() == 0) return "four";
        List<String> leftmissingfingers = new ArrayList<>();
        List<String> leftfingers = new ArrayList<>();
        StringBuffer fingerstoscan = new StringBuffer("");
        leftfingers.add("LL");
        leftfingers.add("LR");
        leftfingers.add("LM");
        leftfingers.add("LI");

        for (String s : missingFingers) {
            if (s.startsWith("L") && !s.endsWith("T")) {
                leftmissingfingers.add(s);
            }
        }
        if (leftmissingfingers.size() == 0) {
            return "four";
        }
        for (String s : leftfingers) {
            if (leftmissingfingers.contains(s)) {
                continue;
            }
            fingerstoscan.append(s);
            fingerstoscan.append(" ");
        }

        if (fingerstoscan.equals("")) return "four";
        else return fingerstoscan.toString();

    }

    public String rightFingersToScan(List<String> missingFingers) {
        if (missingFingers.size() == 0) return "four";
        List<String> rightmissingfingers = new ArrayList<>();
        List<String> rightfingers = new ArrayList<>();
        StringBuffer fingerstoscan = new StringBuffer("");
        rightfingers.add("RL");
        rightfingers.add("RR");
        rightfingers.add("RM");
        rightfingers.add("RI");

        for (String s : missingFingers) {
            if (s.startsWith("R") && !s.endsWith("T")) {
                rightmissingfingers.add(s);
            }
        }
        if (rightmissingfingers.size() == 0) {
            return "four";
        }
        for (String s : rightfingers) {
            if (rightmissingfingers.contains(s)) {
                continue;
            }
            fingerstoscan.append(s);
            fingerstoscan.append(" ");
        }
        if (fingerstoscan.equals("")) return "four";
        else return fingerstoscan.toString();
    }

    public String thumbFingersToScan(List<String> missingFingers) {

        List<String> thumbmissingfingers = new ArrayList<>();
        List<String> thumbfingers = new ArrayList<>();
        StringBuffer fingerstoscan = new StringBuffer("");
        thumbfingers.add("RT");
        thumbfingers.add("LT");

        for (String s : missingFingers) {
            if (s.endsWith("T")) {
                thumbmissingfingers.add(s);
            }
        }
        if (thumbmissingfingers.size() == 0) {
            return "Two";
        }
        for (String s : thumbfingers) {
            if (thumbmissingfingers.contains(s)) {
                continue;
            }
            fingerstoscan.append(s);
            fingerstoscan.append(" ");
        }
        if (fingerstoscan.equals("")) return "Two";
        else return fingerstoscan.toString();
    }

    public int getMissingFingersLeftCount(List<String> missingFingers) {
        List<String> leftmissingfingers = null;
        for (String s : missingFingers) {
            if (s.startsWith("L") && (!s.endsWith("T"))) {
                leftmissingfingers.add(s);
            }
        }
        return leftmissingfingers.size();
    }


    public int getMissingFingersRightCount(List<String> missingFingers) {
        List<String> rightmissingfingers = null;

        for (String s : missingFingers) {
            if (s.startsWith("R") && (!s.endsWith("T"))) {
                rightmissingfingers.add(s);
            }
        }
        return rightmissingfingers.size();
    }

    public int getMissingThumbsCount(List<String> missingFingers) {
        List<String> missingthumbs = null;
        for (String s : missingFingers) {
            if (s.endsWith("T")) {
                missingthumbs.add(s);
            }
        }
        return missingthumbs.size();
    }

    //To check the correct fingers placed or not
    public void placeCorrectFingersRight() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                statusField.setText("Kindly Place the Correct Fingers and Try Again");
            }
        });
        rightScan.setDisable(false);
        return;
    }

    //To check the correct fingers placed or not
    public void placeCorrectFingersThumb() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                statusField.setText("Kindly Place the Correct Fingers and Try Again");
            }
        });
        thumbScan.setDisable(false);
        return;
    }


}