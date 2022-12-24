/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.security;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author root
 */
public class PKIUtil {
    
    public String encrypt(String inputStr) throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        // for clarity, ignoring exceptions and failures
        InputStream keystoreStream = new FileInputStream("/usr/share/enrollment/jks/cacert.jks"); 
        
        KeyStore keystore = KeyStore.getInstance("PKCS12"); 
        keystore.load(keystoreStream, "12qwaszx".toCharArray()); 
        if (!keystore.containsAlias("sample encryption test")) { 
         throw new RuntimeException("Alias for key not found"); 
        } 
            Key key = (PrivateKey) keystore.getKey("sample encryption test", "12qwaszx".toCharArray()); 
            java.security.cert.Certificate cert = keystore.getCertificate("sample encryption test");
            
            PublicKey pub = cert.getPublicKey();
            byte[] encodedCertKey = cert.getEncoded();
            byte[] encodedPublicKey = pub.getEncoded();
            String b64PublicKey = Base64.getMimeEncoder().encodeToString(encodedPublicKey);
            String b64CertKey = Base64.getMimeEncoder().encodeToString(encodedCertKey);
            System.out.println("public key : "+ b64PublicKey);
            KeyPair kPair = new KeyPair(pub, (PrivateKey) key);
            Cipher cipher = Cipher.getInstance("RSA");
            
            cipher.init(Cipher.ENCRYPT_MODE, kPair.getPublic());
            //byte[] textEncrypted = cipher.doFinal("hello world123".getBytes());
            byte[] textEncrypted = cipher.doFinal(inputStr.getBytes());
            System.out.println("encrypted: "+new String(textEncrypted));
            String encryptedSessionKey = new String(textEncrypted);
            
            cipher.init(Cipher.DECRYPT_MODE, kPair.getPrivate());
            byte[] textDecrypted = cipher.doFinal(textEncrypted);
            System.out.println("decrypted: "+new String(textDecrypted));
            //return "";
            return encryptedSessionKey;
            }
    
    public byte[] encrypt_test(String inputStr) throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        // for clarity, ignoring exceptions and failures
        InputStream keystoreStream = new FileInputStream("/usr/share/enrollment/jks/cacert.jks"); 
        
        KeyStore keystore = KeyStore.getInstance("PKCS12"); 
        keystore.load(keystoreStream, "12qwaszx".toCharArray()); 
        if (!keystore.containsAlias("sample encryption test")) { 
         throw new RuntimeException("Alias for key not found"); 
        } 
            Key key = (PrivateKey) keystore.getKey("sample encryption test", "12qwaszx".toCharArray()); 
            java.security.cert.Certificate cert = keystore.getCertificate("sample encryption test");
            
            PublicKey pub = cert.getPublicKey();
            byte[] encodedCertKey = cert.getEncoded();
            byte[] encodedPublicKey = pub.getEncoded();
            String b64PublicKey = Base64.getMimeEncoder().encodeToString(encodedPublicKey);
            String b64CertKey = Base64.getMimeEncoder().encodeToString(encodedCertKey);
            System.out.println("public key : "+ b64PublicKey);
            KeyPair kPair = new KeyPair(pub, (PrivateKey) key);
            Cipher cipher = Cipher.getInstance("RSA");
            
            cipher.init(Cipher.ENCRYPT_MODE, kPair.getPublic());
            //byte[] textEncrypted = cipher.doFinal("hello world123".getBytes());
            byte[] textEncrypted = cipher.doFinal(inputStr.getBytes());
            System.out.println("encrypted: "+new String(textEncrypted));
            String encryptedSessionKey = new String(textEncrypted);
            
            cipher.init(Cipher.DECRYPT_MODE, kPair.getPrivate());
            byte[] textDecrypted = cipher.doFinal(textEncrypted);
            System.out.println("decrypted: "+new String(textDecrypted));
            //return "";
            return textEncrypted;
            }
    
    public String decrypt_test(String inputStr) throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        // for clarity, ignoring exceptions and failures
        InputStream keystoreStream = new FileInputStream("/usr/share/enrollment/jks/cacert.jks"); 
        
        KeyStore keystore = KeyStore.getInstance("PKCS12"); 
        keystore.load(keystoreStream, "12qwaszx".toCharArray()); 
        if (!keystore.containsAlias("sample encryption test")) { 
         throw new RuntimeException("Alias for key not found"); 
        } 
            Key key = (PrivateKey) keystore.getKey("sample encryption test", "12qwaszx".toCharArray()); 
            java.security.cert.Certificate cert = keystore.getCertificate("sample encryption test");
            
            PublicKey pub = cert.getPublicKey();
            byte[] encodedCertKey = cert.getEncoded();
            byte[] encodedPublicKey = pub.getEncoded();
            String b64PublicKey = Base64.getMimeEncoder().encodeToString(encodedPublicKey);
            String b64CertKey = Base64.getMimeEncoder().encodeToString(encodedCertKey);
            System.out.println("public key : "+ b64PublicKey);
            KeyPair kPair = new KeyPair(pub, (PrivateKey) key);
            Cipher cipher = Cipher.getInstance("RSA");
            
           // cipher.init(Cipher.ENCRYPT_MODE, kPair.getPublic());
            //byte[] textEncrypted = cipher.doFinal("hello world123".getBytes());
//            byte[] textEncrypted = cipher.doFinal(inputStr.getBytes());
//            System.out.println("encrypted: "+new String(textEncrypted));
//            String encryptedSessionKey = new String(textEncrypted);
            
            cipher.init(Cipher.DECRYPT_MODE, kPair.getPrivate());
            byte[] textDecrypted = cipher.doFinal(inputStr.getBytes());
            System.out.println("decrypted: "+new String(textDecrypted));
            String decryptedSessionKey = new String(textDecrypted);
            //return "";
            return decryptedSessionKey;
            }
    
    public String decrypt(byte[] inputStr) throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        // for clarity, ignoring exceptions and failures
        InputStream keystoreStream = new FileInputStream("/usr/share/enrollment/jks/cacert.jks"); 
        
        KeyStore keystore = KeyStore.getInstance("PKCS12"); 
        keystore.load(keystoreStream, "12qwaszx".toCharArray()); 
        if (!keystore.containsAlias("sample encryption test")) { 
         throw new RuntimeException("Alias for key not found"); 
        } 
            Key key = (PrivateKey) keystore.getKey("sample encryption test", "12qwaszx".toCharArray()); 
            java.security.cert.Certificate cert = keystore.getCertificate("sample encryption test");
            
            PublicKey pub = cert.getPublicKey();
            byte[] encodedCertKey = cert.getEncoded();
            byte[] encodedPublicKey = pub.getEncoded();
            String b64PublicKey = Base64.getMimeEncoder().encodeToString(encodedPublicKey);
            String b64CertKey = Base64.getMimeEncoder().encodeToString(encodedCertKey);
            System.out.println("public key : "+ b64PublicKey);
            KeyPair kPair = new KeyPair(pub, (PrivateKey) key);
            Cipher cipher = Cipher.getInstance("RSA");
            
            cipher.init(Cipher.ENCRYPT_MODE, kPair.getPublic());
            
            byte[] textEncrypted = cipher.doFinal("hello world123".getBytes());
            System.out.println("encrypted: "+new String(textEncrypted));
            
            
            cipher.init(Cipher.DECRYPT_MODE, kPair.getPrivate());
//            byte[] textDecrypted = cipher.doFinal(textEncrypted);
//            System.out.println("decrypted: "+new String(textDecrypted));
            byte[] textDecrypted = cipher.doFinal(inputStr);
            System.out.println("decrypted Session Key: "+new String(textDecrypted));
            String decryptedSessionKey = new String(textDecrypted);
            
            return decryptedSessionKey;
            }
    
}
