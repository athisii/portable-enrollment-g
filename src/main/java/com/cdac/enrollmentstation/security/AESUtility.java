package com.cdac.enrollmentstation.security;



import java.io.File;
import java.awt.image.BufferedImage;

public class AESUtility
{
   
    private CryptoAES256 cryptoAES256;
    private Boolean cryptoMode256bit;
    
    public AESUtility(final String key) {
        this.cryptoMode256bit = true;
        this.cryptoAES256 = new CryptoAES256(key);
       
    }
    
    public void encryptImageStream(final BufferedImage image, final File file) throws Exception {
        if (this.cryptoMode256bit) {
            this.cryptoAES256.encryptImageStream(image, file);
        }
    }
    
    public void encryptImg(final String inFilePath, final String outFilePath) throws Exception {
        if (this.cryptoMode256bit) {
            this.cryptoAES256.encryptImg(inFilePath, outFilePath);
        }
       
    }
    
    public void decryptImg(final String inFilePath, final String outFilePath) throws Exception {
    	System.out.println("outFilePath :"+outFilePath);
        if (this.cryptoMode256bit) {
            this.cryptoAES256.decryptImg(inFilePath, outFilePath);
        }
       
    }
    
    public Boolean encryptFile256(final String inFilePath, final String outFilePath) throws Exception {
        if (this.cryptoMode256bit) {
            return this.cryptoAES256.encryptFile(inFilePath, outFilePath);
        }
        return null;
    }
    
    public Boolean decryptFile256(final String inFilePath, final String outFilePath) throws Exception {
        if (this.cryptoMode256bit) {
            return this.cryptoAES256.decryptFile(inFilePath, outFilePath);
        }
        return null;
    }
    
    public Boolean encryptFile256(final File inFilePath, final File outFilePath) throws Exception {
        if (this.cryptoMode256bit) {
            return this.cryptoAES256.encryptFile(inFilePath, outFilePath);
        }
        return null;
    }
    
    public Boolean decryptFile256(final File inFilePath, final File outFilePath) throws Exception {
        if (this.cryptoMode256bit) {
            return this.cryptoAES256.decryptFile(inFilePath, outFilePath);
        }
        return null;
    }
    
//    public String decryptString256(final String encryptedData) throws Exception {
//        if (this.cryptoMode256bit) {
//            return this.cryptoAES256.decryptString(encryptedData);
//        }
//        return null;
//    }
    
    
}
