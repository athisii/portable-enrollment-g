package com.cdac.enrollmentstation.security;

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 21/03/23
 */
public class Aes256Util {
    private final static Logger LOGGER = ApplicationLog.getLogger(Aes256Util.class);

    //Suppress default constructor for noninstantiability
    private Aes256Util() {
        throw new AssertionError("The AES256Util methods should be accessed statically");
    }

    private static final Cipher cipher;
    private final static SecureRandom random = new SecureRandom();
    private static byte[] ivBytes = new byte[16];


    static {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
    }

    public static String genUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static Key genKey(String secretKey) {
        return new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

    }

    // returns encrypted bytes
    public static byte[] encrypt(String data, Key key) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            random.nextBytes(ivBytes);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            byteArrayOutputStream.write(ivBytes);
            byteArrayOutputStream.write(encryptedData);
            return byteArrayOutputStream.toByteArray();
        } catch (GeneralSecurityException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);

        }
    }

    public static String decrypt(byte[] ivData, Key key) {
        try {
            ivBytes = Arrays.copyOfRange(ivData, 0, ivBytes.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] actualData = Arrays.copyOfRange(ivData, ivBytes.length, ivData.length);
            byte[] decryptedData = cipher.doFinal(actualData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }
}
