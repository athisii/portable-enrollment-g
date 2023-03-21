/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.security;

/**
 * @author root
 */

import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HmacUtil {

    private final static Logger LOGGER = ApplicationLog.getLogger(HmacUtil.class);

    //Suppress default constructor for noninstantiability
    private HmacUtil() {
        throw new AssertionError("The HmacUtil methods should be accessed statically");
    }

    private final static String algorithm = "HmacSHA256";
    private final static Mac mac;

    static {
        try {
            mac = Mac.getInstance(algorithm);
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
    }


    public static String genHmacSha256(String message, String key) {
        try {
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] bytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }

    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0, v; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}

