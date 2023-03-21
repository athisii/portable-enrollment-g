package com.cdac.enrollmentstation.security;


import java.io.IOException;
import org.apache.commons.codec.binary.Base64;
import java.io.InputStream;
import javax.crypto.CipherInputStream;
import java.io.FileInputStream;
//import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import java.io.OutputStream;
import javax.crypto.CipherOutputStream;
import java.io.FileOutputStream;
import javax.crypto.Cipher;
import java.io.File;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CryptoAES256
{
    private static final String ALGO = "AES";
    private byte[] keyValue;
    private  String imgExt = "JPG";
    
    
    public CryptoAES256() {
        
    }
    public CryptoAES256(final String strKey) {
        this.imgExt = "JPG";
        try {
            this.keyValue = strKey.getBytes("UTF-8");
        }
        catch (Exception ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Key generateKey() throws Exception {
        final MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha.digest(this.keyValue);
        digest = Arrays.copyOf(digest, 16);
        final Key key = new SecretKeySpec(digest, "AES");
        return key;
    }
    
    
    
    public void encryptImageStream(final BufferedImage image, final File file) throws Exception {
        FileOutputStream output = null;
        CipherOutputStream cos = null;
        try {
            final Key key = this.generateKey();
            final Cipher cpr = Cipher.getInstance("AES");
            cpr.init(1, key);
            output = new FileOutputStream(file);
            cos = new CipherOutputStream(output, cpr);
            ImageIO.write(image, "JPG", cos);
        }
        finally {
            if (output != null) {
                output.close();
            }
            if (cos != null) {
                cos.close();
            }
        }
    }
    
    public void encryptImg(final String inFilePath, final String outFilePath) throws Exception {
        FileOutputStream output = null;
        CipherOutputStream cos = null;
        try {
            final Key key = this.generateKey();
            final File imgPath = new File(inFilePath);
            final BufferedImage bufferedImageIn = ImageIO.read(imgPath);
            final Cipher cpr = Cipher.getInstance("AES");
            cpr.init(1, key);
            output = new FileOutputStream(outFilePath);
            cos = new CipherOutputStream(output, cpr);
            ImageIO.write(bufferedImageIn, "JPG", cos);
        }
        finally {
            if (output != null) {
                output.close();
            }
            if (cos != null) {
                cos.close();
            }
        }
    }
    
    public void decryptImg(final String inFilePath, final String outFilePath) throws Exception {
        FileInputStream fileinput = null;
        CipherInputStream cis = null;
        try {
            final Key key = this.generateKey();
            final Cipher cpr = Cipher.getInstance("AES");
            cpr.init(2, key);
            fileinput = new FileInputStream(inFilePath);
            cis = new CipherInputStream(fileinput, cpr);
            final BufferedImage input = ImageIO.read(cis);
            ImageIO.write(input, "JPG", new File(outFilePath));
        }
        finally {
            if (fileinput != null) {
                fileinput.close();
            }
            if (cis != null) {
                cis.close();
            }
        }
    }

    public boolean decryptFile(final File inFilePath, final File outFilePath) throws Exception {
        FileInputStream fileinput = null;
        CipherInputStream cis = null;
        FileOutputStream fos = null;
        try {
            final Key key = this.generateKey();
            final Cipher cpr = Cipher.getInstance("AES");
            cpr.init(2, key);
            fileinput = new FileInputStream(inFilePath);
            cis = new CipherInputStream(fileinput, cpr);
            fos = new FileOutputStream(outFilePath);
            this.writeData(cis, fos);
            return true;
        }
        finally {
            if (fileinput != null) {
                fileinput.close();
            }
            if (cis != null) {
                cis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public String generateRandomUUID(){
        return java.util.UUID.randomUUID().toString();
    }
    
    public static byte[] getRandomNonce() {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        return nonce;
  }
    
    public String encryptString(final String jsonData,   Key key) throws Exception {
        //final Key key = this.generateKey();
        final Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        c.init(1, key, ivSpec);
        System.out.println("before enc");
        //final byte[] decodedValue = Base64.decodeBase64(encryptedData);
        final byte[] encValue = c.doFinal(jsonData.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(iv);
        baos.write(encValue);
        final byte[] encValueWithIV = baos.toByteArray();
        System.out.println("After enc");
        final String encryptedValue = java.util.Base64.getEncoder().encodeToString(encValueWithIV);
        return encryptedValue;
    }
    
    public String decryptString(final String jsonData) {
        System.out.println("keyvalue :" + keyValue + " "+ALGO);
        Key key = new SecretKeySpec(keyValue, "AES");
        System.out.println("After key init");
        final Cipher c;
        final byte[] decValue;
        try {
            final byte[] decodedValue = Base64.decodeBase64(jsonData);
            byte[] iv = Arrays.copyOfRange(decodedValue, 0,16);
            System.out.println("iv length :"+iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            System.out.println("ivspec length :"+ivSpec.getIV().length);
            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            System.out.println("After cipher getinstance"+ivSpec.getIV().length);
            c.init(2, key,ivSpec);
            System.out.println("After c.init"+ivSpec.getIV().length);
            System.out.println("before decrypt" + jsonData);
            
            byte[] actualdata = Arrays.copyOfRange(decodedValue, 16, decodedValue.length);
            System.out.println("decoded bytes "+ decodedValue.toString());
            decValue = c.doFinal(actualdata);
            
       // final byte[] decodedValue = Base64.decodeBase64(jsonData);
        
        System.out.println("After dec");
        final String decryptedValue = new String(decValue);
        return decryptedValue;
        }   
         catch (InvalidKeyException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IllegalBlockSizeException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public String decryptStringSK(final String jsonData, Key key) {
        System.out.println("keyvalue :" + keyValue + " "+ALGO);
       
        final Cipher c;
        final byte[] decValue;
        try {
            final byte[] decodedValue = Base64.decodeBase64(jsonData);
            byte[] iv = Arrays.copyOfRange(decodedValue, 0,16);
            System.out.println("iv length :"+iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            System.out.println("ivspec length :"+ivSpec.getIV().length);
            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(2, key,ivSpec);
            System.out.println("before decrypt" + jsonData);
            
            byte[] actualdata = Arrays.copyOfRange(decodedValue, 16, decodedValue.length);
            System.out.println("decoded bytes "+ decodedValue.toString());
            decValue = c.doFinal(actualdata);
            
       // final byte[] decodedValue = Base64.decodeBase64(jsonData);
        
        System.out.println("After dec");
        final String decryptedValue = new String(decValue);
        return decryptedValue;
        }   
         catch (InvalidKeyException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IllegalBlockSizeException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(CryptoAES256.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    
    private void writeData(final InputStream is, final OutputStream os) throws IOException {
        try {
            final byte[] buffer = new byte[64];
            int numBytes;
            while ((numBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, numBytes);
            }
        }
        finally {
            if (os != null) {
                os.flush();
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }
    
    public Key generateKey32(String key) {

        
        Key aeskey = new SecretKeySpec(key.getBytes(), "AES");
        return aeskey;
}


    
    public static void main(final String[] args) throws Exception {
        final CryptoAES256 algo = new CryptoAES256("CAA8F9CB2FC642CA");
        algo.decryptImg("/opt/examsoft/qpextracted/121_A_ques/english_1/english_1/I_1.jpg", "/opt/examsoft/qpextracted/121_A_ques/english_1/english_1/dec_1.jpg");
    }
}
