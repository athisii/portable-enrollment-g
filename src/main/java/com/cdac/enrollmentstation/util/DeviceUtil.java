package com.cdac.enrollmentstation.util;

import RealScan.RealScan_JNI;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.mantra.midirisenroll.MIDIrisEnroll;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;

/**
 * @author athisii, CDAC
 * Created on 17/12/22
 */
public class DeviceUtil {
    //Suppress default constructor for noninstantiability
    private DeviceUtil() {
        throw new AssertionError("The DeviceUtil methods should be accessed statically");
    }

    // returns true if number of connected device >= expectedNoOfDev else false
    public static boolean isFpScannerConnected(int expectedNoOfDev) {
        return RealScan_JNI.RS_InitSDK(null, 0) >= expectedNoOfDev;
    }

    public static boolean isIrisConnected() {
        var midIrisEnroll = new MIDIrisEnroll(null);
        var devices = new ArrayList<String>();
        int result = midIrisEnroll.GetConnectedDevices(devices);
        if (result != 0) {
            return false;
        }
        return !devices.isEmpty();
    }

    public static boolean isCameraConnected() {
        // automatically call open() if camera index passed when instantiated.
        var videoCapture = new VideoCapture(Integer.parseInt(PropertyFile.getProperty(PropertyName.CAMERA_ID)));
        if (!videoCapture.isOpened()) {
            return false;
        }
        videoCapture.release();
        return true;
    }

    // TODO -- to be implemented
    // throws GenericRuntime Exception
    public static void checkBarcodeStatus() {
    }

    // TODO -- to be implemented
    // throws GenericRuntime Exception
    public static void isCardReaderConnected() {
    }
}
