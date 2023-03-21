/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.security;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;


/**
 * @author K. Karthikeyan
 */

public class AESFileEncryptionDecryption {

    public static SecretKey getKeyFromPassword(String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 128);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static byte[] getIV() {
        byte[] iv = new byte[128 / 8];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }

    public static IvParameterSpec getIVSpec(byte[] iv) {
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        return ivspec;
    }

    static private String processFile(Cipher ci, InputStream in, OutputStream out) {

        try {
            byte[] ibuf = new byte[1024];
            int len;
            while ((len = in.read(ibuf)) != -1) {
                byte[] obuf = ci.update(ibuf, 0, len);
                if (obuf != null) {
                    out.write(obuf);
                }
            }
            byte[] obuf = ci.doFinal();
            if (obuf != null) out.write(obuf);
        } catch (javax.crypto.IllegalBlockSizeException e) {
            return "Error :" + e.toString();
        } catch (javax.crypto.BadPaddingException e) {
            return "Error :" + e.toString();
        } catch (java.io.IOException e) {
            return "Error :" + e.toString();
        }
        return "Success";

    }

    public static String encryptFile(String inputFile, String outputFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String password = "P0rt@b1eEnr011ment";
        String status = "";
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[8];
        secureRandom.nextBytes(salt);

        SecretKey skey = getKeyFromPassword(password, salt);
        byte[] iv = getIV();
        IvParameterSpec ivSpec = getIVSpec(iv);

        File oFile = new File(outputFile);
        oFile.createNewFile();


        FileOutputStream out = new FileOutputStream(outputFile);
        out.write(salt);
        out.write(iv);

        Cipher ci = Cipher.getInstance("AES/CBC/PKCS5Padding");
        ci.init(Cipher.ENCRYPT_MODE, skey, ivSpec);


        try (FileInputStream in = new FileInputStream(inputFile)) {
            status = processFile(ci, in, out);
        } catch (Exception e) {
            status = "Error :" + e.toString();
        }
        return status;
    }

    public static void decryptFile(String inputFile, String outputFile) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        System.out.println("INPUT FILE:::" + inputFile);
        System.out.println("OUTPUT FILE:::" + outputFile);
        FileInputStream in = new FileInputStream(inputFile);
        byte[] salt = new byte[8];
        byte[] iv = getIV();
        String password = "P0rt@b1eEnr011ment";
        in.read(salt);
        in.read(iv);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec skey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher ci = Cipher.getInstance("AES/CBC/PKCS5Padding");
        ci.init(Cipher.DECRYPT_MODE, skey, new IvParameterSpec(iv));

        File oFile = new File(outputFile);
        oFile.createNewFile();

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            processFile(ci, in, out);
        }
        //code to delete encryptedfile
        //delFile(inputFile);
    }

    public static void delFile(String file) throws IOException {
        try {
            Files.deleteIfExists(Paths.get(file));
        } catch (NoSuchFileException e) {
            System.out.println("No such file/directory exists");
        } catch (DirectoryNotEmptyException e) {
            System.out.println("Directory is not empty.");
        }
        System.out.println("Deletion successful.");
    }

    void decryptFile(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
