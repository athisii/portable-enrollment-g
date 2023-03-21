package com.cdac.enrollmentstation.security;

import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 21/03/23
 */
public class AesFileUtil {
    private final static Logger LOGGER = ApplicationLog.getLogger(AesFileUtil.class);

    //Suppress default constructor for noninstantiability
    private AesFileUtil() {
        throw new AssertionError("The AesFileUtil methods should be accessed statically");
    }

    private static final Cipher cipher;
    private final static String password = "P0rt@b1eEnr011ment";
    private final static SecureRandom secureRandom = new SecureRandom();
    private static byte[] saltBytes = new byte[8];
    private static byte[] ivBytes = new byte[16];


    static {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
    }

    public static void encrypt(String jsonData, Path encOutputPath) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            secureRandom.nextBytes(saltBytes);
            SecretKey secretKey = getSecretKey(password, saltBytes);
            secureRandom.nextBytes(ivBytes);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encryptedData = cipher.doFinal(jsonData.getBytes(StandardCharsets.UTF_8));
            // writes salt and iv to the file
            // must follow this order while decrypting
            byteArrayOutputStream.write(saltBytes);
            byteArrayOutputStream.write(ivBytes);
            byteArrayOutputStream.write(encryptedData);
            //saves to file
            Files.write(encOutputPath, byteArrayOutputStream.toByteArray());
        } catch (GeneralSecurityException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
    }


    public static String decrypt(Path path) {
        try {
            byte[] readBytes = Files.readAllBytes(path);
            // read order followed as when encrypting
            saltBytes = Arrays.copyOfRange(readBytes, 0, saltBytes.length);
            ivBytes = Arrays.copyOfRange(readBytes, saltBytes.length, saltBytes.length + ivBytes.length);
            byte[] actualData = Arrays.copyOfRange(readBytes, saltBytes.length + ivBytes.length, readBytes.length);

            SecretKey secretKey = getSecretKey(password, saltBytes);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(actualData);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }

    }

    public static SecretKey getSecretKey(String password, byte[] salt) {
        try {
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 10000, 128);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            return new SecretKeySpec(secretKey.getEncoded(), "AES");
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
    }


}
