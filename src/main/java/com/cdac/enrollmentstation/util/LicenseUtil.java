package com.cdac.enrollmentstation.util;

import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.innovatrics.iengine.ansiiso.AnsiIso;

import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 17/12/22
 */
public class LicenseUtil {
    private static final Logger LOGGER = ApplicationLog.getLogger(LicenseUtil.class);
    private static AnsiIso ansiIso;

    //Suppress default constructor for noninstantiability
    private LicenseUtil() {
        throw new AssertionError("The LicenseUtil methods must be accessed statically.");
    }

    // TODO -- to be implemented
    // throws GenericException
    public static void getAnsiIso() {
        throw new AssertionError("Not implemented.");
    }

    // TODO -- to be implemented
    // throws GenericException
    public static void checkFpScannerLicense() {
        throw new AssertionError("Not implemented.");
    }

    // TODO -- to be implemented
    // throws GenericException
    public static void checkIrisLicense() {
        throw new AssertionError("Not implemented.");
    }

    // TODO -- to be implemented
    // throws GenericException
    public static void checkBarcodeLicense() {
        throw new AssertionError("Not implemented.");
    }

    // TODO -- to be implemented
    // throws GenericException
    public static void checkCardReaderLicense() {
        throw new AssertionError("Not implemented.");
    }
}
