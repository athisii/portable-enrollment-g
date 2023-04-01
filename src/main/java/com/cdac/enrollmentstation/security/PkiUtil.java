package com.cdac.enrollmentstation.security;

import com.cdac.enrollmentstation.App;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;

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
    private static final Logger LOGGER = ApplicationLog.getLogger(App.class);

    //Suppress default constructor for noninstantiability
    private PkiUtil() {
        throw new AssertionError("The PkiUtil methods must be accessed statically.");
    }

    private static final KeyStore keyStore;
    private static final InputStream inputStream;
    private static final String password;
    private static final String alias;
    private static final Cipher cipher;
    private final static KeyPair keyPair;

    static {
        try {
            password = PropertyFile.getProperty(PropertyName.JKS_PASSWORD);
            alias = PropertyFile.getProperty(PropertyName.JKS_ALIAS);
            inputStream = new FileInputStream(PropertyFile.getProperty(PropertyName.JKS_CERT_FILE));
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, password.toCharArray());
            if (!keyStore.containsAlias(alias)) {
                throw new GenericException("Not found for key with alias: " + alias);
            }
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate certificate = keyStore.getCertificate(alias);
            PublicKey publicKey = certificate.getPublicKey();
            keyPair = new KeyPair(publicKey, privateKey);
            cipher = Cipher.getInstance("RSA");
        } catch (GeneralSecurityException | IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }


    // return encrypted bytes
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
