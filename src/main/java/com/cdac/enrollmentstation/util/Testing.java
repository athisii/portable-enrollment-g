/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.util;

import com.cdac.enrollmentstation.security.AESFileEncryptionDecryption;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author K. Karthikeyan
 */
public class Testing {
    public static void main() throws FileNotFoundException, InvalidAlgorithmParameterException {
        System.out.println("ghg");
    }


    public void aess() throws FileNotFoundException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        String secretkey = "portable_enrollment";

        try {
            AESFileEncryptionDecryption encDec = new AESFileEncryptionDecryption();
            System.out.println("inside aess dfkldfdfdl");
            String origFile = "/tmp/encdec/orig/orig.txt";
            String encFile = "/tmp/encdec/enc/orig.txt.enc";
            String decFile = "/tmp/encdec/dec/orig_afterenc_dec.txt";
            //encDec.encryptFile(secretkey, "/tmp/encdec/orig/b4enc.txt", "/tmp/encdec/enc/b4enc.txt.enc");
            encDec.encryptFile(origFile, encFile);
            //System.out.println("After encryption");
            //encDec.decryptFile(secretkey, "/tmp/encdec/enc/b4enc.txt.enc", "/tmp/encdec/dec/b4enc.txt.enc");
            encDec.decryptFile(encFile, decFile);
            System.out.println("After decryption");
        } catch (IOException ex) {
            Logger.getLogger(Testing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Testing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(Testing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(Testing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(Testing.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(Testing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}
