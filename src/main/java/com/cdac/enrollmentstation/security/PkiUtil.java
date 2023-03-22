package com.cdac.enrollmentstation.security;

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;

import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 21/03/23
 */
public class PkiUtil {
    private final static Logger LOGGER = ApplicationLog.getLogger(PKIUtil.class);

    //Suppress default constructor for noninstantiability
    private PkiUtil() {
        throw new AssertionError("The PkiUtil methods should be accessed statically");
    }

    private final static KeyStore keyStore;
    private final static InputStream inputStream;
    private final static String password = "12qwaszx";// TODO: get from PropertyFile.
    private final static String alias = "sample encryption test"; // TODO: get from PropertyFile.
    private final static Cipher cipher;
    private final static KeyPair keyPair;

    static {
        try {
            inputStream = new FileInputStream("/usr/share/enrollment/jks/cacert.jks"); //TODO get from PropertyFile.
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, password.toCharArray());
            if (!keyStore.containsAlias(alias)) {
                throw new GenericException("Alias for key not found");
            }
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate certificate = keyStore.getCertificate(alias);
            PublicKey publicKey = certificate.getPublicKey();
            keyPair = new KeyPair(publicKey, privateKey);
            cipher = Cipher.getInstance("RSA");
        } catch (GeneralSecurityException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
    }


    // return encrypts bytes
    public static byte[] encrypt(String data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

    }

    // return decrypted String
    public static String decrypt(byte[] encryptedData) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedInput = cipher.doFinal(encryptedData);
            return new String(decryptedInput, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

    }
}
